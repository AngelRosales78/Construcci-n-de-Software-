"""Enforce known company identities independently of the language model."""
import re
import unicodedata
from .models import Company

ALIASES={'MSFT':('microsoft',),'AAPL':('apple',),'TSLA':('tesla',),'GOOGL':('google','alphabet'),
         'GOOG':('google','alphabet'),'META':('meta platforms','facebook'),'AMZN':('amazon',),'NVDA':('nvidia',),'KO':('coca cola',)}
AMBIGUOUS={'meta','target','principal','united','general','first','next','a','i','on','it','so','all','are','see','has','be','now','for',
           'es','no','en','si','de','la','el','un','se','lo','por','con','que','ia','roe','roa','per','z'}


def normalized(text):
    return re.sub(r'[^a-z0-9]+',' ',''.join(c for c in unicodedata.normalize('NFKD',text.lower()) if not unicodedata.combining(c))).strip()


def outside_context(question,tickers):
    text=' '+normalized(question)+' '
    explicit={word.upper() for word in re.findall(r'\$?\b[A-Z][A-Z0-9.\-]{1,9}\b',question) if word.lower() not in AMBIGUOUS}
    mentioned=set()
    for ticker,name in Company.objects.values_list('ticker','name'):
        clean=re.sub(r'\b(incorporated|corporation|company|holdings|holding|corp|inc|plc|ltd|limited|class [ac])\b',' ',normalized(name))
        clean=' '.join(clean.split())
        aliases=(*ALIASES.get(ticker,()),clean)
        if ticker in explicit or any(len(alias)>=4 and alias not in AMBIGUOUS and ' '+alias+' ' in text for alias in aliases):
            mentioned.add(ticker)
    # Google has two listed share classes; a company name alone must not force a false mismatch.
    if mentioned & {'GOOG','GOOGL'} and set(tickers) & {'GOOG','GOOGL'}:
        mentioned-={'GOOG','GOOGL'}
    return bool(mentioned-set(tickers))
