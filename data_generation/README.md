# Data Generation 

## Data File Overview

Downloaded files from https://github.com/emeryberger/CSrankings are in the *data/* directory: 
  
* country-info.csv
* csrankings.csv
* generated-author-info.csv

Generated files are in the */output* directory:

*mapping/* (Files for geographical and institutional mapping):

* geo-codes-auto.csv
* inst-geo-map.csv
* geo-codes.csv
* geo-mapping.json
* geo-mapping.csv (final geo file with geographic information for each institution **country, region, geo-coordinates**)

*pid/* (Name to pid mapping from authors in csrankings):

* authors-pid-all.csv
* missing-authors.csv
* authors-pid.csv
  
*dblp/* (Data extraction from the dblp xml dump):

* authors.json
* collabs.json
* inproceedings.json
* proceedings.json

*graph/* (Combination of dblp and csrankings in a node and edge data structure):

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

Use *data_generation/* as the working directory and always run python code in the virutal environment of this directory

```{shell}
virtualenv venv
source venv/bin/activate
pip install -r requirements.txt
```

## Generate Geographical Mapping

The script *scripts/csrankings/geo-mapping.py* takes the *country-info.csv* and *csrankings.csv* and generates the file *inst-geo-map.csv* which consists of all institutions present in csrankings and adds region/country codes and labels for each institution. 

With the API https://api.geoapify.com/v1/geocode/search? the script searches for geo-coordinates for every institution. If there is a match, the name of the found building is compared with the name of the institution using the levenstein distance to veryfy the match and assign a status (OK, WATCH, WRONG) to the result and generates the file *geo-codes-auto.csv*. In the file 210 Institutions where OK, 263 where on WATCH and for 136 the API no geo-coordinates where found.

The file *geo-codes.csv* was edited manualy by going through the file *geo-codes-auto.csv* and check all institutions which do not have an OK status and if the entry is wrong or missing add the true coordinates with the help of google maps. 

Lastly the file *geo-codes.csv* and *inst-geo-map.csv* are joined together to create the file *geo-mapping.csv*. The file *geo-mapping.json* is an adaptation of *geo-mapping.csv* with a slithly differernt structure. 

geo-mapping.csv (609 institutions):

institution|country-id|country-name|region-id|region-name|lat|lon
| -------- | ------- |------- |------- |------- |------- |------- |
|AUEB|gr|Greece|europe|Europe|37.9940338898775|23.732771540394|
|Aalborg University|dk|Denmark|europe|Europe|57.01590705|9.9753082435574|

## Author Name to ID Mapping

To connect the data from csranking with the dblp the author names from csranking must be mapped with the internal author ID (pid) from dblp. The file *scripts/csrankings/gen_pid.py* is used to generate this mapping. With the help of the dblp API https://dblp.org/search/author? the script could exactly identify and get the pid of over 98% of the authors in *csrankings.csv*. The result is saved in the file *authors_pid_all.csv*. 

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


The file *scripts/dblp/parse_dblp.py* containes the parsers for the dblp.xml dump and the scripts/dblp/extract_dblp.py runs the parsers and extracts data abpout the authors, in/proceedings and collaborations later than the year 2005 and saves them in form of json files in the *output/dblp* directory.

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
and the file scripts/dblp/area_mapping.py was used to support the creation of the file.

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

## Graph Data - Combine Datasets

The file *scripts/graph/graph_data.py* is used to combine data from csrankings and dblpdb and bring it in a node/relation structure in the form of csv files
which are saved under the directory *output/graph/* which can than be directly used to integrate the data into a graph database. While combining
the two data sources the script always checks for the validity of the data (some pids from the csrankings datasets where invalid and had to be corrected) also the titles of 
inproceedings or names of authors are cleaned from special characters such that it can be read into the kuzudb without complications. 

The title of conferences couldn't be extracted from the dblp.xml dump but the conference id is contained in the proceeding id. On the dblp website there is a page for conferences which include the conference id in the url and the title (h1 tag) is equal to the title of the conference. This way the script could generate from the proceedings a conference node and a relation form conferences to proceedings. Also the script added two new regions not present in the csrankings geo-mapping.csv namely the region "DACH" (CH, AT, DE) and "World" which includes all countries. Since there are over 3mil authors and 15mil collaborations extracted from the dblp.xml dump but mainly only the authors and collaborations between authors from csranking are of interest (they are the only ones which contain affiliations and can be mapped to countries), a trimmed version of the author (nodes_authors_csrankings.csv) and collaborations (edges_collabs_csrankings.csv) is also created and is merely a subset of the main files. 

## KUZU DB

The in-process property graph database management system (GDBMS) [kuzudb](https://kuzudb.com/) is used to store and query the generated graph data. The files used to integrate
the data into kuzu db and provide imporant queries are under the direcotry *scripts/kuzudb/*. 

*init_kuzu.py* creates a disc based instance of the kuzudb, defines all the schemas of the nodes and relations and finaly loads all the data from *output/graph/* into the 
respective nodes and relations in the db.An overview of the schema and a summary of the entries can be seen in the diagram below. The circles represent nodes and the lines/arrows relations. The numbers below/next to the name of the node/relation represent the number of entries in the db. The bullet points are the properties of the node/relations and a black bold property means that it is used as the primary key. Some properties are colored, where the properties with the same color are identical and can be matched. (This is mainly for performance reasons to enable faster queries and reduce the number of joins)

![kuzudb_schema](https://user-images.githubusercontent.com/34285164/213651650-d266e3b6-4e36-485d-935a-d51f8d69cdaa.svg)

The script *init_kuzu.py* contains functions which perform important queries which can later be used for the analytics, GNN and frontend. 
A general idea which is consitent over all functions is that there is a main configuration for the filtering:

* Time interval of interest: Which years to consider given with starting year "from" and an end year "to". If from is not given it will take the first available year (2005) and if to is not given it will take the latest (2023)
* Area of interest: The computer science area or sub-area to consider given by its id and the type ("a" for area and "s" for sub-area). If no area is specified consider all.
* Regioanl Interst: Region or country of interest given by their id. With an additional configuration "strict_boundary" which if set to "true" only consideres collaborations of authors within the given region but if it is set to "false" only one author in collaboration must be from the given region.
* Institutional or Author interst: If institution is set to true one is interested in the collaborations between institutions else one is interest in collaborations between authors.

This conifg is given as input for almost all query functions but not all entries are used by every query. 

Example config where one is interested only in the data later or equal to the year 2010, which is assosiated with the field of AI and only collaborations and authors within the german speaking DACH region:

```{shell}
{  "from_year": 2010,
   "to_year": None,
   "area_id" : "ai", 
   "area_type": "a", 
   "region_id": "dach",
   "country_id": None,
   "strict_boundary": True,
   "institution":False
}
```

Because of the large size of some tables in the db not all queries could be written in a single [cypher](https://kuzudb.com/docs/cypher) query
since there would be to many joins and conditions that lead to a memory error or very slow performance. This is why some queries are split up and joined/
aggregated with python. The functions are optimzed and if needed broken in multiple functions to make it easier to add a smart cacheing. 

### Query Functions

**get_region_mapping()**

output is a flat table of all countries and regions with the shape (113,4):

| country-id | country-name   | region-id | region-name |
|------------|----------------|-----------|-------------|
| cz         | Czech Republic | europe    | Europe      |
| hu         | Hungary        | europe    | Europe      |

**get_area_mapping()**

output is a flat table of all areas, sub-areas and the corresponding conferences with the shape (128, 6):

| area-id | area-label | sub-area-id | sub-area-label                     | conference-id | conference-title                                  |
|---------|------------|-------------|------------------------------------|---------------|---------------------------------------------------|
| systems | Systems    | security    | Computer Security and Cryptography | ndss          | Network and Distributed System Security Sympos... |
| systems | Systems    | security    | Computer Security and Cryptography | sp            | IEEE Symposium on Security and Privacy (S&P)      |
| ai      | AI         | ml          | Machine Learning                   | ijcnn         | IEEE International Joint Conference on Neural ... |

**get_by_area(config={:keys [from_year, to_year, return_type, area_id, area_type]})**

The function gives back a table with all the inproceedings (return_type="i") or proceedings (return_type="p") that are from the computer science 
area given by the config within the given timespan. 

example output of get_by_area({"area_id":"ai", "area_type":"a", "from_year":2010, "return_type":"p"}):

| id              | title                                             | conf  | year |   |
|-----------------|---------------------------------------------------|-------|------|---|
| conf/ijcnn/2010 | International Joint Conference on Neural Netwo... | ijcnn | 2010 |   |
| conf/ijcnn/2011 | The 2011 International Joint Conference on Neu... | ijcnn | 2011 |   |
| conf/ijcnn/2017 | 2017 International Joint Conference on Neural ... | ijcnn | 2017 |   |

**get_conference(conf)**

get all the inproceedings and proceedings from a given confernce id.

example output from get_conference("aaai"):

| p.id           | p.title                                           | year | i.id                  | i.title                                           |
|----------------|---------------------------------------------------|------|-----------------------|---------------------------------------------------|
| conf/aaai/2016 | Proceedings of the Thirtieth AAAI Conference o... | 2016 | conf/aaai/SternKS16   | Implementing Troubleshooting with Batch Repair.   |
| conf/aaai/2019 | The Thirty-Third AAAI Conference on Artificial... | 2019 | conf/aaai/MarinoMTL19 | Evolving Action Abstractions for Real-Time Pla... |
| conf/aaai/2021 | Thirty-Fifth AAAI Conference on Artificial Int... | 2021 | conf/aaai/WangX21     | Efficient Object-Level Visual Context Modeling... |

**get_weighted_collab(config={})**

This function is a wrapper function which combines the functions get_collaboration(config={}) which gets all the collaboration based on the constraints from the config 
and weighted_collab(collabs, config = {}) which aggregates the collaborations to a weighted collaboration based on authors or institutions. The result is table that
contains two author pids or two institution names and a weight (number of collaborations). This relation is undirected and each pair is unique. 

example output from get_weighted_collab({from_year":2010, "area_id":"ai", "area_type":"a", "region_id":"dach","strict_boundary":True}):

| a         | b       | weight |
|-----------|---------|--------|
| 24/8616   | 61/5017 | 94     |
| 69/4806-1 | 87/6573 | 46     |
| 69/4806-1 | 95/1130 | 41     |

example output get_weighted_collab({"from_year": 2010, "institution":True}) which get the weighted worldwide collaborations of proceedings that are assosiated with a
computer science area.

| a                           | b                           | weight |
|-----------------------------|-----------------------------|--------|
| Tsinghua University         | Tsinghua University         | 993    |
| Chinese Academy of Sciences | Chinese Academy of Sciences | 780    |
| Peking University           | Peking University           | 673    |

**get_collab_pid(pid_x, pid_y, config={})**

Get all in/proceedings and conferences of the collaborations between two authors identified by their pid based on the constraints from the given config.

example output of get_collab_pid("24/8616","61/5017"):

| year | inproceeding_id         | inproceeding_title                     | proceeding_id  | proceeding_title               | conference_id | conference_title                    |
|------|-------------------------|----------------------------------------|----------------|--------------------------------|---------------|-------------------------------------|
| 2021 | conf/bmvc/LiuGGT21      | Deep Line Encoding for Monocular...    | conf/bmvc/2021 | 32nd British Machine Vision... | bmvc          | British Machine Vision Conference   |
| 2019 | conf/bmvc/TripathiDGT19 | Tracking the Known and the Unknown...  | conf/bmvc/2019 | 30th British Machine Vision... | bmvc          | British Machine Vision Conference   |
| 2021 | conf/iccv/LiangS0GT21   | Mutual Affine Network for Spatially... | conf/iccv/2021 | 2021 IEEE/CVF International... | iccv          | IEEE International Conference on... |

**get_collab_institution(inst_x, inst_y, config={})**

get all in/proceedings and conferences of the collaborations between two institutions based on the constraints of the config

example output of get_collab_institution("Tsinghua University","Tsinghua University", {"from_year": 2010}):

| year | inproceeding_id            | inproceeding_title                      | proceeding_id  | proceeding_title            | conference_id | conference_title                  |
|------|----------------------------|-----------------------------------------|----------------|-----------------------------|---------------|-----------------------------------|
| 2020 | conf/ndss/CaoXS0GX20       | When Match Fields Do Not Need to Ma...  | conf/ndss/2020 | 27th Annual Network and ... | ndss          | Network and Distributed System... |
| 2020 | conf/ndss/ZhangLWLCHG0XW20 | Poseidon Mitigating Volumetric DDoS...  | conf/ndss/2020 | 27th Annual Network and ... | ndss          | Network and Distributed System... |
| 2012 | conf/ndss/JiangLLLDW12     | Ghost Domain Names Revoked Yet Still... | conf/ndss/2012 | 19th Annual Network and ... | ndss          | Network and Distributed System... |