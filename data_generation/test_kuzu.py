import kuzu

# create db 
db = kuzu.database('./kuzu_db')
conn = kuzu.connection(db)