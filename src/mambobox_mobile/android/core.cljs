(ns mambobox-mobile.android.core
  (:require [reagent.core :as r :refer [atom create-class]]
            [reagent.ratom :refer [reaction]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [mambobox-mobile.events]
            [mambobox-mobile.subs]
            [clojure.spec :as s]
            [mambobox-core.core-spec]
            [devtools.core :as dt]))

(def ReactNative (js/require "react-native"))
(def device-event-emitter (.-DeviceEventEmitter ReactNative))

(def app-registry (.-AppRegistry ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def scroll-view (r/adapt-react-class (.-ScrollView ReactNative)))
(def slider (r/adapt-react-class (.-Slider ReactNative)))
(def list-view (r/adapt-react-class (.-ListView ReactNative)))
(def tool-bar (r/adapt-react-class (.-ToolbarAndroid ReactNative)))
(def text-input (r/adapt-react-class (.-TextInput ReactNative)))
(def DataSource (-> ReactNative .-ListView .-DataSource))
(def view-pager (r/adapt-react-class (.-ViewPagerAndroid ReactNative))) 
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def touchable-opacity (r/adapt-react-class (.-TouchableOpacity ReactNative)))
(def video (r/adapt-react-class (.-default (js/require "react-native-video/Video"))))
(def icon (r/adapt-react-class (js/require "react-native-vector-icons/FontAwesome")))
(def scrollable-tab-view (r/adapt-react-class (js/require "react-native-scrollable-tab-view")))
(def back-android (.-BackAndroid ReactNative))
(def DialogAndroid (js/require "react-native-dialogs"))

(def tags {"chacha" "#ff0000"
           "mambo" "#9303a7"
           "latin-jazz" "#993366"
           "guaracha" "#64a8d1"
           "salsa dura" "#2219b2"
           "romantica" "#cb0077"
           "bolero" "#e5399e"
           "pachanga" "#999900"
           "boogaloo" "#d9534f"
           "son" "#ff7800"
           "montuno" "#ff9a40"
           "songo" "#ffa700"
           "danzon" "#ffbd40"
           "rumba" "#138900"
           "guaguanco" "#389e28"
           "yambu" "#1dd300"
           "columbia" "#52e93a"
           "afro" "#a64b00"})

(def logo-img (js/require "./images/cljs.png"))

(defn alert [title]
  (.alert (.-Alert ReactNative) title))

(defn show-edit-song-dialog [title song-attr-key song]
  (let [d (DialogAndroid.)]
    (.set d #js{:title title
                :positiveText "Save"
                :negativeText "Cancel"
                :input #js{:prefill (get song song-attr-key)
                           :allowEmptyInput false
                           :callback #(dispatch [:edit-song-attr (:mb.song/id song) song-attr-key %])}})
    (.show d)))

(defn show-tag-select-dialog [song]
  (let [d (DialogAndroid.)]
    (.set d #js{:title "Add tag to song"
                :positiveText "Add"
                :negativeText "Cancel"
                :items (clj->js (keys tags))
                :itemsCallback (fn [_ tag] (dispatch [:add-tag-to-song (:mb.song/id song) tag]))})
    (.show d)))

(defn build-list-view-datasource [rows-data]
  (let [ds (DataSource. #js {:rowHasChanged (constantly false)})]
    (.cloneWithRows ds rows-data)))

(defn format-duration [seconds]
  (let [seconds (int seconds)]
    (cljs.pprint/cl-format nil "~2'0d:~2'0d" (quot seconds 60) (mod seconds 60))))

(defn song [s]
  [touchable-opacity {:on-press #(dispatch [:play-song (:mb.song/id s)])}
   [view {:style {:padding 10
                  :margin 2
                  :border-width 1
                  :border-color "rgba(0,0,0,0.1)"
                  :flex-direction :row
                  :justify-content "space-between"}}
    [view {:flex-direction :row}
     [icon {:name "music"
            :style {:padding 10
                    :margin 5
                    :background-color "rgba(0,0,0,0.1)"}
            :size 20}]
     [view 
      [text {:style {:font-weight :bold
                     :font-size 17}} (:mb.song/name s)]
      [text {} (str (:mb.artist/name s) " . " (format-duration (:mb.song/duration s)))]]]
    [icon {:name "ellipsis-v"
           :style {:padding 10
                   :margin 5}
           :size 20}]]])

(defn my-favorites-tab []
  (let [favorites-songs (subscribe [:favorites-songs])]
   (fn []
     [list-view {:dataSource (build-list-view-datasource (apply array @favorites-songs))
                 :renderRow (comp r/as-element song)}])))

(defn hot-tab []
  (let [hot-songs (subscribe [:hot-songs])]
   (fn []
     [list-view {:dataSource (build-list-view-datasource (apply array @hot-songs))
                 :renderRow (comp r/as-element song)}])))



(defn tags-line [[t1-text t1-bg t1-fg]
                 [t2-text t2-bg t2-fg]]
  (let [tag-style {:padding 20
                   :text-align "center"
                   :margin 10
                   :flex 0.5
                   :font-weight :bold
                   :font-size 15
                   :color "white"}]
   [view {:flex-direction :row}
    [text {:style (merge tag-style {:background-color t1-bg})} t1-text]
    [text {:style (merge tag-style {:background-color t2-bg})} t2-text]]))

(defn tags-tab []
  [scroll-view 
   (for [[t1 t2] (partition-all 2 (into [] tags))]
     ^{:key (first t1)} [tags-line t1 t2])])

(defn header []
  [view {:style {:background-color "#9303a7"}}
   [tool-bar {:title "MamboBox"
              :title-color :white
              :style {:height 40}
              :actions [{:title "Subir musica"}
                        {:title "Preferencias"}]
              :on-action-selected #(case %
                                     0 (dispatch [:pick-song-and-upload])
                                     1 (dispatch [:open-preferences]))}]
   [text-input {:placeholder "Buscar musica..."
                :placeholder-text-color :white
                :underline-color-android :white}]])

(defn full-song-controls [paused?]
  [view {:style {:margin 30
                 :flex-direction :row
                 :justify-content :space-between}}
   [icon {:name "random" :size 12}]
   [icon {:name "step-backward" :size 25}]
   [touchable-opacity {:on-press #(dispatch [:toggle-play])}
    [icon {:name (if paused? "play" "pause") :size 50}]]
   [icon {:name "step-forward" :size 25}]
   [icon {:name "repeat" :size 12}]])

(defn song-editor [song]
  (let [card-style {:flex-direction :row
                    :elevation 3
                    :padding 5
                    :margin-bottom 9
                    :background-color :white
                    :justify-content :space-between
                    :align-items :center}
        text-style {:font-size 18}]
    [view {:style {:padding-bottom 10
                   :padding-top 10}}
     [view {:style {:height 100
                    :margin-bottom 30
                    :justify-content :space-between}}
      [touchable-opacity {:on-press #(show-edit-song-dialog "New song name" :mb.song/name song)}
       [view {:style card-style}
        [text {:style text-style} (:mb.song/name song)]
        [icon {:name "pencil" :size 17}]]]
      [touchable-opacity {:on-press #(show-edit-song-dialog "New artist name" :mb.artist/name song)}
       [view {:style card-style}
        [text {:style text-style} (:mb.artist/name song)]
        [icon {:name "pencil" :size 17}]]]
      [touchable-opacity {:on-press #(show-edit-song-dialog "New album name" :mb.album/name song)}
       [view {:style card-style}
        [text {:style text-style} (:mb.album/name song)]
        [icon {:name "pencil" :size 17}]]]]
     [view {:style {:flex-direction :row
                    :height 100
                    :justify-content :center
                    :align-items :center}}
      (for [tag (:mb.song/tags song)]
        [view {:key tag
               :style {:margin 5
                       :padding 5
                       :background-color (get tags tag)}}
         [text {:style {:color :white}} tag]])
      [touchable-opacity {:on-press #(show-tag-select-dialog song)}
       [icon {:name "tags" :size 35}]]]]))

(defn expanded-player []
  (let [player-status (subscribe [:player-status])
        playing-song (subscribe [:playing-song])]
    (fn []
      (let [pl-stat @player-status
            paused? (:paused? pl-stat)]
        [view {:style {:height 420
                       :justify-content :space-between}}
         [song-editor @playing-song]
         [view {}
          [view {:style {:flex-direction :row
                         :margin 10}}
           [text {}  (format-duration (:playing-song-progress pl-stat))]
           [slider {:style {:flex 0.8}
                    :value (/ (:playing-song-progress pl-stat)
                              (:playing-song-duration pl-stat))
                    :on-sliding-complete #(dispatch [:player-progress-sliding-complete])
                    :on-value-change #(dispatch [:player-progress-sliding %])}]
           [text {} (format-duration (:playing-song-duration pl-stat))]]
          [full-song-controls paused?]]]))))

(defn collapsed-player [playing-song paused?]
  [view {:style {:flex-direction :row
                 :height 60
                 :justify-content :space-between
                 :align-items :center}}
   [touchable-opacity {:on-press #(dispatch [:toggle-player-collapsed])}
    [view {:flex-direction :row}
     [icon {:name "headphones"
            :style {:padding 10
                    :margin 5
                    :background-color "rgba(0,0,0,0.1)"} 
            :size 20}]
     [view {}
      [text {:style {:font-weight :bold
                     :font-size 17}}
       (:mb.song/name playing-song)]
      [text {} (:mb.artist/name playing-song)]]]]
   [view {:style {:margin 10 :flex-direction :row}}
    [touchable-opacity {:on-press #(dispatch [:toggle-play])}
     [icon {:name (if paused? "play" "pause")
            :size 25
            :style {:margin 5}}]]]])

(defn player []
  (let [player-status (subscribe [:player-status])
        playing-song (subscribe [:playing-song])]
    (fn []
      (let [pl-stat @player-status
            pl-song @playing-song]
        [view {:style {:elevation 10
                       :flex 1
                       :position :absolute
                       :bottom 0
                       :left 0
                       :right 0
                       :padding 10
                       :background-color :white}}
        (if (:collapsed? pl-stat)
          [collapsed-player pl-song (:paused? pl-stat)]
          [expanded-player])
        [video {:source {:uri (:mb.song/url pl-song)}
                :play-in-background true
                :play-when-inactive true
                :paused (:paused? pl-stat)
                :on-load #(dispatch [:play-song-ready (.-duration %)])
                :on-end #(dispatch [:playing-song-finished])
                :on-progress #(dispatch [:playing-song-progress-report (.-currentTime %)])}]]))))

(defn app-root []
  (let [selected-tab (subscribe [:selected-tab])]
    (fn []
      [view {:style {:flex 1}}
       [header]
       [scrollable-tab-view {:initial-page @selected-tab
                             :page @selected-tab
                             :on-change-tab #(dispatch [:change-tab (.-i %)])
                             :tab-bar-background-color "#9303a7"
                             :tab-bar-active-text-color :white
                             :tab-bar-inactive-text-color :white
                             :tab-bar-underline-style {:background-color "white"}}
        [view {:tab-label "Favorites"
               :style {:flex 1}}
         [my-favorites-tab]]
        [view {:tab-label "Hot"
               :style {:flex 1}} [hot-tab]]
        [view {:tab-label "Explore"
               :style {:flex 1}} [tags-tab]]]
       [player]])))



(defn init []
  (dispatch-sync [:initialize-app])
  (.registerComponent app-registry "MamboboxMobile" #(r/reactify-component app-root))
  (.addListener device-event-emitter "uploadProgress" (fn [e] (.log js/console e)))
  (.addEventListener back-android "hardwareBackPress" (fn []
                                                        (dispatch [:back])
                                                        true)))
