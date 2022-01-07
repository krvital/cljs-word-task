(ns word-task.core
  (:require [rum.core :as rum]
            [clojure.string :as str]))

(enable-console-print!)

;; DOM utils
(defn target-value [event] (.. event -target -value))
(defn input-value [input-id] (.-value (js/document.getElementById input-id)))

;; State management

(def default-state
  {:task-text "What {{ are | the hell are }} you doing? Where {{ have }} you been?"
   :task-parts nil
   :answers {}})

(def state (atom default-state))

(defn- cursor [path] (rum/cursor-in state path))
(def task-text (cursor [:task-text]))
(def task-parts (cursor [:task-parts]))
(def answers (cursor [:answers]))

(defn select-correct-answers [uuid]
  (let [task-parts (:task-parts @state)]
    (filter #(= uuid (:uuid %)) task-parts)
    (reduce #(into %1 (get-in %2 [:props :answers])) #{} task-parts)))

(defn- split-into-chunks [text]
  (str/split text #"(?i)(\{\{.*?\}\})"))

(defn- find-all-objects [text]
  (set (re-seq #"(?i)\{\{.*?\}\}" text)))

(defn- get-object-value [object]
  (set (map str/trim (str/split (str/replace object #"[\{\}]" "") "|"))))

(defn- parse-template [text]
  (let
   [chunks (split-into-chunks text)
    objects (find-all-objects text)]
    (for [chunk chunks]
      {:uuid (gensym "chunk-")
       :type (if (contains? objects chunk) "input" "text")
       :props (if (contains? objects chunk)
                {:answers (get-object-value chunk)}
                {:value chunk})})))

(defn save-task [text]
  (swap! state assoc :task-parts (parse-template text) :task-text text))

(defn save-answer [uuid value]
  (let [correct-answers (select-correct-answers uuid)]
    (swap! state assoc-in [:answers uuid]
           {:value value
            :status (if (contains? correct-answers value) "correct" "wrong")})))


;; Components


(rum/defc layout [& children]
  [:div.layout
   (for [child children] [:div.column {:key (gensym)} child])])

(rum/defc editor [task-text]
  [:div
   [:h2 "Editor"]
   [:form
    [:textarea {:auto-focus true
                :name "task-text"
                :cols 40
                :id "task-text"
                :default-value task-text}]
    [:br]
    [:button {:type "button"
              :on-click #(save-task (input-value "task-text"))}
     "Save"]]])

(rum/defc input [uuid status size]
  [:input.task-input
   {:type "text"
    :size size
    :class status
    :on-blur #(save-answer uuid (target-value %))}])

(rum/defc task < rum/reactive [parts]
  (let [answers (rum/react answers)]
    (for [part parts]
      [:span {:key (:uuid part)}
       (case (:type part)
         "input" (input
                  (:uuid part)
                  (:status (get answers (:uuid part)))
                  (apply max (map count (:answers (:props part)))))
         "text" (:value (:props part)))])))

;; (rum/defc answers-list < rum/reactive []
;;   (let [ans (vals (rum/react answers))]
;;     [:ul
;;      (for [a ans]
;;        [:li (str (:value a) " - " (:status a))])]))

(rum/defc preview [parts]
  [:div
   [:h2 "Preview"]
   (task parts)])

(rum/defc app < rum/reactive []
  (layout
   (editor (rum/react task-text))
   (preview (rum/react task-parts))))

(rum/mount
 (app)
 (js/document.getElementById "app"))
