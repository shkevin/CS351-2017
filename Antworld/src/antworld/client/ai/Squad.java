package antworld.client.ai;

import antworld.client.Ant;
import antworld.client.game_map.GameMap;
import antworld.client.game_map.Vec2;
import antworld.common.AntAction;
import antworld.common.AntType;
import antworld.common.Direction;

import java.util.ArrayList;
import java.util.HashMap;

public class Squad
{
  private static final HashMap<AntType, Integer> composition = new HashMap<>();
  {
    composition.put(AntType.WARRIOR, 10);
    composition.put(AntType.EXPLORER, 1);
  }

  private GameMap map;
  private Vec2 pos;
  private ArrayList<Ant> ants;

  /**
    * @param map the GameMap object
   */
  public Squad(GameMap map) {
    this.map = map;
    ants = new ArrayList<>();
    //spawn the appropriate ants
    composition.forEach((type, count) -> map.spawnAnts(type, count));
  }

  /**
   * Move the squad in a direction
   * @param direction the direction to move the squad in
   */
  public void move(Direction direction) {
    ants.forEach(ant -> {
      if(ant.getData().state == AntAction.AntState.OUT_AND_ABOUT) {
        ant.move(direction);
      } else {
        System.out.println("Warning: Trying to move ant that is underground.");
      }
    });
  }

  /**
   * Move the squad to a point
   * @param point
   */
  public void moveTo(Vec2 point) {
    throw new UnsupportedOperationException("Method not implemented");
  }

  public void leaveNest() {
    // count how many ants we have already
    HashMap<AntType, Integer> counts = new HashMap<>();
    counts.put(AntType.WARRIOR, 0);
    counts.put(AntType.EXPLORER, 0);

    for(Ant ant : ants) {
      AntType type = ant.getData().antType;
      int count = counts.getOrDefault(type, 0);
      count++;
      counts.put(type, count);
    }

    //get ants from the newAnts list to fill up the composition
    counts.forEach((type, count) -> {
      int correctCount = composition.get(type);
      for(int i = 0; i < correctCount - count; i++) {
        Ant newAnt = map.getNewAnt(type);
        if(newAnt != null) {
          ants.add(newAnt);
        }
      }
    });

    for(Ant ant : ants)
    {
      ant.leaveNest();
    }
  }
}
