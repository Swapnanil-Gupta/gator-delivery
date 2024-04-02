import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * The order manager implementation.
 */
class OrderManager {
  // To keep track of the current system time.
  private int currentSystemTime;
  // To quickly look up orders using the order id.
  private Map<Integer, Order> orderMap;
  // To store the orders with priority as the key.
  private AVLTree<Order> priorityTree;
  // To store the orders with ETA as the key.
  private AVLTree<Order> etaTree;
  // To store already delivered orders.
  private PriorityQueue<Order> deliveredOrders;
  // To store orders that will be delivered when a new order is created.
  private PriorityQueue<Order> deliveryQueue;

  /**
   * Initializes all the required data structures.
   */
  OrderManager() {
    this.orderMap = new HashMap<>();

    // Priorities can be duplicated.
    this.priorityTree = new AVLTree<>(new Comparator<Order>() {
      @Override
      public int compare(Order o1, Order o2) {
        // use priority as key
        if (o1.priority != o2.priority)
          return Double.compare(o1.priority, o2.priority);

        // if priorities are duplicated, use order creation time as resolver.
        if (o1.orderCreationTime != o2.orderCreationTime)
          return Integer.compare(o2.orderCreationTime, o1.orderCreationTime);

        // if order creation time is also duplicated, use order id as resolver.
        return Integer.compare(o2.orderId, o1.orderId);
      }
    });

    // ETAs can never be duplicated.
    this.etaTree = new AVLTree<>((o1, o2) -> Integer.compare(o1.eta, o2.eta));

    // Use ETA as the key for these priority queues to quickly query the last
    // delivered order.
    this.deliveredOrders = new PriorityQueue<>((o1, o2) -> Integer.compare(o2.eta, o1.eta));
    this.deliveryQueue = new PriorityQueue<>((o1, o2) -> Integer.compare(o2.eta, o1.eta));
  }

  /**
   * Creates a new order, populates its ETA, updates the ETAs of the lower
   * priority orders and finally prints all the delivered orders upto the supplied
   * time.
   */
  List<String> createOrder(
      int orderId,
      int orderCreationTime,
      int orderValue,
      int deliveryTime) {
    List<String> res = new ArrayList<>();

    // Update the current system time and internally collects the pending orders to
    // be delivered upto this time.
    setCurrentSystemTime(orderCreationTime);

    // Order id must not be duplicated
    if (!orderMap.containsKey(orderId)) {
      // Create the new order and insert into the map and priority tree. It cannot be
      // inserted into the ETA tree now as its ETA is yet unknown.
      System.out.println("Creating order " + orderId + " at " + currentSystemTime);
      Order newOrder = new Order(
          orderId,
          orderCreationTime,
          orderValue,
          deliveryTime);
      orderMap.put(orderId, newOrder);
      priorityTree.insert(newOrder);

      // Calculate the ETA of this new order.
      newOrder.setEta(getOrderETA(newOrder));
      System.out.println("newOrder created at " + orderCreationTime + " => " + newOrder.toString());
      res.add(
          String.format("Order %d has been created - ETA: %d", orderId, newOrder.eta));

      // Update the ETA of all the orders which have lower priority than this order
      List<Order> lowerPriorityOrders = getLowerPriorityOrders(newOrder);
      Map<Integer, Integer> updates = updateETA(lowerPriorityOrders, 2 * newOrder.deliveryTime);

      // Get the updated ETAs;
      String updatedETAs = getUpdatedETAString(updates);
      System.out.println("updatedETA: " + updatedETAs);
      if (updatedETAs != null)
        res.add(updatedETAs);

      // Insert the new order into the ETA tree as now its ETA has been calculated.
      etaTree.insert(newOrder);
    }

    // Delivers all the pending orders upto the supplied time.
    List<String> delivered = deliverOrders();
    System.out.println("Delivered: " + delivered);
    if (!delivered.isEmpty())
      res.addAll(delivered);

    return res;
  }

  /**
   * Sets the current system time and collects pending orders to be delivered.
   * Does not allow us to go back it time!
   */
  private void setCurrentSystemTime(int currentSystemTime) {
    if (this.currentSystemTime >= currentSystemTime)
      return;

    this.currentSystemTime = currentSystemTime;
    collectDeliveredOrders();
  }

  /**
   * Collects the pending orders to be delivered upto the current system time.
   * This does NOT deliver the orders.
   */
  private void collectDeliveredOrders() {
    List<Order> toBeDelivered = getToBeDeliveredOrdersBetweenTime(0, currentSystemTime);

    for (Order o : toBeDelivered) {
      deliveryQueue.add(o);
      deleteOrder(o);
    }
  }

  /**
   * Delivers all the pending orders by flusing the delivery queue. Delivered
   * orders are permanently stored in the deliveredOrders heap. This is done to
   * query the last delivered order at any
   * time.
   */
  private List<String> deliverOrders() {
    List<String> res = new LinkedList<>();

    while (!deliveryQueue.isEmpty()) {
      Order o = deliveryQueue.poll();
      deliveredOrders.add(o);
      res.add(0, String.format("Order %d has been delivered at time %d", o.orderId, o.eta));
    }

    return res;
  }

  /**
   * Deletes an order from the system. The delivered orders however remain in the
   * delivered orders queue. This is done to query the last delivered order at any
   * time.
   */
  private void deleteOrder(Order order) {
    priorityTree.remove(order);
    etaTree.remove(order);
    orderMap.remove(order.orderId);
  }

  /**
   * Finds all the orders that will be delivered between startTime and endTime,
   * i.e. whose ETA fall in the range [startTime, endTime].
   */
  private List<Order> getToBeDeliveredOrdersBetweenTime(int startTime, int endTime) {
    List<Order> res = new ArrayList<>();

    etaRangeSearch(etaTree.root, startTime, endTime, res);

    return res;
  }

  /**
   * Executes a range search algorithm on the ETA tree to find the nodes whose
   * value fall between the range [low, high].
   */
  private void etaRangeSearch(AVLTreeNode<Order> node, int low, int high, List<Order> res) {
    if (node == null)
      return;

    // Compare the node value with low and high and determine which subtree to
    // visit. If value is between [low, high] then both left and right subtree might
    // contain values in this range. So visit both.
    if (node.value.eta >= low && node.value.eta <= high) {
      etaRangeSearch(node.left, low, high, res);
      res.add(node.value);
      etaRangeSearch(node.right, low, high, res);
    } else if (node.value.eta < low) {
      etaRangeSearch(node.right, low, high, res);
    } else if (node.value.eta > high) {
      etaRangeSearch(node.left, low, high, res);
    }
  }

  /**
   * Calculates the ETA of a new order.
   */
  private int getOrderETA(Order order) {
    // Reference order - the order after which this new order will be delivered.
    Order ref;

    // The inorder successor of this order in terms of its priority. This is the
    // order with the lowest priority greater than the priority of the new order.
    Order prevOrder = getPrevOrder(order);
    System.out.println("prevOrder: " + prevOrder);

    // If the new order does not have an inorder successor, two cases might arise:
    // 1. this is the first order in the system and 2. this new order has the
    // highest priotiy
    // of all the orders.
    if (prevOrder == null) {
      // Get the order that is currently out for delivery.
      Order outForDelivery = getCurrentlyDeliveringOrder();
      System.out.println("outForDelivery: " + outForDelivery);

      if (outForDelivery == null) {
        // If there is no order that is out for delivery, check the order that was last
        // delivered.
        Order lastDeliveredOrder = getLastDeliveredOrder();
        System.out.println("lastDelivered: " + lastDeliveredOrder);

        // If the last delivered order does not exist, this new order is the
        // first order in the system.
        if (lastDeliveredOrder == null || currentSystemTime >= lastDeliveredOrder.eta + lastDeliveredOrder.deliveryTime)
          ref = null;
        // If the last delivered order does exist, then this is the reference order.
        else
          ref = lastDeliveredOrder;
      }
      // If there is an order that is out for delivery, then this order is the
      // reference order.
      else
        ref = outForDelivery;
    }
    // If an inorder successor exists, then this is the reference order.
    else
      ref = prevOrder;

    // If reference order does not exist, the delivery agent is idle and this new
    // order will be immediately picked
    // up.
    if (ref == null)
      return currentSystemTime + order.deliveryTime;
    return ref.eta + ref.deliveryTime + order.deliveryTime;
  }

  /**
   * Returns the orders that have lower priority than the provided order by
   * finding the inorder predecessors of the order in terms of the priority.
   */
  private List<Order> getLowerPriorityOrders(Order order) {
    Order outForDelivery = getCurrentlyDeliveringOrder();
    List<Order> lowerPriorityOrders = priorityTree.getInorderPredecessors(order);

    if (outForDelivery == null)
      return lowerPriorityOrders;

    // Filter out the currently delivering order because it's ETA can no longer be
    // changed.
    return lowerPriorityOrders.stream().filter(o -> o.orderId != outForDelivery.orderId)
        .collect(Collectors.toList());
  }

  /**
   * Increases or decreases the ETA of the provided list of orders by the provided
   * amount. This is needed when a new order is created and lower
   * priority orders are being pushed back or up.
   */
  private Map<Integer, Integer> updateETA(List<Order> toBeUpdatedOrders, int amount) {
    Map<Integer, Integer> updates = new TreeMap<>();

    for (Order o : toBeUpdatedOrders) {
      etaTree.remove(o);
      o.setEta(o.eta + amount);
      etaTree.insert(o);
      updates.put(o.eta, o.orderId);

    }

    return updates;
  }

  /**
   * Returns the updated eta of orders in increasing order as a string.
   */
  private String getUpdatedETAString(Map<Integer, Integer> updates) {
    if (updates == null || updates.size() == 0)
      return null;

    List<String> updateStrings = new ArrayList<>();
    for (int key : updates.keySet()) {
      int eta = key;
      int orderId = updates.get(key);
      updateStrings.add(String.format("%d: %d", orderId, eta));
    }

    return "Updated ETAs: [" + String.join(", ", updateStrings) + "]";
  }

  /**
   * Returns the last delivered order.
   */
  private Order getLastDeliveredOrder() {
    // First peek the delivery queue. If there is nothing in the delivery queue then
    // peek the delivered orders
    // heap.
    Order o = deliveryQueue.peek();
    return o != null ? o : deliveredOrders.peek();
  }

  /**
   * Returns the order that is currently being delivered.
   */
  private Order getCurrentlyDeliveringOrder() {
    // Get the order with the min ETA of all orders.
    // If there is nothing in the ETA tree, return null.
    // If this order has not yet been picked up, i.e. the delivery agent is
    // returning after delivering another order also return null.
    Order minEtaOrder = etaTree.findMin();
    return (minEtaOrder == null || currentSystemTime > minEtaOrder.deliveryStartTime) ? minEtaOrder : null;
  }

  /**
   * Returns the inorder successor of an order in terms of its priority. This is
   * the order that has the lowest priority greater than the provided order.
   */
  private Order getPrevOrder(Order order) {
    AVLTreeNode<Order> successor = priorityTree.getInorderSuccessor(order);
    return successor == null ? null : successor.value;
  }

  /**
   * Prints an order by querying the order map.
   */
  List<String> print(int orderId) {
    Order o = orderMap.get(orderId);
    return o == null ? null : List.of(o.toString());
  }

  /**
   * Prints all the orders that are undelivered and will be delivered between the
   * window [startTime, endTime].
   */
  List<String> print(int startTime, int endTime) {
    List<String> res = new ArrayList<>();

    // Query the ETA tree to find the orders with ETA within range [startTime,
    // endTime].
    List<Order> toBeDelivered = getToBeDeliveredOrdersBetweenTime(startTime, endTime);
    if (toBeDelivered.isEmpty()) {
      res.add("There are no orders in that time period");
      return res;
    }

    // Return the string representation.
    res.add("[" + String.join(", ",
        toBeDelivered.stream().map(o -> String.format("%d", o.orderId)).collect(Collectors.toList())) + "]");
    return res;
  }

  /**
   * Returns the rank of an order or how many orders will be delivered before it.
   */
  List<String> getRankOfOrder(int orderId) {
    Order order = orderMap.get(orderId);
    if (order == null)
      return null;

    // Count the number of inorder predecessor of this order based on the
    // priorities.
    int rank = etaTree.getInorderPredecessors(order).size();
    return List.of(String.format("Order %d will be delivered after %d orders.", orderId, rank));
  }

  /**
   * Cancels an order, deletes it from the system and pushes up the ETAs of the
   * orders with lower priority than the cancelled order's priority and finally
   * prints all the delivered orders upto the supplied time.
   */
  List<String> cancelOrder(int orderId, int currentSystemTime) {
    List<String> res = new ArrayList<>();

    // Update the current system time and internally collect the pending orders to
    // be delivered upto this time.
    setCurrentSystemTime(currentSystemTime);

    Order order = orderMap.get(orderId);
    // If the order is out of delivery or has already been delivered, it cannot be
    // cancelled.
    if (order == null || isDelivered(order) || isOutForDelivery(order)) {
      res.add(String.format("Cannot cancel. Order %d has already been delivered.", orderId));
    } else {
      // If not, cancel the order.
      System.out.println("Cancelling order " + orderId + " at " + currentSystemTime);
      res.add(String.format("Order %d has been canceled", orderId));

      // And shift up the orders with priority lower than the cancelled order's
      // priority.
      List<Order> lowerPriorityOrders = getLowerPriorityOrders(order);
      deleteOrder(order);
      Map<Integer, Integer> updates = updateETA(lowerPriorityOrders, -2 * order.deliveryTime);

      // Get the updated ETAs.
      String updatedETAs = getUpdatedETAString(updates);
      System.out.println("updatedETA: " + updatedETAs);
      if (updatedETAs != null)
        res.add(updatedETAs);
    }

    // Delivers all the pending orders upto the supplied time.
    List<String> delivered = deliverOrders();
    System.out.println("Delivered: " + delivered);
    if (!delivered.isEmpty())
      res.addAll(delivered);

    return res;
  }

  /**
   * Checks if an order is delivered or not.
   */
  private boolean isDelivered(Order order) {
    // If current time is more than this order's ETA, then this order has already
    // been delivered.
    if (order.eta <= currentSystemTime)
      return true;

    return false;
  }

  /**
   * Checks if an order is out for delivery or not.
   */
  private boolean isOutForDelivery(Order order) {
    // If this order is currently out for delivery, it cannot be cancelled. If not
    // it can be cancelled.
    Order outForDelivery = getCurrentlyDeliveringOrder();

    if (outForDelivery == null)
      return false;
    if (order.orderId == outForDelivery.orderId)
      return true;

    return false;
  }

  /**
   * Updates an order with its new deliveryTime and finally prints all the
   * delivered orders upto the supplied time.
   */
  List<String> updateTime(
      int orderId,
      int currentSystemTime,
      int newDeliveryTime) {
    List<String> res = new ArrayList<>();

    // Update the current system time and internally collect the pending orders to
    // be delivered upto this time.
    setCurrentSystemTime(currentSystemTime);

    Order order = orderMap.get(orderId);
    // If the order is out of delivery or has already been delivered, it cannot be
    // updated.
    if (order == null || isDelivered(order) || isOutForDelivery(order)) {
      res.add(String.format("Cannot update. Order %d has already been delivered.", orderId));
    } else {
      System.out.println("Updating order " + orderId + " at " + currentSystemTime);
      // Calculate the change in deliverTime.
      int oldDeliveryTime = order.deliveryTime;
      int change = newDeliveryTime - oldDeliveryTime;

      // Update the order.
      order.deliveryTime = newDeliveryTime;
      order.setEta(order.deliveryStartTime + newDeliveryTime);

      // Update the lower priority orders.
      List<Order> lowerPriorityOrders = getLowerPriorityOrders(order);
      Map<Integer, Integer> updates = updateETA(lowerPriorityOrders, 2 * change);
      updates.put(order.eta, orderId);

      // Get the updated ETAs
      String updatedETAs = getUpdatedETAString(updates);
      System.out.println("updatedETA: " + updatedETAs);
      if (updatedETAs != null)
        res.add(updatedETAs);
    }

    // Delivers all the pending orders upto the supplied time.
    List<String> delivered = deliverOrders();
    System.out.println("Delivered: " + delivered);
    if (!delivered.isEmpty())
      res.addAll(delivered);

    return res;
  }

  /**
   * Delivers rest of the orders in the system.
   */
  List<String> quit() {
    // Set current time to infinite and deliver all order upto that time, i.e. all
    // orders in the system.
    setCurrentSystemTime(Integer.MAX_VALUE);
    return deliverOrders();
  }
}
