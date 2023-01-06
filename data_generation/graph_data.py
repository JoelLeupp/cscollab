from datetime import datetime
import re
import os
import sys
import pandas as pd
import json
import ijson

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


