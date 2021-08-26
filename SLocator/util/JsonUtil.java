package SLocator.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.jdt.core.ICompilationUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import SLocator.GlobalData;
import SLocator.datastructure.JDTEntityClass;
import SLocator.datastructure.JDTJavaMethod;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * 
 */
public class JsonUtil {
	
	private static String sqlRelatedMethodsFileName = "sqlRelatedMethods.txt";
	private static String interfaceAndSuperMethodsFileName = "interfaceAndSuperMethods.json";
	private static String iCompilationUnit = "icompilationUnitList.txt";
	private static String sqlMapFileName = "sqlMap.txt";
	private static String fullPathFileName = "fullPath.txt";
	private static String entityClassFileName = "entityClass.txt";
	private static String JDTMethodFileName = "JDTMethod.txt";
	private static String methodPathFileName = "methodPath.txt";
	private static String bigJDTMethodFileName = "bigJDTMethod.txt";
	private static String requestMethodFileName = "requestMethod.txt";
	
	public static boolean interfaceAndSuperMethodsFileExists() {
		File file = new File(GlobalData.directoryPath + interfaceAndSuperMethodsFileName);
		return file.exists();
	}
	
	/**
	 * store json data of List<ICompilationUnit> icompilationUnitList
	 * @param interfaceAndSuperMethods
	 */
	public static void storeICompilationUnit(List<ICompilationUnit> icompilationUnitList) {
		if (icompilationUnitList == null || icompilationUnitList.isEmpty())
			return;
		
		List<String> jsons = new ArrayList<String>();
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		for (ICompilationUnit ic: icompilationUnitList) {
			jsons.add(gson.toJson(ic));
		}
		
		PrintWriter cfgResultPW = FileUtil.getPrintWriter(GlobalData.directoryPath + iCompilationUnit);
		for (String json : jsons) {
			cfgResultPW.println(json);
		}
		cfgResultPW.close();
	}
	
	public static void storeInterfaceAndSuperMethods(Map<JDTJavaMethod, ArrayList<JDTJavaMethod>> interfaceAndSuperMethods) {
		if (interfaceAndSuperMethods == null || interfaceAndSuperMethods.isEmpty())
			return;
		
		List<String> jsons = new ArrayList<String>();
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		for (Map.Entry<JDTJavaMethod, ArrayList<JDTJavaMethod>> entry : interfaceAndSuperMethods.entrySet()) {
			JDTJavaMethod method = entry.getKey();
			ArrayList<JDTJavaMethod> methods = entry.getValue();
			InterfaceSuperClass interfaceSuper = new InterfaceSuperClass(method, methods);
			jsons.add(gson.toJson(interfaceSuper));
		}
		
		PrintWriter cfgResultPW = FileUtil.getPrintWriter(GlobalData.directoryPath + interfaceAndSuperMethodsFileName);
		for (String json : jsons) {
			cfgResultPW.println(json);
		}
		cfgResultPW.close();
	}
	
	public static Map<JDTJavaMethod, ArrayList<JDTJavaMethod>> restoreInterfaceAndSuperMethods() {
		BufferedReader bf = FileUtil.getFileReader(GlobalData.directoryPath + interfaceAndSuperMethodsFileName);
		Map<JDTJavaMethod, ArrayList<JDTJavaMethod>> interfaceAndSuperMethods = new HashMap<JDTJavaMethod, ArrayList<JDTJavaMethod>>();
		String line;
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		try {
			line = bf.readLine();
			while(line != null) {
				InterfaceSuperClass interfaceSuper = gson.fromJson(line, InterfaceSuperClass.class);
				interfaceAndSuperMethods.put(interfaceSuper.getInterfaceSuper(), interfaceSuper.getImplementedSub());
				line = bf.readLine();
			}
			bf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (interfaceAndSuperMethods.size() == 0)
			return null;
		
		return interfaceAndSuperMethods;
	}
	
	public static boolean sqlRelatedMethodsFileExists() {
		File file = new File(GlobalData.directoryPath + sqlRelatedMethodsFileName);
		return file.exists();
	}
	
	public static void storeJDTJavaMethods(Set<JDTJavaMethod> jDTJavaMethods) {
		if (jDTJavaMethods == null || jDTJavaMethods.isEmpty())
			return;
		
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		List<String> jsons = new ArrayList<String>();
		for (JDTJavaMethod jDTJavaMethod : jDTJavaMethods) {
			jsons.add(gson.toJson(jDTJavaMethod));
			
			/**
			 * TODO check code
			 */
			//jsons.add(JSON.toJSONString(jDTJavaMethod));
		}
		
		PrintWriter cfgResultPW = FileUtil.getPrintWriter(GlobalData.directoryPath + sqlRelatedMethodsFileName);
		for (String json : jsons) {
			cfgResultPW.println(json);
		}
		cfgResultPW.close();
	}
	
	public static Set<JDTJavaMethod> restoreJDTJavaMethods() {
		BufferedReader bf = FileUtil.getFileReader(GlobalData.directoryPath + sqlRelatedMethodsFileName);
		Set<JDTJavaMethod> jDTJavaMethods = ConcurrentHashMap.newKeySet();
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		String line;
		try {
			line = bf.readLine();
			while(line != null) {
				jDTJavaMethods.add(gson.fromJson(line, JDTJavaMethod.class));
				
				/**
				 * TODO check code
				 */
				// jDTJavaMethods.add(JSON.parseObject(line, JDTJavaMethod.class));
				
				line = bf.readLine();
			}
			bf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (jDTJavaMethods.size() == 0)
			return null;
		
		return jDTJavaMethods;
	}

	public static void storeJDTJavaMethodsWithFileName(List<JDTJavaMethod> requestMethodList, String fileName) {
		if (CollectionUtils.isEmpty(requestMethodList)) {
			return;
		}
		
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		List<String> jsons = new ArrayList<String>();
		for (JDTJavaMethod jDTJavaMethod : requestMethodList) {
			jsons.add(gson.toJson(jDTJavaMethod));
		}
		
		PrintWriter cfgResultPW = FileUtil.getPrintWriter(GlobalData.directoryPath + fileName);
		for (String json : jsons) {
			cfgResultPW.println(json);
		}
		cfgResultPW.close();
	}
	
	
	/**
	 *********************************************************************************
	 * request method
	 ********************************************************************************* 
	 */
	public static void storeRequestMethod(List<JDTJavaMethod> requestMethodList) {
		if (requestMethodList == null || requestMethodList.isEmpty())
			return;
		
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().enableComplexMapKeySerialization().create();
		String jsonStr = gson.toJson(requestMethodList);
		PrintWriter cfgResultPW = FileUtil.getPrintWriter(GlobalData.directoryPath + requestMethodFileName);
		cfgResultPW.println(jsonStr);
		cfgResultPW.close();
	}
	
	public static List<JDTJavaMethod> restoreRequestMethod() {
		try {
			String filePath = GlobalData.directoryPath + requestMethodFileName;
			String content = org.apache.commons.io.FileUtils.readFileToString(new File(filePath),
					StandardCharsets.UTF_8);
			Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().enableComplexMapKeySerialization().create();
			Type requestMethodType = new TypeToken<ArrayList<JDTJavaMethod>>(){}.getType();
			return gson.fromJson(content, requestMethodType);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("restore method path error");
			return null;
		}
	}
	
	public static boolean requestMethodExists() {
		File file = new File(GlobalData.directoryPath + requestMethodFileName);
		return file.exists();
	}
	
	
	/**
	 *********************************************************************************
	 * sql method
	 ********************************************************************************* 
	 */
	public static void storeSqlMethods(List<JDTJavaMethod> requestMethodList, String fileName) {
		return;
	}
	
	
	/**
	 *********************************************************************************
	 * sql map
	 ********************************************************************************* 
	 */
	public static void storeSqlMap(Map<JDTJavaMethod, List<String>> methodSqlMap) {
		if (methodSqlMap == null || methodSqlMap.isEmpty())
			return;
		
		/**
		 * test code for performance
		 */
//		for (int i=0;i<=2000;i++) {
//			JDTJavaMethod method = new JDTJavaMethod("packageName" + i, "className", "methodName", JDTClassType.JDT_CLASS);
//			List<String> tempList = new ArrayList<String>();
//			for (int j=0;j<=20;j++) {
//				tempList.add("SELECT visit FROM Visit v where v.pets.id= :id");
//			}
//			methodSqlMap.put(method, tempList);
//		}
		
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().enableComplexMapKeySerialization().create();
		String jsonStr = gson.toJson(methodSqlMap);
		PrintWriter cfgResultPW = FileUtil.getPrintWriter(GlobalData.directoryPath + sqlMapFileName);
		cfgResultPW.println(jsonStr);
		cfgResultPW.close();
	}
	
	public static Map<JDTJavaMethod, List<String>> restoreSqlMap() {
		try {
			String filePath = GlobalData.directoryPath + sqlMapFileName;
			String content = org.apache.commons.io.FileUtils.readFileToString(new File(filePath),
					StandardCharsets.UTF_8);
			Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().enableComplexMapKeySerialization().create();
			Type sqlMapType = new TypeToken<HashMap<JDTJavaMethod, List<String>>>(){}.getType();
			return gson.fromJson(content, sqlMapType);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("restoreSqlMap error");
			return null;
		}
	}
	
	public static boolean sqlMapFileExists() {
		File file = new File(GlobalData.directoryPath + sqlMapFileName);
		return file.exists();
	}
	
	/**
	 *********************************************************************************
	 * full path
	 ********************************************************************************* 
	 */
	public static void storeFullPath(Map<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> fullPath) {
		if (fullPath == null || fullPath.isEmpty())
			return;
		
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().enableComplexMapKeySerialization().create();
		String jsonStr = gson.toJson(fullPath);
		PrintWriter cfgResultPW = FileUtil.getPrintWriter(GlobalData.directoryPath + fullPathFileName);
		cfgResultPW.println(jsonStr);
		cfgResultPW.close();
	}
	
	public static HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> restoreFullPath() {
		try {
			String filePath = GlobalData.directoryPath + fullPathFileName;
			String content = org.apache.commons.io.FileUtils.readFileToString(new File(filePath),
					StandardCharsets.UTF_8);
			Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().enableComplexMapKeySerialization().create();
			Type fullPathType = new TypeToken<HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>>>(){}.getType();
			return gson.fromJson(content, fullPathType);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("restore full path error");
			return null;
		}
	}
	
	public static boolean fullPathExists() {
		File file = new File(GlobalData.directoryPath + fullPathFileName);
		return file.exists();
	}
	
	/**
	 *********************************************************************************
	 * entity class
	 ********************************************************************************* 
	 */
	public static void storeEntityClass(Set<JDTEntityClass> entityClasses) {
		if (entityClasses == null || entityClasses.isEmpty())
			return;
		
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().enableComplexMapKeySerialization().create();
		String jsonStr = gson.toJson(entityClasses);
		PrintWriter cfgResultPW = FileUtil.getPrintWriter(GlobalData.directoryPath + entityClassFileName);
		cfgResultPW.println(jsonStr);
		cfgResultPW.close();
	}
	
	public static Set<JDTEntityClass> restoreEntityClass() {
		try {
			String filePath = GlobalData.directoryPath + entityClassFileName;
			String content = org.apache.commons.io.FileUtils.readFileToString(new File(filePath),
					StandardCharsets.UTF_8);
			Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().enableComplexMapKeySerialization().create();
			Type entityClassType = new TypeToken<HashSet<JDTEntityClass>>(){}.getType();
			return gson.fromJson(content, entityClassType);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("restore entity class error");
			return null;
		}
	}
	
	public static boolean entityClassExists() {
//		File file = new File(GlobalData.directoryPath + entityClassFileName);
//		return file.exists();
		return false;
	}
	
	/**
	 *********************************************************************************
	 * java method list
	 ********************************************************************************* 
	 */
	public static void storeJDTMethod(List<JDTJavaMethod> javaMethodList) {
		if (javaMethodList == null || javaMethodList.isEmpty())
			return;
		
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().enableComplexMapKeySerialization().create();
		String jsonStr = gson.toJson(javaMethodList);
		PrintWriter cfgResultPW = FileUtil.getPrintWriter(GlobalData.directoryPath + JDTMethodFileName);
		cfgResultPW.println(jsonStr);
		cfgResultPW.close();
	}
	
	/**
	 *********************************************************************************
	 * method path
	 ********************************************************************************* 
	 */
	public static void storeMethodPath(HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> javaMethodPath) {
		if (javaMethodPath == null || javaMethodPath.isEmpty())
			return;
		
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().enableComplexMapKeySerialization().create();
		String jsonStr = gson.toJson(javaMethodPath);
		PrintWriter cfgResultPW = FileUtil.getPrintWriter(GlobalData.directoryPath + methodPathFileName);
		cfgResultPW.println(jsonStr);
		cfgResultPW.close();
	}
	
	public static HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> restoreMethodPath() {
		try {
			String filePath = GlobalData.directoryPath + methodPathFileName;
			String content = org.apache.commons.io.FileUtils.readFileToString(new File(filePath),
					StandardCharsets.UTF_8);
			Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().enableComplexMapKeySerialization().create();
			Type fullPathType = new TypeToken<HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>>>(){}.getType();
			return gson.fromJson(content, fullPathType);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("restore method path error");
			return null;
		}
	}
	
	public static boolean methodPathExists() {
		File file = new File(GlobalData.directoryPath + methodPathFileName);
		return file.exists();
	}
	
	/**
	 *********************************************************************************
	 * big java method list for CFG
	 ********************************************************************************* 
	 */
	public static void storeBigJDTMethod(List<JDTJavaMethod> javaMethodList) {
		if (javaMethodList == null || javaMethodList.isEmpty())
			return;
		
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().enableComplexMapKeySerialization().create();
		String jsonStr = gson.toJson(javaMethodList);
		PrintWriter cfgResultPW = FileUtil.getPrintWriter(GlobalData.directoryPath + bigJDTMethodFileName);
		cfgResultPW.println(jsonStr);
		cfgResultPW.close();
	}
}
