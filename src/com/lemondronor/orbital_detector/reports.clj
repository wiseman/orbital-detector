(ns com.lemondronor.orbital-detector.reports
  ""
  (:require [clj-time.coerce :as timecoerce]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [com.lemondronor.orbital-detector.planeplotter :as orbdet]))

(set! *warn-on-reflection* true)


(def db-schema
  {:reports
   {:timestamp :integer
    :icao :text
    :registration :text
    :altitude :integer
    :lat :real
    :lon :real
    :speed :real
    :heading :real}})


(defn log-record-to-db-record [r]
  {:timestamp (long (/ (timecoerce/to-long (:timestamp r)) 1000))
   :icao (:icao r)
   :registration (:registration r)
   :altitude (:altitude r)
   :lat (:lat (:position r))
   :lon (:lon (:position r))
   :speed (:speed r)
   :heading (:heading r)})


(defn add-records! [db-path log]
  (jdbc/with-db-transaction [t-con (db-spec db-path)]
    (doseq [r log]
      (jdbc/insert!
       t-con
       :reports
       (log-record-to-db-record r)))))


(defn -main [& args]
  (let [db-path (first args)
        log-path (second args)]
    (create-db! (db-spec db-path) db-schema)
    (add-records! db-path (orbdet/read-log log-path))))
