package entities;

import game_engine.Attributes;
import graphing.GraphNode;
import graphing.TileGraph;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import levels.Tile;
import sound.Sound;
import utilities.ZombieBoardRenderer;

import java.util.concurrent.atomic.AtomicBoolean;

import static game_engine.ZombieHouse3d.loadMeshViews;

/**
 * @author Atle Olson
 *         Jeffrey McCall
 *         Nick Schrandt
 *         Player object for the game. All methods having
 *         to do with the player object are in this class.
 *         <p>
 *         new: Attack functionality and player weapon
 */
public class Player extends RecordableCreature
{
  private static final double SPRINT_SPEED = Tile.tileSize / 11d;
  public static final double WALKING_SPEED = Tile.tileSize / 16d;

  public static final double CAMERA_TO_PLAYER_GAP_MIN = 0.5;//0.0
  public static final double CAMERA_TO_PLAYER_GAP_MAX = 3.0;
  public double cameraToPlayerGap = 3.0;

  public static final double PLAYER_PUSH_ZOMBIE_RANGE = .6;
  public static final int PLAYER_WOUND_FROM_ZOMBIE = 10;
  public static final int PLAYER_WOUND_FROM_PAST_SELF = 5;
  public int currentFrame = 0;
  public int currentAttackingFrame = 21;
  private boolean direction = true;

  //camera:
  public GameCamera gameCamera;

  //light:
  public PointLight light;
  public boolean lightOn = true;

  public double strafeVelocity;
  private int tickCount = 0;

  //atomic booleans:
  public AtomicBoolean shiftPressed = new AtomicBoolean(false);
  public AtomicBoolean wDown = new AtomicBoolean(false);
  public AtomicBoolean dDown = new AtomicBoolean(false);
  public AtomicBoolean aDown = new AtomicBoolean(false);
  public AtomicBoolean sDown = new AtomicBoolean(false);
  private AtomicBoolean staminaOut = new AtomicBoolean(false);
  private final String PLAYER = "Resources/Meshes/Player/Ana_Model_0000";

  //other player fields:
  public AtomicBoolean isDead = new AtomicBoolean(false);
  AtomicBoolean foundExit = new AtomicBoolean(false);

  //Player Movement
  public boolean turnLeft = false;
  public boolean turnRight = false;
  private boolean pushed = false;
  private double stamina = 5;
  private double regen = .2;
  private double deltaTime = 0;
  private final double PLAYER_DISTANCE_FROM_WALL_DEFAULT_VALUE = 1000;
  public double playerFormerDistanceFromWall = PLAYER_DISTANCE_FROM_WALL_DEFAULT_VALUE;

  //Player weapon.
  public Group weaponMeshGroup = new Group();
  public Group playerAnimations = new Group();
  private final int PLAYER_TOTAL_HIT_POINTS = 100;
  private final double PLAYER_STEP_DISTANCE = 0.65;
  private final double WEAPON_Y_TRANSLATE = -.75;
  private final double PLAYER_Y_TRANSLATE = -.40;
  private final double WEAPON_UPWARD_ANGLE_OFFSET = 50;
  private boolean attacking = false;
  private boolean pushing = false;
  private double weaponRotationRadius = .4;
  private double delX, delZ;
  /**
   * @param x             x coordinate of player
   * @param y             y coordinate of player
   * @param z             z coordinate of player
   * @param gameCamera    GameCamera object used for player sight
   * @param entityManager entityManager object which updates many of the player fields as
   *                      the game runs
   * @param light         The light that emanates from the player
   * @author: original code, Nick Schrandt
   * <p>
   * new: Now sets up the weapon as well as the camera.
   */
  public Player(double x, double y, double z, GameCamera gameCamera, EntityManager entityManager, PointLight light)
  {
    super();
    stepDistance = PLAYER_STEP_DISTANCE;
    hitPoints = PLAYER_TOTAL_HIT_POINTS;
    setEntityManager(entityManager);
    xPos = x;
    yPos = y;
    zPos = z;
    velocity = 0;
    angle = 0;
    strafeVelocity = 0;

    this.gameCamera = gameCamera;
    this.gameCamera.cameraGroup.setRotate(angle);
    this.gameCamera.cameraGroup.setTranslateX(x);
    this.gameCamera.cameraGroup.setTranslateZ(z);

    this.light = light;
    light.setRotationAxis(Rotate.Y_AXIS);

    PlayerStamina staminaCounter = new PlayerStamina();
    staminaCounter.start();

    getBoundingCylinder().setTranslateX(this.gameCamera.cameraGroup.getTranslateX() + delX);
    getBoundingCylinder().setTranslateZ(this.gameCamera.cameraGroup.getTranslateZ() + delZ);
    meshPlayer();

    updatePosition();
    playerAnimations.setTranslateY(PLAYER_Y_TRANSLATE);
    playerAnimations.setRotationAxis(Rotate.X_AXIS);
    playerAnimations.setRotate(WEAPON_UPWARD_ANGLE - WEAPON_UPWARD_ANGLE_OFFSET);
    playerAnimations.setRotationAxis(Rotate.Y_AXIS);
    playerAnimations.getTransforms().addAll(xRotate, yRotate);

    //@Nick: This sets the initial position and rotation of the player cudgel. It also sets up the
    //  two Rotate objects to perform the animations.
    weaponMeshGroup.setTranslateY(WEAPON_Y_TRANSLATE);
    weaponMeshGroup.setRotate(WEAPON_UPWARD_ANGLE);
    weaponMeshGroup.getTransforms().addAll(xRotate, yRotate);
    playerAnimations.getChildren().add(weaponMeshGroup);
    yRotate.setAngle(180);

    entityManager.zombieHouse.root.getChildren().add(playerAnimations);

    lastX = this.gameCamera.cameraGroup.getTranslateX();
    lastZ = this.gameCamera.cameraGroup.getTranslateZ();

    playerAnimations.setVisible(true);
    delX = 0.3*boundingCylinder.getRadius() - .1;
    delZ = 0.3*boundingCylinder.getRadius() + .1;
  }

  /**
   * A constructor for a 2D player.
   *
   * @param x x coordinate of the player
   * @param y y coordinate of the player
   */
  public Player(double x, double y)
  {
    xPos = x;
    yPos = y;
    velocity = 0;
    angle = 0;
  }

  private void concludeAnimation()
  {
    //finishing moves of animation sequence
    weaponMeshGroup.setRotate(WEAPON_UPWARD_ANGLE);
    yRotate.setAngle(180.0);
    xRotate.setAngle(0.0);

    //resetting values
    animationTimer = 0;
    setAttacking(false);
    setPushing(false);
    setPushed(false);
  }

  /**
   * @author Kevin Cox
   * simple extracted logic for meshing the player and setting the current frame
   * visible.
   */
  public void meshPlayer()
  {
    mesh();
    currentFrame = 1;
    playerAnimations.getChildren().get(currentFrame).setVisible(true);
  }

  /**
   * @author Kevin Cox
   * meshes player with all animations (walking, punching)
   */
  private void mesh()
  {
    Node[] playerMesh;
    for (int currentFrame = 0; currentFrame <= 40; currentFrame++)
    {
      if (currentFrame < 10)
      {
        playerMesh = loadMeshViews(PLAYER + "0" + currentFrame + ".obj");
        for (Node aPlayerMesh : playerMesh)
        {
          aPlayerMesh.setVisible(false);
        }
        playerAnimations.getChildren().addAll(playerMesh);
      }
      else
      {
        playerMesh = loadMeshViews(PLAYER + currentFrame + ".obj");

        for (Node aPlayerMesh : playerMesh)
        {
          aPlayerMesh.setVisible(false);
        }
        playerAnimations.getChildren().addAll(playerMesh);
      }
    }
  }

  /**
   * @author Kevin Cox
   * updates the attack sequence for punching, plays sound click where needed.
   */
  public void nextAttackingMesh()
  {
    playerAnimations.getChildren().get(currentFrame).setVisible(false);
    if (currentAttackingFrame >= 40)
    {
      currentFrame = 2;
      playerAnimations.getChildren().get(currentFrame).setVisible(true);
      currentAttackingFrame = 21;
      setAttacking(false);
      return;
    }
    if (currentAttackingFrame >= 22) currentAttackingFrame += 2;
    else if (currentAttackingFrame >= 38) currentAttackingFrame++;
    else currentAttackingFrame++;
    //if (currentAttackingFrame == 32) entityManager.soundManager.playSoundClip(Sound.PUNCH);
    playerAnimations.getChildren().get(currentAttackingFrame).setVisible(true);
  }

  /**
   * @author Kevin Cox
   * cycles through all of the players walking frames.
   */
  void nextWalkingMesh()
  {
    playerAnimations.getChildren().get(currentFrame).setVisible(false);

    if (currentFrame == 10 || currentFrame == 0)
    {
      direction = !direction;
    }
    if (direction) currentFrame++;
    else currentFrame--;

    playerAnimations.getChildren().get(currentFrame).setVisible(true);
  }

  /**
   * @author Nick Schrandt
   * @author Debbie Berlin, restructured logic branching
   * <p>
   * This method is called from the tick and animates the cudgel whenever the player pushes. The player cannot push
   * again until this animation cycle completes.
   */
  private void animatePush()
  {
    if (animationTimer < 10)
    {
      xRotate.setAngle(xRotate.getAngle() - 2);
      yRotate.setAngle(yRotate.getAngle() - 8);
    }
    else if (animationTimer < 30)
    {
      weaponRotationRadius += .01;
    }
    else if (animationTimer < 50)
    {
      weaponRotationRadius -= .01;
    }
    else
    {
      concludeAnimation();
    }
    animationTimer++;
  }

  /**
   * @author Debbie Berlin (based on legacy code)
   * <p>
   * This method is called from the tick and animates the player to show
   * his reaction to being pushed.
   * The player cannot be pushed
   * again until this animation cycle completes.
   */
  private void animatePushed()
  {
    if (animationTimer < 10)
    {
      xRotate.setAngle(xRotate.getAngle() + 2);
      yRotate.setAngle(yRotate.getAngle() + 8);
    }
    else if (animationTimer < 30)
    {
      weaponRotationRadius -= .01;
    }
    else if (animationTimer < 50)
    {
      weaponRotationRadius += .01;
    }
    else
    {
      concludeAnimation();
    }
    animationTimer++;
  }

  /**
   * Updates the player values when called from an animation timer
   * Implemented in 2 dimensions
   */
  public void tick2d()
  {
    if (xPos + (velocity * Math.cos(angle)) > 0
            && yPos + (velocity * Math.sin(angle)) > 0
            && xPos
            + (velocity * Math.cos(angle)) < ZombieBoardRenderer.boardWidth
            * ZombieBoardRenderer.cellSize
            && yPos
            + (velocity * Math.sin(angle)) < ZombieBoardRenderer.boardWidth
            * ZombieBoardRenderer.cellSize)
    {
      xPos += (velocity * Math.cos(angle));
      yPos += (velocity * Math.sin(angle));
    }
  }

  /**
   * Updates the player values when called from an animation timer
   * Implemented in 3 dimensions
   * <p>
   * new: Orients the cudgel weapon to always follow the camera in both rotation and position.
   *
   * @author: original code, Nick Schrandt
   */
  public void tick()
  {
    Cylinder tempX = new Cylinder(getBoundingCylinder().getRadius(), getBoundingCylinder().getHeight());
    Cylinder tempZ = new Cylinder(getBoundingCylinder().getRadius(), getBoundingCylinder().getHeight());

    double movementX = getBoundingCylinder().getTranslateX();
    double movementZ = getBoundingCylinder().getTranslateZ();

    movementX += (velocity * Math.sin(angle * (Math.PI / 180)));
    movementX += (strafeVelocity * Math.sin(angle * (Math.PI / 180) - Math.PI / 2));
    movementZ += (velocity * Math.cos(angle * (Math.PI / 180)));
    movementZ += (strafeVelocity * Math.cos(angle * (Math.PI / 180) - Math.PI / 2));

    tempX.setTranslateX(movementX + delX);
    tempX.setTranslateZ(getBoundingCylinder().getTranslateZ() + delZ);

    tempZ.setTranslateZ(movementZ + delZ);
    tempZ.setTranslateX(getBoundingCylinder().getTranslateX() + delX);

    Box collisionX = entityManager.getBoxOfItemCollidingWith(tempX);
    Box collisionZ = entityManager.getBoxOfItemCollidingWith(tempZ);

    //@Nick: Calls the animation method every frame until the animation timer reaches a certain count.
    if (attacking)
    {
      attackBehaviors.add(tickCount, ATTACK);
    }
    else
    {
      attackBehaviors.add(tickCount, NO_ANIMATION);
    }

    if (pushing)
    {
      animatePush();
      pushBehaviors.add(tickCount, PUSH);
    }
    else
    {
      pushBehaviors.add(tickCount, NO_ANIMATION);
    }

    if (pushed)
    {
      animatePushed();
      pushedBehaviors.add(tickCount, PUSHED);
    }
    else
    {
      pushedBehaviors.add(tickCount, NO_ANIMATION);
    }

    if (turnLeft)
    {
      angle -= Attributes.Player_Rotate_sensitivity;
      this.gameCamera.cameraGroup.setRotate(angle);
      getBoundingCylinder().setRotate(angle);
      //@Nick: This sets the new rotation and position of the cudgel when turning left
      playerAnimations.setRotate(angle);
      updatePosition();
      turnLeft = false;
    }

    if (turnRight)
    {
      angle += Attributes.Player_Rotate_sensitivity;
      this.gameCamera.cameraGroup.setRotate(angle);
      getBoundingCylinder().setRotate(angle);
      light.setRotate(angle);

      //@Nick: This sets the new rotation and position of the cudgel when turning right
      playerAnimations.setRotate(angle);
      updatePosition();
      turnRight = false;
    }

    lastX = this.gameCamera.cameraGroup.getTranslateX();
    lastZ = this.gameCamera.cameraGroup.getTranslateZ();

    //@Hector added player collision with zombies
    if (collisionX == null && !entityManager.playerCollidesWithZombie(tempX))
    {
      //@Sarah: this is where camera group is moved on X axis
      this.gameCamera.cameraGroup.setTranslateX(movementX);

      //@Nick: This moves the player with the camera
      playerAnimations.setTranslateX(this.gameCamera.cameraGroup.getTranslateX()); //(weaponRotationRadius * Math.sin((angle * Math.PI / 180) + (Math.PI / 4))));
    }
    if (collisionZ == null && !entityManager.playerCollidesWithZombie(tempZ))
    {
      //@Sarah: this is where camera group is moved on Z axis
      this.gameCamera.cameraGroup.setTranslateZ(movementZ);

      //@Nick: This moves the player  with the camera
      playerAnimations.setTranslateZ(this.gameCamera.cameraGroup.getTranslateZ()); //(weaponRotationRadius * Math.cos((angle * Math.PI / 180) + (Math.PI / 4))));
    }

    //@Sarah: this is moving the player's bounding cylinder to the camera's location
    getBoundingCylinder().setTranslateX(this.gameCamera.cameraGroup.getTranslateX());
    getBoundingCylinder().setTranslateZ(this.gameCamera.cameraGroup.getTranslateZ());

    //@Sarah: this is logging the player's movement & actions on this tick
    walkBehaviorsX.add(tickCount, getBoundingCylinder().getTranslateX());
    walkBehaviorsZ.add(tickCount, getBoundingCylinder().getTranslateZ());
    xPositions.add(tickCount, xPos);
    zPositions.add(tickCount, zPos);
    angleBehaviors.add(tickCount, angle);
    tickCount++;

    //checking for exit collision
    for (Box box : entityManager.zombieHouse.exits)
    {
      if (box.getBoundsInParent().intersects(getBoundingCylinder().getBoundsInParent()))
      {
        foundExit.set(true);
      }
    }

    if (shiftPressed.get() && !staminaOut.get())
    {
      playerRunning();
    }
    if (staminaOut.get())
    {
      playerWalking();
    }

    updateDistance();
    //@Sarah: sets light to camera location
    light.setVisible(true);
    light.setTranslateX(this.gameCamera.cameraGroup.getTranslateX());
    light.setTranslateZ(this.gameCamera.cameraGroup.getTranslateZ());

    xPos = this.gameCamera.cameraGroup.getTranslateX();
    zPos = this.gameCamera.cameraGroup.getTranslateZ();
  }

  /**
   * @author Debbie Berlin
   * helper method to update player position based on camera & weaponRotationRadius
   */
  private void updatePosition()
  {
    playerAnimations.setTranslateX(this.gameCamera.cameraGroup.getTranslateX() + weaponRotationRadius * Math.sin((angle * Math.PI / 180) + (Math.PI / 4)));
    playerAnimations.setTranslateZ(this.gameCamera.cameraGroup.getTranslateZ() + weaponRotationRadius * Math.cos((angle * Math.PI / 180) + (Math.PI / 4)));
  }

  /**
   * @author Kevin
   * helper function to extract key pressed event when running
   */
  private void playerRunning()
  {
    if (wDown.get()) velocity = SPRINT_SPEED;
    if (sDown.get()) velocity = -SPRINT_SPEED;
    if (aDown.get()) strafeVelocity = SPRINT_SPEED;
    if (dDown.get()) strafeVelocity = -SPRINT_SPEED;
  }

  /**
   * @author Kevin
   * helper function to extract key pressed events when walking
   */
  private void playerWalking()
  {
    if (wDown.get()) velocity = WALKING_SPEED;
    if (sDown.get()) velocity = -WALKING_SPEED;
    if (aDown.get()) strafeVelocity = WALKING_SPEED;
    if (dDown.get()) strafeVelocity = -WALKING_SPEED;
  }

  public int comparePlayerDistanceFromWall(double playerCurrentDistanceFromWall)
  {
    if (this.playerFormerDistanceFromWall == PLAYER_DISTANCE_FROM_WALL_DEFAULT_VALUE)
    {
      this.playerFormerDistanceFromWall = playerCurrentDistanceFromWall;
      return 0;
    }
    else if (playerCurrentDistanceFromWall > this.playerFormerDistanceFromWall)//player moved away from wall
    {
      return 1; //flag for moving away from wall
    }
    else if (playerCurrentDistanceFromWall < this.playerFormerDistanceFromWall)//player moved towards the wall
    {
      return -1; //flag for moving towards the wall
    }
    return 0;
  }

  /**
   * Get the current GraphNode object that represents the tile that the player
   * is standing on. This is the same as the previous method except that it is
   * called for the 2D board, not the 3D one.
   *
   * @return The GraphNode that represents the tile that the player is standing
   * on.
   */
  GraphNode getCurrent2dNode()
  {
    GraphNode currentNode = null;
    Tile currentTile;
    double currentX = xPos / ZombieBoardRenderer.cellSize;
    double currentY = yPos / ZombieBoardRenderer.cellSize;
    currentTile = ZombieBoardRenderer.gameBoard[(int) currentY][(int) currentX];
    if (TileGraph.tileGraph.containsKey(currentTile))
    {
      currentNode = TileGraph.tileGraph.get(currentTile);
      return currentNode;
    }
    return currentNode;
  }

  /**
   * Plays player foot step sound
   */
  @Override
  public void stepSound()
  {
    entityManager.soundManager.playSoundClip(Sound.FOOTSTEP);
  }

  /**
   * Calculates Distance for cameraGroup
   *
   * @return The distance between lastX/Z and cameraGroup.getTranslateX/Z
   */
  @Override
  public double calculateDistance()
  {
    double xDist = gameCamera.cameraGroup.getTranslateX() - lastX;
    double zDist = gameCamera.cameraGroup.getTranslateZ() - lastZ;
    return Math.sqrt((xDist * xDist) + (zDist * zDist));
  }

  public double calculateDistanceToWall(Box wallSection)
  {
    double xDist = gameCamera.cameraGroup.getTranslateX() - wallSection.getTranslateX();
    double zDist = gameCamera.cameraGroup.getTranslateZ() - wallSection.getTranslateZ();
    return Math.sqrt((xDist * xDist) + (zDist * zDist));
  }

  /**
   * Clears Data from previous Game
   */
  void dispose()
  {
    this.gameCamera = null;
    light = null;
    boundingCylinder = null;
  }

  /**
   * @author Jeffrey McCall
   *         This class keeps track of player stamina. While the player
   *         is running, the stamina is decremented until it reaches 0. At that time,
   *         the player can't run until the stamina regenerates. This class takes care
   *         of decrementing and regenerating stamina.
   */
  private class PlayerStamina extends Thread
  {
    /**
     * Once every second, decrement stamina if shift is pressed.
     * If stamina reaches 0, regenerate stamina at a constant rate
     * once every second until stamina reaches max of 5. Exit thread if
     * program is closed.
     */
    @Override
    public void run()
    {
      while (gameIsRunning.get())
      {
        try
        {
          sleep(1000);
        }
        catch (InterruptedException e)
        {
          e.printStackTrace();
        }
        if (shiftPressed.get() && !staminaOut.get())
        {
          stamina--;
          if (stamina <= 0)
          {
            staminaOut.set(true);
          }
        }
        else if (!shiftPressed.get())
        {
          playerWalking();
          deltaTime++;
          if (((deltaTime * regen) + stamina) <= 5)
          {
            stamina += deltaTime * regen;
          }
          else
          {
            stamina = 5;
            deltaTime = 0;
            staminaOut.set(false);
          }
        }
      }
      System.exit(0);
    }
  }

  /**
   * @return the boolean value of whether or not the attack animation is playing. Eventually this will be used to
   * stop the player from attacking while the animation is in progress.
   * @author Nick Schrandt
   */
  public boolean isAttacking()
  {
    return attacking;
  }

  /**
   * @return the boolean value of whether or not the push animation is playing. Eventually this will be used to
   * stop the player from pushing or attacking while the animation is in progress.
   * @author Nick Schrandt
   */
  boolean isPushing()
  {
    return pushing;
  }

  /**
   * @author Nick Schrandt
   * <p>
   * When the attack BUTTON is pressed, this sets attacking to true for the length of the animation.
   */
  public void setAttacking(boolean attacking)
  {
    this.attacking = attacking;
  }

  /**
   * @author Nick Schrandt
   * <p>
   * When the attack BUTTON is pressed, this sets attacking to true for the length of the animation.
   */
  public void setPushing(boolean pushing)
  {
    this.pushing = pushing;
  }

  /**
   * If the past creature is in pushing mode and the player is in
   * the range, the player will be pushed.
   *
   * @author: Debbie Berlin (based on legacy code)
   */
  void setPushed(boolean pushed)
  {
    this.pushed = pushed;
  }

  /**
   * @return Cylinder cudgel
   * @author Nick Schrandt
   * <p>
   * This method returns the cyclinder that represents the player weapon
   */
  public Group getWeapon()
  {
    return weaponMeshGroup;
  }

  /**
   * Public getter for Player hitpoints stat
   *
   * @return hitPoints the double value of player's current HP
   * @author Sarah Salmonson
   */
  double getHitPoints()
  {
    return hitPoints;
  }

  /**
   * public getter for player stamina stat
   *
   * @return current stamina of player
   * @author: kevin
   */
  double getStamina()
  {
    return stamina;
  }

  /**
   * Public setter for Player DEATH status.
   *
   * @author Nick Schrandt
   */
  void setIsDead()
  {
    isDead.set(true);
    angleBehaviors.add(tickCount, DEATH);
  }
}