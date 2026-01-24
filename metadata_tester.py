import urllib.request
import sys

def get_stream_title(url):
    request = urllib.request.Request(url, headers={'Icy-MetaData': '1'})
    try:
        response = urllib.request.urlopen(request)
    except Exception as e:
        print(f"Error opening stream: {e}")
        return

    metaint = int(response.headers['icy-metaint'])
    print(f"Metaint: {metaint}")

    # Read some audio data to get to the metadata
    # We might need to read a few chunks to find a title if it updates infrequently,
    # but the initial connection usually sends the current title immediately after the first chunk.
    
    # Read up to the first metadata block
    response.read(metaint)
    
    # The next byte is the length of the metadata block * 16
    metadata_len_byte = response.read(1)
    if not metadata_len_byte:
        print("Stream ended before metadata")
        return
        
    metadata_len = ord(metadata_len_byte) * 16
    
    if metadata_len > 0:
        metadata = response.read(metadata_len).decode('utf-8', errors='ignore')
        print(f"Raw Metadata: {metadata}")
        
        # Extract StreamTitle
        # Metadata format: StreamTitle='title';StreamUrl='url';
        parts = metadata.split(';')
        for part in parts:
            if 'StreamTitle=' in part:
                title = part.split("StreamTitle='")[1]
                # Handle potential closing quote if it exists in the split
                if title.endswith("'"):
                    title = title[:-1]
                print(f"--> Extracted Title: {title}")
    else:
        print("No metadata in this block (length 0)")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 metadata_tester.py <stream_url>")
        sys.exit(1)
    
    url = sys.argv[1]
    print(f"Testing URL: {url}")
    get_stream_title(url)
