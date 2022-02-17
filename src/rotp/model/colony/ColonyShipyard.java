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
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.Design;
import rotp.model.ships.DesignStargate;
import rotp.model.ships.ShipDesign;
import rotp.model.tech.TechStargate;

public class ColonyShipyard extends ColonySpendingCategory {
    private static final long serialVersionUID = 1L;
    private boolean hasStargate = false;
    private boolean buildingStargate = false;
    private Design design;
    private Design prevDesign;
    private float stargateBC = 0;
    private float shipBC = 0;
    private float shipReserveBC = 0;
    private int newShips = 0;
    private boolean stargateCompleted = false;
    private int buildLimit = 0;
    private boolean shipLimitReached = false;
    private transient ShipFleet rallyFleet;
    private transient float maxAllowedShipBCProd;
    private transient int rallyCount = 0;
    private transient int rallyDesignId = 0;
    private transient int rallyDestSysId = StarSystem.NULL_ID;

    // used by the AI when determining what to build
    private float queuedBC = 0;
    private int desiredShips = 0;

    public boolean hasStargate()              { return hasStargate; }
    public boolean stargateCompleted()        { return stargateCompleted; }
    public void removeStargate()  { hasStargate = stargateCompleted = false; }
    public boolean shipLimitReached()         { return shipLimitReached; }
    public Design design()                    { return design; }
    public void design(Design d)              { design = d; }
    public void addQueuedBC(float d)          { queuedBC += d; }
    public int desiredShips()                 { return desiredShips; }
    public void addDesiredShips(int i)        { desiredShips += i; }
    public ShipFleet rallyFleet()             { return rallyFleet; }
    public int buildLimit()                   { return buildLimit; }
    public void buildLimit(int i)             { buildLimit = max(0,i); }
    public String buildLimitStr() { return buildLimit == 0 ? text("MAIN_COLONY_SHIPYARD_LIMIT_NONE") : str(buildLimit); }
    public boolean incrementBuildLimit(int amt)  { buildLimit += amt;  return true; }
    public boolean decrementBuildLimit(int amt)  { 
        if (buildLimit == 0)
            return false;
        
        buildLimit = max(0, buildLimit - amt);
        return true;
    }
    public boolean resetBuildLimit()         {
        if (buildLimit == 0)
            return false;
        buildLimit = 0;
        return true;
    }
    private float maxAllowedShipBCProd() {
        if (maxAllowedShipBCProd < 0)
            maxAllowedShipBCProd = empire().governorAI().maxShipBCPermitted(colony())* planet().productionAdj();
        return maxAllowedShipBCProd;
    }
    @Override
    public void init(Colony c) {
        super.init(c);
        hasStargate = false;
        buildingStargate = false;
        design = null;
        prevDesign = null;
        stargateBC = 0;
        shipBC = 0;
        newShips = 0;
        maxAllowedShipBCProd = -1;
    }
    @Override
    public int categoryType()            { return Colony.SHIP; }
    @Override
    public float totalBC()              { return super.totalBC() * planet().productionAdj(); }
    @Override
    public boolean isCompleted()         { return false; }
    @Override
    public boolean canLowerMaintenance() { return stargateMaintenanceCost() > 0; }
    @Override
    public void lowerMaintenance()       { hasStargate = false; }
    @Override
    public void assessTurn()             { 
        if (rallyCount ==0)
            return;
        
        Colony c = colony();
        galaxy().ships.rallyOrbitingShips(c.empire().id, c.starSystem().id, rallyDesignId, rallyCount, rallyDestSysId);
 
    }
    public boolean building()            { return queuedBC > 0; }
    public boolean willingToBuild(Design d) {
        if (design == d)
            return queuedBC < maxAllowedShipBCProd();
        else
            return queuedBC == 0;
    }
    public void resetQueueData() {
        queuedBC = 0;
        desiredShips = 0;
    }
    public boolean buildingObsoleteDesign() {
        return !design.active();
    }
    public float queuedBCForDesign(Design d) {
        if (prevDesign != d)
            return 0;
        else if (buildingStargate)
            return stargateBC;
        else
            return shipBC;
    }
    public float turnsToBuild(Design d) {
        if (!willingToBuild(d))
            return Integer.MAX_VALUE;

        float planetProd = maxAllowedShipBCProd();
        float alreadyDone = 0;

        if (d == prevDesign)
            alreadyDone = shipBC - queuedBC;
        else
            alreadyDone = min(shipBC, planetProd) - queuedBC;

        return (d.cost() - alreadyDone) / planetProd;
    }
    public float stargateMaintenanceCost() {
        return hasStargate ? TechStargate.MAINTENANCE : 0;
    }
    public float maintenanceCost() {
        return 0;
    }
    @Override
    public void nextTurn(float totalProd, float totalReserve) {
        rallyCount = 0;
        rallyDesignId = 0;
        rallyDestSysId = StarSystem.NULL_ID;
        maxAllowedShipBCProd = -1;
        // if we switched designs, send previous ship BC to shipyard reserve
        if (design != prevDesign) {
            if (prevDesign instanceof DesignStargate)
                stargateBC += shipBC;
            else
                shipReserveBC += shipBC;
            shipBC = 0;
        }
        // prod gets planetary bonus, but not reserve
        float prodBC = pct()* totalProd * planet().productionAdj();
        // shipyard reserve will match production BC
        float rsvBC = pct() * totalReserve;
        float newBC = prodBC+rsvBC;

        float cost = design.cost();
        newShips = 0;
        shipLimitReached = false;
        stargateCompleted = false;

        if (colony().allocation(categoryType()) == 0)
            return;
        
        // should never happen anymore, but hey
        if (buildingObsoleteDesign()) {
            empire().addReserve(newBC);
            return;
        }
        
        if (buildingStargate) {
            stargateBC += newBC;
            if (stargateBC >= cost) {
                hasStargate = true;
                stargateCompleted = true;
                newShips++;
                if (!empire().divertColonyExcessToResearch())
                    empire().addReserve(stargateBC - cost);
                goToNextDesign();
                stargateBC = 0;
            }
            prevDesign = design;
            return;
        } 
        
        else {
            float shipRsvBC = min(prodBC, shipReserveBC);
            shipBC += newBC;
            shipBC += shipRsvBC;
            shipReserveBC -= shipRsvBC;
            if (buildLimit == 0) {
                while (shipBC >= cost) {
                    newShips++;
                    shipBC -= cost;
                }
            }
            else {
                while ((shipBC >= cost) && (buildLimit > 0)) {
                    newShips++;
                    buildLimit--;
                    shipBC -= cost;
                }
                if (buildLimit == 0) {
                    shipLimitReached = true;
                    if (!empire().divertColonyExcessToResearch())
                        empire().addReserve(shipBC);
                    shipBC = 0;
                }
            }
            if (newShips > 0) {
                ShipDesign shipDesign = (ShipDesign) design;
                shipDesign.addBuildCount(newShips);
                empire().shipLab().recordConstruction(shipDesign, newShips);
                empire().shipBuildingSystems().add(colony().starSystem());
                placeNewShipsInOrbit(shipDesign, newShips);
                if (empire().isPlayerControlled()) {
                    log(colony().name(), " has constructed: ", str(newShips), " ", design.name());
                    session().addShipsConstructed(shipDesign,  newShips);
                }
            }
        }
        prevDesign = design;
    }
    private void placeNewShipsInOrbit(ShipDesign d, int count) {
        Empire emp = colony().empire();
        StarSystem sys = colony().starSystem();
        int sysId = sys.id;
        int designId = d.id();
        
        // if we are rallying, note how many of which design we need to deploy later
        if ((emp.sv.hasRallyPoint(sysId)) && (emp.alliedWith(emp.sv.empId(sysId)))) {
            rallyCount = count;
            rallyDesignId = designId;
            rallyDestSysId = id(emp.sv.rallySystem(sysId));
        }

        galaxy().ships.buildShips(emp.id, sysId, designId, count);
    }
    public void capturedBy(Empire newCiv) {
        if (newCiv == empire())
            return;
        hasStargate = false;
        buildingStargate = false;
        stargateCompleted = false;
        stargateBC = 0;
        shipBC = 0;
        shipReserveBC = 0;
        newShips = 0;
        buildLimit = 0;
        shipLimitReached = false;
        rallyFleet = null;
        maxAllowedShipBCProd = -1;
    }
    public void goToPrevDesign() {
        design = empire().shipLab().prevDesignFrom(design, hasStargate);
        buildingStargate = design == empire().shipLab().stargateDesign();
    }
    public void goToNextDesign() {
        design = empire().shipLab().nextDesignFrom(design, hasStargate);
        buildingStargate = design == empire().shipLab().stargateDesign();
    }
    public void switchToDesign(Design d) {
        design = d;
        buildingStargate = d instanceof DesignStargate;
    }
    public boolean canCycleDesign()   { return design.scrapped() || (empire().shipLab().numDesigns() > 1) || canBuildStargate(); }
    public boolean canBuildStargate() { return tech().canBuildStargate() && !hasStargate; }
    public int upcomingShipCount() {
        if (buildingObsoleteDesign())
            return 0;
        if (colony().allocation(categoryType()) == 0)
            return 0;
        
        float tmpShipReserveBC = shipReserveBC;
        float tmpShipBC = shipBC;
        float tmpStargateBC = stargateBC;
        float accumBC = buildingStargate ? stargateBC : shipBC;
        // if we switched designs, send previous ship BC to shipyard reserve
        if (design != prevDesign) {
            if (prevDesign instanceof DesignStargate)
                tmpShipReserveBC += tmpStargateBC;
            else
                tmpShipReserveBC += tmpShipBC;
            accumBC = 0;
        }
        float prodBC = pct()* colony().totalProductionIncome() * planet().productionAdj();
        float rsvBC = pct() * colony().maxReserveIncome();
        float newBC = prodBC+rsvBC;
        float totalBC =max(newBC+accumBC, 0);
        float cost = design.cost();

        // add BC fromshipyard reserve if buildlign s(ship rsv is capped by prod)
        if (!buildingStargate) 
            totalBC = totalBC+min(tmpShipReserveBC,prodBC);

        if (totalBC < cost)
            return 0;
        
        if (buildingStargate)
            return 1;
        if (buildLimit == 0) 
            return (int) (totalBC / cost);

        return min(buildLimit, (int) (totalBC / cost));
    }
    public String buildLimitResult() {
        return text("MAIN_COLONY_SHIPYARD_LIMIT",buildLimit());
    }
    @Override
    public float excessSpending() {
        if (colony().allocation(categoryType()) == 0)
            return 0;
        
        float prodBC = pct()* colony().totalProductionIncome() * planet().productionAdj();
        float rsvBC = pct() * colony().maxReserveIncome();
        float totalBC = prodBC+rsvBC;   
        
        // if building ships with no build limit, then no possible overflow
        if (!buildingStargate && (buildLimit == 0))
            return 0;
        
        int numBuild = buildLimit;
        if (buildingStargate) {
            totalBC += stargateBC;   
            numBuild = 1;
        }
        else
            totalBC += shipBC;
        
        float buildCost = numBuild * design.cost();
        
        if (buildCost > totalBC)
            return 0;
        
        return totalBC - buildCost;
    }
    @Override
    public String upcomingResult() {
        if (colony().allocation(categoryType()) == 0)
            return noneText;
        
        float tmpShipReserveBC = shipReserveBC;
        float tmpShipBC = shipBC;
        float tmpStargateBC = stargateBC;
        float accumBC = buildingStargate ? stargateBC : shipBC;
        // if we switched designs, send previous ship BC to shipyard reserve
        if (design != prevDesign) {
            if (prevDesign instanceof DesignStargate)
                tmpShipReserveBC += tmpStargateBC;
            else
                tmpShipReserveBC += tmpShipBC;
            accumBC = 0;
        }
        float prodBC = pct()* colony().totalProductionIncome() * planet().productionAdj();
        float rsvBC = pct() * colony().maxReserveIncome();
        float newBC = prodBC+rsvBC;
        float totalBC = max(newBC+accumBC, 0);
        float cost = design.cost();

        // add BC from shipyard reserve if buildling ship (ship rsv is capped by prod)
        if (!buildingStargate) 
            totalBC = totalBC+min(tmpShipReserveBC,prodBC);

        if (totalBC == 0)
            return text(noneText);

        if (buildingObsoleteDesign())
            return overflowText();

        // returns how many years if we are not spending enough to finish even 1 
        if (totalBC < cost) {
            if (newBC == 0)
                return text(noneText);
            else {
                int turns = (int) Math.ceil((cost - accumBC) / newBC);
                if (turns == 1)
                    return text(yearText, 1);
                else if (turns > 99)
                    return text(yearsLongText, turns);
                else
                    return text(yearsText, turns);
            }
        }

        // if building stargate, anything after 1 is overflow
        if (buildingStargate)
            return overflowText();
        
        // if building ships with no limit, specify how many ships
        if (buildLimit == 0)  {
            int  ships = (int) (totalBC / cost);
            if (ships == 1)
                return text(yearText, "1");
            else
                return text(perYearText, ships);            
        }
        
        float totalCost = buildLimit * cost;
        
        // not spending enough to hit the build limit, specify how many ships
        if (totalBC <= totalCost) {
            int  ships = (int) (totalBC / cost);
            if (ships == 1)
                return text(yearText, "1");
            else
                return text(perYearText, ships);
        }
        
        // we are exceeding limit, so result is overflow
        return overflowText();
    }
    public float maxSpendingNeeded() {
        float totalCost = 0;
        if (buildingStargate)
            totalCost = max(0, design.cost() - stargateBC);
        else {
            float shipCost = design.cost() * desiredShips;
            if (design == prevDesign)
                shipCost -= shipBC;

            totalCost = max(0, shipCost);
        }

        // adjust cost for planetary production
        // assume any amount over current production comes from reserve (no adjustment)
        float totalBC = (colony().totalProductionIncome() * planet().productionAdj()) + colony().maxReserveIncome();
        if (totalCost > totalBC)
            totalCost += colony().totalProductionIncome() * (1 - planet().productionAdj());
        else
            totalCost *= colony().totalIncome() / totalBC;

        return totalCost;
    }
}
