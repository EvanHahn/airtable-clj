(ns airtable-clj.util
  (:require [clojure.string :as string]
            [environ.core :refer [env]])
  (:import [java.net URL]
           [java.text SimpleDateFormat]))

(def api-version "0.1.0")
(def project-version (env :airtable-clj-version))
(def user-agent (str "airtable-clj/" project-version))

(def ^:private date-format
  (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
(defn parse-time [s]
  (-> (.parse date-format s) .getTime))

(defn make-url [base-url-string base table]
  (let [base-url (URL. (or base-url-string "https://api.airtable.com"))
        protocol (.getProtocol base-url)
        host (.getHost base-url)
        port (.getPort base-url)
        path (str "/v0/" base "/" table)]
    (-> (URL. protocol host port path) str)))

(defn camelize-keyword [k]
  (-> (name k)
      (string/replace #"-(\w)" (fn ([[_ c]] (string/upper-case c))))))
