# Rohm

A library to work with swannodette's [Om] (http://github.com/swannodette/om) library in a [Pedestal] (http://pedestal.io) style.

Rohm uses Om with some added helpers and a Pedestal style handling of events as input messages.

From Pedestal it takes the idea of creating messages on an input-queue from events and
sending effect messages to an effect-queue to effect changes outside of the app.

Ideally the Pedestal functionality would be provided by integrating Pedestal core but use Om/ReactJS for
rendering instead of the Pedestal diff-driven UI rendering.
For now Rohm just does a simplistic implementation of the Pedestal concepts it uses by using core.async .

## Example
See the [commentmvc] (https://github.com/craygo/commentmvc) example app for a full example.

### Event handling

```clj
; a Pedestal style transformation function
(defn add-comment [old-value {:keys [author text] :as message}]
  (conj old-value {:id (guid) :author author :text text}))

; Pedestal style routing of input messages to functions
(def routes [[:add [:comments] add-comment] ])

(defn comment-form [comments]
  (letfn [(handle-submit [_ owner]
            ; put-msg puts a message in the input-queue 
            ; here with :type :add and the comments cursor which provides the :topic through
            ; the path in its meta data
            ; extract-refs creates a map with all the refs in the owner to their values
            (rohm/put-msg :add comments (rohm/extract-refs owner))
            false)]
    (rohm/component-o  ; like om/component but provides owner in the scope
      (dom/form #js {:className "commentForm" :onSubmit #(handle-submit % owner)} ; call cljs handler
                (dom/input #js {:ref "author" :type "text" :placeholder "Your name" })
                (dom/input #js {:ref "text" :type "text" :placeholder "Say something..."})
                (dom/input #js {:type "submit" :value "Post"})))))
```

### Initialise the app
```clj
(defn comment-app [app]
  (reify
    om/IWillMount
    (will-mount [this owner]
      (let [{:keys [url poll-interval]} app]
        ; call an effect-messages generating function and puts them
        ; on the effect-queue (see next section)
        (rohm/effect-messages (get-server-comments-effect url))
        (js/setInterval #(rohm/effect-messages (get-server-comments-effect %)) poll-interval url)
        ; Tell Rohm to handle input- and effect-messages with your app-state, routes 
        ; and service (see next section)
        (rohm/handle-messages app-state routes comment-service)
        (repl/connect "http://localhost:9000/repl")))
    om/IRender
    (render [_ _]
      (om/build comment-box app {:path [:comments]}))))

(om/root app-state comment-app (.getElementById js/document "container"))
```

### Service and effects
```clj
; transform function
(defn reset-comments [old-value message]
  (:comments message))

; Pedestal style effect functions
; return collection of messages for the effect queue
(defn get-server-comments-effect [url]
  [{:type :read :topic [:comments] :url url}])

; Pedestal style service
(defn comment-service [message input-queue]
  (letfn [(server-res [ev]
            (let [res (js->clj (.getResponseJson (.-target ev)) :keywordize-keys true)
                  res (vec (map #(assoc % :id (guid)) res))]
              (rohm/put-msg :reset [:comments] {:comments res})))
          (get-from-server [url]
            (let [xhr (net/xhr-connection)]
              (gevent/listen xhr "success" server-res)
              (gevent/listen xhr "error" fail)
              (net/transmit xhr url)))]
    ; need some kind of routing here too
    (if (and (= (:type message) :read) (:url message))
      (get-from-server (:url message))
      (.warn js/console (pr-str "comment-service: don't know " message)))))
```

### Guard global constraints
```clj
;; validator 
;; can reject changes that would violate global constraints 
(defn valid? [state] 
  (and (->> state :comments (filter #(:editing %)) count (>= 1)) ; only a single comment may be editing at any time 
       ; ... 
       )) 
 
(set-validator! app-state valid?) 
```

## Using it

Rohm is pre-alpha software.

Follow the Om install instructions first, then install rohm to your local repo
```bash
lein install
```

```clj
(defproject foo "0.1.0-SPAPSHOT"
  ...
  :dependencies [...
                 [rohm "0.1.0-SNAPSHOT"]]
  ...)
```
