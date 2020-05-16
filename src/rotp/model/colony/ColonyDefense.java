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

public class ColonyDefense extends ColonySpendingCategory {
    private static final long serialVersionUID = 1L;
    private MissileBase missileBase;
    private float bases = 0;
    private float previousBases = 0;
    private int maxBases = 1;
    private float shield = 0;
    private float newBases = 0;
    private float newShield = 0;
    private float baseUpgradeBC = 0;
    private float unallocatedBC = 0;
    private float newBaseUpgradeCost = 0;
    private boolean shieldCompleted = false;
    private boolean missileBasesUpgraded = false;

    public MissileBase missileBase()              { return missileBase; }
    public float bases()                         { return bases; }
    public void bases(float d)                   { bases = d; }
    public float shield()                        { return shield; }
    public boolean shieldCompleted()              { return shieldCompleted && shieldAtMaxLevel(); }
    public boolean missileBasesUpgraded()         { return missileBasesUpgraded && (missileBase == tech().bestMissileBase()); }

    public void updateMissileBase()               { missileBase = colony().tech().bestMissileBase(); }
    public void destroyBases(int i)               { bases -= i; }
    @Override
    public boolean isCompleted() {
        return (missileBase == colony().tech().bestMissileBase()) &&  missileBasesCompleted() && shieldAtMaxLevel();
    }
    public boolean shieldAtMaxLevel() {
        return colony().starSystem().inNebula() || (shield >= maxShieldLevel());
    }
    public boolean missileBasesCompleted() {
        return (bases >= maxBases())
            && (missileBase == empire().tech().bestMissileBase());
    }
    public boolean missileBasesCompletedThisTurn() {
        return (deltaBases() > 0) && missileBasesCompleted();
    }
    public void init() {
        missileBase = null;
        bases = 0;
        shield = 0;
        newBases = 0;
        newShield = 0;
        baseUpgradeBC = 0;
        unallocatedBC = 0;
        newBaseUpgradeCost = 0;
    }
    @Override
    public int categoryType()              { return Colony.DEFENSE; }
    @Override
    public float totalBC()                { return super.totalBC() * planet().productionAdj(); }
    public int maxBases()                  { return maxBases; }
    public void maxBases(int i)            { maxBases = i; }
    public int deltaBases()                { return (int) bases - (int) previousBases; }
    public boolean incrementMaxBases() {
        maxBases = max(0, maxBases+1);
        return true;
    }
    public boolean decrementMaxBases() {
        int prev = maxBases;
        maxBases = max(0, maxBases-1);
        return prev != maxBases;
    }
    public String armorDesc()        { return tech().topArmorTech().shortName(); }
    public String battleSuitDesc()   { return tech().topBattleSuitTech().name(); }
    public String weaponDesc()       { return tech().topHandWeaponTech().name(); }
    public String personalShieldDesc() { return tech().topPersonalShieldTech().name(); }
    public int troops()              { return (int) Math.ceil(colony().population()); }

    private float orderedBasesValue() {
        // if no bases needed for this colony, ignore the order for minimum base spending
        return maxBases() == 0 ? 0 : colony().orderAmount(Colony.Orders.BASES);
    }
    private float orderedShieldValue() {
        return shieldAtMaxLevel() ? 0 : colony().orderAmount(Colony.Orders.SHIELD);
    }
    public float maintenanceCost() { return missileBaseMaintenanceCost(); }
    @Override
    public float orderedValue() {
        return max(super.orderedValue(),
                        orderedBasesValue(),
                        orderedShieldValue());
    }
    @Override
    public void removeSpendingOrders()   {
        colony().removeColonyOrder(Colony.Orders.BASES);
        colony().removeColonyOrder(Colony.Orders.SHIELD);
    }
    public void capturedBy(Empire newCiv) {
        bases = 0;
        maxBases = 1;
        missileBase = newCiv.tech().bestMissileBase();
    }
    @Override
    public void nextTurn(float totalProd, float totalReserve) {
        previousBases = bases;
        newBases = 0;
        newShield = 0;
        // prod gets planetary bonus, but not reserve
        float prodBC = pct()* totalProd * planet().productionAdj();
        float rsvBC = pct() * totalReserve;
        float newBC = prodBC+rsvBC;

        float baseCost = missileBase.cost(empire());
        shieldCompleted = false;

        // build shield strength (100 BC per level)
        if (!shieldAtMaxLevel()) {
            newShield = min((newBC/100), (maxShieldLevel() - shield));
            newShield = max(0, newShield);
            newBC -= (newShield * 100);
        }

        newBaseUpgradeCost = 0;
        float newBaseCost = tech().newMissileBaseCost();

        if (bases > 0 && missileBase != tech().bestMissileBase()) {
            newBaseUpgradeCost = (bases * max(0,newBaseCost-baseCost)) - baseUpgradeBC;
            newBaseUpgradeCost = min(newBC, newBaseUpgradeCost);
            newBaseUpgradeCost = max(0, newBaseUpgradeCost);
            newBC -= newBaseUpgradeCost;
        }

        if (bases < maxBases()) {
            newBases = min((newBC / baseCost), maxBases() - bases);
            newBases = max(0, newBases);
            newBC -= (newBases * baseCost);
        }
        else if (bases > maxBases()) {
            newBases = 0;
            float scrappedBases = bases - maxBases();
            bases = maxBases();
            newBC += (scrappedBases * tech().bestMissileBase().cost(empire()));
        }
        // send remaining BC to reserve
        unallocatedBC = newBC;
    }
    @Override
    public void assessTurn() {
        Colony c = colony();
        if (shieldAtMaxLevel()) {
            float orderAmt = c.orderAmount(Colony.Orders.SHIELD);
            if (orderAmt > 0) {
                c.removeColonyOrder(Colony.Orders.SHIELD);
                c.addColonyOrder(Colony.Orders.BASES, orderAmt*2/5);
            }
        }
        if (missileBasesCompleted()) 
            c.removeColonyOrder(Colony.Orders.BASES);
    }
    public void commitTurn() {
        // upgrade shield
        shield += newShield;
        shieldCompleted = (newShield > 0) && shieldAtMaxLevel();

        // upgrade existing missile bases
        baseUpgradeBC += newBaseUpgradeCost;

        missileBasesUpgraded = false;
        if (baseUpgradeBC >= missileUpgradeCost()) {
            missileBasesUpgraded = baseUpgradeBC > 0;
            baseUpgradeBC = 0;
            missileBase = tech().bestMissileBase();
        }

        // add new missile bases to limit
        if (newBases > 0) {
            bases += newBases;
            baseUpgradeBC = 0;
        }

        // remainder goes into reserve
        empire().addReserve(unallocatedBC);
        unallocatedBC = 0;
    }
    public float maxShieldLevel()      { return colony().starSystem().inNebula() ? 0 : tech().maxPlanetaryShieldLevel(); }
    public float missileBaseMaintenanceCost() { return ((int) bases * missileBase.cost(empire()) * .02f); }
    private float missileUpgradeCost()  { return bases * (tech().newMissileBaseCost() - missileBase.cost(empire())); }
    public boolean isArmed()             { return missileBases() >= 1; }
    public int shieldLevel()             { return (int) (shield / 5) * 5; }
    public int missileBases()            { return (int) bases; }
    public int defenders()               { return (int) colony().population(); }
    @Override
    public boolean canLowerMaintenance() { return bases > 0; }
    @Override
    public void lowerMaintenance()       { bases = Math.max(0, bases-1); }
    public float firepower(float shield) {
        return missileBases() * missileBase.firepower(shield);
    }
    public int missileShieldLevel() {
        return empire() == null ? 0 : shieldLevel() + (int) tech().maxDeflectorShieldLevel();
    }
    @Override
    public String upcomingResult() {
        if (colony().allocation(categoryType()) == 0)
            return text(noneText);

        float maxBases = maxBases();
        float prodBC = pct()* colony().totalProductionIncome() * planet().productionAdj();
        float rsvBC = pct() * colony().maxReserveIncome();
        float newBC = prodBC+rsvBC;
        float shieldCost = 0;

        if (!shieldAtMaxLevel())
            shieldCost = (maxShieldLevel() - shield) * 100;

        if (newBC < shieldCost)
            return text(shieldText);

        newBC -= shieldCost;

        float upgradeCost = 0;
        float baseCost = missileBase.cost(empire());
        float newBaseCost = tech().bestMissileBase().cost(empire());

        if (missileBase != tech().bestMissileBase()) {
            newBC += baseUpgradeBC;
            upgradeCost = bases * Math.max(0,newBaseCost-baseCost);
            if (newBC < upgradeCost)
                return text(upgradeBasesText);
        }

        newBC -= upgradeCost;

        float maxCost = (maxBases - bases) * newBaseCost;
        float newBases = bases + (newBC/newBaseCost);
        int delta = (int) newBases - (int) bases;

        if (newBC <= maxCost) {
            if (delta < 1) {
                int turns = (int) Math.ceil( (1- (bases - (int)bases))*newBaseCost/newBC);
                if (turns > 99)
                    return text(yearsLongText, turns);
                else
                    return text(yearsText, turns);
            }
            else if (delta == 1)
                return text(yearText, 1);
            else
                return text(perYearText, delta);
        }
        return text(reserveText);
    }
    public float maxSpendingNeeded() {
        float buildShieldCost = (maxShieldLevel() - shield) * 100;
        buildShieldCost = Math.max(0, buildShieldCost);
        float upgradeMissileBasesCost = missileUpgradeCost() - baseUpgradeBC;
        upgradeMissileBasesCost = Math.max(0, upgradeMissileBasesCost);
        float newMissileBasesCost =  (maxBases() - bases) * tech().newMissileBaseCost();
        newMissileBasesCost = Math.max(0, newMissileBasesCost);
        float totalCost = buildShieldCost + upgradeMissileBasesCost + newMissileBasesCost;

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
}
