(ns com.lemondronor.orbital-detector
  (require [clj-time.format :as timefmt]
           [clojure.data.csv :as csv]
           [clojure.string :as string]
           [geo.poly :as poly]
           [geo.spatial :as spatial])
  (import (org.joda.time.base BaseDateTime))
  (:gen-class)
  (:require [clojure.java.io :as io]))


(def datetime-fmt (timefmt/formatter-local "YYYY/MM/dd HH:mm:ss.SSS"))


(defn parse-log-csv [csv]
  {:timestamp (.getMillis
               ^BaseDateTime
               (timefmt/parse
                datetime-fmt (string/join " " [(csv 0) (csv 1)])))
   :icao (csv 3)
   :registration (string/trim (csv 4))
   :altitude (Long/parseLong (csv 7))
   :position {:lat (Double/parseDouble (csv 9))
              :lon (Double/parseDouble (csv 10))}
   :speed (Double/parseDouble (csv 13))
   :heading (Double/parseDouble (csv 14))})


(defn read-log [log]
  (->> log
       io/reader
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
