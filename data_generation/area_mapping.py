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


# manualy search for top conferences and map them to the area
conf_map = {"vision": {"IEEE/CVF": ['iccvw', 'wacv', 'iccv', 'cvpr'],
                       "CVPR": ['cvpr', 'ncvpripg', 'emmcvpr'],
                       "ECCV": ['eccv'],
                       "ICCV": ['iccvw', 'iccvg', 'iccve', 'eccv', 'iccv']},
            "ai": { "AI": ['aivr', 'isbdai', 'wirn', 'starai', 'taai', 'itqm', 'aitest', 'iccsci', 'aaaiss', 'tpctc', 'iaai', 'icarti', 'miwai', 'epia', 'nesy', 'mobicom', 'sais', 'profitai', 'bnaic', 'aicas', 'gis', 'ccs', 'icse', 'ifip13', 'cicai', 'isdevel', 'cdceo', 'sigsoft', 'ifip12', 'ifip5-4', 'icmai2', 'aic', 'dai2', 'icrai', 'mdai', 'iwann', 'aisc', 'birthday', 'ieaaie', 'ijcai', 'aistats', 'iicai', 'nlxai', 'icai', 'dagstuhl', 'iciks', 'aims2', 'gcaiot', 'aime', 'aied', 'teachml', 'caepia', 'miccai', 'aimsa', 'aire-ws', 'ictai', 'tia', 'icprai', 'iccai', 'acs2', 'scai', 'ahfe', 'iwaipr', 'hhai', 'iicaiet', 'aiee', 'acai2', 'cscs2', 'bife', 'sbia', 'ai4i', 'isaim', 'aike', 'gandalf', 'ecai2', 'aics', 'ccia', 'sc', 'iberamia', 'lpar', 'aaaifs', 'bica', 'ausai', 'aina', 'raai', 'aicv2', 'sigmod', 'tainn', 'overlay', 'ptai', 'icaiis', 'asc', 'aivr2', 'icail', 'cloudtech', 'isaims', 'sam-iot', 'evoW', 'cpaior', 'csai', 'aiam', 'fedcsis', 'icaicst', 'aib', 'micai', 'aici', 'ecai', 'conext', 'jsai', 'csse', 'aia', 'iwinac', 'icaiic', 'aaai', 'rev', 'icaart', 'aiprf', 'bvai', 'csoc', 'icic', 'sigcomm', 'pricai', 'mcait', 'sgai', 'ais', 'aiipcc', 'sensys', 'uai', 'setn', 'aigc', 'mlis', 'aiide', 'aipr2', 'jelia', 'ruleml', 'ricai', 'iciai', 'mochart', 'jcai', 'hpcct', 'instinctive', 'acai', 'avi', 'snpd', 'hci', 'ki', 'rcai', 'dcai', 'aiia', 'skm', 'aiccc', 'aidbei', 'ai', 'medprai', 'ai50', 'hais', 'flairs', 'fair2', 'icccsec', 'maics', 'rweb', 'clip-ws', 'gcai', 'icaisc', 'aicsp', 'icaai', 'ausdm', 're', 'ifip5-7', 'aidr', 'dsmlai'],
                    "AAAI": ['aaai', 'metacognition', 'icia', 'amec', 'eann', 'exact', 'aaaifs', 'aies', 
                            'wiced', 'hcomp', 'atal', 'aiide', 'spaca', 'icwsm', 'aaaiss', 'nesy'],
                    "IJCAI": ['aihc', 'ijcai', 'amec', 'exact', 'ecai', 'confws', 'atal', 'semdeep', 'cdceo']},
            "ml": {"ICLR": ['iclr'],
                   "ICML": ['icmla', 'icmlc2', 'icmlt', 'ijcai', 'icmlsc', 'icmlc', 'atal', 'aaip', 'imcl', 'icml'],
                   "NIPS": ['nips', 'akbc']},
            "nlp": {"ACL": ['emnlp', 'acl-vl', 'iwslt', 'pacling', 'acl-semitic', 'acl-tea', 'acl-sighan', 'hpdc', 'pkdd', 'rep4nlp', 'aclnews', 'mwe', 'tacl', 'textgraphs', 'aclwac', 'dimva', 'akbc', 'lacl', 'acl-multiling', 'starsem', 'acl-peoples', 'latech', 'ACMicec', 'acl-wnlp', 'ismb', 'acl-codeswitch', 'aclnmt', 'wanlp', 'argmining', 'acl2', 'bea', 'slpat', 'ijcnlp', 'semeval', 'acl-dialdoc', 'acl-clpsych', 'hytra', 'acl-cmcl', 'acllaw', 'acl-pitr', 'cogalex', 'acl-xml', 'discomt', 'sc', 'acl-lchange', 'acl-deelio', 'acl', 'acl-bucc', 'acl-spnlp', 'acl-cvsc', 'acling', 'aclevents', 'clfl', 'wassa', 'acl-convai', 'wmt', 'acl-deeplo', 'naacl', 'acl-louhi', 'aclwat', 'acl-alw', 'repeval', 'sew', 'acl-nlpcss', 'vardial', 'acl-bsnlp', 'acl-spmrl', 'acl-figlang', 'ethnlp', 'slt', 'ssst', 'eacl', 'paclic', 'ltedi', 'acl-pwnlp', 'sigmorphon', 'acl-insights', 'acl-socialnlp', 'blackboxnlp', 'bionlp', 'europar'],
                    "EMNLP": ['emnlp', 'acl-codeswitch', 'acl-vl', 'acl-clinicalnlp', 'wanlp', 'argmining', 'ssst', 'conll', 'bea', 'wassa', 'wmt', 'acl-deeplo', 'naacl', 'acl-louhi', 'aclwat', 'acl-alw', 'textgraphs', 'repeval', 'acl-socialnlp', 'discomt', 'blackboxnlp', 'acl-nlpcss', 'acludw', 'bionlp', 'acl-mrqa', 'acl-spnlp', 'acl-spmrl', 'aclnut'],
                    "NAACL": ['acl-figlang', 'acl-codeswitch', 'ethnlp', 'aclevents', 'ssst', 'clfl', 'bea', 'wassa', 'wmt', 'slpat', 'semeval', 'acl-clpsych', 'sigmorphon', 'naacl', 'mwe', 'acl-cmcl', 'acl-louhi', 'acllaw', 'acl-pitr', 'textgraphs', 'aclwac', 'acl-socialnlp', 'akbc', 'acl-deelio', 'bionlp', 'starsem', 'acl-peoples', 'acl-spnlp', 'latech', 'acl-spmrl']}
            }


# top rank 200 machine learning conferences from reasearch.com
top_conf= {"ml": {  "vision":["iccv","cvpr", "eccv","bmvc","accv","avss"],
                    "ai":["aaai","ijcai","aistats","uai","ivs","iui","ai","itsc","aies","aips"],
                    "nlp":["eacl","emnlp","interspeech","ijcnlp","eacl","cicling","asru","lrec","acl"],
                    "ml":["nips","mlmta","iclr","kdd","icde","improve","dmbd","fgr","ijcnn","colt","icdar","slt","pkdd","recsys","iccpr","miccai","pakdd","mlsp","acml","icml","naacl"],
                    "ir": ["sigir","wsdm","cikm","isita","msr","cidr","asunam","mir","bigcom","sdm","ecir","ismir","fusion","www"]
                    }}

all_conf = reduce(lambda x, y: x+y, top_conf["ml"].values())
top_proceedings = list(filter(lambda x: x["id"].split("/")[1] in all_conf , proceedings))


search_conf = "naacl"
found_conf = list(filter(lambda x: re.search(search_conf, x["title"] , re.IGNORECASE), proceedings))
list(set(list(map(lambda x: x["id"].split("/")[1], found_conf))))





