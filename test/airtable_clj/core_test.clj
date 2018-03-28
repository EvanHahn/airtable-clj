(ns airtable-clj.core-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [airtable-clj.test-helpers :refer [mock-server take-mock-request]]
            [airtable-clj.core :as airtable]
            [airtable-clj.util :refer [handle-api-error]]))

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

(def can-integration-test?
  (every? #(contains? env %) [:airtable-api-key
                              :airtable-base
                              :airtable-table
                              :airtable-record-id]))

(deftest select-unit-test
  (testing "simple record selection"
    (let [responses [{:body {:records [fake-record-response]
                             :offset "rec456"}}]
          server (mock-server responses)
          result (airtable/select {:endpoint-url (:url server)
                                   :api-key "abc123"
                                   :base "base123"
                                   :table "My Table"})
          request (take-mock-request server)
          headers (:headers request)]
      (is (= "/v0/base123/My%20Table" (:uri request)))
      (is (= {} (:query-params request)))
      (is (= :get (:request-method request)))
      (is (= expected-user-agent (headers "user-agent")))
      (is (= "0.1.0" (headers "x-api-version")))
      (is (= "rec456" (:offset result)))
      (is (= (list fake-record) (:records result)))))
  (testing "selecting no records"
    (let [server (mock-server [{:body {:records []}}])
          result (airtable/select {:endpoint-url (:url server)
                                   :api-key "abc123"
                                   :base "base123"
                                   :table "My Table"})]
      (is (nil? (:offset result)))
      (is (empty? (:records result)))))
  (testing "adding additional options"
    (let [server (mock-server [{:body {:records []}}])
          _ (airtable/select {:endpoint-url (:url server)
                              :api-key "abc123"
                              :base "base123"
                              :table "My Table"
                              :fields ["Foo Boo" "Bing Bong"]
                              :filter-by-formula "NOT({X} = 2)"
                              :max-records 42
                              :page-size 69
                              ;; TODO
                              ;; :sort [{:field "Foo Boo"
                              ;;         :direction :desc}]
                              :view "My View"
                              :cell-format :json
                              :time-zone "Africa/Algiers"
                              :user-locale "bo"
                              :ignored-param "should not be included"})
          request (take-mock-request server)]
      (is (= "/v0/base123/My%20Table" (:uri request)))
      (is (= {"fields[]" ["Bing+Bong" "Foo+Boo"]
              "filterByFormula" ["NOT%28%7BX%7D+%3D+2%29"]
              "maxRecords" ["42"]
              "pageSize" ["69"]
              "view" ["My+View"]
              "cellFormat" ["json"]
              "timeZone" ["Africa%2FAlgiers"]
              "userLocale" ["bo"]} (:query-params request)))))
  (testing "calls out to handle-api-error"
    (let [server (mock-server [{:body {:records []}}])
          handle-api-error-arg (atom nil)]
      (with-redefs [handle-api-error #(reset! handle-api-error-arg %)]
        (airtable/select {:endpoint-url (:url server)
                          :api-key "abc123"
                          :base "base123"
                          :table "My Table"})
        (is (some? (:status @handle-api-error-arg)))))))

(deftest select-integration-test
  (when can-integration-test?
    (testing "simple record selection"
      (let [result (airtable/select {:api-key (:airtable-api-key env)
                                     :base (:airtable-base env)
                                     :table (:airtable-table env)})
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
                "Formula" 13} (last record-fields)))))))

(deftest retrieve-unit-test
  (testing "retrieving a single record"
    (let [server (mock-server [{:body fake-record-response}])
          result (airtable/retrieve {:endpoint-url (:url server)
                                     :api-key "abc123"
                                     :base "base123"
                                     :table "My Table"
                                     :record-id "rec123"})
          request (take-mock-request server)
          headers (:headers request)]
      (is (= "/v0/base123/My%20Table/rec123" (:uri request)))
      (is (= {} (:query-params request)))
      (is (= :get (:request-method request)))
      (is (= expected-user-agent (headers "user-agent")))
      (is (= "0.1.0" (headers "x-api-version")))
      (is (= fake-record result))))
  (testing "getting a 404 should give nil"
    (let [server (mock-server [{:status 404
                                :body {:error "NOT_FOUND"}}])
          result (airtable/retrieve {:endpoint-url (:url server)
                                     :api-key "abc123"
                                     :base "base123"
                                     :table "My Table"
                                     :record-id "rec123"})]
      (is (nil? result))))
  (testing "calls out to handle-api-error"
    (let [server (mock-server [{:body fake-record-response}])
          handle-api-error-arg (atom nil)]
      (with-redefs [handle-api-error #(reset! handle-api-error-arg %)]
        (airtable/retrieve {:endpoint-url (:url server)
                            :api-key "abc123"
                            :base "base123"
                            :table "My Table"
                            :record-id "rec123"})
        (is (some? (:status @handle-api-error-arg)))))))

(deftest retrieve-integration-test
  (when can-integration-test?
    (testing "retrieving a single record"
      (let [record-id (:airtable-record-id env)
            result (airtable/retrieve {:api-key (:airtable-api-key env)
                                       :base (:airtable-base env)
                                       :table (:airtable-table env)
                                       :record-id record-id})]
        (is (= record-id (:id result)))
        (is (map? (:fields result)))
        (is (integer? (:created-time result)))))
    (testing "retrieving nothing"
      (let [result (airtable/retrieve {:api-key (:airtable-api-key env)
                                       :base (:airtable-base env)
                                       :table (:airtable-table env)
                                       :record-id "recBogus"})]
        (is (nil? result))))))
