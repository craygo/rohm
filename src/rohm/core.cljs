(ns rohm.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [cljs.core.async :refer [put! <! chan]]))

;; Pedestal style input queue
(def ^:private input-queue (chan))
(def ^:private effect-queue (chan))

;; Pedestal style route resolver
(defn- de-route [type topic routes]
  ; TODO implement topic based
  (-> (filter #(= (first %) type) routes) first last))

;; Pedestal style route resolver
(defn- handle-mesg [{:keys [type topic] :as mesg} app-state routes]
  (let [old-value (if topic (get-in @app-state topic))]
    (if-let [func (de-route type topic routes)]
      (let [new-value (func old-value mesg)]
        (try
          (swap! app-state update-in topic (fn [_ nv] nv) new-value)
          (catch js/Object e 
            (.warn js/console (pr-str "validator? " e))))
        )
      (.warn js/console (pr-str "handle-mesg: no routing for " type topic)))))

(defn- handle-effect-mesg [{:keys [type topic] :as mesg} service]
  (service mesg input-queue))

(defn handle-messages 
  "Call this in your app's will-mount function with your global state (atom) and routes table (Pedestal
  style vector-of-vectors).
  Optionally takes a service-fn that receives a message from the effect queue.
  service-fn can do remote services calls and put results with put-msg"
  [app-state routes & [service]]
  (go (while true
        (handle-mesg (<! input-queue) app-state routes)))
  (when service
    (go (while true
          (handle-effect-mesg (<! effect-queue) service)))))

(defn put-msg 
  "Puts a Pedestal style message onto the input-queue.
  The type is a keyword and the topic is resolved as the path part of the cursor."
  ([type cursor-or-topic & [opts]]
   (let [msg (merge {:type type :topic (if (om/cursor? cursor-or-topic) (.-path cursor-or-topic) cursor-or-topic)}
                    (if (map? opts) opts {:value opts}))]
     (put-msg msg)))
  ([msg]
   (if (sequential? msg)
     (doseq [m msg]
       (put-msg m))
     (put! input-queue msg))))

(defn extract-refs 
  "Helper to extract all the refs of the owner into a map of their keywordized names and values.
  Or extract a specific ref and return a map {:value val-of-ref}"
  ([owner]
   (let [ks (keys (js->clj (.-refs owner)))
         m (into {} (map #(vector (keyword %) (.-value (om/get-node owner %))) ks))]
     m))
  ([owner ref]
   {:value (.-value (om/get-node owner ref))}))

(defn effect-messages 
  "Pedestal style: pass effect messages produced by effect functions to the effect-queue"
  [messages]
  (doseq [mesg messages]
    (let [mesg (if (om/cursor? (:topic mesg)) (assoc mesg :topic (.-path (:topic mesg))) mesg)]
      (put! effect-queue mesg))))
