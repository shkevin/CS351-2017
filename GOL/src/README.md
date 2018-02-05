Creator: Kevin Cox
------------------

----------------
Current Version
----------------
* Allows smooth operation with all threads selected, but limited in size of cells within
Game of life.
* Maximum size: either set as 750X750 in Main "SIZE/CELLSIZE" or max resolution size.

---------------------------
Usage:
---------------------------
Location of Main: GOL/src/cs351
* User must select from Preset and Number of Threads drop down. Board is defaulted at Random preset
and one thread.
* When done, press Load button, then Start.
* If user wishes to pause the current generation of cells, simply press pause. To unpause, press 
pause button again.
* Reset handles clearing the entire board, and resetting the state with the previous preset and number 
of threads.
* Next button advances the current generation by one. Next button is enabled during normal run, but
will cause faster generations.
* Press start when choices are to user's liking.

---------------------------
Features Within Program:
---------------------------
* Resizing
* Partial Zooming, allows for 1-50 pixel size
* Age of Cells
* Scrolling/Panning
* Randomized startup, and paused
* Cell toggling during pause
* Efficient use of all threads
* Smooth operation
* All GUI controls
* Dynamic Grid Lines

---------------------------
Features Not Within Program:
---------------------------
* Zooming on cursor location
* Does not implement 10000x10000

---------------------------
Known Bugs:
---------------------------
* Sometimes canvas will stop working. Nothing in API/stack overflow helps with this.
* ArrayIndexOutOfBounds exception, only has happened once. Located in Canvas Class.