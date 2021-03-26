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
import rotp.model.galaxy.StarType;
import rotp.model.ships.ShipDesign;
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
    private transient float maxMaintenance;

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
    public float maxShipMaintainance() {
        if (maxMaintenance < 0) 
            maxMaintenance = max(0.025f, empire.tech().avgTechLevel() / 100.0f);
        return maxMaintenance;
    }
    @Override
    public void nextTurn() {
        if (empire.isAIControlled()) {
            log(toString(), ": nextTurn");
            empire.shipLab().needColonyShips = false;
            empire.shipLab().needExtendedColonyShips = false;
            maxMaintenance = -1;
            sendColonyMissions = !empire.shipLab().colonyDesign().obsolete();
            canBuildShips = true; //since we build only colonizers and scouts here, this should always be possible
            NoticeMessage.setSubstatus(text("TURN_FLEET_PLANS"));
            buildFleetPlans();
            fillFleetPlans();
            handleMilitary();
        }
    }
    @Override
    public boolean inExpansionMode() {
        for(EmpireView contact : empire.contacts())
        {
            if(empire.inShipRange(contact.empId()))
            {
                return false;
            }
        }
        if((empire.tech().planetology().techLevel() > 19 || empire.ignoresPlanetEnvironment())
                    && empire.shipLab().needScouts == false)
        {
            return false;
        }
        return true;
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
    //ail: using completely different approach for handling my attack-fleets
    private StarSystem findBestTarget(ShipFleet fleet, List<StarSystem> ignore, boolean onlyBomberTargets, boolean onlyColonizerTargets)
    {
        Galaxy gal = galaxy();
        StarSystem best = null;
        float bestScore = 0.0f;
        for (int id=0;id<empire.sv.count();id++) 
        {
            StarSystem current = gal.system(id);
            if(ignore.contains(current))
            {
                continue;
            }
            if(!fleet.canReach(current))
            {
                continue;
            }
            boolean handleEvent = false;

            //ignore Orion as long as tech-level is below 40
            if(current.monster() != null && empire.tech().avgTechLevel() < 40)
            {
                continue;
            }
            float score = 0.0f;
            float baseBc = 0.0f;
            float transports = 0.0f;
            float enemyBc = 0.0f;
            float enemyBombardDamage = 0.0f;
            float bombardDamage = 0.0f;
            float bc = 0.0f;
            for(ShipFleet incoming : current.incomingFleets())
            {
                if(incoming.empire().aggressiveWith(empire.id))
                {
                    if(!empire.visibleShips().contains(incoming))
                        continue;
                    enemyBombardDamage += incoming.expectedBombardDamage(current);
                    enemyBc += incoming.bcValue();
                }
                if(incoming.empire() == fleet.empire())
                {
                    bombardDamage += incoming.expectedBombardDamage(current);
                    if(incoming.isArmed() || incoming.hasColonyShip())
                        bc += incoming.bcValue();
                }
            }
            if(empire.sv.isColonized(id))
            {
                score = 10; //max(10, current.colony().production());
                baseBc = empire.sv.bases(current.id)*current.empire().tech().newMissileBaseCost();
                if(onlyColonizerTargets)
                {
                    continue;
                }
            }
            //if we don't have scouts anymore, we still need a way to uncover new systems
            else if(!empire.sv.isScouted(id) && bc == 0 )
            {
                score = 1.0f;
            }
            else if(fleet.canColonizeSystem(current))
            {
                score = 2.0f;
                score *= current.planet().maxSize() / 100.0f;
                score *= (1+empire.sv.artifactLevel(id));
                if (empire.sv.isUltraRich(id))
                    score *= 3;
                else if (empire.sv.isRich(id))
                    score *= 2;
                else if (empire.sv.isPoor(id))
                    score /= 2;
                else if (empire.sv.isUltraPoor(id))
                    score /= 3;
            }
            for(ShipFleet orbiting : current.orbitingFleets())
            {
                if(orbiting.retreating())
                    continue;
                if(orbiting.empire().aggressiveWith(fleet.empId()))
                {
                    if(!empire.visibleShips().contains(orbiting))
                        continue;
                    enemyBombardDamage += orbiting.expectedBombardDamage();
                    enemyBc += orbiting.bcValue();
                }
                if(orbiting.empire() == fleet.empire())
                {
                    bombardDamage += orbiting.expectedBombardDamage();
                    if(orbiting.isArmed())
                        bc += orbiting.bcValue();
                }
            }
            if(current.colony() != null)
            {
                transports += empire.enemyTransportsInTransit(current) * empire.maxRobotControls();
            }
            if(empire.alliedWith(empire.sv.empId(id)))
            {
                if (current.empire() == empire && current.hasEvent()) {
                    if (current.eventKey().equals("MAIN_PLANET_EVENT_PIRACY")) {
                        handleEvent = true;
                    }
                    if (current.eventKey().equals("MAIN_PLANET_EVENT_COMET")) {
                        handleEvent = true;
                    }
                }
                if(current.colony() != null && enemyBombardDamage > current.colony().untargetedHitPoints() && fleet.travelTime(current) > 1)
                {
                    score = 1.0f;
                }
                if(enemyBombardDamage == 0 && transports == 0 && !handleEvent)
                {
                    continue;
                }
                if(onlyBomberTargets && !handleEvent)
                {
                    continue;
                }
            }
            else
            {
                if(empire.sv.isColonized(id))
                {
                    if(!empire.enemies().contains(current.empire()))
                    {
                        continue;
                    }
                    if(bombardDamage > current.colony().untargetedHitPoints() && fleet.system() != current)
                    {
                        continue;
                    }
                }
            }
            if(bombardDamage > 0 && fleet.system() != current)
            {
                score *= Math.max(1 - (bombardDamage / current.colony().untargetedHitPoints()), 0.0f);
            }
            if(enemyBc + baseBc > 0 && fleet.system() != current)
            {
                score *= Math.min((fleet.bcValue()) / (enemyBc + baseBc), 4.0f);
            }
            else 
            {
                score *= 4.0;
                if (bc > 0 && fleet.sysId() != current.id && (current.empire() == null || empire.alliedWith(empire.sv.empId(id))))
                {
                    score /= bc;
                }
            }
            if(handleEvent)
            {
                score = Float.MAX_VALUE;
            }
            score /= fleet.travelTime(current) + 1;
            //System.out.print("\n"+fleet.empire().name()+" Fleet at "+empire.sv.name(fleet.system().id)+" => "+empire.sv.name(current.id)+" score: "+score);
            if(score > bestScore)
            {
                bestScore = score;
                best = current;
            }
        }
        return best;
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
            if (fPlan.canBeFilledBy(stagingFleet)) 
                fPlan.subtract(stagingFleet.orders());
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
        float scoutRange = empire.scoutRange();
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
        if (!empire.sv.isScouted(sysId) && empire.shipLab().needScouts) { //once we no longer use scouts, we do this in another way
            if (empire.alliedWith(empire.sv.empId(sysId))
            || (empire.sv.bases(sysId) == 0)) 
                setScoutFleetPlan(sysId);
        }
    }
    private void setDistantFleetsToRetreat() {
        List<ShipFleet> fleets = galaxy().ships.notInTransitFleets(empire.id);
        float range = empire.tech().scoutRange();
        for (ShipFleet fl: fleets) {
            int sysId = fl.sysId();
            if (!empire.sv.withinRange(sysId, range))  {
                //ail: no retreating, if I can still bomb the enemy
                if(empire.enemies().contains(fl.system().empire()) && fl.expectedBombardDamage() > 0)
                {
                    continue;
                }
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
        value *= (1+empire.sv.artifactLevel(id));
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
    //ail: Entirely new way of handling the military
    private void handleMilitary()
    {
        //ail: when we have colonizers but don't know we need any, we send them with our attacks, so they can colonize the bombed system also this should allow to scout with initial colonizer
        float civTech = empire.tech().avgTechLevel();
        for(ShipFleet fleet:empire.allFleets())
        {
            //If we have made peace and war again, we disable potential retreatOnArrival
            if(fleet.destination() != null && fleet.retreatOnArrival() && empire.enemies().contains(fleet.destination().empire()))
            {
                fleet.toggleRetreatOnArrival();
            }
            if(!fleet.canSend() || fleet.deployed())
            {
                continue;
            }
            else
            {
                boolean canStillSend = true;
                boolean notEnoughFighters = false;
                List<StarSystem> sentToThisRound = new ArrayList<>();
                while(canStillSend)
                {
                    float attackThreshold = 0.625f;
                    boolean allowFighters = true;
                    boolean allowBombers = true;
                    boolean allowColonizers = true;
                    float sendAmount = 1.0f;
                    boolean onlyBomberTargets = false;
                    boolean onlyColonizerTargets = false;
                    
                    if(fleet.numFighters() == 0 || notEnoughFighters)
                        onlyBomberTargets = true;
                    if(fleet.numFighters() == 0 && fleet.numBombers() == 0)
                        onlyColonizerTargets = true;

                    StarSystem target = findBestTarget(fleet, sentToThisRound, onlyBomberTargets, onlyColonizerTargets);
                    if(target != null)
                    {
                        if(target == fleet.system())
                        {
                            canStillSend = false;
                            continue;
                        }
                        StarSystem stagingPoint = null;
                        float fleetSpeed = fleet.slowestStackSpeed();
                        stagingPoint = galaxy().system(empire.optimalStagingPoint(target, fleetSpeed));
                        //ail: When we have hyperspace-communications, we don't want to go to a staging-point for our new target.
                        if(fleet.inTransit())
                            stagingPoint = null;
                        if(stagingPoint != null
                            && fleet.system() != stagingPoint 
                            && fleet.travelTurns(target) > (int)Math.ceil(fleet.travelTime(stagingPoint, target, fleetSpeed)) 
                            && fleet.travelTurns(target) > fleet.travelTurns(stagingPoint)
                            && !onlyColonizerTargets) //colonizers fly directly, no need to stage
                        {
                            //System.out.print("\n"+fleet.empire().name()+" Fleet at "+fleet.system().name()+" going to "+target.name()+" stages at: "+stagingPoint.name());
                            if(fleet.canSendTo(stagingPoint.id))
                            {
                                attackWithFleet(fleet, stagingPoint, 1.0f, false, allowFighters, allowBombers, allowColonizers);
                            }
                            canStillSend = false;
                        }
                        else
                        {
                            float enemyBC = 0.0f;
                            boolean needToGuess = false;
                            float targetTech = civTech;
                            for(ShipFleet orbiting : target.orbitingFleets())
                            {
                                if(orbiting.retreating())
                                    continue;
                                if(orbiting.empire().aggressiveWith(fleet.empId()))
                                {
                                    if(!empire.visibleShips().contains(orbiting))
                                    {
                                        needToGuess = true;
                                        continue;
                                    }
                                    EmpireView ev = empire.viewForEmpire(orbiting.empId());
                                    targetTech = ev.spies().tech().avgTechLevel(); // modnar: target tech level
                                    enemyBC += orbiting.bcValue();
                                }
                            }
                            if(target.monster() != null)
                            {
                                enemyBC += 100000;
                            }
                            for(ShipFleet incoming : target.incomingFleets())
                            {
                                if(incoming.empire().aggressiveWith(empire.id))
                                {
                                    if(!empire.visibleShips().contains(incoming))
                                        continue;
                                    EmpireView ev = empire.viewForEmpire(incoming.empId());
                                    targetTech = ev.spies().tech().avgTechLevel(); // modnar: target tech level
                                    enemyBC += incoming.bcValue();
                                }
                            }
                            if(target.empire() != null)
                            {
                                if(empire.alliedWith(target.empId()) && (enemyBC > 0 || empire.enemyTransportsInTransit(target) > 0))
                                {
                                    allowBombers = false;
                                    allowColonizers = false;
                                    attackThreshold = 1.0f;
                                    if(bcValue(fleet, false, allowFighters, allowBombers, allowColonizers) > 0)
                                    {
                                        sendAmount = min(1.0f, (empire.enemyTransportsInTransit(target) * empire.maxRobotControls() + enemyBC) * 4 / bcValue(fleet, false, allowFighters, allowBombers, allowColonizers));
                                    }
                                    else
                                    {
                                        sendAmount = 1.0f;
                                    }
                                    if (target.hasEvent()) {
                                        if (target.eventKey().equals("MAIN_PLANET_EVENT_PIRACY")
                                                || target.eventKey().equals("MAIN_PLANET_EVENT_COMET")) {
                                            sendAmount = 1.0f;
                                        }
                                    }
                                }
                                else
                                {
                                    //ail: if we can't see the system, assume there's at least a fair share of ships for defense
                                    if(needToGuess)
                                        enemyBC = max(enemyBC, target.empire().totalFleetCost() * target.colony().production() / target.empire().totalIncome());
                                    enemyBC += empire.sv.bases(target.id)*target.empire().tech().newMissileBaseCost();
                                    EmpireView ev = empire.viewForEmpire(empire.sv.empId(target.id));
                                    if(ev != null)
                                    {
                                        targetTech = ev.spies().tech().avgTechLevel(); // modnar: target tech level
                                    }
                                    if(fleet.expectedBombardDamage(target) > 0)
                                    {
                                        sendAmount = min(1.0f, target.colony().untargetedHitPoints() / fleet.expectedBombardDamage(target));
                                    }
                                    else
                                    {
                                        sendAmount = 1.0f;
                                    }
                                }
                            }
                            else
                            {
                                if(fleet.canColonizeSystem(target) && target.monster() == null)
                                {
                                    allowColonizers = true;
                                    sendAmount = 0.01f;
                                }
                            }
                            if(!empire.sv.isScouted(target.id))
                            {
                                sendAmount = 0.01f;
                            }
                            if(target.monster() != null)
                            {
                                allowBombers = false;
                            }
                            if(bcValue(fleet, false, allowFighters, allowBombers, allowColonizers) > 0)
                            {
                                sendAmount = max(sendAmount, min(1.0f, enemyBC*(targetTech+10.0f) / (bcValue(fleet, false, allowFighters, allowBombers, allowColonizers)*(civTech+10.0f)* attackThreshold)));
                            }
                            else
                            {
                                sendAmount = 0.0f;
                                canStillSend = false;
                            }
                            //System.out.print("\n"+fleet.empire().name()+" Fleet at "+fleet.system().name()+" should attack "+target.name()+" "+bcValue(fleet, false, allowFighters, allowBombers, allowColonizers)+":"+enemyBC+" sendAmount: "+sendAmount);
                            //System.out.print("\n"+fleet.empire().name()+" Fleet at "+fleet.system().name()+" should attack "+target.name()+" HP "+target.colony().getHitPoints() +" Bomb-Dmg: "+fleet.expectedBombardDamage(target)*sendAmount);
                            //ail: if we have Hyperspace-communications, we can't split
                            if(fleet.inTransit())
                            {
                                sendAmount = 1.0f;
                                allowFighters = true;
                                allowBombers = true;
                                allowColonizers = true;
                            }
                            if(bcValue(fleet, false, allowFighters, allowBombers, allowColonizers)*(civTech+10.0f) * attackThreshold > enemyBC*(targetTech+10.0f))
                            {
                                if(fleet.canSendTo(target.id))
                                {
                                    int numBeforeSend=fleet.numShips();
                                    attackWithFleet(fleet, target, sendAmount, false, allowFighters, allowBombers, allowColonizers);
                                    if(sendAmount >= 1.0f || numBeforeSend == fleet.numShips())
                                    {
                                        //System.out.print("\n"+fleet.empire().name()+" Fleet at "+fleet.system().name()+" should attack "+target.name()+" allowBombers: "+allowBombers);
                                        canStillSend = false;
                                    }
                                    else
                                    {
                                        sentToThisRound.add(target);
                                    }
                                    //System.out.print("\n"+fleet.empire().name()+" Fleet at "+fleet.system().name()+" has been sent "+target.name()+" sent: "+sendAmount);
                                }
                                else
                                {
                                    canStillSend = false;
                                }
                            }
                            else if(stagingPoint != null
                                && fleet.system() != stagingPoint 
                                && fleet.travelTurns(target) > (int)Math.ceil(fleet.travelTime(stagingPoint, target, fleet.slowestStackSpeed())))
                            {
                                if(fleet.canSendTo(stagingPoint.id))
                                {
                                    attackWithFleet(fleet, stagingPoint, 1.0f, false, allowFighters, allowBombers, allowColonizers);
                                }
                                canStillSend = false;
                            }
                            else
                            {
                                //Fleet too small to attack and no staging-point found either.
                                if(allowBombers == false &&  notEnoughFighters == false)
                                {
                                    notEnoughFighters = true;
                                }
                                else
                                {
                                    canStillSend = false;
                                }
                            }
                        }
                    }
                    else
                    {
                        canStillSend = false;
                    }
                    if(!fleet.hasShips())
                    {
                        canStillSend = false;
                    }
                }
            }
        }
    }
    
    public void attackWithFleet(ShipFleet fl, StarSystem target, float amount, boolean includeScouts, boolean includeFighters, boolean includeBombers, boolean includeColonizer)
    {
        boolean haveToDeploy = false;
        int[] counts = new int[ShipDesignLab.MAX_DESIGNS];
        ShipDesignLab lab = empire.shipLab();
        for (int i=0;i<fl.num.length;i++) {
            int num = fl.num(i);
            ShipDesign d = lab.design(i); 
            if(d.isScout()&& !includeScouts)
            {
                continue;
            }
            if(d.hasColonySpecial() && !includeColonizer)
            {
                continue;
            }
            if(d.isBomber() && !includeBombers)
            {
                continue;
            }
            if((d.isFighter() || d.isDestroyer()) && !includeFighters)
            {
                continue;
            }
            counts[i] = (int)Math.ceil(num * amount);
            if(counts[i] > 0)
                haveToDeploy = true;
        }
        if(haveToDeploy)
            galaxy().ships.deploySubfleet(fl, counts, target.id);
    }
    public float bcValue(ShipFleet fl, boolean countScouts, boolean countFighters, boolean countBombers, boolean countColonizers) {
        float bc = 0;
        ShipDesignLab lab = empire.shipLab();
        for (int i=0;i<fl.num.length;i++) {
            int num = fl.num(i);
            if (num > 0) {
                ShipDesign des = lab.design(i);
                if(des == null)
                    continue;
                if(des.isScout() && !countScouts)
                    continue;
                if(des.isBomber() && !countBombers)
                    continue;
                if(des.isFighter() && !countFighters)
                    continue;
                if(des.hasColonySpecial() && !countColonizers)
                    continue;
                bc += (num * des.cost());
            }
        }
        return bc;
    }

}