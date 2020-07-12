(ns todomvc-db-view.ring.dispatch
  (:require [ring.util.response :as response]))

(defn dispatch-sync
  "Dispatches the Ring request to the synchronous `ring-handlers`."
  [ring-handlers request respond raise]
  (try
    (some
     (fn [sync-handler]
       (when-let [response (sync-handler request)]
         (respond response)
         true))
     ring-handlers)
    (catch Throwable e
      (raise e))))

(defn dispatch-async
  "Dispatches the Ring request to asynchronous `ring-handlers`."
  [ring-handlers request respond raise]
  (some
   (fn [async-handler]
     (async-handler request
                    respond
                    raise))
   ring-handlers))

(defn not-found-handler
  "The 404 page of the system."
  [request respond raise]
  (respond (response/not-found "404 - not found")))
