(ns freecell-web.db
  (:require [freecell-web.cards :refer [shuffled-deck make-columns winning?]]
            [cljs.reader :refer [read-string]]
            [re-frame.core :refer [reg-sub]]))

(defn init-ui []
  ; three possible states -- nil, [:column n], [:freecell n]
  {:selected nil})

(def store (.-localStorage js/window))

(defn store-object [k obj]
  (.setItem store k (prn-str obj)))

(defn get-object [k]
  (when-let [s (.getItem store k)]
    (read-string s)))

(defn save-state [db]
  (store-object "freecell-state" db))

(defn clear-ui [state]
  (assoc state :ui-state (init-ui)))

(defn selected [state] (-> state :ui-state :selected))

(defn selected-area [state] (-> state selected first))

(defn update-card-state [{:keys [::undo-states ::cards-state] :as db} f]

  (let [new-cs (f (assoc cards-state ::new-game false))]
    (if (and new-cs (not= new-cs cards-state))
      {::undo-states (when-not (winning? cards-state)
                       (cons cards-state undo-states))
       ::redo-states nil
       :ui-state (init-ui)
       ::cards-state new-cs}
      (clear-ui db))))

(defn init-cards
  ([] (init-cards (shuffled-deck)))
  ([deck]
   {:columns (make-columns deck)
    :freecells (into [] (repeat 4 nil))
    :sinks {:spades 0 :clubs 0 :diamonds 0 :hearts 0}
    ::new-game true}))

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

(defn redo [{:keys [::undo-states ::redo-states ::cards-state]}]
  (when (seq redo-states)
    {::undo-states (cons cards-state undo-states)
     ::redo-states (rest redo-states)
     :ui-state (init-ui)
     ::cards-state (first redo-states)}))

(defn reset [{:keys [::undo-states ::redo-states ::cards-state]}]
  (let [current-and-past (cons cards-state undo-states)
        rewound-over (take-while (complement ::new-game) current-and-past)
        new-undoes-and-start (drop-while (complement ::new-game) current-and-past)]
    {::undo-states (rest new-undoes-and-start)
     :ui-state (init-ui)
     ::cards-state (first new-undoes-and-start)
     ::redo-states (concat (reverse rewound-over) redo-states)}))

(defn redo-all [{:keys [::undo-states ::redo-states ::cards-state]}]
  (let [future-and-current (reverse (cons cards-state redo-states))]
    {::undo-states (concat (rest future-and-current) undo-states)
     ::cards-state (first future-and-current)
     :redo-states nil}))

(defn undoing [{:keys [::redo-states]}]
  (seq redo-states))

(reg-sub
 :game-state
 (fn [db]
   (::cards-state db)))
