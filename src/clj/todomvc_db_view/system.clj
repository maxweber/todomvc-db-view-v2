(ns todomvc-db-view.system
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.file :as middleware-file]
            [todomvc-db-view.datomic.connection :as datomic]
            [todomvc-db-view.db-view.get :as db-view-get]
            [todomvc-db-view.db-view.notify :as notify]
            [todomvc-db-view.datomic.tx-report-queue :as tx-report-queue]
            [todomvc-db-view.db-view.command :as command]
            [todomvc-db-view.ring.dispatch :as dispatch]
            [datomic.api :as d]
            [ring.util.response :as response]
            [redelay.core :as rd]))

;; Concept:
;;
;; Holds the `system` map that contains all system components. A
;; system component may have a lifecyle, so that it can be started and
;; stopped. One system component for example is the HTTP server that
;; frees the port, when it is stopped.

(def resource-handler
  (-> (fn [request]
        nil)
      (middleware-file/wrap-file "public")))

(defn sync-handlers
  []
  [db-view-get/ring-handler
   command/ring-handler
   resource-handler
   ;; NOTE: add new synchronous Ring handlers above.
   ])

(defn async-handlers
  []
  [notify/ring-handler
   ;; NOTE: add new asynchronous Ring handlers above.
   ])

(defn dispatch
  [request respond raise]
  (dispatch/dispatch-async
   (conj (async-handlers)
         (partial dispatch/dispatch-sync
                  (sync-handlers)))
   request respond raise))

(def server
  (rd/state :start
            (jetty/run-jetty #'dispatch
                             {:port 8080
                              :join? false
                              :async? true
                              })
            :stop
            (.stop this)
            ))

(defn start!
  "Starts the system."
  []
  @tx-report-queue/queue
  @server
 )

(defn stop!
  "Stops the system."
  []
  (rd/stop))

(defn restart!
  "Restarts the system."
  []
  (stop!)
  (start!))

(comment
  (start!)
  (rd/status)
  (restart!)
  )
