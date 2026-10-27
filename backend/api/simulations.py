"""HU06_01 T25: Monte Carlo reproducible sobre cierres ajustados persistidos."""
import calendar
import hashlib
import json
import math
import random
from datetime import date
from statistics import mean, stdev

VERSION = 'gbm-monthly-v1'
PATHS = 2000
STEPS_PER_MONTH = 21
SCENARIOS = {'base': 0.5}


def quantile(values, probability):
    ordered = sorted(values)
    position = (len(ordered) - 1) * probability
    lower = int(position)
    upper = min(lower + 1, len(ordered) - 1)
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (position - lower)


def month_after(day, months):
    year, month = divmod(day.year * 12 + day.month - 1 + months, 12)
    return date(year, month + 1, min(day.day, calendar.monthrange(year, month + 1)[1]))


def source_snapshot(rows):
    snapshot = [{'date': str(row['date']), 'adjusted_close': str(row['adjusted_close']),
                 'currency': row['currency'], 'source': row['source'], 'is_demo': row['is_demo']} for row in rows]
    digest = hashlib.sha256(json.dumps(snapshot, sort_keys=True, separators=(',', ':')).encode()).hexdigest()
    return snapshot, digest


def run_simulation(rows, seed, parameters=None):
    snapshot, digest = source_snapshot(rows)
    unavailable = {'status': 'unavailable', 'reason': 'insufficient_history',
                   'available_observations': len(rows), 'required_observations': 253}
    if len(rows) < 253:
        return unavailable, snapshot
    prices = [float(row['adjusted_close']) for row in rows]
    if (len({row['currency'] for row in rows}) != 1 or
            len({row['date'] for row in rows}) != len(rows) or
            any(not math.isfinite(price) or price <= 0 for price in prices) or
            any(rows[i]['date'] >= rows[i + 1]['date'] for i in range(len(rows) - 1))):
        return unavailable | {'reason': 'invalid_history'}, snapshot
    returns = [math.log(b / a) for a, b in zip(prices, prices[1:])]
    sigma = stdev(returns) * math.sqrt(252)
    drift = mean(returns) * 252 + 0.5 * sigma * sigma
    defaults = {'annual_return': drift, 'annual_volatility': sigma}
    effective = defaults | (parameters or {})
    if any(not math.isfinite(value) or abs(value) > 10 for value in effective.values()):
        return unavailable | {'reason': 'unstable_parameters'}, snapshot
    rng = random.Random(seed)
    paths = [[prices[-1]] for _ in range(PATHS)]
    mu, volatility = effective['annual_return'], effective['annual_volatility']
    for month in range(1, 13):
        for path in paths:
            price = path[-1]
            for _ in range(STEPS_PER_MONTH):
                price *= math.exp((mu - volatility * volatility / 2) / 252 + volatility / math.sqrt(252) * rng.gauss(0, 1))
            path.append(price)
    dates = [str(month_after(rows[-1]['date'], month)) for month in range(13)]
    curves = {name: [{'date': day, 'price': round(quantile([path[i] for path in paths], q), 6)}
                     for i, day in enumerate(dates)] for name, q in SCENARIOS.items()}
    return {'status': 'available', 'model_version': VERSION, 'seed': seed, 'paths': PATHS,
            'horizon_months': 12, 'steps_per_month': STEPS_PER_MONTH,
            'currency': rows[-1]['currency'], 'as_of': str(rows[-1]['date']),
            'historical_start': str(rows[0]['date']), 'observations': len(rows),
            'source_sha256': digest, 'sources': sorted({row['source'] for row in rows}),
            'is_demo': any(row['is_demo'] for row in rows),
            'defaults': defaults, 'parameters': effective, 'scenarios': curves,
            'percentiles': SCENARIOS,
            'assumptions': 'Movimiento browniano geométrico; 252 pasos anuales y 13 puntos mensuales. '
                           'Tasa anual continua del modelo, volatilidad constante, sin comisiones ni impuestos. '
                           'Horizonte desde el último cierre importado; no es una predicción garantizada.'}, snapshot
