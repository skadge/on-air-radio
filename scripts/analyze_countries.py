import json
from pathlib import Path
from collections import Counter

def analyze_countries():
    root = Path("/home/skadge/src/radio")
    json_path = root / "allStations.json"
    
    with open(json_path, 'r') as f:
        data = json.load(f)
    
    sorted_stations = sorted(data, key=lambda x: x.get('clickcount', 0), reverse=True)
    top_10_percent = sorted_stations[:len(data)//10]
    
    quality_stations = [
        s for s in top_10_percent 
        if s.get('name') and s.get('url') and s.get('favicon') and s.get('tags')
    ]
    
    countries = [s.get('countrycode', '??') for s in quality_stations]
    counts = Counter(countries)
    
    print("Top countries in the 10% pool:")
    for code, count in counts.most_common(20):
        print(f"{code}: {count}")

if __name__ == "__main__":
    analyze_countries()
