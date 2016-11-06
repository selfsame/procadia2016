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
        wfc (.AddComponent o (type (SimpleTiledWFC.)))]
    (set! (.xmlpath wfc) "skaters.xml")
    (set! (.gridsize wfc) (int 2))
    (timeline [
      (wait 0.1)
      #(do (local-scale! o (v3 4)) false)])
    (set! (.width wfc) (int w))
    (set! (.depth wfc) (int h)) o))

(defn update-cam [o]
  (let [target (v3+ (.TransformPoint (.transform @player) (v3 0 0 -10))
                    (v3 0 10 0))]
    (position! o (lerp o target 0.1))
    (lerp-look! o @player 0.2)))

(defn gizmo-cam [o]
  (gizmo-color (color 1 0 0))
  (gizmo-line (>v3 o) (>v3 @player)))

(defn upsidedown? [o]
  (and (hit (>v3 o) (.TransformDirection (.transform o) (v3 0 1 0)))
       (not (hit (>v3 o) (.TransformDirection (.transform o) (v3 0 -1 0))))))

(defn ->wheel [o] (cmpt o UnityEngine.WheelCollider))

(defn wheelmap [o]
  {:front [(->wheel (child-named o "front-right"))
           (->wheel (child-named o "front-left"))]
   :rear  [(->wheel (child-named o "rear-right"))
           (->wheel (child-named o "rear-left"))]})

(defn make-player [loc]
  (let [loc (or loc (v3 0 10 0))
        board (clone! :player/board loc)
        cam (clone! :player/skatecam (v3+ loc (v3 0 5 10)))]
        (hook+ cam :update #'game.core/update-cam)
        (hook+ cam :on-draw-gizmos #'game.core/gizmo-cam)
    board))

(defn handle-input [o]
  (let [body (->rigidbody o)
        mass (.mass (->rigidbody o))
        wheels (wheelmap o)]
  (if (key? "w") (force!  body 0 0 (* mass  20)))
  (if (key? "s") (force!  body 0 0 (* mass -20)))
  (cond 
    (key? "a") 
    (mapv #(set! (.steerAngle %) (float 20)) (flatten ((juxt :rear) wheels)))
    (key? "d") 
    (mapv #(set! (.steerAngle %) (float -20)) (flatten ((juxt :rear) wheels)))
    :else
    (mapv #(set! (.steerAngle %) (float 0)) (flatten ((juxt :rear) wheels))))
  ;(if (key? "d") (torque! body 0 (* mass  12) 0))
  (if (and (key? "space") (upsidedown? o)) 
    (torque! body 0 0 (* mass  20)))
  (Input/GetAxis "Vertical")))

(defn make-level []
  (clear-cloned!)
  (make-park 14 14)
  (reset! player (make-player (v3 10 10 10)))
  (hook+ @player :update #'game.core/handle-input))



'(timeline* :loop
  (wait 3.0)
  #(do (make-level) false))

'(make-level)

