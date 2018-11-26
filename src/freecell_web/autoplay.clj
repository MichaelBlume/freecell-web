(ns freecell-web.autoplay
  (:import
   [java.util.concurrent LinkedBlockingQueue TimeUnit])
  (:require
   [freecell-web.db :refer [map->CardsState map->Sinks]]
   [freecell-web.cards :refer [winning? map->Card]]
   [freecell-web.moves :refer :all]))

(defn rec-cards [cards-state]
  (update-in cards-state [:columns] (fn [cs] (into [] (map #(map map->Card %) cs)))))

(def bad-state
  (->
    '{:columns
      [({:n 8, :suit :spades}
         {:n 2, :suit :clubs}
         {:n 2, :suit :diamonds}
         {:n 13, :suit :clubs}
         {:n 3, :suit :clubs}
         {:n 12, :suit :clubs}
         {:n 12, :suit :spades})
       ({:n 13, :suit :hearts}
         {:n 1, :suit :diamonds}
         {:n 2, :suit :spades}
         {:n 3, :suit :hearts}
         {:n 8, :suit :diamonds}
         {:n 4, :suit :clubs}
         {:n 5, :suit :clubs})
       ({:n 9, :suit :diamonds}
         {:n 12, :suit :hearts}
         {:n 10, :suit :hearts}
         {:n 7, :suit :hearts}
         {:n 10, :suit :diamonds}
         {:n 4, :suit :hearts}
         {:n 6, :suit :diamonds})
       ({:n 4, :suit :spades}
         {:n 3, :suit :diamonds}
         {:n 2, :suit :hearts}
         {:n 10, :suit :clubs}
         {:n 3, :suit :spades}
         {:n 7, :suit :clubs}
         {:n 7, :suit :spades})
       ({:n 6, :suit :spades}
         {:n 9, :suit :hearts}
         {:n 13, :suit :diamonds}
         {:n 11, :suit :spades}
         {:n 4, :suit :diamonds}
         {:n 1, :suit :clubs})
       ({:n 11, :suit :diamonds}
         {:n 9, :suit :clubs}
         {:n 1, :suit :hearts}
         {:n 7, :suit :diamonds}
         {:n 5, :suit :spades}
         {:n 9, :suit :spades})
       ({:n 8, :suit :hearts}
         {:n 11, :suit :clubs}
         {:n 6, :suit :hearts}
         {:n 5, :suit :hearts}
         {:n 5, :suit :diamonds}
         {:n 10, :suit :spades})
       ({:n 11, :suit :hearts}
         {:n 8, :suit :clubs}
         {:n 12, :suit :diamonds}
         {:n 6, :suit :clubs}
         {:n 13, :suit :spades}
         {:n 1, :suit :spades})],
      :freecells [nil nil nil nil],
      :sinks {:spades 0, :clubs 0, :diamonds 0, :hearts 0}}
    map->CardsState
    (update-in [:sinks] map->Sinks)
    rec-cards))

(-> bad-state
  (column-to-first-freecell 4)
  (move-column 0 2)
  (move-column 2 4)
  (move-column 4 2))

(let [init (-> bad-state
             (column-to-first-freecell 4)
             (move-column 0 2))
      cycled (-> init
               (move-column 2 4)
               (move-column 4 2))
      m (-> {} (assoc init 3) (assoc cycled 4))]
  (count m))

(defn fully-autosink [state]
  (loop [state state]
    (let [new-state (autosink state)]
      (if (and new-state (not= state new-state))
        (recur new-state)
        state))))

(defn process-state [card-history enqueue states-atom win thread-name]
  (let [card-state (first card-history)
        sinked (fully-autosink card-state)
        reachable (reachable-states sinked)
        new-history (if (= sinked card-state)
                      card-history
                      (cons sinked card-history))]
    (when (winning? sinked)
      (win new-history))
    (dorun reachable)
    (let [reached-states @states-atom]
      (doseq [state reachable
              :when (not (reached-states state))
              :let [result (swap!
                             states-atom
                             (fn [reached-states]
                               (if (reached-states state)
                                 reached-states
                                 (assoc reached-states state {:thread-name thread-name}))))]
              :when (= (:thread-name (result state)) thread-name)]
        (enqueue (cons state new-history))))))

(defn mk-thread-fn [^LinkedBlockingQueue q states result done]
  (fn [n]
    (loop []
      (let [history (.poll q 5 TimeUnit/SECONDS)]
        (if history
          (do
            (process-state
              history
              #(.put q %)
              states
              #(do
                 (.put result %)
                 (reset! done true))
              n)
            (when-not @done
              (recur)))
          (do
            (.put result ::failed)
            (reset! done true)))))))

(defn brute-force-freecell [init-state]
  (let [q (LinkedBlockingQueue.)
        states (atom {init-state {:history []
                                  :thread-name "main, idk"}})
        result (LinkedBlockingQueue.)
        done (atom false)
        thread-fn (mk-thread-fn q states result done)]
    (.put q [init-state])
    (doseq [i (range 16)]
      (.start (Thread. #(thread-fn i))))
    (.take result)))

(comment
  (freecell-web.cards/run-move (:columns bad-state) 0 0 4)

  (fully-autosink bad-state)

  (brute-force-freecell bad-state)

  (def q (LinkedBlockingQueue.))

  (def states (atom {bad-state {:history []
                                :thread-name "main, idk"}}))

  (def result (LinkedBlockingQueue.))

  (def done (atom false))

  (def thread-fn (mk-thread-fn q states result done))

  (.put q [bad-state])

  (.size q)

  (.size result)

  (count @states)

  (apply max (map #(-> % :history count) (vals @states)))

  (doseq [i (range 16)]
    (.start (Thread. #(thread-fn i))))

  (reset! done true)

  (.take result))
