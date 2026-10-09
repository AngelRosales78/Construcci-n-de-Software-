import os

import pytest

# No import Django at module level


@pytest.fixture(autouse=True)
def enable_db_access_for_all_tests(django_db_setup, django_db_blocker):
    """Enable database access for all tests by default."""
    with django_db_blocker.unblock():
        pass
