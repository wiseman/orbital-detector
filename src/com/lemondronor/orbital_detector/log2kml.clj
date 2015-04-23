(ns com.lemondronor.orbital-detector.log2kml
  "Converts a PlanePlotter log to KML."
  (:require [clj-time.coerce :as timecoerce]
            [clj-time.core :as time]
            [clj-time.format :as timefmt]
            [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [com.lemondronor.orbital-detector.planeplotter :as planeplotter]
            [com.lemondronor.orbital-detector.updatedb :as updatedb]
            [com.lemonodor.gflags :as gflags]
            [geo.spatial :as spatial])
  (:import ())
  (:gen-class))

(set! *warn-on-reflection* true)


(gflags/define-string "extended-data"
  nil
  "")

(gflags/define-string "session-timeout-seconds"
  (* 3 60)
  "The session timeout time in seconds.")


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
    r))


(defn agency [icao]
  (:Agency (extended-data icao)))


(defn vehicle-type [icao]
  (:Type (extended-data icao)))


(defn has-position? [r]
  (and (:lat r) (:lon r)))


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
  (str (:lon r)
       ","
       (:lat r)
       ","
       (feet-to-meters (:altitude r))))


(defn point [r]
  (spatial/spatial4j-point
   (:lat r)
   (:lon r)))


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


(defn partition-sessions [max-time-gap-s records]
  (partition-between
   (fn [r1 r2]
     (<=
      (time/in-seconds
       (time/interval (:timestamp r1) (:timestamp r2)))
      max-time-gap-s))
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
                  (for [reports vehicle-groups]
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
     (take (count tracks) path-colors))
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


(defn render-kml [kml]
  (-> kml
      xml/sexp-as-element
      xml/emit-str))



(defn write-track-kml [path track]
  (with-open [wrtr (io/writer path)]
    (binding [*out* wrtr]
      (-> (kmldoc {} (list (kmltrack 0 track)))
          render-kml
          println))))


(defn positions-for-icao [db icao]
  (let [position-reports
        (jdbc/query
         db
         [(str "SELECT * FROM reports WHERE icao = ? "
               "AND lat IS NOT NULL AND lon IS NOT NULL "
               "ORDER BY timestamp ASC")
          icao])]
    (warn "Found" (count position-reports) "position reports for" icao)
    (map #(assoc % :timestamp (timecoerce/from-sql-time (:timestamp %)))
         position-reports)))



(defn -main [& args]
  (let [args (gflags/parse-flags (cons nil args))
        db-url (first args)
        icaos (rest args)
        session-timeout-seconds (gflags/flags :session-timeout-seconds)]
    (load-extended-data)
    (let [vehicle-groups (map #(positions-for-icao db-url %) icaos)
          kml-tracks (->> vehicle-groups
                          (map #(filter-speed 30000.0 %))
                          (mapcat #(partition-sessions session-timeout-seconds %))
                          (map distinct)
                          (filter #(> (count %) 10))
                          (map-indexed kmltrack))]
      (-> (kmldoc vehicle-groups kml-tracks)
          render-kml
          println))))
