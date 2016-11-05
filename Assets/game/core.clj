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

(defn update-cam [o]
  (let [target (v3+ (.TransformPoint (.transform @player) (v3 -10 0 0))
                    (v3 0 10 0))]
    (position! o (lerp o target 0.1))
    (lerp-look! o @player 0.2)))

(defn gizmo-cam [o]
  (gizmo-color (color 1 0 0))
  (gizmo-line (>v3 o) (>v3 @player)))

(defn make-player [loc]
  (let [loc (or loc (v3 0 10 0))
        board (clone! :board2 loc)
        cam (clone! :skatecam (v3+ loc (v3 0 5 10)))]
       (hook+ cam :update #'game.core/update-cam)
       (hook+ cam :on-draw-gizmos #'game.core/gizmo-cam)
    board))

(defn handle-input [o]
  (let [body (->rigidbody o)]
   (if (key? "w") (force! body 0 0  6000))
   (if (key? "s") (force! body 0 0 -6000))
   (if (key? "a") (torque! body 0 -4000 0))
   (if (key? "d") (torque! body 0  4000 0))
   (Input/GetAxis "Vertical")))

(defn make-level []
  (clear-cloned!)
  (make-park 14 14)
  (reset! player (make-player (v3 10 5 10)))
  (hook+ @player :update #'game.core/handle-input))
  