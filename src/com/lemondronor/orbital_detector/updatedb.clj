(ns com.lemondronor.orbital-detector.updatedb
  "Updates a basestation.sqb with info from a CSV file."
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc])
  (:import ()))

(set! *warn-on-reflection* true)


(defn db-spec [path]
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname path})


(defn read-csv [path]
  (let [csv (csv/read-csv (io/reader path))]
    (map
     (fn [columns row]
       (into {} (map vector columns row)))
     (repeat (map keyword (first csv)))
     (rest csv))))


(defn find-aircraft [db-con icao]
  (first
   (jdbc/query
    db-con
    ["SELECT * from AIRCRAFT WHERE ModeS = ?" icao])))


(defn update-aircraft! [db-conn aircraft row]
  (println "UPDATING:")
  (println "----" aircraft)
  (println "----" row)
  (assert (= (:ICAO row) (:modes aircraft)))
  (assert (= (:Registration row) (:registration aircraft)))
  (assert (= (:Type row) (:icaotypecode aircraft)))
  (jdbc/update!
   db-conn
   :Aircraft
   {:interested "1"
    :usertag (:Agency row)}
   ["ModeS = ?" (:ICAO row)]))



(defn insert-aircraft! [db-conn row]
  (let [values {:modes (:ICAO row)
                :interested "1"
                :icaotypecode (:Type row)
                :modescountry "United States"
                :registration (:Registration row)}]
    (println "INSERTING:" values)
    (jdbc/insert!
     db-conn
     :Aircraft
     values)))


(defn update-db! [db-spec csv-path]
  (let [csv (read-csv csv-path)]
    (jdbc/with-db-connection [db-conn db-spec]
      (doseq [row csv]
        (let [aircraft (find-aircraft db-conn (:ICAO row))]
          (if aircraft
            (update-aircraft! db-conn aircraft row)
            (insert-aircraft! db-conn row)))))))



(defn -main [& args]
  (let [csv-path (first args)
        db-path (second args)]
    (update-db! (db-spec db-path) csv-path)))
