import socket
import ssl
from urllib.parse import urlparse
import sys


def read_icy_metadata(stream_url, timeout=15):
    url = urlparse(stream_url)

    host = url.hostname
    port = url.port or (443 if url.scheme == "https" else 80)
    path = url.path or "/"

    # Build HTTP request
    request = (
        f"GET {path} HTTP/1.1\r\n"
        f"Host: {host}\r\n"
        f"User-Agent: Python-ICY-Client/1.0\r\n"
        f"Icy-MetaData: 1\r\n"
        f"Connection: close\r\n\r\n"
    )

    # Create socket
    sock = socket.create_connection((host, port), timeout=timeout)

    # Wrap SSL if needed
    if url.scheme == "https":
        context = ssl.create_default_context()
        sock = context.wrap_socket(sock, server_hostname=host)

    sock.sendall(request.encode())

    # Read response headers
    response = b""
    while b"\r\n\r\n" not in response:
        response += sock.recv(1024)

    header_data, stream_data = response.split(b"\r\n\r\n", 1)

    headers = header_data.decode(errors="ignore").split("\r\n")

    meta_int = None

    print("=== Response Headers ===")
    for h in headers:
        print(h)
        if h.lower().startswith("icy-metaint"):
            meta_int = int(h.split(":")[1].strip())

    if not meta_int:
        print("\nNo ICY metadata supported by this stream.")
        sock.close()
        return

    print(f"\nicy-metaint = {meta_int}")
    print("\nWaiting for metadata...\n")

    data = stream_data

    while True:
        # Make sure we have enough audio data
        while len(data) < meta_int + 1:
            chunk = sock.recv(4096)
            if not chunk:
                return
            data += chunk

        # Skip audio data
        data = data[meta_int:]

        # Metadata length byte
        meta_len = data[0] * 16
        data = data[1:]

        # Make sure we have full metadata block
        while len(data) < meta_len:
            data += sock.recv(4096)

        meta_block = data[:meta_len]
        data = data[meta_len:]

        if meta_len > 0:
            meta_str = meta_block.rstrip(b"\x00").decode("utf-8", errors="ignore")

            if meta_str:
                print("=== ICY Metadata ===")
                print(meta_str)
                print()

                # Optional: parse StreamTitle
                parts = meta_str.split(";")
                for p in parts:
                    if p.startswith("StreamTitle="):
                        title = p.split("=", 1)[1].strip("'")
                        print("Now Playing:", title)
                        print()

        # Keep listening


if __name__ == "__main__":
    read_icy_metadata(sys.argv[1])

