# playground to test functions and get a feeling for the data

import pandas as pd

csrankings = pd.read_csv("csrankings.csv")
print(csrankings.describe())

country_info = pd.read_csv("country-info.csv")
print(country_info.describe())

# view inst-geo-map.csv
inst_geo_map = pd.read_csv("inst-geo-map.csv")
print(inst_geo_map.describe())

# get geo-coordinates of institutions
# use geoapify
import requests
from requests.structures import CaseInsensitiveDict
from fuzzywuzzy import fuzz

def geo_api_url(institution, country):
    text = "{}, {}".format(institution,country)
    api_key = "e929c302233e42a285148887db8e42e2"
    url = "https://api.geoapify.com/v1/geocode/search?text={}&apiKey={}".format(text, api_key)
    return url

headers = CaseInsensitiveDict()
headers["Accept"] = "application/json"


geo_codes = []
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
        print(institution)
        break
    # if len(features) > 1:
    #     features = sorted(features, key = lambda x: x["properties"]["rank"]["importance"], reverse=True)
    feature = features[0]
    properties = feature["properties"]
    if properties["result_type"] == "amenity":
        prop = {"institution" : institution,
                "name" : properties["name"],
                "lat" : properties["lat"] ,
                "lon": properties["lon"],
                "fuzz-ratio": fuzz.ratio(institution, properties["name"])}
        geo_codes.append(prop)
    else: 
        print(institution)
        break
    


