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
    private transient ShipFleet rallyFleet;
    private transient float maxAllowedShipBCProd;

    // used by the AI when determining what to build
    private float queuedBC = 0;
    private int desiredShips = 0;

    public boolean hasStargate()              { return hasStargate; }
    public boolean stargateCompleted()        { return stargateCompleted; }
    public Design design()                    { return design; }
    public void design(Design d)              { design = d; }
    public void addQueuedBC(float d)          { queuedBC += d; }
    public int desiredShips()                 { return desiredShips; }
    public void addDesiredShips(int i)        { desiredShips += i; }
    public ShipFleet rallyFleet()             { return rallyFleet; }

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
    public void assessTurn()             { }
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

        if (buildingObsoleteDesign())
            empire().addReserve(newBC);
        else if (buildingStargate) {
            stargateBC += newBC;
            stargateCompleted = (maxSpendingNeeded() <= 0);
            if (stargateBC >= cost) {
                hasStargate = true;
                newShips++;
                empire().addReserve(stargateBC - cost);
                goToNextDesign();
                prevDesign = design;
                return;
            }
        }
        else {
            float shipRsvBC = min(prodBC, shipReserveBC);
            shipBC += newBC;
            shipBC += shipRsvBC;
            shipReserveBC -= shipRsvBC;
            stargateCompleted = false;
            while (shipBC >= cost) {
                newShips++;
                shipBC -= cost;
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
        
        if ((emp.sv.hasRallyPoint(sysId)) && (emp.alliedWith(emp.sv.empId(sysId)))) 
            galaxy().ships.buildRallyShips(emp.id, sysId, designId, count, id(emp.sv.rallySystem(sysId)));
        else
            galaxy().ships.buildShips(emp.id, sysId, designId, count);
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

        if (totalBC >= cost)
            return buildingStargate ? 1 : (int) (totalBC / cost);
        else
            return 0;
    }
    public String shipCompletionResult() {
        return text("MAIN_COLONY_SHIPYARD_COMPLETED",upcomingResult());
    }
    @Override
    public String upcomingResult() {
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
            return text(reserveText);

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
        else {
            if (buildingStargate)
                return text(reserveText);
            else {
                int  ships = (int) (totalBC / cost);
                if (ships == 1)
                    return text(yearText, "1");
                else
                    return text(perYearText, ships);
            }
        }
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
