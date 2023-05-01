(ns app.cscollab.view.guide.guide
  (:require
   [app.components.layout :as acl]
   [app.components.colors :as colors]
   [app.cscollab.view.conference.conferences :refer (conferences-view)]
   [app.router :as router]))

(defn guide-view []
  #_[acl/section {:color :white :style {:font-size 20}}]
  [:div {:style {:width "100%" :background-color :white :font-size 20}}
   [:div {:style {:padding 20}}

    ;; Overview
    [:h2 {:style {:margin 0 :width "100%"}} "Overview"]
    [:p {:style {:width "100%"}}
     "This application serves as a visualizer, analyzer and explorer of the collaboration network of computer science
            conference papers from the year 2015-2022 with a focus on the field of AI. The collaboration network was created by combining data from"
     [:b [:a {:href "https://dblp.org/"} " dblp "]]
     "a computer science bibliography database with data from " [:b [:a {:href "https://csrankings.org/"} "csrankings"]]

     " which provides detailed information about authors and their affiliation to institutions and countries. The collaboration network 
      is therefore based only on authors that are included in the Computer Science Rankings. Since every author is affiliated with an institution
      the collaborations between authors can be aggregated by their institution. In the collaboration network, the nodes represent therefor
      either an author or an instituion and edges represent collaborations."]
    [:p "The publications are categorized by the research areas (4 main areas: AI, Systems, Theory and Interdisciplinary Areas and 23 sub areas)
      and for every research area only the papers from the leading conferences are considered. The conferences were selected based on the impact score
      from" [:b [:a {:href "https://research.com/conference-rankings/computer-science"} " research.com "]]
     "and the selection of conferences from CSRankings. Below you find an explorer which shows for every research area the selected conferences and with the 
      search function it can be easily checked if a confernce is included in any area or not. The next to the conference titles is a dblp icon which can be clicked 
      to get to the dblp page of the conference."]

    [conferences-view]


    [:p "The network includes a total of 76'546 publications from 127 Conferences, 148'379 individual collaborations between authors, 14'555 Authors
      and 597 Institutions. The application provides various visualizations, analytics and statistics of that network among others a geographical 
         visualization of the collaboration network in the form of an interactive map and a graph representation
         where the nodes are grouped by reseach areas which is done by using Graph Convolutional Network Models. Further, several explorers are provided to
         make the data transparent and accessible and allow a detailed inspecation of the data. A more detailed documentation and the full code of the dataset generation, the backend and the frontend
         can be viewed at the public GitHub repository " [:b [:a {:href "https://github.com/JoelLeupp/cscollab"} "cscollab"]] "."]

    ;; Visualization
    [:a {:href (router/href :home)} [:h2 {:style {:margin 0 :margin-bottom 10 :width "100%"}} "Visualization"]]

    ;; Filters
    [:h3 {:style {:margin 0 :width "100%"}} "Filters, Configuration and Interactions"]
    [:p "On top of the page there is a panel where one can filter, configer and interact with the collaboration network. 
         To apply the filters and load the new collaboration data the refresh button in the top right corner 
         of the visualization views has to be clicked."]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/config.PNG" :width "100%"}]]
    [:p "The graph filter panel allows to filter the collaboration network based on:"
     [:ul
      [:li "the publication year (2005-2022)"]
      [:li "research area/sub area of the publication"]
      [:li "region/country of the authors or affiliated institutions"]
      [:li "An option to set a strict country/region restriction where only collaborations within the selected regions are considered
            otherwise all collaborations from the selected regions are considered."]]
     "In the graph configuaration and interaction panel the following options are available:"
     [:ul
      [:li "chose collaboration network: collaborations can be between authors or institutions"]
      [:li "one can select a node (author or institution) and click 'show' which will highlight the node and zoom to the node position in the current visualization"]
      [:li "one can choose to color the nodes in the current graph visualization where one has the following options: no coloring, color by top area,
            color by top sub area, color by degree centrality or color by eigenvector centrality"]]]

    ;; Views
    [:h3 {:style {:margin 0 :margin-bottom 10 :width "100%"}} "Geographical Visualization, Interactive Map"]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/map.PNG" :width "100%"}]]
    [:p "The selected collaboration network can be viewed as a geographical visualisation in form of an interactive map. 
         One can zoom and pan the map and fully explore the network (also full screen option available). 
         To adapt the map to a new network configuration one has to click the refresh button in the right upper corner.
         If you click on an edge or node (institution or author icon depending on the chosen network) all nodes or edges connected 
         to that node/edge will be highlighted in green and an information box on the right will open which shows 
         information of the node and multiple tabs with different visualizations of network data of that node/edge."]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/map_interaction.PNG" :width "100%"}]]
    [:p "Available visulaizations of nodes are:"
     [:ul
      [:li "Publication plot: show in how many publications of each research area that node collaborated in"]
      [:li "Auhtor list (only if the node is an institution): a list of all authors affiliated with that institution ranked by their publication count"]
      [:li "Institution plot: shows with which institutions that node had collaborated and how my publications per institution"]
      [:li "Country plot: shows with which countries that node had collaborated and how many publications per countries"]
      [:li "Year plot: shows in how many publicaitons that node collaborated in each year"]
      [:li "Author collaboration plot: shows with which authors that node had collaborated and how many publications per authors"]]]

    ;; Graph
    [:h3 {:style {:margin 0 :margin-bottom 10 :width "100%"}} "Graph Representation"]
    [:p "The collaboration network is visualized as a graph where interactions are the same as with the map component. 
         The graph can be viewed in full screen and one can zoom and pan the graph and click on edges/nodes and get exactly 
         the same information box and charts on the right as in the geographical map."]
    [:p "To determin the position of the nodes a graph convolutional network model (GCN) was used that was trained on a node classification task. 
         The task was to classify nodes (institutions or authors) by the research area or sub area where they had to most publicaitons in. 
         This resulted in 4 models: 2 for the author collaboration network where one predicts nodes on the top research area and the other 
         on the top sub area and 2 models for the institutional network where one predicts nodes on the top research area and the other 
         on the top sub area. The position of the nodes can be determined by using the last hidden convolutional output layer and apply 
         a dimensionality reduction algorithm on it that project the results on a 2 dimensioanl plane. The nodes can therefor be positioned
        based on the results of the GCN model and be grouped by their top research area/sub area (determined by the 'color graph by' option)"]
    [:h4 {:style {:margin 0 :padding 0}} "Author collaboration network colored/grouped by sub area"]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/graph.PNG" :width "100%"}]]
    [:h4 {:style {:margin 0 :padding 0}} "Institutional collaboration network colored/grouped by area"]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/graph_area.PNG" :width "100%"}]]

    ;; Analytics
    [:h3 {:style {:margin 0 :margin-bottom 10 :width "100%"}} "Analytics"]
    [:p "Several views are availbe to get analytics, statistics and visualitations 
         of the data of the selected collaboration network."]
    ;; Statistics
    [:h4  {:style {:margin 0 :padding 0}} "Statistics"]
    [:p "A table with key metrics of the selected collaboration network."]
    [:div {:style {:max-width 600 #_#_:margin :auto}} [:img {:src "img/readme/statistics.PNG" :width "100%"}]]
    [:h4  {:style {:margin 0 :padding 0}} "Centralities"]
    [:p "The top degree and eigenvector centralities of the selected network is shown (up to top 200)"]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/centrality.PNG" :width "100%"}]]
    ;; Overview
    [:h4  {:style {:margin 0 :padding 0}} "Overview"]
    [:p "Multiple views can be selected where each view is shown as a horizonal bar chart. 
         For the following views an additonal selection can be made were one can filter the data based on the research area/sub area:"
     [:ul
      [:li "number of publications by institutions"]
      [:li "number of publications by authors"]
      [:li "number of publications by countries"]
      [:li "number of authors by institutions"]
      [:li "number of authors by countries"]]]
    [:div {:style {:max-width 800 #_#_:margin :auto}} [:img {:src "img/readme/overview.PNG" :width "100%"}]]
    [:p "and two views where no research area can be selected:"
     [:ul
      [:li "number of publications by area"]
      [:li "number of publications by sub area"]]]
    [:div {:style {:max-width 800 #_#_:margin :auto}} [:img {:src "img/readme/by_subarea.PNG" :width "100%"}]]
    ;; Timeline
    [:h4  {:style {:margin 0 :padding 0}} "Timeline"]
    [:p "Here the temporal dimension of the publications of the selected network is shown in 3 view:"
     [:ul
      [:li "total publications over time"]
      [:li "publications by area over time"]
      [:li "publications by sub area over time"]]]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/timeline.PNG" :width "100%"}]]
    ;; Institution
    [:h4  {:style {:margin 0 :padding 0}} "Institution"]
    [:p "An institution can be selected and a view, where the views are exatly the same as the views in the infobox 
         when an institutional node is selected in the map/graph:"
     [:ul
      [:li "Publication plot: show in how many publications of each research area the selected institution collaborated in"]
      [:li "Auhtor list: a list of all authors affiliated with the selected institution ranked by their publication count"]
      [:li "Institution plot: shows with which institutions the selected institution had collaborated and how my publications per institution"]
      [:li "Country plot: shows with which countries the selected institution had collaborated and how many publications per countries"]
      [:li "Year plot: shows in how many publicaitons the selected institution collaborated in each year"]
      [:li "Author collaboration plot: shows with which authors the selected institution had collaborated and how many publications per authors"]]]
    [:div {:style {:max-width 800 #_#_:margin :auto}} [:img {:src "img/readme/institution.PNG" :width "100%"}]]
    ;; Author
    [:h4  {:style {:margin 0 :padding 0}} "Author"]
    [:p "An author can be selected and a view, where the views are exatly the same as the views in the infobox 
         when an author node is selected in the map/graph:"
     [:ul
      [:li "Publication plot: show in how many publications of each research area the selected author collaborated in"]
      [:li "Institution plot: shows with which institutions the selected author had collaborated and how my publications per institution"]
      [:li "Country plot: shows with which countries the selected author had collaborated and how many publications per countries"]
      [:li "Year plot: shows in how many publicaitons the selected author collaborated in each year"]
      [:li "Author collaboration plot: shows with which authors the selected author had collaborated and how many publications per authors"]]]
    [:div {:style {:max-width 800 #_#_:margin :auto}} [:img {:src "img/readme/author.PNG" :width "100%"}]]

    ;; Author Explorer 
    [:a {:href (router/href :author-explorer)} [:h2 {:style {:margin-bottom 10 :width "100%"}} "Author Explorer"]]
    [:p "On this page one can easily explore which authors are included in a selected collaboration network. 
         At the top of the page there are the same graph filters as on the main page with which a collaboration network can be selected. 
         Based on the selected network one can now select a country
         that is included in the network and get a nested table with all the institutions of that country each with a count of how many auhtors
         are affiliated with that institution. If the instiution is expanded one get a list of the athors together with a link to the dblp page of that author.
         It is also possible to search directly for an author and click on 'show' which will direct the user to the respective country and institution of that author."]
    [:div {:style {:max-width 800 #_#_:margin :auto}} [:img {:src "img/readme/author_explorer.PNG" :width "100%"}]]

    ;; Conference Explorer 
    [:a {:href (router/href :conference-explorer)} [:h2 {:style {:margin-bottom 10 :width "100%"}} "Conference Explorer"]]
    [:p "On this page all the leading conferences are listed that are included in the respective area/sub area and linked to the dblp conference page. 
         A conference can also be searched for and one can click the button 'show' to see in wich sub area it belongs.
         In total 127 conferences can be included in the collaboration network."]
    [:div {:style {:max-width 800 #_#_:margin :auto}} [:img {:src "img/readme/conference_explorer.PNG" :width "100%"}]]

    ;; Publication Explorer 
    [:a {:href (router/href :publication-explorer)} [:h2 {:style {:margin-bottom 10 :width "100%"}} "Publication Explorer"]]
    [:p "On this page on can explore the publications that are included in the collaboration network.
         At the top of the page there are the same graph filters as on the main page with which a collaboration network can be selected.
         The publications are shown in a nested list where at the top level we have the research area followed by the research sub aree. The
         publications from the sub area are grouped by year first and then by conference. Every publication is linked to its dblp record page
         and the title of the publication is displayed as well as the auhtors from the selected collaboration network."]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/publication_explorer.PNG" :width "100%"}]]
    [:p "The publications from the selected network can be further filtered and following options are available:"]
    [:ul
     [:li "Show all publications"]
     [:li "Show all publications from a selected author"]
     [:li "Show all publications from a selected institution"]
     [:li "Show all publications from the collaboration between two authors"]
     [:li "Show all publications from the collaboration between two institutions"]]]])