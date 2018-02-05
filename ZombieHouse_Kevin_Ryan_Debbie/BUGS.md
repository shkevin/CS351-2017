##BUGS

* Pressing space bar while in game causes glitch with sword. -Kevin
    @Debbie - fixed.
* Setting Max Zombies in "settings" causes the game to quit responding. -Kevin
    -Kevin said he fixed.
*Zombies have potential to get stuck in a wall, doing a side to side semi-circle movement (during this stage
they can't attack and can't be attacked)

-------------------------
Imported from prior class
-------------------------
* MasterZombie sometimes is unable to attack or be attacked unless player shifts position
* Too many bifurcating zombies spawn; each playthrough spawns a new set, and this quickly escalates--N/A
* Very rarely, collision detection will fail to prevent a zombie from getting briefly stuck in a wall
* Weapon collision detection not enabled
* Attack actions aren't replayed in the ghosts @Debbie-fixed.
* Sometimes at the start of the game camera starts of rotating instead of being still @Debbie-fixed.
* When the player during a ghost zombie's timeline dies (e.g. it kills it), it immediately becomes a free zombie. On 
the following playthroughs, even if that zombie went on to kill you, it is still a free zombie (though the one that 
kills you would still become a ghost zombie). This logic could be improved to prevent this apparent "split" of the past 
zombie across timelines.