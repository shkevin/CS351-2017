import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerWorker extends Thread
{
  private Socket client;
  private PrintWriter clientWriter;
  private BufferedReader clientReader;
  private ServerMaster server;

  /**
   * Thread class used for solely listening to client input
   * @param client which worker is issued to
   * @param server which worker is a subordinate of
   */
  ServerWorker(Socket client, ServerMaster server)
  {
    this.server = server;
    this.client = client;

    try
    {
      //          PrintWriter(OutputStream out, boolean autoFlushOutputBuffer)
      clientWriter = new PrintWriter(client.getOutputStream(), true);
    } catch (IOException e)
    {
      System.err.println("Server Worker: Could not open output stream");
      e.printStackTrace();
    }
    try
    {
      clientReader = new BufferedReader(new InputStreamReader(client.getInputStream()));

    } catch (IOException e)
    {
      System.err.println("Server Worker: Could not open input stream");
      e.printStackTrace();
    }
  }

  /**
   * Sends a message to the client.
   * @param msg for the client
   */
  void send(String msg)
  {
//    System.out.println("socketlab.ServerWorker.send(" + msg + ")");
    clientWriter.println(msg);
  }

  private void parseInput(String msg)
  {
    int quantity = 0;
    double price = 0;
    if (msg.startsWith("buy:")||msg.startsWith("sell:"))
    {
      String[] message = msg.split("\\s+");
      quantity = Integer.parseInt(message[1]);
      price = Double.parseDouble(message[2]);
    }
    if (msg.startsWith("buy:"))
    {
      server.getStore().buy(quantity, price);
      server.broadcast("update " + server.getStore().getInventory() + " " +
              server.getStore().getBalance());
    }
    else if (msg.startsWith("sell:"))
    {
      server.getStore().sell(quantity, price);
      server.broadcast("update " + server.getStore().getInventory() + " " +
              server.getStore().getBalance());
      System.out.println("selling");
    }
    else if (msg.startsWith("inventory:"))
    {
      send("Thneeds: " + server.getStore().getInventory());
    }
    else if (msg.equalsIgnoreCase("quit:"))
    {
      send("quit");
      server.cleanConnectionList(this);
    }
    else send("Error" + " " + msg);
  }

  /**
   * Run method inherently called when this thread class is called (worker.start())
   */
  public void run()
  {
    while (!client.isClosed())
    {
      try
      {
        String msg = clientReader.readLine();
        if (msg == null) break;

        parseInput(msg);
      } catch (IOException e)
      {
        System.err.println("Server Worker: Could not read from client socket");
        e.printStackTrace();
      }

    }
  }

}
