"""
Pytest configuration for the S&P 500 platform.
"""
import os

import pytest
from django.test import TestCase


@pytest.fixture(autouse=True)
def enable_db_access_for_all_tests(db):
    """Enable database access for all tests by default."""
    pass


@pytest.fixture
def settings():
    """Allow overriding Django settings in tests."""
    from django.conf import settings
    return settings
