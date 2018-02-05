package entities;

/**
 * @author Ben Matthews
 * Class that contains fields and methods common between all entities. All
 * creatures, objects, player etc. must extend this class, or a class that
 * extends it
 */
public abstract class Entity
{
  public double xPos;
  public double yPos;
  public double zPos;

  public abstract void tick();

  /**
   * Calculate the distance to given entity
   *
   * @param entity The Entity object to calculate the distance to.
   * @return The distance to the entity object
   */
  double distanceFrom (Entity entity)
  {
    double xDist = xPos - entity.xPos;
    double zDist = zPos - entity.zPos;

    return Math.sqrt(xDist * xDist + zDist * zDist);
  }
}
