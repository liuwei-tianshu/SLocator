import nltk
import sqlparse
import re
from sqlparse.sql import Identifier, Where
from sqlparse.tokens import Text


def pre_process(sql):
    sql = sql.strip().rstrip('\r').rstrip('\n')
    sql = get_abstract_sql(sql)
    sql = replace_string_value_sql(sql)
    sql = replace_int_value_sql(sql)
    sql = sql.replace('0_', '').replace('1_', '').replace('2_', '').replace('3_', '')
    return sql


def replace_string_value_sql(sql):
    """
    # before: insert into pets (name, birth_date, owner_id, type_id) values ('pet3', '2019-12-24 00:00:00', 11, 6)
    # after:  insert into pets (name, birth_date, owner_id, type_id) values (?, ?, 11, 6)
    :param sql:
    :return:
    """
    sql_pattern = re.compile(r"'[^']*'")
    return sql_pattern.sub(r"?", sql)


def replace_int_value_sql(sql):
    """
    # before: insert into pets (name, birth_date, owner_id, type_id) values (?, ?, 11, 5)
    # after: insert into pets (name, birth_date, owner_id, type_id) values (?, ?, ?, ?)
    #
    # before: select pettype0_.id as id1_3_0_, pettype0_.name as name2_3_0_ from types pettype0_ where pettype0_.id=6
    # after: select pettype0_.id as id1_3_0_, pettype0_.name as name2_3_0_ from types pettype0_ where pettype0_.id=?
    :param sql:
    :return:
    """
    sql_pattern = re.compile(r"(?<=[\(|\s|=])\d+")
    return sql_pattern.sub(r"?", sql)


def get_abstract_sql(sql):
    operator = sql.split(' ')[0]
    if operator == 'select':
        abstract_sql = get_abstracted_sql(sql).get('str')
        return abstract_sql
    else:
        return sql


def get_abstracted_sql(query):
    """
    Parse and preprocess SQL queries

    For example:
    select visits0_.pet_id as pet_id4_1_0_, visits0_.id as id1_6_0_,
    visits0_.id as id1_6_1_, visits0_.visit_date as visit_da2_6_1_,
    visits0_.description as descript3_6_1_, visits0_.pet_id as pet_id4_6_1_
    from visits visits0_ where visits0_.pet_id=2

    we will preprocess the query and it will become:
    select from visit where visits0_.pet_id=2
    :param query:
    :return:
    """
    abstracted_sql = []
    parsed = sqlparse.parse(query)[0]
    for token in parsed.tokens:
        if token.ttype is not None and token.ttype is not Text.Whitespace:
            # remove the keyword "on"
            if token.value != 'on':
                abstracted_sql.append(token.value)
        # Identifier can be table name in an update SQL: "update pets set ...", in this case, pets will be the Identifier
        if isinstance(token, Identifier):
            identifier = token.value
            # here, identifier can be "visits visits0_", in this case, we remove the variable name "visits0_"
            # if ' ' in identifier:
            #     identifier = identifier.split(' ')[0]
            # # stemmed_identifier = nltk.PorterStemmer().stem(identifier)
            # abstracted_sql.append(identifier)

            if ' ' in identifier:
                identifier = identifier.split(' ')[0]
            stemmedIdentifier = nltk.PorterStemmer().stem(identifier)
            abstracted_sql.append(stemmedIdentifier)

        if isinstance(token, Where):
            abstracted_sql.append(token.value)

        # we are now parsing the "from Table tableVar"
        # from X ... or left outer join X. We need to parse X.
        # if token.ttype == Keyword and (token.value == "from" or "join" in token.value):
        #    visitedFrom = True
        # if token.ttype == Keyword.DML and token.ttype.value == "delete":
        #    visitedFrom = True
        # keyword 'like' in SQL
        # if ' like ' in token.value:
        #     abstracted_sql.append('like')

    abstract_sql_str = ""
    for token in abstracted_sql:
        abstract_sql_str += token
        abstract_sql_str += " "

    return {'sql': query, 'abstract_sql_str': abstract_sql_str, 'str': abstract_sql_str}
