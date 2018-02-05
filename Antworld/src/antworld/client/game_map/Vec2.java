package antworld.client.game_map;

import antworld.common.Constants;
import antworld.common.Direction;
import antworld.common.GameObject;

import java.util.Arrays;

/**
 * A 2D vector of ints for storing positions, and 2d coordinates.
 * This class immutable for now, and returns new instances for all operations.
 */
public class Vec2
{
  // This ordering scheme matches that in Direction, indexed [x+1][1-y]
  private final static int[][] ORDER = {
          {5,  6, 7},
          {4, -1, 0},
          {3,  2, 1}
  };

  // Lookup from unit vector offsets to direction
  public final static Direction[] compass = Direction.values();

  // Lookup array from a direction to its vector offsets
  public final static Vec2[] directionOffsets = Arrays.stream(Direction.values())
          .map(Vec2::new)
          .toArray(Vec2[]::new);

  public final int x, y;

  /**
   * Constructs a new Vec2
   * @param x
   * @param y
   */
  public Vec2(int x, int y)
  {
    this.x = x;
    this.y = y;
  }

  /**
   * Constructs a vec2 from a direction d
   * @param d
   */
  private Vec2(Direction d) {
    x = d.deltaX();
    y = d.deltaY();
  }

  /**
   * Constructs a vec2 from a GameObject's position
   * @param obj
   */
  public Vec2(GameObject obj)
  {
    x = obj.gridX;
    y = obj.gridY;
  }

  /**
   * @return direction of vector offset
   */
  public Direction getDirection()
  {
    return compass[directionIndex()];
  }

  /**
   * Get the direction opposite
   * @param d
   * @return
   */
  public static Direction getOppositeDirection(Direction d) {
    return fromDirection(d).mul(-1).getDirection();
  }

  /**
   * @return the direction index of the current Vec2
   */
  public int directionIndex() {
    return ORDER[x+1][1-y];
  }

  /**
   * A factory that makes a Vec2 from a direction
   * @param d the direction to use during construction
   * @return the offest vector
   */
  public static Vec2 fromDirection(Direction d) {
    return directionOffsets[d.ordinal()];
  }

  /**
   *
   * @param arr
   * @param <E>
   * @return
   */
  public <E> E index(E[][] arr) {
    return arr[x][y]; // TODO: can't use for byte[][]
  }

  /**
   * @param rhs the other vector
   * @return whether the Vec2 passed in is adjacent or not
   */
  public boolean adjacent(Vec2 rhs)
  {
    return sub(rhs).maxAbs() <= 1;
  }

  /**
   * @return the highest absolute dimension
   */
  public int maxAbs()
  {
    return Math.max(Math.abs(x), Math.abs(y));
  }

  /**
   * @param dist a radius
   * @return a random vec2 in that radius
   */
  public static Vec2 randRadius(int dist)
  {
    double len = Math.sqrt(dist);
    double x = (Constants.random.nextDouble() * 2 - 1) * len;
    double y = (Constants.random.nextDouble() * 2 - 1) * len;
    return new Vec2((int) x, (int) y).rotate(Math.PI / 4);
  }

  /**
   * @param theta the angle of rotation in radians
   * @return the new rotated vector
   */
  public Vec2 rotate(double theta)
  {
    double c = Math.cos(theta);
    double s = Math.sin(theta);
    return new Vec2((int) (x * c - y * s), (int) (x * s + y * c));
  }

  /**
   * @param rhs the other vector
   * @return this vector and the other added
   */
  public Vec2 add(Vec2 rhs)
  {
    return new Vec2(x + rhs.x, y + rhs.y);
  }

  /**
   * @param rhs the other vector
   * @return this vector and the other subtracted
   */
  public Vec2 sub(Vec2 rhs)
  {
    return new Vec2(x - rhs.x, y - rhs.y);
  }

  /**
   * @param s the factor of multiplication
   * @return this vector and the other multiplied
   */
  public Vec2 mul(int s)
  {
    return new Vec2(x * s, y * s);
  }

  /**
   * @param s the factor of division
   * @return this vector and the other divided
   */
  public Vec2 div(int s)
  {
    return new Vec2(x / s, y / s);
  }

  /**
   * @return the vector normalized
   */
  double norm()
  {
    return Math.sqrt(dot(this));
  }

  /**
   * @param rhs the other vector
   * @return the dot product of this vector and the rhs
   */
  int dot(Vec2 rhs)
  {
    return x * rhs.x + y * rhs.y;
  }

  /**
   * @param rhs the other vector
   * @return the manhattan distance from this vector to the other
   */
  public int manhattanDist(Vec2 rhs)
  {
    return Math.abs(x - rhs.x) + Math.abs(y - rhs.y);
  }

  /**
   * @param o the other vector
   * @return the euclidian distance from this vector to the other
   */
  public int euclidianDistance(Vec2 o) {
    return (int)Math.round(Math.sqrt(((o.x - x) * (o.x - x)) + ((o.y - y) * (o.y - y))));
  }

  /**
   * @return the vector's signs as a normalized vector
   */
  public Vec2 normalizeXY()
  {
    return new Vec2(Integer.signum(x), Integer.signum(y));
  }

  /**
   * @param rhs the other vector
   * @return true if the vectors are equal, false otherwise
   */
  public boolean equals(Vec2 rhs)
  {
    return x == rhs.x && y == rhs.y;
  }

  /**
   * @param o the other vector
   * @return true if the vectors are equal, false otherwise
   */
  @Override
  public boolean equals(Object o)
  {
    return o instanceof Vec2 && equals((Vec2) o);
  }

  /**
   * @return the vector as a string: "(x, y)"
   */
  @Override
  public String toString() {
    return "(" + x + ", " + y + ")";
  }

  /**
   * @return a unique hash of this vector
   */
  @Override
  public int hashCode()
  {
    return x + GameMap.WIDTH * y;
  }
}
