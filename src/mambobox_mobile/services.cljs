(ns mambobox-mobile.services
  (:require [ajax.core :as ajax]
            [mambobox-mobile.constants :as constants]))

(def http-fxs-commons
  {:timeout 8000
   :format (ajax/json-request-format)
   :response-format (ajax/json-response-format {:keywords? true})})

(defn register-device-http-fx [device-id locale country]
  (merge http-fxs-commons
   {:method :post
    :uri (str constants/server-url "/user/register-device?device-id=" device-id)
    :params {:locale locale
             :country country}}))
                                 
