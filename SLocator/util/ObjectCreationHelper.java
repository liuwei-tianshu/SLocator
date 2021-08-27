package SLocator.util;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import SLocator.datastructure.JDTJavaClass;
import SLocator.datastructure.JDTJavaMethod;
import SLocator.datastructure.JDTJavaVariable;
import SLocator.datastructure.Position;
import SLocator.datastructure.JDTJavaClass.JDTClassType;
import edu.cmu.cs.crystal.cfg.ICFGNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;

/**
 * Helper class for creating objects from ASTs.
 */

public class ObjectCreationHelper {

	public static JDTJavaMethod createMethodFromMethodInvocation(MethodDeclaration analyzedMethod,
			MethodInvocation invoke) {
		
		JDTJavaMethod sourceMethod = ObjectCreationHelper.createMethodFromMethodDeclaration(analyzedMethod);
		JDTJavaClass invokedClass = ObjectCreationHelper.createJavaClassFromMethodInvocation(sourceMethod, invoke);
		JDTJavaMethod invokedMethod = ObjectCreationHelper.createMethodFromMethodInvocation(invokedClass, invoke);
		String vars = JDTHelper.getParameters(invoke);
		invokedMethod.setPrams(vars.replace("\n", ""));
		return invokedMethod;
	}

	public static JDTJavaMethod createMethodFromMethodDeclaration(MethodDeclaration method) {
		JDTJavaMethod invokedMethod = null;	
		IMethodBinding iMethodBinding = method.resolveBinding();
		
		if (iMethodBinding == null) {
			// TODO
			System.err.println("exception");
		} else {
			List<SingleVariableDeclaration> variables = method.parameters();
			List<String> parameterTypes = new ArrayList<String>();
			if (variables != null) {
				for (SingleVariableDeclaration variable : variables) {
					String parameterType = variable.getType().toString().trim();
					if (parameterType.equals("T")) {
						parameterType = "Object";
					}
					
					parameterTypes.add(parameterType);
				}
			}
			
			if (method.getBody() == null) {
				invokedMethod = new JDTJavaMethod(iMethodBinding.getMethodDeclaration().getDeclaringClass().getPackage().getName(), 
						iMethodBinding.getMethodDeclaration().getDeclaringClass().getName(),
						iMethodBinding.getMethodDeclaration().getName(), parameterTypes, JDTClassType.JDT_INTERFACE);
			} else {
				invokedMethod = new JDTJavaMethod(iMethodBinding.getMethodDeclaration().getDeclaringClass().getPackage().getName(), 
						iMethodBinding.getMethodDeclaration().getDeclaringClass().getName(),
						iMethodBinding.getMethodDeclaration().getName(), parameterTypes, JDTClassType.JDT_CLASS);
			}
		}
		return invokedMethod;
	}

	public static JDTJavaClass createJavaClassFromCompilationUnit(CompilationUnit unit) {
		JDTJavaClass c = new JDTJavaClass(unit.getPackage().getName().toString(),
				unit.getJavaElement().getElementName().replace(".java", ""), JDTClassType.JDT_CLASS);

		return c;
	}
	
	public static JDTJavaClass createJavaClassFromICompilationUnit(ICompilationUnit unit) throws JavaModelException {
		JDTClassType jdtClassType = null;
		IType[] itypes = unit.getTypes(); 
		if (itypes != null && itypes.length >= 1) {
			if (unit.getTypes()[0].isInterface()) {
				jdtClassType = JDTClassType.JDT_INTERFACE;
			}
			if (unit.getTypes()[0].isClass()) {
				jdtClassType = JDTClassType.JDT_CLASS;
			}
			
			return new JDTJavaClass(unit.getPackageDeclarations()[0].getElementName(),
					unit.getElementName().split("\\.")[0], jdtClassType);
		} else {
			System.err.println("createJavaClassFromICompilationUnit error, unit: " + unit);
			return new JDTJavaClass(unit.getPackageDeclarations()[0].getElementName(),
					unit.getElementName().split("\\.")[0], JDTClassType.JDT_CLASS);
		}
	}

	public static JDTJavaClass createJavaClassFromVariableDeclaration(VariableDeclaration v) {
		// declared variable
		JDTJavaClass variableClass;
		try {
			if (v.resolveBinding() == null) {
				variableClass = new JDTJavaClass("", "", JDTClassType.JDT_CLASS);
			} else if (v.resolveBinding().getType() == null) {
				variableClass = new JDTJavaClass("", v.resolveBinding().getType().getName(), JDTClassType.JDT_CLASS);
			} else if (v.resolveBinding().getType().getPackage() == null) {
				variableClass = new JDTJavaClass("", v.resolveBinding().getType().getName(), JDTClassType.JDT_CLASS);
			} else {
				variableClass = new JDTJavaClass(v.resolveBinding().getType().getPackage().getName(),
						v.resolveBinding().getType().getName(), JDTClassType.JDT_CLASS);
			}
		} catch (NullPointerException ne) {
			variableClass = new JDTJavaClass("", "", JDTClassType.JDT_CLASS);
		}
		return variableClass;
	}

	/**
	 * Get the java class of the invoked method
	 * 
	 * @param invoke
	 * @return
	 */
	public static JDTJavaClass createJavaClassFromMethodInvocation(JDTJavaMethod callingMethod,
			MethodInvocation invoke) {
		JDTJavaClass invokedClass = null;
		JDTJavaClass callingClass = callingMethod.getJavaClass();

		if (invoke.getExpression() == null) {
			invokedClass = new JDTJavaClass(callingClass.getPackageName(), callingClass.getClassName(), JDTClassType.JDT_CLASS);
		}
		// Calling a function within the same class
		else if (invoke.getExpression().resolveTypeBinding() == null
				|| invoke.getExpression().resolveTypeBinding().getPackage() == null) {
			invokedClass = new JDTJavaClass(callingClass.getPackageName(), callingClass.getClassName(), JDTClassType.JDT_CLASS);
		} else {
			String className = invoke.getExpression().resolveTypeBinding().getName();
			if (className.contains("<")) {
				className = className.split("<")[0];
			}

			invokedClass = new JDTJavaClass(invoke.getExpression().resolveTypeBinding().getPackage().getName(),
					className, JDTClassType.JDT_CLASS);
		}

		return invokedClass;
	}

	/**
	 * Get the java class of the invoked method
	 * 
	 * @param invoke
	 * @return
	 */
	public static JDTJavaClass createJavaClassFromMethodInvocation(JDTJavaClass callingClass, MethodInvocation invoke) {
		JDTJavaClass invokedClass = null;

		if (invoke.getExpression() == null) {
			invokedClass = new JDTJavaClass(callingClass.getPackageName(), callingClass.getClassName(), JDTClassType.JDT_CLASS);
		}

		// Calling a function within the same class
		else if (invoke.getExpression().resolveTypeBinding() == null
				|| invoke.getExpression().resolveTypeBinding().getPackage() == null) {
			invokedClass = new JDTJavaClass(callingClass.getPackageName(), callingClass.getClassName(), JDTClassType.JDT_CLASS);
		} else {
			String className = invoke.getExpression().resolveTypeBinding().getName();
			if (className.contains("<")) {
				className = className.split("<")[0];
			}

			invokedClass = new JDTJavaClass(invoke.getExpression().resolveTypeBinding().getPackage().getName(),
					className, JDTClassType.JDT_CLASS);
		}

		return invokedClass;
	}

	/**
	 * Get the java class of the invoked method
	 * 
	 * @param invoke
	 * @return
	 */
	public static JDTJavaClass createJavaClassFromMethodDeclaration(MethodDeclaration method) {

		JDTJavaClass methodLocation;
		if (method.resolveBinding() == null || method.resolveBinding().getDeclaringClass() == null) {
			methodLocation = new JDTJavaClass("", "",JDTClassType.JDT_CLASS);
		} else if (method.resolveBinding().getDeclaringClass().getPackage() == null) {
			methodLocation = new JDTJavaClass("", method.resolveBinding().getDeclaringClass().getName(),JDTClassType.JDT_CLASS);
		} else {
			methodLocation = new JDTJavaClass(method.resolveBinding().getDeclaringClass().getPackage().getName(),
					method.resolveBinding().getDeclaringClass().getName(),JDTClassType.JDT_CLASS);
		}

		return methodLocation;
	}

	public static JDTJavaMethod createMethodFromMethodInvocation(JDTJavaClass invokedClass, MethodInvocation invoke) {
		// get the method that is invoked
		JDTJavaMethod invokedMethod = null;
		IMethodBinding iMethodBinding = invoke.resolveMethodBinding();

		if (iMethodBinding == null) {
			System.err.println("exception in createMethodFromMethodInvocation, invoke: " + invoke.toString());
		} else {
			List<String> parametersTypes = getSimpleParameters(iMethodBinding.toString());
			invokedMethod = new JDTJavaMethod(iMethodBinding.getMethodDeclaration().getDeclaringClass().getPackage().getName(), 
					iMethodBinding.getMethodDeclaration().getDeclaringClass().getName(),
					iMethodBinding.getMethodDeclaration().getName(), parametersTypes, JDTClassType.JDT_CLASS);
		}

		return invokedMethod;
	}

	public static JDTJavaVariable createInvokedVariableFromMethodInvocation(MethodInvocation invoke,
			JDTJavaClass invokedClass) {
		JDTJavaVariable invokedVar = null;
		if (invoke.getExpression() == null) {
			invokedVar = new JDTJavaVariable(invokedClass, "");
		} else {
			invokedVar = new JDTJavaVariable(invokedClass, invoke.getExpression().toString());
		}

		Position varPosition = new Position(invoke.getStartPosition(), invoke.getStartPosition() + invoke.getLength());
		invokedVar.setPosition(varPosition);

		return invokedVar;
	}

	/**
	 * Return null if it is not a variable
	 * 
	 * @param returnStm
	 * @return
	 */
	public static JDTJavaVariable createReturnedVariable(ReturnStatement returnStm) {
		if (returnStm.getExpression() instanceof Name) {
			Name var = (Name) returnStm.getExpression();
			IBinding binding = var.resolveBinding();
			// System.out.println(binding.);
			String fullName = binding.toString().split(" ")[0];
			String className = fullName.split("\\.")[fullName.split("\\.").length - 1];
			String packageName = fullName.replace("." + className, "");
			JDTJavaClass returnClassType = new JDTJavaClass(packageName, className, JDTClassType.JDT_CLASS);
			String varName = var.toString();
			JDTJavaVariable v = new JDTJavaVariable(returnClassType, varName);
			v.setPosition(new Position(var.getStartPosition(), var.getStartPosition() + var.getLength()));
			return v;
		}
		return null;
	}
	
	/**
	 * @param iMethod
	 * @return
	 * @throws JavaModelException
	 */
	public static JDTJavaMethod createMethodFromIMethod(IMethod iMethod) {
		if (iMethod == null) {
			return null;
		}
		/**
		 * added
		 */
		if (!(iMethod instanceof org.eclipse.jdt.internal.core.SourceMethod)) {
//			System.err.println("createMethodFromIMethod, !(iMethod instanceof org.eclipse.jdt.internal.core.SourceMethod) iMethod:" 
//					+ iMethod.getClass() + ",iMethod:" + iMethod);
			return null;
		}
		if (iMethod.getCompilationUnit() == null) {
			System.err.println("createMethodFromIMethod, iMethod.getCompilationUnit() == nulliMethod:" 
					+ iMethod.getClass() + ",iMethod:" + iMethod);
			return null;
		}
		
		IPackageDeclaration[] iPackageDeclaration = null;
		IType[] iType = null;
		try {
			iPackageDeclaration = iMethod.getCompilationUnit().getPackageDeclarations();
			iType = iMethod.getCompilationUnit().getAllTypes();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("error in createMethodFromIMethod iMethod: " + iMethod.toString());
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("error in createMethodFromIMethod iMethod: " + iMethod.toString());
		}
		
		if (iPackageDeclaration == null || iPackageDeclaration.length == 0
				|| iType == null || iType.length == 0) {
			return null;
		}
		
		// calculate interface or class
		
		
		String[] parameters = iMethod.getParameterTypes();	//	[Visit]   [Owner]   [int]  [DataSource, OwnerRepository, VisitRepository]  [List<Owner>]
		List<String> parameterTypes = new ArrayList<String>();
		if (parameters != null) {
			for(String str : parameters) {
				String parameterType = Signature.toString(str).trim();
				if (parameterType.equals("T")) {
					parameterType = "Object";
				}
				
				parameterTypes.add(parameterType);
			}
		}
		return new JDTJavaMethod(iPackageDeclaration[0].getElementName(), iType[0].getElementName(), iMethod.getElementName(), parameterTypes, JDTClassType.JDT_CLASS);
	}
	
	public static Set<JDTJavaMethod> createMethodSetFromIMethods(Set<IMethod> sqlRelatedMethods) {
		Set<JDTJavaMethod> result = ConcurrentHashMap.newKeySet();
		for (IMethod iMethod : sqlRelatedMethods) {
			JDTJavaMethod jDTJavaMethod = createMethodFromIMethod(iMethod);
			if (jDTJavaMethod != null)
				result.add(jDTJavaMethod);
		}
		return result;
	}
	
	public static JDTJavaMethod createMethodFromICFGNode(ICFGNode<ASTNode> node) {
		JDTJavaMethod invokedMethod = null;
		IMethodBinding iMethodBinding = null;
		if (node.getASTNode() instanceof MethodInvocation){
			MethodInvocation methodInvocation = (MethodInvocation) node.getASTNode();
			iMethodBinding = methodInvocation.resolveMethodBinding();
		} else if (node.getASTNode() instanceof SuperMethodInvocation){
			SuperMethodInvocation methodInvocation = (SuperMethodInvocation) node.getASTNode();
			iMethodBinding = methodInvocation.resolveMethodBinding();
		} else {
			return null;
		}

		if (iMethodBinding == null) {
			String nodeASTStr = node.getASTNode().toString();
		} else {
			List<String> parametersTypes = getSimpleParameters(iMethodBinding.toString());
			invokedMethod = new JDTJavaMethod(iMethodBinding.getMethodDeclaration().getDeclaringClass().getPackage().getName(), 
					iMethodBinding.getMethodDeclaration().getDeclaringClass().getName(),
					iMethodBinding.getMethodDeclaration().getName(), parametersTypes, JDTClassType.JDT_CLASS);
		}
		return invokedMethod; 
	}
	
	public static JDTJavaMethod createMethodFromSuperICFGNode(ICFGNode<ASTNode> node) {
		if (!(node.getASTNode() instanceof SuperMethodInvocation)){
			return null;
		}
		
		JDTJavaMethod invokedMethod = null;
		SuperMethodInvocation methodInvocation = (SuperMethodInvocation) node.getASTNode();
		IMethodBinding iMethodBinding = methodInvocation.resolveMethodBinding();

		if (iMethodBinding == null) {
			String nodeASTStr = node.getASTNode().toString();
		} else {   
			List<String> parametersTypes = getSimpleParameters(iMethodBinding.toString());
			invokedMethod = new JDTJavaMethod(iMethodBinding.getMethodDeclaration().getDeclaringClass().getPackage().getName(), 
					iMethodBinding.getMethodDeclaration().getDeclaringClass().getName(),
					iMethodBinding.getMethodDeclaration().getName(), parametersTypes, JDTClassType.JDT_CLASS);
		}
		return invokedMethod; 
	}
	
	/**
	 * 
	 * @param methodStr like 
	 * 	public abstract boolean add(org.springframework.samples.petclinic.model.Pet)
	 * 	public org.springframework.samples.petclinic.model.Pet getPet(java.lang.String, boolean) 
	 * 	
	 * @return
	 * 	Pet
	 * 	String, boolean
	 */
	public static List<String> getSimpleParameters(String methodStr){
		try {
			int beginIndex = methodStr.indexOf("(");
			int endIndex = methodStr.indexOf(")");
			String paraStr = methodStr.substring(beginIndex + 1, endIndex);
			String[] parameters = paraStr.split(",");
			List<String> result = new ArrayList<String>();
			for(String parameter : parameters) {
				result.add(Signature.getSimpleName(parameter).trim());
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Reads a ICompilationUnit and creates the AST DOM for manipulating the Java
	 * source file
	 * 
	 * @param unit
	 * @return
	 */

	public static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS9);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		return (CompilationUnit) parser.createAST(null); 	// parse
	}


}
