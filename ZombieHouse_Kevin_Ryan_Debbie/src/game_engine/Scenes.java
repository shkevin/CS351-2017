package game_engine;

import gui.Main;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import sound.Sound;
import sound.SoundManager;
import utilities.MapViewerScene;

import java.util.ArrayList;

/**
 * @author Atle Olson
 *         A place to stage Scenes and switch them out
 */
public class Scenes
{
  private SoundManager soundManager;
  private Main main;

  private final int BUTTON_WIDTH = 250;
  private final int BUTTON_HEIGHT = 10;
  private final int SLIDER_WIDTH = 500;
  private final int SLIDER_HEIGHT = 10;
  private final int DEFAULT_DIFFICULTY = 2;

  int winW = 1600;
  int winH = 850;

  public Button mainMenuButton = new Button();
  public Button tryAgainButton = new Button();

  private Button playButton = new Button();
  private Button mapViewerButton = new Button();
  private Button gameOverButton = new Button();
  private Button winScreenButton = new Button();
  private Button settingsButton = new Button();

  private Label playerHitPoints = new Label("HP: ");
  private Label playerStaminaPoints = new Label("Stamina: ");

  private Slider playerHearing = new Slider(0, 50, 1);
  private Slider playerWalkingSpeed = new Slider(0, 2, 0.16);
  private Slider playerSprintSpeed = new Slider(0, 2, 0.25);
  private Slider playerStamina = new Slider(0, 10, 5);
  private Slider playerRegen = new Slider(0, 2, 0.2);
  private Slider zombieSmell = new Slider(0, 30, 15);
  private Slider maxZombies = new Slider(0, 30, 20);
  private Slider minZombies = new Slider(0, 30, 7);
  private Slider mapWidth = new Slider(10, 100, 50);
  private Slider mapHeight = new Slider(10, 100, 50);
  private Slider rotateSensitivity = new Slider(0, 20, 5);

  private ZombieHouse3d zombieHouse = new ZombieHouse3d(0, soundManager, main, this);
  private MapViewerScene mapObject = new MapViewerScene();

  public BorderPane startRoot, twoDGameRoot, settingsRoot, gameOverRoot, loadRoot, winRoot;
  public Scene mainMenu, gameOver, win, settings; //twoDGame & loading Scenes currently unused

  private ImageView bloodView = new ImageView();

  StackPane threeDGameRoot;

  /**
   * @param primaryStage of application
   * @param main         Constructor for a scenes object
   */
  public Scenes(Stage primaryStage, Main main)
  {
    this.main = main;
    mainMenuButton.setText("Main Menu");
    mainMenuButton.setPrefSize(BUTTON_WIDTH,BUTTON_HEIGHT);
    mainMenuButton.setOnAction(event ->
    {
      playButtonSound();
      soundManager.playTrack(0);
      try
      {
        main.assignStage(mainMenu);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      if(zombieHouse.getEntityManager() != null)
      {
        zombieHouse.getEntityManager().destroyZombieHouse();
      }
    });

    playButton.setText("Play");
    playButton.setPrefSize(BUTTON_WIDTH,BUTTON_HEIGHT);
    playButton.setOnAction(event ->
    {
      playButtonSound();
      createNewGameBoard(DEFAULT_DIFFICULTY);
      zombieHouse.build3DMap();
      setUpNewGame(primaryStage, false);
    });

    tryAgainButton.setText("Try Again");
    tryAgainButton.setPrefSize(BUTTON_WIDTH,BUTTON_HEIGHT);
    tryAgainButton.setOnAction(event ->
    {
      playButtonSound();
      setUpNewGame(primaryStage, true);
      soundManager.playTrack(0);
    });

    mapViewerButton.setText("Map Viewer");
    mapViewerButton.setPrefSize(BUTTON_WIDTH,BUTTON_HEIGHT);
    mapViewerButton.setOnAction(event ->
    {
      playButtonSound();
      try
      {
        main.assignStage(mapObject.mapViewerScene(primaryStage, zombieHouse));
        MapViewerScene.root.getChildren().add(mainMenuButton);
        MapViewerScene.root.getStylesheets().add(Scenes.class.getResource("/stylesheets/MapViewSceneStyleSheet.css").toExternalForm());
      } catch (Exception e)
      {
        e.printStackTrace();
      }
    });

    gameOverButton.setText("Game Over");
    gameOverButton.setPrefSize(BUTTON_WIDTH,BUTTON_HEIGHT);
    gameOverButton.setOnAction(event ->
    {
      playButtonSound();
      try
      {
        gameOverRoot.setTop(mainMenuButton);
        main.assignStage(gameOver);
      } catch (Exception e)
      {
        e.printStackTrace();
      }
    });

    winScreenButton.setText("Win Screen");
    winScreenButton.setPrefSize(BUTTON_WIDTH,BUTTON_HEIGHT);
    winScreenButton.setOnAction(event ->
    {
      playButtonSound();
      try
      {
        winRoot.setTop(mainMenuButton);
        main.assignStage(win);
      } catch (Exception e)
      {
        e.printStackTrace();
      }
    });

    settingsButton.setText("Settings");
    settingsButton.setPrefSize(BUTTON_WIDTH,BUTTON_HEIGHT);
    settingsButton.setOnAction(event ->
    {
      playButtonSound();
      try
      {
        settingsRoot.setTop(mainMenuButton);
        main.assignStage(settings);
      } catch (Exception e)
      {
        e.printStackTrace();
      }
    });

    setupSliders();
    addSliderProperties();

    createStartRoot();
    createSettingRoot();
    createGameOverRoot();
    createWinRoot();
    //createLoadRoot(); //@Debbie: commenting out since not currently using
    //create2DGameRoot(); //@Debbie: commenting out since not currently using

    mainMenu = new Scene(startRoot);
    settings = new Scene(settingsRoot);
    gameOver = new Scene(gameOverRoot);
    win = new Scene(winRoot);
    //twoDGame = new Scene(twoDGameRoot); //@Debbie: commenting out since not currently using
    //loading = new Scene(loadRoot);//@Debbie: commenting out since not currently using
  }

  private void createStartRoot()
  {
    //Main menu Scene
    startRoot = new BorderPane();
    startRoot.setPrefSize(winW, winH);
    startRoot.getStylesheets().add(Scenes.class.getResource("/stylesheets/StartRootStyleSheet.css").toExternalForm());
    VBox buttonVBox = new VBox();
    buttonVBox.getChildren().addAll(
            playButton,
            mapViewerButton,
            gameOverButton,
            winScreenButton,
            settingsButton
    );
    buttonVBox.setSpacing(5);
    buttonVBox.setPadding(new Insets(5, 5, 5, 5));
    startRoot.setCenter(buttonVBox);
  }

  private void createSettingRoot()
  {
    //Settings Scene
    settingsRoot = new BorderPane();
    settingsRoot.setPrefSize(winW, winH);
    settingsRoot.getStylesheets().add(Scenes.class.getResource("/stylesheets/SettingsRootStyleSheet.css").toExternalForm());

    VBox sliders = new VBox();
    sliders.getChildren().addAll(
            new Label("Player Hearing"),
            playerHearing,
            new Label("Player Walking Speed"),
            playerWalkingSpeed,
            new Label("Player Sprint Speed"),
            playerSprintSpeed,
            new Label("Player Stamina"),
            playerStamina,
            new Label("Player Regen"),
            playerRegen,
            new Label("Zombie Smell"),
            zombieSmell
    );

    VBox sliders2 = new VBox();
    sliders2.getChildren().addAll(

            new Label("Max Zombies"),
            maxZombies,
            new Label("Min Zombies"),
            minZombies,
            new Label("Map Width"),
            mapWidth,
            new Label("Map Height"),
            mapHeight,
            new Label("Rotate Sensitivity"),
            rotateSensitivity
    );

    HBox slidersColumns = new HBox();//@Debbie: split up sliders into 2 vboxes and placed in hbox for better layout
    slidersColumns.getChildren().addAll(sliders, sliders2);
    slidersColumns.setPadding(new Insets(10, 20, 10, 80));//top, right, bottom, left
    slidersColumns.setSpacing(100);
    settingsRoot.setLeft(slidersColumns);
  }

  private void createGameOverRoot()
  {
    //Game Over Scene
    gameOverRoot = new BorderPane();
    gameOverRoot.setPrefSize(winW, winH);
    gameOverRoot.getStylesheets().add(Scenes.class.getResource("/stylesheets/GameOverRootStyleSheet.css").toExternalForm());
    Label gameOverLabel = new Label("Game Over!");
    gameOverRoot.setCenter(gameOverLabel);
    HBox hBoxGameOver = new HBox();
    hBoxGameOver.getChildren().addAll(mainMenuButton, tryAgainButton);
    gameOverRoot.setTop(hBoxGameOver);
  }

  private void createWinRoot()
  {
    //Win Scene
    winRoot = new BorderPane();
    winRoot.setPrefSize(winW, winH);
    winRoot.getStylesheets().add(Scenes.class.getResource("/stylesheets/WinRootStyleSheet.css").toExternalForm());
    Label winLabel = new Label("You Won!");
    winRoot.setCenter(winLabel);
    HBox hBoxWin = new HBox();
    hBoxWin.getChildren().addAll(mainMenuButton);
    winRoot.setTop(hBoxWin);
  }

  private void create2DGameRoot()
  {
    //2D Game Scene
    twoDGameRoot = new BorderPane();
    twoDGameRoot.setPrefSize(winW, winH);
  }

  private void createLoadRoot()
  {
    loadRoot = new BorderPane();
    loadRoot.setPrefSize(winW, winH);
    loadRoot.setCenter(new Label("Loading screen!"));
  }

  /**
   * Helper to relieve the amount of lines within Scene constructor.
   * Used to add change listeners to sliders.
   */
  private void addSliderProperties()
  {
    playerHearing.valueProperty().addListener((ov, old_val, new_val) ->
            Attributes.Player_Hearing = playerHearing.getValue());

    playerWalkingSpeed.valueProperty().addListener((ov, old_val, new_val) ->
            Attributes.Player_Walking_Speed = playerWalkingSpeed.getValue());

    playerSprintSpeed.valueProperty().addListener((ov, old_val, new_val) ->
            Attributes.Player_Sprint_Speed = playerSprintSpeed.getValue());

    playerStamina.valueProperty().addListener((ov, old_val, new_val) ->
            Attributes.Player_Stamina = playerStamina.getValue());

    playerRegen.valueProperty().addListener((ov, old_val, new_val) ->
            Attributes.Player_Regen = playerRegen.getValue());

    zombieSmell.valueProperty().addListener((ov, old_val, new_val) ->
            Attributes.Zombie_Smell = zombieSmell.getValue());

    maxZombies.valueProperty().addListener((ov, old_val, new_val) ->
    {
      minZombies.setMax(new_val.intValue());
      if (Attributes.Max_Zombies < Attributes.Min_Zombies)
      {
        minZombies.setValue(new_val.intValue());
        minZombies.setMajorTickUnit(1);
      }
      else minZombies.setMajorTickUnit(5);
      Attributes.Max_Zombies = maxZombies.valueProperty().getValue().intValue();
    });

    minZombies.valueProperty().addListener((observable, oldValue, newValue) ->
            Attributes.Min_Zombies = minZombies.valueProperty().getValue().intValue());

    mapWidth.valueProperty().addListener((ov, old_val, new_val) ->
            Attributes.Map_Width = (int) mapWidth.getValue());

    mapHeight.valueProperty().addListener((ov, old_val, new_val) ->
            Attributes.Map_Height = (int) mapHeight.getValue());

    rotateSensitivity.valueProperty().addListener((ov, old_val, new_val) ->
            Attributes.Player_Rotate_sensitivity = rotateSensitivity.getValue());
  }

  /**
   * Helper to relieve amount of lines within Scenes constructor.
   * Used to set the ticks of each slider.
   */
  private void setupSliders()
  {
    ArrayList<Slider> slidersList = new ArrayList<>();
    slidersList.add(playerHearing);
    slidersList.add(playerWalkingSpeed);
    slidersList.add(playerSprintSpeed);
    slidersList.add(playerStamina);
    slidersList.add(playerRegen);
    slidersList.add(zombieSmell);
    slidersList.add(maxZombies);
    slidersList.add(minZombies);
    slidersList.add(mapWidth);
    slidersList.add(mapHeight);
    slidersList.add(rotateSensitivity);

    for (Slider slider : slidersList)
    {
      slider.setShowTickMarks(true);
      slider.setShowTickLabels(true);
      slider.setPrefSize(SLIDER_WIDTH, SLIDER_HEIGHT);
      slider.setScaleY(1.5);
      slider.setSnapToTicks(true);
    }

    playerHearing.setMajorTickUnit(5);
    playerWalkingSpeed.setMajorTickUnit(1);
    playerSprintSpeed.setMajorTickUnit(1);
    playerStamina.setMajorTickUnit(2);
    playerRegen.setMajorTickUnit(1);
    zombieSmell.setMajorTickUnit(5);
    maxZombies.setMajorTickUnit(5);
    minZombies.setMajorTickUnit(5);
    mapWidth.setMajorTickUnit(20);
    mapHeight.setMajorTickUnit(20);
    rotateSensitivity.setMajorTickUnit(4);
  }

  /**
   * Create the HUD for the in-game screen
   *
   * @author Sarah Salmonson
   */
  private void createGameHUD()
  {
    playerHitPoints.setStyle("-fx-text-fill: white; -fx-font: 30px Tahoma;");
    playerStaminaPoints.setStyle("-fx-text-fill: white; -fx-font: 30px Tahoma;");
    BorderPane statsBox = new BorderPane();
    statsBox.setRight(playerStaminaPoints);
    statsBox.setLeft(playerHitPoints);
    statsBox.setMouseTransparent(true);//this prevents the mouse pane from being the mouse focus

    Image blood = new Image("Images/blood.png");
    bloodView.setImage(blood);
    bloodView.setFitWidth(winW);
    bloodView.setFitHeight(winH);
    bloodView.setOpacity(0.0);
    bloodView.setMouseTransparent(true);

    threeDGameRoot.setPadding(new Insets(10, 10, 10, 10));
    threeDGameRoot.getChildren().addAll(statsBox);
    statsBox.getChildren().add(bloodView);
  }

  /**
   * Plays a sound when you click buttons
   */
  private void playButtonSound()
  {
    soundManager.playSoundClip(Sound.BUTTON);
  }

  public void updateWinScreen()
  {
    this.winRoot.setCenter(new Label("You Win!"));
  }

  /**
   * @param soundManager Setter for the sound manager
   */
  public void setSoundManager(SoundManager soundManager)
  {
    this.soundManager = soundManager;
  }

  /**
   * Creates a new game board
   */
  private void createNewGameBoard(int difficulty)
  {
    zombieHouse = new ZombieHouse3d(difficulty, soundManager, main, this);
  }

  /**
   * Sets up a new game
   *
   * @author Sarah Salmonson
   */
  private void setUpNewGame(Stage gameStage, boolean continuingLevel)
  {
    try
    {
      main.assignStage(zombieHouse.initializeZombieHouse3d(this, continuingLevel));////param: boolean continuingLeve
      // Initialize stage
      gameStage.setTitle("Zombie House 3D");
      gameStage.setResizable(false);
      gameStage.setOnCloseRequest(event ->
      {
        if (zombieHouse.getEntityManager() != null)
        {
          zombieHouse.getEntityManager().player.gameIsRunning.set(false);
          zombieHouse.getEntityManager().gameIsRunning.set(false);
        }
      });
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    //add the in-game HUD
    createGameHUD();
  }

  /**
   * Update hitpoints display
   *
   * @param hitpoints
   * @author Sarah Salmonson
   */
  public void displayNewHP(int hitpoints)
  {
    playerHitPoints.setText("HP: " + hitpoints);
  }

  public void displayNewStamina(int stamina)
  {
    playerStaminaPoints.setText("Stamina: " + stamina);
  }

  public void displayBlood()
  {
    bloodView.opacityProperty().set(1);
    FadeTransition fading = new FadeTransition(Duration.millis(1000), bloodView);
    fading.setFromValue(1.0);
    fading.setToValue(0);
    fading.play();
  }
}
