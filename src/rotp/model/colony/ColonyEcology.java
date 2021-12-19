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
package rotp.model.colony;

import rotp.model.empires.Empire;
import rotp.model.planet.Planet;
import rotp.model.tech.TechAtmosphereEnrichment;
import rotp.model.tech.TechTree;

public class ColonyEcology extends ColonySpendingCategory {
    private static final long serialVersionUID = 1L;
    private static final int SOIL_UPGRADE_BC = 150;
    private float hostileBC = 0;
    private float soilEnrichBC = 0;
    private float wasteCleaned = 0;
    private float unallocatedBC = 0;

    private float newGrownPopulation = 0;
    private float newPurchasedPopulation = 0;
    private float newBiosphereIncrease = 0;

    private boolean atmosphereCompleted = false;
    private boolean soilEnrichCompleted = false;
    private boolean terraformCompleted = false;
    private boolean populationGrowthCompleted = false;
    private int expectedPopGrowth = 0;

    public boolean atmosphereCompletedThisTurn()        { return atmosphereCompleted; }
    public boolean soilEnrichCompletedThisTurn()        { return soilEnrichCompleted; }
    public boolean terraformCompletedThisTurn()         { return terraformCompleted; }
    public boolean populationGrowthCompletedThisTurn()  { return populationGrowthCompleted && populationGrowthCompleted(); }
    public boolean populationGrowthCompleted()          { return colony().population() >= colony().maxSize(); }
    public boolean terraformCompleted()                 { return planet().currentSize() >= colony().maxSize(); }

    public void resetBiosphere() {
        hostileBC = 0;
        soilEnrichBC = 0;
    }
    public void init() {
        hostileBC = 0;
        soilEnrichBC = 0;
        planet().resetWaste();
        unallocatedBC = 0;

        wasteCleaned = 0;
        newGrownPopulation = 0;
        newPurchasedPopulation = 0;
        newBiosphereIncrease = 0;
    }
    @Override
    public int categoryType()    { return Colony.ECOLOGY; }
    @Override
    public boolean isCompleted() {
        return colony().population() >= colony().planet().maxSize()
            && (empire().ignoresPlanetEnvironment() || waste() == 0);
    }
    @Override
    public float orderedValue() {
        return max(super.orderedValue(),
                    colony().orderAmount(Colony.Orders.SOIL),
                    colony().orderAmount(Colony.Orders.ATMOSPHERE),
                    colony().orderAmount(Colony.Orders.TERRAFORM));
    }
    @Override
    public void removeSpendingOrders()   {
        colony().removeColonyOrder(Colony.Orders.SOIL);
        colony().removeColonyOrder(Colony.Orders.ATMOSPHERE);
        colony().removeColonyOrder(Colony.Orders.TERRAFORM);
    }
    public void capturedBy(Empire newCiv) {
        if (newCiv == empire())
            return;
        hostileBC = 0;
        soilEnrichBC = 0;
        unallocatedBC = 0;
        wasteCleaned = 0;
        newGrownPopulation = 0;
        newPurchasedPopulation = 0;
        newBiosphereIncrease = 0;
        atmosphereCompleted = false;
        soilEnrichCompleted = false;
        terraformCompleted = false;
        populationGrowthCompleted = false;
        expectedPopGrowth = 0;
    }
    public float waste()           { return planet().waste(); }
    public void addWaste(float w)  { planet().addWaste(w); }
    public float atmosphereTerraformCost() {
        return TechAtmosphereEnrichment.hostileTech.cost;
    }
    public float enrichSoilCost() {
        if (!tech().enrichSoil())
            return 0;

        int envDiff = tech().topSoilEnrichmentTech().environment - planet().environment();
        return Math.max(0, envDiff * SOIL_UPGRADE_BC);
    }
    public float terraformCost() {
        float roomToGrow = Math.max(0, colony().maxSize() - planet().currentSize());
        if (roomToGrow <= 0)
            return 0;

        return roomToGrow * tech().popIncreaseCost();
    }
    @Override
    public void nextTurn(float totalProd, float totalReserve) {
        Colony c = colony();
        Planet p = c.planet();
        Empire emp = c.empire();
        TechTree tr = emp.tech();
        newGrownPopulation = c.normalPopGrowth();
        wasteCleaned = 0;

        float prodBC = pct()* totalProd;
        float rsvBC = pct() * totalReserve;
        float newBC = prodBC+rsvBC;

        // add new waste created from this turn & clean it up
        addWaste(c.newWaste());
        wasteCleaned = emp.ignoresPlanetEnvironment() ? 0 : max(0, min((newBC * tr.wasteElimination()), waste()));
        newBC -= (wasteCleaned / tr.wasteElimination());

        // try to convert hostile atmosphere
        atmosphereCompleted = false;
        if (p.canTerraformAtmosphere(emp))  { 
            float hostileCost = min((atmosphereTerraformCost() - hostileBC), newBC);
            hostileCost = max(hostileCost,0);
            hostileBC += hostileCost;
            newBC -= hostileCost;
            atmosphereCompleted = hostileBC >= atmosphereTerraformCost();
            if (atmosphereCompleted) {
                hostileBC = 0;
                p.terraformAtmosphere();
            }
        }

        //if not Hostile & civ has SoilEnrichment that will improvement this environment,
        // then try to pay for soil enrichment...
        soilEnrichCompleted = false;
        if (!p.isEnvironmentHostile() && tr.enrichSoil() && (tr.topSoilEnrichmentTech().environment > p.environment()))  {
            float enrichCost = min(newBC,enrichSoilCost() - soilEnrichBC);
            enrichCost = max(enrichCost,0);
            soilEnrichBC += enrichCost;
            newBC -= enrichCost;
            while (soilEnrichBC >= SOIL_UPGRADE_BC) {
                soilEnrichBC -= SOIL_UPGRADE_BC;
                p.enrichSoil();
            }
            soilEnrichCompleted = p.environment() >= tr.topSoilEnrichmentTech().environment;
        }

        // try to terraform planet to maxSize
        float terraformCost = terraformCost();
        newBiosphereIncrease = 0;

        terraformCompleted = false;
        if ((newBC > 0) && (terraformCost > 0)) {
            terraformCost = min(newBC, terraformCost);
            newBiosphereIncrease = terraformCost / tr.topTerraformingTech().costPerMillion;
            newBC -= terraformCost;
            p.terraformBiosphere(newBiosphereIncrease);
            terraformCompleted = (newBiosphereIncrease > 0) && (p.currentSize() >= c.maxSize());
        }

        // try to buy new population
        populationGrowthCompleted = false;
        newPurchasedPopulation = 0;
        if (newBC > 0) {
            newPurchasedPopulation = newBC / tr.populationCost();
            newPurchasedPopulation = min(newPurchasedPopulation,(p.currentSize() - c.population() + c.inTransport() - newGrownPopulation));
            newPurchasedPopulation = max(newPurchasedPopulation,0);
            newBC -= (newPurchasedPopulation* tr.populationCost());
        }

        // for poor planets, we want to assume that as much
        // of the remaining BC left (ecoProd) is from reserve
        // this minimizes loss when sending back to the reserve
        float planetAdj = p.productionAdj();
        if (planetAdj < 1) {
            float unadjustedBC = min(rsvBC, newBC);
            float adjustedBC = (newBC - unadjustedBC) * planetAdj;
            unallocatedBC += (unadjustedBC + adjustedBC);
        }
        // for normal/rich planets, we want to assume that as much
        // of the remaining BC left (ecoProd) is from planetary
        // production.. to maximum BC send to the reserve
        else {
            float unadjustedBC = max(0, newBC-prodBC);
            float adjustedBC = (newBC - unadjustedBC) * planetAdj;
            unallocatedBC += (unadjustedBC + adjustedBC);
        }
    }
    @Override
    public void assessTurn() {
        Colony c = colony();
        float orderAmt = 0;
        if (atmosphereCompletedThisTurn()) {
            orderAmt = max(orderAmt, c.orderAmount(Colony.Orders.ATMOSPHERE));
            c.removeColonyOrder(Colony.Orders.ATMOSPHERE);
        }
        if (soilEnrichCompletedThisTurn()) {
            orderAmt = max(orderAmt, c.orderAmount(Colony.Orders.SOIL));
            c.removeColonyOrder(Colony.Orders.SOIL);
        }
        if (terraformCompletedThisTurn()) {
            orderAmt = max(orderAmt, c.orderAmount(Colony.Orders.TERRAFORM));
            c.removeColonyOrder(Colony.Orders.TERRAFORM);
        }
        if (populationGrowthCompletedThisTurn()) {
            orderAmt = max(orderAmt, c.orderAmount(Colony.Orders.POPULATION));
            c.removeColonyOrder(Colony.Orders.POPULATION);
        }
        
        c.addFollowUpSpendingOrder(orderAmt);
    }
    public void commitTurn() {
        Colony c = colony();
        addWaste(-wasteCleaned);
        c.setPopulation(c.population() + newGrownPopulation + newPurchasedPopulation);
        populationGrowthCompleted = ((newGrownPopulation+newPurchasedPopulation) > 0) && (c.population() >= c.maxSize());

        // if affected by waste, deduct population due to decreased planet size
        if (!empire().ignoresPlanetEnvironment()) {
            float pop = colony().population();
            float size = planet().sizeAfterWaste();
            if (pop > size) {
                float over = pop - size;
                float loss= over/pop * .1f * over;
                loss=min(loss,over);
                c.setPopulation(c.population()-loss);
            }
        }
        if (colony().population() < 0)
        {
            err("ERROR: bad pop for ", colony().name(), " pop:"+colony().population(), " newGrown:", str(newGrownPopulation), " newPurchased:", str(newPurchasedPopulation));
        }

        if (!empire().divertColonyExcessToResearch())
            empire().addReserve(unallocatedBC);
        unallocatedBC = 0;
    }
    public int upcomingPopGrowth() {
        upcomingResult();
        return expectedPopGrowth;
    }
    @Override
    public boolean warning() {
        if (empire().ignoresPlanetEnvironment())
            return false;
        
        float prodBC = pct()* colony().totalProductionIncome();
        float rsvBC = pct() * colony().maxReserveIncome();
        float newBC = prodBC+rsvBC; 
        
        return (newBC < colony().wasteCleanupCost());
    }
    @Override
    public String upcomingResult(){
        Colony c = colony();
        
        float prodBC = pct()* c.totalProductionIncome();
        float rsvBC = pct() * c.maxReserveIncome();
        float newBC = prodBC+rsvBC;
        float cost;

        // new population
        float currentPop = c.population();
        float workingPop = c.workingPopulation(); // currentpop - transports away
        float expGrowth = c.normalPopGrowth();
        expectedPopGrowth = (int) (workingPop+expGrowth) - (int) currentPop;

        // check for waste cleanup
        cost = c.wasteCleanupCost();
        if (newBC < cost) 
            return text(wasteText);
        
        if (c.allocation(categoryType()) == 0)
            return text(noneText);
        if (allocation() == cleanupAllocationNeeded())
            return text(cleanupText);

        newBC -= cost;
        // check for atmospheric terraforming
        Empire emp = c.empire();
        Planet p = c.planet();
        boolean canTerraformAtmosphere = p.canTerraformAtmosphere(emp);
        if (canTerraformAtmosphere) {
            cost = atmosphereTerraformCost() - hostileBC;
            if (newBC < cost)
                return text(atmosphereText);
            newBC -= cost;
        }

        // check for soil enrichment
        TechTree tr = emp.tech();
        if ((! p.isEnvironmentHostile()) || canTerraformAtmosphere) {
            if (tr.enrichSoil()) {
                int envUpgrade = tr.topSoilEnrichmentTech().environment - p.environment();
                if (envUpgrade > 0) {
                    cost = ((tr.topSoilEnrichmentTech().environment - p.environment()) * SOIL_UPGRADE_BC) - soilEnrichBC;
                    if (newBC < cost)
                        return text(enrichSoilText);
                    newBC -= cost;
                }
            }
        }

        // check for terraforming
        float maxPopSize = c.maxSize();
        float roomToGrow = maxPopSize - p.currentSize();
        if (roomToGrow > 0) {
            cost = roomToGrow * tr.topTerraformingTech().costPerMillion;
            if (newBC < cost) 
                 return text(terraformText);
            newBC -= cost;
        }

        // check for purchasing new pop
        float newPopPurchaseable = maxPopSize - workingPop - expGrowth;
        float newPopCost = tr.populationCost();
        if (newPopPurchaseable > 0) {
            cost = newPopPurchaseable * newPopCost;
            int newPop = (int) (workingPop+expGrowth+(newBC / newPopCost)) - (int) currentPop;
            expectedPopGrowth = newPop;
            if (newBC < cost)
                return text(growthText);
            newBC -= cost;
        }

        expectedPopGrowth = (int) maxPopSize - (int) currentPop;

        // if less <1% of income, show "Clean", else show "Reserve"
        if (newBC <= (c.totalIncome()/100))
            return text(growthText);
        else
            return overflowText();
    }
    @Override
    public float excessSpending() {
        Colony c = colony();
        if (c.allocation(categoryType()) == 0)
            return 0;
        
        float prodBC = pct()* c.totalProductionIncome();
        float rsvBC = pct() * c.maxReserveIncome();
        float totalBC = prodBC+rsvBC;        
        
        // deduct cost to clean industrial waste
        float cleanCost = c.wasteCleanupCost();
        if (totalBC <= cleanCost)
            return 0;

        totalBC -= cleanCost;
        
        Planet p = c.planet();
        Empire emp = c.empire();
        boolean canTerraformAtmosphere = p.canTerraformAtmosphere(emp);
        // deduct cost for atmospheric terraforing
        if (canTerraformAtmosphere) {
            float atmoCost = atmosphereTerraformCost() - hostileBC;
            if (totalBC <= atmoCost)
                return 0;
            totalBC -= atmoCost;
        }

        // deduct cost for soil enrichment
        TechTree tr = emp.tech();
        if (!emp.ignoresPlanetEnvironment()) {
            if ((! p.isEnvironmentHostile()) || canTerraformAtmosphere) {
                if (tr.enrichSoil()) {
                    int envUpgrade = tr.topSoilEnrichmentTech().environment - p.environment();
                    if (envUpgrade > 0) {
                        float enrichCost = (envUpgrade * SOIL_UPGRADE_BC) - soilEnrichBC;
                        if (totalBC < enrichCost)
                            return 0;
                        totalBC -= enrichCost;
                    }
                }
            }
        }        
        
        // deduct cost for size terraforming
        float maxPopSize = c.maxSize();
        float roomToGrow = maxPopSize - p.currentSize();
        if (roomToGrow > 0) {
            float tformCost = roomToGrow * tr.topTerraformingTech().costPerMillion;
            if (totalBC < tformCost) 
                return 0;
            totalBC -= tformCost;
        }
        
        // deduct cost for purchasing new pop
        float expGrowth = c.normalPopGrowth();
        float workingPop = c.workingPopulation(); // currentpop - transports away
        float newPopPurchaseable = maxPopSize - workingPop - expGrowth;
        if (newPopPurchaseable > 0) {
            float newPopCost = tr.populationCost();
            float growthCost = newPopPurchaseable * newPopCost;
            if (totalBC < growthCost)
                return 0;
            totalBC -= growthCost;
        } 
        
        return max(0,totalBC);
    }
    public float maxSpendingNeeded() {
        // cost to terraform planet
        float tform = terraformSpendingNeeded();
        // try to buy new population
        float newPopCost = (colony().maxSize() - colony().workingPopulation() - colony().normalPopGrowth()) * tech().populationCost();
        newPopCost = max(0,newPopCost);
        return tform + newPopCost;
    }
    public float terraformSpendingNeeded() {
        float cleanCost = colony().minimumCleanupCost();
        Empire emp = empire();
        TechTree tech = emp.tech();
        Planet planet = planet();
        // try to convert hostile atmosphere
        float hostileCost = 0;

        if (planet.canTerraformAtmosphere(emp))
            hostileCost = max(0, atmosphereTerraformCost() - hostileBC);

        // don't count enrichSoil cost unless not hostile or civ can terraform hostile
        float enrichCost = 0;
        if (!emp.ignoresPlanetEnvironment()) {
            if (!planet.isEnvironmentHostile() || planet.canTerraformAtmosphere(emp)) {
                if (tech.enrichSoil() && (tech.topSoilEnrichmentTech().environment > planet.environment())) {
                    enrichCost = ((tech.topSoilEnrichmentTech().environment - planet.environment()) * 150) - soilEnrichBC;
                    enrichCost = max(0,enrichCost);
                }
            }
        }
        // try to terraform planet to maxSize (currently not counting incr from previous terraforms)
        float roomToGrow = colony().maxSize() - planet.currentSize();
        float terraformCost = 0;

        if (roomToGrow > 0) {
            terraformCost = roomToGrow * tech.popIncreaseCost();
            terraformCost = max(0,terraformCost);
        }

        return cleanCost + hostileCost + enrichCost + terraformCost;
    }
    public int terraformAllocationNeeded() {
        float needed = terraformSpendingNeeded();
        if (needed == 0)
            return 0;
        float prod = colony().totalProductionIncome() + colony().maxReserveIncome();
        float pctNeeded = min(1, needed / prod);
        int ticks = (int) Math.ceil(pctNeeded * MAX_TICKS);
        return ticks;
    }
    public int cleanupAllocationNeeded() {
        float needed = colony().minimumCleanupCost();
        if (needed == 0)
            return 0;
        float prod = colony().totalIncome();
        float pctNeeded = min(1, needed / prod);
        int ticks = (int) Math.ceil(pctNeeded * MAX_TICKS);
        return ticks;
    }
    public int maxAllocationNeeded() {
        float needed = maxSpendingNeeded();
        if (needed <= 0)
            return 0;
        float prod = colony().totalProductionIncome() + colony().maxReserveIncome();
        float pctNeeded = min(1, needed / prod);
        int ticks = (int) Math.ceil(pctNeeded * MAX_TICKS);
        return ticks;
    }
}
