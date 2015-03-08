(ns com.lemondronor.orbital-detector.log2kml
  "Converts a PlanePlotter log to KML."
  (:require [com.lemondronor.orbital-detector :as orbdet]
            [com.lemonodor.gflags :as gflags]
            [clojure.data.xml :as xml]
            [clojure.string :as string])
  (:import ())
  (:gen-class))

(set! *warn-on-reflection* true)


(gflags/define-list "icaos"
  []
  "Comma separated list of ICAOs to filter.")


(defn has-position? [r]
  (and (>= (Math/abs ^double (:lat (:position r))) 0.1)
       (>= (Math/abs ^double (:lon (:position r))) 0.1)))


(defn random-rgb []
  (map #(rand-int %) (repeat 3 255)))

(defn rgb-hex [rgb]
  (str "7f" (string/join (map #(format "%02x" %) rgb))))

(defn rgb-hex [rgb]
  (str "d0" (format "%06x" rgb)))


(def path-colors
  [
   0xA6CEE3
   0x1F78B4
   0xB2DF8A
   0x33A02C
   0xFB9A99
   0xE31A1C
   0xFDBF6F
   0xFF7F00
   0xCAB2D6
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


(defn feet-to-meters [f]
  (* f 0.3048))


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
                         (feet-to-meters (:altitude %)))
                   rs))]]])
        groups)]])))


(defn filter-icaos [icaos records]
  (if (seq icaos)
    (let [icaos (set icaos)]
      (filter #(icaos (:icao %)) records))
    records))


(defn pv [msg s]
  (println msg (take 3 s))
  s)


(defn -main [& args]
  (let [args (gflags/parse-flags (cons nil args))]
    (->> args
         (mapcat orbdet/read-log)
         (filter-icaos (gflags/flags :icaos))
         reports2path
         xml/emit-str
         println)))
