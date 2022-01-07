(ns word-task.core
  (:require [rum.core :as rum]
            [word-task.dom :refer [target-value input-value]]
            [word-task.template :refer [parse-template]]))

(enable-console-print!)

;;; STATE

(def default-task-text
  "What {{ are | the hell are }} you doing? Where {{ have }} you been?")

(def default-state
  {:task-text default-task-text
   :task-parts (parse-template default-task-text)
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

(rum/defc heading [text]
  [:h2 {:class "text-3xl font-medium mb-4"} text])

(rum/defc layout [& children]
  [:div {:class "grid grid-cols-2 gap-x-3 w-9/12"}
   (for [child children] [:div {:class "p-4 shadow-md bg-white" :key (gensym)} child])])

(rum/defc editor [task-text]
  [:div
   (heading "Editor")
   [:form
    [:textarea {:name "task-text"
                :id "task-text"
                :rows 10
                :default-value task-text
                :class "resize-none w-full p-2 border-2 border-gray-300 caret-pink-500
                        focus:outline-none focus:border-pink-300
                        text-gray-900"}]
    [:button {:type "button"
              :class "border-2 border-gray-300 font-semibold hover:bg-gray-100
                      hover:border-gray-400 hover:text-gray-900 px-4 py-2 text-gray-600 transition-colors"
              :on-click #(save-task (input-value "task-text"))}
     "Save"]]])

(rum/defc input [uuid status size]
  [:input
   {:type "text"
    :size size
    :class (str
            (case status
              "correct" "border-lime-500"
              "wrong" "border-rose-500"
              "border-gray-300")
            " "
            "border-2 border-gray-300 rounded-none
            focus:outline-none focus:border-pink-300
            px-2 caret-pink-500")
    :on-blur #(save-answer uuid (target-value %))}])

(rum/defc task < rum/reactive [parts]
  [:div {:class "p-2"}
   (let [answers (rum/react answers)]
     (for [part parts]
       [:span {:key (:uuid part)}
        (case (:type part)
          "input" (input
                   (:uuid part)
                   (:status (get answers (:uuid part)))
                   (apply max (map count (:answers (:props part)))))
          "text" (:value (:props part)))]))])

(rum/defc preview [parts]
  [:div
   (heading "Preview")
   (task parts)])

(rum/defc app < rum/reactive []
  (layout
   (editor (rum/react task-text))
   (preview (rum/react task-parts))))

(rum/mount
 (app)
 (js/document.getElementById "app"))
