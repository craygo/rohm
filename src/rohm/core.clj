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
  the component. Uses a default map of {:path [%] :key :id} which is
  merged with the optional alt-map"
  [component list-in-model & [alt-map]]
  (let [a (gensym "a")]
    `(into-array (map (fn [~a] (om.core/build ~component ~list-in-model 
                        ~(merge {:path [a] :key :id} alt-map)))
                      (range (count ~list-in-model))))))

;(macroexpand-1 '(list-of comment [1 2 3] {:key :cut}))
