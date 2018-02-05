package entities;

import com.sun.istack.internal.Nullable;
import game_engine.Attributes;
import game_engine.Scenes;
import game_engine.ZombieHouse3d;
import gui.Main;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Translate;
import levels.Tile;
import sound.Sound;
import sound.SoundManager;
import utilities.ZombieBoardRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Jeffrey McCall 
 *         Ben Matthews
 *         Atle Olson
 *
 *         Hector Carillo
 *         Sarah Salmonson
 *         Nick Schrandt
 *
 *         Kevin Cox
 *         Ryan Vary
 *         Debbie Berlin
 *
 * This class handles many different functions for all of the entities in the
 * game, which are the player and the zombies. Values are updated for the entities
 * every time the animation timer is called. Various other functions are performed
 * here such as calculating the sound balance as well as collision detection.
 *
 * new: Now handles the zombie and player attack/PUSH functionality. Whenever a player
 * his space key, or the left-mouse, the entitymanager checks for zombies to hit. And
 * it checks each zombie in the tick to see if it's in range of a player.
 *
 */
public class EntityManager
{
  public Player player;
  public volatile ArrayList<Zombie> zombies;
  private ArrayList<Zombie> startingZombies;
  private ArrayList<Zombie> deletedZombies = new ArrayList<>();
  public ArrayList<PastCreature> pastCreatures = new ArrayList<>();
  public AtomicBoolean gameIsRunning = new AtomicBoolean(true);
  public SoundManager soundManager;

  ZombieHouse3d zombieHouse;

  public Scenes scenes;
  private Main main;
  private Zombie masterZombie;
  private MasterZombieDecision masterDecision;
  private ZombieDecision zombieDecision;

  public int numTiles = 0;  // The number of wall tiles on the map. Used to check for collisions.
  private int tickCount = 0;
  private int playerLives = 3;
  private int region1Counter = 0;
  private int region2Counter = 0;
  private int region3Counter = 0;
  private int region4Counter = 0;
  private int randomZombieIndex;
  private int frameUpdate = 1;
  private boolean debugRingBuffer = false;
  private final double PLAYER_ANGLE_OFFSET = 60;

  /**
   * Constructor for EntityManager.
   * @param soundManager
   *        The SoundManager class being used to manage all of the sound
   *        of the game.
   * @param main
   *        The Main class that is running the program and is the entry point 
   *        for starting and playing the game.
   * @param scenes
   *        The various screens that are seen throughout playing the game, such as
   *        the main menu, the settings menu, the win screen, etc.
   */
  public EntityManager(SoundManager soundManager, Main main, Scenes scenes)
  {
    this.soundManager = soundManager;
    this.scenes = scenes;
    this.main = main;
    zombies = new ArrayList<>();
    startingZombies = new ArrayList<>();
    zombieDecision = new ZombieDecision();
    zombieDecision.setDaemon(true);
    zombieDecision.start();
  }

  /**
   * Checks if the zombie is colliding with anything.
   * 
   * @return False if no collision detected. True if there is a collision.
   */
  boolean checkTwoD(Circle zombieCircle)
  {
    for (int i = 0; i < numTiles; i++)
    {
      if (zombieCircle.getLayoutBounds().intersects(ZombieBoardRenderer.walls.get(i).getLayoutBounds()))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Collision detection for 3D zombie objects.
   * 
   * @param creature The shape that represents the creature.
   * @return Box with which the creature collided. Null if no collision
   */
  Box getBoxOfItemCollidingWith(Shape3D creature)
  {
    for (int i = 0; i < numTiles; i++)
    {
      if (creature.getBoundsInParent().intersects(zombieHouse.getWalls().get(i).getBoundsInParent()))
      {
        return zombieHouse.getWalls().get(i);
      }
    }
    for(int i = 0; i < zombieHouse.getObstacles().size(); i++)
    {
      if(creature.getBoundsInParent().intersects(zombieHouse.getObstacles().get(i).getBoundsInParent()))
      {
        return zombieHouse.getObstacles().get(i);
      }
    }
    return null;
  }

  /**
   * Collision detection for 3D player objects.
   * 
   * @param player The shape that represents the player.
   * @return True if there is a collision. False if there isn't.
   */
  boolean playerCollidesWithZombie(Shape3D player)
  {
    for (Zombie zombie : zombies)
    {
      if (player.getBoundsInParent().intersects(zombie.boundingCylinder.getBoundsInParent()))
      {
        return true;
      }
    }
    return false;
  }

  boolean collisionWithZombie(Zombie zombie)
  {
    Shape3D creature = zombie.getBoundingCylinder();
    int index = zombies.indexOf(zombie);
    for(int j = 0; j < zombies.size(); j++)
    {
      if(j != index && creature.getBoundsInParent().intersects(zombies.get(j).getBoundingCylinder().getBoundsInParent()))
      {
        return true;
      }
    }
    return false;
  }

    /**
     * This method is called from the MouseEventHandler when the left BUTTON
     * is pressed. It checks if there are any zombies in the player's hitbox (120 degrees)
     * and if so, it calls the playerHitsZombie method.
     *
     * @author Nick Schrandt
     * @author Sarah Salmonson
     */
  public void playerAttack()
  {
    for(Zombie zombie : zombies)
    {
      if(player.hasInHitBox(zombie))
      {
        playerHitsZombie(zombie);
      }
    }
    removeDeletedZombies();
  }

  /**
   * Processes a hit to a zombie and applies damage
   * with weapon sound effect.
   *
   * @author: Nick Schrandt, Sarah Salmonson
   * @param zombie current zombie to be hit by the player.
   */
  private void playerHitsZombie(Zombie zombie)
  {
    damageZombie(zombie);
    soundManager.playSoundClip(Sound.PUNCH);
  }

  /**
   * This method is called when the past creature is in attack mode.
   * It checks if there are any zombies in the past creature's hitbox (120 degrees)
   * and if so, it calls the pastCreatureHitsZombie() method.
   * At the end it calls removeDeletedZombies()
   *
   * @author Debbie Berlin (based on code from playerAttack())
   */
  public void pastCreatureAttack()
  {
    for(PastCreature pastCreature : pastCreatures)
    {
      for(Zombie zombie : zombies)
      {
        if(pastCreature.hasInHitBox(zombie))
        {
          pastCreatureHitsZombie(zombie);
        }
      }
      if (pastCreature.hasInHitBox(player))
      {
        pastCreatureHitsPlayer();
      }
      removeDeletedZombies();
    }
  }

  /**
   * Processes a hit to the player from a past creature
   * and applies damage with no sound effect.
   *
   * @author: Debbie Berlin (based on legacy code)
   */
  private void pastCreatureHitsPlayer()
  {
    player.reduceHP(Player.PLAYER_WOUND_FROM_PAST_SELF);
    if(player.getHitPoints() <= 0)
    {
      player.setIsDead();
    }
  }

  /**
   * Processes a hit to a zombie from a past creature
   * and applies damage with no sound effect.
   *
   * @author Debbie Berlin (based on code from playerHitsZombie())
   * @author: Nick Schrandt, Sarah Salmonson
   * @param zombie current zombie to be hit by the player.
   */
  private void pastCreatureHitsZombie(Zombie zombie)
  {
    damageZombie(zombie);
  }

  /**
   * Reduces zombie hit points, updates its health bar,
   * and adds to deletedZombies if the zombie dies as a result.
   *
   * @author: Debbie Berlin (extracted helper method)
   * @param zombie Zombie that is being damaged
   *               and will have its hit points reduced.
   */
  private void damageZombie(Zombie zombie)
  {
    zombie.reduceHP(Zombie.ZOMBIE_WOUND);
    zombie.updateHealthBar();
    if(zombie.getHitPoints() <= 0)
    {
      deletedZombies.add(zombie);
      removeZombieFromRingBuffer(zombie);
    }
  }

  /**
   * @author: Ryan Vary
   */
  private void removeZombieFromRingBuffer(Zombie zombie)
  {
    int zInd = zombies.indexOf(zombie);
    int div = (zInd/(zombieHouse.ringBufferCapacity - 1));
    int mod = zInd % (zombieHouse.ringBufferCapacity - 1);
    zombieHouse.zombieToRingBuffer.get(div).get(mod).currentMesh.setVisible(false);
    zombieHouse.zombieToRingBuffer.get(div).remove(mod);;
    zombieHouse.deathSequence(zombie.xPos, zombie.zPos);
  }

  private void removeDeletedZombies()
  {
    for(Zombie zombie : deletedZombies)
    {
      zombies.remove(zombie);
    }
  }

  public void freezeZombies()
  {
    for(Zombie zombie : zombies)
    {
      zombie.stop3DZombie();
    }
  }

  /**
   * This method is called from the KeyBoardEventHandler when 'Space'
   * is pressed. It checks if there are any zombies near the player, if so the
   * zombie is pushed aside.
   *
   * @author Hector Carrillo
   */
  public void playerPush()
  {
    for(Zombie zombie: zombies)
    {
      if (player.distanceFrom(zombie) < Player.PLAYER_PUSH_ZOMBIE_RANGE && !player.isPushing())
      {
        soundManager.playSoundClip(Sound.PUSH);
        zombie.setPushed(true, player);
      }
    }
  }

  /**
   * This method is called from the push animation sequence of the past creature.
   * It checks if there are any creatures near the past creature,
   * if so the creature is pushed aside.
   *
   * @author: Debbie Berlin (based on legacy code)
   */
  void pastCreaturePush()
  {
    for(PastCreature pastCreature : pastCreatures)
    {
      for (Zombie zombie : zombies)
      {
        if (pastCreature.distanceFrom(zombie) < Player.PLAYER_PUSH_ZOMBIE_RANGE)
        {
          zombie.setPushed(true, pastCreature);
        }
      }

      if (pastCreature.distanceFrom(player) < Player.PLAYER_PUSH_ZOMBIE_RANGE)
      {
        player.setPushed(true);
      }
    }
  }

  /**
   * @author Ryan
   * computeSoundBalance returns the balance that should be applied to the zombie's sounds contained in the
   * Sound enum and managed in the SoundManager (shuffle, groan). This computaton is based on the polar angle between
   * the player's line of sight and the vector between the player and zombie, hence the use of the atan2 method. The
   * angle between the player's line of sight and x axis (defined as 0 degrees) is constrained to the unit circle
   * (angle % 2*Math.PI).  A player's counterclockwise rotation is negative and a clockwise rotation is positive. A
   * counterclockwise rotation is converted to a clockwise one by simply adding 2*Math.PI to it. The difference between
   * the sight vector and vector between player/zombie is then computed (angleDiff) and sent to a helper method
   * (soundBalance) for conditioning and calculation of the balance.
   *
   * @param zombie Zombie within hearing distance of player.
   * @return number from -1 to 1 that represents balance
   */


  public double computeSoundBalance(Zombie zombie)
  {
    double angle = (player.getBoundingCylinder().getRotate()+270)*(Math.PI/180);
    double xDiff = player.xPos - zombie.xPos;
    double zDiff = player.zPos - zombie.zPos;
    double phi = Math.atan2(zDiff, xDiff);
    double balance;

    angle = angle % (2*Math.PI);

    double[] conditionedAngles = conditionAngles(angle,phi);
    angle = conditionedAngles[0];
    phi = conditionedAngles[1];

    double angleDiff = angle - phi;
    if(angle > phi)
    {
      if(angleDiff > Math.PI)
      {
        phi += 2 * Math.PI;
        angleDiff = phi - angle;
        balance = soundBalance(angleDiff);
      }
      else balance = -soundBalance(angleDiff);

    }
    else if(angle < phi)
    {
      if(angleDiff < -Math.PI)
      {
        angle += 2 * Math.PI;
        angleDiff = angle - phi;
        balance = -soundBalance(angleDiff);
      }
      else balance = soundBalance(angleDiff);
    }
    else
    {
      balance = 0;
    }
    return balance;
  }

  private double[] conditionAngles(double angle, double phi)
  {
    double[] angles = new double[2];
    if(Math.signum(angle) == -1)
    {
      angle += 2*Math.PI;
    }
    if(Math.signum(phi) == -1)
    {
      phi = -phi;
    }
    else if(Math.signum(phi) == 1)
    {
      phi = 2*Math.PI - phi;
    }
    angles[0] = angle;
    angles[1] = phi;
    return angles;
  }

  /**
   * @author Ryan
   * soundBalance returns the balance applied to audioclip.
   *
   * @param angle polar angle between player sight vector and vector from player to zombie
   * @return sound balance
   */
  private double soundBalance(double angle)
  {
    angle = Math.abs(angle);
    double balance;
    if(angle > Math.PI/2 && angle < 3*Math.PI/4)
    {
      angle -= (angle - Math.PI/2);
    }
    else if (angle >= 3*Math.PI/4)
    {
      angle -= Math.PI/2;
    }
    balance = angle/(Math.PI/2);
    return balance;
  }

  /**
   * Creates list of all of the zombies that will spawn
   * on the board.
   *
   * @Nick This now loops through all of the tiles repeatedly until the minimum number of zombies has been
   * created. Since this is how it was handled in the past verison of the game, except during the tiles' creation
   * the distribution should be the same.
   */
  public void createZombies(Tile[][] gameBoard, int zHeight, int xWidth, boolean continuingLevel)
  {
    if(!continuingLevel)//i.e. this code should only be run on the first level
    {
      int zombieCounter = 0;
      while(zombieCounter <= Attributes.Min_Zombies && playerLives == 3)
      {
        for(int col = 0; col < zHeight; col++)
        {
          for(int row = 0; row < xWidth; row++)
          {
            if(gameBoard[col][row].zombieSpawnsOnTile() && !gameBoard[col][row].hasZombie)
            {
              int tileRegion = gameBoard[col][row].getRegion();
              if(getRegionCounter(tileRegion) <= Attributes.Max_Zombies/4)
              {
                gameBoard[col][row].hasZombie = true;
                zombieCounter++;
                incrementRegionCounter(tileRegion);
              }
            }
          }
        }
      }

      int zombieCounter2 = 0;

      randomZombieIndex = (int)(zombies.size()*Math.random());

      for (int col = 0; col < zHeight; col++)
      {
        for (int row = 0; row < xWidth; row++)
        {
          if (gameBoard[col][row].hasZombie && !gameBoard[col][row].isHallway)
          {
            zombieCounter2++;
            Zombie newZombie = new Zombie(row, col, gameBoard[col][row].xPos, gameBoard[col][row].zPos, 0, this);
            newZombie.createZombieCollider(Tile.tileSize);
            zombies.add(newZombie);

            if (zombieCounter2 == Attributes.Max_Zombies) break;
          }
        }
        if (zombieCounter2 == Attributes.Max_Zombies) break;
      }
      int zombieListCounter = 0;

      for (Zombie zombie : zombies)
      {
        if (zombieListCounter == randomZombieIndex)
        {
          zombie.isMasterZombie = true;
          masterZombie = zombie;
          masterZombie.hitPoints = 30.0;
          masterDecision = new MasterZombieDecision();
          masterDecision.setDaemon(true);
          masterDecision.start();
        }
        zombieListCounter++;
      }

      for (Zombie zombie: zombies)
      {
        zombie.initiateZombieToRandomAngle();
      }

      for(Zombie zombie: zombies)
      {
        Zombie replicaZombie = new Zombie(zombie.row, zombie.col, zombie.xPos, zombie.zPos, zombie.angle, this);

        if(zombie.isMasterZombie)
        {
          replicaZombie.isMasterZombie = true;
        }
        startingZombies.add(replicaZombie);//@ Debbie: stores replicas of zombies in starting states for re-use in continuing levels
      }
    }

    else
    {
      restartZombies();
    }
  }

  /**
   * When a zombie detects the player, the master zombie also detects the player
   * and goes after the player.
   */
  public void startMasterZombie()
  {
    for (Zombie zombie : zombies)
    {
      if (zombie.isMasterZombie)
      {
        zombie.masterZombieChasePlayer.set(true);
      }
    }
  }

  /**
   * @author: Debbie Berlin
   * Clears out zombie ArrayList that has been used and manipulated in prior game run
   * and re-initializes it to the original set of zombies that were created in the first level
   */
  public void restartZombies()
  {
    zombies.clear();

    for(Zombie zombie : startingZombies)
    {
      Zombie replicaZombie = new Zombie(zombie.row, zombie.col, zombie.xPos, zombie.zPos, zombie.angle,this);
      replicaZombie.createZombieCollider(Tile.tileSize);
      zombies.add(replicaZombie);
    }
  }


  /**
   * @author: Ryan Vary
   *
   */
  private void loopThroughRingBuffers(ArrayList<Zombie> zombies, int head, Group meshes, int numZombiesInRingBuffer)
  {
    boolean wrapAroundRing = false;
    int ringInd = 0;
    for(int k = 0; k < numZombiesInRingBuffer; k++)
    {
      if(!wrapAroundRing)
      {
        ringInd = head - k - 1;
        if(ringInd < 0)
        {
          ringInd = zombieHouse.ringBufferCapacity - 1;
          wrapAroundRing = true;
        }
      }
      else
      {
        ringInd -= 1;
      }
      for (Zombie zombie : zombies)
      {
        if(zombie.currentFrame == ringInd)
        {
          zombie.tick();
          if(tickCount % frameUpdate == 0)
          {
            zombie.nextMesh(meshes);
          }
          if(zombie.isGoingAfterPlayer.get() && !zombie.isMasterZombie && !Zombie.masterZombieChasePlayer.get())
          {
            startMasterZombie();
          }
          break;
        }
      }
    }
  }

  /**
   * This Method updates all the values of all entities
   * @author: Jeff, Ben, Atle, Sarah Salmonson and Ryan Vary
   */
  public void tick()
  {
    player.tick();
    scenes.displayNewHP((int)player.getHitPoints());
    scenes.displayNewStamina((int)player.getStamina());
    //fog the tiles as a function of distance from edge of far clip
    fadeToBlack(zombieHouse.getWalls());
    fadeToBlack(zombieHouse.getNotWalls());

    for(int j = 0; j < zombieHouse.numRingBuffers; j++)
    {
      synchronized(zombieHouse.zombieToRingBuffer)
      {
        ArrayList<Zombie> zombies = zombieHouse.zombieToRingBuffer.get(j);
        Group meshes = zombieHouse.ringBufferContainer.get(j);
        int head = zombieHouse.ringBufferHead[j];
        loopThroughRingBuffers(zombies, head, meshes, zombieHouse.zombiesInRingBuffer[j]);
      }
        if(tickCount % frameUpdate == 0)
      {
        zombieHouse.ringBufferHead[j]++;
        if(zombieHouse.ringBufferHead[j] > zombieHouse.ringBufferCapacity - 1) zombieHouse.ringBufferHead[j] = 0;
      }
    }

    if(debugRingBuffer)
    {
      if(tickCount % frameUpdate == 0)
      {
        for (int j = 0; j < zombieHouse.numRingBuffers; j++)
        {
          System.out.println("Ring Buffer: " + j + ", Head: " + zombieHouse.ringBufferHead[j]);
          ArrayList<Zombie> zombies = zombieHouse.zombieToRingBuffer.get(j);
          for (int k = 0; k < zombies.size(); k++)
          {
            System.out.println("Zombie #: " + k + " Current Frame: " + zombies.get(k).currentFrame);
          }
        }
      }
    }

    if ((player.wDown.get() || player.aDown.get() || player.sDown.get() ||
            player.dDown.get()) && !player.isAttacking()) player.nextWalkingMesh();
    else if (player.isAttacking())
    {
      if (player.currentAttackingFrame <= 40) player.playerAnimations.getChildren().get(
              player.currentAttackingFrame).setVisible(false);
      player.nextAttackingMesh();
    }

    //@Sarah call replayCreature method for all past players
    for(PastCreature pastCreature : pastCreatures)
    {
      if(pastCreature != null)
      {
        pastCreature.replayCreature(tickCount);
      }
    }

    if (player.isDead.get())
    {
      playerLives--;
      soundManager.stopTrack();
      soundManager.playSoundClip(Sound.DEATH);
      //log this player as a new PastCreature to be replayed upon level restart
      pastCreatures.add(new PastCreature(player.getWalkBehaviorsX(), player.getWalkBehaviorsZ(),
                                         player.getXPositions(), player.getZPositions(),player.getAngleBehaviors(),
                                         player.getAttackBehaviors(), player.getPushBehaviors(), player.getPushedBehaviors(),
                                         tickCount, this));

      HBox hBox = new HBox();
      if(playerLives <= 0)
      {
        hBox.getChildren().addAll(scenes.mainMenuButton);
        Label gameOverLabel = new Label("Game Over!");
        scenes.gameOverRoot.setCenter(gameOverLabel);
        destroyZombieHouse();
      }
      else
      {
        hBox.getChildren().addAll(scenes.mainMenuButton, scenes.tryAgainButton);
        Label livesRemaining = new Label("Lives Remaining: " + playerLives);
        scenes.gameOverRoot.setCenter(livesRemaining);
      }
      scenes.gameOverRoot.setTop(hBox);
      main.assignStage(scenes.gameOver);
      zombieHouse.gameLoop.stop();
    }

    if (player != null && player.foundExit.get())
    {
      soundManager.stopTrack();
      soundManager.playSoundClip(Sound.ACHIEVE);
      destroyZombieHouse();
      HBox hBox = new HBox();
      scenes.updateWinScreen();
      hBox.getChildren().addAll(scenes.mainMenuButton);
      scenes.winRoot.setTop(hBox);
      main.assignStage(scenes.win);
    }

    if(player != null)
    {
      //@author: Debbie Berlin: created below camera related code so that walls/obstacles do not block view of player
      //Instead, the camera will zoom in/out when player approaches a wall and adjusts the field of view accordingly
      boolean cameraGroupIntersect;

      for (int i = 0; i < numTiles; i++)
      {
        cameraGroupIntersect = player.gameCamera.cameraGroup.getBoundsInParent().intersects(zombieHouse.getWalls().get(i).getBoundsInParent());

        if (cameraGroupIntersect)
        {
          int playerToWallDistanceChange = player.comparePlayerDistanceFromWall(player.calculateDistanceToWall(zombieHouse.getWalls().get(i)));

          switch (playerToWallDistanceChange)
          {
            case 0:
              break;
            case 1://player moves away from wall
              if(player.cameraToPlayerGap >= Player.CAMERA_TO_PLAYER_GAP_MAX)
              {
                player.cameraToPlayerGap = Player.CAMERA_TO_PLAYER_GAP_MAX;
              }
              else player.cameraToPlayerGap += 0.5;

              if(player.gameCamera.camera.getFieldOfView() <= GameCamera.CAMERA_FIELD_OF_VIEW_MIN)
              {
                player.gameCamera.camera.setFieldOfView(GameCamera.CAMERA_FIELD_OF_VIEW_MIN);
              }
              else player.gameCamera.camera.setFieldOfView( player.gameCamera.camera.getFieldOfView() - 0.5);

              break;
            case -1://player moves toward wall
              if(player.cameraToPlayerGap <= Player.CAMERA_TO_PLAYER_GAP_MIN)
              {
                player.cameraToPlayerGap = Player.CAMERA_TO_PLAYER_GAP_MIN;
              }
              else
              {
                player.cameraToPlayerGap -= 0.5;

                if(player.gameCamera.camera.getFieldOfView() >= GameCamera.CAMERA_FIELD_OF_VIEW_MAX)
                {
                  player.gameCamera.camera.setFieldOfView(GameCamera.CAMERA_FIELD_OF_VIEW_MAX);
                }
                else player.gameCamera.camera.setFieldOfView( player.gameCamera.camera.getFieldOfView() + 0.5);
              }

              break;
          }
        }
        else
        {
          player.gameCamera.cameraGroup.getTransforms().remove(2);//this is the index of the former Translate transform
          player.gameCamera.cameraGroup.getTransforms().add(new Translate(0,  -.35, -Player.CAMERA_TO_PLAYER_GAP_MAX));
          if(player.cameraToPlayerGap == Player.CAMERA_TO_PLAYER_GAP_MIN)
          {
            player.gameCamera.camera.setFieldOfView(GameCamera.CAMERA_FIELD_OF_VIEW_MAX);
          }
        }
      }
      player.gameCamera.cameraGroup.getTransforms().remove(2);
      player.gameCamera.cameraGroup.getTransforms().add(new Translate(0,  -.35, -player.cameraToPlayerGap));
      if(player.cameraToPlayerGap == Player.CAMERA_TO_PLAYER_GAP_MIN)
      {
        player.gameCamera.camera.setFieldOfView(GameCamera.CAMERA_FIELD_OF_VIEW_MAX);
      }
    }

    tickCount++;
  }

  /**@author Nick Schrandt
   *
   * Increments the counter for the region where a new zombie spawn tile was created.
   * This will create a more general distribution of zombies.
   *
   * @param region the region that a new zombie tile is in
   */
  private void incrementRegionCounter(int region)
  {
    if(region == 1)
    {
      region1Counter++;
    }
    if(region == 2)
    {
      region2Counter++;
    }
    if(region == 3)
    {
      region3Counter++;
    }
    if(region == 4)
    {
      region4Counter++;
    }
  }

  /**@author Nick Schrandt
   *
   * Returns the number of zombies in a specific region. Used to create a more general
   * distribution of zombies.
   *
   * @param region region where a zombie spawn tile is created
   * @return number of zombie spawn tiles in that region.
   */
  private int getRegionCounter(int region)
  {
    if(region == 1)
    {
      return region1Counter;
    }
    if(region == 2)
    {
      return region2Counter;
    }
    if(region == 3)
    {
      return region3Counter;
    }
    if(region == 4)
    {
      return region4Counter;
    }
    return 0;
  }

  /**@author Nick Schrandt
   *
   * @param recordableCreatureAngle the angle that the RecordableCreature is facing
   * @return the lower bounding for the RecordableCreature's hitbox based on the angle they're facing
   */
  double findLowerAttackBound(double recordableCreatureAngle)
  {
    if(recordableCreatureAngle - PLAYER_ANGLE_OFFSET > 0)
    {
      return recordableCreatureAngle - PLAYER_ANGLE_OFFSET;
    }
    else
    {
      return 360 + recordableCreatureAngle - PLAYER_ANGLE_OFFSET;
    }
  }

  /**@author Nick Schrandt
   *
   * @param recordableCreatureAngle the angle that the RecordableCreature is facing
   * @return upper bounding for the RecordableCreature's hitbox
   */
  double findUpperAttackBound(double recordableCreatureAngle)
  {
    return (recordableCreatureAngle + PLAYER_ANGLE_OFFSET)%360;
  }

  /**
   *
   * @author Jeffrey McCall This is a class that extends Thread and is used to
   *         keep track of the decision rate of the zombies, which is 2 seconds.
   *
   */
  private class ZombieDecision extends Thread
  {
    /**
     * Every two seconds, if the zombie is a random walk zombie, a new angle for
     * the zombie to walk in is chosen. If the zombie has hit an obstacle, then
     * the angleAdjusted boolean flag will be on, to indicate that the angle was
     * adjusted when the zombie hit an obstacle. In this case, the
     * "makeDecision()" method is called to determine the new angle for the
     * zombie to travel in, and start it moving again. If the zombie is chasing after
     * the player, then the "findNewPath" boolean is set to "on" to indicate that a new
     * direction towards the player needs to be set.
     */
    @Override
    public void run()
    {
      while (gameIsRunning.get()) {
        try {
          sleep(2000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        synchronized (zombies) {
          for (Zombie zombie : zombies) {
            zombie.moveZombiesOffEachOther = false;
            if(!zombie.isMasterZombie) {
              if(zombie.resonanceState) {
                zombie.resetHeading(player);
                zombie.resonanceState = false;
              } else if(zombie.overcrowdedState) {
                zombie.moveZombiesOffEachOther = true;
                zombie.overcrowdedState = false;
              } else {
                if(zombie.isGoingAfterPlayer.get()) {
                  zombie.findNewPath.set(true);
                }
                if(zombie.isRandomWalkZombie && !zombie.isGoingAfterPlayer.get()) {
                  zombie.angle = zombie.rand.nextInt(360);
                }
                if(zombie.angleAdjusted.get()) {
                  zombie.makeDecision();
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   *
   * @author Jeffrey McCall
   * Thread for the decision rate of the master zombie. It has a
   * faster decision rate than the regular zombies. The same operations
   * are performed on the master zombie that are performed on the
   * other zombies.
   *
   */
  private class MasterZombieDecision extends Thread
  {
    /**
     * While the game is running, perform the same operations on
     * the master zombie that would be performed on the regular zombies.
     */
    @Override
    public void run()
    {
      while (gameIsRunning.get())
      {
        try
        {
          sleep(500);
        } catch (InterruptedException e)
        {
          e.printStackTrace();
        }
        masterZombie.moveZombiesOffEachOther = false;
        if(masterZombie.resonanceState)
        {
          masterZombie.resetHeading(player);
          masterZombie.resonanceState = false;
        }
        else if(masterZombie.overcrowdedState)
        {
          masterZombie.moveZombiesOffEachOther = true;
          masterZombie.overcrowdedState = false;
        }
        else
        {
          if(masterZombie.masterZombieChasePlayer.get())
          {
            masterZombie.findNewPath.set(true);
          }
          if(masterZombie.isRandomWalkZombie && !masterZombie.masterZombieChasePlayer.get())
          {
            masterZombie.angle = masterZombie.rand.nextInt(360);
          }
          if(masterZombie.angleAdjusted.get())
          {
            masterZombie.makeDecision();
          }
        }
      }
    }
  }

  /**
   * @param zombieHouse
   * ZombieHouse3d Object
   *
   * This Method sets the the current instance of zombieHouse3d with the parameter
   * zombieHouse
   */
  public void setZombieHouse3d(ZombieHouse3d zombieHouse)
  {
    this.zombieHouse = zombieHouse;
  }

  /**
   * Clears game data
   * @author: original code, Sarah Salmonson
   */
  private void disposeCreatures()
  {
    gameIsRunning.set(false);

    player.dispose();
    player = null;

    for(Zombie zombie: zombies)
    {
      zombie.dispose();
    }
    zombies.clear();

    for(PastCreature pastCreature : pastCreatures)
    {
      pastCreature.dispose();
    }
    pastCreatures.clear();
  }

  /**
   * Darken tiles as a function of camera's visible distance, or FarClip, setting
   * @author Sarah Salmonson
   */
  private void fadeToBlack(List<Box> tiles)
  {
    double visibleDistance = zombieHouse.getCamera().getFarClip();
    //get camera's current location
    double playerXLocation = player.getBoundingCylinder().getTranslateX();
    double playerZLocation = player.getBoundingCylinder().getTranslateZ();
    for (Box tile : tiles)
    {
      //get the location of the tile we're examining
      double tileXLocation = tile.getTranslateX();
      double tileZLocation = tile.getTranslateZ();

      //get the distance between player and tile X and Y coordinates
      double distanceX = playerXLocation - tileXLocation;
      double distanceZ = playerZLocation - tileZLocation;

      //calculate linear distance using coordinates. Formula d = sqrt[(x1-x2)^2 + (y1-y2)^2]
      double linearDistance = Math.sqrt(distanceX * distanceX + distanceZ * distanceZ);

      //use this calculation to generate a distanceModifier that will impact tile texture color
      double darknessValue = 1.0 - linearDistance / visibleDistance;

      //if distance is in visible range, use the distance modifier to set the tile texture color
      //the further away, the darker the wall texture
      if (darknessValue < 0)
      {
        darknessValue = 0.0;
      }
      ((PhongMaterial) tile.getMaterial()).setDiffuseColor(Color.color(darknessValue, darknessValue, darknessValue));
      ((PhongMaterial) tile.getMaterial()).setSpecularColor(Color.color(darknessValue, darknessValue, darknessValue));
    }
  }

  /**
   * Disposes of the Zombie House elements.
   * @author: Sarah Salmonson
   */
  public void destroyZombieHouse()
  {
    if(zombieHouse != null)
    {
      zombieHouse.dispose();
    }
    disposeCreatures();
  }

  /**
   * Resets the entity manager for a new playthrough of the same map
   * @author Sarah Salmonson
   */
  public void resetEM()
  {
    zombies.clear();
    tickCount = 0;
  }
}