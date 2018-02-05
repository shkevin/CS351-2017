# Word-Star
Created By: Kevin Cox

----------------------
Command Line Arguments
----------------------
Command line takes in specified arguments:
Dictionary name, followed by pairs of words.

Provided error checking of arguments: Can check dictionary of true/false words.
Adjusts argument length to only grab pairs, if odd number of words are given, the 
program simply ignores.

-------------------------
Design Decisions & Issues
-------------------------
Implemented an algorithm to iterate through word, rather than forming 
buckets and iterating through those for adjacency's. With the file provided
(179,000) the program operates under 20 seconds.

Issues: initial design pattern included the above, but rather than a Hashmap
an ArrayList<ArrayList<Node> were used with each Node containing three buckets
for adjacency. This proved to be very slow.

There is an obvious negative to my implementation, The Hashmap stores the entire
dictionary of 100,000+ words as entries of Strings to Node objects. But since the 
program exceeded time allotments, I stuck with the design.

-----------------------
Word Frequency Analysis
-----------------------
Once the graph is built from the provided dictionary, the A* algorithm is ran with 
pairs of words. The algorithm iterates through adjacencies of nodes similar to the start,
and ultimately chooses a path of adjacencies to the goal word. 

--------------------
Algorithmic Analysis
--------------------
In older design implementations the program ran at n^2 or slower, but with iterating through
words and hashing them, the algorithm performs at n.
