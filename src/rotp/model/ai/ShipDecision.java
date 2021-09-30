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
import java.util.List;
import rotp.model.colony.ColonyShipyard;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesign;
import rotp.util.Base;

public class ShipDecision implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public ShipPlan shipPlan;
    public int sysId = StarSystem.NULL_ID;
    public FleetOrders fleetOrders;
    public float shipCurrPriority = Float.MAX_VALUE;
    public float turns;
    public ShipDesign bestDesign;

    public void reset() {
        shipPlan = null;
        sysId = StarSystem.NULL_ID;
        fleetOrders = null;
        turns = Integer.MAX_VALUE;
    }
    public float turns() {
        return turns;
    }
    public boolean multiTurn() {
        return turns() > 1;
    }
    private boolean hasSystem()   { return sysId != StarSystem.NULL_ID; }
    @Override
    public String toString() {
        String design = bestDesign == null ? "null" : bestDesign.name();
        String orders = fleetOrders == null ? "null" : fleetOrders.toString();
        if (sysId == StarSystem.NULL_ID)
            return concat("ShipDecision: ", design, " in ", str((int)Math.ceil(turns)), " turns using: ", orders);
        else
            return concat("ShipDecision: ", design, " in ", str((int)Math.ceil(turns)), " turns from: ", str(sysId));
    }
    public boolean implement(List<FleetOrders> orders, List<Integer> systemsCommitted) {
        if ((fleetOrders == null) && !hasSystem()) {
            shipPlan.plan.removeShipPlan(shipPlan);
            return false;
        }

        if (hasSystem()) {
            buildShipPlanOnPlanet();
            if (!systemsCommitted.contains(sysId))
                systemsCommitted.add(sysId);
        }
        else 
            assignFleetOrdersToPlan(orders);

        // if satisfied, remove ship from fleet plan to eliminate from future decisions
        if ((shipPlan.num <= 0) && (shipPlan.bc <= 0)) {
            shipPlan.plan.removeShipPlan(shipPlan);
        }
        return true;
    }
    public void assignFleetOrdersToPlan(List<FleetOrders> orders) {
        // send ship in fleet orders to fleetPlan dest
        int numRequired = numShipsRequired(shipPlan.num, shipPlan.bc, bestDesign);
        int numAvail = fleetOrders.numAvailable(bestDesign);
        int numShips = min(numRequired, numAvail);

        if (numShips == 0)
            return;

        // reassign fleet if possible (may currently be in transit to another location)
        if (fleetOrders.owningFleet.canSend() && fleetOrders.owningFleet.canSendDesignTo(bestDesign, shipPlan.plan.destId)) {
            if (shipPlan.plan.goToStagingPoint) 
                fleetOrders.reassignShip(bestDesign, shipPlan.plan.currentDestId(), numShips);
            else
                fleetOrders.reassignShip(bestDesign, shipPlan.plan.destId, numShips);
        }

        bestDesign.addUsedCount(numShips);
        shipPlan.num -= numShips;
        shipPlan.bc -= (numShips * bestDesign.cost());
        sysId = StarSystem.NULL_ID;

        // remove associated fleet orders from orders list if it has no more ships to reassign
        if (!fleetOrders.hasShips())
            orders.remove(fleetOrders);
    }
    public static int numShipsRequired(int num, float bc, ShipDesign d) {
        int shipsByBC = (int) Math.ceil(bc / d.cost());
        return Math.max(num, shipsByBC);
    }
    public void buildShipPlanOnPlanet() {
        StarSystem sys = galaxy().system(sysId);
        ColonyShipyard shipyard = sys.colony().shipyard();
        // if chosen planet is not building a ship, set the design
        if (!shipyard.building())
            shipyard.design(shipPlan.design);

        // update queue data to properly evaluate future plans
        shipyard.addQueuedBC(shipPlan.design.cost());

        if (shipyard.design() == shipPlan.design)
            shipyard.addDesiredShips(1);

        shipPlan.num--;
        shipPlan.bc -= shipPlan.design.cost();
        
        if (shipPlan.plan.priority() >= FleetPlan.INTERCEPT) 
            sys.empire().generalAI().rushShipSystems().add(sys);
    }
}
