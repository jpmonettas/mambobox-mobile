(ns mambobox-mobile.android.core
  (:require [reagent.core :as r :refer [atom create-class]]
            [reagent.ratom :refer [reaction]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [mambobox-mobile.events]
            [mambobox-mobile.subs]
            [clojure.spec :as s]
            [mambobox-core.core-spec]
            [mambobox-core.generic-utils :as gen-utils]
            [mambobox-mobile.constants :as constants]
            [devtools.core :as dt]
            [mambobox-mobile.dict :refer [mambo-dictionary]]
            [taoensso.tempura :as tempura :refer [tr]]
            [goog.string :as gstring]
            [goog.string.format]))

(def t (partial tr {:dict mambo-dictionary} [:es]))

(def ReactNative (js/require "react-native"))
(def device-event-emitter (.-DeviceEventEmitter ReactNative))

(def app-registry (.-AppRegistry ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def modal (r/adapt-react-class (.-Modal ReactNative)))
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
(def icon (r/adapt-react-class (.-default (js/require "react-native-vector-icons/FontAwesome"))))
(def scrollable-tab-view (r/adapt-react-class (js/require "react-native-scrollable-tab-view")))
(def back-android (.-BackAndroid ReactNative))
(def DialogAndroid (js/require "react-native-dialogs"))

(def tags {"chacha" "#ff0000"
           "mambo" "#9303a7"
           "cubana" "#e5399e"
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
(def search-img (js/require "./images/search.png"))

(defn alert [title]
  (.alert (.-Alert ReactNative) title))


(defn edit-song-artist-album-dialog []
  (let [edit-song-dialog-subs (subscribe [:edit-song-dialog])
        input-val-atom (r/atom "")]
    (fn []
      (let [{:keys [title compl-items save-dispatch compl-dispatch id]} @edit-song-dialog-subs]
        [view {:style {:position :absolute
                       :padding 15
                       :top 5
                       :bottom 5
                       :elevation 100
                       :left 5
                       :right 5
                       :background-color :white
                       :flex 1}}
         [text {:style {:font-weight :bold}} title]
         [text-input {:style {:flex 0.1}
                      :underline-color-android :grey
                      :on-change-text #(do (reset! input-val-atom %)
                                           (when (and compl-dispatch
                                                      (pos? (count @input-val-atom)))
                                             (dispatch [compl-dispatch @input-val-atom])))}]
         [scroll-view {:style {:flex 0.8}}
          (for [ci compl-items]
            ^{:key ci} [touchable-opacity {:on-press #(dispatch [save-dispatch id ci])}
                        [view {:style {:padding 15
                                       :border-width 1
                                       :margin 2
                                       :border-color "rgba(0,0,0,0.1)"}}
                         [text {:style {:font-size 17}}
                          (gen-utils/denormalize-entity-name-string ci)]]])]
         [view {:flex-direction :row
                       :flex 0.1
                       :justify-content :space-around}
          [touchable-opacity {:on-press #(if (pos? (count @input-val-atom))
                                           (dispatch [save-dispatch id @input-val-atom])
                                           (dispatch [:error (t [:music.error/name-needed "Please add a name"])]))}
           [text (t [:save "Save"])]]
          [touchable-opacity {:on-press #(dispatch [:close-edit-song-dialog])}
           [text (t [:cancel "Cancel"])]]]])))) 

(defn show-tag-select-dialog [song]
  (let [d (DialogAndroid.)]
    (.set d #js{:title (t [:music.edit/add-tag-to-song "Choose a tag"])
                :positiveText (t [:add "Add"])
                :negativeText (t [:cancel "Cancel"])
                :items (clj->js (keys tags))
                :itemsCallback (fn [_ tag] (dispatch [:add-tag-to-song (:db/id song) tag]))})
    (.show d)))

(defn build-list-view-datasource [rows-data]
  (let [ds (DataSource. #js {:rowHasChanged (constantly false)})]
    (.cloneWithRows ds rows-data)))

(defn format-duration [seconds]
  (let [seconds (int seconds)]
    (cljs.pprint/cl-format nil "~2'0d:~2'0d" (quot seconds 60) (mod seconds 60))))

(defn favourite-star-icon [fav? song-id]
  (if fav?
       [touchable-opacity {:on-press #(dispatch [:rm-from-favourites song-id])}
        [icon {:name "star"
               :style {:padding 10
                       :align-self :center
                       :margin 5
                       :color :orange}
               :size 20}]]
       [touchable-opacity {:on-press #(dispatch [:add-to-favourites song-id])}
        [icon {:name "star-o"
               :style {:padding 10
                       :align-self :center
                       :margin 5
                       :color :black}
               :size 20}]]))

(defn song [s]
  [touchable-opacity {:on-press #(dispatch [:play-song (:db/id s)])}
   [view {:style {:padding 10
                  :margin 2
                  :border-width 1
                  :border-color "rgba(0,0,0,0.1)"
                  :flex-direction :row
                  :justify-content "space-between"}}
    [view {:flex-direction :row
           :flex 1}
     (if (:score s)
       [view {:style {:padding 10
                       :margin 5}}
        [icon {:name "thermometer-empty"
               :color :red
               :size 20}]
        [text {:style {:color :red}} (gstring/format "%.1f" (* 10 (:score s)))]]
       [icon {:name "music"
              :style {:padding 10
                      :margin 5
                      :background-color "rgba(0,0,0,0.1)"}
              :size 20}])
     [view {:style {:flex 0.8}}
      [text {:style {:font-weight :bold
                     :font-size 17}
             :number-of-lines 1} (-> s :mb.song/name gen-utils/denormalize-entity-name-string)]
      [text {:number-of-lines 1}
       (-> s :artist :mb.artist/name gen-utils/denormalize-entity-name-string)]
      [view {:style {:flex-direction :row
                     :align-items :center
                     :height 25}}
       (for [tag (:mb.song/tags s)]
         [view {:key tag
                :style {:margin 2
                        :border-radius 5
                        :padding 3
                        :background-color (get tags tag)}}
          [text {:style {:color :white
                         :font-size 10}} tag]])]]]
    [view {:flex-direction :row}
     [favourite-star-icon (:favourite? s) (:db/id s)]]]])

(defn my-favourites-tab []
  (let [favourites-songs (subscribe [:favourites-songs])]
   (fn []
     [list-view {:dataSource (build-list-view-datasource (apply array @favourites-songs))
                 :renderRow (comp r/as-element song)
                 ;; Takes out a warning, will be deprecated soon
                 :enableEmptySections true}])))

(defn hot-tab []
  (let [hot-songs (subscribe [:hot-songs])]
   (fn []
     [list-view {:dataSource (build-list-view-datasource (apply array @hot-songs))
                 :renderRow (comp r/as-element song)
                 ;; Takes out a warning, will be deprecated soon
                 :enableEmptySections true}])))

(defn user-uploaded-songs-tab []
  (let [user-uploaded-songs (subscribe [:user-uploaded-songs])]
   (fn []
     [list-view {:dataSource (build-list-view-datasource (apply array @user-uploaded-songs))
                 :renderRow (comp r/as-element song)
                 ;; Takes out a warning, will be deprecated soon
                 :enableEmptySections true}])))

(defn artist [a]
  [touchable-opacity {:on-press #(dispatch [:load-artist-albums a])}
   [view {:style {:padding 15
                  :border-width 1
                  :margin 2
                  :border-color "rgba(0,0,0,0.1)"}}
    [text {:style {:font-size 17}}
     (gen-utils/denormalize-entity-name-string (:mb.artist/name a))]]])

(defn album [a]
  [touchable-opacity {:on-press #(dispatch [:load-album-songs a])}
   [view {:style {:padding 15
                  :border-width 1
                  :margin 2
                  :border-color "rgba(0,0,0,0.1)"}}
    [text {:style {:font-size 17}}
     (gen-utils/denormalize-entity-name-string (:mb.album/name a))]]])

(defn all-artists-tab []
  (let [all-artists (subscribe [:all-artists])
        selected-artist (subscribe [:selected-artist])
        selected-album-songs (subscribe [:selected-album-songs])]
    (fn []
      (let [s-artist @selected-artist]
        (if s-artist
          (if (:selected-album s-artist)
            ;; All albums songs
            [view
             [text {:style {:font-size 17
                            :background-color "#9303a7"
                            :color :white
                            :margin 10
                            :padding 5
                            :align-self :center}}
              (gen-utils/denormalize-entity-name-string (-> s-artist :selected-album :mb.album/name ))]
             [list-view {:dataSource (build-list-view-datasource (apply array @selected-album-songs))
                         :renderRow (comp r/as-element song)
                         ;; Takes out a warning, will be deprecated soon
                         :enableEmptySections true}]]
            
            ;; No selected album, only artist so show albums
            [view {}
             [view {:style {:flex-direction :row
                            :justify-content :center} }
              [text {:style {:font-size 17
                             :background-color "#9303a7"
                             :color :white
                             :margin 10
                             :padding-left 10
                             :padding-right 10}}
               (gen-utils/denormalize-entity-name-string (:mb.artist/name s-artist))]]
             [list-view {:dataSource (build-list-view-datasource (apply array (:artist-albums s-artist)))
                         :renderRow (comp r/as-element album)
                         ;; Takes out a warning, will be deprecated soon
                         :enableEmptySections true}]])
        
         [list-view {:dataSource (build-list-view-datasource (apply array @all-artists))
                     :renderRow (comp r/as-element artist)
                     ;; Takes out a warning, will be deprecated soon
                     :enableEmptySections true}])))))


(defn tags-line [[t1-text t1-bg t1-fg]
                 [t2-text t2-bg t2-fg]]
  (let [tag-style {:padding 20
                   :text-align "center"
                   :margin 10
                   :elevation 10
                   :flex 1
                   :font-weight :bold
                   :font-size 15
                   :color "white"}]
    [view {:flex-direction :row}
     [touchable-opacity {:on-press #(dispatch [:load-tag-songs t1-text])
                         :style {:flex 0.5}}
      [text {:style (merge tag-style {:background-color t1-bg})} t1-text]]
     [touchable-opacity {:on-press #(dispatch [:load-tag-songs t2-text])
                         :style {:flex 0.5}}
      [text {:style (merge tag-style {:background-color t2-bg})} t2-text]]]))

(defn tags-tab []
  (let [selected-tag (subscribe [:full-selected-tag])]
    (fn []
      (if-let [st @selected-tag]
        ;; show tag songs
        [view {:style {:flex 1}}
         [view {:style {:flex-direction :row
                        :justify-content :center}}
          [text {:style {:font-size 15
                         :background-color "#9303a7"
                         :color :white
                         :margin 10
                         :padding-left 10
                         :padding-right 10
                         :self-align :center
                         }}
           (:tag st)]]
         (if (pos? (count (:songs st)))
          [list-view {:dataSource (build-list-view-datasource (apply array (:songs st)))
                      :renderRow (comp r/as-element song)
                      ;; Takes out a warning, will be deprecated soon
                      :enableEmptySections true}]
          [text (str (t [:music/no-songs-under  "No songs underr "]) (:tag st))])]
        
        ;; show tags for selection
        [scroll-view 
         (for [[t1 t2] (partition-all 2 (into [] tags))]
           ^{:key (first t1)} [tags-line t1 t2])]))))

(defn search-song [s]
  [touchable-opacity {:on-press #(dispatch [:get-and-play-song (:db/id s)])}
   [view {:style {:padding 10
                  :margin 5
                  :elevation 2
                  :border-width 1
                  :border-color "rgba(0,0,0,0.1)"
                  :flex-direction :row
                  :justify-content :space-between}}
    [icon {:name "search"
           :style {:padding 10
                   :margin 5}
           :size 20}]
    [view {:flex 0.8}
     [text {:style {:font-weight :bold
                    :font-size 17}} (-> s :mb.song/name gen-utils/denormalize-entity-name-string)]
     [text {} (-> s :mb.artist/name gen-utils/denormalize-entity-name-string)]
     [text {} (-> s :mb.album/name gen-utils/denormalize-entity-name-string)]]
    [icon {:name "headphones"
           :style {:padding 10
                   :margin 5}
           :size 20}]]])

(defn search []
  (let [searching (subscribe [:searching])
        query-text-atom (r/atom "")]
    (fn []
      (let [songs @searching]
       [view {:style {:flex 0.8}}
        [view {:border-width 1
               :margin-bottom 10
               :border-color "rgba(0,0,0,0.1)"
               :background-color "#9303a7"
               :elevation 3
               :padding 10}
         [view {:background-color :white
                :flex-direction :row
                :flex 1
                :height 40}
          [icon {:name "search"
                 :style {:padding 5
                         :flex 0.1
                         :background-color :white}
                 :size 20}]
          [text-input {:placeholder (t [:music/search "Buscar musica..."])
                       :placeholder-text-color :grey
                       :style {:flex 0.8}
                       :underline-color-android :grey
                       :on-change-text #(do (reset! query-text-atom %)
                                            (dispatch [:re-search @query-text-atom]))}]
          [touchable-opacity {:on-press #(dispatch [:close-search])
                              :style {:flex 0.1}}
           [icon {:name "times"
                  :style {:padding 5
                          :background-color :white}
                  :size 20}]]]]
        [view {}
         (for [s songs]
           ^{:key (:db/id s)} [search-song s])]]))))

(defn header []
  [view {:style {:background-color "#9303a7"}}
   [tool-bar {:title "MamboBox"
              :title-color :white
              :style {:height 50}
              :actions [{:title (t [:music.menu/upload-song "Upload music"])}
                        {:title (t [:music.menu/preferences "Preferences"])}
                        {:title (t [:music.menu/search "Search"]) :icon search-img :show :always}]
              
              :on-action-selected #(case %
                                     0 (dispatch [:pick-song-and-upload])
                                     1 (dispatch [:open-preferences])
                                     2 (dispatch [:open-search]))}]])

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
      [touchable-opacity {:on-press #(dispatch [:open-edit-song-dialog (:db/id song) (t [:music.edit/new-song-name "New song name"]) nil :update-song-name])}
       [view {:style card-style}
        [view {:flex 1}
         [text {:style text-style
                :number-of-lines 1}
          (gen-utils/denormalize-entity-name-string (:mb.song/name song))]]
        [icon {:name "pencil" :size 17}]]]
      [touchable-opacity {:on-press #(dispatch [:open-edit-song-dialog (:db/id song) (t [:music.edit/new-artist-name "New artist name"]) :re-complete-artist-name :update-artist-name])}
       [view {:style card-style}
        [view {:flex 1}
         [text {:style text-style
                :number-of-lines 1}
          (-> song :artist :mb.artist/name gen-utils/denormalize-entity-name-string)]]
        [icon {:name "pencil" :size 17}]]]
      [touchable-opacity {:on-press #(dispatch [:open-edit-song-dialog (:db/id song) (t [:music.edit/new-album-name "New album name"]) :re-complete-album-name :update-album-name])}
       [view {:style card-style}
        [view {:flex 1}
         [text {:style text-style
                :number-of-lines 1}
          (-> song :album :mb.album/name gen-utils/denormalize-entity-name-string)]]
        [icon {:name "pencil" :size 17}]]]]
     [view {:style {:flex-direction :row
                    :height 100
                    :justify-content :center
                    :align-items :center}}
      (for [tag (:mb.song/tags song)]
        [touchable-opacity {:on-long-press #(dispatch [:remove-tag-from-song (:db/id song) tag])}
         [view {:key tag
                :style {:margin 5
                        :padding 5
                        :background-color (get tags tag)}}
          [text {:style {:color :white}} tag]]])
      [touchable-opacity {:on-press #(show-tag-select-dialog song)}
       [icon {:name "tags" :size 35}]]]]))

(defn expanded-player []
  (let [player-status (subscribe [:player-status])
        playing-song (subscribe [:playing-song])]
    (fn []
      (let [pl-stat @player-status
            paused? (:paused? pl-stat)
            pl-song @playing-song]
        [view {:style {:height 420
                       :justify-content :space-between}}
         [favourite-star-icon (:favourite? pl-song) (:db/id pl-song)]
         [song-editor pl-song]
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
                 :justify-content :space-between
                 :align-items :center}}
   [view {:style {:flex-direction :row
                  :flex 1}}
    [touchable-opacity {:on-press #(dispatch [:toggle-player-collapsed])
                        :style {:flex 0.9}}
     [view {:style {:flex-direction :row}}
      [icon {:name "headphones"
             :style {:padding 10
                     :margin 5
                     :background-color "rgba(0,0,0,0.1)"} 
             :size 20}]
      [view {}
       [text {:style {:font-weight :bold
                      :font-size 17}
              :number-of-lines 1}
        (-> playing-song :mb.song/name gen-utils/denormalize-entity-name-string)]
       [text {:number-of-lines 1}
        (-> playing-song :artist :mb.artist/name gen-utils/denormalize-entity-name-string)]]]]
    [view {:style {:margin 10 :flex 0.1 :flex-direction :row}}
     [touchable-opacity {:on-press #(dispatch [:toggle-play])}
      [icon {:name (if paused? "play" "pause")
             :size 25
             :style {:margin 5}}]]]]])

(defn player []
  (let [player-status (subscribe [:player-status])
        playing-song (subscribe [:playing-song])]
    (fn []
      (let [pl-stat @player-status
            pl-song @playing-song]
        [view {:style {:elevation 10
                       :padding 10
                       :background-color :white
                       }}
        (if (:collapsed? pl-stat)
          [collapsed-player pl-song (:paused? pl-stat)]
          [expanded-player])
        [video {:source {:uri (str constants/server-url (:mb.song/url pl-song))}
                :play-in-background true
                :play-when-inactive true
                :paused (:paused? pl-stat)
                :on-load #(dispatch [:play-song-ready (.-duration %)])
                :on-end #(dispatch [:playing-song-finished])
                :on-progress #(dispatch [:playing-song-progress-report (.-currentTime %)])}]]))))

(defn app-root []
  (let [selected-tab (subscribe [:selected-tab])
        player-status (subscribe [:player-status])
        searching (subscribe [:searching])
        edit-song-dialog (subscribe [:edit-song-dialog])]
    (fn []
      [view {:style {:flex 1}}
       (if @searching
         [search]
         [view {:style {:flex 0.9}}
          [header]
          [scrollable-tab-view {:initial-page @selected-tab
                                :page @selected-tab
                                :on-change-tab #(let [tab-idx (.-i %)]
                                                  (dispatch [:change-tab tab-idx])
                                                  (case tab-idx
                                                    1 (dispatch [:reload-hot-songs-if-needed])
                                                    4 (dispatch [:reload-all-artists-if-needed])
                                                    nil))
                                :tab-bar-background-color "#9303a7"
                                :tab-bar-active-text-color :white
                                :tab-bar-inactive-text-color :white
                                :tab-bar-underline-style {:background-color "white"}}
           ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
           ;; ATENTION ADDING, REMOVING OR CHANGING TABS ORDER CAN BREAK THE on-change-tab CALL ;;
           ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
           [view {:tab-label (t [:music.tabs/favourites "Favourites"]) 
                  :style {:flex 1}} [my-favourites-tab]]
           [view {:tab-label (t [:music.tabs/hot "Hot"])
                  :style {:flex 1}} [hot-tab]]
           [view {:tab-label (t [:music.tabs/genres "Genres"])
                  :style {:flex 1}} [tags-tab]]
           [view {:tab-label (t [:music.tabs/my-songs "Uploaded"])
                  :style {:flex 1}} [user-uploaded-songs-tab]]
           [view {:tab-label (t [:music.tabs/artists "Artists"]) 
                  :style {:flex 1}} [all-artists-tab]]]])
       
       (when (:playing-song-id @player-status)
         [player])
       (when @edit-song-dialog
         [edit-song-artist-album-dialog])])))



(defn init []
  (dispatch-sync [:initialize-app])
  (set! (.-disableYellowBox js/console) true)
  (.registerComponent app-registry "MamboboxMobile" #(r/reactify-component app-root))
  (.addListener device-event-emitter "uploadProgress" (fn [e] (dispatch [:upload-progress-updated
                                                                         (.-progress e)
                                                                         (.-filePath e)])))
  (.addEventListener back-android "hardwareBackPress" (fn []
                                                        (dispatch [:back])
                                                        true)))
