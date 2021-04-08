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

import rotp.model.ai.FleetPlan;
import rotp.model.ai.ShipPlan;
import rotp.model.ai.interfaces.Governor;
import rotp.model.colony.Colony;
import rotp.model.colony.ColonySpendingCategory;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.SystemView;
import rotp.model.galaxy.IMappedObject;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.planet.Planet;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.tech.TechTree;
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
            ensureMinimumCleanup(col);
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
        if (!cleanupOK)
            session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_ECO_LOCKED_WASTE", name));
        if (col.ecology().populationGrowthCompletedThisTurn())
            session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_MAX_POPULATION", name, (int)col.maxSize()));
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
            if (col.defense().maxSpendingNeeded() > 0 && col.defense().maxBases() > 0) {    //ail: only start rush-defense when at least one base should be built, otherwise freshly colonized planets sometimes start with a shield
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
        //switch away from stargate, which can accidentally get selected by scrapping after stargate was researched
        if(col.shipyard().design() == empire.shipLab().stargateDesign() || !col.shipyard().design().active()) {
            col.shipyard().goToNextDesign();
        }
        //System.out.print("\n"+empire.name()+" col.shipyard().maxSpendingNeeded(): "+col.shipyard().maxSpendingNeeded()+" bldg: "+col.shipyard().design().id()+ " active: "+col.shipyard().design().active());
        float maxShipBCNeeded = col.shipyard().maxSpendingNeeded();
        float maxShipBC = maxShipBCPermitted(col);
        float shipPctSpending = shipPctForColony(col);
        float currentNet = col.totalIncome() - col.minimumCleanupCost();
        // # of turns we could make ship with 100% ship
        float shipTurns = maxShipBCNeeded/(currentNet*shipPctSpending);
        // pct increase of factories we could make with 100% industry
        float maxNewFactories = min(col.industry().maxUseableFactories()-col.industry().factories(), currentNet/col.industry().newFactoryCost());
        float factoryIncreasePct = maxNewFactories/col.industry().factories();
        int colonizerNeed = empire.generalAI().additionalColonizersToBuild(false);

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

        float netFactoryProduction = 1;
        if(!empire.ignoresPlanetEnvironment())
            netFactoryProduction -= empire.tech().factoryWasteMod() / empire.tech().wasteElimination();
        float workerROI = empire.tech().populationCost() / empire.workerProductivity();
        float factoryROI = empire.tech().baseFactoryCost() / netFactoryProduction;
        
        float enemyBombardPower = 0.0f;
        
        for(ShipFleet fleet:col.starSystem().orbitingFleets())
        {
            if(fleet.empire().aggressiveWith(col.empire().id))
            {
                enemyBombardPower += fleet.expectedBombardDamage();
            }
        }
        float popLoss = enemyBombardPower / 200;
        
        // prod spending gets up to 100% of planet's remaining net prod
        if(col.industry().factories() < col.maxUseableFactories() 
                && (colonizerNeed == 0 || factoryROI < 25 || productionScore(col.starSystem()) < 0.5)
                && enemyBombardPower == 0)
        {
            if(workerROI > factoryROI || col.population() == col.maxSize())
            {
                float prodCost = min(netProd, col.industry().maxSpendingNeeded());
                col.pct(INDUSTRY, prodCost/totalProd);
                prodCost = col.pct(INDUSTRY) * totalProd;
                netProd -= prodCost;

                if (col.totalAmountAllocated() >= maxAllocation)
                    return;
            }
        }

        // eco spending gets up to 100% of planet's remaining net prod

        float nonCleanEcoCost = col.ecology().maxSpendingNeeded() - cleanCost;
        if(colonizerNeed > 0 
                && productionScore(col.starSystem()) >= 0.5
                && workerROI >= 25 
                && col.population() >= col.planet().maxSize() / 2)
        {
            nonCleanEcoCost = 0;
        }
        //if we bomb us, we make ship or research
        if(popLoss * empire.tech().populationCost() > totalProd)
            nonCleanEcoCost = 0;
        float ecoCost = max(0, min(netProd, nonCleanEcoCost));
        col.pct(ECOLOGY, (ecoCost + cleanCost)/totalProd);

        if (col.pct(ECOLOGY) < 0) {
            err("Eco pct < 0");
            throw new RuntimeException("Minimum cleanup cost < 0");
        }

        if (col.totalAmountAllocated() >= maxAllocation)
            return;

        // ail: Remove spending limit since bases are now only built at border and we want to get it over with quickly
        // ail: only build defense when a shield is needed. Otherwise never worth it
        if(wantShield(col))
        {
            float defCost = col.defense().maxSpendingNeeded();
            col.pct(DEFENSE, defCost/totalProd);
        }

        if (col.totalAmountAllocated() >= maxAllocation)
            return;

        // research gets the rest
        // ail: build military, if we want
        // only if we are not already producing ships for other purposes
        int totalAlloc = col.allocation(SHIP)+col.allocation(DEFENSE)+col.allocation(INDUSTRY)+col.allocation(ECOLOGY);
        ShipDesignLab lab = empire.shipLab();
        //System.out.print("\n"+empire.name()+" "+col.name()+" colonizer-production-score "+productionScore(col.starSystem(), true));
        boolean inAttackRange = false;
        boolean enemy = false;
        for(Empire emp : empire.contactedEmpires())
        {
            EmpireView v = empire.viewForEmpire(emp);
            if(v.embassy().isEnemy() && empire.inShipRange(emp.id))
            {
                enemy = true;
            }
            else if(empire.inShipRange(emp.id))
            {
                inAttackRange = true;
            }
        }
        int[] counts = galaxy().ships.shipDesignCounts(empire.id);
        float fighterCost = 0.0f;
        float bomberCost = 0.0f;
        float colonizerCost = 0.0f;
        for (int i=0;i<counts.length;i++) 
        {
            if(lab.design(i).isFighter())
            {
                fighterCost += lab.design(i).cost() * counts[i];
            }
            if(lab.design(i).isBomber())
            {
                bomberCost += lab.design(i).cost() * counts[i];
            }
            if(lab.design(i).isColonyShip())
            {
                colonizerCost += lab.design(i).cost() * counts[i];
            }
        }
        if(colonizerNeed > 0 && col.allocation(SHIP) == 0 && productionScore(col.starSystem()) >= 0.5)
        {
            col.shipyard().design(lab.colonyDesign());
            //Making sure to not just spam colonizers when we at risk of being attacked, also ignoring ship-maintenance-limit in this case
            if(enemy == true || inAttackRange == true)
            {
                if(colonizerCost > fighterCost)
                    col.shipyard().design(lab.fighterDesign());
            }
            col.allocation(SHIP, maxAllocation - totalAlloc);
            totalAlloc = col.allocation(SHIP)+col.allocation(DEFENSE)+col.allocation(INDUSTRY)+col.allocation(ECOLOGY);
            //System.out.print("\n"+empire.name()+" Colony-ship: "+col.shipyard().design().name()+ " needed: "+empire.generalAI().additionalColonizersToBuild());
        }
        float fighterDamage = lab.fighterDesign().firepowerAntiShip(empire.bestEnemyShieldLevel());
        float bomberDamage = lab.bomberDesign().firepower(empire.bestEnemyPlanetaryShieldLevel());
        //ail: No use to build any ships if they won't do damage anyways. Better tech up.
        boolean viableForShipProduction = true;
        float turnsBeforeColonyDestroyed = Float.MAX_VALUE;
        if(popLoss > 0)
            turnsBeforeColonyDestroyed = col.population() / popLoss;
        float fighterBuildTime = lab.fighterDesign().cost() / totalProd;
        if(fighterBuildTime > turnsBeforeColonyDestroyed)
            viableForShipProduction = false;
        if(bomberDamage == 0 && fighterDamage == 0)
        {
            viableForShipProduction = false;
        }
        if(col.allocation(SHIP) == 0 && viableForShipProduction)
        {
            //System.out.print("\n"+empire.name()+" "+col.name()+" production-score "+productionScore(col.starSystem()));
            float maxShipMaintainance = 0.0f;
            float fighterPercentage = 1.0f;

            if(enemy)
            {
                maxShipMaintainance = empire.fleetCommanderAI().maxShipMaintainance();
                fighterPercentage = 0.5f + empire.generalAI().defenseRatio() * 0.5f;
            }
            else if(inAttackRange)
            {
                maxShipMaintainance = empire.fleetCommanderAI().maxShipMaintainance() / 4;
                fighterPercentage = 0.75f;
            }
            float maxShipMaintainanceBeforeAdj = maxShipMaintainance;
            maxShipMaintainance *= productionScore(col.starSystem());
            if(maxShipMaintainance > maxShipMaintainanceBeforeAdj)
                maxShipMaintainance = (min(maxShipMaintainance, 1) + maxShipMaintainanceBeforeAdj) / 2;
            boolean techsLeft = false;
            for (int j=0; j<TechTree.NUM_CATEGORIES; j++) {
                if (!empire.tech().category(j).possibleTechs().isEmpty())
                {
                    techsLeft = true;
                    break;
                }
            }
            
            if(!techsLeft)
                maxShipMaintainance = empire.fleetCommanderAI().maxShipMaintainance();
            //System.out.print("\n"+empire.name()+" "+col.name()+" adjMaxMaint: "+maxShipMaintainance+" baseMaxMaint: "+maxShipMaintainanceBeforeAdj+" avg-tech-level: "+empire.tech().avgTechLevel());
            if(fighterDamage == 0)
            {
                fighterPercentage = 0.25f;
            } 
            else if (bomberDamage == 0)
            {
                fighterPercentage = 1.0f;
            }
            col.shipyard().design(lab.fighterDesign());
            //System.out.print("\n"+empire.name()+" fighterCost: "+fighterCost+" bomberCost: "+bomberCost+" F% reached: "+fighterCost / (bomberCost + fighterCost)+" of "+fighterPercentage);
            if(fighterCost / (bomberCost + fighterCost) > fighterPercentage 
                && enemyBombardPower == 0)
            {
                col.shipyard().design(empire.shipLab().bomberDesign());
            }
            if(empire.shipMaintCostPerBC() < maxShipMaintainance)
            {
                col.allocation(SHIP, maxAllocation - totalAlloc);
            }
        }
        totalAlloc = col.allocation(SHIP)+col.allocation(DEFENSE)+col.allocation(INDUSTRY)+col.allocation(ECOLOGY);
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
    public boolean wantShield(Colony col) {
        StarSystem sys = col.starSystem();
        if(!col.defense().shieldAtMaxLevel()
                && (empire.sv.isAttackTarget(sys.id) || empire.sv.isBorderSystem(sys.id))) {
            return true;
        }
        return false;
    }
    public void suggestMissileBaseCount(Colony col, float prod) {
        StarSystem sys = col.starSystem();
        int currBases = col.defense().missileBases();
        if (empire.contacts().isEmpty()
                || empire.shipLab().needColonyShips)  {
            col.defense().maxBases(max(currBases, 0));
            return;
        }
        if (sys == null)  // this can happen at startup
            col.defense().maxBases(0);
        /*else if (empire.sv.isAttackTarget(sys.id))
            col.defense().maxBases(max(currBases, (int)(col.production()/30))); // modnar: reduce base count
        else if (empire.sv.isBorderSystem(sys.id))
            col.defense().maxBases(max(currBases, (int)(col.production()/40))); // modnar: reduce base count*/
        else
            col.defense().maxBases(max(currBases,0));                           // ail: missile-bases are simply not worth it.
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
        if (p.isEnvironmentHostile()) return .75f;
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
    public float productionScore(StarSystem sys)
    {
        float Score = sys.colony().totalIncome();
        Score *= sys.planet().productionAdj();
        Score /= sys.planet().researchAdj();
        float avgScore = 0;
        float counted = 0;
        for (int id=0;id<empire.sv.count();id++) 
        {
            StarSystem current = galaxy().system(id);
            if(current.colony() == null)
                continue;
            if(current.empId() != empire.id)
                continue;
            if(current.colony().currentProductionCapacity() < 0.5)
                continue;
            float currentScore = current.colony().totalIncome();
            currentScore *= current.planet().productionAdj();
            currentScore /= current.planet().productionAdj();
            avgScore += currentScore;
            counted++;
        }
        avgScore /= counted;
        if(avgScore > 0)
            return Score/avgScore;
        return 0;
    }
}
