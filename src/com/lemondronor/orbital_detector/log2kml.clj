(ns com.lemondronor.orbital-detector.log2kml
  "Converts a PlanePlotter log to KML."
  (:require
   [clj-time.coerce :as timecoerce]
   [clj-time.core :as time]
   [clj-time.format :as timefmt]
   [clojure.pprint :as pprint]
   [com.lemondronor.orbital-detector :as orbdet]
   [com.lemondronor.orbital-detector.updatedb :as updatedb]
   [com.lemonodor.gflags :as gflags]
   [clojure.data.xml :as xml]
   [clojure.string :as string]
   [geo.spatial :as spatial])
  (:import
   ())
  (:gen-class))

(set! *warn-on-reflection* true)


(gflags/define-list "icaos"
  []
  "Comma separated list of ICAOs to filter.")


(gflags/define-string "extended-data"
  nil
  "")


(defn warn [& args]
  (binding [*out* *err*]
    (apply println args)))


(def extended-data nil)


(defn load-extended-data []
  (alter-var-root
   (var extended-data)
   (fn [_]
     (if-let [path (gflags/flags :extended-data)]
       (into
        {}
        (map (fn [r] [(:ICAO r) r]) (updatedb/read-csv path)))
       {}))))


(defn registration [icao]
  (let [r (:Registration (extended-data icao))]
    (warn "Registration of" (pr-str icao) "is" r)
    r))


(defn agency [icao]
  (:Agency (extended-data icao)))


(defn vehicle-type [icao]
  (:Type (extended-data icao)))


(defn has-position? [r]
  (and (>= (Math/abs ^double (:lat (:position r))) 0.1)
       (>= (Math/abs ^double (:lon (:position r))) 0.1)))


(defn rgb-hex [rgb]
  (str "7f" (string/join (map #(format "%02x" %) rgb))))

(defn rgb-hex [rgb]
  (str "d0" (format "%06x" rgb)))


(def path-colors
  (cycle
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
    ]))


(defn feet-to-meters [f]
  (* f 0.3048))


(defn coordinate [r]
  (str (:lon (:position r))
       ","
       (:lat (:position r))
       ","
       (feet-to-meters (:altitude r))))


(defn point [r]
  (spatial/spatial4j-point
   (:lat (:position r))
   (:lon (:position r))))


(defn distance [r1 r2]
  (spatial/distance (point r1) (point r2)))


(defn filter-speed [max-speed records]
  (reverse
   (reduce
    (fn [reasonable r]
      (let [d (distance r (first reasonable))]
        (if (< d max-speed)
          (cons r reasonable)
          (do
            (warn "distance from" (first reasonable) "is" d "dropping" r)
            reasonable))))
    (list (first records))
    (rest records))))


(defn partition-between
  "Splits coll into a lazy sequence of lists, with partition
  boundaries between items where (f item1 item2) is true.
  (partition-between = '(1 2 2 3 4 4 4 5)) =>
  ((1 2) (2 3 4) (4) (4 5))"
  [f coll]
  (lazy-seq
   (when-let [s (seq coll)]
     (let [fst (first s)]
       (if-let [rest-seq (next s)]
         (if (f fst (first rest-seq))
           (cons (list fst) (partition-between f rest-seq))
           (let [rest-part (partition-between f rest-seq)]
             (cons (cons fst (first rest-part)) (rest rest-part))))
         (list (list fst)))))))


(defn partition-between
  [f coll]
  (lazy-seq
   (when-let [s (seq coll)]
     (let [pairs-in-segment (take-while (fn [[a b]] (f a b)) (partition 2 1 s))
           [segment reminder] (split-at (inc (count pairs-in-segment)) s)]
       (cons segment
             (partition-between f reminder))))))


(defn partition-sessions [max-time-gap-ms records]
  (partition-between
   (fn [r1 r2]
     (<= (- (:timestamp r2) (:timestamp r1)) max-time-gap-ms))
   records))


(defn kmldoc [vehicle-groups tracks]
  [:kml {:xmlns "http://www.opengis.net/kml/2.2"}
   [:Document
    [:name "Recent law enforcement aircraft tracks"]
    [:description
     (str
      (count vehicle-groups) " vehicles, "
      (count tracks) " sessions, "
      (format "%.1f km flown"
              (/ (reduce
                  +
                  (for [[_ reports] vehicle-groups]
                    (reduce + (map distance reports (next reports)))))
                 1000.0)))]
    (map-indexed
     (fn [idx color]
       [:Style {:id (str "color" idx)}
        [:LineStyle
         [:color (rgb-hex color)]
         [:width 4]]
        [:PolyStyle
         [:color "7f00ff00"]]])
     (take 100 path-colors))
    tracks]])



(def datetime-fmt (timefmt/formatter-local "YYYY/MM/dd HH:mm"))


(defn track-name [reports]
  (let [r (first reports)]
    (string/join
     " "
     [(or (:registration r) (registration (:icao r)) "Unknown")
      "-"
      (:icao r)
      "|"
      (timefmt/unparse
       datetime-fmt
       (timecoerce/from-long (:timestamp r)))])))


(defn track-description [reports]
  (let [r (first reports)]
    (string/join
     "\n"
     [(if-let [agency (agency (:icao r))]
        (str "Agency: " agency)
        "")
      (if-let [vtype (vehicle-type (:icao r))]
        (str "Type: " vtype)
        "")
      (str "Duration: "
           (time/in-minutes
            (time/interval
             (timecoerce/from-long (:timestamp r))
             (timecoerce/from-long (:timestamp (last reports)))))
           " minutes")

      (str
       "Track length: "
       (format "%.1f"
               (/ (reduce + (map distance reports (next reports))) 1000.0))
       " km")])))


(defn kmltrack [idx reports]
  (let [icao (:icao (first reports))
        registration (:registration (first reports))]
    [:Placemark
     [:name (track-name reports)]
     [:description (track-description reports)]
     [:styleUrl (str "#color" idx)]
     [:LineString
      [:tessellate 1]
      [:altitudeMode "relativeToGround"]
      [:coordinates
       (string/join
        "\n"
        (distinct (map coordinate reports)))]]]))


(defn filter-icaos [icaos records]
  (if (seq icaos)
    (let [icaos (set icaos)]
      (filter #(icaos (:icao %)) records))
    records))


(defn print-count [s]
  (warn "sequence has" (count s) "items")
  s)


(defn -main [& args]
  (let [args (gflags/parse-flags (cons nil args))]
    (load-extended-data)
    (let [vehicle-groups (->> args
                              (mapcat orbdet/read-log)
                              (filter-icaos (gflags/flags :icaos))
                              (filter has-position?)
                              (print-count)
                              (partition-all 1000)
                              (mapcat distinct)
                              (print-count)
                              (group-by :icao))
          kml-tracks (->> vehicle-groups
                          (map second)
                          (map #(filter-speed 30000.0 %))
                          (mapcat #(partition-sessions 600000 %))
                          (map distinct)
                          (filter #(> (count %) 10))
                          (map-indexed kmltrack))]
      (-> (kmldoc vehicle-groups kml-tracks)
          xml/sexp-as-element
          xml/emit-str
          println))))
