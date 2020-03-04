(ns airtable-clj.util-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as json]
            [airtable-clj.util :refer [parse-response]]))

(defn- status [code] {:status code})

(defn- response [status body-map]
  {:status status
   :body (json/generate-string body-map)})

(deftest parse-response-test
  (testing "2xx status codes are fine"
    (parse-response (status 200))
    (parse-response (status 201)))
  (testing "401 errors"
    (is (thrown-with-msg? Throwable #"(?i)api key"
                          (parse-response (status 401)))))
  (testing "403 errors"
    (is (thrown-with-msg? Throwable #"(?i)forbidden"
                          (parse-response (status 403)))))
  (testing "404 errors"
    (is (thrown-with-msg? Throwable #"(?i)not found"
                          (parse-response (status 404))))
    (is (thrown-with-msg? Throwable #"bing bong"
                          (parse-response (response 404 {"error" "bing bong"}))))
    (is (thrown-with-msg? Throwable #"bing bong"
                          (parse-response (response 404 {"error" {"message" "bing bong"}})))))
  (testing "413 errors"
    (is (thrown-with-msg? Throwable #"(?i)too large"
                          (parse-response (status 413)))))
  (testing "422 errors"
    (is (thrown-with-msg? Throwable #"(?i)request cannot be processed"
                          (parse-response (status 422))))
    (is (thrown-with-msg? Throwable #"bing bong"
                          (parse-response (response 422 {"error" {"message" "bing bong"}})))))
  (testing "500 errors"
    (is (thrown-with-msg? Throwable #"(?i)server error"
                          (parse-response (status 500))))
    (is (thrown-with-msg? Throwable #"(?i)server error"
                          (parse-response {:status 500
                                           :body "<html>Catastrophic error</html>"}))))
  (testing "503 errors"
    (is (thrown-with-msg? Throwable #"(?i)service unavailable"
                          (parse-response (status 503)))))
  (testing "other 4xx errors"
    (is (thrown-with-msg? Throwable #"(?i)bad request"
                          (parse-response (status 469)))))
  (testing "other 5xx errors"
    (is (thrown-with-msg? Throwable #"(?i)server error"
                          (parse-response (status 569))))))
