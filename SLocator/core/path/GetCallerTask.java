package SLocator.core.path;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import SLocator.datastructure.JDTJavaMethod;
import SLocator.util.HierarchyUtil;
import SLocator.util.ObjectCreationHelper;


public class GetCallerTask implements Runnable{
	
	int index;
	IMethod sqlMethod;
	Set<JDTJavaMethod> sqlRelatedJDTMethods;
	
	public GetCallerTask(int index, IMethod sqlMethod, Set<JDTJavaMethod> sqlRelatedJDTMethods) {
		this.index = index;
		this.sqlMethod = sqlMethod;
		this.sqlRelatedJDTMethods = sqlRelatedJDTMethods;
	}
	
	public void run() {
		long methodStratTime = System.nanoTime();
		try {
			DFS(sqlMethod);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long seconds = (System.nanoTime() - methodStratTime) / 1000000 / 1000;
	}
	
	/**
	 * get all methods may call the root
	 * @param root
	 * @param visitedMethod
	 * @return
	 * @throws JavaModelException
	 */
	private void DFS(IMethod root) throws JavaModelException {
		JDTJavaMethod jDTJavaMethod = ObjectCreationHelper.createMethodFromIMethod(root);
		
		if (jDTJavaMethod.getMethodName().equals("getBlogById")) {
			System.out.println(root.getElementName());
		}
		
		if (sqlRelatedJDTMethods.contains(jDTJavaMethod)) {			// avoid unlimited loop
			//System.err.println("avoid unlimited loop, sqlRelatedMethods.size: " + sqlRelatedMethods.size());
			return;
		}
		
		// tag root method as sqlRelatedMethod
		sqlRelatedJDTMethods.add(jDTJavaMethod);
		
		// tag super methods of root method as sqlRelatedMethod
		List<IMethod> superMethods = HierarchyUtil.getSuperMethod(root);
		if (superMethods != null) {
			for (IMethod superMethod : superMethods) {
				sqlRelatedJDTMethods.add(ObjectCreationHelper.createMethodFromIMethod(superMethod));
			}
		}
		
		// TODO: need to check interface and supercalss
		List<IMethod> allCallerMethods = new ArrayList<IMethod>();
		
		/**
		 * get all callers
		 */
		List<IMethod> callerMethods = HierarchyUtil.getCallers(root);
		if (callerMethods != null) {
			allCallerMethods.addAll(callerMethods);
		}
		
		for (IMethod iMethod : allCallerMethods) {
			DFS(iMethod);		
		}
	}
}