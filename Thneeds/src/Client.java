import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.Scanner;

/*
 Client class used to simulate a person wishing to buy or sell from
 ThneedStore
 */

public class Client
{
  private Socket clientSocket;
  private PrintWriter write;
  private BufferedReader reader;
  private long startNanoSec;
  private Scanner keyboard;
  private ClientListener listener;

  private int thneedsInStore;
  private double balanceInStore;
  NumberFormat nf = NumberFormat.getCurrencyInstance();

  private Client(String host, int portNumber)
  {
    startNanoSec = System.nanoTime();
//    System.out.println("Starting socketlab.Client: " + timeDiff());

    keyboard = new Scanner(System.in);

    while (!openConnection(host, portNumber))
    {
    }

    listener = new ClientListener();
//    System.out.println("socketlab.Client(): Starting listener = : " + listener);
    listener.start();

    listenToUserRequests();

    closeAll();

  }


  private boolean openConnection(String host, int portNumber)
  {

    try
    {
      clientSocket = new Socket(host, portNumber);
    } catch (UnknownHostException e)
    {
//      System.err.println("socketlab.Client Error: Unknown Host " + host);
      e.printStackTrace();
      return false;
    } catch (IOException e)
    {
//      System.err.println("socketlab.Client Error: Could not open connection to " + host
//              + " on port " + portNumber);
      return false;
    }

    try
    {
      write = new PrintWriter(clientSocket.getOutputStream(), true);
    } catch (IOException e)
    {
//      System.err.println("socketlab.Client Error: Could not open output stream");
      e.printStackTrace();
      return false;
    }
    try
    {
      reader = new BufferedReader(new InputStreamReader(
              clientSocket.getInputStream()));
    } catch (IOException e)
    {
//      System.err.println("socketlab.Client Error: Could not open input stream");
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private void listenToUserRequests()
  {
    while (true)
    {
//      System.out.println("Thneeds in Inventory = " + thneedsInStore);
//      System.out.println("Enter Command (Buy: # | Sell: #):");
      String cmd = keyboard.nextLine();
      if (cmd == null) continue;
      if (cmd.length() < 1) continue;

      char c = cmd.charAt(0);
      if (c == 'q') break;
      if (parseInput(cmd)) write.println(cmd);
    }
  }

  private Boolean parseInput(String msg)
  {
    if (msg.equalsIgnoreCase("inventory:")) return true;
    if (msg.equalsIgnoreCase("quit:")) return true;
    if (msg.startsWith("buy:") || msg.startsWith("sell:"))
    {
      String[] message = msg.split("\\s+");
      int quantity = Integer.parseInt(message[1]);
      Double price = Double.parseDouble(message[2]);
      int decimalCount = price.toString().split("\\.").length;
      return (quantity > 0) && (price > 0) && (decimalCount == 2);
    }
    return false;
  }

  private void closeAll()
  {
//    System.out.println("socketlab.Client.closeAll()");

    if (write != null) write.close();
    if (reader != null)
    {
      try
      {
        reader.close();
        clientSocket.close();
      } catch (IOException e)
      {
//        System.err.println("socketlab.Client Error: Could not close");
        e.printStackTrace();
      }
    }
    System.exit(0);
  }

  private String timeDiff()
  {
    long namoSecDiff = System.nanoTime() - startNanoSec;
    double secDiff = (double) namoSecDiff / 1000000000.0;
    return String.format("%.6f", secDiff);

  }

  /**
   * @param args main class to initiate a Client with the specified port.
   */
  public static void main(String[] args)
  {

    String host = null;
    int port = 0;

    try
    {
      host = args[0];
      port = Integer.parseInt(args[1]);
      if (port < 1) throw new Exception();
    } catch (Exception e)
    {
//      System.out.println("Usage: socketlab.Client hostname portNumber");
      System.exit(0);
    }
    new Client(host, port);
  }

  /**
   * Simple thread to handle listening to the client, calls read which reads output
   * from the server.
   */
  class ClientListener extends Thread
  {
    public void run()
    {
//      System.out.println("ClientListener.run()");
      while (true)
      {
        read();
      }

    }

    /**
     * Reads output from server and addresses the commands appropriately.
     */
    private void read()
    {
      try
      {
        String msg = reader.readLine();
        if (msg.startsWith("Thneeds:"))
        {
          int idxOfNum = msg.indexOf(':') + 2;
          int n = Integer.parseInt(msg.substring(idxOfNum));
          thneedsInStore = n;
          System.out.println("Current Invintory of Thneeds (" + timeDiff()
                  + ") = " + thneedsInStore  + " | Balance = " + nf.format(balanceInStore));
        }
//        else if (msg.startsWith("You just bought "))
//        {
////          System.out.println("Success: " + msg);
//        }
        else if (msg.equals("quit"))
        {
          closeAll();
        }
        else if (msg.startsWith("Error"))
        {
//          System.out.println("Failed: " + msg);
        }
        else if (msg.startsWith("update"))
        {
          String[] message = msg.split("\\s+");
          int quantity = Integer.parseInt(message[1]);
          Double price = Double.parseDouble(message[2]);
          thneedsInStore = quantity;
          balanceInStore = price;
        }
        else
        {
//          System.out.println("Unrecognized message from Server(" + timeDiff()
//                  + ") = " + msg);
        }

      } catch (IOException e)
      {
        e.printStackTrace();
      }
    }

  }

}
