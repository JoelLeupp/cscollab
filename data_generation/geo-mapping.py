# Geographic mapping of institutions

import pandas as pd

csrankings = pd.read_csv("csrankings.csv")
print(csrankings.describe())

country_info = pd.read_csv("country-info.csv")
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
country_info.to_csv("inst-geo-map.csv", index=False)


# view inst-geo-map.csv
inst_geo_map = pd.read_csv("inst-geo-map.csv")
print(inst_geo_map.describe())