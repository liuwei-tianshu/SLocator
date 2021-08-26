package SLocator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import SLocator.datastructure.JDTEntityClass;
import SLocator.datastructure.JDTJavaClass;
import SLocator.datastructure.JDTJavaMethod;
import SLocator.datastructure.JDTJavaVariable;
import SLocator.datastructure.Position;
import SLocator.datastructure.JDTJavaClass.JDTClassType;


public class JDTHelper {

	/**
	 * Find the nearest declared variable in which is invoked
	 * 
	 * @param invokedVar
	 * @param locallyDeclaredEntityVariables
	 * @return
	 */
	public static JDTJavaVariable findClosestVariableDeclaration(JDTJavaVariable invokedVar,
			List<JDTJavaVariable> locallyDeclaredEntityVariables) {
		JDTJavaVariable closestDeclaredVariable = null;

		for (JDTJavaVariable v : locallyDeclaredEntityVariables) {
			if (!v.equals(invokedVar))
				continue;

			if (closestDeclaredVariable == null)
				closestDeclaredVariable = v;
			else {
				if (v.getPosition().getStartPos() > closestDeclaredVariable.getPosition().getStartPos()) {
					closestDeclaredVariable = v;
				}
			}
		}
		return closestDeclaredVariable;
	}

	/**
	 * Get the parameters of invoked methods, separated by "-"
	 * 
	 * @param invoke
	 * @return
	 */
	public static String getParameters(MethodInvocation invoke) {
		StringBuilder vars = new StringBuilder();
		
		invoke.accept(new ASTVisitor() {
			public boolean visit(MethodInvocation i) {
				i.resolveMethodBinding();
				return true;
			}
		});

		
		for (Object o : invoke.arguments()) {
			JDTJavaVariable v = new JDTJavaVariable();

			Expression e = (Expression) o;
			String packageName = "";
			try {
				packageName = e.resolveTypeBinding().getPackage().getName();
			} catch (NullPointerException e2) {
			}
			
			// TODO 
			String cName = "";
			try {
				cName = e.resolveTypeBinding().getName();
			} catch (NullPointerException e2) {
				
			}

			JDTJavaClass c = new JDTJavaClass(packageName, cName, JDTClassType.JDT_CLASS);
			try {
				if (e.resolveConstantExpressionValue() == null) {
					v = new JDTJavaVariable(c, e.toString());
				} else {
					v = new JDTJavaVariable(c, e.resolveConstantExpressionValue().toString().replace('\n', ' '));
				}
				v.setStartPos(e.getStartPosition());
				v.setEndPos(e.getStartPosition() + e.getLength());

			} catch (NullPointerException e2) {
				e2.printStackTrace();
			}

			// for printing purposes
			if (vars.length() == 0)
				vars.append(v);
			else
				vars.append("-" + v);
		}
		return vars.toString();
	}

	public static String parseRESTAnnotation(String annotation) {
		if (annotation.contains("(value = ")) {
			// @RequestMapping(value = "/orders/", ...
			Pattern p = Pattern.compile("value = (.*?),\n");
			Matcher m = p.matcher(annotation);
			if (m.find()) {
				String url = m.group(1);
				if (url.contains("+"))
					url.replace("+", "");
				url = url.replace("\"", "");
				// System.err.println("URL1 " + url);
				return url;
			}
		} else {
			// @RequestMapping("/register")
			// @RequestMapping("/" + NullGiftCardController.GATEWAY_CONTEXT_KEY)
			annotation = annotation.replace(" ", "");
			Pattern p = Pattern.compile("RequestMapping\\((.*?)\\)");
			Matcher m = p.matcher(annotation);

			if (m.find()) {
				StringBuilder finalURL = new StringBuilder();
				Pattern regex = Pattern.compile("[^\\+\"']+|\"([^\"]*)\"|'([^']*)'");
				Matcher regexMatcher = regex.matcher(m.group(1));
				while (regexMatcher.find()) {
					if (regexMatcher.group(1) != null) {
						// Add double-quoted string without the quotes
						finalURL.append(regexMatcher.group(1));
					} else {
						// Add unquoted word
						finalURL.append("{" + regexMatcher.group() + "}");
					}
				}

				return finalURL.toString();
			}
		}

		return "";
	}

	public static List<String> getClassLevelAnnotation(ICompilationUnit icu) {
		ArrayList<String> annotations = new ArrayList<String>();

		try {
			if (icu.getTypes().length > 0)
				// find out if the class is annotated with @Entity - means they are database
				// entity class
				for (IAnnotation a : icu.getTypes()[0].getAnnotations()) {
//					System.out.println(a.getSource());
					annotations.add(a.getSource());//a.getElementName().toString());
				}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return annotations;
	}

	public static List<JDTJavaVariable> getMethodParametersFromMethodDeclaration(MethodDeclaration method) {
		final List<JDTJavaVariable> parameters = new ArrayList<JDTJavaVariable>();
		// get parameter types
		for (Object parm : method.parameters()) {

			SingleVariableDeclaration v = (SingleVariableDeclaration) parm;
			JDTJavaClass variableClass = ObjectCreationHelper.createJavaClassFromVariableDeclaration(v);

			JDTJavaVariable var = new JDTJavaVariable(variableClass, v.getName().toString());

			Position varPosition = new Position(v.getStartPosition(), v.getStartPosition() + v.getLength());
			var.setPosition(varPosition);

			parameters.add(var);
		}

		return parameters;
	}

	/**
	 * Invoked method is the method called in MethodInvocation. Implementation is
	 * when parsing the implementation of the invoked method. Check if the invoked
	 * method is the same as the implemented method.
	 * 
	 * @param invoked
	 * @param implementation
	 * @return
	 */
	public static boolean sameMethod(JDTJavaMethod invoked, JDTJavaMethod implementation,
			Map<JDTJavaClass, JDTJavaClass> superclasses, Map<JDTJavaClass, List<JDTJavaClass>> interfaces) {
		if (!invoked.getMethodName().equals(implementation.getMethodName()))
			return false;

		JDTJavaClass invokedC = new JDTJavaClass(invoked.getPackageName(), invoked.getClassName(), invoked.getJavaClass().getClassType());
		JDTJavaClass implementedC = new JDTJavaClass(implementation.getPackageName(), implementation.getClassName(), implementation.getJavaClass().getClassType());

		if (invokedC.equals(implementedC))
			return true;

		// invoked is Owner.getFirstName, method implemented is
		// Person.getFirstName
		while (superclasses.containsKey(invokedC)) {
			JDTJavaClass parentClass = superclasses.get(invokedC);
			if (parentClass.equals(implementedC)) {
				return true;
			}
			invokedC = parentClass;
		}

		if (superclasses.containsKey(invokedC) && superclasses.get(invokedC).equals(implementedC)) {
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

	public static String getReturnType(MethodDeclaration method) {
		try {
			return method.getReturnType2().toString();
		} catch (NullPointerException e) {
			return "";
		}
	}

	public static String getReturnType(MethodInvocation method, CompilationUnit cu) {
		try {

			if (cu == null || method.resolveMethodBinding() == null) {
				// the declaring node may be null for calling subclass from
				// abstract
				// class
				return null;
			} else {

				if (cu.findDeclaringNode(method.resolveMethodBinding().getKey()) instanceof MethodDeclaration) {

					MethodDeclaration decl = (MethodDeclaration) cu
							.findDeclaringNode(method.resolveMethodBinding().getKey());

					return decl.getReturnType2().toString();
				}
			} // not MethodDeclaration

		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Finds the compilation unit and retrieves the line number of the given node.
	 * 
	 * @param node
	 * @return
	 */
	public static int getLineNumber(ASTNode node) {
		/*
		 * This gives us the starting character position. We need to map this into the
		 * line number using: int
		 * org.eclipse.jdt.core.dom.CompilationUnit.getLineNumber(int position)
		 */
		if (node == null)
			return -1;
		int characterPosition = node.getStartPosition();
		int line = -1;

		/* Search for the compilation unit. */
		ASTNode current = node;
		do {
			current = current.getParent();
		} while (current.getParent() != null && node.getNodeType() != ASTNode.COMPILATION_UNIT);

		/* Have we found a compilation unit? */
		if (ASTNode.COMPILATION_UNIT == current.getNodeType()) {
			CompilationUnit compUnit = (CompilationUnit) current;

			/* Now print the line number. */
			line = compUnit.getLineNumber(characterPosition);
		}
		return line;
	}
	
	/**
	 * convert CompilationUnit (entityUnit) to JDTEntityClass
	 * @param entityUnit entityUnit is already an EntityClass
	 * @return
	 */
	public JDTEntityClass convertToJDTEntityClass(CompilationUnit entityUnit) {
		JDTEntityClass JDTEntityClass = new JDTEntityClass(entityUnit.getPackage().getName().getFullyQualifiedName(), entityUnit.getTypeRoot().getElementName());
		
		
		return JDTEntityClass;
	}
}
