(ns freecell-web.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :refer [reg-sub subscribe]]
              [freecell-web.cards :refer [suits]]
              ))

(reg-sub
 :name
 (fn [db]
   (:name db)))

(reg-sub
 :game-state
 (fn [db]
   (:cards-state db)))

(reg-sub
  :columns
  :<- [:game-state]
  (fn [game-state _]
    (:columns game-state)))

(reg-sub
  :cells
  :<- [:game-state]
  (fn [game-state _]
    (:freecells game-state)))

(reg-sub
  :selected
  (fn [db [_ n]]
    (= n (-> db :ui-state :selected-column))))

(reg-sub
  ; re-built every time game-state changes -- bad?
  :sinks
  :<- [:game-state]
  (fn [game-state _]
    (for [suit suits]
      (let [n (-> game-state :sinks suit)]
        {:suit suit :n n}))))

(reg-sub
  :column
  (fn [[_ n] _]
    [(subscribe [:columns])
     (subscribe [:selected n])])
  (fn [[columns selected] [_ n]]
    {:cards (nth columns n)
     :selected selected}))

(reg-sub
  :ui-state
  (fn [db] (:ui-state db))
  )
