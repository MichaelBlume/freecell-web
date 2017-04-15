(ns freecell-web.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :refer [reg-sub subscribe]]
              [freecell-web.cards :refer [suits]]
              [freecell-web.db :refer [selected]]
              ))

(reg-sub
 :game-state
 (fn [db]
   (:cards-state db)))

(reg-sub
  :columns
  :<- [:game-state]
  (fn [game-state _]
    (let [cs (:columns game-state)
          longest (apply max (map count cs))]
      (for [c cs]
        (concat
          (repeat
            (- longest (count c))
            nil)
          c)))))

(reg-sub
  :cells
  :<- [:game-state]
  (fn [game-state _]
    (:freecells game-state)))

(reg-sub
  :selected
  (fn [db [_ n]]
    (= [:column n] (selected db))))

(reg-sub
  ; re-built every time game-state changes -- bad?
  :sinks
  :<- [:game-state]
  (fn [game-state _]
    (for [suit suits]
      (let [n (-> game-state :sinks suit)]
        {:suit suit :n n}))))
