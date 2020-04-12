# rotp-governor

Remnants of the Precursors fork with Planetary Governor mod 

https://remnantsoftheprecursors.com/

https://rayfowler.itch.io/remnants-of-the-precursors

https://github.com/rayfowler/rotp-public

This governor manages planet spending to:

* Set ecology to minimum "clean"
* Set max production until all factories are built.
* Set max ecology until max population is reached.
* Set max defence until required number of bases is built.
* Build a stargate if technology is available.
* If all above have been built, research.

It can be toggled on or off for each planet. You can basically enable it on any
planet not building ships and leave it untouched for most of the game. With new 
tech discoveries it will readjust the sliders automatically. This cuts down the
amount of micromanagement needed drastically.

To run the mod:

* Download my distribution of ROTP-1.1.jar (large file) and run that instead of 
original game.

or

* Download only the ROTP-1.1-governor.jar and run.bat or run.sh if you are on Linux.
* Place them in same directory that contains original Remnants.jar
* Execute run.bat or run.sh

To enable governor, use 'q' key on keyboard, or else click "Allocate Spending"
text in the planetary spending screen.

---

Additional features.

* This mod will transport population from planets that are full to planets that
are underpopulated. Population from planets with maximum population will be 
transported. Only population that will grow back in 1 turn will be transported 
(usually 1-2 pop). When chosing destination, target population and distance will
be taken into account. If you want to turn this off, add "-Dautotransport=false" 
to Java command line like this:

java -Dautotransport=false -Xmx2048m ROTP-1.1.jar arg1

or

java -cp ROTP-1.1-governor.jar:Remnants.jar -Dautotransport=false -Xmx2048m rotp.Rotp arg1

* This mod will build stargates on all planets when technology is available. If you
want to turn this off, add "-Dautogate=false" to Java command line.

java -Dautotransport=false -Xmx2048m ROTP-1.1.jar arg1

or

java -cp ROTP-1.1-governor.jar:Remnants.jar -Dautogate=false -Xmx2048m rotp.Rotp arg1

---

# Building from source

It's a maven build. Git clone the sources, then do "mvn package" and you have entire
project built and packaged in "target" directory.
