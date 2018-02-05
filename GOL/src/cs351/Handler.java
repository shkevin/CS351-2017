package cs351;

/*
 * Listeners for making the custom canvas zoomable
 * Handles zooming and cell calculations.
 */

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

class Handler
{
  private static final double MAX_SCALE = 50.0;
  private static final double MIN_SCALE = 2.0;
  private Controller controller;
  private CanvasClass canvas;

  Handler(CanvasClass canvas, Controller controller)
  {
    this.controller = controller;
    this.canvas = canvas;
  }

  /**
   * Mouse wheel handler: zoom to pivot point. Currently top left.
   * Allows for cell processing, focuses on top left cell even when
   * shifted/zoomed. Functionality has not been implemented within
   * GUI.
   */
  private EventHandler<ScrollEvent> onScrollEventHandler = new EventHandler<ScrollEvent>()
  {
    @Override
    public void handle(ScrollEvent event)
    {

      double scale = canvas.getScale(); // currently we only use Y, same value is used for X
      controller.horizontalScrollBar.setMax(scale * Main.SIZE);
      controller.verticalScrollBar.setMax(scale * Main.SIZE);

      double delta = 1.1;
      if (event.getDeltaY() < 0) scale /= delta;
      else scale *= delta;

      scale = clamp(scale, MIN_SCALE, MAX_SCALE); //does not allow larger than 50, or smaller than 1

      canvas.dx = controller.pixelX * (1 - delta);
      canvas.dy = controller.pixelY * (1 - delta);

      double hVal = controller.xScrollValue + (-canvas.dx);
      double vVal = controller.yScrollValue + (-canvas.dy);

      canvas.topLeftCellX = (int) Math.floor(hVal / scale);
      canvas.topLeftCellY = (int) Math.floor(vVal / scale);

      canvas.setScale(scale);

      event.consume();
      canvas.updateGUI();
    }
  };

  /**
   * Handles mouse over on Canvas. Used for getting current location
   * and would've been used with implemented zoom on cursor.
   */
  private EventHandler<MouseEvent> onMouseEventHandler = new EventHandler<MouseEvent>()
  {
    @Override
    public void handle(MouseEvent event)
    {
      controller.pixelY = controller.yScrollValue + event.getY();
      controller.pixelX = controller.xScrollValue + event.getX();
//      double scale = canvas.getScale();

//      int cellY = (int) (controller.pixelX

//      if (cellX >= Main.SIZE) cellX = Main.SIZE - 1;
//      if (cellY >= Main.SIZE) cellY = Main.SIZE - 1;

//      System.out.print("cell: " + cellX + ", " + cellY + "    ");
//      System.out.println("topLeft: " + canvas.topLeftCellX + ", " + canvas.topLeftCellY);
    }
  };

  /**
   * simple getter for scroll handler.
   * @return the onScrollHandler/ scale);
  //      int cellX = (int) (controller.pixelY / scale);
   */
  EventHandler<ScrollEvent> getOnScrollEventHandler()
  {
    return onScrollEventHandler;
  }

  /**
   * simple getter for mouse handler within canvas.
   * @return mouse handler
   */
  EventHandler<MouseEvent> getOnMouseEventHandler()
  {
    return onMouseEventHandler;
  }

  /**
   * Method used to set max zoom out/zoom in.
   * @param value value based on negative/max positive.
   * @param min value of zoom out.
   * @param max value of zoom in.
   * @return updated value based on current zoom.
   */
  private static double clamp(double value, double min, double max)
  {

    if (Double.compare(value, min) < 0)
      return min;

    if (Double.compare(value, max) > 0)
      return max;

    return value;
  }
}
