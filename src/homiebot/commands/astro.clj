(ns homiebot.commands.astro
  (:require [libpython-clj2.python
             :refer [as-python as-jvm
                     ->python ->jvm
                     get-attr call-attr call-attr-kw
                     get-item initialize!
                     py. py.- py..]
             :as py]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clj-http.client :as client]
            [discljord.messaging   :as m]
            [discljord.formatting :as f]
            [pyjama.core :refer [ollama]]))

(initialize! :library-path "/usr/lib/libpython3.13.so")

(py/from-import kerykeion
                AstrologicalSubject
                KerykeionChartSVG
                Report)

(defn generate-report
  [{:keys [name city country]
    [month day year hour minute] :birth}]
  (json/parse-string (py.
                      (AstrologicalSubject name year month day hour minute city country)
                      "json")
                     true))

(def kesh (generate-report {:name "Kesh"
                            :birth [9 25 2001 12 55]
                            :city "North Carolina"
                            :country "USA"}))

(use '[homiebot.state :refer [state bot-id]])

(defn create-multi-message! [channel-id content]
  (let [words (clojure.string/split content #"\s+")
        chunks (partition-all 2000 words)]
    (doseq [chunk chunks]
      (let [content (clojure.string/join " " chunk)]
        (println "content:" content)
        (m/create-message! (:rest @state) channel-id :content content)))))

(defn astro-interpret [question]
  (let [resp (pyjama.core/ollama
              "http://localhost:11434/"
              :chat
              {:model "uncensored-deep"
               :stream false
               :options {:num_ctx 4096}
               :messages [{:role :user
                           :content (str "interpret the following using hellenistic astrology, do not use modern western astrology: " question)}]}
              (fn [parsed] (get-in parsed [:message :content])))]
    (println resp)
    (clojure.string/replace resp #"<think>[\s\S]*?</think>" "")))

(defn astro-handler
  [event-type {{bot :bot} :author :keys [author channel-id content mentions]}]
  (when (not bot)
    (let [question (second (re-matches #"!astro (.*)" content))]
      (when question
        (m/create-message! (:rest @state) channel-id :content "gathering the info")
        (let [houses (str/join "\n" (for [planet [:saturn :jupiter :mars :sun :venus :mercury :moon]]
                                      (str (-> kesh planet :house) " " (-> kesh planet :name))))]
          (m/create-message! (:rest @state) channel-id :content houses)
          (create-multi-message! channel-id (astro-interpret houses)))))))
