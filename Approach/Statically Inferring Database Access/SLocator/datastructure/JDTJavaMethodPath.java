package SLocator.datastructure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JDTJavaMethodPath {
	private List<JDTJavaMethod> methodList = new ArrayList<JDTJavaMethod>();
	
	// record all the methods which have been used to expand path
	private Set<JDTJavaMethod> expandedMethods = new HashSet<JDTJavaMethod>();
	
	public Set<JDTJavaMethod> getExpandedMethods() {
		return expandedMethods;
	}

	public void setExpandedMethods(Set<JDTJavaMethod> expandedMethods) {
		this.expandedMethods = expandedMethods;
	}

	public JDTJavaMethodPath() {
		
	}
	
	public List<JDTJavaMethod> getMethodList() {
		return methodList;
	}
	
	public void setMethodList(List<JDTJavaMethod> methodList) {
		this.methodList = methodList;
	}
	
	public void addMethodToPath(JDTJavaMethod method) {
		methodList.add(method);
	}
	
	public void addExpandedMethod(JDTJavaMethod method) {
		expandedMethods.add(method);
	}
	
	public void addExpandedMethods(Set<JDTJavaMethod> expandedMethods) {
		this.expandedMethods.addAll(expandedMethods);
	}
	
	public boolean containExpandedMethod(JDTJavaMethod method) {
		return expandedMethods.contains(method);
	}
}
