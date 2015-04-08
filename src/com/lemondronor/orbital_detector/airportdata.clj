(ns com.lemondronor.orbital-detector.airportdata
  ""
  (:require [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [com.lemondronor.orbital-detector.net :as net]))


 (set! *warn-on-reflection* true)


(def db "postgresql://localhost:5432/orbital")

(def aircraft-info-cache (atom {}))


(defn fetch-aircraft-info-from-db [icao]
  (let [records (jdbc/query
                 db
                 ["SELECT * FROM aircraft_info WHERE icao = ?"
                  icao])]
    (when (seq records)
      (let [record (first records)]
        (assert (= (count records) 1))
        (swap! aircraft-info-cache assoc icao record)
        record))))


(defn insert-aicraft-info-into-db [icao record]
  (assoc record :icao icao)
  (log/info
   "Adding aircraft info for" icao
   "to aircraft_info in DB:" record)
  (jdbc/insert!
   db
   :aircraft_info
   (assoc record :icao icao)))


(defn keywordize-json-obj [o]
  (into {} (for [[k v] o] [(keyword k) v])))


(defn fetch-aircraft-info-from-site [icao]
  (log/info "Fetching info for" icao "from airport-data.com")
  (let [result (json/read-str
                (net/fetch-url
                 (str "http://www.airport-data.com/api/ac_thumb.json?m="
                      icao)))]
    (if (= (result "status") 200)
      (let [record (keywordize-json-obj (first (result "data")))]
        (insert-aicraft-info-into-db icao record)
        (swap! aircraft-info-cache assoc icao record)
        record)
      (do
        (log/warn "bad data; airport-data.com returned" result)
        nil))))


(defn aircraft-info [icao]
  (let [icao (string/upper-case icao)]
    (or (@aircraft-info-cache icao)
        (fetch-aircraft-info-from-db icao)
        (fetch-aircraft-info-from-site icao))))
