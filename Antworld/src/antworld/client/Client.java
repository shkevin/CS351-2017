package antworld.client;

import antworld.client.game_map.GameMap;
import antworld.client.networking.NetworkInterface;
import antworld.common.AntType;
import antworld.common.TeamNameEnum;

import java.io.IOException;

public class Client
{
  public static final TeamNameEnum TEAM = TeamNameEnum.Weaver;
  private NetworkInterface server;
  private final GameMap map;

  private Client(String host, boolean reconnection)
  {
    //create the map
    map = new GameMap();
    //connect
    server = new NetworkInterface(host, map);
    try
    {
      server.connect(reconnection);
    }
    catch(IOException e)
    {
      System.out.println();
    }
    map.spawnAnts(AntType.EXPLORER, 1); //Just test one ant, remove when finished
  }

  /**
   * @param args Array of command-line arguments (See usage()).
   */
  public static void main(String[] args)
  {
    String serverHost = "localhost";
    boolean reconnection = false;
    if(args.length > 0) serverHost = args[args.length - 1];

    new Client(serverHost, reconnection);
  }
}
