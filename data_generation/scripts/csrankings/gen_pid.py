
""" get the internal author id (pid) from dblp for every author in csrankings"""

import requests 
import xml.etree.ElementTree as ET 
import numpy as np  
import pandas as pd 
import time

""" load csrankings data """
csrankings = pd.read_csv("data/csrankings.csv")

""" get all authors from csrankings """
authors = csrankings["name"].to_list()


def url(author):
    """ dblp search author API """
    return "https://dblp.org/search/author?xauthor={}".format(author)


""" author pid map """
authors_pid = []

for author in authors:
    author_clean = author.split("[")[0].strip() # remove brakets
    
    """ use dblp author search API """
    r = requests.get(url(author_clean))
    time.sleep(1)
    root = ET.fromstring(r.content)  

    """ get all the possible author options from the request """
    options = []
    for person in root.iter('author'):  
            person = {"pid": person.attrib["pid"], "name": person.text }
            options.append(person)


    """ check if there is only one unique pid as option """
    single_pid = len(set(map(lambda x : x["pid"], options))) == 1

    """ check if there is an exact match of the author name """
    exact_matches = list(filter(lambda x: x["name"] == author, options))

    if single_pid: 
        p = options[0]
    elif len(exact_matches) == 1:
        p = exact_matches[0]
    else:
        p = {"name": None, "pid": None}

    authors_pid.append({"author" : author, "name": p["name"], "pid": p["pid"]})


""" save result as csv """
author_pid_table = pd.DataFrame(authors_pid)
author_pid_table[["author", "pid"]].to_csv("output/pid/authors-pid-all.csv", index=False)

author_pid_table = pd.read_csv("output/pid/authors-pid-all.csv")

""" check for which other the pid cound not be found """
not_found = author_pid_table[author_pid_table["pid"].isnull()]["author"].to_list()

geo_mapping =  pd.read_csv("output/mapping/geo-mapping.csv")

missing_countries = []
for a in not_found:
    inst = csrankings["affiliation"][csrankings["name"] == a].values[0]
    country = geo_mapping["country_name"][geo_mapping["institution"] == inst].values[0]
    missing_countries.append({"author": a, 
                              "country": country, 
                              "institution": inst})
    
missing_countries = pd.DataFrame(missing_countries)
missing_countries["country"].value_counts()
missing_countries.to_csv("output/pid/missing-authors.csv")

missing_countries = pd.read_csv("output/pid/missing-authors.csv")

""" check the missing authors in austria germany and switzerland """
missing_countries[missing_countries["country"] == "Switzerland"]
missing_countries[missing_countries["country"] == "Germany"]

""" create unique pid set """
author_pid_table = pd.read_csv("output/pid/authors-pid-all.csv")
author_pid_table = author_pid_table[~author_pid_table["pid"].isnull()]
pids = author_pid_table["pid"].unique()
author_pid_unique_name = []
for id in pids:
    author_list = author_pid_table[author_pid_table["pid"]==id]
    
    """ if there are several possible names for a single pid take the first one of the alphabetic order """
    author_pid_unique_name.append({"author": author_list.iloc[0,0], "pid": author_list.iloc[0,1]})
    
""" save output as csv """
author_pid_unique = pd.DataFrame(author_pid_unique_name)
author_pid_unique.to_csv("output/pid/authors-pid.csv", index=False)

""" view output file """
author_pid_unique = pd.read_csv("output/pid/authors-pid.csv")
author_pid_unique.describe()
#             author         pid
# count        20986       20986
# unique       20986       20986
# top     A Min Tjoa  t/AMinTjoa

