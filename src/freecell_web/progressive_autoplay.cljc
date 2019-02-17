(ns freecell-web.progressive-autoplay
  (:require [freecell-web.moves :refer [fully-autosink reachable-states]]
            [freecell-web.cards :refer [winning?]]
            [freecell-web.scoring :as scoring]))

(defn throw-error [s]
  (throw (#?(:clj IllegalArgumentException. :cljs js/Error.) s)))

(defn score-state [state]
  (if state
    (if (winning? state)
      :win
      (scoring/score-state state))
    (throw-error "But where is state")))

(defn get-scores [autoplay-state states]
  (into {}
    (filter identity
      (for [state states]
        (when-let [score (get-in autoplay-state [state :score])]
          [state score])))))

(defn children-without-scores [children child-scores]
  (if child-scores
    (remove child-scores children)
    (throw-error "But where are child scores")))

(defn update-score [ap-state game-state score]
  (assoc-in ap-state [game-state :score] score))

(defn insert-score [ap-state game-state]
  (update-score ap-state game-state (score-state game-state)))

(defn insert-child-scores [autoplay-state missing-children]
  (reduce insert-score autoplay-state missing-children))

(defn score->num [score]
  (case score
    :no-win #?(:clj Long/MIN_VALUE :cljs js/-Infinity)
    :win #? (:clj Long/MAX_VALUE :cljs js/Infinity)
    nil (throw-error "but where is score")
    score))

(defn decrement-score [score]
  (case score
    :no-win :no-win
    :win :win
    nil (throw-error "but where is score")
    (- score 0.5)))

(defn highest-scoring-entry [child-scores]
  (if (seq child-scores)
    (let [[state score] (apply max-key
                          (fn [[state score]]
                            (score->num score))
                          child-scores)]
      [state (decrement-score score)])
    [nil :no-win]))

(defn sort-freecells [state]
  (update state :freecells #(->> % (sort-by hash) (into []))))

(defn standardize-state [state]
  (-> state fully-autosink sort-freecells))

(defn insert-children [ap-state game-state]
  (let [children (map standardize-state (reachable-states game-state))]
    (loop [ap-state ap-state
           children children
           children-to-insert []]
      (if (seq children)
        (let [child (first children)
              child-from-map (get-in ap-state [child :canon])]
          (if child-from-map
            (recur
              ap-state
              (next children)
              (conj children-to-insert child-from-map))
            (recur
              (assoc ap-state child {:canon child})
              (next children)
              (conj children-to-insert child))))
        (assoc-in ap-state [game-state :children] children-to-insert)))))

(defn update-autoplay-state [autoplay-state starting-state]
  (let [fully-sinked (fully-autosink starting-state)]
    (loop [game-state fully-sinked]
      (let [state-info (get autoplay-state game-state)]
        (if-let [state-score (:score state-info)]
          (if (= state-score :win)
            autoplay-state
            (if-let [children (:children state-info)]
              (let [child-scores (get-scores autoplay-state children)
                    missing-children (children-without-scores
                                       children child-scores)]
                (if (seq missing-children)
                  (insert-child-scores autoplay-state missing-children)
                  (let [[child score] (highest-scoring-entry child-scores)]
                    (if (= score state-score)
                      (recur child)
                      (update-score autoplay-state game-state score)))))
              (insert-children autoplay-state game-state)))
          (insert-score autoplay-state game-state))))))
