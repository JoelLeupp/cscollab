import requests
import json
import pandas as pd

""" example GET calls """
# http://127.0.0.1:8030/api/db/get_region_mapping
# http://127.0.0.1:8030/api/db/get_area_mapping
# http://127.0.0.1:8030/api/db/get_conference?conf=aaai

""" get region mapping """
url = "http://127.0.0.1:8030/api/db/get_region_mapping"
x = requests.get(url)
res = pd.DataFrame(json.loads(x.content))
print(res)

""" get area mapping """
url = "http://127.0.0.1:8030/api/db/get_area_mapping"
x = requests.get(url)
res = pd.DataFrame(json.loads(x.content))
print(res)

""" get all inporceedings and proceedings from confernce"""
url = "http://127.0.0.1:8030/api/db/get_conference"
input = {"conf":"icse"}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)


""" get authors from csranking with their affiliation filtered on region/country"""
url = "http://127.0.0.1:8030/api/db/get_csauthors"
input = {"region_id":"dach"}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)

""" get collaboration of author/institution"""
url = "http://127.0.0.1:8030/api/db/get_collaboration"
collab_config = {"area_id" : "ai", 
                "area_type":  "a", 
                "region_id":"dach",
                "country_id":None,
                "strict_boundary":True
                }
input = {"config": json.dumps(collab_config)}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)


""" get weighted collaboration of author"""
url = "http://127.0.0.1:8030/api/db/get_weighted_collab"
config = {  "from_year": 2010,
            "area_id" : "ai", 
            "area_type":  "a", 
            "region_id":"dach",
            "strict_boundary":True,
            "institution":False}
input = {"config": json.dumps(collab_config)}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)

""" get weighted collaboration of institutions"""
url = "http://127.0.0.1:8030/api/db/get_weighted_collab"
config = {"from_year": 2010, "institution":True}
input = {"config": json.dumps(config)}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)


""" get all the collaborations between two authors"""
url = "http://127.0.0.1:8030/api/db/get_collab_pid"
config ={"from_year": 2010,"area_id" : "ai", "area_type":  "a"}
input = {"pid_x": "24/8616","pid_y": "61/5017", "config": json.dumps(config)}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)


""" get all the collaborations between two institutions"""
url = "http://127.0.0.1:8030/api/db/get_collab_institution"
config ={"from_year": 2010}
input = {"inst_x": "Tsinghua University","inst_y": "Tsinghua University", "config": json.dumps(config)}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)