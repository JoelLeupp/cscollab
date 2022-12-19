from datetime import datetime
import re
import os
import sys
import pandas as pd
import json
from lxml import etree as ElementTree
import gzip
import timeit

dblp_path = 'data/dblp.xml'

parser = ElementTree.XMLParser(attribute_defaults=True, load_dtd=True)

def clear_element(element):
    """Free up memory for temporary element tree after processing the element"""
    element.clear()
    while element.getprevious() is not None:
        del element.getparent()[0]

count = 0
db_iter = ElementTree.iterparse(source=dblp_path, dtd_validation=True, load_dtd=True)
for _, element in db_iter:
    if element.tag == "proceedings":
        for child in element:
            print("{}: {}".format(child.tag, child.text))
        # key = element.attrib["key"]
        # is_conf = key.split("/")[0] == "conf"
        # title_ele = element.find("title")
        # if title_ele:
        #    break 
    else:
        clear_element(element)
        
ElementTree.tostring(element)

#_with open(dblp_path, mode="r") as f:

with gzip.open("data/dblp.xml.gz") as f:  
    
    oldnode = None

    for (event, node) in ElementTree.iterparse(f, events=["start", "end"]):
        
        if oldnode is not None:
                oldnode.clear()
        oldnode = node
        
        print(node.text)
        
            
        # if node.tag == "proceedings":
        #     break
            # for child in node:
            #     print("{}: {}".format(child.tag, child.text))
       
parser = ElementTree.XMLParser(attribute_defaults=True, load_dtd=True)


def parseDBLP():

    # with open("dblp.xml", mode="r") as f:
    proceedings = []

    with gzip.open('data/dblp.xml.gz') as f:
        
        oldnode = None
        count = 0

        for (event, node) in ElementTree.iterparse(f, dtd_validation=True, load_dtd=True, events=["start", "end"]):
            
            if count >= 10:
                break
            
            if oldnode is not None:
                oldnode.clear()
            oldnode = node
            
            if node.tag == "article":
                for child in node:
                    print("{}: {}".format(child.tag, child.text))
                count += 1
            
            # if node.tag == "proceedings":
            #     for child in element:
            #         print("{}: {}".format(child.tag, child.text))
            
      
parseDBLP()      
 

start = timeit.timeit()       
with gzip.open('data/dblp.xml.gz') as f:
            
    oldnode = None
    count = 0

    for (event, node) in ElementTree.iterparse(f, dtd_validation=True, load_dtd=True, events=["start", "end"]):
        
        if oldnode is not None:
            oldnode.clear()
        oldnode = node
        
        count +=1
        
        # if node.tag == "proceedings":
        #     for child in node:
        #             print("{}: {}".format(child.tag, child.text))
        #     count += 1
end = timeit.timeit()
print(end - start)

db_iter = ElementTree.iterparse(source=dblp_path, dtd_validation=False, load_dtd=True)
for _, element in db_iter:
    if element.tag == "proceedings":
        for child in element:
            print("{}: {}".format(child.tag, child.text))
    clear_element(element)

print(etree.tostring(ele[70], pretty_print=True))
for sub in ele[70]:
    print(sub.tag)

import edn_format
edn_format.dumps([{"a": 1}, {"c": 4}, {"b": 3}], keyword_keys=True, indent=True)
edn_format.loads("{:node/v 2 node/u 1}")

