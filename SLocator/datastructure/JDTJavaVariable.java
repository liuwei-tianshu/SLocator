package SLocator.datastructure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import SLocator.datastructure.JDTJavaClass.JDTClassType;

public class JDTJavaVariable {
	// private String type;

	private JDTJavaClass type;
	private String name;
	private ArrayList<String> annotations = new ArrayList<String>();
	private Position position;

	private Set<JDTJavaVariable> associatedEagerVariables;

	private int startPos = 0;
	private int endPos = 0;

	private int nthParameter;

	private JDTJavaMethod declaredMethod = null; // where is this variable declared

	int weight = 0;

	public JDTJavaVariable(JDTJavaClass type, String name) {
		this.type = type;
		this.name = name;
	}

	public JDTJavaVariable(JDTJavaClass type, String name, JDTJavaMethod declaredMethod) {
		this.type = type;
		this.name = name;
		this.declaredMethod = declaredMethod;
	}

	/**
	 * Copy constructor
	 * 
	 * @param v
	 */
	public JDTJavaVariable(JDTJavaVariable v) {
		type = new JDTJavaClass(v.getType().getPackageName(), v.getType()
				.getClassName(), JDTClassType.JDT_CLASS);
		name = v.getName();
		for (String annotation : v.getAnnotations()) {
			addAnnotation(annotation);
		}
	}

	public JDTJavaVariable(String type, String name) {
		// this.type = type;
		// this.name = name;
	}

	public JDTJavaVariable() {
		// TODO Auto-generated constructor stub
	}

	public JDTJavaClass getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addAnnotation(String annotation) {
		this.annotations.add(annotation);
	}

	public ArrayList<String> getAnnotations() {
		return this.annotations;
	}

	public void setAssociatedVariables(Set<JDTJavaVariable> associatedEagerVariables) {
		this.associatedEagerVariables = new HashSet<JDTJavaVariable>();
		for (JDTJavaVariable v : associatedEagerVariables) {
			this.associatedEagerVariables.add(new JDTJavaVariable(v));
		}
	}

	public void setPosition(Position position){
		this.position = position;
	}
	
	public Position getPosition(){
		return this.position;
	}
	
	public Set<JDTJavaVariable> getAssociatedVariables() {
		return this.associatedEagerVariables;
	}

	public void setStartPos(int startPos) {
		this.startPos = startPos;
	}

	public void setEndPos(int endPos) {
		this.endPos = endPos;
	}

	public void setDeclaredMethod(JDTJavaMethod declaredMethod) {
		this.declaredMethod = declaredMethod;
	}

	public int getStartPos() {
		return this.startPos;
	}

	public int getEndPos() {
		return this.endPos;
	}

	public JDTJavaMethod getDeclaredMethod() {
		return this.declaredMethod;
	}

	/**
	 * start counting from 0
	 * 
	 * @param n
	 */
	public void setParameterOrder(int n) {
		this.nthParameter = n;
	}

	/**
	 * start counting from 0
	 * 
	 * @param n
	 */
	public int getParameterOrder() {
		return this.nthParameter;
	}

	@Override
	public boolean equals(Object B) {
		JDTJavaVariable toCompare = (JDTJavaVariable) B;

		if (toCompare.declaredMethod == null || this.declaredMethod == null) {
			if (this.type.equals(toCompare.type)
					&& this.name.equals(toCompare.name)) {
				return true;
			}

			// the variable was from a function call (e.g., fA(user.getName()))
			if (this.type.equals(toCompare.type)
					&& (this.name.equals("") || toCompare.name.equals(""))
					&& this.getParameterOrder() == toCompare
							.getParameterOrder()) {
				return true;
			}
		}

		if (toCompare.declaredMethod != null && this.declaredMethod != null) {
			if (this.type.equals(toCompare.type)
					&& this.name.equals(toCompare.name)
					&& this.declaredMethod.equals(toCompare.declaredMethod)) {
				return true;
			}

			// the variable was from a function call (e.g., fA(user.getName()))
			if (this.type.equals(toCompare.type)
					&& (this.name.equals("") || toCompare.name.equals(""))
					&& this.declaredMethod.equals(toCompare.declaredMethod)
					&& this.getParameterOrder() == toCompare
							.getParameterOrder()) {
				return true;
			}
		}

		return false;
	}

	@Override
	public int hashCode() {
		return (type + "(val)" + name).hashCode();
	}

	@Override
	public String toString() {
		if (declaredMethod == null)
			return type + "(val)" + name;
		else
			return declaredMethod.toString() + "|" + type + "|" + name;
	}
}
