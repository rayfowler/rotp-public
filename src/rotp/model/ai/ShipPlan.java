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
import java.util.Comparator;
import java.util.List;
import rotp.model.colony.Colony;
import rotp.model.empires.Empire;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesign;
import rotp.util.Base;

public class ShipPlan implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public FleetPlan plan;
    public ShipDesign design;
    public int num = 0;
    public float bc = 0;
    private ShipDecision decision;

    public static Comparator<ShipPlan> PRIORITY = (ShipPlan pl1, ShipPlan pl2) -> Base.compare(pl1.priority(), pl2.priority());
    public ShipPlan(ShipDesign d, FleetPlan p, int n) {
        design = d;
        plan = p;
        num = n;
    }
    public String name() {
        return concat(str(num), " ", design.name(), "(", str(bc), ") to ", str(plan.destId));
    }
    public float priority() {
        return plan.priority();
    }
    private float travelTimeFrom(float x, float y, ShipDesign d) {
        StarSystem dest = galaxy().system(plan.destId);
        return dest.distanceTo(x,y) / d.engine().warp();
    }
    private void checkIfMatch(FleetOrders orders) {
        if (orders.num(design.id()) > 0) {
            ShipFleet fl = orders.owningFleet;
            float dist = fl.empire().sv.distance(plan.destId);
            if (design.validMission(plan.destId) && (design.range() > dist) ) {
                StarSystem dest = galaxy().system(plan.destId);
                //ail: Fix for https://www.reddit.com/r/rotp/comments/mpw4qr/base_ai_producing_way_too_many_scouts/
                //Description of how this fixes it in comments
                int turns = (int)Math.ceil(travelTimeFrom(fl.x(), fl.y(), design));
                if (turns < decision.turns) {
                    float priority = 0;
                    if (fl.inOrbit()) {
                        // get the fleet plan for the system that this fleet is currently in so we can understand the
                        // priority of the system fleet plan we are potentially pulling ships away from
                        FleetPlan systemPlan = fl.empire().sv.view(fl.sysId()).fleetPlan();
                        if (plan.overlaps(systemPlan))
                            priority = systemPlan.priority;
                    }
                    decision.turns = turns;
                    decision.fleetOrders = orders;
                    decision.bestDesign = design;
                    decision.shipCurrPriority = priority;
                }
                else if ((turns == decision.turns) && (decision.shipCurrPriority > 0)) {
                   float priority = 0;
                    if (fl.inOrbit()) {
                        // get the fleet plan for the system that this fleet is currently in so we can understand the
                        // priority of the system fleet plan we are potentially pulling ships away from
                        FleetPlan systemPlan = fl.empire().sv.view(fl.sysId()).fleetPlan();
                        if (plan.overlaps(systemPlan))
                            priority = systemPlan.priority;
                    }
                    if (priority < decision.shipCurrPriority) {
                        decision.turns = turns;
                        decision.fleetOrders = orders;
                        decision.bestDesign = design;
                        decision.shipCurrPriority = priority;  
                    }
                }
            }
        }
    }
    public ShipDecision decide(List<FleetOrders> orders, List<Integer> systems, Empire emp) {
        if (decision == null)
            decision = new ShipDecision();

        decision.reset();
        decision.shipPlan = this;

        // find closest fleet orders, time-wise. Fleet orders must contain a ship in this plan
        for (FleetOrders next : orders)
            checkIfMatch(next);

        // don't bother trying to build if we can get there within 1 turn
        if (decision.turns <= 1)
            return decision;

        
        // this is the advantage to use existing ships in orbit rather than
        // build them unless they are this mnay turns faster
        int existingShipTurnAdvantage = 3;
        int designCost = design.cost();
        // find fastest planet, build & time-wise. Planet must be willing to build (i.e. not already committed to a different design)
        for (int id: systems) {
            StarSystem sys = galaxy().system(id);
            Colony col = sys.colony();
            if (emp.governorAI().readyToBuild(col, this, designCost)) {
                float buildTurns = (float)Math.ceil(col.shipyard().turnsToBuild(design));
                float travelTime = travelTimeFrom(sys.x(), sys.y(), design);
                float nextTurns = buildTurns+travelTime+existingShipTurnAdvantage;
                if (nextTurns < decision.turns) {
                    decision.turns = nextTurns;
                    decision.sysId = sys.id;
                    decision.bestDesign = design;
                }
            }
        }
        return decision;
    }
}
