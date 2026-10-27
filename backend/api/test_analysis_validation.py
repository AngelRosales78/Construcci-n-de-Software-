"""HU07_04 T48: contexto insuficiente, estructura y cifras falsas."""
import copy
import pytest
from .analysis_validation import context_from_pillars, validate_analysis
from .test_pillars_liquidity import case

URL = '/api/v1/companies/MSFT/analysis/?fiscal_year=2024'


def valid(context):
    available = [key for key, metric in context['metrics'].items() if metric['status'] == 'available']
    return {'summary': {'text': 'La liquidez debe interpretarse junto a las obligaciones.', 'metric_ids': available[:1]},
        'pillars': {key: {'text': 'La liquidez permite examinar las obligaciones corrientes.' if key == 'liquidity' else 'No disponible por falta de datos comparables.',
            'metric_ids': [m for m in available if context['metrics'][m]['pillar'] == key][:1]} for key in context['pillars']},
        'strengths': [], 'risks': []}


@pytest.fixture
def analysis_case(case, monkeypatch):
    monkeypatch.setattr('api.analysis_views.AnalysisThrottle.allow_request', lambda *a: True)
    pillars = case[0].get('/api/v1/companies/MSFT/pillars/?fiscal_year=2024').data
    context = context_from_pillars(pillars)
    monkeypatch.setattr('api.analysis_views.generate', lambda *args: valid(context))
    return case[0], context


def test_fake_number_is_rejected_before_publication(analysis_case, monkeypatch):
    client, context = analysis_case
    answer = valid(context); answer['summary']['text'] = 'La liquidez es de 999999 veces.'
    monkeypatch.setattr('api.analysis_views.generate', lambda *a: answer)
    response = client.get(URL)
    assert response.status_code == 503 and response.data['reason'] == 'unsupported_number'
    assert 'analysis' not in response.data


def test_missing_context_does_not_call_provider(case, monkeypatch):
    monkeypatch.setattr('api.analysis_views.AnalysisThrottle.allow_request', lambda *a: True)
    case[1].delete()
    monkeypatch.setattr('api.analysis_views.generate', lambda *a: (_ for _ in ()).throw(AssertionError('No llamar')))
    assert case[0].get(URL).data['reason'] == 'insufficient_context'


@pytest.mark.parametrize('bad', [None, {}, {'summary': 'texto'}])
def test_invalid_structure_is_rejected(analysis_case, bad):
    with pytest.raises(ValueError): validate_analysis(bad, analysis_case[1])


def test_only_the_authoritative_display_rounding_is_accepted(analysis_case):
    context = copy.deepcopy(analysis_case[1])
    context['metrics']['current_ratio'].update(value='1.005', display_value='1,01')
    answer = valid(context)
    answer['summary']['text'] = 'La liquidez corriente de 1,01 requiere una lectura conjunta del balance.'
    answer['summary']['metric_ids'] = ['current_ratio']
    validate_analysis(answer, context)


def test_labels_padded_with_spaces_are_not_published(analysis_case):
    answer = valid(analysis_case[1]); answer['summary']['text'] = 'Liquidez: ' + ' '*70
    with pytest.raises(ValueError): validate_analysis(answer, analysis_case[1])


def test_liquidity_is_not_published_as_profit_generation(analysis_case):
    answer = valid(analysis_case[1])
    answer['summary'] = {
        'text': 'La razón corriente demuestra una elevada generación de beneficios.',
        'metric_ids': ['current_ratio'],
    }
    with pytest.raises(ValueError, match='unsupported_claim'):
        validate_analysis(answer, analysis_case[1])


@pytest.mark.parametrize('repeated', ['iones ' * 10, 'obligaciones corrientes ' * 5])
def test_repeated_words_and_phrases_are_not_published(analysis_case, repeated):
    answer = valid(analysis_case[1])
    answer['summary']['text'] = 'La liquidez corriente indica ' + repeated + '.'
    with pytest.raises(ValueError, match='invalid_text'):
        validate_analysis(answer, analysis_case[1])


@pytest.mark.parametrize('text', [
    'La liquidez permite cubrir las obligaciones corrientes mientras que la',
    'La liquidez permite examinar las obligaciones corrientes con margen净.',
])
def test_truncated_or_non_latin_generation_is_not_published(analysis_case, text):
    answer = valid(analysis_case[1]); answer['summary']['text'] = text
    with pytest.raises(ValueError, match='invalid_text'):
        validate_analysis(answer, analysis_case[1])
