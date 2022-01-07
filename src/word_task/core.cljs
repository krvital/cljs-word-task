(ns word-task.core
  (:require [rum.core :as rum]
            [clojure.string :as str]))

(enable-console-print!)

;; DOM utils
(defn target-value [event] (.. event -target -value))
(defn input-value [input-id] (.-value (js/document.getElementById input-id)))


;; State management


(def default-state
  {:task-value "What {{ are | the hell are }} you doing? Where {{ have }} you been?"
   :task-parts nil
   :answers {}})

(def state (atom default-state))

(defn- selector [state path]
  (rum/cursor-in state path))

(def task-value (selector state [:task-value]))
(def task-parts (selector state [:task-parts]))
(def answers (selector state [:answers]))

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
  (swap! state assoc :task-parts (parse-template text) :task-value text))

(defn save-answer [uuid correct-answers value]
  (swap! state assoc-in [:answers uuid]
         {:value value
          :status (if (contains? correct-answers value) "correct" "wrong")}))


;; Components


(rum/defc layout [& children]
  [:div.layout
   (for [child children] [:div.column {:key (gensym)} child])])

(rum/defc editor [task-value]
  [:div
   [:h2 "Editor"]
   [:form
    [:textarea {:auto-focus true
                :name "task-text"
                :cols 40
                :id "task-text"
                :default-value task-value}]
    [:br]
    [:button {:type "button"
              :on-click #(save-task (input-value "task-text"))}
     "Save"]]])

(rum/defc input [uuid correct-answers status]
  [:input.task-input
   {:type "text"
    :size (apply max (map count correct-answers))
    :class status
    :on-blur #(save-answer uuid correct-answers (target-value %))}])

(rum/defc task < rum/reactive [parts]
  (let [answers (rum/react answers)]
    (for [part parts]
      [:span {:key (:uuid part)}
       (case (:type part)
         "input" (input
                  (:uuid part)
                  (:answers (:props part))
                  (:status (get answers (:uuid part))))
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
   (editor (rum/react task-value))
   (preview (rum/react task-parts))))

(rum/mount
 (app)
 (js/document.getElementById "app"))
