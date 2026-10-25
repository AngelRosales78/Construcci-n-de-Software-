"""Local SMTP receiver for integration tests; never sends to external recipients."""
from contextlib import contextmanager
from pathlib import Path
from socketserver import StreamRequestHandler, ThreadingTCPServer
from threading import Thread
from uuid import uuid4


class LocalSMTPServer(ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True

    def __init__(self, address, directory=None):
        self.messages = []
        self.directory = Path(directory) if directory else None
        if self.directory:
            self.directory.mkdir(parents=True, exist_ok=True)
        super().__init__(address, LocalSMTPHandler)


class LocalSMTPHandler(StreamRequestHandler):
    def handle(self):
        self.wfile.write(b'220 localhost Test SMTP\r\n')
        while line := self.rfile.readline():
            verb = line.split(b' ', 1)[0].strip().upper()
            if verb in (b'EHLO', b'HELO'):
                self.wfile.write(b'250 localhost\r\n')
            elif verb == b'DATA':
                self.wfile.write(b'354 End with dot\r\n')
                message = bytearray()
                while data := self.rfile.readline():
                    if data == b'.\r\n':
                        break
                    message.extend(data[1:] if data.startswith(b'..') else data)
                self.server.messages.append(bytes(message))
                if self.server.directory:
                    (self.server.directory / f'{uuid4().hex}.eml').write_bytes(message)
                self.wfile.write(b'250 Message accepted\r\n')
            elif verb == b'QUIT':
                self.wfile.write(b'221 Bye\r\n')
                return
            elif verb in (b'MAIL', b'RCPT', b'RSET', b'NOOP'):
                self.wfile.write(b'250 OK\r\n')
            else:
                self.wfile.write(b'502 Not implemented\r\n')


@contextmanager
def smtp_receiver():
    server = LocalSMTPServer(('127.0.0.1', 0))
    thread = Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        yield server
    finally:
        server.shutdown()
        server.server_close()
        thread.join(timeout=2)
