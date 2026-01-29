import requests
import yaml
import json

# Wikidata API requires a User-Agent header
HEADERS = {
    "User-Agent": "OnAirRadioBot/0.1 (https://github.com/skadge/radio; contact@example.com) python-requests/2.x"
}

def search_wikidata(name, country_q):
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
        print(f"Error searching for {name}: {e}")
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
            print(f"Error getting entity {qid}: {e}")
            continue
        
        claims = entity_data.get("claims", {})
        
        # Verify country if possible
        item_country_q = None
        if "P17" in claims:
            # P17 might have multiple values, check first
            try:
                item_country_q = claims["P17"][0]["mainsnak"]["datavalue"]["value"]["id"]
            except (KeyError, IndexError):
                pass
        
        if country_q and item_country_q != country_q:
            continue
            
        # Get website (P856)
        if "P856" in claims:
            try:
                website = claims["P856"][0]["mainsnak"]["datavalue"]["value"]
                return {
                    "qid": qid,
                    "name": result.get("label"),
                    "website": website,
                    "description": result.get("description")
                }
            except (KeyError, IndexError):
                pass
            
    return None

test_stations = [
    ("BBC Radio 4", "Q145"),
    ("Deutschlandfunk", "Q183"),
    ("France Musique", "Q142"),
    ("WNYC", "Q30")
]

for name, cq in test_stations:
    print(f"Searching for {name}...")
    res = search_wikidata(name, cq)
    if res:
        print(f"Found: {res['name']} ({res['qid']}) - {res['website']}")
    else:
        print(f"Not found for {name}")
