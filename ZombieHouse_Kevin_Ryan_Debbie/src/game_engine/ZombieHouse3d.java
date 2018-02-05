package game_engine;

import com.interactivemesh.jfx.importer.obj.ObjImportOption;
import com.interactivemesh.jfx.importer.obj.ObjModelImporter;
import entities.*;
import graphing.GraphNode;
import graphing.TileGraph;
import gui.Main;
import javafx.animation.AnimationTimer;
import javafx.scene.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import levels.ProceduralMap;
import levels.TextureMaps;
import levels.Tile;
import levels.Tile.TileType;
import sound.SoundManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * @author Atle Olson
 * @author Jeffrey McCall
 * @author Sarah Salmonson, Hector Carrillo, Nick Schrandt
 *         This class will create a 3d representation of our game
 *         <p>
 *         new: Added textures for the walls, ceiling and floor in the different regions.
 */
public class ZombieHouse3d
{
  public int boardWidth;
  public int boardHeight;
  public int numRingBuffers;
  public int ringBufferCapacity = 38;
  public int[] ringBufferHead;
  public Tile[][] gameBoard;
  public Group root;
  public Scene scene;
  public ArrayList<Box> exits = new ArrayList<>();
  public Group[] zombieMeshRingBuffer;
  public int[] zombiesInRingBuffer;
  public HashMap<Integer, Group> ringBufferContainer;
  public volatile HashMap<Integer, ArrayList<Zombie>> zombieToRingBuffer;
  public AnimationTimer gameLoop;

  int difficulty;
  boolean isWall;

  private boolean firstGame = true;  // this is to prevent obstacle meshes from being randomized after first game
  private boolean paused = false;
  private int currentObstacleIndex = 0;
  private int maxObstacleIndex = 0;

  private GameCamera gameCamera;

  private PointLight light = new PointLight();
  private PointLight exitLight = new PointLight();
  private Box[][] ceilingAndWallDrawingBoard;
  private Box[][] floorDrawingBoard;
  private Node[][][] obstacleBoard;
  private List<Box> walls = new ArrayList<>();  // The list of walls used for collision detection and for location-based lighting
  private List<Box> notWalls = new ArrayList<>();  //@Sarah: list of all other tiles to be used for location-based lighting
  private List<Box> obstacles = new ArrayList<>();
  private ArrayList<Integer> obstacleList = new ArrayList<>();
  private ArrayList<Integer> orientation = new ArrayList<>();
  private EntityManager entityManager;
  private SoundManager soundManager;
  private Scenes scenes;
  private Main main;

  private static Random random = new Random();
  private static final ObjModelImporter importer = new ObjModelImporter();
  private static final int TOTAL_FRAMES = 11;
  private static final int TOTAL_SELF_FRAMES = 13;
  private static final int LARGEST_ZOMBIE_FRAME = 20;
  private static final int LARGEST_PAST_SELF_FRAME = 12;
  private static final int CAMERA_FAR_CLIP = 7;
  private static final int CAMERA_FIELD_OF_VIEW = 70;
  private static final int SUBSCENE_WIDTH = 1600;
  private static final int SUBSCENE_HEIGHT = 850;
  public static final String ZOMBIE = "Resources/Meshes/Zombie/zombie_0000";
  private final String MASTER = "Resources/Meshes/MasterZombie/zombie_0000";
  private static final String PAST_SELF = "Resources/Meshes/Past_Self/simpleMan";
  private static final String BONE_PILE = "Resources/Meshes/bones/bonePile.obj";
  private static final String GUILLOTINE = "Resources/Meshes/Guillotine/guillotine.obj";
  private static final String DUNGEON_PILLAR = "Resources/Meshes/DungeonPillar/dungeonPillar.obj";
  private static final String BOOK_PILE = "Resources/Meshes/Books/bookPile.obj";
  private static final String CHAIR = "Resources/Meshes/chair/chair.obj";
  private static final String HOROSCOPE_TABLE = "Resources/Meshes/HoroscopeTable/htable.obj";
  private static final String KNIGHT_2 = "Resources/Meshes/knight/knight2.obj";
  private static final String KNIGHT_1 = "Resources/Meshes/knight/knight1.obj";
  private static final String TOMBSTONE = "Resources/Meshes/tombstone/Tombstone.obj";
  private static final String[] OBSTACLE_STRINGS = {BONE_PILE, GUILLOTINE, DUNGEON_PILLAR, CHAIR, BOOK_PILE, HOROSCOPE_TABLE, KNIGHT_1, KNIGHT_2};
  private static final int[] R_1 = {0, 0, 1, 1, 1, 1, 1, 2, 2, 2};
  private static final int[] R_2 = {3, 3, 3, 3, 3, 4, 4, 4, 5, 5};
  private static final int[] R_3 = {6, 6, 6, 7, 7, 7, 5};
  private static final int[][] regions = {R_1, R_2, R_3};

  /**
   * Constructor for ZombieHouse3d object
   *
   * @param difficulty   The difficulty setting
   * @param soundManager Sound manager
   * @param main         Copy of Main
   * @param scenes       Scenes object
   */
  public ZombieHouse3d(int difficulty, SoundManager soundManager, Main main, Scenes scenes)
  {
    this.difficulty = difficulty;
    this.soundManager = soundManager;
    this.main = main;
    this.scenes = scenes;
  }

  /**
   * @param input The filepath to the mesh (.obj)
   * @return mesh
   * The Node[] that contains the model
   */
  public static Node[] loadMeshViews(String input)
  {
    importer.setOptions(ObjImportOption.NONE);
    importer.read(input);
    Node[] mesh = importer.getImport();
    for (Node aMesh : mesh)
    {
      aMesh.setTranslateY(1);
      aMesh.setScaleX(1);
      aMesh.setScaleY(1);
      aMesh.setScaleZ(1);
      aMesh.setCache(true);
      aMesh.setCacheHint(CacheHint.SPEED);
      aMesh.setRotationAxis(Rotate.Y_AXIS);
    }
    importer.clear();
    return mesh;
  }

   /**
   * @return group
   * the Group that is used by zombieHouse3d to initialize content
   */
  private Parent createContent(boolean continuingLevel) throws Exception
  {
    root = new Group();
    root.setCache(true);
    root.setCacheHint(CacheHint.SPEED);

    // initialize entity manager if not already initialized
    if (entityManager == null)
    {
      entityManager = new EntityManager(soundManager, main, scenes);
    }
    else
    {
      entityManager.resetEM();
    }
    entityManager.setZombieHouse3d(this);

    // Initialize camera - @Debbie: converted to 3rd person view
    gameCamera = new GameCamera();
    gameCamera.cameraGroup.getTransforms().addAll(
                                                  new Rotate(0, Rotate.Y_AXIS),//this 0 angle Y axis rotate seems necessary, or a slew of error messages will print
                                                  new Rotate(-8, Rotate.X_AXIS),
                                                  new Translate(0,  -.35, -3.0));

    gameCamera.camera.setFieldOfView(CAMERA_FIELD_OF_VIEW);
    gameCamera.camera.setFarClip(CAMERA_FAR_CLIP);
    gameCamera.camera.setRotationAxis(Rotate.Y_AXIS);

    // Initialize player
    //(x,z) were set by legacy code to (3,3) so that player and light not starting off stuck in wall
    entityManager.player = new Player(3, 0, 3, gameCamera, entityManager, light);

    //Initialize zombies
    entityManager.createZombies(gameBoard, boardHeight, boardWidth, continuingLevel);

    root.getChildren().add(entityManager.player.light);

    obstacleBoard = new Node[boardHeight][boardWidth][];
    // Build the Scene Graph
    //modified by Sarah Salmonson to "create" a new instance of phongmaterial for each wall, floor, or ceiling tile,
    //rather than using the same shared copy and repeating it, which was a huge problem when implementing lighting
    for (int col = 0; col < boardHeight; col++)
    {
      for (int row = 0; row < boardWidth; row++)
      {
        ceilingAndWallDrawingBoard[col][row] = new Box(1, 0, 1);
        floorDrawingBoard[col][row] = new Box(1, 0, 1);
        switch (gameBoard[col][row].type)
        {
          case wall:
            if (!gameBoard[col][row].isObstacle)
            {
              Tile currentTile = gameBoard[col][row];
              ceilingAndWallDrawingBoard[col][row] = new Box(1, 2, 1);
              if (currentTile.isBorder || currentTile.getRegion() == 1)
              {
                ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createStoneFloor());
              }
              if (!currentTile.isBorder && currentTile.getRegion() == 2)
              {
                ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createBooks());
              }
              if (!currentTile.isBorder && currentTile.getRegion() == 3)
              {
                ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createMarble());
              }
              if (!currentTile.isBorder && currentTile.getRegion() == 4)
              {
                ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createHedge());
              }
            }
            else //is obstacle
            {
              makeObstacle(gameBoard[col][row], ceilingAndWallDrawingBoard[col][row], floorDrawingBoard[col][row], col, row);
            }
            break;
          case region1:
            ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createStoneFloor());
            floorDrawingBoard[col][row].setMaterial(TextureMaps.createStoneFloor());
            break;
          case region2:
            ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createWoodCeiling());
            floorDrawingBoard[col][row].setMaterial(TextureMaps.createCarpet());
            break;
          case region3:
            ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createParlorCeiling());
            floorDrawingBoard[col][row].setMaterial(TextureMaps.createTile());
            break;
          case region4:
            ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createPurgolaCeiling());
            floorDrawingBoard[col][row].setMaterial(TextureMaps.createPavestone());
            break;
          case exit:
            ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createIron());
            floorDrawingBoard[col][row].setMaterial(TextureMaps.createIron());
            Box box = new Box(1, 2, 1);
            box.setTranslateX(gameBoard[col][row].zPos);
            box.setTranslateZ(gameBoard[col][row].xPos);
            box.setMaterial(TextureMaps.createGlow());
            exits.add(box);
            break;
        }
        if (col == 0 || col == boardHeight - 1 || row == 0 || row == boardWidth - 1)
        {
          ceilingAndWallDrawingBoard[col][row].setTranslateX(row + .5);
          ceilingAndWallDrawingBoard[col][row].setTranslateZ(col + .5);
          floorDrawingBoard[col][row].setTranslateX(row + .5);
          floorDrawingBoard[col][row].setTranslateZ(col + .5);
        }
        else
        {
          ceilingAndWallDrawingBoard[col][row].setTranslateX(gameBoard[col][row].xPos);
          ceilingAndWallDrawingBoard[col][row].setTranslateZ(gameBoard[col][row].zPos);
          floorDrawingBoard[col][row].setTranslateX(gameBoard[col][row].xPos);
          floorDrawingBoard[col][row].setTranslateZ(gameBoard[col][row].zPos);
        }
        if (!gameBoard[col][row].type.equals(TileType.wall))
        {
          ceilingAndWallDrawingBoard[col][row].setTranslateY(-1);
          floorDrawingBoard[col][row].setTranslateY(1);
        }
        root.getChildren().add(ceilingAndWallDrawingBoard[col][row]);
        root.getChildren().add(floorDrawingBoard[col][row]);
      }
    }
    // Spawn zombies on board and create list of wall tiles for
    // purposes of collision detection.
    for (int col = 0; col < boardHeight; col++)
    {
      for (int row = 0; row < boardWidth; row++)
      {
        if (gameBoard[col][row].getType().equals("wall") && !gameBoard[col][row].isObstacle)
        {
          walls.add(ceilingAndWallDrawingBoard[col][row]);
          entityManager.numTiles++;
          isWall = true;
        }
        //@Sarah: build tile collection of all other tile types
        else
        {
          notWalls.add(ceilingAndWallDrawingBoard[col][row]);
          notWalls.add(floorDrawingBoard[col][row]);
          isWall = false;
        }

        // The following code calls the appropriate methods to build the graph
        // to be used in zombie pathfinding.
        if (col == 0 && row == 0)
        {
          GraphNode newNode = new GraphNode(gameBoard[col + 1][row], gameBoard[col][row + 1],
                  gameBoard[col + 1][row + 1], row, col, isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (col == 0 && row == boardWidth - 1)
        {
          GraphNode newNode = new GraphNode(gameBoard[col + 1][row], gameBoard[col][row - 1],
                  gameBoard[col + 1][row - 1], row, col, isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (col == boardHeight - 1 && row == 0)
        {
          GraphNode newNode = new GraphNode(gameBoard[col - 1][row], gameBoard[col][row + 1],
                  gameBoard[col - 1][row + 1], row, col, isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (col == boardHeight - 1 && row == boardWidth - 1)
        {
          GraphNode newNode = new GraphNode(gameBoard[col - 1][row], gameBoard[col][row - 1],
                  gameBoard[col - 1][row - 1], row, col, isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (row == 0 && col != 0 && col != boardHeight - 1)
        {
          GraphNode newNode = new GraphNode(gameBoard[col + 1][row], gameBoard[col - 1][row],
                  gameBoard[col][row + 1], gameBoard[col + 1][row + 1], gameBoard[col - 1][row + 1],
                  row,col, isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (row == boardWidth - 1 && col != 0 && col != boardHeight - 1)
        {
          GraphNode newNode = new GraphNode(gameBoard[col + 1][row], gameBoard[col - 1][row],
                  gameBoard[col][row - 1], gameBoard[col + 1][row - 1], gameBoard[col - 1][row - 1],
                  row, col, isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (col == 0 && row != 0 && row != boardWidth - 1)
        {
          GraphNode newNode = new GraphNode(gameBoard[col + 1][row], gameBoard[col][row + 1],
                  gameBoard[col][row - 1], gameBoard[col + 1][row + 1], gameBoard[col + 1][row - 1],
                  row, col, isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (col == boardHeight - 1 && row != 0 && row != boardWidth - 1)
        {
          GraphNode newNode = new GraphNode(gameBoard[col][row + 1],
                  gameBoard[col - 1][row], gameBoard[col][row - 1],
                  gameBoard[col - 1][row + 1], gameBoard[col - 1][row - 1], row,
                  col, isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (col >= 1 && col < boardHeight - 1 && row >= 1
                && row < boardWidth - 1)
        {
          GraphNode newNode = new GraphNode(gameBoard[col + 1][row], gameBoard[col - 1][row],
                  gameBoard[col][row + 1], gameBoard[col][row - 1], gameBoard[col + 1][row + 1],
                  gameBoard[col + 1][row - 1], gameBoard[col - 1][row + 1], gameBoard[col - 1][row - 1],
                  row, col, isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
      }
    }
    firstGame = false;
    buildRingBuffer();
    associateZombiesWithBuffer();

    exitLight = new PointLight();
    exitLight.setTranslateX(exits.get(0).getTranslateX());
    exitLight.setTranslateZ(exits.get(0).getTranslateZ());
    root.getChildren().addAll(exits);
    //root.getChildren().add(exitLight);

    // Use a SubScene
    SubScene subScene = new SubScene(root, SUBSCENE_WIDTH, SUBSCENE_HEIGHT, true, SceneAntialiasing.BALANCED);
    //@author: Sarah set subScene color to BLACK to prevent the BLUE rectangles in the distance
    subScene.setFill(Color.BLACK);
    subScene.setCamera(gameCamera.camera);
    subScene.setCursor(Cursor.NONE);

    Group group = new Group();
    group.getChildren().add(subScene);
    group.addEventFilter(MouseEvent.MOUSE_MOVED, new MouseEventHandler(entityManager.player, this));

    /*
      Nick Schrandt

      Added this so that when the mouse is clicked, the eventhandler is called.
     */
    group.addEventFilter(MouseEvent.MOUSE_CLICKED, new MouseEventHandler(entityManager.player, this));
    return group;
  }

  /*
     Hector Carrillo

     creates an obstacle using a mesh and a collision box, can easily be made to work with different regions
  */
  private void makeObstacle(Tile gameBoardTile, Box floorBox, Box roofBox, int col, int row)
  {
    Box collisionBox = new Box(1, 2, 1);
    collisionBox.setVisible(false);
    int randomRotateModifier;

    Node[] obstacleMesh;
    if (firstGame)
    {
      randomRotateModifier = random.nextInt(4);
      orientation.add(randomRotateModifier);
    }
    else
    {
      randomRotateModifier = orientation.get(currentObstacleIndex);
    }

    int region = gameBoardTile.getRegion();

    // floor tiles above and below mesh
    switch (region)
    {
      case 1:
        floorBox.setMaterial(TextureMaps.createStoneFloor());
        roofBox.setMaterial(TextureMaps.createStoneFloor());
        break;
      case 2:
        floorBox.setMaterial(TextureMaps.createWoodCeiling());
        roofBox.setMaterial(TextureMaps.createCarpet());
        break;
      case 3:
        floorBox.setMaterial(TextureMaps.createParlorCeiling());
        roofBox.setMaterial(TextureMaps.createTile());
        break;
      case 4:
        floorBox.setMaterial(TextureMaps.createPurgolaCeiling());
        roofBox.setMaterial(TextureMaps.createPavestone());
        break;
    }

    if (firstGame)
    {
      if (region < 4)
      {
        int[] distribution = regions[region - 1];
        int dLength = distribution.length;
        int meshIndex = distribution[random.nextInt(dLength)];
        obstacleList.add(meshIndex);
        obstacleMesh = loadMeshViews(OBSTACLE_STRINGS[meshIndex]);
        ++maxObstacleIndex;
      }
      else obstacleMesh = loadMeshViews(TOMBSTONE);
    }
    else
    {
      if (region < 4)
      {
        int meshIndex = obstacleList.get(currentObstacleIndex);
        obstacleMesh = loadMeshViews(OBSTACLE_STRINGS[meshIndex]);
        currentObstacleIndex++;
      }
      else obstacleMesh = loadMeshViews(TOMBSTONE);
    }

    if (currentObstacleIndex >= maxObstacleIndex)
    {
      currentObstacleIndex = 0;
    }

    obstacleBoard[col][row] = obstacleMesh;
    root.getChildren().addAll(obstacleMesh);
    root.getChildren().add(collisionBox);

    for (Node anObstacleMesh : obstacleMesh)
    {
      anObstacleMesh.setTranslateZ(gameBoard[col][row].zPos);
      anObstacleMesh.setTranslateX(gameBoard[col][row].xPos);
      anObstacleMesh.setRotationAxis(Rotate.Y_AXIS);
      anObstacleMesh.setRotate(randomRotateModifier * 90);
    }

    collisionBox.setTranslateZ(gameBoard[col][row].zPos);
    collisionBox.setTranslateX(gameBoard[col][row].xPos);
    obstacles.add(collisionBox);

    floorBox.setTranslateY(-1);
    roofBox.setTranslateY(1);

    notWalls.add(floorBox);
    notWalls.add(roofBox);
  }

  /**
   * @param self version of PastCreature player that needs to be rendered.
   * @aurhor Hector Carrillo and Nick Schrandt
   * <p>
   * Nick wrote this method, but the logic is all adapted from Hector's meshZombie method.
   */
  private void makePastSelf(PastCreature self)
  {
    Node[] playerMesh;
    for (int currentFrame = 0; currentFrame <= LARGEST_PAST_SELF_FRAME; currentFrame++)
    {
      if (currentFrame < 10)
      {
        playerMesh = loadMeshViews(PAST_SELF + "0" + currentFrame + ".obj");
        for (Node aPlayerMesh : playerMesh)
        {
          aPlayerMesh.setVisible(false);
        }
        self.pastCreatureMeshes.getChildren().addAll(playerMesh);
        self.pastCreatureMeshes.setScaleX(.4);
        self.pastCreatureMeshes.setScaleY(.4);
        self.pastCreatureMeshes.setScaleZ(.4);
        self.pastCreatureMeshes.setTranslateY(-.5);
      }
      else
      {
        playerMesh = loadMeshViews(PAST_SELF + currentFrame + ".obj");

        for (Node aPlayerMesh : playerMesh)
        {
          aPlayerMesh.setVisible(false);
        }

        self.pastCreatureMeshes.getChildren().addAll(playerMesh);
      }
    }
    self.currentFrame = 1 + random.nextInt(TOTAL_SELF_FRAMES - 2);
    self.pastCreatureMeshes.getChildren().get(self.currentFrame).setVisible(true);
    self.pastCreatureMeshes.setRotationAxis(Rotate.Y_AXIS);
    root.getChildren().addAll(self.pastCreatureMeshes);
  }

  /**
   * @author Ryan vary
   * Method builds the mesh ring buffers
   */
  public void buildRingBuffer()
  {
    int numZombies = entityManager.zombies.size()-1;
    numRingBuffers = (numZombies/(ringBufferCapacity -1)) + 1;
    ringBufferHead = new int[numRingBuffers + 1];
    zombiesInRingBuffer = new int[numRingBuffers + 1];
    ringBufferContainer = new HashMap<>();
    zombieMeshRingBuffer = new Group[numRingBuffers+1];
    Node[] meshes;

    for(Zombie zombie : entityManager.zombies)
    {
      if(zombie.isMasterZombie)
      {
        zombie.getBoundingCylinder().setTranslateY(-.1);
        break;
      }
    }
    for(int j = 0; j <= numRingBuffers-1; j++)
    {
      if(j == numRingBuffers - 1)
      {
        ringBufferHead[j] = numZombies % (ringBufferCapacity - 1);
        zombiesInRingBuffer[j] = numZombies % (ringBufferCapacity - 1);
      }
      else
      {
        ringBufferHead[j] = ringBufferCapacity - 1;
        zombiesInRingBuffer[j] = ringBufferCapacity - 1;
      }
      zombieMeshRingBuffer[j] = new Group();
    }
    for(int j = 0; j < numRingBuffers; j++)
    {
      for(int k = 0; k < LARGEST_ZOMBIE_FRAME; k++)
      {
        if(k < 10)
        {
          meshes = loadMeshViews(ZOMBIE + "0" + k + ".obj");
        }
        else
        {
          meshes = loadMeshViews(ZOMBIE + k + ".obj");
        }
        zombieMeshRingBuffer[j].getChildren().addAll(meshes);
      }
      for(int k = 18; k >= 1; k--)
      {
        if(k >= 10)
        {
          meshes = loadMeshViews(ZOMBIE + k  + ".obj");
        }
        else
        {
          meshes = loadMeshViews(ZOMBIE + "0" + k + ".obj");
        }
        zombieMeshRingBuffer[j].getChildren().addAll(meshes);
      }
      for(Node node : zombieMeshRingBuffer[j].getChildren())
      {
        node.setVisible(false);
      }
      zombieMeshRingBuffer[j].setRotationAxis(Rotate.Y_AXIS);
      ringBufferContainer.put(j,zombieMeshRingBuffer[j]);
      root.getChildren().addAll(zombieMeshRingBuffer[j]);
    }
  }

  private void associateZombiesWithBuffer()
  {
    int cap = ringBufferCapacity - 1;
    int rem = (entityManager.zombies.size() - 1) % cap;
    int loop = cap;
    zombieToRingBuffer = new HashMap<>();
    Zombie zombie;
    for(int j = 0; j < numRingBuffers; j++)
    {
      ArrayList<Zombie> zombieList = new ArrayList<>(ringBufferCapacity - 1);
      if( j == numRingBuffers - 1) loop = rem;
      for(int k = 0; k < loop; k++)
      {
        zombie = entityManager.zombies.get((ringBufferCapacity-1)*j + k);
        zombie.currentFrame = k;
        zombie.currentMesh = ringBufferContainer.get(j).getChildren().get(zombie.currentFrame);
        zombie.currentMesh.setVisible(true);
        zombieList.add(k,zombie);
        root.getChildren().add(zombie.getHealthBar());
      }
      zombieToRingBuffer.put(j,zombieList);
    }
    for (PastCreature pastCreature : entityManager.pastCreatures)
    {
      pastCreature.pastCreatureMeshes.setVisible(true);
      makePastSelf(pastCreature);
    }
  }

  public void deathSequence(double x, double z)
  {
    int xPos, zPos;
    Random rand = new Random();
    Node[] obstacleMesh;
    if(x - Math.floor(x) > 0.5) xPos = (int)Math.ceil(x);
    else xPos = (int)Math.floor(x);
    if(z - Math.floor(z) > 0.5) zPos = (int)Math.ceil(z);
    else zPos = (int)Math.floor(z);
    obstacleMesh = loadMeshViews(OBSTACLE_STRINGS[0]);
    obstacleBoard[zPos][xPos] = obstacleMesh;
    root.getChildren().addAll(obstacleMesh);
    for (Node mesh : obstacleMesh)
    {
      mesh.setTranslateZ(gameBoard[zPos][xPos].zPos);
      mesh.setTranslateX(gameBoard[zPos][xPos].xPos);
      mesh.setRotationAxis(Rotate.Y_AXIS);
      mesh.setRotate(rand.nextInt(2)*45);
    }
  }

  /**
   * @author Hector Carrillo
   * this bit of code is so that the engine only renders objects close to the player to prevent lagging
   */
  private void gameDistanceBasedRendering(int distanceSquared)
  {
    // used for distance calculation
    double distanceFromPlayerSquared;
    double playerZ = entityManager.player.getBoundingCylinder().getTranslateZ();
    double playerX = entityManager.player.getBoundingCylinder().getTranslateX();
    double zombieZ;
    double zombieX;
    double pastSelfZ;
    double pastSelfX;

    root.getChildren().clear();

    // calculates distances to static objects if the distance squared to the player is less than 60 it is rendered
    for (int col = 0; col < boardHeight; col++)
    {
      for (int row = 0; row < boardWidth; row++)
      {
        distanceFromPlayerSquared = ((col - playerZ) * (col - playerZ)) + ((row - playerX) * (row - playerX));

        if (distanceFromPlayerSquared < distanceSquared)
        {
          root.getChildren().add(floorDrawingBoard[col][row]);
          root.getChildren().add(ceilingAndWallDrawingBoard[col][row]);
          if (obstacleBoard[col][row] != null)
          {
            root.getChildren().addAll(obstacleBoard[col][row]);
          }
        }
      }
    }

    // calculates distance to past self
    for (PastCreature pastCreature : entityManager.pastCreatures)
    {
      pastSelfZ = pastCreature.getBoundingCylinder().getTranslateZ();
      pastSelfX = pastCreature.getBoundingCylinder().getTranslateX();
      distanceFromPlayerSquared = ((pastSelfZ - playerZ) * (pastSelfZ - playerZ)) + ((pastSelfX - playerX) * (pastSelfX - playerX));
      if (distanceFromPlayerSquared < distanceSquared)
      {
        root.getChildren().addAll(pastCreature.pastCreatureMeshes);
      }
    }

    // exit distance calculation
    double exitX = exits.get(0).getTranslateX();
    double exitZ = exits.get(0).getTranslateZ();
    distanceFromPlayerSquared = ((exitZ - playerZ) * (exitZ - playerZ)) + ((exitX - playerX) * (exitX - playerX));
    if (distanceFromPlayerSquared < distanceSquared)
    {
      root.getChildren().addAll(exits);
    }
  }

  /**
   * The animation timer used in running the game.
   */
  private class MainGameLoop extends AnimationTimer
  {
    int timekeeper = 30;

    /**
     * Call the appropriate method to update the attributes of the
     * entities in the game.
     */
    public void handle(long now)
    {
      // only update every second (player range is so limited this might not hurt)
      ++timekeeper;
      if (timekeeper == 30)
      {
        gameDistanceBasedRendering(110);
        timekeeper = 0;
      }
      if (!paused && !entityManager.player.isDead.get())
      {
        entityManager.tick();
      }
      else
      {
        entityManager.player.tick();
      }
    }
  }

  /**
   * @param scenes The scenes into which all of the attributes of the game
   *               are being placed and rendered.
   * @return scene
   * Returns the scene that is our game
   */
  Scene initializeZombieHouse3d(Scenes scenes, boolean continuingLevel) throws Exception
  {
    //3D Game Scene... We're unable to do this in "Scenes" class due to the way legacy code is structured
    scenes.threeDGameRoot = new StackPane();
    scenes.threeDGameRoot.setPrefSize(scenes.winW, scenes.winH);
    scenes.threeDGameRoot.getChildren().clear();//@Sarah: clear stage's scene's nodes in case we're starting from a previous level
    scenes.threeDGameRoot.getChildren().add(createContent(continuingLevel));//call createContent on cleared scene
    scene = new Scene(scenes.threeDGameRoot);//adds root to scene
    scene.addEventHandler(KeyEvent.KEY_PRESSED, new KeyboardEventHandler(entityManager.player, this));
    scene.addEventHandler(KeyEvent.KEY_RELEASED, new KeyboardEventHandler(entityManager.player, this));
    gameLoop = new MainGameLoop();
    gameLoop.start();
    return scene;
  }

  /**
   * Generates gameBoard and builds 3D wall, ceiling, and floor objects
   */
  void build3DMap()
  {
    gameBoard = ProceduralMap.generateMap(Attributes.Map_Width, Attributes.Map_Height, difficulty);
    boardWidth = gameBoard[0].length;
    boardHeight = gameBoard.length;
    ceilingAndWallDrawingBoard = new Box[boardWidth][boardHeight];
    floorDrawingBoard = new Box[boardWidth][boardHeight];
  }

  /**
   * Delete game data after game has ended. Used when starting new game
   */
  public void dispose()
  {
    gameLoop.stop();
    entityManager = null;
    scene = null;
    gameCamera.camera = null;
    light = null;
    gameBoard = null;
    walls.clear();
    exits.clear();
    root.getChildren().clear();
  }

  /**
   * In the interests of beginning to encapsulate this messy legacy code, I set the camera to private and created
   * this getter so that other classes, like the EntityManager, can use player's field of view and far clip settings
   * to create lighting and other spooky effects
   *
   * @return PerspectiveCamera
   * @author: Sarah Salmonson
   */
  public PerspectiveCamera getCamera()
  {
    return gameCamera.camera;
  }

  /**
   * Getter for the ArrayList notWalls which contains all Tile objects that are not wall tiles
   *
   * @return List of all Tile objects that are not wall tiles
   * @author: Sarah Salmonson
   */
  public List<Box> getNotWalls()
  {
    return notWalls;
  }

  /**
   * Getter for the ArrayList walls which contains all Tile objects that are wall tiles
   *
   * @return List of all Tile objects that are  wall tiles
   */
  public List<Box> getWalls()
  {
    return walls;
  }

  /**
   * Getter for the ArrayList obstacles which contains all Tile objects that are obstacles
   *
   * @return List of all Tile objects that are obstacles
   */
  public List<Box> getObstacles()
  {
    return obstacles;
  }

  /**
   * Getter for the game's EntityManager
   *
   * @return EntityManager
   * @author Sarah Salmonson
   */
  EntityManager getEntityManager()
  {
    return entityManager;
  }
}