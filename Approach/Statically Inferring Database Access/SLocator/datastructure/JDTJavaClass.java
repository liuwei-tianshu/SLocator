package SLocator.datastructure;

import org.eclipse.jdt.core.ICompilationUnit;

import com.google.gson.annotations.Expose;

import lombok.Getter;
import lombok.Setter;

public class JDTJavaClass {
	@Getter@Expose private String packageName = "";			//just package name
	@Getter@Expose private String className = "";				//without ".java"
	@Getter@Setter@Expose private JDTClassType classType = JDTClassType.JDT_CLASS;
	@Getter@Setter private ICompilationUnit unit;
	@Getter private String fullNameWithOutJava = "";	//packageName+"."+className
	
	public enum JDTClassType {
		JDT_CLASS("jdt_normal_class"),
		JDT_INTERFACE("jdt_interface"),
		JDT_UNKNOWN("jdt_unknown");
		
		@Getter private String type;
		
		JDTClassType(){
			
		}
		
		JDTClassType(String type) {
			this.type = type;
		}
	}
	
	/**
	 * the gson will call it
	 */
	public JDTJavaClass() {
		
	}
	
	public JDTJavaClass(String packageName, String className) {
		this.packageName = packageName;
		this.className = className;
		this.fullNameWithOutJava = packageName+"." + className;
	}
	
	public JDTJavaClass(String packageName, String className, ICompilationUnit unit, JDTClassType classType) {
		this.packageName = packageName;
		this.className = className;
		this.classType = classType;
		this.fullNameWithOutJava = packageName+"." + className;
		this.unit = unit;
	}
	
	public JDTJavaClass(String packageName, String className, JDTClassType classType) {
		this.packageName = packageName;
		this.className = className;
		this.classType = classType;
		this.fullNameWithOutJava = packageName+"." + className;
	}
	
	public JDTJavaClass(String packageName, String className, JDTClassType classType, ICompilationUnit unit) {
		this.packageName = packageName;
		this.className = className;
		this.classType = classType;
		this.unit = unit;
		this.fullNameWithOutJava = packageName+"." + className;
	}
	
	public boolean equals(Object other) {
		JDTJavaClass otherClass = (JDTJavaClass) other;
		if (this.packageName.equals(otherClass.getPackageName())
				&& this.className.equals(otherClass.getClassName()))
			return true;
		return false;
	}

	public int hashCode() {
		return this.toString().hashCode();
	}
	
	public String toString(){
		if (this.packageName.equals(""))
			return this.className;
		return this.packageName + "." + this.className;
	}
}
