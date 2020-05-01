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

    public boolean atmosphereCompleted()         { return atmosphereCompleted; }
    public boolean soilEnrichCompleted()         { return soilEnrichCompleted; }
    public boolean terraformCompleted()         { return terraformCompleted; }
    public boolean populationGrowthCompleted()   { return populationGrowthCompleted; }

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
            && (empire().race().ignoresPlanetEnvironment() || waste() == 0);
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
        hostileBC = 0;
        soilEnrichBC = 0;
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
        wasteCleaned = emp.race().ignoresPlanetEnvironment() ? 0 : max(0, min((newBC * tr.wasteElimination()), waste()));
        newBC -= (wasteCleaned / tr.wasteElimination());

        // try to convert hostile atmosphere
        atmosphereCompleted = false;
        if (p.canTerraformAtmosphere(emp))  {
            float hostileCost = min((atmosphereTerraformCost() - hostileBC), newBC);
            hostileCost = max(hostileCost,0);
            hostileBC += hostileCost;
            newBC -= hostileCost;
            if (hostileBC >= atmosphereTerraformCost()) {
                atmosphereCompleted = true;
                hostileBC = 0;
                p.terraformAtmosphere();
                float orderAmt = c.orderAmount(Colony.Orders.ATMOSPHERE);
                if (orderAmt > 0) {
                    c.removeColonyOrder(Colony.Orders.ATMOSPHERE);
                    c.addColonyOrder(Colony.Orders.TERRAFORM, orderAmt);
                }
            }
        }

        //if not Hostile & civ has SoilEnrichment that will improvement this environment,
        // then try to pay for soil enrichment... silicoids cannot do this
        if (!emp.race().ignoresPlanetEnvironment()) {
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
                if (soilEnrichCompleted) {
                    float orderAmt = c.orderAmount(Colony.Orders.SOIL);
                    if (orderAmt > 0) {
                        c.removeColonyOrder(Colony.Orders.SOIL);
                        c.addColonyOrder(Colony.Orders.TERRAFORM, orderAmt);
                    }
                }
            }
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
            if (terraformCompleted) {
                float orderAmt = c.orderAmount(Colony.Orders.TERRAFORM);
                if (orderAmt > 0) {
                    c.removeColonyOrder(Colony.Orders.TERRAFORM);
                    c.addColonyOrder(Colony.Orders.FACTORIES, orderAmt);
                }
            }
        }

        // try to buy new population
        populationGrowthCompleted = false;
        newPurchasedPopulation = 0;
        if (newBC > 0) {
            newPurchasedPopulation = newBC / tr.populationCost();
            newPurchasedPopulation = min(newPurchasedPopulation,(p.currentSize() - c.population() + c.inTransport() - newGrownPopulation));
            newPurchasedPopulation = max(newPurchasedPopulation,0);
            newBC -= (newPurchasedPopulation* tr.populationCost());
            populationGrowthCompleted = (newPurchasedPopulation > 0) && (c.population() >= c.maxSize());
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
        populationGrowthCompleted = ((newGrownPopulation+newPurchasedPopulation) > 0) && (colony().population() >= colony().maxSize());
        if (populationGrowthCompleted) {
            float orderAmt = colony().orderAmount(Colony.Orders.TERRAFORM);
            if (orderAmt > 0) {
                colony().removeColonyOrder(Colony.Orders.TERRAFORM);
                colony().addColonyOrder(Colony.Orders.FACTORIES, orderAmt);
            }
        }
    }
    public void commitTurn() {
        addWaste(-wasteCleaned);
        colony().setPopulation(colony().population() + newGrownPopulation);
        colony().setPopulation(colony().population() + newPurchasedPopulation);

        // if affected by waste, deduct population due to decreased planet size
        if (!empire().race().ignoresPlanetEnvironment()) {
            float pop = colony().population();
            float size = planet().sizeAfterWaste();
            if (pop > size) {
                float over = pop - size;
                float loss= over/pop * .1f * over;
                colony().setPopulation(colony().population()-loss);
            }
        }
        if (colony().population() < 0)
            err("ERROR: bad pop for ", colony().name(), " pop:"+colony().population(), " newGrown:", str(newGrownPopulation), " newPurchased:", str(newPurchasedPopulation));

        empire().addReserve(unallocatedBC);
        unallocatedBC = 0;
    }
    public int upcomingPopGrowth() {
        upcomingResult();
        return expectedPopGrowth;
    }
    @Override
    public boolean warning() {
        if (empire().race().ignoresPlanetEnvironment())
            return false;
        
        float prodBC = pct()* colony().totalProductionIncome();
        float rsvBC = pct() * colony().maxReserveIncome();
        float newBC = prodBC+rsvBC; 
        
        return (newBC < colony().wasteCleanupCost());
    }
    @Override
    public String upcomingResult(){
        Colony c = colony();
        Planet p = c.planet();
        Empire emp = c.empire();
        TechTree tr = emp.tech();

        float prodBC = pct()* c.totalProductionIncome();
        float rsvBC = pct() * c.maxReserveIncome();
        float newBC = prodBC+rsvBC;
        float cost;

        // new population
        float currentPop = c.population();
        float expGrowth = c.normalPopGrowth();
        float newPopCost = tr.populationCost();
        float workingPop = c.workingPopulation(); // currentpop - transports away
        float newPopPurchaseable = c.maxSize() - workingPop - expGrowth;
        expectedPopGrowth = (int) workingPop - (int) currentPop;
        
        // check for waste cleanup
        cost = c.wasteCleanupCost();
        if (newBC < cost) 
            return text(wasteText);
        
        if (colony().allocation(categoryType()) == 0)
            return text(noneText);
        if (allocation() == cleanupAllocationNeeded())
            return text(cleanupText);

        expectedPopGrowth = (int) (workingPop+expGrowth) - (int) currentPop;
        newBC -= cost;
        // check for atmospheric terraforming
        cost = 0;
        if (planet().canTerraformAtmosphere(empire()))
            cost = atmosphereTerraformCost() - hostileBC;

        if (newBC < cost)
            return text(atmosphereText);

        newBC -= cost;

        // check for soil enrichment, not for silicoids
        if (!emp.race().ignoresPlanetEnvironment()) {
            cost = 0;
            if ((! p.isEnvironmentHostile()) || p.canTerraformAtmosphere(empire())) {
                if (tr.enrichSoil() && (tr.topSoilEnrichmentTech().environment > p.environment()))
                    cost = ((tr.topSoilEnrichmentTech().environment - p.environment()) * SOIL_UPGRADE_BC) - soilEnrichBC;
            }
            if (newBC < cost)
                return text(enrichSoilText);
            newBC -= cost;
        }

        // check for terraforming
        float roomToGrow = c.maxSize() - p.currentSize();
        cost = 0;
        if (roomToGrow > 0)
            cost = roomToGrow * tr.topTerraformingTech().costPerMillion;

        if (newBC < cost) 
             return text(terraformText);
 
        newBC -= cost;

        cost = max(0, newPopPurchaseable * newPopCost);
        if (newBC < cost) {
            int newPop = (int) (workingPop+expGrowth+(newBC / newPopCost)) - (int) currentPop;
            expectedPopGrowth = newPop;
            return text(growthText);
        }

        newBC -= cost;
        expectedPopGrowth = (int) c.maxSize() - (int) currentPop;

        // if less <1% of income, show "Clean", else show "Reserve"
        if (newBC <= (c.totalIncome()/100))
            return text(growthText);
        else
            return text(reserveText);
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
        Empire emp = empire();
        TechTree tech = emp.tech();
        Planet planet = planet();
        // try to convert hostile atmosphere
        float hostileCost = 0;

        if (planet.canTerraformAtmosphere(emp))
            hostileCost = max(0, atmosphereTerraformCost() - hostileBC);

        // don't count enrichSoil cost unless not hostile or civ can terraform hostile
        float enrichCost = 0;
        if (!emp.race().ignoresPlanetEnvironment()) {
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

        return hostileCost + enrichCost + terraformCost;
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
        float prod = colony().totalProductionIncome() + colony().maxReserveIncome();
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
