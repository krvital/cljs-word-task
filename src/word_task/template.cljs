(ns word-task.template
  (:require [clojure.string :as str]))

;; split text into chunks
;; "This is {{ the }} text" -> ["This is " "{{ the }}" "text"]
(defn- split-into-chunks [text]
  (str/split text #"(?i)(\{\{.*?\}\})"))

;; find all text objects meet the pattern
;; "This is {{ the }} text" -> #{"{{ the }}"}
(defn- find-all-objects [text]
  (set (re-seq #"(?i)\{\{.*?\}\}" text)))

(defn- get-object-value [object]
  (set (map str/trim (str/split (str/replace object #"[\{\}]" "") "|"))))

;; functions takes a template string such as "This is {{ the }} template string"
;; and parses it into collection of items with structure suitable for using
;; in a react component
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
