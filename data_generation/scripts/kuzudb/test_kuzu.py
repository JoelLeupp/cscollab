import kuzu

# create db 
db = kuzu.database('./kuzu_db')
conn = kuzu.connection(db)

# create schema

## author
conn.execute("""CREATE NODE TABLE 
                Author( pid STRING, 
                        name STRING, 
                        affiliation STRING, 
                        homepage STRING, 
                        scholarid STRING, 
                        PRIMARY KEY (pid))""")
                   
## Institution
conn.execute("""CREATE NODE TABLE  
                Institution(    institution STRING, 
                                country_code STRING, 
                                country_name STRING, 
                                region_code STRING, 
                                region_name STRING, 
                                lat DOUBLE,
                                lon DOUBLE,
                                PRIMARY KEY (institution))""")


# load author data in db
conn.execute('COPY Author FROM "output/graph/nodes_authors.csv" (DELIM=";")')

conn.execute('COPY Institution FROM "output/mapping/geo-mapping.csv" (HEADER=true)')

# check data
results = conn.execute('''  MATCH (a:Author) 
                            WHERE a.affiliation IS NOT NULL 
                            RETURN a.pid, a.name, a.affiliation;''').getAsDF()
results.head()
results.shape


results = conn.execute('''  MATCH (i:Institution) 
                            RETURN *;''').getAsDF()

# delete relationships 
# conn.execute('DROP TABLE Author')

conn.execute("CREATE NODE TABLE Person(name STRING, inst STRING, PRIMARY KEY (name))")
conn.execute("CREATE NODE TABLE Paper(id INT64, year INT64, PRIMARY KEY (id))")
conn.execute("CREATE REL TABLE Contributed(FROM Person TO Paper)")
conn.execute("CREATE REL TABLE Collab(FROM Person TO Person, paper INT64)")

 

# load data
conn.execute('COPY Person FROM "data/test/author.csv"')
conn.execute('COPY Paper FROM "data/test/paper.csv"')
conn.execute('COPY Contributed FROM "data/test/contributed.csv"')
conn.execute('COPY Collab FROM "data/test/collabs.csv"')

 

# query
conn.execute('MATCH (a:Person) where a.inst = "A" RETURN a.name;').getAsDF()

conn.execute('''MATCH (a:Person)-[c:Contributed]->(p:Paper) 
                where a.inst = "A" RETURN p.id, a.name;''').getAsDF()

conn.execute('''MATCH (a:Person) -[c:Contributed]->(p:Paper)
                MATCH (ab:Person) -[cb:Contributed]->(pb:Paper)
                where a.inst = "A" AND ab.inst = "A" AND
                p.id = pb.id AND a.name <> ab.name
                RETURN p.id, a.name, ab.name
                ;''').getAsDF()


conn.execute('''MATCH (a:Person)-[c:Collab]->(b:Person)
                MATCH (p:Paper)
                WHERE c.paper = p.id  AND a.name = "A" AND b.name = "D"
                RETURN [T | p.id]
                ;''').getAsDF()



# delete

conn.execute("DROP TABLE contributed")
conn.execute("DROP TABLE Author")
conn.execute("DROP TABLE Paper")

