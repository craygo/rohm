(ns rohm.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [cljs.core.async :refer [put! <! chan]]
  ))

(def input-queue (chan))

(defn- de-route [type topic routes]
  ; TODO implement topic based
  (-> (filter #(= (first %) type) routes) first last))

(defn- handle-mesg [{:keys [type topic] :as mesg} app-state routes]
  (let [old-value (get-in @app-state topic)]
    (if-let [func (de-route type topic routes)]
      (let [new-value (func old-value mesg)]
        (swap! app-state update-in topic (fn [_ nv] nv) new-value)
        )
      (.warn js/console (pr-str "handle-mesg: no routing for " type topic)))))

(defn handle-messages [app-state routes]
  (go (while true
        (handle-mesg (<! input-queue) app-state routes))))

(defn put-msg [type cursor & opts]
  ;(.info js/console (pr-str "put-msg " type cursor opts))
  (put! input-queue (merge {:type type :topic (:om.core/path (meta cursor))} opts)))

(defn extract-refs [owner]
  (let [ks (keys (js->clj (.-refs owner)))
        m (into {} (map #(vector (keyword %) (.-value (om/get-node owner %))) ks))]
    ;(.info js/console (pr-str "handle-submit " ks m))
    m))

