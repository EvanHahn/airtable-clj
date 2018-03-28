(ns airtable-clj.core-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [airtable-clj.test-helpers :refer [mock-server take-mock-request]]
            [airtable-clj.core :as airtable]))

(def fake-record-response
  {"id" "rec123"
   "fields" {"foo" "boo"
             "friend" "rec123"
             "bar" 123}
   "createdTime" "2018-04-20T16:20:00.000Z"})

(def fake-record
  {:id (fake-record-response "id")
   :fields (fake-record-response "fields")
   :created-time 1524241200000})

(def expected-user-agent (str "airtable-clj/" (env :airtable-clj-version)))

(deftest select-unit-test
  (testing "simple record selection"
    (let [responses [{:status 200
                      :body {:records [fake-record-response]
                             :offset "rec456"}}]
          server (mock-server responses)
          result (airtable/select {:endpoint-url (:url server)
                                   :api-key "abc123"
                                   :base "base123"
                                   :table "My Table"})
          request (take-mock-request server)
          headers (:headers request)]
      (is (= "/v0/base123/My%20Table" (:path request)))
      (is (= :get (:request-method request)))
      (is (= expected-user-agent (headers "user-agent")))
      (is (= "0.1.0" (headers "x-api-version")))
      (is (= "rec456" (:offset result)))
      (is (= (list fake-record) (:records result))))))

(deftest select-integration-test
  (testing "simple record selection"
    (let [options {:api-key (env :airtable-api-key)
                   :base (env :airtable-base)
                   :table (env :airtable-table)}
          can-integration-test? (every? some? (vals options))]
      (when can-integration-test?
        (let [result (airtable/select options)
              records (:records result)
              relevant-keys ["Primary" "Single" "Formula" "Checkbox"]
              record-fields (->> records
                                 (map :fields)
                                 (sort-by #(% "Primary"))
                                 (map #(select-keys % relevant-keys)))]
          (is (nil? (:offset result)))
          (is (= 3 (count records)))
          (is (= {"Primary" 1
                  "Single" "Foo"
                  "Formula" 11} (first record-fields)))
          (is (= {"Primary" 2
                  "Single" "Bar"
                  "Formula" 12
                  "Checkbox" true} (second record-fields)))
          (is (= {"Primary" 3
                  "Formula" 13} (last record-fields)))
          )))))
