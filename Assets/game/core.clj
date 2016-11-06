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
  (gizmo-line (>v3 o) (>v3 @player))
  (gizmo-color (color 0 1 1))
  (gizmo-line  (>v3 @player) (.TransformPoint (.transform @player) (v3 0 -1.0 0))))

(defn upsidedown? [o]
  (and (hit (>v3 o) (.TransformDirection (.transform o) (v3 0 1 0)))
       #_(not (hit (>v3 o) (.TransformDirection (.transform o) (v3 0 -1 0)))) ))

(defn wheel-contact? [o]
  (and (first (range-hits (>v3 o) (.TransformDirection (.transform o) (v3 0 -1 0)) 1.0))))

(defn ->wheel [o] (cmpt o UnityEngine.WheelCollider))

(defn wheelmap [o]
  {:front [(->wheel (child-named o "front-right"))
           (->wheel (child-named o "front-left"))]
   :rear  [(->wheel (child-named o "rear-right"))
           (->wheel (child-named o "rear-left"))]})

(defn steer [n col]
  (dorun (map #(set! (.steerAngle %) (float n)) col)))
(defn motor [n col]
  (dorun (map #(set! (.motorTorque %) (float n)) col)))

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
        wheels (wheelmap o)
        motorforce (* mass 1)]
  (cond 
    (and (key? "w") (wheel-contact? o)) 
    (do (force! body 0 0 (* mass  10))
        (motor motorforce (:rear wheels))
        (motor motorforce (:front wheels)))
    (and (key? "s") (wheel-contact? o)) 
    (do (force! body 0 0 (* mass  -10))
        (motor (- motorforce) (:rear wheels))
        (motor (- motorforce) (:front wheels)))
    
    :else
    (do (motor 0 (:rear wheels))
        (motor 0 (:front wheels))))
  (cond 
    (key? "a") 
    (do (steer 20 (:rear wheels))
        (steer -20 (:front wheels)))
    (key? "d") 
    (do (steer -20 (:rear wheels))
        (steer 20 (:front wheels)))
    :else
    (do (steer 0 (:rear wheels))
        (steer 0 (:front wheels))))
  (if (key? "e") (torque! body 0 0 (* mass -4)))
  (if (key? "q") (torque! body 0 0 (* mass  4)))
  (if (and (key? "tab") (upsidedown? o)) 
    (torque! body 0 0 (* mass  60)))
  (when (and (key-down? "space") (wheel-contact? o)) 
    (torque! body (* mass  -2) 0 0)
    (force! body 0 (* mass 45) 0))
  (if (wheel-contact? o) (force! body 0 (* mass -4) 0))
  (Input/GetAxis "Vertical")))

(defn make-level []
  (clear-cloned!)
  (make-park 16 16)
  (reset! player (make-player (v3 10 10 10)))
  (hook+ @player :update #'game.core/handle-input)
  (clone! :EventSystem)
  (clone! :Canvas))






(defn message [s]
  (destroy (the message))
  (let [cam (the skatecam)
        o (clone! :message (.TransformPoint (.transform cam) (v3 0 0 10)))
        txt (cmpt (child-named o "text") UnityEngine.TextMesh)]
    (parent! o cam)
    (set! (.text txt) s)
    (timeline [#(lerp-look! o cam (float 0.2))])))


'(timeline* :loop
  (wait 3.0)
  #(do (make-level) nil)
  (wait 0.1)
  #(do (message "brb   6") nil))

'(make-level)
