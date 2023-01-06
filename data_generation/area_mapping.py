#----------------------------------------------------
# Categorize proceedings into computer science areas 
#----------------------------------------------------

import pandas as pd
import numpy as np
from fuzzywuzzy import fuzz #fuzzy search for best match
import os
import json
import ijson
import re

output_dir = "output/dblp" 

# load proceedings
with open(os.path.join(output_dir, "proceedings.json"), "r") as f:
    proceedings = json.load(f)

# areas as defined by csrankings 
with open(os.path.join("data", "areas-csrankings.json"), "r") as f:
    area_map = json.load(f)

    
aiAreas = ["ai", "vision", "mlmining", "nlp", "inforet"]
systemsAreas = ["arch", "comm", "sec", "mod", "da", "bed", "hpc", "mobile", "metrics", "ops", "plan", "soft"]
theoryAreas = ["act", "crypt", "log"]
interdisciplinaryAreas = ["bio", "graph", "ecom", "chi", "robotics", "visualization"]

all_conf = list(map(lambda x: x["area"], area_map))

def get_title(area):
    if area in all_conf:
        return list(filter(lambda x: x["area"]== area,area_map))[0]["title"]
    else: 
        return None
    

proceeding_conf = list(map(lambda x: x["conf"], proceedings))

def conf_found(conf):
    trimmed = conf.replace("Workshops", "")
    if sum(list(map(lambda x: True if re.search(x, trimmed, re.IGNORECASE) else False, all_conf))) == 1:
        return True
    else:
        return False

list(filter(lambda x: conf_found(x), proceeding_conf))


