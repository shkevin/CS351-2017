package game_engine;

import entities.Player;
import javafx.event.EventHandler;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import sound.Sound;

import java.awt.*;

/**
 * @author Jeffrey McCall
 * This class handles all of the mouse input
 * into the game. When the mouse is moved, the camera
 * is rotated appropriately.
 */

/**
 * 
 * @author Atle Olson 
 *        Jeffrey McCall
 *        modified (and cleaned up) by Hector Carrillo using original code + ideas from Maxwell's code
 *        Nick Schrandt
 *          
 * Handles mouse events in the game. Moves the player camera to the 
 * left and right. 
 */
public class MouseEventHandler implements EventHandler<MouseEvent>
{
  // player whose camera is rotated
  private Player player;
  private ZombieHouse3d zombiehouse;

  // the center x coordinate of the current scene
  private double centerX = 0;
  private double rotationSpeed = 1.5*Math.PI;
  // the x coordinate of the mouse when it was moved
  private double currentX = 0;
  private boolean robotMove = false;

  /**
   * Constructor for the program.
   *
   * @param player used to modify his angle and camera angle
   * @param zombiehouse uses scene from zombie house to change
   */
  public MouseEventHandler(Player player, ZombieHouse3d zombiehouse)
  {
    this.player = player;
    this.zombiehouse = zombiehouse;
  }

  private class myRobot extends Robot
  {
    myRobot() throws java.awt.AWTException
    {
      super();
    }

    @Override
    public synchronized void mouseMove(int x, int y)
    {
      super.mouseMove(x, y);
    }
  }
  /**
   * Create a robot to reset the mouse to the middle of the screen.
   */
  private myRobot robot;
  {
    try
    {
      robot = new myRobot();
    }
    catch (Exception e)
    {
      System.out.println("ERROR");
      e.printStackTrace();
    }
  }

  /**
   * Handles all of the mouse movement events. Moves the camera based on if the mouse is left or right of the center.
   * This avoids moving the camera when the mouse is reset back since the mouse x coordinate will equal the center x
   * coordinate (which is neither left or right).
   *
   * @param event All mouse motion events are automatically passed into
   *              this method.
   * @authors: original code, Nick Schrandt
   */
  @Override
  public void handle(MouseEvent event)
  {
    /*
    Author: Nick Schrandt

    Added this to the handle so that the left mouse click will attack any zombies in the player's bounding circle.
    Also sets the player to attacking, preventing any further attacks until the animation is complete. Also activates
    the sword SWING sound.
     */
    if(event.getEventType() == MouseEvent.MOUSE_CLICKED)
    {
      if (event.getButton() == MouseButton.PRIMARY && !player.isAttacking())
      {
        player.setAttacking(true);
        zombiehouse.getEntityManager().playerAttack();
      }
    }

    // Do this if the mouse event is actually the robot moving the cursor back
    if(robotMove)
    {
      // top corner coordinates of scene
      double topX = event.getScreenX() - event.getSceneX();
      centerX = topX + zombiehouse.scene.getWidth() / 2;
      currentX = topX + event.getSceneX();
      turnCamera();
      robotMove = false;
    }
    else//robotMove is false
    {
      // top corner coordinates of scene
      double topX = event.getScreenX() - event.getSceneX();
      double topY = event.getScreenY() - event.getSceneY();
      centerX = topX + zombiehouse.scene.getWidth() / 2;
      currentX = topX + event.getSceneX();
      turnCamera();
      robotMove = true;

      try
      {
        // Reset mouse to middle of screen,
        // @Debbie: changed to call on new Robot() so it wouldn't change robotMoved boolean from MyRobot.mouseMove()
        new Robot().mouseMove((int) topX + (int) (zombiehouse.scene.getWidth() / 2), (int) topY +
                (int) (zombiehouse.scene.getHeight() / 2));
        currentX = topX + zombiehouse.scene.getWidth() / 2;
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  //@Debbie: encapsulated into method to reduce duplicate code
  private void turnCamera()
  {
    // cursor is to the right of the center so rotate right
    if (currentX-10 > centerX)
    {
      player.gameCamera.cameraGroup.setRotate(player.angle += rotationSpeed);
      player.turnRight = true;
    }
    // cursor is to the left of the center so rotate left
    else if (currentX+10 < centerX)
    {
      player.gameCamera.cameraGroup.setRotate(player.angle -= rotationSpeed);
      player.turnLeft = true;
    }
  }
}