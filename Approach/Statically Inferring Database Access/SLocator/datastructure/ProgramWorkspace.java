package SLocator.datastructure;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import SLocator.GlobalData;
import SLocator.core.hibernate.InheritanceRelationshipVisitor;
import SLocator.core.hibernate.RESTEntryPointVisitor;
import SLocator.core.sql.EntityParser;
import SLocator.core.sql.JPAClassRelationshipVisitor;
import SLocator.core.sql.JPAClassRelationshipVisitor2;
import SLocator.util.FileUtil;
import SLocator.util.JsonUtil;
import SLocator.util.ObjectCreationHelper;
import lombok.Data;


/**
 * ProgramWorkspace represents the whole program to be analyzed
 */
@Data
public class ProgramWorkspace {
	private Program program;
	private Long projectNumber = 0L;
	private Long classFileNumber = 0L;
	private List<IProject> projectList = new ArrayList<IProject>();
	
	// all CompilationUnit from the workspace
	List<CompilationUnit> compilationUnitList = new ArrayList<CompilationUnit>();
	
	// all ICompilationUnit from the workspace
	List<ICompilationUnit> icompilationUnitList = new ArrayList<ICompilationUnit>();
	
	Map<CompilationUnit, ICompilationUnit> unitRelationship = new HashMap<CompilationUnit, ICompilationUnit>();
	
	// all javaMethod from workspace
	private List<JDTJavaMethod> javaMethodList = new ArrayList<JDTJavaMethod>();
	
	// all javaMethod from workspace
	private Set<JDTJavaMethod> javaMethodSet = new HashSet<JDTJavaMethod>();
	
	// interface relationship, <interface, implementation classes>
	private Map<JDTJavaClass, List<JDTJavaClass>> interfaces = new HashMap<JDTJavaClass, List<JDTJavaClass>>();
	
	private BiMap<JDTJavaClass, JDTJavaClass> superclasses = new BiMap<JDTJavaClass, JDTJavaClass>();
	
	// the class annotated with @Entity
	private Set<JDTEntityClass> entityClasses = new HashSet<JDTEntityClass>();
	
	// entity relationship
	private List<String> classRelationshipResult = new ArrayList<String>(); 
	
	// the method annotated with @RequestMapping
	private List<JDTJavaMethod> requestMethodList = new ArrayList<JDTJavaMethod>();
	
	// all javaClass from workspace, some inner classes are not in it
	private Set<JDTJavaClass> javaClassSet = new HashSet<JDTJavaClass>();
	
	public ProgramWorkspace(Program program) {
		this.program = program;
	}
	
	private static List<String> filterProject = new ArrayList<String>();
	static {
		filterProject.add("BroadleafCommerce-broadleaf-6.0.3-GA");
		filterProject.add("DemoSite-broadleaf-6.0.3-GA");
		filterProject.add("admin");
		filterProject.add("boot-community-demo-admin");
		filterProject.add("BroadleafCommerce-broadleaf-6.0.3-GA");
		filterProject.add("BroadleafCommerce-broadleaf-6.0.3-GA");
		filterProject.add("BroadleafCommerce-broadleaf-6.0.3-GA");
	}
	
	/**
	 * traverse workspace to get compilationUnitList and icompilationUnitList
	 * @throws JavaModelException
	 * @throws CoreException
	 */
	public void traverseWorkspace() throws JavaModelException, CoreException {
		System.out.println("==================================");
		System.out.println("ProgramWorkspace.traverseWorkspace");
		long appStartTime = System.nanoTime();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects(); // projects:
		for (IProject project : projects) {
//			if (!project.getName().equals("boot-community-demo-site") && !project.getName().equals("broadleaf-framework-web")) {
//				continue;
//			}
			
//			if (project.getName().equals("boot-community-demo-admin") || project.getName().equals("boot-community-demo-api")
//					|| project.getName().equals("broadleaf-rest-api")
//					|| project.getName().equals("broadleaf-admin-functional-tests")
//					|| project.getName().equals("broadleaf-admin-module")
//					|| project.getName().equals("broadleaf-open-admin-platform")) {
//				continue;
//			}
		
//			System.out.println("project name: " + project.getName());

			
			if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
				projectNumber++;
				projectList.add(project);
				IPackageFragment[] packages = JavaCore.create(project).getPackageFragments();
				for (IPackageFragment mypackage : packages) {
					if (mypackage.getElementName().equals("eu.cloudscale.showcase.db.dao.hibernate.impl")) {
						System.out.print("debug");
					}
					
					if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
						
						/**
						 * debug code to filter packages
						 */
//						if (!mypackage.getElementName().contains("com.broadleafcommerce.rest.api.endpoint.catalog") &&
//								!mypackage.getElementName().contains("org.broadleafcommerce.core.catalog")) {
//							continue;
//						}
						
						for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
							if(unit.getElementName().equals("package-info.java")) {
								continue;
							}
												
							classFileNumber++;
							
							// icompilationUnitList
							icompilationUnitList.add(unit);
							
							// compilationUnitList
							CompilationUnit parsedUnit = ObjectCreationHelper.parse(unit);
							compilationUnitList.add(parsedUnit);
							
							// relationship between icompilationUnitList and compilationUnitList
							unitRelationship.put(parsedUnit, unit);
						}
					}
				}
			}
		}
		
		long seconds = (System.nanoTime() - appStartTime) / 1000000 / 1000;
		System.out.println(seconds + " seconds");
	}

	public void traverseToGetJavaClass() throws JavaModelException {
		System.out.println("==================================");
		System.out.println("ProgramWorkspace.traverseToGetJavaClass");
		long appStartTime = System.nanoTime();
		for (ICompilationUnit icompilationUnit : icompilationUnitList) {
			JDTJavaClass jdtJavaClass = ObjectCreationHelper.createJavaClassFromICompilationUnit(icompilationUnit);
			if(jdtJavaClass != null) {
				javaClassSet.add(jdtJavaClass);
			}
		}
		long seconds = (System.nanoTime() - appStartTime) / 1000000 / 1000;
		System.out.println(seconds + " seconds");
	}
	
	public void traverseToGetJavaMethod() throws JavaModelException {
		System.out.println("==================================");
		System.out.println("ProgramWorkspace.traverseToGetJavaMethod");
		long appStartTime = System.nanoTime();
		//System.out.println("package: " + mypackage.getElementName());
		for (CompilationUnit compilationUnit : compilationUnitList) {
			compilationUnit.accept(new ASTVisitor() {
				public boolean visit(MethodDeclaration method) {
					JDTJavaMethod javaMethod = ObjectCreationHelper.createMethodFromMethodDeclaration(method);
					
					/**
					 * debug
					 */
					if (javaMethod.getMethodName().equals("shrani")) {
						System.out.println(javaMethod);
					}
					
					if(javaMethod != null) {
						javaMethodList.add(javaMethod);
						javaMethodSet.add(javaMethod);
					}
					
					if(javaMethod.getClassName().equals("IDao")) {
						GlobalData.sqlRelatedMethods.add(javaMethod);
					}
					
					return true;
				}
			});
		}
		GlobalData.javaMethodList = javaMethodList;
		GlobalData.javaMethodSet =  javaMethodSet;
		
		JsonUtil.storeJDTMethod(javaMethodList);
		
		long seconds = (System.nanoTime() - appStartTime) / 1000000 / 1000;
		System.out.println(seconds + " seconds");
	}
	
	public void traverseToGetInterfaceAndSuperClass() {
		for (CompilationUnit compilationUnit : compilationUnitList) {			
			/**
			 * get all Interfaces & Inheritance relationships information, and store the info globally
			 */
			InheritanceRelationshipVisitor inheritanceVisitor = new InheritanceRelationshipVisitor();
			inheritanceVisitor.setInterfaceMapForUpdate(interfaces);
			inheritanceVisitor.setSuperClassMapForUpdate(superclasses);
			compilationUnit.accept(inheritanceVisitor);
		}
	}
	
	public void traverseToGetEntityClass() throws JavaModelException {
		long appStartTime = System.nanoTime();
		
		if (JsonUtil.entityClassExists()) {
			System.out.println("=================================");
			System.out.println("read entity class");
			entityClasses = JsonUtil.restoreEntityClass();
			
		} else {
			System.out.println("=================================");
			System.out.println("generate entity class");
			traverseToGetEntityClassAction();
			JsonUtil.storeEntityClass(entityClasses);
		}
		
		for (JDTEntityClass entity: entityClasses) {
			System.out.println(entity.getDbTableName());
		}
		System.out.println(entityClasses.size());
		
		long seconds = (System.nanoTime() - appStartTime) / 1000000 / 1000;
		System.out.println(seconds + " seconds");
	}
	
	private void traverseToGetEntityClassAction() throws JavaModelException {
		for (ICompilationUnit unit : icompilationUnitList) {
			/**
			 * change to another method to generate entity
			 */
//			JDTEntityClass entityClass = new JDTEntityClass(unit.getPackageDeclarations()[0].getElementName(),
//					unit.getElementName().split("\\.")[0], unit);
			CompilationUnit compilationUnit = ObjectCreationHelper.parse(unit);
			JDTEntityClass entityClass = EntityParser.converToEntityClass(compilationUnit);
			if (entityClass == null) {
				continue;
			}
			
			/**
			 * get entity class
			 */
			boolean isEntity = true;
			if (isEntity) {
				// get relationship for this entity
				JPAClassRelationshipVisitor2 jpaRelationshipVisitor2 = new JPAClassRelationshipVisitor2(compilationUnit);
				compilationUnit.accept(jpaRelationshipVisitor2);
				entityClass.setRelationshipResult(jpaRelationshipVisitor2.getRelationshipResult());
				entityClass.setHibernateRelationshipResult(jpaRelationshipVisitor2.getHibernateRelationshipResult());
				entityClass.setUnit(unit);
			
				entityClasses.add(entityClass);
			}
		}
		
		PrintWriter entityClassResultPW = FileUtil.getPrintWriter(GlobalData.directoryPath + "EntityClasses.csv");
		entityClassResultPW.println("entityClass, tableName, relationship");
		for (JDTEntityClass jdtEntityClass : entityClasses) {
			entityClassResultPW.println(jdtEntityClass + "," + jdtEntityClass.getDbTableName() + "," + jdtEntityClass.getRelationshipResult());
		}
		entityClassResultPW.close();
	}
	
	public void traverseClassRelationShip() {
		System.out.println("==================================");
		System.out.println("ProgramWorkspace.traverseClassRelationShip");
		long appStartTime = System.nanoTime();
		for (JDTEntityClass entityClass : entityClasses) {		
			// build relationship among java entity classes
			CompilationUnit compilationUnit = ObjectCreationHelper.parse(entityClass.getUnit());
			JPAClassRelationshipVisitor jpaRelationshipVisitor = new JPAClassRelationshipVisitor(entityClasses,
					compilationUnit, classRelationshipResult);
			compilationUnit.accept(jpaRelationshipVisitor);
		}
		long seconds = (System.nanoTime() - appStartTime) / 1000000 / 1000;
		System.out.println(seconds + " seconds");
	}
	
	public void traverseRequestMethod() {
		long appStartTime = System.nanoTime();
		if (JsonUtil.requestMethodExists()) {
			System.out.println("=================================");
			System.out.println("read request method");
			requestMethodList = JsonUtil.restoreRequestMethod();
		} else {
			System.out.println("=================================");
			System.out.println("generate request method");
			for (CompilationUnit compilationUnit : compilationUnitList) {
				RESTEntryPointVisitor entryPointVisitor = new RESTEntryPointVisitor(requestMethodList, unitRelationship.get(compilationUnit));
				compilationUnit.accept(entryPointVisitor);
			}
			JsonUtil.storeRequestMethod(requestMethodList);
		}
		
		for (JDTJavaMethod method:requestMethodList) {
			System.out.println(method.getFullNameWithParams());
		}
		System.out.println(requestMethodList.size());
		
		long seconds = (System.nanoTime() - appStartTime) / 1000000 / 1000;
		System.out.println(seconds + " seconds");
	}
	
	public void showInformation() {
		System.out.println("==================================");
		System.out.println("Program information");
		System.out.println("name: " + program.getName());
		System.out.println("path: " + program.getPath());
		System.out.println("projectNum: " + projectNumber);
		projectList.forEach(project -> {
			System.out.println("    projectName: " + project.getName());			
		});
		System.out.println("classFileNum: " + classFileNumber);
		System.out.println("compilationUnitList size : " + compilationUnitList.size());
		System.out.println("javaMethodList size : " + javaMethodList.size());
		System.out.println("entityClasses size : " + entityClasses.size());
		System.out.println("requestMethodList size: " + requestMethodList.size());
		System.out.println("==================================");
	}
}
