package Lab1;
//*******************************************************
// Kevin Cox
//
// Class to build Graph based on the provided dictionary.
// Prints the path between two words.
//*******************************************************

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class Graph
{
  public HashMap<String, Node> dictionary;

  public Graph(File file)
  {
    this.dictionary = new HashMap<>();

    try
    {
      for (String str : Files.readAllLines(file.toPath()))
      {
        Node currentNode = new Node(str);
        setupAdjacency(currentNode);
        dictionary.put(str, currentNode);
      }
    } catch (IOException e)
    {
      System.out.println("FILE NOT FOUND: enter correct file name");
    }
  }

  /**
   * prints entire dictionary, used for debugging.
   */
  private void printDictionary()
  {
    for (String str : dictionary.keySet())
    {
      System.out.println(str);
    }
  }

  /**
   * iterates through incoming node to check if deleting, inserting, or
   * exchanging a letter in the word is available in dictionary. If it is, add
   * to nodes adjacency.
   * @param one node built from reading in dictionary
   */
  private void setupAdjacency(Node one)
  {
    String wordOne = one.getWord();
    StringBuilder oneLess;
    StringBuilder oneMore;
    StringBuilder equal;
    Node node;

    for (int i = 0; i < wordOne.length(); i++)
    {
      oneLess = new StringBuilder(wordOne).deleteCharAt(i);        //check for deleting
      node = dictionary.get(oneLess.toString());
      check(node, one);

      for (char alphabet = 'a'; alphabet <= 'z'; alphabet++)
      {
        oneMore = new StringBuilder(wordOne).insert(i, alphabet);  //check for inserting
        node = dictionary.get(oneMore.toString());
        check(node, one);

        equal = new StringBuilder(wordOne);
        equal.setCharAt(i, alphabet);                             //check for exchanging letters
        node = dictionary.get(equal.toString());
        check(node, one);
      }
    }
  }

  /**
   * Extracted method to check if dictionary contains the node's word
   * @param node iterated node
   * @param one node that is adding adjacencies
   */
  private void check(Node node, Node one)
  {
    if (node != null)
    {
      one.addAdjacency(one, node);
    }
  }

  /**
   * A* implemntation for words in given dictionary. Sorts through the adjacencies
   * of the word, computes the cost of the node within adjacency and adds to priority queue.
   * Word ladder is then reconstructed from the start word to goal word.
   * @param one start word
   * @param two goal word
   */
  public void aStar(String one, String two)
  {
    PriorityQueue<Node> frontier = new
            PriorityQueue<>(Comparator.comparingInt(node -> node.fCost));
    HashSet<Node> closed = new HashSet<>();
    Node start = dictionary.get(one);
    Node goal = dictionary.get(two);
    frontier.offer(start);
    closed.add(start);

    while (!frontier.isEmpty())
    {
      Node current = frontier.poll();
      closed.add(current);

      if (current.equals(goal))
      {
        reconstructPath(current);
        break;
      }

      for (Node node : current.getAdjcencies())
      {
        if (closed.contains(node)) continue;
        if (!frontier.contains(node))
        {
          node.gCost = current.gCost + 1;
          node.hCost = node.levenshteinDistance(node.getWord(), two);
          node.fCost = node.gCost + node.hCost;
          node.previous = current;
          frontier.offer(node);
          closed.add(node);
        }
      }
      if (frontier.isEmpty()) System.out.print("PATH NOT FOUND: " +
              one + " to " + two);
    }
    resetCameFrom(closed);
  }

  /**
   * reconstructs the word ladder by iterating through the given node's parents.
   * @param node goal node
   */
  public void reconstructPath(Node node)
  {
    LinkedList<Node> totalPath = new LinkedList<>();
    Node current = node;
    while (current != null)
    {
      totalPath.add(0, current);
      current = current.previous;
    }
    for (Node n : totalPath) System.out.print(n.getWord() + " ");
  }

  /**
   * Resets the parents for which we evaluated in Astar method.
   * @param set list of everything that was adjacent to word ladder path
   */
  private void resetCameFrom(HashSet<Node> set)
  {
    for (Node node : set) node.previous = null;
  }

}
