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
