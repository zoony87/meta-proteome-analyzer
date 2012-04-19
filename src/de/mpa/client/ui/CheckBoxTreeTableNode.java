package de.mpa.client.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

public class CheckBoxTreeTableNode extends DefaultMutableTreeTableNode {

	/**
	 * The flag determining whether the selection state may be changed.
	 */
	private boolean fixed = false;
	
	public CheckBoxTreeTableNode() {
		super();
	}

	public CheckBoxTreeTableNode(Object obj) {
		super(obj);
	}
	
	
	public CheckBoxTreeTableNode(Object obj, boolean fixed) {
		super(obj);
		this.fixed = fixed;
	}
	
	public boolean isFixed() {
		return fixed;
	}
	
	public void setFixed(boolean fixed) {
		this.fixed = fixed;
	}
	
	// starting here everything was copied from CheckBoxTreeTableNode
	// TODO: optimize finding all leaves (see depthFirstEnumeration)

    /**
     * Returns true if <code>aNode</code> is a child of this node.  If
     * <code>aNode</code> is null, this method returns false.
     *
     * @return	true if <code>aNode</code> is a child of this node; false if 
     *  		<code>aNode</code> is null
     */
	public boolean isNodeChild(TreeNode aNode) {
		boolean retval;

		if (aNode == null) {
			retval = false;
		} else {
			if (getChildCount() == 0) {
				retval = false;
			} else {
				retval = (aNode.getParent() == this);
			}
		}

		return retval;
	}

    /**
     * Returns this node's first child.  If this node has no children,
     * throws NoSuchElementException.
     *
     * @return	the first child of this node
     * @exception	NoSuchElementException	if this node has no children
     */
	public TreeNode getFirstChild() {
		if (getChildCount() == 0) {
			throw new NoSuchElementException("node has no children");
		}
		return getChildAt(0);
	}

    /**
     * Returns the child in this node's child array that immediately
     * follows <code>aChild</code>, which must be a child of this node.  If
     * <code>aChild</code> is the last child, returns null.  This method
     * performs a linear search of this node's children for
     * <code>aChild</code> and is O(n) where n is the number of children; to
     * traverse the entire array of children, use an enumeration instead.
     *
     * @see		#children
     * @exception	IllegalArgumentException if <code>aChild</code> is
     *					null or is not a child of this node
     * @return	the child of this node that immediately follows
     *		<code>aChild</code>
     */
	public TreeNode getChildAfter(TreeNode aChild) {
		if (aChild == null) {
			throw new IllegalArgumentException("argument is null");
		}

		int index = getIndex(aChild); // linear search

		if (index == -1) {
			throw new IllegalArgumentException("node is not a child");
		}

		if (index < getChildCount() - 1) {
			return getChildAt(index + 1);
		} else {
			return null;
		}
	}
	
	/**
     * Returns true if <code>anotherNode</code> is a sibling of (has the
     * same parent as) this node.  A node is its own sibling.  If
     * <code>anotherNode</code> is null, returns false.
     *
     * @param	anotherNode	node to test as sibling of this node
     * @return	true if <code>anotherNode</code> is a sibling of this node
     */
	public boolean isNodeSibling(TreeNode anotherNode) {
		boolean retval;

		if (anotherNode == null) {
			retval = false;
		} else if (anotherNode == this) {
			retval = true;
		} else {
			TreeNode myParent = getParent();
			retval = (myParent != null && myParent == anotherNode.getParent());

			if (retval
					&& !((CheckBoxTreeTableNode) getParent())
							.isNodeChild(anotherNode)) {
				throw new Error("sibling has different parent");
			}
		}

		return retval;
	}

	/**
     * Returns true if <code>anotherNode</code> is a sibling of (has the
     * same parent as) this node.  A node is its own sibling.  If
     * <code>anotherNode</code> is null, returns false.
     *
     * @param	anotherNode	node to test as sibling of this node
     * @return	true if <code>anotherNode</code> is a sibling of this node
     */
	public CheckBoxTreeTableNode getNextSibling() {
		CheckBoxTreeTableNode retval;

		CheckBoxTreeTableNode myParent = (CheckBoxTreeTableNode)getParent();

		if (myParent == null) {
			retval = null;
		} else {
			retval = (CheckBoxTreeTableNode)myParent.getChildAfter(this);	// linear search
		}

		if (retval != null && !isNodeSibling(retval)) {
			throw new Error("child of parent is not a sibling");
		}

		return retval;
	}
    
	/**
     * Finds and returns the first leaf that is a descendant of this node --
     * either this node or its first child's first leaf.
     * Returns this node if it is a leaf.
     *
     * @see	#isLeaf
     * @see	#isNodeDescendant
     * @return	the first leaf in the subtree rooted at this node
     */
	public CheckBoxTreeTableNode getFirstLeaf() {
		CheckBoxTreeTableNode node = this;

		while (!node.isLeaf()) {
			node = (CheckBoxTreeTableNode) node.getFirstChild();
		}

		return node;
	}
    
	/**
     * Returns the leaf after this node or null if this node is the
     * last leaf in the tree.
     * <p>
     * In this implementation of the <code>MutableNode</code> interface,
     * this operation is very inefficient. In order to determine the
     * next node, this method first performs a linear search in the 
     * parent's child-list in order to find the current node. 
     * <p>
     * That implementation makes the operation suitable for short
     * traversals from a known position. But to traverse all of the 
     * leaves in the tree, you should use <code>depthFirstEnumeration</code>
     * to enumerate the nodes in the tree and use <code>isLeaf</code>
     * on each node to determine which are leaves.
     *
     * @see	#depthFirstEnumeration
     * @see	#isLeaf
     * @return	returns the next leaf past this node
     */
	public CheckBoxTreeTableNode getNextLeaf() {
		CheckBoxTreeTableNode nextSibling;
		CheckBoxTreeTableNode myParent = (CheckBoxTreeTableNode) getParent();

		if (myParent == null)
			return null;

		nextSibling = getNextSibling();	// linear search

		if (nextSibling != null)
			return nextSibling.getFirstLeaf();

		return myParent.getNextLeaf();	// tail recursion
	}
	
	public TreePath getPath() {
		CheckBoxTreeTableNode node = this;
	    List<CheckBoxTreeTableNode> list = new ArrayList<CheckBoxTreeTableNode>();

	    // Add all nodes to list
	    while (node != null) {
	        list.add(node);
	        node = (CheckBoxTreeTableNode) node.getParent();
	    }
	    Collections.reverse(list);

	    // Convert array of nodes to TreePath
	    return new TreePath(list.toArray());
	}

}