from datetime import datetime
import re
import os
import sys
import pandas as pd
import json
import ijson
from  parse_dblp import parse_proceedings, parse_authors, parse_proceedings

dblp_path = 'data/dblp.xml'
output_dir = "output/dblp" 

# parse authors
# parse_authors()

# parse proceedings
# parse_proceedings()

with open(os.path.join(output_dir, "proceedings.json"), "r") as f:
    proceedings = json.load(f)

with open(os.path.join(output_dir, "authors.json"), "r") as f:
    authors = json.load(f)
    
# get all valid proceeding ids
proceedings_ids = list(map(lambda x: x["id"], proceedings))

# map every author name to its pid
name_pid_map = {}
for author in authors:
    name_pid_map[author["name"]] = author["pid"]
    

# parse proceedings and collabs
# parse_proceedings(proceedings_ids, name_pid_map)
    
with open(os.path.join(output_dir, "inproceedings.json"), "r") as f:
    inproceedings = json.load(f)

counter = 0
with open(os.path.join(output_dir, "collabs.json"), "r") as f:
    #collabs = json.load(f)
    for item in ijson.items(f, 'item'):
        counter+=1
  