(ns game.gif
 (require [arcadia.core :as a]
          [hard.core :as hard]
          [tween.core :refer :all]
          [game.data :as data])
 (import [UnityEngine RenderTexture Camera Texture2D TextureFormat Rect Application Time Canvas]
         [GifEncoder AnimatedGifEncoder]
         GameUtils))

(def gif-cam (atom nil))
(def gif-tex (atom nil))
(def gif-encoder (atom nil))

(defn setup-encoder []
 (let [encoder (AnimatedGifEncoder. (GameUtils/CreateDocumentFile))]
  (reset! gif-encoder encoder)))

(defn setup [w h b]
 (let [render-texture (RenderTexture. w h b)
       gif-canvas (hard/clone! :ui/gif-canvas)
       cam-go (hard/clone! :gif-cam)
       cam (a/cmpt cam-go Camera)]
  (reset! gif-cam cam)
  (reset! gif-tex render-texture)
  (setup-encoder)
  (set! (.targetTexture cam) render-texture)
  (set! (.worldCamera (a/cmpt gif-canvas Canvas)) cam) 
  (a/hook+ cam-go :update
   (fn [go]
    (let [skatecam (a/object-tagged "skatecam")]
     (if (false? (a/null-obj? skatecam))
      (do
       (set! (.. go transform position) (.. (a/object-tagged "skatecam") transform position))
       (set! (.. go transform rotation) (.. (a/object-tagged "skatecam") transform rotation)))))))
  (.SetDelay @gif-encoder (float (/ 1000 24)))
  (add-watch data/recording? nil
   (fn [_ _ _ new-state]
    (if new-state
     (start-recording))))))

(defn start-recording []
 (let [start-time Time/time
       recording-canvas (hard/clone! :ui/recording-canvas)]
  (set! (.worldCamera (a/cmpt recording-canvas Canvas)) (a/cmpt (a/object-tagged "skatecam") Camera))
  (timeline* :loop
   (wait Time/deltaTime)
   #(do
     (if (or
          (false? @data/recording?)
          (>= (- Time/time start-time) 15))
      (do
       (.Finish @gif-encoder)
       (reset! data/recording? false)
       (setup-encoder)
       (a/destroy recording-canvas)
       (abort!)))
     (.Render @gif-cam)
     (set! RenderTexture/active @gif-tex)
     (let [frame-texture (Texture2D. (.width @gif-tex) (.height @gif-tex) (TextureFormat/RGB24) false)
           gif-rect (Rect. 0 0 (.width @gif-tex) (.height @gif-tex))]
      (.ReadPixels frame-texture gif-rect 0 0)
      (.AddFrame @gif-encoder frame-texture)
      (a/destroy frame-texture))
     nil))))

'(setup 800 480 24)
'(start-recording)
