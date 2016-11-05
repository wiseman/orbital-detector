- [orbital detector](#)
    - [Creating the database](#)
        - [Prep the database](#)
        - [Load a basestation.sqb](#)
        - [Ingest PlanePlotter logs](#)
    - [Visualizations](#)
        - [log2kml](#)
        - [Tippecanoe](#)
        - [Gorilla REPL worksheet](#)
    - [License](#)

# orbital detector

Code for finding, analyzing and visualizing police aircraft over Los Angeles.

## Creating the database

This code assumes you have postgres and postgis installed.


### Prep the database

```
$ createdb orbital
$ cat create-tables.sql | psql orbital
```


### Load a basestation.sqb

Put a basestation.sqb, from whatever source, in the current directory,
then run the following:

```
$ pgloader basestation.load
```

### Load the FAA aircraft registry

Once you've downloaded a copy of the
[FAA aircraft registry](registration database), expand the archive and
copy the `MASTER.txt` file to the current directory. You can then load
the FAA registry into your postgres database with the following:

```
$ pgloader faa.load
```

### Ingest PlanePlotter logs

You can load PlanePlotter "receiver logs" AKA "Mode-S logs" AKA "RTL
logs" into the database. You can load gzipped or uncompressed logs
(log files typically get ~98% compression).

There is one annoying detail: You need to change the line endings in
the log files from Windows (CRLF) to Unix (LF). Here's an example of
doing that:

```
$ echo *.log | \
  xargs -t -n 1 -P 4 -I {} \
  sh -c "cat {} | tr -d '\r' | time gzip -9cv > {}.gz"
```

Then to load the logs in the database:

```
$ lein run -m com.lemondronor.orbital-detector.reports \
  postgresql://localhost:5432/orbital logs/RTL*.gz
```

Reports are put into the `reports` table, which has the following schema:

| Column       | Description                 |
|--------------|-----------------------------|
| timestamp    | timestamp with time zone    |
| icao         | ICAO hex code (string)      |
| registration | Callsign/registration       |
| altitude     | Altitude                    |
| lat          | Latitude                    |
| lon          | Longitude                   |
| position     | PostGIS geometry of lat/lon |
| speed        | Speed                       |
| heading      | Heading                     |
| squawk       | [Squawk code](http://en.wikipedia.org/wiki/Transponder_%28aeronautics%29#Code_assignments) |


By default, at most one ping per aircraft per second is saved. You can
use `--time-window-secs` to do something different:
`--time-window-secs 0` will keep every ping, `--time-window-secs 10`
will keep only one ping per aircraft per 10 seconds.

By default, all pings are saved, including positionless (mode A/C)
pings. If you only want to save pings that have positions, use
`--with-position-only`.


## Visualizations

### log2kml

![KML screenshot](/screenshots/log2kml.jpg?raw=true "KML screenshot")

You can create KML tracks of aircraft with log2kml.

```
$ lein run -m com.lemondronor.orbital-detector.log2kml \
  --icaos AE094B,A68A37 \
  --extended-data aircraft.csv \
  RTL15030200.log.gz RTL15030300.log.gz > log.kml
```

It can handle gzipped or uncompressed logs.

You can use the `--icaos` flag to specify a comma-delimited list of
the ICAO codes of the aircraft you're interested in.

The `--extended-data` flag can be used to provide extra information
about aircraft. It should be the path to a CSV-exported version of a
file that has the same structure as
[this spreadsheet](https://docs.google.com/spreadsheets/d/1lAJzkdHX554RbqzRU3hIIl6Aen9b57d_oXbOQnQTzRY/edit?usp=sharing).


### Tippecanoe

You can use [tippecanoe](https://github.com/mapbox/tippecanoe) to
create highly detailed, zoomable maps.

![tippecanoe map screenshot](/screenshots/tippecanoe-map.jpg?raw=true "tippecanoe map screenshot")

<iframe width="100%" height="500px" frameBorder="0" src="https://api.tiles.mapbox.com/v4/wiseman.61620a88/page.html?access_token=pk.eyJ1Ijoid2lzZW1hbiIsImEiOiJHbzAtOHgwIn0.Pj1Nx77LS1-ujzRKJVOttA#10/33.9365/-118.3736"></iframe>


### Gorilla REPL worksheet

There is a Gorilla REPL worksheet to get you started with miscellaneous exploration and analysis.

![Gorilla repl screenshot](/screenshots/worksheet-map.jpg?raw=true "Gorilla repl screenshot")
![Gorilla repl screenshot](/screenshots/worksheet-hour-of-week.png?raw=true "Gorilla repl screenshot")

You can
[preview it in the Gorilla REPL viewer](http://viewer.gorilla-repl.org/view.html?source=github&user=wiseman&repo=orbital-detector&path=stats.cljw),
or you can run it yourself:

```
$ lein gorilla
Gorilla-REPL: 0.3.4
Started nREPL server on port 61968
Running at http://127.0.0.1:61970/worksheet.html .
Ctrl+C to exit.
```

Go to the specified URL, then type CTRL-g CTRL-l and load the
worksheet called `stats.cljw`.


## License

Copyright © 2015 John Wiseman

Distributed under the MIT License.
