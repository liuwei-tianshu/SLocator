package SLocator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import SLocator.datastructure.JDTJavaMethod;
import SLocator.datastructure.JDTJavaMethodPath;
import SLocator.datastructure.NodePath;
import SLocator.datastructure.Program;

public class GlobalData {
	// <method, all paths in a method body>
	public static Map<MethodDeclaration, ArrayList<NodePath>> paths = new HashMap<MethodDeclaration,ArrayList<NodePath>>();
	
	// <method, all paths in a method body>
	public static HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> javaMethodPaths = new HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>>();
	
	// all methods have annotation "Repository"
	public static List<IMethod> sqlIMethods = new ArrayList<>();
	
	// all methods have annotation "Repository"
	public static List<JDTJavaMethod> sqlJDTMethods = new ArrayList<>();
	
	// all methods may call sqlMethods
	public static Set<JDTJavaMethod> sqlRelatedMethods = ConcurrentHashMap.newKeySet();
	
	// all methods have annotation "RequestMapping"
	public static Set<IMethod> entryMethods = new HashSet<IMethod>();
	
	public static HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> cfgResult = new HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>>();
	
	public static HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>> simpleCFGResult = new HashMap<JDTJavaMethod, ArrayList<ArrayList<JDTJavaMethod>>>();
	
	public static HashMap<JDTJavaMethod, ArrayList<JDTJavaMethodPath>> simpleCFGPathResult = new HashMap<JDTJavaMethod, ArrayList<JDTJavaMethodPath>>();
	
	public static long indexOfClass = 0L;
	
	public static long nodeLimte = 0L;
	
	public static Long nodeMax = 0L;
	
	public static String directoryPath = "";		// path to output
	
	// <interface or super method, implemented or sub method>
	public static Map<JDTJavaMethod, ArrayList<JDTJavaMethod>> interfaceAndSuperMethods = new HashMap<JDTJavaMethod, ArrayList<JDTJavaMethod>>();
	
	public static long dfsPathDepth = 0L;
	
	// reference to workspace
	// all javaMethod from workspace
	public static List<JDTJavaMethod> javaMethodList = null;
	
	// all javaMethod from workspace
	public static Set<JDTJavaMethod> javaMethodSet = null;
	
	public static Set<String> hibernateAssociation = new HashSet<String>();
	static {
		hibernateAssociation.add("OneToMany");
		hibernateAssociation.add("ManyToOne");
		hibernateAssociation.add("OneToOne");
		hibernateAssociation.add("ManyToMany");
	}
	
	public static Program program = null;
}
