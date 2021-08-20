package SLocator.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

/**
 */
public class HierarchyUtil {
	
	public static CallHierarchy callHierarchy = CallHierarchy.getDefault();
	
	/**
	 * get all callers of method
	 * 
	 * 1 interface
	 * 	 
	 * 
	 * 2 superclass
	 *
	 * @param method
	 * @return
	 * @throws JavaModelException
	 */
	public static List<IMethod> getCallers(IMethod method) throws JavaModelException {
		// get the default CallHierarchy object
		// CallHierarchy callHierarchy = CallHierarchy.getDefault();

		// use this CallHierarchy object to get the callee-root of the input IMethod
		IMember[] members = { method };
		MethodWrapper[] rootsWrappers = callHierarchy.getCallerRoots(members);
		Assert.isTrue(rootsWrappers != null && rootsWrappers.length == 1);
		MethodWrapper rootWrapper = rootsWrappers[0];

		// getCalls in turn returns an array of MethodWrapper objects and these are the
		// actual callees.
		List<IMethod> callees = new ArrayList<IMethod>();
		MethodWrapper[] calleesWrapper = rootWrapper.getCalls(new NullProgressMonitor());
		for (MethodWrapper calleeWrapper : calleesWrapper) {
			IMethod im = getIMethodFromMethodWrapper(calleeWrapper);
			if (im != null && !im.isBinary()) {
				callees.add(im);
			}
		}

		return callees;
	}
	
	public static List<IMethod> getCallees(IMethod method) throws JavaModelException {
		// get the default CallHierarchy object
		CallHierarchy callHierarchy = CallHierarchy.getDefault();

		// use this CallHierarchy object to get the callee-root of the input IMethod
		IMember[] members = { method };
		MethodWrapper[] rootsWrappers = callHierarchy.getCalleeRoots(members);
		Assert.isTrue(rootsWrappers != null && rootsWrappers.length == 1);
		MethodWrapper rootWrapper = rootsWrappers[0];

		// getCalls in turn returns an array of MethodWrapper objects and these are the
		// actual callees.
		List<IMethod> callees = new ArrayList<IMethod>();
		MethodWrapper[] calleesWrapper = rootWrapper.getCalls(new NullProgressMonitor());
		for (MethodWrapper calleeWrapper : calleesWrapper) {
			IMethod im = getIMethodFromMethodWrapper(calleeWrapper);
			if (im != null && !im.isBinary()) {
				callees.add(im);
			}
		}

		return callees;
	}

	private static IMethod getIMethodFromMethodWrapper(MethodWrapper m) {
		try {
			IMember im = m.getMember();
			if (im.getElementType() == IJavaElement.METHOD) {
				return (IMethod) m.getMember();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * get all the subMethod of iMethod
	 * 
	 * @param iMethod
	 * @return
	 * @throws JavaModelException
	 */
	public static List<IMethod> getSuperMethod(IMethod iMethod) throws JavaModelException {
		List<IMethod> res = new ArrayList<>();

		// get type
		ITypeRoot iTypeRoot = iMethod.getTypeRoot();
		IType itype = iTypeRoot.findPrimaryType();

		// get supertypes
		ITypeHierarchy iTypeHierarchy = itype.newTypeHierarchy(new NullProgressMonitor());
		IType[] superTypes = iTypeHierarchy.getSupertypes(itype);

		// the all the methods in all the supertypes
		for (IType superType : superTypes) {
			for (IMethod superMethod : superType.getMethods()) {
				if (isEqual(superMethod, iMethod) && !superMethod.isBinary()) {
					res.add(superMethod);
				}
			}
		}

		return res;
	}
	
	/**
	 * get all the subMethod of iMethod
	 * 
	 * cover:
	 * 1 implements
	 * 2 extends
	 * 
	 * @param iMethod
	 * @return
	 * @throws JavaModelException
	 */
	public static List<IMethod> getSubMethod(IMethod iMethod) throws JavaModelException {
		List<IMethod> res = new ArrayList<>();

		// get type
		ITypeRoot iTypeRoot = iMethod.getTypeRoot();
		IType itype = iTypeRoot.findPrimaryType();

		// get subtypes
		ITypeHierarchy iTypeHierarchy = itype.newTypeHierarchy(new NullProgressMonitor());
		IType[] subTypes = iTypeHierarchy.getSubtypes(itype);

		// the all the methods in all the subtypes
		for (IType subType : subTypes) {
			for (IMethod subMethod : subType.getMethods()) {
				if (isEqual(subMethod, iMethod) && !subMethod.isBinary()) {
					res.add(subMethod);
				}
			}
		}

		return res;
	}
	
	/**
	 * get all the subMethod of iMethod
	 * 
	 * cover:
	 * 1 implements
	 * 
	 * @param iMethod
	 * @return
	 * @throws JavaModelException
	 */
	public static List<IMethod> getImplementMethod(IMethod iMethod) throws JavaModelException {
		List<IMethod> res = new ArrayList<>();

		// get type
		ITypeRoot iTypeRoot = iMethod.getTypeRoot();
		IType itype = iTypeRoot.findPrimaryType();

		// get subtypes
		ITypeHierarchy iTypeHierarchy = itype.newTypeHierarchy(new NullProgressMonitor());
		IType[] subTypes = iTypeHierarchy.getSubclasses(itype);

		// the all the methods in all the subtypes
		for (IType subType : subTypes) {
			for (IMethod subMethod : subType.getMethods()) {
				if (isEqual(subMethod, iMethod)) {
					res.add(subMethod);
				}
			}
		}

		return res;
	}
	
	private static boolean isEqual(IMethod im1, IMethod im2) {
		if (im1 == null || im2 == null) {
			return false;
		}

		if (!im1.getElementName().equals(im2.getElementName())) {
			return false;
		}

		String[] types1 = im1.getParameterTypes();
		String[] types2 = im2.getParameterTypes();
		if (types1.length != types2.length) {
			return false;
		}

		for (int i = 0; i < types1.length; i++) {
			if (!types1[i].equals(types2[i])) {
				return false;
			}
		}

		return true;
	}
}
