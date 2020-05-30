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
package rotp.model.ai.community;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import rotp.model.ai.FleetPlan;
import rotp.model.ai.interfaces.General;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.galaxy.Galaxy;
import rotp.model.galaxy.Ship;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesignLab;
import rotp.util.Base;

public class AICGeneral implements Base, General {
    private final Empire empire;
    private float civProd = 0;
    private final HashMap<StarSystem, List<Ship>> targetedSystems;
    private final List<StarSystem> rushDefenseSystems;
    private final List<StarSystem> rushShipSystems;

    public AICGeneral (Empire c) {
        empire = c;
        targetedSystems = new HashMap<>();
        rushDefenseSystems = new ArrayList<>();
        rushShipSystems = new ArrayList<>();
    }
    private HashMap<StarSystem, List<Ship>> targetedSystems() { return targetedSystems; }
    @Override
    public List<StarSystem> rushDefenseSystems() { return rushDefenseSystems; }
    @Override
    public List<StarSystem> rushShipSystems() { return rushShipSystems; }
    @Override
    public String toString()   { return concat("General: ", empire.raceName()); }
    @Override
    public boolean inWarMode()  { return empire.numEnemies() > 0; }
    @Override
    public void nextTurn() {
        civProd = empire.totalPlanetaryProduction();
        resetTargetedSystems();
        rushDefenseSystems.clear();
        rushShipSystems.clear();

        Galaxy gal = galaxy();
        for (int id=0;id<empire.sv.count();id++)
            reviseFleetPlan(gal.system(id));
    }
    
    // Value to take planet, in pop-multiplied units
    public float takePlanetValue(StarSystem sys) {
        int sysId = sys.id;
        if (!empire.sv.inShipRange(sysId))  return 0.0f;
        if (!empire.sv.isScouted(sysId))    return 0.0f;
        if (!empire.sv.isColonized(sysId))  return 0.0f;
        
        float val = empire.sv.currentSize(sysId);

        if (empire.sv.isUltraPoor(sysId))
            val *= 0.25;
        else if (empire.sv.isPoor(sysId))
            val *= 0.5;
        else if (empire.sv.isResourceNormal(sysId))
            val *= 1;
        else if (empire.sv.isRich(sysId))
            val *= 2;
        else if (empire.sv.isUltraRich(sysId))
            val *= 3;

        //float for artifacts, triple for super-artifacts
        if (empire.sv.isArtifact(sysId))
            val *= 2;
        else if (empire.sv.isOrionArtifact(sysId))
            val *= 3;

        return val;
    }
    @Override
    public float invasionPriority(StarSystem sys) {
        int sysId = sys.id;
        if (!empire.sv.inShipRange(sysId))  return 0.0f;
        if (!empire.sv.isScouted(sysId))    return 0.0f;
        if (!empire.sv.isColonized(sysId))  return 0.0f;
        if (!empire.canColonize(sys.planet()))  return 0.0f;

        float pr = empire.sv.currentSize(sysId);

        if (empire.sv.isPoor(sysId))
            pr *= 2;
        else if (empire.sv.isResourceNormal(sysId))
            pr *= 3;
        else if (empire.sv.isRich(sysId))
            pr *= 4;
        else if (empire.sv.isUltraRich(sysId))
            pr *= 5;

        //float for artifacts, triple for super-artifacts
        if (empire.sv.isArtifact(sysId))
            pr *= 2;
        else if (empire.sv.isOrionArtifact(sysId))
            pr *= 3;
        pr /= Math.sqrt(max(1,empire.sv.distance(sysId)));
        pr /= Math.sqrt(max(1,empire.sv.bases(sysId)));
        return pr/10;
    }
    public void reviseFleetPlan(StarSystem sys) {
        int sysId = sys.id;
        
        // if out of ship range, ignore
        if (!empire.sv.inShipRange(sysId))
            return;

        boolean enemyFleetInOrbit = false;
        float enemyFleetSize = 0.0f;
        List<ShipFleet> fleets = sys.orbitingFleets();
        for (ShipFleet fl: fleets) {
            if (!fl.empire().alliedWith(empire.id) && !fl.empire().pactWith(empire.id)) {
                enemyFleetInOrbit = true;
                if (fl.isArmed())
                    enemyFleetSize += fl.bcValue();
            }
        }

        // for uncolonized systems
        if (!empire.sv.isColonized(sysId)) {
            if (enemyFleetInOrbit)
                setExpelFleetPlan(sys, enemyFleetSize);
            return;
        }

        // for our systems
        if (empire == empire.sv.empire(sysId)) {
            if (sys.hasEvent()) {
                if (sys.eventKey().equals("MAIN_PLANET_EVENT_PIRACY")) {
                    setExpelPiratesPlan(sys);
                    return;
                }
                if (sys.eventKey().equals("MAIN_PLANET_EVENT_COMET")) {
                    setDestroyCometPlan(sys, false);
                    return;
                }
            }
            float value = invasionPriority(sys);
            if (sys.colony().inRebellion())
                orderRebellionFleet(sys, enemyFleetSize);
            else if (enemyFleetInOrbit)
                setRepelFleetPlan(sys, enemyFleetSize);
            else if (targetedSystems.keySet().contains(sys))
                setInterceptFleetPlan(sys, enemyFleetSize);
            else if (empire.sv.isAttackTarget(sysId))
                setMinimumFighterGuard(sys, FleetPlan.GUARD_ATTACK_TARGET+value);
            else if (empire.sv.isBorderSystem(sysId))
                setMinimumFighterGuard(sys, FleetPlan.GUARD_BORDER_COLONY+value);
            else
                setMinimumFighterGuard(sys, FleetPlan.GUARD_INNER_COLONY+value);
            return;
        }

        EmpireView ev = empire.viewForEmpire(empire.sv.empId(sysId));
        
        // help our allies with the comet
        if (ev.embassy().alliance()) {
            if (sys.eventKey().equals("MAIN_PLANET_EVENT_COMET")) {
                setDestroyCometPlan(sys, true);
                return;
            }
        }

        // for empires we are at war with.. we always invade or bomb
        if (ev.embassy().anyWar() || ev.embassy().onWarFooting()) {
            if (willingToInvade(ev, sys))
                orderInvasionFleet(ev, sys, enemyFleetSize);
            else
                orderBombardmentFleet(ev, sys, enemyFleetSize);
            return;
        }

        // protect allied colonies from enemy fleets
        if (ev.embassy().alliance()) {
            if (enemyFleetInOrbit)
                setAssistAllyFleetPlan(sys, enemyFleetSize);
            return;
        }
        
        // for empires with no treaty
        if (ev.embassy().noTreaty()) {
            if (ev.embassy().encroaching(sys))
                orderBombEncroachmentFleet(ev, sys, enemyFleetSize);
            else
                considerSneakAttackFleet(ev, sys, enemyFleetSize);
        }
    }
    public boolean willingToInvade(EmpireView v, StarSystem sys) {
        if (!empire.canSendTransportsTo(sys))
            return false;
        float pop = empire.sv.population(sys.id);
        float needed = troopsNecessaryToTakePlanet(v, sys);   
        // Willing to take 2:1 odds on a 100-pop planet, 4:1 if it's rich, etc.
        float value = takePlanetValue(sys) / 50;
        return needed < pop * value;
    }
    public void orderRebellionFleet(StarSystem sys, float enemyFleetSize) {
        if (enemyFleetSize == 0)
            launchRebellionTroops(sys);
        else
            setRepelFleetPlan(sys, enemyFleetSize);      
    }
    public void orderInvasionFleet(EmpireView v, StarSystem sys, float enemyFleetSize) {
        if (empire.sv.hasFleetForEmpire(sys.id, empire))
            launchGroundTroops(v, sys, 1);
        else if (empire.combatTransportPct() > 0)
            launchGroundTroops(v, sys, 1/empire.combatTransportPct());

        float baseBCPresent = empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost();
        float bcMultiplier = 1 + (empire.sv.hostilityLevel(sys.id));
        float bcNeeded = (baseBCPresent*4) +bcMultiplier*civProd/10;
        
        // use up to half of BC for Destroyers... rest for fighters
        int destroyersNeeded = (int) Math.ceil((bcNeeded/2)/empire.shipLab().destroyerDesign().cost());
        bcNeeded -= (destroyersNeeded * empire.shipLab().destroyerDesign().cost());
        int fightersNeeded = (int) Math.ceil(bcNeeded/empire.shipLab().fighterDesign().cost());

        // set fleet orders for invasion...
        ShipDesignLab lab = empire.shipLab();
        float speed = max(lab.destroyerDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.addShips(empire.shipLab().destroyerDesign(), destroyersNeeded);
        fp.addShips(empire.shipLab().fighterDesign(), fightersNeeded);
        if (v.embassy().finalWar()) 
            fp.priority = FleetPlan.INVADE_FINAL_WAR+ invasionPriority(sys)/100;
        else
            fp.priority = FleetPlan.INVADE + invasionPriority(sys)/100;
        fp.stagingPointId = empire.optimalStagingPoint(sys, speed);
    }
    public void launchGroundTroops(EmpireView v, StarSystem target, float mult) {
        //float troops0 = troopsNecessaryToBypassBases(target);
        float troops1 = mult*troopsNecessaryToTakePlanet(v, target);
        int alreadySent = empire.transportsInTransit(target);
        float troopsDesired = troops1 + empire.sv.currentSize(target.id) - alreadySent;

        if (troopsDesired < 1)
            return;

        List<StarSystem> allSystems = empire.allColonizedSystems();
        List<StarSystem> launchPoints = new ArrayList<>();
        StarSystem.TARGET_SYSTEM = target;
        Collections.sort(allSystems,StarSystem.DISTANCE_TO_TARGET_SYSTEM);

        float troopsAvailable = 0;
        float maxTravelTime = 0;

        for (StarSystem sys : allSystems) {
            if (troopsAvailable < troopsDesired) {
                float travelTime = sys.colony().transport().travelTime(target);
                // only consider systems within 10 travel turns
                if ((travelTime <= 10) && sys.colony().canTransport()) {
                    launchPoints.add(sys);
                    maxTravelTime = max(maxTravelTime, travelTime);
                    troopsAvailable += sys.colony().maxTransportsAllowed();
                }
            }
        }

        //not enough troops to take planet! switch to defense
        if (troopsAvailable < troops1)
            return;

        for (StarSystem sys: launchPoints)
            maxTravelTime = max(maxTravelTime, sys.colony().transport().travelTime(target));

        // send transports from launch points
        for (StarSystem sys : launchPoints) {
            int troops = sys.colony().maxTransportsAllowed();
            sys.colony().scheduleTransportsToSystem(target, troops, maxTravelTime);
        }
    }
    public void launchRebellionTroops(StarSystem target) {
        float troops1 =  target.colony().rebels()*2;
        int alreadySent = empire.transportsInTransit(target);
        float troopsDesired = troops1 - alreadySent;

        if (troopsDesired < 1)
            return;

        List<StarSystem> allSystems = empire.allColonizedSystems();
        List<StarSystem> launchPoints = new ArrayList<>();
        StarSystem.TARGET_SYSTEM = target;
        Collections.sort(allSystems,StarSystem.DISTANCE_TO_TARGET_SYSTEM);

        float troopsAvailable = 0;

        for (StarSystem sys : allSystems) {
            if (troopsAvailable < troopsDesired) {
                if (sys.colony().canTransport()) {
                    launchPoints.add(sys);
                    troopsAvailable += sys.colony().maxTransportsAllowed();
                }
            }
        }

        // send transports from launch points
        for (StarSystem sys : launchPoints) {
            int troops = sys.colony().maxTransportsAllowed();
            sys.colony().scheduleTransportsToSystem(target, troops);
        }
    }
    public float troopsNecessaryToBypassBases(StarSystem sys) {
        return empire.sv.bases(sys.id) * troopToEnemyBaseRatio(sys);
    }
    public float troopToEnemyBaseRatio(StarSystem sys) {
        int id = sys.id;
        EmpireView ev = empire.viewForEmpire(empire.sv.empire(id));
        return ev.spies().tech().weapon().techLevel() / empire.tech().construction().techLevel();
    }
    public float troopsNecessaryToTakePlanet(EmpireView ev, StarSystem sys) {
        int id = sys.id;
        return empire.sv.population(id) * (50 + ev.spies().tech().troopCombatAdj(true)) / (50 + empire.tech().troopCombatAdj(false));
    }
    public void orderBombardmentFleet(EmpireView v, StarSystem sys, float fleetSize) {
        float baseBCPresent = empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost();
        // set fleet orders for bombardment...
        float bcMultiplier = 1 + (empire.sv.hostilityLevel(sys.id)/2);
        float bcNeeded = (baseBCPresent*4)+bcMultiplier*civProd/20;
        int fightersNeeded = (int) Math.ceil(bcNeeded/empire.shipLab().fighterDesign().cost());
        int bombersNeeded = (int) Math.ceil(4*bcNeeded/empire.shipLab().bomberDesign().cost());

        ShipDesignLab lab = empire.shipLab();
        float speed = max(lab.bomberDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.addShips(empire.shipLab().bomberDesign(), bombersNeeded);
        fp.addShips(empire.shipLab().fighterDesign(), fightersNeeded);
        fp.stagingPointId = empire.optimalStagingPoint(sys, speed);
        if (v.embassy().finalWar()) 
            fp.priority = FleetPlan.BOMB_FINAL_WAR+ invasionPriority(sys)/100;
        else
            fp.priority = FleetPlan.BOMB_ENEMY+ invasionPriority(sys)/100;
    }
    public void orderBombEncroachmentFleet(EmpireView v, StarSystem sys, float fleetSize) {
        // set fleet orders for bombardment...
        float bcMultiplier = 1 + (empire.sv.hostilityLevel(sys.id)/2);
        float bcNeeded = bcMultiplier*civProd/20;
        int fightersNeeded = (int) Math.ceil(bcNeeded/empire.shipLab().fighterDesign().cost());
        int bombersNeeded = (int) Math.ceil(bcNeeded/empire.shipLab().bomberDesign().cost());

        ShipDesignLab lab = empire.shipLab();
        float speed = max(lab.bomberDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.addShips(empire.shipLab().bomberDesign(), bombersNeeded);
        fp.addShips(empire.shipLab().fighterDesign(), fightersNeeded);
        fp.stagingPointId = empire.optimalStagingPoint(sys, speed);
        fp.priority = FleetPlan.BOMB_ENCROACHMENT;
    }
    public void considerSneakAttackFleet(EmpireView v, StarSystem sys, float fleetSize) {
        // pacifist/honorable never sneak attack
        if (empire.leader().isPacifist()
        || empire.leader().isHonorable())
            return;

        float baseChance = 0.3f - (empire.numEnemies()*0.3f);
        if (empire.leader().isAggressive())
            baseChance += 0.6f;
        else if (empire.leader().isDiplomat())
            baseChance -= 0.2f;
        else if (empire.leader().isRuthless())
            baseChance += 0.3f;

        // lower sneak attack chance on planet we can't capture
        if (!empire.canColonize(sys.planet().type()))
                baseChance -= 0.3f;

        float value = (empire.sv.factories(sys.id) * 10);
        float cost = fleetSize + (empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost());
        float bonus = -0.5f + (value / (value+cost));

        if ((baseChance+bonus) > 0.5)  {
            orderBombardmentFleet(v, sys, fleetSize);
            empire.sv.fleetPlan(sys.id).priority = FleetPlan.BOMB_UNDEFENDED;
        }
    }
    private void setMinimumFighterGuard(StarSystem sys, float priority) {
        float basesNeeded = empire.sv.desiredMissileBases(sys.id) - empire.sv.bases(sys.id);
        if (basesNeeded <= 0) 
            return;
        float baseCost = empire.tech().newMissileBase().cost(empire);
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.priority = priority;
        // one base BC = 10 fighter BC
        fp.addShipBC(empire.shipLab().fighterDesign(), basesNeeded*baseCost/10);
    }
    private void setRepelFleetPlan(StarSystem sys, float fleetSize) {
        float baseBCPresent = empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost();
        float bcNeeded = max(empire.shipLab().fighterDesign().cost(), fleetSize*10);
        bcNeeded -= baseBCPresent;
        if (bcNeeded <= 0)
            return;
        
        rushDefenseSystems.add(sys);

        // use up to half of BC for Destroyers... rest for fighters
        int destroyersNeeded = (int) Math.ceil((bcNeeded/2)/empire.shipLab().destroyerDesign().cost());
        bcNeeded -= (destroyersNeeded * empire.shipLab().destroyerDesign().cost());
        int fightersNeeded = (int) Math.ceil(bcNeeded/empire.shipLab().fighterDesign().cost());

        ShipDesignLab lab = empire.shipLab();
        float speed = max(lab.destroyerDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.priority = FleetPlan.REPEL + invasionPriority(sys)/100;
        fp.stagingPointId = empire.optimalStagingPoint(sys, speed);
        fp.addShips(empire.shipLab().destroyerDesign(), destroyersNeeded);
        fp.addShips(empire.shipLab().fighterDesign(), fightersNeeded);
    }
    private void setInterceptFleetPlan(StarSystem sys, float fleetSize) {
        float baseBCPresent = empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost();
        float bcNeeded = max(empire.shipLab().fighterDesign().cost(), fleetSize*6);
        bcNeeded -= baseBCPresent;
        if (bcNeeded <= 0)
            return;

        rushDefenseSystems.add(sys);
        // use up to half of BC for Destroyers... rest for fighters
        int destroyersNeeded = (int) Math.ceil((bcNeeded/2)/empire.shipLab().destroyerDesign().cost());
        bcNeeded -= (destroyersNeeded * empire.shipLab().destroyerDesign().cost());
        int fightersNeeded = (int) Math.ceil(bcNeeded/empire.shipLab().fighterDesign().cost());

        ShipDesignLab lab = empire.shipLab();
        float speed = max(lab.destroyerDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.priority = FleetPlan.INTERCEPT + invasionPriority(sys)/100;
        fp.addShips(empire.shipLab().destroyerDesign(), destroyersNeeded);
        fp.addShips(empire.shipLab().fighterDesign(), fightersNeeded);
    }
    private void setExpelFleetPlan(StarSystem sys, float fleetSize) {
        float baseBCPresent = empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost();
        float bcNeeded = max(empire.shipLab().fighterDesign().cost(), fleetSize*10);
        bcNeeded -= baseBCPresent;
        if (bcNeeded <= 0)
            return;

        // use up to half of BC for Destroyers... rest for fighters
        int destroyersNeeded = (int) Math.ceil((bcNeeded/2)/empire.shipLab().destroyerDesign().cost());
        bcNeeded -= (destroyersNeeded * empire.shipLab().destroyerDesign().cost());
        int fightersNeeded = (int) Math.ceil(bcNeeded/empire.shipLab().fighterDesign().cost());

        ShipDesignLab lab = empire.shipLab();
        float speed = max(lab.destroyerDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.priority = FleetPlan.EXPEL + invasionPriority(sys)/100;
        fp.stagingPointId = empire.optimalStagingPoint(sys, speed);
        fp.addShips(empire.shipLab().destroyerDesign(), destroyersNeeded);
        fp.addShips(empire.shipLab().fighterDesign(), fightersNeeded);
    }
    private void setExpelPiratesPlan(StarSystem sys) {
        float bcNeeded = 300;
        int fightersNeeded = (int) Math.ceil(bcNeeded/empire.shipLab().fighterDesign().cost());

        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.priority = FleetPlan.EXPEL;
        fp.addShips(empire.shipLab().fighterDesign(), fightersNeeded);
    }
    private void setDestroyCometPlan(StarSystem sys, boolean alliance) {
        float bcNeeded = alliance ? 10000 : 20000;
        
        int destroyersNeeded = (int) Math.ceil(bcNeeded/empire.shipLab().destroyerDesign().cost());
        // fighters are not much help here but we want to at least rush any available fighters there
        // while destroyers are potentially being constructed
        int fightersNeeded = (int) Math.ceil(0.02f*bcNeeded/empire.shipLab().fighterDesign().cost());

        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.priority = alliance ? FleetPlan.ASSIST_ALLY : FleetPlan.REPEL;
        fp.addShips(empire.shipLab().destroyerDesign(), destroyersNeeded);
        fp.addShips(empire.shipLab().fighterDesign(), fightersNeeded);
    }
    private void setAssistAllyFleetPlan(StarSystem sys, float fleetSize) {
        float baseBCPresent = empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost();
        float bcNeeded = max(empire.shipLab().fighterDesign().cost(), fleetSize*5);
        bcNeeded -= baseBCPresent;
        if (bcNeeded <= 0)
            return;

        // use up to half of BC for Destroyers... rest for fighters
        int destroyersNeeded = (int) Math.ceil((bcNeeded/2)/empire.shipLab().destroyerDesign().cost());
        bcNeeded -= (destroyersNeeded * empire.shipLab().destroyerDesign().cost());
        int fightersNeeded = (int) Math.ceil(bcNeeded/empire.shipLab().fighterDesign().cost());

        ShipDesignLab lab = empire.shipLab();
        float speed = max(lab.destroyerDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.priority = FleetPlan.ASSIST_ALLY + invasionPriority(sys)/100;
        fp.stagingPointId = empire.optimalStagingPoint(sys, speed);
        fp.addShips(empire.shipLab().destroyerDesign(), destroyersNeeded);
        fp.addShips(empire.shipLab().fighterDesign(), fightersNeeded);
    }
    private void resetTargetedSystems() {
        Set<StarSystem> systems = targetedSystems().keySet(); // re-inits
        for (StarSystem s: systems)
            targetedSystems.get(s).clear();

        Galaxy gal = galaxy();
        for (Ship ship: empire.visibleShips()){
            if (ship.inTransit() && empire.aggressiveWith(ship.empId())) {
                if (empire.knowETA(ship)) {
                    StarSystem dest = gal.system(ship.destSysId());
                    if (!targetedSystems.containsKey(dest))
                        targetedSystems.put(dest, new ArrayList<>());
                    targetedSystems.get(dest).add(ship);
                }
            }
        }
    }
}