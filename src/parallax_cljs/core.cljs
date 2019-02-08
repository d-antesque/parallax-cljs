(ns parallax-cljs.core
    (:require ))

(enable-console-print!)

(defonce app-state (atom {:container (.querySelector js/document ".vis-container")
                          :buildings []
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

(defn add-building [offset height]
  (let [sizes (:sizes @app-state)]
    (def building {:width 50
                   :height height
                   :x (+ (:width sizes) offset)
                   :y (- (:height sizes) height)
                   :color "#222"})
    (swap! app-state update-in [:buildings] conj building)
    building
    ))

;; Parametros por defecto
(defn populate [accum-width]
  (println accum-width)
  (if (<= accum-width (:width (:sizes @app-state)))
    (do (def height (+ 20 (rand-int 120)))
        (recur (+ accum-width (:width (add-building accum-width height))))))
  )

(defn create-canvas [container]
  (let [canvas (.createElement js/document "CANVAS")]
    (.appendChild container canvas)
    (.add (.-classList canvas) "vis-canvas")
    (set! (.-width canvas) (.-offsetWidth container))
    (set! (.-height canvas) (.-offsetHeight container))
    canvas
    )
  )

(defn advance []
  (let [{buildings :buildings base-speed :base-speed sizes :sizes} @app-state]
    (swap! app-state assoc :buildings (map (fn [b] (update-in b [:x] #(- % base-speed))) buildings))

    ;; Mejor forma de hacer esto
    (def leftmost (first buildings))
    (when (<= (+ (:width leftmost) (:x leftmost)) 0)
      (def new-building (assoc leftmost :x (:width sizes)))
      (swap! app-state assoc :buildings (conj (into [] (drop 1 buildings)) new-building))
      )
    )
  )

(defn render []
  (let [{sizes :sizes ctx :ctx buildings :buildings} @app-state]
    (advance)
    (.clearRect ctx 0 0 (:width sizes) (:height sizes))
    ;; Esto tiene mala pinta
    (doall (map draw-building buildings))
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

  ;; (add-building)
  (populate 0)

  ;; Una forma mejor de hacer timeouts?
  ;; (js/setTimeout add-building 1000)
  ;; (js/setTimeout add-building 2000)

  (render)
  )

(defn clear []
  (.remove (.querySelector js/document ".vis-canvas"))
  (swap! app-state assoc :buildings [])
  (println @app-state)
  )

(defn on-js-reload []
  (clear)
  (main)
)
