
# %%
import requests
import json
import pandas as pd


local_url = "http://127.0.0.1:8030"
server_url = "https://cscollab.ifi.uzh.ch/backend"

url_base = local_url 
# %%

url_filtered_collab = url_base+"/api/db/get_filtered_collaboration"
config={ "from_year": 2005,
            "region_ids":["wd"],
            "strict_boundary":False,
            "institution":False}
input = {"config": json.dumps(config),
         "top": 10}
x = requests.post(url_filtered_collab, json = input)
res = pd.DataFrame(json.loads(x.content))
print("authors: {}".format(len(set(res["a_pid"]).union(set(res["b_pid"])))))
print("institutions: {}".format(len(set(res["a_inst"]).union(set(res["b_inst"])))))
print("publications: {}".format(len(set(res["rec_id"]))))
print("collaborations: {}".format(len(res)))

url_analytics = url_base+"/api/analytics/get_analytics_collab"
x = requests.post(url_analytics, json = input)
res = json.loads(x.content)
statistics=res["statistics"]

# %%
from_year = 2005
region_ids = ["wd"]
strict_boundary = False

url_filtered_collab = url_base+"/api/db/get_filtered_collaboration"
config={ "from_year": from_year,
            "region_ids":region_ids,
            "strict_boundary":strict_boundary,
            "institution":False}
input = {"config": json.dumps(config)}
x = requests.post(url_filtered_collab, json = input)
res = pd.DataFrame(json.loads(x.content))

results = {"authors": len(set(res["a_pid"]).union(set(res["b_pid"]))),
           "institution": len(set(res["a_inst"]).union(set(res["b_inst"]))),
           "publications": len(set(res["rec_id"])),
           "collaborations":len(res)}    


url_analytics = url_base+"/api/analytics/get_analytics_collab"
input = {"config": json.dumps(config),
         "top": 10}
x = requests.post(url_analytics, json = input)
res = json.loads(x.content)
statistics_athors=res["statistics"]
results["average degree author"] = statistics["average_degree"]
results["max degree author"] = statistics["max_degree"]
results["unique author collaborations"] = statistics["edges"]
results["authors connected?"] = statistics["is_connected"]
results["connected components authors"] = statistics["number_connected_components"]
results["larges connected component authors"] = statistics["largest_connected_component"]

config_inst ={ "from_year": from_year,
            "region_ids":region_ids,
            "strict_boundary":strict_boundary,
            "institution":True}
input = {"config": json.dumps(config_inst),
         "top": 10}
x = requests.post(url_analytics, json = input)
res = json.loads(x.content)
statistics_inst=res["statistics"]
results["average degree institution"] = statistics_inst["average_degree"]
results["max degree institution"] = statistics_inst["max_degree"]
results["unique institution collaborations"] = statistics_inst["edges"]
results["institutions connected?"] = statistics_inst["is_connected"]
results["connected components institutions"] = statistics_inst["number_connected_components"]
results["larges connected component institutions"] = statistics_inst["largest_connected_component"]

df = pd.DataFrame(results,index=[0])
df_results = df.transpose()
df_results
# %%

