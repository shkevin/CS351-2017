# Read Me
Creators: Kevin Cox, Ryan Vary, Debbie Berlin
---

## Introduction
This Zombie House game is based on a significant portion of code written by Atle Olson, Jeffrey McCall, and Ben Matthews
in Spring 2016 at UNM. The team of Nick Schrandt, Hector Carillo, and Sarah Salmonson enhanced the existing code and 
features, resolved existing bugs, and implemented new features to modify game logic and behaviors in Fall 2016 at UNM.
In the Spring of 2017, the team of Kevin Cox, Ryan Vary, and Debbie Berlin updated the code to meet current 
specifications, improved code structure and readability, sound quality, graphical elements, layout and styling of 
game screens, game mechanics, and addressed existing bugs.

## How to Use the Program:
Entry Point: Main.java
* Launches a window with buttons:
  * Play Zombie House 3d: Launches the main 3D implementation of the game
  * (debug feature) Map Viewer Screen: Takes you to the procedurally generated map layout for each zombie house.
  * (debug feature) Game Over: Takes you to the game over scene
  * (debug feature) Win Screen: Takes you to the game win scene  
  * Settings: Takes you to a settings screen with several modifiable values on sliders with reasonable values (note Map 
    Width and Map Height can go up to 100 to demonstrate A* and procedural generation with the 2d game)
    
* Controls:
  * W, A, S, D: Player Movement
  * Mouse on screen: rotates camera
  * Left and Right arrow keys: rotates camera
  * Space: push back zombie
  * Shift: makes player run
  * Left-click: Attack with sword
  * F5 key: decrements player hitpoints by 100 (cheat code)
  * F6 key: increments player hitpoints by 200 (cheat code)
  * F: toggle player lighting
  * ESC: Exit game

---

## Program Architecture:
##### general architecture:
* Main - instantiates a soundManager and Scenes
  * SoundManager - handles game sounds
  * Scenes - creates the different states of the game (menu, game, pause, etc...)

##### Game Architecture:
* 3D Renderer - creates and renders game objects
  * EntityManager - controls and instantiates entities
  * MouseHandler - controls mouse input
  * KeyboardHandler - controls the keyboard input

##### Entity Architecture:
* Entity - abstract entity
  * Creature - abstract moving entity
    * Player - the player object
    * Zombie - a zombie object
    * PastCreature - a ghost player
    * RecordableCreature - parent class of Player and PastSelft that maintains a log
    of Creature movements that can be recorded to (for current Player) and/or played back (for PastCreature)
    
##### Mesh Memory Architecture
* Ring buffer with a capacity of 38 meshes was implemented with the provided zombie mesh files. The ring buffer can
hold a maximum of n = 37 zombies, with one head and tail zombie. 
* Each iteration through tick() results in the head zombie moving forward in the buffer, pointing to the mesh at that 
memory location and rendering it appropriately in the game. Subsequently, each zombie following the head is iteratively 
pushed forward one mesh in the buffer. Once the tail updates its mesh pointer, the rendering iteration is complete. 
When a zombie reaches the end of the buffer, it loops back around to the front of the buffer and loads the respective mesh (frame). 
* Optimization dramatically improves performance since each zombie didn't require a local copy of the meshes.
* Implementation handles Master Zombie scaling in the nextMesh() method found in Zombie.java.
* All other meshes (player attacking sequences, walking and previous selfs are stored in their respective arrays).
    
---

## GamePlay

Some number of zombies are spawned in an old abandoned and haunted Victorian Mansion, and 
you must navigate the house, survive the zombies, and escape with at least one of your 3 lives.

1. Your hit points are displayed at the top of the in-game screen.

2. You can attack and kill zombies. Their health is displayed as a red health bar above their heads.

3. Zombies will attack and kill you. You may push them away to get out of sticky situation, but this doesn't deal any 
   damage.

4. When you die, you come back to life, and your ghost and all zombies you previously attacked or exist. 
   Your past self ghost exists in its own timelines. The replay of their pushing/attacking 
   actions can engage/damage zombies and the current player. The past self ghosts, however, do not
   receive damage. Past zombies spawn in the same place they did in their former life and will pursue either 
   the current player or a player's ghost (whichever is closest).
   
5. Zombies now have a new death sequence (bones are scattered where they are eliminated by the player).
---

## Zombie pathfinding, movement and AI:

The zombies are created in EntityManager. A list of zombies is created. When zombies are created, a zombie mesh is 
created for that individual zombie. This is the 3D representation of that zombie on the screen. The zombie also has a 
bounding circle that is drawn around it. It is used for collision detection. When the animation timer in ZombieHouse3d 
starts, the zombies are assigned a random direction to travel in. The tick() method in Zombie is called at 60 fps every 
time the timer is called. The tick() method is the main method that drives everything about the zombies. It does the 
following in the listed sequence:

1. A collision is checked for with all of the walls in the level. If the zombie's bounding circle intersects with one of 
the walls, the angle that the zombie is going is subtracted by 180, and the zombie is moved in the opposite direction of 
the wall. When it is no longer intersecting the walls, the zombie stops moving. This pops the zombie out of the wall. If 
the zombie does not detect the player, the zombie will pick a random uniformly distributed angle from 0-360 degrees to 
travel in. The thread used to govern zombie decision rate in EntityManager is set to wait for 2 seconds, and then update the needed 
values for the zombie. So the next time the decision rate timer moves forward and the zombie makes a decision, the 
zombie will move in the new direction.

2. If the zombie has detected the player and is going after the player, then after the zombie has collided with the 
wall, a check is done to see if it has hit a corner. If it has, then the zombie is popped out of that corner and centered 
on the tile that is diagonal to the corner wall. The zombie will then choose to go in the direction of the player if the 
player is still in detection range of the zombie.

3. If a collision is not detected, then the zombie will continue to move in the direction of its current angle at a 
constant speed. 

4. A number of checks are done in tick() to see where the zombie is in relation to the center of tiles. If the zombie is 
at a position that is smaller than the halfway point of a tile in relation to the direction that the zombie is going, 
then I set the zombie position for purposes of pathfinding as the previous tile. This is so that the path that is being 
constructed for the zombie follows along the centers of tiles and not the corners. This helps the zombie to not get stuck 
in doorways.

5. The current tile that the zombie is standing on is determined by the checks I mentioned previously, and a function is 
called to do the pathfinding to the player for that zombie. If the zombie is within a Manhattan distance of 20 from the 
player, then the shortest path to the player is calculated using the A* pathfinding algorithm. The calculation of A* is 
contained in an inner class in Zombie called CalculatePath. 

6. The shortest path to the player is constantly being calculated for every zombie that is in range of the player. The 
“angle” field of zombie, which represents the direction that the zombies are going, can only be changed every 2 seconds, 
the decision rate of the zombies. So while the shortest path is constantly being calculated, the angle value is only 
reset every 2 seconds. This angle is based on the difference between the x and z values of the first 2 tiles in the 
shortest path. The zombie can move in 8 directions total to reach the player. 

7. A state machine exists in the Zombie/MasterZombie thread classes that exist in EntityManager.java. Initially, zombies
look for collisions with other zombies and if they occur subsequently, the zombies are assigned a state to move spread out away
from each other. The reason this state (overcrowded) exists is that many zombies tracking a player will form a queue as they pursue the player
and zombie collisions will result when the zombie at the head of the queue collides with the player (very much in a dominoes style).
This helps generate a 'swarm' attack state, however, it still appears at time that zombies are pulled into this queue if they aren't
able to 'escape it' fast enough before the leading ('head') zombie collides with the player. This state is given the highest priority
in the state machine. The second state (collision resonance) results in the following sequence: If a collision
is detected, the zombie is returned to its original location. If a subsequent collision occurs in zombie's next decision
and the heading set from the A* method is within 1 degree of the previous heading, a resonance state is established.
An angle of +/- 45 degrees relative to the previous heading is assigned to the zombie for it to get out of this resonance condition.

##### Pathfinding:
The pathfinding for each zombie starts with building a graph of nodes that represent the tiles of the game map. This 
graph nodes are created in ZombieHouse3d, and are represented by the GraphNode class. The class TileGraph contains the 
graph itself, which is contained in a synchronized hash map. The nodes of the graph have a list of 8 neighbors as well 
as various other fields that aid in pathfinding. 

The actual implementation of A* itself is pretty standard, except for some added condition checking in the
checkNeighbors method of CalculatePath. This condition checking is done so that A* doesn’t calculate paths that go 
diagonally through doorways. This was done to solve the problem of zombies getting stuck on doorways. 

The getPathLength method in CalculatePath returns the path length from the zombie to the player. If it is less than or 
equal to zombieSmell, which is set to 15, than it chases after the player. 

There is some additional functionality with the A* pathfinding specifically for the 2D game board. If the 2D board is 
being run, then the drawPath method in CalculatePath is called to draw a visual representation of the path from the 
zombie to the player. This path is only drawn to the screen if the zombie is within shortest path distance of 
zombieSmell from the player. 
 
##### Master Zombie
The master zombie is a single zombie that has special attributes. It runs on a faster decision rate thread in 
EntityManager which is separate from the other zombies. It is much faster than the other zombies. It also will 
immediately find a path to the player and go towards the player if any of the zombies detect the player. This 
functionality is governed by a boolean value “isMasterZombie” which is set to true if a zombie is designated as 
the master zombie. In EntityManager, the check is done to see if any zombies are going after the player, and if so, the 
master zombie is started towards the player. The Master Zombie is recognizable in the game because it is larger
than the other zombies.

---

## Player Movement and Controls

The stamina system with the player makes it so that when the player is running, the stamina is decreasing at a constant 
rate until it hits 0. When it hits 0, the player can no longer run until the stamina regenerates. The decrease and 
regeneration of stamina at a constant rate is handled within a thread in player called “PlayerStamina.” This is all 
handled within the player class.

All keyboard and mouse events are handled by the KeyboardEventHandler and the MouseEventHandler respectively. 

The code for handling player collision detection is in the Player class. When the player hits a wall, the collision 
detection stops the player from going in that same direction. The collision detection will slide the player along the wall to the 
left or right when the player is up against the wall and trying to move in the direction of the wall. 

When the player backs up towards a wall or advances towards a wall, the distance between the player and the 
camera is adjusted accordingly to creat a zooming effect (rather than having the camera go behind the walls which
would create a blackout.)

---

## Procedural Map Generation: 

Procedural map generation is performed based on a series of algorithms that take in width, height, and difficulty.

##### Setup:
First, a map is created that is 1/4th the size of the given map dimensions. This is done because it allows for simple 
hallway calculation on a resize (a room of width 1 becomes a hallway of width 3). A rectangular region for the map is 
created.

##### Dividing the Space:
The space is divided using a binary splitting function, which takes a rectangle and splits it into 2 smaller rectangles 
that fill the space of the previous one. This is done such that the split is perpendicular to the smaller side. 

This function is repeated first 3 times to get the 4 region dimensions in the game, and is then repeated for each region 
until all rooms are at least less than size 3 (12 in full size game space). After this, hallways are split off of the 
current rooms. The room at coordinate [0, 0] is split into a hallway if it is not already one.

##### Connecting Rooms Hallways and Regions:
Rooms and hallways are connected such that all rooms and hallways are connected and every hallway has at least 2 doors. 
First, all rooms and hallways for a region are connected in a non-directed graph with no cycles. Then, hallways with less 
than 2 connections are connected to a random neighbor. After all the rooms and hallways have been connected for each 
region, a single connection is inserted from each region to the next region in the path (1 -> 2 -> 3 -> 4).

##### Finishing:
The rooms and hallways are resized to full size (x4) and a 2D Tile array is created. For each room the tile is set to 
the region type unless it is the lower or right-hand edge, in which case it is a wall. The exit is added to a random 
border room in region 4 and obstacles are created based on the difficulty setting on tiles with odd-numbered-coordinates 
(to prevent unreachable areas).

This tile array is returned as the product of the procedural generation.

---

## Sounds Textures and Meshes:

##### Textures:
Every Tile has 3 maps:
* Specular - Defines specular reflections
* Diffuse - Defines diffuse reflections
* Normal/Bump - displays a pseudo 3d texture

##### Meshes and 3D rendering:
Each object that is rendered in the game was added to the game scene and then viewed using a 
javaFX camera object

Walls and Floors are rendered in the game using a Box object which was assigned the appropriate Specular diffuse and 
Normal/Bump texturing as a material

Zombie Meshes were imported using the jfx3dObjimporter by InteractiveMesh.org. The importer returns an array of 
Mesh Objects which are then assigned to each zombie individually.

##### Sounds:
Sounds are implemented using a SoundManager class which loads all of the sounds in the game and handles playing those 
sounds. AudioClips are used for the short game sounds and a MediaPlayer are used for the “long” Mp3 files. 

All sound clips used for this project were either created using FL-studio (music creation software) or taken from 
Freesound.org. Music for the project was taken from Purple-Planet Music.

http://soundbible.com/1773-Strong-Punch.html

##### Sound Balancing: 
Sound balancing is accomplished through consideration of the player's sight vector angle and the angle from the player
to each zombie. Angle conditioning and some trigonometry are used to solve for the correct channel output. See the 
soundBalance, computeSoundBalance and conditionAngles methods in EntityManager.java class for the implementation. 

---

## Sources:

##### Texture sources:
* www.planetminecraft.com/texture_pack/stcms-resourcepack-128x-parallax-amp-normal-mapping/

*OpenGameArt.org
*http://opengameart.org/node/10606
*http://opengameart.org/node/7921
*http://opengameart.org/node/9288
*http://opengameart.org/node/8015
*http://opengameart.org/node/9777
*http://opengameart.org/node/10054

*http://seamless-pixels.blogspot.com/2012/09/free-seamless-floor-tile-textures.html

*http://ghantapic.blogspot.in/2015/01/dark-ceiling-texture.html

*http://www.deviantart.com/art/tileable-pergola-wood-texture-png-215262316 by ftourini

*http://www.lughertexture.com/fabric-cloth-hires-textures/fabric-patterns-2-2371

*http://seamless-pixels.blogspot.com/2015/09/seamless-green-hedge-texture.html

*http://www.english-blog.com/images/

*https://50173199james.wordpress.com/category/visual-storytelling/mini-brief/mini-brief-2card-game-development/

*http://dedyone.deviantart.com/art/Haunted-Victorian-House-376928616

*http://pophipi.deviantart.com/art/Zombie-1-148457899

*http://www.deviantart.com/art/Zombie-107256041

*http://www.deviantart.com/art/The-Workbench-478944156

*http://www.deviantart.com/art/Safe-House-421477127

*http://revenant-frost.deviantart.com/art/Blue-Mist-98343044 by Revenant-Frost

* wwww.turbosquid.com

##### 3D Model sources:
* Model Importer - jfx3dObjimporter by InteractiveMesh.org

* Sword - http://tf3dm.com/3d-model/simple-sword-65623.html
* Guillotine - http://tf3dm.com/3d-models/guillotine/1/obj
* Zombie - http://www.blendswap.com/blends/view/76443
* Bone Pile - https://sketchfab.com/models/5ec580b41a934cea86d4297980d1378f
* chair - http://tf3dm.com/3d-model/chair-85457.html
* Knights - http://tf3dm.com/3d-model/knight-84265.html
* bear - http://tf3dm.com/3d-model/black-bear-87483.html
* horoscope table - http://tf3dm.com/3d-model/horoscop-table-52119.html
* tombstone - http://www.blendswap.com/blends/view/85096
* book pile - http://tf3dm.com/3d-model/books-31117.html
* pillar - https://sketchfab.com/models/c7e9384d01e544c0ae11d06938bb3a4c
* Lowpoly Man - http://www.blendswap.com/blends/view/66412
* Ana Character Player - https://www.turbosquid.com/FullPreview/Index.cfm/ID/534232


##### Sound sources:
* http://freesound.org/
* S: male_Thijs_loud_scream.aiff by thanvannispen | License: Attribution
* S: rip_tear FLESH!!!!.wav by aust_paul | License: Creative Commons 0
* S: Tearing Flesh by dereklieu | License: Attribution
* S: Man die by thestigmata | License: Attribution Noncommercial
* S: Zombie Attack by soykevin | License: Creative Commons 0
* S: Click by RADIY | License: Attribution
* S: btn121.wav by junggle | License: Attribution
* S: Footstep Drag Indoors .wav by abbahoot | License: Creative Commons 0
* S: Zombie Growling by gneube | License: Attribution
* S: Zombie 1 by Under7dude | License: Creative Commons 0
* S: Solo Zombie 1 by Slave2theLight | License: Attribution
* S: Zombie 20 by missozzy | License: Attribution
* S: Zombie by nanity05 | License: Creative Commons 0
* S: Footsteps on Tiles.wav by RutgerMuller | License: Creative Commons 0
* S: Slashkut by Abyssmal | License: Creative Commons 0
* S: thud.wav by OtisJames | License: Creative Commons 0
* S: GRUNT 2.wav by vmgraw | License: Creative Commons 0

* http://soundbible.com/
* S: http://soundbible.com/706-Swoosh-3.html

##### Music sources:
* Harbinger of Death by Purple-Planet Music
* http://www.purple-planet.com/horror/4583971268

---

## Known Bugs / Missing Features
* Game periodically (rarely) crashes at startup with no error messages


* The bounding cylinder on the player doesn't appear to be centered on the blender model (player). After many attempts, we 
  estimated the offsets that needed to be applied to the cylinder. This results in some cases where the player will slightly
  walk through the corner of an obstacle and missed attack sequences when it would seem that the zombie and player's bounding
  cylinders would intersect.

