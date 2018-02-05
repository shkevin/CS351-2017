package Lab1;
//*************************************
// Kevin Cox
//
// Used to create Node for Graph setup
//*************************************

import java.util.HashSet;

public class Node
{
  Node previous;
  int gCost;
  int hCost;
  int fCost;
  private String word;
  private HashSet<Node> adjcencies;

  /**
   * constructor to initialize new node, used in Graph implementation
   * @param word String for Node object
   */
  public Node(String word)
  {
    adjcencies = new HashSet<>();
    Node previous = null;   //reference to previous node in word ladder
    this.word = word;
    this.gCost = 0;         // cost from the start node to current node
    this.hCost = 0;         // estimated movement cost from start to goal (levenshtein)
    this.fCost = 0;         // total cost G+H
  }

  /**
   * adds a bidirectional adjacency to given nodes
   * @param one first node
   * @param two second node
   */
  public void addAdjacency(Node one, Node two)
  {
    if (!one.equals(two))
    {
      one.adjcencies.add(two);
      two.adjcencies.add(one);
    }
  }

  /**
   * Checks the distance between two given words, essentially acts as manhattan distance
   * for words.
   *
   * from: https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
   * @param lhs first word which is compared
   * @param rhs second word
   * @return difference of the two words
   */
  public int levenshteinDistance(CharSequence lhs, CharSequence rhs)
  {

    int len0 = lhs.length() + 1;
    int len1 = rhs.length() + 1;

    // the array of distances
    int[] cost = new int[len0];
    int[] newCost = new int[len0];

    // initial cost of skipping prefix in String s0
    for (int i = 0; i < len0; i++) cost[i] = i;

    // dynamically computing the array of distances

    // transformation cost for each letter in s1
    for (int j = 1; j < len1; j++)
    {
      // initial cost of skipping prefix in String s1
      newCost[0] = j;

      // transformation cost for each letter in s0
      for (int i = 1; i < len0; i++)
      {
        // matching current letters in both strings
        int match = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;

        // computing cost for each transformation
        int cost_replace = cost[i - 1] + match;
        int cost_insert = cost[i] + 1;
        int cost_delete = newCost[i - 1] + 1;

        // keep minimum cost
        newCost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
      }

      // swap cost/newCost arrays
      int[] swap = cost;
      cost = newCost;
      newCost = swap;
    }

    // the distance is the cost for transforming all letters in both strings
    return cost[len0 - 1];
  }

  /**
   * simple getter for adjecency in node
   * @return adjacencies
   */
  HashSet<Node> getAdjcencies()
  {
    return adjcencies;
  }

  /**
   * simple getter for the string contained in the node
   * @return node's string
   */
  String getWord()
  {
    return word;
  }
}