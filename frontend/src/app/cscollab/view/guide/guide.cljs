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
     [:b [:a {:href "https://dblp.org/"} " DBLP "]]
     ", a computer science bibliography database, with data from " [:b [:a {:href "https://csrankings.org/"} "CSRankings"]]

     " which provides detailed information about authors and their affiliation with institutions and countries. The collaboration network 
      is therefore based only on authors that are included in the Computer Science Rankings. Since every author is affiliated with an institution,
      the collaborations between authors can be aggregated by their affiliated institutions to form an affiliation network. In the collaboration/affiliation network, the nodes represent therefor
      either an author or an institution and edges represent collaborations."]
    [:p "The publications are categorized by a research area (4 main areas: AI, Systems, Theory, and Interdisciplinary Areas and 23 sub-areas),
      and for every research area only the papers from the leading conferences are considered. The conferences were selected based on the impact score
      from" [:b [:a {:href "https://research.com/conference-rankings/computer-science"} " research.com "]]
     "and the selection of conferences from CSRankings. Below you find an explorer which shows for every research area the selected conferences. With the 
      search function it can be easily checked if a conference is included in any area or not. Next to the conference titles is a DBLP icon which can be clicked 
      to get to the DBLP page of the conference."] 
    

    [conferences-view]


    [:p "The network includes a total of 76'546 publications from 127 conferences, 148'379 collaborations between authors, 14'555 authors
      , and 597 institutions. The application provides various visualizations, analytics and statistics of that network among others, a geographical 
         visualization of the collaboration network in the form of an interactive map and a graph representation
         where the nodes are grouped by research areas which are done with the help of Graph Convolutional Network Models and the t-SNE dimensionality reduction method. Further, several explorers are provided, to
         make the data transparent and accessible and allow an in-depth inspection of the data. More detailed documentation and the full code of the dataset generation, the backend, and the frontend
         can be viewed at the public GitHub repository " [:b [:a {:href "https://github.com/JoelLeupp/cscollab"} "cscollab"]] "."]

      
    ;; Data selection
    [:h3 {:style {:margin 0 :width "100%"}} "Data Selection"]
    [:p "On the right of the visualization page and the two explorers is a collapsable drawer to expand the data selection panel. Setting the filters for the network in this panel is the first step of the workflow
         of this application. The filters are valid for all pages and can be applied by clicking the button at the bottom of this panel or the refresh buttons in the top right corner of every visualization or statistic."]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/data_selection.png" :width "100%"}]]
    [:p "The data selection panel allows filtering the collaboration network based on:"
     [:ul
      [:li "The publication year (2005-2022)"]
      [:li "Research area/sub-area of the publication"]
      [:li "Region/country of the affiliated institutions"]
      [:li "An option to set a strict country/region restriction where only collaborations within the selected regions are considered.
            Otherwise, all collaborations from the selected regions are considered."]]]

           
    ;; Visualization
    [:a {:href (router/href :home)} [:h2 {:style {:margin 0 :margin-bottom 10 :width "100%"}} "Visualization"]]
    



    ;; configuration
    [:h3 {:style {:margin 0 :width "100%"}} "Configuration and Interaction Panel"]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/config_panel.png" :width "100%"}]]
    [:p "On the top of the visualization page is the graph configuration and interaction panel where the following options are available:"
     [:ul
      [:li "Choose between visualizations of the collaboration network or the affiliation network (collaborations between authors or institutions)."]
      [:li "One can select a node (author or institution) and click 'show' which will highlight the node and zoom to the node's position in the current visualization."]
      [:li "One can choose to color the nodes in the current graph visualization where one has the following options: no coloring, color by top research area,
            color by top research sub-area, color by degree centrality or color by eigenvector centrality."]]]

    ;; Views
    
    [:h3 {:style {:margin 0 :margin-bottom 10 :width "100%"}} "Geographical Visualization, Interactive Map"]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/map.PNG" :width "100%"}]]
    [:p "The selected collaboration network can be viewed as a geographical visualization in the form of an interactive map. 
         One can zoom, pan the map, and fully explore the network (also full-screen option available). 
         To adapt the map to a new network configuration, one has to click the refresh button in the right upper corner or the apply button in the data 
         selection or configuration panel.
         If you click on an edge or node (institution or author icon depending on the chosen network), all nodes or edges connected 
         to that node/edge will be highlighted in green and an information box on the right will open which shows 
         information of the node and multiple views of different visualizations of network data of that node/edge."]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/map_interaction.PNG" :width "100%"}]]
    [:p "Available visualizations of nodes are:"
     [:ul
      [:li "Publication plot: shows the publications of the collaborations by research area."]
      [:li "Year plot:  shows the publications of the collaborations by year."]
      [:li "Author list (only if the node is an institution): a list of all authors affiliated with that institution ranked by their publication count."]
      [:li "Collaboration with authors plot: shows with which authors that node had collaborated and how many publications per author."]
      [:li "Collaboration with institutions plot: shows with which institutions that node had collaborated and how my publications per institution."]
      [:li "Collaboration with countries plot: shows with which countries that node had collaborated and how many publications per countries."]]]

    ;; Graph
    
    [:h3 {:style {:margin 0 :margin-bottom 10 :width "100%"}} "Graph Representation"]
    [:p "The collaboration network is visualized as a graph where interactions are the same as with the map component. 
         The graph can be viewed in full screen and one can zoom and pan the graph and click on edges/nodes and get exactly 
         the same information box and charts on the right as in the geographical map."]
    [:p "To determine the position of the nodes, a graph convolutional network model (GCN) was used that was trained on a node classification task. 
         The task was to classify nodes (institutions or authors) by the research area or sub-area where they had to most publications in. 
         This resulted in 4 models: 2 for the author collaboration network where one predicts nodes on the top research area and the other 
         on the top sub-area and 2 models for the institutional network where one predicts nodes on the top research area and the other 
         on the top sub-area. The position of the nodes can be determined by using the activations of the hidden convolutional output layer and applying the t-SNE
        dimensionality reduction algorithm on it, which projects the results on a 2 dimensional space. The nodes can therefore be positioned
        based on the results of the GCN model and be grouped by their top research area/sub-area (determined by the 'color graph by' option in the configuration panel)."]
     
    [:h4 {:style {:margin 0 :padding 0}} "Author collaboration network colored/grouped by sub-area"] 
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/graph.PNG" :width "100%"}]]
     
    [:h4 {:style {:margin 0 :padding 0}} "Institutional collaboration network colored/grouped by area"] 
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/graph_area.PNG" :width "100%"}]]

         
    ;; Analytics
    [:h3 {:style {:margin 0 :margin-bottom 10 :width "100%"}} "Analytics"]
    [:p "Several views are available to get analytics, statistics, and visualisations 
         of the data of the selected collaboration network."]
    ;; Statistics
    [:h4  {:style {:margin 0 :padding 0}} "Statistics"]
    [:p "A table with key metrics of the selected collaboration network."]
    [:div {:style {:max-width 600 #_#_:margin :auto}} [:img {:src "img/readme/statistics.png" :width "50%"}]]
    
    [:h4  {:style {:margin 0 :padding 0}} "Centralities"]
    [:p "The top degree and eigenvector centralities of the selected network are shown (up to top 200)"]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/centrality.PNG" :width "100%"}]]
    ;; Overview
    
    [:h4  {:style {:margin 0 :padding 0}} "Overview"]
    [:p "Multiple views can be selected where each view is shown as a horizonal bar chart. 
         For the following views, an additional selection can be made where one can filter the data based on the research area/sub-area:"
     [:ul
      [:li "Number of publications by institutions"]
      [:li "Number of publications by authors"]
      [:li "Number of publications by countries"]
      [:li "Number of authors by institutions"]
      [:li "Number of authors by countries"]]]
    [:div {:style {:max-width 800 #_#_:margin :auto}} [:img {:src "img/readme/overview.PNG" :width "100%"}]]
    [:p "and two views where no research area can be selected:"
     [:ul
      [:li "Number of publications by area"]
      [:li "Number of publications by sub-area"]]]
    [:div {:style {:max-width 800 #_#_:margin :auto}} [:img {:src "img/readme/by_subarea.PNG" :width "100%"}]]
    ;; Timeline
    
    [:h4  {:style {:margin 0 :padding 0}} "Timeline"]
    [:p "Here the temporal dimension of the publications of the selected network is shown in 3 views:"
     [:ul
      [:li "Total publications over time"]
      [:li "Publications by area over time"]
      [:li "Publications by sub-area over time"]]]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/timeline.PNG" :width "100%"}]]
    ;; Institution
    
    [:h4  {:style {:margin 0 :padding 0}} "Institution"]
    [:p "An institution can be selected together with a view, where the views are exactly the same as the views in the info box 
         when an institutional node is selected in the map/graph:"
     [:ul
      [:li "Publication plot: shows the publications of the collaborations by research area"]
      [:li "Year plot:  shows the publications of the collaborations by year"]
      [:li "Author list: a list of all authors affiliated with that institution ranked by their publication count"]
      [:li "Collaboration with authors plot: shows with which authors that node had collaborated and how many publications per author"]
      [:li "Collaboration with institutions plot: shows with which institutions that node had collaborated and how my publications per institution"]
      [:li "Collaboration with countries plot: shows with which countries that node had collaborated and how many publications per countries"]]]
    [:div {:style {:max-width 800 #_#_:margin :auto}} [:img {:src "img/readme/institution.PNG" :width "100%"}]]
    ;; Author
    
    [:h4  {:style {:margin 0 :padding 0}} "Author"]
    [:p "An author can be selected together with a view, where the views are exatly the same as the views in the infobox 
         when an author node is selected in the map/graph:"
     [:ul
      [:li "Publication plot: shows the publications of the collaborations by research area"]
      [:li "Year plot:  shows the publications of the collaborations by year"]
      [:li "Collaboration with authors plot: shows with which authors that node had collaborated and how many publications per author"]
      [:li "Collaboration with institutions plot: shows with which institutions that node had collaborated and how my publications per institution"]
      [:li "Collaboration with countries plot: shows with which countries that node had collaborated and how many publications per countries"]]]
    [:div {:style {:max-width 800 #_#_:margin :auto}} [:img {:src "img/readme/author.PNG" :width "100%"}]]

    
    ;; Author Explorer 
    [:a {:href (router/href :author-explorer)} [:h2 {:style {:margin-bottom 10 :width "100%"}} "Author Explorer"]]
    [:p "On this page one can easily explore which authors are included in a selected collaboration network defined by the data selection panel. 
         Based on the selected network one can now select a country
         that is included in the network and get a nested table with all the institutions of that country, each with a count of how many authors
         are affiliated with that institution. If the institution is expanded, one gets a list of the authors together with a link to the DBLP page of that author.
         It is also possible, to search directly for an author and click on 'show' which will direct the user to the respective country and institution of that author."]
    [:div {:style {:max-width 800 #_#_:margin :auto}} [:img {:src "img/readme/author_explorer.PNG" :width "100%"}]]


    ;; Publication Explorer 
    [:a {:href (router/href :publication-explorer)} [:h2 {:style {:margin-bottom 10 :width "100%"}} "Publication Explorer"]]
    [:p "On this page one can explore the publications that are included in the collaboration network.
         At the top of the page, there are the same graph filters as on the main page with which a collaboration network can be selected.
         The publications are shown in a nested list where at the top level we have the research area followed by the research sub-area. The
         publications from the sub-area are grouped by year first and then by conference. Every publication is linked to its DBLP record page
         and the title of the publication is displayed as well as the authors from the selected collaboration network."]
    [:div {:style {:max-width 1100 #_#_:margin :auto}} [:img {:src "img/readme/publication_explorer.PNG" :width "100%"}]]
    [:p "The publications from the selected network can be further filtered by the following available options:"]
    [:ul
     [:li "Show all publications"]
     [:li "Show all publications from a selected author"]
     [:li "Show all publications from a selected institution"]
     [:li "Show all publications from the collaboration between two authors"]
     [:li "Show all publications from the collaboration between two institutions"]]]])