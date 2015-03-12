(ns com.lemondronor.orbital-detector.planeplotter
  "Reads and processes PlanePlotter logs."
  (:require [clj-time.coerce :as timecoerce]
            [clj-time.format :as timefmt]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (java.util.zip GZIPInputStream)))

(set! *warn-on-reflection* true)


(def datetime-fmt (timefmt/formatter-local "YYYY/MM/dd HH:mm:ss.SSS"))


(defn parse-log-csv [csv]
  {:timestamp (timecoerce/to-long
               (timefmt/parse
                datetime-fmt (string/join " " [(csv 0) (csv 1)])))
   :icao (csv 3)
   :registration (let [r (string/trim (csv 4))]
                   (if (pos? (count r))
                     r
                     nil))
   :altitude (Long/parseLong (csv 7))
   :position {:lat (Double/parseDouble (csv 9))
              :lon (Double/parseDouble (csv 10))}
   :speed (Double/parseDouble (csv 13))
   :heading (Double/parseDouble (csv 14))})


(defn gzip-reader [path]
  (io/reader (GZIPInputStream. (io/input-stream path))))


(defn log-reader [^String path]
  (if (.endsWith path ".gz")
    (gzip-reader path)
    (io/reader path)))


(defn read-log [log]
  (->> log
       log-reader
       csv/read-csv
       (map parse-log-csv)))
