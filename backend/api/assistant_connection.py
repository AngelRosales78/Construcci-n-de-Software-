"""Server-only connection to the configured, OpenAI-compatible assistant service."""
import json
import re
import time
from urllib.parse import urlsplit
import urllib.request
from django.conf import settings
from django.utils import timezone
from .monitoring_services import reserve_call, record_ai


def connected():
    return bool(settings.ASSISTANT_API_BASE_URL and settings.ASSISTANT_MODEL)


def identity():
    return {'model': settings.ASSISTANT_MODEL if connected() else 'Qwen3 1.7B',
            'conversational': connected()}


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # A configured credential must never follow a redirect to another host.
        return None


def complete_json(messages, schema, max_tokens=650):
    base=settings.ASSISTANT_API_BASE_URL.rstrip('/')
    parsed=urlsplit(base)
    if parsed.scheme not in ('http','https') or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment:
        raise ValueError('invalid_assistant_endpoint')
    payload={'model':settings.ASSISTANT_MODEL,'messages':messages,'temperature':0,'max_tokens':max_tokens,
             'stream':False,'chat_template_kwargs':{'enable_thinking':False},
             'response_format':{'type':'json_schema','json_schema':{'name':'findscore_response','strict':True,'schema':schema}}}
    headers={'Content-Type':'application/json','Accept':'application/json'}
    if settings.ASSISTANT_API_KEY:
        headers['Authorization']='Bearer '+settings.ASSISTANT_API_KEY
    request=urllib.request.Request(base+'/chat/completions',data=json.dumps(payload,ensure_ascii=False).encode(),headers=headers)
    reserve_call('local_ai')
    started,clock,observed,outcome=timezone.now(),time.monotonic(),{},'error'
    try:
        with urllib.request.build_opener(NoRedirect()).open(request,timeout=25) as response:
            raw=response.read(48001)
        if len(raw)>48000:
            raise ValueError('assistant_response_too_large')
        data=json.loads(raw)
        usage=data.get('usage') or {}
        observed={'tokens_evaluated':usage.get('prompt_tokens'),'tokens_predicted':usage.get('completion_tokens')}
        choice=data['choices'][0]
        if choice.get('finish_reason') not in ('stop',None):
            raise ValueError('incomplete_assistant_response')
        content=choice['message']['content']
        if not isinstance(content,str) or len(content)>14000 or re.search(r'<think>|```',content):
            raise ValueError('invalid_assistant_json')
        result=json.loads(content)
        outcome='success'
        return result
    finally:
        record_ai(started,time.monotonic()-clock,outcome,observed,model=settings.ASSISTANT_MODEL)
