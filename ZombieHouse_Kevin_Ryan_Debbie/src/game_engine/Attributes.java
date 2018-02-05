package game_engine;

/**
 * @author Ben Matthews
 *
 *         This class contains all of the attributes for the game
 */
public class Attributes
{
  public static double Frame_Rate = 60; // frames per second

  // Player
  public static double Player_Hearing = 20;
  static double Player_Walking_Speed = .1;
  static double Player_Sprint_Speed = .16;
  static double Player_Stamina = 5;
  static double Player_Regen = .2; // regen of stamina per second
  public static double Player_Rotate_sensitivity = 5; 

  // Zombie
  static double Zombie_Smell = 15;
  public static double Max_Zombies = 30;
  public static double Min_Zombies = 15;
  
  // Map
  public static int Map_Width = 50;
  public static int Map_Height = 50;
}
