(ns freecell-web.subs
    (:require [cljs.pprint :refer [pprint]]
              [re-frame.core :refer [reg-sub]]
              [freecell-web.cards :refer [suits get-all-cards]]
              [freecell-web.db :refer [selected]]))

(reg-sub
  :columns
  :<- [:game-state]
  (fn [game-state _]
    (let [cs (map get-all-cards (:columns game-state))
          longest (inc (apply max (map count cs)))]
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
  :selected-freecell
  (fn [db [_ n]]
    (= [:freecell n] (selected db))))

(reg-sub
  ; re-built every time game-state changes -- bad?
  :sinks
  :<- [:game-state]
  (fn [game-state _]
    (for [suit suits]
      (let [n (-> game-state :sinks suit)]
        {:suit suit :n n}))))

(reg-sub
  :debug
  :<- [:game-state]
  (fn [game-state _]
    (with-out-str
      (pprint game-state))))
