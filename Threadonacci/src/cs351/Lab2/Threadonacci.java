//***********************************
// Kevin Cox
//
// print fibonacci nums with threads
//***********************************


package cs351.Lab2;

public class Threadonacci
{

  public static void main(String[] args)
  {
    Threadonacci app = new Threadonacci();
    FiboThread thread1 = app.new FiboThread("thread 1");
    FiboThread thread2 = app.new FiboThread("thread 2");

    thread1.start();
    thread2.start();

    for (int i = 0; i < 10; i++)
    {
      Data temp1 = thread1.getInfo();
      Data temp2 = thread2.getInfo();
      System.out.println(thread1.NAME + " step: " + temp1.steps + ": x = " + temp1.x +
              " y = " + temp1.y + " z = " + temp1.z);
      System.out.println(thread2.NAME + " step: " + temp2.steps + ": x = " + temp2.x +
              " y = " + temp2.y + " z = " + temp2.z);
      System.out.println();
      try
      {
        Thread.sleep(2000);
      } catch (InterruptedException e)
      {
        e.printStackTrace();
      }
    }
    thread1.running = false;
    thread2.running = false;
  }

  /**
   * New thread object, Overrided run to compute fibonacci
   */
  class FiboThread extends Thread
  {
    private final String NAME;

    private FiboThread(String NAME)
    {
      this.NAME = NAME;
    }

    private Data data = new Data();
    private final Object LOCK = new Object();
    private long temp = 0;
    private boolean running = true;

    @Override
    public void run()
    {
      while (running)
      {
        calculate();
      }
    }

    /**
     * computes the new Fibonacci numbers for each thread
     */
    private void calculate()
    {
      synchronized (LOCK)
      {
        data.steps++;
        temp = data.z;
        data.z = data.z + data.y;
        //noinspection SuspiciousNameCombination
        data.x = data.y;
        data.y = temp;
        if (data.z <= 0)
        {
          data.z = 2;
          data.x = 1;
          data.y = 1;
        }
      }
    }

    /**
     * creates a copy of the data for future access
     * @return new data object copied from old
     */
    private Data getInfo()
    {
      synchronized (LOCK)
      {
        return new Data(data);
      }
    }

  }

  /**
   * Object to store long x,y,z, steps
   * and to create and copy the olds data
   * in order to reference correct data fields.
   */
  private class Data
  {
    public Data()
    {

    }

    public Data(Data data)
    {
      this.z = data.z;
      this.y = data.y;
      this.x = data.x;
      this.steps = data.steps;
    }

    private long z = 2;
    private long y = 1;
    private long x = 1;
    private long steps = 0;
  }

}

