package antworld.client.game_map;

import antworld.client.Ant;
import antworld.client.Client;
import antworld.client.ai.Brain;
import antworld.common.*;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static antworld.common.Constants.TIME_STEP_MSEC;

/**
 * This class is a massive collection of bookkeeping. It sends and recieves packets from the NetworkInterface
 * and persists that information as Tiles
 */
public class GameMap
{
  public final static BufferedImage GAME_MAP_IMAGE = Util.loadImage("AntWorld.png", null);
  public final static int WIDTH = GAME_MAP_IMAGE.getWidth(), HEIGHT = GAME_MAP_IMAGE.getHeight();

  private NestData nest;
  private Vec2 nestPos;
  private Brain brain = new Brain(this);

  private HashMap<Integer, Ant> ants = new HashMap<>();
  private ArrayList<Ant> newAnts = new ArrayList<>();
  private ArrayList<AntData> antsToSend = new ArrayList<>();
  private ArrayList<AntData> enemyAntData;
  private ArrayList<FoodData> foodSeen;

  private Tile[][] tiles = new Tile[WIDTH][HEIGHT];
  private LinkedList<Tile> nestToWaterPath;
  private Tile waterTile;

  private Direction[][] directionsToNest;

  public GameMap()
  {
    loadMap();
  }

  private void loadMap()
  {
    for (int x = 0; x < WIDTH; x++)
      for (int y = 0; y < HEIGHT; y++)
        tiles[x][y] = new Tile(GAME_MAP_IMAGE.getRGB(x, y), new Vec2(x, y), this);
  }

  /**
   * Used to spawn new ants at the nest
   * @param type the type of ant to spawn
   * @param count the number of that type of ant to spawn
   */
  public synchronized void spawnAnts(AntType type, int count)
  {
    for (int i = 0; i < count; i++)
    {
      antsToSend.add(new AntData(type, Client.TEAM));
    }
  }

  /**
   * @return an array list of all the ants
   * @author Adrian, Alex
   */
  public Collection<Ant> getAnts()
  {
    return ants.values();
  }

  /**
   * Get our nest from the list of nests on the map
   * Assumes only one nest, error if our nest isn't present.
   *
   * @param nests nest data from server packet
   */
  private void updateNest(NestData[] nests)
  {
    boolean firstTime = nest == null;
    nest = Arrays.stream(nests).filter(n -> n.team == Client.TEAM).findFirst().get();

    if (firstTime) {
      nestPos = new Vec2(nest.centerX, nest.centerY);
      directionsToNest = getDirectionsToTile(getTile(nest.centerX, nest.centerY), 1_000_000); // get all paths
      waterTile = findClosestWater(nest.centerX, nest.centerY);
      System.out.println("closest water to nest: x="+waterTile.getLocation().x+" y="+waterTile.getLocation().y);
    }
  }

  /**
   * @return the position of the nest as a Vec2
   */
  public Vec2 getNestPos() { return this.nestPos; }

  /**
   * Finds the closest water to a point
   * @param startX the x coord
   * @param startY the y coord
   * @return the Tile that contains water closest to (startX, startY)
   */
  public Tile findClosestWater(int startX, int startY) {
    //int nestX = nest.centerX;
    //int nestY = nest.centerY;
    int waterX = 0;
    int waterY = 0;
    int waterPixel = LandType.WATER.getMapColor();
    //System.out.println("startX="+startX+" y="+startY);

    waterFinder: for (int d = 0; d < 1000; d++) {
      for (int x = (startX-d); x < (startX+d+1); x++) {
        //System.out.println("[x="+x+", y="+(startY-d)+"] [x="+x+", y="+(startY+d)+"]");

        if (x < 0 || x > WIDTH -1) continue;

        if ((startY-d >= 0) && (GAME_MAP_IMAGE.getRGB(x, startY-d) & 0x00FFFFFF) == waterPixel) {
          waterX = x;
          waterY = startY-d;
          break waterFinder;
        }

        if((startY+d < HEIGHT) && (GAME_MAP_IMAGE.getRGB(x, startY+d) & 0x00FFFFFF) == waterPixel) {
          waterX = x;
          waterY = startY+d;
          break waterFinder;
        }
      }

      for(int y = (startY-d+1); y < (startY+d); y++) {
        //System.out.println("[x="+(startX-d)+", y="+y+"] [x="+(startX+d)+", y="+y+"]");

        if (y < 0 || y > HEIGHT -1) continue;


        if((startX-d >= 0) && (GAME_MAP_IMAGE.getRGB(startX-d, y) & 0x00FFFFFF) == waterPixel) {
          waterX = startX-d;
          waterY = y;
          break waterFinder;
        }

        if((startX+d < WIDTH) && (GAME_MAP_IMAGE.getRGB(startX+d, y) & 0x00FFFFFF) == waterPixel) {
          waterX = startX+d;
          waterY = y;
          break waterFinder;
        }
      }
    }

    /*if (waterX != 0 && waterY != 0) {
      System.out.println("waterX="+waterX+" waterY="+waterY);
      Tile nestTile = getTile(nestX, nestY);
      waterTile = getTile(waterX, waterY);
      //nestToWaterPath = aStar(nestTile, waterTile); //TODO: save an aStarred path
    }*/
    return getTile(waterX, waterY);
  }

  /**
   * @return the water tile that is closest to the nest
   */
  public Tile getWaterTile() { return this.waterTile; }

  /**
   * Update all ants with the new packet, checking if new ants were added
   * @param myAnts the list of updated AntData objects
   */
  private void updateAnts(ArrayList<AntData> myAnts)
  {
    for (AntData data : myAnts)
    {
      Ant ant = ants.get(data.id);

      if (ant != null)
      {
        //remove old, dead ants
        if(data.action.type == AntAction.AntActionType.DIED) {
          ants.remove(data.id);
        }
        ant.setData(data);
      }
      else
      {
        Ant newAnt = new Ant(data, this);
        ants.put(data.id, newAnt);
        newAnts.add(newAnt);
      }
    }
  }

  /**
   * Returns a single ant of type 'type' from the newAnts list
   * This 'new ants' list is used to assign ants to squads/other organized units. Any ants that have been recently
   * spawned will be in this list, and then removed when this function is called
   * @param type the ant type to get
   * @return the ant object
   */
  public synchronized Ant getNewAnt(AntType type) {
    Ant recruit = newAnts.stream().filter(ant -> ant.getData().antType == type).findFirst().orElse(null);
    if (recruit != null) newAnts.remove(recruit);
    return recruit; // TODO: set for new ants
  }

  /**
   * Recieves a list of new FoodData objects seen by our ants and stores that information in the array of Tiles
   * @param foodList the new FoodData objects
   */
  private void processFood(ArrayList<FoodData> foodList) {
    // TODO: this is probably bad but it works (it's actually not too many calcualtions as there is very little food in general

    //go through all the food and remove it from the map
    if(foodSeen != null)
    {
      for (FoodData oldFood : foodSeen)
      {
        Tile current = getTile(new Vec2(oldFood.gridX, oldFood.gridY));
        current.setFood(null);
      }
    }
    foodSeen = foodList;
    //go through all the food from the recent packet and add it to the map
    if(foodSeen != null) {
      for(FoodData newFood : foodSeen)
      {
        Tile current = getTile(new Vec2(newFood.gridX, newFood.gridY));
        current.setFood(newFood);
      }
    }
  }

  /**
   * Called by NetworkInterface with the new packet from the server
   * @param packet the new packet
   */
  public void updateWithPacketFromServer(PacketToClient packet)
  {
    updateNest(packet.nestData);
    updateAnts(packet.myAntList);
    processFood(packet.foodList); // TODO: foodSeen == null checks
    enemyAntData = packet.enemyAntList;

    if (nestToWaterPath != null) System.out.println(nestToWaterPath.toString());

    long start = System.currentTimeMillis();

    try {
      brain.update().get(TIME_STEP_MSEC - 8, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (TimeoutException e) {
      System.err.println("Error: Could not finish in time");
    }

    // System.out.println("Time taken (ms): " + (System.currentTimeMillis() - start));
  }

  /**
   * @return the NestData object for our nest
   */
  public NestData getNest()
  {
    return nest;
  }

  /**
   * @return the ArrayList of FoodData objects currently seen by our ants
   */
  public ArrayList<FoodData> getFoodSeen()
  {
    return foodSeen;
  }

  /**
   * @return all the enemy ants currently seen by our ants
   */
  public ArrayList<AntData> getEnemyAnts()
  {
    return enemyAntData;
  }

  /**
   * @return the array of ants to send to the server next tick
   */
  public synchronized ArrayList<AntData> getAntsToSend()
  {
    ArrayList<AntData> toSend = new ArrayList<>(antsToSend);
    antsToSend.clear();
    return toSend;
  }

  /**
   * @param data the AntData object to send to the server
   */
  public synchronized void sendAntAction(AntData data)
  {
    antsToSend.add(data);
  }

  /**
   * Fetch the pixel value for a given coordinate
   * @param x the x value
   * @param y the y value
   * @return the int value in the standard java int color format of that particular coordinate
   */
  public int getRGB(int x, int y)
  {
    if(x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return 0;
    return GAME_MAP_IMAGE.getRGB(x, y);
  }

  /**
   * @param tile the tile you are currently on
   * @return the Direction to move to move one step towards the nest
   */
  public Direction getToNestDirection(Tile tile) {
    return directionsToNest[tile.getLocation().x][tile.getLocation().y];
  }

  /**
   * @param pos the position to check
   * @return true if pos is within bounds of the map
   */
  public static boolean inBounds(Vec2 pos)
  {
    return pos.x >= 0 && pos.x < WIDTH &&
           pos.y >= 0 && pos.y < HEIGHT;
  }

  private Direction[][] getDirectionsToTile(Tile startTile, int radius) {
    Direction[][] directions = new Direction[WIDTH][HEIGHT];

    PriorityQueue<Tile> frontier = new PriorityQueue<>(Comparator.comparing(Tile::getgCost));
    HashSet<Tile> closed = new HashSet<>();
    ArrayList<Tile> edgeTiles = new ArrayList<>();
    frontier.offer(startTile);
    closed.add(startTile);

    while (!frontier.isEmpty())
    {
      Tile currentLocation = frontier.poll();
      currentLocation.getAdjacencies();
      closed.add(currentLocation);

      for (Tile location : currentLocation.getAdjacencies())
      {
        if (closed.contains(location)) continue;
        if (!frontier.contains(location))
        {
          if(currentLocation.getgCost() <= radius) {
            //ignore locations that aren't passable
            if (!location.isPassable()) continue;
            location.setgCost(currentLocation.getgCost() + 1);
            location.setCameFrom(currentLocation);
            frontier.offer(location);
            closed.add(location);
            //TODO
          } else {
            edgeTiles.add(currentLocation);
          }
        }
      }
    }

    for(Tile tile : closed)
    {
      Vec2 pos = tile.getLocation();
      Tile parent = tile.getCameFrom();
      if(parent != null) { // because the starting tile won't have a direction
        Direction d = parent.getLocation().sub(pos).getDirection();
        directions[pos.x][pos.y] = d;
      }
    }

    return directions;

  }

  public static void main(String[] args) {
    GameMap map = new GameMap();
    long startTime = System.currentTimeMillis();

    Tile start = map.getTile(200, 350);
    Tile end = map.getTile(900, 550);

    System.out.println(start + " " + end);

    Direction[][] dirs = map.getDirectionsToTile(start, 20);
    System.out.println(System.currentTimeMillis() - startTime);
    System.out.println(dirs[start.getLocation().x + 1][start.getLocation().y + 1]);

    for(int y = 190; y < 205; y++) {
      for(int x = 345; x < 355; x++) {
        System.out.print(String.format("%10s", dirs[y][x]) + " ");
      }
      System.out.println();
    }
  }

  /**
   * @param startLocation  of ant
   * @param targetLocation of object to travel to
   * @author Kevin Cox
   * Calculates a path to the target location from the start location.
   */
  public LinkedList<Tile> aStar(Tile startLocation, Tile targetLocation)
  {
    PriorityQueue<Tile> frontier = new PriorityQueue<>(Comparator.comparing(Tile::getfCost));
    HashSet<Tile> closed = new HashSet<>();
    frontier.offer(startLocation);
    closed.add(startLocation);
    LinkedList<Tile> tempPath = null;

    while (!frontier.isEmpty())
    {
      Tile currentLocation = frontier.poll();
      currentLocation.getAdjacencies();
      closed.add(currentLocation);

      if (currentLocation.getLocation().equals(targetLocation.getLocation()))
      {
        tempPath = constructPath(currentLocation);
        Collections.reverse(tempPath);
        break;
      }

      for (Tile location : currentLocation.getAdjacencies())
        {
        if (closed.contains(location)) continue;
        if (!frontier.contains(location))
        {
          //ignore locations that aren't passable
          if (!location.isPassable()) continue;
          //take into account the height cost
          if(location.height > currentLocation.height) location.setgCost(currentLocation.getgCost() + 2);
          else location.setgCost(currentLocation.getgCost() + 1);
          location.setfCost(location.getgCost() + location.manhattanDist(targetLocation));
          location.setCameFrom(currentLocation);
          frontier.offer(location);
          closed.add(location);
        }
      }
    }

    return tempPath;
  }

  /**
   * @param currentLocation of tile (essentially target location)
   * @return path of ant
   * @author Kevin Cox
   * Constructs a path for an ant based off of the current tile (target)
   * and iterates through the current tile's parent to build up a
   * linkedlist for the ants path.
   */
  private LinkedList<Tile> constructPath(Tile currentLocation)
  {
    LinkedList<Tile> path = new LinkedList<>();
    Tile currentTile = currentLocation;
    while (currentTile != null)
    {
      path.add(currentTile);
      currentTile = currentTile.getCameFrom();
    }
    return path;
  }

  public Tile getTile(int x, int y)
  {
    return tiles[x][y];
  }

  /**
   * @param pos tile location
   * @return the tile at the given location
   */
  public Tile getTile(Vec2 pos) {
    return tiles[pos.x][pos.y];
  }
}
