# Data Generation 

## CSV File Overview

Downloaded files from https://github.com/emeryberger/CSrankings are in the */data* directory: 
  
* country-info.csv
* csrankings.csv
* generated-author-info.csv
* dblp-aliases.csv

generated files are in the */output* directory:

* geo-codes-auto.csv
* inst-geo-map.csv
* geo-codes.csv
* geo-mapping.csv (final geo file with geographic information for each institution **country, region, geo-coordinates**)


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

To connect the data from csranking with the dblp the author names from csranking must be mapped with the internal author ID (pid) from dblp. The file *gen_pid.py* is used to generate this mapping. With the help of the dblp API https://dblp.org/search/author? the script could exactly identify and get the pid of over 98% of the authors in *csrankings.csv*. The result is saved in the file *authors_pid.csv*. 

The file *missing_authors.csv* is a list of all authors and their affiliation for which the script couldn't find an excat match. Looking at this file it can be seen that 49 authors affiliated with Switzerland, Germany or Austria are missing. For those missing authors I manualy looked at the result of the API https://dblp.org/search/author?xauthor and if there are multiple options I checked the API https://dblp.org/pid/$pid$.xml with all possible pid's and checked the affiliation to manualy find the matching pid. For some authors like "Anelis Kaiser" there is no person registerd in the dblp with such a name and no authors can be found. For others there are two differnt author names in csrankings.csv like "Christian Holz 0001" and "Christian Holz" but they are actualy the same person. In some cases the name from csranking.csv had multiple occurences in dblp and dblp adds a 4 digit number at the end of the name for this cases the matching pid can be found by checking all the matches. With manualy going through the 49 authors from GE,CH,AU additional 18 authors could be mapped.

