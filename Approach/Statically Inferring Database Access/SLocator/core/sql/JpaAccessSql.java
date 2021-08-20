package SLocator.core.sql;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeLiteral;

import SLocator.core.hibernate.JDTEntityHibernateUtil;
import SLocator.datastructure.JDTEntityClass;
import SLocator.datastructure.JDTJavaClass;
import SLocator.datastructure.ProgramWorkspace;
import SLocator.datastructure.JDTJavaClass.JDTClassType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


public class JpaAccessSql {
	@Getter String classFileContent = "";
	@Getter CompilationUnit classUnit = null;
	@Getter Map<MethodDeclaration, List<String>> sqlListForClass = new HashMap<MethodDeclaration, List<String>>();
	@Setter Map<String, List<String>> sqlMap = null;
	Map<String, Long> emFunctionNumber;
	Set<JDTEntityClass> jdtEntityClassSet;
	ProgramWorkspace programWorkspace = null;
	
	public JpaAccessSql(Map<String, List<String>> sqlMap, ProgramWorkspace programWorkspace) {
		this.sqlMap = sqlMap;
		this.jdtEntityClassSet = programWorkspace.getEntityClasses();
		this.programWorkspace = programWorkspace;
	}
	
	protected CompilationUnit parse(String str) {
		ASTParser parser = ASTParser.newParser(AST.JLS9);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(str.toCharArray());
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		return (CompilationUnit) parser.createAST(null);
	}
	
	@Data
	private class ClassVisitor extends ASTVisitor {
		// all methods in this class
		List<MethodDeclaration> methodList = new ArrayList<MethodDeclaration>();
		List<String> importList = new ArrayList<String>();
		
		/**
		 * in jpa, it will use EntityManager to access database
		 * 
		 * like this:
		 * 		    private EntityManager em;
		 */
		@Getter FieldDeclaration entityManager = null;

		public boolean visit(MethodDeclaration node) {
			methodList.add(node);
			return super.visit(node);
		}
		
		public boolean visit(FieldDeclaration node) {
			//System.out.println("field: " + node);
			if (node.getType().toString().equals("EntityManager")) {
				entityManager = node;
			}
			return true;
		}
		
		public boolean visit(ImportDeclaration node) {
			importList.add(node.getName().toString());
			return true;
		}
	}
	
	@Data
	private class MethodVisitor extends ASTVisitor {	
		FieldDeclaration entityManager;
		Map<String, List<String>> sqlMap;
		Map<MethodDeclaration, List<String>> sqlList = new HashMap<MethodDeclaration, List<String>>();
		MethodDeclaration methodDeclaration;
		Map<String, Long> emFunctionNumber;
		List<String> importList = new ArrayList<String>();	// like org.broadleafcommerce.core.rating.domain.RatingSummaryImpl
		Set<JDTEntityClass> jdtEntityClassSet;
		Set<JDTJavaClass> jdtJavaClassSet;
		ProgramWorkspace programWorkspace;
		
		public MethodVisitor(FieldDeclaration entityManager, MethodDeclaration methodDeclaration, Map<String, List<String>> sqlMap, 
				Map<String, Long> emFunctionNumber, List<String> importList, Set<JDTEntityClass> jdtEntityClassSet, ProgramWorkspace programWorkspace){
			this.entityManager = entityManager;
			this.methodDeclaration = methodDeclaration;
			this.sqlMap = sqlMap;
			this.emFunctionNumber = emFunctionNumber;
			this.importList = importList;
			this.jdtEntityClassSet = jdtEntityClassSet;
			this.jdtJavaClassSet = programWorkspace.getJavaClassSet();
			this.programWorkspace = programWorkspace;
		}
		
		/**
		 * for example:
		 * node: this.namedParameterJdbcTemplate.queryForObject("SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE id= :id",params,BeanPropertyRowMapper.newInstance(Owner.class)), 
		 * method name: queryForObject, 
		 * method expression: this.namedParameterJdbcTemplate
		 */
		public boolean visit(MethodInvocation node) {
			String entityManagerName = getVariableName(entityManager);
			if (entityManagerName == null) {
				System.err.println("entityManagerName == null:" + node);
				return false;
			}
			
			if (node.getExpression() == null) {
				//System.err.println("node.getExpression() == null, node:" + node);
				return false;
			}
			String nodeExpression = node.getExpression().toString();
			
			/**
			 * private EntityManager em;
			 */
			Object parameter = null;
			if (entityManager != null && (nodeExpression.equals(entityManagerName) || nodeExpression.equals("this." + entityManagerName))) {
				switch(node.getName().toString()) {
					case "createQuery":
						parameter = node.arguments().get(0);
						if (parameter instanceof StringLiteral) {
							String sql = ((StringLiteral)parameter).getLiteralValue();
							List<String> fullSqls = JDTEntityHibernateUtil.getHibernateFullSqls(sql, jdtEntityClassSet);
							addSqls(fullSqls);
						} else if (parameter instanceof InfixExpression
								&& ((InfixExpression)parameter).getLeftOperand() instanceof StringLiteral
								&& ((InfixExpression)parameter).getRightOperand() instanceof StringLiteral) {
							/**
							 * it is like:
							 * 
							 *  this.namedParameterJdbcTemplate.update(
			                    "UPDATE owners SET first_name=:firstName, last_name=:lastName, address=:address, " +
			                            "city=:city, telephone=:telephone WHERE id=:id",
			                    parameterSource);
							 */
							Expression left = ((InfixExpression)parameter).getLeftOperand();
							Expression right = ((InfixExpression)parameter).getRightOperand();
							addSql(((StringLiteral)left).getLiteralValue() + ((StringLiteral)right).getLiteralValue());
						} else {
							System.err.println("error in sql access createQuery parameter, node: " + node.toString());
						}
						addNumber("createQuery");
						break;
						
					/**
					 * like:
					 * 	 Query query = em.createNamedQuery("BC_READ_DEFAULT_CURRENCY");
					 */
					case "createNamedQuery":
						parameter = node.arguments().get(0);
						if (parameter instanceof StringLiteral) {
							String strParameter = ((StringLiteral)parameter).getLiteralValue();
							List<String> sqls = sqlMap.get(strParameter);
							if (sqls == null) {
								System.err.println("error in sql access createNamedQuery, no strParameter in the sqlMap. strParameter: " + strParameter + ", node: " + node.toString());
							} else {
								addSqls(sqls);
							}
						}
						addNumber("createNamedQuery");
						break;
						
					case "find":
						addNumber("find");
						parameter = node.arguments().get(0);	// RatingSummaryImpl.class
						if (!(parameter instanceof TypeLiteral)) {
							System.err.println("error in sql access find node: " + node.toString());
							return false;
						} 
						
						JDTEntityClass jdtEntityClass = getJDTEntityClass(((TypeLiteral)parameter).toString());
						if (jdtEntityClass == null) {
							System.err.println("error in sql access find no jdtEntityClass node: " + node.toString());
							return false;
						}
						String sql = JDTEntityHibernateUtil.getHibernateJoinSql(jdtEntityClass, jdtEntityClassSet);
						addSql(sql);
						break;
						
					case "remove":
						addNumber("remove");
						parameter = node.arguments().get(0);
						JDTEntityClass jdtEntityClassRemove = getEntityClassForParameter((SimpleName)parameter);
						if (jdtEntityClassRemove == null) {
							System.err.println("error in sql access remove no jdtEntityClass node: " + node.toString());
							return false;
						}
						addSql(jdtEntityClassRemove.getRemoveSql());
						break;
						
					case "persist":
						/**
						 * there are two situations
						 * 
						 * 1 Object type of interface
						 * OrderLock ol = (OrderLock) entityConfiguration.createEntityInstance(OrderLock.class.getName());
                		 * em.persist(ol);
                		 * 
                		 * 2 object is type of class
                		 * 
						 */
						addNumber("persist");
						parameter = node.arguments().get(0);
						JDTEntityClass jdtEntityClassPersist = getEntityClassForParameter((SimpleName)parameter);
						if (jdtEntityClassPersist == null) {
							System.err.println("error in sql access persist no jdtEntityClass node: " + node.toString());
							return false;
						}
						addSql(jdtEntityClassPersist.getPersistSql());
						break;
						
					/**
					 * this.em.merge(owner);
					 * Hibernate: select owner0_.id as id1_0_1_, owner0_.first_name as first_na2_0_1_, owner0_.last_name as last_nam3_0_1_, owner0_.address as address4_0_1_, owner0_.city as city5_0_1_, owner0_.telephone as telephon6_0_1_, pets1_.owner_id as owner_id4_0_3_, pets1_.id as id1_1_3_, pets1_.id as id1_1_0_, pets1_.name as name2_1_0_, pets1_.birth_date as birth_da3_1_0_, pets1_.owner_id as owner_id4_1_0_, pets1_.type_id as type_id5_1_0_ from owners owner0_ left outer join pets pets1_ on owner0_.id=pets1_.owner_id where owner0_.id=?
					   Hibernate: select pettype0_.id as id1_3_0_, pettype0_.name as name2_3_0_ from types pettype0_ where pettype0_.id=?
					   Hibernate: select visits0_.pet_id as pet_id4_1_0_, visits0_.id as id1_6_0_, visits0_.id as id1_6_1_, visits0_.visit_date as visit_da2_6_1_, visits0_.description as descript3_6_1_, visits0_.pet_id as pet_id4_6_1_ from visits visits0_ where visits0_.pet_id=?
					   Hibernate: update owners set first_name=?, last_name=?, address=?, city=?, telephone=? where id=?
					 */
					case "merge":
						parameter = node.arguments().get(0);
						JDTEntityClass jdtEntityClassMerge = null;
						if (parameter instanceof org.eclipse.jdt.core.dom.SimpleName) {
							jdtEntityClassMerge = getEntityClassForParameter((SimpleName)parameter);
							if (jdtEntityClassMerge == null) {
								System.err.println("error in sql access merge no jdtEntityClass node: " + node.toString());
								return false;
							}
						} else {
							System.err.println("error in sql access merge no jdtEntityClass node, parameter not SimpleName" + node.toString() 
								+ ", parameter type:" + parameter.getClass());
							return false;
						}
						
						addSqls(JDTEntityHibernateUtil.getHibernateMergeFullSqls(jdtEntityClassMerge, jdtEntityClassSet));
						addNumber("merge");
						break;
					default:
						System.err.println("error in sql access func, exception in em method node: " + node.toString());
				}
			}
			
			return true;
		}
		
		private JDTEntityClass getEntityClassForParameter(SimpleName parameter) {
			String parameterName = parameter.toString();
			ASTNode parent = parameter.getParent();
			while (!(parent instanceof MethodDeclaration)) {
				parent = parent.getParent();
			}
			
			/**
			 * get the class name as string
			 * 
			 * get the string"OrderLock"
			 *   OrderLock ol = (OrderLock) 
			 */
			String block = parent.toString();
			String regEx = "\\b\\w+\\b(?=\\s" + parameterName + "\\b)" ;	// like this
			Pattern pattern = Pattern.compile(regEx);
			Matcher matcher = pattern.matcher(block);
			String className = "";
			if (matcher.find()) {
				className = matcher.group();
			} else {
				System.err.println("error in getClassOfParameter parameter: " 
						+ parameter + ", parameter.parent: " + parameter.getParent());
				System.err.println("block:" + block);
				return null;
			}
			
			/**
			 * get the real class
			 * 1 this class is a class
			 * 
			 * 2 this class is an interface
			 * TODO need change: should use reflect to get the sqls
			 */
			JDTJavaClass jdtJavaClass = getJDTJavaClass(className);
			if (jdtJavaClass == null) {
				System.err.println("error in getEntityClassForParameter(SimpleName parameter) parameter:" + className + "parameter.parent: " + parameter.getParent() + ",interface: " + jdtJavaClass);
				return null;
			}
			if (jdtJavaClass.getClassType() == JDTClassType.JDT_CLASS) {
				return getJDTEntityClass(jdtJavaClass);
			} else if (jdtJavaClass.getClassType() == JDTClassType.JDT_INTERFACE) {
				Map<JDTJavaClass, List<JDTJavaClass>> interfaces = programWorkspace.getInterfaces();
				if (interfaces != null && interfaces.get(jdtJavaClass) != null) {
					JDTJavaClass implementClass = interfaces.get(jdtJavaClass).get(0);
					return getJDTEntityClass(implementClass);
				} else {
					System.err.println("error in getClassOfParameter interfaces.get(jdtJavaClass) != null, parameter: " + parameter 
							+ ", parameter.parent: " + parameter.getParent() + ",interface: " + jdtJavaClass);
					return null;
				}
			}
			
			return null;
		}
		
		private JDTEntityClass getJDTEntityClass(JDTJavaClass jdtJavaClass) {
	        for(JDTEntityClass jdtEntityClass: jdtEntityClassSet){
	        	String fullPackage = jdtEntityClass.getPackageName() + "." + jdtEntityClass.getClassName();
	        	if (fullPackage.equals(jdtJavaClass.toString())) {
	        		return jdtEntityClass;
	        	}
	        }
	        return null;
		}
		
		private JDTJavaClass getJDTJavaClass(String className) {
			if (className == null) {
				System.err.println("");
				return null;
			}
			String packageName = null;
			for (int j = 0; j< importList.size(); j++) {
				String importStr = importList.get(j);
				String[] strs = importStr.split("\\.");
				if (strs[strs.length-1].equals(className)) {
					packageName = importStr;	//org.broadleafcommerce.core.rating.domain.RatingSummaryImpl
				}
			}
			if (packageName == null) {
				System.err.println("can not get package name");
				return null;
			} else {
		        for(JDTJavaClass jdtJavaClass: jdtJavaClassSet){
		        	String fullPackage = jdtJavaClass.getPackageName() + "." + jdtJavaClass.getClassName();
		        	if (fullPackage.equals(packageName)) {
		        		return jdtJavaClass;
		        	}
		        }
			}
			return null;
		}
		
		private JDTEntityClass getJDTEntityClass(String parameter) {
			String className = (parameter.toString().split("\\.")[0]);
			String packageName = null;
			for (int j = 0; j< importList.size(); j++) {
				String importStr = importList.get(j);
				String[] strs = importStr.split("\\.");
				if (strs[strs.length-1].equals(className)) {
					packageName = importStr;	//org.broadleafcommerce.core.rating.domain.RatingSummaryImpl
				}
			}
			if (packageName == null) {
				System.err.println("can not get package name");
				return null;
			} else {
		        for(JDTEntityClass jdtEntityClass: jdtEntityClassSet){
		        	String fullPackage = jdtEntityClass.getPackageName() + "." + jdtEntityClass.getClassName();
		        	if (fullPackage.equals(packageName)) {
		        		return jdtEntityClass;
		        	}
		        }
			}
			return null;
		}
		
		private void addNumber(String methodName) {
			if (emFunctionNumber.get(methodName) == null) {
				emFunctionNumber.put(methodName, 1L);
			} else {
				emFunctionNumber.put(methodName, emFunctionNumber.get(methodName) +1);
			}
		}
		
		/**
		 * normally, namedParameterJdbcTemplate is like this:
    	 * private EntityManager em;
		 * 
		 * but sometimes it could have several variables like this:
    	 * private EntityManager em, var1, var2;
		 * @return
		 */
		private String getVariableName(FieldDeclaration fieldDeclaration) {
			if (fieldDeclaration == null) {
				System.err.println("fieldDeclaration is null");
				return null;
			}
			List list = fieldDeclaration.fragments();
			if (list == null || list.size() != 1) {
				System.err.println("fieldDeclaration is wrong in size");
				return null;
			}
			return list.get(0).toString();
		}
		
		private void addSql(String str) {
			if (sqlList.get(methodDeclaration) != null) {
				sqlList.get(methodDeclaration).add(str);
			} else {
				List<String> strs = new ArrayList<String>();
				strs.add(str);
				sqlList.put(methodDeclaration, strs);
			}
		}
		
		private void addSqls(List<String> sqls) {
			if (sqls != null) {
				sqls.forEach(sql -> {
					addSql(sql);
				});
			}
		}
	}
	
	@Data
	private class AnnotationMethodVisitor extends ASTVisitor {	
		MethodDeclaration methodDeclaration;
		Map<MethodDeclaration, List<String>> sqlList = new HashMap<MethodDeclaration, List<String>>();
		
		public AnnotationMethodVisitor(MethodDeclaration methodDeclaration){
			this.methodDeclaration = methodDeclaration;
		}
		
		public boolean visit(SingleMemberAnnotation node) {
			switch(node.getTypeName().toString()) {
			case "org.springframework.data.jpa.repository.Query":
			case "Query":
				Expression ex = node.getValue();
				if (ex instanceof StringLiteral) {
					addSql(((StringLiteral)ex).getLiteralValue());
				}
				break;
			}
			return true;
		}
		
		private void addSql(String str) {
			if (sqlList.get(methodDeclaration) != null) {
				sqlList.get(methodDeclaration).add(str);
			} else {
				List<String> strs = new ArrayList<String>();
				strs.add(str);
				sqlList.put(methodDeclaration, strs);
			}
		}
	}
	
	public Map<MethodDeclaration, List<String>> calculateSqls(String filePath, Map<String, Long> emFunctionNumber) {
		// read file content into a string
		try {
			classFileContent = new String(Files.readAllBytes(Paths.get(filePath)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		classUnit = parse(classFileContent);
		this.emFunctionNumber = emFunctionNumber;
		run();
		return sqlListForClass;
	}
	
	public Map<MethodDeclaration, List<String>> calculateSqls(CompilationUnit cu, Map<String, Long> emFunctionNumber) {
		classUnit = cu;
		this.emFunctionNumber = emFunctionNumber;
		run();
		return sqlListForClass;
	}
	
	private void run(){
		ClassVisitor classVisitor = new ClassVisitor();
		classUnit.accept(classVisitor);
		FieldDeclaration entityManager = classVisitor.getEntityManager();
		List<MethodDeclaration> methodDeclarationList = classVisitor.getMethodList();
		
		/**
		 * get the annotation sql of a method
		 */
		for (MethodDeclaration methodDeclaration : methodDeclarationList) {
			
			AnnotationMethodVisitor annotationMethodVisitor = new AnnotationMethodVisitor(methodDeclaration);
			methodDeclaration.accept(annotationMethodVisitor);
			
			// store sqls
			Map<MethodDeclaration, List<String>> sqlList = annotationMethodVisitor.getSqlList();
			if (sqlList != null && !sqlList.isEmpty()) {
				sqlListForClass.putAll(sqlList);
			}
		}
		
		/**
		 * get the inside sql of a method
		 */
		if (entityManager != null) {
			for (MethodDeclaration methodDeclaration : methodDeclarationList) {
				MethodVisitor methodVisitor = new MethodVisitor(entityManager, methodDeclaration, sqlMap, emFunctionNumber, classVisitor.getImportList(), jdtEntityClassSet, programWorkspace);
				methodDeclaration.accept(methodVisitor);
				
				// store sqls
				Map<MethodDeclaration, List<String>> sqlList = methodVisitor.getSqlList();
				if (sqlList != null && !sqlList.isEmpty()) {
					sqlListForClass.putAll(sqlList);
				}
			}
		}
	}
	
	public static void main(String args[]) {
		String block = "public void delete(Order salesOrder) {\r\n" + 
				"        if (!em.contains(salesOrder)) {\r\n" + 
				"            salesOrder = readOrderById(salesOrder.getId());\r\n" + 
				"        }\r\n" + 
				"\r\n" + 
				"        //need to null out the reference to the Order for all the OrderPayments\r\n" + 
				"        //as they are not deleted but Archived.\r\n" + 
				"        for (OrderPayment payment : salesOrder.getPayments()) {\r\n" + 
				"            payment.setOrder(null);\r\n" + 
				"            payment.setArchived('Y');\r\n" + 
				"            for (PaymentTransaction transaction : payment.getTransactions()) {\r\n" + 
				"                transaction.setArchived('Y');\r\n" + 
				"            }\r\n" + 
				"        }\r\n" + 
				"\r\n" + 
				"        em.remove(salesOrder);\r\n" + 
				"    }";
		String regEx = "\\b\\w+\\b(?=\\s" + "salesOrder" + "\\b)" ;	// like this
		Pattern pattern = Pattern.compile(regEx);
		Matcher matcher = pattern.matcher(block);
		String className = "";
		if (matcher.find()) {
			className = matcher.group();
			System.out.println(className + matcher.groupCount());
		} else {
			
		}
	}
}
