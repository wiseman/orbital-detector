(ns com.lemondronor.orbital-detector
  (:require [com.lemondronor.orbital-detector.geo :as geo]
            [geo.spatial :as spatial])
  (:gen-class))

(set! *warn-on-reflection* true)


(defn segments [reports]
  (partition 2 1 reports))


(defn turns [segments]
  (partition 2 1 segments))

(defn turns-to-reports [turns]
  ()


(defn segment-bearing [[a b]]
  (geo/bearing
   (:lat (:position a))
   (:lon (:position a))
   (:lat (:position b))
   (:lon (:position b))))


(defn turn-angle [[segment1 segment2]]
  (geo/normalize-bearing
   (- (segment-bearing segment1)
      (segment-bearing segment2))))


(defn turn-direction [a b c]
  (let [delta (bearing-delta a b c)]
    (cond (neg? bearing-delta)
          :counter-clockwise
          (pos? bearing-delta)
          :clockwise
          :else nil)))


(defn longest-subseq [f coll]
  (let [f (memoize f)
        a (partition-by #(apply f %) (partition 2 1 coll))
        ;;_ (println a)
        b (filter (fn [[[x1 x2]]] (f x1 x2)) a)
        ;;_ (println b)
        c (first (sort-by count > b))
        ;;_ (println c)
        ]
    (concat (first c) (map last (rest c)))))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
