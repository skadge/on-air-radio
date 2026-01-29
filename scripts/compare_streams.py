import requests
import yaml
import time
import os

# Wikidata API requires a User-Agent header
HEADERS = {
    "User-Agent": "OnAirRadioBot/0.1 (https://github.com/skadge/radio; contact@example.com) python-requests/2.x"
}

def get_wikidata_stream_url(qid):
    entity_url = f"https://www.wikidata.org/wiki/Special:EntityData/{qid}.json"
    try:
        response = requests.get(entity_url, headers=HEADERS)
        response.raise_for_status()
        data = response.json().get("entities", {}).get(qid, {})
    except Exception as e:
        print(f"  Error fetching Wikidata for {qid}: {e}")
        return None
    
    claims = data.get("claims", {})
    
    # Property P963: streaming media URL
    if "P963" in claims:
        try:
            # There could be multiple, take the first one
            return claims["P963"][0]["mainsnak"]["datavalue"]["value"]
        except (KeyError, IndexError):
            pass
            
    return None

def main():
    if not os.path.exists('stations.yaml'):
        print("stations.yaml not found.")
        return

    with open('stations.yaml', 'r', encoding='utf-8') as f:
        data = yaml.safe_load(f)
    
    stations = data.get('stations', [])
    report = []
    
    print(f"Comparing stream URLs for {len(stations)} stations...")
    
    for s in stations:
        name = s['name']
        wiki_url = s.get('wikidata_url')
        current_stream = s.get('stream_url')
        
        if not wiki_url:
            continue
            
        qid = wiki_url.split('/')[-1]
        print(f"Checking {name} ({qid})...")
        
        wiki_stream = get_wikidata_stream_url(qid)
        
        if wiki_stream:
            # Normalize for comparison? (Remove trailing slashes, etc.)
            norm_current = current_stream.strip().lower().rstrip('/')
            norm_wiki = wiki_stream.strip().lower().rstrip('/')
            
            if norm_current != norm_wiki:
                print(f"  DIFFERENCE FOUND!")
                report.append({
                    "name": name,
                    "id": s.get('id'),
                    "current": current_stream,
                    "wikidata": wiki_stream,
                    "wikidata_item": wiki_url
                })
        
        # Rate limit
        time.sleep(0.3)
        
    if report:
        print(f"\nFound {len(report)} differences. Generating report...")
        with open('stream_comparison_report.md', 'w', encoding='utf-8') as f:
            f.write("# Stream URL Comparison Report\n\n")
            f.write("Found differences between `stations.yaml` and Wikidata (P963).\n\n")
            f.write("| Station | ID | Current URL | Wikidata URL | Wikidata Item |\n")
            f.write("| :--- | :--- | :--- | :--- | :--- |\n")
            for item in report:
                f.write(f"| {item['name']} | `{item['id']}` | `{item['current']}` | `{item['wikidata']}` | [Item]({item['wikidata_item']}) |\n")
        print("Report saved to stream_comparison_report.md")
    else:
        print("\nNo differences found or no Wikidata stream URLs found.")

if __name__ == "__main__":
    main()
