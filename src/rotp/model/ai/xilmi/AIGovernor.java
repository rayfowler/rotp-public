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
import static rotp.model.colony.ColonySpendingCategory.MAX_TICKS;
import rotp.model.combat.CombatStackColony;
import rotp.model.combat.ShipCombatManager;
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
import rotp.ui.UserPreferences;
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
        //System.out.println(galaxy().currentTurn()+" "+empire.name()+" "+col.name()+" estProd: "+estProd+" designCost: "+designCost+" sh.plan.priority(): "+sh.plan.priority()+" Repel: "+FleetPlan.REPEL+" enemy-fleet: "+!empire.enemyFleets().isEmpty());
        return estProd > designCost;
    }
    @Override
    public void setColonyAllocations(Colony col) {
        if (empire.isAIControlled()) {
            baseSetColonyAllocations(col);
            ensureMinimumCleanup(col);
            col.validate();
            //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+col.name()+" col.pct(SHIP): "+col.pct(SHIP)+" col.pct(DEFENSE): "+col.pct(DEFENSE)+" col.pct(INDUSTRY): "+col.pct(INDUSTRY)+" col.pct(ECOLOGY): "+col.pct(ECOLOGY)+" col.pct(TECH): "+col.pct(RESEARCH)+" yard: "+col.shipyardProject());
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

        // if this is a ship-building-colony that is not researching put rest in ships
        if(!col.locked(SHIP)
                && prevShip > 0
                && prevRes == 0
                && !col.shipyard().shipLimitReached()
                && !col.shipyard().stargateCompleted())
            col.addAllocation(SHIP, col.allocationRemaining());
        
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

        if(col.shipyard().canLowerMaintenance())
            col.shipyard().lowerMaintenance();
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

        //System.out.print("\n"+empire.name()+" col.shipyard().maxSpendingNeeded(): "+col.shipyard().maxSpendingNeeded()+" bldg: "+col.shipyard().design().id()+ " active: "+col.shipyard().design().active());
        boolean needToMilitarize = false;
        if(empire.atWar() || empire.generalAI().sensePotentialAttack())
        {
            if(empire.diplomatAI().militaryRank(empire, false) > empire.diplomatAI().popCapRank(empire, false))
                if(col.currentProductionCapacity() > 0.5f)
                    needToMilitarize = true;
        }
        float netFactoryProduction = 1;
        if(!empire.ignoresPlanetEnvironment())
            netFactoryProduction -= empire.tech().factoryWasteMod() / empire.tech().wasteElimination();
        float workerROI = empire.tech().populationCost() / empire.workerProductivity();
        float factoryROI = empire.tech().newFactoryCost(col.industry().robotControls()) / col.planet().productionAdj() / netFactoryProduction;
        if(col.industry().factories() > 0)
            factoryROI = (empire.tech().newFactoryCost(col.industry().robotControls()) + col.industry().upgradeCost() / col.industry().factories()) / col.planet().productionAdj() / netFactoryProduction;
        //if our factories need to be refitted
        if(col.industry().factories() > col.maxUseableFactories())
            factoryROI += workerROI;
        float popGrowthROI = Float.MAX_VALUE;
        if(col.normalPopGrowth() > 0)
            popGrowthROI = empire.tech().populationCost() / col.normalPopGrowth();
        float maxShipBC = maxShipBCPermitted(col);
        float prodScore = productionScore(col.starSystem());
        float factoriesNeeded = max(0, col.maxUseableFactories() + col.normalPopGrowth() * empire.maxRobotControls() - col.industry().factories());
        float workerGoal = max(0, col.industry().factories() / empire.maxRobotControls() - col.workingPopulation() - col.normalPopGrowth());
        if(popGrowthROI > workerROI && !needToMilitarize)
            workerGoal = col.maxSize() - col.workingPopulation();
        
        workerGoal -= empire.transportsInTransit(col.starSystem());
        
        //float colShipTime = empire.shipLab().colonyDesign().cost() / (col.totalIncome() - col.minimumCleanupCost()) / col.planet().productionAdj();
        
        boolean buildingVitalShip = false;
        //Mostly for Sakkra and Meklar, so they expand quicker when they can 72% pop is where the growth drops below 80%
        if(col.shipyard().desiredShips() > 0
                && (col.currentProductionCapacity() > 0.5f || col.production() > col.shipyard().design().cost()))
        {
            workerGoal = 0;
            buildingVitalShip = true;
        }
        
        //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+col.name()+" popGrowthROI: "+popGrowthROI+" colship-time: "+colShipTime);
        //System.out.print("\n"+empire.name()+" "+col.name()+" workerROI: "+workerROI+" popGrowthROI: "+popGrowthROI+" factoryROI: "+factoryROI+" prodScore: "+prodScore+" factoriesNeeded: "+factoriesNeeded+" workergoal: "+workerGoal+" needToMilitarize: "+needToMilitarize);
        //System.out.print("\n"+empire.name()+" "+col.name()+" workerROI: "+workerROI+" popGrowthROI: "+popGrowthROI+" factoryROI: "+factoryROI+" warROI: "+warROI+" techROI: "+techROI);
        
        suggestMissileBaseCount(col);
        col.clearSpending();
        
        lowerExpenses(col);
        float totalProd = col.totalIncome();
        float cleanCost = col.minimumCleanupCost();
        // calculate minimum eco cleanup pct
        col.pct(ECOLOGY, cleanCost/totalProd);
        float netProd = totalProd - totalProd * col.pct(ECOLOGY);
        float shipCost = 0;
        
        //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+col.name()+" totalProd: "+totalProd+" netProd: "+netProd+" TPI: "+col.totalProductionIncome()+" Res: "+col.maxReserveIncome());

        if (col.allocation(ECOLOGY) < 0) {
            err("Minimum cleanup cost < 0");
            throw new RuntimeException("Minimum cleanup cost < 0");
        }

        // don't change ship allocation for players else reset to zero
        if (empire.isPlayerControlled()) {
            shipCost = totalProd * col.pct(SHIP);
            if (shipCost > netProd) {
                col.pct(SHIP, netProd/totalProd);
            }
        }
        else {
            col.pct(SHIP, 0);
        }
        if (col.totalAmountAllocated() >= maxAllocation)
        {
            return;
        }

        float enemyBombardPower = 0.0f;
        
        for(ShipFleet fleet:col.starSystem().orbitingFleets())
        {
            if(fleet.empire().aggressiveWith(col.empire().id))
            {
                if(empire.visibleShips().contains(fleet))
                    enemyBombardPower += fleet.expectedBombardDamage();
            }
        }
        float popLoss = enemyBombardPower / 200;

        if(buildingVitalShip)
        {
            if(col.shipyard().design() == empire.shipDesignerAI().BestDesignToColonize())
                netProd -= col.shipyard().design().cost();
            else
                netProd -= min(col.shipyard().maxSpendingNeeded(), col.shipyard().design().cost());
        }
        
        // prod spending gets up to 100% of planet's remaining net prod
        if((col.industry().factories() < col.maxUseableFactories() + (col.normalPopGrowth() + empire.transportsInTransit(col.starSystem())) * empire.maxRobotControls())
            && enemyBombardPower == 0
            && !needToMilitarize)
        {
            float prodCost = min(netProd, col.industry().maxSpendingNeeded(), factoriesNeeded * empire.tech().newFactoryCost(col.industry().robotControls()));
            int alloc = (int)Math.ceil(prodCost/totalProd*MAX_TICKS);
            alloc = min(alloc, col.allocationRemaining());
            col.allocation(INDUSTRY, alloc);
            prodCost = col.pct(INDUSTRY) * totalProd;
            netProd -= prodCost;
            if (col.totalAmountAllocated() >= maxAllocation)
            {
                return;
            }
        }
        //for experiment:
        float workerCost = workerGoal * empire.tech().populationCost();
        workerCost += col.ecology().terraformCost();
        
        // eco spending gets up to 100% of planet's remaining net prod
        if(popLoss * empire.tech().populationCost() > totalProd)
            workerCost = 0;
        
        float ecoCost = max(0, min(netProd, workerCost));
        col.pct(ECOLOGY, (ecoCost + cleanCost)/totalProd);
        
        if (col.pct(ECOLOGY) < 0) {
            err("Eco pct < 0");
            throw new RuntimeException("Minimum cleanup cost < 0");
        }

        if (col.totalAmountAllocated() >= maxAllocation)
        {
            return;
        }

        // ail: Remove spending limit since bases are now only built at border and we want to get it over with quickly
        // ail: only build defense when a shield is needed. Otherwise never worth it
        
        if (col.shipyard().desiredShips() > 0){
            shipCost = min(maxShipBC, col.shipyard().maxSpendingNeeded());
            float shipPct = shipCost/totalProd;
            if(col.shipyard().design().isShip())
            {
                ShipDesign d = (ShipDesign)col.shipyard().design();
                if(d.hasColonySpecial())
                    shipPct = 1;
            }
            col.pct(SHIP, shipPct);
            //System.out.println("\n"+empire.name()+" "+col.name()+" shipPct: "+shipPct+" shipCost-A: "+shipCost+" maxShipBC: "+maxShipBC+" col.shipyard().maxSpendingNeeded(): "+col.shipyard().maxSpendingNeeded()+" totalProd: "+totalProd+" totalAlloc: "+totalAlloc);
            //System.out.println("\n"+empire.name()+" "+col.name()+" shipPct: "+shipPct+" col.pct(SHIP): "+col.pct(SHIP)+" col.pct(ECOLOGY): "+col.pct(ECOLOGY)+" shipCost-B: "+shipCost+" maxShipBC: "+maxShipBC+" totalProd: "+totalProd);
        }
        
        if(wantShield(col) || col.defense().maxBases() > col.defense().bases())
        {
            float defCost = col.defense().maxSpendingNeeded();
            col.pct(DEFENSE, defCost/totalProd);
        }

        if (col.totalAmountAllocated() >= maxAllocation)
        {
            return;
        }

        // research gets the rest
        // ail: build military, if we want
        // only if we are not already producing ships for other purposes
        int totalAlloc = col.allocation(SHIP)+col.allocation(DEFENSE)+col.allocation(INDUSTRY)+col.allocation(ECOLOGY);
        ShipDesignLab lab = empire.shipLab();
        //System.out.print("\n"+empire.name()+" "+col.name()+" colonizer-production-score "+productionScore(col.starSystem(), true));
        boolean enemy = false;
        for(Empire emp : empire.contactedEmpires())
        {
            EmpireView v = empire.viewForEmpire(emp);
            if(v.embassy().isEnemy() && empire.inShipRange(emp.id))
            {
                enemy = true;
            }
        }
        int[] counts = galaxy().ships.shipDesignCounts(empire.id);
        float fighterCost = 0.0f;
        float bomberCost = 0.0f;
        for (int i=0;i<counts.length;i++) 
        {
            if(lab.design(i).hasColonySpecial())
                continue;
            fighterCost += lab.design(i).cost() * counts[i] * empire.shipDesignerAI().fightingAdapted(lab.design(i));
            bomberCost += lab.design(i).cost() * counts[i] * empire.shipDesignerAI().bombingAdapted(lab.design(i));
        }
        //ail: No use to build any ships if they won't do damage anyways. Better tech up.
        boolean viableForShipProduction = prodScore >= 1 || needToMilitarize;
        float turnsBeforeColonyDestroyed = Float.MAX_VALUE;
        if(popLoss > 0)
            turnsBeforeColonyDestroyed = col.population() / popLoss;
        float fighterBuildTime = empire.shipDesignerAI().BestDesignToFight().cost() / totalProd;
        if(fighterBuildTime > turnsBeforeColonyDestroyed)
            viableForShipProduction = false;
        //System.out.print("\n"+empire.name()+" "+col.name()+" production-score "+productionScore(col.starSystem())+" needToMilitarize: "+needToMilitarize+" viableForShipProduction: "+viableForShipProduction+" Mil-Rank: "+empire.diplomatAI().militaryRank(empire, false)+" Pop-Rank: "+empire.diplomatAI().popCapRank(empire, false));
        //System.out.print("\n"+empire.name()+" "+col.name()+" col.allocation(SHIP): "+col.allocation(SHIP));
        if(col.allocation(SHIP) == 0 && viableForShipProduction)
        {
            float maxShipMaintainance = 0.0f;
            float fighterPercentage = empire.generalAI().defenseRatio();
            
            if(enemy || empire.generalAI().sensePotentialAttack())
            {
                maxShipMaintainance = empire.fleetCommanderAI().maxShipMaintainance();
            }

            float maxShipMaintainanceBeforeAdj = maxShipMaintainance;
            maxShipMaintainance *= prodScore;
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
            //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+col.name()+" adjMaxMaint: "+maxShipMaintainance+" baseMaxMaint: "+maxShipMaintainanceBeforeAdj+" CurrMaint: "+empire.shipMaintCostPerBC());
            col.shipyard().design(empire.shipDesignerAI().BestDesignToFight());
            //System.out.print("\n"+empire.name()+" fighterCost: "+fighterCost+" bomberCost: "+bomberCost+" F% reached: "+fighterCost / (bomberCost + fighterCost)+" of "+fighterPercentage);
            if(fighterCost / (bomberCost + fighterCost) > fighterPercentage 
                && enemyBombardPower == 0)
            {
                if(empire.shipDesignerAI().BestDesignToBomb() != null)
                    col.shipyard().design(empire.shipDesignerAI().BestDesignToBomb());
            }
            if(empire.shipMaintCostPerBC() < maxShipMaintainance)
            {
                col.allocation(SHIP, maxAllocation - totalAlloc);
            }
        }
        totalAlloc = col.allocation(SHIP)+col.allocation(DEFENSE)+col.allocation(INDUSTRY)+col.allocation(ECOLOGY);
        col.allocation(RESEARCH, maxAllocation - totalAlloc);

        //ail: Rich and Ultra-Rich that are doing research which is not a project should put their stuff into reserve instead of conducting research
        boolean shiftResearchToIndustry = false;
        if(prodScore > 1 && (col.planet().isResourceRich() || col.planet().isResourceUltraRich()) && !col.research().hasProject() && col.pct(RESEARCH) > 0)
        {
            shiftResearchToIndustry = true;
            if(empire.divertColonyExcessToResearch())
                empire.toggleColonyExcessToResearch();
        }
        if(enemyBombardPower > 0)
            shiftResearchToIndustry = false;
        
        if (shiftResearchToIndustry) {
            float rsvAmt = col.pct(RESEARCH);
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
        if(!col.defense().shieldAtMaxLevel()) {
            for(Empire emp : empire.contactedEmpires())
            {
                if(empire.friendlyWith(emp.id))
                    continue;
                if(emp.sv.inShipRange(sys.id))
                    return true;
            }
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
        float enemyBombardDamage = 0;
        float enemyBc = 0;
        boolean allowBases = false;
        for(ShipFleet fl : col.starSystem().incomingFleets())
        {
            if(fl.empire().aggressiveWith(empire.id))
            {
                if(!empire.visibleShips().contains(fl))
                    continue;
                enemyBombardDamage += expectedBombardDamageAsIfBasesWereThere(fl, col.starSystem());
                if(fl.isArmed())
                    enemyBc += fl.bcValue();
            }
        }
        for(ShipFleet fl : col.starSystem().orbitingFleets())
        {
            if(fl.empire().aggressiveWith(empire.id))
            {
                if(!empire.visibleShips().contains(fl))
                    continue;
                enemyBombardDamage += expectedBombardDamageAsIfBasesWereThere(fl, col.starSystem());
                if(fl.isArmed())
                    enemyBc += fl.bcValue();
            }
        }
        //System.out.print("\n"+empire.name()+" "+col.name()+" expected bombard-Damage: "+enemyBombardDamage+" Bc: "+enemyBc);
        if(enemyBc > 0 && enemyBombardDamage == 0)
            allowBases = true;
        if (sys == null)  // this can happen at startup
            col.defense().maxBases(0);
        else if (allowBases)
            col.defense().maxBases(max(currBases, 1));
        /*
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
        //too risky during war
        if(!empire.warEnemies().isEmpty())
        {
            for(Empire warEnemy : empire.warEnemies())
            {
                if(warEnemy.sv.inShipRange(sysId))
                    return 0;
            }
        }
        for(ShipFleet fl : sv.system().orbitingFleets())
        {
            if(fl.isArmed() && empire.enemies().contains(fl.empire()))
                return 0;
        }
        if(empire.generalAI().totalEmpirePopulationCapacity(empire) > 0)
        {
            float tgtPercentage = empire.totalEmpirePopulation() / empire.generalAI().totalEmpirePopulationCapacity(empire);
            Planet p = sv.system().planet();
            tgtPercentage *= p.productionAdj() * p.researchAdj();
            //Systems that are building colony-ships should keep their population
            float factoryTgt = tgtPercentage;
            tgtPercentage = max(factoryTgt, tgtPercentage);
            if(tgtPercentage <= 1)
                tgtPercentage = min(0.9f, tgtPercentage);
            else
                tgtPercentage = 1;
            return tgtPercentage;
        }
        return 0;
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
        return 1;
    }
    @Override
    public float productionScore(StarSystem sys)
    {
        float Score = sqrt(max(0, sys.colony().totalIncome()));
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
            float currentScore = sqrt(current.colony().totalIncome());
            currentScore *= current.planet().productionAdj();
            currentScore /= current.planet().researchAdj();
            avgScore += currentScore;
            counted++;
        }
        avgScore /= counted;
        if(avgScore > 0)
            return Score/avgScore;
        return 0;
    }
    public float expectedBombardDamageAsIfBasesWereThere(ShipFleet fl, StarSystem sys) {
        if (!sys.isColonized())
            return 0;

        float damage = 0;
        ShipCombatManager mgr = galaxy().shipCombat();
        CombatStackColony planetStack = new CombatStackColony(sys.colony(), mgr);
        planetStack.num = 1;

        for (int i=0;i<fl.num.length;i++) {
            if (fl.num[i] > 0) {
                ShipDesign d = fl.empire().shipLab().design(i);
                for (int j=0;j<ShipDesign.maxWeapons();j++)
                    if(!d.weapon(j).isBioWeapon())
                        damage += (fl.num[i] * d.wpnCount(j) * d.weapon(j).estimatedBombardDamage(d, planetStack));
                for (int j=0;j<ShipDesign.maxSpecials();j++)
                    damage += d.special(j).estimatedBombardDamage(d, planetStack);
            }
        }
        return damage;
    }
}
