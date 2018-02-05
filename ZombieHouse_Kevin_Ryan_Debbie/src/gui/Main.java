package gui;

import game_engine.Scenes;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import sound.SoundManager;

/**
 * @author Atle Olson
 *         Main launch point for the program
 */
public class Main extends Application
{
  private Stage gameStage;

  /**
   * @param primaryStage Start function to launch program
   */
  @Override
  public void start(Stage primaryStage) throws Exception
  {
    gameStage = primaryStage;
    Scenes scenes = new Scenes(primaryStage, this);

    SoundManager soundManager = new SoundManager();
    scenes.setSoundManager(soundManager);
    soundManager.playTrack(0);

    gameStage.setTitle("Zombie House By Kevin, Ryan, and Debbie");
    gameStage.setScene(scenes.mainMenu);
    gameStage.show();

    primaryStage.setOnCloseRequest(e -> System.exit(0));
  }

  /**
   * @param scene Sets the stages' scene equal to scene
   */
  public void assignStage(Scene scene)
  {
    gameStage.setScene(scene);
  }

  /**
   * Gets the current stage
   *
   * @return gameStage
   */
  public Stage retrieveStage()
  {
    return gameStage;
  }

  /**
   * Gets the current scene
   *
   * @return gameStage.getScene()
   */
  public Scene retrieveScene()
  {
    return gameStage.getScene();
  }

  /**
   * main, launches our program
   */
  public static void main(String[] args)
  {
    launch(args);
  }

}
