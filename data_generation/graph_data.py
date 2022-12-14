from datetime import datetime
import re
import os
import sys
import pandas as pd
import json
import ijson
from functools import reduce

output_dir = "output/graph" 

# generate author node structures 
#--------------------------------------------------------------------------------
# combine dblp and csranking data
def author_struct(pid, name, affiliation=None, homepage=None, scholarid=None):
    info = {"pid": pid, 
            "name": name, 
            "affiliation": affiliation, 
            "homepage": homepage, 
            "scholarid": scholarid}
    return {k: v for k, v in info.items() if v is not None}

def gen_author_nodes():
    
    with open(os.path.join("output/dblp", "authors.json"), "r") as f:
        authors = json.load(f)
    
    author_pid_csrankings = pd.read_csv("output/pid/authors-pid.csv")

    csrankings = pd.read_csv("data/csrankings.csv")

    # get a unique pid to name mapping 
    #pids = list(set(list(map(lambda x: x["pid"], authors))))
    
    # get a unique pid to name index mapping for fast lookup
    pid_index = {}
    for author in authors:
        if author["pid"] not in pid_index.keys():
            pid_index[author["pid"]] = author["name"]    

    pids = list(pid_index.keys())
    
    # check if the pids from csranking are also contained in the set of all pids
    if author_pid_csrankings[~author_pid_csrankings["pid"].isin(pids)].empty:
        print("All pids from csranking are valid")
    else:
        return False

    csrankings = pd.read_csv("data/csrankings.csv")
    
    # create index for fast lookup
    pid_csrankings_index = {}
    for row in author_pid_csrankings.iterrows():
        pid_csrankings_index[row[1]["pid"]] = row[1]["author"]
    
    author_nodes = []
    
    for pid in pids:
        # check if pid is part of csrankings
        if pid in pid_csrankings_index.keys():
            name = pid_csrankings_index[pid] #author_pid_csrankings[author_pid_csrankings["pid"] == pid].iloc[0]["author"]
            author_info = csrankings[csrankings["name"] == name].iloc[0]
            affiliation = author_info["affiliation"]
            homepage =  author_info["homepage"]
            scholarid =  author_info["scholarid"]
            author_nodes.append(author_struct(pid, name, affiliation, homepage, scholarid))
        else:
            # get the name of the first match (for when there are mulitple names for one pid)
            name = pid_index[pid] #_list(filter(lambda x: x["pid"] == pid, authors))[0]["name"]
            author_nodes.append(author_struct(pid, name))

    # check that all pids from csrankings are correctly included
    if len(list(filter(lambda x: x.get("affiliation"), author_nodes))) != len(author_pid_csrankings):
        return False
    
    #save as json
    with open(os.path.join(output_dir, "nodes_authors.json"), "w") as write_file:
        json.dump(author_nodes, write_file, indent=3,ensure_ascii=False)
    
    # save as csv
    author_nodes_df = pd.DataFrame(author_nodes, columns = ["pid", "name", "affiliation", "homepage", "scholarid"])
    author_nodes_df.to_csv(os.path.join(output_dir, "nodes_authors.csv"), 
                           index=False, header=False, sep=";",doublequote=False, escapechar="\\")  
    
# generate graph data
gen_author_nodes()

# with open(os.path.join(output_dir, "nodes_authors.json"), "r") as f:
#     author_nodes = json.load(f)

# generate in/proceedings node and structures 
#--------------------------------------------------------------------------------
output_dblp = "output/dblp" 

# load proceedings
with open(os.path.join(output_dblp, "proceedings.json"), "r") as f:
    proceedings = json.load(f)
    
# load inproceedings
with open(os.path.join(output_dblp, "inproceedings.json"), "r") as f:
    inproceedings = json.load(f)

# generate nodes for proceedings
proceedings_df = pd.DataFrame(proceedings)
proceedings_df.to_csv(os.path.join(output_dir, "nodes_proceedings.csv"), 
                        index=False, header=True, sep=";",doublequote=False, escapechar="\\")  

# generate nodes for inproceedings
inproceedings_df = pd.DataFrame(inproceedings, columns=["id","title","year"])
inproceedings_df["title"] = list(map(lambda x: re.sub(";|:","",x), inproceedings_df["title"].to_list()))
inproceedings_df.to_csv(os.path.join(output_dir, "nodes_inproceedings.csv"), 
                        index=False, header=True, sep=";",doublequote=False, escapechar="\\")  
    
# no_title = inproceedings_df[inproceedings_df["title"].isnull()]

# generate edges for crossref (connection between inproceedings and proceedings)
crossref = []
for inproceeding in inproceedings:
    crossref.append((inproceeding["id"], inproceeding["crossref"]))
    
crossref_df =  pd.DataFrame(crossref)
crossref_df.to_csv(os.path.join(output_dir, "edges_crossref.csv"), 
                        index=False, header=False, sep=";",doublequote=False, escapechar="\\")  

# generate collaboration edges structures 
#--------------------------------------------------------------------------------

# load collaborations
with open(os.path.join(output_dblp, "collabs.json"), "r") as f:
    collabs = json.load(f)
    
collabs_df = pd.DataFrame(collabs)
collabs_df.to_csv(os.path.join(output_dir, "edges_collabs.csv"), 
                        index=False, header=True, sep=";",doublequote=False, escapechar="\\") 

# computer science area graph data 
# ------------------------------------------------------

# load proceedings
with open(os.path.join(output_dblp, "proceedings.json"), "r") as f:
    proceedings = json.load(f)
    
# load areas 
with open(os.path.join(output_dblp, "area-mapping.json"), "r") as f:
    area_map = json.load(f)

# generate area node structure
area_nodes = list(map(lambda x: {"id":x[0], "label": x[1]["label"]}, area_map.items()))

area_df = pd.DataFrame(area_nodes, columns = ["id", "label"])
area_df.to_csv(os.path.join(output_dir, "nodes_area.csv"), 
                        index=False, header=True, sep=";",doublequote=False, escapechar="\\")  

# generate sub area node structure
ai_areas = area_map["ai"]["areas"] # only ai area so far

sub_area_nodes = list(map(lambda x: {"id":x[0], "label": x[1]["label"]}, ai_areas.items()))

# save as csv
sub_area_df = pd.DataFrame(sub_area_nodes, columns = ["id", "label"])
sub_area_df.to_csv(os.path.join(output_dir, "nodes_sub_area.csv"), 
                        index=False, header=True, sep=";",doublequote=False, escapechar="\\")  

# generate connection between subarea and area
belongs_to = []
for area in ["ai"]: #area_map.keys()
    for sub_area in area_map[area]["areas"].keys():
        belongs_to.append((sub_area, area))
    
sub_area_of =  pd.DataFrame(belongs_to)
sub_area_of.to_csv(os.path.join(output_dir, "edges_sub_area_of.csv"), 
                        index=False, header=False, sep=";",doublequote=False, escapechar="\\")  

# generate connection between conference and subarea
conf_belongs_to = []
for area in ["ai"]: #area_map.keys()
    for sub_area, conferences in area_map[area]["areas"].items():
        sub_area_conf = list(filter(lambda x: x["conf"] in conferences["conferences"], proceedings))
        conf_belongs_to.append(list(map(lambda x: (x["id"],sub_area), sub_area_conf)))
        
conf_belongs_to = reduce(lambda x, y: x+y, conf_belongs_to)
conf_belongs_to_df =  pd.DataFrame(conf_belongs_to)
conf_belongs_to_df.to_csv(os.path.join(output_dir, "edges_conf_belongs_to.csv"), 
                        index=False, header=False, sep=";",doublequote=False, escapechar="\\")  

# Geographical and institutional graph data
# ----------------------------------------------------------------------------------------------

# load institutional geo mapping
geo_mapping = pd.read_csv(os.path.join("output/mapping", "geo-mapping.csv"))
    
nodes_institution = geo_mapping[["institution","lat","lon"]]
nodes_institution.to_csv(os.path.join(output_dir, "nodes_institution.csv"), 
                        index=False, header=True, sep=";",doublequote=False, escapechar="\\")  

geo_mapping_rec = json.loads(geo_mapping.to_json(orient="records"))

# node countries
countries = list(set(list(map(lambda x: (x["country-id"], x["country-name"]), geo_mapping_rec))))
countries_df =  pd.DataFrame(countries, columns=["id","name"])
countries_df.to_csv(os.path.join(output_dir, "nodes_countries.csv"), 
                        index=False, header=True, sep=";",doublequote=False, escapechar="\\")  

# node regions
regions = list(set(list(map(lambda x: (x["region-id"], x["region-name"]), geo_mapping_rec))))
regions_df =  pd.DataFrame(regions, columns=["id","name"])
regions_df.to_csv(os.path.join(output_dir, "nodes_regions.csv"), 
                        index=False, header=True, sep=";",doublequote=False, escapechar="\\")  

# map countries to regions
in_region = list(set(list(map(lambda x: (x["country-id"], x["region-id"]), geo_mapping_rec))))
in_region_df =  pd.DataFrame(in_region)
in_region_df.to_csv(os.path.join(output_dir, "edges_in_region.csv"), 
                        index=False, header=False, sep=";",doublequote=False, escapechar="\\")

# map author to affiliation
author_nodes = pd.read_csv(os.path.join(output_dir, "nodes_authors.csv"),
                           sep=";", names=["pid", "name", "affiliation", "homepage", "scholarid"])
author_nodes_rec = json.loads(author_nodes.to_json(orient="records"))

csrankings_authors = list(filter(lambda x: x.get("affiliation"), author_nodes_rec))

affiliated = list(map(lambda x: (x["pid"], x["affiliation"]), csrankings_authors))
affiliated_df =  pd.DataFrame(affiliated)
affiliated_df.to_csv(os.path.join(output_dir, "edges_affiliated.csv"), 
                        index=False, header=False, sep=";",doublequote=False, escapechar="\\")