# SLocator: Localizing the Origin of SQL Queries in Database-Backed Web Applications
> **Note:** This is the source code (**SLocator**), demo (**Demo**), experiment (**Experiment**) and evaluation dataset (**Dataset**) for SLocator

## 1. SLocator
This directory constians **Statically Inferring Database Access**, the static analysis to infer the database access of each control flow path in the source code.

We use [Crystal](https://code.google.com/archive/p/crystalsaf/), a Java static analysis framework that is built on top of Eclipse JDT, to analyze the source code and extract the CFG.

## 2. Demo
This directory contains **Locating the Paths that Generate a Given SQL Query**. Given SQL queries, we use information retrieval techniques to rank the control flow paths that have the highest database access similarity.

We give a demo to show SLocator works on PetClinic.

#### 2.1 Requirement
- Python 3.7
- Dependency: sklearn nltk sqlparse

#### 2.2 Install and Running
Download whole directory **Demo**.

Options:
```
  -s, --sql <SQL query>  Set the SQL query to be located. 
                         1 SQL session log (should be separated with |)
                         2 individual query log
```

**Command for SQL session log**: python SLocator.py -s "select distinct owner0_.id as id1_0_0_, pets1_.id as id1_1_1_, owner0_.first_name as first_na2_0_0_, owner0_.last_name as last_nam3_0_0_, owner0_.address as address4_0_0_, owner0_.city as city5_0_0_, owner0_.telephone as telephon6_0_0_, pets1_.name as name2_1_1_, pets1_.birth_date as birth_da3_1_1_, pets1_.owner_id as owner_id4_1_1_, pets1_.type_id as type_id5_1_1_, pets1_.owner_id as owner_id4_0_0__, pets1_.id as id1_1_0__ from owners owner0_ left outer join pets pets1_ on owner0_.id=pets1_.owner_id where owner0_.last_name like 'xxxx'|select pettype0_.id as id1_3_0_, pettype0_.name as name2_3_0_ from types pettype0_ where pettype0_.id=1|select visits0_.pet_id as pet_id4_1_0_, visits0_.id as id1_6_0_, visits0_.id as id1_6_1_, visits0_.visit_date as visit_da2_6_1_, visits0_.description as descript3_6_1_, visits0_.pet_id as pet_id4_6_1_ from visits visits0_ where visits0_.pet_id=1"

The output of SLocator is as following (ranked top 5 paths):
```
Given SQL queries:
select distinct owner0_.id as id1_0_0_, pets1_.id as id1_1_1_, owner0_.first_name as first_na2_0_0_, owner0_.last_name as last_nam3_0_0_, owner0_.address as address4_0_0_, owner0_.city as city5_0_0_, owner0_.telephone as telephon6_0_0_, pets1_.name as name2_1_1_, pets1_.birth_date as birth_da3_1_1_, pets1_.owner_id as owner_id4_1_1_, pets1_.type_id as type_id5_1_1_, pets1_.owner_id as owner_id4_0_0__, pets1_.id as id1_1_0__ from owners owner0_ left outer join pets pets1_ on owner0_.id=pets1_.owner_id where owner0_.last_name like 'xxxx'
select pettype0_.id as id1_3_0_, pettype0_.name as name2_3_0_ from types pettype0_ where pettype0_.id=1
select visits0_.pet_id as pet_id4_1_0_, visits0_.id as id1_6_0_, visits0_.id as id1_6_1_, visits0_.visit_date as visit_da2_6_1_, visits0_.description as descript3_6_1_, visits0_.pet_id as pet_id4_6_1_ from visits visits0_ where visits0_.pet_id=1

Pre-processed SQL queries:
select distinct from owner left outer join pet where owner.last_name like ?
select from type where pettype.id=?
select from visit where visits.pet_id=?

-------Top1 ranked control flow path according to similarity score (Only top 5 are presented here) ------
request:org.springframework.samples.petclinic.web.ownercontroller.processfindform(owner,bindingresult,map)
method:org.springframework.samples.petclinic.service.clinicserviceimpl.findownerbylastname(string)
method:org.springframework.samples.petclinic.repository.jpa.jpaownerrepositoryimpl.findbylastname(string)
[select distinct owner from owner owner left join fetch owner.pets where owner.lastname like :lastname]
[select  from types where id=?]
[select visit_date, description, null from visits where id=?]
Syntactic Similarity:0.6198747639278099
Semantic Similarity:1.0
Combining Similarity Score (Syntactic Similarity + Semantic Similarity): 1.61987476392781

--------Top2 ranked control flow path according to similarity score (Only top 5 are presented here) -------
...
```

**Command for individual query log**: python SLocator.py -s "select owner0_.id as id1_0_1_, owner0_.first_name as first_na2_0_1_, owner0_.last_name as last_nam3_0_1_, owner0_.address as address4_0_1_, owner0_.city as city5_0_1_, owner0_.telephone as telephon6_0_1_, pets1_.owner_id as owner_id4_0_3_, pets1_.id as id1_1_3_, pets1_.id as id1_1_0_, pets1_.name as name2_1_0_, pets1_.birth_date as birth_da3_1_0_, pets1_.owner_id as owner_id4_1_0_, pets1_.type_id as type_id5_1_0_ from owners owner0_ left outer join pets pets1_ on owner0_.id=pets1_.owner_id where owner0_.id=1" 

The output of SLocator is as following (ranked top 5 paths):
```
Given SQL queries:
select owner0_.id as id1_0_1_, owner0_.first_name as first_na2_0_1_, owner0_.last_name as last_nam3_0_1_, owner0_.address as address4_0_1_, owner0_.city as city5_0_1_, owner0_.telephone as telephon6_0_1_, pets1_.owner_id as owner_id4_0_3_, pets1_.id as id1_1_3_, pets1_.id as id1_1_0_, pets1_.name as name2_1_0_, pets1_.birth_date as birth_da3_1_0_, pets1_.owner_id as owner_id4_1_0_, pets1_.type_id as type_id5_1_0_ from owners owner0_ left outer join pets pets1_ on owner0_.id=pets1_.owner_id where owner0_.id=1

Pre-processed SQL queries:
select from owner left outer join pet where owner.id=? 

------Top1 ranked control flow path according to similarity score (Only top 5 are presented here) -----------
request:org.springframework.samples.petclinic.web.ownercontroller.showowner(int)
method:org.springframework.samples.petclinic.service.clinicserviceimpl.findownerbyid(int)
method:org.springframework.samples.petclinic.repository.jpa.jpaownerrepositoryimpl.findbyid(int)
[select owner from owner owner left join fetch owner.pets where owner.id =:id]
[select  from types where id=?]
[select visit_date, description, null from visits where id=?]
Syntactic Similarity:0.7683219433653732
Semantic Similarity:0.3333333333333333
Combining Similarity Score (Syntactic Similarity + Semantic Similarity): 1.1016552766987064

-------Top2 ranked control flow path according to similarity score (Only top 5 are presented here) ---------
...
```


## 3. Experiment
- Instrument: we use [AspectJ](https://www.eclipse.org/aspectj/) to instrument the applications when executing the workloads to get the dynamic execution paths.

- Workload: equivalent test cases of the workloads (only for demonstration). 


## 4. Dataset
This directory contains the **evaluation dataset on studied applications**. Below, we explain the format of the dataset.


#### 1 given SQL query (or given SQL session query) and pre-processed SQL query
The dynamic value (of owner_id) has been removed to delete duplicate given SQL query.
```
======================================= 5 given SQL query ===========================================
Given individual SQL query: select ... from owners owner0_ left outer join pets pets1_ ... where owner0_.id=?
Pre-processed SQL query: select from owners owner left outer join pets where owner.id=?
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

For each given SQL query, Only top 5 are presented here.
```
---------Top1 ranked control flow path according to similarity score (Only top 5 are presented here) -----------
request:org.springframework.samples.petclinic.web.ownercontroller.showowner(int)
method:org.springframework.samples.petclinic.service.clinicserviceimpl.findownerbyid(int)
method:org.springframework.samples.petclinic.repository.jpa.jpaownerrepositoryimpl.findbyid(int)
[select owner from owner owner left join fetch owner.pets where owner.id =:id]
[select  from types where id=?]
[select visit_date, description, null from visits where id=?]

Syntactic Similarity:0.7683219433653732
Semantic Similarity:0.3333333333333333
Combining Similarity Score (Syntactic Similarity + Semantic Similarity): 1.1016552766987064
Path matching:True
Request matching:True

---------Top2 ranked control flow path according to similarity score (Only top 5 are presented here) -----------
...
```

#### 4 metrics for this SQL query
We calculate metrics on path matching and request matching for this given SQL query.

We calculate FINAL metrics based on all given SQL queries.
```
+++++++++++++++++++++++++ Path matching metrics +++++++++++++++++++++++++++
path_matching_boolean:[True, True, True, False, False]
path_matching_number_k:[1, 2, 3, 3, 3]
path_precision_k:[1.0, 1.0, 1.0, 0.75, 0.6]
path_ap:0.7222222222222222
+++++++++++++++++++++++++ Request matching metrics +++++++++++++++++++++++++++
request_matching_boolean:[True, True, True, False, False]
request_matching_number_k:[1, 2, 3, 3, 3]
request_precision_k:[1.0, 1.0, 1.0, 0.75, 0.6]
request_ap:0.7222222222222222
```


