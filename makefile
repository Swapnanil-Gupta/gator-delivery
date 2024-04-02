SOURCE = gatorDelivery.java OrderManager.java Order.java AVLTree.java AVLTreeNode.java
TARGET = gatorDelivery.class

default: $(TARGET)

$(TARGET): $(SOURCE)
	javac $(SOURCE)