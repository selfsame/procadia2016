(ns game.gif
 (require [arcadia.core :as a])
 (import [UnityEngine RenderTexture Camera Texture2D TextureFormat Rect Application]
         [GifEncoder AnimatedGifEncoder]))

(def gif-cam (atom nil))
(def gif-tex (atom nil))
(def gif-encoder (atom nil))

(defn setup [w h b cam-go]
 (let [render-texture (RenderTexture. w h b)
       encoder (AnimatedGifEncoder. "C:\\Users\\polyr_000\\Desktop\\skate.gif")
       cam (a/cmpt cam-go Camera)]
  (reset! gif-cam cam)
  (reset! gif-tex render-texture)
  (reset! gif-encoder encoder)
  (set! (.enabled cam) false)
  (set! (.targetTexture cam) render-texture)
  (.SetDelay encoder (float (/ 1000 30)))))

(defn start-recording []
 (a/hook+ @gif-cam :update :recording
  (fn [go]
   (let [frame-texture (Texture2D. (.width @gif-tex) (.height @gif-tex) (TextureFormat/RGB24) false)
         gif-rect (Rect. 0 0 (.width @gif-tex) (.height @gif-tex))]
    (.ReadPixels frame-texture gif-rect 0 0)
    (.AddFrame @gif-encoder frame-texture)
    (a/destroy frame-texture)))))

(defn stop-recording []
 (.Finish @gif-encoder)
 (a/hook- @gif-cam :update :recording))
   
'(setup 800 480 24 (a/object-named "gif-cam"))
'(start-recording)
'(stop-recording)