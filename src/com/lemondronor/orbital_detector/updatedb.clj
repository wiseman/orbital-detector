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
            [clojure.java.jdbc :as jdbc]
            [com.lemondronor.orbital-detector.basestationdb :as basestationdb]))

(set! *warn-on-reflection* true)

(def this-namespace *ns*)


(defn error [& args]
  (flush)
  (binding [*out* *err*]
    (apply println args)))


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
    (basestationdb/update-aircraft! db-conn (:ICAO csv-rec) values)))


(defn insert-aircraft! [db-conn csv-rec]
  (let [values (assoc (db-values csv-rec)
                      :modes (:ICAO csv-rec))]
    (println "INSERTING:" values)
    (basestationdb/insert-aircraft! db-conn values)))


(defn update-db! [db-spec csv-path]
  (let [csv (read-csv csv-path)]
    (jdbc/with-db-transaction [t-conn db-spec]
      (doseq [csv-rec csv]
        (let [db-rec (basestationdb/find-aircraft t-conn (:ICAO csv-rec))]
          (if db-rec
            (update-aircraft! t-conn db-rec csv-rec)
            (insert-aircraft! t-conn csv-rec)))))))



(defn -main [& args]
  (if-not (= (count args) 2)
    (do (error "Usage:" this-namespace "<csv path> <basestation.sqb path>")
        1)
    (let [csv-path (first args)
          db-path (second args)]
      (update-db! (basestationdb/db-spec db-path) csv-path))))
