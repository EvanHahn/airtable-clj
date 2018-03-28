(ns airtable-clj.core-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as json]
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
          request (take-mock-request server)]
      ; TODO: assertions on request
      (is (= "rec456" (:offset result)))
      (is (= [fake-record] (:records result))))))
