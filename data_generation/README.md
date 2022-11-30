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
* geo-mapping.csv (final geo file with geographic information for each institution (country, region, geo-coordinates)


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

Lastly the file *geo-codes.csv* and *inst-geo-map.csv* are joined together to create the file *geo-mapping.csv* 


