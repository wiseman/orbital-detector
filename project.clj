(defproject com.lemondronor/orbital-detector "0.1.0-SNAPSHOT"
  :description "Detect orbiting rotorcraft."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[clj-time "0.12.2"]
                 [com.lemondronor.leaflet-gorilla "0.1.3"]
                 [com.lemonodor/gflags "0.7.3"]
                 [enlive "1.1.6"]
                 [factual/geo "1.0.0"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.postgis/postgis-jdbc "1.3.3" :exclusions [postgresql/postgresql]]
                 [org.xerial/sqlite-jdbc "3.8.11.2"]
                 [com.lemondronor/modesbeast "0.0.2"]
                 [org.clojure/core.async "0.2.374"]
                 [postgresql "9.3-1102.jdbc41"]]
  :plugins [[lein-gorilla "0.3.5-SNAPSHOT" :exclusions [cider/cider-nrepl]]]
  :main ^:skip-aot com.lemondronor.orbital-detector
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :jvm-opts ["-server" "-Xmx1G"])
