(ns airtable-clj.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [airtable-clj.util :as util]))

(defn- format-record [record]
  {:id (record "id")
   :fields (record "fields")
   :created-time (util/parse-time (record "createdTime"))})

(defn select
  "Select records from an Airtable base."
  [options]
  (let [{:keys [endpoint-url api-key base table]} options
        url (util/make-url endpoint-url base table)
        headers {"X-API-Version" util/api-version
                 "Authorization" (str "Bearer " api-key)
                 "User-Agent" util/user-agent}
        response (http/get url {:headers headers
                                :throw-exceptions false})
        body (json/parse-string (:body response))]
    {:records (map format-record (body "records"))
     :offset (body "offset")}))
