#-------------------------------------
# A Collection of usefull queries on the kuzu db 
#-------------------------------------

import kuzu
import pandas as pd
import numpy as np
import json

# connect to db
db = kuzu.database(database_path='./kuzu_db', buffer_pool_size=4294967296)
# db.resize_buffer_manager(4294967296) # buffer pool size 4GB
conn = kuzu.connection(db)

# get country region mapping
country_map = conn.execute('''  
                MATCH (c:Country)-[i:InRegion]->(r:Region) 
                RETURN *;
                ''').getAsDF()            
print(country_map.head(),"\n", country_map.shape)

# get computer science area and sub-areas
area_mapping = conn.execute('''  
                MATCH (s:SubArea)-[o:SubAreaOf]->(a:Area) 
                RETURN a.label, s.label;
                ''').getAsDF()            
print(area_mapping,"\n", area_mapping.shape)

# get all inproceedings that are categorized with an area
proceedings_all = conn.execute('''  
                MATCH (p:Proceeding)-[b:BelongsToArea]->(a:SubArea),
                (i:Inproceeding)-[c:Crossref]->(p)
                RETURN  p;
                ''').getAsDF()            
print(proceedings_all.head(),"\n", proceedings_all.shape)

# get all proceedings of a subarea
sub_area = "vision"
proceedings_in_sub = conn.execute('''  
                MATCH (p:Proceeding)-[b:BelongsToArea]->(a:SubArea) 
                WHERE a.id = "{}"
                RETURN  p;
                '''.format(sub_area)).getAsDF()            
print(proceedings_in_sub.head(),"\n", proceedings_in_sub.shape)

# get all inproceedings of an area
area = "ai"
proceedings_in_area = conn.execute('''  
                MATCH 
                (sa:SubArea)-[s:SubAreaOf]->(a:Area {{id:"{}"}}), 
                (p:Proceeding)-[b:BelongsToArea]->(sa),
                (i:Inproceeding)-[c:Crossref]->(p)
                RETURN  i;
                '''.format(area)).getAsDF()            
print(proceedings_in_area.head(),"\n", proceedings_in_area.shape)


# get the conference name of a proceeding
conn.execute('''MATCH 
                (p:Proceeding {id:"conf/aaai/2011learning"})-[b:BelongsToConf]->(c:Conference)
                RETURN c.title;
                ''').getAsDF() 


# get all auhtors and institutions for which we have an affiliation
csranking_authors = conn.execute('''  
                MATCH (a:Author)-[af:Affiliation]->(c:Institution)
                RETURN a.name,c;
                ''').getAsDF()       
print(csranking_authors.head(),"\n", csranking_authors.shape)

# get all collaborations from cs rankings
csranking_collabs = conn.execute('''  
                MATCH (a:Author)-[c:Collaboration]->(b:Author)
                WHERE 
                c.year >= 2010
                AND
                a.affiliation IS NOT NULL
                AND 
                b.affiliation IS NOT NULL
                RETURN a.name, b.name, count(c.record) AS weight
                ''').getAsDF()       
print(csranking_collabs.head(),"\n", csranking_collabs.shape)

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
                (i:Institution)-[l:LocatedIn]->(c:Country)-[ir:InRegion]->(r:Region),
                (a:Author)-[af:Affiliation]->(i),
                (a)-[col:Collaboration]->(b:Author)
                WHERE r.id = "europe"
                RETURN DISTINCT col.record
                ''').getAsDF()       
print(result.head(),"\n", result.shape)


rec_from_ch_inst = result["col.record"]

result = conn.execute('''  
                MATCH 
                (i:Institution)-[l:LocatedIn]->(c:Country)-[ir:InRegion]->(r:Region)
                WHERE r.id = "europe"
                RETURN DISTINCT i.name
                ''').getAsDF()       
print(result.head(),"\n", result.shape)

european_inst = result["i.name"].values

def as_string(l):
    s = '['
    for i in l:
        s += '"' + i + '", '
    return s[:-2] + "]"

result = conn.execute('''UNWIND {} AS x
                         WITH x
                         MATCH (a:Author)-[af:Affiliation]->(i:Institution)
                         WHERE i.name = x
                         RETURN a.pid
                         '''.format(as_string(european_inst))).getAsDF()       
print(result.head(),"\n", result.shape)