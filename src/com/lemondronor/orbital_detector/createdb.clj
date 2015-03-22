(ns com.lemondronor.orbital-detector.createdb
  ""
  (:require [clj-time.coerce :as timecoerce]
            [clojure.java.jdbc :as jdbc]
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
   "CREATE TABLE IF NOT EXISTS reports (timestamp integer, icao text, registration text, type text, altitude integer, lat real, lon real, speed real, heading real)"))

   ;; (println (jdbc/create-table-ddl
   ;;  :reports
   ;;  [:timestamp :integer]
   ;;  [:icao :text]
   ;;  [:registration :text]
   ;;  [:altitude :integer]
   ;;  [:lat :real]
   ;;  [:lon :real]
   ;;  [:speed :real]
   ;;  [:heading :real]))))


(defn add-records [log]
  (jdbc/with-db-transaction [t-con db-spec]
    (doseq [r log]
      (jdbc/insert!
       t-con
       :reports
       {:timestamp (long (/ (timecoerce/to-long (:timestamp r)) 1000))
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
