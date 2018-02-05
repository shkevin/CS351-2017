package Lab1;
//***********************************************************
// Kevin Cox
//
// Main program: provides error checking for graph structure
//***********************************************************

import java.io.File;

public class Main
{

  public static void main(String[] args)
  {

    if (args.length < 2)
    {
      System.out.println("NOT ENOUGH ARGUMENTS");
      System.exit(0);
    }
    String fileName = args[0];
    File file = new File(fileName);
    Graph graph = new Graph(file);
    String start;
    String goal;

    for (int i = 1; i < args.length-1; i += 2)  //handles only pairs of words, ignores odd number
    {
      start = args[i].toLowerCase();
      goal = args[i + 1].toLowerCase();
      if (!checkArgs(start, goal, graph)) continue;
      graph.aStar(start, goal);
      System.out.println();
    }
  }

  /**
   * Checks the arguments given: Builds string based on whether
   * both given words are in dictionary.
   * @param start first argument to compare.
   * @param goal second argument.
   * @param graph graph implementation of dictionary.
   * @return true/false depending on valid dictionary words.
   */
  private static boolean checkArgs(String start, String goal, Graph graph)
  {
    StringBuilder string = new StringBuilder();
    String template = "NOT IN DICTIONARY:";
    if (graph.dictionary.get(start) == null) string.append(start + " ");
    if (graph.dictionary.get(goal) == null)
    {
      if (string.length() > 0) string.append("-> " + goal);
      else string.append(goal);
    }
    if (string.length() > 0)
    {
      System.out.println(template + " " + string);
      return false;
    }
    return true;
  }
}
