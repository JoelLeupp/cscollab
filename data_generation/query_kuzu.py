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
country_mapping = conn.execute('''  MATCH (c:Country)-[i:InRegion]->(r:Region) 
                                    RETURN *;''').getAsDF()            
print(country_mapping.head(),"\n", country_mapping.shape)
