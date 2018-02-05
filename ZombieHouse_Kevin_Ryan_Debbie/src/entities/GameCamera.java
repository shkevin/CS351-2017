package entities;

import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;

/**
 * Class intended to manage camera related properties in the game.
 * Each GameCamera class has a PerspectiveCamera, a Box used for camera bounding, and
 * a cameraGroup to synchronize their movements.
 *
 * @author: Debbie Berlin
 */
public class GameCamera
{
  public Group cameraGroup;
  public PerspectiveCamera camera;
  public Box boundingBox;//a thin box with a long depth, essentially a guidance stick

  public final static int CAMERA_FIELD_OF_VIEW_MIN = 70;
  public final static int CAMERA_FIELD_OF_VIEW_MAX = 80;


  //The box is centered at the camera (aka camera group position).
  //(I.e. it extends an equal distance in front of and behind the camera)
  //This is done so that it serves as a trigger when the camera is approaching a wall
  //behind it, and continues to trigger after the camera has gone completely behind the wall.
  private final double DEPTH_OF_BOX = 8;//8

  public GameCamera()
  {
    camera = new PerspectiveCamera(true);
    boundingBox = new Box(.01, .01, DEPTH_OF_BOX);

    cameraGroup = new Group();
    cameraGroup.setRotationAxis(Rotate.Y_AXIS);
    cameraGroup.getChildren().addAll(camera, boundingBox);
  }
}