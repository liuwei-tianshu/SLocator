package SLocator.util;

import java.util.ArrayList;

import com.google.gson.annotations.Expose;

import SLocator.datastructure.JDTJavaMethod;

/**
 * 
 */
public class InterfaceSuperClass{
	@Expose private JDTJavaMethod interfaceSuper;
	@Expose private ArrayList<JDTJavaMethod> implementedSub;
	
	/**
	 * @param interfaceSuper
	 * @param implementedSub
	 */
	public InterfaceSuperClass(JDTJavaMethod interfaceSuper, ArrayList<JDTJavaMethod> implementedSub) {
		super();
		this.interfaceSuper = interfaceSuper;
		this.implementedSub = implementedSub;
	}
	/**
	 * @return the interfaceSuper
	 */
	public JDTJavaMethod getInterfaceSuper() {
		return interfaceSuper;
	}
	/**
	 * @param interfaceSuper the interfaceSuper to set
	 */
	public void setInterfaceSuper(JDTJavaMethod interfaceSuper) {
		this.interfaceSuper = interfaceSuper;
	}
	/**
	 * @return the implementedSub
	 */
	public ArrayList<JDTJavaMethod> getImplementedSub() {
		return implementedSub;
	}
	/**
	 * @param implementedSub the implementedSub to set
	 */
	public void setImplementedSub(ArrayList<JDTJavaMethod> implementedSub) {
		this.implementedSub = implementedSub;
	}
}
