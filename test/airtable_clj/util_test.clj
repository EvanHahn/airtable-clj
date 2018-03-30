(ns airtable-clj.util-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as json]
            [airtable-clj.util :refer [handle-api-error]]))

(defn- status [code] {:status code})

(defn- response [status body-map]
  {:status status
   :body (json/generate-string body-map)})

(deftest handle-api-error-test
  (testing "2xx status codes are fine"
    (handle-api-error (status 200))
    (handle-api-error (status 201)))
  (testing "401 errors"
    (is (thrown-with-msg? Throwable #"(?i)api key"
                          (handle-api-error (status 401)))))
  (testing "403 errors"
    (is (thrown-with-msg? Throwable #"(?i)forbidden"
                          (handle-api-error (status 403)))))
  (testing "404 errors"
    (is (thrown-with-msg? Throwable #"(?i)not found"
                          (handle-api-error (status 404))))
    (is (thrown-with-msg? Throwable #"bing bong"
                          (handle-api-error (response 404 {"error" "bing bong"}))))
    (is (thrown-with-msg? Throwable #"bing bong"
                          (handle-api-error (response 404 {"error" {"message" "bing bong"}})))))
  (testing "413 errors"
    (is (thrown-with-msg? Throwable #"(?i)too large"
                          (handle-api-error (status 413)))))
  (testing "422 errors"
    (is (thrown-with-msg? Throwable #"(?i)request cannot be processed"
                          (handle-api-error (status 422))))
    (is (thrown-with-msg? Throwable #"bing bong"
                          (handle-api-error (response 422 {"error" {"message" "bing bong"}})))))
  (testing "429 errors"
    (is (thrown-with-msg? Throwable #"(?i)too many requests"
                          (handle-api-error (status 429)))))
  (testing "500 errors"
    (is (thrown-with-msg? Throwable #"(?i)server error"
                          (handle-api-error (status 500))))
    (is (thrown-with-msg? Throwable #"(?i)server error"
                          (handle-api-error {:status 500
                                             :body "<html>Catastrophic error</html>"}))))
  (testing "503 errors"
    (is (thrown-with-msg? Throwable #"(?i)service unavailable"
                          (handle-api-error (status 503)))))
  (testing "other 4xx errors"
    (is (thrown-with-msg? Throwable #"(?i)bad request"
                          (handle-api-error (status 469)))))
  (testing "other 5xx errors"
    (is (thrown-with-msg? Throwable #"(?i)server error"
                          (handle-api-error (status 569))))))
