-- The orbital database should have 3 tables:
--
--  * Aircraft (imported from a basestation.sqb, see basestation.load)
--  * aircraft_info (defined below)
--  * reports (See ...)

CREATE TABLE IF NOT EXISTS aircraft_info (
       icao text primary key,
       image text,
       link text,
       photographer text);
CREATE INDEX aircraft_info_index on aircraft_info (icao);
