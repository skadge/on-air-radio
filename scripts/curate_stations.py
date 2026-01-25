import requests
import yaml
import time
import os
import shutil

# Wikidata API requires a User-Agent header
HEADERS = {
    "User-Agent": "OnAirRadioBot/0.1 (https://github.com/skadge/radio; contact@example.com) python-requests/2.x"
}

COUNTRY_MAP = {
    "uk": "Q145",
    "germany": "Q183",
    "france": "Q142",
    "usa": "Q30",
    "italy": "Q38",
    "spain": "Q29",
    "netherlands": "Q55",
    "russia": "Q159",
    "india": "Q668",
    "poland": "Q36",
    "china": "Q148",
    "switzerland": "Q39",
    "czech_republic": "Q213",
    "canada": "Q16",
    "greece": "Q41",
    "ukraine": "Q212",
    "brazil": "Q155",
    "austria": "Q40",
    "mexico": "Q96"
}

def search_wikidata(name, country_code):
    country_q = COUNTRY_MAP.get(country_code)
    # If country is not in our map, we search without country restriction but it's riskier
    # For now, let's stick to the mapped countries for quality
    if not country_q:
        return None

    url = "https://www.wikidata.org/w/api.php"
    params = {
        "action": "wbsearchentities",
        "format": "json",
        "language": "en",
        "search": name,
        "type": "item"
    }
    
    try:
        response = requests.get(url, params=params, headers=HEADERS)
        response.raise_for_status()
        data = response.json()
    except Exception as e:
        print(f"  Error searching for {name}: {e}")
        return None
    
    results = data.get("search", [])
    if not results:
        return None
    
    # Check the first few results
    for result in results[:3]:
        qid = result["id"]
        entity_url = f"https://www.wikidata.org/wiki/Special:EntityData/{qid}.json"
        try:
            res = requests.get(entity_url, headers=HEADERS)
            res.raise_for_status()
            entity_data = res.json().get("entities", {}).get(qid, {})
        except Exception as e:
            continue
        
        claims = entity_data.get("claims", {})
        
        # Verify country
        item_country_q = None
        if "P17" in claims:
            try:
                item_country_q = claims["P17"][0]["mainsnak"]["datavalue"]["value"]["id"]
            except (KeyError, IndexError):
                pass
        
        # If country doesn't match, skip
        if country_q and item_country_q and item_country_q != country_q:
            continue
            
        # Get website (P856)
        website = None
        if "P856" in claims:
            try:
                website = claims["P856"][0]["mainsnak"]["datavalue"]["value"]
            except (KeyError, IndexError):
                pass
        
        # Get SVG logo (P154)
        logo_svg = None
        if "P154" in claims:
            try:
                logo_file = claims["P154"][0]["mainsnak"]["datavalue"]["value"]
                if logo_file.lower().endswith('.svg'):
                    logo_svg = f"https://commons.wikimedia.org/wiki/Special:FilePath/{logo_file.replace(' ', '_')}"
            except (KeyError, IndexError):
                pass

        if website:
            return {
                "qid": qid,
                "website": website,
                "logo_svg": logo_svg,
                "wikidata_url": f"https://www.wikidata.org/wiki/{qid}"
            }
            
    return None

import re

def normalize_search_name(name):
    # Remove things like [MP3 128k], (96kb/s), etc.
    name = re.sub(r'\s*[\[\(].*?[\]\)]', '', name)
    # Remove "Radio" from the end if it's redundant for search? No, keep it.
    # Remove extra whitespace
    name = ' '.join(name.split())
    return name

def main():
    if not os.path.exists('stations.yaml'):
        print("stations.yaml not found.")
        return

    # Backup
    shutil.copy('stations.yaml', 'stations.yaml.bak')
    print("Backup created as stations.yaml.bak")

    with open('stations.yaml', 'r', encoding='utf-8') as f:
        data = yaml.safe_load(f)
    
    stations = data.get('stations', [])
    updated_stations = []
    removed_count = 0
    updated_count = 0
    
    total = len(stations)
    for i, s in enumerate(stations):
        country = s.get('country')
        name = s['name']
        print(f"[{i+1}/{total}] Processing {name} ({country})...")
        
        # If it already has wikidata_url AND website_url, we keep it as is (or update logo)
        if s.get('wikidata_url') and s.get('website_url'):
            updated_stations.append(s)
            continue

        search_name = normalize_search_name(name)
        res = search_wikidata(search_name, country)
        
        if res:
            s['website_url'] = res['website']
            s['wikidata_url'] = res['wikidata_url']
            if res['logo_svg'] and not s.get('logo_svg_url'):
                s['logo_svg_url'] = res['logo_svg']
            print(f"  Found Wikidata: {res['wikidata_url']} and Website: {res['website']}")
            updated_stations.append(s)
            updated_count += 1
        else:
            print(f"  No Wikidata entry with website found. REMOVING.")
            removed_count += 1
        
        # Rate limiting to be polite
        time.sleep(0.3)
            
    data['stations'] = updated_stations
    
    print(f"\nSummary:")
    print(f"  Total stations processed: {total}")
    print(f"  Stations updated/kept: {len(updated_stations)}")
    print(f"  Stations removed: {removed_count}")
    print(f"  New Wikidata matches: {updated_count}")

    if updated_count > 0 or removed_count > 0:
        print(f"Saving changes to stations.yaml...")
        with open('stations.yaml', 'w', encoding='utf-8') as f:
            yaml.dump(data, f, sort_keys=False, allow_unicode=True)
    else:
        print("No changes to save.")

if __name__ == "__main__":
    main()
