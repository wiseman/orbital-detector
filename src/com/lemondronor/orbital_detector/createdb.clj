(ns com.lemondronor.orbital-detector.createdb
  ""
  (:require [clojure.java.jdbc :as jdbc]
            [com.lemondronor.orbital-detector.planeplotter :as orbdet]))

(set! *warn-on-reflection* true)


(def db-spec
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "orbital.db"
   })


(defn create-tables [db]
  (jdbc/db-do-commands
   db
   (jdbc/create-table-ddl
    :reports
    [:timestamp :integer]
    [:icao :text]
    [:registration :text]
    [:altitude :integer]
    [:lat :real]
    [:lon :real]
    [:speed :real]
    [:heading :real])))


(defn add-records [log]
  (jdbc/with-db-transaction [t-con db-spec]
    (doseq [r log]
      (jdbc/insert!
       t-con
       :reports
       {:timestamp (long (/ (:timestamp r) 1000))
        :icao (:icao r)
        :registration (:registration r)
        :altitude (:altitude r)
        :lat (:lat (:position r))
        :lon (:lon (:position r))
        :speed (:speed r)
        :heading (:heading r)}))))



(defn -main [& args]
  (create-tables db-spec)
  (add-records (orbdet/read-log (first args))))
