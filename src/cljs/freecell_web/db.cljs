(ns freecell-web.db
  (:require [freecell-web.cards :refer [shuffled-deck make-columns]]))

(def default-db
  {:name "re-frame"})

(defn init-ui []
  {:selected-column nil})

(defn init-state
  ([] (init-state (shuffled-deck)))
  ([deck])
    {:undo-states nil
     :redo-states nil
     :ui-state (init-ui)
     :cards-state
     {:columns (make-columns deck)
      :freecells (into [] (repeat 4 nil))
      :sinks {:spades 0 :clubs 0 :diamonds 0 :hearts 0}}})

(defn undo [{:keys [undo-states redo-states cards-state] :as state}]
  (when (seq undo-states)
    {:undo-states (rest undo-states)
     :redo-states (cons cards-state redo-states)
     :ui-state (init-ui)
     :cards-state (first undo-states)}))

(defn redo [{:keys [undo-states redo-states cards-state] :as state}]
  (when (seq redo-states)
    {:undo-states (cons cards-state undo-states)
     :redo-states (rest undo-states)
     :ui-state (init-ui)
     :cards-state (first redo-states)}))
