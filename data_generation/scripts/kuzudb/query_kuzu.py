#-------------------------------------
# A Collection of usefull queries on the kuzu db 
#-------------------------------------

import kuzu
import pandas as pd
import numpy as np
import json

# connect to db
db = kuzu.database(database_path='./kuzu_db', buffer_pool_size=4294967296)
# db.resize_buffer_manager(8589934592) 
conn = kuzu.connection(db)

# helper function to convert lists as strings
def list_as_string(l):
    s = '['
    for i in l:
        s += '"{}", '.format(i)
    return s[:-2] + "]"

# get country region mapping
def get_region_mapping():
    result = conn.execute('''  
                    MATCH (c:Country)-[i:InRegion]->(r:Region) 
                    RETURN c.id, c.name, r.id, r.name;
                    ''').getAsDF()     
    result.columns=['country-id', 'country-name','region-id', 'region-name' ]
    return result
# region_mapping = get_region_mapping()
# print(region_mapping.head(),"\n", region_mapping.shape)

# get computer science area and sub-areas
def get_area_mapping():
    result = conn.execute('''MATCH (p:Proceeding)-[b:BelongsToArea]->(s:SubArea)-[o:SubAreaOf]->(a:Area),
                         (c:Conference)
                         WHERE p.conf = c.id
                         RETURN DISTINCT a.id, a.label, s.id, s.label, p.conf, c.title;
                    ''').getAsDF()      
    return result    

# area_mapping = get_area_mapping()
# print(area_mapping.head(),"\n", area_mapping.shape)

# get proceedings and inproceedings filtered by area/subarea
# area_type: a if main area s if sub area
# return_type: p if proceeding area i if inproceeding
# area_config = {"area_id" : "ai", 
#                "area_type":  "a", 
#                "cut_off":None,
#                "return_type" :"p"}
def get_by_area(area_config):
    
    cut_off = area_config.get("cut_off")
    return_type = area_config.get("return_type","p")
    area_id = area_config.get("area_id")
    area_type =  area_config.get("area_type")
    
    # generate where clause
    if area_id or cut_off:
        clauses = []
        if cut_off:
            clauses.append("{}.year >= {}".format(return_type,cut_off))
        if area_id:
            clauses.append('{}.id = "{}"'.format(area_type,area_id))
        where_clause =  "WHERE " + " AND ".join(clauses)
    else: 
        where_clause = ""
        
    # get return clause
    if return_type=="i":
        return_clause = "RETURN i"
    else: 
        return_clause = "RETURN DISTINCT p"
        
    result = conn.execute('''  
                    MATCH (p:Proceeding)-[b:BelongsToArea]->(s:SubArea)-[o:SubAreaOf]->(a:Area),
                    (i:Inproceeding)-[c:Crossref]->(p)
                    {}
                    {};
                    '''.format(where_clause,return_clause)).getAsDF()     
    return result       

# result = get_by_area({  "area_id" : "ai", 
#                         "area_type":  "a", 
#                         "cut_off":2010,
#                         "return_type" :"p"})
# print(result.head(),"\n", result.shape)

# result = get_by_area({  "area_id" : "vision", 
#                         "area_type":  "s", 
#                         "cut_off":2010,
#                         "return_type" :"p"})
# print(result.head(),"\n", result.shape)

# result = get_by_area({"return_type" :"i"})
# print(result.head(),"\n", result.shape)


# get the conference name of a given proceedings
def get_conference_name(proceeding_list):
    result = conn.execute('''UNWIND {} AS proceedings
                    WITH proceedings
                    MATCH 
                    (p:Proceeding)-[b:BelongsToConf]->(c:Conference)
                    WHERE p.id = proceedings
                    RETURN DISTINCT c.id, c.title;
                    '''.format(list_as_string(proceeding_list))).getAsDF() 
    result.columns=["id", "title"]
    return result

# proceeding_list = ['conf/ijcnn/2011', 'conf/ijcnn/2017', 'conf/ijcnn/2021','conf/mod/2016', 'conf/mod/2021-1']
# get_conference_name(proceeding_list)


# get all in/proceedings of a conference with name
def get_conference(conf):
    result = conn.execute('''  
                    MATCH 
                    (p:Proceeding)-[b:BelongsToConf]->(conf:Conference),
                    (i:Inproceeding)-[c:Crossref]->(p)
                    WHERE conf.id = "{}"
                    RETURN DISTINCT p.id, p.title, p.year, i.id, i.title;
                    '''.format(conf)).getAsDF()   
    return result 
# result = get_conference()   
# print(result.head(),"\n", result.shape)

# get authors from csranking with their affiliation filtered on region/country
def get_csauthors(country_id = None, region_id = "wd"):
     
    if country_id:
        where_clause = 'c.id = "{}"'.format(country_id)
    else:
        where_clause = 'r.id = "{}"'.format(region_id)
    
    result = conn.execute('''  
                MATCH 
                (a:AuthorCS)-[af:AffiliationCS]->
                (i:Institution)-[l:LocatedIn]->
                (c:Country)-[ir:InRegion]->(r:Region)
                WHERE {}
                RETURN a.pid, a.name,i
                '''.format(where_clause)).getAsDF()  
    result.columns = ["pid", "name", "institution", "lat", "lon"]  
    return result
# result = get_csauthors(region_id="dach")
# print(result.head(),"\n", result.shape)

# get collaboration of author/institution filtered on region and area 
# collab_config = {"area_id" : "ai", 
#                "area_type":  "a", 
#                "region_id":"dach",
#                "country_id":None,
#                "strict_boundary":True
#                }
def get_collaboration(collab_config):
    
    area_id = collab_config.get("area_id")
    area_type =  collab_config.get("area_type")
    region_id = collab_config.get("region_id")
    country_id =collab_config.get("country_id")
    strict_boundary = collab_config.get("area_type", True)

    # get authors from given region
    csauthors_region = get_csauthors(country_id=country_id, region_id=region_id)
    csauthors_region_idx = dict(zip(csauthors_region["pid"],np.repeat(True, csauthors_region.shape[0])))

    # get all inproceedings from cs rankings from an area
    inproceedings_of_area = get_by_area({  "area_id" : area_id, 
                            "area_type": area_type, 
                            "return_type" :"i"})


    inproceedings = inproceedings_of_area["i.id"].values
    inproceedings_idx = dict(zip(inproceedings,np.repeat(True, len(inproceedings))))

    # get all collaborations from csrankings authors
    collab = conn.execute('''MATCH (a:AuthorCS)-[col:CollaborationCS]->(b:AuthorCS)
                             RETURN a.pid AS a, b.pid AS b, col.record AS rec, col.year AS year''').getAsDF() 

    # filter collaboration by area
    collabs_area = collab[list(map(lambda x: inproceedings_idx.get(x, False), collab["rec"]))]

    # sort authors alphabetically to make sure the collaborations between authors is correctly aggregated
    def order_tuple(row):
        if row["a"]>row["b"]:
            row_tuple = (row["b"],row["a"],row["rec"], row["year"])
        else:
            row_tuple = (row["a"],row["b"],row["rec"], row["year"])
        return row_tuple
    
    # get ordered collaboration tuples
    collabs_tuples = np.array(list(map(lambda x: order_tuple(x[1]),collabs_area.iterrows())))

    # check if author is part of given region
    x_in_region = list(map(lambda x: csauthors_region_idx.get(x[0], False), collabs_tuples))
    y_in_region = list(map(lambda x: csauthors_region_idx.get(x[1], False), collabs_tuples))

    # exclude authors based on regional constraints
    if strict_boundary:
        # both authors must be from given region
        region_filter = np.logical_and(x_in_region,y_in_region)
    else: 
        # at least one author must be from given region
        region_filter = np.logical_or(x_in_region,y_in_region)

    # collaborations filtered on regional constraint
    collabs_tuples_filtered = collabs_tuples[region_filter]
    collabs_sorted = pd.DataFrame(collabs_tuples_filtered, columns = ["a", "b", "rec", "year"])
    collabs_sorted = collabs_sorted.astype({"year":'int'})
    return collabs_sorted

collabs_sorted = get_collaboration(
            {"area_id" : "ai", 
            "area_type":  "a", 
            "region_id":"dach",
            "strict_boundary":True
            })
collabs_sorted[collabs_sorted["year"]>=2018]

# get all csauthors and create a mapping pid->institution
csauthors_all = get_csauthors()
author_inst_map = dict(zip(csauthors_all["pid"],csauthors_all["institution"]))

collab = "institution"
if collab == "institution":
    collabs_ai_sorted = collabs_ai_sorted.replace(author_inst_map)

grouped = collabs_ai_sorted.groupby(["a", "b"])["rec"].count().sort_values(ascending=False).reset_index() 

test1=conn.execute('''MATCH (x:AuthorCS)-[col:CollaborationCS]->(y:AuthorCS)
                WHERE 
                (x.pid = "c/XilinChen" AND y.pid = "s/ShiguangShan") 
                OR 
                (y.pid = "c/XilinChen" AND x.pid = "s/ShiguangShan")
                WITH x.pid as x, y.pid as y, col.record as record
                MATCH (p:Proceeding)-[b:BelongsToArea]->(s:SubArea)-[o:SubAreaOf]->(a:Area),
                (i:Inproceeding)-[c:Crossref]->(p)
                WHERE a.id = "ai" AND i.id = record AND i.year > 2010
                RETURN x, y, record
                ''').getAsDF()  


# t = result["i.name"].str.encode(encoding = 'utf-8').str.decode(encoding = 'utf-8')
# t = t.str.decode(encoding = 'utf-8')



