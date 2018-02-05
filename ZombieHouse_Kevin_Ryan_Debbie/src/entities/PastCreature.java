package entities;

import javafx.scene.Group;
import sound.Sound;

import java.util.List;

/**
 * Class that tracks states of all past selves in the single-player co-op game
 * Works for both zombies and player creatures
 * @author Sarah Salmonsons
 */
public class PastCreature extends RecordableCreature
{
  //for animation of a past player
  public Group pastCreatureMeshes = new Group();
  public int currentFrame = 0;
  private boolean direction = true;

  /**
   * Constructor takes three Lists, the first of X-axis walking behaviors, the second of Z-axis walkingdw
   * behaviors, and the final of action behaviors so that these actions can be "replayed" where each index is
   * the equivalent turn counter in the game. It also takes a boolean value that is true if the new PastCreature
   * is a Zombie, and false if it is a Player
   * @param walkBehaviorsX x-axis
   * @param walkBehaviorsZ z-axis
   * @param actionBehaviors behavior of current creature
   * @author Sarah Salmonson
   */
  public PastCreature(List<Double> walkBehaviorsX, List<Double> walkBehaviorsZ,
                      List<Double> xPositions , List<Double> zPositions, List<Double> actionBehaviors,
                      List<Integer> attackBehaviors, List<Integer> pushBehaviors, List<Integer> pushedBehaviors,
                      int tickThatCreatureDies, EntityManager entityManager)
  {
    super();
    this.walkBehaviorsX.addAll(walkBehaviorsX);
    this.walkBehaviorsZ.addAll(walkBehaviorsZ);
    this.xPositions.addAll(xPositions);
    this.zPositions.addAll(zPositions);
    this.angleBehaviors.addAll(actionBehaviors);
    this.attackBehaviors.addAll(attackBehaviors);
    this.pushBehaviors.addAll(pushBehaviors);
    this.pushedBehaviors.addAll(pushedBehaviors);
    setEntityManager(entityManager);

    xPos = 3;//in order to match the initial values of a Player
    zPos = 3;

    pastCreatureMeshes.getTransforms().addAll(xRotate, yRotate);
  }

  /**
   * Moves the PastCreature and implements the actions it took on its original timeline, like a sad marionette
   * @param turnIndex the index in array of behavior to replay
   * @return Returns NO_ANIMATION, ATTACK, or PUSH accordingly
   * @author Sarah Salmonson
   */
  int replayCreature(int turnIndex)
  {
    int action = 0;

    if(this.walkBehaviorsX.size() > turnIndex)
    {
      getBoundingCylinder().setTranslateX(this.walkBehaviorsX.get(turnIndex));
      pastCreatureMeshes.setTranslateX(this.walkBehaviorsX.get(turnIndex));
      getBoundingCylinder().setTranslateZ(this.walkBehaviorsZ.get(turnIndex));
      pastCreatureMeshes.setTranslateZ(this.walkBehaviorsZ.get(turnIndex));

      xPos = getXPositions().get(turnIndex);
      zPos = getZPositions().get(turnIndex);
    }

    if(angleBehaviors.size() > turnIndex)
    {
      if(angleBehaviors.get(turnIndex) == DEATH)
      {
        getBoundingCylinder().setTranslateX(0);
        getBoundingCylinder().setTranslateZ(0);
        pastCreatureMeshes.setVisible(false);
      }
      else
      {
        pastCreatureMeshes.setVisible(true);
        double currentAngle = this.angleBehaviors.get(turnIndex) + 180;
        getBoundingCylinder().setRotate(currentAngle);
        pastCreatureMeshes.setRotate(currentAngle);
        angle = currentAngle - 180;
      }
    }

    //@Debbie: added to log & replay attack sequence, adapted from the Player Class code
    if(attackBehaviors.size() > turnIndex)
    {
      if(attackBehaviors.get(turnIndex) == ATTACK)//the get(turnIndex) is returning the stored animation timer # of when it was logged
      {
        action = ATTACK;
        if (animationTimer < 10)
        {
          xRotate.setAngle(xRotate.getAngle() + 2);//needed to make this positive b/c ghost was leaning bkward, if can determine why & correct elsewhere, then could mk this reusable code from Parent class
          yRotate.setAngle(yRotate.getAngle() - 10);
        }
        else if (animationTimer < 25)
        {
          yRotate.setAngle(yRotate.getAngle() + 10);
        }
        else if (animationTimer < 40)
        {
          yRotate.setAngle(yRotate.getAngle() - 2);
        }
        else//conclude animation
        {
          pastCreatureMeshes.setRotate(WEAPON_UPWARD_ANGLE);
          entityManager.pastCreatureAttack();
          xRotate.setAngle(0.0);
          yRotate.setAngle(0.0);
          animationTimer = 0;
        }
        animationTimer++;
      }
    }

    //@Debbie: adapted from the Player Class code
    if(pushBehaviors.size() > turnIndex)
    {
      if(pushBehaviors.get(turnIndex) == PUSH)
      {
        action = PUSH;
        if (animationTimer < 10)
        {
          xRotate.setAngle(xRotate.getAngle() - 2);
          yRotate.setAngle(yRotate.getAngle() - 8);
        }
        //do nothing in b/n these markers, in Player method it updates weaponRotationRadius, but that change is read from the x, y behaviors stored for PastCreature
        if (animationTimer >= 50)//conclude animation
        {
          pastCreatureMeshes.setRotate(WEAPON_UPWARD_ANGLE);
          entityManager.pastCreaturePush();
          xRotate.setAngle(0.0);
          yRotate.setAngle(0.0);
          animationTimer = 0;
        }
        animationTimer++;
      }
    }

    if(pushedBehaviors.size() > turnIndex)
    {
      if(pushedBehaviors.get(turnIndex) == PUSHED)
      {
        action = PUSHED;
        if (animationTimer < 10)
        {
          xRotate.setAngle(xRotate.getAngle() + 2);
          yRotate.setAngle(yRotate.getAngle() + 8);
        }

        //do nothing in b/n these markers, in Player method it updates weaponRotationRadius, but that change is read from the x, y behaviors stored for PastCreature
        if (animationTimer >= 50)//conclude animation
        {
          pastCreatureMeshes.setRotate(WEAPON_UPWARD_ANGLE);
          xRotate.setAngle(0.0);
          yRotate.setAngle(0.0);
          animationTimer = 0;
        }
        animationTimer++;
      }
    }

    if(turnIndex % 2 == 0)
    {
      nextMesh();
    }

    return action;
  }

  /**
   * Dispose of PastCreature object and associated elements in memory
   */
  void dispose()
  {
    walkBehaviorsX.clear();
    walkBehaviorsZ.clear();
    angleBehaviors.clear();
    boundingCylinder = null;
    pastCreatureMeshes.setVisible(false);
  }

  /**
   * @author Hector Carrillo and Nick Schrandt
   *
   * Sets the next mesh in the animation sequence as visible and the current one as not visible
   *
   * note: Taken and adapted from the Zombie class. -Nick
   */
  private void nextMesh()
  {
    pastCreatureMeshes.getChildren().get(currentFrame).setVisible(false);
    int maxFrame = 12;
    if(currentFrame == maxFrame || currentFrame == 0)
    {
      direction = !direction;
    }

    if(direction) currentFrame++;
    else currentFrame--;
    pastCreatureMeshes.getChildren().get(currentFrame).setVisible(true);
  }
}