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
package rotp.model.ai;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.util.Base;

public class FleetPlan implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public static final int MAX_TURNS = 9999;
    public static Comparator<FleetPlan> PRIORITY = (FleetPlan o1, FleetPlan o2) -> Base.compare(o2.priority(),o1.priority());
    public int destId;
    public int empireId;
    public int stagingPointId = StarSystem.NULL_ID;
    public boolean goToStagingPoint = false;
    public List<ShipPlan> shipPlans = new ArrayList<>();
    private final int[] needed = new int[ShipDesignLab.MAX_DESIGNS];
    public float priority;
    public boolean inSynch = false;
    private transient ShipDesignLab lab;

    // PRIORITY VALUES
    public static final int RETREAT = 9999;  // highest priority since these ships MUST move somewhere else
    public static final int BOMB_FINAL_WAR = 5500;
    public static final int INVADE_FINAL_WAR = 5000;
    public static final int QUASH_REBELLION = 2000;
    public static final int REPEL = 1500;   // defending a colony under attack
    public static final int INTERCEPT = 1400;   // defending a colony under attack
    public static final int ASSIST_ALLY = 1300;   // ally colony under attack
    public static final int INVADE = 1200;
    public static final int BOMB_ENEMY = 1100;
    public static final int COLONIZE = 1000;
    public static final int SCOUT_TO_UNEXPLORED = 900;
    public static final int BOMB_ENCROACHMENT = 800;    // sneak attack bombing missions against untreatied systems that have encroaching fleets
    public static final int GUARD_ATTACK_TARGET = 700;  // send guard fleet to systems in range of enemy attacks
    public static final int EXPEL = 600;                // expel enemy fleets from nearby uncolonized systemss
    public static final int BOMB_UNDEFENDED = 500;      // sneak attack bombing msisions against untreatied systems
    public static final int GUARD_BORDER_COLONY = 400;
    public static final int GUARD_INNER_COLONY = 300;
    public static final int CLAIM = 200;
    public static final int SCOUT_TO_EXPLORED = 100;
    public static final int NO_MISSION = 0;

    public FleetPlan(int empId, int sysId) {
        empireId = empId;
        destId = sysId;
    }
    @Override
    public String toString() {
        String name = str(destId);
        if (name.isEmpty())
            name = concat("Unexplored (", str(destId), ")");
        return concat(str(priority()), ":", name, " - ");
    }
    private ShipDesignLab lab() {
        if (lab == null)
            lab = galaxy().empire(empireId).shipLab();
        return lab;
    }
    public float priority()       { return priority; }
    public boolean isClear()       { return priority == NO_MISSION; }
    public boolean isStaged()      { return (stagingPointId != StarSystem.NULL_ID); }
    public void clear() {
        priority = NO_MISSION;
        stagingPointId = StarSystem.NULL_ID;
        goToStagingPoint = false;
        for (int i=0;i<needed.length;i++)
            needed[i]=0;
    }
    public String fullName() {
        String name = concat("fp ", str(destId), " pri:", str(priority), "  ships:");
        //check that otherFleet has matching stacks with enough ships & BC
        for (int i=0;i<needed.length;i++) {
            if (needed[i]> 0)
                name = concat(name, str(needed[i]), "-", lab().design(i).name(), ";");
        }
        return name;
    }
    public int currentDestId() {
        return goToStagingPoint && isStaged() ? stagingPointId : destId;
    }
    public boolean overlaps(FleetPlan fp) {
        for (int i=0;i<needed.length;i++) {
            if ((needed[i] > 0) && (fp.needed[i] > 0)) 
                return true;
        }
        return false;
    }
    public void switchToStagingPoint() {
        if (isStaged())
            goToStagingPoint = true;
    }
    public void subtract(FleetOrders orders) {
        if (orders.destSysId == currentDestId())
            eliminateCommonShips(orders);
    }
    public boolean canBeFilledBy(ShipFleet fleet) {
        if (fleet == null)
            return false;

        //check that otherFleet has matching stacks with enough ships & BC
        for (int i=0;i<needed.length;i++) {
            if (needed[i] > fleet.num(i) )
                return false;
        }
        return true;
    }
    public void fillFrom(ShipFleet fleet) {
        ShipDesign bestDesign;
        FleetOrders orders = fleet.orders();

        for (int i=0;i<needed.length;i++) {
            if (needed[i] > 0) {
                boolean stackFilled = false;
                int neededMission = lab.design(i).mission();
                while (!stackFilled){
                    bestDesign = orders.newestOfType(neededMission);
                    if (bestDesign != null) {
                        int numAvail = orders.numAvailable(bestDesign);
                        int numShips = min(needed[i], numAvail);
                        if (numShips > 0) {
                            orders.reassignShip(bestDesign, destId, numShips);
                            needed[i] -= numShips;
                        }
                    }
                    stackFilled = ((bestDesign == null) || (needed[i]<=0));
                }
            }
        }
    }
    public int numNeededShips() {
        int num = 0;
        for (int i=0;i<needed.length;i++)
            num += needed[i];
        return num;
    }
    public List<ShipPlan> shipPlans() {
        shipPlans.clear();
        for (int i=0;i<needed.length;i++) {
            if (needed[i] > 0) {
                ShipDesign design = lab().design(i);
                if (design != null) {
                    ShipPlan pl = new ShipPlan(design, this, needed[i]);
                    shipPlans.add(pl);
                }
            }
        }
        return shipPlans;
    }
    public void removeShipPlan(ShipPlan sh) {
        // a shipPlan (1 ship in a fleetPlan) has been met by a ship in this FleetOrders
        for (int i=0;i<needed.length;i++) {
            if (lab().design(i) == sh.design) {
                needed[i] = max(0, needed[i]-1);
                return;
            }
        }
    }
    public void addShips(ShipDesign design, int num) {
        if (lab().design(design.id()) != design) 
            log("Error: adding ship with invalid design");
        
        needed[design.id()] += num;
    }
    public void addShipBC(ShipDesign design, float bc) {
        int num = (int) Math.ceil(bc/design.cost());
        addShips(design, num);
    }
    public boolean needsShips()  {
        for (int i=0;i<needed.length;i++) {
            if (needed[i] > 0)
                return true;
        }
        return false;
    }
    public void eliminateCommonShips(FleetOrders orders) {
        // this is used for the FleetPlan, FleetOrders logic
        for (int designId=0;designId<needed.length;designId++) {
            if (needed[designId] > 0) {
                int available = orders.num(designId);
                if (available > 0) {
                    ShipDesign planDesign = lab().design(designId);
                    if (planDesign.validMission(destId)) {
                        int numToUse = min(needed[designId], available);
                        planDesign.addUsedCount(numToUse);
                        needed[designId] -= numToUse;
                        orders.removeShips(designId, numToUse);
                    }
                }
            }
        }
    }
}
