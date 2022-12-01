# get the internal author id (pid) from dblp for every author in csrankings

import requests 
import xml.etree.ElementTree as ET 
import numpy as np  
import pandas as pd 

csrankings = pd.read_csv("data/csrankings.csv")

# dblp search author API
def url(author):
    return "https://dblp.org/search/author?xauthor={}".format(author)

# get all authors from csrankings
authors = csrankings["name"].to_list()

# author pid map
authors_pid = []

for author in authors:
    # use dblp author search API
    r = requests.get(url(author))
    root = ET.fromstring(r.content)  

    # get all the possible author options from the request
    options = []
    for person in root.iter('author'):  
            person = {"pid": person.attrib["pid"], "name": person.text }
            options.append(person)

    # check if there is only one unique pid as option
    single_pid = len(set(map(lambda x : x["pid"], options))) == 1

    # check if there is an exact match of the author name
    exact_matches = list(filter(lambda x: x["name"] == author, options))

    if single_pid: 
        p = options[0]
    elif len(exact_matches) == 1:
        p = exact_matches[0]
    else:
        p = {"name": None, "pid": None}

    authors_pid.append({"author" : author, "name": p["name"], "pid": p["pid"]})


# save result as csv
author_pid_table = pd.DataFrame(authors_pid)
author_pid_table.to_csv("output/authors_pid.csv")

# check for which other the pid cound not be found 
not_found = author_pid_table[author_pid_table["pid"].isnull()]["author"].to_list()
print(not_found)