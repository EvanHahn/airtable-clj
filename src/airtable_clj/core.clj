(ns airtable-clj.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json])
  (:import [java.net URL]
           [java.text SimpleDateFormat]))

(def ^:private api-version "0.1.0")

(def ^:private date-format
  (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))

(defn- make-url [endpoint-url base table]
  (let [base-url (URL. endpoint-url)
        protocol (.getProtocol base-url)
        host (.getHost base-url)
        port (.getPort base-url)
        path (str "/" base "/" table)]
    (-> (URL. protocol host port path) str)))

(defn- format-record [record]
  {:id (record "id")
   :fields (record "fields")
   :created-time (->> (record "createdTime") (.parse date-format) .getTime)})

(defn select
  "Select records from an Airtable base."
  [options]
  (let [{:keys [endpoint-url api-key base table]} options
        url (make-url endpoint-url base table)
        headers {"X-API-Version" api-version
                 "Authorization" (str "Bearer " api-key)}
        response (http/get url {:headers headers})
        body (json/parse-string (:body response))]
    {:records (map format-record (body "records"))
     :offset (body "offset")}))
