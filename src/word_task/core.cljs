(ns word-task.core
  (:require [rum.core :as rum]
            [word-task.dom :refer [target-value input-value]]
            [word-task.template :refer [parse-template]]
            ))

(enable-console-print!)

;;; STATE

(def default-state
  {:task-text "What {{ are | the hell are }} you doing? Where {{ have }} you been?"
   :task-parts nil
   :answers {}})

(def state (atom default-state))

(defn- cursor [path] (rum/cursor-in state path))

;; Cursors
(def task-text (cursor [:task-text]))
(def task-parts (cursor [:task-parts]))
(def answers (cursor [:answers]))

(defn select-correct-answers [uuid]
  (let [task-parts (:task-parts @state)]
    (filter #(= uuid (:uuid %)) task-parts)
    (reduce #(into %1 (get-in %2 [:props :answers])) #{} task-parts)))

;; Actions
(defn save-task [text]
  (swap! state assoc :task-parts (parse-template text) :task-text text))

(defn save-answer [uuid value]
  (let [correct-answers (select-correct-answers uuid)]
    (swap! state assoc-in [:answers uuid]
           {:value value
            :status (if (contains? correct-answers value) "correct" "wrong")})))

;;; UI

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
