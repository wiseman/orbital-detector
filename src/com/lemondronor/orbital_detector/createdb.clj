(ns com.lemondronor.orbital-detector.createdb
  ""
  (:require [clj-time.coerce :as timecoerce]
            [clj-time.core :as time]
            [clj-time.format :as timefmt]
            [clojure.java.jdbc :as jdbc]
            [clojure.pprint :as pprint]
            [com.lemondronor.orbital-detector.db :as db]
            [com.lemondronor.orbital-detector.planeplotter :as planeplotter]
            [com.lemonodor.gflags :as gflags]))

(set! *warn-on-reflection* true)


(gflags/define-integer "time-window-secs"
  1
  "Only 1 ping will be recorded for each aircraft during each interval of this long.")

(gflags/define-boolean "with-position-only"
  true
  "Only keep pings that have position information.")


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


(defn has-position? [r]
  (not (and (zero? (:lat (:position r)))
            (zero? (:lon (:position r))))))


(defn add-records [db log]
  (jdbc/with-db-transaction [t-con db]
    (let [ping-window (gflags/flags :time-window-secs)
          position-only? (gflags/flags :with-position-only)]
      (loop [records log
             previous-ping-time {}
             num-inserted 0
             num-skipped {:no-position 0
                             :non-monotonic-time 0
                             :rate-too-high 0}]
        (if (seq records)
          (let [r (first records)
                rstrecords (rest records)
                icao (:icao r)
                ping-ts (:timestamp r)
                previous-ping-ts (previous-ping-time icao)]
            (cond
              ;; No position, and position is required.
              (and position-only? (not (has-position? r)))
              (recur rstrecords
                     previous-ping-time
                     num-inserted
                     (update-in num-skipped [:no-position] inc))
              ;; Non-monotonic time.
              (and previous-ping-ts
                   (time/before? ping-ts previous-ping-ts))
              (recur rstrecords
                     previous-ping-time
                     num-inserted
                     (update-in num-skipped [:non-monotonic-time] inc))
              ;; Too soon since last ping.
              (and previous-ping-ts
                   (< (time/in-seconds (time/interval previous-ping-ts ping-ts))
                      ping-window))
              (recur rstrecords
                     previous-ping-time
                     num-inserted
                     (update-in num-skipped [:rate-too-high] inc))
              :else
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
                       num-skipped))))
          (do
            (println "Inserted" num-inserted "records")
            (println "Skipped:" (reduce + (map second num-skipped)) "total")
            (pprint/print-table [num-skipped])))))))



(defn -main [& args]
  (let [args (gflags/parse-flags (cons nil args))
        db (db/db-spec (first args))]
    (create-tables db)
    (add-records
     db
     (mapcat
      (fn [log-path]
        (println "Reading " log-path)
        (planeplotter/read-log log-path))
      (rest args)))))
