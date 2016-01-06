-- The orbital database should have 3 tables:
--
--  * Aircraft (imported from a basestation.sqb, see basestation.load)
--  * aircraft_info (defined below)
--  * reports (See ...)

CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS provenances(
       code INTEGER primary key,
       description VARCHAR(20)
);

INSERT INTO provenances VALUES
       (1, 'planeplotter logs'),
       (2, 'mlat');

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
       squawk VARCHAR(4),
       provenance INTEGER
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

CREATE TABLE IF NOT EXISTS faa_registration (
       n_number VARCHAR(6),
       serial_number VARCHAR(30),
       mfr_mdl_code VARCHAR(7),
       eng_mfr_mdl VARCHAR(5),
       year_mfr VARCHAR(4),
       type_registrant VARCHAR(1),
       name varchar(50),
       street VARCHAR(33),
       street2 VARCHAR(33),
       city VARCHAR(18),
       state VARCHAR(2),
       zip_code VARCHAR(10),
       region VARCHAR(1),
       county VARCHAR(3),
       country VARCHAR(2),
       last_action_date VARCHAR(8),
       cert_issue_date VARCHAR(8),
       certification VARCHAR(10),
       type_aircraft VARCHAR(1),
       type_engine VARCHAR(2),
       status_code VARCHAR(2),
       mode_s_code VARCHAR(8),
       fract_owner VARCHAR(1),
       air_worth_date VARCHAR(8),
       other_names_1 VARCHAR(50),
       other_names_2 VARCHAR(50),
       other_names_3 VARCHAR(50),
       other_names_4 VARCHAR(50),
       other_names_5 VARCHAR(50),
       expiration_date VARCHAR(8),
       unique_id VARCHAR(8),
       kit_mfr VARCHAR(30),
       kit_model VARCHAR(20),
       mode_s_code_hex VARCHAR(10)
 );

CREATE INDEX faa_registration_mode_s_code_hex_index on faa_registration (mode_s_code_hex);
