(ns homiebot.commands.ask
  (:require [cheshire.core :as json]
            [clj-http.client :as client]
            [discljord.messaging   :as m]
            [discljord.formatting :as f]))

(use '[homiebot.state :refer [state bot-id]])

(defn random-response [user]
  (str (rand-nth ["Hello there" "Good evening" "Good morning" "G'day" "Hi" "Howdy :cowboy:"])
       ", "
       (f/mention-user user)
       \!))

(defn ask-handler
  [event-type {{bot :bot} :author :keys [author channel-id content mentions]}]
  (when (not bot)
    (let [question (second (re-matches #"!ask (.*)" content))]
      (when question
        (m/create-message! (:rest @state) channel-id :content (random-response author))))))
