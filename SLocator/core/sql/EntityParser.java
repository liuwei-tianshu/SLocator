package SLocator.core.sql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import SLocator.GlobalData;
import SLocator.datastructure.JDTEntityClass;
import SLocator.datastructure.JDTEntityField;
import lombok.Getter;

public class EntityParser {
	public static Set<JDTEntityClass> converToEntityClassSet(List<CompilationUnit> compilationUnitList) throws RuntimeException {
		Set<JDTEntityClass> entityClassSet = new HashSet<JDTEntityClass>();
		compilationUnitList.forEach(unit -> {
			JDTEntityClass jdtEntityClass = converToEntityClass(unit);
			if (jdtEntityClass != null) {
				entityClassSet.add(jdtEntityClass);
			}
		});
		return entityClassSet;
	}
	
	public static JDTEntityClass converToEntityClass(CompilationUnit compilationUnit) throws RuntimeException {
		ClassVisitor classVisitor = new ClassVisitor();
		compilationUnit.accept(classVisitor);
		if (classVisitor.isEntityClass() && classVisitor.getTableName() != null) {
			String className = null;
			if (compilationUnit.getJavaElement() != null) {
				className = compilationUnit.getJavaElement().getElementName().replace(".java", "");
			} else {
				TypeDeclaration typeDeclaration = (TypeDeclaration) compilationUnit.types().get(0);		// when load a test case java file as str, there is no java element
				className = typeDeclaration.getName().getIdentifier();
			}
			return new JDTEntityClass(compilationUnit.getPackage().getName().toString(), className, classVisitor.getTableName(), classVisitor.getEntityFieldList());
		}
		return null;
	}
	
	public static class ClassVisitor extends ASTVisitor {
		@Getter private boolean entityClass;
		@Getter private List<JDTEntityField> entityFieldList = new ArrayList<JDTEntityField>();
		@Getter private String tableName;
		
		public boolean visit(FieldDeclaration node) {
			// to construct a entity field
			String fieldName = null;
			String columnName = null;
			boolean primaryKey = false;
			String className = null; 				// TODO simple className, need to be varified by import 
			String hibernateAssociation = null;		// @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
			
			// get field name
			List list = node.fragments();
			VariableDeclarationFragment variableFragment = (VariableDeclarationFragment) list.get(0);
			fieldName = variableFragment.getName().getIdentifier();
			className = node.getType().toString();
			
			/**
			 * get field annotation
			 * @Column(name = "MODULE_CONFIG_ID")
			 * @Id
			 */
			/**
			 * modifers
			 * example1:
			 * [@Column(name="visit_date"), @Type(type="org.jadira.usertype.dateandtime.joda.PersistentDateTime"), @DateTimeFormat(pattern="yyyy/MM/dd"), private]
			 * 
			 * exmaple2:
			 * [@Column(name="address"), @NotEmpty, private]
			 * 
			 * example3:
			 * [@ManyToMany(fetch=FetchType.EAGER), @JoinTable(name="vet_specialties",joinColumns=@JoinColumn(name="vet_id"),inverseJoinColumns=@JoinColumn(name="specialty_id")), private]
			 */
			List modifers = node.modifiers();
			for (int i = 0; i < modifers.size(); i++) {
				Object modifer = modifers.get(i);
				
				/**
				 * annotations except @Id
				 */
				if (modifer instanceof NormalAnnotation) {
					NormalAnnotation normalAnnotation = (NormalAnnotation) modifer;
					
					/**
					 * @Column
					 */
					if (normalAnnotation.getTypeName().toString().equals("Column")) {
						String name = getAnnotationName(normalAnnotation);
						if (name != null && !name.equals("")) {
							columnName = name;
						}
					}
					
					/**
					 * ManytoOne...
					 */
					if (GlobalData.hibernateAssociation.contains(normalAnnotation.getTypeName().toString())) {
						className = node.getType().toString();
						Type type = node.getType();
						if(type instanceof ParameterizedType) {
							ParameterizedType parameterizedType = (ParameterizedType)type;
							className = parameterizedType.typeArguments().get(0).toString();
						}
					}
				}
				
				/**
				 * @Id
				 */
				if (modifer instanceof MarkerAnnotation) {
					MarkerAnnotation markerAnnotation = (MarkerAnnotation) modifer;
					if (markerAnnotation.getTypeName().toString().equals("Id")) {
						primaryKey = true;
					}
					
					/**
					 * ManytoOne...
					 */
					if (GlobalData.hibernateAssociation.contains(markerAnnotation.getTypeName().toString())) {
						className = node.getType().toString();
						Type type = node.getType();
						if(type instanceof ParameterizedType) {
							ParameterizedType parameterizedType = (ParameterizedType)type;
							className = parameterizedType.typeArguments().get(0).toString();
						}
					}
				}			
			}
			
			// construct a entity field
			if (fieldName != null) {
				entityFieldList.add(new JDTEntityField(fieldName, columnName, primaryKey, className));
			}
			
			return true;
		}
		
		/**
		 * @Entity
		 */
		public boolean visit(MarkerAnnotation node) {
			//System.out.println("MarkerAnnotation: " + node);
			if (node.getTypeName().toString().equals("Entity")) {
				entityClass = true;
			}
			return true;
		}
		
		/**
		 * @Table(name = "BLC_MODULE_CONFIGURATION")
		 */
		public boolean visit(NormalAnnotation node) {
			if (node.getTypeName().toString().equals("Table")) {
				String name = getAnnotationName(node);
				if (name != null && !name.equals("")) {
					tableName = name;
				}
			}
			return true;
		}
		
		private String getAnnotationName(NormalAnnotation annotation) {
			List<MemberValuePair> values = annotation.values();
			for (int j = 0; j < values.size(); j++) {
				MemberValuePair member = values.get(j);
				if (member.getName().getIdentifier().equals("name")) {
					if (member.getValue() instanceof StringLiteral)
					return ((StringLiteral)member.getValue()).getLiteralValue();
				}
			}
			return null;
		}
	}
}
