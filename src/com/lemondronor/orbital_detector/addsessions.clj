(ns com.lemondronor.orbital-detector.addsessions
  ""
  (:require [clj-time.coerce :as timecoerce]
            [clj-time.core :as time]
            [clojure.java.jdbc :as jdbc])
  (:import (java.sql ResultSet)))

(set! *warn-on-reflection* true)


(defn new-session-time? [prev-ts ts]
  (or (nil? prev-ts)
      (let [d (time/in-seconds (time/interval prev-ts ts))]
        (> d (* 60 60)))))


(defn process-result-set [^ResultSet rs]
  (loop [sessions []
         ac-infos {}
         num-rows 1
         rows (rest (jdbc/result-set-seq rs :as-arrays? true))]
    (if (= (mod num-rows 10000000) 0)
      (println "Processed" num-rows "records"))
    (if (seq rows)
      (let [[icao ts lat lon] (first rows)
            ts (timecoerce/from-sql-time ts)
            ac-info (ac-infos icao)]
        (if (or (nil? ac-info)
                (new-session-time? (get ac-info :end-ts) ts))
          (do
            ;;(println "New session for" icao ts (nil? ac-info))
            (recur (if (nil? ac-info)
                     sessions
                     (conj sessions ac-info))
                   (assoc ac-infos icao
                          {:icao icao :start-ts ts :end-ts ts})
                   (inc num-rows)
                   (rest rows)))
          (recur sessions
                 (assoc ac-infos icao
                        (merge ac-info {:end-ts ts}))
                 (inc num-rows)
                 (rest rows))))
      (concat sessions ac-infos))))


(defn session-length [s]
  (if (and (:start-ts s) (:end-ts s))
    (time/in-seconds (time/interval (:start-ts s) (:end-ts s)))
    nil))


(defn mean [ns]
  (/ (apply + ns) (count ns)))

(defn add-sessions [db-spec]
  (let [sessions
        (jdbc/with-db-transaction [db db-spec]
          (jdbc/db-query-with-resultset
           db
           [
            ;; These options attempts to persuade the database to stream
            ;; results back instead of returning one giant resultset.
            {:fetch-size 10000
             :concurrency :read-only
             :result-type :forward-only}
            "select icao, timestamp, lat, lon from reports order by timestamp asc"]
           process-result-set))]
    (println (count sessions) "sessions")
    (println (count (distinct (map :icao sessions))) "aircraft")
    (->> sessions
         (map session-length)
         (filter (complement nil?))
         mean
         float
         (println "Mean session length"))))
