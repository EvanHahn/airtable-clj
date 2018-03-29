(ns airtable-clj.test-helpers
  (:require [clojure.string :as string]
            [environ.core :refer [env]]
            [cheshire.core :as json])
  (:import [okhttp3.mockwebserver MockWebServer MockResponse]
           [java.net URL]
           [java.util.logging Logger Level]
           [java.util.concurrent TimeUnit]))

(def can-integration-test?
  (every? #(contains? env %) [:airtable-api-key
                              :airtable-base
                              :airtable-table
                              :airtable-record-id]))

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
  (if-not mock-request
    (throw (ex-info "No request to take from mock server" {})))
  (let [url (URL. "http" "example.com" -1 (.getPath mock-request))]
    {:uri (.getPath url)
     :path (.getFile url)
     :query-params (if-let [query (.getQuery url)]
                     (->> (string/split query #"&")
                          (map #(string/split % #"="))
                          (reduce (fn [params [k v]] (update params k conj v)) {})
                          (map (fn [[k v]] [k (sort v)]))
                          (into {}))
                     {})
     :request-method (-> (.getMethod mock-request) string/lower-case keyword)
     :headers (->> (.getHeaders mock-request)
                   .toMultimap
                   (map (fn [[k v]] [k (first v)]))
                   (into {}))
     :body (-> (.getBody mock-request) .readUtf8 json/parse-string)}))

(defn mock-server [responses]
  (disable-mockwebserver-logger)
  (let [server (MockWebServer.)
        url (str (.url server ""))
        mock-responses (map ring-to-mock-response responses)]
    (doseq [response mock-responses] (.enqueue server response))
    {:url url, :server server}))

(defn take-mock-request [{:keys [server]}]
  (mock-request-to-map (.takeRequest server 2 TimeUnit/SECONDS)))
