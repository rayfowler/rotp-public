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
	private float civTech = 0;
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
		civTech = empire.tech().avgTechLevel();
        resetTargetedSystems();
        rushDefenseSystems.clear();
        rushShipSystems.clear();

        Galaxy gal = galaxy();
        for (int id=0;id<empire.sv.count();id++)
            reviseFleetPlan(gal.system(id));
    }
    
	// modnar: adjustments to invasion valuation
    // Desire value to invade planet, factor in both planet size and factories
	// Higher desire value for Rich, Ultra-Rich, Artifacts
	// Lower desire value for Poor, Ultra-Poor
    public float takePlanetValue(StarSystem sys) {
        int sysId = sys.id;
        if (!empire.sv.inShipRange(sysId))  return 0.0f;
        if (!empire.sv.isScouted(sysId))    return 0.0f;
        if (!empire.sv.isColonized(sysId))  return 0.0f;
        
        float size = empire.sv.currentSize(sysId); // planet size
		float fact = empire.sv.factories(sysId); // factory count
		
		// increase planet value depending on size and factories (val is normalized below)
		// (4*pow(SIZE, 0.7) + min(20, sqrt(FACTORIES)))
		// min(20) is ballpark max invasion tech chances (400 factories)
		// 
		// Normal,   size-100,    0 factories:  val = 100
		// Normal,   size-100,  100 factories:  val = 110
		// Normal,   size-100,  200 factories:  val = 115
		// Normal,   size-100,  300 factories:  val = 118
		// Normal,   size-100,  400 factories:  val = 120
		// Normal,   size-140,  560 factories:  val = 147
		// Normal,   size-220, 1540 factories:  val = 194
		// Normal,   size-70,   140 factories:  val =  90
		// Normal,   size-70,   210 factories:  val =  93
		// Poor,     size-100,    0 factories:  val =  75
		// Poor,     size-100,  200 factories:  val =  86
		// Rich,     size-50,    50 factories:  val = 138
		// Artifact, size-80,   240 factories:  val = 203
		float val = (float) (4.0f*Math.pow(size, 0.7f) + Math.min(20.0f, Math.sqrt(fact)));

        // Higher desire value for Rich, Ultra-Rich, Artifacts
	    // Lower desire value for Poor, Ultra-Poor
		// modnar: increase values for poor/ultra-poor
        if (empire.sv.isUltraPoor(sysId))
            val *= 0.6;
        else if (empire.sv.isPoor(sysId))
            val *= 0.75;
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
		
		// modnar: killer instinct
		// higher value for the last few planets of an empire
		int remainingSystems = galaxy().empire(empire.sv.empId(sysId)).numColonies();
		if (remainingSystems <=4) {
			val *= ((remainingSystems + 7)/(remainingSystems + 1));
		}
		
        // normalized to normal size-100 planet with 200 factories (115)
        return val/115;
    }
    @Override
    public float invasionPriority(StarSystem sys) {
        int sysId = sys.id;
        if (!empire.sv.inShipRange(sysId))  return 0.0f;
        if (!empire.sv.isScouted(sysId))    return 0.0f;
        if (!empire.sv.isColonized(sysId))  return 0.0f;
        if (!empire.canColonize(sys.planet()))  return 0.0f;
		
		// increase invasion priority with planet size and factory count
        float pr = empire.sv.currentSize(sysId) + empire.sv.factories(sysId)/20.0f;
		
		// modnar: killer instinct
		// higher priority to take out the last few planets of an empire
		int remainingSystems = galaxy().empire(empire.sv.empId(sysId)).numColonies();
		if (remainingSystems <=3) {
			pr *= ((remainingSystems + 7)/(remainingSystems + 1));
		}
		
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
			
			// modnar: stop double fleetplan when enemy is in orbit of colony
            else if (empire.sv.isAttackTarget(sysId) && !enemyFleetInOrbit)
				// modnar: if under attack, more fighters on top of missle bases
                setHighFighterGuard(sys, FleetPlan.GUARD_ATTACK_TARGET+value);
            else if (empire.sv.isBorderSystem(sysId))
				// modnar: if on border, slightly more fighters
                setNormalFighterGuard(sys, FleetPlan.GUARD_BORDER_COLONY+value);
            else
				// modnar: for inner system, minimum fighter guard adjusted lower
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
		// modnar: scale back willingness to take losses
        // Willing to take 1.1:1 losses to invade normal 100-pop size planet with 200 factories.
		// For invading normal 80-pop size planet with 320 factories, be willing to take ~1:1 losses.
        float value = takePlanetValue(sys) * 1.1f;
        return needed < pop * value;
    }
    public void orderRebellionFleet(StarSystem sys, float enemyFleetSize) {
        if (enemyFleetSize == 0)
            launchRebellionTroops(sys);
        else
            setRepelFleetPlan(sys, enemyFleetSize);      
    }
    public void orderInvasionFleet(EmpireView v, StarSystem sys, float enemyFleetSize) {
		// modnar: scale up invasion multiplier with factories
		// to account for natural pop growth (enemy transport, etc.) with invasion troop travel time
		float size = empire.sv.currentSize(sys.id); // planet size
		float fact = empire.sv.factories(sys.id); // factory count
		float mult = (1.0f + fact/(10.0f*size)); // invasion multiplier
		
		int sysId = sys.id;
		EmpireView ev = empire.viewForEmpire(empire.sv.empId(sysId));
		float targetTech = ev.spies().tech().avgTechLevel(); // modnar: target tech level
		
        if (empire.sv.hasFleetForEmpire(sys.id, empire))
            launchGroundTroops(v, sys, mult);
        else if (empire.combatTransportPct() > 0)
            launchGroundTroops(v, sys, mult/empire.combatTransportPct());

        float baseBCPresent = empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost();
        float bcMultiplier = 1 + (empire.sv.hostilityLevel(sys.id));
		
		// modnar: include enemyFleetSize, factoring in relative tech levels
        float bcNeeded = (baseBCPresent*4 + 2*enemyFleetSize)*(targetTech+10.0f)/(civTech+10.0f) + bcMultiplier*civProd/16;
        
        // modnar: balance invasion fleet to use 50% destroyers, 30% bombers, and 20% fighters
        int destroyersNeeded = (int) Math.ceil(0.5f*bcNeeded/empire.shipLab().destroyerDesign().cost());
        int bombersNeeded = (int) Math.ceil(0.3f*bcNeeded/empire.shipLab().bomberDesign().cost());
        int fightersNeeded = (int) Math.ceil(0.2f*bcNeeded/empire.shipLab().fighterDesign().cost());

        // set fleet orders for invasion...
        ShipDesignLab lab = empire.shipLab();
		// modnar: should use min speed here (?)
        float speed = min(lab.destroyerDesign().warpSpeed(), lab.bomberDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.addShips(empire.shipLab().destroyerDesign(), destroyersNeeded);
		fp.addShips(empire.shipLab().bomberDesign(), bombersNeeded);
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
                // modnar: only consider systems within 8 travel turns at the start of the game
				// decrease with faster warp (faster transport speed)
				// down to 3 travel turns with warp-9
				// warp (topSpeed): 1, 2, 3, 4, 5, 6, 7, 8, 9
				// transport speed: 1, 1, 2, 3, 4, 5, 6, 7, 8
				// allowableTurns:  8, 8, 8, 6 ,5, 4, 4, 3, 3
				// max distance:    8, 8,16,18,20,20,24,21,24
				float topSpeed = empire.tech().topSpeed();
				float allowableTurns = (float) (1 + Math.min(7, Math.floor(22 / topSpeed)));
                if ((travelTime <= allowableTurns) && sys.colony().canTransport()) {
                    launchPoints.add(sys);
                    maxTravelTime = max(maxTravelTime, travelTime);
					// modnar: keep planets at least 60% full
					// to prevent complete draining of planets
					// TODO: modify with leader personality and source planet fertility
                    //troopsAvailable += sys.colony().maxTransportsAllowed();
					troopsAvailable += Math.max(0.0f, sys.colony().population() - 0.6f*sys.colony().planet().currentSize());
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
			// modnar: keep planets at least 60% full
			// to prevent complete draining of planets
			// TODO: modify with leader personality and source planet fertility
            // int troops = sys.colony().maxTransportsAllowed();
			int troops = (int) Math.floor(Math.max(0.0f, sys.colony().population() - 0.6f*sys.colony().planet().currentSize()));
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
		
		// modnar: (?) this old estimate gives completely wrong results for ground combat
        //return empire.sv.population(id) * (50 + ev.spies().tech().troopCombatAdj(true)) / (50 + empire.tech().troopCombatAdj(false));
		
		// modnar: correct ground combat ratio estimates for troopsNecessary
		if (ev.spies().tech().troopCombatAdj(true) >= empire.tech().troopCombatAdj(false)) {
			float defAdv = ev.spies().tech().troopCombatAdj(true) - empire.tech().troopCombatAdj(false);
			// killRatio = attackerCasualties / defenderCasualties
			float killRatio = (float) ((Math.pow(100,2) - Math.pow(100-defAdv,2)/2) / (Math.pow(100-defAdv,2)/2));
			return empire.sv.population(id) * killRatio;
		}
		else {
			float atkAdv = empire.tech().troopCombatAdj(false) - ev.spies().tech().troopCombatAdj(true);
			// killRatio = attackerCasualties / defenderCasualties
			float killRatio = (float) ((Math.pow(100-atkAdv,2)/2) / (Math.pow(100,2) - Math.pow(100-atkAdv,2)/2));
			return empire.sv.population(id) * killRatio;
		}
    }
    public void orderBombardmentFleet(EmpireView v, StarSystem sys, float fleetSize) {
		
		int sysId = sys.id;
		EmpireView ev = empire.viewForEmpire(empire.sv.empId(sysId));
		float targetTech = ev.spies().tech().avgTechLevel(); // modnar: target tech level
		
        float baseBCPresent = empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost();
        float bcMultiplier = 1 + (empire.sv.hostilityLevel(sys.id)/2);
        
		// modnar: test fleet sizes, include enemyFleetSize, factoring in relative tech levels
        float bcNeeded = (baseBCPresent*4 + 2*fleetSize)*(targetTech+10.0f)/(civTech+10.0f) + bcMultiplier*civProd/16;
		
		// modnar: bombing fleet to use 50% destroyers, 30% bombers, and 20% fighters
		int destroyersNeeded = (int) Math.ceil(0.5f*bcNeeded/empire.shipLab().destroyerDesign().cost());
        int bombersNeeded = (int) Math.ceil(0.3f*bcNeeded/empire.shipLab().bomberDesign().cost());
        int fightersNeeded = (int) Math.ceil(0.2f*bcNeeded/empire.shipLab().fighterDesign().cost());

        ShipDesignLab lab = empire.shipLab();
		// modnar: should use min speed here (?)
        float speed = min(lab.destroyerDesign().warpSpeed(), lab.bomberDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
		fp.addShips(empire.shipLab().destroyerDesign(), destroyersNeeded);
        fp.addShips(empire.shipLab().bomberDesign(), bombersNeeded);
        fp.addShips(empire.shipLab().fighterDesign(), fightersNeeded);
        fp.stagingPointId = empire.optimalStagingPoint(sys, speed);
        if (v.embassy().finalWar()) 
            fp.priority = FleetPlan.BOMB_FINAL_WAR+ invasionPriority(sys)/100;
        else
            fp.priority = FleetPlan.BOMB_ENEMY+ invasionPriority(sys)/100;
    }
    public void orderBombEncroachmentFleet(EmpireView v, StarSystem sys, float fleetSize) {
        
		int sysId = sys.id;
		EmpireView ev = empire.viewForEmpire(empire.sv.empId(sysId));
		float targetTech = ev.spies().tech().avgTechLevel(); // modnar: target tech level
		
		float baseBCPresent = empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost();
        float bcMultiplier = 1 + (empire.sv.hostilityLevel(sys.id)/2);
		
        // modnar: test fleet sizes, include enemyFleetSize, factoring in relative tech levels
        float bcNeeded = (baseBCPresent*4 + 2*fleetSize)*(targetTech+10.0f)/(civTech+10.0f) + bcMultiplier*civProd/24;
		
        // modnar: bombing fleet to use 50% destroyers, 30% bombers, and 20% fighters
		int destroyersNeeded = (int) Math.ceil(0.5f*bcNeeded/empire.shipLab().destroyerDesign().cost());
        int bombersNeeded = (int) Math.ceil(0.3f*bcNeeded/empire.shipLab().bomberDesign().cost());
        int fightersNeeded = (int) Math.ceil(0.2f*bcNeeded/empire.shipLab().fighterDesign().cost());

        ShipDesignLab lab = empire.shipLab();
		// modnar: should use min speed here (?)
        float speed = min(lab.destroyerDesign().warpSpeed(), lab.bomberDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
		fp.addShips(empire.shipLab().destroyerDesign(), destroyersNeeded);
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
		
		// modnar: no sneak attack when number of enemies > 2
		// same as regular war declaration check, significant factor in extra wars
		if (empire.numEnemies() > 2)
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
		
		// modnar: factor in own empire average tech level
		// suppress sneak attack war in early game when average tech level is below 10
		float myTechLvl = empire.tech().avgTechLevel(); // minimum average tech level is 1.0
		float techMod = 1.0f;
		if (myTechLvl < 10.0f) {
			techMod = myTechLvl / 20.0f + 0.5f; // linear change with tech level (range from 0.55 to 1.0)
		}
		baseChance *= techMod;
		
		// modnar: change sneak attack chance by number of our wars vs. number of their wars
		// try not to get into too many wars, and pile on if target is in many wars
		float enemyMod = (float) (0.2f * (v.empire().numEnemies() - empire.numEnemies()));
		baseChance += enemyMod;

        float value = (empire.sv.factories(sys.id) * 10);
        float cost = fleetSize + (empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost());
        float bonus = -0.5f + (value / (value+cost));

        if ((baseChance+bonus) > 0.5)  {
            orderBombardmentFleet(v, sys, fleetSize);
            empire.sv.fleetPlan(sys.id).priority = FleetPlan.BOMB_UNDEFENDED;
        }
    }
	// modnar: setHighFighterGuard added for most threatened
	private void setHighFighterGuard(StarSystem sys, float priority) {
        float basesWanted = empire.sv.desiredMissileBases(sys.id);
        float baseCost = empire.tech().newMissileBase().cost(empire);
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.priority = priority;
        // modnar: fighter guard on top of any bases, 4 base BC = 1 fighter BC
        fp.addShipBC(empire.shipLab().fighterDesign(), basesWanted*baseCost/4);
    }
	// modnar: setNormalFighterGuard added for possibly threatened
	private void setNormalFighterGuard(StarSystem sys, float priority) {
        float basesNeeded = empire.sv.desiredMissileBases(sys.id) - empire.sv.bases(sys.id);
        if (basesNeeded <= 0) 
            return;
        float baseCost = empire.tech().newMissileBase().cost(empire);
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.priority = priority;
        // modnar: normal fighter guard, 8 base BC = 1 fighter BC
        fp.addShipBC(empire.shipLab().fighterDesign(), basesNeeded*baseCost/8);
    }
    private void setMinimumFighterGuard(StarSystem sys, float priority) {
        float basesNeeded = empire.sv.desiredMissileBases(sys.id) - empire.sv.bases(sys.id);
        if (basesNeeded <= 0) 
            return;
        float baseCost = empire.tech().newMissileBase().cost(empire);
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.priority = priority;
        // modnar: adjust minium fighter guard, 20 base BC = 1 fighter BC (previous one base BC = 10 fighter BC, typo(?), equation is 10:1)
        fp.addShipBC(empire.shipLab().fighterDesign(), basesNeeded*baseCost/20); // modnar: reduce minium fighter guard needed
    }
    private void setRepelFleetPlan(StarSystem sys, float fleetSize) {
        float baseBCPresent = empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost();
        float bcNeeded = max(empire.shipLab().fighterDesign().cost(), fleetSize*3); // modnar: reduce repel fleet
        bcNeeded -= baseBCPresent;
        if (bcNeeded <= 0)
            return;
        
        rushDefenseSystems.add(sys);

        // use up to half of BC for Destroyers... rest for fighters
        int destroyersNeeded = (int) Math.ceil((bcNeeded/2)/empire.shipLab().destroyerDesign().cost());
        bcNeeded -= (destroyersNeeded * empire.shipLab().destroyerDesign().cost());
        int fightersNeeded = (int) Math.ceil(bcNeeded/empire.shipLab().fighterDesign().cost());

        ShipDesignLab lab = empire.shipLab();
		// modnar: should use min speed here (?)
        float speed = min(lab.destroyerDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.priority = FleetPlan.REPEL + invasionPriority(sys)/100;
        fp.stagingPointId = empire.optimalStagingPoint(sys, speed);
        fp.addShips(empire.shipLab().destroyerDesign(), destroyersNeeded);
        fp.addShips(empire.shipLab().fighterDesign(), fightersNeeded);
    }
    private void setInterceptFleetPlan(StarSystem sys, float fleetSize) {
        float baseBCPresent = empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost();
        float bcNeeded = max(empire.shipLab().fighterDesign().cost(), fleetSize*2); // modnar: reduce intercept fleet
        bcNeeded -= baseBCPresent;
        if (bcNeeded <= 0)
            return;

        rushDefenseSystems.add(sys);
        // use up to half of BC for Destroyers... rest for fighters
        int destroyersNeeded = (int) Math.ceil((bcNeeded/2)/empire.shipLab().destroyerDesign().cost());
        bcNeeded -= (destroyersNeeded * empire.shipLab().destroyerDesign().cost());
        int fightersNeeded = (int) Math.ceil(bcNeeded/empire.shipLab().fighterDesign().cost());

        ShipDesignLab lab = empire.shipLab();
		// modnar: should use min speed here (?)
        float speed = min(lab.destroyerDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.priority = FleetPlan.INTERCEPT + invasionPriority(sys)/100;
        fp.addShips(empire.shipLab().destroyerDesign(), destroyersNeeded);
        fp.addShips(empire.shipLab().fighterDesign(), fightersNeeded);
    }
    private void setExpelFleetPlan(StarSystem sys, float fleetSize) {
        float baseBCPresent = empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost();
        float bcNeeded = max(empire.shipLab().fighterDesign().cost(), fleetSize*3); // modnar: reduce expel fleet
        bcNeeded -= baseBCPresent;
        if (bcNeeded <= 0)
            return;

        // use up to half of BC for Destroyers... rest for fighters
        int destroyersNeeded = (int) Math.ceil((bcNeeded/2)/empire.shipLab().destroyerDesign().cost());
        bcNeeded -= (destroyersNeeded * empire.shipLab().destroyerDesign().cost());
        int fightersNeeded = (int) Math.ceil(bcNeeded/empire.shipLab().fighterDesign().cost());

        ShipDesignLab lab = empire.shipLab();
		// modnar: should use min speed here (?)
        float speed = min(lab.destroyerDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
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
		// modnar: should use min speed here (?)
        float speed = min(lab.destroyerDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
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