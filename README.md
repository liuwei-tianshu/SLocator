# SLocator: Localizing the Origin of SQL Queries in Database-Backed Web Applications
> **Note:** This is the source code (**SLocator**), demo (**Demo**), experiment (**Experiment**) and evaluation dataset (**Dataset**) for SLocator

## 1. SLocator
This directory constians **Statically Inferring Database Access**, the static analysis to infer the database access of each control flow path in the source code.

We use [Crystal](https://code.google.com/archive/p/crystalsaf/), a Java static analysis framework that is built on top of Eclipse JDT, to analyze the source code and extract the CFG.

## 2. Demo
This directory contains **Locating the Paths that Generate a Given SQL Query**. Given SQL queries, we use information retrieval techniques to rank the control flow paths that have the highest database access similarity.

We give a demo to show SLocator works on PetClinic:
```
Options:
  --type <style>     Set types of DBMS logs: individual, session
  --SQL <SQL query>  Set the SQL query to be locate, several SQL queries for SQL session should be seperated with comma
```



## 3. Experiment
- Instrument: we use [AspectJ](https://www.eclipse.org/aspectj/) to instrument the applications when executing the workloads to get the dynamic execution paths.

- Workload: equivalent test cases of the workloads (only for demonstration). 


## 4. Dataset
This directory contains the **evaluation dataset on studied applications**. Below, we explain the format of the dataset.


#### 1 given SQL query (or given SQL session query) and pre-processed SQL query
```
======================================= 5 given SQL query ===========================================
Given individual SQL query: select owner0_.id as id1_0_1_, owner0_.first_name as first_na2_0_1_, ...
Pre-processed SQL query : select from owners owner left outer join pets where owner.id=? 
```

#### 2 instrument dynamic execution path using AspectJ
The dynamic execution path is surrounded by "before rest" and "after rest".
It includes called methods and ORM generated SQL queries. For example, ORM generaed one SQL query in method jpaownerrepositoryimpl.findbyid(int).
```
+++++++++++++++++++++++ 1 instrument dynamic execution path using AspectJ +++++++++++++++++++++++++++++
before rest| modelandview org.springframework.samples.petclinic.web.ownercontroller.showowner(int) 
called| modelandview org.springframework.samples.petclinic.web.ownercontroller.showowner(int) 
called| owner com.sun.proxy..proxy49.findownerbyid(int) 
called| owner org.springframework.samples.petclinic.service.clinicserviceimpl.findownerbyid(int) 
called| owner com.sun.proxy..proxy47.findbyid(int) 
called| owner org.springframework.samples.petclinic.repository.jpa.jpaownerrepositoryimpl.findbyid(int) 
sql|select owner0_.id as id1_0_0_, pets1_.id as id1_1_1_, owner0_.first_name as first_na2_0_0_, ...
after rest| modelandview org.springframework.samples.petclinic.web.ownercontroller.showowner(int) 
```

#### 3 ranked control flow path according to similarity score
We rank control flow path according to similarity score. 
Then we compare this control flow with the ground truth (i.e., dynamic execution path) to see if it is path matching or request matching.
For example, this control flow path and the dynamic execution path has same HTTP request handling method ownercontroller.showowner(int). As a result, this control flow path is request matching.
```
---------Top1 ranked control flow path according to similarity score (Only top 10 are presented here) -----------
request:org.springframework.samples.petclinic.web.ownercontroller.showowner(int)
method:org.springframework.samples.petclinic.service.clinicserviceimpl.findownerbyid(int)
method:org.springframework.samples.petclinic.repository.jpa.jpaownerrepositoryimpl.findbyid(int)
[select owner from owner owner left join fetch owner.pets where owner.id =:id]
[select  from types where id=?]
[select visit_date, description, null from visits where id=?]

Syntactic Similarity:0.5464303776063751
Semantic Similarity:0.3333333333333333
Combining Similarity Score: (Syntactic Similarity + Semantic Similarity):0.8797637109397085
Path matching:True
Request matching:True
```

#### 4 metrics for this SQL query
We calculate metrics on path matching and request matching for this given SQL query.

We calculate final metrics on all given SQL queries.
```
+++++++++++++++++++++++++ Path matching metrics +++++++++++++++++++++++++++
path_matching_boolean:[True, True, True, False, False, True, False, True, False, False]
path_matching_number_k:[1, 2, 3, 3, 3, 4, 4, 5, 5, 5]
path_precision_k:[1.0, 1.0, 1.0, 0.75, 0.6, 0.6666666666666666, 0.5714285714285714, 0.625, 0.5555555555555556, 0.5]
path_ap:0.7222222222222222
+++++++++++++++++++++++++ Request matching metrics +++++++++++++++++++++++++++
request_matching_boolean:[True, True, True, False, False, True, False, True, False, False]
request_matching_number_k:[1, 2, 3, 3, 3, 4, 4, 5, 5, 5]
request_precision_k:[1.0, 1.0, 1.0, 0.75, 0.6, 0.6666666666666666, 0.5714285714285714, 0.625, 0.5555555555555556, 0.5]
request_ap:0.7222222222222222
```


