package SLocator;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import SLocator.core.path.InterfaceAndSuperMethod;
import SLocator.core.path.MethodCaller;
import SLocator.core.sql.SqlParser;
import SLocator.core.sql.XMLReader;
import SLocator.datastructure.JDTJavaMethod;
import SLocator.datastructure.Program;
import SLocator.datastructure.ProgramWorkspace;
import SLocator.util.FileUtil;
import SLocator.util.WriteUtil;

public class Application implements IApplication {
	@Override
	public Object start(IApplicationContext context) throws Exception {
		
		/**
		 * configuration of study application
		 */
		GlobalData.directoryPath = "E:\\Projects\\analyze-workspace\\data-result\\PetClinic\\";
		Program program = Program.PETCLINIC;	;
		GlobalData.program = Program.PETCLINIC;
		System.out.println("Application start");
		long startTime = System.nanoTime();
		
		/**
		 * Step 1 read program basic information
		 */
		ProgramWorkspace programWorkspace = new ProgramWorkspace(program);
		programWorkspace.traverseWorkspace();
		programWorkspace.traverseToGetJavaClass();
		programWorkspace.traverseToGetJavaMethod();
		programWorkspace.traverseToGetEntityClass();
		programWorkspace.traverseClassRelationShip();
		programWorkspace.traverseRequestMethod();
		programWorkspace.showInformation();
	
		/**
		 * Step 2 analyze the source code to get all control flow paths
		 */
		// traverse project to get all related sql methods
		MethodCaller.run(programWorkspace.getIcompilationUnitList());
		
		// traverse project to get all interface and sub methods
		InterfaceAndSuperMethod.run(programWorkspace.getIcompilationUnitList());
		
		// generate control flow graphs in every method
		GenerateCFG.run(programWorkspace.getInterfaces(), programWorkspace.getIcompilationUnitList(),
				programWorkspace.getJavaMethodSet());
		
		// generate all paths
		GenerateFullPath.run(programWorkspace.getRequestMethodList());
	    
		/**
		 * Step 3 Infer database access (Template SQL queries)
		 */
		XMLReader.run(program);
		Map<String, List<String>> sqlMap = XMLReader.getSqlMap();
		Map<JDTJavaMethod, List<String>> methodSqlMap = SqlParser.run(sqlMap, programWorkspace);
		
		/**
		 * Part4 add sql queries to control flow paths
		 */
		addSqlToPath(methodSqlMap);
		
		System.out.println("Total executed in: " + (System.nanoTime() - startTime)/1000000000 + "s");
		return null;
	}
	
	public static void addSqlToPath(Map<JDTJavaMethod, List<String>> methodSqlMap) {
		/**
		 * step1: add sqls to paths
		 */
		HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> result = GlobalData.cfgResult;
		for(Entry<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> entry : result.entrySet()){
			ArrayList<ArrayList<JDTJavaMethod>> paths = entry.getValue();
			for (ArrayList<JDTJavaMethod> path : paths) {
				for (JDTJavaMethod node : path) {
					node.addAllSqs(methodSqlMap.get(node));
				}
			}
		}
		
		PrintWriter cfgResultPW = FileUtil.getPrintWriter(GlobalData.directoryPath + "all_call_paths_with_sqls.txt");
		WriteUtil.writeEntryMethodsList(cfgResultPW, result);
		cfgResultPW.close();

		
		/**
		 * step2: filter the paths without sqls
		 */
		HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> new_result = new HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>>();
		for (JDTJavaMethod request : result.keySet()) {
			ArrayList<ArrayList<JDTJavaMethod>> paths = result.get(request);
			ArrayList<ArrayList<JDTJavaMethod>> new_paths = new ArrayList<ArrayList<JDTJavaMethod>>();
			
			for (ArrayList<JDTJavaMethod> path : paths) {	
				boolean hasSql = false;
				for (JDTJavaMethod method: path) {
					if(method.getSqls() != null && !method.getSqls().isEmpty()) {
						hasSql = true;
					}
				}
				if (hasSql) {
					new_paths.add(path);
				}
			}
			
			if (!new_paths.isEmpty()) {
				new_result.put(request, new_paths);
			}
		}
		
		cfgResultPW = FileUtil.getPrintWriter(GlobalData.directoryPath + "filter_call_paths_with_sqls.txt");
		WriteUtil.writeEntryMethodsList(cfgResultPW, new_result);
		cfgResultPW.close();
	}

	@Override
	public void stop() {
	}
}
