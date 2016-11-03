(ns mambobox-mobile.events
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx after debug]]
    [clojure.spec :as s]
    [mambobox-mobile.db :as db :refer [app-db]]
    [mambobox-mobile.fxs]
    [day8.re-frame.http-fx]
    [mambobox-mobile.services :as services]
    [cljs-time.core :as time]))

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

(reg-event-db
 :http-no-on-success
 []
 (fn [db _]
   (.log js/console "Warning !!! :http-no-on-success event generated")
   db))

;;;;;;;;;;;;;;;;;;;;
;; Initialization ;;
;;;;;;;;;;;;;;;;;;;;

(reg-event-fx
 :device-register-error
 []
 (fn [_ [_ {:keys [status] :as error}]]
   ;; if the device is already registered don't show any errors
   (when (not= status 409)
     {:dispatch [:error error]})))

(reg-event-fx
  :initialize-app
  [validate-spec-mw debug (inject-cofx :device-info)]
  (fn [cofxs _]
    (let [{:keys [uniq-id locale country]} (:device-info cofxs)]
     {:db app-db
      :http-xhrio (assoc (services/register-device-http-fx uniq-id locale country)
                         :on-failure [:device-register-error]
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
   (let [right-now (time/now)]
    (-> db
        (assoc :favourites-songs-ids favourites-songs-ids)
        (assoc :user-uploaded-songs-ids user-uploaded-songs-ids)
        (assoc :hot-songs-ids-and-scores hot-songs-ids)
        (assoc :songs (->> songs
                           (map (fn [s] [(:db/id s) s]))
                           (into {})))
        (assoc :all-artists all-artists)
        (assoc :catched-last-dumps {:artists right-now
                                    :hot-songs right-now})))))

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
     (-> cofx :db :ui :edit-song-dialog) {:db (update (:db cofx) :ui dissoc :edit-song-dialog)}
     (not (-> cofx :db :player-status :collapsed?)) {:dispatch [:toggle-player-collapsed]}
     (-> cofx :db :selected-artist :selected-album) {:db (update (:db cofx) :selected-artist dissoc :selected-album)}
     (-> cofx :db :selected-artist) {:db (dissoc (:db cofx) :selected-artist)}
     (-> cofx :db :selected-tag) {:db (dissoc (:db cofx) :selected-tag)}
     (-> cofx :db :searching) {:db (dissoc (:db cofx) :searching)}
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
     {:create-sys-notification {:id not-id
                                :subject "Uploading to mambobox"
                                :message (:displayName song)}
      :upload-song {:song-path (:path song)
                    :file-name (:displayName song)
                    :device-id (-> cofx :device-info :uniq-id)} 
      
      :db (-> (:db cofx)
              (update :uploading assoc (:path song) {:name (:displayName song)
                                                     :notification-id not-id}))})))

(reg-event-fx
 :upload-progress-updated
 [debug]
 (fn [cofx [_ progress file-path]]
   {}))

(reg-event-fx
 :file-uploaded
 [validate-spec-mw debug]
 (fn [cofx [_ song path]]
   (let [not-id (get-in (:db cofx) [:uploading path :notification-id])]
    {:db (-> (:db cofx)
             (update :uploading dissoc path)
             (update :songs assoc (:db/id song) song)
             (update :user-uploaded-songs-ids conj (:db/id song))
             (update :all-artists conj (:artist song)))
     :remove-sys-notification not-id
     :toast (str "Uploaded " (:mb.song/name song))})))

(reg-event-fx
 :file-upload-error
 [validate-spec-mw debug]
 (fn [cofx [_ path error]]
   (let [not-id (get-in (:db cofx) [:uploading path :notification-id])]
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

(defn play-song [db song-id]
  (-> db
      (assoc-in [:player-status :playing-song-id] song-id)
      (assoc-in [:player-status :paused?] false)
      (assoc-in [:player-status :reported-play] false)
      (assoc-in [:player-status :playing-song-progress] 0)))

(reg-event-db
 :play-song
 [validate-spec-mw debug]
 (fn [db [_ song-id]]
  (play-song db song-id)))

(reg-event-db
 :play-song-ready
 [validate-spec-mw debug]
 (fn [db [_ duration]]
   (-> db
       (assoc-in [:player-status :playing-song-duration] duration))))

(reg-event-db
 :playing-song-finished
 [validate-spec-mw debug]
 (fn [db [_ _]]
   (-> db
       (assoc-in [:player-status :paused?] true)
       (assoc-in [:player-status :reported-play] false)
       (assoc-in [:player-status :playing-song-progress] 0))))

(reg-event-fx
 :playing-song-progress-report
 [(inject-cofx :device-info)]
 (fn [{:keys [db device-info]} [_ progress]]
   (let [played-percentage (* (/ (-> db :player-status :playing-song-progress)
                                 (-> db :player-status :playing-song-duration))
                              100)]
     (cond-> {:db (-> db
                       (assoc-in [:player-status :playing-song-progress] progress))}
           
       (and (> played-percentage 40)
            (not (-> db :player-status :reported-play)))
       (->
        (assoc :http-xhrio (assoc (services/track-song-play-http-fx (-> device-info :uniq-id)
                                                                    (-> db :player-status :playing-song-id))
                                  :on-failure [:error]))
        (assoc-in [:db :player-status :reported-play] true))))))

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

(reg-event-fx
 :add-tag-to-song
 [debug (inject-cofx :device-info)]
 (fn [cofxs [_ song-id tag]]
   {:http-xhrio (assoc (services/tag-song-http-fx (-> cofxs :device-info :uniq-id) song-id tag)
                       :on-failure [:error]
                       :on-success [:song-updated])}))
(reg-event-fx
 :remove-tag-from-song
 [debug (inject-cofx :device-info)]
 (fn [cofxs [_ song-id tag]]
   {:http-xhrio (assoc (services/untag-song-http-fx (-> cofxs :device-info :uniq-id) song-id tag)
                       :on-failure [:error]
                       :on-success [:song-updated])}))

(reg-event-db
 :song-updated
 [validate-spec-mw debug]
 (fn [db [_ updated-song]]
   (-> db
       (update :songs assoc (:db/id updated-song) updated-song)
       (update :all-artists conj (:artist updated-song)))))

(reg-event-db
 :close-edit-song-dialog
 [validate-spec-mw debug]
 (fn [db _]
   (update db :ui dissoc :edit-song-dialog)))

(reg-event-db
 :open-edit-song-dialog
 [validate-spec-mw debug]
 (fn [db [_ song-id title compl-dispatch save-dispatch]]
   (assoc-in db [:ui :edit-song-dialog] {:id song-id
                                         :title title
                                         :compl-items []
                                         :compl-dispatch compl-dispatch
                                         :save-dispatch save-dispatch})))

(reg-event-fx
 :update-song-name
 [debug (inject-cofx :device-info)]
 (fn [cofxs [_ song-id new-name]]
   {:http-xhrio (assoc (services/update-song-name-http-fx (-> cofxs :device-info :uniq-id) song-id new-name)
                       :on-failure [:error]
                       :on-success [:song-updated])
    :dispatch [:close-edit-song-dialog]}))

(reg-event-fx
 :update-artist-name
 [debug (inject-cofx :device-info)]
 (fn [cofxs [_ song-id new-name]]
   {:http-xhrio (assoc (services/update-song-artist-http-fx (-> cofxs :device-info :uniq-id) song-id new-name)
                       :on-failure [:error]
                       :on-success [:song-updated])
    :dispatch [:close-edit-song-dialog]}))

(reg-event-fx
 :update-album-name
 [debug (inject-cofx :device-info)] 
 (fn [cofxs [_ song-id new-name]]
   {:http-xhrio (assoc (services/update-song-album-http-fx (-> cofxs :device-info :uniq-id) song-id new-name)
                       :on-failure [:error]
                       :on-success [:song-updated])
    :dispatch [:close-edit-song-dialog]}))

(reg-event-fx
 :re-complete-artist-name
 [debug (inject-cofx :device-info)]
 (fn [cofxs [_ q]]
   {:http-xhrio (assoc (services/search-artists-http-fx (-> cofxs :device-info :uniq-id) q)
                       :on-failure [:error]
                       :on-success [:complete-name-results])}))

(reg-event-fx
 :re-complete-album-name
 [debug (inject-cofx :device-info)]
 (fn [cofxs [_ q]]
   {:http-xhrio (assoc (services/search-artists-http-fx (-> cofxs :device-info :uniq-id) q)
                       :on-failure [:error]
                       :on-success [:complete-name-results])}))

(reg-event-db
 :complete-name-results
 [debug validate-spec-mw]
 (fn [db [_ results]]
   (assoc-in db [:ui :edit-song-dialog :compl-items] results)))




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
                       :on-success [:tag-songs-received])}))

(reg-event-db
 :tag-songs-received
 [validate-spec-mw debug]
 (fn [db [_ songs]]
   (-> db
       (update :songs into (map (fn [s] [(:db/id s) s]) songs))
       (assoc-in [:selected-tag :selected-tag-songs-ids] (into #{} (map :db/id songs))))))

(reg-event-fx
 :reload-hot-songs-if-needed
 [debug (inject-cofx :device-info)]
 (fn [{:keys [db] :as cofxs} _]
   (let [minutes-since-last-dump (time/in-minutes (time/interval (-> db :catched-last-dumps :hot-songs) (time/now)))]
     (.log js/console (str  minutes-since-last-dump " since last hot-songs dump."))
     (if (> minutes-since-last-dump 2)
       {:http-xhrio (assoc (services/load-hot-songs-http-fx (-> cofxs :device-info :uniq-id))
                           :on-failure [:error]
                           :on-success [:hot-songs-received])}
       {}))))

(reg-event-db
 :hot-songs-received
 [validate-spec-mw debug]
 (fn [db [_ songs]]
   (-> db
       (assoc :hot-songs-ids-and-scores (map (fn [[song score]]
                                               [(:db/id song) score])
                                             songs))
       (update :songs into (map (fn [[song score]] [(:db/id song) song]) songs))
       (assoc-in [:catched-last-dumps :hot-songs] (time/now)))))

(reg-event-fx
 :reload-all-artists-if-needed
 [debug (inject-cofx :device-info)]
 (fn [{:keys [db] :as cofxs} _]
   (let [minutes-since-last-dump (time/in-minutes (time/interval (-> db :catched-last-dumps :artists) (time/now)))]
     (.log js/console (str  minutes-since-last-dump " since last all-artists dump."))
     (if (> minutes-since-last-dump 2)
       {:http-xhrio (assoc (services/load-all-artists-http-fx (-> cofxs :device-info :uniq-id))
                           :on-failure [:error]
                           :on-success [:all-artists-received])}
       {}))))

(reg-event-db
 :all-artists-received
 [validate-spec-mw debug]
 (fn [db [_ artists]]
   (-> db
       (assoc :all-artists artists)
       (assoc-in [:catched-last-dumps :artists] (time/now)))))


(reg-event-fx
 :add-to-favourites 
 [validate-spec-mw debug (inject-cofx :device-info)]
 (fn [cofxs [_ song-id]]
   {:db (update (:db cofxs) :favourites-songs-ids conj song-id)
    :http-xhrio (assoc (services/set-song-as-favourite-http-fx (-> cofxs :device-info :uniq-id) song-id)
                       :on-failure [:error])}))

(reg-event-fx
 :rm-from-favourites  
 [validate-spec-mw debug (inject-cofx :device-info)]
 (fn [cofxs [_ song-id]]
   {:db (update (:db cofxs) :favourites-songs-ids disj song-id)
    :http-xhrio (assoc (services/unset-song-as-favourite-http-fx (-> cofxs :device-info :uniq-id) song-id)
                       :on-failure [:error])}))

;;;;;;;;;;;;;;;;
;; Searching  ;;
;;;;;;;;;;;;;;;;

(reg-event-db
 :open-search
 [validate-spec-mw debug]
 (fn [db _]
   (assoc db :searching [])))

(reg-event-db
 :close-search
 [validate-spec-mw debug]
 (fn [db _]
   (dissoc db :searching)))

(reg-event-fx
 :re-search
 [debug (inject-cofx :device-info)]
 (fn [cofxs [_ q]]
   (if (>= (count q) 3)
     {:http-xhrio (assoc (services/search-songs-http-fx (-> cofxs :device-info :uniq-id) q)
                         :on-failure [:error]
                         :on-success [:search-results])}
     {:db (assoc (:db cofxs) :searching [])})))

(reg-event-db
 :search-results
 [debug]
 (fn [db [_ songs]]
   (assoc db :searching songs)))

(reg-event-fx
 :get-and-play-song
 [debug (inject-cofx :device-info)]
 (fn [cofxs [_ song-id]]
   {:http-xhrio (assoc (services/song-by-id-http-fx (-> cofxs :device-info :uniq-id) song-id)
                       :on-failure [:error]
                       :on-success [:song-for-play])}))

(reg-event-db
 :song-for-play
 (fn [db [_ song]]
   (-> db
       (update :songs assoc (:db/id song) song)
       (play-song (:db/id song)))))


