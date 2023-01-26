
""" A Collection of usefull queries on the kuzu db """


import kuzu
import pandas as pd
import numpy as np
import json

pd.options.mode.chained_assignment = None

""" connect to db """
db = kuzu.database(database_path='./kuzudb/db', buffer_pool_size=2147483648)
# db.resize_buffer_manager(8589934592) 4294967296
conn = kuzu.connection(db)

"""config for global filters
if strict_boundary is True all authors must be from the given region 
else at least one author must be from given region
"""
config = {  "from_year": 2005,
            "to_year": 2023,
            "area_id" : "ai", 
            "area_type": "a", 
            "region_id": "wd",
            "country_id": None,
            "strict_boundary": True,
            "institution":False
            }
            

def list_as_string(l):
    """ helper function to convert lists as strings """
    s = '['
    for i in l:
        s += '"{}", '.format(i)
    return s[:-2] + "]"


def get_region_mapping():
    """ get country region mapping """
    result = conn.execute('''  
                    MATCH (c:Country)-[i:InRegion]->(r:Region) 
                    RETURN c.id, c.name, r.id, r.name;
                    ''').getAsDF()     
    result.columns=['country-id', 'country-name','region-id', 'region-name' ]
    return result
# region_mapping = get_region_mapping()
# print(region_mapping.head(),"\n", region_mapping.shape)


def get_area_mapping():
    """ get computer science area and sub-areas """ 
    area_map = conn.execute('''MATCH (p:Proceeding)-[b:BelongsToArea]->(s:SubArea)-[o:SubAreaOf]->(a:Area)
                         RETURN DISTINCT a.id, a.label, s.id, s.label, p.conf;
                    ''').getAsDF()    
    area_map.columns = ["area-id","area-label","sub-area-id","sub-area-label", "conference-id"]
    
    """ add conference names"""
    conferences = conn.execute('''MATCH (c:Conference)
                         RETURN c.id as id, c.title as title;
                    ''').getAsDF()  
    conf_idx = dict(zip(conferences["id"],conferences["title"]))
    area_map["conference-title"] = list(map(lambda x: conf_idx[x], area_map["conference-id"].values))
    
    return area_map    

# area_mapping=get_area_mapping()
# print(area_mapping.head(),"\n", area_mapping.shape)


def get_by_area(area_config):
    """get proceedings and inproceedings filtered by area/subarea
    area_type: a if main area s if sub area
    return_type: p if proceeding area i if inproceeding
    example: 
    area_config = {"area_id" : "ai", 
                "area_type":  "a", 
                "cut_off":None,
                "return_type" :"p"}"""
    from_year = area_config.get("from_year")
    to_year = area_config.get("to_year")
    return_type = area_config.get("return_type","p")
    area_id = area_config.get("area_id")
    area_type =  area_config.get("area_type")
    
    """ generate where clause """
    if area_id or from_year or to_year:
        clauses = []
        if from_year:
            clauses.append("{}.year >= {}".format(return_type,from_year))
        if to_year:
            clauses.append("{}.year < {}".format(return_type,to_year))
        if area_id:
            clauses.append('{}.id = "{}"'.format(area_type,area_id))
        where_clause =  "WHERE " + " AND ".join(clauses)
    else: 
        where_clause = ""
        
    """ get return clause """
    if return_type=="i":
        return_clause = "RETURN i"
    else: 
        return_clause = "RETURN DISTINCT p"
        
    result = conn.execute('''  
                    MATCH (p:Proceeding)-[b:BelongsToArea]->(s:SubArea)-[o:SubAreaOf]->(a:Area),
                    (i:Inproceeding)-[c:Crossref]->(p)
                    {}
                    {};
                    '''.format(where_clause,return_clause)).getAsDF()     
    return result       

# result = get_by_area({  "area_id" : "ai", 
#                         "area_type":  "a", 
#                         "from_year":2010,
#                         "return_type" :"i"})
# print(result.head(),"\n", result.shape)

# result = get_by_area({  "area_id" : "vision", 
#                         "area_type":  "s", 
#                         "cut_off":2010,
#                         "return_type" :"p"})
# print(result.head(),"\n", result.shape)

# result = get_by_area({"return_type" :"i"})
# print(result.head(),"\n", result.shape)



def get_conference_name(proceeding_list):
    """ get the conference name of given proceedings """
    result = conn.execute('''UNWIND {} AS proceedings
                    WITH proceedings
                    MATCH 
                    (p:Proceeding)-[b:BelongsToConf]->(c:Conference)
                    WHERE p.id = proceedings
                    RETURN DISTINCT c.id, c.title;
                    '''.format(list_as_string(proceeding_list))).getAsDF() 
    result.columns=["id", "title"]
    return result

# proceeding_list = ['conf/ijcnn/2011', 'conf/ijcnn/2017', 'conf/ijcnn/2021','conf/mod/2016', 'conf/mod/2021-1']
# get_conference_name(proceeding_list)


def get_conference(conf):
    """ get all in/proceedings of a conference with name """
    result = conn.execute('''  
                    MATCH 
                    (p:Proceeding)-[b:BelongsToConf]->(conf:Conference),
                    (i:Inproceeding)-[c:Crossref]->(p)
                    WHERE conf.id = "{}"
                    RETURN DISTINCT p.id, p.title, p.year, i.id, i.title;
                    '''.format(conf)).getAsDF()   
    return result 
# result = get_conference()   
# print(result.head(),"\n", result.shape)

def get_csauthors(country_id = None, region_id = "wd"):
    """ get authors from csranking with their affiliation filtered on region/country """
     
    if country_id:
        where_clause = 'c.id = "{}"'.format(country_id)
    else:
        where_clause = 'r.id = "{}"'.format(region_id)
    
    result = conn.execute('''  
                MATCH 
                (a:AuthorCS)-[af:AffiliationCS]->
                (i:Institution)-[l:LocatedIn]->
                (c:Country)-[ir:InRegion]->(r:Region)
                WHERE {}
                RETURN a.pid, a.name,i
                '''.format(where_clause)).getAsDF()  
    result.columns = ["pid", "name", "institution", "lat", "lon"]  
    return result
# result = get_csauthors(region_id="dach")
# print(result.head(),"\n", result.shape)

def get_flat_collaboration(ignore_area=False):
    """get collaboration of csranking author with country and area information"""

    """ get all collaborations from csrankings authors """
    collab = conn.execute('''MATCH (a:AuthorCS)-[col:CollaborationCS]->(b:AuthorCS)
                             RETURN a.pid AS a_pid, b.pid AS b_pid, col.record AS rec_id, col.year AS year''').getAsDF() 
    
    """ get authors country mapping """
    author_country = conn.execute(''' MATCH 
                                    (a:AuthorCS)-[af:AffiliationCS]->
                                    (i:Institution)-[l:LocatedIn]->(c:Country)
                                    RETURN a.pid AS a, c.id AS country''').getAsDF() 
    country_mapping = dict(zip(author_country["a"],author_country["country"]))

    """ inproceeding sub-area mapping """
    ip_area = conn.execute( '''MATCH 
                            (i:Inproceeding)-[c:Crossref]->(p:Proceeding)-[ba:BelongsToArea]->(s:SubArea)
                            RETURN i.id AS rec, s.id AS area
                            ''').getAsDF() 
    ip_area_mapping = dict(zip(ip_area["rec"],ip_area["area"]))
    
    if not ignore_area:
        """ only consider records which have an area assignment"""
        has_area = list(map(lambda x: True if ip_area_mapping.get(x) else False, collab["rec_id"]))
        collab = collab[has_area]

    """add country and area data to collab"""
    collab["a_country"]=list(map(lambda x: country_mapping[x], collab["a_pid"]))
    collab["b_country"]=list(map(lambda x: country_mapping[x], collab["b_pid"]))
    if not ignore_area:
        collab["rec_sub_area"]=list(map(lambda x: ip_area_mapping[x], collab["rec_id"]))
        
    """sort authors alphabetically to make sure the collaborations between authors is correctly aggregated"""
    is_ordered = collab["a_pid"]<collab["b_pid"]
    a_pid = list(map(lambda x: collab["a_pid"].iloc[x[0]] if x[1] else collab["b_pid"].iloc[x[0]] ,enumerate(is_ordered)))
    b_pid = list(map(lambda x: collab["b_pid"].iloc[x[0]] if x[1] else collab["a_pid"].iloc[x[0]] ,enumerate(is_ordered)))
    collab["a_pid"] = a_pid
    collab["b_pid"] = b_pid

    return collab

def filter_collab(config = {}):
    """filter flat collaborationn
    config = {  "area_ids" : ["ai","systems"], 
                "sub_area_ids":  ["robotics","bio"], 
                "region_ids":["europe","northamerica"],
                "country_ids":["jp","sg"],
                "strict_boundary":True
                }"""
    
    area_ids = config.get("area_ids")
    sub_area_ids =  config.get("sub_area_ids")
    region_ids = config.get("region_ids",["wd"])
    country_ids =config.get("country_ids")
    strict_boundary = config.get("strict_boundary", True)
    
    """get collaboration"""
    if (area_ids is None) and (sub_area_ids is None):
        ignore_area=True
    else: 
        ignore_area=False
    collab = get_flat_collaboration(ignore_area=ignore_area)
    
    """region filters"""
    region_mapping = get_region_mapping()
    country_filter = list(map(lambda x: x in country_ids , region_mapping["country-id"]))
    region_filter = list(map(lambda x: x in region_ids , region_mapping["region-id"]))
    countries = set(region_mapping[np.logical_or(country_filter,region_filter)]["country-id"])
    
    a_in_region = list(map(lambda x: x in countries , collab["a_country"]))
    b_in_region = list(map(lambda x: x in countries , collab["b_country"]))
    
    """ exclude authors based on regional constraints """
    if strict_boundary:
        """ both authors must be from given region """
        collab_country_filter = np.logical_and(a_in_region,b_in_region)
    else: 
        """ at least one author must be from given region """
        collab_country_filter = np.logical_or(a_in_region,b_in_region)

    if (area_ids is None) and (sub_area_ids is None):
        """filter collaborations based on region"""
        collab_filtered = collab[collab_country_filter][["a_pid","b_pid","rec_id","year"]]
        collab_filtered.columns = ["a","b","rec","year"]
    else:
        """ area filters """
        area_mapping = get_area_mapping()
        area_filter = list(map(lambda x: x in area_ids , area_mapping["area-id"]))
        sub_area_filter = list(map(lambda x: x in sub_area_ids , area_mapping["sub-area-id"]))
        areas = set(area_mapping[np.logical_or(area_filter,sub_area_filter)]["sub-area-id"])
        collab_area_filter = list(map(lambda x: x in areas , collab["rec_sub_area"]))
    
        """filter collaborations based on region and areas"""
        collab_filtered = collab[np.logical_and(collab_country_filter,collab_area_filter)][["a_pid","b_pid","rec_id","year"]]
        collab_filtered.columns = ["a","b","rec","year"]
    
    collab_filtered = collab_filtered.astype({"year":'int'})
    return collab_filtered
# collab = get_flat_collaboration(ignore_area=False)
# config = {  "area_ids" : ["ai","systems"], 
#             "sub_area_ids":  ["robotics","bio"], 
#             "region_ids":["europe","northamerica"],
#             "country_ids":["jp","sg"],
#             "strict_boundary":True
#             }
# collab_filtered = filter_collab(config)


def get_collaboration(collab_config={}):
    """get collaboration of author/institution filtered on region and area 
    collab_config = {"area_id" : "ai", 
                "area_type":  "a", 
                "region_id":"dach",
                "country_id":None,
                "strict_boundary":True
                }"""
    
    area_id = collab_config.get("area_id")
    area_type =  collab_config.get("area_type")
    region_id = collab_config.get("region_id","wd")
    country_id =collab_config.get("country_id")
    strict_boundary = collab_config.get("strict_boundary", True)

    """ get authors from given region """
    csauthors_region = get_csauthors(country_id=country_id, region_id=region_id)
    csauthors_region_idx = dict(zip(csauthors_region["pid"],np.repeat(True, csauthors_region.shape[0])))

    """ get all inproceedings from cs rankings from an area """
    inproceedings_of_area = get_by_area({  "area_id" : area_id, 
                            "area_type": area_type, 
                            "return_type" :"i"})


    inproceedings = inproceedings_of_area["i.id"].values
    inproceedings_idx = dict(zip(inproceedings,np.repeat(True, len(inproceedings))))

    """ get all collaborations from csrankings authors """
    collab = conn.execute('''MATCH (a:AuthorCS)-[col:CollaborationCS]->(b:AuthorCS)
                             RETURN a.pid AS a, b.pid AS b, col.record AS rec, col.year AS year''').getAsDF() 

    """ filter collaboration by area """
    collabs_area = collab[list(map(lambda x: inproceedings_idx.get(x, False), collab["rec"]))]


    def order_tuple(row):
        """ sort authors alphabetically to make sure the collaborations between authors is correctly aggregated """
        if row["a"]>row["b"]:
            row_tuple = (row["b"],row["a"],row["rec"], row["year"])
        else:
            row_tuple = (row["a"],row["b"],row["rec"], row["year"])
        return row_tuple
    
    """ get ordered collaboration tuples """
    collabs_tuples = np.array(list(map(lambda x: order_tuple(x[1]),collabs_area.iterrows())))

    """ check if author is part of given region """
    x_in_region = list(map(lambda x: csauthors_region_idx.get(x[0], False), collabs_tuples))
    y_in_region = list(map(lambda x: csauthors_region_idx.get(x[1], False), collabs_tuples))

    """ exclude authors based on regional constraints """
    if strict_boundary:
        """ both authors must be from given region """
        region_filter = np.logical_and(x_in_region,y_in_region)
    else: 
        """ at least one author must be from given region """
        region_filter = np.logical_or(x_in_region,y_in_region)

    """ collaborations filtered on regional constraint """
    collabs_tuples_filtered = collabs_tuples[region_filter]
    collabs_sorted = pd.DataFrame(collabs_tuples_filtered, columns = ["a", "b", "rec", "year"])
    collabs_sorted = collabs_sorted.astype({"year":'int'})
    return collabs_sorted

# collabs = get_collaboration(
#             {"area_id" : "ai", 
#             "area_type":  "a", 
#             "region_id":"dach",
#             "strict_boundary":True
#             })
# collabs_sorted = get_collaboration()


def weighted_collab(collabs,from_year=None, to_year = None, institution = False):
    """generate a weighted collaboration based on author or instituion from a set of collaborations
    example collabs = get_collaboration()"""
    
    """ get all csauthors and create a mapping pid->institution """
    csauthors_all = get_csauthors()
    author_inst_map = dict(zip(csauthors_all["pid"],csauthors_all["institution"]))

    if institution:
        collabs["a"] = list(map(lambda x: author_inst_map[x], collabs["a"].values))
        collabs["b"] = list(map(lambda x: author_inst_map[x], collabs["b"].values))
        """make sure to weight the collaboration between institutions  
        only once per inproceeding even if multiple auhtors collaborated"""
        collabs = collabs.drop_duplicates()
    """ only consider collabs later or equal to the cut_off year """
    if from_year:
        collabs = collabs[collabs["year"]>=from_year]
    if to_year:
        collabs = collabs[collabs["year"]<to_year]
    
    """ count collaborations between authors or institutions """
    weighted = collabs.groupby(["a", "b"])["rec"].count().sort_values(ascending=False).reset_index() 
    weighted.columns = ["a", "b", "weight"]  
    return weighted
# collabs = get_collaboration(
#             {"area_id" : "ai", 
#             "area_type":  "a", 
#             "region_id":"dach",
#             "strict_boundary":True
#             })
# weighted_collab(collabs, cut_off = 2010, institution=True)


def get_weighted_collab(config={}):
    """ wrapper for the combination of get_collaboration() and weighted_collab() """
    
    institution = config.get("institution")
    from_year = config.get("from_year")
    to_year = config.get("to_year")
    
    collabs = get_collaboration(config)
    weighted = weighted_collab(collabs, from_year = from_year,to_year=to_year, institution=institution)
    return weighted
# get_weighted_collab({   "from_year": 2010,
#                         "area_id" : "ai", 
#                         "area_type":  "a", 
#                         "region_id":"dach",
#                         "strict_boundary":True,
#                         "institution":False
#                         })
# get_weighted_collab({"from_year": 2010, "institution":True})
# 


def get_collab_pid(pid_x, pid_y, config={}):
    """ get all the collaborations between two authors with the constraints given in the config """
    
    """ get constraints from config """
    from_year = config.get("from_year")
    to_year = config.get("to_year")
    area_id = config.get("area_id")
    area_type = config.get("area_type","a")
    
    """ generate where clause """
    if area_id or from_year or to_year:
        clauses = []
        if from_year:
            clauses.append("i.year >= {}".format(from_year))
        if to_year:
            clauses.append("i.year < {}".format(to_year))
        if area_id:
            clauses.append('{}.id = "{}"'.format(area_type,area_id))
        where_clause =  "WHERE i.id = record AND " + " AND ".join(clauses)
    else: 
        where_clause = "WHERE i.id = record"
    
    """ get all the collaboration inproceedings, proceedings and conferences between the two pids  """
    result = conn.execute('''MATCH (x:AuthorCS)-[col:CollaborationCS]->(y:AuthorCS)
                WHERE 
                (x.pid = "{0}" AND y.pid = "{1}") 
                OR 
                (y.pid = "{0}" AND x.pid = "{1}") 
                WITH x.pid as x, y.pid as y, col.record as record
                MATCH (p:Proceeding)-[ba:BelongsToArea]->(s:SubArea)-[o:SubAreaOf]->(a:Area),
                (i:Inproceeding)-[c:Crossref]->(p)-[bc:BelongsToConf]->(conf:Conference)
                {2}
                RETURN i.year, i.id, i.title, p.id, p.title, conf.id, conf.title
                '''.format(pid_x,pid_y,where_clause)).getAsDF() 
    result.columns = ["year", "inproceeding_id", "inproceeding_title", "proceeding_id", 
                      "proceeding_title", "conference_id", "conference_title"]
    return result
# get_collab_pid("24/8616","61/5017", {"from_year": 2010,"area_id" : "ai", "area_type":  "a"})


def get_collab_institution(inst_x, inst_y, config={}):
    """ get all the collaborations between two institutions with the constraints given in the config """
    
    """ get constraints from config """
    from_year = config.get("from_year")
    to_year = config.get("to_year")
    area_id = config.get("area_id")
    area_type = config.get("area_type","a")
    
    """ generate where clause """
    if from_year or to_year:
        clauses = []
        if from_year:
            clauses.append("col.year >= {}".format(from_year))
        if to_year:
            clauses.append("col.year < {}".format(to_year))

        where_clause_collab =  " AND " + " AND ".join(clauses)
    else: 
        where_clause_collab = ""
    
    """ get all the collaboration inproceedings, proceedings and conferences between the two pids  """
    collab_inst = conn.execute('''MATCH (x:AuthorCS)-[col:CollaborationCS]->(y:AuthorCS)
                WHERE x.affiliation = "{0}" AND y.affiliation = "{1}"{2}
                RETURN DISTINCT col.record AS rec
                '''.format(inst_x,inst_y,where_clause_collab)).getAsDF() 
    collab_inst_rec = collab_inst["rec"].values
    collab_inst_idx = dict(zip(collab_inst_rec,np.repeat(True, len(collab_inst_rec))))
    
    if area_id or from_year or to_year:
        clauses = []
        if from_year:
            clauses.append("i.year >= {}".format(from_year))
        if to_year:
            clauses.append("i.year < {}".format(to_year))
        if area_id:
            clauses.append('{}.id = "{}"'.format(area_type,area_id))
        where_clause =  "WHERE " + " AND ".join(clauses)
    else: 
        where_clause = ""
    
    """ filter inproceedings by area and year constraint """
    inproceedings_area = conn.execute('''
                        MATCH (p:Proceeding)-[ba:BelongsToArea]->(s:SubArea)-[o:SubAreaOf]->(a:Area),
                        (i:Inproceeding)-[c:Crossref]->(p)-[bc:BelongsToConf]->(conf:Conference)
                        {}
                        RETURN i.year, i.id, i.title, p.id, p.title, conf.id, conf.title
                        '''.format(where_clause)).getAsDF() 
    inproceedings_area.columns = ["year", "inproceeding_id", "inproceeding_title", "proceeding_id", 
                      "proceeding_title", "conference_id", "conference_title"]
               
    """ filter by the constraint on the instittution """
    inproceedings_area= inproceedings_area[list(map(
                                            lambda x: collab_inst_idx.get(x,False),
                                            inproceedings_area["inproceeding_id"]))]
    
    """ remove dublicates from collaborations within the institution """
    inproceedings_area = inproceedings_area.drop_duplicates()

    return inproceedings_area
# get_collab_institution("Tsinghua University","Tsinghua University",{"from_year": 2010})
# get_weighted_collab({"from_year": 2010, "institution":True})

# t = result["i.name"].str.encode(encoding = 'utf-8').str.decode(encoding = 'utf-8')
# t = t.str.decode(encoding = 'utf-8')
