package cs351;

/*
 * Created by Kevin Cox on 2/10/2017.
 * Custom canvas object, sets up Color array for cell growth,
 * handles GUI updates, and integrates with Handler class.
 */

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;

  class CanvasClass extends Canvas
  {
    private Controller controller;
    private ArrayList<Color> colorArrayList = new ArrayList<>(10);
    private Board board;
    private DoubleProperty myScale = new SimpleDoubleProperty(Main.CELLSIZE);
    int topLeftCellX = 0;
    int topLeftCellY = 0;
    double dx = 0;
    double dy = 0;
    private GraphicsContext gc;

    CanvasClass(Board board, Controller controller)
    {
      this.controller = controller;
      initializeColors();
      this.board = board;
      widthProperty().addListener(event -> updateGUI());
      heightProperty().addListener(event -> updateGUI());
    }

    /**
     * Initializes array of colors used in GUI. Allows for
     * different ages of cells. Currently selected is Blue.
     */
    private void initializeColors()
    {
//      colorArrayList.add(0, Color.rgb(0,255,0));
//      colorArrayList.add(1, Color.rgb(0,235,0));
//      colorArrayList.add(2, Color.rgb(0,215,0));
//      colorArrayList.add(3, Color.rgb(0,195,0));
//      colorArrayList.add(4, Color.rgb(0,175,0));
//      colorArrayList.add(5, Color.rgb(0,155,0));
//      colorArrayList.add(6, Color.rgb(0,135,0));
//      colorArrayList.add(7, Color.rgb(0,120,0));
//      colorArrayList.add(8, Color.rgb(0,100,0));
//      colorArrayList.add(9, Color.rgb(0,80,0));
      colorArrayList.add(0, Color.rgb(111,105,239));
      colorArrayList.add(1, Color.rgb(90,83,237));
      colorArrayList.add(2, Color.rgb(75,67,235));
      colorArrayList.add(3, Color.rgb(56,47,233));
      colorArrayList.add(4, Color.rgb(34,24,230));
      colorArrayList.add(5, Color.rgb(31,22,212));
      colorArrayList.add(6, Color.rgb(28,20,192));
      colorArrayList.add(7, Color.rgb(25,18,170));
      colorArrayList.add(8, Color.rgb(24,17,157));
      colorArrayList.add(9, Color.rgb(14,10,96));
    }

    /**
     * draws the Game of Life grid. Handles resising and scrolling.
     */
    private void drawGrid()
    {
      double w = widthProperty().get();
      double h = heightProperty().get();

      setMouseTransparent(true);

      gc = getGraphicsContext2D();
      gc.clearRect(0, 0, w, h);

      gc.setStroke(Color.rgb(112,67,0));
      gc.setLineWidth(1);

      // draw grid lines
      for (double i =  0; i < w+controller.yScrollValue; i += myScale.get())
      {
        if (myScale.get() >= 5)
        {
          // vertical
          gc.strokeLine(i-controller.yScrollValue, 0, i-controller.yScrollValue, h);
        }
      }
      for (double i =  0; i < h+controller.yScrollValue; i += myScale.get())
      {
        if (myScale.get() >= 5)
        {
          // horizontal
          gc.strokeLine(0, i-controller.xScrollValue, w, i-controller.xScrollValue);
        }
      }
      toBack();
    }

    /**
     * updates the GUI after the board has been updated.
     * Allows for shifting and zooming with cells.
     */
    void updateGUI()
    {
      drawGrid();
//      gc = getGraphicsContext2D();
      for (int i = 0; i < Main.SIZE; i++) //will need to be changed to grid width and height
      {
        for (int j = 0; j < Main.SIZE; j++)
        {
          if (board.board[i][j] != 0)
          {
            if (board.board[i][j] != 0)
            {
              if (board.board[i][j] >= 10) gc.setFill(colorArrayList.get(9));
              else gc.setFill(colorArrayList.get(board.board[i][j]-1)); //occasional out of bounds
              gc.fillRect(i*myScale.get()-controller.yScrollValue, j*myScale.get()-controller.xScrollValue, myScale.get() - 1, myScale.get() - 1);
            }
          }
        }
      }
    }

    /**
     * getter for the scaled integer. Used for zooming/panning.
     * @return myScale
     */
    double getScale() {
      return myScale.get();
    }

    /**
     *  sets the new scale for the canvas.
     * @param scale myScale
     */
    void setScale(double scale) {
      myScale.set(scale);
    }
}
