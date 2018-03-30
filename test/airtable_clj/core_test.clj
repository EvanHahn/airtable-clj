(ns airtable-clj.core-test
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [airtable-clj.test-helpers :refer [mock-server take-mock-request]]
            [airtable-clj.core :as airtable]
            [airtable-clj.util :refer [handle-api-error]]))

(def ^:private fake-record-response
  {"id" "rec123"
   "fields" {"foo" "boo"
             "friend" "rec123"
             "bar" 123}
   "createdTime" "2018-04-20T16:20:00.000Z"})

(def ^:private fake-record
  {:id (fake-record-response "id")
   :fields (fake-record-response "fields")
   :created-time 1524241200000})

(def ^:private fake-404-response
  {:status 404
   :body {"error" "NOT_FOUND"}})

(def ^:private fake-record-404-response
  {:status 404
   :body {"error" {"type" "MODEL_ID_NOT_FOUND"
                   "message" "Not found!!"}}})

(def ^:private fake-delete-response
  {"id" "rec123"
   "deleted" true})

(def expected-user-agent (str "airtable-clj/" (env :airtable-clj-version)))

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
  (testing "getting a non-record 404 should throw"
    (let [server (mock-server [fake-404-response])]
      (is (thrown-with-msg? Throwable #"NOT_FOUND"
                            (airtable/retrieve {:endpoint-url (:url server)
                                                :api-key "abc123"
                                                :base "base123"
                                                :table "My Table"
                                                :record-id "rec123"})))))
  (testing "getting a record 404 should give nil by default"
    (let [server (mock-server [fake-record-404-response])
          result (airtable/retrieve {:endpoint-url (:url server)
                                     :api-key "abc123"
                                     :base "base123"
                                     :table "My Table"
                                     :record-id "rec123"})]
      (is (nil? result))))
  (testing "getting a record 404 can throw an error"
    (let [server (mock-server [fake-404-response])]
      (is (thrown-with-msg? Throwable #"NOT_FOUND"
                            (airtable/retrieve {:endpoint-url (:url server)
                                                :api-key "abc123"
                                                :base "base123"
                                                :table "My Table"
                                                :record-id "rec123"
                                                :throw-if-not-found? true})))))
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

(deftest create-unit-test
  (testing "record creation"
    (let [server (mock-server [{:body fake-record-response}])
          result (airtable/create {:endpoint-url (:url server)
                                   :api-key "abc123"
                                   :base "base123"
                                   :table "My Table"
                                   :fields {"Foo" "Boo"}})
          request (take-mock-request server)
          headers (:headers request)]
      (is (= "/v0/base123/My%20Table" (:uri request)))
      (is (= {} (:query-params request)))
      (is (= :post (:request-method request)))
      (is (= expected-user-agent (headers "user-agent")))
      (is (= "0.1.0" (headers "x-api-version")))
      (is (= "application/json" (headers "content-type")))
      (is (= {"fields" {"Foo" "Boo"}} (:body request)))
      (is (= fake-record result))))
  (testing "typecast? parameter set to true"
    (let [server (mock-server [{:body fake-record-response}])
          result (airtable/create {:endpoint-url (:url server)
                                   :api-key "abc123"
                                   :base "base123"
                                   :table "My Table"
                                   :fields {"Foo" "Boo"}
                                   :typecast? true})
          request-body (:body (take-mock-request server))]
      (is (= {"fields" {"Foo" "Boo"}
              "typecast" true} request-body))))
  (testing "typecast? parameter set to false"
    (let [server (mock-server [{:body fake-record-response}])
          result (airtable/create {:endpoint-url (:url server)
                                   :api-key "abc123"
                                   :base "base123"
                                   :table "My Table"
                                   :fields {"Foo" "Boo"}
                                   :typecast? false})
          request-body (:body (take-mock-request server))]
      (is (= {"fields" {"Foo" "Boo"}} request-body))))
  (testing "calls out to handle-api-error"
    (let [server (mock-server [{:body fake-record-response}])
          handle-api-error-arg (atom nil)]
      (with-redefs [handle-api-error #(reset! handle-api-error-arg %)]
        (airtable/select {:endpoint-url (:url server)
                          :api-key "abc123"
                          :base "base123"
                          :table "My Table"
                          :fields {"Foo" "Boo"}})
        (is (some? (:status @handle-api-error-arg)))))))

(deftest modify-unit-test
  (testing "modifying a record"
    (let [server (mock-server [{:body fake-record-response}])
          result (airtable/modify {:endpoint-url (:url server)
                                   :api-key "abc123"
                                   :base "base123"
                                   :table "My Table"
                                   :record-id "rec123"
                                   :fields {"Foo" "Boo"}})
          request (take-mock-request server)
          headers (:headers request)]
      (is (= "/v0/base123/My%20Table/rec123" (:uri request)))
      (is (= {} (:query-params request)))
      (is (= :patch (:request-method request)))
      (is (= expected-user-agent (headers "user-agent")))
      (is (= "0.1.0" (headers "x-api-version")))
      (is (= {"fields" {"Foo" "Boo"}} (:body request)))
      (is (= fake-record result))))
  (testing "clobbering a record"
    (let [server (mock-server [{:body fake-record-response}])
          _ (airtable/modify {:endpoint-url (:url server)
                              :api-key "abc123"
                              :base "base123"
                              :table "My Table"
                              :record-id "rec123"
                              :fields {"Foo" "Boo"}
                              :clobber? true})
          request (take-mock-request server)]
      (is (= :put (:request-method request)))
      (is (= {"fields" {"Foo" "Boo"}} (:body request)))))
  (testing "typecast? parameter set to true"
    (let [server (mock-server [{:body fake-record-response}])
          _ (airtable/modify {:endpoint-url (:url server)
                              :api-key "abc123"
                              :base "base123"
                              :table "My Table"
                              :record-id "rec123"
                              :fields {"Foo" "Boo"}
                              :typecast? true})
          request (take-mock-request server)]
      (is (= {"fields" {"Foo" "Boo"}
              "typecast" true} (:body request)))))
  (testing "typecast? parameter set to false"
    (let [server (mock-server [{:body fake-record-response}])
          _ (airtable/modify {:endpoint-url (:url server)
                              :api-key "abc123"
                              :base "base123"
                              :table "My Table"
                              :record-id "rec123"
                              :fields {"Foo" "Boo"}
                              :typecast? false})
          request (take-mock-request server)]
      (is (= {"fields" {"Foo" "Boo"}} (:body request)))))
  (testing "calls out to handle-api-error"
    (let [server (mock-server [{:body fake-record-response}])
          handle-api-error-arg (atom nil)]
      (with-redefs [handle-api-error #(reset! handle-api-error-arg %)]
        (airtable/modify {:endpoint-url (:url server)
                          :api-key "abc123"
                          :base "base123"
                          :table "My Table"
                          :record-id "rec123"
                          :fields {"Foo" "Boo"}})
        (is (some? (:status @handle-api-error-arg)))))))

(deftest delete-unit-test
  (testing "deleting a record"
    (let [server (mock-server [{:body fake-delete-response}])
          result (airtable/delete {:endpoint-url (:url server)
                                   :api-key "abc123"
                                   :base "base123"
                                   :table "My Table"
                                   :record-id "rec123"})
          request (take-mock-request server)
          headers (:headers request)]
      (is (= "/v0/base123/My%20Table/rec123" (:uri request)))
      (is (= {} (:query-params request)))
      (is (= :delete (:request-method request)))
      (is (= expected-user-agent (headers "user-agent")))
      (is (= "0.1.0" (headers "x-api-version")))
      (is (= {:id "rec123"} result))))
  (testing "calls out to handle-api-error"
    (let [server (mock-server [{:body fake-delete-response}])
          handle-api-error-arg (atom nil)]
      (with-redefs [handle-api-error #(reset! handle-api-error-arg %)]
        (airtable/delete {:endpoint-url (:url server)
                          :api-key "abc123"
                          :base "base123"
                          :table "My Table"
                          :record-id "rec123"})
        (is (some? (:status @handle-api-error-arg)))))))
