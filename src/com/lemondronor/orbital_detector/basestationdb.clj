(ns com.lemondronor.orbital-detector.basestationdb
  ""
  (:require
   [clojure.java.jdbc :as jdbc]))

(set! *warn-on-reflection* true)


(defn db-spec
  "Returns the JDBC spec for an sqlite database at the specified path."
  [path]
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname path})


(defn find-aircraft
  "Looks up an aircraft in the database by ICAO ID."
  [db-con icao]
  (first
   (jdbc/query
    db-con
    ["SELECT * from AIRCRAFT WHERE ModeS = ?" icao])))


(defn update-aircraft! [db-conn icao values]
  (jdbc/update! db-conn :Aircraft values ["ModeS = ?" icao]))


(defn insert-aircraft! [db-conn values]
  (jdbc/insert! db-conn :Aircraft values))
