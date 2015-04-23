(ns com.lemondronor.orbital-detector.geo
  ""
  (:require [geo.spatial :as spatial]))

(set! *warn-on-reflection* true)


(defn bearing [lat1 lon1 lat2 lon2]
  (Math/atan2
   (- (* (Math/cos lat1) (Math/cos lat2))
      (* (Math/sin lat1) (Math/cos lat2) (Math/cos (- lon2 lon1))))
   (* (Math/sin (- lon2 lon1)) (Math/cos lat2))))


(defn point-bearing [a b]
  (let [lat1 (spatial/latitude a)
        lon1 (spatial/longitude a)
        lat2 (spatial/latitude b)
        lon2 (spatial/longitude b)]
    (bearing lat1 lon1 lat2 lon2)))


(defn normalize-bearing [b]
  (cond
    (> b 180.0)
    (normalize-bearing (- b 360.0))
    (< b -180.0)
    (normalize-bearing (+ b 360.0))
    :else b))


(defn point [lat lon]
  (spatial/spatial4j-point lat lon))


(defn rad2deg [r]
  (* r (/ 180.0 Math/PI)))
