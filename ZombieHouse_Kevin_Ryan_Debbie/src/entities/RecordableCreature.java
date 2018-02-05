package entities;

import graphing.GraphNode;
import graphing.TileGraph;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import levels.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Debbie Berlin
 *
 * A RecordableCreature Object maintains a record of it's actions
 * and behaviors in the game.
 */
public class RecordableCreature extends Creature
{
  List<Double> walkBehaviorsX = new ArrayList<>();
  List<Double> walkBehaviorsZ = new ArrayList<>();
  List<Double> xPositions = new ArrayList<>();
  List<Double> zPositions = new ArrayList<>();
  List<Double> angleBehaviors = new ArrayList<>();
  List<Integer> attackBehaviors = new ArrayList<>();
  List<Integer> pushBehaviors = new ArrayList<>();
  List<Integer> pushedBehaviors = new ArrayList<>();

  //state handling
  final static double DEATH = -1.0;
  final static int NO_ANIMATION = 0;
  final static int ATTACK = 1;
  final static int PUSH = 2;
  final static int PUSHED = 3;
  int animationTimer = 0;

  final double WEAPON_UPWARD_ANGLE = 30;

  private final double BOUNDING_CYLINDER_RADIUS = 0.4;
  private final double BOUNDING_CYLINDER_HEIGHT = 1;

  Rotate xRotate = new Rotate();
  Rotate yRotate = new Rotate();
  Rotate zRotate = new Rotate();//@Debbie adding this for future flexibility

  EntityManager entityManager;

  public RecordableCreature()
  {
    xRotate.setAxis(Rotate.X_AXIS);
    yRotate.setAxis(Rotate.Y_AXIS);
    zRotate.setAxis(Rotate.Z_AXIS);

    boundingCylinder = new Cylinder(BOUNDING_CYLINDER_RADIUS, BOUNDING_CYLINDER_HEIGHT);
  }

  //This method is intended to be overwritten in child classes if necessary
  @Override
  public void tick(){}

  //This method is intended to be overwritten in child classes if necessary  @Override
  public void stepSound(){}

  //This method is intended to be overwritten in child classes if necessary
  //Otherwise default return value will be 0
  @Override
  public double calculateDistance()
  {
    return 0;
  }

  /**This method checks if another Creautre is in this Creature's hitbox.
   *
   * @author: Nick Schrandt
   *
   * @param otherCreature The other Creature being checked for being in the hitbox of this Creature
   * @return Returns whether or not the other Creature is in this Creature's hitbox
   */
  boolean hasInHitBox(Creature otherCreature)
  {
    double angleToAttacker = otherCreature.getAbsoluteAngleTo(this);
    double attackerAngle = getAbsoluteAngle();

    double lowerAttackBound = entityManager.findLowerAttackBound(attackerAngle);
    double upperAttackBound = entityManager.findUpperAttackBound(attackerAngle);
    double distanceFromOtherCreature = distanceFrom(otherCreature);

    if (distanceFromOtherCreature < 1 && (upperAttackBound < lowerAttackBound))
    {
      if(angleToAttacker > lowerAttackBound || angleToAttacker < upperAttackBound)
      {
        return true;
      }
    }
    else if(distanceFromOtherCreature < ATTACK_RANGE && upperAttackBound > lowerAttackBound)
    {
      if(angleToAttacker > lowerAttackBound && angleToAttacker < upperAttackBound)
      {
        return true;
      }
    }
    return false;
  }

  /**
   *
   * @return The List<Double> containing the X translations of Recordable Creature's bounding cylinder
   */
  public List<Double> getWalkBehaviorsX()
  {
    return walkBehaviorsX;
  }

  /**
   *
   * @return The List<Double> containing the Z translations of Recordable Creature's bounding cylinder
   *
   */
  public List<Double> getWalkBehaviorsZ()
  {
    return walkBehaviorsZ;
  }

  /**
   *
   * @return The List<Double> containing the X positions of Recordable Creature
   */
  public List<Double> getXPositions()
  {
    return xPositions;
  }

  /**
   *
   * @return The List<Double> containing the Z positions of Recordable Creature
   */
  public List<Double> getZPositions()
  {
    return zPositions;
  }

  /**
   *
   * @return The List<Double> containing the action behvaviors of Recordable Creature
   */
  public List<Double> getAngleBehaviors()
  {
    return angleBehaviors;
  }

  /**
   *
   * @return The List<Double> containing the attack behvaviors of Recordable Creature
   */
  public List<Integer> getAttackBehaviors()
  {
    return attackBehaviors;
  }

  /**
   *
   * @return The List<Double> containing the push behaviors of Recordable Creature
   */
  public List<Integer> getPushBehaviors()
  {
    return pushBehaviors;
  }

  /**
   *
   * @return The List<Double> containing the pushed behaviors of Recordable Creature
   */
  public List<Integer> getPushedBehaviors()
  {
    return pushedBehaviors;
  }

  /**
   * Get the current GraphNode object that represents the tile that the player
   * is standing on.
   *
   * @return The GraphNode that represents the tile that the player is standing
   * on.
   */
  GraphNode getCurrentNode()
  {
    GraphNode currentNode = null;
    Tile currentTile;
    double currentX = getBoundingCylinder().getTranslateX();
    double currentZ = getBoundingCylinder().getTranslateZ();
    currentTile = entityManager.zombieHouse.gameBoard[(int) currentZ][(int) currentX];
    if (TileGraph.tileGraph.containsKey(currentTile))
    {
      currentNode = TileGraph.tileGraph.get(currentTile);
      return currentNode;
    }
    return currentNode;
  }

  /**
   * @author Debbie Berlin
   * @param entityManager EntityManager that RecordableCreature's entity manager will be set to
   */
  void setEntityManager(EntityManager entityManager)
  {
    this.entityManager = entityManager;
  }
}