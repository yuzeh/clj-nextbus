(ns clj-nextbus.core
  (:require [clj-http.client :as http]
            [clojure.contrib.def :refer :all]
            [clojure.data.xml :as xml]
            [clojure.string :as string]
            [camel-snake-kebab :refer :all]))

(def ^:dynamic next-bus-url-prefix
  "http://webservices.nextbus.com/service/publicXMLFeed")

(def ^:dynamic *default-nextbus-agency* "MUNI")

(defn key-value->http-option [[k v]]
  (let [camel-cased-option (->camelCase (name k))]
    (str camel-cased-option "=" v)))

(defn mk-arg-string
  "Makes an arg string, given a Clojure map. For symbol keys, they will be converted from
   lisp-casing to camel-casing which is the norm for the NextBus API."
  [options]
  (string/join "&" (map key-value->http-option (seq options))))

(defn build-url
  "Given the NextBus API command to run and the proper options, and then creates the URL."
  ([command] (build-url command {}))
  ([command options]
   (let [full-options (assoc options :command command)
         arg-string (mk-arg-string full-options)]
     (str next-bus-url-prefix "?" arg-string))))

(defn success? [response] (= 200 (:status response)))

(defn response->xml [response]
  (-> response
      :body
      xml/parse-str))

(defn execute [url]
  (let [response (http/get url)]
    (when (success? response) (response->xml response))))

(defn get-varargs
  [arg-name args]
  (apply str (map #(str "&" arg-name "=" %) args)))

(defn agency-list
  "Calls the \"agencyList\" command on the NextBus API."
  []
  (execute (build-url "agencyList")))

(defnk route-list
  "Calls the \"routeList\" command on the NextBus API."
  [:agency *default-nextbus-agency*]
  (execute (build-url "routeList" {:a agency})))

(defnk route-config
  "Calls the \"route-config\" command on the NextBus API."
  [:route nil :agency *default-nextbus-agency*]
  (when-not (nil? route)
    (execute (build-url "routeConfig" {:a agency :r route}))))

(defnk predictions
  "Calls the \"predictions\" command on the NextBus API."
  [:route nil
   :tag nil
   :stop-id nil
   :agency *default-nextbus-agency*
   :use-short-titles false]
  (let [options (cond
                  stop-id {:stop-id stop-id}
                  (and tag route) {:s tag :r route}
                  :else nil)]
    (when-not (nil? options)
      (execute
        (build-url
          "predictions"
          (assoc options
                 :a agency
                 :use-short-titles (str use-short-titles)))))))

(defnk predictions-for-multi-stops
  "Calls the \"predictionsForMultiStops\" command on the NextBus API."
  [:stops []
   :agency *default-nextbus-agency*
   :use-short-titles false]
  (when (seq stops)
    (let [options {:a agency :use-short-titles use-short-titles}
          url-without-stops (build-url "predictionsForMultiStops" options)
          routes (take-nth 2 stops)
          tags (take-nth 2 (rest stops))
          url-suffix (get-varargs "stops" (map #(str %1 "|" %2) routes tags))
          full-url (str url-without-stops url-suffix)]
      (execute full-url))))

(defnk schedule
  "Calls the \"schedule\" command on the NextBus API."
  [:route nil :agency *default-nextbus-agency*]
  (when-not (nil? route)
    (execute (build-url "schedule" {:a agency :r route}))))

(defnk messages
  "Calls the \"messages\" command on the NextBus API."
  [:routes [] :agency *default-nextbus-agency*]
  (when (seq routes)
    (let [url-root (build-url "messages" {:a agency})
          url-suffix (get-varargs "r" routes)
          full-url (str url-root url-suffix)]
      (execute full-url))))

(defnk vehicle-locations
  "Calls the \"vehicleLocations\" command on the NextBus API."
  [:route nil :tag nil :agency *default-nextbus-agency*]
  (when (and route tag)
    (execute (build-url "vehicleLocations" {:r route :t tag :a agency}))))
