(ns com.lemondronor.orbital-detector.geo
  ""
  (:require [geo.spatial :as spatial]))

(set! *warn-on-reflection* true)


(defn bearing [a b]
  (let [lat1 (spatial/latitude a)
        lon1 (spatial/longitude a)
        lat2 (spatial/latitude b)
        lon2 (spatial/longitude b)]
    (Math/atan2
     (- (* (Math/cos lat1) (Math/cos lat2))
        (* (Math/sin lat1) (Math/cos lat2) (Math/cos (- lon2 lon1))))
     (* (Math/sin (- lon2 lon1)) (Math/cos lat2)))))


(defn normalize-bearing [b]
  (cond
    (> b 180.0)
    (normalize-bearing (- b 360.0))
    (< b -180.0)
    (normalize-bearing (+ b 360.0))
    :else b))
