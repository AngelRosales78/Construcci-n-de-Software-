"""HU07_02 T56: cinco secciones y rechazo de referencias no respaldadas."""
import pytest
from .test_pillars_liquidity import case
from .test_analysis_validation import analysis_case, valid
from .analysis_validation import validate_analysis


def test_five_pillars_and_absent_data_are_explicit(analysis_case):
    context = analysis_case[1]
    assert len(validate_analysis(valid(context), context)['pillars']) == 5


@pytest.mark.parametrize('change', ['missing', 'unknown_metric', 'invented_missing'])
def test_unbacked_pillar_content_is_rejected(analysis_case, change):
    context = analysis_case[1]; answer = valid(context)
    if change == 'missing': answer['pillars'].pop('growth')
    if change == 'unknown_metric': answer['pillars']['growth']['metric_ids'] = ['fiction']
    if change == 'invented_missing': answer['pillars']['growth']['text'] = 'La empresa crece con gran fortaleza financiera.'
    with pytest.raises(ValueError): validate_analysis(answer, context)
