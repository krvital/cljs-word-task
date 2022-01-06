(ns word-task.core
  (:require [rum.core :as rum]
            [goog.dom.forms :as gforms]))

(enable-console-print!)

;; DOM utils
(defn target-value [event] (.. event -target -value))
(defn input-value [input-id] (.-value (js/document.getElementById input-id)))

;; App state
(def default-text  "What {are} you doing?")

(def state
  (atom
   {:task-value default-text
    :task-parts '()
    :answers {}}))


;; Selectors
(defn- selector [state path]
  (rum/cursor-in state path))

(def task-value (selector state [:task-value]))
(def task-parts (selector state [:task-parts]))
(def answers (selector state [:answers]))


;; App
(rum/defc layout [& children]
  [:div.layout
   (for [child children] [:div.column {:key (gensym)} child])])

(defn parse-template [text]
  (def pattern #"(?i)(\{\w+\})")

  (def chunks (clojure.string/split text pattern))
  (def matches (set (re-seq #"(?i)\{\w+\}" text)))

  (for [chunk chunks]
    (if (contains? matches chunk)
      {:type "input" :props {:answer (clojure.string/replace chunk #"[\{\}]" "")
                             :uuid (gensym "task-")}}
      {:type "text" :props {:value chunk :uuid (gensym "text-")}})))

(defn save-task [text]
  (swap! state assoc :task-parts (parse-template text)))

(rum/defc editor [state task-value]
  [:div
   [:h2 "Editor"]
   [:form
    [:textarea {:auto-focus true
                :name "task-text"
                :cols 30
                :id "task-text"
                :default-value task-value}]
    [:br]
    [:button
     {:type "button"
      :on-click #(save-task (input-value "task-text"))}
     "Save"]]])

(defn save-answer [uuid correct-answer value]
  (swap! state assoc-in [:answers uuid]
         {:value value
          :status (if (= value correct-answer) "correct" "wrong")}))

(rum/defc input [answer uuid status]
  [:input.task-input
   {:type "text"
    :size (count answer)
    :class status
    :on-blur #(save-answer uuid answer (target-value %))}])

(rum/defc task [parts]
  (for [part parts]
    [:span {:key (:uuid (:props part))}
     (case (:type part)
       "input" (input (:answer (:props part)) (:uuid (:props part)) "init")
       "text" (:value (:props part)))]))

(rum/defc answers-list < rum/reactive []
  (let [ans (vals (rum/react answers))]
    [:ul
     (for [a ans]
       [:li (str (:value a) " - " (:status a))])]))

(rum/defc preview [parts]
  [:div
   [:h2 "Preview"]
   (task parts)
   (answers-list)])

(rum/defc app < rum/reactive [state]
  (layout
   (editor state (rum/react task-value))
   (preview (rum/react task-parts))))

(rum/mount
 (app state)
 (js/document.getElementById "app"))

