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
input = {"conf":"aaai"}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)


""" get authors from csranking with their affiliation filtered on region/country"""
url = "http://127.0.0.1:8030/api/db/get_csauthors"
input = {"region_id":"dach"}
x = requests.post(url, json = input)
res = pd.DataFrame(json.loads(x.content))
print(res)
