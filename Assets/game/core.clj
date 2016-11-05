(ns game.core
  (use 
    arcadia.core
    arcadia.linear
    tween.core
    hard.core)
  (import [SimpleTiledWFC]))


(defn make-park [w h]
  (let [o (clone! :maps/autopark)
        wfc (.GetComponent o "SimpleTiledWFC")]
    (timeline [
      (wait 0.1)
      #(do (local-scale! o (v3 4)) false)])
    (set! (.width wfc) (int w))
    (set! (.depth wfc) (int h)) o))

(defn make-player [loc]
  (let [loc (or loc (v3 0 10 0))
        board (clone! :simple loc)]
    board))


(defn make-level []
  (clear-cloned!)
  (make-park 10 10)
  (make-player (v3 5 5 5)))

(make-level)

