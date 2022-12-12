# Geographic mapping of institutions

import pandas as pd

csrankings = pd.read_csv("data/csrankings.csv")
print(csrankings.describe())

country_info = pd.read_csv("data/country-info.csv")
print(country_info.describe())

# get missing affiliation in the country-info.csv
missing_inst= list(filter(lambda x: x not in country_info["institution"].to_list(), 
       csrankings["affiliation"].unique()))

# map canada to the northamerica region
country_info["region"].replace("canada", "northamerica", inplace=True)

# strip whitespaces in region
country_info["region"] = list(map(lambda x: x.strip(), country_info["region"]))

# all missing affiliations are in the USA
# add us affiliation to country-info
country_info = pd.concat([country_info,
                          pd.DataFrame(list(map(lambda x: [x,"northamerica" , "us"], missing_inst)),
                                 columns=country_info.columns)]) 

# add country names to country-info
country_names_csv = "https://pkgstore.datahub.io/core/country-list/data_csv/data/d7c9d7cfb42cb69f4422dec222dbbaa8/data_csv.csv"
country_names = pd.read_csv(country_names_csv)

def get_country_name(abbrv):
    code = abbrv.upper()
    if code in country_names["Code"].to_list():
        return country_names["Name"][country_names["Code"] == code].values[0]
    elif abbrv == "uk":
        return "United Kingdom" 
    else:
        return "unknown"

country_info["countryname"] = list(map(get_country_name, country_info["countryabbrv"]))

# add region names to country-info
regions = {"europe": "Europe", 
           "africa": "Africa", 
           "asia" :"Asia",
           "australasia": "Australasia",
           "northamerica": "North America",
           "southamerica" : "South America"} 

country_info["regionname"] = list(map(lambda x: regions[x], country_info["region"]))

# rearange and rename columns
cols = ['institution', 'countryabbrv', 'countryname', 'region','regionname']
country_info = country_info[cols]
country_info.columns = ['institution', 'country_code', 'country_name', 'region_code','region_name']

# write new country-info in csv file
country_info.to_csv("output/inst-geo-map.csv", index=False)


# view inst-geo-map.csv
inst_geo_map = pd.read_csv("output/inst-geo-map.csv")
print(inst_geo_map.describe())


# get geo-coordinates of institutions
# use https://www.geoapify.com/
import requests
from requests.structures import CaseInsensitiveDict
from fuzzywuzzy import fuzz

def geo_api_url(institution, country):
    text = "University, {}, {}".format(institution,country)
    api_key = "e929c302233e42a285148887db8e42e2"
    url = "https://api.geoapify.com/v1/geocode/search?text={}&apiKey={}".format(text, api_key)
    return url

headers = CaseInsensitiveDict()
headers["Accept"] = "application/json"


geo_codes = []
def empty_prop(institution, country):   
    return     {"country": country,
                "institution" : institution,
                "name" : None,
                "lat" : None ,
                "lon": None,
                "fuzz-ratio": None,
                "status": "WRONG"}
    
# institution = "Bielefeld University"
# country = "Germany"
for _, row in inst_geo_map.iterrows():
    institution = row["institution"]
    country = row["country_name"]
    resp = requests.get(geo_api_url(institution, country), headers=headers)
    res = resp.json()

    # sort results by importance and only take the most important result
    features = res["features"]
    if features == []:
        geo_codes.append(empty_prop(institution, country))
        continue
    # if len(features) > 1:
    #     features = sorted(features, key = lambda x: x["properties"]["rank"]["importance"], reverse=True)
    feature = features[0]
    properties = feature["properties"]
    if "name" not in properties.keys():
        geo_codes.append(empty_prop(institution, country))
        continue
    if properties["result_type"] == "amenity":
        prop = {"country": country,
                "institution" : institution,
                "name" : properties["name"],
                "lat" : properties["lat"] , 
                "lon": properties["lon"],
                "fuzz-ratio": fuzz.ratio(institution, properties["name"]),
                "status": "OK" if fuzz.ratio(institution, properties["name"]) > 80 else "WATCH"}
        geo_codes.append(prop)
    else: 
        geo_codes.append(empty_prop(institution, country))
        
    
inst_geo_codes = pd.DataFrame(geo_codes)
print("OK: {}\nWATCH: {}\nWRONG: {}".format(sum(inst_geo_codes["status"] == "OK"),
                                            sum(inst_geo_codes["status"] == "WATCH"),
                                            sum(inst_geo_codes["status"] == "WRONG")))
# OK: 210
# WATCH: 263
# WRONG: 136
inst_geo_codes.to_csv("geo-codes-auto.csv")

# load manualy completed geo codes
geo_codes = pd.read_csv("output/geo-codes.csv", index_col = "index")

# create final institutional geo mapping with geo-codes included 
geo_mapping = inst_geo_map
geo_mapping["lat"] = geo_codes["lat"]
geo_mapping["lon"] = geo_codes["lon"]
geo_mapping.to_csv("output/geo-mapping.csv", index=False)


# check final file
geo_mapping = pd.read_csv("output/geo-mapping.csv")
geo_mapping.describe()

#        institution country_code   country_name   region_code    region_name         lat         lon
# count          609          609            609           609            609  609.000000  609.000000
# unique         609           55             55             6              6         NaN         NaN

coordinates = []
for _, row in geo_mapping.iterrows():
    coordinates.append({"lat": row["lat"], "lon": row["lon"]})
geo_mapping["coord"] = coordinates
json_map = geo_mapping.drop(['lat', 'lon'], axis=1)
json_map.columns = ['institution', 'country-id', 'country-name', 'region-id','region-name', 'coord']
json_map.to_json("output/geo-mapping.json", 
                 orient="records",
                 indent=3)  

