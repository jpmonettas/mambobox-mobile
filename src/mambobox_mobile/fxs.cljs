(ns mambobox-mobile.fxs
  (:require [re-frame.core :refer [reg-fx reg-cofx dispatch]]
            [promesa.core :as p :include-macros true]
            [mambobox-mobile.constants :refer [server-url]]))

(def upload-file (-> (js/require "react-native")
                     .-NativeModules
                     .-FileUpload
                     .-upload))

(def device-info-uuid (-> (js/require "react-native-device-info") .-getUniqueID))
(def device-info-locale (-> (js/require "react-native-device-info") .-getDeviceLocale))
(def device-info-country (-> (js/require "react-native-device-info") .-getDeviceCountry))

(def pick (-> (js/require "react-native") .-NativeModules .-PickerModule .-pick))

;; select-song effect recieves an event to generate
;; when song selected. Will generate that event with the
;; selected song
(reg-fx
 :select-song
 (fn [next-event]
   (.then (pick) (fn [real-path]
                   (dispatch [next-event real-path])))))

(reg-fx
 :upload-song
 (fn [song-real-path file-name device-id]
   (upload-file (clj->js {:uploadUrl (str server-url "/uploads")
                          :method "POST"
                          :headers {"Accept" "application/json"}
                          :fields {"device-id" device-id}
                          :files [{:filename file-name
                                   :filepath song-real-path}]})
                (fn [err result]
                  (.log js/console "Upload " err result)))))

(reg-cofx
 :device-info
 (fn [coeffects _]
   (assoc coeffects :device-info {:uniq-id (device-info-uuid)
                                  :locale (device-info-locale)
                                  :country (device-info-country)}))) 
