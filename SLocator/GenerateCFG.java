package SLocator;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import SLocator.core.path.CFGVisitor;
import SLocator.datastructure.JDTJavaClass;
import SLocator.datastructure.JDTJavaMethod;
import SLocator.util.FileUtil;
import SLocator.util.JsonUtil;
import SLocator.util.ObjectCreationHelper;
import SLocator.util.WriteUtil;


public class GenerateCFG {
	
	public static void run(Map<JDTJavaClass, List<JDTJavaClass>> interfaces, 
			List<ICompilationUnit> icompilationUnitList,
			Set<JDTJavaMethod> javaMethodSet) throws CoreException, Exception {
		long appStartTime = System.nanoTime();
		
		/**
		 * get all sqlMethods AND entryMehtods in workspace
		 */
		traverseMethodInWorkspace(icompilationUnitList);
		System.out.println("sqlIMethods size: " + GlobalData.sqlIMethods.size());
		System.out.println("entryMethods size: " + GlobalData.entryMethods.size());
		System.out.println("sqlJDTMethods size: " + GlobalData.sqlJDTMethods.size());
		
		if (JsonUtil.methodPathExists()) {
			System.out.println("=================================");
			System.out.println("GenerateCFG read full path");
			GlobalData.javaMethodPaths = JsonUtil.restoreMethodPath();
		} else {
			System.out.println("=================================");
			System.out.println("GenerateCFG generate full path");
			action(interfaces, icompilationUnitList, javaMethodSet);
			JsonUtil.storeMethodPath(GlobalData.javaMethodPaths);
		}
		
		long seconds = (System.nanoTime() - appStartTime) / 1000000 / 1000;
		System.out.println(seconds + " seconds");
	}
	
	public static void action(Map<JDTJavaClass, List<JDTJavaClass>> interfaces, 
			List<ICompilationUnit> icompilationUnitList,
			Set<JDTJavaMethod> javaMethodSet) throws CoreException, Exception {
		/**
		 * get all paths in a method
		 */
		for (ICompilationUnit icu : icompilationUnitList) {
			CompilationUnit parsedUnit = ObjectCreationHelper.parse(icu);
			CFGVisitor visitor = new CFGVisitor(javaMethodSet);
			parsedUnit.accept(visitor);
		}
		
		System.out.println("javaMethodPaths size: " + GlobalData.javaMethodPaths.size());
		System.out.println("big methods size: " + CFGVisitor.getBigMethods().size());
		JsonUtil.storeBigJDTMethod(CFGVisitor.getBigMethods());
		
		// print result 
		PrintWriter printWriter = FileUtil.getPrintWriter(GlobalData.directoryPath + "cfg.txt");
		WriteUtil.writeCFG(printWriter, GlobalData.javaMethodPaths);
		printWriter.close();
	}
	
	public static void traverseMethodInWorkspace(List<ICompilationUnit> icompilationUnitList) throws CoreException, Exception {
		for (ICompilationUnit icu : icompilationUnitList) {
			/**
			 * get sqlMethods AND entryMehtods
			 */
			for (IType itype : icu.getTypes()) {
				for (IAnnotation annotation : itype.getAnnotations()) {
					// System.out.println("annotation: " + annotation.getSource());
					if (annotation.getElementName().equals("Repository")) {
						for (IMethod iMethod : itype.getMethods()) {
							JDTJavaMethod jDTJavaMethod = ObjectCreationHelper.createMethodFromIMethod(iMethod);
							if (jDTJavaMethod != null) {
								GlobalData.sqlJDTMethods.add(jDTJavaMethod);
							}
							GlobalData.sqlIMethods.add(iMethod);
						}
					}
				}

				// entryMehtods
				for (IMethod iMethod : itype.getMethods()) {
					if (isEntryMethod(iMethod)) {
						GlobalData.entryMethods.add(iMethod);
					}
				}
			}
		}
	}
	
	public static boolean isEntryMethod(IMethod iMethod) throws JavaModelException {
		IAnnotation[] annotations = iMethod.getAnnotations();
		if (annotations != null) {
			for (IAnnotation iAnnotation : annotations) {
				if (iAnnotation.getElementName().equals("RequestMapping")) {
					return true;
				}
			}
		}
		return false;	
	}	
}
