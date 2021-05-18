(ns airtable-clj.integration-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [airtable-clj.test-helpers :refer [can-integration-test?]]
            [airtable-clj.core :as airtable]
            [airtable-clj.util :refer [parse-response]])
  (:import [java.util Date]))

(defn- pause [] (Thread/sleep 1000))

(def ^:private options
  (partial merge {:api-key (:airtable-api-key env)
                  :base (:airtable-base env)
                  :table (:airtable-table env)}))

(defn- check-record [{:keys [id created-time fields]}]
  (is (string? id))
  (is (seq id))
  (is (integer? created-time))
  (is (> created-time 1514786400000))
  (let [primary (get fields "Primary")
        formula (get fields "Formula")]
    (is (integer? primary))
    (is (= formula (+ primary 10)))))

(when can-integration-test?
  (deftest select-integration-test
    (pause)
    (let [result (airtable/select (options {}))
          records (:records result)]
      (is (seq records))
      (doall (map check-record records))))
  (deftest retrieve-nothing-test
    (pause)
    (is (nil? (airtable/retrieve (options {:record-id "recBogus"})))))
  (deftest create-read-update-delete-test
    (pause)
    (let [single-value (str "Created on " (Date.))
          create-result (airtable/create (options {:fields {"Single" single-value}}))
          record-id (:id create-result)
          retrieve-result (airtable/retrieve (options {:record-id record-id}))
          modify-result (airtable/modify (options {:record-id record-id
                                                   :fields {"Checkbox" true}}))
          modify-expected (update create-result :fields assoc "Checkbox" true)
          clobber-result (airtable/modify (options {:record-id record-id
                                                    :fields {"Checkbox" true}
                                                    :clobber? true}))
          clobber-expected (update modify-result :fields dissoc "Single")
          delete-result (airtable/delete (options {:record-id record-id}))]
      (check-record create-result)
      (is (= single-value (get-in create-result [:fields "Single"])))
      (is (= create-result retrieve-result))
      (is (= modify-expected modify-result))
      (is (= clobber-expected clobber-result))
      (is (= {:id record-id} delete-result)))))
