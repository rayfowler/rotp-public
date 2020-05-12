/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.model.game;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.model.empires.Race;
import rotp.model.galaxy.GalaxyCircularShape;
import rotp.model.galaxy.GalaxyEllipticalShape;
import rotp.model.galaxy.GalaxyRectangularShape;
import rotp.model.galaxy.GalaxyRingShape;
import rotp.model.galaxy.GalaxyShape;
import rotp.model.galaxy.GalaxySpiralShape;
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.StarType;
import rotp.model.planet.Planet;
import rotp.model.planet.PlanetType;
import rotp.ui.game.SetupGalaxyUI;
import rotp.util.Base;

public class MOO1GameOptions implements Base, IGameOptions, Serializable {
    private static final long serialVersionUID = 1L;
    private final String[] opponentRaces = new String[MAX_OPPONENTS];
    private final List<Integer> colors = new ArrayList<>();
    private final List<Color> empireColors = new ArrayList<>();
    private final static NewPlayer player = new NewPlayer();

    private String selectedGalaxySize;
    private String selectedGalaxyShape;
    private String selectedGameDifficulty;
    private int selectedNumberOpponents;
    private boolean communityAI = false;
    private boolean disableRandomEvents = false;

    private transient GalaxyShape galaxyShape;

    public MOO1GameOptions() {
        init();
    }
    private void init() {
        initOpponentRaces();
        randomizeColors();
        setDefaultOptionValues();
    }
    private void resetSelectedOpponentRaces() {
        for (int i=0;i<opponentRaces.length;i++)
            selectedOpponentRace(i,null);
    }
    @Override
    public int numPlayers()                      { return 1; }
    @Override
    public int numColors()                       { return 10; }
    @Override
    public NewPlayer selectedPlayer()            { return player; }
    @Override
    public boolean communityAI()                 { return communityAI; }
    @Override
    public void communityAI(boolean b)           { communityAI = b; }
    @Override
    public boolean disableRandomEvents()           { return disableRandomEvents; }
    @Override
    public void disableRandomEvents(boolean b)     { disableRandomEvents = b; }
    @Override
    public String selectedGalaxySize()           { return selectedGalaxySize; }
    @Override
    public void selectedGalaxySize(String s)     {
        int prevMaxOpp = maximumOpponentsOptions();
        selectedGalaxySize = s; 
        if (selectedNumberOpponents() == prevMaxOpp)
            selectedNumberOpponents(maximumOpponentsOptions());
        generateGalaxy();
    }
    @Override
    public String selectedGalaxyShape()          { return selectedGalaxyShape; }
    @Override
    public void selectedGalaxyShape(String s)    { selectedGalaxyShape = s; setGalaxyShape(); generateGalaxy(); }
    @Override
    public String selectedGameDifficulty()       { return selectedGameDifficulty; }
    @Override
    public void selectedGameDifficulty(String s) { selectedGameDifficulty = s; }
    @Override
    public int selectedNumberOpponents()         { return selectedNumberOpponents; }
    @Override
    public void selectedNumberOpponents(int i)   { selectedNumberOpponents = i; generateGalaxy(); }
    @Override
    public String selectedPlayerRace()           { return selectedPlayer().race; }
    @Override
    public void selectedPlayerRace(String s)     { selectedPlayer().race = s;  resetSelectedOpponentRaces(); }
    @Override
    public int selectedPlayerColor()             { return selectedPlayer().color; }
    @Override
    public void selectedPlayerColor(int i)       { selectedPlayer().color = i; }
    @Override
    public String selectedLeaderName()           { return selectedPlayer().leaderName; }
    @Override
    public void selectedLeaderName(String s)     { selectedPlayer().leaderName = s.trim(); }
    @Override
    public String selectedHomeWorldName()        { return selectedPlayer().homeWorldName; }
    @Override
    public void selectedHomeWorldName(String s)  { selectedPlayer().homeWorldName = s.trim(); }
    @Override
    public String[] selectedOpponentRaces()      { return opponentRaces; }
    @Override
    public String selectedOpponentRace(int i)    { return i >= opponentRaces.length ? null : opponentRaces[i]; }
    @Override
    public void selectedOpponentRace(int i, String s) {
        if (i < opponentRaces.length)
            opponentRaces[i] = s;
    }
    @Override
    public int maximumOpponentsOptions() {
        int maxEmpires = min(numberStarSystems()/8, colors.size(), MAX_OPPONENT_TYPE*startingRaceOptions().size());
        int maxOpponents = min(SetupGalaxyUI.MAX_DISPLAY_OPPS);
        return min(maxOpponents, maxEmpires-1);
    }
    @Override
    public String name()                 { return "SETUP_RULESET_ORION"; }
    @Override
    public GalaxyShape galaxyShape()   {
        if (galaxyShape == null)
            setGalaxyShape();
        return galaxyShape;
    }
    private void setGalaxyShape() {
        switch(selectedGalaxyShape) {
            case SHAPE_ELLIPTICAL:
                galaxyShape = new GalaxyEllipticalShape(this);
                return;
            case SHAPE_CIRCULAR:
                galaxyShape = new GalaxyCircularShape(this);
                return;
            case SHAPE_RING:
                galaxyShape = new GalaxyRingShape(this);
                return;
            case SHAPE_SPIRAL:
                galaxyShape = new GalaxySpiralShape(this);
                return;
            case SHAPE_RECTANGLE:
            default:
                galaxyShape = new GalaxyRectangularShape(this);
                return;
        }
    }
    @Override
    public int numberStarSystems() {
            // MOO Strategy Guide, Table 3-2, p.50
        /*
        switch (selectedGalaxySize()) {
                case SIZE_SMALL:  return 24;
                case SIZE_MEDIUM: return 48;
                case SIZE_LARGE1:  return 70;
                case SIZE_HUGE:   return 108;
                default: return 48;
        }
        */
        switch (selectedGalaxySize()) {
            case SIZE_TINY:       return 33;
            case SIZE_SMALL:      return 50;
            case SIZE_SMALL2:     return 70;
            case SIZE_MEDIUM:     return 100;
            case SIZE_MEDIUM2:    return 150;
            case SIZE_LARGE:      return 225;
            case SIZE_LARGE2:     return 333;
            case SIZE_HUGE:       return 500;
            case SIZE_HUGE2:      return 700;
            case SIZE_MASSIVE:    return 1000;
            case SIZE_MASSIVE2:   return 1500;
            case SIZE_MASSIVE3:   return 2250;
            case SIZE_MASSIVE4:   return 3333;
            case SIZE_MASSIVE5:   return 5000;
            case SIZE_INSANE:     return 10000;
            case SIZE_LUDICROUS:  return 100000;
            case SIZE_MAXIMUM:    return maximumSystems();
        }
        return 8*(selectedNumberOpponents()+1);
    }
    @Override
    public int numberNebula() {
        // MOO Strategy Guide, Table 3-3, p.51
        /*
        switch (selectedGalaxySize()) {
        case SIZE_SMALL:     return roll(0,1);
        case SIZE_MEDIUM:    return roll(1,2);
        case SIZE_LARGE:     return roll(2,3);
        case SIZE_HUGE:      return roll(2,4);
        case SIZE_LUDICROUS: return roll(10,20);
        default: return roll(1,2);
        }
        */
        int n = this.numberStarSystems();
        return roll(n/50, n/25);
    }
    @Override
    public float researchCostBase() {
        return 30.0f;
        /*
        switch (selectedGameDifficulty()) {
        case DIFFICULTY_SIMPLE:      return 20;
        case DIFFICULTY_EASY:        return 25;
        case DIFFICULTY_AVERAGE:     return 30;
        case DIFFICULTY_HARD:        return 35;
        case DIFFICULTY_IMPOSSIBLE:  return 40;
        default:                     return 25;
        }
        */
    }
    @Override
    public String randomStarType() {
        // distribution per MOO1 Official Strategy Guide
        float r = random();
        if (r <= .30)
            return StarType.RED;
        else if (r <= .55)
            return StarType.ORANGE;
        else if (r <= .70)
            return StarType.YELLOW;
        else if (r <= .85)
            return StarType.BLUE;
        else if (r <= .95)
            return StarType.WHITE;
        else
            return StarType.PURPLE;
    }
    @Override
    public Planet randomPlanet(StarSystem s) {
        Planet p = new Planet(s);
        float r = random();
        String[] planetTypes = { "PLANET_NONE", "PLANET_RADIATED", "PLANET_TOXIC", "PLANET_INFERNO",
                        "PLANET_DEAD", "PLANET_TUNDRA", "PLANET_BARREN", "PLANET_MINIMAL", "PLANET_DESERT",
                        "PLANET_STEPPE", "PLANET_ARID", "PLANET_OCEAN", "PLANET_JUNGLE", "PLANET_TERRAN" };

        float[] pcts;

        float[] redPcts =    { .05f, .10f, .15f, .20f, .25f, .30f, .35f, .40f, .50f, .60f, .75f, .85f, .95f, 1.0f };
        float[] greenPcts =  { .05f, .10f, .15f, .20f, .25f, .30f, .35f, .40f, .45f, .55f, .65f, .75f, .85f, 1.0f };
        float[] yellowPcts = { .00f, .00f, .00f, .05f, .05f, .10f, .15f, .20f, .25f, .30f, .40f, .50f, .60f, 1.0f };
        float[] bluePcts =   { .15f, .25f, .35f, .45f, .55f, .65f, .75f, .85f, .90f, .95f, 1.0f, 1.0f, 1.0f, 1.0f };
        float[] whitePcts =  { .10f, .15f, .25f, .35f, .45f, .55f, .65f, .75f, .85f, .90f, .95f, 1.0f, 1.0f, 1.0f };
        float[] purplePcts = { .20f, .45f, .60f, .75f, .85f, .90f, .95f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f };

        int typeIndex = 0;
        switch (s.starType().key()) {
            case StarType.RED:    pcts = redPcts;    break;
            case StarType.ORANGE:  pcts = greenPcts;  break;
            case StarType.YELLOW: pcts = yellowPcts; break;
            case StarType.BLUE:   pcts = bluePcts;   break;
            case StarType.WHITE:  pcts = whitePcts; break;
            case StarType.PURPLE: pcts = purplePcts; break;
            default:
                pcts = redPcts; break;
        }

        for (int i=0;i<pcts.length;i++) {
            if (r <= pcts[i]) {
                typeIndex = i;
                break;
            }
        }
        p.initPlanetType(planetTypes[typeIndex]);

        checkForHostileEnvironment(p, s);

        checkForPoorResources(p, s);
        if (p.isResourceNormal())
            checkForRichResources(p, s);
        if (p.isResourceNormal())
            checkForArtifacts(p, s);
        return p;
    }
    @Override
    public String randomPlayerStarType(Race r)     { return StarType.YELLOW; }
    @Override
    public String randomRaceStarType(Race r)       { 
        List<String> types = new ArrayList<>();
        types.add(StarType.RED);
        types.add(StarType.ORANGE);
        types.add(StarType.YELLOW);

        return random(types); 
    }
    @Override
    public Planet orionPlanet(StarSystem s) {
        Planet p = new Planet(s);
        p.initPlanetType("PLANET_TERRAN");
        return p;
    }
    @Override
    public Planet randomPlayerPlanet(Race r, StarSystem s) {
        Planet p = new Planet(s);
        p.initPlanetType(r.homeworldPlanetType);
        return p;
    }
    @Override
    public List<String> galaxySizeOptions() {
        int max = maximumSystems();
        List<String> list = new ArrayList<>();
        list.add(SIZE_TINY);
        if (max > 50)
            list.add(SIZE_SMALL);
        if (max > 70)
            list.add(SIZE_SMALL2);
        if (max > 100)
            list.add(SIZE_MEDIUM);
        if (max > 150)
            list.add(SIZE_MEDIUM2);
        if (max > 225)
            list.add(SIZE_LARGE);
        if (max > 333)
            list.add(SIZE_LARGE2);
        if (max > 500)
            list.add(SIZE_HUGE);
        if (max > 700)
            list.add(SIZE_HUGE2);
        if (max > 1000)
            list.add(SIZE_MASSIVE);
        if (max > 1500)
            list.add(SIZE_MASSIVE2);
        if (max > 2250)
            list.add(SIZE_MASSIVE3);
        if (max > 3333)
            list.add(SIZE_MASSIVE4);
        if (max > 5000)
            list.add(SIZE_MASSIVE5);
        if (max > 10000)
            list.add(SIZE_INSANE);
        if (max > 100000)
            list.add(SIZE_LUDICROUS);
        list.add(SIZE_MAXIMUM);
        return list;
    }
    @Override
    public List<String> galaxyShapeOptions() {
        List<String> list = new ArrayList<>();
        list.add(SHAPE_RECTANGLE);
        list.add(SHAPE_CIRCULAR);
        list.add(SHAPE_RING);
        list.add(SHAPE_ELLIPTICAL);
        list.add(SHAPE_SPIRAL);
        return list;
    }
    @Override
    public List<String> gameDifficultyOptions() {
        List<String> list = new ArrayList<>();
        list.add(DIFFICULTY_EASIEST);
        list.add(DIFFICULTY_EASIER);
        list.add(DIFFICULTY_EASY);
        list.add(DIFFICULTY_NORMAL);
        list.add(DIFFICULTY_HARD);
        list.add(DIFFICULTY_HARDER);
        list.add(DIFFICULTY_HARDEST);
        return list;
    }
    @Override
    public List<String> startingRaceOptions() {
        List<String> list = new ArrayList<>();
        list.add("RACE_HUMAN");
        list.add("RACE_ALKARI");
        list.add("RACE_BULRATHI");
        list.add("RACE_DARLOK");
        list.add("RACE_KLACKON");
        list.add("RACE_MEKLAR");
        list.add("RACE_MRRSHAN");
        list.add("RACE_PSILON");
        list.add("RACE_SAKKRA");
        list.add("RACE_SILICOID");
        return list;
    }
    @Override
    public List<Integer> possibleColors() {
        return new ArrayList<>(colors);
    }
    protected void setDefaultOptionValues() {
        selectedGalaxySize = SIZE_SMALL;
        selectedGalaxyShape = galaxyShapeOptions().get(0);
        selectedGameDifficulty(gameDifficultyOptions().get(0));
        selectedNumberOpponents = maximumOpponentsOptions();
        selectedPlayerRace(random(startingRaceOptions()));
        selectedGameDifficulty = DIFFICULTY_NORMAL;
        generateGalaxy();
    }
    private void generateGalaxy() {
        galaxyShape().quickGenerate();
    }
    @Override
    public Color color(int i)  { return empireColors.get(i); }
    @Override
    public void randomizeColors() {
        empireColors.clear();
        empireColors.add(new Color(9,131,214));   // blue
        empireColors.add(new Color(132,57,20));   // brown
        empireColors.add(new Color(0,166,81));    // green
        empireColors.add(new Color(255,127,0));   // orange
        empireColors.add(new Color(247,127,230)); // pink
        empireColors.add(new Color(145,51,188));  // purple
        empireColors.add(new Color(237,28,36));   // red
        empireColors.add(new Color(56,232,186));  // teal
        empireColors.add(new Color(247,229,60));  // yellow
        empireColors.add(new Color(255,255,255)); // white
        colors.clear();
        //primary color list
        List<Integer> list1 = new ArrayList<>();
        list1.add(0);
        list1.add(1);
        list1.add(2);
        list1.add(3);
        list1.add(4);
        list1.add(5);

        //secondary color list
        List<Integer> list1a = new ArrayList<>();
        list1a.add(6);
        list1a.add(7);
        list1a.add(8);
        list1a.add(9);

        // start repeating the 10-color list for copies of races (up to 5 per race)
        List<Integer> list2 = new ArrayList<>(list1);
        list2.addAll(list1a);
        List<Integer> list3 = new ArrayList<>(list2);
        List<Integer> list4 = new ArrayList<>(list2);
        List<Integer> list5 = new ArrayList<>(list2);
            
        Collections.shuffle(list1);
        Collections.shuffle(list1a);
        Collections.shuffle(list2);
        Collections.shuffle(list3);
        Collections.shuffle(list4);
        Collections.shuffle(list5);
        colors.addAll(list1);
        colors.addAll(list1a);
        colors.addAll(list2);
        colors.addAll(list3);
        colors.addAll(list4);
        colors.addAll(list5);
    }
    private void initOpponentRaces() {
    }
    private void checkForHostileEnvironment(Planet p, StarSystem s) {
        // these planet types and no chance for poor resources -- skip
        switch(p.type().key()) {
            case PlanetType.NONE:
                p.makeEnvironmentNone();
                break;
            case PlanetType.RADIATED:
            case PlanetType.TOXIC:
            case PlanetType.INFERNO:
            case PlanetType.DEAD:
            case PlanetType.TUNDRA:
            case PlanetType.BARREN:
                p.makeEnvironmentHostile();
                break;
            case PlanetType.DESERT:
            case PlanetType.STEPPE:
            case PlanetType.ARID:
            case PlanetType.OCEAN:
            case PlanetType.JUNGLE:
            case PlanetType.TERRAN:
                if (random() < .083333)
                    p.makeEnvironmentFertile();  // become fertile
                break;
        }
    }
    private void checkForPoorResources(Planet p, StarSystem s) {
        // these planet types and no chance for poor resources -- skip
        switch(p.type().key()) {
            case PlanetType.NONE:
            case PlanetType.RADIATED:
            case PlanetType.TOXIC:
            case PlanetType.INFERNO:
            case PlanetType.DEAD:
            case PlanetType.TUNDRA:
            case PlanetType.BARREN:
            case PlanetType.JUNGLE:
            case PlanetType.TERRAN:
                return;
        }

        float r1 = 0;
        float r2 = 0;
        switch(s.starType().key()) {
            case StarType.BLUE:
            case StarType.WHITE:
            case StarType.YELLOW:
                r1 = .025f; r2 = .10f;
                break;
            case StarType.RED:
                r1 = .06f;  r2 = .20f;
                break;
            case StarType.ORANGE:
                r1 = .135f; r2 = .30f;
                break;
            case StarType.PURPLE:
                // can never have poor/ultrapoor
                return;
            default:
                throw new RuntimeException(concat("Invalid star type for options: ", s.starType().key()));
        }
        float r = random();
        if (r <= r1)
            p.setResourceUltraPoor();
        else if (r <= r2)
            p.setResourcePoor();
    }
    private void checkForRichResources(Planet p, StarSystem s) {
        // planet/star ratios per Table 3-9a of Strategy Guide
        float r1 = 0;
        float r2 = 0;
        switch(s.starType().key()) {
            case StarType.RED:
            case StarType.WHITE:
            case StarType.YELLOW:
            case StarType.ORANGE:
                switch(p.type().key()) {
                    case PlanetType.RADIATED:   r1 = .2625f; r2 = .35f; break;
                    case PlanetType.TOXIC:      r1 = .225f;  r2 = .30f; break;
                    case PlanetType.INFERNO:    r1 = .1875f; r2 = .25f; break;
                    case PlanetType.DEAD:       r1 = .15f;   r2 = .20f; break;
                    case PlanetType.TUNDRA:     r1 = .1125f; r2 = .15f; break;
                    case PlanetType.BARREN:     r1 = .075f;  r2 = .10f; break;
                    case PlanetType.MINIMAL:    r1 = .0375f; r2 = .05f; break;
                }
                break;
            case StarType.BLUE:
                switch(p.type().key()) {
                    case PlanetType.RADIATED:   r1 = .2925f; r2 = .45f; break;
                    case PlanetType.TOXIC:      r1 = .26f;   r2 = .40f; break;
                    case PlanetType.INFERNO:    r1 = .2275f; r2 = .35f; break;
                    case PlanetType.DEAD:       r1 = .195f;  r2 = .30f; break;
                    case PlanetType.TUNDRA:     r1 = .1625f; r2 = .25f; break;
                    case PlanetType.BARREN:     r1 = .13f;   r2 = .20f; break;
                    case PlanetType.MINIMAL:    r1 = .0975f; r2 = .15f; break;
                    case PlanetType.DESERT:     r1 = .065f;  r2 = .10f; break;
                    case PlanetType.STEPPE:     r1 = .0325f; r2 = .05f; break;
                }
                break;
            case StarType.PURPLE:
                switch(p.type().key()) {
                    case PlanetType.RADIATED:   r1 = .30f;   r2 = .60f; break;
                    case PlanetType.TOXIC:      r1 = .275f;  r2 = .55f; break;
                    case PlanetType.INFERNO:    r1 = .25f;   r2 = .50f; break;
                    case PlanetType.DEAD:       r1 = .225f;  r2 = .45f; break;
                    case PlanetType.TUNDRA:     r1 = .20f;   r2 = .40f; break;
                    case PlanetType.BARREN:     r1 = .175f;  r2 = .35f; break;
                    case PlanetType.MINIMAL:    r1 = .15f;   r2 = .30f; break;
                }
                break;
            default:
                throw new RuntimeException(concat("Invalid star type for options: ", s.starType().key()));
        }

        float r = random();
        if (r <= r1)
            p.setResourceRich();
        else if (r <= r2)
            p.setResourceUltraRich();
    }
    private void checkForArtifacts(Planet p, StarSystem s) {
        switch(p.type().key()) {
            case PlanetType.STEPPE:
            case PlanetType.ARID:
            case PlanetType.OCEAN:
            case PlanetType.JUNGLE:
            case PlanetType.TERRAN:
                if (random() <= 0.10)
                    p.setArtifact();
        }
    }
}
