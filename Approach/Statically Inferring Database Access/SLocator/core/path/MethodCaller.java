package SLocator.core.path;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import SLocator.GlobalData;
import SLocator.datastructure.JDTJavaMethod;
import SLocator.util.JsonUtil;
import SLocator.util.ObjectCreationHelper;


public class MethodCaller {
	
	// all methods have annotation "Repository"
	private static List<IMethod> sqlMethods = new ArrayList<>();
	
	private static Long classNumber = 0L;
	private static Long methodNumber = 0L;
	/**
	 * 1 get data from storage if it exists
	 * 
	 * 2 generate data from source code
	 * @throws CoreException
	 * @throws Exception
	 */
	public static void run(List<ICompilationUnit> icompilationUnitList) throws CoreException, Exception {
		long appStartTime = System.nanoTime();
		
		/**
		 * 1 get all SqlMethods
		 */
		getSqlMethods(icompilationUnitList);
		
		/**
		 * check code write all sql methods
		 */
		List<JDTJavaMethod> tempMethodList = new ArrayList<JDTJavaMethod>();
		for(IMethod iMethod : sqlMethods) {
			JDTJavaMethod jMethod = ObjectCreationHelper.createMethodFromIMethod(iMethod);
			if (jMethod != null) {
				tempMethodList.add(jMethod);
			}
		}
		JsonUtil.storeJDTJavaMethodsWithFileName(tempMethodList, "sqlMethods.txt");
		
		/**
		 * 2 get all SqlRelatedMethods from the SqlMethods
		 */
		if (JsonUtil.sqlRelatedMethodsFileExists()) {
			System.out.println("read sql related methods...");
			GlobalData.sqlRelatedMethods = JsonUtil.restoreJDTJavaMethods();			
		} else {
			System.out.println("generate sql related methods and store");
			getCallers();
			JsonUtil.storeJDTJavaMethods(GlobalData.sqlRelatedMethods);
		}
		
		/**
		 * 3 Result
		 */
		long seconds = (System.nanoTime() - appStartTime) / 1000000 / 1000;
		System.out.println("classNumber: " + classNumber);
		System.out.println("methodNumber: " + methodNumber);
		System.out.println("sqlMethodNumber: " + sqlMethods.size());
		System.out.println("sqlRelatedMethodNumber: " + GlobalData.sqlRelatedMethods.size());
		System.out.println(seconds + " seconds");
	}
	
	public static void getCallers() {
//		int index = 1;
//		for (IMethod sqlMethod : sqlMethods) {
//			System.out.print("index: " + index++ + ", sqlMethod: " + sqlMethod.getCompilationUnit().getElementName() + "." + sqlMethod.getElementName());
//			long methodStratTime = System.nanoTime();
//			DFS(sqlMethod);
//			long seconds = (System.nanoTime() - methodStratTime) / 1000000 / 1000;
//			System.out.println(", costTime: " + seconds + ", sqlRelatedMethodNumber: " + sqlRelatedMethods.size());
//		}
		
/*		int index = 1;
		for (IMethod sqlMethod : sqlMethods) {
			new GetCallerTask(index++, sqlMethod, sqlRelatedJDTMethods).run();
			
		}*/
		
		/**
		 * using multi-thread to get all SqlRelatedMethods from the SqlMethods
		 */
		ExecutorService executor = Executors.newCachedThreadPool();
		int index = 1;
		for (IMethod sqlMethod : sqlMethods) {
			executor.submit(new GetCallerTask(index++, sqlMethod, GlobalData.sqlRelatedMethods));
		}
		try {
		    System.out.println("attempt to shutdown executor");
		    executor.shutdown();
		    executor.awaitTermination(3600, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
		    System.err.println("tasks interrupted");
		}
		finally {
		    if (!executor.isTerminated()) {
		        System.err.println("cancel non-finished tasks");
		    }
		    executor.shutdownNow();
		    System.out.println("shutdown finished");
		}
	}
	
	public static void getSqlMethods(List<ICompilationUnit> icompilationUnitList) throws CoreException, Exception {
		for (ICompilationUnit icompilationUnit : icompilationUnitList) {
			processClass(icompilationUnit);
		}
	}
	
	private static void processClass(ICompilationUnit icu) throws JavaModelException {
		icu.getElementName();
		classNumber++;
		
		for (IType itype : icu.getTypes()) {
			methodNumber += itype.getMethods().length;
			// sqlMethods
			for (IAnnotation annotation : itype.getAnnotations()) {
				if (annotation.getElementName().equals("Repository")) {
					for (IMethod iMethod : itype.getMethods()) {
						sqlMethods.add(iMethod);
						GlobalData.sqlJDTMethods.add(ObjectCreationHelper.createMethodFromIMethod(iMethod));
					}
				}
			}
		}
	}
	
}
