(ns airtable-clj.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [airtable-clj.util :refer [camelize-keyword
                                       parse-time
                                       make-url
                                       user-agent
                                       handle-api-error
                                       api-version]]))

(defn- format-record [record]
  {:id (record "id")
   :fields (record "fields")
   :created-time (parse-time (record "createdTime"))})

(def ^:private select-options
  (->> [:fields
        :filter-by-formula
        :max-records
        :page-size
        :view
        :cell-format
        :time-zone
        :user-locale]
       (map (fn [param] [param (camelize-keyword param)]))
       (into {})))

(defn select
  "Select records from an Airtable base."
  [{:keys [endpoint-url
           api-key
           base
           table] :as options}]
  (let [url (make-url endpoint-url base table)
        query-params (->> (select-keys options (keys select-options))
                          (map (fn [[k v]]
                                 [(k select-options)
                                  (if (keyword? v) (name v) v)]))
                          (into {}))
        headers {"X-API-Version" api-version
                 "Authorization" (str "Bearer " api-key)
                 "User-Agent" user-agent}
        http-options (cond-> {:headers headers
                              :throw-exceptions false
                              :multi-param-style :array}
                       (seq query-params) (assoc :query-params query-params))
        response (http/get url http-options)
        _ (handle-api-error response)
        body (json/parse-string (:body response))]
    {:records (map format-record (body "records"))
     :offset (body "offset")}))
