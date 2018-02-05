package cs351;

/*
 * Created by Kevin Cox on 2/3/17.
 * Custom thread, runs simulation on cell growth. Uses Life's
 * CyclicBarrier.
 */

import javafx.beans.property.IntegerProperty;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class BoardThread extends Thread
{
  int startRow;
  private CyclicBarrier barrier;
  private Board oldBoard;
  private Board newBoard;
  int endRow;

  BoardThread(int start, int end, Board oldBoard, Board newBoard, CyclicBarrier barrier)
  {
    this.startRow = start;
    this.endRow = end;
    this.barrier = barrier;
    this.oldBoard = oldBoard;
    this.newBoard = newBoard;
  }

  /**
   * Auto generated method from thread extension. Advances generation of cells
   * until the CyclicBarrier has been approached by thread.
   */
  @Override
  public void run()
  {
    while(!Controller.paused)
    {
      advanceGeneration();
      try
      {
        barrier.await();
        sleep(Controller.frames);
      } catch (InterruptedException | BrokenBarrierException e)
      {
        return;
      }
    }
  }

  /**
   * Advances generation of cells by one generation. Updates the new board based
   * on old board values. If the GUI/Thread has been paused then the GUI will only
   * advance by one, else it'll be continuous throughout run method.
   */
  void advanceGeneration()
  {
    int neighborCount;
    for (int row = startRow; row < endRow; row++)
    {
      for (int col = 0; col < Main.SIZE; col++)
      {
        neighborCount = oldBoard.getNeighborCount(row, col);

        if (oldBoard.board[row][col] != 0 && (neighborCount == 2 || neighborCount == 3))
        {
          if (oldBoard.board[row][col] <= 10) newBoard.board[row][col] = (byte) (oldBoard.board[row][col]+1);
        }
        else if (oldBoard.board[row][col] == 0 && neighborCount == 3) newBoard.board[row][col] = 1;
        else newBoard.board[row][col] = 0;
      }
    }
    if (!this.isAlive() || this.isInterrupted())
    {
      byte[][] temp = oldBoard.board;
      oldBoard.board = newBoard.board;
      newBoard.board = temp;
    }
  }

}
