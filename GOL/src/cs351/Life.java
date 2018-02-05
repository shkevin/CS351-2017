package cs351;

/*
 * Created by Kevin Cox on 2/10/2017.
 * Simple class to provide a way to simulate the cells lifecycle.
 */

import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;

class Life
{
  private Board newBoard;
  private Board oldBoard;
  private CyclicBarrier barrier;
  private int numThreads;
  private ArrayList<BoardThread> boardThreads;
  private CanvasClass canvas;

  Life(Board oldBoard, int threads, CanvasClass canvas)
  {
    this.canvas = canvas;
    this.numThreads = threads;
    boardThreads = new ArrayList<>();
    this.barrier = new CyclicBarrier(threads, this::onBarrierBroken);
    this.oldBoard = oldBoard;
    this.newBoard = new Board();
    initializeThreads();
    canvas.updateGUI();
  }

  /**
   * partitions the row/col sections for threads. Ensures even work
   * for each thread.
   */
  private void initializeThreads()
  {
    int start = 0;
    int end = 0;
    int oddPartition = Main.SIZE % numThreads;
    for (int i = 1; i <= numThreads; i++)
    {
      if (oddPartition == 1)
      {
        end = Main.SIZE / numThreads + end;
//        System.out.println("start: " + start + " end: " + end);
        BoardThread thread = new BoardThread(start, end, oldBoard,
                newBoard, barrier);
        boardThreads.add(thread);
        start = end;
      }
      //if it's an even partition
      else
      {
        end = Main.SIZE / numThreads + end;
        BoardThread thread = new BoardThread(start, end, oldBoard,
                newBoard, barrier);
        boardThreads.add(thread);
        start = end;
      }
    }
    int newEnd = Main.SIZE - end;
    if (newEnd != 0)
    {
      boardThreads.get(numThreads - 1).endRow += newEnd;     //update odd partition end
    }
  }

  /**
   * Starts up threads, or unpauses them based on status of GUI.
   */
  void simulateLife()
  {
    unPauseLife();
  }

  /**
   * unpauses animation and threads for GUI.
   */
  void unPauseLife()
  {
    for (BoardThread thread : boardThreads)
    {
      if (thread != null)
      {
        thread = new BoardThread(thread.startRow, thread.endRow, oldBoard, newBoard, barrier);
        thread.start();
      }
    }
  }

  /**
   * advances the generation of the current board state by one.
   */
  void advanceOneGeneration()
  {
    for (BoardThread thread : boardThreads)
    {
      thread.advanceGeneration();
    }
  }

  /**
   * When each thread has finished with their row section, update GUI
   * and swap double buffer.
   */
  private void onBarrierBroken()
  {
    if (!Controller.paused)
    {
      byte[][] temp = oldBoard.board;
      oldBoard.board = newBoard.board;
      newBoard.board = temp;
      canvas.updateGUI();
    }
  }


}
