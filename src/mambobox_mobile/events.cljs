(ns mambobox-mobile.events
  (:require
    [re-frame.core :refer [reg-event-db after debug]]
    [clojure.spec :as s]
    [mambobox-mobile.db :as db :refer [app-db]]))

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
    (after (partial check-and-throw ::db/app-db))
    []))

;; -- Handlers --------------------------------------------------------------

(reg-event-db
  :initialize-db
  [validate-spec-mw debug]
  (fn [_ _]
    app-db))


(reg-event-db
 :toggle-play
 [validate-spec-mw debug]
 (fn [db [_ value]]
   (update db :playing not)))

(reg-event-db
 :change-tab
 [validate-spec-mw debug]
 (fn [db [_ tab-idx]]
   (assoc-in db [:ui :selected-tab] tab-idx)))

