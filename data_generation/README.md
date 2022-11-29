# Data Generation 

## CSV File Overview

Downloaded files from https://github.com/emeryberger/CSrankings: 
  
* country-info.csv
* csrankings.csv
* generated-author-info.csv
* dblp-aliases.csv

geo-mapping.csv: Geographic information for each institution (country, region, geo-coordinates)


## Virtual Environment 

To run python code you must first activate the virtual environment

```{shell}
source venv/bin/activate
```

## Generate Geographical Mapping

Main output: geo-mapping.csv 

The script *geo-mapping.py* takes the *country-info.csv* and *csrankings.csv* and generates the file *inst-geo-map.csv* which consists of all institutions present in csrankings and adds region/country codes and labels for each institution. 

Further, with the API https://api.geoapify.com/v1/geocode/search? the script tried to get the geo-coordinates of every institution and generated the file *geo-codes-auto.csv*. 





