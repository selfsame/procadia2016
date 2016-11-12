(ns game.gif
 (require [arcadia.core :as a]
          [hard.core :as hard]
          [tween.core :refer :all])
 (import [UnityEngine RenderTexture Camera Texture2D TextureFormat Rect Application Time]
         [GifEncoder AnimatedGifEncoder]
         GameUtils))

(def gif-cam (atom nil))
(def gif-tex (atom nil))
(def gif-encoder (atom nil))

(defn setup [w h b]
 (let [render-texture (RenderTexture. w h b)
       encoder (AnimatedGifEncoder. (GameUtils/CreateDocumentFile))
       cam-go (hard/clone! :gif-cam)
       cam (a/cmpt cam-go Camera)]
  (reset! gif-cam cam)
  (reset! gif-tex render-texture)
  (reset! gif-encoder encoder)
  (set! (.enabled cam) false)
  (set! (.targetTexture cam) render-texture)
  (a/hook+ cam-go :update
   (fn [go]
    (set! (.. go transform position) (.. Camera/main transform position))
    (set! (.. go transform rotation) (.. Camera/main transform rotation))))
  (.SetDelay encoder (float (/ 1000 30)))))

(defn start-recording []
 (let [start-time Time/time]
  (timeline* :loop
   #(do
     (if (>= (- Time/time start-time) 5)
      (do
       (.Finish @gif-encoder)
       (abort!)))
     (.Render @gif-cam)
     (set! RenderTexture/active @gif-tex)
     (let [frame-texture (Texture2D. (.width @gif-tex) (.height @gif-tex) (TextureFormat/RGB24) false)
           gif-rect (Rect. 0 0 (.width @gif-tex) (.height @gif-tex))]
      (.ReadPixels frame-texture gif-rect 0 0)
      (.AddFrame @gif-encoder frame-texture)
      (a/destroy frame-texture))))))

'(setup 800 480 24)
'(start-recording)
