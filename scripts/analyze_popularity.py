import json
import yaml
from pathlib import Path

def analyze_stations():
    root = Path("/home/skadge/src/radio")
    json_path = root / "allStations.json"
    
    with open(json_path, 'r') as f:
        data = json.load(f)
    
    total = len(data)
    print(f"Total stations in JSON: {total}")
    
    # Sort by clickcount
    sorted_stations = sorted(data, key=lambda x: x.get('clickcount', 0), reverse=True)
    
    # 10% threshold
    top_10_percent_count = total // 10
    top_stations = sorted_stations[:top_10_percent_count]
    
    if top_stations:
        min_clicks = top_stations[-1].get('clickcount', 0)
        max_clicks = top_stations[0].get('clickcount', 0)
        print(f"Top 10% ({top_10_percent_count} stations) clickcount range: {min_clicks} to {max_clicks}")
    
    # Filter for quality: must have favicon, name, url, and some tags
    quality_stations = [
        s for s in top_stations 
        if s.get('name') and s.get('url') and s.get('favicon') and s.get('tags')
    ]
    
    print(f"Quality stations in top 10%: {len(quality_stations)}")
    
    # Let's see some samples
    for i in range(min(5, len(quality_stations))):
        s = quality_stations[i]
        print(f"- {s['name']} ({s['clickcount']} clicks): {s['tags']}")

if __name__ == "__main__":
    analyze_stations()
