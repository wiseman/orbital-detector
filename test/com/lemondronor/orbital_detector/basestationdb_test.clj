(ns com.lemondronor.orbital-detector.basestationdb-test
  (:require [com.lemondronor.orbital-detector.basestationdb :as basestationdb]
            [clojure.test :refer :all]))


(def N520PD
  {:usertag "Burbank and Glendale Police Joint Air Support Unit",
   :modes "A68A37",
   :interested 1,
   :icaotypecode "H500",
   :modescountry "United States",
   :registration "N520PD"})

(def N521PD
  {:usertag "Hawthorne Police",
   :modes "A68DEE",
   :interested 1,
   :icaotypecode "H500",
   :operatorflagcode nil,
   :modescountry "United States",
   :registration "N521PD"})


(defn records-equiv? [a b]
  (doseq [[k v] a]
    (assert (= (b k) v) (str k " of " b " was " (b k) ", not " v)))
  true)


(deftest find-aircraft-test
  (testing "find N520PD in basestation-1.sqb"
    (is (records-equiv? N521PD
                        (basestationdb/find-aircraft
                         (basestationdb/db-spec "dev-resources/basestation-1.sqb")
                         "A68DEE"))))
  (testing "find N521PD in basestation-1.sqb"
    (is (records-equiv? N520PD
                        (basestationdb/find-aircraft
                         (basestationdb/db-spec "dev-resources/basestation-1.sqb")
                         "A68A37"))))
  (testing "find N520PD in basestation-planebase.sqb"
    (is (records-equiv? N521PD
                        (basestationdb/find-aircraft
                         (basestationdb/db-spec "dev-resources/basestation-planebase.sqb")
                         "A68DEE"))))
  (testing "find on non-existent aircraft"
    (is (= nil
           (basestationdb/find-aircraft
            (basestationdb/db-spec "dev-resources/basestation-1.sqb")
            "XXXXXX")))))


(deftest load-db-test
  (let [recs (basestationdb/load-db "dev-resources/basestation-1.sqb")]
    (is (= 2 (count recs)))
    (is (= #{"A68DEE" "A68A37"} (set (map :modes recs))))))
