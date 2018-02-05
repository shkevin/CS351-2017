import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;

public class ServerMaster
{
  private ServerSocket serverSocket;
  private LinkedList<ServerWorker> allConnections = new LinkedList<>();

  private ThneedStore store;

  /**
   * Constructor method to create new server, done once.
   * @param portNumber for the server
   */
  public ServerMaster(int portNumber)
  {
    store = new ThneedStore();
    try
    {
      serverSocket = new ServerSocket(portNumber);
    }
    catch (IOException e)
    {
      System.err.println("Server error: Opening socket failed.");
      e.printStackTrace();
      System.exit(-1);
    }

    waitForConnection(portNumber);
  }

  private void waitForConnection(int port)
  {
    String host = "";
    try
    {
      host = InetAddress.getLocalHost().getHostName();
    }
    catch (UnknownHostException e)
    {
      e.printStackTrace();
    }
    while (true)
    {
//      System.out.println("socketlab.ServerMaster("+host+"): waiting for Connection on port: "+port);
      try
      {
        Socket client = serverSocket.accept();
        ServerWorker worker = new ServerWorker(client, this);
        worker.start();
        System.out.println("socketlab.ServerMaster: *********** new Connection");
        allConnections.add(worker);
//        worker.send("socketlab.ServerMaster says hello!");
      }
      catch (IOException e)
      {
        System.err.println("Server error: Failed to connect to client.");
        e.printStackTrace();
      }

    }
  }

  /**
   * Removes the worker thread when client is closed. Stops the thread from running.
   * @param worker for the client
   */
  void cleanConnectionList(ServerWorker worker)
  {
    allConnections.remove(allConnections.get(allConnections.indexOf(worker)));
    worker.interrupt();
  }

  /**
   * used to broadcast to all clients when a sell or buy is completed.
   * @param s message from worker
   */
  void broadcast(String s)
  {
    for (ServerWorker workers : allConnections)
    {
      workers.send(s);
    }
  }

  /**
   * Simple getter to access the store from the server. Called from server worker class.
   * @return the ThneedStore
   */
  ThneedStore getStore()
  {
    return store;
  }

  /**
   * Main class for the server, instantiates the port used for the server.
   * @param args
   */
  public static void main(String args[])
  {
    //Valid port numbers are Port numbers are 1024 through 65535.
    //  ports under 1024 are reserved for system services http, ftp, etc.
    int port = 5555; //default
    if (args.length > 0)
    try
    {
      port = Integer.parseInt(args[0]);
      if (port < 1) throw new Exception();
    }
    catch (Exception e)
    {
//      System.out.println("Usage: socketlab.ServerMaster portNumber");
      System.exit(0);
    }
    new ServerMaster(port);
  }
}
