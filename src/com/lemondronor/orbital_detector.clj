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
  ())


(defn segment-bearing [a b]
  (geo/bearing
   (:lat (:position a))
   (:lon (:position a))
   (:lat (:position b))
   (:lon (:position b))))


(defn turn-angle [a b c]
  (geo/normalize-bearing
   (- (segment-bearing a b)
      (segment-bearing b c))))


(defn total-track-angle [track]
  (reduce + (map #(apply turn-angle %) (partition 3 1 track))))


(defn subseqs [f coll]
  (let [f (memoize f)
        a (partition-by #(apply f %) (partition 2 1 coll))
        ;;_ (println a)
        b (filter (fn [[[x1 x2]]] (f x1 x2)) a)
        ]
    (map #(concat (first %) (map last (rest %))) b)))


(defn orbits [track]
  (let [turning-left? (memoize
                       (fn [[a b] [c d]]
                         (and (= b c)
                              (let [angle (turn-angle a b d)]
                                (< angle 0.001)))))
        segments (partition 2 1 track)
        _ (println (count segments) "segments")
        potential-orbits (map #(concat (first %) (map last (rest %)))
                              (subseqs turning-left? segments))
        _ (println (count potential-orbits) "potential orbits")
        _ (println (sort < (map #(* (/ 180.0 Math/PI) (total-track-angle %)) potential-orbits)))]
    (println (first potential-orbits))
    (filter #(< (total-track-angle %) (* Math/PI -2.0)) potential-orbits)))



;; (defn turn-direction [a b c]
;;   (let [delta (bearing-delta a b c)]
;;     (cond (neg? bearing-delta)
;;           :counter-clockwise
;;           (pos? bearing-delta)
;;           :clockwise
;;           :else nil)))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
