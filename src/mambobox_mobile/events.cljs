(ns mambobox-mobile.events
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx after debug]]
    [clojure.spec :as s]
    [mambobox-mobile.db :as db :refer [app-db]]
    [mambobox-mobile.fxs]
    [day8.re-frame.http-fx]
    [mambobox-mobile.services :as services]))

;; -- Middleware ------------------------------------------------------------
;;
;; See https://github.com/Day8/re-frame/wiki/Using-Handler-Middleware
;;
(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db]
  (when-not (s/valid? spec db)
    (let [explain-data (s/explain-data spec db)]
      (.log js/console "Spec check failed: " explain-data)
      (throw (ex-info (str "Spec check failed: " explain-data) explain-data)))))

(def validate-spec-mw
  (if goog.DEBUG
    (after (partial check-and-throw ::db/db))
    []))

;; -- Handlers --------------------------------------------------------------

;;;;;;;;;;;;;;;;;;;;
;; Initialization ;;
;;;;;;;;;;;;;;;;;;;;

(reg-event-fx
  :initialize-app
  [validate-spec-mw debug (inject-cofx :device-info)]
  (fn [cofxs _]
    (let [{:keys [uniq-id locale country]} (:device-info cofxs)]
     {:db app-db
      :http-xhrio (assoc (services/register-device-http-fx uniq-id locale country)
                         :on-failure [:error]
                         :on-success [:initialize-success])
      ;; TODO Remove this hack, it's only until we call register if it wasn't registered
      :dispatch [:initialize-success]})))

(reg-event-fx
  :initialize-success
  [debug (inject-cofx :device-info)]
  (fn [cofxs _]
    {:http-xhrio (assoc (services/get-initial-dump-http-fx (-> cofxs :device-info :uniq-id))
                        :on-failure [:error]
                        :on-success [:initial-dump])}))

(reg-event-db
 :initial-dump
 [validate-spec-mw debug]
 (fn [db [_ {:keys [songs favourites-songs-ids hot-songs-ids user-uploaded-songs-ids all-artists] :as data}]]
   (.log js/console "Got initial dump ! " data)
   (-> db
       (assoc :favourites-songs-ids favourites-songs-ids)
       (assoc :user-uploaded-songs-ids user-uploaded-songs-ids)
       (assoc :hot-songs-ids hot-songs-ids)
       (assoc :songs songs)
       (assoc :all-artists all-artists))))

;;;;;;;;;;;;;;;;;;;;;;
;; General handlers ;;
;;;;;;;;;;;;;;;;;;;;;;

(reg-event-fx
 :error
 [debug]
 (fn [_ [_ error-msg]]
   {:toast (str error-msg)}))

(reg-event-fx
 :back
 [debug]
 (fn [cofx [_ _]]
   (cond
     (not (-> cofx :db :player-status :collapsed?)) {:dispatch [:toggle-player-collapsed]}
     (-> cofx :db :selected-artist :selected-album) {:db (update (:db cofx) :selected-artist dissoc :selected-album)}
     (-> cofx :db :selected-artist) {:db (dissoc (:db cofx) :selected-artist)}
     (-> cofx :db :selected-tag) {:db (dissoc (:db cofx) :selected-tag)}
     )))





(reg-event-fx
 :pick-song-and-upload
 [debug]
 (fn [cofx _]
   ;; select-song effect recieves an event to generate
   ;; when song selected. Will generate that event with the
   ;; selected song
   {:select-song {:with-selected-event :song-for-upload-selected}}))

(reg-event-db
 :open-preferences
 [validate-spec-mw debug]
 (fn [db _]
   db))

(reg-event-fx
 :song-for-upload-selected
 [validate-spec-mw debug (inject-cofx :device-info)]
 (fn [cofx [_ song]]
   (let [not-id (rand-int 10000)]
     ;; TODO For some reason if we call this two fxs the notification
     ;; only gets created after the upload completes
     {
      ;; :create-sys-notification {:id not-id
      ;;                           :subject "Uploading to mambobox"
      ;;                           :message (:displayName song)}
      :upload-song {:song-path (:path song)
                    :file-name (:displayName song)
                    :device-id (-> cofx :device-info :uniq-id)} 
      
      :db (-> (:db cofx)
              (update :uploading assoc (:path song) {:name (:displayName song)
                                                     :notification-id not-id}))})))


(reg-event-fx
 :file-uploaded
 [validate-spec-mw debug]
 (fn [cofx [_ song path]]
   (let [not-id (get-in (:db cofx) [:uploading path :notification-id])]
    {:db (-> (:db cofx)
             (update :uploading dissoc path)
             (update :songs conj song)
             (update :user-uploaded-songs-ids conj (:db/id song))
             (update :all-artists conj (:artist song)))
     ;; TODO fix notification stuff
     ;; :remove-sys-notification not-id
     :toast (str "Uploaded " (:mb.song/name song))})))

(reg-event-fx
 :file-upload-error
 [validate-spec-mw debug]
 (fn [cofx [_ path error]]
   (let [not-id (get-in (:db cofx) [:uploading path :notification-id])]
     ;; If the error happens too quick, we aren't being able to remove the notification
     ;; looks like it wasn't created yet
    {:db (-> (:db cofx)
             (update :uploading dissoc path))
     :remove-sys-notification not-id
     :dispatch [:error error]})))


;;;;;;;;;;;;
;; Player ;;
;;;;;;;;;;;;

(reg-event-db
 :toggle-play
 [validate-spec-mw debug]
 (fn [db [_ value]]
   (update-in db [:player-status :paused?] not)))

(reg-event-db
 :toggle-player-collapsed
 [validate-spec-mw debug]
 (fn [db [_ value]]
   (update-in db [:player-status :collapsed?] not)))

(reg-event-db
 :play-song
 [validate-spec-mw debug]
 (fn [db [_ song-id]]
   (-> db
       (assoc-in [:player-status :playing-song-id] song-id)
       (assoc-in [:player-status :paused?] false))))

(reg-event-db
 :play-song-ready
 [validate-spec-mw debug]
 (fn [db [_ duration]]
   (-> db
       (assoc-in [:player-status :playing-song-duration] duration)
       (assoc-in [:player-status :playing-song-progress] 0))))

(reg-event-db
 :playing-song-finished
 [validate-spec-mw debug]
 (fn [db [_ _]]
   (-> db)))

(reg-event-db
 :playing-song-progress-report
 []
 (fn [db [_ progress]]
   (-> db
       (assoc-in [:player-status :playing-song-progress] progress))))

(reg-event-db
 :player-progress-sliding
 (fn [db [_ v]]
   (-> db
       (assoc-in [:player-status :playing-progress-sliding] v))))

(reg-event-db
 :player-progress-sliding-complete
 [validate-spec-mw debug]
 (fn [db [_ v]]
   (-> db
       (assoc-in [:player-status :playing-song-progress] (* (-> db :player-status :playing-progress-sliding)
                                                            (-> db :player-status :playing-song-duration))))))



(reg-event-db
 :change-tab
 [validate-spec-mw debug]
 (fn [db [_ tab-idx]]
   (assoc-in db [:ui :selected-tab] tab-idx)))

;;;;;;;;;;;;;;;;;;
;; Song edition ;;
;;;;;;;;;;;;;;;;;;

(reg-event-db
 :edit-song-attr
 [validate-spec-mw debug]
 (fn [db [_ song-id song-attr-key new-val]]
   db))

(reg-event-fx
 :add-tag-to-song
 [debug (inject-cofx :device-info)]
 (fn [cofxs [_ song-id tag]]
   {:http-xhrio (assoc (services/tag-song-http-fx (-> cofxs :device-info :uniq-id) song-id tag)
                       :on-failure [:error]
                       :on-success [:song-updated])}))

(reg-event-db
 :song-updated
 [validate-spec-mw debug]
 (fn [db [_ updated-song]]
   (update db :songs (fn [songs]
                       (map (fn [s]
                              (if (= (:db/id s) (:db/id updated-song))
                                updated-song
                                s))
                            songs)))))

;;;;;;;;;;;;;;;;
;; Artist tab ;;
;;;;;;;;;;;;;;;;

(reg-event-fx
 :load-artist-albums
 [validate-spec-mw debug (inject-cofx :device-info)]
 (fn [cofxs [_ artist]]
   {:db (assoc (:db cofxs) :selected-artist artist)
    :http-xhrio (assoc (services/load-artist-albums-http-fx (-> cofxs :device-info :uniq-id) (:db/id artist))
                       :on-failure [:error]
                       :on-success [:artist-albums])}))

(reg-event-db
 :artist-albums
 [validate-spec-mw debug]
 (fn [db [_ albums]]
   (update db :selected-artist assoc :artist-albums albums)))

(reg-event-fx
 :load-album-songs
 [validate-spec-mw debug (inject-cofx :device-info)]
 (fn [cofxs [_ album]]
   {:db (assoc-in (:db cofxs) [:selected-artist :selected-album] album)
    :http-xhrio (assoc (services/load-album-songs-http-fx (-> cofxs :device-info :uniq-id) (:db/id album))
                       :on-failure [:error]
                       :on-success [:album-songs])}))

(reg-event-db
 :album-songs
 [validate-spec-mw debug]
 (fn [db [_ songs]]
   (update-in db [:selected-artist :selected-album] assoc :songs songs)))

(reg-event-fx
 :load-tag-songs
 [validate-spec-mw debug (inject-cofx :device-info)]
 (fn [cofxs [_ tag]]
   {:db (assoc-in (:db cofxs) [:selected-tag :tag] tag)
    ;; TODO replace page in las argument for paginaiton
    :http-xhrio (assoc (services/load-tag-songs-http-fx (-> cofxs :device-info :uniq-id) tag 0)
                       :on-failure [:error]
                       :on-success [:tag-songs])}))

(reg-event-db
 :tag-songs
 [validate-spec-mw debug]
 (fn [db [_ songs]]
   (update db :selected-tag assoc :songs songs)))
