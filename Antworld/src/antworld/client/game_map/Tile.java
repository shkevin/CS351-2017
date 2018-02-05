package antworld.client.game_map;

import antworld.client.Ant;
import antworld.common.Constants;
import antworld.common.Direction;
import antworld.common.FoodData;
import antworld.common.LandType;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Stores a single pixel's data
 */
public class Tile
{
  LandType tileType;
  final int height;
  private ArrayList<Tile> adjacencies;
  private Tile cameFrom;
  private Vec2 location;
  private GameMap map;
  private Ant ant;
  private FoodData food;

  private int gCost;
  private int fCost;

  private int weightRadius = 5;

  /**
   * @return true if the pixel is water
   */
  public boolean isWater()
  {
    return tileType == LandType.WATER;
  }

  /**
   * An enum of types of tile weights
   */
  public enum WeightType {
    FOOD, ENEMIES, TERRAIN, NEST, TIME
  }

  private HashMap<WeightType, Double> weights;

  /**
   * @param pixel the pixel color data encoded in a single int
   * @param location the coordinate of the tile
   * @param map a reference to the game map
   */
  public Tile(int pixel, Vec2 location, GameMap map)
  {
    this.map = map;
    this.location = location;
    tileType = fromPixel(pixel);
    height = LandType.getMapHeight(pixel);
    cameFrom = null;
    weights = new HashMap<>();
    this.gCost = 0;         // cost from the start node to current node + grass height
    this.fCost = 0;         // total cost G + manhattanDistance
  }

  /**
   * @param radius the radius of tiles to get
   * @return the tiles in a circle of radius `radius`
   */
  public ArrayList<Tile> getTilesAroundRadius(int radius) {
    ArrayList<Tile> neighbors = new ArrayList<>();
    for(int x = -radius; x < radius + 1; x++) {
      for(int y = -radius; y < radius + 1; y++)
      {
        //skip the middle tile and tiles beyond bounds of board
        if((x == 0 && y == 0) || !map.inBounds(new Vec2(location.x + x, location.y + y))) continue;
        neighbors.add(map.getTile(location.x + x, location.y + y));
      }
    }
    return neighbors;
  }

  /**
   * @param food the food to store
   */
  public void setFood(FoodData food) {
    this.food = food;
  }

  /**
   * @return the FoodData object on this tile (null if no food present)
   */
  public FoodData getFood() {
    return food;
  }

  /**
   * @param ant the Ant object to save
   */
  public void setAnt(Ant ant) {
    this.ant = ant;
  }

  /**
   * @return the Ant on this Tile (null if no ant on this tile)
   */
  public Ant getAnt() {
    return ant;
  }

  /**
   * This function goes to all the tiles in a circle `weightRadius` and calculates the potential weights for all the tiles
   */
  public void calculateWeights() {
    ArrayList<Tile> neighbors = getTilesAroundRadius(weightRadius);

    Vec2 nestPos = new Vec2(map.getNest().centerX, map.getNest().centerY);
    for(Tile tile : neighbors)
    {
      //calculate terrain weights
      if(getWeight(WeightType.TERRAIN) == -1) {
        //TODO: this is maybe not needed. keep it around though
        /*
        int edgeMapPixel = GameMap.WEIGHT_MAP_IMAGE.getRGB(tile.getLocation().x, tile.getLocation().y);
        int red = (edgeMapPixel >> 16) & 255;
        */

        if(isWater()) {
          tile.setWeight(WeightType.TERRAIN, -1_000_000_000);
        } else {
          //TODO: I had an inconsistent null-pointer exception at this line that I couldn't replicate. -Adrian
          tile.setWeight(WeightType.TERRAIN, 0);
        }
      }

      if(getWeight(WeightType.NEST) == -1)
      {
        tile.setWeight(WeightType.NEST, nestPos.euclidianDistance(tile.getLocation()));
      }

      if(tile == null) System.err.println("TILE IS NULL");
      if(tile.getWeight(WeightType.TIME) == -1) tile.setWeight(WeightType.TIME, 0);
      int timeIncrement = weightRadius - location.manhattanDist(tile.getLocation())/2;
      timeIncrement = (int)Math.round(Math.pow(timeIncrement, 1));
      tile.setWeight(WeightType.TIME, tile.getWeight(WeightType.TIME) + timeIncrement);

      if(map.getFoodSeen() != null) {
        double totalFoodWeight = 0;
        for(FoodData food : map.getFoodSeen())
        {
          double distance = new Vec2(food.gridX, food.gridY).euclidianDistance(tile.getLocation());
          if(distance < 100) {
            if(101-distance > totalFoodWeight) {
              totalFoodWeight = 101-distance;
            }
          }
        }
        tile.setWeight(WeightType.FOOD, totalFoodWeight);
      }
    }
  }

  /**
   * @param other the tile to get the direction to
   * @return a Direction object to move towards the given tile `other`
   */
  public Direction getDirectionToTile(Tile other) {
    Vec2 a = location;
    Vec2 b = other.getLocation();
    Vec2 diff = a.sub(b);
    double bearing = Math.atan2(diff.x, diff.y) - Math.PI/2;
    return angleToDirection(bearing);
  }

  /**
   * Converts an angle in radians to a direction
   * @param angle the angle in radians
   * @return the direction associated with the angle
   */
  private Direction angleToDirection(double angle) {
    double twoPI = Math.PI * 2;
    angle = (angle % twoPI + twoPI) % twoPI;
    double pi8 = Math.PI / 8;
    double pi4 = Math.PI / 4;

    if(angle >= pi8 + 0 * pi4 && angle < pi8 + 1 * pi4) {
      return Direction.NORTHEAST;
    }
    if(angle >= pi8 + 1 * pi4 && angle < pi8 + 2 * pi4) {
      return Direction.NORTH;
    }
    if(angle >= pi8 + 2 * pi4 && angle < pi8 + 3 * pi4) {
      return Direction.NORTHWEST;
    }
    if(angle >= pi8 + 3 * pi4 && angle < pi8 + 4 * pi4) {
      return Direction.WEST;
    }
    if(angle >= pi8 + 4 * pi4 && angle < pi8 + 5 * pi4) {
      return Direction.SOUTHWEST;
    }
    if(angle >= pi8 + 5 * pi4 && angle < pi8 + 6 * pi4) {
      return Direction.SOUTH;
    }
    if(angle >= pi8 + 6 * pi4 && angle < pi8 + 7 * pi4) {
      return Direction.SOUTHEAST;
    }
    if(angle >= pi8 + 7 * pi4 && angle < Math.PI * 2 || angle >= 0 && angle < pi8) {
      return Direction.EAST;
    }
    return null;
  }

  /**
   * Uses hill climbing based on the weights calculated in `calculateWeights`
   * @param combiner a WeightCombiner object to use to combine the two tiles weights
   * @return the tile with the highest weight value based on the WeightCombiner
   */
  public Tile getHighestNeighborOfWeightTypes(WeightCombiner combiner) {
    ArrayList<Tile> neighbors = getTilesAroundRadius(weightRadius);
    ArrayList<Tile> best = new ArrayList<>();
    for(Tile t : neighbors)
    {
      //seed the 'best' list with the first non-water tile
      if(best.isEmpty()) {
        best.add(t);
        continue;
      }

      double currentWeight = combiner.calculate(t);
      double bestWeight = combiner.calculate(best.get(0));
      if(currentWeight > bestWeight) {
        best.clear();
        best.add(t);
      }
      else if(currentWeight == bestWeight) {
        best.add(t);
      }
    }
    Tile result = best.get(Constants.random.nextInt(best.size()));
    return result;
  }

  /**
   * Used by `getHighestNeighborOfWeightTypes`
   */
   public interface WeightCombiner {
    double calculate(Tile ant);
  }

  /**
   * @param type the WeightType to set
   * @param weight the value to set it to
   */
  public void setWeight(WeightType type, double weight) {
    weights.put(type, weight);
  }

  /**
   * @param type the type of weight to get
   * @return returns the weight or -1 if the weight is not present
   */
  public double getWeight(WeightType type) {
    if(weights.containsKey(type)) return weights.get(type);
    return -1;
  }

  /**
   * @author Kevin, Alex
   * Helpler function to return land type
   * @param pixel from game map
   * @return land type associated with pixel
   */
  static LandType fromPixel(int pixel)
  {
    //TODO maybe also figure out the yellow tiles around the core nest nodes?
    pixel &= 0x00FFFFFF;
    if(pixel == 0x0) return LandType.NEST;
    else if (pixel == 0xF0E68C) return LandType.NEST;
    else if (pixel == LandType.WATER.getMapColor()) return LandType.WATER;
    return LandType.GRASS;
  }

  /**
   * @return built up adjacency list
   * @author Kevin Cox
   * Constructs a ArrayList of position Tiles for the adjacent location. Used in sStar method
   * inside GameMap class.
   */
  public ArrayList<Tile> getAdjacencies()
  {
    if (adjacencies == null || adjacencies.size() >= 8)
    {
      adjacencies = new ArrayList<>(8);
    }
    else return adjacencies;

    Tile tile;
    int currentX, currentY;
    for (Direction direction : Direction.values())
    {
      currentX = getLocation().x + direction.deltaX();
      currentY = getLocation().y + direction.deltaY();
      tile = map.getTile(currentX, currentY);
      adjacencies.add(tile);
    }
    return adjacencies;
  }

  /**
   * @author Alex
   * Function used in A*, helps with building priorities.
   * @return whether or not this tile is passable
   */
  public boolean isPassable()
  {
    return tileType != LandType.WATER && food == null && ant == null;
  }

  /**
   * Used by A* and Dijkstras to signify the previous tile in the path
   * @return the parent tile
   */
  public Tile getCameFrom()
  {
    return cameFrom;
  }

  /**
   * Used by A* and Dijkstras to signify the previous tile in the path
   * @param cameFrom the tile to set as the parent
   */
  public void setCameFrom(Tile cameFrom)
  {
    this.cameFrom = cameFrom;
  }

  /**
   * @return the location of the tile
   */
  public Vec2 getLocation()
  {
    return location;
  }

  /**
   * @param other the other tile
   * @return the manhattan distance to the other tile from this one
   */
  public int manhattanDist(Tile other) {
    return location.manhattanDist(other.location);
  }

  /**
   * @return the g cost of this tile
   */
  public int getgCost()
  {
    return gCost;
  }

  /**
   * @param gCost the g cost to set this tile to have
   */
  public void setgCost(int gCost)
  {
    this.gCost = gCost;
  }

  /**
   * @return the f cost of this tile
   */
  public int getfCost()
  {
    return fCost;
  }

  /**
   * @param fCost the f cost to set this tile to have
   */
  public void setfCost(int fCost)
  {
    this.fCost = fCost;
  }
}
