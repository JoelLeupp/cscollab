#-------------------------------------
# example of usefull queries on the kuzu db 

import kuzu
import pandas as pd
import numpy as np
import json

# create db 
db = kuzu.database('./kuzu_db')
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
                RETURN *;
                ''').getAsDF()            
print(area_mapping,"\n", area_mapping.shape)

# get all proceedings that are categorized with an area
proceedings_all = conn.execute('''  
                MATCH (p:Proceeding)-[b:BelongsToArea]->(a:SubArea) 
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
