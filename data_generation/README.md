# Data Generation 

## Data File Overview

Downloaded files from https://github.com/emeryberger/CSrankings are in the */data* directory: 
  
* country-info.csv
* csrankings.csv
* generated-author-info.csv

Generated files are in the */output* directory:

*/mapping* (Files for geographical and institutional mapping):

* geo-codes-auto.csv
* inst-geo-map.csv
* geo-codes.csv
* geo-mapping.json
* geo-mapping.csv (final geo file with geographic information for each institution **country, region, geo-coordinates**)

*/pid* (Name to pid mapping from authors in csrankings):

* authors-pid-all.csv
* missing-authors.csv
* authors-pid.csv
  
*/dblp* (Data extraction from the dblp xml dump):

* authors.json
* collabs.json
* inproceedings.json
* proceedings.json

*/graph* (Combination of dblp and csrankings in a node and edge data structure):

* edges_affiliated.csv
* edges_collabs.csv
* edges_conf_belongs_to.csv
* edges_crossref.csv
* edges_in_region.csv
* edges_sub_area_of.csv
* nodes_area.csv
* nodes_authors.csv
* nodes_countries.csv
* nodes_inproceedings.csv # replaced \\\\" with \\\\\\\"
* nodes_institution.csv
* nodes_proceedings.csv
* nodes_regions.csv
* nodes_sub_area.csv


## Virtual Environment 

Always run python code in the virutal environment of this directory

```{shell}
virtualenv venv
source venv/bin/activate
pip install -r requirements.txt
```

## Generate Geographical Mapping

The script *geo-mapping.py* takes the *country-info.csv* and *csrankings.csv* and generates the file *inst-geo-map.csv* which consists of all institutions present in csrankings and adds region/country codes and labels for each institution. 

With the API https://api.geoapify.com/v1/geocode/search? the script searches for geo-coordinates for every institution. If there is a match, the name of the found building is compared with the name of the institution using the levenstein distance to veryfy the match and assign a status (OK, WATCH, WRONG) to the result and generates the file *geo-codes-auto.csv*. In the file 210 Institutions where OK, 263 where on WATCH and for 136 the API no geo-coordinates where found.

The file *geo-codes.csv* was edited manualy by going through the file *geo-codes-auto.csv* and check all institutions which do not have an OK status and if the entry is wrong or missing add the true coordinates with the help of google maps. 

Lastly the file *geo-codes.csv* and *inst-geo-map.csv* are joined together to create the file *geo-mapping.csv*. The file *geo-mapping.json* is an adaptation of *geo-mapping.csv* with a slithly differernt structure. 

geo-mapping.csv (609 institutions):

institution|country-id|country-name|region-id|region-name|lat|lon
| -------- | ------- |------- |------- |------- |------- |------- |
|AUEB|gr|Greece|europe|Europe|37.9940338898775|23.732771540394|
|Aalborg University|dk|Denmark|europe|Europe|57.01590705|9.9753082435574|

## Author Name to ID Mapping

To connect the data from csranking with the dblp the author names from csranking must be mapped with the internal author ID (pid) from dblp. The file *gen_pid.py* is used to generate this mapping. With the help of the dblp API https://dblp.org/search/author? the script could exactly identify and get the pid of over 98% of the authors in *csrankings.csv*. The result is saved in the file *authors_pid_all.csv*. 

The file *missing-authors.csv* is a list of all authors and their affiliation for which the script couldn't find an excat match. Looking at this file it can be seen that 49 authors affiliated with Switzerland, Germany or Austria are missing. For those missing authors I manualy looked at the result of the API https://dblp.org/search/author?xauthor and if there are multiple options I checked the API https://dblp.org/pid/$pid$.xml with all possible pid's and checked the affiliation to manualy find the matching pid. For some authors like "Anelis Kaiser" there is no person registerd in the dblp with such a name and no authors can be found. For others there are two differnt author names in csrankings.csv like "Christian Holz 0001" and "Christian Holz" but they are actualy the same person. In some cases the name from csranking.csv had multiple occurences in dblp and dblp adds a 4 digit number at the end of the name for this cases the matching pid can be found by checking all the matches. With manualy going through the 49 authors from GE,CH,AU additional 18 authors could be mapped.

Lastly, the names in csrankings are not unique for one person, meaning an author can have multiple occurences with different name variations like "Kalinka Regina Lucas Jaquie Castelo Branco" or "Kalinka R. L. J. C. Branco" or "Kalinka Branco" which are all mapped to the same pid and are therefor the same person. The dataset *authors-pid-all.csv* is trimmed suched that the pid to name mapping is a one to one mapping and this results in the file *authors-pid.csv*. 

authors-pid.csv (20986 unique authors):

| author  | pid |
| ------------- | ------------- |
| A Min Tjoa  | t/AMinTjoa  |
| A. Akbari Azirani  | 36/9367  |

## Extract DBLP data

Latest XML dump dowloaded at 12.12.2022 from https://dblp.org/xml/

* dblp-2022-12-01.xml.gz: https://dblp.org/xml/release/dblp-2022-12-01.xml.gz
* dblp-2019-11-22.dtd: https://dblp.org/xml/release/dblp-2019-11-22.dtd

DBLP XML APIs:

* Proceedings (records in general): https://dblp.org/rec/*$crossref*.xml
* All records assosiated with a certain pid: https://dblp.org/pid/*$pid*.xml


The file *parse_dblp.py* containes the parsers for the dblp.xml dump and the extract_dblp.py runs the parsers and extracts data abpout the authors, in/proceedings and collaborations later than the year 2005 and saves them in form of json files in the *output/dblp* directory.

Parsers:

*  parse_proceedings(cut_off=2005): outputs the file proceedings.json (41'507 proceedings)
*  parse_authors(): outputs the file authors.json  (3'220'379 authors) The pid is the unique dblp person identifier. In the xml file is no clear mapping of pid and author names but there is a “www” xml element tag which lists all dblp pages and every author has a dblp homepage and the url of the dblp homepage uses the pid. This way the pid can be extracted and mapped to all names occurring in the author homepage record under the “www” tag. In the dblp dataset an author can have multiple name versions like “Kalinka R. L. J. C. Branco" and "Kalinka Branco" is the same author and mapped to the same pid. Therefor the authors.json file can have multiple occurrences of the same author (pid) in combination with different names.
* parse_inproceedings(proceedings_ids, name_pid_map, cut_off=2005): outputs the files inproceedings.json (2'542'194 inproceedings) and collabs.json (15'641'542 collaborations) In the xml file the authors of the inproceedings are listed with their name and not pid this is why a mapping of name->pid (generated from authors.json) is given as input such that for every pairwise author combination in an inproceeding the collaboration can be constructed with their pid’s and the id of the inproceeding and create an additional collaboration id (edge/id) that is a combination of the 3. 
The inproceeding must have a cross-reference to the proceeding to which they belong but unfortunately this is not always the case and the crossref tag is sometimes missing. However, the dblp url of the inpoceeding and the id of the corresponding proceeding are very similar and have a root part in common. This is why as an additional input all the ids of proceedings taken from the proceeding.json is given such that the url could be matched with a fuzzy search to a proceeding id and this proceeding could than be taken as the crossref id. If no clear match could be made the inproceeding or the provided corssref is not a valid id the proceeding is getting ignored. This guarantees us that all inproceedings have a valid reference to an available proceeding.


Output file overview:

### authors.json (3'220'379 authors)

```{shell}
[
   {
      "pid": "308/3672",
      "name": "Raden Putra"
   },
   ...
]
```

### proceedings.json (41'507 proceedings)

```{shell}
[
   {
      "id": "conf/coopis/2022",
      "title": "Cooperative Information Systems - 28th International Conference, CoopIS 2022, Bozen-Bolzano, Italy, October 4-7, 2022, Proceedings",
      "conf": "CoopIS",
      "year": 2022
   },
   ...
]
```

### inproceedings.json (2'542'194 inproceedings)

```{shell}
[
   {
      "id": "conf/coopis/WangLG22",
      "title": "Bi2E: Bidirectional Knowledge Graph Embeddings Based on Subject-Object Feature Spaces.",
      "year": 2022,
      "crossref": "conf/coopis/2022"
   },
   ...
]
```

### collabs.json (15'641'542 collaborations)

```{shell}
[
   {
      "node/u": "75/3158",
      "node/v": "53/4014",
      "rec/id": "conf/coopis/WangLG22",
      "edge/id": "75/3158-53/4014-conf/coopis/WangLG22"
   },
   ...
]
```

# Computer Science Area Mapping

The mapping of conferences to computer science areas is saved under output/dblp/area-mapping.json 
and the file area_mapping.py was used to support the creation of the file.

The areas are inspired by [csrankings.org](https://csrankings.org/) and [research.com](https://research.com/). 
All the conferences present in csrankings are also in the area-mapping and since the focus is on the field of AI
the top 200 rank AI related conferences rated by research.com are also included in the mapping as well 
as the conferences regarding AI listed on  [List_of_computer_science_conferences](https://en.wikipedia.org/wiki/List_of_computer_science_conferences#Artificial_intelligence). The categorized AI conferences include 2160 proceedings and 
229'958 inproceedings and all categorized conferences include 5488 proceedings and 391'856 inproceedings.

file structure of area-mapping.json
```{shell}
{
   "ai":{
       "label":"AI",
       "areas":{
          "vision":{
             "label":"Computer Vision",
             "conferences":[
                "iccv",
                "cvpr",
                "eccv",
                "bmvc",
                "accv",
                "avss"
             ]
          },
          ...
   },
   ...
}
```
The areas are categorized as follows:

| Area                    | Sub Area                              |
|-------------------------|---------------------------------------|
| AI                      | Computer Vision                       |
| AI                      | Artificial Intelligence               |
| AI                      | Natural language Processing           |
| AI                      | Machine Learning                      |
| AI                      | Information Retrieval                 |
| Systems                 | Computer Hardware and Architecture    |
| Systems                 | Computer Networks and Communications  |
| Systems                 | Computer Security and Cryptography    |
| Systems                 | Databases & Information Systems       |
| Systems                 | High-performance computing            |
| Systems                 | Web, Mobile & Multimedia Technologies |
| Systems                 | Measurement & perf. analysis          |
| Systems                 | Operating systems                     |
| Systems                 | Programming languages                 |
| Systems                 | Software engineering                  |
| Systems                 | Design automation                     |
| Systems                 | Embedded & real-time systems          |
| Theory                  | Computational Theory and Mathematics  |
| Interdisciplinary Areas | Biomedical & Medical Engineering      |
| Interdisciplinary Areas | Computer graphics                     |
| Interdisciplinary Areas | Economics & computation               |
| Interdisciplinary Areas | Human-computer interaction            |
| Interdisciplinary Areas | Robotics                              |
| Interdisciplinary Areas | Visualization                         |