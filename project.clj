(defproject com.lemondronor/orbital-detector "0.1.0-SNAPSHOT"
  :description "Detect orbiting rotorcraft."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[clj-time "0.9.0"]
                 [com.lemondronor.leaflet-gorilla "0.1.2"]
                 [com.lemonodor/gflags "0.7.2"]
                 [factual/geo "1.0.0"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/data.csv "0.1.2"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.xerial/sqlite-jdbc "3.8.7"]
                 [postgresql "9.3-1102.jdbc41"]]
  :plugins [[lein-gorilla "0.3.4" :exclusions [cider-nrepl]]]
  :main ^:skip-aot com.lemondronor.orbital-detector
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :jvm-opts ["-server" "-Xmx2G"])
