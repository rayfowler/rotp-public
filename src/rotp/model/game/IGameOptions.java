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
import rotp.model.ai.AI;
import rotp.model.empires.Empire;
import rotp.model.empires.Race;
import rotp.model.events.RandomEvent;
import rotp.model.galaxy.GalaxyShape;
import rotp.model.galaxy.StarSystem;
import rotp.model.planet.Planet;
import rotp.model.tech.TechEngineWarp;
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
    public static final String SHAPE_ELLIPTICAL = "SETUP_GALAXY_SHAPE_ELLIPSE";
    public static final String SHAPE_SPIRAL = "SETUP_GALAXY_SHAPE_SPIRAL";

    public static final String DIFFICULTY_EASIEST = "SETUP_DIFFICULTY_EASIEST";
    public static final String DIFFICULTY_EASIER  = "SETUP_DIFFICULTY_EASIER";
    public static final String DIFFICULTY_EASY    = "SETUP_DIFFICULTY_EASY";
    public static final String DIFFICULTY_NORMAL  = "SETUP_DIFFICULTY_NORMAL";
    public static final String DIFFICULTY_HARD    = "SETUP_DIFFICULTY_HARD";
    public static final String DIFFICULTY_HARDER  = "SETUP_DIFFICULTY_HARDER";
    public static final String DIFFICULTY_HARDEST = "SETUP_DIFFICULTY_HARDEST";
    
    public static final String RESEARCH_NORMAL  = "SETUP_RESEARCH_RATE_NORMAL";
    public static final String RESEARCH_SLOW    = "SETUP_RESEARCH_RATE_SLOW";
    public static final String RESEARCH_SLOWER  = "SETUP_RESEARCH_RATE_SLOWER";
    public static final String RESEARCH_SLOWEST = "SETUP_RESEARCH_RATE_SLOWEST";
    
    public static final String TECH_TRADING_YES     = "SETUP_TECH_TRADING_YES";
    public static final String TECH_TRADING_ALLIES  = "SETUP_TECH_TRADING_ALLIES";
    public static final String TECH_TRADING_NO      = "SETUP_TECH_TRADING_NO";
    
    public static final String GALAXY_AGE_NORMAL = "SETUP_GALAXY_AGE_NORMAL";
    public static final String GALAXY_AGE_YOUNG  = "SETUP_GALAXY_AGE_YOUNG";
    public static final String GALAXY_AGE_OLD    = "SETUP_GALAXY_AGE_OLD";

    public static final String RANDOM_EVENTS_ON  = "SETUP_RANDOM_EVENTS_ON";
    public static final String RANDOM_EVENTS_OFF = "SETUP_RANDOM_EVENTS_OFF";
    public static final String RANDOM_EVENTS_NO_MONSTERS = "SETUP_RANDOM_EVENTS_NO_MONSTERS";
    
    public static final String WARP_SPEED_NORMAL = "SETUP_WARP_SPEED_NORMAL";
    public static final String WARP_SPEED_FAST   = "SETUP_WARP_SPEED_FAST";
    
    public static final String NEBULAE_NONE      = "SETUP_NEBULA_NONE";
    public static final String NEBULAE_RARE      = "SETUP_NEBULA_RARE";
    public static final String NEBULAE_UNCOMMON  = "SETUP_NEBULA_UNCOMMON";
    public static final String NEBULAE_NORMAL    = "SETUP_NEBULA_NORMAL";
    public static final String NEBULAE_COMMON    = "SETUP_NEBULA_COMMON";
    public static final String NEBULAE_FREQUENT  = "SETUP_NEBULA_FREQUENT";
    
    public static final String COUNCIL_IMMEDIATE = "SETUP_COUNCIL_IMMEDIATE";
    public static final String COUNCIL_REBELS    = "SETUP_COUNCIL_REBELS";
    public static final String COUNCIL_NONE      = "SETUP_COUNCIL_NONE";
    
    public static final String STAR_DENSITY_LOWEST   = "SETUP_STAR_DENSITY_LOWEST";
    public static final String STAR_DENSITY_LOWER    = "SETUP_STAR_DENSITY_LOWER";
    public static final String STAR_DENSITY_LOW      = "SETUP_STAR_DENSITY_LOW";
    public static final String STAR_DENSITY_NORMAL   = "SETUP_STAR_DENSITY_NORMAL";
    public static final String STAR_DENSITY_HIGH     = "SETUP_STAR_DENSITY_HIGH";
    public static final String STAR_DENSITY_HIGHER   = "SETUP_STAR_DENSITY_HIGHER";
    public static final String STAR_DENSITY_HIGHEST  = "SETUP_STAR_DENSITY_HIGHEST";
    
    public static final String PLANET_QUALITY_POOR   = "SETUP_PLANET_QUALITY_POOR";
    public static final String PLANET_QUALITY_MEDIOCRE  = "SETUP_PLANET_QUALITY_MEDIOCRE";
    public static final String PLANET_QUALITY_NORMAL = "SETUP_PLANET_QUALITY_NORMAL";
    public static final String PLANET_QUALITY_GOOD   = "SETUP_PLANET_QUALITY_GOOD";
    public static final String PLANET_QUALITY_GREAT  = "SETUP_PLANET_QUALITY_GREAT";
        
    public static final String TERRAFORMING_NORMAL   = "SETUP_TERRAFORMING_NORMAL";
    public static final String TERRAFORMING_REDUCED  = "SETUP_TERRAFORMING_REDUCED";
    public static final String TERRAFORMING_NONE     = "SETUP_TERRAFORMING_NONE";
        
    public static final String COLONIZING_NORMAL      = "SETUP_COLONIZING_NORMAL";
    public static final String COLONIZING_RESTRICTED  = "SETUP_COLONIZING_RESTRICTED";

    public static final String FUEL_RANGE_NORMAL   = "SETUP_FUEL_RANGE_NORMAL";
    public static final String FUEL_RANGE_HIGH     = "SETUP_FUEL_RANGE_HIGH";
    public static final String FUEL_RANGE_HIGHER   = "SETUP_FUEL_RANGE_HIGHER";
    public static final String FUEL_RANGE_HIGHEST  = "SETUP_FUEL_RANGE_HIGHEST";
    
    public static final String RANDOMIZE_AI_NONE        = "SETUP_RANDOMIZE_AI_NONE";
    public static final String RANDOMIZE_AI_PERSONALITY = "SETUP_RANDOMIZE_AI_PERSONALITY";
    public static final String RANDOMIZE_AI_ABILITY     = "SETUP_RANDOMIZE_AI_ABILITY";
    public static final String RANDOMIZE_AI_BOTH        = "SETUP_RANDOMIZE_AI_BOTH";

    public static final String AI_HOSTILITY_LOWEST   = "SETUP_AI_HOSTILITY_LOWEST";
    public static final String AI_HOSTILITY_LOWER    = "SETUP_AI_HOSTILITY_LOWER";
    public static final String AI_HOSTILITY_LOW      = "SETUP_AI_HOSTILITY_LOW";
    public static final String AI_HOSTILITY_NORMAL   = "SETUP_AI_HOSTILITY_NORMAL";
    public static final String AI_HOSTILITY_HIGH     = "SETUP_AI_HOSTILITY_HIGH";
    public static final String AI_HOSTILITY_HIGHER   = "SETUP_AI_HOSTILITY_HIGHER";
    public static final String AI_HOSTILITY_HIGHEST  = "SETUP_AI_HOSTILITY_HIGHEST";
    
    public static final String OPPONENT_AI_BASE       = "SETUP_OPPONENT_AI_BASE";
    public static final String OPPONENT_AI_MODNAR     = "SETUP_OPPONENT_AI_MODNAR";
    public static final String OPPONENT_AI_XILMI      = "SETUP_OPPONENT_AI_XILMI";
    public static final String OPPONENT_AI_SELECTABLE = "SETUP_OPPONENT_AI_SELECT";
    
    public static final String AUTOPLAY_OFF           = "SETUP_AUTOPLAY_OFF";
    public static final String AUTOPLAY_AI_BASE       = "SETUP_AUTOPLAY_AI_BASE";
    public static final String AUTOPLAY_AI_MODNAR     = "SETUP_AUTOPLAY_AI_MODNAR";
    public static final String AUTOPLAY_AI_XILMI      = "SETUP_AUTOPLAY_AI_XILMI";
    
    public default boolean isAutoPlay()          { return !selectedAutoplayOption().equals(AUTOPLAY_OFF); }
    public default boolean communityAI()         { return false; }
    public default boolean selectableAI()        { return selectedOpponentAIOption().equals(OPPONENT_AI_SELECTABLE); }
    public default boolean usingExtendedRaces()  { return (selectedNumberOpponents()+1) > startingRaceOptions().size(); }
    public default void communityAI(boolean b)   { }
    public default int maxOpponents()            { return MAX_OPPONENTS; }
    public default float hostileTerraformingPct() { return 1.0f; }
    public default boolean restrictedColonization() { return selectedColonizingOption().equals(COLONIZING_RESTRICTED); }
    public default int baseAIRelationsAdj()       { return 0; }
    public default int selectedAI(Empire e)       { return AI.BASE; }
    public default boolean randomizeAIPersonality()  { 
        switch (selectedRandomizeAIOption()) {
            case RANDOMIZE_AI_PERSONALITY:
            case RANDOMIZE_AI_BOTH:
                return true;
            default:
                return false;
        }
    }
    public default boolean randomizeAIAbility()  { 
        switch (selectedRandomizeAIOption()) {
            case RANDOMIZE_AI_ABILITY:
            case RANDOMIZE_AI_BOTH:
                return true;
            default:
                return false;
        }
    }
    public String name();
    public void setToDefault();

    public int numberStarSystems();
    public int numberNebula();
    public default float nebulaSizeMult()                { return 1.0f; }
    public List<Integer> possibleColors();
    public float researchCostBase(int techLevel);
    public boolean canTradeTechs(Empire e1, Empire e2);
    public int warpSpeed(TechEngineWarp tech);
    public boolean allowRandomEvent(RandomEvent ev);
    public String randomStarType();
    public String randomPlayerStarType(Race r);
    public String randomRaceStarType(Race r);
    public String randomOrionStarType();
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
    public List<String> galaxyShapeOptions1();
    public List<String> galaxyShapeOptions2();
    public List<String> galaxyAgeOptions();
    public List<String> researchRateOptions();
    public List<String> techTradingOptions();
    public List<String> randomEventOptions();
    public List<String> warpSpeedOptions();
    public List<String> nebulaeOptions();
    public List<String> councilWinOptions();
    public List<String> starDensityOptions();
    public List<String> aiHostilityOptions();
    public List<String> planetQualityOptions();
    public List<String> terraformingOptions();
    public List<String> colonizingOptions();
    public List<String> fuelRangeOptions();
    public List<String> randomizeAIOptions();
    public List<String> autoplayOptions();
    public List<String> opponentAIOptions();
    public List<String> specificOpponentAIOptions();
	
    public List<String> gameDifficultyOptions();
    public int maximumOpponentsOptions();
    public int defaultOpponentsOptions();
    public List<String> startingRaceOptions();

    public String selectedGalaxySize();
    public void selectedGalaxySize(String s);
    public String selectedGalaxyShape();
    public void selectedGalaxyShape(String s);
    public String selectedGalaxyAge();
    public void selectedGalaxyAge(String s);
    public String selectedResearchRate();
    public void selectedResearchRate(String s);
    public String selectedTechTradeOption();
    public void selectedTechTradeOption(String s);
    public String selectedRandomEventOption();
    public void selectedRandomEventOption(String s);
    public String selectedWarpSpeedOption();
    public void selectedWarpSpeedOption(String s);
    public String selectedNebulaeOption();
    public void selectedNebulaeOption(String s);
    public String selectedCouncilWinOption();
    public void selectedCouncilWinOption(String s);
    public String selectedStarDensityOption();
    public void selectedStarDensityOption(String s);
    public String selectedAIHostilityOption();
    public void selectedAIHostilityOption(String s);
    public String selectedPlanetQualityOption();
    public void selectedPlanetQualityOption(String s);
    public String selectedTerraformingOption();
    public void selectedTerraformingOption(String s);
    public String selectedColonizingOption();
    public void selectedColonizingOption(String s);
    public String selectedFuelRangeOption();
    public void selectedFuelRangeOption(String s);
    public String selectedRandomizeAIOption();
    public void selectedRandomizeAIOption(String s);
    public String selectedOpponentAIOption();
    public void selectedOpponentAIOption(String s);
    public String specificOpponentAIOption(int empId);
    public void specificOpponentAIOption(String s, int empId);
    public String selectedAutoplayOption();
    public void selectedAutoplayOption(String s);
	
    public String selectedGalaxyShapeOption1();
    public void selectedGalaxyShapeOption1(String s);
    public String selectedGalaxyShapeOption2();
    public void selectedGalaxyShapeOption2(String s);

    public int numGalaxyShapeOption1();
    public int numGalaxyShapeOption2();
    
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

    default void copyOptions(IGameOptions opt) { }
    default boolean immediateCouncilWin()    { return selectedCouncilWinOption().equals(COUNCIL_IMMEDIATE); }
    default boolean noGalacticCouncil()      { return selectedCouncilWinOption().equals(COUNCIL_NONE); }
    default float fuelRangeMultiplier() {
        switch(selectedFuelRangeOption()) {
            case FUEL_RANGE_NORMAL: return 1;
            case FUEL_RANGE_HIGH: return 1.5f;
            case FUEL_RANGE_HIGHER: return 2;
            case FUEL_RANGE_HIGHEST: return 2.5f;
            default: return 1;
        }
    }
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
    default String nextGalaxyShapeOption1() {
        List<String> opts = galaxyShapeOptions1();
        if (opts.isEmpty())
            return "";
        int index = opts.indexOf(selectedGalaxyShapeOption1())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String prevGalaxyShapeOption1() {
        List<String> opts = galaxyShapeOptions1();
        if (opts.isEmpty())
            return "";
        int index = opts.indexOf(selectedGalaxyShapeOption1())-1;
        return index < 0 ? opts.get(opts.size()-1) : opts.get(index);
    }
    default String nextGalaxyShapeOption2() {
        List<String> opts = galaxyShapeOptions2();
        if (opts.isEmpty())
            return "";
        int index = opts.indexOf(selectedGalaxyShapeOption2())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String prevGalaxyShapeOption2() {
        List<String> opts = galaxyShapeOptions2();
        if (opts.isEmpty())
            return "";
        int index = opts.indexOf(selectedGalaxyShapeOption2())-1;
        return index < 0 ? opts.get(opts.size()-1) : opts.get(index);
    }
    default String nextGalaxyAge() {
        List<String> opts = galaxyAgeOptions();
        int index = opts.indexOf(selectedGalaxyAge())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
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
    default String nextOpponentAI() {
        List<String> opts = opponentAIOptions();
        int index = opts.indexOf(selectedOpponentAIOption())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String prevOpponentAI() {
        List<String> opts = opponentAIOptions();
        int index = opts.indexOf(selectedOpponentAIOption())-1;
        return index < 0 ? opts.get(opts.size()-1) : opts.get(index);
    }
    default String nextResearchRate() {
        List<String> opts = researchRateOptions();
        int index = opts.indexOf(selectedResearchRate())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String nextTechTradeOption() {
        List<String> opts = techTradingOptions();
        int index = opts.indexOf(selectedTechTradeOption())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String nextRandomEventOption() {
        List<String> opts = randomEventOptions();
        int index = opts.indexOf(selectedRandomEventOption())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String nextWarpSpeedOption() {
        List<String> opts = warpSpeedOptions();
        int index = opts.indexOf(selectedWarpSpeedOption())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String nextNebulaeOption() {
        List<String> opts = nebulaeOptions();
        int index = opts.indexOf(selectedNebulaeOption())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String nextCouncilWinOption() {
        List<String> opts = councilWinOptions();
        int index = opts.indexOf(selectedCouncilWinOption())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String nextStarDensityOption() {
        List<String> opts = starDensityOptions();
        int index = opts.indexOf(selectedStarDensityOption())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String nextAIHostilityOption() {
        List<String> opts = aiHostilityOptions();
        int index = opts.indexOf(selectedAIHostilityOption())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String nextPlanetQualityOption() {
        List<String> opts = planetQualityOptions();
        int index = opts.indexOf(selectedPlanetQualityOption())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String nextTerraformingOption() {
        List<String> opts = terraformingOptions();
        int index = opts.indexOf(selectedTerraformingOption())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String nextColonizingOption() {
        List<String> opts = colonizingOptions();
        int index = opts.indexOf(selectedColonizingOption())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String nextFuelRangeOption() {
        List<String> opts = fuelRangeOptions();
        int index = opts.indexOf(selectedFuelRangeOption())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String nextRandomizeAIOption() {
        List<String> opts = randomizeAIOptions();
        int index = opts.indexOf(selectedRandomizeAIOption())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default String nextAutoplayOption() {
        List<String> opts = autoplayOptions();
        int index = opts.indexOf(selectedAutoplayOption())+1;
        return index >= opts.size() ? opts.get(0) : opts.get(index);
    }
    default void nextSpecificOpponentAI(int i) {
        List<String> allAIs = specificOpponentAIOptions();
        String currAI = specificOpponentAIOption(i);
        
        int nextIndex = currAI == null ? 0 : allAIs.indexOf(currAI)+1;
        if (nextIndex >= allAIs.size())
            nextIndex = 0;
        
        String nextAI = allAIs.get(nextIndex);
        specificOpponentAIOption(nextAI, i);
    }
    default void prevSpecificOpponentAI(int i) {
        List<String> allAIs = specificOpponentAIOptions();
        String currAI = specificOpponentAIOption(i);
        
        int nextIndex = currAI == null ? 0 : allAIs.indexOf(currAI)-1;
        if (nextIndex < 0)
            nextIndex = allAIs.size()-1;
        
        String nextAI = allAIs.get(nextIndex);
        specificOpponentAIOption(nextAI, i);
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
