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
                   

# load author data in db
conn.execute('COPY Author FROM "output/graph/nodes_authors.csv" (DELIM=";")')

# check data
results = conn.execute('''  MATCH (a:Author) 
                            WHERE a.affiliation IS NOT NULL 
                            RETURN a.pid, a.name, a.affiliation;''').getAsDF()
results.head()
results.shape


# delete relationships 
# conn.execute('DROP TABLE Author')

