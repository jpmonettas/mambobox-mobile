(ns mambobox-mobile.fxs 
  (:require [re-frame.core :refer [reg-fx reg-cofx dispatch]]
            [promesa.core :as p :include-macros true]
            [mambobox-mobile.constants :refer [server-url]]))

(def upload-file (-> (js/require "react-native")
                     .-NativeModules
                     .-FileUpload
                     .-upload))

(def notification (js/require "react-native-system-notification"))

(def device-info-uuid (-> (js/require "react-native-device-info") .-getUniqueID))
(def device-info-locale (-> (js/require "react-native-device-info") .-getDeviceLocale))
(def device-info-country (-> (js/require "react-native-device-info") .-getDeviceCountry))

(def toast-android (-> (js/require "react-native") .-ToastAndroid))
(def toast-android-short (.-SHORT toast-android))
(def toast-android-long (.-LONG toast-android))
(def show-toast (.-show toast-android))

(def pick (-> (js/require "react-native") .-NativeModules .-PickerModule .-pick))


(reg-fx
 :toast
 (fn [msg]
   (show-toast msg toast-android-long)))

;; select-song effect recieves an event to generate
;; when song selected. Will generate that event with the
;; selected song
(reg-fx
 :select-song
 (fn [{:keys [with-selected-event]}]
   (.then (pick) (fn [song]
                   (dispatch [with-selected-event (js->clj song :keywordize-keys true)])))))

(reg-fx
 :upload-song
 (fn [{:keys [song-path file-name device-id]}]
   (println "UPLOADING song" server-url "/song/upload?device-id=" device-id)
   (upload-file (clj->js {:uploadUrl (str server-url "/song/upload?device-id=" device-id)
                          :method "POST"
                          :headers {"Accept" "application/json"}
                          :fields {}
                          :files [{:filename file-name
                                   :filepath song-path}]})
                (fn [err song]
                  (if err
                    (dispatch [:file-upload-error song-path err])
                    (let [song (js->clj (.parse js/JSON (.-data song)))]
                      (dispatch [:file-uploaded {:db/id (get song "id")
                                                 :mb.song/name (get song "name")
                                                 :mb.song/file-id (get song "fileId")
                                                 :mb.song/plays-count (get song "playsCount")
                                                 :mb.song/url (get song "url")
                                                 :artist {:db/id (get-in song ["artist" "id"])
                                                                  :mb.artist/name (get-in song ["artist" "name"])}
                                                 :album {:db/id (get-in song ["album" "id"])
                                                         :mb.album/name (get-in song ["album" "name"])}}
                                 song-path])))))))

(reg-cofx
 :device-info
 (fn [coeffects _]
   (assoc coeffects :device-info {:uniq-id (device-info-uuid)
                                  :locale (device-info-locale)
                                  :country (device-info-country)})))

(reg-fx
 :create-sys-notification
 (fn [{:keys [id subject message]}]
   (println "CREATING NOTIFICATION")
   (.then (.create notification #js {:id id :subject subject :message message :progress -1})
          (fn [n] (println "NOTIFICATION CREATED")))))

(reg-fx
 :remove-sys-notification
 (fn [id]
   (println "REMOVING NOTIFICATION")
   (.delete notification id)))
