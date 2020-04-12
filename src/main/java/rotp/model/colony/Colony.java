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

import java.io.Serializable;
import java.util.*;

import static rotp.model.colony.ColonySpendingCategory.MAX_TICKS;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.events.SystemCapturedEvent;
import rotp.model.events.SystemDestroyedEvent;
import rotp.model.events.SystemRandomEvent;
import rotp.model.galaxy.IMappedObject;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.Transport;
import rotp.model.incidents.ColonyCapturedIncident;
import rotp.model.incidents.ColonyInvadedIncident;
import rotp.model.planet.Planet;
import rotp.model.ships.Design;
import rotp.model.ships.ShipDesign;
import rotp.model.tech.Tech;
import rotp.model.tech.TechTree;
import rotp.ui.RotPUI;
import rotp.ui.notifications.GNNNotification;
import rotp.ui.notifications.InvadersKilledAlert;
import rotp.ui.notifications.TransportsKilledAlert;
import rotp.util.Base;

public final class Colony implements Base, IMappedObject, Serializable {
    private static final long serialVersionUID = 1L;
    private static final int[] validationSeq = { 3, 2, 0, 1, 4, 5 };
    private static final int[] spendingSeq = { 4, 0, 1, 2, 3, 5 };
    private static final int[] cleanupSeq =  { 2, 4, 1, 0, 3, 5 };
    private static final String[] categoryNames = { "MAIN_COLONY_SHIP", "MAIN_COLONY_DEFENSE", "MAIN_COLONY_INDUSTRY",
                    "MAIN_COLONY_ECOLOGY", "MAIN_COLONY_TECHNOLOGY" };

    public static String categoryName(int i) {
        return categoryNames[i];
    }

    // if these values are changed, the spendingSeq array needs to be changed
    private static final int NUM_CATS = 5;
    public static final int SHIP = 0;
    public static final int DEFENSE = 1;
    public static final int INDUSTRY = 2;
    public static final int ECOLOGY = 3;
    public static final int RESEARCH = 4;

    private static final float TECH_PLUNDER_PCT = 0.02f;
    private static final int MAX_TECHS_CAPTURED = 6;
    private static final int TARGETED_DAMAGE_FOR_POPLOSS = 400;
    private static final int TARGETED_DAMAGE_FOR_FACTLOSS = 100;
    private static final int UNTARGETED_DAMAGE_FOR_POPLOSS = 200;
    private static final int UNTARGETED_DAMAGE_FOR_FACTLOSS = 50;

    public enum Orders {
        NONE(""), SHIELD("TECH_ALLOCATE_SHIELD"), BASES("TECH_ALLOCATE_MISSILE_BASES"), FACTORIES(
                        "TECH_ALLOCATE_FACTORIES"), SOIL("TECH_ALLOCATE_ENRICH_SOIL"), ATMOSPHERE(
                        "TECH_ALLOCATE_ATMOSPHERE"), TERRAFORM("TECH_ALLOCATE_TERRAFORM");
        private final String label;
        Orders(String s) { label = s; }
        @Override
        public String toString() { return label; }
    }

    private Empire empire;
    private final Planet planet;
    private Transport transport;

    private float population = 0;
    private float previousPopulation = 0;
    private int rebels = 0;
    private float captives = 0;
    private float reserveIncomeBC = 0;
    private boolean rebellion = false;
    private boolean quarantined = false;
    private int fortressNum = 0;
    private final int[] allocation = new int[NUM_CATS];
    private final boolean[] locked = new boolean[NUM_CATS];
    private final EnumMap<Colony.Orders, Float> orders = new EnumMap<>(Orders.class);
    public ColonySpendingCategory[] spending = new ColonySpendingCategory[] { new ColonyShipyard(), new ColonyDefense(),
            new ColonyIndustry(), new ColonyEcology(), new ColonyResearch() };

    private boolean underSiege = false;
    private transient boolean hasNewOrders = false;

    public boolean underSiege()                { return underSiege; }
    public float reserveIncome()              { return reserveIncomeBC; }
    public void clearReserveIncome()           { reserveIncomeBC = 0; }
    public void adjustReserveIncome(float bc) { reserveIncomeBC += bc; }
    public boolean quarantined()               { return quarantined; }
    public void becomeQuarantined()            { quarantined = true; }
    public void clearQuarantine()              { quarantined = false; }
    public int fortressNum()                   { return fortressNum; }
    public int allocation(int i)               { return allocation[i]; }
    public void allocation(int i, int val)     { allocation[i] = val; }
    public void addAllocation(int i, int val)  { allocation[i]+= val; }
    public void setAllocation(int i, int val) {
        // attempt to set allocation for category[i] to val
        // do not add more than allocationRemaining()
        // do not reduce current allocation
        int addMax = allocationRemaining();
        int addAmt = max(0, val-allocation(i));
        addAllocation(i,min(addMax, addAmt));
    }
    public boolean locked(int i)               { return locked[i]; }
    public void locked(int i, boolean b)       { locked[i] = b; }
    public void toggleLock(int i)              { locked[i] = !locked[i]; }
    public boolean hasNewOrders()              { return hasNewOrders; }
    public void hasNewOrders(boolean b)        { hasNewOrders = b; }
    public float pct(int i)                    { return (float)allocation[i]/MAX_TICKS; }
    public void pct(int i, float d)            { allocation[i]=(int)Math.ceil(d*MAX_TICKS); }
    public void addPct(int i, float d)         { pct(i, pct(i)+d); }

    public boolean warning(int i)              { return category(i).warning(); }
    public boolean canAdjust(int i) {
        if (inRebellion() || locked(i))
            return false;
        else
            return true;
    }
    public void clearAllRebellion() {
        rebels = 0;
        rebellion = false;
    }
    public float currentProductionCapacity() {
        // returns a pct (0 to 1) representing the colony's current
        // production vs its maximum possible formula
        float factories = industry().factories();
        float maxFactories = industry().maxFactories();
        float pop = population();
        float maxPop = planet().maxSize();
        
        float workerProd = empire.workerProductivity();
        float maxProd = maxFactories + (maxPop * workerProd);
        float currProd = factories + (pop*workerProd);
        return currProd/maxProd;
    }
    public boolean creatingWaste() {
        int needed = ecology().cleanupAllocationNeeded();
        int curr = ecology().allocation();
        return curr < needed;
    }
    @Override
    public String toString()                   { return "Colony: " + name();  }

    public Colony(Empire c, Planet p) {
        empire = c;
        planet = p;
        init();
    }
    private void init() {
        buildFortress();
        clearTransport();

        for (int i = 0; i < spending.length; i++)
            spending[i].init(this);

        setPopulation(2);
        shipyard().goToNextDesign();
        defense().updateMissileBase();
    }
    public boolean isBuildingShip() { return shipyard().design() instanceof ShipDesign; }
    private void buildFortress()    { fortressNum = empire.race().randomFortress(); }
    public boolean isAutopilot()    { return empire.isAIControlled(); }
    // MappedObject overrides
    @Override
    public float x()               { return starSystem().x(); }
    @Override
    public float y()               { return starSystem().y(); }
    public StarSystem starSystem()  { return planet().starSystem(); }
    public Planet planet()          { return planet; }
    public Empire empire()          { return empire; }
    public TechTree tech()          { return empire.tech(); }
    public String name()            { return starSystem().name(); }

    public ColonySpendingCategory category(int i) { return spending[i]; }
    public ColonyShipyard shipyard() { return (ColonyShipyard) spending[SHIP]; }
    public ColonyDefense defense()   { return (ColonyDefense) spending[DEFENSE]; }
    public ColonyIndustry industry() { return (ColonyIndustry) spending[INDUSTRY]; }
    public ColonyEcology ecology()   { return (ColonyEcology) spending[ECOLOGY]; }
    public ColonyResearch research() { return (ColonyResearch) spending[RESEARCH]; }

    public boolean hasStargate()         { return shipyard().hasStargate(); }
    public boolean hasStargate(Empire e) { return (empire == e) && hasStargate(); }

    public int totalAmountAllocated() {
        int amt = 0;
        for (ColonySpendingCategory cat : spending)
            amt += cat.allocation();

        return amt;
    }
    public int allocationRemaining()  { return MAX_TICKS - totalAmountAllocated(); }

    public String printString() {
        return empire.sv.name(starSystem().id) + "-- pop:"
                + (float) Math.round(population() * 100) / 100 + " reb:"
                + (float) Math.round(rebels * 100) / 100 + " fac:"
                + (float) Math.round(industry().factories() * 100) / 100 + " was:"
                + (float) Math.round(ecology().waste() * 100) / 100 + " bas:"
                + (float) Math.round(defense().bases() * 100) / 100 + " shd:"
                + (float) Math.round(defense().shield() * 100) / 100;
    }

    public int displayPopulation()          { return population < 1 ? (int) Math.ceil(population) : (int) population; }
    public float population()               { return population; }
    public void setPopulation(float pop)    { population = pop; }
    public void adjustPopulation(float pop) { population += pop; }
    public int rebels()                      { return rebels; }
    public void rebels(int i)                { rebels = i; }
    public int deltaPopulation()             { return (int) population - (int) previousPopulation; }
    public boolean destroyed()               { return population <= 0; }
    public boolean inRebellion()             { return rebellion && (rebels > 0); }
    public float rebellionPct()             { return rebels / population(); }
    public boolean hasOrders()               { return !orders.isEmpty(); }

    public float orderAmount(Colony.Orders order) {
        Colony.Orders priorityOrder = empire.priorityOrders();
        // amount for this order
        float amt = orders.containsKey(order) ? orders.get(order) : 0;
        // if empire has a priority and this is not it, return 0
        if (orders.containsKey(priorityOrder))
            return order == priorityOrder ? amt : 0;
        else
            return amt;
    }
    public void forcePct(int catNum, float d) {
        // occurs when new spending orders are applied
        allocation(catNum, (int) Math.ceil(d*MAX_TICKS));
        realignSpending(spending[catNum]);
        spending[catNum].removeSpendingOrders();
    }
    public void setCleanupPct(float d) {
        // occurs when ai is reseting eco for minimum cleanup
        allocation(ECOLOGY, (int) Math.ceil(d*MAX_TICKS));
        cleanupSpending(ecology());
    }
    public boolean increment(int catNum, int amt) {
        if (!canAdjust(catNum))
            return false;

        int newValue = allocation(catNum)+amt;
        if ((newValue < 0)
        || (newValue > MAX_TICKS))
            return false;

        allocation(catNum, newValue);
        realignSpending(spending[catNum]);
        spending[catNum].removeSpendingOrders();
        return true;
    }
    public String shipyardProject() {
        if (shipyard().allocation() > 0)
            return shipyard().design().name();
        return "";
    }
    public void clearSpending() {
        allocation(SHIP, 0);
        allocation(DEFENSE, 0);
        allocation(INDUSTRY, 0);
        allocation(ECOLOGY, 0);
        allocation(RESEARCH, 0);
    }
    public void clearUnlockedSpending() {
        if (!locked(SHIP))
            allocation(SHIP, 0);
        if (!locked(DEFENSE))
            allocation(DEFENSE, 0);
        if (!locked(INDUSTRY))
            allocation(INDUSTRY, 0);
        if (!locked(ECOLOGY))
            allocation(ECOLOGY, 0);
        if (!locked(RESEARCH))
            allocation(RESEARCH, 0);
    }
    public void addColonyOrder(Colony.Orders order, float amt) {
        if (amt == 0)
            return;
        if (planet().isEnvironmentHostile() && order == Orders.SOIL)
            return;
        if (!planet().isEnvironmentHostile() && order == Orders.ATMOSPHERE)
            return;
        if (starSystem().inNebula() && order == Orders.SHIELD)
            return;
        
        float existingAmt = orders.containsKey(order) ? orders.get(order) : 0;

        if (amt <= existingAmt)
            return;

        hasNewOrders = true;
        orders.put(order, amt);
        empire().governorAI().setColonyAllocations(this);
        validate();
    }

    public void removeColonyOrder(Colony.Orders order) {
        if (orders.containsKey(order)) {
            orders.remove(order);
            empire().governorAI().setColonyAllocations(this);
            validate();
        }
    }

    public void setHomeworldValues() {
        setPopulation(50);
        previousPopulation = population();
        industry().factories(30);
        industry().previousFactories(30);

        Empire emp = empire();
        galaxy().ships.buildShips(emp.id, starSystem().id, empire().shipLab().scoutDesign().id(), 2);
        galaxy().ships.buildShips(emp.id, starSystem().id, empire().shipLab().colonyDesign().id(), 1);
    }

    public void spreadRebellion() {
        inciteRebels(0.50f, "GNN_PLAYER_REBELLION_SPREAD");
    }
    public int inciteRebels(float pct, String messageKey) {
        // always guaranteed to incite at least 1 pop
        int newRebels = max(1, (int) Math.ceil(pct * population()));
        rebels = (int) min(population(), rebels + newRebels);

        if (rebels >= population() / 2)
            rebel(messageKey);
        return newRebels;
    }
    public void rebel(String messageKey) {
        if (inRebellion())
            return;
        StarSystem sys = starSystem();
        rebellion = true;
        sys.addEvent(new SystemRandomEvent("SYSEVENT_REBELLION"));
        empire.setRecalcDistances();
        if (empire.inRevolt()) {
            empire.overthrowLeader();
            return;
        }
        Empire pl = player();
        String message = null;
        if (empire == pl) {
            message = text(messageKey, sys.name(), empire.name(), rebels);
            galaxy().giveAdvice("MAIN_ADVISOR_REBELLION", sys.name());
        }
        else if (empire.hasContact(pl) && pl.sv.isScouted(sys.id))
            message = text("GNN_ALIEN_REBELLION", pl.sv.name(sys.id), empire.name(), rebels);
        if (message != null)
            GNNNotification.notifyRebellion(message);
    }

    public float orderAdjustment() {
        // if orders for different spending categories exceed 100%
        // we need a modifier to adjust them back down so they don't
        float totalOrders = 0;
        for (int i = 0; i < NUM_CATS; i++) {
            ColonySpendingCategory cat = category(i);
            totalOrders += cat.orderedValue();
        }
        return totalOrders <= 100 ? 1.0f : 100.0f / totalOrders;
    }
    public void validate() {
        int maxTicks = ColonySpendingCategory.MAX_TICKS;

        // bounds all allocations from 0 to max_ticks (in case negative values in save)
        // do them in spending sequence order to ensure ECO spending doesn't get shorted
        for (int i=0;i<allocation.length;i++) {
            int index = validationSeq[i];
            allocation[index] = min(maxTicks, max(0,allocation[index]));
            maxTicks -= allocation[index];
        }
    }
    public void validateOnLoad() {
        if (population < 0)
            previousPopulation = population = 1;
        if (planet.waste() > planet.maxWaste()) {
            planet.resetWaste();
            planet.addWaste(planet.maxWaste());
        }
    }

    public void nextTurn() {
        log("Colony: ", empire.sv.name(starSystem().id),  ": NextTurn [" , shipyard().design().name() , "|" ,str(shipyard().allocation()) , "-"
                    , str(defense().allocation()) , "-" , str(industry().allocation()) , "-" , str(ecology().allocation()) , "-"
                    , str(research().allocation()) , "]");
        previousPopulation = population;

        // if rebelling, nothing happens (only enough prod assumed to clean new
        // waste and maintain existing structures)
        if (inRebellion())
            return;

        // make sure that the colony's expenses aren't too high
        empire().governorAI().lowerExpenses(this);

        // if colony is still in the hole, give it up
        float totalProd = totalProductionIncome();
        float reserveIncome = maxReserveIncome();

        if ((totalProd <= 0) && (reserveIncome <= 0))
            return;

        float usedReserve = maxReserveIncome();

        shipyard().nextTurn(totalProd, reserveIncome);
        defense().nextTurn(totalProd, reserveIncome);
        industry().nextTurn(totalProd, reserveIncome);
        ecology().nextTurn(totalProd, reserveIncome);
        research().nextTurn(totalProd, reserveIncome);

        // COMMIT CONTRIBUTIONS
        defense().commitTurn();
        industry().commitTurn();
        ecology().commitTurn();
        research().commitTurn();

        adjustReserveIncome(-usedReserve);

        if ((shipyard().allocation() < 0) || (defense().allocation() < 0) || (industry().allocation() < 0)
            || (ecology().allocation() < 0) || (research().allocation() < 0)) {
            err("ERROR: bad allocation for " + starSystem().name() + " ship:" + shipyard().allocation() + " def:"
                    + defense().allocation() + " ind:" + industry().allocation() + " eco:" + ecology().allocation()
                    + " res:" + research().allocation());
            throw new RuntimeException("ERROR: bad allocation for " + starSystem().name() + " ship:"
                    + shipyard().allocation() + " def:" + defense().allocation() + " ind:" + industry().allocation()
                    + " eco:" + ecology().allocation() + " res:" + research().allocation());
        }
    }
    public void assessTurn() {
        underSiege = orbitingEnemyShips();
        shipyard().assessTurn();
        defense().assessTurn();
        industry().assessTurn();
        ecology().assessTurn();
        research().assessTurn();
    }
    public boolean canLowerMaintenance() { return transporting(); }

    public void lowerMaintenance() {
        if (transporting()) {
            int maxSize = maxTransportsAllowed();
            if (inTransport() > maxSize) {
                // log("reducing transport levels from ", planet.name(), " to ",
                // str(transport().originalSize), " to avoid income loss");
                transport().size(maxSize);
                transport().originalSize(maxSize);
            } else {
                // log("cancelling transports from ", planet.name(), " to
                // avoid income loss");
                clearTransport();
            }
        }
    }
    public void reallocateSpending(int cat, int ticks) {
        if (allocation(cat) >= ticks)
            return;

        int tmp = allocation(cat);
        int delta = ticks - tmp;

        allocation(cat, ticks);

        // deduct from research 1st
        if (cat != RESEARCH) {
            tmp = allocation(RESEARCH);
            if (tmp >= delta) {
                allocation(RESEARCH, allocation(RESEARCH) - delta);
                return;
            } else {
                allocation(RESEARCH, 0);
                delta -= tmp;
            }
        }

        // deduct from ships 2nd
        if (cat != SHIP) {
            tmp = allocation(SHIP);
            if (tmp >= delta) {
                allocation(SHIP, allocation(SHIP) - delta);
                return;
            } else {
                allocation(SHIP, 0);
                delta -= tmp;
            }
        }

        // deduct from defense 3rd
        if (cat != DEFENSE) {
            tmp = allocation(DEFENSE);
            if (tmp >= delta) {
                allocation(DEFENSE, allocation(DEFENSE) - delta);
                return;
            } else {
                allocation(DEFENSE, 0);
                delta -= tmp;
            }
        }

        // deduct from industry 4th
        if (cat != INDUSTRY) {
            tmp = allocation(INDUSTRY);
            if (tmp >= delta) {
                allocation(INDUSTRY, allocation(INDUSTRY) - delta);
                return;
            } else {
                allocation(INDUSTRY, 0);
                delta -= tmp;
            }
        }

        // deduct from ecology last
        if (cat != ECOLOGY) {
            tmp = allocation(ECOLOGY);
            if (tmp >= delta)
                allocation(ECOLOGY, allocation(ECOLOGY) - delta);
            else {
                allocation(ECOLOGY, 0);
                delta -= tmp;
            }
        }
    }
    public void cleanupSpending(ColonySpendingCategory cat) {
        int maxAllocation = ColonySpendingCategory.MAX_TICKS;
        // determine how much categories are over/under spent
        int spendingTotal = 0;
        for (int i = 0; i < NUM_CATS; i++)
            spendingTotal += spending[i].allocation();

        int adj = maxAllocation - spendingTotal;

        for (int i = 0; i < NUM_CATS; i++) {
            ColonySpendingCategory currCat = spending[cleanupSeq[i]];
            if ((currCat != cat) && !locked(cleanupSeq[i]))
                adj -= currCat.adjustValue(adj);
        }

        // if any adj remaining, send back to original cat
        cat.adjustValue(adj);
    }
    public void realignSpending(ColonySpendingCategory cat) {
        int maxAllocation = ColonySpendingCategory.MAX_TICKS;
        // determine how much categories are over/under spent
        int spendingTotal = 0;
        for (int i = 0; i < NUM_CATS; i++)
            spendingTotal += spending[i].allocation();

        int adj = maxAllocation - spendingTotal;

        for (int i = 0; i < NUM_CATS; i++) {
            ColonySpendingCategory currCat = spending[spendingSeq[i]];
            if ((currCat != cat) && !locked(spendingSeq[i]))
                adj -= currCat.adjustValue(adj);
        }

        // if any adj remaining, send back to original cat
        // this is always player-driver, so remove spending orders
        if (adj != 0) {
            cat.adjustValue(adj);
            cat.removeSpendingOrders();
        }
    }
    public float transportPriority() {
        float pr;
        if (inRebellion())
            pr = planet.currentSize() + rebels;
        else
            pr = (planet.currentSize() - expectedPopulation()) / expectedPopPct();

        pr *= planet.productionAdj();
        if (planet.isOrionArtifact())
            pr *= 3;
        else if (planet.isArtifact())
            pr *= 2; // float for artifacts, triple for super-artifacts

        return pr;
    }
    private boolean orbitingEnemyShips() {
        List<ShipFleet> fleets = starSystem().orbitingFleets();
        for (ShipFleet fleet : fleets) {
            if (fleet.isArmed() && empire.atWarWith(fleet.empId()))
                return true;
        }
        return false;
    }
    public boolean hasInterdiction()  { return tech().subspaceInterdiction();  }
    public boolean isHomeworld()      { return ((empire != null) && (empire.homeSysId() == starSystem().id)); }
    public boolean isCapital()        { return ((empire != null) && (empire.capitalSysId() == starSystem().id)); }
    public float workingPopulation() { return population() - inTransport(); }
    public float usedFactories()     {
        return (int) min(industry().factories(), workingPopulation() * industry().effectiveRobotControls());
    }
    public float production() {
        if (inRebellion())
            return 0.0f;
        float mod = empire().isPlayer() ? 1.0f : options().aiProductionModifier();
        float workerProd = workingPopulation() * empire.workerProductivity();
        return mod*(workerProd + usedFactories());
    }
    public float maxProduction() {
        float workerProd = planet().maxSize() * empire.workerProductivity();
        return workerProd + industry().maxFactories();
    }
    public float maxReserveUseable() {
        return production();
    }
    public float maxReserveIncome() {
        return min(reserveIncome(), maxReserveUseable());
    }
    public float maxReserveNeeded() {
        return max(0, maxReserveUseable() - reserveIncome());
    }
    public boolean embargoed() {
        return underSiege || starSystem().piracy() || quarantined();
    }
    public float actualTradeIncome() {
        if (embargoed())
            return 0;
        else
            return production() * empire.tradeIncomePerBC();
    }
    public float totalIncome() {
        return totalProductionIncome() + maxReserveIncome();
    }
    public float totalProductionIncome() {
        if (inRebellion())
            return 0.1f;

        float prod = production();
        float reserveCost = prod * empire.empireTaxPct();
        float securityCost = prod * empire.totalSecurityCostPct();
        float shipCost = prod * empire.shipMaintCostPerBC();
        float tradeIncome = actualTradeIncome();
        float defenseCost = prod * empire.totalMissileBaseCostPct();
        float shipyardCost = shipyard().maintenanceCost();
        float transportCost = transportCost();

        return prod - reserveCost - securityCost - defenseCost - shipyardCost - transportCost + tradeIncome - shipCost;
    }
    public float expectedPopulation() {
        return workingPopulation() + normalPopGrowth() + incomingTransports();
    }
    public int incomingTransports() {
        return galaxy().friendlyPopApproachingSystem(starSystem());
    }
    public float populationPct() {
        return (population() / planet.currentSize());
    }
    public float expectedPopPct() {
        return (expectedPopulation() / planet.currentSize());
    }
    public int calcPopNeeded(float pct) {
        return (int) ((planet.currentSize() * pct) - expectedPopulation());
    }
    public int calcPopToGive(float pct) {
        if (!canTransport())
            return 0;
        int p1 = maxTransportsAllowed();
        int p2 = (int) (population() - (empire.ai().targetPopPct(starSystem()) * planet().currentSize()));
        return min(p1,p2);
    }
    public float newWaste() {
        return max(0, usedFactories() * tech().factoryWasteMod());
    }
    public float wasteCleanupCost() {
        if (empire.race().ignoresPlanetEnvironment())
            return 0;
        
        float mod = empire().isPlayer() ? 1.0f : options().aiWasteModifier();
        return mod*(min(planet.maxWaste(), planet.waste() + newWaste())) / tech().wasteElimination();
    }
    public float minimumCleanupCost() {
        return min(wasteCleanupCost(), totalIncome());
    }
    public float maxSize() {
        return max(planet.currentSize(), planet.baseSize()+tech().terraformAdj());
    }
    public float maxUseableFactories() {
        return workingPopulation() * empire().maxRobotControls();
    }
    public float normalPopGrowth() {
        // calculate growth rate based on current pop, environment & race
        float maxNewPopulation = planet.currentSize() - workingPopulation();
        float baseGrowthRate = max(0, (1 - (workingPopulation() / planet.currentSize())) / 10);
        baseGrowthRate *= empire.race().growthRateMod();
        if (!empire.race().ignoresPlanetEnvironment())
            baseGrowthRate *= planet.growthAdj();

        // always at least .1 base growth in pop
        float newGrownPopulation = max(.1f, workingPopulation() * baseGrowthRate);
        newGrownPopulation = min(newGrownPopulation, maxNewPopulation);

        return newGrownPopulation;
    }
    public ShipFleet homeFleet() {
        return starSystem().orbitingFleetForEmpire(empire());
    }
    public float defenderCombatAdj() {
        return tech().troopCombatAdj(true);
    }
    public Transport transport() {
        if (transport == null)
            transport = new Transport(starSystem());
        return transport;
    }
    public StarSystem transportDestination() {
        if ((transport == null) || !transport.isActive())
            return null;
        return transport.destination();
    }
    public boolean transporting() {
        return (transport().isActive());
    }
    public boolean canTransport() {
        return (!inRebellion() && !transporting());
    }
    public float inTransport() {
        return transporting() ? transport().size() : 0;
    }
    public float transportCost() {
        return inTransport();
    }
    public void clearTransport() {
        transport().reset(empire);
    }
    public int maxTransportsAllowed() {
        if (quarantined())
            return 0;
        else
            return (int) (population() / 2);
    }
    public void launchTransports() {
        if (transport().isActive()) {
            transport().launch();
            setPopulation(population() - transport().size());
            transport = new Transport(starSystem());
            if (empire.isPlayer())
                starSystem().transportSprite().launch();
        }
    }
    public void scheduleTransportsToSystem(StarSystem dest, int pop, float travelTime) {
        scheduleTransportsToSystem(dest, pop);
        if (dest != starSystem()) {
            float dist = starSystem().distanceTo(dest);
            transport().travelSpeed(dist/travelTime);
        }
    }
    public void scheduleTransportsToSystem(StarSystem dest, int pop) {
        // adjust pop to max allowed

        int xPop = min(pop, maxTransportsAllowed());
        log("Scheduling " + xPop + " transports from: " + starSystem().name() + "  to: " + dest.name());

        // if zero or to this system, then clear
        if ((dest == starSystem()) || (xPop == 0))
            clearTransport();
        else {
            transport().size(pop);
            transport().setDest(dest);
            transport().setDefaultTravelSpeed();
        }

        // reset ship views
        if (empire.isPlayer())
            empire.setVisibleShips();

        // recalculate governor if transports are sent
        if (!this.isAutopilot() && this.isGovernor()) {
            govern();
        }
    }
    public void acceptTransport(Transport t) {
        setPopulation(min(planet.currentSize(), (population() + t.size())));
        log("Accepting ", str(t.size()), " transports at: ", starSystem().name(), ". New pop:", fmt(population(), 2));
        t.size(0);
    }
    public float maxTransportsToReceive() {
        return planet.currentSize() - workingPopulation();
    }
    public void resistTransportWithRebels(Transport tr) {
        log(str(rebels), " ", empire().raceName(), " rebels at ", starSystem().name(), " resisting ",
                    str(tr.size()), " ", tr.empire().raceName(), " transports");
        captives = population() - rebels;
        setPopulation(rebels);

        if (population() > 0) {
            if (empire.isPlayer() || tr.empire().isPlayer())
                RotPUI.instance().selectGroundBattlePanel(this, tr);
            else
                completeDefenseAgainstTransports(tr);
        }

        rebels = (int) population();
        setPopulation(rebels + captives);
        captives = 0;

        // are there rebels left?
        if (rebels > 0)
            return;

        empire.setRecalcDistances();
        rebellion = false;
        setPopulation(max(1, tr.size() + population));
        tr.size(0);
    }
    public void resistTransport(Transport tr) {
        log(empire().raceName() + " colony at " + starSystem().name() + " resisting " + tr.size() + " "
                        + tr.empire().raceName() + " transports");
        int passed = 0;
        int num = tr.size();
        float pct = tr.combatTransportPct();

        EmpireView ev = tr.empire().viewForEmpire(empire);

        if (ev != null) {
            if (ev.embassy().unity())
                return;
            // don't cause war if treaty signed since launch
            if (!ev.embassy().war() && (ev.embassy().treatyDate() >= tr.launchTime()))
                return;
            // don't cause war if planet now occupied by another race
            if (!ev.embassy().war() && (empire != tr.targetCiv()))
                return;
            if (!ev.embassy().war())
                ev.embassy().declareWar();
        }

        if (tech().subspaceInterdiction())
            pct /= 2;

        // check for automatic passing if combat transporters
        if (pct > 0) {
            for (int i = 0; i < num; i++) {
                if (random() <= pct)
                    passed++;
            }
        }

        num -= passed;
        int missileDmg = tech().topBaseMissileTech().damage() * 3;
        int lost = 0;

        // start with base missile damage
        float defenderDmg = defense().missileBases() * missileDmg;

        // add firepower for each allied ship in orbit
        List<ShipFleet> fleets = starSystem().orbitingFleets();
        for (ShipFleet fl : fleets) {
            if (empire.alliedWith(fl.empId()))
                defenderDmg += fl.firepower(0);
        }

        // run the gauntlet
        for (int j = 0; j < tr.gauntletRounds(); j++)
            lost += (int) (defenderDmg / tr.hitPoints());

        passed += Math.max(0, (num - lost));

        tr.size(passed);

        // if gauntlet not passed, stop and inform player (if player)
        // neither of these incidents are added to the embassies. They are for
        // player notification only.
        if (tr.size() == 0) {
            log(concat(str(tr.originalSize()), " ", tr.empire().raceName(), " transports perished at ", name()));
            if (tr.empire().isPlayer()) 
                TransportsKilledAlert.create(empire(), starSystem(), tr.originalSize());
            else if (empire().isPlayer()) 
                InvadersKilledAlert.create(tr.empire(), starSystem(), tr.originalSize());
            return;
        }

        float startingPop = population();
        if (population() > 0) {
            if (empire.isPlayer() || tr.empire().isPlayer())
                RotPUI.instance().selectGroundBattlePanel(this, tr);
            else
                completeDefenseAgainstTransports(tr);
        }

        if (empire == galaxy().orionEmpire()) {
            capturedOrion(tr);
            return;
        }

        float pctLost = min(1, ((startingPop - population()) / startingPop));
        int popLost = (int) startingPop -  (int) population();
        int rebelsLost = (int) Math.ceil(pctLost*rebels);
        rebels = rebels - rebelsLost;

        // did planet ownership change?
        if (tr.size() > 0) {
            ColonyCapturedIncident.create(tr.empire(), empire(), starSystem(), popLost);
            capturedByTransport(tr);
        } else
            ColonyInvadedIncident.create(tr.empire(), empire(), starSystem(), popLost);
    }
    public void completeDefenseAgainstTransports(Transport tr) {
        while ((tr.size() > 0) && (defense().troops() > 0))
            singleCombatAgainstTransports(tr);
    }
    public boolean singleCombatAgainstTransports(Transport tr) {
        float attRoll = random(100) + tr.combatAdj();
        float defRoll = random(100) + defenderCombatAdj();

        if (attRoll < defRoll)
            tr.size(tr.size() - 1);
        else
            setPopulation(population() - 1);

        if (population() <= 0)
            setPopulation(0);

        // true: attacker defeated
        // false: defender defeated
        return attRoll < defRoll;
    }
    private void capturedByTransport(Transport tr) {
        Empire loser = empire();
        if (isCapital())
            loser.chooseNewCapital();
        
        loser.lastAttacker(tr.empire());
        starSystem().addEvent(new SystemCapturedEvent(tr.empId()));
        tr.empire().lastAttacker(loser);
        if (loser == galaxy().orionEmpire()) {
            capturedOrion(tr);
            return;
        }
        Empire pl = player();
        if (tr.empire() == pl)
            session().addSystemToAllocate(starSystem(), text("MAIN_ALLOCATE_COLONY_CAPTURED", pl.sv.name(starSystem().id), pl.raceName()));

        // list of possible techs that could be recovered from factories
        List<Tech> possibleTechs = empire().tech().techsUnknownTo(tr.empire());
        int techsCaptured = 0;
        // each factory is 2% chance to plunder an unknown tech
        for (int i = 0; i < (int) industry().factories(); i++) {
            if (techsCaptured >= MAX_TECHS_CAPTURED)
                break;
            if (!possibleTechs.isEmpty() && (random() <= TECH_PLUNDER_PCT)) {
                Tech t = random(possibleTechs);
                possibleTechs.remove(t);
                tr.empire().plunderTech(t, starSystem(), empire());
                techsCaptured++;
            }
        }

        setPopulation(tr.size());
        tr.size(0);
        industry().capturedBy(tr.empire());
        defense().capturedBy(tr.empire());
        ecology().capturedBy(tr.empire());

        empire.removeColonizedSystem(starSystem());
        tr.empire().addColonizedSystem(starSystem());

        empire = tr.empire();
        buildFortress();
        shipyard().goToNextDesign();

        rebels = 0;
        rebellion = false;
        clearReserveIncome();
        clearTransport();
        loser.sv.refreshFullScan(starSystem().id);
        empire.sv.refreshFullScan(starSystem().id);

        if (loser.numColonies() == 0)
            loser.goExtinct();
    }
    public void capturedOrion(Transport tr) {
        setPopulation(tr.size());
        tr.size(0);
        industry().capturedBy(tr.empire());
        defense().capturedBy(tr.empire());
        ecology().capturedBy(tr.empire());

        empire = tr.empire();
        empire.setRecalcDistances();
        buildFortress();
        shipyard().goToNextDesign();

        rebels = 0;
        rebellion = false;
        clearReserveIncome();
    }
    public void takeCollateralDamage(float damage) {
        if (destroyed())
            return;

        if (defense().bases() < 1)
            takeUntargetedCollateralDamage(damage);
        else
            takeTargetedCollateralDamage(damage);
    }
    public void takeTargetedCollateralDamage(float damage) {
        float newPop = max(0, population() - (damage / TARGETED_DAMAGE_FOR_POPLOSS));
        float newFact = max(0, industry().factories() - (damage / TARGETED_DAMAGE_FOR_FACTLOSS));

        setPopulation(newPop);
        industry().factories(newFact);

        if (population() <= 0)
            destroy();
    }
    public void takeUntargetedCollateralDamage(float damage) {
        float newPop = max(0, population() - (damage / UNTARGETED_DAMAGE_FOR_POPLOSS));
        float newFact = max(0, industry().factories() - (damage / UNTARGETED_DAMAGE_FOR_FACTLOSS));

        setPopulation(newPop);
        industry().factories(newFact);

        if (population() <= 0)
            destroy();
    }
    public void takeBioweaponDamage(float damage) {
        float popLost = max(0, damage - tech().antidoteLevel());

        setPopulation(max(0, population() - popLost));

        float newWaste = popLost * 10;
        ecology().addWaste(newWaste);

        if (population() <= 0)
            destroy();
    }
     public void destroy() {
        if (isCapital())
            empire().chooseNewCapital();
        
        starSystem().addEvent(new SystemDestroyedEvent(empire().lastAttacker()));

        setPopulation(0);
        rebels = 0;
        captives = 0;
        rebellion = false;
        planet.addAlienFactories(empire.id, (int) industry().factories());

        transport = null;
        clearReserveIncome();
        empire.removeColonizedSystem(starSystem());
        planet.setColony(null);
        // update system views of civs that would notice
        empire.sv.refreshFullScan(starSystem().id);
        List<ShipFleet> fleets = starSystem().orbitingFleets();
        for (ShipFleet fl : fleets)
            fl.empire().sv.refreshFullScan(starSystem().id);
    }

    private boolean governor = false;

    public boolean isGovernor() {
        return governor;
    }

    public void setGovernor(boolean governor) {
        this.governor = governor;
    }

    /**
     * Increment slider. Stop moving when results no longer contains "stopWhenDisappears".
     * Stop when results contain "stopWhenAppears".
     * If moving slider doesn't change production, stop as well.
     */
    private void moveSlider(int category, String stopWhenDisappears, String stopWhenAppears) {
        ColonySpendingCategory cat = category(category);
        int previousAllocaton = -1;
        for (;;) {
            String result = cat.upcomingResult();
            if (stopWhenDisappears != null && !result.contains(stopWhenDisappears)) {
                break;
            }
            if (stopWhenAppears != null) {
                if (result.contains(stopWhenAppears)) {
                    break;
                }
            }
            increment(category, 1);
            if (previousAllocaton == cat.allocation()) {
                break;
            }
            previousAllocaton = cat.allocation();
        }
    }

    public void governIfNeeded() {
        if (!this.isAutopilot() && this.isGovernor()) {
            govern();
        }
    }
    /**
     * Govern the colony.
     * - First, set ecology to minimum.
     * - Then set industry to maximum. Skip a turn if there are more factories than population can control to
     * spend production on ecology instead.
     * - Then set ecology to maximum.
     * - Then set defence to maximum.
     *
     * This is quite crude- works by moving slider by 1 tick until desired results happen.
     * Better way would be to calculate and set each slider directly to the right percentage.
     *
     */
    public void govern() {
        if (!"false".equalsIgnoreCase(System.getProperty("autotransport"))) {
            autotransport();
        }
        // unlock all sliders
        for (int i = 0; i <= 4; i++) {
            locked(i, false);
        }
        // start from scratch
        clearSpending();
        // Add minimum ecology
        moveSlider(Colony.ECOLOGY, "Waste", null);
        locked(Colony.ECOLOGY, true);
        // Add maximum industry if factories would actually get used
        float canBeUsed = workingPopulation() * (float)industry().effectiveRobotControls();
        // if we can refit, we need to spend on industry anyway
        boolean refit = industry().effectiveRobotControls() < empire().maxRobotControls();
        if (!industry().isCompleted() && (refit || industry().factories() <= canBeUsed)) {
            moveSlider(Colony.INDUSTRY, null, "Reserve");
        }
        locked(Colony.INDUSTRY, true);
        // Add maximum ecology
        if (!ecology().isCompleted() || ecology().enrichSoilCost() > 0.0 || planet().canTerraformAtmosphere(this.empire())) {
            locked(Colony.ECOLOGY, false);
            moveSlider(Colony.ECOLOGY, null, "Reserve");
            locked(Colony.ECOLOGY, true);
        }
        // add maximum defence
        if (!defense().isCompleted()) {
            moveSlider(Colony.DEFENSE, null, "Reserve");
        }
        // Build gate if tech is available. Also add a system property to turn it off.
        if (!"false".equalsIgnoreCase(System.getProperty("autogate"))) {
            if (this.shipyard().canBuildStargate()) {
                Design first = this.shipyard().design();
                Design current = this.shipyard().design();
                while (!this.empire.shipLab().stargateDesign().equals(current)) {
                    this.shipyard().goToNextDesign();
                    current = this.shipyard().design();
                    if (current.equals(first)) {
                        System.out.println("unable to cycle to Shargate design");
                        break;
                    }
                }
                if (this.empire.shipLab().stargateDesign().equals(current)) {
                    locked(Colony.SHIP, false);
                    moveSlider(Colony.SHIP, null, "Reserve");
                }
            }
        }

        // if all sliders are set to 0, increase research.
        boolean noSpending = true;
        for (int i = Colony.SHIP; i <= Colony.RESEARCH; i++) {
            if (allocation[i] > 0) {
                noSpending = false;
                break;
            }
        }
        if (noSpending) {
//            System.out.println("NO SPENDING "+this.name());
            moveSlider(Colony.RESEARCH, null, "Reserve");
        }
        // unlock industry slider. Thanks DM666a
        locked(Colony.INDUSTRY, false);
    }

    public float unrestrictedPopGrowth() {
        // assume we send out 2 population, calc growth then
        float baseGrowthRate = this.max(0.0f, (1.0f - this.workingPopulation() / (this.planet.currentSize()-2)) / 10.0f);
        baseGrowthRate *= this.empire.race().growthRateMod();
        if (!this.empire.race().ignoresPlanetEnvironment()) {
            baseGrowthRate *= this.planet.growthAdj();
        }
        float newGrownPopulation = this.max(0.1f, this.workingPopulation() * baseGrowthRate);
        return newGrownPopulation;
    }
    // Try to transport extra population to other plants.
    // Let's not do complex pop growth calculations. Send 1 transport at max pop, assume planets don't grow
    // on their own
    // TODO: add a toggle to make this optional
    private void autotransport() {
        if (transporting() || !canTransport() || maxTransportsAllowed() <= 0) {
            return;
        }
        // we don't have excess population
        if (expectedPopulation() < planet.currentSize()) {
            return;
        }
        float floatExcess = this.workingPopulation() + unrestrictedPopGrowth() + incomingTransports() - planet().currentSize();
        // if we are at max pop, send out transports even if growth is between 0 and 1, so always round up
        int excess = (int)Math.ceil(floatExcess);
//        System.out.println("autotransport "+this.name()+" excess "+excess);
//        System.out.println("autotransport "+this.name()+" growth "+normalPopGrowth());
//        System.out.println("autotransport "+this.name()+" ugrowth "+unrestrictedPopGrowth());
        // find a suitable target. Closest colony that needs population
        List<StarSystem> targets = new ArrayList<>(empire().allColonizedSystems().size());
        Map<StarSystem, Float> transportTimes = new HashMap<>();
        Map<StarSystem, Float> populationFractions = new HashMap<>();
        for (StarSystem ss: empire().allColonizedSystems()) {
            // don't transport to self
            if (ss.colony() == this) {
                continue;
            }
            // don't transport to systems that have 80% population already
            if (ss.colony().expectedPopulation() >= ss.planet().currentSize()*0.8) {
                continue;
            }
            float transportTime = ss.travelTime(this, ss, this.empire().tech().transportSpeed());
            double expectedPopAtTransportTime = ss.colony().population() +
                    Math.pow(1+ss.colony().normalPopGrowth() / ss.colony().population(), transportTime);
            float popFraction = ss.colony().population() / ss.planet().currentSize();
//            System.out.println("autotransport "+this.name()+" to "+ss.name()+" time "+transportTime+" exp growth "+expectedPopAtTransportTime+" pop % "+popFraction);
            if (expectedPopAtTransportTime >= ss.planet().currentSize()*0.9) {
                continue;
            }
            targets.add(ss);
            transportTimes.put(ss, transportTime);
            populationFractions.put(ss, popFraction);
        }
        // no viable targets for pop transport
        if (targets.isEmpty()) {
            return;
        }

        // Turn distance into relative distance (% of distance to furthest possible target).

        // Turn distance % and population size % into rank.
        // Lets use distance% linear and pop% linear
        float maxTransportTime = transportTimes.values().stream().max(Float::compare).get();
        Map<StarSystem, Float> ranks = new HashMap<>();
        for (StarSystem ss: transportTimes.keySet()) {
            float absolute = transportTimes.get(ss);
            float relativeTransportTime = absolute / maxTransportTime;

            float populationFraction = populationFractions.get(ss);

            float rank = relativeTransportTime + populationFraction;
            ranks.put(ss, rank);
        }

        Collections.sort(targets, (o1, o2) -> {
            float rank1 = ranks.get(o1);
            float rank2 = ranks.get(o2);
            return (int) Math.signum(rank1-rank2);
        });
//        for (StarSystem ss: targets) {
//            System.out.println("autotransport target from "+this.name()+" to "+ss.name()+" rank "+ranks.get(ss));
//        }
        // round excess down
        int toTransport = Math.min(maxTransportsAllowed(), excess);
        // let's make sure we don't trigger governor in scheduleTransportsToSystem, otherwise we get endless recursion
        governor = false;
        scheduleTransportsToSystem(targets.get(0), toTransport);
        governor = true;
    }

}
