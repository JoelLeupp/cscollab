import requests
import json
import pandas as pd

""" example GET calls """
# http://127.0.0.1:8030/api/db/get_region_mapping
# https://cscollab.ifi.uzh.ch/backend/api/db/get_region_mapping
# https://cscollab.ifi.uzh.ch/backend/api/db/get_area_mapping
# https://cscollab.ifi.uzh.ch/backend/api/db/get_conference?conf=aaai
# http://127.0.0.1:8030/api/db/get_area_mapping
# http://127.0.0.1:8030/api/db/get_conference?conf=aaai

local_url = "http://127.0.0.1:8030"
server_url = "https://cscollab.ifi.uzh.ch/backend"

url_base = server_url 

""" get region mapping """
url = url_base+"/api/db/get_region_mapping"
x = requests.get(url)
res = pd.DataFrame(json.loads(x.content))
print(res)

""" get area mapping """
url =  url_base+"/api/db/get_area_mapping"
x = requests.get(url)
res = pd.DataFrame(json.loads(x.content))
print(res)

""" get all inporceedings and proceedings from confernce"""
url =  url_base+"/api/db/get_conference"
input = {"conf":"icse"}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)


""" get authors from csranking with their affiliation filtered on region/country"""
url =  url_base+"/api/db/get_csauthors"
input = {"region_id":"dach"}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content, encoding='utf-8'))
print(res)

""" get collaboration of author/institution"""
url = url_base+"/api/db/get_collaboration"
collab_config = {  "from_year":2015,
            "area_ids" : ["ai","systems"], 
            "sub_area_ids":  ["robotics","bio"], 
            "region_ids":["europe","northamerica"],
            "country_ids":["jp","sg"],
            "strict_boundary":True
            }
input = {"config": json.dumps(collab_config)}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)

""" get collaboration of author plus area, country and institution information"""
url = url_base+"/api/db/get_flat_collaboration"
input = {"ignore_area": False}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)


""" get weighted collaboration of author"""
url = url_base+"/api/db/get_weighted_collab"
# config = {  "from_year": 2010,
#             "area_id" : "ai", 
#             "area_type":  "a", 
#             "region_id":"dach",
#             "strict_boundary":True,
#             "institution":False}
config = { "from_year": 2015,
            "to_year": 2023,
            "area_ids" : ["ai","systems"], 
            "sub_area_ids":  ["robotics","bio"], 
            "region_ids":["europe","northamerica"],
            "country_ids":["jp","sg"],
            "strict_boundary":True,
            "institution":False}
input = {"config": json.dumps(config)}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)

""" area and sub area frequeny"""
url = url_base+"/api/db/get_frequency_research_field"
config={ "from_year": 2015,
            "region_ids":["wd"],
            "strict_boundary":True,
            "institution":False}
input = {"config": json.dumps(config)}
x = requests.post(url, json = input)
res = json.loads(x.content)
print(res)

""" get all publications of a node based on the config"""
url = url_base+"/api/db/get_publications_node"
config = { "from_year": 2015,
            "to_year": 2023,
            "area_ids" : ["ai","systems"], 
            "sub_area_ids":  ["robotics","bio"], 
            "region_ids":["europe","northamerica"],
            "country_ids":["jp","sg"],
            "strict_boundary":True,
            "institution":False}
input = {"config": json.dumps(config),
         "node":"m/EvangelosMarkakis"}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)

url = url_base+"/api/db/get_publications_node"
config = { "from_year": 2015,
            "to_year": 2023,
            "area_ids" : ["ai","systems"], 
            "sub_area_ids":  ["robotics","bio"], 
            "region_ids":["europe","northamerica"],
            "country_ids":["jp","sg"],
            "strict_boundary":True,
            "institution":True}
input = {"config": json.dumps(config),
         "node":"EPFL"}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)

""" get all publications of an edge based on the config"""
url = url_base+"/api/db/get_publications_edge"
config = { "from_year": 2015,
            "to_year": 2023,
            "area_ids" : ["ai","systems"], 
            "sub_area_ids":  ["robotics","bio"], 
            "region_ids":["europe","northamerica"],
            "country_ids":["jp","sg"],
            "strict_boundary":True,
            "institution":True}
input = {"config": json.dumps(config),
         "edge":["EPFL", "Ecole Normale Superieure"]}
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
url = url_base+"/api/db/get_collab_institution"
config ={"from_year": 2010}
input = {"inst_x": "Tsinghua University","inst_y": "Tsinghua University", "config": json.dumps(config)}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)

""" get graph analytics"""
url = url_base+"/api/analytics/get_analytics"
input = {"nodes": [1, 2, 3, 4],
         "edges": [[2,1],[2,4],[1,3],[3,2],[1,4]], 
         "weighted":False,
         "top": 5}
x = requests.post(url, json = input)
res = json.loads(x.content)
print(res["statistics"])
print(res["centralities"])


""" get graph analytics from weighted collaboration graph"""
url = url_base+"/api/analytics/get_analytics_collab"
config={ "from_year": 2015,
            "to_year": 2023,
            "area_ids" : ["ai","systems"], 
            "sub_area_ids":  ["robotics","bio"], 
            "region_ids":["europe","northamerica"],
            "country_ids":["jp","sg"],
            "strict_boundary":True,
            "institution":True}
input = {"config": json.dumps(config),
         "top": 5}
x = requests.post(url, json = input)
res = json.loads(x.content)
print(res)



url = url_base+"/api/gcn/get_node_position"
config = { "from_year": 2015,
            "region_ids":["dach"],
            "strict_boundary":True,
            "institution":True}
use_sub_areas = False
input = {"config": json.dumps(config),
         "sub_areas":use_sub_areas}
x = requests.post(url, json = input)
res = json.loads(x.content)
print(res)


url = url_base+"/api/gcn/get_node_position"
config = { "from_year": 2015,
            "region_ids":["dach"],
            "strict_boundary":True,
            "institution":True}
use_sub_areas = True
input = {"config": json.dumps(config),
         "sub_areas":use_sub_areas}
x = requests.post(url, json = input)
res = json.loads(x.content)
print(res)


