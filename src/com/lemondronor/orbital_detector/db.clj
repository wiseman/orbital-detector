(ns com.lemondronor.orbital-detector.db
  "Database utilties."
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.java.jdbc.deprecated :as jdbcdep]
   [clojure.string :as string]
   [clojure.tools.logging :as log]))

(set! *warn-on-reflection* true)


(defn sqlite-db-spec [path]
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname path})


(defn streaming-query [db query fn]
  (let [db-con (doto (jdbc/get-connection db)
                 (.setAutoCommit false))]
    (let [stmt (jdbc/prepare-statement
                db-con
                query
                :fetch-size 10000
                :concurrency :read-only
                :result-type :forward-only)]
      (jdbcdep/with-query-results* [stmt] fn))))


(defn -main [& args]
  (let [db-url (first args)]
    (println db-url)
    (streaming-query
     db-url
     ;;"select timestamp, icao, lat, lon from reports where lat is not null and lon is not null order by timestamp asc"
     "select distinct icao from reports where lat is not null and lon is not null"
     (fn [rs]
       (println (count rs))))))
