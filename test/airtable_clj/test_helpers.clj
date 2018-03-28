(ns airtable-clj.test-helpers
  (:require [clojure.string :as string]
            [cheshire.core :as json])
  (:import [okhttp3.mockwebserver MockWebServer MockResponse]
           [java.util.logging Logger Level]))

(defn- disable-mockwebserver-logger []
  (let [logger-name (.getName MockWebServer)
        logger (Logger/getLogger logger-name)]
    (.setLevel logger Level/WARNING)))

(defn- ring-to-mock-response [{:keys [status body headers]}]
  (let [result (MockResponse.)
        headers (merge {"Content-Type" "application/json"} headers)]
    (if status (.setResponseCode result status))
    (doseq [[header-key header-value] headers]
      (.setHeader result header-key header-value))
    (.setBody result (json/generate-string body))
    result))

(defn- mock-request-to-map [mock-request]
  {:path (.getPath mock-request)
   :request-method (-> (.getMethod mock-request) string/lower-case keyword)
   :headers (-> (.getHeaders mock-request) .toMultimap)
   })
   ;; :body (-> (.getBody mock-request) slurp json/parse-string)})

(defn mock-server [responses]
  (disable-mockwebserver-logger)
  (let [server (MockWebServer.)
        url (str (.url server ""))
        mock-responses (map ring-to-mock-response responses)]
    (doseq [response mock-responses] (.enqueue server response))
    {:url url, :server server}))

(defn take-mock-request [{:keys [server]}]
  (mock-request-to-map (.takeRequest server)))
