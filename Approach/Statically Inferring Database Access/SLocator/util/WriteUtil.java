package SLocator.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import SLocator.GlobalData;
import SLocator.datastructure.JDTJavaClass;
import SLocator.datastructure.JDTJavaMethod;
import SLocator.datastructure.JDTJavaMethodPath;
import SLocator.datastructure.JDTJavaClass.JDTClassType;

/**
 */
public class WriteUtil {
	public static void writeEntryMethodPaths(PrintWriter cfgResultPW, HashMap<JDTJavaMethod, ArrayList<JDTJavaMethodPath>> methodPaths) {
		for (Map.Entry<JDTJavaMethod, ArrayList<JDTJavaMethodPath>> entry : methodPaths.entrySet()) {				
			// print the method
			// to split control flow of different method
			cfgResultPW.println("##################################################################"); 
			JDTJavaMethod method = entry.getKey();
			writeMethodPaths(cfgResultPW, entry.getValue(), method.getFullNameWithParams());
		}
	}
	
	public static void writeCFG(PrintWriter cfgResultPW, HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> methodsList) {
		for (Map.Entry<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> entry : methodsList.entrySet()) {				
			// print the method
			// to split control flow of different method
			cfgResultPW.println("##################################################################"); 
			JDTJavaMethod request = entry.getKey();
			ArrayList<ArrayList<JDTJavaMethod>> paths = entry.getValue();

			// print the control flow of the method
			int index = 1;
			for(ArrayList<JDTJavaMethod> javaMethodPath : paths) {
				if(javaMethodPath == null)
					continue;
				
				// to split control flow of same method
				if (index++ !=1) {
					cfgResultPW.println("------------------------------------------------------------------");
				}
				
				cfgResultPW.println("root:" + request.getFullNameWithParams());
				Set<JDTJavaMethod> interfaceSet = GlobalData.interfaceAndSuperMethods.keySet();
				for(JDTJavaMethod jDTJavaMethod : javaMethodPath) {
					// method
//					cfgResultPW.print(" ");
//					for (int i = 0; i < jDTJavaMethod.getCallLevel(); i++)
//						cfgResultPW.print("-");
					
					cfgResultPW.println("method:" + jDTJavaMethod.getFullNameWithParams());
					
					// method sqls
					if (jDTJavaMethod.getSqls() != null) {
						jDTJavaMethod.getSqls().forEach((sql) -> {
//							cfgResultPW.print(" ");
//							for (int i = 0; i < jDTJavaMethod.getCallLevel(); i++)
//								cfgResultPW.print(" ");
							cfgResultPW.println("[" + sql + "]");
						});
					}
				}
			}
		}
	}
	
	public static void writeEntryMethodsList(PrintWriter cfgResultPW, HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> methodsList) {
		for (Map.Entry<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> entry : methodsList.entrySet()) {				
			// print the method
			// to split control flow of different method
			cfgResultPW.println("##################################################################"); 
			JDTJavaMethod method = entry.getKey();
			writeMethodsList(cfgResultPW, entry.getValue(), method.getFullNameWithParams());
		}
	}
	
	public static void writeEntryMethodsList(PrintWriter cfgResultPW, Map<JDTJavaMethod, Set<List<String>>> requestSqlMap) {
		requestSqlMap.forEach((method, pathsSql) -> {
			cfgResultPW.println("##################################################################"); 
			pathsSql.forEach(pathSql -> {
				cfgResultPW.println("method:" + method.getFullNameWithParams());
				pathSql.forEach(sql -> {
					cfgResultPW.println("--(" + sql + ")");
				});
				cfgResultPW.println("------------------------------------------------------------------");
			});
		});
	}
	
	public static void writeMethodPaths(PrintWriter cfgResultPW, ArrayList<JDTJavaMethodPath> paths, String methodName) {
		// print the control flow of the method
		int index = 1;
		for(JDTJavaMethodPath methodPath : paths) {
			if(methodPath.getMethodList().size() == 0)
				continue;
			
			// to split control flow of same method
			if (index++ !=1) {
				cfgResultPW.println("------------------------------------------------------------------");
			}
			
			cfgResultPW.println("method: " + methodName);
			for(JDTJavaMethod jDTJavaMethod : methodPath.getMethodList()) {
				cfgResultPW.print(" ");
				for (int i = 0; i < jDTJavaMethod.getCallLevel(); i++)
					cfgResultPW.print("-");
				cfgResultPW.println(jDTJavaMethod.getFullNameWithParams());
			}
		}
	}
	
	public static void writeMethodsList(PrintWriter cfgResultPW, ArrayList<ArrayList<JDTJavaMethod>> paths, String methodName) {
		// print the control flow of the method
		int index = 1;
		for(ArrayList<JDTJavaMethod> javaMethodPath : paths) {
			if(javaMethodPath.size() == 0)
				continue;
			
			// to split control flow of same method
			if (index++ !=1) {
				cfgResultPW.println("------------------------------------------------------------------");
			}
			
			cfgResultPW.println("request:" + methodName);
			Set<JDTJavaMethod> interfaceSet = GlobalData.interfaceAndSuperMethods.keySet();
			for(JDTJavaMethod jDTJavaMethod : javaMethodPath) {
				// method
//				cfgResultPW.print(" ");
//				for (int i = 0; i < jDTJavaMethod.getCallLevel(); i++)
//					cfgResultPW.print("-");
				
				if (interfaceSet.contains(jDTJavaMethod)) {
					jDTJavaMethod.setJDTClassType(JDTClassType.JDT_INTERFACE);
				}
				
				// ignore interfaces because aspectj call methods have not interface
				if (jDTJavaMethod.getJavaClass() == null
							||jDTJavaMethod.getJavaClass().getClassType() == JDTJavaClass.JDTClassType.JDT_CLASS) {
					cfgResultPW.println("method:" + jDTJavaMethod.getFullNameWithParams());
				}
				
				// method sqls
				if (jDTJavaMethod.getSqls() != null) {
					jDTJavaMethod.getSqls().forEach((sql) -> {
//						cfgResultPW.print(" ");
//						for (int i = 0; i < jDTJavaMethod.getCallLevel(); i++)
//							cfgResultPW.print(" ");
						cfgResultPW.println("[" + sql + "]");
					});
				}
			}
		}
	}
}
