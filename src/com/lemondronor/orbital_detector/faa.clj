(ns com.lemondronor.orbital-detector.faa
  ""
  (:require [clojure.string :as string]
            [net.cgrand.enlive-html :as html]))

(set! *warn-on-reflection* true)


(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))


(defn faa-lookup-url [n-number]
  (str "http://registry.faa.gov/aircraftinquiry/NNum_Results.aspx?NNumbertxt="
       n-number))


(defn lookup-registration [n-number]
  (let [result (fetch-url (faa-lookup-url n-number))
        lookup (fn [sel] (as-> result $
                           (html/select $ sel)
                           (map html/text $)
                           (string/join "" $)
                           (string/trim $)))]
    {:owner
     {:name (lookup [:span#content_lbOwnerName])
      :street (lookup [:span#content_lbOwnerStreet])
      :city (lookup [:span#content_lbOwnerCity])
      :state (lookup [:span#content_lbOwnerState])}}))
