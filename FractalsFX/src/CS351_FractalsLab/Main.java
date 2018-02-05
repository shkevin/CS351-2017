package CS351_FractalsLab;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Basic application to visually see the Mandelbrot set
 */

public class Main extends Application
{
  static int width = 600;
  static int height = 500;

  @Override
  public void start(Stage primaryStage) throws Exception
  {
    primaryStage.setTitle("Fractals");
    FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
    Parent root = loader.load();
    primaryStage.setScene(new Scene(root, width, height));
    primaryStage.setMinWidth(500);
    primaryStage.setMinHeight(500);

    Mandelbrot mbrot = new Mandelbrot(170, loader.getController());
    mbrot.createContent();

    primaryStage.show();
    primaryStage.setOnCloseRequest(event -> System.exit(0));
  }

  public static void main(String[] args)
  {
    launch(args);
  }
}
