(ns com.lemondronor.orbital-detector.reports
  "Code for building and accessing a ping report database."
  (:require [clj-time.coerce :as timecoerce]
            [clj-time.core :as time]
            [clj-time.format :as timefmt]
            [clojure.java.jdbc :as jdbc]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [com.lemondronor.orbital-detector.db :as db]
            [com.lemondronor.orbital-detector.planeplotter :as planeplotter]
            [com.lemonodor.gflags :as gflags]))

(set! *warn-on-reflection* true)


(gflags/define-integer "time-window-secs"
  1
  "Only 1 ping will be recorded for each aircraft during each interval of this long.")

(gflags/define-boolean "with-position-only"
  false
  "Only keep pings that have position information.")


(def db-timestamp-formatter
  (timefmt/formatters :basic-date-time))


(defn sql-name [x] (name x))


(defn sql-str [x]
  (if x
    (str "'" (string/replace x #"'" "''") "'")
    "NULL"))


(defn has-position? [r]
  (not (and (zero? (:lat r))
            (zero? (:lon r)))))


(defn log-record-to-db-record [r]
  (let [db-rec
        {:timestamp (sql-str (timefmt/unparse db-timestamp-formatter (:timestamp r)))
         :icao (sql-str (:icao r))
         :registration (sql-str (:registration r))
         :altitude (:altitude r)
         :speed (:speed r)
         :heading (:heading r)
         :squawk (sql-str (:squawk r))}]
    (if (has-position? r)
      (assoc db-rec
             :lat (:lat r)
             :lon (:lon r)
             :position (format "ST_SetSRID(ST_MakePoint(%s,%s), 4326)" (:lon r) (:lat r)))
      (assoc db-rec
             :lat "NULL"
             :lon "NULL"
             :position "NULL"))))


;; I really did try to do the right thing and not work with SQL text,
;; but man it was going to be a lot of work just to see if I could
;; make using postgis data types work via JDBC.

(defn insert-sql [table record]
  (string/join
   " "
   ["INSERT INTO" (sql-name table)
    "(" (string/join ", " (map sql-name (keys record))) ")"
    "VALUES"
    "(" (string/join ", " (vals record)) ")"]))


(defn insert! [db table record]
  (let [sql (insert-sql table record)]
    (jdbc/execute! db [sql])))


(defn add-records! [db log]
  (jdbc/with-db-transaction [t-con db]
    (let [ping-window (gflags/flags :time-window-secs)
          position-only? (gflags/flags :with-position-only)
          start-time (time/now)]
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
                (insert! t-con :reports (log-record-to-db-record r))
                (recur rstrecords
                       (assoc previous-ping-time icao ping-ts)
                       (inc num-inserted)
                       num-skipped))))
          (do
            (println "Inserted" num-inserted "records"
                     "in"
                     (time/in-seconds (time/interval start-time (time/now)))
                     "seconds.")
            (println "Skipped:" (reduce + (map second num-skipped)) "total")
            (pprint/print-table [num-skipped])))))))



(defn -main [& args]
  (let [args (gflags/parse-flags (cons nil args))
        db-url (first args)
        log-paths (rest args)]
    (doseq [log-path log-paths]
      (println "Importing" log-path)
      (add-records!
       db-url
       (planeplotter/read-log log-path)))))
