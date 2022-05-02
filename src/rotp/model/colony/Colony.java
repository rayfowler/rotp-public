/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU GeneraFl Public License, Version 3 (the "License");
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
import java.util.EnumMap;
import java.util.List;
import static rotp.model.colony.ColonySpendingCategory.MAX_TICKS;
import rotp.model.empires.DiplomaticTreaty;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.events.SystemAbandonedEvent;
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
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.tech.Tech;
import rotp.model.tech.TechMissileWeapon;
import rotp.model.tech.TechTree;
import rotp.ui.RotPUI;
import rotp.ui.notifications.GNNNotification;
import rotp.ui.notifications.InvadersKilledAlert;
import rotp.ui.notifications.TransportsKilledAlert;
import rotp.util.Base;

public final class Colony implements Base, IMappedObject, Serializable {
    private static final long serialVersionUID = 1L;
    private static final int[] validationSeq = { 3, 2, 0, 1, 4 };
    private static final int[] spendingSeq = { 4, 0, 1, 2, 3 };
    private static final int[] cleanupSeq =  { 2, 4, 1, 0, 3 };
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
                        "TECH_ALLOCATE_ATMOSPHERE"), TERRAFORM("TECH_ALLOCATE_TERRAFORM"), POPULATION("TECH_ALLOCATE_POPULATION");
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
    public boolean keepEcoLockedToClean; 
    private transient boolean hasNewOrders = false;
    private transient int cleanupAllocation = 0;
    private transient boolean recalcSpendingForNewTaxRate;
    public transient boolean reallocationRequired = false;

    public void toggleRecalcSpending()         { recalcSpendingForNewTaxRate = true; }
    public boolean underSiege()                { return underSiege; }
    public float reserveIncome()               { return reserveIncomeBC; }
    public void clearReserveIncome()           { reserveIncomeBC = 0; }
    public void adjustReserveIncome(float bc)  { reserveIncomeBC += bc; }
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
    public float untargetedHitPoints()         { return UNTARGETED_DAMAGE_FOR_POPLOSS * population(); }
    public void clearAllRebellion() {
        rebels = 0;
        rebellion = false;
    }
    public boolean isGovernor()                { return false; }
    public float currentProductionCapacity() {
        // returns a pct (0 to 1) representing the colony's current
        // production vs its maximum possible formula
        float pop = population();
        float maxPop = planet().maxSizeAfterSoilAtmoTform();
        float maxFactories = maxPop * industry().maxRobotControls();
        float factories = min(maxFactories, industry().factories(), industry().maxUseableFactories());
        
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
    private int cleanupAllocation() {
        if (cleanupAllocation < 0)
            cleanupAllocation = ecology().cleanupAllocationNeeded();
        return cleanupAllocation;
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
        defense().maxBases(empire().defaultMaxBases());
        cleanupAllocation = -1;

        // empires of orbiting fleets should see ownership change
        StarSystem sys = starSystem();
        List<ShipFleet> fleets = sys.orbitingFleets();
        for (ShipFleet fl: fleets) {
            Empire flEmp = fl.empire();
            if (flEmp != empire)
                flEmp.sv.refreshFullScan(sys.id);
        }

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
    public void removeStargate()         { shipyard().removeStargate(); }

    public int totalAmountAllocated() {
        int amt = 0;
        for (ColonySpendingCategory cat : spending)
            amt += cat.allocation();

        return amt;
    }
    public int allocationRemaining()              { return MAX_TICKS - totalAmountAllocated(); }
    public float totalPlanetaryResearch()         { 
        float totalBC = research().totalSpending();
        float productAdj = planet().productionAdj();
        if (empire.divertColonyExcessToResearch()) {
            totalBC += shipyard().excessSpending() / productAdj;
            totalBC += defense().excessSpending() / productAdj;
            totalBC += industry().excessSpending() / productAdj;
            totalBC += ecology().excessSpending();
        }        
        float totalRP = totalBC * research().researchBonus();
        return max(0, totalRP-research().projectRemainingBC());
    }
    public float totalPlanetaryResearchSpending() { 
        float totalBC = research().totalSpending(); 
        float productAdj = planet().productionAdj();
        if (empire.divertColonyExcessToResearch()) {
            totalBC += shipyard().excessSpending() / productAdj;
            totalBC += defense().excessSpending() / productAdj;
            totalBC += industry().excessSpending() / productAdj;
            totalBC += ecology().excessSpending();
        }
        return totalBC;
    }  
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
    public int deltaPopulation()             { return (int) population - (int) previousPopulation - (int) inTransport(); }
    public boolean destroyed()               { return population <= 0; }
    public boolean inRebellion()             { return rebellion && (rebels > 0); }
    public float rebellionPct()             { return rebels / population(); }
    public boolean hasOrders()               { return !orders.isEmpty(); }
    
    public boolean isDeveloped()  {
        return defense().isCompleted() && industry().isCompleted() && ecology().isCompleted(); 
    }

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
        redistributeReducedEcoSpending();
    }
    public boolean increment(int catNum, int amt) {
        if (!canAdjust(catNum))
            return false;

        int newValue = allocation(catNum)+amt;
        if ((newValue < 0)
        || (newValue > MAX_TICKS))
            return false;

        if (catNum == ECOLOGY)
            keepEcoLockedToClean = false;
            
        allocation(catNum, newValue);
        realignSpending(spending[catNum]);
        spending[catNum].removeSpendingOrders();
        return true;
    }
    public String shipyardProject() {
        if (shipyard().allocation() > 0) {
            int limit = shipyard().buildLimit();
            if (limit == 0)
                return shipyard().design().name();
            else
                return str(limit)+" "+shipyard().design().name();
        }
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
        reallocationRequired = true;
    }
    public void removeColonyOrder(Colony.Orders order) {
        if (orders.containsKey(order)) {
            orders.remove(order);
            reallocationRequired = true;
        }
    }

    public void setHomeworldValues() {
        setPopulation(50);
        previousPopulation = population();
        industry().factories(30);
        industry().previousFactories(30);

        Empire emp = empire();
        ShipDesignLab lab = emp.shipLab();
        ShipDesign scout = lab.scoutDesign();
        ShipDesign colony = lab.colonyDesign();
        galaxy().ships.buildShips(emp.id, starSystem().id, scout.id(), 2);
        galaxy().ships.buildShips(emp.id, starSystem().id, colony.id(), 1);
        lab.recordConstruction(scout, 2);
        lab.recordConstruction(colony, 1);
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
            message = text(messageKey, sys.name(), rebels);
            message = empire.replaceTokens(message, "rebelling");
            galaxy().giveAdvice("MAIN_ADVISOR_REBELLION", sys.name());
        }
        else if (empire.hasContact(pl) && pl.sv.isScouted(sys.id)) {
            message = text("GNN_ALIEN_REBELLION", pl.sv.name(sys.id), rebels);
            message = empire.replaceTokens(message, "rebelling");
        }
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
        cleanupAllocation = -1;
    }

    public void nextTurn() {
        log("Colony: ", empire.sv.name(starSystem().id),  ": NextTurn [" , shipyard().design().name() , "|" ,str(shipyard().allocation()) , "-"
                    , str(defense().allocation()) , "-" , str(industry().allocation()) , "-" , str(ecology().allocation()) , "-"
                    , str(research().allocation()) , "]");
        keepEcoLockedToClean = empire().isPlayerControlled() && (allocation[ECOLOGY] <= cleanupAllocation());
        previousPopulation = population;
        reallocationRequired = false;          
        ensureProperSpendingRates();
        validateOnLoad();
        
        // if rebelling, nothing happens (only enough prod assumed to clean new
        // waste and maintain existing structures)
        if (inRebellion())
            return;

        // after turn is over, we may need to reset ECO spending to adjust for cleanup
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

        planet().removeExcessWaste();
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
        
        checkEcoAtClean();
        
        if (reallocationRequired)
            empire().governorAI().setColonyAllocations(this);            

    }
    public void addFollowUpSpendingOrder(float orderAmt) {
        if (orderAmt <= 0)
            return;

        // a spending order has completed, ripple it down to the next priority
        // so the colony progresses to completion
        ColonyEcology eco = ecology();
        ColonyIndustry ind = industry();
        ColonyDefense def = defense();
        if (!eco.terraformCompleted()) 
            addColonyOrder(Colony.Orders.TERRAFORM, orderAmt);
        else if (!ind.isCompleted())
            addColonyOrder(Colony.Orders.FACTORIES, orderAmt);
        else if (!eco.populationGrowthCompleted())
            addColonyOrder(Colony.Orders.POPULATION, orderAmt);
        else if (!def.shieldAtMaxLevel())
            addColonyOrder(Colony.Orders.SHIELD, orderAmt);
        else if (!def.missileBasesCompleted())
            addColonyOrder(Colony.Orders.BASES, orderAmt/5);
    }
    public void checkEcoAtClean() {
        recalcSpendingForNewTaxRate = false;
        if (locked[ECOLOGY]) 
            return;
        
        int cleanAlloc = ecology().cleanupAllocationNeeded();
        if (allocation[ECOLOGY] == cleanAlloc)
            return;
        
        // always ensure we are at least at clean
        if (allocation[ECOLOGY] < cleanAlloc) {
            allocation[ECOLOGY] = cleanupAllocation = cleanAlloc;
            redistributeReducedEcoSpending();
            return;
        }
        
        // if we are over clean but the colony started its turn at clean
        // then lower
        if ((allocation[ECOLOGY] > cleanAlloc) && keepEcoLockedToClean) {
            allocation[ECOLOGY] = cleanupAllocation = cleanAlloc;
            redistributeReducedEcoSpending();
        }
    }
    public void lowerECOToCleanIfEcoComplete() {
        // this will NOT adjust ECO spending if it is locked
        // or manually set to level lower than clean
        if (locked[ECOLOGY])
            return;
        
        int cleanAlloc = ecology().cleanupAllocationNeeded();
        if (allocation[ECOLOGY] < cleanAlloc)
            return;
        
        // if ECO spending is complete, just lower ECO to clean
        // and auto-realign the other categories
        if (ecology().isCompleted()) {
            if (allocation[ECOLOGY] != cleanAlloc) {
                allocation(ECOLOGY, cleanAlloc);
                realignSpending(ecology());
            }
            return;
        }
            
        // if ECO is not complete, then if it exceeds
        // maxAlloc, then lower it to that and realign
        int maxAlloc = ecology().maxAllocationNeeded();
        if (allocation[ECOLOGY] > maxAlloc) {
                allocation(ECOLOGY, maxAlloc);
                realignSpending(ecology());
        }
        
    }
    public boolean canLowerMaintenance() { return transporting(); }

    public void lowerMaintenance() {
        if (transporting()) {
            int maxSize = maxTransportsAllowed();
            if (inTransport() > maxSize) {
                // log("reducing transport levels from ", planet.name(), " to ",
                // str(transport().originalSize), " to avoid income loss");
                transport().size(maxSize);
                transport().launchSize(maxSize);
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
    private void redistributeReducedEcoSpending() {
        int maxAllocation = ColonySpendingCategory.MAX_TICKS;
        // determine how much categories are over/under spent
        int spendingTotal = 0;
        for (int i = 0; i < NUM_CATS; i++)
            spendingTotal += spending[i].allocation();

        int adj = maxAllocation - spendingTotal;
        if (adj == 0)
            return;
        
        // funnel excess to industry if it's not completed
        if (!industry().isCompleted() && adj > 0)
            adj -= spending[INDUSTRY].adjustValue(adj);
        
        // if we are building ships and doing no research, then assume this is a shipbuilding
        // colony and put the rest of the excess in shipbuilding. Good catch, Xilmi
        if (!locked(SHIP) && (spending[SHIP].allocation() > 0) && (spending[RESEARCH].allocation() == 0))
            adj -= spending[SHIP].adjustValue(adj);
        
        if (adj == 0)
            return;
        
        // put whatever is left or take whatever is missing acording to the spending-sequence
        for (int i = 0; i < NUM_CATS; i++) {
            ColonySpendingCategory currCat = spending[spendingSeq[i]];
            if ((spendingSeq[i] != ECOLOGY) && !locked(spendingSeq[i]))
                adj -= currCat.adjustValue(adj);
        }
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
        float factoryOutput = mod*(workerProd + usedFactories());
        return factoryOutput - transportCost();
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
    public float colonyTaxPct() {
        if (embargoed())
            return 0f;
        // we are taxed at the empire rate if the empire is taxing all colonies, or we are finished developing
        float empireTaxPct = empire.empireTaxPct();
        float colonyTaxPct = 0.0f;
        if (empireTaxPct > 0) { // let's avoid unnecessary calls to isDeveloped()
            if (!empire.empireTaxOnlyDeveloped() || isDeveloped())
                colonyTaxPct = empireTaxPct;
        }
        return colonyTaxPct;
    }
    public void ensureProperSpendingRates() {
        if (recalcSpendingForNewTaxRate) 
            checkEcoAtClean();
    }
    public float totalProductionIncome() {
        if (inRebellion())
            return 0.1f;
        
        ensureProperSpendingRates();

        float prod = production();       
        float reserveCost = prod * colonyTaxPct();
        float securityCost = prod * empire.totalSecurityCostPct();
        float shipCost = prod * empire.shipMaintCostPerBC();
        float stargateCost = prod * empire.stargateCostPerBC();
        float tradeIncome = actualTradeIncome();
        float defenseCost = prod * empire.missileBaseCostPerBC();
        float shipyardCost = shipyard().maintenanceCost();

        return max(0, prod - reserveCost - securityCost - defenseCost - shipyardCost + tradeIncome - shipCost - stargateCost);
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
    public int calcPopNeeded(float desiredPct) {
        return (int) ((planet.currentSize() * desiredPct) - expectedPopulation());
    }
    public int calcPopToGive(float retainPct) {
        if (!canTransport())
            return 0;
        int p1 = maxTransportsAllowed();
        int p2 = (int) (population() - (retainPct * planet().currentSize()));
        return min(p1,p2);
    }
    public float newWaste() {
        float mod = empire().isPlayer() ? 1.0f : options().aiWasteModifier();
        return max(0, usedFactories() * tech().factoryWasteMod() * mod);
    }
    public float wasteCleanupCost() {
        if (empire.ignoresPlanetEnvironment())
            return 0;
        
        return (min(planet.maxWaste(), planet.waste()) + newWaste()) / tech().wasteElimination();
    }
    public float minimumCleanupCost() {
        return min(wasteCleanupCost(), totalIncome());
    }
    public void ensureMinimumCleanup() {
        float pct = wasteCleanupCost()/totalIncome();
        if (ecology().pct() < pct)
            forcePct(ECOLOGY, pct);
    }
    public float maxSize() {
        float terraformAdj = tech().terraformAdj();
        if (planet.isEnvironmentHostile())
            terraformAdj *= options().hostileTerraformingPct();
        return max(planet.currentSize(), planet.baseSize()+terraformAdj);
    }
    public float maxUseableFactories() {
        return workingPopulation() * empire().maxRobotControls();
    }
    public float normalPopGrowth() {
        // calculate growth rate based on current pop, environment & race
        float maxNewPopulation = planet.currentSize() - workingPopulation();
        float baseGrowthRate = max(0, (1 - (workingPopulation() / planet.currentSize())) / 10);
        baseGrowthRate *= empire.growthRateMod();
        if (!empire.ignoresPlanetEnvironment())
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
        starSystem().clearTransportSprite();
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
            if (transport().size() >= (int)population()) {
                abandon();
                return;
            }
            setPopulation(population() - transport().size());
            transport = new Transport(starSystem());
            if (empire.isPlayerControlled())
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
        checkEcoAtClean();
        // reset ship views
        if (empire.isPlayerControlled())
            empire.setVisibleShips();
    }
    public void acceptTransport(Transport t) {
        if (!t.empire().canColonize(starSystem())) {
            // no appropriate alert message for this transport loss. This is an edge case anyway
            // as it occurs only when the destination system has been rendered inhabitable by a 
            // random event while the transport was in transits
            t.size(0);
            return;
        }
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
        
        if (!tr.empire().canColonize(starSystem())) {
            // no appropriate alert message for this transport loss. Even more of an edge case.
            tr.size(0);
            return;
        }
        
        captives = population() - rebels;
        setPopulation(rebels);

        if (population() > 0) {
            if (empire.isPlayerControlled() || tr.empire().isPlayerControlled())
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
        
        if (!tr.empire().canColonize(starSystem())) {
            if (tr.empire().isPlayerControlled()) 
                TransportsKilledAlert.create(empire(), starSystem(), tr.launchSize());
            else if (empire().isPlayerControlled()) 
                InvadersKilledAlert.create(tr.empire(), starSystem(), tr.launchSize());
            tr.size(0);
            return;
        }
        
        int passed = 0;
        int num = tr.size();
        float pct = tr.combatTransportPct();

        EmpireView ev = tr.empire().viewForEmpire(empire);

        if (ev != null) {
            if (ev.embassy().unity())
                return;
            // don't cause war if treaty signed since launch
            if (!ev.embassy().anyWar() && (ev.embassy().treatyDate() >= tr.launchTime()))
                return;
            // don't cause war if planet now occupied by another race
            if (!ev.embassy().anyWar() && (empire != tr.targetCiv()))
                return;
            if (!ev.embassy().anyWar())
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

        // choose most effective missile dmg
        int missileDmg = 0;
        MissileBase base = defense().missileBase();
        TechMissileWeapon scatter = base.scatterPack() == null ? null : base.scatterPack().tech();
        TechMissileWeapon missile = defense().missileBase().missile().tech();
        if (scatter != null)
            missileDmg = 3*max(missile.damage(), scatter.damage() * scatter.scatterAttacks());
        else 
            missileDmg = 3*missile.damage();

        int lost = 0;

        // start with base missile damage
        float defenderDmg = defense().missileBases() * missileDmg;

        // add firepower for each allied ship in orbit
            // modnar: use firepowerAntiShip to only count ship weapons that can hit ships
            // to prevent ground bombs from being able to damage transports
        List<ShipFleet> fleets = starSystem().orbitingFleets();
        for (ShipFleet fl : fleets) {
            if (fl.empire().aggressiveWith(tr.empId()))
                defenderDmg += fl.firepowerAntiShip(0);
        }

        // run the gauntlet
        for (int j = 0; j < tr.gauntletRounds(); j++)
            lost += (int) (defenderDmg / tr.hitPoints());

        passed += max(0, (num - lost));

        tr.size(passed);

        // if gauntlet not passed, stop and inform player (if player)
        // neither of these incidents are added to the embassies. They are for
        // player notification only.
        if (tr.size() == 0) {
            log(concat(str(tr.launchSize()), " ", tr.empire().raceName(), " transports perished at ", name()));
            if (tr.empire().isPlayerControlled()) 
                TransportsKilledAlert.create(empire(), starSystem(), tr.launchSize());
            else if (empire().isPlayerControlled()) 
                InvadersKilledAlert.create(tr.empire(), starSystem(), tr.launchSize());
            return;
        }

        float startingPop = population();
        if (population() > 0) {
            if (empire.isPlayerControlled() || tr.empire().isPlayerControlled())
                RotPUI.instance().selectGroundBattlePanel(this, tr);
            else
                completeDefenseAgainstTransports(tr);
        }

        float pctLost = min(1, ((startingPop - population()) / startingPop));
        int popLost = (int) startingPop -  (int) population();
        int rebelsLost = (int) Math.ceil(pctLost*rebels);
        rebels = rebels - rebelsLost;
        
        DiplomaticTreaty treaty = empire().treaty(tr.empire());
        if (treaty != null) {
            treaty.losePopulation(empire(), startingPop-population());
            treaty.losePopulation(tr.empire(), tr.launchSize()-tr.size());
        }

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

        Empire pl = player();
        if (tr.empire().isPlayerControlled()) {
            allocation(SHIP, 0);
            allocation(DEFENSE,0);
            allocation(INDUSTRY,0);
            allocation(ECOLOGY,0);
            allocation(RESEARCH,0);
            String str1 = text("MAIN_ALLOCATE_COLONY_CAPTURED", pl.sv.name(starSystem().id), pl.raceName());
            str1 = pl.replaceTokens(str1, "spy");
            session().addSystemToAllocate(starSystem(), str1);
        }
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

        setPopulation(min(planet.currentSize(),tr.size()));
        tr.size(0);
        shipyard().capturedBy(tr.empire());
        industry().capturedBy(tr.empire());
        defense().capturedBy(tr.empire());
        ecology().capturedBy(tr.empire());
        research().capturedBy(tr.empire());

        StarSystem sys = starSystem();
        empire.removeColonizedSystem(sys);
        empire.stopRalliesWithSystem(sys);
        tr.empire().addColonizedSystem(sys);

        empire = tr.empire();
        defense().maxBases(empire.defaultMaxBases());
        buildFortress();
        shipyard().goToNextDesign();

        rebels = 0;
        rebellion = false;
        clearReserveIncome();
        clearTransport();
        loser.sv.refreshFullScan(sys.id);
        empire.sv.refreshFullScan(sys.id);
        
        // empires of orbiting fleets should see ownership change
        List<ShipFleet> fleets = sys.orbitingFleets();
        for (ShipFleet fl: fleets) {
            Empire flEmp = fl.empire();
            if ((flEmp != loser) && (flEmp != empire))
                flEmp.sv.refreshFullScan(sys.id);
        }

        if (loser.numColonies() == 0)
            loser.goExtinct();
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
        planet().removeExcessWaste();

        if (population() <= 0)
            destroy();
    }
    public void abandon() {
        if (isCapital())
            empire.chooseNewCapital();
        
        StarSystem sys = starSystem();
        sys.addEvent(new SystemAbandonedEvent(empire.id));
        sys.abandoned(true);

        setPopulation(0);
        rebels = 0;
        captives = 0;
        rebellion = false;
        planet.addAlienFactories(empire.id, (int) industry().factories());

        transport = null;
        clearReserveIncome();
        empire.removeColonizedSystem(sys);
        empire.stopRalliesWithSystem(sys);
        planet.setColony(null);
        // update system views of civs that would notice
        empire.sv.refreshFullScan(sys.id);
        List<ShipFleet> fleets = sys.orbitingFleets();
        for (ShipFleet fl : fleets) 
            fl.empire().sv.refreshFullScan(sys.id);
        
        for (Empire emp: galaxy().empires()) {
            if (emp.knowsOf(empire) && !emp.sv.name(sys.id).isEmpty()) 
                emp.sv.view(sys.id).setEmpire();                   
        }
    }
    public void destroy() {
        if (isCapital())
            empire.chooseNewCapital();
        
        StarSystem sys = starSystem();
        sys.addEvent(new SystemDestroyedEvent(empire.lastAttacker()));

        setPopulation(0);
        rebels = 0;
        captives = 0;
        rebellion = false;
        planet.addAlienFactories(empire.id, (int) industry().factories());

        transport = null;
        clearReserveIncome();
        empire.removeColonizedSystem(sys);
        empire.stopRalliesWithSystem(sys);
        planet.setColony(null);
        // update system views of civs that would notice
        empire.sv.refreshFullScan(sys.id);
        List<ShipFleet> fleets = sys.orbitingFleets();
        for (ShipFleet fl : fleets) 
            fl.empire().sv.refreshFullScan(sys.id);
        
        for (Empire emp: galaxy().empires()) {
            if (emp.knowsOf(empire) && !emp.sv.name(sys.id).isEmpty()) 
                emp.sv.view(sys.id).setEmpire();                   
        }
    }
}
