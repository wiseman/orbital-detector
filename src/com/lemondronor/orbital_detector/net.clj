(ns com.lemondronor.orbital-detector.net
  ""
  (:import (java.io BufferedReader InputStreamReader)))

 (set! *warn-on-reflection* true)

(defn fetch-url [url]
  (with-open [stream (.openStream (java.net.URL. url))]
    (let [buf (java.io.BufferedReader.
               (java.io.InputStreamReader. stream))]
      (slurp buf))))
