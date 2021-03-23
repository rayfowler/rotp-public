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
        
        // remove all systems in rebellion and poor/ultra-poor. None of these
        // systems will receive reserve. However poor/ultra-poor will be added
        // back in so we can collect reserve from them.
        // modnar: (???) poor/ultra-poor should be getting reserve to build factories
        // they are terrible to collect reserve from...
        // instead remove rich/ultra-rich from receiving reserves
        List<StarSystem> systems = new ArrayList<>(allSystems);
        for (StarSystem sys: allSystems) {
            if (sys.colony().inRebellion())
                systems.remove(sys);
            else {
                Planet pl = sys.planet();
                if (pl.isResourcePoor() || pl.isResourceUltraPoor()) {
                    poorSystems.add(sys); // modnar: still make poorSystems list
                }
                if (pl.isResourceRich() || pl.isResourceUltraRich()) {
                    systems.remove(sys);
                    richSystems.add(sys); // modnar: make richSystems list
                }
            }
        }
        
    
        // first, help systems that are fighting plague or supernova research events
        if (empire.totalReserve() > 0) {
            List<StarSystem> remainingSystems = new ArrayList<>(systems);
            for (StarSystem sys: remainingSystems) {
                if (empire.totalReserve() <= 0)
                    break;
                Colony col = sys.colony();
                if (col.research().hasProject()) {
                    int rsvNeeded = (int) col.maxReserveNeeded();
                    empire.allocateReserve(col, rsvNeeded);
                    systems.remove(sys);
                }
            }
        }
        
        // next, give reserve to systems designed as "rush defense" by the general
        if (empire.totalReserve() > 0) {
            List<StarSystem> needDefense = empire.generalAI().rushDefenseSystems();
            for (StarSystem sys: needDefense) {
                if (empire.totalReserve() <= 0)
                    break;
                Colony col = sys.colony();
                int rsvNeeded = (int) col.maxReserveNeeded();
                empire.allocateReserve(col, rsvNeeded);
                systems.remove(sys);
            }
        }
        
        // next, give reserve to systems designed as "rush build ships" by the fleet commander
        if (empire.totalReserve() > 0) {
            List<StarSystem> needShips = empire.generalAI().rushShipSystems();
            for (StarSystem sys: needShips) {
                if (empire.totalReserve() <= 0)
                    break;
                Colony col = sys.colony();
                int rsvNeeded = (int) col.maxReserveNeeded();
                empire.allocateReserve(col, rsvNeeded);
                systems.remove(sys);
            }
        }
    
        // sort remaining colonies from smallest to largest
        // assist in prod to build factories on smaller colonies
        if (empire.totalReserve() > 0) {
            List<StarSystem> remainingSystems = new ArrayList<>(systems);
            Collections.sort(remainingSystems,StarSystem.BASE_PRODUCTION);
            Collections.reverse(remainingSystems);
            for (StarSystem sys : remainingSystems) {
                if (empire.totalReserve() <= 0)
                    break;
                Colony col = sys.colony();
                float max = col.industry().maxSpendingNeeded();
                float curr = col.maxProduction();
                int rsvNeeded = (int) max(0, min(col.maxReserveUseable(), max-curr));
                if (rsvNeeded > 0) {
                    empire.allocateReserve(col,rsvNeeded);
                    systems.remove(sys);
                }
            }
        }
    }
}
