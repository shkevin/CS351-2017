package cs351;

/*
 * Created by Kevin Cox on 2/3/17.
 * Board object, stores all necessary information pertaining to
 * the "cells" for Game of Life.
 */

import java.util.Random;

class Board
{
  private int size;                       // size of board NxN;
  byte[][] board;
  private Random rand = new Random();

  Board()
  {
    this.size = Main.SIZE;
    this.board = new byte[size][size];
    initializeRandom();
  }

  /**
   * tie fighter explosion, used in presets for controller.
   */
  void initializeSomethingCool()
  {
    board[18][18] = 1;
    board[20][18] = 1;
    board[22][18] = 1;
    board[18][19] = 1;
    board[22][19] = 1;
    board[18][20] = 1;
    board[22][20] = 1;
    board[18][21] = 1;
    board[22][21] = 1;
    board[18][22] = 1;
    board[20][22] = 1;
    board[22][21] = 1;
    board[22][22] = 1;
  }

  /**
   * Creates Upper left checkerboard for presets. Used in controller.
   */
  void initializeCheckerBoard()
  {
    for (int i = 1; i < Main.SIZE-1; i++)
    {
      for (int j = i; j < Main.SIZE-1; j++)
      {
        if ((i+j) % 2 == 0)
        {
          board[j][i] = 1;
          if ((i+j) % 9 == 0)
          {
            board[j][i] = 0;
            j+=1;
          }
        }
        else board[j][i] = 0;
      }
    }
  }

  /**
   * random board generation. Used in presets for controller.
   */
  void initializeRandom()
  {
    for (int i = 0; i < size; i++)
    {
      for (int j = 0; j < size; j++)
      {
        if (rand.nextInt() <= .5) board[i][j] = 1;
      }
    }
  }

  /**
   * Upper left Gosper gun, shoots towards lower right.
   * Used in presets for controller.
   */
  void initializeGosper()
  {
    board[1][5] = 1;
    board[2][5] = 1;
    board[1][6] = 1;
    board[2][6] = 1;

    board[11][5] = 1;
    board[11][6] = 1;
    board[11][7] = 1;
    board[12][4] = 1;
    board[12][8] = 1;
    board[13][3] = 1;
    board[14][3] = 1;
    board[13][9] = 1;
    board[14][9] = 1;
    board[15][6] = 1;
    board[16][4] = 1;
    board[16][8] = 1;
    board[17][5] = 1;
    board[17][6] = 1;
    board[17][7] = 1;
    board[18][6] = 1;

    board[21][3] = 1;
    board[21][4] = 1;
    board[21][5] = 1;
    board[22][3] = 1;
    board[22][4] = 1;
    board[22][5] = 1;
    board[23][2] = 1;
    board[23][6] = 1;
    board[25][1] = 1;
    board[25][2] = 1;
    board[25][6] = 1;
    board[25][7] = 1;

    board[35][4] = 1;
    board[36][4] = 1;
    board[35][5] = 1;
    board[36][5] = 1;
  }

  /**
   * initializes all but edges to alive. Used in presets for controller.
   */
  void initializeAllAlive()
  {
    for (int i = 1; i < size - 1; i++)
    {
      for (int j = 1; j < size - 1; j++)
      {
        board[i][j] = 1;
      }
    }
  }

  /**
   * Prints entire board. A if alive, x if dead. Used for debugging.
   */
  void boardToString()
  {
    for (int i = 0; i < size; i++)
    {
      for (int j = 0; j < size; j++)
      {
        if (board[i][j] != 0) System.out.print(" A ");
        else System.out.print(" x ");
      }
      System.out.println("\n");
    }
  }

  /**
   * prints the neighbors of the specified indices.
   * Used for debugging.
   */
  void printNeighborCount()
  {
    for (int i = 0; i < board.length; i++)
    {
      for (int j = 0; j < board.length; j++)
      {
        System.out.print(getNeighborCount(i, j));
      }
      System.out.print("\n");
    }
  }

  /**
   * initializes everything to dead. Used in presets for controller.
   */
  void initializeAllDead()
  {
    board = new byte[Main.SIZE][Main.SIZE];
  }

  /**
   * toggles the cell to dead or alive. Used for selecting cells in GUI.
   * @param x row
   * @param y col
   */
  void toggleLife(int x, int y)
  {
    if (board[x][y] != 0) board[x][y] = 0;
    else board[x][y] = 1;
  }

  /**
   * gets the neighbor count at specified indices.
   * @param row index
   * @param col index
   * @return neighbor count of cell.
   */
  int getNeighborCount(int row, int col)
  {
    int count = 0;
    for (int i = row - 1; i <= row + 1; i++)
    {
      if (i >= 0 && i < board.length)
      {
        for (int j = col - 1; j <= col + 1; j++)
        {
          if (j >= 0 && j < board[i].length)
          {
            if (i != row || j != col)
            {
              if (board[i][j] != 0) count++;
            }
          }
        }
      }
    }
    return count;
  }

}
