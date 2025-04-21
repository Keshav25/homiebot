e(ns homiebot.commands.ask
   (:require [cheshire.core :as json]
             [clj-http.client :as client]
             [discljord.messaging   :as m]
             [discljord.formatting :as f]
             [pyjama.core :refer [ollama]]))

(use '[homiebot.state :refer [state bot-id]])

(defn use-ollama [question]
  (let [resp (pyjama.core/ollama
              "http://localhost:11434/"
              :chat
              {:system "you are the ancient greek philosoher socrates."
               :model "uncensored-deep"
               :stream false
               :options {:num_ctx 4096}
               :messages [{:role :user
                           :content question}]}
              (fn [parsed] (get-in parsed [:message :content])))]
    (println resp)
    (#(clojure.string/replace resp #"<think>[\s\S]*?</think>" ""))))

(defn ask-handler
  [event-type {{bot :bot} :author :keys [author channel-id content mentions]}]
  (when (not bot)
    (let [question (second (re-matches #"!ask (.*)" content))]
      (when question
        (println "question asked: "question)
        (m/create-message! (:rest @state) channel-id :content (str "I am pondering upon \"" question "\""))
        (println "starting ollama call")
        (try
          (let [answer (use-ollama (str "as the greek philosoper socrates, answer the following question, do not say \"As Socrates\" in your answer: " question))]
            (println "OLLAMA ANSWER:" answer)
            (m/create-message! (:rest @state) channel-id :content answer))
          (catch Exception e
            (m/create-message! (:rest @state) channel-id :content "there's been an error")
            (println "Error in use-ollama or create-message!:" (.getMessage e))))))))
