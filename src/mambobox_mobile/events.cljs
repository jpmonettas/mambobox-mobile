(ns mambobox-mobile.events
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx after debug]]
    [clojure.spec :as s]
    [mambobox-mobile.db :as db :refer [app-db]]
    [mambobox-mobile.fxs]))

;; -- Middleware ------------------------------------------------------------
;;
;; See https://github.com/Day8/re-frame/wiki/Using-Handler-Middleware
;;
(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db]
  (when-not (s/valid? spec db)
    (let [explain-data (s/explain-data spec db)]
      (throw (ex-info (str "Spec check failed: " explain-data) explain-data)))))

(def validate-spec-mw
  (if goog.DEBUG
    (after (partial check-and-throw ::db/db))
    []))

;; -- Handlers --------------------------------------------------------------

(reg-event-db
  :initialize-db
  [validate-spec-mw debug]
  (fn [_ _]
    app-db))

(reg-event-fx
 :error
 [debug]
 (fn [_ [_ error-msg]]
   {:toast error-msg}))

(reg-event-fx
 :back
 [debug]
 (fn [cofx [_ _]]
   (cond
     (not (-> cofx :db :player-status :collapsed?)) {:dispatch [:toggle-player-collapsed]})))

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
 :file-uploaded
 [validate-spec-mw debug]
 (fn [cofx [_ path]]
   (let [not-id (get-in (:db cofx) [:uploading path :notification-id])]
    {:db (-> (:db cofx)
             (update :uploading dissoc path))
     :remove-sys-notification not-id})))

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

