package antworld.client.game_map;

import antworld.common.Direction;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

/**
 * A Callable that calculates the A* path between two points
 */
class Searcher implements Callable<Deque<Direction>> {
  private final static byte EMPTY = Byte.MAX_VALUE;

  Vec2 start, target;
  private final GameMap map;
  private final Queue<Searcher> queue;
  private byte[][] previous = new byte[GameMap.WIDTH][GameMap.HEIGHT];

  public Searcher(GameMap map, Queue<Searcher> queue) {
    this.map = map;
    this.queue = queue;
  }

  /**
   * @return the Deque of directions
   * @throws Exception
   */
  @Override
  public Deque<Direction> call() throws Exception {
    Deque<Direction> path = getPath();

    // Clean up and return
    IntStream.range(0, previous.length).parallel().forEach(i -> Arrays.fill(previous[i], EMPTY));
    queue.add(this); // Queue this worker for more work
    return path;
  }

  /**
   * @param current the current position
   * @return the direction to go
   */
  Direction getDirection(Vec2 current) {
    byte data = previous[current.x][current.y];
    return data != EMPTY ? Vec2.compass[data] : null;
  }

  /**
   * @return path from start to target
   */
  private Deque<Direction> constructPath() {
    Deque<Direction> path = new ArrayDeque<>(25);
    Vec2 current = target;

    while (!current.equals(start)) {
      Direction next = getDirection(current);
      path.add(next);
      current = current.add(Vec2.fromDirection(next));
    }

    // TODO: reverse
    return path;
  }

  /**
   * @author Kevin Cox, Alex Johnson
   * Calculates a path from the start location to the target location.
   */
  public Deque<Direction> getPath()
  {
    if (map.getTile(target).isWater()) return null; // TODO:

    PriorityQueue<Vec2> frontier = new PriorityQueue<>(Comparator.comparing(v -> map.getTile(v).getfCost()));

    Set<Vec2> closed = new HashSet<>(100);
    frontier.offer(start);
    closed.add(start);

    while (!frontier.isEmpty())
    {
      Vec2 current = frontier.poll();
      closed.add(current);

      if (current.equals(target)) return constructPath();

      for (Vec2 delta : Vec2.directionOffsets)
      {
        Vec2 next = current.add(delta);

        if (!closed.contains(next) && map.inBounds(next) && map.getTile(next).isPassable())
        {
          previous[next.x][next.y] = (byte) delta.directionIndex();
          frontier.offer(next);
          closed.add(next);
        }
      }
    }

    return null; // No path exists
  }
}
