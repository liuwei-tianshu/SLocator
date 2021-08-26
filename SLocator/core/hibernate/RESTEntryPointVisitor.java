package SLocator.core.hibernate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import SLocator.GlobalData;
import SLocator.datastructure.JDTJavaMethod;
import SLocator.datastructure.MethodPosition;
import SLocator.datastructure.Program;
import SLocator.util.JDTHelper;
import SLocator.util.ObjectCreationHelper;

/**
 * get entry point (entry method) mainly using "RequestMapping"
 */
public class RESTEntryPointVisitor extends ASTVisitor {
	private ICompilationUnit icu;
	private String classLevelRequestMapping = "";
	private Set<MethodPosition> httpRequestMethods = new HashSet<MethodPosition>();
	private List<JDTJavaMethod> entryPoints;

	public RESTEntryPointVisitor(List<JDTJavaMethod> entryPoints, ICompilationUnit icu) {
		this.entryPoints = entryPoints;
		this.icu = icu;

		try {
			if (icu.getTypes().length > 0)
				// shouldn't declare transaction at the class level
				for (IAnnotation a : icu.getTypes()[0].getAnnotations()) {
					
					boolean result = false;
					if (GlobalData.program == Program.PETCLINIC || GlobalData.program == Program.CLOUD_STORE 
							|| GlobalData.program == Program.WALL_RIDE || GlobalData.program == Program.PUBLIC_CMS) {
						result = a.getElementName().contains("RequestMapping");
					} 
					
					if (result) {
						classLevelRequestMapping = JDTHelper.parseRESTAnnotation(a.getSource());
					}
				}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	public String getClassName() {
		return icu.getElementName();
	}

	public void preVisit(ASTNode node) {
		try {
			if (node instanceof MethodDeclaration) {
				MethodDeclaration method = ((MethodDeclaration) node);

				for (IAnnotationBinding annotation : method.resolveBinding().getAnnotations()) {
					
					boolean result = false;
					if (GlobalData.program == Program.PETCLINIC || GlobalData.program == Program.CLOUD_STORE 
							|| GlobalData.program == Program.WALL_RIDE || GlobalData.program == Program.PUBLIC_CMS) {
						result = annotation.toString().contains("RequestMapping") || annotation.toString().contains("@GET")
								|| annotation.toString().contains("@POST") || annotation.toString().contains("@PUT")
								|| annotation.toString().contains("@DELETE");
					}
					
					// get positions of all http request methods
					if (result) {
						MethodPosition m = new MethodPosition(method.getName().toString(), method.getStartPosition(),
								method.getStartPosition() + method.getLength());

						httpRequestMethods.add(m);
					}
				}
			}
		} catch (NullPointerException e) {
			if (node instanceof MethodDeclaration) {
				MethodDeclaration method = ((MethodDeclaration) node);
				System.out.println(method.getName());
			}
			e.printStackTrace();
		}
	}

	public boolean visit(final MethodDeclaration method) {
		MethodPosition m = new MethodPosition(method.getName().toString(), method.getStartPosition(),
				method.getStartPosition() + method.getLength());

		// parse all Spring HTTP REST methods
		if (!httpRequestMethods.contains(m) && !this.getClassName().contains("Resource1_8"))
			return true;

		final JDTJavaMethod httpMethod = ObjectCreationHelper.createMethodFromMethodDeclaration(method);

		final StringBuilder methodRestInfo = new StringBuilder();
		for (IAnnotationBinding i : method.resolveBinding().getAnnotations()) {
			if (i.toString().contains("@Path")) {
				methodRestInfo.append(i.toString());
			}
		}
		if (methodRestInfo.length() == 0) {
			for (IAnnotationBinding i : method.resolveBinding().getAnnotations()) {
				
				boolean result = false;
				if (GlobalData.program == Program.PETCLINIC || GlobalData.program == Program.CLOUD_STORE 
						|| GlobalData.program == Program.WALL_RIDE || GlobalData.program == Program.PUBLIC_CMS) {
					result = i.toString().contains("@RequestMapping");
				}
				
				if (result) {
					Pattern p = Pattern.compile("value = \\{(.*?)\\},");
					Matcher matcher = p.matcher(i.toString());
					String url = "";
					if (matcher.find()) {
						url = matcher.group(1);
						String newURL = url;

						if (!classLevelRequestMapping.equals("\\")) {
							newURL = classLevelRequestMapping + url;
						}
						methodRestInfo.append(i.toString().replace("{" + url + "}", "{" + newURL + "}"));

					} else {
						p = Pattern.compile("value = \\{(.*?)\\}");
						matcher = p.matcher(i.toString());
						if (matcher.find()) {
							url = matcher.group(1);
							String newURL = url;
							if (!classLevelRequestMapping.equals("\\")) {
								newURL = classLevelRequestMapping + url;
							}
							methodRestInfo.append(i.toString().replace("{" + url + "}", "{" + newURL + "}"));
						}
					}

				}
			}
		}
		
		httpMethod.setAnnotationInfo(methodRestInfo.toString());
		return true;
	}

}
