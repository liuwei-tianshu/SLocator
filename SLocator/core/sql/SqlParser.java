package SLocator.core.sql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import SLocator.datastructure.JDTJavaMethod;
import SLocator.datastructure.ProgramWorkspace;
import SLocator.util.JsonUtil;
import SLocator.util.ObjectCreationHelper;


public class SqlParser {
	
	public static Map<JDTJavaMethod, List<String>> run(Map<String, List<String>> sqlMap, ProgramWorkspace programWorkspace) throws RuntimeException{
		long appStartTime = System.nanoTime();
		Map<JDTJavaMethod, List<String>> jdtMethodSqlMap = null;
		if (JsonUtil.sqlMapFileExists()) {
			System.out.println("=================================");
			System.out.println("read sql map");
			jdtMethodSqlMap = JsonUtil.restoreSqlMap();
		} else {
			System.out.println("=================================");
			System.out.println("generate sql map");
			jdtMethodSqlMap = action(sqlMap, programWorkspace);
			JsonUtil.storeSqlMap(jdtMethodSqlMap);
		}
		
		long seconds = (System.nanoTime() - appStartTime) / 1000000 / 1000;
		System.out.println(seconds + " seconds");
		return jdtMethodSqlMap;
	}
	
	public static Map<JDTJavaMethod, List<String>> action(Map<String, List<String>> sqlMap, ProgramWorkspace programWorkspace) throws RuntimeException {
		/**
		 * step1: calculate Map<JDTJavaMethod, List<String>> methodSqlMap for whole workspace
		 */
		List<CompilationUnit> compilationUnitList = programWorkspace.getCompilationUnitList();
		Map<String, Long> emFunctionNumber = new HashMap<String, Long>();
		// <a method(database access method), all sqls in the method>
		Map<JDTJavaMethod, List<String>> methodSqlMap = new HashMap<JDTJavaMethod, List<String>>();
		for (CompilationUnit cu : compilationUnitList) {
			JpaAccessSql jpaAccessSql = new JpaAccessSql(sqlMap, programWorkspace);
			
			Map<MethodDeclaration, List<String>> sqlsForClass = jpaAccessSql.calculateSqls(cu, emFunctionNumber);
			sqlsForClass.forEach((method, sqls) -> {
				JDTJavaMethod jDTJavaMethod = ObjectCreationHelper.createMethodFromMethodDeclaration(method);
				if (jDTJavaMethod == null) {
					throw new RuntimeException("createMethodFromMethodDeclaration exception, method: " + method); 
				}
				methodSqlMap.put(jDTJavaMethod, sqls);
			});
		}
		// Statistics
		System.out.println("=======================================================");
		System.out.println("#EntityManager calls#");
		emFunctionNumber.forEach((functionName, number) -> {
			System.out.println(functionName + " number: " + number);
		});
		
//		System.out.println("=======================================================");
//		System.out.println("#All sqls in program#");
//		methodSqlMap.forEach((jdtJavaMethod, sqlList) -> {
//			System.out.println("-----------------------------------------------");
//			System.out.println(jdtJavaMethod.getFullNameWithParams());
//			sqlList.forEach(sql -> {
//				System.out.println(sql);
//			});
//		});
		JsonUtil.storeSqlMap(methodSqlMap);
		
		return methodSqlMap;
	}
	
}
