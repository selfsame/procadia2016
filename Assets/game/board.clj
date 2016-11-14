(ns game.board
  (use 
    arcadia.core
    arcadia.linear
    tween.core
    hard.core
    hard.seed
    hard.physics
    hard.input
    game.tricks)
  (:require
    game.data
    tween.core
    game.ui))

(def steerage (atom 0.0))
(def wheelmap (atom nil))
(def ragmap (atom nil))
(def IN-AIR (atom false))
(def TOUCHING (atom false))
(def STANDING (atom true))
(def TRICK-STREAK (atom []))
(def CRASH-COUNT (atom 0))

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

(defn update-trick-ui []
  (try 
   (let [tricklist (apply str (map (comp #(str % "\n") first) @TRICK-STREAK))
         multiplier-color (cond
                           (< @game.data/trick-multiplier 6) "white"
                           (< @game.data/trick-multiplier 15) "orange"
                           (< @game.data/trick-multiplier 20) "purple"
                           (< @game.data/trick-multiplier 30) "magenta"
                           :else "red")]
     (text! (the tricks) tricklist)
     (text! (the tricks-bg) tricklist)
     (text! (the score) (str @game.data/skater-name ": "
                         @game.data/trick-score " <color=\"" multiplier-color
                         "\">x" @game.data/trick-multiplier "</color>")))
   (catch Exception e (log e))))

(defn message [s]
  (destroy (the message))
  (let [cam (the skatecam)
        o (clone! :message (.TransformPoint (.transform cam) (v3 0 0 10)))
        txt (cmpt (child-named o "text") UnityEngine.TextMesh)]
    (parent! o cam)
    (set! (.text txt) s)
    (update-trick-ui)
    (timeline [#(lerp-look! o cam (float 0.2))])))

(defn tally-tricks [o]
 (let [total (v3* (state o :total-euler) 1/90)
       x (int (.x total))
       y (int (.y total))
       z (int (.z total))]
   (when (or (not (zero? x))
           (not (zero? y))
           (not (zero? z)))
    (swap! TRICK-STREAK #(conj % [(trick [x y z]) (trick-score [x y z])]))
    (let [multiplier (cond
                      (< (count @TRICK-STREAK) 3) 1
                      (< (count @TRICK-STREAK) 6) 2
                      (< (count @TRICK-STREAK) 10) 3
                      (< (count @TRICK-STREAK) 15) 4
                      (< (count @TRICK-STREAK) 20) 5
                      (< (count @TRICK-STREAK) 30) 6
                      (< (count @TRICK-STREAK) 40) 7)]
     (reset! game.data/trick-multiplier multiplier)
     (reset! game.data/trick-score (+ @game.data/trick-score (* multiplier (trick-score [x y z]))))
     (update-trick-ui)
     (game.ui/tween-rect-scale (the score) (v3 1.2 1.2 1) 0.2)))))

(defn detach-skater [o]
  (let [legr (:leg-lower-r @ragmap)
        legl (:leg-lower-l @ragmap)]
    (swap! CRASH-COUNT inc)
    (reset! TRICK-STREAK [])
    (reset! game.data/trick-score 0)
    (reset! game.data/trick-multiplier 1)
    (cmpt- legr UnityEngine.FixedJoint)
    (cmpt- legl UnityEngine.FixedJoint)
    (reset! STANDING false)
    (timeline [
               (wait 4.0)
               #(do (update-trick-ui)
                    (@game.data/respawn-fn) nil)])))



(defn upsidedown? [o]
  (and (hit (>v3 o) (.TransformDirection (.transform o) (v3 0 1 0)))
       #_(not (hit (>v3 o) (.TransformDirection (.transform o) (v3 0 -1 0))))))

(defn wheel-contact? [o]
  (if (and (first 
            (range-hits (>v3 o) (.TransformDirection (.transform o) (v3 0 -1 0)) 0.9)))
    true false))

(defn fall-check [o]
  (let [p (>v3 o)]
    (when (or (< (.y p) -50) (> (.y p) 300))
      (position! o (v3 100 20 100))
      (set! (.velocity (->rigidbody o)) (v3 0)))))

(defn turn-limit [n]
  (- 30 (* 8.3 (Mathf/Log (Mathf/Abs (float n))))))

(defn delta-quat [a b]
  (q* (Quaternion/Inverse a) b))

(defn ecl [n] (if (> n 180) (- n 360) (if (< n -180) (+ n 360) n)))

(defn delta-euler [a b]
  (let [d (v3- b a)
        x (.x d) y (.y d) z (.z d)]
    (v3 (ecl x)(ecl y)(ecl z))))

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
    (when-not (state o :speed) (set-state! o :speed forward-speed))
    (fall-check o)
    (reset! IN-AIR (and (not grounded) (not @TOUCHING)))
    (if (and (not was-in-air) @IN-AIR) 
      (do (message "")
        (set-state! o :total-euler (v3))))
    (if (and was-in-air 
            (not @IN-AIR) 
            #_(or (not @TOUCHING)
                grounded)) 
      (do (tally-tricks o)))

    (when (< 8.0 (abs (- (abs forward-speed) (abs (state o :speed)))))
     (log (abs (- (abs forward-speed) (abs (state o :speed)))))
     (detach-skater o))

    (text! (the debug) ""
      #_(str :steerage "   " @steerage "\n"
         :forward-speed "  " forward-speed "\n"
         :max-turn "  " max-turn "\n"
         :in-air "  " @IN-AIR "\n"
         :touching "  " @TOUCHING "\n"
         :angular-vel "  " (.angularVelocity body) "\n"))
      
    (update-state! o :total-euler 
          #(v3+ % (delta-euler (state o :rotation) rotation)))
    (set-state! o :rotation (.eulerAngles (.transform o)))
    (set-state! o :speed forward-speed)


    (when (key-down? "escape") 
     (destroy-immediate @game.data/player)
     (@game.data/selection-fn))

    (when (key-up? "l")
     (reset! game.data/recording? (not @game.data/recording?)))

    (when @STANDING 
     (cond 
      (or (key? "w")
          (joy-up?))
      (if grounded 
       (if (< forward-speed max-speed)
          (do (force! body 0 0 (* mass dspeed 20))
              (motor motorforce (:rear wheels))
              (motor motorforce (:front wheels))))
       (torque! body (* mass dspeed 10) 0 0))

      (or (key? "s")
          (joy-down?))
      (if grounded 
       (if (> forward-speed (- max-speed))
          (do (force! body 0 0 (* mass dspeed -20))
              (motor (- motorforce) (:rear wheels))
              (motor (- motorforce) (:front wheels))))
       (torque! body (* mass dspeed -10) 0 0))
    
      :else
       (do (motor 0 (:rear wheels))
           (motor 0 (:front wheels))))
     (cond 
       (or (key? "a")
           (joy-left?))
       (if grounded
           (do (swap! steerage #(max (- max-turn) (- % (∆ 45))))
               (steer (- @steerage) (:rear wheels))
               (steer @steerage (:front wheels)))
           (torque! body 0 (* mass dspeed -24) 0))
       (or (key? "d")
           (joy-right?))
       (if grounded
           (do (swap! steerage #(min max-turn (+ % (∆ 45))))
               (steer (- @steerage) (:rear wheels))
               (steer @steerage (:front wheels)))
           (torque! body 0 (* mass dspeed 24) 0))
       :else
       (do (swap! steerage #(* % 0.95))
           (steer (- @steerage) (:rear wheels))
           (steer @steerage (:front wheels))))
     (if (or (key? "e")
             (button? "RB"))
       (torque! body 0 0 (* mass dspeed -6)))
     (if (or (key? "q")
             (button? "LB"))
       (torque! body 0 0 (* mass dspeed 6)))
     (if (and (or (key? "tab")
                  (button? "X"))
              (upsidedown? o))
       (torque! body 0 0 (* mass  dspeed 60)))
     (when (and (or (key-down? "space")
                    (button? "A"))
                grounded)
       (torque! body (* mass  -20) 0 0)
       (force! body 0 (* mass 580) 0))
     (if grounded (force! body 0 (* mass dspeed -25) 0))
     (global-force! (->rigidbody (:spine @ragmap)) 0 (* dspeed 650) 0))
    ::gravity
    (global-force! body 0 (* mass dspeed -8) 0))
   (catch Exception e (log e))))
