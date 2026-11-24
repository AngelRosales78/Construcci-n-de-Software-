"""SQLite local: cada transacción toma el bloqueo de escritura al empezar, como select_for_update en PostgreSQL.

Sin esto, dos lecturas simultáneas que luego escriben fallan con "database is locked"."""
from django.db.backends.sqlite3.base import DatabaseWrapper as SQLiteWrapper


class DatabaseWrapper(SQLiteWrapper):
    def _start_transaction_under_autocommit(self):
        self.cursor().execute('BEGIN IMMEDIATE')
