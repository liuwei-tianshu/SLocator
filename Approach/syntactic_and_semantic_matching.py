from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import re
import nltk


def locating_the_paths_that_generate_a_given_sql_query(given_sql_queries, control_flow_paths):
    """
    1 compute similarity score between given sql query and each control flow path
    2 rank control_flow_paths_with_similarity_score according to similarity score
    :param given_sql_queries: Given SQL query (an individual SQL query or SQL session queries)
    :param control_flow_paths: Control flow paths with database access
            List<(List paths, String sql_query, List database_access)>
            one example of control_flow_path:
                (paths)
                request:org.springframework.samples.petclinic.web.OwnerController.showOwner(int)
                method:org.springframework.samples.petclinic.service.ClinicService.findOwnerById(int)
                method:org.springframework.samples.petclinic.service.ClinicServiceImpl.findOwnerById(int)
                method:org.springframework.samples.petclinic.repository.OwnerRepository.findById(int)
                method:org.springframework.samples.petclinic.repository.jpa.JpaOwnerRepositoryImpl.findById(int)
                [select owner from owner owner left join fetch owner.pets where owner.id =:id]
                [select  from types where id=?]
                [select visit_date, description, null from visits where id=?]

                (sql_query)
                str:  select owner from owner owner left join fetch owner.pets where owner.id =:id select  from types where id=? select visit_date, description, null from visits where id=?

                (database_access)
                [select owner from owner owner left join fetch owner.pets where owner.id =:id]
                [select  from types where id=?]
                [select visit_date, description, null from visits where id=?]
    :return: Ranked control flow paths according to the similarity score between SQL query and the database access of control path
                List<[List paths, String sql_query, Number syntactic_similarity, Number semantic_similarity, Number Similarity Score]>
    """
    # Computing Syntactic Similarity (between given SQL query and each control flow path's database access)
    docs = list()
    for control_flow_path in control_flow_paths:
        docs.append(control_flow_path[1])
    sql_query_str = ""  # take the list of
    for sql_query in given_sql_queries:
        sql_query_str += (' ' + sql_query)
    sim_syn_list = computing_syntactic_similarity(sql_query_str, docs)

    # Computing Semantic Similarity (between given SQL query and each control flow path's database access)
    control_flow_paths_sql_query_list = list()
    for control_flow_path in control_flow_paths:
        control_flow_paths_sql_query_list.append(control_flow_path[2])
    sim_sem_list = computing_semantic_similarity(given_sql_queries, control_flow_paths_sql_query_list)

    control_flow_paths_with_similarity_score = list()
    for index, control_flow_path in enumerate(control_flow_paths):
        # 0     List paths
        # 1     String sql_query
        # 2     Syntactic Similarity
        # 3     Semantic Similarity
        # 4     Combining Similarity Scores: Syntactic Similarity + Semantic Similarity
        control_flow_paths_with_similarity_score.append([control_flow_path[0], control_flow_path[1], sim_syn_list[index], sim_sem_list[index], sim_syn_list[index] + sim_sem_list[index]])

    # Deriving Path Ranking based on Combining Similarity Scores
    ranked_control_flow_paths = sorted(control_flow_paths_with_similarity_score, key=lambda item: item[4], reverse=True)
    return ranked_control_flow_paths


def computing_syntactic_similarity(sql_query, docs):
    """
    Computing Syntactic Similarity
    :param sql_query: Given SQL query
    :param docs: list of string
    :return: list of syntactic similarity
    """
    # 1 calculate tfidf matrix
    documents = list()
    documents.append(sql_query)
    for doc in docs:
        documents.append(doc)
    tfidf_vectorizer = TfidfVectorizer()
    tfidf_matrix = tfidf_vectorizer.fit_transform(documents)

    # 2 calculate cosine_similarity
    similarity = cosine_similarity(tfidf_matrix[0:1], tfidf_matrix).flatten()[1:]
    return similarity


def computing_semantic_similarity(given_sql_queries, control_flow_paths_sql_query_list):
    """
    Computing Semantic Similarity by operator and table name
    :param given_sql_queries: Given SQL query
    :param control_flow_paths_sql_query_list: control flow paths' sql query list
            list[list[database_access_sql_query]]
    :return: list of semantic similarity
    """
    similarity_list = list()

    features_set_q = set()
    for sql in given_sql_queries:
        sql = sql.lower()
        features_set_q.add(get_operator_table_name(sql))

    for control_flow_path_sql_query_list in control_flow_paths_sql_query_list:
        features_set_p = set()
        for sql in control_flow_path_sql_query_list:
            sql = sql.lower()
            features_set_p.add(get_operator_table_name(sql))

        number_intersection = len(features_set_q.intersection(features_set_p))
        number_union = len(features_set_q.union(features_set_p))

        similarity_list.append(number_intersection/number_union)
    return similarity_list


def get_operator_table_name(sql_str):
    operator = sql_str.split(' ')[0].strip()
    assert operator in ['select', 'insert', 'delete', 'update']

    if operator == 'select' or operator == 'delete':
        tables = re.findall('(?<=from\s)\w+', sql_str)
    elif operator == 'insert':
        tables = re.findall('(?<=into\s)\w+', sql_str)
    elif operator == 'update':
        tables = re.findall('(?<=update\s)\w+', sql_str)
    else:
        print(sql_str)
        return None

    assert tables is not None
    assert len(tables) >= 1

    result_table = tables[0]
    # if result_table[-1] == 's':
    #     result_table = result_table[0:-1]
    lemmatizer = nltk.WordNetLemmatizer()
    result_table = lemmatizer.lemmatize(result_table)
    return operator, result_table
