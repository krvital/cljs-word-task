(ns word-task.template
  (:require [clojure.string :as str]))


(defn- split-into-chunks [text]
  (str/split text #"(?i)(\{\{.*?\}\})"))

(defn- find-all-objects [text]
  (set (re-seq #"(?i)\{\{.*?\}\}" text)))

(defn- get-object-value [object]
  (set (map str/trim (str/split (str/replace object #"[\{\}]" "") "|"))))

(defn parse-template [text]
  (let
   [chunks (split-into-chunks text)
    objects (find-all-objects text)]
    (for [chunk chunks]
      {:uuid (gensym "chunk-")
       :type (if (contains? objects chunk) "input" "text")
       :props (if (contains? objects chunk)
                {:answers (get-object-value chunk)}
                {:value chunk})})))
