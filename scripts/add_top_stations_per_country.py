import json
import yaml
from pathlib import Path
import re

# Canonical tags from FilterData.kt
CANONICAL_TAGS = ["pop", "rock", "hits", "jazz", "classical", "news", "talk", "ambient", "world", "oldies"]

# Mapping for common variations to canonical tags
TAG_MAPPING = {
    "world music": "world",
    "worldmusic": "world",
    "information": "news",
    "news/talk": "talk",
    "news talk": "talk",
    "indie": "rock",
    "alternative": "rock",
    "dance": "hits",
    "electronic": "ambient",
    "chillout": "ambient",
    "chill": "ambient",
}

def clean_tag(tag):
    tag = tag.lower().strip()
    if tag in CANONICAL_TAGS:
        return tag
    return TAG_MAPPING.get(tag, None)

def get_primary_tag(tags_list):
    for tag in tags_list:
        if tag in CANONICAL_TAGS:
            return tag
    return None

def slugify(text):
    text = text.lower()
    text = re.sub(r'[^a-z0-9]+', '_', text)
    return text.strip('_')

def run():
    root = Path("/home/skadge/src/radio")
    json_path = root / "allStations.json"
    yaml_path = root / "stations.yaml"
    
    with open(json_path, 'r') as f:
        data = json.load(f)
    
    with open(yaml_path, 'r') as f:
        current_yaml = yaml.safe_load(f)
    
    existing_ids = {s['id'] for s in current_yaml['stations']}
    existing_urls = {s['stream_url'] for s in current_yaml['stations']}
    
    country_map = {
        'DE': 'germany',
        'US': 'usa',
        'RU': 'russia',
        'FR': 'france',
        'GB': 'uk',
        'CH': 'switzerland',
        'IT': 'italy',
        'IN': 'india',
        'PL': 'poland',
        'CN': 'china',
        'ES': 'spain',
        'CZ': 'czech_republic',
        'NL': 'netherlands',
        'CA': 'canada',
        'GR': 'greece',
        'UA': 'ukraine',
        'BR': 'brazil',
        'AT': 'austria',
        'MX': 'mexico'
    }
    
    # Sort all by clickcount
    sorted_data = sorted(data, key=lambda x: x.get('clickcount', 0), reverse=True)
    
    new_stations = []
    stations_by_country = {code: 0 for code in country_map.keys()}
    
    for s in sorted_data:
        cc = s.get('countrycode')
        if not cc or cc not in country_map:
            continue
            
        if stations_by_country[cc] >= 10:
            continue
            
        name = s.get('name', '').strip()
        url = s.get('url_resolved') or s.get('url')
        favicon = s.get('favicon', '').strip()
        tags_raw = s.get('tags', '')
        
        if not name or not url or not favicon or not tags_raw:
            continue
            
        # Deduplicate
        if url in existing_urls:
            continue
            
        # Process tags
        raw_tag_list = tags_raw.split(',')
        cleaned_tags = []
        for t in raw_tag_list:
            ct = clean_tag(t)
            if ct and ct not in cleaned_tags:
                cleaned_tags.append(ct)
        
        # We need at least one recognizable tag to map properly
        primary = get_primary_tag(cleaned_tags)
        if not primary:
            # Try to find any canonical tag in raw tags even if not separated by comma
            for ct in CANONICAL_TAGS:
                if ct in tags_raw.lower():
                    primary = ct
                    if ct not in cleaned_tags:
                        cleaned_tags.append(ct)
                    break
        
        if not primary:
            continue
            
        station_id = slugify(name)
        if station_id in existing_ids:
            station_id = f"{station_id}_{cc.lower()}"
        
        new_station = {
            'id': station_id,
            'name': name,
            'stream_url': url,
            'logo_url': favicon,
            'description': f"{name} from {s.get('country', country_map[cc].capitalize())}",
            'country': country_map[cc],
            'tags': ",".join(cleaned_tags),
            'popularity': s.get('clickcount', 0),
            'primary_tag': primary
        }
        
        new_stations.append(new_station)
        stations_by_country[cc] += 1
        existing_ids.add(station_id)
        existing_urls.add(url)

    print(f"Adding {len(new_stations)} new stations.")
    current_yaml['stations'].extend(new_stations)
    
    with open(yaml_path, 'w') as f:
        yaml.dump(current_yaml, f, sort_keys=False, allow_unicode=True)

if __name__ == "__main__":
    run()
