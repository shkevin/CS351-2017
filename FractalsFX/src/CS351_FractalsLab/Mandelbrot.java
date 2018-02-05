package CS351_FractalsLab;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Created by Kevin Cox on 4/1/2017.
 *
 * Simple class containing everything related to Mandelbrot sets and  updating the
 * canvas.
 */
class Mandelbrot
{
  private Canvas canvas;
  private int maxIterations;
  private Controller controller;

  Mandelbrot(int iterations, Controller controller)
  {
    this.controller = controller;
    this.maxIterations = iterations;
    canvas = new Canvas();
    System.out.println("initializing canvas");
  }

  /**
   * Sets up the content for the canvas size properties, and draws to canvas here.
   */
  void createContent()
  {
    canvas.heightProperty().bind(controller.anchorPane.heightProperty());
    canvas.widthProperty().bind(controller.anchorPane.widthProperty());
    controller.anchorPane.getChildren().add(canvas);
    canvas.heightProperty().addListener(event -> update());
    canvas.widthProperty().addListener(event -> update());
  }

  /**
   * updated the canvas and checks the convergence of each c. Uses threshold to determine
   * whether converging or diverging, and colors the canvas based off of iterations and
   * maxIterations.
   *
   * version from http://jonisalonen.com/2013/lets-draw-the-mandelbrot-set/
   */
  private void update()
  {
    int width = Main.width;
    int height = Main.height;
    GraphicsContext gc = canvas.getGraphicsContext2D();
    for (int row = 0; row < height; row++) {
      for (int col = 0; col < width; col++) {
        double c_re = (col - width/2)*4.0/width;
        double c_im = (row - height/2)*4.0/width;
        double x = 0, y = 0;
        int iterations = 0;
        int threshold = 4;
        while (x*x+y*y < threshold && iterations < maxIterations) {
          double x_new = x*x-y*y+c_re;
          y = 2*x*y+c_im;
          x = x_new;
          iterations++;
        }
        double t1 = (double) iterations / maxIterations;
        double c1 = Math.min(255 * 2 * t1, 255);
        double c2 = Math.max(255 * (2 * t1 - 1), 0);
        if (iterations != maxIterations) {
          gc.setFill(Color.color(c2 / 255.0, c1 / 255.0, c2 / 255.0));
        } else {
          gc.setFill(Color.RED);
        }
        gc.fillRect(col, row, 1, 1);
      }
    }
  }

}
