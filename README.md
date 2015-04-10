# orbital detector

![KML screenshot](/screenshots/log2kml.jpg?raw=true "KML screenshot")

Code for finding, analyzing and visualizing police aircraft over Los Angeles


## Creating the database

This code assumes you have postgres and postgis installed.


### Prep the database

```
$ createdb orbital
$ cat orbital.sql | psql orbital
```


### Load a basestation.sqb

Put a basestation.sqb, from whatever source, in the current directory,
then run the following:

```
$ pgloader basestation.load
```


#### Ingest PlanePlotter logs

You can load PlanePlotter "receiver logs" AKA "Mode-S logs" AKA "RTL
logs" into the database. You can load gzipped or uncompressed logs
(log files typically get ~98% compression).

```
$ lein run -m com.lemondronor.orbital-detector.reports \
  postgresql://localhost:5432/orbital logs/RTL*.gz
```


## log2kml

`log2kml` creates a KML visualization from one or more PlanePlotter logs.

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

## License

Copyright © 2015 John Wiseman

Distributed under the MIT License.
