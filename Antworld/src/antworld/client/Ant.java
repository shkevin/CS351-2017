package antworld.client;

import antworld.client.game_map.GameMap;
import antworld.client.game_map.Tile;
import antworld.client.game_map.Vec2;
import antworld.common.*;
import fuzzy4j.flc.*;
import fuzzy4j.flc.defuzzify.Centroid;

import java.util.LinkedList;
import java.util.Map;

/**
 * Class to create the basic Ant and ant functions
 * (Extracted from GameMap)
 */
public class Ant
{
  public enum AiState
  {
    EXPLORING, RETURNING, FIGHTING, GATHERING, WATER_NEST, WATER_FIELD, DROPPING, HEAL
  }

  private static final double DANGER_ZONE = 0.5;

  private AiState strategy;
  private AiState prevState;  //TODO: get rid of this when we have a better FSM in place
  private AntData data;
  private GameMap map;
  private LinkedList<Tile> path;
  private int maxCarry;
  private int busy;
  public Vec2 location;
  Vec2 target;

  /**
   * Construct a new ant
   * @param data the AntData object for the ant
   * @param map a reference to the GameMap
   */
  public Ant(AntData data, GameMap map)
  {
    this.map = map;
    setData(data);
    this.data.action = new AntAction(AntAction.AntActionType.NOOP);
    path = new LinkedList<>();
    maxCarry = data.antType.getCarryCapacity() / 2;
    strategy = AiState.EXPLORING;
  }

  /**
   * Uses fuzzy logic to determine if the ant should attack
   * @param enemy the enemy ant object
   * @return true if attack and false if flee
   */
  private boolean shouldAttack(Ant enemy)
  {
    double friendlyHealth, enemyHealthValue;

    friendlyHealth = data.health / AntType.WARRIOR.getMaxHealth();
    enemyHealthValue = enemy.getData().health / AntType.WARRIOR.getMaxHealth();

    //inputs
    Term nearDead = Term.term("nearDead", 0, 0, 0.1, 0.2);
    Term healthy = Term.term("healthy", 0.1, 0.3, 1, 1);

    Variable health = Variable.input("health", nearDead, healthy).start(0).end(1);
    Variable enemyHealth = Variable.input("enemyHealth", nearDead, healthy).start(0).end(1);

    //outputs
    Term attack = Term.term("attack", 0, 0, 1);
    Term flee = Term.term("flee", 0, 1, 1);

    Variable action = Variable.output("action", attack, flee).start(0).end(1);

    FLC impl = ControllerBuilder.newBuilder()
            .when().var(health).is(nearDead).and().var(enemyHealth).is(nearDead).then().var(action).is(attack)
            .when().var(health).is(healthy).and().var(enemyHealth).is(nearDead).then().var(action).is(attack)
            .when().var(health).is(nearDead).and().var(enemyHealth).is(healthy).then().var(action).is(flee)
            .when().var(health).is(healthy).and().var(enemyHealth).is(healthy).then().var(action).is(attack)
            .defuzzifier(new Centroid())
            .create();

    InputInstance input = new InputInstance().is(health, friendlyHealth).is(enemyHealth, enemyHealthValue);

    Map<Variable, Double> out = impl.apply(input);
    return !(out.get(action) > .5);
  }

  /**
   * decides if ant is far enough away from nest for in field healing
   * TODO: make this have a better name
   * @return true if it should return to the nest to heal
   */
  private boolean withinRange()
  {
    return location.manhattanDist(map.getNestPos()) < 80;
  }

  /**
   * @return true if low health and false if high health
   */
  private boolean checkLowHealth()
  {
    if (getHealthUtility()) {
      if (data.carryType == GameObject.GameObjectType.WATER) strategy = AiState.HEAL;
      else if (map.getNest().waterInNest != 0 && withinRange()) strategy = AiState.RETURNING;
      else {
        prevState = strategy;  //store previous strategy so it can be reset once ant's health is restored
        strategy = AiState.WATER_FIELD;
      }
      return true;
    }
    else return false;
  }

  /**
   * Is called before each ant's update and should transition the ant between state machine states
   */
  private void transition()
  {
    switch (strategy)
    {
      case EXPLORING:
        if (checkLowHealth()) return;  //if health low, break out
        for (Tile tile : map.getTile(location).getTilesAroundRadius(1))
        {
          if (tile.getFood() != null)
          {
            strategy = AiState.GATHERING;
            return;
          }
        }
        break;
      //TODO: check for food/water/whatever near the ant
      case RETURNING:
        //TODO: check if the ant is at the base yet and maybe if they are being attacked?
        if (withinNestRadius())
        {
          strategy = AiState.DROPPING;
          return;
        }
        break;

      case DROPPING:
        if (data.carryUnits == 0) strategy = AiState.EXPLORING;
        return;

//      case HEAL:
//        if (data.health == data.antType.getMaxHealth() || map.getNest().waterInNest == 0)
//        {
//          strategy = AiState.EXPLORING;
//        }
//        return;

      case GATHERING:
        if (data.carryUnits >= maxCarry) strategy = AiState.RETURNING;
        if (checkLowHealth()) return;  //if health low, break out
        break;

      case WATER_NEST:
        if (data.carryUnits >= maxCarry) strategy = AiState.RETURNING;
        break;

      case WATER_FIELD:
        //if ant is holding water, heal
        if (data.carryUnits > 0 && data.carryType == GameObject.GameObjectType.WATER) strategy = AiState.HEAL;
        return;

      case HEAL:
        if (data.state == AntAction.AntState.OUT_AND_ABOUT)
        {
          if (data.health == data.antType.getMaxHealth())  //if at full health, quash target position and reset strategy
          {
            target = null;
            strategy = prevState;
          }
          else {  //ant has less than full health
            //pickup more water if out, otherwise it will just continue healing
            if (data.carryUnits == 0 || data.carryType != GameObject.GameObjectType.WATER) strategy = AiState.WATER_FIELD;
          }
        }
        break;
    }
  }

  /**
   * Is called each update tick and should choose an action for the ant to do based on the current state and other
   * heuristics
   */
  public void update()
  {
    if(busy > 0) {
      busy--;
      return;
    }

    transition();

    if (data.state == AntAction.AntState.UNDERGROUND)
    {
      if (data.carryUnits > 0)
      {
        drop(null, data.carryUnits);
      }
      else if (data.health < data.antType.getMaxHealth() && map.getNest().waterInNest > 0)
      {
        printHealing();
        healInNest();
      }
      else leaveNest();

      return;
    }

    Tile curTile = map.getTile(location);
    curTile.calculateWeights();

    switch (strategy)
    {
      case EXPLORING:
        Tile result = curTile.getHighestNeighborOfWeightTypes(t ->
        {
          double res = //t.getWeight(Tile.WeightType.NEST)
                  (Math.tanh(t.getWeight(Tile.WeightType.TIME) / 1_000) * 250)
                          //- t.getWeight(Tile.WeightType.TIME
                          - t.getWeight(Tile.WeightType.FOOD) * 1000
                          + t.getWeight(Tile.WeightType.TERRAIN) * 0.05;
          return res;
        });

        move(curTile.getDirectionToTile(result));
        break;

      case GATHERING:
        if (data.carryUnits < maxCarry)
        {
          for (Tile tile : map.getTile(location).getTilesAroundRadius(1))
          {
            if (tile.getFood() != null)
            {
              pickUp(tile.getLocation().sub(location).getDirection(), maxCarry);
              return;
            }
          }
        }

        break;

      case HEAL:
        if (data.carryType == GameObject.GameObjectType.WATER)
        {
          printHealing();
          healAboveGround();
        }
        break;

      case RETURNING:
        move(map.getToNestDirection(curTile));
        break;

      case WATER_FIELD:
        //find closest water
        if (target == null) target = map.findClosestWater(location.x, location.y).getLocation();
        else {  //move towards water or pick it up
          if (!location.adjacent(target)) move(calculateDirection(location, target));
          else pickUp(target.sub(location).getDirection(), 0); //determine quantity in pickUp()
        }
        break;

      case WATER_NEST:
        if (!location.adjacent(map.getWaterTile().getLocation()))
          move(calculateDirection(location, map.getWaterTile().getLocation()));
        else
        {
          System.out.println("picking up water");
          pickUp(map.getWaterTile().getLocation().sub(location).getDirection(), maxCarry);
        }
        break;

      case DROPPING:
        if (data.state == AntAction.AntState.OUT_AND_ABOUT) enterNest();
        else
        {
          if (data.carryUnits > 0) drop(null, data.carryUnits); // TODO:
//          else System.out.println("dropped: " + data.carryType);
        }
    }
  }

  /**
   * @return true if the ant is on top of the nest false otherwise
   */
  private boolean withinNestRadius()
  {
    //System.out.println("ant loc: x="+location.x+ " y="+location.y+"    nestX="+map.getNest().centerX+"   nextY="+map.getNest().centerY);
    Vec2 distance = location.sub(map.getNestPos());
    //System.out.println("nest distance: "+distance.toString());
    return Math.abs(distance.x) < Constants.NEST_RADIUS - 7 && Math.abs(distance.y) < Constants.NEST_RADIUS - 7;
  }

  /**
   * ACTION: enters the nest
   * @return true if the ant is successful false otherwise
   */
  private boolean enterNest()
  {
    if (withinNestRadius() && this.data.state == AntAction.AntState.OUT_AND_ABOUT)
    {
      data.action.type = AntAction.AntActionType.ENTER_NEST;
      data.action.x = location.x;
      data.action.y = location.y;
      return true;
    }
    return false;
  }

  /**
   * @return the current AntData object
   */
  public AntData getData()
  {
    return data;
  }

  /**
   * Sets the AntData object
   * Updates other internal state as well as updating the map Tile objects with the ant's new position
   * @param data the new AntData object
   */
  public void setData(AntData data)
  {
    // See if the ant was on a tile
    Tile oldTile = null;
    if (location != null) oldTile = map.getTile(location);

    this.data = data;

    location = new Vec2(data.gridX, data.gridY); // Update the location

    Tile newTile = map.getTile(location);

    //update the tiles
    if (oldTile != null)
    {
      oldTile.setAnt(null);
    }
    newTile.setAnt(this);

    //set the ant to busy after a move
    if(data.action.quantity != 0 && data.action.type == AntAction.AntActionType.MOVE) {
      busy = data.action.quantity;
    }
  }

  /**
   * Leaves the nest at a random location
   * Will block the ant from leaving the nest if it is not full health and there is water in the nest
   */
  public void leaveNest()
  {
    data.action.type = AntAction.AntActionType.EXIT_NEST;
    Vec2 loc = Vec2.randRadius(Constants.NEST_RADIUS - 2).add(map.getNestPos());
    assert loc.manhattanDist(map.getNestPos()) <= Constants.NEST_RADIUS;

    data.action.x = loc.x;
    data.action.y = loc.y;
  }

  /**
   * @param direction to attack in
   * @return true if successful
   * @author Kevin Cox
   * Attacks in the specified direction. Currently iterates through all enemies and checks if within
   * range.
   */
  public boolean attack(Direction direction)
  {
    for (AntData enemy : map.getEnemyAnts())
    {
      if (location.adjacent(new Vec2(enemy.gridX, enemy.gridY)))
      {
        data.action.type = AntAction.AntActionType.ATTACK;
        data.action.direction = direction;
        return true;
      }
    }
    return false;
  }

  /**
   * @param direction of item
   * @param quantity  of item
   * @return true if successful
   * @author Kevin Cox
   * Picks up the specified number of items in the specified direction
   */
  public boolean pickUp(Direction direction, int quantity)
  {
    System.out.println(quantity);
    if (data.carryUnits >= data.antType.getCarryCapacity()) return false;
    if (direction == null) return false;
    data.action.type = AntAction.AntActionType.PICKUP;
    data.action.direction = direction;


    //TODO: move all quantity logic into one location. Ants will sometimes need to pick up more than maxCarry
    if (strategy == AiState.WATER_FIELD) {  //if ant is healing in field, pick up as much water as possible
      data.action.quantity = Math.min((data.antType.getCarryCapacity() - data.carryUnits), (data.antType.getMaxHealth() - data.health));
    }
    else {
      if (quantity + data.carryUnits <= maxCarry)
      {
        data.action.quantity = quantity;
      }
      else data.action.quantity = maxCarry - quantity;
    }

    //System.out.println("quantity: "+data.action.quantity);
    return true;
  }

  /**
   * @param direction to drop item
   * @param quantity  of items to be dropped
   * @author Kevin Cox
   * Drops the specified number of same item in the specified direction
   */
  public void drop(Direction direction, int quantity)
  {
    data.action.type = AntAction.AntActionType.DROP;
    data.action.direction = direction;
    data.action.quantity = quantity;
  }

  /**
   * @param direction to move in
   * @return true if valid move
   * Instructs the ant to move in the specified direction.
   * @author Adrian
   */
  public boolean move(Direction direction)
  {
    if (direction == null) direction = Direction.SOUTH;
    if (map.getTile(location.add(Vec2.fromDirection(direction))).isPassable())
    {
      data.action.type = AntAction.AntActionType.MOVE;
      data.action.direction = direction;
      return true;
    }
    // if our ai chooses an invalid tile, just move randomly for the sake of doing /something/
    data.action.type = AntAction.AntActionType.MOVE;
    data.action.direction = Direction.getRandomDir();
    return false;
  }

  /**
   * Heal inside of the nest and subtract the water
   */
  private void healInNest()
  {
    data.action.type = AntAction.AntActionType.HEAL;

    int toBeHealed = Math.min(
            data.antType.getHealWaterUnitsPerTick(AntAction.AntState.UNDERGROUND),
            data.antType.getMaxHealth() - data.health);

    assert toBeHealed > 0 && toBeHealed <= data.antType.getHealWaterUnitsPerTick(AntAction.AntState.UNDERGROUND);

    // Even if waterInNest < toBeHealed, this is still fine
    map.getNest().waterInNest -= toBeHealed; // TODO: make sure this is properly synchronized
  }

  private void healAboveGround()
  {
    data.action.type = AntAction.AntActionType.HEAL;
    data.action.direction = null; // null direction -> heals yourself
  }

  private void printHealing()
  {
    System.out.println(String.format("ant #%d is healing %s, health = %d", data.id, data.state, data.health));
  }

  /**
   * @param start location of ant
   * @param end   location of target
   * @return direction from start vector to end vector
   * @author Kevin Cox
   * Returns the direction of the ojbect, calculated by finding the direction vector between
   * the ant and the object. Notice that the North and Sorth direcitons are flipped compared to
   * what is normally thought of. Picture provided below in Comment.
   */
  public Direction calculateDirection(Vec2 start, Vec2 end)
  {
    return end.sub(start).normalizeXY().getDirection();
    /*
                       S
                  A    |
                  <\> )|(
                    \  |  (
                  )  \ |    (
                )     \|      (
          W---)--------O--------(---E
                )      |\     (
                  )    | \  (
                    )  |  \
                      )|( <\>
                       |
                       N
   */
  }

  public LinkedList<Tile> getPath()
  {
    return path;
  }

  public void setPath(LinkedList<Tile> path)
  {
    this.path = path;
  }

  /**
   * @return true if the ant is in the DANGER ZONE (YEAHHHHHHHHHHHHHHHHH)
   */
  private boolean getHealthUtility()
  {
    return data.health < data.antType.getMaxHealth() * DANGER_ZONE;
  }

  /**
   * @return a string representation of the ant
   */
  @Override
  public String toString()
  {
    return "Ant{" +
            "strategy=" + strategy +
            ", prevState=" + prevState +
            ", data=" + data +
            ", path=" + path +
            ", maxCarry=" + maxCarry +
            ", location=" + location +
            ", target=" + target +
            '}';
  }
}
