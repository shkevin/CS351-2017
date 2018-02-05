package antworld.client.networking;

import antworld.client.Client;
import antworld.client.game_map.GameMap;
import antworld.common.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class NetworkInterface
{
  private Socket connection;
  private ObjectOutputStream writer;
  private ObjectInputStream reader;
  private Thread listener;
  private String host;
  private PacketToServer packetToServer;

  private GameMap map;

  private double serverStartTime = -1.0;
  private long clientStartTime = 0;
  private long latency = 0;
  private final double latencyBufferFactor = 1.2;

  public NetworkInterface(String serverHostName, GameMap map)
  {
    host = serverHostName;
    this.map = map;
  }

  public void connect(boolean reconnect) throws IOException
  {
    //connect
    connection = new Socket(host, Constants.PORT);
    writer = new ObjectOutputStream(connection.getOutputStream());
    reader = new ObjectInputStream(connection.getInputStream());

    // If we've successfully connected, start listening to the server
    listener = new Thread(this::readLoop);
    listener.start();

    // send the connection packet
    packetToServer = new PacketToServer(Client.TEAM);
    send(null);
  }

  void close() throws IOException
  {
    listener.interrupt();
    connection.close();
  }

  private void send(ArrayList<AntData> ants)
  {
    try
    {
      packetToServer.myAntList = ants;
      writer.writeObject(packetToServer);
      writer.flush();
      writer.reset();
    }
    catch(IOException e)
    {
      System.out.println("Error: Failed to send object <" + packetToServer.toString() + ">");
      e.printStackTrace();
    }
  }

  private void readLoop()
  {
    System.out.println("NetworkInterface: readLoop starting");
    try
    {
      while(true)
      {
        PacketToClient packet = (PacketToClient) reader.readObject();

        if(packet.errorMsg != null)
        {
          System.err.println(packet.errorMsg);
        }
        //establish when the server/client started
        if(serverStartTime == -1.0)
        {
          serverStartTime = 0.0;
        }
        if(clientStartTime == 0)
        {
          clientStartTime = System.nanoTime();
        }

        //TODO: calculate the network latency
        //System.out.println((System.nanoTime() - clientStartTime) / 1000000);
        //System.out.println((long) ((packet.tickTime - serverStartTime) * 1000000000 / 1000000)); //convert to nanos
        //latency = (System.nanoTime() - clientStartTime) - (long) ((packet.tickTime - serverStartTime) * 1000000000); //convert to nanos
        map.updateWithPacketFromServer(packet);

        //read
        //long totalTravelTimeMillis = (long) (latency * 2 * latencyBufferFactor) / 1_000_000;
        long totalTravelTimeMillis = 10;
        //System.out.println("travel time latency" + totalTravelTimeMillis + " " + latency);
        //Thread.sleep(Constants.TIME_STEP_MSEC - totalTravelTimeMillis);
        send(map.getAntsToSend());

      }
    }
    catch(SocketException e)
    {
      // This isn't an error, we stop the blocking read by closing the socket
      // in close(), which results in a SocketException.
      System.out.println("Blocking read terminated: connection closed");
    }
    catch(IOException e)
    {
      System.out.println("Error: Failed to read from server.");
      e.printStackTrace();
    }
    catch(ClassNotFoundException e)
    {
      System.out.println("Error: Unkown interface object recieved from server.");
      e.printStackTrace();
    }
    /*
    catch(InterruptedException e)
    {
      e.printStackTrace();
      System.err.println("NetworkInterface: write loop inturrupted!");
    }
    */
  }
}
