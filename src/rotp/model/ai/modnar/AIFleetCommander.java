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
package rotp.model.ai.modnar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.model.ai.interfaces.FleetCommander;
import rotp.model.ai.FleetOrders;
import rotp.model.ai.FleetPlan;
import rotp.model.ai.ShipDecision;
import rotp.model.ai.ShipPlan;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.galaxy.Galaxy;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesignLab;
import rotp.ui.NoticeMessage;
import rotp.util.Base;

public class AIFleetCommander implements Base, FleetCommander {
    private static final int DEFAULT_SIZE = 25;
    private static final float MAX_ALLOWED_SHIP_MAINT = 0.35f;
    private final Empire empire;

    private boolean sendColonyMissions;
    private final List<FleetPlan> fleetPlans;
    private final List<Integer> systems;
    private final List<Integer> systemsCommitted;
    private transient boolean canBuildShips = true;

    private List<FleetPlan> fleetPlans()      { return fleetPlans; }
    private List<Integer> systems()           { return systems;  }
    private List<Integer> systemsCommitted()  {return systemsCommitted;  }
    public AIFleetCommander (Empire c) {
        empire = c;
        sendColonyMissions = true; 
        fleetPlans = new ArrayList<>(DEFAULT_SIZE);
        systems = new ArrayList<>(DEFAULT_SIZE);
        systemsCommitted = new ArrayList<>(DEFAULT_SIZE);
    }
    @Override
    public String toString()   { return concat("FleetCommander: ", empire.raceName()); }
    @Override
    public void nextTurn() {
        if (empire.isAIControlled()) {
            log(toString(), ": nextTurn");
            empire.shipLab().needColonyShips = false;
            empire.shipLab().needExtendedColonyShips = false;
            sendColonyMissions = !empire.shipLab().colonyDesign().obsolete();
            canBuildShips = empire.shipMaintCostPerBC() < MAX_ALLOWED_SHIP_MAINT;
            NoticeMessage.setSubstatus(text("TURN_FLEET_PLANS"));
            buildFleetPlans();
            fillFleetPlans();
//            for (StarSystem sys: empire.allColonizedSystems()) {
//                ColonyShipyard ship = sys.colony().shipyard();
//                if (ship.building())
//                    log("Building ", str(ship.desiredShips()), ":", ship.design().name(), " at ", empire.sv.name(sys.id));
//            }
        }
    }
    @Override
    public float maxShipMaintainance()   { return MAX_ALLOWED_SHIP_MAINT; }
    @Override
    public boolean inExpansionMode() {
        return ((empire.tech().shipRange() < 6) || (empire.contacts().isEmpty())); // modnar: keep in expansion mode if not in any contact
    }
    @Override
    public float transportPriority(StarSystem sv) {
        int id = sv.id;
        float pr = sv.colony().transportPriority();

        if (empire.sv.colony(id).inRebellion())
            return pr * 5;
        else if (empire.sv.isBorderSystem(id))
            return pr * 2;
        else if (empire.sv.isInnerSystem(id))
            return pr / 2;
        else
            return pr;
    }
    private void buildFleetPlans() {
        // clear existing fleet plans & planetary build queues
        fleetPlans().clear();
        systems().clear();
        systemsCommitted().clear();
        for (int id=0; id<empire.sv.count();id++) {
            empire.sv.clearFleetPlan(id);
            if (empire.sv.empire(id) == empire) {
                empire.sv.colony(id).shipyard().resetQueueData();
                if (canBuildShips)
                    systems.add(id);
            }
        }

        // allow AIGeneral to determine war plans first
        empire.generalAI().nextTurn();

        // make fleet plans for each unplanned system
        for (int id=0; id<empire.sv.count();id++) {
            reviseFleetPlan(id);
            if (empire.sv.hasFleetPlan(id) && !empire.sv.fleetPlan(id).isClear()) 
                fleetPlans.add(empire.sv.fleetPlan(id));
        }

        // fleets that are somehow trapped beyond scout range need to retreat
        setDistantFleetsToRetreat();
        
        // sort fleet plans by priority
        Collections.sort(fleetPlans, FleetPlan.PRIORITY);
    }
    private void fillFleetPlans() {
        if (empire.tech().topFuelRangeTech().range() > 8)
            empire.shipLab().needScouts = false;
        else if (empire.scanPlanets())
            empire.shipLab().needScouts = false;
            
        NoticeMessage.setSubstatus(text("TURN_DEPLOY_FLEETS"));
        // get fleet orders for each fleet
        List<ShipFleet> fleets = galaxy().ships.allFleets(empire.id);
        List<FleetOrders> fleetOrders = new ArrayList<>(fleets.size());
        for (ShipFleet fleet : fleets) {
            if (fleet.hasShips())
                fleetOrders.add(fleet.newOrders());
        }

        int numPlans = fleetPlans.size();
        int numShips = 0;
        for (FleetPlan fp: fleetPlans)
            numShips += fp.numNeededShips();

        int numComplete = 0;

        log("Fleet Plans to fill:  ", str(numPlans), "  ships: ", str(numShips));
        //for (FleetPlan fp: fleetPlans) 
        //    log(fp.fullName());          
        
        List<FleetPlan> retreatPlans = new ArrayList<>();
        for (FleetPlan fPlan: fleetPlans) {
            NoticeMessage.setSubstatus(text("TURN_DEPLOY_FLEET_X", str(numComplete), str(numPlans)));
            if (fPlan.priority() == FleetPlan.RETREAT) 
                retreatPlans.add(fPlan);
            else 
                assignShipsToFleetPlans(fPlan, fleetOrders);
            numComplete += 1;
        }

        //  if we have retreat plans (unlikely), process them
        if (!retreatPlans.isEmpty()) {
            NoticeMessage.setSubstatus(text("TURN_RETREAT_FLEETS"));
            for (FleetPlan fp: retreatPlans) {
                ShipFleet fleet = empire.sv.orbitingFleet(fp.destId);
                if (fleet != null) {
                    StarSystem dest = galaxy().system(fp.destId);
                    StarSystem safeSystem = empire.ai().shipCaptain().retreatSystem(dest);
                    log("Withdrawing fleet: ", fleet.toString(), " from: ", str(fp.destId), "  to: ", safeSystem.toString());
                    galaxy().ships.retreatFleet(fleet, safeSystem.id);
                }
            }
        }
    }
    private void assignShipsToFleetPlans(FleetPlan fPlan, List<FleetOrders> fleetOrders) {
        // remove all plans of this priority from future loops
        //  log(str(plansToMeet.size()), " fleets:", str(plansToMeet.get(0).priority()));
        // 1. for each plan, fill from fleets already at the dest
        ShipFleet destFleet = empire.sv.orbitingFleet(fPlan.destId);
        // subtract from plan the ships already at the dest
        if (destFleet != null)
            fPlan.subtract(destFleet.orders());
        // if this is a staged plan and is unfilled by fleet at staging point, all future ships go to staging point
        if (fPlan.isStaged() && fPlan.needsShips()) {
            ShipFleet stagingFleet = empire.sv.orbitingFleet(fPlan.stagingPointId);
            if (fPlan.canBeFilledBy(stagingFleet)) {
                fPlan.subtract(stagingFleet.orders());
				
				// modnar: if the fleet plan can be filled, fill and send out right away
				// may conflict with other priorities, but trade-off for combined multi-design fleets
				fPlan.fillFrom(stagingFleet);
			}
            else
                fPlan.switchToStagingPoint();
        }

        // 2. for each plans, deduct any existing fleet orders that are currently in transit to the dest
        for (FleetOrders orders : fleetOrders) {
            if (orders.destSysId == fPlan.currentDestId())
                fPlan.eliminateCommonShips(orders);
        }

        // 3. for unmet fleet plans, build list of remaining ship plans to meet
        List<ShipPlan> shipPlans = fPlan.shipPlans();

        // loop through ship design plans & orders, looking for the optimal match
        for (ShipPlan sPlan: shipPlans) {
            boolean finished = false;
            while (!finished) {
                // find the best decision for this plan
                ShipDecision bestDecision = shipPlans.get(0).decide(fleetOrders, systems, empire);
                // implement the best decision
                float prevNum = sPlan.num;
                bestDecision.implement(fleetOrders, systemsCommitted());
                // we are finished with this plan if all of its ship needs are met (num == 0)
                // or we are no longer able to meet any more needs (num == prevNum)
                finished = (sPlan.num == 0) || (sPlan.num == prevNum);
            }
        }
    }
    private void reviseFleetPlan(int sysId) {        
        Galaxy gal = galaxy();
        float scoutRange = empire.scoutRange();
        float shipRange = empire.shipRange();
        // if out of scout range, forget it
        if (!empire.sv.withinRange(sysId, scoutRange)) 
            return;
        // if a fleet plan has already been assigned (by AIGeneral), then skip
        if (empire.sv.fleetPlan(sysId).needsShips()) 
            return;
        // if guarded, forget it for now
        if (empire.sv.isGuarded(sysId)) 
            return;

        // if not scouted and owner still using scouts, send a scout
        // if it has no known missile bases or if it is an ally 
        if (!empire.sv.isScouted(sysId)) {
            if (empire.alliedWith(empire.sv.empId(sysId))
            || (empire.sv.bases(sysId) == 0)) 
                setScoutFleetPlan(sysId);
            return;
        }

        // if there is a non-allied colony here and no fleet plan is set
        // by the AIGeneral, then retreat any fleets we have there
        if (empire.sv.empire(sysId) != null) {
            if (!empire.sv.empire(sysId).alliedWith(empire.id)) {
                if (empire.sv.fleetPlan(sysId).isClear())
                    setRetreatFleetPlan(sysId);
            }
            return;
        }

        // Only UNCOLONIZED systems beyond this point
        
        // if we can colonize this, send a colony fleet if in ship range
        // else send colony if they can reach, other post a fighter/scout to watch
        if (empire.canColonize(gal.system(sysId).planet().type())) {
            if (empire.sv.withinRange(sysId, shipRange)) {
                empire.shipLab().needColonyShips = true;
                setColonyFleetPlan(sysId);
            }
            else{
                empire.shipLab().needExtendedColonyShips = true;
                if (empire.shipLab().colonyDesign().isExtendedRange())
                    setColonyFleetPlan(sysId);
                else
                    setScoutFleetPlan(sysId);
            }
            return;
        }

        // if we can't colonize this, station a fighter or scout to 'claim' the system
        if (!empire.canColonize(gal.system(sysId).planet().type())) {
            if (empire.sv.withinRange(sysId, shipRange))
                setClaimFleetPlan(sysId);
            else
                setScoutFleetPlan(sysId);
            return;
        }
    }
    private void setDistantFleetsToRetreat() {
        List<ShipFleet> fleets = galaxy().ships.notInTransitFleets(empire.id);
        float range = empire.tech().scoutRange();
        for (ShipFleet fl: fleets) {
            int sysId = fl.sysId();
            if (!empire.sv.withinRange(sysId, range))  {
                setRetreatFleetPlan(sysId);
                fleetPlans.add(empire.sv.fleetPlan(sysId));
            }
        }
    }
    private void setRetreatFleetPlan(int id) {
        empire.sv.fleetPlan(id).priority = FleetPlan.RETREAT;
    }
    private void setScoutFleetPlan (int id) {
        FleetPlan plan = empire.sv.fleetPlan(id);
        if (empire.sv.isScouted(id))
            plan.priority = FleetPlan.SCOUT_TO_EXPLORED;
        else {
            float closeRangeBonus = 100 - empire.sv.distance(id)/10;
            plan.priority = FleetPlan.SCOUT_TO_UNEXPLORED + closeRangeBonus;
        }

        if (empire.shipLab().scoutDesign().obsolete())
            return;

        if (empire.shipLab().needScouts)
            plan.addShips(empire.shipLab().scoutDesign(), 1);
        else if (empire.sv.inShipRange(id))
            plan.addShips(empire.shipLab().fighterDesign(), 1);
    }
    private void setColonyFleetPlan (int id) {
        if (!sendColonyMissions)
            return;

        Galaxy gal = galaxy();
        FleetPlan plan = empire.sv.fleetPlan(id);

        float value = empire.sv.currentSize(id);
        
        //increase value by 5 for each of our systems it is near, and 
        //decrease by 2 for alien systems. This is an attempt to encourage
        //colonization of inner colonies (easier to defend) even if they 
        //are not as good as outer colonies
        int[] nearbySysIds = empire.sv.galaxy().system(id).nearbySystems();
        for (int nearSysId: nearbySysIds) {
            int nearEmpId = empire.sv.empId(nearSysId);
            if (nearEmpId != Empire.NULL_ID) {
                if (nearEmpId == empire.id)
                    value += 5;
                else
                    value -= 2;
            }
        }
        // assume that we will terraform  the planet
        value += empire.tech().terraformAdj();
        //multiply *2 for artifacts, *3 for super-artifacts
        //value *= (1+empire.sv.artifactLevel(id));
        // modnar: artifactLevel returns 0/6/7 for normal/artifact/orion
        // replace with explicit *2 and *3
        if (empire.sv.isArtifact(id))
            value *= 2;
        else if (empire.sv.isOrionArtifact(id))
            value *= 3;
        
        if (empire.sv.isUltraRich(id))
            value *= 3;
        else if (empire.sv.isRich(id))
            value *= 2;
        else if (empire.sv.isPoor(id))
            value /= 2;
        else if (empire.sv.isUltraPoor(id))
            value /= 3;

        plan.priority = FleetPlan.COLONIZE + (value/10);

        if (!empire.shipLab().colonyDesign().obsolete()) {
            plan.addShips(empire.shipLab().colonyDesign(), 1);
            // if we are at war with a race that can reach this system, set staging point with fighter escort
            boolean needEscort = false;
            for (EmpireView ev: empire.enemyViews()) {
                float range = ev.spies().tech().shipRange();
                if (ev.empire().sv.withinRange(id, range))
                    needEscort = true;
            }
            log(empire.sv.name(id), ": setting Colony Plan: ", str(plan.priority), "  staged at:", str(plan.stagingPointId));
            if (needEscort) {
                ShipDesignLab lab = empire.shipLab();
                float speed = max(lab.colonyDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
                plan.stagingPointId = empire.alliedColonyNearestToSystem(gal.system(id), speed);
                log(empire.sv.name(id), ": setting escorted Colony Plan: ", str(plan.priority), "  staged at:", str(plan.stagingPointId));
                if (!empire.shipLab().fighterDesign().obsolete()) {
                    int numFighters = (int) max(1, value/60);
                    plan.addShips(empire.shipLab().fighterDesign(), numFighters);
                }
            }
        }
    }
    private void setClaimFleetPlan (int id) {
        StarSystem sys = galaxy().system(id);
        FleetPlan plan = empire.sv.fleetPlan(id);
        plan.priority = FleetPlan.CLAIM+empire.generalAI().invasionPriority(sys);

        if (empire.shipLab().fighterDesign().obsolete())
            return;
        plan.addShips(empire.shipLab().fighterDesign(), 1);
    }
}