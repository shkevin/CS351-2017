package antworld.client.game_map;

import antworld.common.Direction;

import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.stream.IntStream;

class SearchPool extends ThreadPoolExecutor
{
  private Queue<Searcher> workers = new ConcurrentLinkedQueue<>();

  public SearchPool(int nThreads, GameMap map)
  {
    // equivalent to newFixedSizeThreadPool
    super(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

    IntStream.range(0, nThreads).forEach(i -> workers.add(new Searcher(map, workers)));
  }

  Future<Deque<Direction>> getPath(Vec2 start, Vec2 target)
  {
    Searcher s = workers.poll();
    s.start = start;
    s.target = target;
    return submit(s);
  }
}
