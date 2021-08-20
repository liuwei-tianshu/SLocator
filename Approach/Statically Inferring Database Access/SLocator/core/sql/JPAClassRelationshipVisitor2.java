package SLocator.core.sql;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import SLocator.core.hibernate.HibernateRelationship;
import SLocator.core.hibernate.HibernateRelationship.RelationShipType;
import SLocator.datastructure.JDTJavaClass;
import SLocator.util.ObjectCreationHelper;
import lombok.Getter;


/**
 * get entity relationship by annotation (OneToMany) and the type of the Class
 */
public class JPAClassRelationshipVisitor2 extends ASTVisitor {

	private JDTJavaClass currentlyAnalyzedClass;
	private PrintWriter pw;
	@Getter private	List<String> relationshipResult;
	@Getter private List<HibernateRelationship> hibernateRelationshipResult;
	
	public JPAClassRelationshipVisitor2(CompilationUnit unit) {
		this.currentlyAnalyzedClass = ObjectCreationHelper.createJavaClassFromCompilationUnit(unit);
		this.relationshipResult = new ArrayList<String>();
		this.hibernateRelationshipResult = new ArrayList<HibernateRelationship>();
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
		RelationShipType relationShipType = null;

		for (IAnnotationBinding a : method.resolveBinding().getAnnotations()) {
			String annotationString = a.toString();

			Matcher matcher = pattern.matcher(annotationString);
			while (matcher.find()) {
				targetClass = matcher.group(1);
//				System.out.println("TARGET CLASS " + targetClass);
			}

			if (annotationString.contains("OneToMany")) {
				relationship = "OneToMany";
				relationShipType = RelationShipType.ONE_TO_MANY;
				if (annotationString.contains("EAGER"))
					eager = true;
			}

			if (annotationString.contains("OneToOne")) {
				relationship = "OneToOne";
				relationShipType = RelationShipType.ONE_TO_ONE;
				// default is eager
				if (!annotationString.contains("LAZY"))
					eager = true;
			}

			if (annotationString.contains("ManyToOne")) {
				relationship = "ManyToOne";
				relationShipType = RelationShipType.MANY_TO_ONE;
				// default is eager
				if (!annotationString.contains("LAZY"))
					eager = true;
			}

			if (annotationString.contains("ManyToMany")) {
				relationship = "ManyToMany";
				relationShipType = RelationShipType.MANY_TO_MANY;
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

//			System.out.println(relationship + ", " + this.currentlyAnalyzedClass + ", " + targetClass + ", " + eager
//					+ ", " + cascadeType);
			relationshipResult.add(relationship + "|" + this.currentlyAnalyzedClass + "|" + targetClass + "|" + eager
					+ "|" + cascadeType);
			hibernateRelationshipResult.add(new HibernateRelationship(relationShipType, this.currentlyAnalyzedClass.toString(), targetClass));

		}

		return true;

	}

	private void analyzeAnnotatedVariable(VariableDeclaration v) {

		JDTJavaClass annotatedVarClass = ObjectCreationHelper.createJavaClassFromVariableDeclaration(v);

		String relationship = null;
		boolean eager = false;
		String cascadeType = "";
		RelationShipType relationShipType = null;

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
				relationShipType = RelationShipType.ONE_TO_MANY;
				if (annotationString.contains("EAGER"))
					eager = true;
			}

			if (annotationString.contains("OneToOne")) {
				relationship = "OneToOne";
				relationShipType = RelationShipType.ONE_TO_ONE;
				// default is eager
				if (!annotationString.contains("LAZY"))
					eager = true;
			}

			if (annotationString.contains("ManyToOne")) {
				relationship = "ManyToOne";
				relationShipType = RelationShipType.MANY_TO_ONE;
				// default is eager
				if (!annotationString.contains("LAZY"))
					eager = true;
			}

			if (annotationString.contains("ManyToMany")) {
				relationship = "ManyToMany";
				relationShipType = RelationShipType.MANY_TO_MANY;
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
		}

		if (relationship != null) {
			if (cascadeType.equals("")) {
				cascadeType = "None";
			}

//			System.out.println(relationship + "," + currentlyAnalyzedClass + "," + annotatedVarClass + "," + eager
//					+ "," + cascadeType);
			relationshipResult.add(relationship + "|" + currentlyAnalyzedClass + "|" + annotatedVarClass + "|"
					+ eager + "|" + cascadeType);
			hibernateRelationshipResult.add(new HibernateRelationship(relationShipType, currentlyAnalyzedClass.toString(), annotatedVarClass.toString()));
		}
	}
}
