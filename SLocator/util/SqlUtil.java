package SLocator.util;

import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;

/**
 */
public class SqlUtil {
	
	/**
	 * in hibernate, getTableList actually returns the class name
	 * @param sql
	 * @return
	 */
	public static List<String> getTableClassList(String sql){
		/**
		 * remove token: "fetch" "FETCH" from sql
		 * sqlparser doesn't support them
		 */
		sql = sql.replace("fetch", "");
		sql = sql.replace("FETCH", "");
		
		Statement statement;
		try {
			statement = CCJSqlParserUtil.parse(sql);
		} catch (JSQLParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		Select selectStatement = (Select) statement;
		TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
		return tablesNamesFinder.getTableList(selectStatement);
	}
	
	
	public static void main(String[] args){
		System.out.println(getTableClassList("SELECT * FROM MY_TABLE1, MY_TABLE2, (SELECT * FROM MY_TABLE3) LEFT OUTER JOIN MY_TABLE4 "+
				" WHERE ID = (SELECT MAX(ID) FROM MY_TABLE5) AND ID2 IN (SELECT * FROM MY_TABLE6)"));
	}
}
