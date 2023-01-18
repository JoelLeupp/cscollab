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
            clauses.append("{}.year > {}".format(return_type,cut_off))
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

# get all auhtors and institutions for which we have an affiliation
def get_csranking_authors():
    result = conn.execute('''  
                    MATCH (a:Author)-[af:Affiliation]->(i:Institution)
                    RETURN a.pid, a.name, i.name, i.lat, i.lon;
                    ''').getAsDF()   
    result.columns=["pid", "name", "institution", "lat", "lon"]
    return result 
# csranking_authors = get_csranking_authors() 
# print(csranking_authors.head(),"\n", csranking_authors.shape)

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

def get_csauthors(country_id = None, region_id = "wd"):
    result = conn.execute('''  
                MATCH 
                (a:AuthorCS)-[af:AffiliationCS]->
                (i:Institution)-[l:LocatedIn]->
                (c:Country)-[ir:InRegion]->(r:Region)
                WHERE r.id = "dach"
                RETURN a.pid, a.name,i
                ''').getAsDF()       
    print(result.head(),"\n", result.shape) 


# get all collaborations from cs rankings
result = get_by_area({  "area_id" : "ai", 
                        "area_type":  "a", 
                        "cut_off":2010,
                        "return_type" :"i"})

ai_inproceedings = result["i.id"].values
ai_inproceedings_idx = dict(zip(ai_inproceedings,np.repeat(True, len(ai_inproceedings))))

result = conn.execute('''
                        MATCH (x:AuthorCS)-[col:CollaborationCS]->(y:AuthorCS)
                        RETURN x.pid, y.pid, col
                        ''').getAsDF()       
print(result.head(),"\n", result.shape)
collabs_ai = result[list(map(lambda x: ai_inproceedings_idx.get(x, False), result["col.record"]))]

# sort authors alphabetically to make sure the collaborations between authors is correctly aggregated
collabs_ai_tuples = []
for _, row in collabs_ai.iterrows():
    if row["x.pid"]>row["y.pid"]:
        row_tuple = (row["y.pid"],row["x.pid"],row["col.record"])
    else :
        row_tuple = (row["x.pid"],row["y.pid"],row["col.record"])
    collabs_ai_tuples.append(row_tuple)
collabs_ai_sorted = pd.DataFrame(collabs_ai_tuples, columns = ["author_a", "author_b", "rec"])
    
grouped = collabs_ai_sorted.groupby(["author_a", "author_b"])["rec"].count().sort_values(ascending=False).reset_index() 

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



# get all collaborations between instituions from cs rankings
csranking_collabs = conn.execute('''  
                MATCH (c:Country)-[i:InRegion]->(r:Region) 
                WHERE 
                c.year >= 2010
                AND
                a.affiliation IS NOT NULL
                AND 
                b.affiliation IS NOT NULL
                RETURN a.affiliation, b.affiliation, count(c.record) AS weight
                ''').getAsDF()       
print(csranking_collabs.head(),"\n", csranking_collabs.shape)

# get all institutuions from country
csranking_collabs = conn.execute('''  
                MATCH 
                (i:Institution)-[l:LocatedIn]->(c:Country),
                (c)-[ir:InRegion]->(r:Region)
                WHERE c.id = "ch"
                RETURN i.name, c.name
                ''').getAsDF()       
print(csranking_collabs.head(),"\n", csranking_collabs.shape)

result = conn.execute('''  
                MATCH 
                (a:AuthorCS)-[af:AffiliationCS]->
                (i:Institution)-[l:LocatedIn]->
                (c:Country)-[ir:InRegion]->(r:Region)
                WHERE r.id = "dach"
                RETURN a.pid, a.name,i
                ''').getAsDF()       
print(result.head(),"\n", result.shape)


rec_from_ch_inst = result["col.record"]

result = conn.execute('''  
                MATCH 
                (i:Institution)-[l:LocatedIn]->(c:Country)-[ir:InRegion]->(r:Region)
                WHERE c.id = "ch"
                RETURN DISTINCT i.name
                ''').getAsDF()       
print(result.head(),"\n", result.shape)

#result["i.name"].str.encode(encoding = 'utf-8').str.decode(encoding = 'utf-8')
dach_inst = result["i.name"].values

def list_as_string(l):
    s = '['
    for i in l:
        s += '"{}", '.format(i)
    return s[:-2] + "]"

result = conn.execute('''UNWIND {} AS x
                         WITH x
                         MATCH 
                         (a:Author)-[af:Affiliation]->(i:Institution),
                         (a)-[col:Collaboration]->(b:Author)
                         WHERE  i.name = x
                         RETURN col.id
                         '''.format(list_as_string(dach_inst))).getAsDF()       
print(result.head(),"\n", result.shape)

result = conn.execute('''UNWIND {} AS x
                         WITH x
                         MATCH 
                         (a:Author)-[af:Affiliation]->(i:Institution),
                         (a)-[c:Collaboration]->(b:Author)
                         WHERE  i.name = x
                         RETURN a.name, b.name, c.record
                         UNION
                         UNWIND {} AS x
                         WITH x
                         MATCH 
                         (b:Author)-[af:Affiliation]->(i:Institution),
                         (a:Author)-[c:Collaboration]->(b)
                         WHERE  i.name = x
                         RETURN a.name, b.name, c.record
                         '''.format(list_as_string(dach_inst),list_as_string(dach_inst))).getAsDF()       
print(result.head(),"\n", result.shape)

result = conn.execute('''UNWIND {} AS x
                         WITH x
                         MATCH 
                         (a:Author)-[af:Affiliation]->(i:Institution)
                         WHERE  i.name = x
                         RETURN a.pid
                         '''.format(list_as_string(dach_inst))).getAsDF()       
print(result.head(),"\n", result.shape)

dach_authors = result["a.pid"].values

result = conn.execute('''UNWIND {} AS dach_authors1
                         WITH dach_authors1
                         UNWIND {} AS dach_authors2
                         WITH dach_authors1, dach_authors2
                         MATCH 
                         (a:Author)-[c:Collaboration]->(b:Author)
                         WHERE  a.pid = dach_authors1 AND b.pid = dach_authors2
                         RETURN a.pid, b.pid, c.record
                         '''.format(list_as_string(dach_authors),list_as_string(dach_authors))).getAsDF()       
print(result.head(),"\n", result.shape)

result = conn.execute(''' MATCH 
                         (a:Author)
                         RETURN a.pid
                         '''.format(list_as_string(dach_authors),list_as_string(dach_authors))).getAsDF()       
print(result.head(),"\n", result.shape)

result = conn.execute('''MATCH 
                         (a:Author)-[af1:Affiliation]->(i2:Institution)-[l1:LocatedIn]->(c1:Country)-[ir1:InRegion]->(r:Region),
                         (b:Author)-[af2:Affiliation]->(i1:Institution)-[l2:LocatedIn]->(c2:Country)-[ir2:InRegion]->(r),
                         (a)-[col:Collaboration]->(b)
                         WHERE r.id = "dach"
                         RETURN a.pid, b.pid, col.record, c1.name, c2.name
                         '''.format(list_as_string(dach_authors),list_as_string(dach_authors))).getAsDF()       
print(result.head(),"\n", result.shape)


ch_inst = ['EPFL', 'ETH Zurich', 'University of Bern', 'University of Zurich','Università della Svizzera italiana']
result = conn.execute('''MATCH 
                         (c1:Country)-[ir1:InRegion]->(r1:Region),
                         (c2:Country)-[ir2:InRegion]->(r2:Region),
                         (a:Author)-[col:Collaboration]->(b:Author)
                         WHERE 
                         a.country = c1.id AND r1.id = "dach" AND b.country = c2.id AND r2.id = "dach"
                         RETURN a.pid, b.pid
                         ''').getAsDF()       
print(result.head(),"\n", result.shape)

result = conn.execute('''UNWIND ["ch","at","de"] AS dach_region
                         WITH dach_region 
                         MATCH 
                         (a:Author)-[col:Collaboration]->(b:Author)
                         WHERE 
                         a.country = "de" AND b.country = dach_region AND col.year > 2005
                         RETURN a.pid, b.pid, count(col.record)
                         '''.format(list_as_string(ai_inproceedings))).getAsDF()       
print(result.head(),"\n", result.shape)

result = conn.execute('''MATCH (a:Author)-[col:Collaboration]->(b:Author)
                         WHERE 
                         a.country = "ch" AND b.country = "de" AND col.year > 2005
                         RETURN a.pid, b.pid, count(col.record)
                         ''').getAsDF()       
print(result.head(),"\n", result.shape)

result = conn.execute('''UNWIND {} AS ch_inst
                        WITH ch_inst
                        MATCH 
                        (a:Author)-[col:Collaboration]->(b:Author)
                        WHERE a.affiliation = ch_inst
                        RETURN a.name, a.affiliation
                        '''.format(list_as_string(ch_inst))).getAsDF()       
print(result.head(),"\n", result.shape)

t = result["i.name"].str.encode(encoding = 'utf-8').str.decode(encoding = 'utf-8')
t = t.str.decode(encoding = 'utf-8')



