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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import rotp.model.Sprite;
import rotp.model.ai.FleetOrders;
import rotp.model.combat.CombatStackColony;
import rotp.model.combat.CombatStackShip;
import rotp.model.combat.ShipCombatManager;
import rotp.model.empires.Empire;
import rotp.model.empires.ShipView;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.tech.TechEngineWarp;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.map.IMapHandler;
import rotp.ui.sprites.FlightPathSprite;
import rotp.util.Base;

public class ShipFleet implements Base, Sprite, Ship, Serializable {
    private static final long serialVersionUID = 1L;
    enum Status { ORBITING, DEPLOYED, IN_TRANSIT };
    public final int empId;
    private int sysId;
    private int destSysId = StarSystem.NULL_ID;
    private int rallySysId = StarSystem.NULL_ID;
    public final int[] num = new int[ShipDesignLab.MAX_DESIGNS];
    private Status status = Status.ORBITING;

    private boolean retreating = false;
    private float fromX, fromY, destX, destY;
    private float launchTime = NOT_LAUNCHED;
    private float arrivalTime = Float.MAX_VALUE;

    private transient FleetOrders orders;
    private transient FlightPathSprite pathSprite;
    private transient Rectangle selectBox;
    private transient boolean hovering;
    private transient int[] bombardCount = new int[ShipDesignLab.MAX_DESIGNS];
    private transient boolean displayed = false;

    public int sysId()                  { return sysId; }
    public void sysId(int i)            { sysId = i; }
    @Override
    public int destSysId()              { return destSysId; }
    public void destSysId(int i)        { 
        destSysId = i; 
        if (hasDestination()) {
            StarSystem s = galaxy().system(destSysId);
            destX = s.x();
            destY = s.y();
        }
    }
    public void destination(int i, float x, float y) {
        destSysId = i;
        destX = x;
        destY = y;
    }
    public int rallySysId()             { return rallySysId; }
    public void rallySysId(int i)       { rallySysId = i; }
    public Empire empire()              { return galaxy().empire(empId); }
    @Override
    public boolean retreating()         { return retreating; }
    public void retreating(boolean b)   { retreating = b; }
    public StarSystem system()          { return galaxy().system(sysId); }
    public void system(StarSystem s)    { sysId = id(s); }
    public StarSystem destination()     { return galaxy().system(destSysId);  }
    public float destY()                { return destY; }
    public void makeOrbiting()          { status = Status.ORBITING; }
    public void makeDeployed()          { status = Status.DEPLOYED; }
    public void makeInTransit()         { status = Status.IN_TRANSIT; }
    public boolean isOrbiting()         { return status == Status.ORBITING; }
    public boolean isDeployed()         { return status == Status.DEPLOYED; }
    public boolean isInTransit()        { return status == Status.IN_TRANSIT; }
    public void setXY(float x, float y) { fromX = x; fromY = y; }
    public void setXY(StarSystem sys)   { fromX = sys.x(); fromY = sys.y(); }
    @Override
    public boolean isRallied()          { return rallySysId != StarSystem.NULL_ID; }
    public boolean isRalliedThisTurn()  {
        return isRallied() && (launchTime == galaxy().currentTime()); 
    }
    public boolean isRetreatingThisTurn() {
        return retreating() && (launchTime == galaxy().currentTime()); 
    }
    public boolean hasDestination()     { return destSysId != StarSystem.NULL_ID; }
    
    public Rectangle selectBox() {
        if (selectBox == null)
            selectBox = new Rectangle();
        return selectBox;
    }

    @Override
    public int empId()                  { return empId; }
    @Override
    public float arrivalTime()         { return arrivalTime; }
    @Override
    public boolean visibleTo(int emp) {
        if (emp == empId)
            return true;

        for (int i=0;i<num.length;i++) {
            ShipDesign d = design(i);
            if ((num[i] > 0) && (d != null) && !d.allowsCloaking())
                return true;
        }
        return false;
    }
    @Override
    public boolean hovering()                   { return hovering; }
    @Override
    public void hovering(boolean b)             { hovering = b; }
    @Override
    public boolean displayed()                  { return displayed; }
    public ShipFleet(int emp, StarSystem s) {
        empId = emp;
        sysId = s.id;
        fromX = s.x();
        fromY = s.y();
        reloadBombs();
    }
    final public void reloadBombs() {
        if (bombardCount == null)
            bombardCount = new int[ShipDesignLab.MAX_DESIGNS];
        
        for (int i=0;i<bombardCount.length;i++)
            bombardCount[i]=0;
    }
    public int bombardCount(int i)    { return bombardCount[i]; }
    public void bombarded(int i)      { bombardCount[i]++; }
    public void clear() {
        for (int i=0;i<num.length;i++)
            num[i]=0;
    }
    
    @Override
    public boolean persistOnClick()             { return empire() == player(); }
    public static ShipFleet copy(ShipFleet fl) {
        // returns a new ship fleet with identical stacks & count
        ShipFleet temp = new ShipFleet(fl.empId, fl);
        
        System.arraycopy(fl.num, 0, temp.num, 0, fl.num.length);
        return temp;
    }
    public static ShipFleet copy(ShipFleet fl, List<ShipDesign> designs) {
        // returns a new ship fleet with only stacks & count matching designs
        ShipFleet temp = new ShipFleet(fl.empId, fl.system());
        for (ShipDesign desn: designs)
            temp.num[desn.id()] = fl.num[desn.id()];
        return temp;
    }
     private ShipFleet(int emp, ShipFleet f) {
        empId = emp;
        sysId = f.sysId;
        fromX = f.x();
        fromY = f.y();
        destSysId = f.destSysId;
        destX = f.destX;
        destY = f.destY;
        status = f.status;
        launchTime = f.launchTime;
        
        reloadBombs();
    }
    public ShipFleet(ShipFleet fl) {
        empId = fl.empId;
        sysId = fl.sysId;
        fromX = fl.fromX;
        fromY = fl.fromY;
        destSysId = fl.destSysId;
        destX = fl.destX;
        destY = fl.destY;
    }
    @Override
    public FlightPathSprite pathSprite() {
        int destId = hasDestination() ? destSysId : rallySysId;
        if (destId == StarSystem.NULL_ID)
            return null;
        StarSystem dest = galaxy().system(destId);
        if (pathSprite == null)
            pathSprite = new FlightPathSprite(this, dest);
        if (pathSprite.destination() != dest)
            pathSprite.destination(dest);
        return pathSprite;
    }
    private ShipDesign design(int i) { return empire().shipLab().design(i); }
    public ShipFleet matchingFleetWithin(List<ShipFleet> fleets, StarSystem dest) {
        for (ShipFleet fl: fleets) {
            if ((fl.empId == empId)
            && (fl.destSysId == destSysId)
            && (fl.slowestStackSpeed() == slowestStackSpeed())
            && (fl.retreating() == retreating())
            && (fl.rallySysId == rallySysId)) {
                return fl;
            }
        }
        return null;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(Integer.toHexString(hashCode()));
        sb.append('(');
        for (int i=0;i<num.length;i++) {
            ShipDesign d = design(i);
            if ((num[i] > 0) && (d != null)){
                sb.append(d.name());
                sb.append('-');
                sb.append(num[i]);
                sb.append('.');
            }
        }
        sb.append(')');
        return sb.toString();
    }
    @Override
    public float x() { return isInTransit() ? transitX() : fromX; }
    @Override
    public float y() { return isInTransit() ? transitY() : fromY; }

    private float transitX() {
        float p = travelPct();
        return fromX + (p*(destX - fromX));
    }
    private float transitY() {
        float p = travelPct();
        return fromY + (p*(destY - fromY));
    }
    private float travelPct() {
        float currTime = galaxy().currentTime();
        if ((launchTime == NOT_LAUNCHED) || (launchTime == currTime))
            return 0;
        else 
            return (currTime-launchTime) / (arrivalTime-launchTime);
    }
    public void launch() {
        StarSystem sys = system();
        launch(sys.x(), sys.y());
    }
    public void launch(float x, float y)  {
        fromX = x;
        fromY = y;
        launchTime = galaxy().currentTime();
        setArrivalTime();
        makeInTransit();
    }
    public void arrive(StarSystem sys) {
        sysId = sys.id;
        destSysId = StarSystem.NULL_ID;
        rallySysId = StarSystem.NULL_ID;
        fromX = sys.x();
        fromY = sys.y();
        retreating = false;
        launchTime = NOT_LAUNCHED;
        makeOrbiting();
        empire().sv.view(sys.id).refreshSystemEntryScan();
    }
    public boolean inOrbit()        { return isOrbiting(); }
    @Override
    public boolean deployed()       { return isDeployed(); }
    public boolean launched()       { return launchTime > NOT_LAUNCHED; }
    public boolean canUndeploy()    { 
        if (isRalliedThisTurn())
            return true;
        return isDeployed() && !retreating(); 
    }
    @Override
    public boolean inTransit()      { return isInTransit(); }
    public boolean isActive()       { return hasShips(); }

    public int num(int i)             { return num[i]; }
    public void num(int i, int count) { num[i] = count; }
    public void reset() {
        for (int i=0;i<num.length;i++)
            num[i] = 0;
    }
    public int visibleNum(int emp, int i) {
        ShipDesign d = design(i);
        if ((emp == empId)
        || ((d != null) && !d.allowsCloaking()))
            return num[i];
        else
            return 0;
    }
    public boolean visibleTo(Empire emp) {
        if (emp.id == empId)
            return true;

        for (int i=0;i<num.length;i++) {
            ShipDesign d = design(i);
            if ((num[i] > 0) && (d != null) && !d.allowsCloaking())
                return true;
        }
        return false;
    }
    public ShipDesign design(int emp, int num) {
        int[] visible = visibleShips(emp);
        int cnt = num;
        for (int i=0;i<visible.length;i++) {
            if (visible[i] > 0) {
                if (cnt == 0)
                    return design(i);
                else
                    cnt--;
            }
        }
        return null;
    }
    public ShipDesign visibleDesign(int emp, int num) {
        int[] visible = visibleShips(emp);
        int cnt = num;
        for (int i=0;i<visible.length;i++) {
            if (visible[i] > 0) {
                if (cnt == 0)
                    return design(i);
                else
                    cnt--;
            }
        }
        return null;
    }
    public int[] visibleShips(int emp) {
        if (empId == emp)
            return num;

        int[] visible = new int[num.length];
        for (int i=0;i<num.length;i++) {
            ShipDesign d = design(i);
            if ((num[i] > 0) && (d != null) && !d.allowsCloaking())
                visible[i] = num[i];
            else
                visible[i] = 0;
        }
        return visible;
    }
    public float visibleFirepower(int emp, int shieldLevel) {
        // this calculates the visible firepower threat against SystemView sv
        // it only checks visible stacks for sv's owner
        // for unscanned ShipDesigns it asks the sv's owner to estimate threat based on size
        //  (this allows the owner to check any spied tech trees for worst possible weapons)

        float firepower = 0;
        int[] visible = visibleShips(emp);
        for (int i=0; i<visible.length; i++) {
            int cnt = visible[i];
            if (cnt > 0) {
                ShipDesign design = design(i);
                if (design != null) {
                    Empire empire = galaxy().empire(emp);
                    ShipView shipView = empire.shipViewFor(design);
                    if (shipView == null)
                        firepower += (cnt * empire.estimatedShipFirepower(empire(), design.size(), shieldLevel));
                    else
                        firepower += (cnt * shipView.visibleFirepower(shieldLevel));
                }
            }
        }
        return firepower;
    }
    public boolean aggressiveWith(ShipFleet fl, StarSystem sys) {
        // only possibly aggressive if one of the fleets is armed
        if (isArmed() || fl.isArmed())
            return empire().aggressiveWith(fl.empire(), sys);
        else
            return false;
    }
    public void checkColonize() {
        if ((system() != null) && inOrbit())
            empire().ai().checkColonize(system(), this);
    }
    public void colonizeSystem(StarSystem sys) {
        ShipDesign d = empire().shipDesignerAI().bestDesignToColonize(this, sys);
        if (d != null)
            colonizeSystem(sys, d);
    }
    public void colonizeSystem(StarSystem sys, ShipDesign d) {
        String sName = empire().sv.name(sys.id);
        log("Fleet: ", toString(), " colonizing system: ", sName, " with design:", d.name());
        empire().colonize(sName, sys);
        empire().sv.refreshFullScan(sys.id);

        // if fleet will be disbanded, ensure UIs are no longer selecting the fleet
        if (numShips() == 1)
            session().replaceVarValue(this, sys);

        removeShips(d.id(), 1, true);
        empire().shipLab().recordUse(d, 1);
    }
    public boolean canAttackPlanets() {
        for (int i=0;i<num.length;i++) {
            ShipDesign d = design(i);
            if ((num[i]>0) && (d != null) && d.canAttackPlanets())
                return true;
        }
        return false;
    }
    public boolean isArmed() {
        for (int i=0;i<num.length;i++) {
            if (num[i] > 0) {
                ShipDesign d = design(i);
                if ((d != null) && d.isArmed())
                    return true;
            }
        }
        return false;
    }
    public boolean isArmedForShipCombat() {
        for (int i=0;i<num.length;i++) {
            if (num[i] > 0) {
                ShipDesign d = design(i);
                if ((d != null) && d.isArmedForShipCombat())
                    return true;
            }
        }
        return false;
    }
    public boolean isArmed(StarSystem sys) {
        for (int i=0;i<num.length;i++) {
            ShipDesign d = design(i);
            if ((num[i]>0) && (d != null)) {
                for (int j=0;j<ShipDesign.maxWeapons();j++) {
                    if (!d.weapon(j).isNone()) {
                        if (d.weapon(j).canAttackShips())
                            return true;
                        if (sys.isColonized() && !empire().alliedWith(sys.empire().id) & d.weapon(j).canAttackPlanets())
                            return true;
                    }
                }
                for (int j=0;j<ShipDesign.maxSpecials();j++) {
                    if (d.special(j).canAttackShips()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    @Override
    public boolean isPotentiallyArmed(Empire e) {
        if (empire() == e)
            return isArmed();
        // only return as definitely unarmed if every
        // ship design in this fleet has been scanned
        // by Empire e and is known to be unarmed
        for (int i=0;i<num.length;i++) {
            if (num[i] > 0) {
                ShipDesign des = design(i);
                ShipView sv = e.shipViewFor(des);
                if ((sv == null) || sv.isPotentiallyArmed())
                    return true;
            }
        }
        return false;
    }
    public boolean hasShips()  {
        for (int i=0;i<num.length;i++) {
            if (num[i]>0)
                return true;
        }
        return false;
    }
    public boolean hasShip(ShipDesign d)  {
            return (d == null) || !d.active() ? false : num[d.id()] > 0;
    }
    public void setArrivalTime() {
        arrivalTime = galaxy().currentTime();
        if (hasDestination())
            arrivalTime += travelTime(destination());
        if (arrivalTime <= galaxy().currentTime()) 
            log("Error: ship arrivalTime <= currentTime");
    }
    public FleetOrders newOrders() {
        if (orders == null)
            orders = new FleetOrders(this);
        orders.reset();
        return orders;
    }
    public FleetOrders orders() {
        if (orders == null)
            orders = newOrders();
        return orders;
    }
    public boolean canBeAdjustedBy(Empire c) {
        return (c == empire()) && canAdjust();
    }
    public boolean canBeSentBy(Empire c) {
        return (c == empire()) && canSend();
    }
    public boolean canAdjust() {
        return hasShips() && !inTransit();
    }
    public boolean canSend() {
        return hasShips() && (!inTransit() || isRalliedThisTurn() || inOrbit() || empire().tech().hyperspaceCommunications());
    }
    @Override
    public boolean canSendTo(int id) {
        if (id == StarSystem.NULL_ID)
            return false;

        // retreating fleets can only go to different systems, colonized by friendly empire
        if (retreating)  {
            StarSystem sys = galaxy().system(id);
            return isRetreatingThisTurn() && (id != sysId) && sys.isColonized() && sys.empire().alliedWith(empId());
        }
        if (!canSend())
            return false;

        //cannot send if already orbiting the sv.system
        if (!inTransit() && (sysId == id))
            return false;
        if (!empire().sv.withinRange(id, range()))
            return false;
        
        return true;
    }
    public boolean canSend(Empire c) {
        return canSend() && (empire() == c);
    }
    public boolean canMassDeployTo(StarSystem sys) {
        if (sys == null)
            return false;
        //allow if already orbiting the sv.system
        if (inOrbit() && !inTransit() && (system() == sys))
            return true;
        // retreating fleets can only go to different systems, colonized by friendly empire
        if (retreating)
            return (sys != system()) && sys.isColonized() && sys.empire().alliedWith(empId());
        return true;
    }
    @Override
    public float hullPoints() {
        float pts = 0;
        for (int i=0;i<num.length;i++) {
            if (num[i] > 0) {
                ShipDesign d = design(i);
                if (d != null)
                    pts += (num[i] * d.hullPoints());
            }
        }
        return pts;
    }
    public float range() {
        if (isEmpty())
            return 0;

        for (int i=0;i<num.length;i++) {
            if (num[i]>0) {
                ShipDesign des = design(i);
                if (!des.isExtendedRange())
                    return empire().tech().shipRange();
            }
        }
        return empire().tech().scoutRange();
    }
    private float slowestStackSpeed() {
        float maxSpeed = TechEngineWarp.MAX_SPEED;
        for (int i=0;i<num.length;i++) {
            if (num[i]>0) {
                ShipDesign des = design(i);
                if (des != null)
                    maxSpeed = min(maxSpeed, des.engine().warp());
            }
        }
        return maxSpeed;
    }
    public float firepower(float shield) {
        float dmg = 0;
        for (int i=0;i<num.length;i++) {
            if (num[i]>0) {
                ShipDesign des = design(i);
                if (des != null)
                    dmg += (num[i] * des.firepower(shield));
            }
        }
        return dmg;
    }
    public boolean canReach(StarSystem dest) {
        return empire().sv.withinRange(dest.id, range());
    }
    public float travelTime(StarSystem to) {
        return travelTime(to, slowestStackSpeed());
    }
    public float travelTime(StarSystem dest, float speed) {
        if (inOrbit() || deployed()) {
            if (system().hasStargate(empire()) && dest.hasStargate(empire()))
                return 1;
        }
        return travelTime(this,dest,speed);
    }
    public int travelTurns(StarSystem dest) { 
        return (int)Math.ceil(travelTime(dest));  
    }
    private int travelTurns(StarSystem dest, float speed) { 
        return (int)Math.ceil(travelTime(dest, speed));  
    }
    public int fullTravelTurns(StarSystem finalDest, ShipDesign design) {
        // calculate full travel turns for a ship in this fleet of type design
        // to travel from its current position (may be in transit to another 
        // system) to the requested finalDest
        
        // if we can travel directly (i.e. in orbit or hyperspace comms), return 
        // turns to finalDest for the requested design
        if (canSend()) 
            return travelTurns(finalDest, design.warpSpeed());  

        // ok, fleet needs to reach its current dest and the travel to final dest
        
        // get turns to current dest
        StarSystem currDest = destination();
        int currTurns = travelTurns(currDest);
        
        // if we can then stargate hop, just add 1 turn
        if (currDest.hasStargate(empire()) && finalDest.hasStargate(empire()))
            return currTurns + 1;
        
        // calculate turns to next dest and then return total
        int nextTurns = (int) Math.ceil(travelTime(currDest,finalDest,design.warpSpeed()));
        return currTurns+nextTurns;
    }
    public int travelTurnsRemaining()     { return hasDestination() ?  travelTurns(destination()) : 0;  }
    public int numScouts()   { return numShipType(ShipDesign.SCOUT); }
    public int numFighters() { return numShipType(ShipDesign.FIGHTER); }
    public int numBombers()  { return numShipType(ShipDesign.BOMBER); }
    public int numColonies() { return numShipType(ShipDesign.COLONY); }
    public boolean isEmpty()  { return numShips() == 0; }
    public int numShips ()   {
        int count = 0;
        for (int i=0;i<num.length;i++)
            count += num[i];
        return count;
    }
    public int numShipType(int missionType) {
        int count = 0;
        for (int i=0;i<num.length;i++)
            if (num[i]>0) {
                ShipDesign des = design(i);
                if ((des != null) && (des.mission() == missionType))
                    count += num[i];
            }
        return count;
    }
    public float bcValue() {
        float bc = 0;
        for (int i=0;i<num.length;i++) {
            if (num[i] > 0) {
                ShipDesign des = design(i);
                if (des != null)
                    bc += (num[i] * des.cost());
            }
        }
        return bc;
    }
    public ShipDesign newestOfType (int missionType) {
        ShipDesign newestDesign = null;
        for (int i=0;i<num.length;i++) {
            if (num[i] > 0) {
                ShipDesign desn = design(i);
                if ((desn != null) && (desn.mission() == missionType)) {
                    if ((newestDesign == null) || (newestDesign.seq() < desn.seq()))
                        newestDesign = desn;
                }
            }
        }
        return newestDesign;
    }
    public boolean hasColonyShip() {
        for (int i=0;i<num.length;i++) {
            if (num[i] > 0) {
                ShipDesign des = design(i);
                if ((des != null) && des.hasColonySpecial())
                    return true;
            }
        }
        return false;
    }
    public int[] colonyShips() {
        // return the ship stacks which have colony ships
        int[] colony = new int[num.length];
        for (int i=0;i<num.length;i++) {
            if (num[i] > 0) {
                ShipDesign des = design(i);
                if ((des != null) && des.hasColonySpecial())
                    colony[i] = num[i];
            }
        }
        return colony;
    }
    public void disband() {
         galaxy().ships.deleteFleet(this);
    }
    public void addFleet(ShipFleet fl) {
        if (this != fl) {
            for (int i=0;i<num.length;i++) 
                num[i] += fl.num[i];
        }
    }
    public void addShips(int designId, int n) {
        num[designId] = max(0, num[designId]+n);
    }
    public void addShips(ShipFleet otherFleet) {
        if (otherFleet == this)
            return;

        for (int i=0;i<num.length;i++)
            num[i] += otherFleet.num(i);

        session().replaceVarValue(otherFleet, this);
        log("disband#3 fleet: ", otherFleet.toString());
        otherFleet.disband();
    }
    public void removeShips(int designId, int n, boolean disbandIfEmpty) {
        num[designId] = max(0, num[designId]-n);

        if (disbandIfEmpty && !hasShips()) {
            log("disband#4 fleet: ", toString());
            disband();
        }
    }
    public int removeScrappedShips(int designId) {
        int scrappedCount = num[designId];
        // design has been scrapped
        num[designId] = 0;
        if (!hasShips()) 
            disband();
        
        return scrappedCount;
    }
    public void removeShips(ShipFleet subfleet) {
        for (int i=0;i<num.length;i++)
            num[i] = max(0, num[i]-subfleet.num(i));
    }
    public void bombard() {
        StarSystem sys = system();
        if (!sys.isColonized())
            return;

        Empire victim = sys.empire();
        victim.lastAttacker(empire());
        log(empire().name(), " fleet bombarding ", sys.name());
        ShipCombatManager mgr = galaxy().shipCombat();
        mgr.setupBombardment(system(), this);

        CombatStackColony colonyStack = mgr.results().colonyStack;
        for (int i=0;i<num.length;i++) {
            if (num[i] > 0) {
                ShipDesign d = design(i);
                if (d != null) {
                    CombatStackShip shipStack = new CombatStackShip(this, i, mgr);
                    for (int j=0;j<ShipDesign.maxWeapons();j++) {
                        int wpnCount = d.wpnCount(j);
                        int attackCount = d.weapon(j).attacksPerRound();
                        int bombAtt = d.weapon(j).bombardAttacks() - bombardCount(i);
                        int numAttacks = num[i] * attackCount * wpnCount * bombAtt;
                        for (int k=0;k<numAttacks && system().isColonized();k++)
                            d.weapon(j).fireUpon(shipStack, colonyStack, 1);
                    }
                    for (int j=0;j<ShipDesign.maxSpecials();j++) {
                        int numAttacks = d.special(j).bombardAttacks() - bombardCount(i);
                        for (int k=0;k<numAttacks && system().isColonized();k++)
                            d.special(j).fireUpon(shipStack, colonyStack, 1);
                    }
                }
            }
        }
        mgr.endOfCombat(true);
        empire().sv.refreshFullScan(sysId);
        victim.sv.refreshFullScan(sysId);
    }
    public float expectedBombardDamage() {
        if (!system().isColonized())
            return 0;

        float damage = 0;
        ShipCombatManager mgr = galaxy().shipCombat();
        CombatStackColony planetStack = new CombatStackColony(system().colony(), mgr);

        for (int i=0;i<num.length;i++) {
            if (num[i] > 0) {
                ShipDesign d = design(i);
                for (int j=0;j<ShipDesign.maxWeapons();j++)
                    damage += (num[i] * d.wpnCount(j) * d.weapon(j).estimatedBombardDamage(d, planetStack));
                for (int j=0;j<ShipDesign.maxSpecials();j++)
                    damage += d.special(j).estimatedBombardDamage(d, planetStack);
            }
        }
        return damage;
    }
    //
    // Fleet Sprite behavior is here now
    public final static Comparator<ShipFleet> DEST_Y = (ShipFleet o1, ShipFleet o2) -> Base.compare(o1.destY(), o2.destY());
    public FlightPathSprite pathSpriteTo(StarSystem sys) {
        return new FlightPathSprite(this, sys);
    }
    public void use(Sprite o, IMapHandler ui) {
        if (o instanceof StarSystem) {
            StarSystem sys = (StarSystem) o;
            FlightPathSprite.workingPath(pathSpriteTo(sys));
            ui.repaint();
        }
    }
    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int mapX, int mapY) {
        return displayed && selectBox().contains(mapX, mapY);
    }
    @Override
    public float selectDistance(GalaxyMapPanel map, int mapX, int mapY)  { 
        float centerX = selectBox().x+(selectBox().width/2);
        float centerY = selectBox().y+(selectBox().height/2);
        return distance(mapX, mapY, centerX, centerY);
    }
    @Override
    public int mapX(GalaxyMapPanel map) {
        int x = map.mapX(x());
        
        if (isInTransit())
            return x;
        // if in orbit, offset map position above and to right of the star
        float mult = max(4, min(100,map.scaleX()));
        if (isDeployed() || isRallied())
            x -= scaled((int)(600/mult)); // 20 px to left if leaving system
        else if (isOrbiting())
            x += scaled((int)(400/mult)); // 20 px to right of the star
        return x;
    }
    @Override
    public int mapY(GalaxyMapPanel map) {
        int vSpacing = BasePanel.s14;
        int y = map.mapY(y());
        if (isInTransit())
            return y;
        float mult = max(4, min(60,map.scaleX()));
        int shipH = (int) (500/mult);
        // check orbiting fleets
        if (!isInTransit()) {
            // 20 px above star, then +10 down for its position in fleet list
            y -= shipH;
            StarSystem sys = system();
            List<ShipFleet> fleets = sys.exitingFleets();
            // check if in exiting fleets first...so that list so that
            // fleets moving upward in galaxy are at the top of the list
            if (fleets.contains(this)) {
                Collections.sort(fleets, DEST_Y);
                int index = fleets.indexOf(this);
                y += (vSpacing*index);
                return y;
            }

            // search for a fleet matching this fleet's civ, not this fleet itself
            // this allows "working" fleets in the process of deployment to be found
            // at the correct index and the flight path line drawn properly
            List<ShipFleet> orbitingFleets = sys.orbitingFleets();
            int index  = 0;
            for (int i=0;i<orbitingFleets.size();i++) {
                ShipFleet fl = orbitingFleets.get(i);
                if (fl.empire() == empire())
                    index = i;
            }
            y += (vSpacing*index);
        }
        return y;
    }
    @Override
    public int centerMapX(GalaxyMapPanel map) {
        return mapX(map) + BasePanel.s7;
    }
    @Override
    public int centerMapY(GalaxyMapPanel map) {
        return mapY(map) + BasePanel.s5;
    }
    @Override
    public int maxMapScale() {
        float size = hullPoints();
        if (size >= 10000)
            return GalaxyMapPanel.MAX_FLEET_HUGE_SCALE;
        else if (size >= 100)
            return GalaxyMapPanel.MAX_FLEET_LARGE_SCALE;
        else
            return GalaxyMapPanel.MAX_FLEET_SMALL_SCALE;
    }
    @Override
    public void setDisplayed(GalaxyMapPanel map) {
        displayed = false;
        if (!map.displays(this))
            return;
        if (map.scaleX() > maxMapScale())
            return;
        Sprite clickedSprite = map.parent().clickedSprite();
        boolean clickingOnThisFleet = false;
        if ((clickedSprite == this)
        || ((destination() != null) && (destination() == clickedSprite)))
            clickingOnThisFleet = true;

        // if fleet is unarmed and map is not showing unarmed, then
        // don't draw unless we are clicking on this fleet
        boolean armed = isPotentiallyArmed(player());    
        if (!armed && !map.showUnarmedShips() && !clickingOnThisFleet)
            return;
        
        // stop drawing unarmed AI fleets at a certain zoom level
        if (!armed && !empire().isPlayer() && (map.scaleX() > GalaxyMapPanel.MAX_FLEET_UNARMED_SCALE))
            return;

        // because fleets can be disbanded asynchronously to the ui thread,
        // make sure this fleet is still active before drawing
        if (!isActive())
            return;
        
        if (!map.parent().shouldDrawSprite(this))
            return;

        displayed = true;
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        if (!displayed)
            return;
        float size = hullPoints();
        int imgSize = 1;
        if (size >= 100)
            imgSize = 2;
        else if (size >= 10000)
            imgSize = 3;
        
        if (map.scaleX() > GalaxyMapPanel.MAX_FLEET_LARGE_SCALE) 
            imgSize--;
        if (map.scaleX() > GalaxyMapPanel.MAX_FLEET_SMALL_SCALE)
            imgSize--;
        
        // are we zoomed out too far to show a fleet of this size?
        if (imgSize < 1)
            return;
        
        int x = mapX(map);
        int y = mapY(map);
        BufferedImage img;
        
        boolean armed = isPotentiallyArmed(player());    
        if (armed) {
            if (imgSize == 1)
                img = empire().shipImage();
            else if (imgSize == 2)
                img = empire().shipImageLarge();
            else
                img = empire().shipImageHuge();
        }
        else
            img = empire().scoutImage();

        int w = img.getWidth();
        int h = img.getHeight();

        if (!hasDestination() || (destX > x()))
            g2.drawImage(img, x, y, w, h, map);
        else
            g2.drawImage(img, x+w, y, -w, h, map);

        int pad = BasePanel.s8;
        selectBox().setBounds(x-pad,y-pad,w+pad+pad,h+pad+pad);

        int s5 = BasePanel.s5;
        int s10 = BasePanel.s10;
        int cnr = BasePanel.s10;
        if (map.parent().isClicked(this))
            drawSelection(g2, map, x-s5, y-s5, w+s10, h+s10, cnr);
        else if (map.parent().isHovering(this))
            drawHovering(g2, map, x-s5, y-s5, w+s10, h+s10, cnr);
    }
    private void drawSelection(Graphics2D g, GalaxyMapPanel map, int x, int y, int w, int h, int cnr) {
        if (empire() == null)
            return;

        g.setColor(empire().selectionColor());
        g.fillRoundRect(x, y, w, h, cnr, cnr);

        Stroke prev = g.getStroke();
        g.setStroke(BasePanel.stroke2);
        g.setColor(empire().color());
        g.drawRoundRect(x,y,w,h, cnr, cnr);
        g.setStroke(prev);
    }
    private void drawHovering(Graphics2D g, GalaxyMapPanel map, int x, int y, int w, int h, int cnr) {
        if (empire() == null)
            return;

        Stroke prev = g.getStroke();
        g.setStroke(BasePanel.stroke2);
        g.setColor(empire().color());
        g.drawRoundRect(x,y,w,h, cnr, cnr);
        g.setStroke(prev);
    }
    @Override
    public IMappedObject source() { return this; }
}