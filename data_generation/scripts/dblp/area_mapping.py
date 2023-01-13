#----------------------------------------------------
# Categorize proceedings into computer science areas 
# 
# This is a helper file for the creation of the area-mapping.json
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
    
    
# load areas 
with open(os.path.join(output_dir, "area-mapping.json"), "r") as f:
    area_map = json.load(f)

ai_areas = area_map["ai"]["areas"]

all_conf_csrankings = list(map(lambda x: x["area"], area_map))

def get_title(area):
    if area in all_conf_csrankings:
        return list(filter(lambda x: x["area"]== area,area_map))[0]["title"]
    else: 
        return None
    
proceeding_conf = list(map(lambda x: x["conf"], proceedings))

# all conferences in dblp
unique_conf =  set(list(map(lambda x: x["id"].split("/")[1], proceedings)))

# get all proceedings that include the search_conf somewhere in their name
search_conf = "intelligence"
found_conf = list(filter(lambda x: re.search(search_conf, x["title"] , re.IGNORECASE), proceedings))
list(set(list(map(lambda x: x["id"].split("/")[1], found_conf))))

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
           "systems": {"label": "Systems",
                       "areas":
                        {"architecture": {"label": "Computer Hardware and Architecture", 
                                          "conferences":["arch","asplos","isca","micro","hpca"]},
                       "networks":  {"label": "Computer Networks and Communications", 
                                     "conferences": ["iccomm","sigcomm","nsdi"]},
                       "security":{"label": "Computer Security and Cryptography", 
                                   "conferences":  ["sec","ccs","sp","uss","ndss","pet","crypto","eurocrypt"]},
                       "databases":{"label": "Databases & Information Systems", 
                                   "conferences":["sigmod","vldb","icde","pods"]},
                       "hpc": {"label": "High-performance computing", 
                                   "conferences":["hpc","sc","hpdc","ics"]},
                       "mobile+web":{"label": "Web, Mobile & Multimedia Technologies", 
                                   "conferences":["mobicom","mobisys","sensys","iswc", "semweb","www"]},
                       "metrics":{"label": "Measurement & perf. analysis", 
                                   "conferences":["imca","sigmetrics"]},
                       "os":{"label": "Operating systems", 
                                   "conferences":["osdi","sosp","fast","usenix","eurosys"]},
                       "pl":{"label": "Programming languages", 
                                   "conferences":["pldi","popl","icfp","oopsla"]},
                       "se":{"label": "Software engineering", 
                                   "conferences":["se","fse","icse","kbse","issta"]},
                       "da":{"label": "Design automation", 
                                   "conferences":["dac","iccad"]},
                       "embedded":{"label": "Embedded & real-time systems", 
                                   "conferences":["emsoft","rtas","rtss"]}}},
           "theory": {"label": "Theory",
                       "areas": {"math":{"label": "Computational Theory and Mathematics", 
                                   "conferences":["focs","soda","stoc","cav","lics"]}}},
           "interdiscip" : 
               {"label": "Interdisciplinary Areas",
                       "areas": {"bio":{"label": "Biomedical & Medical Engineering", 
                                   "conferences":["ismb","recomb"]},
                                 "graphics":{"label": "Computer graphics", 
                                   "conferences":["siggraph","siggrapha"]},
                                 "eco":{"label": "Economics & computation", 
                                   "conferences":[]},
                                 "hci":{"label": "Human-computer interaction", 
                                   "conferences":["chi","huc","pervasive","uist"]},
                                 "robotics":{"label": "Robotics", 
                                   "conferences":["icra","iros","rss"]},
                                 "vis":{"label": "Visualization", 
                                   "conferences":["visualization","vr"]}
                                 }}
           }


# all_conf = reduce(lambda x, y: x+y, top_conf["ml"].values())
# top_proceedings = list(filter(lambda x: x["id"].split("/")[1] in all_conf , proceedings))








