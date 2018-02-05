/**
 * Created by Kevin Cox on 4/11/2017.
 * Simple class to keep track of all inventory and balances (like Amazon)
 */
class ThneedStore
{
  private volatile int inventory;
  private volatile double balance;

  ThneedStore()
  {
    inventory = 0;
    balance = 1000;
  }


  /**
   * Simple getter for the Thneeds inventory. Used for tracking.
   * @return inventory of Thneeds
   */
  synchronized int getInventory()
  {
    return inventory;
  }

  /**
   * Simple getter for the balance inside the store.
   * @return balance of money in store
   */
  synchronized double getBalance()
  {
    return balance;
  }

  /**
   * Method called when client issues the buy command. Checks if balance can
   * support the number being asked for.
   * @param quantity of Thneeds being bought
   * @param price for each Thneed
   */
  synchronized void buy(int quantity, double price)
  {
    if (quantity * price <= balance)
    {
      balance -= quantity * price;
      inventory += quantity;
      System.out.println("buying");
    }
  }

  /**
   * Method called when client issues the sell command. Checks if the inventory
   * can support the number being asked for.
   * @param quantity of Thneeds
   * @param price for each Thneed
   */
  synchronized void sell(int quantity, double price)
  {
    if (quantity <= inventory)
    {
      inventory -= quantity;
      balance += quantity * price;
    }
  }

}
