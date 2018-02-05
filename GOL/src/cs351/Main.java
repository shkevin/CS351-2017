package cs351;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Created by Kevin Cox on 2/3/17.
 * Initializes and runs GOL program
 */

public class Main extends Application
{
  static final int CELLSIZE = 10;
  static final int SIZE = 150;

  @Override
  public void start(Stage primaryStage) throws Exception
  {
    primaryStage.setTitle("Game Of Life");

    FXMLLoader loader = new FXMLLoader(getClass().getResource("GUI.fxml"));
    Parent root = loader.load();

    primaryStage.setScene(new Scene(root, 800, 500));
    primaryStage.setMinHeight(500);
    primaryStage.setMinWidth(500);

    primaryStage.show();
    primaryStage.setOnCloseRequest(event -> System.exit(0));
  }


  public static void main(String[] args)
  {
    launch(args);
  }
}
