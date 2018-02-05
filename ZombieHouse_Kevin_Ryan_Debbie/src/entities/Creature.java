package entities;

import javafx.scene.shape.Cylinder;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Ben Matthews
 *         Class that contains fields and methods
 *         that are common between all creatures
 *         (player + monsters)
 */
public abstract class Creature extends Entity
{
  //@Sarah: adding HP element to all humans and zombies
  double hitPoints;
  double stepDistance;
  private double distanceTraveled;

  public double velocity;
  public double angle;
  double lastX;
  double lastZ;

  Cylinder boundingCylinder = new Cylinder();

  static final double ATTACK_RANGE = .65;

  public AtomicBoolean gameIsRunning = new AtomicBoolean(true);

  /**
   * @param damage amount of damage done to the Creature
   *               <p>
   *               This method is called when a creature is hit and reduces it's Hit Points by damage amount
   * @author Nick
   * @author Sarah Salmonson
   */
  public void reduceHP(int damage)
  {
    this.hitPoints -= damage;
  }

  /**
   * Get distance that the creature has traveled and
   * if greater than the 'stepDistance', play
   * step sound effect.
   */
  void updateDistance()
  {
    distanceTraveled += calculateDistance();
    if (distanceTraveled > stepDistance)
    {
      distanceTraveled = 0;
      stepSound();
    }
  }

  /**
   * Gets the angle that this Creature is moving towards another Creature
   *
   * @param otherCreature The other creature to calculate this creature's general angle from
   * @return The angle that this Creature is moving towards another Creature
   */
  double getAngleTo(Creature otherCreature)
  {
    double xDiff = otherCreature.getBoundingCylinder().getTranslateX() - this.boundingCylinder.getTranslateX();
    double zDiff = otherCreature.getBoundingCylinder().getTranslateZ() - this.boundingCylinder.getTranslateZ();

    if (zDiff < 0)
    {
      return (Math.atan(xDiff / zDiff) - Math.PI) * (180 / Math.PI) - 180;
    }

    return (Math.atan(xDiff / zDiff)) * (180 / Math.PI) - 180;
  }

  /**
   * Returns the other creature's absolute angle to this creature
   * (An absolute angle converts the general angle to an angle between 0 and 360)
   *
   * @param otherCreature The other creature to calculate this creature's absolute angle from
   * @return The other creature's absolute angle to this creature
   */
  double getAbsoluteAngleTo(RecordableCreature otherCreature)
  {
    if(getAngleTo(otherCreature) >= 0)
    {
      return getAngleTo(otherCreature);
    }
    else
    {
      return (360 + (getAngleTo(otherCreature) % 360));
    }
  }

  /**In the original code, the player angle was a value decreased as you turned left, and increased as you turned
   * right, and could become negative and go beyond 360 degrees in either direction. Since the attack hitbox is based
   * off of the relative angles, I created this method to convert the player angle into a value going from 0 to 360. It
   * cannot be negative, nor can it be greater than 360.
   *
   * @author: Nick Schrandt
   * @return double value of the absolute angle
   */
  double getAbsoluteAngle()
  {
    if(angle >= 0)
    {
      return angle % 360;
    }
    else
    {
      return (360 + (angle % 360));
    }
  }

  /**
   * Gets Creature's boundingCylinder
   *
   * @author Debbie Berlin
   * @return boundingCylinder of Creature
   */
  public Cylinder getBoundingCylinder()
  {
    return boundingCylinder;
  }

  /**
   * Plays sound effects for player and zombies.
   */
  public abstract void stepSound();

  /**
   * For zombie and player, calculates distance between the last
   * and current locations.
   *
   * @return The square root of the sum of the squares of deltaX and
   * deltaZ.
   */
  public abstract double calculateDistance();
}
