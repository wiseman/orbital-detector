(ns com.lemondronor.orbital-detector
  (require
   [clj-time.coerce :as timecoerce]
   [clj-time.format :as timefmt]
   [clojure.data.csv :as csv]
   [clojure.string :as string]
   [com.lemonodor.gflags :as gflags]
   [geo.poly :as poly]
   [geo.spatial :as spatial])
  (import
   (java.util.zip GZIPInputStream)
   (org.joda.time.base BaseDateTime))
  (:gen-class)
  (:require [clojure.java.io :as io]))

(set! *warn-on-reflection* true)


(gflags/define-string "extended-data"
  nil
  "CSV file containing extended aircraft data.")


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
       line-seq
       (map csv/read-csv)
       (map first)
       (map parse-log-csv)))


(defn normalize-hdg [h]
  (cond
    (> h 360.0) (normalize-hdg (- h 360.0))
    (< h 0.0) (normalize-hdg (+ h 360.0))
    (= h 360.0) 0.0))


(defn turning-direction [hdg1 hdg2])


(defn constantly-turning? [headings])


(defn is-orbit? [reports]
  (and (> (count reports) 5)
       (let [points (map #(spatial/spatial4j-point (:lat %) (:lon %))
                         reports)]
         (println points))))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
