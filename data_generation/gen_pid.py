# get the internal author id (pid) from dblp for every author in csrankings

import requests 
import xml.etree.ElementTree as ET 
import numpy as np  
import pandas as pd 
import time


csrankings = pd.read_csv("data/csrankings.csv")

# dblp search author API
def url(author):
    return "https://dblp.org/search/author?xauthor={}".format(author)

# get all authors from csrankings
authors = csrankings["name"].to_list()

# author pid map
authors_pid = []

for author in authors:
    author_clean = author.split("[")[0].strip() # remove brakets
    # use dblp author search API
    r = requests.get(url(author_clean))
    time.sleep(1)
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
author_pid_table.to_csv("output/authors_pid_3.csv")
author_pid_table = pd.read_csv("output/authors_pid_4.csv")


# check for which other the pid cound not be found 
not_found = author_pid_table[author_pid_table["pid"].isnull()]["author"].to_list()

geo_mapping = author_pid_table = pd.read_csv("output/geo-mapping.csv")

missing_countries = []
for a in not_found:
    inst = csrankings["affiliation"][csrankings["name"] == a].values[0]
    country = geo_mapping["country_name"][geo_mapping["institution"] == inst].values[0]
    missing_countries.append({"author": a, 
                              "country": country, 
                              "institution": inst})
    
missing_countries = pd.DataFrame(missing_countries)
missing_countries["country"].value_counts()
missing_countries.to_csv("output/missing_authors.csv")

# check the missing authors in austria germany and switzerland
missing_countries[missing_countries["country"] == "Switzerland"]
missing_countries[missing_countries["country"] == "Germany"]
