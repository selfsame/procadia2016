(ns game.core
  (use 
    arcadia.core
    arcadia.linear
    tween.core
    hard.core
    hard.seed
    hard.physics
    hard.animation
    hard.input)
  (require [game.data :as data]
    human.core)
  (import [SimpleTiledWFC]
          [Training]
          GameUtils))

(def park-scale 4.3)
(def park-size 20)
(def city-scale 4.3)
(def city-size 30)
(def player (atom nil))
(def steerage (atom 0.0))
(def wheelmap (atom nil))
(def ragmap (atom nil))
(def IN-AIR (atom false))
(def TOUCHING (atom false))


(defn text! [o s] (set! (.text (cmpt o UnityEngine.UI.Text)) s))

(defn message [s]
  (destroy (the message))
  (let [cam (the skatecam)
        o (clone! :message (.TransformPoint (.transform cam) (v3 0 0 10)))
        txt (cmpt (child-named o "text") UnityEngine.TextMesh)]
    (parent! o cam)
    (set! (.text txt) s)
    (timeline [#(lerp-look! o cam (float 0.2))])))

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

#_(defn update-cam [o]
   (let [body (->rigidbody @player)
         local-velocity (.InverseTransformPoint (.transform @player) 
                         (v3+ (>v3 @player) (.velocity body)))
         vel-offset (v3* local-velocity -1)
         z-offset (v3 0 0 15)
         offset (if true;(> (.magnitude vel-offset) 1.0) 
                 (v3* (.normalized vel-offset) -15) 
                 z-offset)
         target (v3+ (.TransformPoint (.transform @player) offset)
                     (v3 0 8 0))]
     (position! o (lerp o target (∆ 4)))
     (lerp-look! o @player (∆ 6))) )

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
  (if (and (first 
            (range-hits (>v3 o) (.TransformDirection (.transform o) (v3 0 -1 0)) 0.9)))
    true false))

(defn ->wheel [o] (cmpt o UnityEngine.WheelCollider))



(defn make-head []
  (destroy (the infihead))
  (destroy (the neck))
  (let [head (human.core/make-head)
        rag-head (the Bone.001)
        rag-neck (the Bone.002)
        neck (child-named head "neck")]
    (local-scale! head (v3 1))
    (rotation! head (.rotation (.transform rag-head)))
    (rotate! head (v3 -90 0 0))
    (position! head (.TransformPoint (.transform rag-head) (v3 0 0.0015 0)))
    (parent! head rag-head)
    (rotate! neck (v3 0 0 0))
    (position! neck (>v3 rag-neck))
    (parent! neck rag-neck)
    (local-scale! neck (v3 0.2))
    (timeline* :loop
      #(do (cross-fade head (str (srand-nth human.core/emotions)) 0.5))
      (wait (?f 0.3 1.0)))
    head))

'(make-head)

(defn make-player [loc]
  (let [loc (or loc (v3 0 10 0))
        board (clone! :player/board loc)
        cam (clone! :player/skatecam (v3+ loc (v3 0 5 7)))]
    (reset! wheelmap 
      {:front [(->wheel (child-named board "front-right"))
               (->wheel (child-named board "front-left"))]
       :rear  [(->wheel (child-named board "rear-right"))
               (->wheel (child-named board "rear-left"))]})
    (reset! ragmap (ragbody-map))
    (hook+ cam :update #'game.core/update-cam)
    (hook+ cam :on-draw-gizmos #'game.core/gizmo-cam)
    (timeline [(wait 0.01) 
               #(do (make-head) 
                 (reset! ragmap (ragbody-map))
                 nil)])
    board))

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
   (global-force! (->rigidbody (:spine @ragmap)) 0 650 0)
  ;(global-force! (->rigidbody (the "ArmUpper.L")) 0 60 0)
  ;(global-force! (->rigidbody (the "ArmLower.L")) 0 60 0)
  
  ;(global-force! (->rigidbody (the "ArmLower.R")) 0 60 0)
  
   ::gravity
   (global-force! body 0 (* mass -8) 0)
   (Input/GetAxis "Vertical")))

(defn make-park [w h]
  (let [o (clone! :maps/autopark (v3 26 4 26))
        wfc (.AddComponent o (type (SimpleTiledWFC.)))]
   (set! (.xmlpath wfc) "skaters.xml")
   (set! (.gridsize wfc) (int 2))
   (timeline [
              (wait 0.1)
              #(do (local-scale! o (v3 park-scale)) false)])
   (set! (.width wfc) (int w))
   (set! (.depth wfc) (int h)) o))

(defn make-city [w h]
 (let [o (clone! :maps/autocity (v3 0 0 0))
       sample (clone! :maps/city-sample (v3 0 -500 0))
       trainer (.AddComponent sample (type (Training.)))
       wfc (.AddComponent o (type (OverlapWFC.)))]
  (set! (.gridsize trainer) (int 10))
  (set! (.width trainer) (int 20))
  (set! (.depth trainer) (int 14))
  (set! (.training wfc) trainer)
  (set! (.gridsize wfc) (int 10))
  (set! (.width wfc) (int w))
  (set! (.depth wfc) (int h))
  (set! (.periodicInput wfc) true)
  (set! (.incremental wfc) true)
  ;(set! (.iterations wfc) (int 1))
  ;(.Generate wfc)
  ;(.Run wfc)
  wfc))



(defn prune-city-center [city]
 (GameUtils/PruneMiddle city 4 4))

(defn make-level []
  (clear-cloned!)
  (destroy! (the Camera))
  (clone! :Canvas)
  (make-park park-size park-size)
  (let [citywfc (make-city city-size city-size)
        city (.gameObject citywfc)]
   (timeline [
    (wait 0.1)
    #(do 
      (prune-city-center city) 
      (local-scale! city (v3 city-scale))
      (position! city 
        (v3 (* city-size -4 city-scale) 0 
            (* city-size -4 city-scale)))
      (cmpt- city (type citywfc))
      false)]))          
  (reset! player (make-player 
                  (v3 (* park-size park-scale)
                    (* 6 park-scale) 
                    (* park-size park-scale))))

 (reset! data/player-spawned? true)
 (set-state! @player :total-euler (v3))
 (set-state! @player :rotation (v3))
 (hook-clear @player :update)
 (hook-clear @player :on-collision-enter)
 (hook-clear @player :on-collision-exit)
 (hook+ @player :update #'game.core/handle-input)
 (hook+ @player :on-collision-enter #(do %1 %2 (reset! TOUCHING true)))
 (hook+ @player :on-collision-exit #(do %1 %2 (reset! TOUCHING false))))

'(timeline* :loop
  (wait 3.0)
  #(do (make-level) nil)
  (wait 0.1)
  #(do (message "brb   7min") nil))

'(reset! game.data/seed (hash "selfsame"))
'(make-level)
