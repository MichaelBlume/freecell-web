(ns freecell-web.progressive-autoplay
  (:require [freecell-web.moves :refer [fully-autosink reachable-states]]
            [freecell-web.cards :refer [winning?]]
            [freecell-web.scoring :as scoring]))

(defn into-set [state k]
  (update state k #(into #{} %)))

(defn deset [state k n]
  (update state k #(into [] (take n (concat % (repeat nil))))))

(defn canonize-state [state]
  (-> state fully-autosink (into-set :freecells) (into-set :columns)))

(defn decanonize-state [state]
  (-> state (deset :columns 8) (deset :freecells 4)))

(defn throw-error [s]
  (throw (#?(:clj IllegalArgumentException. :cljs js/Error.) s)))

(defn score-state [state]
  (if state
    (let [state (decanonize-state state)]
      (if (winning? state)
        :win
        (scoring/score-state state)))
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

(defn insert-children [ap-state game-state]
  (let [children (map canonize-state (reachable-states (decanonize-state game-state)))]
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

(defn maybe-insert-children [ap-state game-state]
  (if (get-in ap-state [game-state :children])
    ap-state
    (insert-children ap-state game-state)))

(defn update-autoplay-state [autoplay-state starting-state]
  (loop [game-state (canonize-state starting-state)]
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
        (insert-score autoplay-state game-state)))))

(defn get-next-move [ap-state starting-state]
  (let [potential-moves (reachable-states starting-state)]
    (apply max-key
      (fn [state]
        (let [canonical (canonize-state state)]
          (score->num
            (or (get-in ap-state [canonical :score])
                :no-win))))
      potential-moves)))

(defn lose-blitz [ap-state losing-states]
  (reduce
    #(update-score %1 %2 :no-win)
    ap-state
    losing-states))

(defn finish-blitz [ap-state visited-states terminal-states]
  (let [available-scores (get-scores ap-state terminal-states)
        missing-scores (children-without-scores terminal-states available-scores)
        ap-state (insert-child-scores ap-state missing-scores)
        ;; Yes, again
        available-scores (get-scores ap-state terminal-states)
        best-score (second (highest-scoring-entry available-scores))
        best-score-num (score->num best-score)]
    (reduce
      (fn [ap-state game-state]
        (update-in ap-state [game-state :score]
          (fn [old-score]
            (let [old-score (or old-score (score-state game-state))]
              (if (> (score->num old-score) best-score-num)
                best-score
                old-score)))))
      ap-state
      visited-states)))

(def empty-queue
  #?(:clj clojure.lang.PersistentQueue/EMPTY
     :cljs cljs.core.PersistentQueue.EMPTY))

(defn blitz [autoplay-state starting-state max-states]
  (let [fully-sinked (fully-autosink starting-state)]
    (loop [ap-state autoplay-state
           visited-states #{fully-sinked}
           q (conj empty-queue fully-sinked)
           to-visit max-states]
      (if (empty? q)
        (lose-blitz ap-state visited-states)
        (if (= to-visit 0)
          (finish-blitz ap-state visited-states q)
          (let [next-state (peek q)
                ap-state (maybe-insert-children ap-state next-state)
                children (get-in ap-state [next-state :children])
                children-to-visit (remove visited-states children)
                q (pop q)
                q (reduce conj q children-to-visit)]
            (recur
              ap-state
              (into visited-states children-to-visit)
              q
              (dec to-visit))))))))

(defn lookup-score [autoplay-state state]
  (get-in autoplay-state [(canonize-state state) :score]))
