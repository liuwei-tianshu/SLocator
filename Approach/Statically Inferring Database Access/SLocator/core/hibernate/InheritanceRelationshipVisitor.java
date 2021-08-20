package SLocator.core.hibernate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import SLocator.datastructure.BiMap;
import SLocator.datastructure.JDTJavaClass;
import SLocator.datastructure.JDTJavaClass.JDTClassType;

/**
 * build Interfaces & Inheritance relationships
 */
public class InheritanceRelationshipVisitor extends ASTVisitor {

	// <interface, implementation classes>
	private Map<JDTJavaClass, List<JDTJavaClass>> interfaces;
	
	// <implementation class type, interface class type>
	private BiMap<JDTJavaClass, JDTJavaClass> superclasses;

	@Override
	public boolean visit(TypeDeclaration t) {
		if (t.resolveBinding() == null)
			return true;
		
		// return all interfaces directly implemented by this class.
		if (t.resolveBinding().getInterfaces() != null) {
			for (ITypeBinding binding : t.resolveBinding().getInterfaces()) {
				// interface
				JDTJavaClass parent = new JDTJavaClass(binding.getPackage()
						.getName(), binding.getJavaElement()
						.getElementName(), JDTClassType.JDT_INTERFACE);
				// child
				JDTJavaClass child = new JDTJavaClass(t.resolveBinding()
						.getPackage().getName(), t.resolveBinding()
						.getJavaElement().getElementName(), JDTClassType.JDT_CLASS);

				if (interfaces.containsKey(parent)) {
					interfaces.get(parent).add(child);
				} else {
					interfaces.put(parent, new ArrayList<JDTJavaClass>());
					interfaces.get(parent).add(child);
				}
			}
		}

		// direct superclass of this class is returned
		if (t.resolveBinding().getSuperclass() != null) {
			JDTJavaClass parent = new JDTJavaClass(t.resolveBinding()
					.getSuperclass().getPackage().getName(), t
					.resolveBinding().getSuperclass().getJavaElement()
					.getElementName(), JDTClassType.JDT_INTERFACE);

			// ignore classes whose super class is object
			if (parent.getClassName().equals("Object")) {
				return true;
			}

			JDTJavaClass child = new JDTJavaClass(t.resolveBinding()
					.getPackage().getName(), t.resolveBinding()
					.getJavaElement().getElementName(), JDTClassType.JDT_CLASS);

			superclasses.put(child, parent);
		}
		 
		return true;
	}

	public void setInterfaceMapForUpdate(
			Map<JDTJavaClass, List<JDTJavaClass>> interfaces) {
		this.interfaces = interfaces;
	}

	public void setSuperClassMapForUpdate(
			BiMap<JDTJavaClass, JDTJavaClass> superclasses) {
		this.superclasses = superclasses;
	}

	public Map<JDTJavaClass, List<JDTJavaClass>> getUpdatedInterfaceMap() {
		return interfaces;
	}

	public BiMap<JDTJavaClass, JDTJavaClass> getUpdatedSuperclassesMap() {
		return superclasses;
	}

}