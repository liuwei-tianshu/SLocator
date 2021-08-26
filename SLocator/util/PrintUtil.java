package SLocator.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;

import SLocator.GlobalData;
import SLocator.datastructure.JDTJavaMethod;
import SLocator.datastructure.NodePath;
import edu.cmu.cs.crystal.cfg.ICFGEdge;
import edu.cmu.cs.crystal.cfg.ICFGNode;
import edu.cmu.cs.crystal.cfg.eclipse.EclipseCFGEdge;

public class PrintUtil {
	
	public static void printMethodPath(List<JDTJavaMethod> methodPath) {
		if(methodPath == null || methodPath.isEmpty())
			return;
		
		for(JDTJavaMethod method : methodPath) {
			System.out.print(method.getClassName() + "." + method.getMethodName() + " ---> ");
		}
		System.out.println("");
	}
	
	public static void printMethodPaths(List<List<JDTJavaMethod>> methodPaths) {
		if(methodPaths == null || methodPaths.isEmpty())
			return;
		
		for(List<JDTJavaMethod> path : methodPaths) {
			printMethodPath(path);
		}
	}
	
	/**
	 * print node paths in cfgNodeLists 
	 * @param cfgNodeLists
	 */
	public static void printCFGNodePaths(ArrayList<ArrayList<ICFGNode<ASTNode>>> cfgNodeLists) {
		if(cfgNodeLists == null || cfgNodeLists.isEmpty())
			return;
		
		for(ArrayList<ICFGNode<ASTNode>> cfgNodeList : cfgNodeLists) {
			printCFGNodePath(cfgNodeList);
		}
	}
	
	public static void printPaths(ArrayList<NodePath> paths) {
		if(paths == null || paths.isEmpty())
			return;
		
		for(NodePath path : paths) {
			printPath(path);
		}
	}
	
	/**
	 * print node path in cfgNodeList
	 * @param cfgNodeList
	 */
	public static void printCFGNodePath(ArrayList<ICFGNode<ASTNode>> cfgNodeList) {
		if(cfgNodeList == null || cfgNodeList.isEmpty())
			return;
		
		for(ICFGNode<ASTNode> node : cfgNodeList) {
			System.out.print(node.toString() + " ---> ");
		}
		System.out.println("");
	}
	
	public static void printPath(NodePath nodePath) {
		if(nodePath == null || nodePath.isEmpty())
			return;
		
		for(ICFGNode<ASTNode> node : nodePath.getPath()) {
			System.out.print(node.toString() + " ---> ");
		}
		System.out.println("");
	}
	
	/**
	 * print method paths in cfgNodeLists
	 * only consider the method in ICFGNode
	 * @param cfgNodeLists
	 */
	public static void printCFGMehtodPaths(ArrayList<ArrayList<ICFGNode<ASTNode>>> cfgNodeLists) {
		if(cfgNodeLists == null || cfgNodeLists.isEmpty())
			return;
		
		for(ArrayList<ICFGNode<ASTNode>> cfgNodeList : cfgNodeLists) {
			printCFGMethodPath(cfgNodeList);
		}
	}
	
	public static void printPaths(List<NodePath> paths) {
		if(paths == null || paths.isEmpty())
			return;
		
		for(NodePath nodePath : paths) {
			printCFGMethodPath(nodePath.getPath());
		}
	}
	
	/**
	 * print method path in cfgNodeList 
	 * only consider the method in ICFGNode
	 * @param cfgNodeList
	 */
	public static void printCFGMethodPath(ArrayList<ICFGNode<ASTNode>> cfgNodeList) {
		if(cfgNodeList == null || cfgNodeList.isEmpty())
			return;
		
		for(ICFGNode<ASTNode> node : cfgNodeList) {
			if (node.getASTNode() instanceof MethodInvocation) {		// only consider the method
				System.out.print(node.toString() + " ---> ");
			}	
		}
		System.out.println("");
	}
}
