(ns homiebot.core
  (:require [clojure.edn :as edn]
            [clojure.core.async :refer [chan close!]]
            [discljord.messaging :as discord-rest]
            [discljord.connections :as discord-ws]
            [discljord.formatting :refer [mention-user]]
            [discljord.events :as e]))

(use '[homiebot.commands.mention :as mention])
(use '[homiebot.commands.ask :as ask])
(use '[homiebot.commands.astro :as astro])
(use '[homiebot.state :as state])

(def config (edn/read-string (slurp "config.edn")))

(defmulti handle-event
  "Event handling multi method. Dispatches on the type of the event."
  (fn [type _data] type))

(defmethod handle-event :ready
  [_ _]
  (discord-ws/status-update! (:gateway @state) :activity (discord-ws/create-activity :name (:playing config))))

(defmethod handle-event :default [_ _])

(defn start-bot!
  "Start a discord bot using the token specified in `config.edn`.

  Returns a map containing the event channel (`:events`), the gateway connection (`:gateway`) and the rest connection (`:rest`)."
  [token & intents]
  (let [event-channel (chan 100)
        gateway-connection (discord-ws/connect-bot! token event-channel :intents (set intents))
        rest-connection (discord-rest/start-connection! token)]
    {:events  event-channel
     :gateway gateway-connection
     :rest    rest-connection}))

(defn stop-bot!
  "Takes a state map as returned by [[start-bot!]] and stops all the connections and closes the event channel."
  [{:keys [rest gateway events] :as _state}]
  (discord-rest/stop-connection! rest)
  (discord-ws/disconnect-bot! gateway)
  (close! events))

(def handlers {:message-create [#'mention/mention-handler
                                #'ask/ask-handler
                                #'astro/astro-handler
                                ]})


(defn -main [& args]
  (reset! state (start-bot! (:token config) :guilds :guild-messages))
  (reset! bot-id (:id @(discord-rest/get-current-user! (:rest @state))))
  (println "START")
  (try
    (e/message-pump! (:events @state) (partial e/dispatch-handlers #'handlers))
    (finally (stop-bot! @state))))
