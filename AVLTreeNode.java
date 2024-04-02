/**
 * The node structure for the AVL tree.
 */
class AVLTreeNode<T> {
  T value;
  int height;
  int balanceFactor;
  AVLTreeNode<T> left;
  AVLTreeNode<T> right;

  AVLTreeNode(T value) {
    this.value = value;
  }
}
