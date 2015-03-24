(ns com.lemondronor.orbital-detector-test
  (:require [clojure.test :refer :all]
            [com.lemondronor.orbital-detector :as orbdet]))


;; (deftest parsing-tests
;;   (testing "Parsing CSV"
;;     (let [r (orbdet/parse-log-csv
;;              ["2015/03/02"
;;               "16:29:15.000"
;;               "1111111"
;;               "A8BCF0"
;;               "N662PD"
;;               "Unknown"
;;               "0"
;;               "900"
;;               "900"
;;               "33.98392"
;;               "-118.27755"
;;               "0"
;;               "0"
;;               "51.9"
;;               "332.4"
;;               "0"
;;               "0000"])]
;;       (is (= "A8BCF0" (:icao r)))
;;       (is (= "N662PD" (:registration r)))
;;       (is (= 900 (:altitude r)))
;;       (is (= {:lat 33.98392 :lon -118.27755}
;;              (:position r)))
;;       (is (= 51.9 (:speed r)))
;;       (is (= 332.4 (:heading r)))))
;;   (testing "Parsing CSV w/ whitespace"
;;     (let [r (orbdet/parse-log-csv
;;              ["2015/03/02"
;;               "16:29:15.000"
;;               "1111111"
;;               "A8BCF0"
;;               "N662PD   "
;;               "Unknown"
;;               "0"
;;               "900"
;;               "900"
;;               "33.98392"
;;               "-118.27755"
;;               "0"
;;               "0"
;;               "51.9"
;;               "332.4"
;;               "0"
;;               "0000"])]
;;       (prn r)
;;       (is (= "A8BCF0" (:icao r)))
;;       (is (= "N662PD" (:registration r)))
;;       (is (= 900 (:altitude r)))
;;       (is (= {:lat 33.98392 :lon -118.27755}
;;              (:position r)))
;;       (is (= 51.9 (:speed r)))
;;       (is (= 332.4 (:heading r))))))
