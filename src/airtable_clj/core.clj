(ns airtable-clj.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [airtable-clj.util :refer [parse-time
                                       make-url
                                       user-agent
                                       api-version]]))

(defn- format-record [record]
  {:id (record "id")
   :fields (record "fields")
   :created-time (parse-time (record "createdTime"))})

(defn select
  "Select records from an Airtable base."
  [{:keys [endpoint-url
           api-key
           base
           table
           fields
           filter-by-formula]}]
  (let [url (make-url endpoint-url base table)
        query-params (cond-> {}
                       fields (assoc "fields" fields)
                       filter-by-formula (assoc "filterByFormula" filter-by-formula))
        headers {"X-API-Version" api-version
                 "Authorization" (str "Bearer " api-key)
                 "User-Agent" user-agent}
        http-options (cond-> {:headers headers
                              :throw-exceptions false
                              :multi-param-style :array}
                              ;; :ignore-nested-query-string true}
                       (seq query-params) (assoc :query-params query-params))
        response (http/get url http-options)
        body (json/parse-string (:body response))]
    {:records (map format-record (body "records"))
     :offset (body "offset")}))
