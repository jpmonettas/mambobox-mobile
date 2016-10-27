(ns mambobox-mobile.services
  (:require [ajax.edn :as ajax-edn]
            [ajax.core :as ajax]
            [mambobox-mobile.constants :as constants]))

(def http-fxs-commons
  {:timeout 8000
   :format (ajax-edn/edn-request-format)
   :response-format (ajax-edn/edn-response-format)})

(defn register-device-http-fx [device-id locale country]
  (merge http-fxs-commons
   {:method :post
    :uri (str constants/server-url "/user/register-device?device-id=" device-id)
    :params {:locale locale
             :country country}}))

(defn get-initial-dump-http-fx [device-id]
  (merge http-fxs-commons
         {:method :post
          :uri (str constants/server-url "/song/initial-dump?device-id=" device-id)}))
                                 
