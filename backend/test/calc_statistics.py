
import requests
import json
import pandas as pd


local_url = "http://127.0.0.1:8030"
server_url = "https://cscollab.ifi.uzh.ch/backend"

url_base = local_url 

regions = ["wd","northamerica","asia","europe","dach"]
from_year = 2005
df_results = []

for region in regions:
    for strict_boundary in [True,False]:
        region_ids = [region]
        name = region + " strict" if strict_boundary else region

        url_filtered_collab = url_base+"/api/db/get_filtered_collaboration"
        config_author={ "from_year": from_year,
                    "region_ids":region_ids,
                    "strict_boundary":strict_boundary,
                    "institution":False}
        input = {"config": json.dumps(config_author)}
        x = requests.post(url_filtered_collab, json = input)
        res_filtered = pd.DataFrame(json.loads(x.content))

        results = {"authors": len(set(res_filtered["a_pid"]).union(set(res_filtered["b_pid"]))),
                "institution": len(set(res_filtered["a_inst"]).union(set(res_filtered["b_inst"]))),
                "publications": len(set(res_filtered["rec_id"])),
                "collaborations":len(res_filtered)}    


        url_analytics = url_base+"/api/analytics/get_analytics_collab"
        input_author = {"config": json.dumps(config_author),
                "top": 10}
        x = requests.post(url_analytics, json = input_author)
        res = json.loads(x.content)
        statistics_athors=res["statistics"]

        config_inst ={ "from_year": from_year,
                    "region_ids":region_ids,
                    "strict_boundary":strict_boundary,
                    "institution":True}
        input_inst = {"config": json.dumps(config_inst),
                "top": 10}
        x = requests.post(url_analytics, json = input_inst)
        res = json.loads(x.content)
        statistics_inst=res["statistics"]

        results["unique author collaborations"] = statistics_athors["edges"]
        results["unique institution collaborations"] = statistics_inst["edges"]
        results["average degree author"] = statistics_athors["average_degree"]
        results["max degree author"] = statistics_athors["max_degree"]
        results["average degree institution"] = statistics_inst["average_degree"]
        results["max degree institution"] = statistics_inst["max_degree"]
        results["authors connected?"] = statistics_athors["is_connected"]
        results["connected components authors"] = statistics_athors["number_connected_components"]
        results["larges connected component authors"] = statistics_athors["largest_connected_component"]
        results["institutions connected?"] = statistics_inst["is_connected"]
        results["connected components institutions"] = statistics_inst["number_connected_components"]
        results["larges connected component institutions"] = statistics_inst["largest_connected_component"]

        df = pd.DataFrame(results,index=[name])
        df_results.append(df.transpose())
        
results_final = pd.concat(df_results,axis=1)
results_final.to_excel("statistics_" + from_year + ".xlsx")


url =  url_base+"/api/db/get_csauthors"
input = {}
x = requests.post(url, json = input)
csauthors = pd.DataFrame(json.loads(x.content, encoding='utf-8'))

csauthors_map = dict(zip(csauthors["pid"],csauthors["name"]))

regions = ["wd","northamerica","asia","europe","dach"]
from_year = 2015

df_centrality_authors = []
df_centrality_institutions = []

for region in regions:
    for strict_boundary in [True,False]:
        region_ids = [region]
        name = region + " strict" if strict_boundary else region

        url_filtered_collab = url_base+"/api/db/get_filtered_collaboration"
        config_author={ "from_year": from_year,
                    "region_ids":region_ids,
                    "strict_boundary":strict_boundary,
                    "institution":False}
    

        url_analytics = url_base+"/api/analytics/get_analytics_collab"
        input_author = {"config": json.dumps(config_author),
                "top": 10}
        x = requests.post(url_analytics, json = input_author)
        res = json.loads(x.content)
        centrality_athors=res["centralities"]["eigenvector_centrality"] #degree_centrality
        top_authors = list(map(lambda x: csauthors_map[x["id"]],centrality_athors))
        author_df = pd.DataFrame({name:top_authors})
        df_centrality_authors.append(author_df)

        config_inst ={ "from_year": from_year,
                    "region_ids":region_ids,
                    "strict_boundary":strict_boundary,
                    "institution":True}
        input_inst = {"config": json.dumps(config_inst),
                "top": 10}
        x = requests.post(url_analytics, json = input_inst)
        res = json.loads(x.content)
        centrality_inst=res["centralities"]["eigenvector_centrality"] #degree_centrality
        top_inst = list(map(lambda x: x["id"],centrality_inst))
        inst_df = pd.DataFrame({name:top_inst})
        df_centrality_institutions.append(inst_df)

        
results_centrality_authors = pd.concat(df_centrality_authors,axis=1)
results_centrality_institutions = pd.concat(df_centrality_institutions,axis=1)

results_centrality_authors.to_excel("centrality_author_" + str(from_year) + ".xlsx")
results_centrality_institutions.to_excel("centrality_inst_" + str(from_year) + ".xlsx")