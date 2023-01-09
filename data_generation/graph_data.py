from datetime import datetime
import re
import os
import sys
import pandas as pd
import json
import ijson
from functools import reduce

output_dir = "output/graph" 

# combine dblp and csranking data
# generate author node structures 
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

# area mapping 
# ------------------------------------------------------
output_dblp = "output/dblp" 

# load proceedings
with open(os.path.join(output_dblp, "proceedings.json"), "r") as f:
    proceedings = json.load(f)
    
# load areas 
with open(os.path.join(output_dblp, "area-mapping.json"), "r") as f:
    area_map = json.load(f)

# generate area node structure
area_nodes = list(map(lambda x: {"id":x[0], "label": x[1]["label"]}, area_map.items()))

area_df = pd.DataFrame(area_nodes, columns = ["id", "label"])
area_df.to_csv(os.path.join(output_dir, "area_nodes.csv"), 
                        index=False, header=True, sep=";",doublequote=False, escapechar="\\")  

# generate sub area node structure
ai_areas = area_map["ai"]["areas"] # only ai area so far

sub_area_nodes = list(map(lambda x: {"id":x[0], "label": x[1]["label"]}, ai_areas.items()))

# save as csv
sub_area_df = pd.DataFrame(sub_area_nodes, columns = ["id", "label"])
sub_area_df.to_csv(os.path.join(output_dir, "sub_area_nodes.csv"), 
                        index=False, header=True, sep=";",doublequote=False, escapechar="\\")  

# generate connection between subarea and area
belongs_to = []
for area in ["ai"]: #area_map.keys()
    for sub_area in area_map[area]["areas"].keys():
        belongs_to.append((sub_area, area))
    
sub_area_of =  pd.DataFrame(belongs_to)
sub_area_of.to_csv(os.path.join(output_dir, "sub_area_of_edges.csv"), 
                        index=False, header=False, sep=";",doublequote=False, escapechar="\\")  

# generate connection between conference and subarea
conf_belongs_to = []
for area in ["ai"]: #area_map.keys()
    for sub_area, conferences in area_map[area]["areas"].items():
        sub_area_conf = list(filter(lambda x: x["conf"] in conferences["conferences"], proceedings))
        conf_belongs_to.append(list(map(lambda x: (x["id"],sub_area), sub_area_conf)))
        
conf_belongs_to = reduce(lambda x, y: x+y, conf_belongs_to)
conf_belongs_to_df =  pd.DataFrame(conf_belongs_to)
conf_belongs_to_df.to_csv(os.path.join(output_dir, "conf_belongs_to_edges.csv"), 
                        index=False, header=False, sep=";",doublequote=False, escapechar="\\")  