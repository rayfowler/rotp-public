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
import java.util.List;
import rotp.model.empires.Race;
import rotp.model.galaxy.GalaxyShape;
import rotp.model.galaxy.StarSystem;
import rotp.model.planet.Planet;
import rotp.ui.game.SetupGalaxyUI;

public interface IGameOptions {
    public static final int MAX_OPPONENTS = SetupGalaxyUI.MAX_DISPLAY_OPPS;
    public static final int MAX_OPPONENT_TYPE = 5;
    public static final String SIZE_TINY = "SETUP_GALAXY_SIZE_TINY";
    public static final String SIZE_SMALL = "SETUP_GALAXY_SIZE_SMALL";
    public static final String SIZE_SMALL2 = "SETUP_GALAXY_SIZE_SMALL2";
    public static final String SIZE_MEDIUM = "SETUP_GALAXY_SIZE_AVERAGE";
    public static final String SIZE_MEDIUM2 = "SETUP_GALAXY_SIZE_AVERAGE2";
    public static final String SIZE_LARGE = "SETUP_GALAXY_SIZE_LARGE";
    public static final String SIZE_LARGE2 = "SETUP_GALAXY_SIZE_LARGE2";
    public static final String SIZE_HUGE = "SETUP_GALAXY_SIZE_HUGE";
    public static final String SIZE_HUGE2 = "SETUP_GALAXY_SIZE_HUGE2";
    public static final String SIZE_MASSIVE = "SETUP_GALAXY_SIZE_MASSIVE";
    public static final String SIZE_MASSIVE2 = "SETUP_GALAXY_SIZE_MASSIVE2";
    public static final String SIZE_MASSIVE3 = "SETUP_GALAXY_SIZE_MASSIVE3";
    public static final String SIZE_MASSIVE4 = "SETUP_GALAXY_SIZE_MASSIVE4";
    public static final String SIZE_MASSIVE5 = "SETUP_GALAXY_SIZE_MASSIVE5";
    public static final String SIZE_INSANE = "SETUP_GALAXY_SIZE_INSANE";
    public static final String SIZE_LUDICROUS = "SETUP_GALAXY_SIZE_LUDICROUS";
    public static final String SIZE_MAXIMUM = "SETUP_GALAXY_SIZE_MAXIMUM";

    public static final String SHAPE_RECTANGLE = "SETUP_GALAXY_SHAPE_RECTANGLE";
    public static final String SHAPE_CIRCULAR = "SETUP_GALAXY_SHAPE_CIRCULAR";
    public static final String SHAPE_RING = "SETUP_GALAXY_SHAPE_RING";
    public static final String SHAPE_ELLIPTICAL = "SETUP_GALAXY_SHAPE_ELLIPSE";
    public static final String SHAPE_SPIRAL = "SETUP_GALAXY_SHAPE_SPIRAL";

    public static final String DIFFICULTY_EASIEST = "SETUP_DIFFICULTY_EASIEST";
    public static final String DIFFICULTY_EASIER  = "SETUP_DIFFICULTY_EASIER";
    public static final String DIFFICULTY_EASY    = "SETUP_DIFFICULTY_EASY";
    public static final String DIFFICULTY_NORMAL  = "SETUP_DIFFICULTY_NORMAL";
    public static final String DIFFICULTY_HARD    = "SETUP_DIFFICULTY_HARD";
    public static final String DIFFICULTY_HARDER  = "SETUP_DIFFICULTY_HARDER";
    public static final String DIFFICULTY_HARDEST = "SETUP_DIFFICULTY_HARDEST";

    public default boolean isAutoPlay()          { return false; }
    public default boolean communityAI()         { return false; }
    public default void communityAI(boolean b)   { }
    public default int maxOpponents()            { return MAX_OPPONENTS; }
    public String name();

    public int numberStarSystems();
    public int numberNebula();
    public List<Integer> possibleColors();
    public float researchCostBase();
    public String randomStarType();
    public String randomPlayerStarType(Race r);
    public String randomRaceStarType(Race r);
    public Planet randomPlanet(StarSystem s);
    public Planet randomPlayerPlanet(Race r, StarSystem s);
    public Planet orionPlanet(StarSystem s);
    public void randomizeColors();
    public GalaxyShape galaxyShape();
    public int numColors();
    public Color color(int i);
    public boolean disableRandomEvents();
    public void disableRandomEvents(boolean b);

    // selectable options
    public List<String> galaxySizeOptions();
    public List<String> galaxyShapeOptions();
    public List<String> gameDifficultyOptions();
    public int maximumOpponentsOptions();
    public List<String> startingRaceOptions();

    public String selectedGalaxySize();
    public void selectedGalaxySize(String s);
    public String selectedGalaxyShape();
    public void selectedGalaxyShape(String s);
    public String selectedGameDifficulty();
    public void selectedGameDifficulty(String s);
    public int selectedNumberOpponents();
    public void selectedNumberOpponents(int i);

    public int numPlayers();
    public NewPlayer selectedPlayer();
    public String selectedPlayerRace();
    public void selectedPlayerRace(String s);
    public int selectedPlayerColor();
    public void selectedPlayerColor(int i);
    public String selectedLeaderName();
    public void selectedLeaderName(String s);
    public String selectedHomeWorldName();
    public void selectedHomeWorldName(String s);
    public String[] selectedOpponentRaces();
    public String selectedOpponentRace(int i);
    public void selectedOpponentRace(int i, String s);

    default String nextGalaxySize(boolean bounded) {
        List<String> opts = galaxySizeOptions();
        int index = opts.indexOf(selectedGalaxySize())+1;
        if (bounded && (index >= opts.size()))
            return selectedGalaxySize();
        return index >= opts.size() ? opts.get(index-1) : opts.get(index);
    }
    default String prevGalaxySize(boolean bounded) {
        List<String> opts = galaxySizeOptions();
        int index = opts.indexOf(selectedGalaxySize())-1;
        if (bounded && index < 0)
            return selectedGalaxySize();
        return index < 0 ? opts.get(0) : opts.get(index);
    }
    default String nextGalaxyShape() {
        List<String> opts = galaxyShapeOptions();
        int index = opts.indexOf(selectedGalaxyShape())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String prevGalaxyShape() {
        List<String> opts = galaxyShapeOptions();
        int index = opts.indexOf(selectedGalaxyShape())-1;
        return index < 0 ? opts.get(opts.size()-1) : opts.get(index);
    }
    default String nextGameDifficulty() {
        List<String> opts = gameDifficultyOptions();
        int index = opts.indexOf(selectedGameDifficulty())+1;
        return index >= opts.size() ? opts.get(index-1) : opts.get(index);
    }
    default String prevGameDifficulty() {
        List<String> opts = gameDifficultyOptions();
        int index = opts.indexOf(selectedGameDifficulty())-1;
        return index < 0 ? opts.get(0) : opts.get(index);
    }
    default void nextOpponent(int i) {
        String player = selectedPlayerRace();
        List<String> allOpps = startingRaceOptions();
        String[] selectedOpps = selectedOpponentRaces();
        String currOpp = this.selectedOpponentRace(i);

        int nextIndex = currOpp == null ? 0 : allOpps.indexOf(currOpp)+1;
        if (nextIndex >= allOpps.size())
            nextIndex = -1;
        while (true) {
            String nextOpp = nextIndex < 0 ? null : allOpps.get(nextIndex);
            int count = (nextOpp != null) && nextOpp.equals(player) ? 1 : 0;
            for (String opp: selectedOpps) {
                if ((nextOpp != null) && nextOpp.equals(opp))
                    count++;
            }
            if (count < MAX_OPPONENT_TYPE) {
                selectedOpponentRace(i, nextOpp);
                return;
            }
            nextIndex++;
            if (nextIndex >= allOpps.size())
                nextIndex = -1;
        }
    }
    default void prevOpponent(int i) {
        String player = selectedPlayerRace();
        List<String> allOpps = startingRaceOptions();
        String[] selectedOpps = selectedOpponentRaces();
        String currOpp = selectedOpponentRace(i);
        int lastIndex = allOpps.size()-1;

        int prevIndex = currOpp == null ? lastIndex : allOpps.indexOf(currOpp)-1;
        while (true) {
            String nextOpp = prevIndex < 0 ? null : allOpps.get(prevIndex);
            int count = (nextOpp != null) && nextOpp.equals(player) ? 1 : 0;
            for (String opp: selectedOpps) {
                if ((nextOpp != null) && nextOpp.equals(opp))
                    count++;
            }
            if (count < MAX_OPPONENT_TYPE) {
                selectedOpponentRace(i, nextOpp);
                return;
            }
            prevIndex--;
            if (prevIndex < -1)
                prevIndex = lastIndex;
        }
    }
    default float aiProductionModifier() {
        switch(selectedGameDifficulty()) {
            case DIFFICULTY_EASIEST: return 0.5f;
            case DIFFICULTY_EASIER:  return 0.75f;
            case DIFFICULTY_EASY:    return 0.9f;
            case DIFFICULTY_HARD:    return 1.1f;
            case DIFFICULTY_HARDER:  return 1.4f;
            case DIFFICULTY_HARDEST: return 2.0f;
            default: return 1.0f;
        }
    }
    default float aiWasteModifier() {
        switch(selectedGameDifficulty()) {
            case DIFFICULTY_EASIEST: return 0.5f;
            case DIFFICULTY_EASIER:  return 0.75f;
            case DIFFICULTY_EASY:    return 0.9f;
            default: return 1.0f;
        }
    }
}
