import sys
import getopt
from syntactic_and_semantic_matching import *
from pre_process import *


def main(argv):
    """
    We give a demo to show SLocator works on PetClinic:
    :param argv: 
      Options:
      -s, --sql <SQL query>  Set the given SQL query to be located.
        1 SQL session log (should be separated with |)
        2 individual query log
    :return:
    """
    try:
        opts, args = getopt.getopt(argv, "s:", ["sql="])
    except getopt.GetoptError:
        print('SLocator.py -s <sql>')
        sys.exit()

    for opt, arg in opts:
        if opt not in ("-s", "--sql"):
            print('SLocator.py -s <sql>')
            sys.exit()
        else:
            """
            Pre-processing SQL Queries
                1 SQL session log (seperated with |)
                2 individual query log
            """
            given_sql_query = arg
            given_sql_query_list = list()
            if '|' in given_sql_query:
                sql = given_sql_query.strip().rstrip('\r').rstrip('\n')
                sql_str_array = sql.split('|')
                for single_sql in sql_str_array:
                    given_sql_query_list.append(single_sql)
            else:
                given_sql_query_list.append(given_sql_query.strip().rstrip('\r').rstrip('\n'))

            pre_processed_given_sql_query_list = list()
            for original_sql in given_sql_query_list:
                pre_processed_sql = pre_process(original_sql)
                pre_processed_given_sql_query_list.append(pre_processed_sql)

            """
            Applying Information Retrieval for Syntactic and Semantic Matching 
            """
            print('Given SQL queries:')
            for original_sql in given_sql_query_list:
                print(original_sql)

            print('\nPre-processed SQL queries:')
            for pre_processed_sql in pre_processed_given_sql_query_list:
                print(pre_processed_sql)
            print('\n')

            cfp_file = "PetClinic_inferred control flow paths with database access.txt"
            locate_control_flow_paths_for_given_sql(pre_processed_given_sql_query_list, cfp_file)


def locate_control_flow_paths_for_given_sql(pre_processed_given_sql_query_list, cfp_file):
    """
    locate control flow paths for given SQL query
    :param pre_processed_given_sql_query_list:
    :param cfp_file: control flow path file to be loaded
    :return:
    """
    control_flow_path_list = load_control_flow_path(cfp_file)
    ranked_control_flow_paths = compute_similarity_score_and_rank_paths(pre_processed_given_sql_query_list, control_flow_path_list)
    for index_inner, path in enumerate(ranked_control_flow_paths):
        if index_inner >= 5:
            break
        print('-------------------------Top' + str(index_inner+1) + ' ranked control flow path according to similarity score (Only top 5 are presented here) -------------------------------------')
        for item in path[0]:
            print(item)
        # print('\n')
        print('Syntactic Similarity:' + str(path[2]))
        print('Semantic Similarity:' + str(path[3]))
        print('Combining Similarity Score (Syntactic Similarity + Semantic Similarity): ' + str(path[4]) + '\n')


def load_control_flow_path(file_path):
    """
    load control flow path
    :param file_path:
    :return: the list of control flow path
        format: List<[List path_list, List method_list, String sql, List sql_list]>
    """
    control_flow_path_list = list()
    with open(file_path, 'r') as reader:
        line = reader.readline().strip().rstrip('\r').rstrip('\n').lower()
        while line:
            sql = ""
            path_list = list()
            method_list = list()
            sql_list = list()
            while True:
                if line.startswith('#####') or line.startswith('------') or (not line):
                    line = reader.readline().strip().rstrip('\r').rstrip('\n').lower()
                    break
                else:
                    line = line.strip().rstrip('\r').rstrip('\n')
                    line = format_parameter_of_collection(line)
                    path_list.append(line)
                    if line.startswith('['):
                        sql_str = line.strip().rstrip('\r').rstrip('\n').strip('[').rstrip(']').lower()
                        sql += (' ' + sql_str)

                        if sql_str is not None and sql_str != '':
                            sql_list.append(sql_str)

                    else:
                        method_list.append(line.strip().rstrip('\r').rstrip('\n'))
                    line = reader.readline().strip().rstrip('\r').rstrip('\n').lower()

            if path_list and [path_list, method_list, sql, sql_list] not in control_flow_path_list:
                control_flow_path_list.append([path_list, method_list, sql, sql_list])
    reader.close()
    return control_flow_path_list


def format_parameter_of_collection(method):
    """
    before: org.springframework.samples.petclinic.web.ownercontroller.processfindform(owner,bindingresult,map<string,object>)
    after: org.springframework.samples.petclinic.web.ownercontroller.processfindform(owner,bindingresult,map)
    :return:
    """
    sql_pattern = re.compile(r"<[\w|,]*>")
    return sql_pattern.sub(r"", method)


if __name__ == '__main__':
    main(sys.argv[1:])
