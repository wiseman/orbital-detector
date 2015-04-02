Add PostGIS extension:

```
$ psql orbital
orbital=# CREATE EXTENSION postgis;
```

Create the sqlite database:

```
$ time lein run -m com.lemondronor.orbital-detector.reports reports.sqb logs/*.gz
```

Import into postgres:

```
$ pgloader --type sqlite reports.sqb 'postgresql:///orbital?reports'
```

Add the geometry:

```
$ psql orbital
orbital=# SELECT AddGeometryColumn('reports', 'position', 4326, 'POINT', 2);
orbital=# UPDATE reports SET position = ST_SetSRID(ST_MakePoint(lon, lat), 4326);
```

Add a view that includes distance from a particular point (in my case, my house):

```
orbital=# CREATE VIEW distanced_from_home AS SELECT *,  ST_Distance_Sphere(position, ST_MakePoint(-118.192310, 34.133777)) AS distance FROM reports;
```


Create an index on ICAO:

```
orbital=# CREATE INDEX icao_index ON reports (icao);
```
