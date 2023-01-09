import kuzu
import pandas as pd
import json

# create db 
db = kuzu.database('./kuzu_db')
conn = kuzu.connection(db)

# create schema
#-------------------------------------------

# -------- Nodes ----------
# author
conn.execute("""CREATE NODE TABLE Author(
                pid STRING, 
                name STRING, 
                affiliation STRING, 
                homepage STRING, 
                scholarid STRING, 
                PRIMARY KEY (pid))""")

# proceeding
conn.execute("""CREATE NODE TABLE Proceeding(
                id STRING, 
                title STRING, 
                conf STRING,
                year INT64,   
                PRIMARY KEY (id))""")

# inproceeding
conn.execute("""CREATE NODE TABLE Inproceeding(
                id STRING, 
                title STRING, 
                year INT64,   
                PRIMARY KEY (id))""")

# institution
conn.execute("""CREATE NODE TABLE Institution(
                name STRING, 
                lat DOUBLE, 
                lon DOUBLE,  
                PRIMARY KEY (name))""")

# country
conn.execute("""CREATE NODE TABLE Country(
                id STRING, 
                name STRING, 
                PRIMARY KEY (id))""")

# Region
conn.execute("""CREATE NODE TABLE Region(
                id STRING, 
                name STRING, 
                PRIMARY KEY (id))""")

# area
conn.execute("""CREATE NODE TABLE Area(
                id STRING, 
                label STRING, 
                PRIMARY KEY (id))""")

# sub area
conn.execute("""CREATE NODE TABLE SubArea(
                id STRING, 
                label STRING, 
                PRIMARY KEY (id))""")

# -------- Edges ----------
# collaborations
conn.execute("""CREATE REL TABLE Collaboration(
                FROM Author TO Author)""")

# affiliation
conn.execute("""CREATE REL TABLE Affiliation(
                FROM Author TO Institution)""")

# crossref
conn.execute("""CREATE REL TABLE Crossref(
                FROM Inproceeding TO Proceeding)""")

# area of conference
conn.execute("""CREATE REL TABLE BelongsToArea(
                FROM Proceeding TO SubArea)""")

# subarea 
conn.execute("""CREATE REL TABLE SubAreaOf(
                FROM SubArea TO Area)""")

# load data in db
conn.execute('COPY Author FROM "output/graph/nodes_authors.csv" (DELIM=";")')
conn.execute('COPY Proceeding FROM "output/graph/nodes_proceedings.csv" (DELIM=";", HEADER=true)')
conn.execute('COPY Inproceeding FROM "output/graph/nodes_inproceedings_short.csv" (DELIM=";", HEADER=true)')
conn.execute('COPY Institution FROM "output/graph/nodes_institution.csv" (DELIM=";", HEADER=true)')
conn.execute('COPY Country FROM "output/graph/nodes_countries.csv" (DELIM=";", HEADER=true)')
conn.execute('COPY Region FROM "output/graph/nodes_regions.csv" (DELIM=";", HEADER=true)')


# check data
results = conn.execute('MATCH (x:Region) RETURN *;').getAsDF()            
print(results.head())
print(results.shape)

inproceedings_df = pd.read_csv("output/graph/nodes_inproceedings.csv", sep=";")
inproceedings_rec = json.loads(inproceedings_df.to_json(orient="records"))

for i, rec in enumerate(inproceedings_rec):
    print(i)
    conn.execute('CREATE (p:Proceeding {{id: "{}", title: "{}", year:{}}})'.format(rec["id"],rec["title"],rec["year"]))
    
inproceedings_rec[311]

# inproceedings_df.iloc[:100].to_csv("output/graph/nodes_inproceedings_short.csv", 
#                         index=False, header=True, sep=";",doublequote=False, escapechar="\\")  


# drop tables
conn.execute("DROP TABLE Inproceeding")
