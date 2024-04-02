import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The AVL tree implementation.
 */
class AVLTree<T> {
  // Expose the root so that other tree algorithms can be executed on the tree.
  AVLTreeNode<T> root;
  private Comparator<T> comp;

  /**
   * The constructor accepts a comparator which allows us to compare any two node
   * values while having an arbitrary node structure.
   */
  AVLTree(Comparator<T> comp) {
    this.comp = comp;
  }

  /**
   * Returns the min value in the tree.
   */
  T findMin() {
    return findMin(root);
  }

  /**
   * Returns the max value in the tree.
   */
  T findMax() {
    return findMax(root);
  }

  /**
   * Returns all the values that appear before the supplied value in
   * the
   * inorder traversal of the tree.
   */
  List<T> getInorderPredecessors(T value) {
    List<T> res = new ArrayList<>();
    getInorderPredecessors(root, value, res);
    return res;
  }

  /**
   * Returns the value that appears just after the supplied value in
   * the
   * inorder traversal of the tree.
   */
  AVLTreeNode<T> getInorderSuccessor(T value) {
    return getInorderSuccessor(find(value));
  }

  /**
   * Returns the node that has the supplied value in the tree.
   */
  AVLTreeNode<T> find(T value) {
    return find(root, value);
  }

  /**
   * Inserts a new value in the tree.
   */
  boolean insert(T value) {
    if (value == null)
      return false;

    // If the value already exists. Do not insert.
    if (find(root, value) != null)
      return false;

    root = insert(root, value);
    return true;
  }

  /**
   * Removes a value the tree.
   */
  boolean remove(T value) {
    if (value == null)
      return false;

    // If the value does not exist, there is nothing to remove.
    if (find(root, value) == null)
      return false;

    root = remove(root, value);
    return true;
  }

  /**
   * Private recursive method to get all the values that appear before the
   * supplied value in
   * the inorder traversal of the tree. It is called by the corresponding public
   * method.
   */
  private void getInorderPredecessors(AVLTreeNode<T> node, T value, List<T> res) {
    if (node == null)
      return;

    int cmp = comp.compare(node.value, value);
    if (cmp < 0) {
      res.add(node.value);
      getInorderPredecessors(node.left, value, res);
      getInorderPredecessors(node.right, value, res);
    } else {
      getInorderPredecessors(node.left, value, res);
    }
  }

  /**
   * Private method to get the value that appears just after the supplied value in
   * the
   * inorder traversal of the tree. It is called by the corresponding public
   * method.
   */
  private AVLTreeNode<T> getInorderSuccessor(AVLTreeNode<T> node) {
    if (node == null) {
      return null;
    }

    if (node.right != null) {
      // If the right subtree of the node is not null,
      // the leftmost node in the right subtree is the successor.
      AVLTreeNode<T> successor = node.right;
      while (successor.left != null) {
        successor = successor.left;
      }
      return successor;
    } else {
      // Otherwise traverse from the root to find the successor.
      AVLTreeNode<T> successor = null;
      AVLTreeNode<T> ancestor = root;
      while (ancestor != node) {
        int cmp = comp.compare(node.value, ancestor.value);
        if (cmp < 0) {
          successor = ancestor;
          ancestor = ancestor.left;
        } else if (cmp > 0) {
          ancestor = ancestor.right;
        }
      }
      return successor;
    }
  }

  /**
   * Private recursive method to find a value in the tree. It is called by the
   * corresponding public
   * method.
   */
  private AVLTreeNode<T> find(AVLTreeNode<T> node, T value) {
    if (value == null || node == null)
      return null;

    int cmp = comp.compare(value, node.value);
    if (cmp < 0)
      return find(node.left, value);
    if (cmp > 0)
      return find(node.right, value);

    return node;
  }

  /**
   * Private recursive method to insert a new value the tree. It is called by the
   * corresponding public
   * method.
   */
  private AVLTreeNode<T> insert(AVLTreeNode<T> node, T value) {
    if (node == null)
      return new AVLTreeNode<T>(value);

    int cmp = comp.compare(value, node.value);
    if (cmp < 0)
      node.left = insert(node.left, value);
    else
      node.right = insert(node.right, value);

    // After the insertion, the tree might be unbalanced.
    // So rebalance it.
    updateBalanceFactor(node);
    return rebalance(node);
  }

  /**
   * Private recursive method to delete a value from the tree. It is called by the
   * corresponding public
   * method.
   */
  private AVLTreeNode<T> remove(AVLTreeNode<T> node, T value) {
    if (node == null)
      return null;

    int cmp = comp.compare(value, node.value);
    if (cmp < 0)
      node.left = remove(node.left, value);
    else if (cmp > 0)
      node.right = remove(node.right, value);
    else {
      if (node.left == null)
        // If left subtree is null, return right subtree.
        return node.right;
      else if (node.right == null)
        // If right subtree is null, return left subtree.
        return node.left;
      else {
        // Otherwise return the largest value in the left subtree
        // and recursively delete the largest value in the left subtree.
        T maxValueInLeftSubtree = findMax(node.left);
        node.value = maxValueInLeftSubtree;
        node.left = remove(node.left, maxValueInLeftSubtree);
      }
    }

    // After the deletion, the tree might be unbalanced.
    // So rebalance it.
    updateBalanceFactor(node);
    return rebalance(node);
  }

  /**
   * Private method to find the min value in the tree. It is called by the
   * corresponding public
   * method.
   */
  private T findMin(AVLTreeNode<T> node) {
    // The leftmost leaf is the min value
    if (node == null)
      return null;
    while (node.left != null)
      node = node.left;
    return node.value;
  }

  /**
   * Private method to find the max value in the tree. It is called by the
   * corresponding public
   * method.
   */
  private T findMax(AVLTreeNode<T> node) {
    // The rightmost leaf is the max value
    if (node == null)
      return null;
    while (node.right != null)
      node = node.right;
    return node.value;
  }

  /**
   * Private method to update the balance factor
   * of a node. Called whenever the tree is modified.
   */
  private void updateBalanceFactor(AVLTreeNode<T> node) {
    int lh = -1;
    int rh = -1;

    if (node.left != null)
      lh = node.left.height;
    if (node.right != null)
      rh = node.right.height;

    node.height = 1 + Math.max(lh, rh);
    node.balanceFactor = lh - rh;
  }

  /**
   * Private method to rebalance the tree using rotations.
   * Called whenever the tree is modified.
   */
  private AVLTreeNode<T> rebalance(AVLTreeNode<T> node) {
    // Identify the imbalance type
    // and perform the corresponding rotation.
    if (node.balanceFactor == 2) {
      if (node.left.balanceFactor >= 0)
        return leftLeftRotate(
            node);
      else
        return leftRightRotate(node);
    } else if (node.balanceFactor == -2) {
      if (node.right.balanceFactor >= 0)
        return rightLeftRotate(
            node);
      else
        return rightRightRotate(node);
    } else
      return node;
  }

  /**
   * Private method for RR rotation.
   */
  private AVLTreeNode<T> rightRightRotate(AVLTreeNode<T> node) {
    return leftRotate(node);
  }

  /**
   * Private method for RL rotation.
   */
  private AVLTreeNode<T> rightLeftRotate(AVLTreeNode<T> node) {
    node.right = rightRotate(node.right);
    return leftRotate(node);
  }

  /**
   * Private method for LR rotation.
   */
  private AVLTreeNode<T> leftRightRotate(AVLTreeNode<T> node) {
    node.left = leftRotate(node.left);
    return rightRotate(node);
  }

  /**
   * Private method for LL rotation.
   */
  private AVLTreeNode<T> leftLeftRotate(AVLTreeNode<T> node) {
    return rightRotate(node);
  }

  /**
   * Private method for right rotation. Used by the LL rotation.
   */
  private AVLTreeNode<T> rightRotate(AVLTreeNode<T> node) {
    AVLTreeNode<T> temp = node.left;
    node.left = temp.right;
    temp.right = node;

    updateBalanceFactor(node);
    updateBalanceFactor(temp);

    return temp;
  }

  /**
   * Private method for left rotation. Used by the RR rotation.
   */
  private AVLTreeNode<T> leftRotate(AVLTreeNode<T> node) {
    AVLTreeNode<T> temp = node.right;
    node.right = temp.left;
    temp.left = node;

    updateBalanceFactor(node);
    updateBalanceFactor(temp);

    return temp;
  }
}
