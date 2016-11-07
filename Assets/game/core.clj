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
(def steerage (atom 0.0))
(def wheelmap (atom nil))

(defn text! [o s] (set! (.text (cmpt o UnityEngine.UI.Text)) s))

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
                    (v3 0 8 0))]
    (position! o (lerp o target (∆ 4)))
    (lerp-look! o @player (∆ 6))))

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



(defn steer [n col]
  (dorun (map #(set! (.steerAngle %) (float n)) col)))
(defn motor [n col]
  (dorun (map #(set! (.motorTorque %) (float n)) col)))

(defn make-player [loc]
  (let [loc (or loc (v3 0 10 0))
        board (clone! :player/board loc)
        cam (clone! :player/skatecam (v3+ loc (v3 0 5 7)))]
    (reset! wheelmap 
      {:front [(->wheel (child-named board "front-right"))
               (->wheel (child-named board "front-left"))]
       :rear  [(->wheel (child-named board "rear-right"))
               (->wheel (child-named board "rear-left"))]})
    (hook+ cam :update #'game.core/update-cam)
    (hook+ cam :on-draw-gizmos #'game.core/gizmo-cam)
    board))

(defn fall-check [o]
  (let [p (>v3 o)]
    (if (< (.y p) -50)
      (position! o (v3 (.x p) 20 (.z p))))))

(defn turn-limit [n]
  (- 30 (* 8.3 (Mathf/Log (Mathf/Abs (float n))))))

(defn handle-input [o]
  (let [body (->rigidbody o)
        mass (.mass (->rigidbody o))
        dspeed (∆ 30)
        wheels @wheelmap
        motorforce (* mass dspeed)
        local-velocity (.InverseTransformPoint (.transform o) 
                                        (v3+ (>v3 o)(.velocity body)))
        forward-speed (.z local-velocity)
        max-speed 10.0
        max-turn (max 14 (min 42 (turn-limit forward-speed)))]

  (fall-check o)

  (text! (the debug) 
    (str :steerage "  " @steerage "\n"
      :forward-speed " " forward-speed "\n"
      :max-turn max-turn))

  (cond 
    (and (key? "w") (wheel-contact? o)) 
    (if (< forward-speed max-speed)
        (do (force! body 0 0 (* mass dspeed 20))
            (motor motorforce (:rear wheels))
            (motor motorforce (:front wheels))))

    (and (key? "s") (wheel-contact? o)) 
    (if (> forward-speed (- max-speed))
        (do (force! body 0 0 (* mass dspeed -20))
            (motor (- motorforce) (:rear wheels))
            (motor (- motorforce) (:front wheels))))
    
    :else
    (do (motor 0 (:rear wheels))
        (motor 0 (:front wheels))))
  (cond 
    (key? "a") 
    (if (wheel-contact? o)
        (do (swap! steerage #(max (- max-turn) (- % (∆ 35))))
            (steer (- @steerage) (:rear wheels))
            (steer @steerage (:front wheels)))
        (torque! body 0 (* mass dspeed -24) 0))
    (key? "d") 
    (if (wheel-contact? o)
        (do (swap! steerage #(min max-turn (+ % (∆ 35))))
            (steer (- @steerage) (:rear wheels))
            (steer @steerage (:front wheels)))
        (torque! body 0 (* mass dspeed 24) 0))
    :else
    (do (swap! steerage #(* % 0.95))
        (steer (- @steerage) (:rear wheels))
        (steer @steerage (:front wheels))))
  (if (key? "e") (torque! body 0 0 (* mass -6)))
  (if (key? "q") (torque! body 0 0 (* mass  6)))
  (if (and (key? "tab") (upsidedown? o)) 
    (torque! body 0 0 (* mass  60)))
  (when (and (key-down? "space") (wheel-contact? o)) 
    (torque! body (* mass  -20) 0 0)
    (force! body 0 (* mass 285) 0))
  (if (wheel-contact? o) (force! body 0 (* mass -9) 0))
  (Input/GetAxis "Vertical")))

(defn make-level []
  (clear-cloned!)
  (make-park 20 20)
  (reset! player (make-player (v3 10 10 10)))
  (hook- (the board) :update #'game.core/handle-input)
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
  #(do (message "brb   5min") nil))

'(make-level)

