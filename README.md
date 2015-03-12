# orbital detector

![KML screenshot](/screenshots/log2kml.jpg?raw=true "KML screenshot")

Code for finding, analyzing and visualizing police aircraft over Los Angeles

## log2kml

`log2kml` creates a KML visualization from one or more PlanePlotter logs.

```
$ lein run -m com.lemondronor.orbital-detector.log2kml \
  --icaos $(cat icaos.txt) \
  --extended-data aircraft.csv\
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
