import kuzu
import pandas as pd
import json
import time

""" create db """
db = kuzu.database(database_path='./kuzudb/db', buffer_pool_size=2147483648)
# db.resize_buffer_manager(4294967296) # buffer pool size 4GB
conn = kuzu.connection(db)

"""
create schema
"""


""" -------- Nodes ---------- """
""" author"""
conn.execute("""CREATE NODE TABLE Author(
                pid STRING, 
                name STRING, 
                affiliation STRING, 
                country STRING,
                homepage STRING, 
                scholarid STRING, 
                PRIMARY KEY (pid))""")

""" author csrankings """
conn.execute("""CREATE NODE TABLE AuthorCS(
                pid STRING, 
                name STRING, 
                affiliation STRING, 
                country STRING,
                homepage STRING, 
                scholarid STRING, 
                PRIMARY KEY (pid))""")

""" proceeding """
conn.execute("""CREATE NODE TABLE Proceeding(
                id STRING, 
                title STRING, 
                conf STRING,
                year INT64,   
                PRIMARY KEY (id))""")

""" inproceeding"""
conn.execute("""CREATE NODE TABLE Inproceeding(
                id STRING, 
                title STRING, 
                year INT64,
                PRIMARY KEY (id))""")

""" conference """
conn.execute("""CREATE NODE TABLE Conference(
                id STRING, 
                title STRING, 
                PRIMARY KEY (id))""")

""" institution """
conn.execute("""CREATE NODE TABLE Institution(
                name STRING, 
                lat DOUBLE, 
                lon DOUBLE,  
                PRIMARY KEY (name))""")

""" country """
conn.execute("""CREATE NODE TABLE Country(
                id STRING, 
                name STRING, 
                PRIMARY KEY (id))""")

""" Region """
conn.execute("""CREATE NODE TABLE Region(
                id STRING, 
                name STRING, 
                PRIMARY KEY (id))""")

""" area """
conn.execute("""CREATE NODE TABLE Area(
                id STRING, 
                label STRING, 
                PRIMARY KEY (id))""")

""" sub area """
conn.execute("""CREATE NODE TABLE SubArea(
                id STRING, 
                label STRING, 
                PRIMARY KEY (id))""")

""" -------- Edges ----------"""
""" collaborations """
conn.execute("""CREATE REL TABLE Collaboration(
                FROM Author TO Author, record STRING, year INT64, id STRING)""")

""" collaborations csrankings """
conn.execute("""CREATE REL TABLE CollaborationCS(
                FROM AuthorCS TO AuthorCS, record STRING, year INT64, id STRING)""")

""" affiliation """
conn.execute("""CREATE REL TABLE Affiliation(
                FROM Author TO Institution)""")

""" affiliation """
conn.execute("""CREATE REL TABLE AffiliationCS(
                FROM AuthorCS TO Institution)""")

""" located in """
conn.execute("""CREATE REL TABLE LocatedIn(
                FROM Institution TO Country)""")

""" crossref """
conn.execute("""CREATE REL TABLE Crossref(
                FROM Inproceeding TO Proceeding)""")

""" BelongsToConf """
conn.execute("""CREATE REL TABLE BelongsToConf(
                FROM Proceeding TO Conference)""")

""" area of conference """
conn.execute("""CREATE REL TABLE BelongsToArea(
                FROM Proceeding TO SubArea)""")

""" InRegion """
conn.execute("""CREATE REL TABLE InRegion(
                FROM Country TO Region)""")
""" subarea """
conn.execute("""CREATE REL TABLE SubAreaOf(
                FROM SubArea TO Area)""")

""" load data in db"""
"""---------------- nodes --------------------------------------------------------------------"""
conn.execute('COPY Author FROM "kuzudb/data/nodes_authors.csv" (DELIM=";")')
conn.execute('COPY AuthorCS FROM "kuzudb/data/nodes_authors_csrankings.csv" (DELIM=";")')
conn.execute('COPY Proceeding FROM "kuzudb/data/nodes_proceedings.csv" (DELIM=";", HEADER=true)')
conn.execute('COPY Inproceeding FROM "kuzudb/data/nodes_inproceedings.csv" (DELIM=";", HEADER=true)')
conn.execute('COPY Conference FROM "kuzudb/data/nodes_conferences.csv" (DELIM=";", HEADER=true)')
conn.execute('COPY Institution FROM "kuzudb/data/nodes_institution.csv" (DELIM=";", HEADER=true)')
conn.execute('COPY Country FROM "kuzudb/data/nodes_countries.csv" (DELIM=";", HEADER=true)')
conn.execute('COPY Region FROM "kuzudb/data/nodes_regions.csv" (DELIM=";", HEADER=true)')
conn.execute('COPY Area FROM "kuzudb/data/nodes_area.csv" (DELIM=";", HEADER=true)')
conn.execute('COPY SubArea FROM "kuzudb/data/nodes_sub_area.csv" (DELIM=";", HEADER=true)')
"""---------------- edges --------------------------------------------------------------------"""
conn.execute('COPY Collaboration FROM "kuzudb/data/edges_collabs.csv" (DELIM=";", HEADER=true)')
conn.execute('COPY CollaborationCS FROM "kuzudb/data/edges_collabs_csrankings.csv" (DELIM=";")')
conn.execute('COPY Affiliation FROM "kuzudb/data/edges_affiliated.csv" (DELIM=";")')
conn.execute('COPY AffiliationCS FROM "kuzudb/data/edges_affiliated.csv" (DELIM=";")')
conn.execute('COPY Crossref FROM "kuzudb/data/edges_crossref.csv" (DELIM=";")')
conn.execute('COPY BelongsToConf FROM "kuzudb/data/edges_belongs_to_conf.csv" (DELIM=";")')
conn.execute('COPY BelongsToArea FROM "kuzudb/data/edges_belongs_to_area.csv" (DELIM=";")')
conn.execute('COPY SubAreaOf FROM "kuzudb/data/edges_sub_area_of.csv" (DELIM=";")')
conn.execute('COPY InRegion FROM "kuzudb/data/edges_in_region.csv" (DELIM=";")')
conn.execute('COPY LocatedIn FROM "kuzudb/data/edges_located_in.csv" (DELIM=";")')


"""------------- drop tables------------"""
# conn.execute("DROP TABLE LocatedIn")
# conn.execute("DROP TABLE InRegion")
# conn.execute("DROP TABLE Collaboration")
# conn.execute("DROP TABLE CollaborationCS")
# conn.execute("DROP TABLE Affiliation")
# conn.execute("DROP TABLE AffiliationCS")
# conn.execute("DROP TABLE Crossref")
# conn.execute("DROP TABLE BelongsToArea")
# conn.execute("DROP TABLE SubAreaOf")
# conn.execute("DROP TABLE Author")
# conn.execute("DROP TABLE AuthorCS")
# conn.execute("DROP TABLE Inproceeding")
# conn.execute("DROP TABLE Proceeding")
# conn.execute("DROP TABLE Conference")
# conn.execute("DROP TABLE Institution")
# conn.execute("DROP TABLE Country")
# conn.execute("DROP TABLE Region")
# conn.execute("DROP TABLE Area")
# conn.execute("DROP TABLE SubArea")


# """ check data """
# results = conn.execute('MATCH (x:Conference) RETURN DISTINCT x.title;').getAsDF()            
# print(results)
# print(results.shape)


# test_csv = pd.read_csv("data/test/test.csv")

# conn.execute("""CREATE NODE TABLE TEST(
#                 name STRING,
#                 PRIMARY KEY (name))""")
# conn.execute('COPY TEST FROM "data/test/test.csv"')
# conn.execute('MATCH (x:Institution) RETURN *;').getAsDF()[20:30]

# conn.execute("DROP TABLE TEST")