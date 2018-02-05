package entities;

import game_engine.Attributes;
import graphing.GraphNode;
import graphing.Heading;
import graphing.NodeComparator;
import graphing.TileGraph;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import levels.Tile;
import sound.Sound;
import utilities.ZombieBoardRenderer;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Jeffrey McCall
 *         Atle Olson
 *         Ben Matthews
 *         Sets and contains all of the attributes of zombies in
 *         the game.
 */
public class Zombie extends Creature
{
  public int currentFrame = 0;

  public boolean isMasterZombie = false;
  public boolean twoDBoard = false;
  public Circle zombieCircle = null;
  public Node currentMesh;

  int row;
  int col;

  boolean isRandomWalkZombie = false;

  Random rand = new Random();

  private EntityManager entityManager;

  private static final int TWO_D_SIZE = 3;
  private static final int TOTAL_HIT_POINTS = 30;

  private static final double ZOMBIE_WALKING_SPEED = .035;
  private static final double MASTER_ZOMBIE_SPEED = .05;
  private static final double MASTER_ZOMBIE_2_D_SPEED = .3;
  public static final int ZOMBIE_WOUND = 10;

  private double zombieSmell = 15.0;
  private double twoDSpeed = (.5 / 60) * ZombieBoardRenderer.cellSize;
  private double angleToPusher;
  private double angleToPlayer;
  private double prevAngle = 0;

  private int pushTimekeeper = 0;
  private int attackTimer = 0;
  private int turnCounter = 0;

  private boolean pushed = false;

  private Box healthBar;

  RecordableCreature target;

  private boolean zombieRepositioned, zombieCollision;
  public volatile boolean resonanceState, overcrowdedState, moveZombiesOffEachOther;

  /*
   * Booleans used to keep track of events in the animation timer and the thread
   * in EntityManager that governs the decision rate of each zombie.
   */
  public AtomicBoolean isGoingAfterPlayer = new AtomicBoolean(false);

  AtomicBoolean angleAdjusted = new AtomicBoolean(false);
  AtomicBoolean findNewPath = new AtomicBoolean(false);
  public static AtomicBoolean masterZombieChasePlayer = new AtomicBoolean(false);

  private AtomicBoolean collisionDetected = new AtomicBoolean(false);
  private boolean obstacleCollision;
  private CalculatePath calcPath = new CalculatePath();
  protected Heading zombieHeading, previousHeading;

  private double lastAngle = 0;
  private double lastX;
  private double lastZ;

  /**
   * Constructor that sets whether this zombie is a random walk zombie or a line
   * walk zombie. Also sets the values for the location of initial spawning
   * point of the zombie.
   */
  public Zombie(int row, int col, double xPos, double zPos, double angle, EntityManager entityManager)
  {

    stepDistance = 1;
    this.hitPoints = TOTAL_HIT_POINTS;
    this.entityManager = entityManager;

    target = this.entityManager.player;

    if (rand.nextInt(2) == 0)//50% chance that the zombie is random/line walk
    {
      isRandomWalkZombie = true;
    }
    this.row = row;
    this.col = col;
    this.xPos = xPos;
    this.zPos = zPos;
    this.angle = angle;

    healthBar = new Box(1, .01, .01);
    healthBar.setMaterial(new PhongMaterial(Color.RED));
    healthBar.setRotationAxis(Rotate.Y_AXIS);
    healthBar.setTranslateY(-1);
  }

  /**
   * Updates the health bar on top of each zombie
   */
  void updateHealthBar()
  {
    if (hitPoints <= 0)
    {
      healthBar.setVisible(false);
    }
    double rectangleLength = 1 * hitPoints / TOTAL_HIT_POINTS;
    healthBar.setWidth(rectangleLength);
  }

  public Box getHealthBar()
  {
    return healthBar;
  }

  /**
   * Creates a circle object that represents a zombie drawn on a 2D board. It is
   * given the initial x and y coordinates of the spawn point on the game map.
   *
   * @param zombieCounter The number of zombies to spawn.
   * @param row           The row of the 2D game map.
   * @param col           The column of the 2D game map.
   * @param zombies       The list of Zombie objects.
   * @param cellSize      The size of cells on the game map.
   */
  public void twoDZombie(int zombieCounter, int row, int col, ArrayList<Zombie> zombies, int cellSize)
  {
    Circle zombie;
    if (zombies.get(zombieCounter).col == col &&
        zombies.get(zombieCounter).row == row)
    {
      double xPos = zombies.get(zombieCounter).xPos;
      double yPos = zombies.get(zombieCounter).zPos;
      zombie = new Circle((xPos * cellSize), (yPos * cellSize), TWO_D_SIZE, Color.GREENYELLOW);
      zombieCircle = zombie;
    }
  }

  /**
   * Creates a cylinder that is placed around the zombie mesh. This is
   * used for collision detection. It is given
   * the initial x and z coordinates of the spawn point on the game map.
   *
   * @param cellSize The size of cells on the game map.
   */
  void createZombieCollider(int cellSize)
  {
    Cylinder cylinder;
    cylinder = new Cylinder(.25, 1);
    cylinder.setTranslateX(xPos * cellSize);
    cylinder.setTranslateZ(zPos * cellSize);
    boundingCylinder = cylinder;
  }

  /**
   * This method is called every frame by the animation timer to move the zombie
   * forward in the current direction it's traveling which is determined by the
   * current angle value. It is not called when the zombie is stopped against a
   * wall or other obstacle.
   */
  private void moveTwoDZombie(double angle, double zombieWalkingSpeed, Circle zombieCirc)
  {
    double cosTransform = Math.cos(angle * (Math.PI / 180));
    double sinTransform = Math.sin(angle * (Math.PI / 180));
    double movementAmountY = zombieCirc.getCenterY() + (zombieWalkingSpeed * (cosTransform));
    double movementAmountX = zombieCirc.getCenterX() + (zombieWalkingSpeed * (sinTransform));
    if (movementAmountX > 0 && movementAmountY > 0 &&
        movementAmountX < ZombieBoardRenderer.boardWidth * ZombieBoardRenderer.cellSize &&
        movementAmountY < ZombieBoardRenderer.boardWidth * ZombieBoardRenderer.cellSize)
    {
      zombieCirc.setCenterY(movementAmountY);
      zombieCirc.setCenterX(movementAmountX);
    }
  }

  /**
   * Moves the zombie forward in a direction determined by the current angle in
   * a 3D environment.
   *
   * @return true if movement is successful (no wall collisions)
   */
  private boolean move3DZombie(double angle, double zombieWalkingSpeed)
  {
    lastX = boundingCylinder.getTranslateX();
    lastZ = boundingCylinder.getTranslateZ();

    double cosTransform = Math.cos(angle * (Math.PI / 180));
    double sinTransform = Math.sin(angle * (Math.PI / 180));
    double movementAmountZ = boundingCylinder.getTranslateZ() + (zombieWalkingSpeed * (cosTransform));
    double movementAmountX = boundingCylinder.getTranslateX() + (zombieWalkingSpeed * (sinTransform));

    if (movementAmountX > 0 && movementAmountZ > 0 && movementAmountX < entityManager.zombieHouse.boardWidth &&
            movementAmountZ < entityManager.zombieHouse.boardHeight)
    {
      boundingCylinder.setTranslateZ(movementAmountZ);
      boundingCylinder.setTranslateX(movementAmountX);

      // @hector wall collision on this location, don't move there
      if (entityManager.getBoxOfItemCollidingWith(boundingCylinder) != null)
      {
        boundingCylinder.setTranslateZ(lastZ);
        boundingCylinder.setTranslateX(lastX);
        obstacleCollision = true;
        return false;
      }

      if(entityManager.collisionWithZombie(this))
      {
        boundingCylinder.setTranslateZ(lastZ);
        boundingCylinder.setTranslateX(lastX);
        zombieCollision = true;
        return false;
      }

      angleToPlayer = getAngleTo(entityManager.player);
      // move zombie
      currentMesh.setTranslateX(movementAmountX);
      currentMesh.setTranslateZ(movementAmountZ);
      currentMesh.setRotate(angleToPlayer);

      // move health bar with zombie
      healthBar.setTranslateX(movementAmountX);
      healthBar.setTranslateZ(movementAmountZ);
      healthBar.setRotate(angleToPlayer);
    }
    xPos = boundingCylinder.getTranslateX();
    zPos = boundingCylinder.getTranslateZ();

    obstacleCollision = false;
    return true;
  }

  void setPushed(boolean pushed, Creature pusher)
  {
    this.pushed = pushed;
    angleToPusher = getAngleTo(pusher);
  }

  /**
   * @author Nick
   * @author Debbie Berlin: adapted and moved to Zombie class
   * @return boolean true if zombie attack damages player
   * This method is called every zombie tick
   */
  boolean checkIfHurtingPlayer()
  {
    if(entityManager.player.distanceFrom(this) < ATTACK_RANGE)
    {
      return hurtPlayer();
    }
    return false;
  }

  /**
   * @author Debbie: segmented into this helper method for clarity
   * Called when player is within hit range, decreases player
   * HP by PLAYER_WOUND_FROM_ZOMBIE amount
   * Checks and sets player death accordingly
   * @return Always returns true once player damage complete
   */
  private boolean hurtPlayer()
  {
    entityManager.player.reduceHP(Player.PLAYER_WOUND_FROM_ZOMBIE);
    entityManager.soundManager.playSoundClip(Sound.GRUNT);
    entityManager.scenes.displayBlood();
    if(entityManager.player.getHitPoints() <= 0)
    {
      entityManager.player.setIsDead();
    }
    return true;
  }

  /**
   * Selects a random angle as the direction for the zombie to start moving.
   */
  public void initiateZombieToRandomAngle()
  {
    angle = rand.nextInt(360);
  }

  /**
   * Stops the zombie on the 3D game map when it has hit an obstacle.
   */
  void stop3DZombie()
  {
    boundingCylinder.setTranslateZ(boundingCylinder.getTranslateZ());
    boundingCylinder.setTranslateX(boundingCylinder.getTranslateX());
    collisionDetected.set(false);
  }

  /**
   * When the zombie hits an obstacle, this is called to reverse the direction
   * of the angle. This is done since in the animation timer there is a piece of
   * code that moves the zombie out of the obstacle a very small amount in the
   * reverse direction, and then a random angle is selected for the zombie to
   * travel in.
   */
  private void adjustAngle()
  {
    prevAngle = angle;
    angle = prevAngle - 180;
    angleAdjusted.set(true);
  }

  /**
   * Pick a random direction for the zombie to travel in, then set the boolean
   * flags off so that the timer will call the code that moves the zombie
   * forward.
   */
  void makeDecision()
  {
    pickRandomAngle();
    angleAdjusted.set(false);
    collisionDetected.set(false);
  }

  /**
   * Pick a new random angle for the zombie after it has collided with an
   * obstacle. If the random angle chosen equals the previous angle, do not
   * choose that one again, but pick a new one. If the zombie detects the player,
   * select the angle towards the player to travel in.
   */
  private void pickRandomAngle()
  {
    if (!isGoingAfterPlayer.get())
    {
      int newAngle = rand.nextInt(360);
      if (newAngle != prevAngle)
      {
        angle = newAngle;
      }
      else
      {
        while (newAngle == prevAngle)
        {
          newAngle = rand.nextInt(360);
        }
        angle = newAngle;
      }
    }
    else
    {
      if (zombieHeading != null)
      {
        angle = zombieHeading.direction;
      }
      if (lastAngle != angle)
      {
        findNewPath.set(true);
      }
      lastAngle = angle;
    }
  }

  /**
   * When the zombie has detected the player and is moving toward the player,
   * this method is called to move the zombie in the appropriate direction.
   */
  private void moveTowardPlayer(double zombieWalkingSpeed)
  {
    if (zombieHeading != null)
    {
      angle = zombieHeading.direction;
    }
    if (lastAngle != angle)
    {
      findNewPath.set(true);
    }
    lastAngle = angle;
    move3DZombie(angle, zombieWalkingSpeed);
  }

  /**
   * This method does the same thing as the moveTowardPlayer() method, but it
   * does it specifically for the zombie on the 2d board.
   */
  private void moveTowardPlayerTwoD(double zombieWalkingSpeed)
  {
    if (zombieHeading != null)
    {
      angle = zombieHeading.direction;
      if (lastAngle != angle)
      {
        findNewPath.set(false);
      }
      lastAngle = angle;
      moveTwoDZombie(angle, zombieWalkingSpeed, zombieCircle);
    }
  }

  /**
   * This method calculates the heading for the zombie to travel to go in the
   * direction of the player.
   *
   * @param tile1 The starting position of the zombie.
   * @param tile2 The next position on the path towards the player.
   */
  private void calculateHeadings(Tile tile1, Tile tile2)
  {
    zombieHeading = new Heading(tile1, tile2);
  }

  private void checkIfInCombat()
  {
    if (attackTimer > 40)
    {
      attackTimer = 0;
    }
    if (attackTimer > 0)
    {
      attackTimer++;
    }
    if (attackTimer == 0)
    {
      checkIfHurtingPlayer();
      attackTimer = 1;
    }

    if (pushed)
    {
      if (!move3DZombie(angleToPlayer, ZOMBIE_WALKING_SPEED + .1)) pushed = false;
      ++pushTimekeeper;

      if (pushTimekeeper == 7)
      {
        pushTimekeeper = 0;
        pushed = false;
      }
    }
  }

  /**
   * This method is called every time the animation time is called. A collision
   * is checked for. If the zombie has collided with an obstacle, while the
   * zombie is collided with that obstacle, move the zombie in the opposite
   * direction out of that obstacle. If there is no collision, simply keep
   * moving the zombie in the appropriate direction. Also, get the current
   * position of the zombie for purposes of pathfinding. Check to see where the
   * zombie is in relation to the center of the tile, and adjust accordingly to
   * keep the zombie centered as it moves toward the player. This is to ensure
   * that the zombie moves in the right directions at the right times. Without
   * doing these checks, the zombie might move in a direction prematurely and
   * needlessly hit obstacles. After these checks are done, the findPathToTargetFromTile
   * method is called to find the shortest path to the player.
   */
  @Override
  public void tick()
  {
    Cylinder zombieCylinder = getBoundingCylinder();
    boolean playerCollision = boundingCylinder.getBoundsInParent().intersects(entityManager.player.boundingCylinder.getBoundsInParent());

    if(previousHeading == null)
    {
      Tile currentTile = getPositionTilePath();
      Tile playerTile = entityManager.player.getCurrentNode().nodeTile;
      previousHeading = new Heading(currentTile, playerTile);
    }

    checkIfInCombat();

    if(zombieRepositioned)
    {
      Tile currentTile = getPositionTilePath();
      Tile playerTile = entityManager.player.getCurrentNode().nodeTile;
      zombieHeading = new Heading(currentTile, playerTile);
      if(zombieHeading.direction < (previousHeading.direction + 1) && zombieHeading.direction > (previousHeading.direction - 1))
      {
        resonanceState = true;
      }
      zombieRepositioned = false;
    }

    if(zombieCollision && !angleAdjusted.get())
    {
      int numCollisions = 0;
      stop3DZombie();
      adjustAngle();
      if(isGoingAfterPlayer.get())
      {
        while(zombieCollision)
        {
          move3DZombie(angle, ZOMBIE_WALKING_SPEED);
          if(zombieCollision)
          {
            if(numCollisions < 3)
            {
              adjustAngle();
              numCollisions++;
            }
            else
            {
              zombieCollision = false;
              overcrowdedState = true;
            }
          }
        }
      }
    }

    if (obstacleCollision && !angleAdjusted.get())
    {
      zombieRepositioned = true;
      stop3DZombie();
      adjustAngle();
      // Move the zombie out of the bounds of the obstacle.
      move3DZombie(angle, ZOMBIE_WALKING_SPEED);
      if (isGoingAfterPlayer.get())
      {
        while (obstacleCollision)
        {
          adjustAngle();
          move3DZombie(angle, ZOMBIE_WALKING_SPEED);
          if (!obstacleCollision)
          {
            previousHeading.direction = zombieHeading.direction;
          }
        }
        double currentX = zombieCylinder.getTranslateX();
        double currentZ = zombieCylinder.getTranslateZ();
        checkIfCornerTile(entityManager.zombieHouse.gameBoard[(int) Math.floor(currentZ)][(int) Math.floor(currentX)]);
      }
    }
    else if(moveZombiesOffEachOther)
    {
      if(lastX > zombieCylinder.getTranslateX() && lastZ > zombieCylinder.getTranslateZ())
      {
        double ang = getAngleTo(entityManager.player);
        double rand = Math.random();
        if(rand < 0.5)
        {
          angle = ang - Math.min(100, 5 / rand);
        }
        else
        {
          angle = ang + Math.min(100, 5 / (1 - rand));
        }
        move3DZombie(angle, ZOMBIE_WALKING_SPEED);
      }
    }
    else
    {
      if (!isGoingAfterPlayer.get() && !isMasterZombie)
      {
        if (playerCollision)
        {
          stop3DZombie();
        }
        else
        {
          move3DZombie(angle, ZOMBIE_WALKING_SPEED);
        }
      }
      else if (!isMasterZombie && isGoingAfterPlayer.get())
      {
        if (playerCollision)
        {
          stop3DZombie();
        }
        else moveTowardPlayer(ZOMBIE_WALKING_SPEED);
      }
      else if (isMasterZombie && !isGoingAfterPlayer.get())
      {
        if (playerCollision)
        {
          stop3DZombie();
        }
        else move3DZombie(angle, MASTER_ZOMBIE_SPEED);
      }
      else if (isMasterZombie && isGoingAfterPlayer.get())
      {
        if (playerCollision)
        {
          stop3DZombie();
        }
        else moveTowardPlayer(MASTER_ZOMBIE_SPEED);
      }
      getPositionTilePath();
      updateDistance();
      turnCounter++;
    }
  }

  private Tile getPositionTilePath()
  {
    int[] position;
    position = checkAngleAndUpdatePosition();
    Tile currentTile = entityManager.zombieHouse.gameBoard[position[0]][position[1]];
    findPathToTargetFromTile(currentTile);
    return currentTile;
  }

  public void resetHeading(Player p)
  {
    double orientation = this.xPos - p.xPos;
    if(orientation < 0)
    {
      angle = zombieHeading.direction += 45;
    }
    else
    {
      angle = zombieHeading.direction -= 45;
    }
  }

  /**
   * @author: Debbie Berlin: helper method to update position based on angle, extracted from current code
   */
  private int[] checkAngleAndUpdatePosition()
  {
    int[] pos = new int[2];
    double currentX = getBoundingCylinder().getTranslateX();
    double currentZ = getBoundingCylinder().getTranslateZ();
    if (angle == 180)
    {
      if (currentZ > (Math.floor(currentZ) + .5))
      {
        currentZ++;
      }
    }
    if (angle == 90)
    {
      if (currentX < (Math.floor(currentX) + .5))
      {
        currentX--;
      }
    }
    if (angle == 0)
    {
      if (currentZ < (Math.floor(currentZ) + .5))
      {
        currentZ--;
      }
    }
    if (angle == 270)
    {
      if (currentX > (Math.floor(currentX) + .5))
      {
        currentX++;
      }
    }
    if (angle > 90 && angle < 180)
    {
      if (currentX < (Math.floor(currentX) + .5))
      {
        currentX--;
      }
      if (currentZ > (Math.floor(currentZ) + .5))
      {
        currentZ++;
      }
    }
    if (angle > 0 && angle < 90)
    {
      if (currentX < (Math.floor(currentX) + .5))
      {
        currentX--;
      }
      if (currentZ < (Math.floor(currentZ) + .5))
      {
        currentZ--;
      }
    }
    if (angle < 360 && angle > 270)
    {
      if (currentX > (Math.floor(currentX) + .5))
      {
        currentX++;
      }
      if (currentZ < (Math.floor(currentZ) + .5))
      {
        currentZ--;
      }
    }
    if (angle > 180 && angle < 270)
    {
      if (currentX > (Math.floor(currentX) + .5))
      {
        currentX++;
      }
      if (currentZ > (Math.floor(currentZ) + .5))
      {
        currentZ++;
      }
    }
    pos[0] = (int)currentZ;
    pos[1] = (int)currentX;
    return pos;
  }

  /**
   * This method does the same things that tick() does, but it is called for
   * zombies that are being rendered on a 2D board.
   */
  public void tick2d()
  {
    if (entityManager.checkTwoD(zombieCircle) && !angleAdjusted.get())
    {
      if (!collisionDetected.get())
      {
        collisionDetected.set(true);
        stop3DZombie();
        adjustAngle();
        // Move the zombie out of the bounds of the obstacle.
        if (isGoingAfterPlayer.get())
        {
          while (entityManager.checkTwoD(zombieCircle))
          {
            moveTwoDZombie(angle, twoDSpeed, zombieCircle);
          }
          double currentXVal = zombieCircle.getCenterX() / ZombieBoardRenderer.cellSize;
          double currentZVal = zombieCircle.getCenterY() / ZombieBoardRenderer.cellSize;
          checkIfCornerTile(ZombieBoardRenderer.gameBoard[(int) currentZVal][(int) currentXVal]);
        }
        else
        {
          while (entityManager.checkTwoD(zombieCircle))
          {
            moveTwoDZombie(angle, twoDSpeed, zombieCircle);
          }
        }
      }
    }
    else if (!collisionDetected.get())
    {
      if (!isGoingAfterPlayer.get() && !isMasterZombie)
      {
        moveTwoDZombie(angle, twoDSpeed, zombieCircle);
      }
      else if (!isMasterZombie && isGoingAfterPlayer.get())
      {
        moveTowardPlayerTwoD(twoDSpeed);
      }
      else if (isMasterZombie && !isGoingAfterPlayer.get())
      {
        moveTwoDZombie(angle, MASTER_ZOMBIE_2_D_SPEED, zombieCircle);
      }
      else if (isMasterZombie && isGoingAfterPlayer.get())
      {
        moveTowardPlayerTwoD(MASTER_ZOMBIE_2_D_SPEED);
      }
    }
    double currentX = zombieCircle.getCenterX() / ZombieBoardRenderer.cellSize;
    double currentY = zombieCircle.getCenterY() / ZombieBoardRenderer.cellSize;
    if (!collisionDetected.get() && Math.abs(angle) == 180)
    {
      if (currentY > (Math.floor(currentY) + .5))
      {
        currentY++;
      }
    }
    if (!collisionDetected.get() && Math.abs(angle) == 90)
    {
      if (currentX < (Math.floor(currentX) + .5))
      {
        currentX--;
      }
    }
    if (!collisionDetected.get() && Math.abs(angle) == 0)
    {
      if (currentY < (Math.floor(currentY) + .5))
      {
        currentY--;
      }
    }
    if (!collisionDetected.get() && Math.abs(angle) == 270)
    {
      if (currentX > (Math.floor(currentX) + .5))
      {
        currentX++;
      }
    }
    if (!collisionDetected.get() && Math.abs(angle) > 90 && Math.abs(angle) < 180)
    {
      if (currentX < (Math.floor(currentX) + .5))
      {
        currentX--;
      }
      if (currentY > (Math.floor(currentY) + .5))
      {
        currentY++;
      }
    }
    if (!collisionDetected.get() && Math.abs(angle) > 0 && Math.abs(angle) < 90)
    {
      if (currentX < (Math.floor(currentX) + .5))
      {
        currentX--;
      }
      if (currentY < (Math.floor(currentY) + .5))
      {
        currentY--;
      }
    }
    if (!collisionDetected.get() && Math.abs(angle) < 360 && Math.abs(angle) > 270)
    {
      if (currentX > (Math.floor(currentX) + .5))
      {
        currentX++;
      }
      if (currentY < (Math.floor(currentY) + .5))
      {
        currentY--;
      }
    }
    if (!collisionDetected.get() && Math.abs(angle) > 180 && Math.abs(angle) < 270)
    {
      if (currentX > (Math.floor(currentX) + .5))
      {
        currentX++;
      }
      if (currentY > (Math.floor(currentY) + .5))
      {
        currentY++;
      }
    }
    if (currentX >= ZombieBoardRenderer.gameBoard.length)
    {
      currentX--;
    }
    if (currentY >= ZombieBoardRenderer.gameBoard.length)
    {
      currentY--;
    }
    Tile currentTile = ZombieBoardRenderer.gameBoard[(int) currentY][(int) currentX];
    findPathToTargetFromTile(currentTile);
  }

  /**
   * @author: Debbie Berlin: generalized existing method to
   * determine target (ie. Current Player vs. Past Player, whichever is closest),
   * then to pursue the closest as the target.
   * This method checks to see that the current tile where the zombie is located
   * is in the tile graph. If so, the target position is gotten, and the
   * appropriate methods are called to find the shortest path to the target.
   * Only the zombies that are within a Manhattan distance of the value assigned to
   * zombieSmell call the pathfinding method
   *
   * @param currentTile The current tile where the zombie is.
   */
  private void findPathToTargetFromTile(Tile currentTile)
  {
    if (TileGraph.tileGraph.containsKey(currentTile))
    {
      GraphNode zombieNode = TileGraph.tileGraph.get(currentTile);
      Tile zombieTile = zombieNode.nodeTile;

      GraphNode targetNode;
      Tile targetTile;

      if (!twoDBoard)
      {
        targetNode = entityManager.player.getCurrentNode();
        targetTile = targetNode.nodeTile;
        target = entityManager.player;

        if(entityManager.pastCreatures.size() > 0)
        {
          for(PastCreature pastCreature : entityManager.pastCreatures)
          {
            Tile pastCreatureTile = pastCreature.getCurrentNode().nodeTile;
            GraphNode pastCreatureNode = pastCreature.getCurrentNode();

            if (calcPath.findDistance(zombieTile, pastCreatureTile) <
                calcPath.findDistance(zombieTile, targetTile))
            {
              targetTile = pastCreatureTile;
              targetNode = pastCreatureNode;
              target = pastCreature;
            }
          }
        }
      }
      else //twoDBoard
      {
        targetNode = entityManager.player.getCurrent2dNode();
        targetTile = targetNode.nodeTile;
        calcPath.twoD = true;
      }

      if (calcPath.findDistance(zombieTile, targetTile) <= zombieSmell ||
         (isMasterZombie && masterZombieChasePlayer.get()))
      {
        if (!zombieTile.isWall)
        {
          calcPath.findPath(zombieTile, targetTile, zombieNode);
        }
        if (zombieTile.isWall)
        {
          calcPath.distanceToPlayer = 30;
        }
      }
      else if (calcPath.findDistance(zombieTile, targetTile) > zombieSmell)
      {
        isGoingAfterPlayer.set(false);
        calcPath.distanceToPlayer = 30;
        if (twoDBoard && calcPath.oldPath.size() >= 1)
        {
          calcPath.removePath();
        }
      }
      if (calcPath.distanceToPlayer <= zombieSmell ||
         (isMasterZombie && masterZombieChasePlayer.get()))
      {
        isGoingAfterPlayer.set(true);
      }
      else
      {
        isGoingAfterPlayer.set(false);
      }
    }
  }

  /**
   * Checks if the zombie is standing on a corner tile. If so, the zombie is
   * centered on that tile. This is done to deal with an occasional issue where
   * zombies will continue to walk into a corner tile and get stuck there if
   * they are walking into it at a 90 degree angle.
   *
   * @param currentTile The current tile that we are checking.
   */
  private void checkIfCornerTile(Tile currentTile)
  {
    if (twoDBoard)
    {
      if (currentTile.wallNE || currentTile.wallNW || currentTile.wallSW || currentTile.wallSE)
      {
        zombieCircle.setCenterX(currentTile.xPos * ZombieBoardRenderer.cellSize);
        zombieCircle.setCenterY(currentTile.zPos * ZombieBoardRenderer.cellSize);
      }
    }
    else
    {
      if (currentTile.wallNE || currentTile.wallNW || currentTile.wallSW || currentTile.wallSE)
      {
        boundingCylinder.setTranslateZ(currentTile.zPos);
        boundingCylinder.setTranslateX(currentTile.xPos);
      }
    }
  }

  /**
   * If zombie is in range of player, play appropriate sound.
   */
  @Override
  public void stepSound()
  {
    double distance = entityManager.player.distanceFrom(this);
    if (distance < Attributes.Player_Hearing)
    {
      double balance = entityManager.computeSoundBalance(this);
      entityManager.soundManager.playSoundClip(Sound.SHUFFLE, distance, balance);
      if (Math.random() < .03)
      {
        entityManager.soundManager.playSoundClip(Sound.GROAN, distance, balance);
      }
    }
  }

  /**
   * Calculates Distance for zombies.
   *
   * @return The distance between lastX/Z and boundingCylinder.getTranslateX/Z
   */
  @Override
  public double calculateDistance()
  {
    double xDist = boundingCylinder.getTranslateX() - lastX;
    double zDist = boundingCylinder.getTranslateZ() - lastZ;
    return Math.sqrt((xDist * xDist) + (zDist * zDist));
  }

  /**
   * Gets rid of values from the last game before we start a
   * new one.
   */
  void dispose()
  {
    boundingCylinder = null;
    healthBar = null;
  }

  /**
   * @author Ryan Vary
   * Sets the next mesh in the animation sequence as visible and the current one as not visible
   * The animation sequence only contains objects for taking one step forward, so to take two steps
   * the animation must change directions and go backwards
   */
  void nextMesh(Group meshes)
  {
    currentFrame++;
    if(currentFrame > (meshes.getChildren().size()-1))
    {
      currentFrame = 0;
    }
    currentMesh.setVisible(false);
    currentMesh = meshes.getChildren().get(currentFrame);
    if(isMasterZombie)
    {
      currentMesh.setScaleX(2);
      currentMesh.setScaleY(1.05);
      currentMesh.setScaleZ(2);
    }
    else
    {
      currentMesh.setScaleX(1);
      currentMesh.setScaleY(0.9);
      currentMesh.setScaleZ(1);
    }
    currentMesh.setTranslateX(xPos);
    currentMesh.setTranslateZ(zPos);
    currentMesh.setRotate(getAngleTo(target));
    currentMesh.setVisible(true);
  }

  /**
   * @author Jeffrey McCall This class is used for zombie pathfinding to find
   *         the shortest distance from the zombie to the player.
   */
  private class CalculatePath
  {
    Comparator<GraphNode> comparator = new NodeComparator();
    PriorityQueue<GraphNode> priorityQueue = new PriorityQueue<>(1, comparator);
    LinkedHashMap<Tile, Tile> cameFromTiles = new LinkedHashMap<>();
    LinkedHashMap<Tile, Double> costSoFar = new LinkedHashMap<>();
    ArrayList<Circle> oldPath = new ArrayList<>();

    double newCost;
    double priority;
    int lastPathSize = 0;
    int distanceToPlayer;
    boolean twoD = false;

    Tile destination;
    Tile end;

    /**
     * This method implements the A* algorithm to find the shortest distance
     * between the zombie and the player. I based my implementation on Justin
     * Hall's A* pathfinding program posted on the CS 351 website,
     * https://www.cs.unm.edu/~joel/cs351/. His implementation was itself based
     * on the implementation found on the website
     * http://www.redblobgames.com/pathfinding/a-star/introduction.html.
     *
     * @param originationTile       The tile the zombie is at.
     * @param destinationTile         The tile the player is at.
     * @param zombieNode The node on the graph that represents the location of the
     *                   zombie.
     */
    private void findPath(Tile originationTile, Tile destinationTile, GraphNode zombieNode)
    {
      if (originationTile != null && destinationTile != null)
      {
        end = destinationTile;
        destination = destinationTile;
        priorityQueue.add(zombieNode);
        costSoFar.put(originationTile, 0.0);
        cameFromTiles.put(originationTile, null);
        while (!priorityQueue.isEmpty())
        {
          GraphNode currentNode = priorityQueue.peek();
          Tile currentTile = priorityQueue.poll().nodeTile;
          if (currentTile.equals(destinationTile))
          {
            break;
          }
          for (Tile neighbor : currentNode.neighbors)
          {
            if (costSoFar.get(currentTile) != null)
            {
              newCost = costSoFar.get(currentTile) + neighbor.movementCost;
              if ((!costSoFar.containsKey(neighbor) ||
                    newCost < costSoFar.get(neighbor)) &&
                    !checkNeighbors(currentTile, neighbor, currentNode))
              {
                costSoFar.put(neighbor, newCost);
                priority = newCost + findDistance(neighbor, destinationTile);
                GraphNode nextNode = TileGraph.getNode(neighbor);
                if (nextNode != null) nextNode.priority = priority;
                priorityQueue.add(nextNode);
                cameFromTiles.put(neighbor, currentTile);
              }
            }
          }
        }
      }
      distanceToPlayer = getPathLength(cameFromTiles, destinationTile);

      if (twoD) drawPath();

      cameFromTiles.clear();
      priorityQueue.clear();
      costSoFar.clear();
    }

    /**
     * This method is used with A* to check if a tile is next to a wall. If it
     * is, and there is the possibility of diagonal movement to either side of
     * that wall, then we want to make it so that the path doesn't go in the
     * diagonal direction, and can only go to the tiles on either side of the
     * current tile. We are doing this since movement in the diagonal direction
     * would mean that the zombie would be trying to move through a wall.
     *
     * @param current     The current tile we are evaluating for pathfinding.
     * @param neighbor    The neighboring tile of the current tile.
     * @param currentNode The current node in the tile graph that is being evaluated.
     * @return True if the neighbor is a diagonal tile and the current tile is
     * against a wall. False otherwise.
     */
    private boolean checkNeighbors(Tile current, Tile neighbor, GraphNode currentNode)
    {
      if (!twoD)
      {
        if (currentNode.wallToRight)
        {
          if (neighbor.equals(entityManager.zombieHouse.gameBoard[current.col - 1][current.row - 1]))
          {
            return true;
          }
          if (neighbor.equals(entityManager.zombieHouse.gameBoard[current.col + 1][current.row - 1]))
          {
            return true;
          }
        }
        if (currentNode.wallToLeft)
        {
          if (neighbor.equals(entityManager.zombieHouse.gameBoard[current.col - 1][current.row + 1]))
          {
            return true;
          }
          if (neighbor.equals(entityManager.zombieHouse.gameBoard[current.col + 1][current.row + 1]))
          {
            return true;
          }
        }
        if (currentNode.wallOnBottom)
        {
          if (neighbor.equals(entityManager.zombieHouse.gameBoard[current.col + 1][current.row - 1]))
          {
            return true;
          }
          if (neighbor.equals(entityManager.zombieHouse.gameBoard[current.col + 1][current.row + 1]))
          {
            return true;
          }
        }
        if (currentNode.wallOnTop)
        {
          if (neighbor.equals(entityManager.zombieHouse.gameBoard[current.col - 1][current.row + 1]))
          {
            return true;
          }
          if (neighbor.equals(entityManager.zombieHouse.gameBoard[current.col - 1][current.row - 1]))
          {
            return true;
          }
        }
      }
      else
      {
        if (currentNode.wallToRight)
        {
          if (neighbor.equals(ZombieBoardRenderer.gameBoard[current.col - 1][current.row + 1]))
          {
            return true;
          }
          if (neighbor.equals(ZombieBoardRenderer.gameBoard[current.col + 1][current.row + 1]))
          {
            return true;
          }
        }
        if (currentNode.wallToLeft)
        {
          if (neighbor.equals(ZombieBoardRenderer.gameBoard[current.col - 1][current.row - 1]))
          {
            return true;
          }
          if (neighbor.equals(ZombieBoardRenderer.gameBoard[current.col + 1][current.row - 1]))
          {
            return true;
          }
        }
        if (currentNode.wallOnBottom)
        {
          if (neighbor.equals(ZombieBoardRenderer.gameBoard[current.col + 1][current.row - 1]))
          {
            return true;
          }
          if (neighbor.equals(ZombieBoardRenderer.gameBoard[current.col + 1][current.row + 1]))
          {
            return true;
          }
        }
        if (currentNode.wallOnTop)
        {
          if (neighbor.equals(ZombieBoardRenderer.gameBoard[current.col - 1][current.row + 1]))
          {
            return true;
          }
          if (neighbor.equals(ZombieBoardRenderer.gameBoard[current.col - 1][current.row - 1]))
          {
            return true;
          }
        }
      }
      return false;
    }

    /**
     * Get length of path between player and zombie. We do this since the zombie
     * only goes after the player in the case of the shortest path length being
     * less than 15. The way I structured this was partially inspired by code from
     * Justin Hall's A* pathfinding program from his "printPath" method. This code is
     * from the CS 351 website:
     * https://www.cs.unm.edu/~joel/cs351/
     *
     * @param cameFrom The map that represents the shortest path that was found to get
     *                 to the player.
     * @param end      The ending tile in the path. This is where the player is.
     * @return The length of the shortest path to the player.
     */
    private int getPathLength(LinkedHashMap<Tile, Tile> cameFrom, Tile end)
    {
      int counter;
      LinkedList<Tile> path = new LinkedList<>();
      Tile curr = end;
      while (curr != null)
      {
        path.addFirst(curr);
        curr = cameFrom.get(curr);
      }
      if (path.size() >= 2 && findNewPath.get())
      {
        calculateHeadings(path.get(0), path.get(1));
      }
      counter = path.size();
      return counter;
    }

    /**
     * When 2D board is being displayed, draw the paths from each zombie to the
     * player on the screen.
     */
    private void drawPath()
    {
      LinkedList<Tile> path = new LinkedList<>();
      ArrayList<Circle> circles = new ArrayList<>();
      Tile currentTile = end;
      while (currentTile != null)
      {
        path.addFirst(currentTile);
        currentTile = cameFromTiles.get(currentTile);
      }
      for (Tile tile : path)
      {
        Circle pathCircle = new Circle(tile.xPos * ZombieBoardRenderer.cellSize,
                                       tile.zPos * ZombieBoardRenderer.cellSize,
                                        2, Color.WHITE);
        circles.add(pathCircle);
      }
      if (lastPathSize != 0)
      {
        ZombieBoardRenderer.root.getChildren().removeAll(oldPath);
      }
      ZombieBoardRenderer.root.getChildren().addAll(circles);
      lastPathSize = circles.size();
      oldPath = circles;
    }

    /**
     * When the zombie is out of detection range of the player on the
     * 2D board, remove the visual representation of the path from
     * the screen.
     */
    private void removePath()
    {
      ZombieBoardRenderer.root.getChildren().removeAll(oldPath);
    }

    /**
     * Finds the Manhattan distance between a certain location on the map and
     * the player's location. This is based on code from:
     * http://www.redblobgames.com/pathfinding/a-star/introduction.html
     *
     * @param tile1 The first location.
     * @param tile2 The location of the player.
     * @return The distance between the two locations.
     */
    int findDistance(Tile tile1, Tile tile2)
    {
      return (int) (Math.abs(tile1.xPos - tile2.xPos) + Math.abs(tile1.zPos - tile2.zPos));
    }
  }

  /**
   * @return current hit point value of Zombie
   */
  double getHitPoints()
  {
    return hitPoints;
  }

  /**
   * getter for the zombie turnCounter
   *
   * @return int turnCounter
   * @author Sarah Salmonson
   */
  int getTurnCounter()
  {
    return turnCounter;
  }

  /**
   * setter for the zombie turnCounter
   *
   * @param turnCounter
   * @author Sarah Salmonson
   */
  void setTurnCounter(int turnCounter)
  {
    this.turnCounter = turnCounter;
  }
}