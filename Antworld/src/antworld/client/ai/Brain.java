package antworld.client.ai;

import antworld.client.game_map.GameMap;
import antworld.common.AntType;
import com.sun.org.glassfish.external.amx.AMX;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This is the 'interface' between the GameMap and the AI
 */
public class Brain
{
  private static final ExecutorService ex = Executors.newSingleThreadExecutor();

  private static int MAX_ANTS = 120;

  private GameMap map;

  public Brain(GameMap map)
  {
    this.map = map;
  }

  /**
   * Called once per tick (every 40 ms)
   * Updates all the ants and chooses actions for each of them
   */
  public Future<?> update()
  {
    return ex.submit(() -> {
      map.spawnAnts(AntType.EXPLORER, MAX_ANTS - map.getAnts().size());

      map.getAnts().parallelStream().forEach(
              ant -> {
                ant.update();
                map.sendAntAction(ant.getData());
              });
    });
  }
}
