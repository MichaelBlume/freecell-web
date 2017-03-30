(ns freecell-web.cards)

(def colors
  {:spades :black
   :hearts :red
   :clubs :black
   :diamonds :red})

(defn color [card] (-> card :suit colors))

(def deck
  (into []
    (for [suit [:spades :hearts :clubs :diamonds]
          n (range 1 14)]
      {:n n :suit suit})))

(defn shuffled-deck [] (shuffle deck))

(def suit-chars
  {:spades \S
   :hearts \H
   :clubs \C
   :diamonds \D})

(def face-chars
  {11 \J
   12 \Q
   13 \K})

(defn display-string [card]
  (str (face-chars (:n card) (:n card)) (suit-chars (:suit card))))
