package levels;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * @author Atle Olson , Nick Schrandt
 * Player objects with position coordinates
 *
 * new: added more textures for the various regions.
 * 
 */
public class TextureMaps
{
  //public final static String BRICK_DIFFUSE_MAP = "File:Resources/Images/Textures/brick.png";
  private final static String GLOW_DIFFUSE_MAP = "File:Resources/Images/Textures/glowstone.png";
  private final static String IRON_DIFFUSE_MAP = "File:Resources/Images/Textures/iron_block.png";

  //private final static String BRICK_SPECULAR_MAP = "File:Resources/Images/Textures/brick_s.png";
  //private final static String BRICK_NORMAL_MAP = "File:Resources/Images/Textures/brick_n.png";
  private final static String IRON_SPECULAR_MAP = "File:Resources/Images/Textures/iron_block_s.png";
  private final static String IRON_NORMAL_MAP = "File:Resources/Images/Textures/iron_block_n.png";
  private final static String GLOW_SPECULAR_MAP = "File:Resources/Images/Textures/glowstone_s.png";
  private final static String GLOW_NORMAL_MAP = "File:Resources/Images/Textures/glowstone_n.png";

  //@Nick: All the newly added image files.
  private final static String STONE_FLOOR = "File:Resources/Images/Textures/stonebricks1.jpg";
  private final static String STONE_FLOOR_S = "File:Resources/Images/Textures/stonebricks1_s.jpg";
  private final static String STONE_FLOOR_N = "File:Resources/Images/Textures/stonebricks1_n.jpg";
  private final static String WOOD_CEILING = "File:Resources/Images/Textures/wood_0.png";
  private final static String PAVESTONE = "File:Resources/Images/Textures/pavestone.png";
  private final static String TILE = "File:Resources/Images/Textures/marble2.jpg";
  private final static String PARLOR_CEILING = "File:Resources/Images/Textures/ceiling.jpg";
  private final static String PURGOLA_CEILING = "File:Resources/Images/Textures/purgola.png";
  private final static String CARPET = "File:Resources/Images/Textures/fabric.jpg";
  private final static String HEDGE = "File:Resources/Images/Textures/shrub1.png";
  private final static String HEDGE_S = "File:Resources/Images/Textures/shrub1_s.png";
  private final static String HEDGE_N = "File:Resources/Images/Textures/shrub1_n.png";
  private final static String BOOKS = "File:Resources/Images/Textures/bookshelf.jpg";
  private final static String BOOKS_S = "File:Resources/Images/Textures/bookshelf_s.jpg";
  private final static String BOOKS_N = "File:Resources/Images/Textures/bookshelf_n.jpg";
  private final static String MARBLE = "File:Resources/Images/Textures/parlor_wall.jpg";
  private final static String MARBLE_S = "File:Resources/Images/Textures/parlor_wall_s.jpg";
  private final static String MARBLE_N = "File:Resources/Images/Textures/parlor_wall_n.jpg";

  private final static int IMAGE_HEIGHT = 128;
  private final static int IMAGE_WIDTH = 128;

  private static Image ironD = new Image(IRON_DIFFUSE_MAP, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image ironS = new Image(IRON_SPECULAR_MAP, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image ironN = new Image(IRON_NORMAL_MAP, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image glowD = new Image(GLOW_DIFFUSE_MAP, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image glowS = new Image(GLOW_SPECULAR_MAP, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image glowN = new Image(GLOW_NORMAL_MAP, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);

  //@Nick: Newly created Image objects
  private static Image stone_floor = new Image(STONE_FLOOR, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image stone_floor_s = new Image(STONE_FLOOR_S, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image stone_floor_n = new Image(STONE_FLOOR_N, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  //static Image wood_floor = new Image(WOOD_FLOOR, 128, 128, true, true, false);
  private static Image wood_ceiling = new Image(WOOD_CEILING, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image pavestone = new Image(PAVESTONE, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image tile = new Image(TILE, IMAGE_WIDTH, IMAGE_HEIGHT, true, true,
          false);
  private static Image parlor_ceiling = new Image(PARLOR_CEILING, IMAGE_WIDTH, IMAGE_HEIGHT,
          true, true, false);
  private static Image purgola_ceiling = new Image(PURGOLA_CEILING, IMAGE_WIDTH, IMAGE_HEIGHT,
          true, true, false);
  private static Image carpet = new Image(CARPET, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image hedge = new Image(HEDGE, IMAGE_WIDTH, IMAGE_HEIGHT, true, true,
          false);
  private static Image hedge_s = new Image(HEDGE_S, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image hedge_n = new Image(HEDGE_N, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image books = new Image(BOOKS, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image books_s = new Image(BOOKS_S, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image books_n = new Image(BOOKS_N, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image marble = new Image(MARBLE, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image marble_s = new Image(MARBLE_S, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);
  private static Image marble_n = new Image(MARBLE_N, IMAGE_WIDTH, IMAGE_HEIGHT, true,
          true, false);

  /**
   * Static initializers create a new tile with required phongmaterial properties
   * This replaces the single static tile of each type used previously to tile over the entire level,
   * which caused huge issues with lighting
   *
   * @return PhongMaterial tile for wall, ceiling or floor
   * @author Sarah Salmonson, Nick Schrandt
   */

  public static PhongMaterial createIron()
  {
    return new PhongMaterial(Color.WHITE, ironD, ironS, ironN, null);
  }

  public static PhongMaterial createGlow()
  {
    return new PhongMaterial(Color.WHITE, glowD, glowS, glowN, null);
  }

  public static PhongMaterial createStoneFloor() { return new PhongMaterial(Color.WHITE, stone_floor, stone_floor_s, stone_floor_n,
          null);}

  public static PhongMaterial createWoodCeiling() { return new PhongMaterial(Color.WHITE, wood_ceiling, null,
          null, null);}

  public static PhongMaterial createPavestone() { return new PhongMaterial(Color.WHITE, pavestone, null,
          null, null);}

  public static PhongMaterial createTile() { return new PhongMaterial(Color.WHITE, tile, null, null,
          null);}

  public static PhongMaterial createParlorCeiling() { return new PhongMaterial(Color.WHITE, parlor_ceiling,
          null, null, null);}

  public static PhongMaterial createPurgolaCeiling() { return new PhongMaterial(Color.WHITE, purgola_ceiling,
          null, null, null);}

  public static PhongMaterial createCarpet() { return new PhongMaterial(Color.WHITE, carpet, null,
          null, null);}

  public static PhongMaterial createHedge() { return new PhongMaterial(Color.WHITE, hedge, hedge_s, hedge_n,
          null);}

  public static PhongMaterial createBooks() { return new PhongMaterial(Color.WHITE, books, books_s, books_n,
          null);}

  public static PhongMaterial createMarble() { return new PhongMaterial(Color.WHITE, marble, marble_s, marble_n,
          null);}
}