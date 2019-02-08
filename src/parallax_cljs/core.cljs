(ns parallax-cljs.core
    (:require ))

(enable-console-print!)

(defonce app-state (atom {:container (.querySelector js/document ".vis-container")
                          :buildings []
                          :layers []
                          :base-speed 5}))

(defn draw-rect [ctx x y width height color]
  (set! (.-fillStyle ctx) color)
  (.beginPath ctx)
  (.rect ctx x y width height)
  (.fill ctx)
  )

(defn draw-building [building]
  (let [ctx (:ctx @app-state)]
    (draw-rect ctx (:x building) (:y building) (:width building) (:height building) (:color building))))

(defn create-building [offset-x offset-y height color]
  (let [sizes (:sizes @app-state)]
    (def building {:width 50
                   :height height
                   :x (+ (:width sizes) offset-x)
                   :y (- (:height sizes) height offset-y)
                   :color color})
    building
    ))

;; Parametros por defecto
(defn populate-layer [layer accum-width]
  (if (<= accum-width (:width (:sizes @app-state)))
    (do (def height (+ 20 (rand-int 120)))
        (def building (create-building accum-width (:y layer) height (:color layer)))
        (recur (update-in layer [:buildings] conj building) (+ accum-width (:width building))))
    layer
  ))

(defn create-canvas [container]
  (let [canvas (.createElement js/document "CANVAS")]
    (.appendChild container canvas)
    (.add (.-classList canvas) "vis-canvas")
    (set! (.-width canvas) (.-offsetWidth container))
    (set! (.-height canvas) (.-offsetHeight container))
    canvas
    )
  )

(defn update-layer [layer]
  (def buildings (map (fn [b] (update-in b [:x] #(- % (:speed layer)))) (:buildings layer)))
  (def leftmost (first buildings))

  (if (<= (+ (:width leftmost) (:x leftmost)) 0)
    (do (def new-building (assoc leftmost :x (:width (:sizes @app-state))))
        (assoc layer :buildings (conj (into [] (drop 1 buildings)) new-building)))
    (assoc layer :buildings buildings)
    ;(swap! app-state assoc :buildings (conj (into [] (drop 1 buildings)) new-building))
    )
  )

(defn draw-layer [layer]
  (doseq [building (:buildings layer)]
    (draw-building building))
  )

(defn advance []
  (def updated-layers
    (swap! app-state assoc :layers (doall (map update-layer (:layers @app-state))))
    )
  )

(defn render []
  (let [{sizes :sizes ctx :ctx layers :layers} @app-state]
    (advance)
    (.clearRect ctx 0 0 (:width sizes) (:height sizes))
    ;; Esto tiene mala pinta
    (doseq [layer (reverse layers)] (draw-layer layer))
    (.requestAnimationFrame js/window render)
    )
  )

(defn main []
  (def canvas (create-canvas (:container @app-state)))
  (def ctx (.getContext canvas "2d"))
  (def sizes {:width (.-width canvas)
              :height (.-height canvas)})
  (swap! app-state merge {:sizes sizes
                          :canvas canvas
                          :ctx ctx})

  (def layers [(populate-layer {:buildings [] :color "#222" :y 0 :speed 2} 0)
               (populate-layer {:buildings [] :color "#333" :y 20 :speed 1.6} 0)
               (populate-layer {:buildings [] :color "#444" :y 40 :speed 1.1} 0)
               (populate-layer {:buildings [] :color "#555" :y 60 :speed 0.6} 0)
               (populate-layer {:buildings [] :color "#666" :y 80 :speed 0.2} 0)
               ])

  (swap! app-state assoc :layers layers)

  ;; Una forma mejor de hacer timeouts?
  ;; (js/setTimeout add-building 1000)
  ;; (js/setTimeout add-building 2000)

  (render)
  )

(defn clear []
  (.remove (.querySelector js/document ".vis-canvas"))
  (swap! app-state assoc :buildings [])
  )

(defn on-js-reload []
  (clear)
  (main)
)
