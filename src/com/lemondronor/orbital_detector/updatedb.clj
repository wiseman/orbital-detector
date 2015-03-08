(ns com.lemondronor.orbital-detector.updatedb
  "Updates a basestation.sqb with info from a CSV file.

  The CSV file should look like this:

    Registration,Callsign,ICAO,Type,Agency
    N184SD,,A1511C,AS50,Orange County Sheriff
    [etc.]

  Records that don't exist, using the ICAO code as a key, will be
  added to the basestation.sqb with the following fields set:

    ModeS         - ICAO code
    ICAOTypeCode  - Type
    Interested    - \"1\"
    UserTag       - Agency
    ModeSCountry  - \"United States\"
    Registration  - Registration code

  Records that already exist will be updated with the following
  values:

    ICAOTypeCode  - Type
    Interested    - \"1\"
    UserTag       - Agency
    ModeSCountry  - \"United States\"
    Registration  - Registration code"
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc])
  (:import ()))

(set! *warn-on-reflection* true)


(defn db-spec
  "Returns the JDBC spec for an sqlite database at the specified path."
  [path]
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname path})


(defn read-csv
  "Parses a CSV file. Assumes the first row defines the column names.
  Returns a sequence of records where each record is a map of
  columns (keywords) to values, e.g. {:id \"4\" :name \"john\"}."
  [path]
  (let [csv (csv/read-csv (io/reader path))]
    (map
     (fn [columns row]
       (into {} (map vector columns row)))
     (repeat (map keyword (first csv)))
     (rest csv))))


(defn find-aircraft
  "Looks up an aircraft in the database by ICAO ID."
  [db-con icao]
  (first
   (jdbc/query
    db-con
    ["SELECT * from AIRCRAFT WHERE ModeS = ?" icao])))


(defn db-values [csv-rec]
  {:interested "1"
   :usertag (:Agency csv-rec)
   :icaotypecode (:Type csv-rec)
   :modescountry "United States"
   :registration (:Registration csv-rec)})


(defn update-aircraft! [db-conn db-rec csv-rec]
  (println "UPDATING:")
  (println "----" db-rec)
  ;; I'm paranoid, so abort if we find mismatched ICAO, registration
  ;; or type.
  (assert (= (:ICAO csv-rec) (:modes db-rec)))
  (assert (= (:Registration csv-rec) (:registration db-rec)))
  (assert (= (:Type csv-rec) (:icaotypecode db-rec)))
  (let [values (db-values csv-rec)]
    (println "---- with" values)
    (jdbc/update!
     db-conn
     :Aircraft
     values
     ["ModeS = ?" (:ICAO csv-rec)])))



(defn insert-aircraft! [db-conn csv-rec]
  (let [values (assoc (db-values csv-rec)
                      :modes (:ICAO csv-rec))]
    (println "INSERTING:" values)
    (jdbc/insert!
     db-conn
     :Aircraft
     values)))


(defn update-db! [db-spec csv-path]
  (let [csv (read-csv csv-path)]
    (jdbc/with-db-connection [db-conn db-spec]
      (doseq [csv-rec csv]
        (let [db-rec (find-aircraft db-conn (:ICAO csv-rec))]
          (if csv-rec
            (update-aircraft! db-conn db-rec csv-rec)
            (insert-aircraft! db-conn csv-rec)))))))



(defn -main [& args]
  (let [csv-path (first args)
        db-path (second args)]
    (update-db! (db-spec db-path) csv-path)))
