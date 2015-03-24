(ns com.lemondronor.orbital-detector.createdb
  ""
  (:require [clj-time.coerce :as timecoerce]
            [clj-time.core :as time]
            [clj-time.format :as timefmt]
            [clojure.java.jdbc :as jdbc]
            [com.lemondronor.orbital-detector.planeplotter :as planeplotter]
            [com.lemonodor.gflags :as gflags]))

(set! *warn-on-reflection* true)


(gflags/define-integer "time-window-secs"
  1
  "Only 1 ping will be recorded for each aircraft during each interval of this long.")

(gflags/define-boolean "with-position-only"
  true
  "Only keep pings that have position information.")


(defn db-spec [path]
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname path})


(defn create-tables [db]
  (jdbc/db-do-commands
   db
   "CREATE TABLE IF NOT EXISTS reports (timestamp integer, icao text, registration text, altitude integer, lat real, lon real, speed real, heading real)"))

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


(defn add-records [db log]
  (jdbc/with-db-transaction [t-con db]
    (let [ping-window (gflags/flags :time-window-secs)
          position-only? (gflags/flags :with-position-only)]
      (loop [records log
             previous-ping-time {}
             num-inserted 0
             num-skipped 0]
        (if (seq records)
          (let [r (first records)
                rstrecords (rest records)
                icao (:icao r)
                ping-ts (:timestamp r)
                previous-ping-ts (previous-ping-time icao)]
            (if (and (or (not position-only?)
                         (> (:lat (:position r)) 0.0))
                     (or (nil? previous-ping-ts)
                         (>= (time/in-seconds
                              (time/interval previous-ping-ts ping-ts))
                             ping-window)))
              (do
                (jdbc/insert!
                 t-con
                 :reports
                 {:timestamp (timefmt/unparse
                              ;; 2015-03-20 11:39:00
                              (timefmt/formatters :basic-date-time)
                              (:timestamp r))
                  :icao (:icao r)
                  :registration (:registration r)
                  :altitude (:altitude r)
                  :lat (:lat (:position r))
                  :lon (:lon (:position r))
                  :speed (:speed r)
                  :heading (:heading r)})
                (recur rstrecords
                       (assoc previous-ping-time icao ping-ts)
                       (inc num-inserted)
                       num-skipped))
              (recur rstrecords
                     previous-ping-time
                     num-inserted
                     (inc num-skipped))))
          (do
            (println "Inserted" num-inserted "records")
            (println "Skipped" num-skipped "records")))))))



(defn -main [& args]
  (let [args (gflags/parse-flags (cons nil args))
        db (db-spec (first args))]
    (create-tables db)
    (add-records db (planeplotter/read-log (second args)))))
