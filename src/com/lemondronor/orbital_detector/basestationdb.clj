(ns com.lemondronor.orbital-detector.basestationdb
  "Code for accessing 'basestation.sqb' databases, the ugly ad hoc
  standard of the planespotting community."
  (:require [clojure.java.jdbc :as jdbc]
            [com.lemondronor.orbital-detector.db :as db]))

(set! *warn-on-reflection* true)


(defn find-aircraft
  "Looks up an aircraft in the database by ICAO ID."
  [db-con icao]
  (first
   (jdbc/query
    db-con
    ["SELECT * FROM Aircraft WHERE ModeS = ?" icao])))


(defn update-aircraft! [db-conn icao values]
  (jdbc/update! db-conn :Aircraft values ["ModeS = ?" icao]))


(defn insert-aircraft! [db-conn values]
  (jdbc/insert! db-conn :Aircraft values))


(defn load-db [path]
  (jdbc/query
   (db/sqlite-db-spec path)
   "SELECT * FROM Aircraft"))
