(ns todomvc-db-view.db-view.notify
  (:require [datomic.api :as d]
            [todomvc-db-view.util.edn :as edn]))

;; Concept:
;;
;; Provides an API endpoint that allows clients to listen for changes
;; in the Datomic transaction log, which affect their current
;; logged-in user.
;;
;; Thereby the client can refresh the `:db-view/output` map as soon as
;; it is notified by this API endpoint. HTTP long polling is used here
;; to allow the server to push a message to the client. It is less
;; complex to implement in comparison to WebSockets. Furthermore the
;; low latency and reduced payload size of WebSockets is not required
;; for this use case.

(defonce client-listeners-state
  ;; holds the httpkit channels of the clients, which are waiting for
  ;; a db-view notify:
  (atom #{}))

(defn ring-handler
  "Ring-handler for the '/db-view/notify' API endpoint."
  [request respond raise]
  (when (and (= (:request-method request) :post)
             (= (:uri request) "/db-view/notify"))
    ;; NOTE: for a production app add an authentication check
    ;;       here:
    (swap! client-listeners-state
           conj
           {:respond respond})
    true))

(defn notify
  "A Datomic transaction listener that notifies all user browser
   sessions, where the user was affected by the transaction of the
   `tx-report`."
  [tx-report]
  (let [basis-t (d/basis-t (:db-after tx-report))
        response (edn/response
                  {:db/basis-t basis-t})]
    ;; NOTE: for a production app only send notifications to the
    ;;       users which are affected by this `tx-report`:
    (doseq [{:keys [respond] :as listener} @client-listeners-state]
      (try
        (respond response)
        (catch Exception e
          ;; ignore exceptions here, they are thrown if the HTTP
          ;; connection was already closed for example.
          ))
      (swap! client-listeners-state
             disj
             listener))))
