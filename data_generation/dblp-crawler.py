# crawl dblp records for all authors in csranking
import pandas as pd
import json
import requests 
import time
import xml.etree.ElementTree as ET 
import os
from tqdm.notebook import trange, tqdm

output_dir = "output/dblp" 

output = {
# save the total number of publications for each pid (key = pid, value = #records)
"publication_n": {},

# occurences of authors that are not in csrankings (key = pid, value = occurences)
"missing_authors": {},

# list of all proceedings
"proceedings": [],

# list of all proceeding ids (for later use to crawle thorugh all crossref proceedings)
"proceeding_ids": [],

# list of all inproceedings
"inproceedings": [],

# list of all inproceedings that have no collaboration within the csranking pids
"inproceedings_dropouts": [],

# edges for the collaboration network
"collabs": [],

# pids with a failed request
"error_pids" : []

}

# get all pids from csrankings
author_pid = pd.read_csv("output/pid/authors_pid.csv")
pids = author_pid["pid"].to_list()


# dblp xml record url
def url_rec(key):
    return "https://dblp.org/rec/{}.xml".format(key)

# dblp xml all records for a certain pid
def url_pid(pid):
    return "https://dblp.org/pid/{}.xml".format(pid)

# dblp API request
def get_xml(dblp_url):
    try:
        r = requests.get(dblp_url)
        time.sleep(1) # to prevent temporary timeout
        root = ET.fromstring(r.content) 
        return root
    except:
         time.sleep(5) # 
         return None
    


# edge id conisting of the two pids and the dblp record key
def gen_edge_id(pid_u, pid_v, key):
    return "{}-{}-{}".format(pid_u, pid_v, key)

# create edge datastructure for the collaboratioin network
def edge_struct(pid_u, pid_v, key):
    return {"node/u": pid_u,
            "node/v": pid_v,
            "rec/id": key, # dblp record key for the inproceedings
            "edge/id": gen_edge_id(pid_u, pid_v, key)}
    
# create inproceedings datastructure 
def inproceeding_struct(id, title, year, crossref):
    return {"id": id, "title": title, "year": year, "crossref": crossref}

# create proceedings datastructure 
def proceeding_struct(id, title, year):
    return {"id": id, "title": title, "year": year}


inital_count = 6000
for count, pid in enumerate(pids[6000:]):

    # get xml records of the pid
    root = get_xml(url_pid(pid))

    if not root:
        output["error_pids"].append(pid)
        continue

    # save the number of records for that pid (might be useful information to weight authors)
    output["publication_n"][pid] = int(root.attrib.get("n"))

    # loop through all inproceedings assosiated with that pid
    inproccedings = root.findall("./r/inproceedings")
    for inprocceding in inproccedings:
        key = inprocceding.attrib["key"]
        title = inprocceding.find("title").text
        year = int(inprocceding.find("year").text)
        
        # crossref is sometimes missing in dblp if that happens take the proceedings id from the url
        crossref_ele = inprocceding.find("crossref")
        if crossref_ele:
            crossref = crossref_ele.text
        else:
            inprocceding_url = inprocceding.find("url").text
            crossref = inprocceding_url.split("db/")[1].split(".html#")[0]
            
        # get the collaborations
        collab_count = 0
        authors =  inprocceding.findall("author")
        for a in authors: 
            # if no pid exist take the authors name
            p = a.attrib.get("pid", a.text) 
            
            # ignore the main pid
            if p == pid:
                continue
            # check if the pid occures in csrankings
            elif p in pids:
                
                edge_id = gen_edge_id(pid, p, key)
                mirror_id = gen_edge_id(p, pid, key)
                ids_edges = list(map(lambda x : x["edge/id"],  output["collabs"]))
                
                # check if edge already exist (sicne edges unirected check both ways)
                if not (edge_id in ids_edges or mirror_id in ids_edges):
                
                    # add the collaboration
                    edge = edge_struct(pid, p, key)
                    output["collabs"].append(edge)
                    collab_count +=1
            else:
                # count occurences of authors in dblp but not in csrankings
                output["missing_authors"][p] = output["missing_authors"].get(p, 0) + 1
                
        # only add the inproceeding and the corresponding proceeding if there is a collaboration
        if collab_count > 0:
            # add the inproceeding if inproceeding does not exist yet 
            ids_inproceedings = list(map(lambda x : x["id"],  output["inproceedings"]))
            if not key in ids_inproceedings:
                output["inproceedings"].append(inproceeding_struct(key, title, year, crossref))

            # add the crossref proceeding id if it does not exist yet 
            if not crossref in output["proceeding_ids"]:
                output["proceeding_ids"].append(crossref)
        else: 
            output["inproceedings_dropouts"].append(key)
            
        if (count > 0) and (count % 1000 == 0):
            with open(os.path.join(output_dir, "output_{}.json".format(count+inital_count)), "w") as write_file:
                json.dump(output, write_file, indent=3) 
            
with open(os.path.join(output_dir, "output_final.json"), "w") as write_file:
    json.dump(output, write_file, indent=3)

# len(output["collabs"])
# len(output["proceeding_ids"])
# len(output["inproceedings"])
# len(output["inproceedings_dropouts"])
# len(output["missing_authors"])
# len(output["publication_n"])
# dict(sorted(output["missing_authors"].items(), key=lambda item: item[1]))

with open(os.path.join(output_dir, "output_6000.json"), "r") as f:
    output = json.load(f)

inproceedings = output["proceeding_ids"]
collabs = output["collabs"]
pid = 't/AMinTjoa'
collab_pid = list(filter(lambda x: x["node/u"]==pid, collabs))
rec_pid = list(map(lambda x: x["rec/id"], collab_pid))
output["error_pids"]