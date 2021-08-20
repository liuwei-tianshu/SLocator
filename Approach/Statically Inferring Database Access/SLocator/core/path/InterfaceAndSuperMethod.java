package SLocator.core.path;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import SLocator.GlobalData;
import SLocator.datastructure.JDTJavaMethod;
import SLocator.util.HierarchyUtil;
import SLocator.util.JsonUtil;
import SLocator.util.ObjectCreationHelper;


public class InterfaceAndSuperMethod {
	/**
	 * 1 get data from storage if it exists
	 * 
	 * 2 generate data from source code
	 * 
	 * @throws CoreException
	 * @throws Exception
	 */
	public static void run(List<ICompilationUnit> icompilationUnitList) throws CoreException, Exception {
		long appStartTime = System.nanoTime();
		if (JsonUtil.interfaceAndSuperMethodsFileExists()) {
			System.out.println("=================================");
			System.out.println("read interface and sub methods...");
			GlobalData.interfaceAndSuperMethods = JsonUtil.restoreInterfaceAndSuperMethods();
			
		} else {
			System.out.println("=================================");
			System.out.println("generate interface and sub methods and store");
			getSubMethod(icompilationUnitList);
			JsonUtil.storeInterfaceAndSuperMethods(GlobalData.interfaceAndSuperMethods);
		}
		
		long seconds = (System.nanoTime() - appStartTime) / 1000000 / 1000;
		System.out.println(seconds + " seconds");
	}
	
	public static void getSubMethod(List<ICompilationUnit> icompilationUnitList) throws CoreException, Exception {
		for (ICompilationUnit icu : icompilationUnitList) {
			processClass(icu);
		}
	}
	
	/**
	 * test this method using EclipseCFG
	 * @param iMethod
	 * @throws JavaModelException 
	 */
	private static void processClass(ICompilationUnit icu) throws JavaModelException {
		for (IType itype : icu.getTypes()) {
			/**
			 * ignore the super class and sub class
			 * just consider the interface and 
			 */
			if (!itype.isInterface()) {		
				continue;
			}
			
			for (IMethod iMethod : itype.getMethods()) {
				List<IMethod> subMethods = HierarchyUtil.getSubMethod(iMethod);
				if (subMethods != null && !subMethods.isEmpty()) {
					JDTJavaMethod superJDTMethod = ObjectCreationHelper.createMethodFromIMethod(iMethod);
					ArrayList<JDTJavaMethod> subJDTMethods = new ArrayList<JDTJavaMethod>();
					for (IMethod subMethod : subMethods) {
						subJDTMethods.add(ObjectCreationHelper.createMethodFromIMethod(subMethod));
					}
					if (superJDTMethod != null && subJDTMethods != null) {
						GlobalData.interfaceAndSuperMethods.put(superJDTMethod, subJDTMethods);
					}
				}
			}
		}
	}
}
