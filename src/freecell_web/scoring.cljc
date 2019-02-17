(ns freecell-web.scoring
  (:require [freecell-web.moves :refer [movable-card-counts]]))

(defn sinked-cards [card-state]
  (->> card-state :sinks vals (apply +)))

(defn count-based-cards [card-state]
  (apply + (for [column (:columns card-state)
                 :when (empty? (:immovable column))]
             (count (:movable column)))))

;; Higher scores are better
(defn score-state [card-state]
  (+
    (* -15 (:man-sinked card-state))
    (* 2 (sinked-cards card-state))
    (count-based-cards card-state)
    (* 4 (Math/sqrt (first (movable-card-counts card-state))))))
