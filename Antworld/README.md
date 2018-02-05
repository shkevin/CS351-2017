# AntWorld

## Group
- Adrian Aleixandre
- Nolan Alimonti
- Kevin Cox
- Alex Johnson

## Techniques

- We use a potential field approach for much of our logic. We guide the ants away from areas that they have already been which encourages them to explore.
- We run Dijkstras's algorithm from the nest outwards and save all paths from any tile to the nest in memory to always have a way for an ant to return home.
- Our ants will go find water and heal when at low health, which keeps 99% of them alive.
- We chose to use only explorer ants because we believe that their speed and sight abilities are superior to any of the other ant types

## Issues/Limitations

- We have no combat implemented
- Our ants have a tendency to find local maxima while hill climbing and get stuck in certain areas.
