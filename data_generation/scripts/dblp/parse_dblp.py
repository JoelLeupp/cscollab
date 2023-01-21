from datetime import datetime
import re
import os
import sys
import pandas as pd
import json
from lxml import etree as ElementTree
import gzip
import timeit
import time
from itertools import combinations
from fuzzywuzzy import fuzz

dblp_path = 'data/dblp.xml'
output_dir = "output/dblp" 

def clear_element(element):
    """Free up memory for temporary element tree after processing the element"""
    element.clear()
    while element.getprevious() is not None:
        del element.getparent()[0]



def proceeding_struct(id, title, conf, year):
    """ create proceedings datastructure """
    return {"id": id, "title": title, "conf": conf, "year": year}
 

def parse_proceedings(cut_off = 2005):
    """extract all proceedings from dblp """
    proceedings = []   
    
    with gzip.open('data/dblp.xml.gz') as f:
                
        for (event, node) in ElementTree.iterparse(f, dtd_validation=True, load_dtd=True):
            
            if node.tag == "proceedings":
                
                key = node.attrib["key"] 
                """only consider conferences"""
                if key.split("/")[0] != "conf":
                    clear_element(node)
                    continue
                
                title = node.find("title").text
                year = int(node.find("year").text)
                """only consider proceedings later than the GIVEN year"""
                if (year < cut_off):
                    clear_element(node)
                    continue
                conf_ele = node.find("booktitle")
                conf = conf_ele.text if conf_ele is not None else key.split("/")[1]
                
                proceedings.append(proceeding_struct(key, title, conf, year))
                clear_element(node)


    with open(os.path.join(output_dir, "proceedings.json"), "w") as write_file:
        json.dump(proceedings, write_file, indent=3,ensure_ascii=False)
    

def author_struct(pid, name):
    """ create proceedings datastructure """
    return {"pid": pid, "name": name}

def parse_authors():
    """ extract all authors with name and pid from dblp """
    authors = []   
    with gzip.open('data/dblp.xml.gz') as f:

        for (event, node) in ElementTree.iterparse(f, dtd_validation=True, load_dtd=True):
            
            if node.tag == "www":
                key = node.attrib["key"]
                """ check if the webpage is a dblp homepage of an author"""
                if not "homepages/" in key:
                    clear_element(node)
                    continue 
                """ extract pid which is used in the homepage path """
                pid = key.split("homepages/")[1]
                for author in node.findall("./author"):
                    
                    authors.append(author_struct(pid, author.text))
                
                clear_element(node)
            
            elif node.tag in ["article", "inproceedings" ,"proceedings"]:
                """ clear nodes that are not processed """
                clear_element(node)

    with open(os.path.join(output_dir, "authors.json"), "w") as write_file:
        json.dump(authors, write_file, indent=3,ensure_ascii=False)
    


def inproceeding_struct(id, title, year, crossref):
    """ create inproceedings datastructure """
    return {"id": id, "title": title, "year": year, "crossref": crossref}


def gen_edge_id(pid_u, pid_v, key):
    """ edge id conisting of the two pids and the dblp record key """
    return "{}-{}-{}".format(pid_u, pid_v, key)


def edge_struct(pid_u, pid_v, key):
    """ create edge datastructure for the collaboratioin network """
    return {"node/u": pid_u,
            "node/v": pid_v,
            "rec/id": key, # dblp record key for the inproceedings
            "edge/id": gen_edge_id(pid_u, pid_v, key)}
    
def parse_inproceedings(proceedings_ids, name_pid_map, cut_off = 2005):
    inproceedings = []   
    collabs = []
    
    with gzip.open('data/dblp.xml.gz') as f:
                
        for (event, node) in ElementTree.iterparse(f, dtd_validation=True, load_dtd=True):
            
            if node.tag == "inproceedings":
                
                key = node.attrib["key"] 
                title = re.sub("\n","",''.join(node.find("title").itertext())) #node.find("title").text
                year = int(node.find("year").text)
                """ only consider proceedings later than the GIVEN year """
                if (year < cut_off):
                    clear_element(node)
                    continue
                # crossref = node.find("crossref").text
                crossref_ele = node.find("crossref")
                if crossref_ele is not None:
                    crossref = crossref_ele.text
                else:
                    """ infer crossref from url """
                    inprocceding_url = node.find("url").text
                    crossref_url = inprocceding_url.split("db/")[1].split(".html#")[0]
                    ratios = list(map(lambda x: (x,fuzz.token_set_ratio(crossref_url, x)), proceedings_ids))
                    best_match = sorted(ratios, key= lambda x :x[1], reverse=True)[0]
                    if best_match[1] < 80:
                        print(best_match)
                        clear_element(node)
                        continue
                    else: 
                        crossref = best_match[0]  
                
                
                """ ignore inproceeding if it is not a valid reference (proceeding before cut off date) """
                if crossref not in proceedings_ids:
                    clear_element(node)
                    continue
                
                """ list of auhtor pids """
                authors =(list(map(lambda x: name_pid_map[x.text], node.findall("author"))))
                """ pairwise combination of authors """
                comb = combinations(authors, 2)
                """ create collaboration edge struct (only in one direction to save memory) """
                for c in comb:
                    collabs.append(edge_struct(c[0], c[1], key))
                
                inproceedings.append(inproceeding_struct(key, title, year, crossref))
                clear_element(node)
            
            elif node.tag in ["article", "proceedings" ,"www"]:
                """ clear nodes that are not processed """
                clear_element(node)


    with open(os.path.join(output_dir, "inproceedings.json"), "w") as write_file:
        json.dump(inproceedings, write_file, indent=3,ensure_ascii=False)
        
    with open(os.path.join(output_dir, "collabs.json"), "w") as write_file:
        json.dump(collabs, write_file, indent=3,ensure_ascii=False)
    
