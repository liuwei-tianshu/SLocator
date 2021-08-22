## SLocator: Localizing the Origin of SQL Queries in Database-Backed Web Applications
> **Note:** This is the source code (in directory **Approach**) and evaluation dataset (in directory **Evaluation**) for SLocator

### 1. Approach
The approach contains two parts: 
1. We first use static analysis to infer the database access of each control flow path in the source code. (in directory /Approach/Statically Inferring Database Access/SLocator/)
2. Then, given SQL queries, we use information retrieval techniques to rank the control flow paths that have the highest database access similarity. (file: /Approach/syntactic_and_semantic_matching.py /)

Correspondly, there are two steps to run SLocator:



### 2. Evaluation


