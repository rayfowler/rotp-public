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
import rotp.model.tech.TechRoboticControls;

public class ColonyIndustry extends ColonySpendingCategory {
    private static final long serialVersionUID = 1L;
    private float factories = 0;
    private float previousFactories = 0;
    private int robotControls = 2;
    private float industryReserveBC = 0;
    private float unallocatedBC = 0;
    private float newFactories = 0;

    @Override
    public void init(Colony c) {
        super.init(c);
        factories = 0;
        robotControls = TechRoboticControls.BASE_ROBOT_CONTROLS;
        industryReserveBC = 0;
        unallocatedBC = 0;
        newFactories = 0;
    }
    @Override
    public int categoryType()               { return Colony.INDUSTRY; }
    public float factories()               { return factories; }
    public void factories(float d)         { factories = d; }
    public void previousFactories(float d) { previousFactories = d; }
    public int deltaFactories()             { return (int)factories - (int)previousFactories; }
    public int robotControls()              { return robotControls; }
    public float newFactoryCost()          { return tech().newFactoryCost(robotControls()); }
    public int effectiveRobotControls()     { return robotControls() + empire().race().robotControlsAdj(); }
    public int maxRobotControls()           { return tech().baseRobotControls() + empire().race().robotControlsAdj(); }
    @Override
    public float totalBC()              { return super.totalBC() * planet().productionAdj(); }
    public float maxFactories()         { return planet().maxSize() * maxRobotControls(); }
    public int maxBuildableFactories()   { return (int) (planet().currentSize() * maxRobotControls()); }
    public int maxBuildableFactories(int rc)   { return (int) (planet().currentSize() * (rc+empire().race().robotControlsAdj())); }
    public int maxUseableFactories()     { return maxUseableFactories(robotControls()); }
    public int maxUseableFactories(int rc) { return (int) colony().population() * (rc+empire().race().robotControlsAdj()); }
    @Override
    public boolean isCompleted()         { return factories >= maxFactories(); }
    public boolean isCompletedThisTurn() { return isCompleted() && (newFactories > 0); }
    @Override
    public float orderedValue()         { return max(super.orderedValue(), colony().orderAmount(Colony.Orders.FACTORIES)); }
    @Override
    public void removeSpendingOrders()   { colony().removeColonyOrder(Colony.Orders.FACTORIES); }
    public void capturedBy(Empire newCiv) {
        if (newCiv == empire())
            return;

        Planet p = planet();
        p.addAlienFactories(empire().id, (int) factories);
        industryReserveBC = 0;
        factories = p.alienFactories(newCiv.id);
        p.removeAlienFactories(newCiv.id);
    }
    @Override
    public void nextTurn(float totalProd, float totalReserve) {
        previousFactories = factories;
        // prod gets planetary bonus, but not reserve
        float prodBC = pct()* totalProd * planet().productionAdj();
        float rsvBC = pct() * totalReserve;
        float newBC = prodBC+rsvBC+industryReserveBC;
        industryReserveBC = 0;
        newFactories = 0;

        // convert captured factories, one at a time until no more BC or alien factories
        while (hasAlienFactories() && (newBC > factoryConversionCost())) {
            convertRandomAlienFactory();
            newBC -= factoryConversionCost();
        }

        // if unconverted factories remain, save off BC remainder for next turn
        if (hasAlienFactories()) {
            industryReserveBC = newBC;
            return;
        }

        //build up to max useable factories at current robot controls level
        float costPerFactory = newFactoryCost();
        float factoriesToBuild = Math.max(0, maxBuildableFactories(robotControls)-factories-newFactories);
        if (factoriesToBuild > 0) {
            costPerFactory = newFactoryCost();
            float buildCost = factoriesToBuild * costPerFactory;
            float bcSpent = Math.min(newBC, buildCost);
            newFactories += (bcSpent/costPerFactory);
            newBC -= bcSpent;
        }

        // if we can't refit, then we are done for this turn
        // send remaining BC to empire reserve
        if (robotControls >= tech().baseRobotControls()) {
            unallocatedBC = newBC;
            return;
        }

        // if we can refit, build up to max buildable factories,
        // then incrementally upgrade factories to better robot controls
        while ((newBC > 0) && (robotControls < tech().baseRobotControls())) {
            // calculate cost to refit existing factories
            float upgradeCost = 0;
            float factoriesToUpgrade = min(factories+newFactories, maxBuildableFactories(robotControls));
            if (!empire().race().ignoresFactoryRefit)
                upgradeCost = factoriesToUpgrade * tech().baseFactoryCost() / 2;
            // not enough to upgrade? save off BC for next turn and exit
            if (upgradeCost > newBC) {
                industryReserveBC = newBC;
                return;
            }
            else {
                // pay to upgrade all factories to new RC at once
                newBC -= upgradeCost;
                robotControls++;
            }
            //after refitting, build up to max useable factories at current robot controls level
            factoriesToBuild = max(0, maxBuildableFactories(robotControls)-factories-newFactories);
            if (factoriesToBuild > 0) {
                costPerFactory = newFactoryCost();
                float buildCost = factoriesToBuild * costPerFactory;
                float bcSpent = min(newBC, buildCost);
                newFactories += (bcSpent/costPerFactory);
                newBC -= bcSpent;
            }
        }
        // send remaining BC to empire reserve
        unallocatedBC = newBC;
    }
    @Override
    public void assessTurn() {
        if (isCompleted()) {
            Colony c = colony();
            float orderAmt = c.orderAmount(Colony.Orders.FACTORIES);
            if (orderAmt > 0) {
                c.removeColonyOrder(Colony.Orders.FACTORIES);
                if (!c.defense().shieldAtMaxLevel())
                    c.addColonyOrder(Colony.Orders.SHIELD, orderAmt);
                else if (!c.defense().missileBasesCompleted())
                    c.addColonyOrder(Colony.Orders.BASES, orderAmt*2/5);
            }
        }
    }
    public void commitTurn() {
        factories += newFactories;
        empire().addReserve(unallocatedBC);
        unallocatedBC = 0;
    }
    @Override
    public String upcomingResult() {
        if (colony().allocation(categoryType()) == 0)
            return text(noneText);

        float possibleNewFactories = 0;
        float prodBC = pct()* colony().totalProductionIncome() * planet().productionAdj();
        float rsvBC = pct() * colony().maxReserveIncome();
        float startBC = prodBC+rsvBC+industryReserveBC;
        float newBC = prodBC+rsvBC+industryReserveBC;
        int colonyControls = robotControls;

        // cost to convert alien factories
        float convertCost = totalAlienConversionCost();
        if (newBC <= convertCost)
            return text(convertAlienFactoriesText);

        newBC -= convertCost;

        // cost to build up to max useable factories
        float costPerFactory = newFactoryCost();
        //float maxUseable = maxUseableFactories(colonyControls);
        float maxBuildable = maxBuildableFactories(colonyControls);
        float factoriesToBuild = max(0, maxBuildable-factories-possibleNewFactories);

        if (factoriesToBuild > 0) {
            float totalBuildCost = factoriesToBuild * costPerFactory;
            float buildCost = Math.min(newBC, totalBuildCost);
            float delta = buildCost/costPerFactory;
            possibleNewFactories += delta;
            newBC -= buildCost;
        }

        while ((newBC > 0) && (colonyControls < tech().baseRobotControls())) {
            // calculate cost to refit existing factories
            float upgradeCost = 0;
            float factoriesToUpgrade = min(factories+possibleNewFactories, maxBuildableFactories(colonyControls));
            if (!empire().race().ignoresFactoryRefit)
                upgradeCost = factoriesToUpgrade * tech().baseFactoryCost() / 2;
            // not enough to upgrade? save off BC for next turn and exit
            if (upgradeCost > newBC) 
                return text(refitFactoriesText);
            else {
                // pay to upgrade all factories to new RC at once
                newBC -= upgradeCost;
                colonyControls++;
            }
            //after refitting, build up to max useable factories at current robot controls level
            factoriesToBuild = max(0, maxBuildableFactories(colonyControls)-factories-possibleNewFactories);
            if (factoriesToBuild > 0) {
                costPerFactory = tech().newFactoryCost(colonyControls);
                float buildCost = factoriesToBuild * costPerFactory;
                float bcSpent = Math.min(newBC, buildCost);
                possibleNewFactories += (bcSpent/costPerFactory);
                newBC -= bcSpent;
            }
        }
        if (newBC > 0)
            return text(reserveText);
        else
            return buildFactoriesText(possibleNewFactories, startBC);
    }
    private String buildFactoriesText(float delta, float newBC) {
        float deltaRounded = delta >= 10 ? (int) delta : round(delta,0.1f);
        if (deltaRounded == (int) deltaRounded)
            return text(perYearText, (int)deltaRounded);
        else
            return text(perYearText, fmt(deltaRounded,1));
    }
    public float maxSpendingNeeded() {
        float refitFactoriesCost = 0;
        float convertFactoriesCost = 0;
        float newFactoriesCost = 0;
        float plannedFactories = factories;
        int colonyControls = robotControls;

        // cost to upgrade existing factories
        if (hasAlienFactories()) {
            convertFactoriesCost = totalAlienConversionCost();
            convertFactoriesCost = Math.max(0, convertFactoriesCost);
        }

        newFactoriesCost += Math.max(0, maxBuildableFactories(colonyControls) - plannedFactories) * tech().newFactoryCost(colonyControls);
        plannedFactories = Math.max(maxBuildableFactories(colonyControls), plannedFactories);

        // cost to upgrade existing factories
        while (colonyControls < tech().baseRobotControls()) {
            if (!empire().race().ignoresFactoryRefit)
                refitFactoriesCost += maxBuildableFactories(colonyControls) * tech().baseFactoryCost() / 2;
            colonyControls++;
            // cost to build up for new control level
            newFactoriesCost += Math.max(0, maxBuildableFactories(colonyControls) - plannedFactories) * tech().newFactoryCost(colonyControls);
            plannedFactories = Math.max(maxBuildableFactories(colonyControls), plannedFactories);
        }

        refitFactoriesCost = Math.max(0, refitFactoriesCost);
        newFactoriesCost = Math.max(0, newFactoriesCost);
        float totalCost = Math.max(0, convertFactoriesCost + refitFactoriesCost + newFactoriesCost - industryReserveBC);

        // adjust cost for planetary production
        // assume any amount over current production comes from reserve (no adjustment)
        float totalBC = (colony().totalProductionIncome() * planet().productionAdj()) + colony().maxReserveIncome();
        if (totalCost > totalBC)
            totalCost += colony().totalProductionIncome() * (1 - planet().productionAdj());
        else
            totalCost *= colony().totalIncome() / totalBC;

        return totalCost;
    }
    public int maxAllocationNeeded() {
        float needed = maxSpendingNeeded();
        if (needed <= 0)
            return 0;
        float pctNeeded = min(1, needed / colony().totalIncome());
        int ticks = (int) Math.ceil(pctNeeded * MAX_TICKS);
        return ticks;
    }   
    //
    // PRIVATE METHODS
    //
    private float factoryConversionCost()    { return 2; }
    private boolean hasAlienFactories()       { return planet().numAlienFactories() > 0; }
    private float totalAlienConversionCost() { return planet().numAlienFactories() * factoryConversionCost(); }
    private void convertRandomAlienFactory() {
        Planet p = planet();
        if (p.numAlienFactories() == 0)
            return;

        // select random race from alienFactories and convert 1 factory
        int randomEmpId = p.randomAlienFactoryEmpire();
        p.addAlienFactories(randomEmpId, -1);
        factories++;
    }
}
