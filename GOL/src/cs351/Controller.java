package cs351;

/*
 * Created by Kevin Cox on 2/3/17.
 * Main controller for Game of Life application, handles all events
 * and links model and view.
 */

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

public class Controller
{
  @FXML ComboBox<Integer> threadCombo = new ComboBox<>();
  @FXML ComboBox<String> presetCombo = new ComboBox<>();
  @FXML BorderPane pane = new BorderPane();
  @FXML ScrollBar horizontalScrollBar;
  @FXML ScrollBar verticalScrollBar;
  @FXML private Button pausedButton;
  @FXML private Button nextButton;
  @FXML private Button startButton;
  @FXML private Button resetButton;
  @FXML private Button loadButton;
  @FXML private Slider slider = new Slider();

  static int frames = 300;
  private Pane canvasPane = new Pane();
  private Board board = new Board();
  private CanvasClass canvas = new CanvasClass(board, this);
  private Handler handler = new Handler(canvas, this);
  static volatile boolean paused = true;
  private Life life;
  private int storedPreset = 1;

  double yScrollValue = 0;
  double xScrollValue = 0;
  double pixelX = 0;
  double pixelY = 0;

  private ObservableList<Integer> threadsList = FXCollections.observableArrayList(1, 2, 3, 4, 5,
          6, 7, 8);
  private ObservableList<String> presetList = FXCollections.observableArrayList("1: All Dead", "2: Random",
          "3: Glider Gun", "4: All Alive but Edges", "5: UL Checkerboard", "6: Something Cool");

  /**
   * Gets current selection of threads and preset, updates game board
   * accordingly and disables/enables start and pause for GUI.
   * @param event Button "load" has been pressed.
   */
  @FXML
  private void load(ActionEvent event)
  {
    if (!threadCombo.getSelectionModel().isEmpty() && !presetCombo.getSelectionModel().isEmpty())
    {
      paused = true;
      int numThreads = threadCombo.getSelectionModel().getSelectedIndex()+1;
      board.board = new byte[Main.SIZE][Main.SIZE];
      handlePreset();
      storedPreset = presetCombo.getSelectionModel().getSelectedIndex();
      life = new Life(board,numThreads, canvas);
      canvas.updateGUI();
      pausedButton.setText("pause");
      pausedButton.setDisable(true);
      startButton.setDisable(false);
    }
  }

  @FXML
  private void setupFrames()
  {
    slider.valueProperty().addListener((observable, oldValue, newValue) -> frames = newValue.intValue());
    System.out.println(frames);
  }

  /**
   * updates board accordingly based off of selected preset.
   * Handled on load button event.
   */
  private void handlePreset()
  {
    switch (presetCombo.getSelectionModel().getSelectedIndex())
    {
      case 0: board.initializeAllDead(); break;
      case 1: board.initializeRandom(); break;
      case 2: board.initializeGosper(); break;
      case 3: board.initializeAllAlive(); break;
      case 4: board.initializeCheckerBoard(); break;
      case 5: board.initializeSomethingCool(); break;
    }
  }

  /**
   * resets GUI/game board. Retains previously selected preset and number
   * of threads the user selected.
   */
  @FXML
  private void reset()
  {
    System.out.println("resetting...");
    presetCombo.getSelectionModel().select(storedPreset);
    board.board = new byte[Main.SIZE][Main.SIZE];
    handlePreset();
    canvas.updateGUI();
    startButton.setDisable(false);
    paused = true;
    pausedButton.setText("pause");
    System.out.println("resetting");
  }

  /**
   * Starts the animation for the GUI/thread workers.
   * Updates applicable buttons accordingly.
   */
  @FXML
  private void start()
  {
    System.out.println("starting...");
    paused = false;
    pausedButton.setDisable(false);
    startButton.setDisable(true);
    life.simulateLife();
  }

  /**
   * Updates the GUI by one generation of cells.
   */
  @FXML
  private void next()
  {
    System.out.println("Next...");
    life.advanceOneGeneration();
    canvas.updateGUI();
  }

  /**
   * Pauses or unpauses current process in GUI/threads.
   */
  @FXML
  private void togglePause()
  {
    System.out.println("Pause...");
    paused = !paused;
    toggleButtons();
    startButton.setDisable(true);
    if (!paused) life.unPauseLife();
  }

  /**
   * toggles pause button Text
   */
  private void toggleButtons()
  {
    if (paused) pausedButton.setText("unpause");
    else pausedButton.setText("pause");
  }

  /**
   * initial setup for the Controller. Handles all GUI components, and sets
   * up initial Life object. The game starts with 1 thread, and random board.
   */
  @FXML
  public void initialize()
  {
    setupCombos();
    paused = false;
    setupCanvas();
    setUPScroll();
    life = new Life(board, 1, canvas);
    pausedButton.setDisable(true);
  }

  /**
   * helper to setup items within Preset and Threads choices.
   */
  private void setupCombos()
  {
    threadCombo.setItems(threadsList);
    presetCombo.setItems(presetList);
    presetCombo.getSelectionModel().select(1);
    threadCombo.getSelectionModel().select(0);
  }

  /**
   * Sets up initial canvas and initializes event handles that is/are
   * contained in "Handler" class. Adds cell toggling within GUI.
   */
  private void setupCanvas()
  {
    canvas.widthProperty().bind(canvasPane.widthProperty());
    canvas.heightProperty().bind(canvasPane.heightProperty());
    canvasPane.addEventFilter(ScrollEvent.ANY, handler.getOnScrollEventHandler());
    canvasPane.addEventFilter(MouseEvent.ANY, handler.getOnMouseEventHandler());
    canvasPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event ->
    {
     if (paused)
     {
       int cellX = (int) (pixelX/canvas.getScale());
       int cellY = (int)(pixelY/canvas.getScale());

       if (cellX >= Main.SIZE) cellX = Main.SIZE-1;
       if (cellY >= Main.SIZE) cellY = Main.SIZE-1;
       board.toggleLife(cellX, cellY);
       canvas.updateGUI();
     }
    });
    canvasPane.getChildren().add(canvas);
    pane.setCenter(canvasPane);
  }

  /**
   * Sets up scroll bars within parent Pane. Updates values accordingly.
   */
  private void setUPScroll()
  {
    horizontalScrollBar.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      horizontalScrollBar.setValue(newValue.doubleValue());
      yScrollValue = newValue.doubleValue();
      canvas.updateGUI();
    });

    verticalScrollBar.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      verticalScrollBar.setValue(newValue.doubleValue());
      xScrollValue = newValue.doubleValue();
      canvas.updateGUI();
    });
  }

  /**
   * Simple zoom in function. Focuses top left.
   */
  @FXML
  private void zoomIn()
  {
    if (canvas.getScale() > 50) return;
    canvas.setScale(canvas.getScale() + 1);
    canvas.updateGUI();
  }

  /**
   * Simple zoom out function. Focuses top left.
   */
  @FXML
  private void ZoomOut()
  {
    if (canvas.getScale() <= 1) return;
    if (Main.SIZE * canvas.getScale() <= Main.SIZE * Main.CELLSIZE) return;
    canvas.setScale(canvas.getScale() - 1);
    canvas.updateGUI();
  }

}
