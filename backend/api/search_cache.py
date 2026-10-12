"""Cachés de la épica 3, separadas de la revocación de sesiones."""
import json
import uuid
from redis.exceptions import RedisError
from .redis_utils import get_redis_connection

AUTOCOMPLETE_KEY = 'sp500:autocomplete:active'
HISTORY_TTL = 3600

def preload_autocomplete(companies):
    """Publica un índice completo mediante un puntero; nunca expone un ZSET parcial."""
    r = get_redis_connection()
    key = f'sp500:autocomplete:{uuid.uuid4().hex}'
    entries = {}
    for company in companies:
        payload = json.dumps({'ticker': company.ticker, 'name': company.name}, ensure_ascii=False)
        for text in {company.ticker.casefold(), company.name.casefold()}:
            entries[f'{text}\x00{payload}'] = 0
    if not entries:
        entries['\x00{}'] = 0
    pipe = r.pipeline(transaction=True)
    pipe.zadd(key, entries)
    pipe.expire(key, 86400)
    pipe.set(AUTOCOMPLETE_KEY, key, ex=86400)
    pipe.execute()
    return len(entries)

def autocomplete(prefix, limit=10):
    prefix = prefix.strip().casefold()
    if not prefix or len(prefix) > 200:
        return []
    r = get_redis_connection()
    key = r.get(AUTOCOMPLETE_KEY)
    if not key:
        return []
    members = r.zrangebylex(key, f'[{prefix}', f'[{prefix}\U0010ffff', start=0, num=limit*2)
    suggestions = {}
    for member in members:
        item = json.loads(member.split('\x00', 1)[1])
        suggestions.setdefault(item['ticker'], item)
        if len(suggestions) == limit:
            break
    return list(suggestions.values())

def history_key(user_id):
    return f'sp500:history:{user_id}'

def cache_history(user_id, rows):
    """La base de datos es la fuente de verdad; el espejo Redis contiene como máximo diez."""
    try:
        r = get_redis_connection()
        pipe = r.pipeline(transaction=True)
        pipe.delete(history_key(user_id))
        if rows:
            pipe.rpush(history_key(user_id), *[json.dumps(row) for row in rows[:10]])
            pipe.expire(history_key(user_id), HISTORY_TTL)
        pipe.execute()
    except RedisError:
        # Una caída de caché no revierte una escritura confirmada en PostgreSQL.
        pass
