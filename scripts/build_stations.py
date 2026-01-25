#!/usr/bin/env python3
"""
Radio Station Build Script

This script processes stations.yaml and:
1. Fetches missing SVG logos from URLs
2. Converts SVG files to Android Vector Drawables (XML)
3. Validates stream URLs (optional)
4. Generates RadioRepository.kt

Usage:
    python scripts/build_stations.py [options]

Options:
    --validate-streams    Check if stream URLs are reachable
    --force-logos        Re-download all logos even if they exist
    --dry-run            Don't write any files, just validate
    --verbose            Show detailed output
"""

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path
from typing import Optional
from xml.etree import ElementTree as ET

import requests
import yaml


# Paths relative to project root
PROJECT_ROOT = Path(__file__).parent.parent
STATIONS_YAML = PROJECT_ROOT / "stations.yaml"
SVG_DIR = PROJECT_ROOT / "app/src/main/res/drawable/svgs"
DRAWABLE_DIR = PROJECT_ROOT / "app/src/main/res/drawable"
REPOSITORY_PATH = PROJECT_ROOT / "app/src/main/java/org/guakamole/onair/data/RadioRepository.kt"


def log(msg: str, verbose: bool = False, force: bool = False):
    """Print message if verbose mode or forced."""
    if verbose or force:
        print(msg)


def load_stations() -> dict:
    """Load and validate stations.yaml."""
    with open(STATIONS_YAML, 'r', encoding='utf-8') as f:
        data = yaml.safe_load(f)
    
    if 'stations' not in data:
        raise ValueError("stations.yaml must contain a 'stations' key")
    if 'constants' not in data:
        raise ValueError("stations.yaml must contain a 'constants' key")
    
    return data


def fetch_svg(station: dict, force: bool = False, verbose: bool = False, dry_run: bool = False) -> Optional[Path]:
    """
    Fetch SVG logo for a station if it doesn't exist locally.
    Returns the path to the SVG file, or None if not available.
    """
    station_id = station['id']
    svg_path = SVG_DIR / f"logo_{station_id}.svg"
    
    # Check if we already have the SVG
    if svg_path.exists() and not force:
        log(f"  ✓ SVG exists: {svg_path.name}", verbose)
        return svg_path
    
    # Check if logo_svg_url is provided
    svg_url = station.get('logo_svg_url')
    if not svg_url:
        log(f"  ⚠ No logo_svg_url for {station_id}", verbose, force=True)
        return None
    
    if dry_run:
        log(f"  [DRY-RUN] Would fetch: {svg_url}", verbose, force=True)
        return svg_path if svg_path.exists() else None
    
    # Fetch the SVG
    try:
        log(f"  ↓ Fetching SVG for {station_id}...", verbose, force=True)
        response = requests.get(svg_url, timeout=30, headers={
            'User-Agent': 'Mozilla/5.0 (compatible; OnAirRadio/1.0)'
        })
        response.raise_for_status()
        
        # Validate it looks like SVG
        content = response.text
        if '<svg' not in content.lower():
            log(f"  ✗ Downloaded content doesn't look like SVG for {station_id}", force=True)
            return None
        
        # Save the SVG
        SVG_DIR.mkdir(parents=True, exist_ok=True)
        with open(svg_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        log(f"  ✓ Saved: {svg_path.name}", verbose, force=True)
        return svg_path
        
    except requests.RequestException as e:
        log(f"  ✗ Failed to fetch SVG for {station_id}: {e}", force=True)
        return None


def convert_svg_to_vector(svg_path: Path, force: bool = False, verbose: bool = False, dry_run: bool = False) -> Optional[Path]:
    """
    Convert SVG to Android Vector Drawable XML.
    Uses a simple Python-based conversion for common SVG patterns.
    Re-converts if the SVG file is newer than the existing XML.
    """
    xml_path = DRAWABLE_DIR / svg_path.name.replace('.svg', '.xml')
    
    # Check if XML already exists and is newer than SVG
    if xml_path.exists() and not force:
        if xml_path.stat().st_mtime >= svg_path.stat().st_mtime:
            log(f"  ✓ Vector XML up-to-date: {xml_path.name}", verbose)
            return xml_path
        else:
            log(f"  ↻ SVG is newer, re-converting: {svg_path.name}", verbose, force=True)
    
    if dry_run:
        log(f"  [DRY-RUN] Would convert: {svg_path.name} -> {xml_path.name}", verbose, force=True)
        return xml_path if xml_path.exists() else None
    
    log(f"  ⚙ Converting {svg_path.name} to Android Vector...", verbose, force=True)
    
    try:
        # Try using vd-tool if available (from Android SDK)
        result = convert_svg_with_vd_tool(svg_path, xml_path)
        if result:
            log(f"  ✓ Converted with vd-tool: {xml_path.name}", verbose, force=True)
            return xml_path
    except Exception as e:
        log(f"  ⚠ vd-tool not available, using fallback: {e}", verbose)
    
    # Fallback: simple SVG to VectorDrawable conversion
    try:
        result = convert_svg_simple(svg_path, xml_path)
        if result:
            log(f"  ✓ Converted: {xml_path.name}", verbose, force=True)
            return xml_path
    except Exception as e:
        log(f"  ✗ Failed to convert {svg_path.name}: {e}", force=True)
        return None
    
    return None


def convert_svg_with_vd_tool(svg_path: Path, xml_path: Path) -> bool:
    """Try to use Android SDK's vd-tool for conversion."""
    # Look for vd-tool in common locations
    sdk_path = os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME')
    if not sdk_path:
        return False
    
    # Find vd-tool
    vd_tool = None
    sdk_cmdline = Path(sdk_path) / 'cmdline-tools'
    if sdk_cmdline.exists():
        for version_dir in sdk_cmdline.iterdir():
            potential = version_dir / 'bin' / 'vd-tool'
            if potential.exists():
                vd_tool = potential
                break
    
    if not vd_tool:
        return False
    
    # Run vd-tool
    result = subprocess.run(
        [str(vd_tool), '-c', '-in', str(svg_path), '-out', str(xml_path.parent)],
        capture_output=True,
        text=True
    )
    
    return result.returncode == 0 and xml_path.exists()


def convert_svg_simple(svg_path: Path, xml_path: Path) -> bool:
    """
    Simple SVG to VectorDrawable conversion.
    Handles basic SVG files with paths, rectangles, circles.
    """
    with open(svg_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Parse SVG
    # Remove namespaces for easier parsing
    content_clean = re.sub(r'\sxmlns[^=]*="[^"]*"', '', content)
    content_clean = re.sub(r'<\?xml[^>]*\?>', '', content_clean)
    content_clean = re.sub(r'<!DOCTYPE[^>]*>', '', content_clean)
    content_clean = re.sub(r'<!--.*?-->', '', content_clean, flags=re.DOTALL)
    
    try:
        root = ET.fromstring(content_clean)
    except ET.ParseError as e:
        raise ValueError(f"Failed to parse SVG: {e}")
    
    # Get viewBox or width/height
    viewbox = root.get('viewBox')
    if viewbox:
        parts = viewbox.split()
        vp_width = float(parts[2])
        vp_height = float(parts[3])
    else:
        vp_width = float(root.get('width', '24').replace('px', '').replace('pt', ''))
        vp_height = float(root.get('height', '24').replace('px', '').replace('pt', ''))
    
    # Collect paths
    paths = []
    collect_paths(root, paths, '')
    
    if not paths:
        raise ValueError("No paths found in SVG")
    
    # Generate VectorDrawable XML
    vector_xml = f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{vp_width}dp"
    android:height="{vp_height}dp"
    android:viewportWidth="{vp_width}"
    android:viewportHeight="{vp_height}">
'''
    
    for path_data, fill_color, stroke_color, fill_rule in paths:
        vector_xml += '  <path\n'
        vector_xml += f'      android:pathData="{path_data}"\n'
        if fill_color and fill_color != 'none':
            vector_xml += f'      android:fillColor="{normalize_color(fill_color)}"\n'
        if stroke_color and stroke_color != 'none':
            vector_xml += f'      android:strokeColor="{normalize_color(stroke_color)}"\n'
        if fill_rule == 'evenodd':
            vector_xml += '      android:fillType="evenOdd"\n'
        vector_xml = vector_xml.rstrip('\n') + '/>\n'
    
    vector_xml += '</vector>\n'
    
    # Write output
    with open(xml_path, 'w', encoding='utf-8') as f:
        f.write(vector_xml)
    
    return True


def collect_paths(element, paths: list, parent_transform: str):
    """Recursively collect path data from SVG elements."""
    tag = element.tag.split('}')[-1] if '}' in element.tag else element.tag
    
    # Get style attributes
    style = element.get('style', '')
    fill = element.get('fill') or extract_style(style, 'fill') or '#000000'
    stroke = element.get('stroke') or extract_style(style, 'stroke')
    fill_rule = element.get('fill-rule') or extract_style(style, 'fill-rule')
    
    if tag == 'path':
        d = element.get('d')
        if d:
            # Clean up path data
            d = normalize_path_data(d)
            paths.append((d, fill, stroke, fill_rule))
    
    elif tag == 'rect':
        x = float(element.get('x', 0))
        y = float(element.get('y', 0))
        w = float(element.get('width', 0))
        h = float(element.get('height', 0))
        rx = float(element.get('rx', 0))
        ry = float(element.get('ry', rx))
        
        if rx > 0 or ry > 0:
            # Rounded rectangle
            d = f"M{x+rx},{y} L{x+w-rx},{y} Q{x+w},{y} {x+w},{y+ry} L{x+w},{y+h-ry} Q{x+w},{y+h} {x+w-rx},{y+h} L{x+rx},{y+h} Q{x},{y+h} {x},{y+h-ry} L{x},{y+ry} Q{x},{y} {x+rx},{y} Z"
        else:
            d = f"M{x},{y} L{x+w},{y} L{x+w},{y+h} L{x},{y+h} Z"
        paths.append((d, fill, stroke, fill_rule))
    
    elif tag == 'circle':
        cx = float(element.get('cx', 0))
        cy = float(element.get('cy', 0))
        r = float(element.get('r', 0))
        # Approximate circle with bezier curves
        k = 0.5522847498  # Magic number for circle approximation
        d = f"M{cx},{cy-r} C{cx+r*k},{cy-r} {cx+r},{cy-r*k} {cx+r},{cy} C{cx+r},{cy+r*k} {cx+r*k},{cy+r} {cx},{cy+r} C{cx-r*k},{cy+r} {cx-r},{cy+r*k} {cx-r},{cy} C{cx-r},{cy-r*k} {cx-r*k},{cy-r} {cx},{cy-r} Z"
        paths.append((d, fill, stroke, fill_rule))
    
    elif tag == 'ellipse':
        cx = float(element.get('cx', 0))
        cy = float(element.get('cy', 0))
        rx = float(element.get('rx', 0))
        ry = float(element.get('ry', 0))
        k = 0.5522847498
        d = f"M{cx},{cy-ry} C{cx+rx*k},{cy-ry} {cx+rx},{cy-ry*k} {cx+rx},{cy} C{cx+rx},{cy+ry*k} {cx+rx*k},{cy+ry} {cx},{cy+ry} C{cx-rx*k},{cy+ry} {cx-rx},{cy+ry*k} {cx-rx},{cy} C{cx-rx},{cy-ry*k} {cx-rx*k},{cy-ry} {cx},{cy-ry} Z"
        paths.append((d, fill, stroke, fill_rule))
    
    elif tag == 'polygon':
        points = element.get('points', '').strip()
        if points:
            # Parse points
            coords = re.findall(r'[-+]?\d*\.?\d+', points)
            if len(coords) >= 4:
                d = f"M{coords[0]},{coords[1]}"
                for i in range(2, len(coords), 2):
                    d += f" L{coords[i]},{coords[i+1]}"
                d += " Z"
                paths.append((d, fill, stroke, fill_rule))
    
    elif tag == 'polyline':
        points = element.get('points', '').strip()
        if points:
            coords = re.findall(r'[-+]?\d*\.?\d+', points)
            if len(coords) >= 4:
                d = f"M{coords[0]},{coords[1]}"
                for i in range(2, len(coords), 2):
                    d += f" L{coords[i]},{coords[i+1]}"
                paths.append((d, fill, stroke, fill_rule))
    
    # Recurse into children
    for child in element:
        collect_paths(child, paths, parent_transform)


def extract_style(style: str, prop: str) -> Optional[str]:
    """Extract a property value from CSS style string."""
    match = re.search(rf'{prop}\s*:\s*([^;]+)', style)
    return match.group(1).strip() if match else None


def normalize_color(color: str) -> str:
    """Normalize color to Android-compatible format."""
    if not color:
        return '#000000'
    
    color = color.strip()
    
    # Named colors
    named_colors = {
        'black': '#000000', 'white': '#FFFFFF', 'red': '#FF0000',
        'green': '#00FF00', 'blue': '#0000FF', 'yellow': '#FFFF00',
        'cyan': '#00FFFF', 'magenta': '#FF00FF', 'none': '#00000000',
        'transparent': '#00000000'
    }
    if color.lower() in named_colors:
        return named_colors[color.lower()]
    
    # Already hex
    if color.startswith('#'):
        if len(color) == 4:  # #RGB -> #RRGGBB
            return f"#{color[1]*2}{color[2]*2}{color[3]*2}"
        return color.upper()
    
    # RGB/RGBA
    rgb_match = re.match(r'rgba?\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)', color)
    if rgb_match:
        r, g, b = int(rgb_match.group(1)), int(rgb_match.group(2)), int(rgb_match.group(3))
        return f"#{r:02X}{g:02X}{b:02X}"
    
    return color


def normalize_path_data(d: str) -> str:
    """Clean up path data for Android compatibility."""
    # Remove newlines and excess whitespace
    d = ' '.join(d.split())
    # Ensure proper spacing around commands
    d = re.sub(r'([a-zA-Z])', r' \1 ', d)
    d = ' '.join(d.split())
    return d.strip()


def validate_stream(station: dict, verbose: bool = False) -> tuple[bool, str]:
    """Validate that a stream URL is reachable and returns audio content."""
    stream_url = station['stream_url']
    station_id = station['id']
    
    try:
        response = requests.head(stream_url, timeout=10, allow_redirects=True, headers={
            'User-Agent': 'Mozilla/5.0 (compatible; OnAirRadio/1.0)'
        })
        
        if response.status_code == 405:  # Method not allowed, try GET
            response = requests.get(stream_url, timeout=10, stream=True, headers={
                'User-Agent': 'Mozilla/5.0 (compatible; OnAirRadio/1.0)'
            })
            response.close()
        
        if response.status_code >= 400:
            return False, f"HTTP {response.status_code}"
        
        content_type = response.headers.get('Content-Type', '').lower()
        valid_types = ['audio/', 'application/ogg', 'application/vnd.apple.mpegurl', 
                       'application/x-mpegurl', 'video/']
        
        if any(t in content_type for t in valid_types):
            return True, content_type
        elif content_type:
            return True, f"Unknown type: {content_type}"
        else:
            return True, "No content-type"
            
    except requests.Timeout:
        return False, "Timeout"
    except requests.RequestException as e:
        return False, str(e)


def generate_repository(data: dict, dry_run: bool = False, verbose: bool = False) -> bool:
    """Generate RadioRepository.kt from station data."""
    constants = data['constants']
    stations = sorted(data['stations'], key=lambda x: x['name'].lower())
    #stations = sorted(data['stations'], key=lambda x: x.get('popularity', 0), reverse=True)
    
    # Build station entries
    station_entries = []
    for station in stations:
        genre_key = station.get('genre', '')
        country_key = station.get('country', '')
        
        # Map to R.string references
        genre_res = f"R.string.{constants['genres'].get(genre_key, 'genre_' + genre_key)}" if genre_key else "0"
        country_res = f"R.string.{constants['countries'].get(country_key, 'country_' + country_key)}" if country_key else "0"
        
        # Check if logo XML exists
        logo_xml = DRAWABLE_DIR / f"logo_{station['id']}.xml"
        logo_res_id = f"R.drawable.logo_{station['id']}" if logo_xml.exists() else "0"
        
        # Escape quotes in strings
        name = station['name'].replace('"', '\\"')
        description = station.get('description', '').replace('"', '\\"')
        tags = station.get('tags', '').replace('"', '\\"')
        popularity = station.get('popularity', 0)
        
        entry = f'''                        RadioStation(
                                id = "{station['id']}",
                                name = "{name}",
                                streamUrl = "{station['stream_url']}",
                                logoUrl = "{station.get('logo_url', '')}",
                                logoResId = {logo_res_id},
                                description = "{description}",
                                genre = {genre_res},
                                country = {country_res},
                                popularity = {popularity},
                                tags = "{tags}",
                                metadataType = {f'"{station["metadata_type"]}"' if "metadata_type" in station else "null"},
                                metadataParam = {f'"{station["metadata_param"]}"' if "metadata_param" in station else "null"}
                        )'''
        station_entries.append(entry)
    
    # Generate the full file
    stations_list = ',\n'.join(station_entries)
    
    repository_content = f'''// AUTO-GENERATED from stations.yaml - DO NOT EDIT MANUALLY
// Run: ./gradlew buildStations (or python scripts/build_stations.py)
// Generated with {len(stations)} stations

package org.guakamole.onair.data

import android.content.Context
import android.content.SharedPreferences
import org.guakamole.onair.R

/** Repository providing a curated list of public radio stations */
object RadioRepository {{

        private const val PREFS_NAME = "radio_prefs"
        private const val FAVORITES_KEY = "favorite_stations"
        private var prefs: SharedPreferences? = null
        private val favoriteIds = mutableSetOf<String>()

        private val baseStations: List<RadioStation> =
                listOf(
{stations_list}
                )

        /** Initialize favorites from persistent storage */
        fun initialize(context: Context) {{
                if (prefs == null) {{
                        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val savedFavorites =
                                prefs?.getStringSet(FAVORITES_KEY, emptySet()) ?: emptySet()
                        favoriteIds.clear()
                        favoriteIds.addAll(savedFavorites)
                }}
        }}

        /**
         * Returns the list of stations with correctly set isFavorite flags, sorted by favorite
         * status
         */
        val stations: List<RadioStation>
                get() =
                        baseStations
                                .map {{ station ->
                                        station.copy(isFavorite = favoriteIds.contains(station.id))
                                }}
                                .sortedByDescending {{ it.isFavorite }}

        fun toggleFavorite(stationId: String) {{
                if (favoriteIds.contains(stationId)) {{
                        favoriteIds.remove(stationId)
                }} else {{
                        favoriteIds.add(stationId)
                }}

                prefs?.edit()?.putStringSet(FAVORITES_KEY, favoriteIds)?.apply()
        }}

        fun getStationById(id: String): RadioStation? {{
                return stations.find {{ it.id == id }}
        }}

        fun getStationByIndex(index: Int): RadioStation? {{
                return stations.getOrNull(index)
        }}

        fun getStationIndex(id: String): Int {{
                return stations.indexOfFirst {{ it.id == id }}
        }}
}}
'''

    if dry_run:
        log(f"[DRY-RUN] Would generate RadioRepository.kt with {len(stations)} stations", force=True)
        return True
    
    with open(REPOSITORY_PATH, 'w', encoding='utf-8') as f:
        f.write(repository_content)
    
    log(f"✓ Generated RadioRepository.kt with {len(stations)} stations", force=True)
    return True


def main():
    parser = argparse.ArgumentParser(description='Build radio station resources')
    parser.add_argument('--validate-streams', action='store_true',
                        help='Check if stream URLs are reachable')
    parser.add_argument('--force-logos', action='store_true',
                        help='Re-download all logos even if they exist')
    parser.add_argument('--dry-run', action='store_true',
                        help="Don't write any files, just validate")
    parser.add_argument('--verbose', '-v', action='store_true',
                        help='Show detailed output')
    
    args = parser.parse_args()
    
    print("🎵 Building radio stations...")
    
    # Load stations
    try:
        data = load_stations()
        print(f"   Loaded {len(data['stations'])} stations from stations.yaml")
    except Exception as e:
        print(f"✗ Failed to load stations.yaml: {e}")
        sys.exit(1)
    
    # Process logos
    print("\n📥 Processing logos...")
    for station in data['stations']:
        log(f"\n  {station['name']}:", args.verbose, force=args.verbose)
        svg_path = fetch_svg(station, force=args.force_logos, verbose=args.verbose, dry_run=args.dry_run)
        if svg_path and svg_path.exists():
            convert_svg_to_vector(svg_path, force=args.force_logos, verbose=args.verbose, dry_run=args.dry_run)
    
    # Validate streams
    if args.validate_streams:
        print("\n📡 Validating streams...")
        for station in data['stations']:
            success, message = validate_stream(station, verbose=args.verbose)
            status = "✓" if success else "✗"
            print(f"  {status} {station['name']}: {message}")
    
    # Generate repository
    print("\n📝 Generating RadioRepository.kt...")
    generate_repository(data, dry_run=args.dry_run, verbose=args.verbose)
    
    print("\n✅ Done!")
    return 0


if __name__ == '__main__':
    sys.exit(main())
