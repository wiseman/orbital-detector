import sys


MAX_GAP_SECS = 300

# MSG,3,1,1,A5FD2D,1,2015/09/23,03:29:23.705,2015/09/23,03:29:23.705,,39732,441,335,33.9931,-118.4594,888,,,,,

PROLOGUE = """
{ "type": "FeatureCollection",
    "features": [
      { "type": "Feature",
        "geometry": {"type": "Point", "coordinates": [102.0, 0.5]},
        "properties": {"prop0": "value0"}
        },
      { "type": "Feature",
        "geometry": {
          "type": "LineString",
          "coordinates": [
"""
EPILOGUE = """
]}}]}
"""


def main():
    coords = []
    for line in sys.stdin:
        pieces = line.split(',')
        lat = pieces[14]
        lon = pieces[15]
        coords.append('[%s, %s]' % (lon, lat))
    sys.stderr.write('%s coords\n' % (len(coords),))
    print PROLOGUE
    print ','.join(coords)
    print EPILOGUE


if __name__ == '__main__':
    main()
