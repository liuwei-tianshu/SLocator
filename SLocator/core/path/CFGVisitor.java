package SLocator.core.path;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import SLocator.GlobalData;
import SLocator.datastructure.JDTJavaMethod;
import SLocator.datastructure.NodePath;
import SLocator.util.ObjectCreationHelper;
import edu.cmu.cs.crystal.cfg.ICFGEdge;
import edu.cmu.cs.crystal.cfg.ICFGNode;
import edu.cmu.cs.crystal.cfg.eclipse.EclipseCFG;
import lombok.Getter;

public class CFGVisitor extends ASTVisitor {
	
	@Getter private static List<JDTJavaMethod> bigMethods = new ArrayList<JDTJavaMethod>();
	
	@Getter private Set<JDTJavaMethod> javaMethodSet;
	
	static int index = 0;
	
	public CFGVisitor(Set<JDTJavaMethod> javaMethodSet) {
		this.javaMethodSet = javaMethodSet;
	}

	public boolean visit(MethodDeclaration method) {
		// for interface, the method is not implemented now
		if (method.getBody() == null) {
			return true;
		}
	
		/**
		 * if this method is not sql related
		 * it shoudl not be in the call path
		 */
		JDTJavaMethod javaMethod = ObjectCreationHelper.createMethodFromMethodDeclaration(method);
		if(!GlobalData.sqlRelatedMethods.contains(javaMethod)) {
			return true;
		}

		EclipseCFG cfg = null;
		try {
			cfg = new EclipseCFG(method);
		} catch (EmptyStackException | UnsupportedOperationException | NullPointerException e) {
			e.printStackTrace();
			System.err.println("EmptyStackException in ControlFlowGraphVisitor, method: ");
			System.err.println(method);
			System.err.println("method.class: " + method.getParent().getClass());
			return false;
		}
		
		GlobalData.nodeLimte = 0L;
		ArrayList<NodePath> nodePaths = DFSSimple(cfg.getStartNode(), method);
		ArrayList<ArrayList<JDTJavaMethod>> javaMethodPaths = convertJavaMethodPaths(nodePaths, javaMethodSet);
		if(javaMethodPaths != null && !javaMethodPaths.isEmpty()) {
			GlobalData.javaMethodPaths.put(javaMethod, javaMethodPaths);
		}
		
		return true;
	}
	
	private static ArrayList<NodePath> DFSSimple(ICFGNode<ASTNode> root, MethodDeclaration method){
		if (GlobalData.nodeLimte >= 1048576L) {		//>2^26---->2^20---->2^24
			// System.err.println("Exception in TestCFGVisitor.DFSSimple, root: " + root.getASTNode().toString());
		}
		
		// record the big method's name
		if (GlobalData.nodeLimte == 1048576L) {
			bigMethods.add(ObjectCreationHelper.createMethodFromMethodDeclaration(method));
		}
		
		GlobalData.nodeLimte++;
		
		// just put method in the path
		boolean isMethod = root.getASTNode() instanceof MethodInvocation || root.getASTNode() instanceof SuperMethodInvocation;
		JDTJavaMethod jdt = ObjectCreationHelper.createMethodFromICFGNode(root);
		boolean isSqlRelated = ((jdt != null) && GlobalData.sqlRelatedMethods.contains(jdt));
		boolean isSourceMethod = ((jdt != null) && GlobalData.javaMethodList.contains(jdt));
		
		if (GlobalData.nodeLimte >= 1048576L || root == null || root.getOutputs() == null || root.getOutputs().isEmpty()) {
			ArrayList<NodePath> paths = new ArrayList<NodePath>();
			NodePath path = new NodePath();
			if (isMethod) {				// easy to debug
				if (isSqlRelated) {	
					path.add(root);
				}
			}
			paths.add(path);
			return paths;
		}
		
		ArrayList<NodePath> paths = new ArrayList<NodePath>();
		Set<ICFGEdge<ASTNode>> outputs = (Set<ICFGEdge<ASTNode>>) root.getOutputs();
		for (ICFGEdge<ASTNode> output : outputs) {
			ICFGNode<ASTNode> node = output.getSink();
			if (node != null) {
				ArrayList<NodePath> currentPath = DFSSimple(node, method);
				for(NodePath path : currentPath) {
					if (isMethod) {	
						if (isSqlRelated)
							if (!path.isContainNode(root)) {
								path.add(0, root);
							}
					}
					paths.add(path);
				}
			}				
		}
		return paths;
	}
	
	/**
	 * convert allPaths of this method from ICFGNode type to JDTJavaMethod type
	 * only consider the method in ICFGNode
	 * @param method 
	 * @param allPaths allPaths to be converted
	 * @return allPaths in JDTJavaMethod type
	 */
	private ArrayList<ArrayList<JDTJavaMethod>> convertJavaMethodPaths(ArrayList<NodePath> paths, Set<JDTJavaMethod> javaMethodSet) {
		Set<ArrayList<JDTJavaMethod>> resultPaths = new HashSet<ArrayList<JDTJavaMethod>>();
		for (NodePath path : paths) {
			ArrayList<JDTJavaMethod> currentPath = new ArrayList<JDTJavaMethod>();
			for (ICFGNode<ASTNode> node : path.getPath()) {
				if (node.getASTNode() instanceof MethodInvocation || node.getASTNode() instanceof SuperMethodInvocation) {
					JDTJavaMethod javaMethod = ObjectCreationHelper.createMethodFromICFGNode(node);
					if(javaMethodSet.contains(javaMethod)) {
						currentPath.add(javaMethod);
					}
				}
			}
			
			//if (!currentPath.isEmpty()) {
				resultPaths.add(currentPath);
			//}
		}
		
		return new ArrayList<>(resultPaths);
	}
}

