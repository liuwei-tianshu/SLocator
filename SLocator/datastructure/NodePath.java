package SLocator.datastructure;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import edu.cmu.cs.crystal.cfg.ICFGNode;
import sqlfinder.datastructure.jdt.JDTJavaMethod;
import sqlfinder.datastructure.jdt.JDTJavaClass.JDTClassType;

/**
 * as same as ArrayList<ICFGNode<ASTNode>>
 */
public class NodePath {
	private ArrayList<ICFGNode<ASTNode>> path;
	
	public NodePath() {
		path = new ArrayList<ICFGNode<ASTNode>>();
	}
	
	public NodePath(ArrayList<ICFGNode<ASTNode>> path) {
		this.path = path;
	}
	
	public ArrayList<ICFGNode<ASTNode>> getPath(){
		return path;
	}
	
	public void add(ICFGNode<ASTNode> node) {
		path.add(node);
	}
	
	public void add(int index, ICFGNode<ASTNode> node) {
		path.add(index,node);
	}
	
	public boolean isNotEmpty() {
		return !path.isEmpty();
	}
	
	public boolean isEmpty() {
		return path.isEmpty();
	}
	
	public boolean isContainNode(ICFGNode<ASTNode> node) {
		for (ICFGNode<ASTNode> e : path) {
			if(isSimilar(e,node)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isSimilar(ICFGNode<ASTNode> node1, ICFGNode<ASTNode> node2) {
		if (!(node1.getASTNode() instanceof MethodInvocation) || !(node2.getASTNode() instanceof MethodInvocation))
			return false;
		
		IMethodBinding iMethodBinding1 = ((MethodInvocation) node1.getASTNode()).resolveMethodBinding();
		IMethodBinding iMethodBinding2 = ((MethodInvocation) node2.getASTNode()).resolveMethodBinding();
		
		if (iMethodBinding1 == null || iMethodBinding2 == null)
			return false;
		
		JDTJavaMethod invokedMethod1 = new JDTJavaMethod(iMethodBinding1.getMethodDeclaration().getDeclaringClass().getPackage().getName(), 
				iMethodBinding1.getMethodDeclaration().getDeclaringClass().getName(),
				iMethodBinding1.getMethodDeclaration().getName(), JDTClassType.JDT_CLASS);
		
		JDTJavaMethod invokedMethod2 = new JDTJavaMethod(iMethodBinding2.getMethodDeclaration().getDeclaringClass().getPackage().getName(), 
				iMethodBinding2.getMethodDeclaration().getDeclaringClass().getName(),
				iMethodBinding2.getMethodDeclaration().getName(), JDTClassType.JDT_CLASS);
		
		return invokedMethod1.equals(invokedMethod2);
	}
}
