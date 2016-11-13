(ns game.board
  (use 
    arcadia.core
    arcadia.linear
    tween.core
    hard.core
    hard.seed
    hard.physics
    hard.input)
  (:require
    game.data))

(def steerage (atom 0.0))
(def wheelmap (atom nil))
(def ragmap (atom nil))
(def IN-AIR (atom false))
(def TOUCHING (atom false))
(def STANDING (atom true))

(defn ragbody-map [] {
  :hips         (the Hips)
  :spine        (the Spine)
  :head         (the Bone.001)
  :arm-upper-l  (the ArmUpper.L)
  :arm-lower-l  (the ArmLower.L)
  :leg-upper-l  (the LegUpper.L)
  :leg-lower-l  (the LegLower.L)
  :arm-upper-r  (the ArmUpper.R)
  :arm-lower-r  (the ArmLower.R)
  :leg-upper-r  (the LegUpper.R)
  :leg-lower-r  (the LegLower.R)})

(defn ->wheel [o] (cmpt o UnityEngine.WheelCollider))

(defn record-wheels [board]
  (reset! wheelmap 
    {:front [(->wheel (child-named board "front-right"))
             (->wheel (child-named board "front-left"))]
     :rear  [(->wheel (child-named board "rear-right"))
             (->wheel (child-named board "rear-left"))]}))

(defn text! [o s] (set! (.text (cmpt o UnityEngine.UI.Text)) s))

(defn message [s]
  (destroy (the message))
  (let [cam (the skatecam)
        o (clone! :message (.TransformPoint (.transform cam) (v3 0 0 10)))
        txt (cmpt (child-named o "text") UnityEngine.TextMesh)]
    (parent! o cam)
    (set! (.text txt) s)
    (timeline [#(lerp-look! o cam (float 0.2))])))


(defn detach-skater [o]
  (let [legr (:leg-lower-r @ragmap)
        legl (:leg-lower-l @ragmap)]
    ;(set! (.connectedBody (cmpt legr UnityEngine.FixedJoint)) nil)
    (cmpt- legr UnityEngine.FixedJoint)
    (cmpt- legl UnityEngine.FixedJoint)
    (reset! STANDING false)
    (timeline [
      (wait 4.0)
      #(do (@game.data/respawn-fn) nil)])))



(defn upsidedown? [o]
  (and (hit (>v3 o) (.TransformDirection (.transform o) (v3 0 1 0)))
       #_(not (hit (>v3 o) (.TransformDirection (.transform o) (v3 0 -1 0)))) ))

(defn wheel-contact? [o]
  (if (and (first 
            (range-hits (>v3 o) (.TransformDirection (.transform o) (v3 0 -1 0)) 0.9)))
    true false))




(defn fall-check [o]
  (let [p (>v3 o)]
    (if (< (.y p) -50)
      (position! o (v3 (.x p) 20 (.z p))))))

(defn turn-limit [n]
  (- 30 (* 8.3 (Mathf/Log (Mathf/Abs (float n))))))

(defn delta-quat [a b]
  (q* (Quaternion/Inverse a) b))

(defn ecl [n] (if (> n 180) (- n 360) (if (< n -180) (+ n 360) n)))

(defn delta-euler [a b]
  (let [d (v3- b a)
        x (.x d) y (.y d) z (.z d)]
    (v3 (ecl x)(ecl y)(ecl z))))

(defn tally-tricks [o]
  (let [total (v3* (state o :total-euler) 1/90)
        x (int (.x total))
        y (int (.y total))
        z (int (.z total))]
    (if (or (not (zero? x))
            (not (zero? y))
            (not (zero? z)))
     (timeline [
                #(do (message (str [x y z])) nil) 
                (wait 1.0) 
                #(do (destroy (the message)) nil)]))))

(defn steer [n col]
  (dorun (map #(set! (.steerAngle %) (float n)) col)))
(defn motor [n col]
  (dorun (map #(set! (.motorTorque %) (float n)) col)))

(defn handle-input [o]
  (try
  (let [body (->rigidbody o)
        grounded (wheel-contact? o)
        was-in-air @IN-AIR
        rotation (.eulerAngles (.transform o))
        mass (.mass (->rigidbody o))
        dspeed (∆ 30)
        wheels @wheelmap
        motorforce (* mass dspeed)
        local-velocity (.InverseTransformPoint (.transform o) 
                                        (v3+ (>v3 o)(.velocity body)))
        forward-speed (.z local-velocity)
        max-speed 15.0
        max-turn (max 16 (min 42 (turn-limit forward-speed)))]

   (fall-check o)
   (reset! IN-AIR (and (not grounded) (not @TOUCHING)))
   (if (and (not was-in-air) @IN-AIR) 
     (do (message "")
       (set-state! o :total-euler (v3))))
   (if (and was-in-air 
           (not @IN-AIR) 
           #_(or (not @TOUCHING)
               grounded) ) 
     (do (tally-tricks o)))

   (text! (the debug) 
     (str :steerage "   " @steerage "\n"
       :forward-speed "  " forward-speed "\n"
       :max-turn "  " max-turn "\n"
       :in-air "  " @IN-AIR "\n"
       :touching "  " @TOUCHING "\n"
       :angular-vel "  " (.angularVelocity body) "\n"))
      ;:delta-euler "  " (delta-euler (state o :rotation) rotation) "\n"
      ;:total-euler "  " (state o :total-euler) "\n"
      
   (update-state! o :total-euler 
         #(v3+ % (delta-euler (state o :rotation) rotation)))

   (set-state! o :rotation (.eulerAngles (.transform o)))
   (if (key-down? "escape") (@game.data/selection-fn))
   (when @STANDING 
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
     (if grounded
         (do (swap! steerage #(max (- max-turn) (- % (∆ 45))))
             (steer (- @steerage) (:rear wheels))
             (steer @steerage (:front wheels)))
         (torque! body 0 (* mass dspeed -24) 0))
     (key? "d") 
     (if grounded
         (do (swap! steerage #(min max-turn (+ % (∆ 45))))
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
   (when (and (key-down? "space") grounded) 
     (torque! body (* mass  -20) 0 0)
     (force! body 0 (* mass 500) 0))
   (if grounded (force! body 0 (* mass dspeed -25) 0))
   (global-force! (->rigidbody (:spine @ragmap)) 0 650 0))
   ::gravity
   (global-force! body 0 (* mass -8) 0))
  (catch Exception e (log e))))