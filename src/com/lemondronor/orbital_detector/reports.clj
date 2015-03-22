(ns com.lemondronor.orbital-detector.reports
  ""
  (:require [clj-time.coerce :as timecoerce]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [com.lemondronor.orbital-detector.planeplotter :as orbdet]))

(set! *warn-on-reflection* true)


(defn db-spec [path]
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     path})


(def db-schema
  {:reports
   {:timestamp :integer
    :icao :text
    :registration :text
    :type :text
    :altitude :integer
    :lat :real
    :lon :real
    :speed :real
    :heading :real}})


(defn- ddl-clause [table-name schema]
  (let [nm (fn [s] (if (or (keyword? s) (symbol? s)) (name s) s))]
    (str
     (nm table-name)
     " ("
     (string/join
      ", "
      (for [[column type] schema] (str (nm column) " " (nm type))))
     ")")))


(defn- create-db! [db db-schema]
  (doseq [[table-name schema] db-schema]
    (jdbc/db-do-commands
     db
     (str "CREATE TABLE IF NOT EIXSTS "
          (ddl-clause table-name schema)))))


(defn add-records! [db-path log]
  (jdbc/with-db-transaction [t-con (db-spec db-path)]
    (doseq [r log]
      (jdbc/insert!
       t-con
       :reports
       {:timestamp (long (/ (timecoerce/to-long (:timestamp r)) 1000))
        :icao (:icao r)
        :registration (:registration r)
        :altitude (:altitude r)
        :lat (:lat (:position r))
        :lon (:lon (:position r))
        :speed (:speed r)
        :heading (:heading r)}))))



(defn -main [& args]
  (let [db-path (first args)
        log-path (second args)]
    (create-db! (db-spec db-path) db-schema)
    (add-records! db-path (orbdet/read-log log-path))))
