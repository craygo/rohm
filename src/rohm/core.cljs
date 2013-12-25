(ns rohm.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [cljs.core.async :refer [put! <! chan]]))

;; Pedestal style input queue
(def ^:private input-queue (chan))

;; Pedestal style route resolver
(defn- de-route [type topic routes]
  ; TODO implement topic based
  (-> (filter #(= (first %) type) routes) first last))

;; Pedestal style route resolver
(defn- handle-mesg [{:keys [type topic] :as mesg} app-state routes]
  (let [old-value (get-in @app-state topic)]
    (if-let [func (de-route type topic routes)]
      (let [new-value (func old-value mesg)]
        (swap! app-state update-in topic (fn [_ nv] nv) new-value)
        )
      (.warn js/console (pr-str "handle-mesg: no routing for " type topic)))))

;; called from the web-app in the will-mount function of the overall app
(defn handle-messages 
  "Call this in your app's will-mount function with your global state (atom) and routes table (Pedestal
  style vector-of-vectors)."
  [app-state routes]
  (go (while true
        (handle-mesg (<! input-queue) app-state routes))))

(defn put-msg 
  "Puts a Pedestal style message onto the input-queue.
  The type is a keyword and the topic is resolved as the path part of the cursor."
  [type cursor & opts]
  ;(.info js/console (pr-str "put-msg " type cursor opts))
  (put! input-queue (merge {:type type :topic (:om.core/path (meta cursor))} opts)))

(defn extract-refs 
  "Helper to extract all the refs of the owner into a map of their keywordized names and values."
  [owner]
  (let [ks (keys (js->clj (.-refs owner)))
        m (into {} (map #(vector (keyword %) (.-value (om/get-node owner %))) ks))]
    ;(.info js/console (pr-str "handle-submit " ks m))
    m))
