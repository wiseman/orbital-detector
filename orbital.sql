-- The orbital database should have 3 tables:
--
--  * Aircraft (imported from a basestation.sqb, see basestation.load)
--  * aircraft_info (defined below)
--  * reports (See ...)

CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS reports (
       "timestamp" timestamp with time zone NOT NULL,
       icao VARCHAR(6) NOT NULL,
       registration VARCHAR(20),
       altitude INTEGER,
       lat DOUBLE PRECISION,
       lon DOUBLE PRECISION,
       "position" geometry(Point,4326),
       speed REAL,
       heading REAL,
       squawk VARCHAR(4)
);

CREATE INDEX reports_icao_index on reports (icao);

CREATE VIEW reports_with_distance_from_me AS
       SELECT *,  ST_Distance_Sphere(position, ST_MakePoint(-118.192310, 34.133777)) AS distance
       FROM reports;


CREATE TABLE IF NOT EXISTS aircraft_info (
       icao VARCHAR(6) primary key,
       image text,
       link text,
       photographer text
);

CREATE INDEX aircraft_info_icao_index on aircraft_info (icao);
