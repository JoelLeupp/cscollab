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

dblp_path = 'data/dblp.xml'
output_dir = "output/dblp" 


def clear_element(element):
    """Free up memory for temporary element tree after processing the element"""
    element.clear()
    while element.getprevious() is not None:
        del element.getparent()[0]


# create proceedings datastructure 
def proceeding_struct(id, title, conf, year):
    return {"id": id, "title": title, "conf": conf, "year": year}


def parse_proceedings(cut_off = 2005):
    
    proceedings = []   
    
    with gzip.open('data/dblp.xml.gz') as f:
                
        for (event, node) in ElementTree.iterparse(f, dtd_validation=True, load_dtd=True):
            
            if node.tag == "proceedings":
                
                key = node.attrib["key"] 
                # only consider conferences
                if key.split("/")[0] != "conf":
                    clear_element(node)
                    continue
                
                title = node.find("title").text
                year = int(node.find("year").text)
                # only consider proceedings later than the year 2005
                if (year < cut_off):
                    clear_element(node)
                    continue
                conf_ele = node.find("booktitle")
                conf = conf_ele.text if conf_ele is not None else key.split("/")[1]
                
                proceedings.append(proceeding_struct(key, title, conf, year))
                clear_element(node)


    with open(os.path.join(output_dir, "proceedings.json"), "w") as write_file:
        json.dump(proceedings, write_file, indent=3,ensure_ascii=False)


parse_proceedings()

with open(os.path.join(output_dir, "proceedings.json"), "r") as f:
    proceedings = json.load(f)
    

# print(ElementTree.tostring(node, pretty_print=True))


