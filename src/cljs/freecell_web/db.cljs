(ns freecell-web.db
  (:require [freecell-web.cards :refer [shuffled-deck make-columns]]
            [cljs.reader :refer [read-string]]
            [re-frame.core :refer [reg-sub]]))

(def default-db
  {:name "re-frame"})

(defn init-ui []
  ; three possible states -- nil, [:column n], [:freecell n]
  {:selected nil})

(def store (.-sessionStorage js/window))

(defn store-object [k obj]
  (.setItem store k (prn-str obj)))

(defn get-object [k]
  (when-let [s (.getItem store k)]
    (read-string s)))

(defn save-state [db]
  (store-object "freecell-state" db))

(defn clear-ui [state]
  (assoc-in state [:ui-state] (init-ui)))

(defn selected [state] (-> state :ui-state :selected))

(defn selected-area [state] (-> state selected first))

(defn update-card-state [{:keys [::undo-states ::cards-state] :as db} f]

  (let [new-cs (f cards-state)]
    (if (and new-cs (not= new-cs cards-state))
      {::undo-states (cons cards-state undo-states)
       ::redo-states nil
       :ui-state (init-ui)
       ::cards-state new-cs}
      (clear-ui db))))

(defn init-cards
  ([] (init-cards (shuffled-deck)))
  ([deck]
   {:columns (make-columns deck)
    :freecells (into [] (repeat 4 nil))
    :sinks {:spades 0 :clubs 0 :diamonds 0 :hearts 0}}))

(defn init-state
  ([]
   (if-let [saved (get-object "freecell-state")]
     saved
     (init-state (shuffled-deck))))
  ([deck]
   {::undo-states nil
    ::redo-states nil
    :ui-state (init-ui)
    ::cards-state (init-cards deck)}))

(defn undo [{:keys [::undo-states ::redo-states ::cards-state] :as state}]
  (when (seq undo-states)
    {::undo-states (rest undo-states)
     ::redo-states (cons cards-state redo-states)
     :ui-state (init-ui)
     ::cards-state (first undo-states)}))

(defn redo [{:keys [::undo-states ::redo-states ::cards-state] :as state}]
  (when (seq redo-states)
    {::undo-states (cons cards-state undo-states)
     ::redo-states (rest redo-states)
     :ui-state (init-ui)
     ::cards-state (first redo-states)}))

(defn undoing [{:keys [::redo-states]}]
  (seq redo-states))

(reg-sub
 :game-state
 (fn [db]
   (::cards-state db)))
