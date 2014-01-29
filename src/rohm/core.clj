(ns rohm.core)

;; owner is no longer passed in life-cylce functions
#_(defmacro component-o
  "Sugar over reify for quickly putting together components that
  only need to implement om.core/IRender,provides automatic owner argument."
  [& body]
  `(reify
     om.core/IRender
     (~'render [this# ~'owner]
       ~@body)))


(defmacro list-of 
  "Produces a list-unrolling with each element in the list-in-model onto
  the component. Uses a default map of {:opts idx :key :id} which is
  merged with the optional alt-map"
  [component list-in-model & [alt-map]]
  (let [idx (gensym "idx")]
    `(for [~idx (range (count ~list-in-model))]
       (om.core/build ~component ~list-in-model 
                        ~(identity {:opts (if (:opts alt-map) (merge {:index idx} (:opts alt-map)) idx)
                                    :key (or (:key alt-map) :id)})))))

(defmacro map-of 
  "Produces a map-unrolling with each element in the map-in-model onto
  the component. Uses a default map of {:opts idx :key :id} which is
  merged with the optional alt-map"
  [component map-in-model & [alt-map]]
  (let [idx (gensym "idx")]
    `(for [~idx (keys ~map-in-model)]
       (om.core/build ~component ~map-in-model 
                        ~(identity {:opts (if (:opts alt-map) (merge {:index idx} (:opts alt-map)) idx)
                                    :key (or (:key alt-map) :id)})))))


;(macroexpand-1 '(list-of comment [1 2 3] {:key :cut}))
;(macroexpand-1 '(list-of comment [1 2 3] ))
;(macroexpand-1 '(list-of comment [1 2 3] {:opts {:current "foo"}}))
