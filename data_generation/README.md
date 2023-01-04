# Data Generation 

## CSV File Overview

Downloaded files from https://github.com/emeryberger/CSrankings are in the */data* directory: 
  
* country-info.csv
* csrankings.csv
* generated-author-info.csv
* dblp-aliases.csv

generated files are in the */output* directory:

*/mapping*

Files for geographical and institutional mapping:

* geo-codes-auto.csv
* inst-geo-map.csv
* geo-codes.csv
* geo-mapping.csv (final geo file with geographic information for each institution **country, region, geo-coordinates**)

*/pid*

Name to pid mapping from authors in csrankings:

* authors_pid_all.csv
* authors_pid_not_unique.csv
* missing_authors.csv
* authors_pid.csv


*/dblp*

Data extraction from the dblp xml dump:

* authors.json
* collabs.json
* inproceedings.json
* proceedings.json

*/graph*

Combination of dblp and csrankings in a node and edge data structure 

* node_authors.json

## Virtual Environment 

Always run python code in the virutal environment of this directory

```{shell}
virtualenv venv
source venv/bin/activate
pip install -r requirements.txt
```

## Generate Geographical Mapping

Main output: geo-mapping.csv 

The script *geo-mapping.py* takes the *country-info.csv* and *csrankings.csv* and generates the file *inst-geo-map.csv* which consists of all institutions present in csrankings and adds region/country codes and labels for each institution. 

With the API https://api.geoapify.com/v1/geocode/search? the script searches for geo-coordinates for every institution. If there is a match, the name of the found building is compared with the name of the institution using the levenstein distance to veryfy the match and assign a status (OK, WATCH, WRONG) to the result and generates the file *geo-codes-auto.csv*. In the file 210 Institutions where OK, 263 where on WATCH and for 136 the API no geo-coordinates where found.

The file *geo-codes.csv* was edited manualy by going through the file *geo-codes-auto.csv* and check all institutions which do not have an OK status and if the entry is wrong or missing add the true coordinates with the help of google maps. 

Lastly the file *geo-codes.csv* and *inst-geo-map.csv* are joined together to create the file *geo-mapping.csv*. The file *geo-mapping.json* is an adaptation of *geo-mapping.csv* with a slithly differernt structure and naming. 

## Author Name to ID Mapping

To connect the data from csranking with the dblp the author names from csranking must be mapped with the internal author ID (pid) from dblp. The file *gen_pid.py* is used to generate this mapping. With the help of the dblp API https://dblp.org/search/author? the script could exactly identify and get the pid of over 98% of the authors in *csrankings.csv*. The result is saved in the file *authors_pid_all.csv*. 

The file *missing_authors.csv* is a list of all authors and their affiliation for which the script couldn't find an excat match. Looking at this file it can be seen that 49 authors affiliated with Switzerland, Germany or Austria are missing. For those missing authors I manualy looked at the result of the API https://dblp.org/search/author?xauthor and if there are multiple options I checked the API https://dblp.org/pid/$pid$.xml with all possible pid's and checked the affiliation to manualy find the matching pid. For some authors like "Anelis Kaiser" there is no person registerd in the dblp with such a name and no authors can be found. For others there are two differnt author names in csrankings.csv like "Christian Holz 0001" and "Christian Holz" but they are actualy the same person. In some cases the name from csranking.csv had multiple occurences in dblp and dblp adds a 4 digit number at the end of the name for this cases the matching pid can be found by checking all the matches. With manualy going through the 49 authors from GE,CH,AU additional 18 authors could be mapped.

Lastly, the names in csrankings are not unique for one person, meaning the same other (same pid) can have multiple occurences with different name variations like "Kalinka Regina Lucas Jaquie Castelo Branco" or "Kalinka R. L. J. C. Branco" or "Kalinka Branco" are all mapped to the same pid and are therefor the same person. The dataset *authors_pid_all.csv* is trimmed suched that the pid to name mapping is a one to one mapping and this results in the file *authors_pid.csv* with 20986 unique authors. 

## Extract DBLP data

Latest XML dump dowloaded at 12.12.2022 from https://dblp.org/xml/

* dblp-2022-12-01.xml.gz: https://dblp.org/xml/release/dblp-2022-12-01.xml.gz
* dblp-2019-11-22.dtd: https://dblp.org/xml/release/dblp-2019-11-22.dtd

DBLP XML APIs:

* Proceedings (records in general): https://dblp.org/rec/*$crossref*.xml
* All records assosiated with a certain pid: https://dblp.org/pid/*$pid*.xml

The file *parse_dblp.py* containes the parsers for the dblp.xml dump which extracts data abpout the authors, in/proceedings and collaborations and saves them in form of json files in the *output/dblp* directory

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

### inproceedings.json (2'542'194 proceedings)

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