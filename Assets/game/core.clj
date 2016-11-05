(ns game.core
  (use 
    arcadia.core
    arcadia.linear
    tween.core
    hard.core
    hard.physics
    hard.input)
  (import [SimpleTiledWFC]))

(def player (atom nil))

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
        board (clone! :board loc)]
    board))

(defn handle-input [o]
  (let [body (->rigidbody o)]
  (if (key? "w") (force! body  40  0 0))
  (if (key? "s") (force! body -40  0 0))
  (if (key? "a") (torque! body 0 -20 0))
  (if (key? "d") (torque! body 0  20 0))
  (Input/GetAxis "Vertical")))

(defn make-level []
  (clear-cloned!)
  (make-park 10 10)
  (reset! player (make-player (v3 5 5 5)))
  (hook+ @player :update #'game.core/handle-input)
  )



'(timeline* :loop
  (wait 1.0)
  #(do (make-level) false))

'(make-level) 
