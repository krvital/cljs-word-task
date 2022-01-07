(ns word-task.dom)

;;; DOM utilities

(defn target-value [event]
  (.. event -target -value))

(defn input-value [input-id]
  (.-value (js/document.getElementById input-id)))
