(ns com.lemondronor.orbital-detector.log2kml
  "Converts a PlanePlotter log to KML."
  (:require [com.lemondronor.orbital-detector :as orbdet]
            [clojure.data.xml :as xml]
            [clojure.string :as string])
  (:import ())
  (:gen-class))

(set! *warn-on-reflection* true)


(defn has-position? [r]
  (and (>= (Math/abs (:lat (:position r))) 0.1)
       (>= (Math/abs (:lon (:position r))) 0.1)))


(defn random-rgb []
  (map #(rand-int %) (repeat 3 255)))

(defn rgb-hex [rgb]
  (str "7f" (string/join (map #(format "%02x" %) rgb))))

(defn rgb-hex [rgb]
  (str "d0" (format "%06x" rgb)))


(def path-colors
  [
   0xFF0000
   0x00FF00
   0x0000FF
   0xF15854 ;; (red))
   0xB276B2 ;; (purple)
   0xDECF3F ;; (yellow)
   0x4D4D4D ;; (gray)
   0x5DA5DA ;; (blue)
   0xFAA43A ;; (orange)
   0x60BD68 ;; (green)
   0xF17CB0 ;; (pink)
   0xB2912F ;; (brown)
   ])


(defn reports2path [rs]
  (let [groups (group-by :icao (filter has-position? rs))
        colors (take (count groups) path-colors)]
    (xml/sexp-as-element
     [:kml {:xmlns "http://www.opengis.net/kml/2.2"}
      [:Document
       [:name "Recent law enforcement aircraft tracks"]
       (map-indexed
        (fn [idx color]
          [:Style {:id (str "color" idx)}
           [:LineStyle
            [:color (rgb-hex color)]
            [:width 4]]
           [:PolyStyle
            [:color "7f00ff00"]]])
        colors)
       (map-indexed
        (fn [idx [icao rs]]
          [:Placemark
           [:name (str icao " - " (:registration (first rs)))]
           [:styleUrl (str "#color" idx)]
           [:LineString
            [:tessellate 1]
            [:altitudeMode "relativeToGround"]
            [:coordinates
             (string/join
              "\n"
              (map #(str (:lon (:position %))
                         ","
                         (:lat (:position %))
                         ","
                         (:altitude %))
                   rs))]]])
        groups)]])))


(defn pv [x]
  (println x)
  x)


(defn -main [& args]
  (-> args
      first
      orbdet/read-log
      reports2path
      xml/emit-str
      println))
