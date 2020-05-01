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
package rotp.model.galaxy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.util.Base;

public class Ships implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    private final List<ShipFleet> allFleets = new ArrayList<>();
    private List<ShipFleet> allFleetsCopy() { return new ArrayList<>(allFleets); }
    
    public void buildRallyShips(int empId, int sysId, int designId, int count, int rallySysId) {
        // are we relocating new ships? If so, do so as long as dest is still allied with us
        ShipFleet existingFleet = rallyingFleet(empId, sysId, rallySysId);
        
        if (existingFleet == null) {
            StarSystem sys = galaxy().system(sysId);
            existingFleet = new ShipFleet(empId, sys);
            existingFleet.rallySysId(rallySysId);
            existingFleet.makeDeployed();
            allFleets.add(existingFleet);
        }       
        existingFleet.addShips(designId, count);   
    }
    public void buildShips(int empId, int sysId, int designId, int count) {
        // are we relocating new ships? If so, do so as long as dest is still allied with us
        ShipFleet existingFleet = orbitingFleet(empId, sysId);
        
        if (existingFleet == null) {
            StarSystem sys = galaxy().system(sysId);
            existingFleet = new ShipFleet(empId, sys);
            existingFleet.makeOrbiting();
            allFleets.add(existingFleet);
        }        
        existingFleet.addShips(designId, count);
    }
    public void deployShips(int empId, int sysId, int[] counts, int destSysId) {    
        // get orbiting fleet to pull ships from
        ShipFleet orbitingFleet = orbitingFleet(empId, sysId);
        if (orbitingFleet == null) {
            err("Attempting to deploy fleet from a system that has no orbiting fleet");
            return;
        }
        
        // adjust ship counts
        int[] actual = new int[counts.length];
        int totalDeployed = 0;
        int totalOrbiting = 0;
        for (int i=0;i<actual.length;i++) {
            actual[i] = min(counts[i], orbitingFleet.num(i));
            totalOrbiting += orbitingFleet.num(i);
            totalDeployed += actual[i];
        }
        if (totalDeployed == 0) {
            err("Unable to deploy.. actual ships deployed = 0");
            return;
        }

        // if entire orbiting fleet is being deployed just use it
        if (totalOrbiting == totalDeployed) {
            orbitingFleet.destSysId(destSysId);
            orbitingFleet.makeDeployed();
            return;
        }
            
        // else we create a new deployed fleet 
        // else we create a new deployed subfleet from the source 
        StarSystem sys = galaxy().system(sysId);
        StarSystem destSys = galaxy().system(destSysId);
        
        // calculate warp speed of new fleet and travel turns to the destination
        int minSpeed = 9;
        Empire sourceEmp = orbitingFleet.empire();
        for (int i=0; i<actual.length; i++) {
            if (counts[i] > 0) 
                minSpeed = min(minSpeed, sourceEmp.shipLab().design(i).warpSpeed());
        }   
        int turns = sourceEmp.travelTurns(sys, destSys, minSpeed);
        // find any existing deployed fleets to that dest with the same travel turns
        ShipFleet deployedFleet = deployedFleet(empId, sysId, destSysId, turns);    
        
        if (deployedFleet == null) {
            deployedFleet = new ShipFleet(empId, sys);
            deployedFleet.destSysId(destSysId);
            deployedFleet.makeDeployed();
            allFleets.add(deployedFleet);            
            galaxy().empire(empId).addVisibleShip(deployedFleet);
        }
        
        // transfer ships from orbiting to deplooyed fleet
        for (int i=0; i<actual.length; i++) {
            int startCount = orbitingFleet.num(i);
            deployedFleet.num(i, actual[i]);
            orbitingFleet.num(i, startCount-actual[i]);
        }        
        deployedFleet.setArrivalTime();
        // if source fleet is gone, remove it and subst session vars
        if (orbitingFleet.isEmpty()) {
            deleteFleet(orbitingFleet);
            session().replaceVarValue(orbitingFleet, deployedFleet);
        }
    }
    public ShipFleet deployFleet(ShipFleet sourceFleet, int destSysId) {
        if (sourceFleet.inTransit()) {
            if (sourceFleet.canSend())
                return redirectFleet(sourceFleet, destSysId);
            else
                return sourceFleet;
        }
        
        // returns the deployed fleet
        int sysId = id(sourceFleet.system());
        
        StarSystem destSys = galaxy().system(destSysId);
        int turns = sourceFleet.travelTurns(destSys);
        
        // get deployed fleet to add ships to
        ShipFleet deployedFleet = deployedFleet(sourceFleet.empId, sysId, destSysId, turns);   
        
        if (deployedFleet == sourceFleet) 
            return sourceFleet;
        
        // if no deployed fleet, use this one
        if (deployedFleet == null) {
            sourceFleet.destSysId(destSysId);
            sourceFleet.makeDeployed();
            sourceFleet.setArrivalTime();
            return sourceFleet;
        }   
        
        // transfer ships from source to deployed fleet
        for (int i=0; i<sourceFleet.num.length; i++) {
            int a = sourceFleet.num(i);
            int b = deployedFleet.num(i);
            deployedFleet.num(i, a+b);
            sourceFleet.num(i, 0);
        }         
        // recalc arrival time (added ships may change this)
        deployedFleet.setArrivalTime();
        
        // if source fleet is gone, remove it and subst session vars
        if (sourceFleet.isEmpty()) {
            deleteFleet(sourceFleet);
            session().replaceVarValue(sourceFleet, deployedFleet);
        }
        return deployedFleet;
    }
    public boolean deploySubfleet(ShipFleet sourceFleet, List<ShipDesign> designs, int destSysId) {  
        int[] counts = new int[sourceFleet.num.length];
        for (ShipDesign d: designs) 
            counts[d.id()] = sourceFleet.num(d.id());
        
        return deploySubfleet(sourceFleet, counts, destSysId);        
    }
    public boolean deploySubfleet(ShipFleet sourceFleet, int[] counts, int destSysId) {   
        // returns true if a new subfleet was created
         // adjust ship counts
        int[] actual = new int[counts.length];
        int totalDeployed = 0;
        int totalOrbiting = 0;
        for (int i=0;i<actual.length;i++) {
            actual[i] = min(counts[i], sourceFleet.num(i));
            totalOrbiting += sourceFleet.num(i);
            totalDeployed += actual[i];
        }
        if (totalDeployed == 0) {
            err("Unable to deploy.. actualships  deployed = 0");
            return false;
        }
        if (totalDeployed == sourceFleet.numShips()) {
            deployFleet(sourceFleet, destSysId);
            return true;
        }
        
        // cannot redirect a partial fleet, even with HC
        if (sourceFleet.inTransit()) 
            return false;
              
        // else we create a new deployed subfleet from the source 
        StarSystem sys = sourceFleet.system();
        StarSystem destSys = galaxy().system(destSysId);
        
        // calculate warp speed of new fleet and travel turns to the destination
        int minSpeed = 9;
        Empire sourceEmp = sourceFleet.empire();
        for (int i=0; i<actual.length; i++) {
            if (counts[i] > 0) 
                minSpeed = min(minSpeed, sourceEmp.shipLab().design(i).warpSpeed());
        }   
        int turns = sourceEmp.travelTurns(sys, destSys, minSpeed);
        
        // find any existing deployed fleets to that dest with the same travel turns
        int empId = sourceFleet.empId;
        ShipFleet deployedFleet = deployedFleet(empId, sys.id, destSysId, turns);    
        
        if (deployedFleet == null) {
            // if entire source fleet is being deployed just use it
            if (totalOrbiting == totalDeployed) {
                sourceFleet.destSysId(destSysId);
                sourceFleet.makeDeployed();
                sourceFleet.setArrivalTime();
                return false;
            }           
            deployedFleet = new ShipFleet(empId, sys);
            deployedFleet.destSysId(destSysId);
            deployedFleet.makeDeployed();
            allFleets.add(deployedFleet); 
            galaxy().empire(empId).addVisibleShip(deployedFleet);
        }
        
        // transfer ships from orbiting to deplooyed fleet
        for (int i=0; i<actual.length; i++) {
            int srcCount = sourceFleet.num(i);
            int destCount = deployedFleet.num(i);
            deployedFleet.num(i, destCount+actual[i]);
            sourceFleet.num(i, srcCount-actual[i]);
        }        
        deployedFleet.setArrivalTime();
        
        // if source fleet is gone, remove it and subst session vars
        if (sourceFleet.isEmpty()) {
            deleteFleet(sourceFleet);
            session().replaceVarValue(sourceFleet, deployedFleet);
        }
        return true;
    }
    public ShipFleet redirectFleet(ShipFleet sourceFleet, int destSysId) {   
        // else we creat a new deployed subfleet from the source 
        float currX = sourceFleet.x();
        float currY = sourceFleet.y();
      
        sourceFleet.destSysId(destSysId);
        sourceFleet.launch(currX, currY);
        
        return sourceFleet;
    }
    public ShipFleet retreatFleet(ShipFleet sourceFleet, int destSysId) {
        ShipFleet retreatingFleet = retreatingFleet(sourceFleet.empId, id(sourceFleet.system()), destSysId);

        if (retreatingFleet == null) {
            sourceFleet.destSysId(destSysId);
            sourceFleet.makeDeployed();
            sourceFleet.retreating(true);
            sourceFleet.setArrivalTime();
            return sourceFleet;  
        }
        
        // transfer ships from orbiting to retreating fleet
        for (int i=0; i<sourceFleet.num.length; i++) {
            int a = sourceFleet.num(i);
            int b = retreatingFleet.num(i);
            retreatingFleet.num(i, a+b);
            sourceFleet.num(i, 0);
        }        
        retreatingFleet.setArrivalTime();
        deleteFleet(sourceFleet);
        session().replaceVarValue(sourceFleet, retreatingFleet);
            
        return retreatingFleet;
    }
    public ShipFleet retreatSubfleet(ShipFleet sourceFleet, int designId, int destSysId) {
        int retreatCount = sourceFleet.num(designId);
        if (retreatCount == 0) 
            return null;
        
        int allCount = sourceFleet.numShips();
        
        StarSystem sys = sourceFleet.system();
        int empId = sourceFleet.empId;
        ShipFleet retreatingFleet = retreatingFleet(sourceFleet.empId, sys.id, destSysId);
        
        if (sys.id == destSysId) {
            err("Trying to retreat to same system");
            return null;
        }

        if (retreatingFleet == null) {
            // if entire source fleet is retreatuing just use it
            if (retreatCount == allCount) {
                sourceFleet.destSysId(destSysId);
                sourceFleet.makeDeployed();
                sourceFleet.retreating(true);
                sourceFleet.setArrivalTime();
                return sourceFleet;
            }                
            retreatingFleet = new ShipFleet(empId, sys);
            retreatingFleet.destSysId(destSysId);
            retreatingFleet.makeDeployed();
            retreatingFleet.retreating(true);
            allFleets.add(retreatingFleet); 
            galaxy().empire(empId).addVisibleShip(retreatingFleet);
        }
        
        // transfer ships from orbiting to deplooyed fleet
        int a = sourceFleet.num(designId);
        int b = retreatingFleet.num(designId);
        retreatingFleet.num(designId, a+b);
        sourceFleet.num(designId, 0);
        retreatingFleet.setArrivalTime();
        
        // if source fleet is gone, remove it and subst session vars
        if (sourceFleet.isEmpty()) {
            deleteFleet(sourceFleet);
            session().replaceVarValue(sourceFleet, retreatingFleet);
        }

        return retreatingFleet;
    }
    public boolean undeployFleet(ShipFleet sourceFleet) {
        if (!sourceFleet.deployed() && !sourceFleet.isRalliedThisTurn())
            return false;
        // returns true if the source fleet was scrapped
        StarSystem sys = sourceFleet.system();
        int empId = sourceFleet.empId;
        ShipFleet orbitingFleet = orbitingFleet(empId, sys.id);
        if (orbitingFleet == null) {
            sourceFleet.makeOrbiting();
            sourceFleet.rallySysId(StarSystem.NULL_ID);
            sourceFleet.destSysId(StarSystem.NULL_ID);
            return false;
        }        
        
        for (int i=0;i<sourceFleet.num.length;i++) {
            int a = sourceFleet.num(i);
            int b = orbitingFleet.num(i);
            orbitingFleet.num(i, a+b);
            sourceFleet.num(i,0);
        }
        deleteFleet(sourceFleet);
        session().replaceVarValue(sourceFleet, orbitingFleet);
        
        return true;
    }
    public void undeployFleet(ShipFleet sourceFleet, List<ShipDesign> designs) {
        if (!sourceFleet.deployed())
            return;
        // returns true if the source fleet was scrapped
        StarSystem sys = sourceFleet.system();
        int empId = sourceFleet.empId;
        
        int count = 0;
        for (ShipDesign d: designs) 
            count += sourceFleet.num(d.id());
            
        if (count == 0) {
            err("Undeploying subfleet of zero ships");
            return;
        }    
        if (count == sourceFleet.numShips()) {
            undeployFleet(sourceFleet); // undeploy entire fleet
            return;
        }
                
        ShipFleet orbitingFleet = orbitingFleet(empId, sys.id);     
        // if none exists, creating orbiting fleet to hold undeploying ships
        if (orbitingFleet == null) {
            orbitingFleet = new ShipFleet(empId, sys);
            orbitingFleet.makeOrbiting();
            allFleets.add(orbitingFleet); 
            galaxy().empire(empId).addVisibleShip(orbitingFleet);
        }        
        
        // move undeploying ships into orbiting fleet
        for (ShipDesign d: designs) {
            int i = d.id();
            int a = orbitingFleet.num(i);
            int b = sourceFleet.num(i);
            orbitingFleet.num(i, a+b);
            sourceFleet.num(i, 0);
        }
    }
    public void deleteFleet(ShipFleet fl) {
        fl.reset();
        allFleets.remove(fl);
        
        Galaxy g = galaxy();
        for (Empire emp: g.empires())
            emp.visibleShips().remove(fl);
    }
    public void disembarkFleets() {
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if (fl.isDeployed() && !fl.isRallied()) {
                fl.launch();
            }
        }
    }
    public void disembarkFleets(int sysId) {
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if (fl.isDeployed() && (id(fl.system()) == sysId) && !fl.isRallied()) {
                fl.launch();
            }
        }
    }
    public void reloadBombs() {
        List<ShipFleet> fleetsAll = allFleetsCopy();
        for (ShipFleet fl: fleetsAll) 
            fl.reloadBombs();
    }
    public void disembarkRalliedFleets() {
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if (fl.isDeployed() && fl.isRallied()) {
                fl.destSysId(fl.rallySysId());
                fl.launch();
            }
        }
    }
    public boolean arriveFleet(ShipFleet fleet) {
        StarSystem sys = galaxy().system(fleet.destSysId());
        
        // if an orbiting fleet already exists, merge with it
        ShipFleet orbitingFleet = orbitingFleet(fleet.empId, sys.id);       
        if (orbitingFleet == null) {
            fleet.arrive(sys);
            orbitingFleet = fleet;
        }
        else {
            for (int i=0;i<fleet.num.length;i++) {
                int a = orbitingFleet.num(i);
                int b = fleet.num(i);
                orbitingFleet.num(i, a+b);
                fleet.num(i,0);
            }
            allFleets.remove(fleet);
        }

        // update ship views
        List<ShipFleet> fleets = orbitingFleets(sys.id);
        if (fleets.size() > 1) {
            for (ShipFleet fl: fleets) {
                if (fl != fleet) {
                    fl.empire().encounterFleet(orbitingFleet);
                    fleet.empire().encounterFleet(fl);
                }
            }
        }
        if (sys.isColonized())
            sys.empire().scanFleet(orbitingFleet);

        if (!sys.orbitingShipsInConflict())
            orbitingFleet.empire().sv.refreshFullScan(sys.id);  
        return false;
    }
    public List<ShipFleet> visibleFleets(int empId) {
        List<ShipFleet> fleets = new ArrayList<>();
        return fleets;
    }
    public ShipFleet anyFleetAtSystem(int empId, int sysId) {
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if ((fl.empId == empId) && (fl.sysId() == sysId))
                return fl;
        }
        return null;
    }
    public ShipFleet orbitingFleet(int empId, int sysId) {
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if ((fl.empId == empId) && (fl.sysId() == sysId) 
            && fl.isOrbiting() && !fl.isRallied())
                return fl;
        }
        return null;
    }
    public ShipFleet rallyingFleet(int empId, int sysId, int rallySysId) {
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if ((fl.empId == empId) && (fl.sysId() == sysId) 
            && fl.isOrbiting() && (fl.rallySysId() == rallySysId))
                return fl;
        }
        return null;
    }
    public ShipFleet retreatingFleet(int empId, int sysId, int destSysId) {
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if ((fl.empId == empId) && (fl.sysId() == sysId) 
            && fl.isDeployed() && fl.retreating() && (fl.destSysId() == destSysId))
                return fl;
        }
        return null;
    }
    public ShipFleet deployedFleet(int empId, int sysId, int destSysId, int turns) {
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if ((fl.empId == empId) && (fl.sysId() == sysId) 
            && fl.isDeployed() && (fl.destSysId() == destSysId)) {
                StarSystem destSys = galaxy().system(destSysId);
                if (fl.travelTurns(destSys) == turns)
                    return fl;
            }
        }
        return null;
    }
    public List<ShipFleet> orbitingFleets(int sysId) {
        List<ShipFleet> fleets = new ArrayList<>();
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            // NPE was found on a map repaint during next turn. 
            // unsure how this is possible since allFleets var is private with no accessor
            // all allFleets.add() calls are in this class and only add new ShipFleet().
            if (fl != null) {
                if ((id(fl.system()) == sysId) && fl.isOrbiting())
                    fleets.add(fl);
            }
        }
        return fleets;
    }
    public List<ShipFleet> deployedFleets(int sysId) {
        List<ShipFleet> fleets = new ArrayList<>();
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if ((id(fl.system()) == sysId) && fl.isDeployed())
                fleets.add(fl);
        }
        return fleets;
    }
    public List<ShipFleet> inTransitFleets() {
        List<ShipFleet> fleets = new ArrayList<>();
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if (fl.isInTransit())
                fleets.add(fl);
        }
        return fleets;
    }
    public List<ShipFleet> inTransitNotRetreatingFleets(int empId, int designId) {
        // this specific piece of code is used to find any colony ships stiil 
        // en route to their dest so the AI doesn't prematurely scrap them
        List<ShipFleet> fleets = new ArrayList<>();
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if ((fl.empId == empId) && fl.isInTransit() 
            && !fl.retreating() && (fl.num[designId] > 0))
                    fleets.add(fl);
        }
        return fleets;
    }
    public List<ShipFleet> notInTransitFleets(int empireId) {
        List<ShipFleet> fleets = new ArrayList<>();
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if ((fl.empId == empireId) && !fl.isInTransit())
                fleets.add(fl);
        }
        return fleets;
    }
    public List<ShipFleet> allFleets(int empireId) {
        List<ShipFleet> fleets = new ArrayList<>();
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if (fl.empId == empireId)
                fleets.add(fl);
        }
        return fleets;
    }
    public int scrapDesign(int empireId, int designId) {
        int scrapCount = 0;
        List<ShipFleet> emptyFleets = new ArrayList<>();
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if (fl.empId == empireId) {
                int count = fl.num(designId);
                if (count > 0) {
                    scrapCount += count;
                    fl.num(designId, 0);
                    if (fl.isEmpty())
                        emptyFleets.add(fl);
                }
            }
        }
        
        for (ShipFleet fl: emptyFleets) 
            this.deleteFleet(fl);
        
        return scrapCount;
    }
    public int[] shipDesignCounts(int empireId) {
        int[] count = new int[ShipDesignLab.MAX_DESIGNS];
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if (fl.empId == empireId) {
                for (int i=0;i<count.length;i++)
                    count[i] += fl.num(i);
            }
        }
        return count;
    }
    public int shipDesignCount(int empireId, int designId) {
        int count = 0;
        List<ShipFleet> fleetsAll = allFleetsCopy();
        
        for (ShipFleet fl: fleetsAll) {
            if (fl.empId == empireId) 
                count += fl.num(designId);
        }       
        return count;        
    }
    public int hullSizeCount(int empireId, int hullSize) {
        int[] count = shipDesignCounts(empireId);
        
        int hullCount = 0;
        ShipDesignLab lab = galaxy().empire(empireId).shipLab();
        for (int i=0;i<count.length;i++) {
            if (lab.design(i).size() == hullSize)
                hullCount += count[i];
        }
        return hullCount;        
    }
}
