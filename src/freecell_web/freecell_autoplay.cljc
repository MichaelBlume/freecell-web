(ns freecell-web.freecell-autoplay
  (:require [freecell-web.moves :refer [movable-card-counts fully-autosink reachable-states]]
            [freecell-web.cards :refer [winning?]]
            [freecell-web.progressive-autoplay :refer [SolitaireGame]]))

(defn sinked-cards [card-state]
  (->> card-state :sinks vals (apply +)))

(defn count-based-cards [card-state]
  (apply + (for [column (:columns card-state)
                 :when (empty? (:immovable column))]
             (count (:movable column)))))


(defn into-set [state k]
  (update state k #(into #{} %)))

(defn deset [state k n]
  (update state k #(into [] (take n (concat % (repeat nil))))))

(defrecord FreecellGame []
  SolitaireGame
  (reachable-states [this card-state]
    (reachable-states card-state))
  (score-state [this card-state]
    (if (winning? card-state)
      :win
      (+
        (* -15 (:man-sinked card-state))
        (* 2 (sinked-cards card-state))
        (count-based-cards card-state)
        (* 4 (Math/sqrt (first (movable-card-counts card-state)))))))
  (canonize-state [this state]
    (-> state fully-autosink (into-set :freecells) (into-set :columns)))
  (decanonize-state [this state]
    (-> state (deset :columns 8) (deset :freecells 4))))

(def freecell (->FreecellGame))
