/**
 * The Order object. Stores all information about to an order.
 */
class Order {
  final float VALUE_WEIGHT = 0.3f;
  final float TIME_WEIGHT = 0.7f;
  int orderId;
  int orderValue;
  int orderCreationTime;
  int deliveryTime;
  double priority;
  int deliveryStartTime;
  int eta;

  /**
   * Constructor calculates the priority and populates all other fields of an
   * order.
   */
  Order(
      int orderId,
      int orderCreationTime,
      int orderValue,
      int deliveryTime) {
    this.orderId = orderId;
    this.orderValue = orderValue;
    this.orderCreationTime = orderCreationTime;
    this.deliveryTime = deliveryTime;
    this.priority = (VALUE_WEIGHT * (orderValue / 50)) - (TIME_WEIGHT * orderCreationTime);
  }

  /**
   * Sets the ETA and the deliveryStartTime of an order.
   */
  void setEta(int eta) {
    this.eta = eta;
    this.deliveryStartTime = this.eta - this.deliveryTime;
  }

  @Override
  public String toString() {
    return String.format(
        "[%d, %d, %d, %d, %d]",
        orderId,
        orderCreationTime,
        orderValue,
        deliveryTime,
        eta);
  }
}
