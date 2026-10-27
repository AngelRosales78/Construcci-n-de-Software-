"""HU07_03 T60: listas cortas, incompletas y afirmaciones falsas."""
import pytest
from .test_pillars_liquidity import case
from .test_analysis_validation import analysis_case, valid
from .analysis_validation import validate_analysis


def test_empty_lists_do_not_require_filler(analysis_case):
    context = analysis_case[1]
    assert validate_analysis(valid(context), context)['strengths'] == []


@pytest.mark.parametrize('change', ['fake', 'duplicate', 'incomplete', 'too_many'])
def test_invalid_arguments_are_rejected(analysis_case, change):
    context = analysis_case[1]; answer = valid(context)
    claim = {'text': 'La liquidez apoya el análisis de obligaciones.', 'metric_ids': ['current_ratio']}
    answer['strengths'] = [claim]
    if change == 'fake': answer['risks'] = [{'text': 'Hay deuda de 987654321 millones.', 'metric_ids': ['current_ratio']}]
    if change == 'duplicate': answer['risks'] = [claim.copy()]
    if change == 'incomplete': answer['risks'] = [{}]
    if change == 'too_many': answer['strengths'] = [claim]*6
    with pytest.raises(ValueError): validate_analysis(answer, context)
