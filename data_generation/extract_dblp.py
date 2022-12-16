from datetime import datetime
import re
import os
import sys
import pandas as pd
import json
from lxml import etree

dblp_path = 'data/dblp.xml'

def clear_element(element):
    """Free up memory for temporary element tree after processing the element"""
    element.clear()
    while element.getprevious() is not None:
        del element.getparent()[0]

count = 0
db_iter = etree.iterparse(source=dblp_path, dtd_validation=True, load_dtd=True)
for _, element in db_iter:
    if count > 100:
        break
    if element.tag == "inproceedings":
        count += 1
        for sub in element:
            print("{}: {}".format(sub.tag,sub.text))
    else:
        clear_element(element)
        
    
print(etree.tostring(ele[70], pretty_print=True))
for sub in ele[70]:
    print(sub.tag)

import edn_format
edn_format.dumps([{"a": 1}, {"c": 4}, {"b": 3}], keyword_keys=True, indent=True)
edn_format.loads("{:node/v 2 node/u 1}")

