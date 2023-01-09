#----------------------------------------------------
# Categorize proceedings into computer science areas 
# 
# dblp conf html page example: https://dblp.uni-trier.de/db/conf/ai/index.html
# conference ranking: https://research.com/conference-rankings/computer-science/machine-learning
#----------------------------------------------------

import pandas as pd
import numpy as np
from fuzzywuzzy import fuzz #fuzzy search for best match
import os
import json
import ijson
import re
from functools import reduce

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


all_conf_csrankings = list(map(lambda x: x["area"], area_map))

def get_title(area):
    if area in all_conf_csrankings:
        return list(filter(lambda x: x["area"]== area,area_map))[0]["title"]
    else: 
        return None
    
proceeding_conf = list(map(lambda x: x["conf"], proceedings))

# all conferences in dblp
unique_conf =  set(list(map(lambda x: x["id"].split("/")[1], proceedings)))



# top rank 200 machine learning conferences from reasearch.com
top_conf= {"ai": {"label": "AI",
                  "areas": 
                    { "vision": {"label": "Computer Vision", "conferences": ["iccv","cvpr", "eccv","bmvc","accv","avss"]},
                    "ai": {"label": "Artificial Intelligence", "conferences":["aaai","ijcai","aistats","uai","ivs","iui","ai","itsc","aies","aips"]},
                    "nlp":{"label": "Natural language Processing", "conferences": ["eacl","emnlp","interspeech","ijcnlp","eacl","cicling","asru","lrec","acl"]},
                    "ml": {"label": "Machine Learning", "conferences":["nips","mlmta","iclr","kdd","icde","improve","dmbd","fgr","ijcnn","colt","icdar","slt",
                                                                      "pkdd","recsys","iccpr","miccai","pakdd","mlsp","acml","icml","naacl","mod","ruleml"]},
                    "ir": {"label": "Information Retrieval", "conferences": ["sigir","wsdm","cikm","isita","msr","cidr","asunam","mir","bigcom","sdm","ecir","ismir","fusion"]}}
                    },
           "systems": {"architecture":["arch","asplos","isca","micro","hpca"],
                       "networks": ["iccomm","sigcomm","nsdi"],
                       "security":["sec","ccs","sp","uss","ndss","pet"],
                       "databases":["sigmod","vldb","icde","pods"],
                       "hpc": ["hpc","sc","hpdc","ics"],
                       "mobile":["mobicom","mobisys","sensys"],
                       "web":["iswc", "semweb","www"],
                       "metrics":["imca","sigmetrics"],
                       "os":["osdi","sosp","fast","usenix","eurosys"],
                       "pl":["pldi","popl","icfp","oopsla"],
                       "se":["se","fse","icse","kbse","issta"],
                       "da":["dac","iccad"],
                       "embedded":["emsoft","rtas","rtss"]}}

all_conf = reduce(lambda x, y: x+y, top_conf["ml"].values())
top_proceedings = list(filter(lambda x: x["id"].split("/")[1] in all_conf , proceedings))


search_conf = "naacl"
found_conf = list(filter(lambda x: re.search(search_conf, x["title"] , re.IGNORECASE), proceedings))
list(set(list(map(lambda x: x["id"].split("/")[1], found_conf))))





