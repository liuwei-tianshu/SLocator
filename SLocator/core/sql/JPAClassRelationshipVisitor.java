package SLocator.core.sql;

import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import SLocator.datastructure.JDTEntityClass;
import SLocator.datastructure.JDTJavaClass;
import SLocator.util.ObjectCreationHelper;
import lombok.Getter;

/**
 * get entity relationship by annotation (OneToMany) and the type of the Class
 */
public class JPAClassRelationshipVisitor extends ASTVisitor {

	private JDTJavaClass currentlyAnalyzedClass;
	private PrintWriter pw;
//	private Set<JDTEntityClass> entityClasses;
	@Getter
	private List<String> relationshipResult;
	
	public JPAClassRelationshipVisitor(Set<JDTEntityClass> entityClasses, CompilationUnit unit,
			List<String> relationshipResult) {
		currentlyAnalyzedClass = ObjectCreationHelper.createJavaClassFromCompilationUnit(unit);
//		this.entityClasses = entityClasses;
		this.relationshipResult = relationshipResult;
		
	}

	public boolean visit(VariableDeclarationFragment v) {
		analyzeAnnotatedVariable(v);
		return true;
	}

	public boolean visit(SingleVariableDeclaration v) {
		analyzeAnnotatedVariable(v);
		return true;
	}

	// parse getOrderLines @OneToMany(targetEntity = OrderLine.class, fetch = public
	// static final javax.persistence.FetchType LAZY, mappedBy = item)
	String parseTargetEntityRegex = "targetEntity\\s*=\\s*(\\S+).class";
	Pattern pattern = Pattern.compile(parseTargetEntityRegex);

	public boolean visit(MethodDeclaration method) {

		String relationship = null;
		boolean eager = false;
		String cascadeType = "";
		String targetClass = "";

		for (IAnnotationBinding a : method.resolveBinding().getAnnotations()) {
			String annotationString = a.toString();

			Matcher matcher = pattern.matcher(annotationString);
			while (matcher.find()) {
				targetClass = matcher.group(1);
//				System.out.println("TARGET CLASS " + targetClass);
			}

			if (annotationString.contains("OneToMany")) {
				relationship = "OneToMany";
				if (annotationString.contains("EAGER"))
					eager = true;
			}

			if (annotationString.contains("OneToOne")) {
				relationship = "OneToOne";
				// default is eager
				if (!annotationString.contains("LAZY"))
					eager = true;
			}

			if (annotationString.contains("ManyToOne")) {
				relationship = "ManyToOne";
				// default is eager
				if (!annotationString.contains("LAZY"))
					eager = true;
			}

			if (annotationString.contains("ManyToMany")) {
				relationship = "ManyToMany";
				if (annotationString.contains("EAGER"))
					eager = true;
			}

			if (annotationString.contains("CascadeType")) {
				if (annotationString.contains("ALL")) {
					cascadeType += "All";
				}

				if (annotationString.contains("DETACH")) {
					cascadeType += "Detach";
				}

				if (annotationString.contains("MERGE")) {
					cascadeType += "Merge";
				}

				if (annotationString.contains("PERSIST")) {
					cascadeType += "Persist";
				}

				if (annotationString.contains("REFRESH")) {
					cascadeType += "Refresh";
				}

				if (annotationString.contains("REMOVE")) {
					cascadeType += "Remove";
				}
			}
			if (cascadeType.equals("")) {
				cascadeType = "None";
			}

		}

		if (relationship != null) {
			if (cascadeType.equals("")) {
				cascadeType = "None";
			}

//			if (entityClasses.contains(currentlyAnalyzedClass)) {
//				System.out.println(relationship + ", " + this.currentlyAnalyzedClass + ", " + targetClass + ", " + eager
//						+ ", " + cascadeType);
				relationshipResult.add(relationship + "|" + this.currentlyAnalyzedClass + "|" + targetClass + "|" + eager
						+ "|" + cascadeType);
//			} else {
//				//System.err.println(currentlyAnalyzedClass + " is not one of the enetity classes");
//			}
		}

		// System.out.println(method.getName() + " " + annotation);
		return true;

	}

	private void analyzeAnnotatedVariable(VariableDeclaration v) {

		JDTJavaClass annotatedVarClass = ObjectCreationHelper.createJavaClassFromVariableDeclaration(v);

		String relationship = null;
		boolean eager = false;
		String cascadeType = "";

		for (IAnnotationBinding a : v.resolveBinding().getAnnotations()) {
			String annotationString = a.toString();
			// System.out.println(annotationString);

			// TODO: need to consider lazy and eager. Sometimes if it is lazy, it may or may
			// not contain a join.

			// TODO: print out relationship,source,sink,eagerOrLazy,cascadeType

			// TODO: need to think about if inheritance will affect entity classes, e.g.,
			// parents are entity classes and the children are automatically entity classes

			if (annotationString.contains("OneToMany")) {
				relationship = "OneToMany";
				if (annotationString.contains("EAGER"))
					eager = true;
			}

			if (annotationString.contains("OneToOne")) {
				relationship = "OneToOne";
				// default is eager
				if (!annotationString.contains("LAZY"))
					eager = true;
			}

			if (annotationString.contains("ManyToOne")) {
				relationship = "ManyToOne";
				// default is eager
				if (!annotationString.contains("LAZY"))
					eager = true;
			}

			if (annotationString.contains("ManyToMany")) {
				relationship = "ManyToMany";
				if (annotationString.contains("EAGER"))
					eager = true;
			}

			if (annotationString.contains("CascadeType")) {
				if (annotationString.contains("ALL")) {
					cascadeType += "All";
				}

				if (annotationString.contains("DETACH")) {
					cascadeType += "Detach";
				}

				if (annotationString.contains("MERGE")) {
					cascadeType += "Merge";
				}

				if (annotationString.contains("PERSIST")) {
					cascadeType += "Persist";
				}

				if (annotationString.contains("REFRESH")) {
					cascadeType += "Refresh";
				}

				if (annotationString.contains("REMOVE")) {
					cascadeType += "Remove";
				}
			}

//			System.out.println("annotation string " + annotationString);
		}

		if (relationship != null) {
			if (cascadeType.equals("")) {
				cascadeType = "None";
			}

//			if (entityClasses.contains(currentlyAnalyzedClass)) {
//				System.out.println(relationship + "," + currentlyAnalyzedClass + "," + annotatedVarClass + "," + eager
//						+ "," + cascadeType);
				relationshipResult.add(relationship + "|" + currentlyAnalyzedClass + "|" + annotatedVarClass + "|"
						+ eager + "|" + cascadeType);
//			} else {
//				//System.err.println(currentlyAnalyzedClass + " is not one of the enetity classes");
//			}
		}
	}
}
