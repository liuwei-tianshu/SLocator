package SLocator.datastructure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.gson.annotations.Expose;

import SLocator.datastructure.JDTJavaClass.JDTClassType;

public class JDTJavaMethod {

	@Expose private String packageName;
	@Expose private String className;
	@Expose private String methodName;
	@Expose private String params = "";
	@Expose private List<String> parameterTypes;
	
	@Expose private JDTJavaClass javaClass;
	// recored visit times in traverse
	@JSONField(serialize=false) private Long visitNum = 0L;	
	@JSONField(serialize=false) private String annotationInfo = "";
	@JSONField(serialize=false) private boolean isSqlRelated = false;
	@JSONField(serialize=false) private int callLevel = 0;
	
	// callerMethod
	@JSONField(serialize=false) private List<JDTJavaMethod> callerMethodList = new ArrayList<JDTJavaMethod>();
	@JSONField(serialize=false) private Set<JDTJavaMethod> callerMethodSet = new HashSet<JDTJavaMethod>();
	
	// callerMethod
	@JSONField(serialize=false) private Set<JDTJavaMethod> calleeMethodSet = new HashSet<JDTJavaMethod>();
	@JSONField(serialize=false) private List<JDTJavaMethod> calleeMethodList = new ArrayList<JDTJavaMethod>();
	
	// record all the methods which have been used to expand path
	@JSONField(serialize=false) private Set<JDTJavaMethod> expandedMethods = new HashSet<JDTJavaMethod>();

	/**
	 * if this method accesses database
	 * sqls is all the sqls it has
	 */
	
	@JSONField(serialize=false) private List<String> sqls = new ArrayList<String>();
	
	/**
	 * the gson will call it
	 */
	public JDTJavaMethod() {
		
	}
	
	public JDTJavaMethod(String packageName, String className, String methodName, JDTClassType classType) {
		this.packageName = packageName;
		this.className = className;
		this.methodName = methodName;
		this.javaClass = new JDTJavaClass(this.packageName, this.className, classType);
	}
	
	public JDTJavaMethod(String packageName, String className, String methodName, String params, JDTClassType classType) {
		this.packageName = packageName;
		this.className = className;
		this.methodName = methodName;
		this.params = params;
		this.javaClass = new JDTJavaClass(this.packageName, this.className, classType);
	}

	public JDTJavaMethod(String packageName, String className, String methodName, List<String> parameterTypes, JDTClassType classType) {
		this.packageName = packageName;
		this.className = className;
		this.methodName = methodName;
		this.parameterTypes = parameterTypes;
		this.javaClass = new JDTJavaClass(this.packageName, this.className, classType);
	}
	
	public void setJDTClassType(JDTClassType classType) {
		if(javaClass != null) {
			javaClass.setClassType(classType);
		}
	}
	
	public void setCallLevel(int callLevel) {
		this.callLevel = callLevel;
	}
	
	public int getCallLevel() {
		return this.callLevel;
	}
	
	/**
	 * @return the expandedMethods
	 */
	public Set<JDTJavaMethod> getExpandedMethods() {
		return expandedMethods;
	}

	/**
	 * @param expandedMethods the expandedMethods to set
	 */
	public void setExpandedMethods(Set<JDTJavaMethod> expandedMethods) {
		this.expandedMethods = expandedMethods;
	}
	
	public List<String> getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(List<String> parameterTypes) {
		this.parameterTypes = parameterTypes;
	}
	
	public JDTJavaMethod cloneSimpleMethod() {
		if (this.javaClass != null) {
			return new JDTJavaMethod(this.packageName, this.className, this.methodName, this.parameterTypes, this.javaClass.getClassType());
		} else {
			return new JDTJavaMethod(this.packageName, this.className, this.methodName, this.parameterTypes, JDTClassType.JDT_UNKNOWN);
		}
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

	public String getPackageName() {
		return this.packageName;
	}

	public String getClassName() {
		return this.className;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public JDTJavaClass getJavaClass() {
		return this.javaClass;
	}

	public String getPrams() {
		return this.params;
	}

	public void setPrams(String params) {
		this.params = params;
	}

	public void setAnnotationInfo(String annotationInfo) {
		this.annotationInfo = annotationInfo;
	}
	
	public String getAnnotationInfo() {
		return this.annotationInfo;
	}

	public String toString() {
		if (annotationInfo.equals(""))
			return packageName + "." + className + "." + methodName + getParameterStr();
		else
			return annotationInfo + "," + packageName + "." + className + "." + methodName + getParameterStr();

	}
	
	public String getFullNameWithoutParams() {
		return packageName + "." + className + "." + methodName;
	}
	public String getFullNameWithParams() {
		return packageName + "." + className + "." + methodName + getParameterStr();
	}

    @Override
	public int hashCode() {
		return (packageName + "." + className + "." + methodName).hashCode();
	}

	public String getParameterStr() {
		if (parameterTypes == null) {
			return "()";
		} else {
			return "(" + StringUtils.join(parameterTypes, ",") + ")";
		}
	}
	
	/**
	 * changed by liuwei, add parameters
	 */
    @Override
	public boolean equals(Object other) {
		JDTJavaMethod m = (JDTJavaMethod) other;
		if (this.getPackageName().equals(m.getPackageName()) && this.getClassName().equals(m.getClassName())
				&& this.getMethodName().equals(m.getMethodName()) 
				//&& this.getPrams().equals(m.getPrams())
				&& this.getParameterStr().equals(m.getParameterStr())
				)
			return true;
		return false;
	}

	/**
	 * invoked method is the method called in MethodInvocation. Implementation is
	 * when parsing the implementation of the invoked method
	 * 
	 * @param invoked
	 * @param implementation
	 * @return
	 */
	public static boolean same(JDTJavaMethod invoked, JDTJavaMethod implementation,
			BiMap<JDTJavaClass, JDTJavaClass> superclasses, Map<JDTJavaClass, List<JDTJavaClass>> interfaces) {
		if (!invoked.getMethodName().equals(implementation.getMethodName()))
			return false;

		JDTJavaClass invokedC = new JDTJavaClass(invoked.getPackageName(), invoked.getClassName(), JDTClassType.JDT_CLASS);
		JDTJavaClass implementedC = new JDTJavaClass(implementation.getPackageName(), implementation.getClassName(), JDTClassType.JDT_CLASS);

		if (invokedC.equals(implementedC))
			return true;

		// invoked is Owner.getFirstName, method implemented is
		// Person.getFirstName
		while (superclasses.containsChild(invokedC)) {
			JDTJavaClass parentClass = superclasses.getParent(invokedC);
			if (parentClass.equals(implementedC)) {
				return true;
			}
			invokedC = parentClass;
		}

		if (superclasses.containsChild(invokedC) && superclasses.getParent(invokedC).equals(implementedC)) {
			return true;
		}

		if (interfaces.containsKey(invokedC)) {
			for (JDTJavaClass subclass : interfaces.get(invokedC)) {
				if (subclass.equals(implementedC))
					return true;
			}
		}
		// invoked is Person.getFirstName

		return false;
	}
	
	public void visit() {
		visitNum++;
	}
	
	public Long getVisitNum() {
		return visitNum;
	}

	public void addCaller(JDTJavaMethod e) {
		callerMethodList.add(e);
		callerMethodSet.add(e);
	}
	
	public void addCallee(JDTJavaMethod e) {
		calleeMethodList.add(e);
		calleeMethodSet.add(e);
	}
	
	/**
	 * @return the callerMethodList
	 */
	public List<JDTJavaMethod> getCallerMethodList() {
		return callerMethodList;
	}

	/**
	 * @param callerMethodList the callerMethodList to set
	 */
	public void setCallerMethodList(List<JDTJavaMethod> callerMethodList) {
		this.callerMethodList = callerMethodList;
	}

	/**
	 * @return the callerMethodSet
	 */
	public Set<JDTJavaMethod> getCallerMethodSet() {
		return callerMethodSet;
	}

	/**
	 * @param callerMethodSet the callerMethodSet to set
	 */
	public void setCallerMethodSet(Set<JDTJavaMethod> callerMethodSet) {
		this.callerMethodSet = callerMethodSet;
	}

	/**
	 * @return the calleeMethodSet
	 */
	public Set<JDTJavaMethod> getCalleeMethodSet() {
		return calleeMethodSet;
	}

	/**
	 * @param calleeMethodSet the calleeMethodSet to set
	 */
	public void setCalleeMethodSet(Set<JDTJavaMethod> calleeMethodSet) {
		this.calleeMethodSet = calleeMethodSet;
	}

	/**
	 * @return the calleeMethodList
	 */
	public List<JDTJavaMethod> getCalleeMethodList() {
		return calleeMethodList;
	}

	/**
	 * @param calleeMethodList the calleeMethodList to set
	 */
	public void setCalleeMethodList(List<JDTJavaMethod> calleeMethodList) {
		this.calleeMethodList = calleeMethodList;
	}

	public boolean isSqlRealted() {
		return isSqlRelated;
	}
	
	public void setSqlRelated(boolean isSqlRelated) {
		this.isSqlRelated = isSqlRelated;
	}
	
	public void addAllSqs(List<String> sqls) {
		if (this.sqls == null && sqls != null) {
			this.sqls = new ArrayList<String>();
			this.sqls.addAll(sqls);
		}
		
		if (this.sqls != null && this.sqls.isEmpty() && sqls != null) {
			this.sqls.addAll(sqls);
		}
	}
	
	public List<String> getSqls() {
		return this.sqls;
	}
}
