(ns airtable-clj.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [airtable-clj.util :refer [camelize-keyword
                                       parse-time
                                       make-url
                                       headers
                                       handle-api-error]]))

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
  "Select records from an base."
  [{:keys [api-key] :as options}]
  (let [url (make-url options)
        query-params (->> (select-keys options (keys select-options))
                          (map (fn [[k v]]
                                 [(k select-options)
                                  (if (keyword? v) (name v) v)]))
                          (into {}))
        http-options (cond-> {:headers (headers api-key)
                              :throw-exceptions false
                              :multi-param-style :array}
                       (seq query-params) (assoc :query-params query-params))
        response (http/get url http-options)
        _ (handle-api-error response)
        body (json/parse-string (:body response))]
    {:records (map format-record (body "records"))
     :offset (body "offset")}))

(defn retrieve
  "Retrieve a single record from a base."
  [{:keys [api-key record-id] :as options}]
  (let [url (str (make-url options) "/" record-id)
        response (http/get url {:headers (headers api-key)
                                :throw-exceptions false})]
    (when-not (= 404 (:status response))
      (handle-api-error response)
      (-> response :body json/parse-string format-record))))
