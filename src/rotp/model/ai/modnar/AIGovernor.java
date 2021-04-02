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

import rotp.model.ai.FleetPlan;
import rotp.model.ai.ShipPlan;
import rotp.model.ai.interfaces.Governor;
import rotp.model.colony.Colony;
import rotp.model.colony.ColonySpendingCategory;
import rotp.model.empires.Empire;
import rotp.model.empires.SystemView;
import rotp.model.galaxy.StarSystem;
import rotp.model.game.GameSession;
import rotp.model.planet.Planet;
import rotp.util.Base;

public class AIGovernor implements Base, Governor {
    public static final int SHIP = Colony.SHIP;
    public static final int DEFENSE = Colony.DEFENSE;
    public static final int INDUSTRY = Colony.INDUSTRY;
    public static final int ECOLOGY = Colony.ECOLOGY;
    public static final int RESEARCH = Colony.RESEARCH;
    private final Empire empire;

    public AIGovernor (Empire c) {
        empire = c;
    }
    @Override
    public void setInitialAllocations(Colony col) {
        baseSetColonyAllocations(col);
    }
    @Override
    public boolean readyToBuild(Colony col, ShipPlan sh, int designCost) {
        float pct = col.currentProductionCapacity();
        float estProd = col.industry().factories()*col.planet().productionAdj();
        if (pct > 0.9)  // modnar: change to 90% to build anything
            return true;
        else if (pct > 0.75) // modnar: change to 75%, colonize is the lowest priority we can build
            return sh.plan.priority() >= FleetPlan.COLONIZE;
        
        return estProd > designCost*5;
    }
    @Override
    public void setColonyAllocations(Colony col) {
        if (empire.isAIControlled()) {
            baseSetColonyAllocations(col);
            col.validate();
            return;
        }

        StarSystem sys = col.starSystem();
        String name = empire.sv.name(sys.id);
        boolean cleanupOK = ensureMinimumCleanup(col);
        int bases = (int) col.defense().bases();
        int maxBases = col.defense().maxBases();
        if (col.shipyard().design().scrapped()) {
            if (col.shipyard().building() || (col.shipyard().allocation() > 0))
                session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_DESIGN_SCRAPPED", name));
            else
                col.shipyard().goToNextDesign();
        }
        if (col.shipyard().stargateCompleted())
            session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_STARGATE_COMPLETE", name));
        if (col.shipyard().shipLimitReached())
            session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_SHIPS_COMPLETE", name, col.shipyard().design().name()));
        if (col.defense().shieldCompleted())
            session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_SHIELD_COMPLETE", name, col.empire().tech().topPlanetaryShieldTech().name()));
        if ((bases > 0) && (bases >= maxBases) && col.defense().missileBasesUpgraded())
            session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_BASES_UPGRADED", name, col.empire().tech().topBaseMissileTech().name()));
        if ((bases > 0) && col.defense().missileBasesCompletedThisTurn())
            session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_BASES_COMPLETE", name, col.defense().maxBases()));
        if (col.industry().isCompletedThisTurn())
            session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_MAX_FACTORIES", name, (int)col.industry().maxBuildableFactories()));
        if (!cleanupOK) {
            // ignore cleanup messages for governed colonies
            if (!col.isGovernor()) {
                session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_ECO_LOCKED_WASTE", name));
            }
        }
        if (col.ecology().populationGrowthCompletedThisTurn()) {
            // disable colony at max pop message if governor is on and we are using auto transport
            if (!col.isGovernor()) {
                session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_MAX_POPULATION", name, (int) col.maxSize()));
            }
        }
        if (col.ecology().atmosphereCompletedThisTurn())
            session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_ATMOSPHERE_COMPLETE", name));
        if (col.ecology().soilEnrichCompletedThisTurn()) {
            if (col.planet().isEnvironmentGaia())
                session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_GAIA_COMPLETE", name));
            else
                session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_FERTILE_COMPLETE", name));
        }
        if (col.ecology().terraformCompletedThisTurn()) 
            session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_TERRAFORM_COMPLETE", name));
        if (col.research().hasCompletedProject()) 
            session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_PROJECT_ENDED", name, col.research().completedProject().projectKey()));
            
        if (col.hasNewOrders() || (col.allocationRemaining() != 0) || session().awaitingAllocation(sys)) {
            baseSetPlayerAllocations(col);
            col.validate();
        }
    }
    private void baseSetPlayerAllocations(Colony col) {
        int prevShip = col.shipyard().allocation();
        int prevDef = col.defense().allocation();
        int prevInd = col.industry().allocation();
        int prevEco = col.ecology().allocation();
        int prevRes = col.research().allocation();

        int cleanEco = col.ecology().cleanupAllocationNeeded();
        int maxInd = col.industry().maxAllocationNeeded();
        int maxEco = col.ecology().terraformAllocationNeeded();
        int maxEco2 = col.ecology().maxAllocationNeeded();
        int maxDef = col.defense().maxAllocationNeeded();
        int orderedInd = col.industry().orderedAllocation();
        int orderedEco = col.ecology().orderedAllocation();
        int orderedDef = col.defense().orderedAllocation();

        // reset all unlocked allocations to zero
        col.clearUnlockedSpending();
        col.hasNewOrders(false);
        
        // 1. spend ECO for cleaning before anything else
        if (!col.locked(ECOLOGY))
            col.addAllocation(ECOLOGY, min(col.allocationRemaining(), cleanEco-col.allocation(ECOLOGY)));

        // 2. NOW ENSURE ORDERED AMOUNTS ARE MET (orders set when techs are learned)
        // priority of orders is: industry, ecology, defense, ship, research
        // but do not exceed the max needed to finish the project
        if (!col.locked(INDUSTRY))
            col.setAllocation(INDUSTRY, min(orderedInd, maxInd));
        if (!col.locked(ECOLOGY))
            col.setAllocation(ECOLOGY,  min(orderedEco, maxEco));
        if (!col.locked(DEFENSE))
            col.setAllocation(DEFENSE,  min(orderedDef, maxDef));
        
        // 3. Unless we have just completed building a stargate or reached a ship
        // limit, ensure that SHIP spending 
        // is maintained. Ship spending is never allocated for player colonies by the AI 
        //Governor so any spending here must be treated similarly to a player order
        if (!col.locked(SHIP) 
        && !col.shipyard().stargateCompleted()
        && !col.shipyard().shipLimitReached())
            col.setAllocation(SHIP, prevShip);
 
        // 4. now fill any remaining build requirements for ind/eco/def
        // being careful not to exceed previous spending in that category
        // i.e. if we raise spending in a category, it is only because
        // it was ordered by a new technology
        if (!col.locked(INDUSTRY))
            col.setAllocation(INDUSTRY, min(prevInd, maxInd));
        if (!col.locked(ECOLOGY))
            col.setAllocation(ECOLOGY,  min(prevEco, maxEco));
        if (!col.locked(DEFENSE))
            col.setAllocation(DEFENSE,  min(prevDef, maxDef));

        // SPEND THE EXCESS
        // if there is industry left to build, go there first
        if (!col.locked(INDUSTRY))
            col.setAllocation(INDUSTRY, maxInd);
        // if there is terraforming left to build, go there first
        if (!col.locked(ECOLOGY))
            col.setAllocation(ECOLOGY, maxEco);
        // if there is defense left to build, go there next
        if (!col.locked(DEFENSE))
            col.setAllocation(DEFENSE, maxDef);
        // if there is population to grow, go there
        if (!col.locked(ECOLOGY))
            col.setAllocation(ECOLOGY, maxEco2);

        // if research not locked go there
        if (!col.locked(RESEARCH))
            col.addAllocation(RESEARCH, col.allocationRemaining());
        else if (!col.locked(INDUSTRY)) 
            col.addAllocation(INDUSTRY, col.allocationRemaining());
        else if (!col.locked(ECOLOGY)) 
            col.addAllocation(ECOLOGY, col.allocationRemaining());
        else if (!col.locked(DEFENSE))
            col.addAllocation(DEFENSE, col.allocationRemaining());
        else if (!col.locked(SHIP))
            col.addAllocation(SHIP, col.allocationRemaining());
    }
    private void baseSetColonyAllocations(Colony col) {                
        int maxAllocation = ColonySpendingCategory.MAX_TICKS;

        // for systems that have a research project, focus research and forget
        // everything else until the project is done
        if (col.research().hasProject()) {
            float totalProd = col.totalIncome();
            float cleanCost = col.minimumCleanupCost();
            col.clearSpending();
            col.pct(ECOLOGY, cleanCost/totalProd);
            col.allocation(RESEARCH, maxAllocation - col.totalAmountAllocated());
            return;
        }

        // for systems that are flagged as rush defense, do that and forget
        // everything else until the project is done
        if (empire.generalAI().rushDefenseSystems().contains(col.starSystem())) {
            if (col.defense().maxSpendingNeeded() > 0) {
                float totalProd = col.totalIncome();
                float cleanCost = col.minimumCleanupCost();
                col.clearSpending();
                col.pct(ECOLOGY, cleanCost/totalProd);
                col.allocation(DEFENSE, maxAllocation - col.totalAmountAllocated());
                return;
            }
        }
        
        // for systems that are flagged as rush ship, do that and forget
        // everything else until the project is done
        if (empire.generalAI().rushShipSystems().contains(col.starSystem())) {
            float totalProd = col.totalIncome();
            float cleanCost = col.minimumCleanupCost();
            col.clearSpending();
            col.pct(ECOLOGY, cleanCost/totalProd);
            col.allocation(SHIP, maxAllocation - col.totalAmountAllocated());
            return;
        }

        // calc this now before spending amts are  reset
        float maxShipBCNeeded = col.shipyard().maxSpendingNeeded();
        float maxShipBC = maxShipBCPermitted(col);
        float shipPctSpending = shipPctForColony(col);
        float currentNet = col.totalIncome() - col.minimumCleanupCost();
        // # of turns we could make ship with 100% ship
        float shipTurns = maxShipBCNeeded/(currentNet*shipPctSpending);
        // pct increase of factories we could make with 100% industry
        float maxNewFactories = min(col.industry().maxUseableFactories()-col.industry().factories(), currentNet/col.industry().newFactoryCost());
        float factoryIncreasePct = maxNewFactories/col.industry().factories();

        suggestMissileBaseCount(col);
        col.clearSpending();

        lowerExpenses(col);
        float totalProd = col.totalIncome();
        float cleanCost = col.minimumCleanupCost();
        float netProd = totalProd - cleanCost;
        float shipCost = 0;
        // calculate minimum eco cleanup pct
        col.pct(ECOLOGY, cleanCost/totalProd);

        if (col.allocation(ECOLOGY) < 0) {
            err("Minimum cleanup cost < 0");
            throw new RuntimeException("Minimum cleanup cost < 0");
        }

        // don't change ship allocation for players else reset to zero
        if (empire.isPlayerControlled()) {
            shipCost = totalProd * col.pct(SHIP);
            if (shipCost > netProd) {
                col.pct(SHIP, netProd/totalProd);
                shipCost = col.pct(SHIP) * totalProd;
            }
        }
        else {
            shipCost = 0;
            col.pct(SHIP, 0);
        }

        // modnar: set 90% research overhead for inner colonies >90% full production
		// or 30% research overhead for non-inner colonies >90% full production (not just border colonies)
		// not applicable to rich/ultra-rich
		// no need to allocate anything here, should be added in automatically to research at the end
		int bases = (int) col.defense().bases();
        int maxBases = col.defense().maxBases();
		float resOverhead = 0.1f*netProd;
		StarSystem sys = col.starSystem();
		float prodPct = col.currentProductionCapacity();
		if (bases >= maxBases) { // only if missile bases are in place
			if ((prodPct > 0.9) && empire.sv.isInnerSystem(sys.id) && !col.planet().isResourceRich() && !col.planet().isResourceUltraRich()) { 
				netProd -= 9*resOverhead;
			}
			if ((prodPct > 0.9) && !empire.sv.isInnerSystem(sys.id) && !col.planet().isResourceRich() && !col.planet().isResourceUltraRich()) { 
				netProd -= 3*resOverhead;
			}
		}
		
        // ship spending, if requested
        if (!col.shipyard().buildingObsoleteDesign()
        && (col.shipyard().desiredShips() > 0)
        && ((1.0/shipTurns) > factoryIncreasePct)){
            shipCost = min(maxShipBC, col.shipyard().maxSpendingNeeded());
            float shipPct = shipCost/totalProd;
            col.pct(SHIP, shipPct);
            shipCost = col.pct(SHIP) * totalProd;
        }
        netProd -= shipCost;

        if (col.totalAmountAllocated() >= maxAllocation)
            return;

        // prod spending gets up to 100% of planet's remaining net prod
        float prodCost = min(netProd, col.industry().maxSpendingNeeded());
        col.pct(INDUSTRY, prodCost/totalProd);
        prodCost = col.pct(INDUSTRY) * totalProd;
        netProd -= prodCost;

        if (col.totalAmountAllocated() >= maxAllocation)
            return;

        // eco spending gets up to 40% of planet's remaining net prod
        float nonCleanEcoCost = col.ecology().maxSpendingNeeded() - cleanCost;
        float ecoCost = min((netProd * .4f), nonCleanEcoCost);
        col.pct(ECOLOGY, (ecoCost + cleanCost)/totalProd);

        if (col.pct(ECOLOGY) < 0) {
            err("Eco pct < 0");
            throw new RuntimeException("Minimum cleanup cost < 0");
        }

        ecoCost = col.pct(ECOLOGY) * totalProd;
        netProd -= (ecoCost - cleanCost);

        if (col.totalAmountAllocated() >= maxAllocation)
            return;

        // modnar: reduce defense spending, "up to 30%" (previous 50%)
        float defCost = min((netProd * .3f), col.defense().maxSpendingNeeded());
        col.pct(DEFENSE, defCost/totalProd);
        defCost = col.pct(DEFENSE) * totalProd;

        if (col.totalAmountAllocated() >= maxAllocation)
            return;

        // research gets the rest
        int totalAlloc = col.allocation(SHIP)+col.allocation(DEFENSE)+col.allocation(INDUSTRY)+col.allocation(ECOLOGY);
        col.allocation(RESEARCH, maxAllocation - totalAlloc);

        // check to allocate reserve
        // modnar: reduce to 0%, since it's taken care of by the AICTreasurer (?)
        if (col.planet().noArtifacts() && (col.pct(RESEARCH) > 0.5) ) {
            int rsvAmt = (int) Math.min(0.0, col.pct(RESEARCH) - 0.5);
            col.addPct(RESEARCH, -rsvAmt);
            col.addPct(INDUSTRY, rsvAmt);
        }

        for (int i=0;i<col.spending.length;i++)
            col.locked(i, false);
    }
    @Override
    public float maxShipBCPermitted(Colony col) {
        float maxAllowed = max(0, col.totalIncome() - col.wasteCleanupCost());
        return min(maxAllowed, col.totalIncome()*shipPctForColony(col));
    }
    public void suggestMissileBaseCount(Colony col) {
        if (empire.isAIControlled())
            suggestMissileBaseCount(col, col.production());
    }
    public void suggestMissileBaseCount(Colony col, float prod) {
        if (empire.contacts().isEmpty())  {
            col.defense().maxBases(0);
            return;
        }
        StarSystem sys = col.starSystem();
        int currBases = col.defense().missileBases();
        
        // modnar: scale desired base count based on difficulty, rich-ness, and artifact level
        // (no decrease with poor-ness, since they will become easy tech capture targets)
        float baseMultiplier = (float) Math.sqrt(options().aiProductionModifier());
        if (empire.sv.isArtifact(sys.id))
            baseMultiplier *= 1.5f;
        else if (empire.sv.isOrionArtifact(sys.id))
            baseMultiplier *= 2.5f;
        
        if (empire.sv.isUltraRich(sys.id))
            baseMultiplier *= 2.5f;
        else if (empire.sv.isRich(sys.id))
            baseMultiplier *= 1.5f;
        
        if (sys == null)  // this can happen at startup
            col.defense().maxBases(0);
        else if (empire.sv.isAttackTarget(sys.id))
            col.defense().maxBases(max(currBases, (int)(baseMultiplier * col.production()/40))); // modnar: reduce
        else if (empire.sv.isBorderSystem(sys.id))
            col.defense().maxBases(max(currBases, (int)(baseMultiplier * col.production()/60))); // modnar: reduce
        else
            col.defense().maxBases((int)(baseMultiplier * col.production()/200)); // modnar: reduce and allow scrap
    }
    @Override
    public int suggestedEmpireTaxLevel() {
        // this will hopefully be handled at the planet level,so return 0
        return 0;
    }
    @Override
    public void lowerExpenses(Colony col) {
        float totalProd = col.totalIncome();

        // does this colony have a positive income? If not, start canceling some activities
        // 1. start by reducing outgoing transports
        while ((totalProd <= 0) && col.canLowerMaintenance()) {
            col.lowerMaintenance();
            totalProd = col.totalIncome();
        }

        // 2. try reducing shipyard maintenance (stargate)
        while ((totalProd <= 0) && col.shipyard().canLowerMaintenance()) {
            col.shipyard().lowerMaintenance();
            totalProd = col.totalIncome();
        }

        // 3. try reducing defense maintenance (bases)
        while ((totalProd <= 0) && col.defense().canLowerMaintenance()) {
            col.defense().lowerMaintenance();
            totalProd = col.totalIncome();
        }
        col.validate();
    }
    @Override
    public float targetPopPct(int sysId) {
        SystemView sv = empire.sv.view(sysId);
        if (sv.borderSystem()) return .75f;

        Planet p = sv.system().planet();
        if (p.isResourceRich()) return .75f;
        if (p.isResourceUltraRich()) return .75f;
        if (p.isArtifact()) return .75f;
        if (p.isOrionArtifact()) return .75f;
        if (p.currentSize() <= 20) return .75f;

        if (sv.supportSystem()) return .5f;
        if (p.currentSize() <= 40) return .5f;

        return .25f;
    }
    //
// PRIVATE
//
    private boolean ensureMinimumCleanup(Colony col) {
        // return true if eco spending is set to enough for waste
        float totalProd = col.totalIncome();
        float minEco = col.minimumCleanupCost();
        float minPct = minEco/totalProd;
        minPct = min(minPct, 1.0f);

        // if locked and insufficient ECO spending, return false
        if (col.locked(ECOLOGY))
            return col.pct(ECOLOGY) >= minPct;
        
        if (minPct < 0)
            err("Minimum cleanup pct: ", str(minPct), "  totalProd:",str(totalProd), "   minEco:", str(minEco));

        if (col.pct(ECOLOGY) < minPct)
            col.setCleanupPct(minPct);
        return true;
    }
    private float shipPctForColony(Colony col) {
        // 20% or research spending, whichever is greater
        float pct = max(col.pct(SHIP)+col.pct(RESEARCH), .2f);
        // adjust upwards are downwards based on planet bonuses
        pct *= col.planet().productionAdj();
        pct /= col.planet().researchAdj();
        return min(pct, 1);
    }
}
