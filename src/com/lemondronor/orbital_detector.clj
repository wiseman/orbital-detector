(ns com.lemondronor.orbital-detector
  (:require [geo.spatial :as spatial])
  (:gen-class))

(set! *warn-on-reflection* true)


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
