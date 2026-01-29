import urllib.request
import sys

def check_icy_metadata(url):
    print(f"Checking {url} for ICY metadata...")
    req = urllib.request.Request(url)
    req.add_header('Icy-Metadata', '1')
    try:
        with urllib.request.urlopen(req) as response:
            headers = response.info()
            print("--- Response Headers ---")
            print(headers)
            
            metaint = int(headers.get('icy-metaint', 0))
            if metaint == 0:
                print("\nResult: icy-metaint is 0. This stream does NOT support in-stream ICY metadata.")
                return False
            
            print(f"\nResult: icy-metaint is {metaint}. Reading stream to find metadata...")
            # Read first metaint bytes
            response.read(metaint)
            # Next byte is length of metadata / 16
            meta_len_byte = response.read(1)
            if not meta_len_byte:
                print("End of stream reached before metadata.")
                return False
                
            meta_len = ord(meta_len_byte) * 16
            if meta_len > 0:
                metadata = response.read(meta_len).decode('utf-8', errors='replace')
                print(f"Captured Metadata: {metadata}")
                return True
            else:
                print("Metadata length byte is 0 (no metadata currently).")
                return True
    except Exception as e:
        print(f"Error: {e}")
        return False

if __name__ == "__main__":
    url = "https://novazz.ice.infomaniak.ch/novazz-128.mp3"
    check_icy_metadata(url)
