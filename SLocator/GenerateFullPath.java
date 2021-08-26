package SLocator;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import SLocator.datastructure.JDTJavaMethod;
import SLocator.datastructure.JDTJavaClass.JDTClassType;
import SLocator.util.FileUtil;
import SLocator.util.JsonUtil;
import SLocator.util.WriteUtil;

public class GenerateFullPath {
	// <method, all paths in a method body>
	private static HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> javaMethodPaths;
	
	private static List<JDTJavaMethod> localEntryPoints;
	
	public static long debug_combination = 0;
	
	public static void run(List<JDTJavaMethod> entryPoints) {
		long appStartTime = System.nanoTime();
		if (JsonUtil.fullPathExists()) {
			System.out.println("=================================");
			System.out.println("CalculateFullPath read full path");
			GlobalData.cfgResult = JsonUtil.restoreFullPath();
			
		} else {
			System.out.println("=================================");
			System.out.println("CalculateFullPath generate full path");
			action(entryPoints);
			JsonUtil.storeFullPath(GlobalData.cfgResult);
		}
		
		long seconds = (System.nanoTime() - appStartTime) / 1000000 / 1000;
		System.out.println(seconds + " seconds");
	}
	
	public static void action(List<JDTJavaMethod> entryPoints) {
		
		/**
		 * when calling an interface or superclass's method, take it as a branch statement
		 * 
		 * take every implemented method as a branch
		 */
		javaMethodPaths = new HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>>();
		javaMethodPaths.putAll(GlobalData.javaMethodPaths);		
		
		for (Map.Entry<JDTJavaMethod, ArrayList<JDTJavaMethod>> entry : GlobalData.interfaceAndSuperMethods.entrySet()) {
			JDTJavaMethod key = entry.getKey();
			key.setJDTClassType(JDTClassType.JDT_INTERFACE);
			ArrayList<JDTJavaMethod> methods = entry.getValue();
			ArrayList<ArrayList<JDTJavaMethod>> paths = new ArrayList<ArrayList<JDTJavaMethod>>();
			for (JDTJavaMethod method : methods) {
				if (method == null) {
					System.err.println("err in CalculateFullPath.run, method == null");
					continue;
				}
				method.setExpandedMethods(new HashSet<JDTJavaMethod>());
				ArrayList<JDTJavaMethod> path = new ArrayList<JDTJavaMethod>();
				path.add(method);
				paths.add(path);
			}
			javaMethodPaths.put(key, paths);
		}
		
		localEntryPoints = entryPoints;
		
		/**
		 * find full path for every entry
		 */
		Set<JDTJavaMethod> javaMethodSet = javaMethodPaths.keySet();
		int index = 0;
		System.out.println("=======================================================");
		System.out.println("#FullPath#");
		for (JDTJavaMethod key: entryPoints) {
			if (javaMethodSet.contains(key)) {
				System.out.println("index " + ++index + " has sql: " + key);
			} else {
				System.out.println("index " + ++index + " no sql: " + key);
			}
		}
		
		// write the result
		HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> temp = GlobalData.cfgResult;
		for (JDTJavaMethod root : temp.keySet()) {
			ArrayList<ArrayList<JDTJavaMethod>> path = temp.get(root);
			path.size();
		}
		PrintWriter cfgResultPW = FileUtil.getPrintWriter(GlobalData.directoryPath + "all_call_paths.txt");
		WriteUtil.writeEntryMethodsList(cfgResultPW, GlobalData.cfgResult);
		cfgResultPW.close();
	}
	
	public static ArrayList<ArrayList<JDTJavaMethod>> getFullWithCycle(JDTJavaMethod root){
		if (javaMethodPaths.get(root) == null || root.containExpandedMethod(root) || GlobalData.sqlJDTMethods.contains(root)
				|| root.getCallLevel() >= 50) {
			if (root.getCallLevel() >= 50) {
				System.err.println("root level: " + root.getFullNameWithParams());
			}
			ArrayList<ArrayList<JDTJavaMethod>> paths = new ArrayList<ArrayList<JDTJavaMethod>>();
			ArrayList<JDTJavaMethod> node = new ArrayList<>(); node.add(root);
			paths.add(node);
			return paths;
		}
		
		ArrayList<ArrayList<JDTJavaMethod>> subPaths = javaMethodPaths.get(root);
		ArrayList<ArrayList<JDTJavaMethod>> result = new ArrayList<ArrayList<JDTJavaMethod>>();
		for (ArrayList<JDTJavaMethod> subPath : subPaths) {		//do union
			for (JDTJavaMethod tempMethod : subPath) {
				tempMethod.addExpandedMethods(root.getExpandedMethods());
				tempMethod.addExpandedMethod(root);
				tempMethod.setCallLevel(root.getCallLevel() + 1);
			}
			ArrayList<ArrayList<JDTJavaMethod>> resultPath = calcuteForPathWithCycle(subPath);
			
			if (resultPath != null && !resultPath.isEmpty()) {
				for (ArrayList<JDTJavaMethod> path : resultPath) {
					if (!localEntryPoints.contains(root)) {		// ignore the entry point
						path.add(0, root);
					}
				}
				
				/**
				 * change this code to be more strong
				 * remove duplicate path
				 */
				//result.addAll(resultPath);
				for (ArrayList<JDTJavaMethod> path : resultPath) {
					if (!containPath(path, result)) {
						result.add(path);
					}
				}
			}
			
			if (resultPath != null && resultPath.isEmpty()) {
				ArrayList<JDTJavaMethod> tempPath = new ArrayList<JDTJavaMethod>();
				if (!localEntryPoints.contains(root)) {		// ignore the entry point
					tempPath.add(0, root);
				}
				if (!containPath(tempPath, result)) {
					result.add(tempPath);
				}
			}
		}
		
		Set<ArrayList<JDTJavaMethod>> h = new HashSet(result);
		result.clear();
		result.addAll(h);
		return result;
	}
	
	private static ArrayList<ArrayList<JDTJavaMethod>> calcuteForPathWithCycle(ArrayList<JDTJavaMethod> subPath) {
		if (subPath == null || subPath.isEmpty()) {
			ArrayList<ArrayList<JDTJavaMethod>> result = new ArrayList<ArrayList<JDTJavaMethod>>();
			return result;
		}
		
		ArrayList<ArrayList<ArrayList<JDTJavaMethod>>> allPaths = new ArrayList<ArrayList<ArrayList<JDTJavaMethod>>>();
		for (JDTJavaMethod node : subPath) {
			allPaths.add(getFullWithCycle(node));
		}

		// remove the duplicate path for an entry
		ArrayList<ArrayList<JDTJavaMethod>> result = calculateCombination(allPaths);
		
		if (result == null || result.isEmpty()) {
			System.err.println("error in calcuteForPathWithCycle, subPath: " + subPath + ", allPaths: " + allPaths);
			return null;
		}
		Set<ArrayList<JDTJavaMethod>> h = new HashSet(result);
		result.clear();
		result.addAll(h);
		return result;
	}
	
	private static boolean containPath(ArrayList<JDTJavaMethod> path, ArrayList<ArrayList<JDTJavaMethod>> paths) {
		for (ArrayList<JDTJavaMethod> tempPath : paths) {
			if (equalPath(path, tempPath)) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean equalPath(ArrayList<JDTJavaMethod> p1, ArrayList<JDTJavaMethod> p2) {
		if (p1.size() != p2.size()) 
			return false;
		
		for(JDTJavaMethod m : p1) {
			if (!p2.contains(m))
				return false;
		}
		
		for(JDTJavaMethod m : p2) {
			if (!p1.contains(m))
				return false;
		}
		
		return true;
	}
	
	public static ArrayList<ArrayList<JDTJavaMethod>> getFull(JDTJavaMethod root){
		if (javaMethodPaths.get(root) == null) {
			ArrayList<ArrayList<JDTJavaMethod>> paths = new ArrayList<ArrayList<JDTJavaMethod>>();
			ArrayList<JDTJavaMethod> node = new ArrayList<>(); node.add(root);
			paths.add(node);
			return paths;
		} 
		
		ArrayList<ArrayList<JDTJavaMethod>> subPaths = javaMethodPaths.get(root);
		ArrayList<ArrayList<JDTJavaMethod>> result = new ArrayList<ArrayList<JDTJavaMethod>>();
		for (ArrayList<JDTJavaMethod> subPath : subPaths) {		//do union
			ArrayList<ArrayList<JDTJavaMethod>> resultPath = calcuteForPath(subPath);
			if (resultPath != null && !resultPath.isEmpty())
				result.addAll(resultPath);
		}
		return result;
	}
	
	private static ArrayList<ArrayList<JDTJavaMethod>> calcuteForPath(ArrayList<JDTJavaMethod> subPath) {
		ArrayList<ArrayList<ArrayList<JDTJavaMethod>>> allPaths = new ArrayList<ArrayList<ArrayList<JDTJavaMethod>>>();
		for (JDTJavaMethod node : subPath) {
			allPaths.add(getFull(node));
		}
		return calculateCombination(allPaths);
	}
	
	private static ArrayList<ArrayList<JDTJavaMethod>> calculateCombination(List<ArrayList<ArrayList<JDTJavaMethod>>> all) {
		if(all.size() == 1) {
			debug_combination--;
			return all.get(0);
		}
		
		ArrayList<ArrayList<JDTJavaMethod>> result = new ArrayList<ArrayList<JDTJavaMethod>>();
		ArrayList<ArrayList<JDTJavaMethod>> firstResult = all.get(0);
		List<ArrayList<ArrayList<JDTJavaMethod>>> second = getSubListExcludeFirst(all);
		ArrayList<ArrayList<JDTJavaMethod>> secondResult = calculateCombination(second);
		for (int i = 0; i <= firstResult.size() - 1; i++) {
			for (int j = 0; j <= secondResult.size() -1 ; j++) {
				ArrayList<JDTJavaMethod> newPath = new ArrayList<JDTJavaMethod>();
				newPath.addAll(firstResult.get(i)); 
				newPath.addAll(secondResult.get(j));
				
				/**
				 * change code to be strong
				 * remove duplicate path
				 */
				if (!containPath(newPath, result)) {
					result.add(newPath);
				}
			}
		}
		return result;
	}
	
	private static List<ArrayList<ArrayList<JDTJavaMethod>>> getSubListExcludeFirst(List<ArrayList<ArrayList<JDTJavaMethod>>> all) {
		if (all == null || all.isEmpty() || all.size() == 1) {
			System.err.println("getSubListExcludeFirst err, (all == null || all.isEmpty() || all.size() == 1)");
		}
		List<ArrayList<ArrayList<JDTJavaMethod>>> result = new ArrayList<ArrayList<ArrayList<JDTJavaMethod>>>();
		for (int i=1; i < all.size(); i++) {
			result.add(all.get(i));
		}
		return result;
	}
}
