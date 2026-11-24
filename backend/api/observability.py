from rest_framework.views import exception_handler as default_exception_handler
from .monitoring_services import record_error


def exception_handler(exc, context):
    response = default_exception_handler(exc, context)
    if response is None or response.status_code >= 500:
        record_error('unexpected_error', 'api', http_status=response.status_code if response else 500)
    return response
