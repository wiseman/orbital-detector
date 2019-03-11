import sys


def main():
    first_line = True
    coords = []
    for line in sys.stdin:
        if first_line:
            first_line = False
        else:
            pieces = line.split(',')
            # sys.stderr.write('%s\n' % (pieces,))
            lat = float(pieces[2][1:])
            lon = float(pieces[3][:-1])
            coords.append([lat, lon])
    print '{"type": "Feature", "geometry": {"type": "LineString", "coordinates": ['
    print ','.join(['[%s,%s]' % (c[1], c[0]) for c in coords])
    print ']}}'


if __name__ == '__main__':
    main()
