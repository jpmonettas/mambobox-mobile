(ns mambobox-mobile.android.core
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [mambobox-mobile.events]
            [mambobox-mobile.subs]))

(def ReactNative (js/require "react-native"))

(def app-registry (.-AppRegistry ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def view-pager (r/adapt-react-class (.-ViewPagerAndroid ReactNative))) 
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def video (r/adapt-react-class (.-default (js/require "react-native-video/Video"))))

(def pick (-> ReactNative .-NativeModules .-PickerModule .-pick))
(def upload-file (-> ReactNative .-NativeModules .-FileUpload .-upload))

(defn pick-and-upload []
  (.then (pick) (fn [real-path]
                  (.log js/console "Uploading selected file " real-path)
                  (upload-file (clj->js {:uploadUrl "http://192.168.1.8:1155/uploads"
                                         :method "POST"
                                         :headers {"Accept" "application/json"}
                                         :fields {"param1" "value1"}
                                         :files [{:filename "audiofile"
                                                  :filepath real-path}]})
                               (fn [err result]
                                 (.log js/console "Upload " err result))))))

(def logo-img (js/require "./images/cljs.png"))

(defn alert [title]
      (.alert (.-Alert ReactNative) title))

(def styles {:align-items "center"
             :padding 20
             :background-color "#FF5577"})

(defn pager []
  [view-pager {:init-page 0 :style {:height 300}} 
   [view {:style {:height 200 :background-color "#345678" }}
    ]
   [view {:style {:background-color "#FFFF45"}}
    [text {} "chauuuuuuuuusito"]]
   ])

(defn app-root []
  (let [greeting (subscribe [:get-greeting])
        player-status (subscribe [:player-status])]
    (fn []
      #_[pager]
      [view {:style {:flex-direction "column" :margin 40 :align-items "center"}}
       [text {} (str "holaaaaaaaaaa testtt : " (:playing @player-status) "]")]
       [video {:source {:uri "http://192.168.1.8:1122/Rehab.mp3"}
               :play-in-background true
               :play-when-inactive true
               :paused (:playing @player-status)}]
       [text {:style {:font-size 30 :font-weight "100" :margin-bottom 20 :text-align "center"}} @greeting]
       [image {:source logo-img
               :style  {:width 80 :height 80 :margin-bottom 30}}]
       [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}
                             :on-press #(dispatch [:toggle-play])}
        [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "stop/pause"]]
       [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}
                             :on-press pick-and-upload}
        [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "upload file"]]])))



(defn init []
  (dispatch-sync [:initialize-db])
  (.registerComponent app-registry "MamboboxMobile" #(r/reactify-component app-root)))
