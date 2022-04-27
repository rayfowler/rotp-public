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
package rotp.model.ai.xilmi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.model.ai.interfaces.Treasurer;
import rotp.model.colony.Colony;
import rotp.model.colony.ColonySpendingCategory;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import static rotp.model.game.IGameOptions.RANDOM_EVENTS_OFF;
import rotp.model.planet.Planet;
import rotp.util.Base;

public class AITreasurer implements Base, Treasurer {
    private final Empire empire;

    public AITreasurer (Empire c) {
        empire = c;
    }
    @Override
    public String toString()   { return concat("Treasurer: ", empire.raceName()); }
    @Override
    public void allocateReserve() {
        List<StarSystem> allSystems = empire.allColonizedSystems();
        List<StarSystem> poorSystems = new ArrayList<>();
        List<StarSystem> richSystems = new ArrayList<>(); // modnar: make richSystem list
        
        boolean needToSolveEvent = false;
        
        // remove all systems in rebellion and poor/ultra-poor. None of these
        // systems will receive reserve. However poor/ultra-poor will be added
        // back in so we can collect reserve from them.
        // modnar: (???) poor/ultra-poor should be getting reserve to build factories
        // they are terrible to collect reserve from...
        // instead remove rich/ultra-rich from receiving reserves
        float reserveGoal = 0;
        List<StarSystem> systems = new ArrayList<>(allSystems);
        for (StarSystem sys: allSystems) {
            if (sys.colony().inRebellion())
                systems.remove(sys);
            else {
                if (sys.colony().research().hasProject())
                    needToSolveEvent = true;
                Planet pl = sys.planet();
                if(!galaxy().options().selectedRandomEventOption().equals(RANDOM_EVENTS_OFF) && sys.colony().maxReserveUseable() > reserveGoal)
                    reserveGoal = sys.colony().maxReserveUseable();
                if (pl.isResourcePoor() || pl.isResourceUltraPoor()) {
                    poorSystems.add(sys); // modnar: still make poorSystems list
                }
                if (pl.isResourceRich() || pl.isResourceUltraRich()) {
                    //ail: If the system with the event is rich, we mustn't remove it from the list of receivers!
                    if(!sys.colony().research().hasProject() && sys.colony().currentProductionCapacity() >= 1.0f)
                    {
                        systems.remove(sys);
                        richSystems.add(sys); // modnar: make richSystems list
                    }
                }
            }
        }
        
        //keep 5 times of what our biggest planet could use as reserve to handle events
        reserveGoal *= 5;
        
        //System.out.print("\n"+empire.name()+" wanted reserve: "+reserveGoal+" current reserve: "+empire.totalReserve());
        
        // first, help systems that are fighting plague or supernova research events
        if (empire.totalReserve() > 0) {
            List<StarSystem> remainingSystems = new ArrayList<>(systems);
            for (StarSystem sys: remainingSystems) {
                if (empire.totalReserve() <= 0)
                    break;
                Colony col = sys.colony();
                if (col.research().hasProject()) {
                    needToSolveEvent = true;
                    int rsvNeeded = (int) col.maxReserveNeeded();
                    empire.allocateReserve(col, rsvNeeded);
                    //ail: might be able to lower it due to more income meaning less percent for clean needed
                    col.lowerECOToCleanIfEcoComplete();
                    systems.remove(sys);
                }
            }
        }
        
        // sort remaining colonies from smallest to largest
        // assist in prod to build factories on smaller colonies
        if (!needToSolveEvent) {
            List<StarSystem> remainingSystems = new ArrayList<>(systems);
            Collections.sort(remainingSystems,StarSystem.CAPACITY);
            for (StarSystem sys : remainingSystems) {
                if (empire.totalReserve() <= 0)
                    break;
                Colony col = sys.colony();
                float max = col.industry().maxSpendingNeeded();
                float curr = col.maxProduction();
                int rsvNeeded = (int) max(0, min(col.maxReserveUseable(), max-curr));
                //since I recently learned that artifact-worlds always benefit from reserve:
                if(sys.planet().researchAdj() > 1)
                    rsvNeeded = (int)col.maxReserveUseable();
                //if we somehow have more than what we should keep as reserve, we'll spend it even on planets that are finished developing or have no research-bonus
                if(empire.totalReserve() > reserveGoal)
                    rsvNeeded = (int)col.maxReserveUseable();
                if (rsvNeeded > 0) {
                    empire.allocateReserve(col,rsvNeeded);
                    //ail: might be able to lower it due to more income meaning less percent for clean needed
                    col.lowerECOToCleanIfEcoComplete();
                    systems.remove(sys);
                }
            }
        }
        //We only gather tax when we need to deal with a nova/plague
        if(needToSolveEvent && empire.totalReserve() < reserveGoal)
        {
            if(empire.divertColonyExcessToResearch())
                empire.toggleColonyExcessToResearch();
            float totalProd = empire.totalPlanetaryProduction();
            float desiredRsv = totalProd * 0.1f; // modnar: reduce reserve collection, less is more efficient in general
            int maxAlloc = ColonySpendingCategory.MAX_TICKS; 
            if (empire.totalReserve() < desiredRsv) {
                systems.addAll(richSystems); // modnar: check for richSystems first to collect reserve
                Collections.reverse(systems);
                for (StarSystem sys : systems) {
                    Colony col = sys.colony();
                    //ail: The colony that has the project that we are saving for shouldn't be one of those who are saving!
                    if(col.research().hasProject())
                        continue;
                    int res = col.research().allocation();
                    if (res > 0) {
                        Planet pl = sys.planet();
                        if (pl.isResourceRich() || pl.isResourceUltraRich()) {
                            if (col.population() >= pl.maxSize()) {
                                int eco = col.ecology().allocation();
                                int ecoAdj = min(res, maxAlloc - eco); // modnar: collect all we can from rich/ultra-rich
                                col.research().adjustValue(-ecoAdj);
                                col.ecology().adjustValue(ecoAdj);                            
                            }
                        } else if (!pl.isArtifact()) {
                            if (col.industry().factories() >= col.industry().maxFactories()) {
                                int ind = col.industry().allocation();
                                int indAdj = min(res, maxAlloc - ind)/4; // ail: 1/6 wasn't enough to prevent the nova
                                col.research().adjustValue(-indAdj);
                                col.industry().adjustValue(indAdj);                            
                            }                        
                        }
                    }
                }
            }
        }
    }
}
