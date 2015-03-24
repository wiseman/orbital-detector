(ns com.lemondronor.orbital-detector.db
  ""
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.java.jdbc.deprecated :as jdbcdep]
   ))

(set! *warn-on-reflection* true)


(defn query-seq1 [db query]
  (jdbc/query db query))


(defn query-seq2 [db query]
  (let [db-con (doto (jdbc/get-connection db)
                 (.setAutoCommit false))]
    (println db-con)
    (let [stmt (jdbc/prepare-statement
                db-con
                query
                :fetch-size 1000
                :concurrency :read-only
                :result-type :forward-only)]
    (jdbc/query
     db-con
     [stmt]
     :as-arrays? true))))


(defn mycnt [s]
  (reduce (fn [c e] (inc c)) 0 s))


(defn query-seq3 [db query]
  (let [db-con (doto (jdbc/get-connection db)
                 (.setAutoCommit false))]
    (println db-con)
    (let [stmt (jdbc/prepare-statement
                db-con
                query
                :fetch-size 1000
                :concurrency :read-only
                :result-type :forward-only)]
      (jdbcdep/with-query-results results [stmt]
        (println (mycnt results))))))


;; (def records (com.lemondronor.orbital-detector.db/query-seq
;;                     {:classname   "org.sqlite.JDBC"
;;                      :subprotocol "sqlite"
;;                      :subname     "pings.sqb"}
;;                     "select * from reports"))
