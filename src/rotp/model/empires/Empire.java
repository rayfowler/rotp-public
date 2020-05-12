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
package rotp.model.empires;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import rotp.model.ai.AI;
import rotp.model.ai.interfaces.Diplomat;
import rotp.model.ai.interfaces.FleetCommander;
import rotp.model.ai.interfaces.General;
import rotp.model.ai.interfaces.Governor;
import rotp.model.ai.interfaces.Scientist;
import rotp.model.ai.interfaces.ShipDesigner;
import rotp.model.ai.interfaces.SpyMaster;
import rotp.model.colony.Colony;
import rotp.model.colony.ColonyShipyard;
import rotp.model.empires.SpyNetwork.FleetView;
import rotp.model.events.SystemColonizedEvent;
import rotp.model.events.SystemHomeworldEvent;
import rotp.ui.notifications.*;
import rotp.model.galaxy.Galaxy;
import rotp.model.galaxy.IMappedObject;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.NamedObject;
import rotp.model.galaxy.Ship;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.Transport;
import rotp.model.incidents.GenocideIncident;
import rotp.model.planet.Planet;
import rotp.model.planet.PlanetType;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.ships.ShipLibrary;
import rotp.model.tech.Tech;
import rotp.model.tech.TechRoboticControls;
import rotp.model.tech.TechTree;
import rotp.ui.NoticeMessage;
import rotp.util.Base;

public final class Empire implements Base, NamedObject, Serializable {
    private static final long serialVersionUID = 1L;
    private static final float SHIP_MAINTENANCE_PCT = .02f;
    private static final int MAX_SECURITY_TICKS = 10;
    private static final float MAX_SECURITY_PCT = 0.20f;
    public static final int PLAYER_ID = 0;
    public static final int NULL_ID = -1;
    public static final int ABSTAIN_ID = -2;
    
    public static final int SHAPE_CIRCLE = 0;
    public static final int SHAPE_SQUARE = 1;
    public static final int SHAPE_DIAMOND = 2;
    public static final int SHAPE_TRIANGLE1 = 3;
    public static final int SHAPE_TRIANGLE2 = 4;

    public static Empire thePlayer() { return Galaxy.current().player(); }

    public final int id;
    private Leader leader;
    private final String raceKey;
    private final int raceNameIndex;
    private TechTree tech = new TechTree();
    private final ShipDesignLab shipLab;
    private final int homeSysId;
    private int capitalSysId;
    public final SystemInfo sv;
    private final EmpireView[] empireViews;
    private final List<Ship> visibleShips = new ArrayList<>();
    private final List<StarSystem> shipBuildingSystems = new ArrayList<>();
    private final List<StarSystem> colonizedSystems = new ArrayList<>();
    private boolean extinct = false;
    private boolean galacticAlliance = false;
    private int lastCouncilVoteEmpId = Empire.NULL_ID;
    private Colony.Orders priorityOrders = Colony.Orders.NONE;
    private int bannerColor;
    private final List<StarSystem> newSystems = new ArrayList<>();
    private final EmpireStatus status;

    //bounds
    private float minX, maxX, minY, maxY;
    private float planetScanningRange = 0;
    private float shipScanningRange = 0;
    private boolean knowShipETA = false;
    private boolean scanPlanets = false;
    private boolean recalcDistances = true;
    private float combatTransportPct = 0;
    private int securityAllocation = 0;
    private int empireTaxLevel = 0;
    private float totalReserve = 0;
    private float tradePiracyRate = 0;
    private NamedObject lastAttacker;

    private transient AI ai;
    private transient boolean[] canSeeShips;
    private transient Race race;
    private transient BufferedImage shipImage;
    private transient BufferedImage shipImageLarge;
    private transient BufferedImage shipImageHuge;
    private transient BufferedImage scoutImage;
    private transient BufferedImage transportImage;
    private transient Color ownershipColor;
    private transient Color selectionColor;
    private transient Color reachColor;
    private transient Color shipBorderColor;
    private transient Color scoutBorderColor;
    private transient Color empireRangeColor;

    public AI ai()                                {
        if (ai == null)
            ai = new AI(this);
        return ai;
    }
    public Diplomat diplomatAI()                  { return ai().diplomat(); }
    public FleetCommander fleetCommanderAI()      { return ai().fleetCommander(); }
    public General generalAI()                    { return ai().general(); }
    public Governor governorAI()                  { return ai().governor(); }
    public SpyMaster spyMasterAI()                { return ai().spyMaster(); }
    public Scientist scientistAI()                { return ai().scientist(); }
    public ShipDesigner shipDesignerAI()          { return ai().shipDesigner(); }
    
    public Leader leader()                        { return leader; }
    public ShipDesignLab shipLab()                { return shipLab; }
    public EmpireStatus status()                  { return status; }
    public int homeSysId()                        { return homeSysId; }
    public int capitalSysId()                     { return capitalSysId; }
    public String unparsedRaceName()              { return race().nameVariant(raceNameIndex); }
    public String raceName()                      { return raceName(0); }
    public String raceName(int i) {
        List<String> names = substrings(unparsedRaceName(), '|');
        if (i >= names.size() || names.get(i).isEmpty())
            return names.get(0);
        else
            return names.get(i);
    }
    public List<StarSystem> shipBuildingSystems() { return shipBuildingSystems; }
    public boolean inGalacticAlliance()           { return galacticAlliance; }
    public void joinGalacticAlliance()            { galacticAlliance = true; }
    public float planetScanningRange()            { return planetScanningRange; }
    public void planetScanningRange(float d)      { planetScanningRange = d; }
    public float shipScanningRange()              { return shipScanningRange; }
    public void shipScanningRange(float d)        { shipScanningRange = d; }
    public float combatTransportPct()             { return combatTransportPct; }
    public void combatTransportPct(float d)       { combatTransportPct = d; }
    public float tradePiracyRate()                { return tradePiracyRate; }
    public void tradePiracyRate(float f)          { tradePiracyRate = f; }
    public boolean extinct()                      { return extinct; }
    public TechTree tech()                        { return tech; }
    public float totalReserve()                   { return totalReserve; }
    public NamedObject lastAttacker()             { return lastAttacker; }
    public void lastAttacker(NamedObject e)       { lastAttacker = e; }
    public List<ShipFleet> assignableFleets()     {
        if (tech().hyperspaceCommunications())
            return galaxy().ships.allFleets(id);
        else
            return galaxy().ships.notInTransitFleets(id);
    }
    public List<Ship> visibleShips()              { return visibleShips; }
    public EmpireView[] empireViews()             { return empireViews; }
    public List<StarSystem> newSystems()          { return newSystems; }
    public int lastCouncilVoteEmpId()             { return lastCouncilVoteEmpId; }
    public void lastCouncilVoteEmpId(int e)       { lastCouncilVoteEmpId = e; }
    public boolean knowShipETA()                  { return knowShipETA; }
    public void knowShipETA(boolean b)            { knowShipETA = (knowShipETA || b); }
    public boolean scanPlanets()                  { return scanPlanets; }
    public void scanPlanets(boolean b)            { scanPlanets = (scanPlanets || b); }
    public void setRecalcDistances()              { recalcDistances = true; }

    public Colony.Orders priorityOrders()         { return priorityOrders; }
    public void priorityOrders(Colony.Orders o)   { priorityOrders = o; }
    public int colorId()                          { return bannerColor; }
    public void colorId(int i)                    { bannerColor = i; }
    public int shape()                            { return id / options().numColors(); }
    public float minX()                           { return minX; }
    public float maxX()                           { return maxX; }
    public float minY()                           { return minY; }
    public float maxY()                           { return maxY; }
    
    private boolean canSeeShips(int empId) {
        if (canSeeShips == null) {
            canSeeShips = new boolean[galaxy().numEmpires()];
            for (int i=0;i<canSeeShips.length;i++) 
                canSeeShips[i] = (i == id) || viewForEmpire(i).embassy().unity(); 
        }
        return canSeeShips[empId];
    }

    public Race race() {
        if (race == null)
            race = Race.keyed(raceKey);
        return race;
    }
    public BufferedImage scoutImage() {
        if (scoutImage == null)
            scoutImage = ShipLibrary.current().scoutImage(shipColorId());
        return scoutImage;
    }
    public BufferedImage shipImage() {
        if (shipImage == null)
            shipImage = ShipLibrary.current().shipImage(shipColorId());
        return shipImage;
    }
    public BufferedImage shipImageLarge() {
        if (shipImageLarge == null)
            shipImageLarge = ShipLibrary.current().shipImageLarge(shipColorId());
        return shipImageLarge;
    }
    public BufferedImage shipImageHuge() {
        if (shipImageHuge == null)
            shipImageHuge = ShipLibrary.current().shipImageHuge(shipColorId());
        return shipImageHuge;
    }
    public BufferedImage transportImage() {
        if (transportImage == null)
            transportImage = ShipLibrary.current().transportImage(shipColorId());
        return transportImage;
    }
    public Color ownershipColor() {
        if (ownershipColor == null) {
            Color c = color();
            ownershipColor = newColor(c.getRed(), c.getGreen(), c.getBlue(), 80);
        }
        return ownershipColor;
    }
    public Color selectionColor() {
        if (selectionColor == null) {
            Color c = color();
            selectionColor = newColor(c.getRed(), c.getGreen(), c.getBlue(), 160);
        }
        return selectionColor;
    }
    public Color shipBorderColor() {
        if (shipBorderColor == null) {
            Color c = color();
            int cR = c.getRed();
            int cG = c.getGreen();
            int cB = c.getBlue();
            shipBorderColor = newColor(cR*6/8,cG*6/8,cB*6/8);
        }
        return shipBorderColor;
    }
    public Color scoutBorderColor() {
        if (scoutBorderColor == null) {
            Color c = color();
            int cR = c.getRed();
            int cG = c.getGreen();
            int cB = c.getBlue();
            scoutBorderColor = newColor(cR*4/8,cG*4/8,cB*4/8);
        }
        return scoutBorderColor;
    }
    public Color empireRangeColor() {
        if (empireRangeColor == null) {
            Color c = color();
            int cR = c.getRed();
            int cG = c.getGreen();
            int cB = c.getBlue();
            empireRangeColor = newColor(cR*3/12,cG*3/12,cB*3/12);
        }
        return empireRangeColor;
    }
    public Color reachColor() {
        if (reachColor == null) {
            Color c = color();
            reachColor = newColor(c.getRed(), c.getGreen(), c.getBlue(), 48);
        }
        return reachColor;
    }
    public Empire(Galaxy g, int empId, String rk, StarSystem s, Integer cId, String name) {
        log("creating empire for ",  rk);
        id = empId;
        raceKey = rk;
        homeSysId = capitalSysId = s.id;     
        empireViews = new EmpireView[options().selectedNumberOpponents()+1];
        status = new EmpireStatus(this);
        sv = new SystemInfo(this);
        // many things need to know if this is the player civ, so set it early
        if (options().selectedPlayerRace().equals(rk))
            g.player(this);

        colorId(cId);
        String raceName = race().nextAvailableName();
        raceNameIndex = race().nameIndex(raceName);
        String leaderName = name == null ? race.nextAvailableLeader() : name;
        leader = new Leader(this, leaderName);
        shipLab = new ShipDesignLab();
    }
    public void setBounds(float x1, float x2, float y1, float y2) {
        minX = x1;
        maxX = x2;
        minY = y1;
        maxY = y2;
    }
    public void loadStartingTechs() {
        tech.recalc(this);
    }
    public void loadStartingShipDesigns() {
        shipLab.init(this);
    }
    public boolean isPlayer()            { return id == PLAYER_ID; };
    public boolean isAI()                { return !isPlayer(); };
    public boolean isPlayerControlled()  { return !isAIControlled(); }
    public boolean isAIControlled()      { return isAI() || options().isAutoPlay(); }
    public Color color()                 { return options().color(bannerColor); }
    public int shipColorId()             { return colorId(); }
    @Override
    public String name()                 { return race().text("GOVT_EMPIRE", raceName()); }
    
    public void chooseNewCapital() {
        // make list of every colony that is not the current capital
        StarSystem currentCapital = galaxy().system(capitalSysId);
        List<StarSystem> allExceptCapital = new ArrayList<>(allColonizedSystems());
        allExceptCapital.remove(currentCapital);
        
        // from that, make a list of non-rebelling colonies
        List<StarSystem> possible = new ArrayList<>();
        for (StarSystem sys: allExceptCapital) {
            if (!sys.colony().inRebellion())
                possible.add(sys);
        }
        // if EVERY colony is rebelling, then choose from any of them
        if (possible.isEmpty())
            possible.addAll(allExceptCapital);
        
        // if still no other colonies, give it up (we are losing our capital and last colony)
        if (possible.isEmpty())
            return;
        
        // sort based on production, choose highest(last), then end any rebellions on it
        Collections.sort(possible, StarSystem.BASE_PRODUCTION);
        StarSystem newHome = possible.get(possible.size()-1);
        capitalSysId = newHome.id;
        newHome.colony().clearAllRebellion();        
    }
    public int shipCount(int hullSize) {
        return galaxy().ships.hullSizeCount(id, hullSize);
    }
    public float totalShipMaintenanceCost() {
        int[] counts = galaxy().ships.shipDesignCounts(id);
        float cost = 0;
        for (int i=0;i<counts.length;i++) 
            cost += (counts[i] * shipLab.design(i).cost());
        
        return cost * SHIP_MAINTENANCE_PCT;
    }
    public float totalStargateCost() {
        float totalCostBC = 0;
        List<StarSystem> allSystems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: allSystems)
            totalCostBC += sys.colony().shipyard().stargateMaintenanceCost();
        return totalCostBC;
    }
    @Override
    public String toString()   { return concat("Empire: ", raceName()); }

    public String replaceTokens(String s, String key) {
        List<String> tokens = this.varTokens(s, key);
        String s1 = s;
        for (String token: tokens) {
            String replString = concat("[",key, token,"]");
            // leader name is special case, not in dictionary
            if (token.equals("_name")) 
                s1 = s1.replace(replString, leader().name());
            else if (token.equals("_home"))
                s1 = s1.replace(replString, sv.name(capitalSysId()));              
            else {
                List<String> values = substrings(race().text(token), ',');
                String value = raceNameIndex < values.size() ? values.get(raceNameIndex) : values.get(0);
                s1 = s1.replace(replString, value);
            }
        }
        return s1;
    }
    public String label(String token) {
        List<String> values = substrings(race().text(token), ',');
        return raceNameIndex < values.size() ? values.get(raceNameIndex) : values.get(0);      
    }
    public boolean canSendTransportsFrom(StarSystem sys) {
        return (sys != null) && (sv.empire(sys.id) == this) && (sv.maxTransportsToSend(sys.id) > 0)  && (allColonizedSystems().size() > 1) && !sys.colony().inRebellion() && !sys.colony().quarantined();
    }
    public boolean canSendTransportsTo(StarSystem sys) {
        if (sys == null)
            return false;
        if (!sv.isScouted(sys.id))
            return false;
        if (!sv.isColonized(sys.id))
            return false;
        if (!sv.inShipRange(sys.id))
            return false;
        if ((sys.empire() == this) && sys.colony().inRebellion())
            return true;
        return canColonize(sys.planet());
    }
    public boolean canRallyFleetsFrom(int sysId) {
        return (sysId != StarSystem.NULL_ID) && (sv.empire(sysId) == this) && (allColonizedSystems().size() > 1);
    }
    public boolean canRallyFleetsTo(int sysId) {
        return (sysId != StarSystem.NULL_ID) && (sv.empire(sysId) == this) && (allColonizedSystems().size() > 1);
    }
    public boolean canRallyFleets() {
        return allColonizedSystems().size() > 1;
    }
    public boolean canSendTransports() {
        return allColonizedSystems().size() > 1;
    }
    public int maxTransportsAllowed(StarSystem sys) {
        if (!canSendTransportsTo(sys))
            return 0;
        else if (sys.empire() == this)
            return (int)(sv.currentSize(sys.id) - (int) sv.population(sys.id));
        else
            return (int) sv.currentSize(sys.id);
    }
    public void changeAllExistingRallies(StarSystem dest) {
        if (canRallyFleetsTo(id(dest))) {
            for (StarSystem sys: allColonizedSystems()) {
                if (sv.hasRallyPoint(sys.id))
                    sv.rallySystem(sys.id, dest);
            }
        }
    }
    public void startRallies(List<StarSystem> fromSystems, StarSystem dest) {
        if (canRallyFleetsTo(id(dest))) {
            for (StarSystem sys: fromSystems)
                sv.rallySystem(sys.id, dest);
        }
    }
    public void stopRallies(List<StarSystem> fromSystems) {
        for (StarSystem sys: fromSystems)
            sv.stopRally(sys.id);
    }
    public void cancelTransport(StarSystem from) {
        from.transportSprite().clear();
    }
    public void deployTransport(StarSystem from) {
        from.transportSprite().accept();
    }
    public void deployTransports(List<StarSystem> fromSystems, StarSystem dest, boolean synch) {
        if (synch) {
            float maxTime = 0;
            for (StarSystem from: fromSystems)
                maxTime = max(maxTime, from.colony().transport().travelTime(dest));
            for (StarSystem from: fromSystems)
                from.transportSprite().accept(maxTime);
        }
        else {
            for (StarSystem from: fromSystems)
                from.transportSprite().accept();
        }
    }
    public int travelTurns(StarSystem from, StarSystem dest, float speed) {
        if (from.hasStargate(this) && dest.hasStargate(this))
            return 1;
        return (int) Math.ceil(from.travelTime(from,dest,speed));
    }
    public void stopRalliesWithSystem(StarSystem dest) {
        List<StarSystem> systems = allColonizedSystems();
        sv.stopRally(dest.id);
        for (StarSystem sys: systems) {
            if (sv.rallySystem(sys.id) == dest) 
                sv.stopRally(sys.id);
        }
    }
    public void validate() {
        validateColonizedSystems();
        for (StarSystem sys: colonizedSystems)
            sys.colony().validate();
    }
    private void validateColonizedSystems() {
        List<StarSystem> good = new ArrayList<>();
        for (StarSystem sys: colonizedSystems) {
            if (sys.isColonized() && (sys.empire() == this)) {
                if (!good.contains(sys))
                    good.add(sys);
            }
        }
        colonizedSystems.clear();
        colonizedSystems.addAll(good);
    }
    public void cancelTransports(List<StarSystem> fromSystems) {
        for (StarSystem from: fromSystems)
            from.transportSprite().clear();
    }
    public void addColonyOrder(Colony.Orders order, float amt) {
        for (StarSystem sys: allColonizedSystems())
            sys.colony().addColonyOrder(order, amt);
    }
    public void addColonizedSystem(StarSystem s) {
        if (!colonizedSystems.contains(s)) {
            colonizedSystems.add(s);
            setRecalcDistances();
            for (Empire ally: allies())
                ally.setRecalcDistances();       
        }
    }
    public void removeColonizedSystem(StarSystem s) {
        colonizedSystems.remove(s);
        setRecalcDistances();
        for (Empire ally: allies())
            ally.setRecalcDistances();
        
        if (colonizedSystems.isEmpty())
            goExtinct();
    }
    public Colony colonize(String sysName, StarSystem sys) {
        StarSystem home = galaxy().system(capitalSysId);
        sys.addEvent(new SystemColonizedEvent(id));
        newSystems.add(sys);
        addColonizedSystem(sys);
        Colony c = sys.becomeColonized(sysName, this);
        governorAI().setInitialAllocations(c);
        if (isPlayer()) {
            int maxTransportPop =(int)(sys.planet().maxSize()-sys.colony().population());
            galaxy().giveAdvice("MAIN_ADVISOR_TRANSPORT", sysName, str(maxTransportPop), home.name());
        }
        return c;
    }
    public Colony colonizeHomeworld() {
        StarSystem home = galaxy().system(homeSysId);
        home.addEvent(new SystemHomeworldEvent(id));
        newSystems.add(home);
        colonizedSystems.add(home);
        Colony c = home.becomeColonized(home.name(), this);
        c.setHomeworldValues();
        governorAI().setInitialAllocations(c);
        sv.refreshFullScan(homeSysId);
        return c;
    }
    public boolean isHomeworld(StarSystem sys) {
        return sys.id == homeSysId;
    }
    public boolean isCapital(StarSystem sys) {
        return sys.id == capitalSysId;
    }
    public boolean isColony(StarSystem sys) {
        return (sv.empire(sys.id) == this) && isEnvironmentHostile(sys);
    }
    public boolean isEnvironmentHostile(StarSystem  sys) {
        return !race().ignoresPlanetEnvironment() && sys.planet().isEnvironmentHostile();
    }
    public boolean isEnvironmentFertile(StarSystem  sys) {
        return sys.planet().isEnvironmentFertile();
    }
    public boolean isEnvironmentGaia(StarSystem  sys) {
        return sys.planet().isEnvironmentGaia();
    }
    public void setBeginningColonyAllocations() {
        // try to maximum industry at start of game (for players)
        Colony c = galaxy().system(homeSysId).colony();
        c.clearSpending();
        governorAI().setInitialAllocations(c);
    }
    public boolean colonyCanScan(StarSystem sys) {
        return scanPlanets && sv.withinRange(sys.id, planetScanningRange());
    }
    public boolean fleetCanScan(StarSystem sys) {
        if (!scanPlanets)
            return false;
        List<ShipFleet> fleets = galaxy().ships.allFleets(id);
        for (ShipFleet fl: fleets) {
            if (shipScanningRange() >= fl.distanceTo(sys))
                return true;
        }
        return false;
    }
    public int shipRange()              { return tech().shipRange(); }
    public int scoutRange()             { return tech().scoutRange(); }
    public int researchingShipRange()   { return tech().researchingShipRange(); }
    public int researchingScoutRange()  { return tech().researchingScoutRange(); }
    public int learnableShipRange()     { return tech().learnableShipRange(); }
    public int learnableScoutRange()    { return tech().learnableScoutRange(); }
    public float shipReach(int turns)   { return min(shipRange(), turns*tech().topSpeed()); }
    public float scoutReach(int turns)  { return min(scoutRange(), turns*tech().topSpeed()); }
    
    public String rangeTechNeededToScout(int sysId) {
        float dist = sv.distance(sysId);
        return tech().rangeTechNeededToScoutDistance(dist);
    }
    public String rangeTechNeededToReach(int sysId) {
        float dist = sv.distance(sysId);
        return tech().rangeTechNeededToReachDistance(dist);
    }    
    public String environmentTechNeededToColonize(int sysId) {
        if (canColonize(sysId))
            return null;
        int hostility = sv.planetType(sysId).hostility();
        return tech().environmentTechNeededToColonize(hostility);
    }    
    public boolean canColonize(Planet p) { return canColonize(p.type()); }
    public boolean canLearnToColonize(Planet p) { return canLearnToColonize(p.type()); }
    public boolean isLearningToColonize(Planet p) { return isLearningToColonize(p.type()); }

    public boolean canColonize(int sysId) {
        StarSystem sys = galaxy().system(sysId);
        return canColonize(sys.planet().type());
    }
    public boolean canColonize(StarSystem sys) {
        return canColonize(sys.planet().type());
    }
    public boolean canColonize(PlanetType pt) {
        if (pt == null)  // hasn't been scouted yet
            return false;
        if (pt.isAsteroids())
            return false;
        if (race().ignoresPlanetEnvironment())
            return true;
        return pt.hostility() <= tech().hostilityAllowed();
    }
    public boolean isLearningToColonize(PlanetType pt) {
        if (pt == null)  // hasn't been scouted yet
            return false;
        if (pt.isAsteroids())
            return false;
        if (race().ignoresPlanetEnvironment())
            return true;
        return pt.hostility() <= tech().researchingHostilityAllowed();
    }
    public boolean canLearnToColonize(PlanetType pt) {
        if (pt == null)  // hasn't been scouted yet
            return false;
        if (pt.isAsteroids())
            return false;
        if (race().ignoresPlanetEnvironment())
            return true;
        return pt.hostility() <= tech().learnableHostilityAllowed();
    }
    public boolean knowETA(Ship sh) {
        return knowShipETA || canSeeShips(sh.empId());
    }
    public StarSystem defaultSystem() {
        StarSystem home = galaxy().system(capitalSysId);
        if (home.empire() == this)
            return home;
        else
            return allColonizedSystems().get(0);
    }
    public void addViewFor(Empire emp) {
        if ((emp != null) && (emp != this))
            empireViews[emp.id] = new EmpireView(this, emp);
    }
    public float tradeIncomePerBC() {
        float empireBC = totalPlanetaryProduction();
        float income = netTradeIncome();
        return income / empireBC;
    }
    public float shipMaintCostPerBC() {
        float empireBC = totalPlanetaryProduction();
        float shipMaint = totalShipMaintenanceCost();
        return shipMaint / empireBC;
    }
    public float stargateCostPerBC() {
        float empireBC = totalPlanetaryProduction();
        float shipMaint = totalStargateCost();
        return shipMaint / empireBC;
    }
    public void nextTurn() {
        log(this + ": NextTurn");
        shipBuildingSystems.clear();
        newSystems.clear();

        for (ShipDesign d : shipLab.designs()) {
            if (d != null)
                d.preNextTurn();
        }
        
        refreshViews();
        List<StarSystem> allColonies = allColonizedSystems();
        List<Transport> transports = transports();

        if (!extinct) {
            if (allColonies.isEmpty() && transports.isEmpty()) {
                goExtinct();
                return;
            }
        }

        // assign planetary funds/costs & enact development
        for (StarSystem s: allColonies) {
            Colony col = s.planet().colony();
            addReserve(col.production() * empireTaxPct());
            col.nextTurn();
        }
    }
    public void postNextTurn() {
        log(this + ": postNextTurn");
        float civProd = totalPlanetaryProduction();

        // assign funds/costs for diplomatic activities
        for (EmpireView v : empireViews()) {
          if ((v!= null) && v.embassy().contact())
                v.nextTurn(civProd);
        }
    }
    public void assessTurn() {
        log(this + ": AssessTurn");

        if (status() != null)
            status().assessTurn();

        for (int i=0;i<sv.count();i++) {
            if ((sv.empire(i) == this) && sv.isColonized(i))
                sv.colony(i).assessTurn();
        }

    }
    public void makeDiplomaticOffers() {
        for (EmpireView v : empireViews()) {
            if ((v!= null) && v.embassy().contact())
                v.makeDiplomaticOffers();
        }
    }
    public void completeResearch() {
        tech.allocateResearch();
    }
    public void acquireTradedTechs() {
        tech.acquireTradedTechs();
    }
    public void makeNextTurnDecisions() {
        //long tm0 = System.currentTimeMillis();
        log(this + ": make NextTurnDecisions");
        if (recalcDistances) {
            NoticeMessage.setSubstatus(text("TURN_RECALC_DISTANCES"));
            sv.calculateSystemDistances();
            recalcDistances = false;
            //long tm1 = System.currentTimeMillis();
            //log("recalcDistances: "+(tm1-tm0)+"ms");
            //tm0 = tm1;
        }

        NoticeMessage.setSubstatus(text("TURN_REFRESHING"));
        refreshViews();
        //long tm2 = System.currentTimeMillis();
        //log("refreshViews: "+(tm2-tm0)+"ms");

        NoticeMessage.setSubstatus(text("TURN_SCRAP_SHIPS"));
        shipLab.nextTurn();

        fleetCommanderAI().nextTurn();
        NoticeMessage.setSubstatus(text("TURN_DESIGN_SHIPS"));
        shipDesignerAI().nextTurn();
        NoticeMessage.setSubstatus(text("TURN_COLONY_SPENDING"));
        setAllocations();

        //long tm3 = System.currentTimeMillis();
        //log("remainder: "+(tm3-tm2)+"ms");
    }
    public String decode(String s, Empire listener) {
        String s1 = this.replaceTokens(s, "my");
        s1 = listener.replaceTokens(s1, "your");
        return s1;
    }
    public String decode(String s, Empire listener, Empire other) {
        String s1 = this.replaceTokens(s, "my");
        s1 = listener.replaceTokens(s1, "your");
        s1 = other.replaceTokens(s1, "other");
        return s1;
    }
    public List<Transport> transports() {
        Galaxy gal = galaxy();
        List<Transport> transports = new ArrayList<>();
        for (Transport tr: gal.transports()) {
            if (tr.empId() == id)
                transports.add(tr);
        }
        return transports;
    }
    public int transportsInTransit(StarSystem s) {
        Galaxy gal = galaxy();
        int transports = s.orbitingTransports(id);
        
        for (Transport tr: gal.transports()) {
            if ((tr.empId() == id) && (tr.destSysId() == s.id))
                transports += tr.size();
        }
        return transports;
    }
    public float transportSpeed(IMappedObject fr, IMappedObject to) {
        float time = fr.travelTime(fr, to, tech().transportSpeed());
        float dist = fr.distanceTo(to);
        return dist/time;
    }
    private void setAllocations() {
        if (isAIControlled())
            ai().sendTransports();

        // colony development (sometimes done for player if auto-pilot)
        for (int n=0; n<sv.count(); n++) {
            if (sv.empId(n) == id)
                governorAI().setColonyAllocations(sv.colony(n));
        }

        if (!isAIControlled())
            return;

        ai().treasurer().allocateReserve();
        // diplomatic activities
        for (EmpireView ev : empireViews()) {
            if ((ev != null) && ev.embassy().contact())
                ev.setSuggestedAllocations();
        }
        // empire settings
        ai().scientist().setTechTreeAllocations();
        securityAllocation = spyMasterAI().suggestedInternalSecurityLevel();
        empireTaxLevel = governorAI().suggestedEmpireTaxLevel();
    }
    public void checkForRebellionSpread() {
        if (extinct)
            return;

        // for each rebelling colony, check the nearest 10 systems to see
        // if rebellion spreads to a nearby colony. Chance of spread is 5%
        // divided by the distance in l-y... i.e a colony 5 light-years away
        // has a 1% chance each turn to rebel
        //
        // this is a different and less extreme mechanic than the one described 
        // in the OSG (which was never implemented in actual MOO1 code anyway) 
        // and will scale better for really large maps. 
        
        // build a list of rebelling systems
        List<StarSystem> allSystems = allColonizedSystems();
        List<StarSystem> rebellingSystems = new ArrayList<>();
        for (StarSystem sys: allSystems) {
            if (sys.colony().inRebellion()) {
                rebellingSystems.add(sys);
            }
        }

        // for each rebelling system, build a list of nearby systems that
        // are colonized by the same empire, are not the capital and not currently
        // rebelling. Check each of them for potential rebellion
        for (StarSystem sys: rebellingSystems) {
            for (int nearbyId: sys.nearbySystems()) {
                StarSystem near = galaxy().system(nearbyId);
                if ((near.empire() == this) && !near.colony().isCapital() && !near.colony().inRebellion()) {
                    float pct = 0.05f / (sys.distanceTo(near));
                    if (random() < pct)
                        near.colony().spreadRebellion();
                }
            }
        }
    }
    public boolean inRevolt() {
        float rebellingPop = 0;
        float loyalPop = 0;
        for (StarSystem sys: allColonizedSystems()) {
            if (sys.colony().inRebellion())
                rebellingPop += sys.colony().population();
            else
                loyalPop += sys.colony().population();
        }
        return rebellingPop > loyalPop;
    }
    public void overthrowLeader() {
        if (isPlayer()) {
            //session().status().loseOverthrown();
            return;
        }

        leader = new Leader(this);
        for (EmpireView view: empireViews()) {
            if (view != null)
                view.breakAllTreaties();
        }
        
        // end all rebellions
        for (StarSystem sys: allColonizedSystems()) 
            sys.colony().rebels(0);    

        if (viewForEmpire(player()).embassy().contact()) {
            String leaderDesc = text("LEADER_PERSONALITY_FORMAT", leader.personality(),leader.objective());
            String message = text("GNN_OVERTHROW", name(), leaderDesc);
            GNNNotification.notifyRebellion(message);
        }
    }
    public boolean inEconomicRange(int empId) {
        Empire e = galaxy().empire(empId);
        int range = max(e.scoutRange(), scoutRange());
        for (StarSystem sys: e.allColonizedSystems()) {
            if (sv.distance(sys.id) <= range)
                return true;
        }
        return false;
    }
    // only used as a test method to accelerate testing
    // of the galactic council
    public void makeFullContact() {
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.embassy().contact())
                v.embassy().makeFirstContact();
        }
    }
    public float orionCouncilBonus() {
        Galaxy gal = galaxy();
        for (int i=0; i<sv.count(); i++) {
            if ((sv.empire(i) == this) && gal.system(i).planet().isOrionArtifact())
                return .2f;
        }
        return 0;
    }
    public void addVisibleShip(Ship sh) {
        if (!visibleShips.contains(sh))
            visibleShips.add(sh);
    }
    public void addVisibleShips(List<? extends Ship> ships) {
        if (ships != null) {
            for (Ship sh: ships)
                addVisibleShip(sh);
        }
    }
    public List<ShipFleet> enemyFleets() {
        List<ShipFleet> list = new ArrayList<>();
        for (Ship sh: visibleShips()) {
            if (sh instanceof ShipFleet) {
                ShipFleet fl = (ShipFleet) sh;
                if ((fl.empId() != id) && !fl.isEmpty())
                    list.add((ShipFleet) sh);
            }
        }
        return list;
    }
    public void startGame() {
        refreshViews();
        StarSystem home = galaxy().system(homeSysId);
        governorAI().setInitialAllocations(home.colony());
    }
    public void refreshViews() {
        Galaxy gal = galaxy();
        for (int i=0;i<sv.count();i++) {
            StarSystem sys = gal.system(i);
            if (sys.empire() == this)
                sv.refreshFullScan(i);
            else if ((sys.orbitingFleetForEmpire(this) != null)
            && !sys.orbitingShipsInConflict())
                sv.refreshFullScan(i);
            else if (colonyCanScan(sys))
                sv.refreshLongRangeScan(i);
            else if (fleetCanScan(sys))
                sv.refreshLongRangeScan(i);
            else if (sv.isScouted(i)) // don't keep stale fleet info
                sv.view(i).clearFleetInfo();
        }

        for (EmpireView v : empireViews()) {
            if (v!= null)
                v.refresh();
        }
        // redetermine border/support/inner status for colonies
        for (int n=0;n<sv.count();n++)
            sv.resetSystemData(n);

        setVisibleShips();
    }
    public void setVisibleShips(int sysId) {
        addVisibleShips(sv.orbitingFleets(sysId));
        addVisibleShips(sv.exitingFleets(sysId));
    }
    public void setVisibleShips() {
        Galaxy gal = galaxy();
        visibleShips.clear();
        
        float scanRange = planetScanningRange();
        int numEmps = gal.numEmpires();
        // get ships orbiting visible systems
        for (int sysId=0;sysId<sv.count();sysId++) {
            StarSystem sys = sv.system(sysId);
            boolean canScan = sv.withinRange(sysId, scanRange);
            if (!canScan)  {
                for (int empId=0;empId<numEmps;empId++) {
                    if (!canScan && canSeeShips(empId) && (gal.ships.anyFleetAtSystem(empId,sysId) != null))
                        canScan = true;
                }
            }
            if (canScan) {
                addVisibleShips(sys.orbitingFleets());
                addVisibleShips(sys.exitingFleets());
            }
        }

        // get transports in transit
        for (Transport tr : gal.transports()) {
            if (canSeeShips(tr.empId())
            || (tr.visibleTo(id) && canScanTo(tr) ))
                addVisibleShip(tr);
        }

        // get fleets in transit
        for (ShipFleet sh : gal.ships.inTransitFleets()) {
            if (canSeeShips(sh.empId())
            || (sh.visibleTo(id) && canScanTo(sh) ))
                addVisibleShip(sh);
        }

        // inform our spies!
        for (Ship fl : visibleShips) {
            if (fl instanceof ShipFleet)
                detectFleet((ShipFleet)fl);
        }
    }
    public boolean canScanTo(IMappedObject loc) {
        return planetsCanScanTo(loc) || shipsCanScanTo(loc);
    }
    public boolean planetsCanScanTo(IMappedObject loc) {
        if (planetScanningRange() == 0)
            return false;

        Galaxy gal = galaxy();
        for (int i=0; i<sv.count(); i++) {
            if ((sv.empire(i) == this) && (gal.system(i).distanceTo(loc) <= planetScanningRange()))
                return true;
        }
        return false;
    }
    public boolean shipsCanScanTo(IMappedObject loc) {
        if (shipScanningRange == 0)
            return false;

        List<ShipFleet> fleets = galaxy().ships.allFleets(id);
        for (ShipFleet fl : fleets) {
            if (fl.distanceTo(loc) < shipScanningRange)
                return true;
        }
        return false;
    }
    public float estimatedShipFirepower(Empire emp, int shipSize, int shieldLevel) {
        return 0;
    }
    public boolean canScoutTo(Location xyz) {
        Galaxy gal = galaxy();
        for (int i=0; i<gal.numStarSystems(); i++) {
           StarSystem s = gal.system(i);
            if ((s.empire() == this) && (s.distanceTo(xyz) <= tech.scoutRange())  )
                return true;
        }
        return false;
    }
    public float distanceToSystem(StarSystem sys, List<StarSystem> froms) {
        float distance = Float.MAX_VALUE;
        for (StarSystem from: froms)
            distance = min(from.distanceTo(sys), distance);

        return distance;
    }

    public float distanceTo(IMappedObject xyz) {
        float distance = Float.MAX_VALUE;

        Galaxy gal = galaxy();
        List<Empire> allies = this.allies();
        if (allies.isEmpty()) {
            for (int i=0; i<gal.numStarSystems(); i++) {
                StarSystem s = gal.system(i);
                if (s.empire() == this)
                    distance = min(s.distanceTo(xyz), distance);
                if (distance == 0)
                    return distance;
            }
        }
        else {
            for (int i=0; i<gal.numStarSystems(); i++) {
                StarSystem s = gal.system(i);
                if (alliedWith(s.empire().id))
                    distance = min(s.distanceTo(xyz), distance);
                if (distance == 0)
                    return distance;
            }
        }
        return distance;
    }
    public int rangeTo(StarSystem sys) {
        return (int) Math.ceil(sv.distance(sys.id));
    }
    public StarSystem mostPopulousSystemForCiv(Empire c) {
        StarSystem bestSystem, biggestSystem;

        bestSystem = null;
        biggestSystem = null;
        float maxPop1 = 0;
        float maxPop2 = 0;

        for (StarSystem sys: systemsForCiv(c)) {
            float sysPop = sv.population(sys.id);
            Colony col = sv.colony(sys.id);
            if (sysPop > maxPop1) {
                maxPop1 = sysPop;
                biggestSystem = sys;
            }
            if (col != null) {
                if ((sysPop > maxPop2) && (! col.inRebellion()) ){
                    maxPop2 = sysPop;
                    bestSystem = sys;
                }
            }
        }

        if (bestSystem != null)
            return bestSystem;
        else
            return biggestSystem;
    }
    public boolean isAnyColonyConstructing(ShipDesign d) {
        for (int n=0;n<sv.count();n++) {
            if ((sv.empire(n) == this) && (sv.colony(n).shipyard().queuedBCForDesign(d) > 0))
                return true;
        }
        return false;
    }
    public List<StarSystem> coloniesConstructing(ShipDesign d) {
        Galaxy gal = galaxy();
        List<StarSystem> colonies = new ArrayList<>();

        for (int i=0;i<sv.count();i++) {
            if ((sv.empire(i) == this) && (sv.colony(i).shipyard().queuedBCForDesign(d) > 0))
                colonies.add(gal.system(i));
        }
        return colonies;
    }
    public int shipDesignCount(int designId) {
        return galaxy().ships.shipDesignCount(id, designId);
    }
    public void swapShipConstruction(ShipDesign oldDesign) {
        swapShipConstruction(oldDesign, null);
    }
    public void swapShipConstruction(ShipDesign oldDesign, ShipDesign newDesign) {
        for (StarSystem sys: allColonizedSystems()) {
            ColonyShipyard shipyard = sys.colony().shipyard();
            if (shipyard.design() == oldDesign) {
                if ((newDesign != null) && newDesign.active())
                    shipyard.switchToDesign(newDesign);
                else
                    shipyard.goToNextDesign();
            }
        }
    }
    public EmpireView viewForEmpire(Empire emp) {
        if ((emp != null) && (emp != this))
            return empireViews[emp.id];
        return null;
    }
    public EmpireView viewForEmpire(int empId) {
        if ((empId < 0) || (empId >= empireViews.length) || (empId == id))
            return null;
        
        return empireViews[empId];
    }
    public boolean hasContact(Empire c) {
        EmpireView v = viewForEmpire(c);
        return (v != null) && (v.embassy().contact() && !v.empire().extinct);
    }
    public void makeContact(Empire c) {
        EmpireView v = viewForEmpire(c);
        if (v != null)
            v.setContact();
    }
    public boolean atWar() {
        for (EmpireView v: empireViews()) {
            if ((v != null) && v.embassy().anyWar())
                return true;
        }
        return false;
    }
    public float powerLevel(Empire e) {
        return militaryPowerLevel(e) + industrialPowerLevel(e);
    }
    public float militaryPowerLevel(Empire e) {
        TechTree t0 = e == this ? tech() : viewForEmpire(e).spies().tech();
        float fleet = totalFleetSize(e);
        float techLvl = t0.avgTechLevel();
        return fleet*techLvl;
    }
    public float militaryPowerLevel() {
        float fleet = totalArmedFleetSize();
        float techLvl = tech().avgTechLevel();
        return fleet*techLvl;
    }
    public float industrialPowerLevel(Empire e) {
        TechTree t0 = e == this ? tech() : viewForEmpire(e).spies().tech();
        float prod = totalPlanetaryProduction(e);
        float techLvl = t0.avgTechLevel();
        return prod*techLvl;
    }
    public boolean hasAnyContact() {  return !contactedEmpires().isEmpty(); }

    public List<Empire> contactedEmpires() {
        List<Empire> r = new ArrayList<>();

        for (EmpireView v: empireViews()) {
            if ((v!= null) && v.embassy().contact() && !v.empire().extinct)
                r.add(v.empire());
        }
        return r;
    }
    public List<EmpireView> contactedCivsThatKnow(Tech t) {
        List<EmpireView> r = new ArrayList<>();

        for (EmpireView cv : empireViews()) {
            if ((cv!= null) && cv.embassy().contact() && cv.spies().tech().knows(t))
                r.add(cv);
        }
        return r;
    }
    public DiplomaticTreaty treatyForCiv(Empire c) {
        EmpireView v = viewForEmpire(c);
        if (v == null)
            return null;
        else
            return v.embassy().treaty();
    }
    public int numColonies() {
        int count = 0;
        for (int n=0; n<sv.count(); n++) {
            if (sv.empire(n) == this)
                count++;
        }
        return count;
    }
    public float totalSpyCostPct() {
        float sum = 0;
        for (EmpireView ev : empireViews()) {
            if (ev != null)
                sum += ev.spies().allocationCostPct();
        }
        return sum;
    }
    public int totalActiveSpies() {
        int sum = 0;
        for (EmpireView ev : empireViews()) {
            if (ev != null)
                sum += ev.spies().numActiveSpies();
        }
        return sum;
    }
    public int internalSecurity()            { return securityAllocation; }
    public void internalSecurity(int i)      { securityAllocation = bounds(0,i,MAX_SECURITY_TICKS); }
    public float internalSecurityPct()       { return (float) securityAllocation/MAX_SECURITY_TICKS; }
    public void increaseInternalSecurity()   { internalSecurity(securityAllocation+1); }
    public void decreaseInternalSecurity()   { internalSecurity(securityAllocation-1); }
    public void securityAllocation(float d) {
        // d assumed to be between 0 & 1, representing pct of slider clicked
        float incr = 1.0f/(MAX_SECURITY_TICKS+1);
        float sum = 0;
        for (int i=0;i<MAX_SECURITY_TICKS+1;i++) {
            sum += incr;
            if (d <= sum) {
                internalSecurity(i);
                return;
            }
        }
        internalSecurity(MAX_SECURITY_TICKS);
    }
    public float totalInternalSecurityPct() {
        return  MAX_SECURITY_PCT*securityAllocation/MAX_SECURITY_TICKS;
    }
    public float internalSecurityCostPct() {
        return (totalInternalSecurityPct()/2);
    }
    public float totalSecurityCostPct() {
        return totalSpyCostPct() + internalSecurityCostPct();
    }
    public float baseSpyCost() {
        return (25 + (tech.computer().techLevel()*2)) * race().spyCostMod();
    }
    public float troopKillRatio(StarSystem s) {
        float killRatio = (50 + tech.troopCombatAdj(false)) / (50 + sv.defenderCombatAdj(s.id));
        return killRatio;
    }
    public List<EmpireView> contacts() {
        List<EmpireView> r = new ArrayList<>();
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct && v.embassy().contact())
                r.add(v);
        }
        return r;
    }
    public List<EmpireView> commonContacts(Empire emp2) {
        List<EmpireView> r = new ArrayList<>();
        if (emp2.extinct)
            return r;
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct && v.embassy().contact()) {
                if (v.empire() == emp2)
                    r.add(v);
                else {
                    EmpireView v2 = v.empire().viewForEmpire(emp2);
                    if (v2.embassy().contact())
                        r.add(v);
                }
            }
        }
        return r;
    }
    public int numEnemies() {
        int n = 0;
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct
            && (v.embassy().anyWar() || v.embassy().onWarFooting()))
                n++;
        }
        return n;
    }
    public List<Empire> enemies() {
        List<Empire> r = new ArrayList<>();
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct
            && (v.embassy().anyWar() || v.embassy().onWarFooting()))
                r.add(v.empire());
        }
        return r;
    }
    public List<EmpireView> enemyViews() {
        List<EmpireView> r = new ArrayList<>();
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct
            && (v.embassy().anyWar() || v.embassy().onWarFooting()))
                r.add(v);
        }
        return r;
    }
    public List<EmpireView> hostiles() {
        List<EmpireView> r = new ArrayList<>();
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct && !v.embassy().isFriend())
                r.add(v);
        }
        return r;
    }
    public boolean hasNonEnemiesKnownBy(Empire e) {
        return !nonEnemiesKnownBy(e).isEmpty();
    }
    public List<Empire> nonEnemiesKnownBy(Empire empOther) {
        List<Empire> enemies = new ArrayList<>();
        // return any empires we are both in economic range of 
        // and this empire not already at war with
        List<Empire> contacts = contactedEmpires();
        contacts.remove(empOther);
        for (Empire emp: contacts) {
            if (!atWarWith(emp.id) && inEconomicRange(emp.id) && empOther.inEconomicRange(emp.id) && !unityWith(emp.id))
                enemies.add(emp);
        }
        return enemies;
    }
    public List<Empire> allies() {
        List<Empire> r = new ArrayList<>();
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct && v.embassy().isAlly())
                r.add(v.empire());
        }
        return r;
    }
    public boolean hasAlliesKnownBy(Empire emp1) {
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct && (v.empire() != emp1) && v.embassy().isAlly() && emp1.hasContact(v.empire()))
                return true;
        }
        return false;
    }
    public List<Empire> alliesKnownBy(Empire emp1) {
        List<Empire> allies = new ArrayList<>();
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct && (v.empire() != emp1) && v.embassy().isAlly() && emp1.hasContact(v.empire()))
                allies.add(v.empire());
        }
        return allies;
    }
    public boolean friendlyWith(int empId) {
        if (empId == id) return true;
        if (empId == Empire.NULL_ID) return false;

        EmpireView v = viewForEmpire(empId);
        return v == null ? false : v.embassy().isFriend();
    }
    public boolean pactWith(int empId) {
        if (empId == id) return true;
        if (empId == Empire.NULL_ID) return false;

        EmpireView v = viewForEmpire(empId);
        return v == null ? false : v.embassy().pact();
    }
    public boolean alliedWith(int empId) {
        if (empId == id) return true;
        if (empId == Empire.NULL_ID) return false;

        EmpireView v = viewForEmpire(empId);
        return v == null ? false : v.embassy().alliance() || v.embassy().unity();
    }
    public boolean unityWith(int empId) {
        if (empId == id) return true;
        if (empId == Empire.NULL_ID) return false;

        EmpireView v = viewForEmpire(empId);
        return v == null ? false : v.embassy().unity();
    }
    public boolean tradingWith(Empire c) {
        if (c == this) return true;
        if (c == null) return false;
        if (c.extinct) return false;

        EmpireView v = viewForEmpire(c);
        return v == null ? false : v.trade().active();
    }
    public boolean aggressiveWith(int empId) {
        if (empId == id) return false;
        if (empId == Empire.NULL_ID) return false;

        EmpireView v = viewForEmpire(empId);
        if (v == null)
            return false;
        return v.embassy().canAttackWithoutPenalty();
    }
    public boolean aggressiveWith(Empire c, StarSystem s) {
        if (c == this) return false;
        if (c == null) return false;
        if (c.extinct) return true;

        EmpireView v = viewForEmpire(c);
        if (v == null)
            return true;
        return v.embassy().canAttackWithoutPenalty(s);
    }
    public boolean atWarWith(int empId) {
        if (empId == id) return false;
        if (empId == Empire.NULL_ID) return false;

        EmpireView v = viewForEmpire(empId);
        if (v == null)
            return false;
        return v.embassy().anyWar();
    }
    public boolean hasTradeWith(Empire c) {
        if (c == this) return false;
        if (c == null) return false;
        if (c.extinct) return false;

        EmpireView v = viewForEmpire(c);
        if (v == null)
            return false;
        return v.trade().active();
    }
    public int contactAge(Empire c) {
        if (c == this) return 0;
        if (c == null) return 0;
        if (c.extinct) return 0;

        EmpireView v = viewForEmpire(c);
        if (v == null)
            return 0;
        return v.embassy().contactAge();
    }
    public List<StarSystem> systemsNeedingTransports(int minTransport) {
        List<StarSystem> systems = new ArrayList<>();
        for (StarSystem sys: colonizedSystems) {
            if (sys.colony().inRebellion() || (sv.popNeeded(sys.id) >= minTransport) )
                systems.add(sys);
        }
        return systems;
    }
    public List<StarSystem> systemsInShipRange(Empire c) {
        // returns list of systems in ship range
        // if c provided, restricts list to that owner
        Galaxy gal = galaxy();
        List<StarSystem> systems = new ArrayList<>();
        for (int n=0;n>sv.count();n++) {
            StarSystem sys = gal.system(n);
            if (sv.inShipRange(sys.id)) {
                if ((c == null) || (sv.empire(sys.id) == c))
                    systems.add(sys);
            }
        }
        return systems;
    }
    public List<StarSystem> systemsSparingTransports(int minTransport) {
        List<StarSystem> systems = new ArrayList<>();
        for (StarSystem sys: colonizedSystems) {
            if (!sys.colony().inRebellion() && sv.maxPopToGive(sys.id) >= minTransport )
                systems.add(sys);
        }
        return systems;
    }
    public List<StarSystem> allColonizedSystems() {
        return colonizedSystems;
    }
    public List<StarSystem> allySystems() {
        Galaxy gal = galaxy();
        List<StarSystem> systems = new ArrayList<>();
        for (int i=0;i<sv.count();i++) {
            if (alliedWith(sv.empId(i)))
                systems.add(gal.system(i));
        }
            return systems;
    }
    public StarSystem colonyNearestToSystem(StarSystem sys) {
        List<StarSystem> colonies = new ArrayList<>(allColonizedSystems());
        colonies.remove(sys);
        if (colonies.isEmpty())
            return null;

        StarSystem.TARGET_SYSTEM = sys;
        Collections.sort(colonies, StarSystem.DISTANCE_TO_TARGET_SYSTEM);
        return colonies.get(0);
    }
    public int alliedColonyNearestToSystem(StarSystem sys, float speed) {
        List<StarSystem> colonies = allySystems();
        colonies.remove(sys);

        // build list of allied systems closest to sys, in travel turns (not distance)
        List<StarSystem> closestSystems = new ArrayList<>();
        int minTurns = Integer.MAX_VALUE;
        for (StarSystem colony: colonies) {
            int turns = (int) Math.ceil(colony.travelTimeTo(sys, speed));
            if (turns < minTurns) {
                closestSystems.clear();
                closestSystems.add(colony);
                minTurns = turns;
            }
            else if (turns == minTurns)
                closestSystems.add(colony);
        }
        if (closestSystems.isEmpty())
            return StarSystem.NULL_ID;
        if (closestSystems.size() == 1)
            return closestSystems.get(0).id;
       
        // if there is more than one system within the minimum travel turns, 
        // choose the one closest, by distance
        StarSystem.TARGET_SYSTEM = sys;
        Collections.sort(closestSystems, StarSystem.DISTANCE_TO_TARGET_SYSTEM);
        return closestSystems.get(0).id;
    }
    public int optimalStagingPoint(StarSystem target, float speed) {
        List<StarSystem> colonies = allySystems();
        colonies.remove(target);

        // build a list of allied systems from sys that take the fewer travel turns
        // from that list, return the one with the greatest range
        // the idea is to try and keep the staging point outside of enemy sensor range
        // without hurting the travel time to the target
        List<StarSystem> closestSystems = new ArrayList<>();
        int minTurns = Integer.MAX_VALUE;
        for (StarSystem stagingPoint: colonies) {
            int turns = (int) Math.ceil(stagingPoint.travelTimeTo(target, speed));
            if (turns < minTurns) {
                closestSystems.clear();
                closestSystems.add(stagingPoint);
                minTurns = turns;
            }
            else if (turns == minTurns)
                closestSystems.add(stagingPoint);
        }
        if (closestSystems.isEmpty())
            return StarSystem.NULL_ID;
        
        if (closestSystems.size() == 1)
            return closestSystems.get(0).id;
        
        Empire targetEmpire = target.empire();
        if (targetEmpire == null) 
            return alliedColonyNearestToSystem(target, speed);
        
        float maxDistance = Float.MIN_VALUE;
        StarSystem bestStagingPoint = null;
        for (StarSystem stage: closestSystems) {
            float dist = targetEmpire.sv.distance(stage.id);
            if (dist > maxDistance) {
                maxDistance = dist;
                bestStagingPoint = stage;
            }
        }
        return bestStagingPoint == null ? StarSystem.NULL_ID : bestStagingPoint.id;
    }
    public List<StarSystem> systemsForCiv(Empire emp) {
        Galaxy gal = galaxy();
        List<StarSystem> systems = new ArrayList<>();
        for (int i=0;i<sv.count();i++) {
            if (sv.empire(i) == emp) 
                systems.add(gal.system(i));
        }
        return systems;
    }
    public int numSystemsForCiv(Empire emp) {
        int num = 0;
        for (int n=0;n<sv.count();n++) {
            if (sv.empire(n) == emp)
                num++;
        }
        return num;
    }
    public int numColonizedSystems() {
        return colonizedSystems.size();
    }
    public List<ShipFleet> fleetsForEmpire(Empire c) {
        List<ShipFleet> fleets2 = new ArrayList<>();
        for (Ship sh: visibleShips()) {
            if ((sh.empId() == c.id) && sh instanceof ShipFleet)
                fleets2.add((ShipFleet) sh);
        }
        return fleets2;
    }
    public boolean anyUnexploredSystems() {
        for (int n=0;n<sv.count(); n++) {
            if (!sv.isScouted(n))
                return true;
        }
        return false;
    }
    public List<StarSystem> unexploredSystems() {
        Galaxy gal = galaxy();
        List<StarSystem> systems = new ArrayList<>();
        for (int n=0;n<sv.count(); n++) {
            if (!sv.isScouted(n))
                systems.add(gal.system(n));
        }
        return systems;
    }
    public List<StarSystem> uncolonizedPlanetsInShipRange(int bestType) {
        Galaxy gal = galaxy();
        List<StarSystem> systems = new ArrayList<>();
        for (int i=0;i<sv.count();i++) {
            StarSystem sys = gal.system(i);
            if (sv.isScouted(i) && sv.inShipRange(i) && canColonize(sys.planet()))
                systems.add(sys);
        }
        return systems;
    }
    public PlanetType minUncolonizedPlanetTypeInShipRange(boolean checkHabitable) {
        // of all uncolonized planets in range that we can colonize
        // find the most hostile type... this guides the colony ship design
        PlanetType minType = PlanetType.keyed(PlanetType.TERRAN);
        for (int n=0;n<sv.count();n++) {
            if (sv.isScouted(n) && sv.inShipRange(n) && !sv.isColonized(n)) {
                PlanetType pType = sv.planetType(n);
                if (!checkHabitable || canColonize(pType)) {
                    if (pType.hostility() > minType.hostility())
                        minType = pType;
                }
            }
        }
        return minType;
    }
    public boolean knowsAllActiveEmpires() {
        for (Empire e: galaxy().activeEmpires()) {
            if (this != e) {
                if (!knowsOf(e))
                    return false;
            }
        }
        return true;
    }
    public boolean hasContacted(Empire e) {
        EmpireView ev = this.viewForEmpire(e);
        return (ev != null) && ev.embassy().contact();
    }
    public boolean knowsOf(Empire e) {
        if (e == null)
            return false;
        if (e == this)
            return true;
        if (hasContacted(e))
            return true;
        for (Empire emp : contactedEmpires()) {
            if (emp.hasContacted(e))
                return true;
        }
        return false;
    }
    public boolean hasContacted(int empId) {
        EmpireView ev = this.viewForEmpire(empId);
        return (ev != null) && ev.embassy().contact();
    }
    public boolean knowsOf(int empId) {
        if (empId == Empire.NULL_ID)
            return false;
        if (empId == id)
            return true;
        if (hasContacted(empId))
            return true;
        for (Empire emp : contactedEmpires()) {
            if (emp.hasContacted(empId))
                return true;
        }
        return false;
    }
    public List<ShipFleet> fleetsTargetingSystem(StarSystem target) {
        List<ShipFleet> fleets1 = new ArrayList<>();
        for (ShipFleet fl : fleets1) {
            if (fl.inTransit() && (fl.destSysId() == target.id))
                fleets1.add(fl);
            else if (!fl.inTransit() && (fl.sysId() == target.id))
                fleets1.add(fl);
        }
        return fleets1;
    }
    public void scrapExcessBases(StarSystem sys, int max) {
        if (sv.empire(sys.id) == this) {
            Colony col = sys.colony();
            if (col.defense().bases() > max) {
                log("civScrapBases  bases:", str(col.defense().bases()), " max: ", str(max), " cost: ", str(tech.newMissileBaseCost()));
                totalReserve += ((col.defense().bases() - max) * tech.newMissileBaseCost() / 4);
                col.defense().bases(max);
                //ai().setColonyAllocations(col);
                sv.refreshFullScan(sys.id);
            }
        }
    }
    public float bestEnemyShieldLevel() {
        float best = 0;
        for (EmpireView v : empireViews()) {
            if (v != null)
                best = max(best, v.spies().tech().maxDeflectorShieldLevel());
        }
        return best;
    }
    public float bestEnemyPlanetaryShieldLevel() {
        float best = 0;
        for (EmpireView v : empireViews()) {
            if (v != null) {
                float shieldLevel = v.spies().tech().maxDeflectorShieldLevel() + v.spies().tech().maxPlanetaryShieldLevel();
                best = max(best, shieldLevel);
            }
        }
        return best;
    }
    public void addToTreasury(float amt) {
        totalReserve += amt;
    }
    public void addReserve(float amt) {
        addToTreasury(amt/2);
    }
    public void stealTech(String id) {
        tech().learnTech(id);
        log("Tech: "+tech(id).name(), " stolen");
    }
    public void learnTech(String techId) {
        boolean newTech = tech().learnTech(techId);
        if (newTech && isPlayer()) {
            log("Tech: ", techId, " researched");
            DiscoverTechNotification.create(techId);
        }
        // share techs with New Republic allies
        for (EmpireView v: empireViews) {
            if ((v != null) && v.embassy().unity())
                v.empire().tech().acquireTechThroughTrade(techId, id);
        }
    }
    /*
    public void acquireTechThroughTrade(String techId, int empId) {
        tech().acquireTechThroughTrade(techId, empId);
    }
*/
    public void plunderTech(Tech t, StarSystem s, Empire emp) {
        boolean newTech = tech().learnTech(t.id);
        if (newTech && isPlayer()) {
            log("Tech: ", t.name(), " plundered from: ", s.name());
            PlunderTechNotification.create(t.id, s.id, emp.id);
        }
    }
    public void plunderShipTech(Tech t, int empId) {
        boolean newTech = tech().learnTech(t.id);
        if (newTech && isPlayer()) {
            log("Ship tech: ", t.name(), " plundered ");
            PlunderShipTechNotification.create(t.id, empId);
        }
    }
    public void plunderAncientTech(StarSystem s) {
        boolean isOrion = s.planet().isOrionArtifact();
        int numTechs = s.planet().bonusTechs();
        int levelDiff = isOrion ? 25: 10;
        int minLevel = isOrion ? 20: 1;
        s.planet().plunderBonusTech();
        
        for (int i=0;i<numTechs;i++) {
            Tech t = tech().randomUnknownTech(minLevel, levelDiff);
            if (t == null) // if none found, then break out of loop
                break;
            boolean newTech = tech().learnTech(t.id);
            if (newTech && isPlayer()) {
                log("Tech: ", t.name(), " discovered on: ", s.name());
                PlunderTechNotification.create(t.id, s.id, -1);
            }
        }
    }
    public int maxRobotControls() {
        return tech.baseRobotControls() + race().robotControlsAdj();
    }
    public int baseRobotControls() {
        return TechRoboticControls.BASE_ROBOT_CONTROLS + race().robotControlsAdj();
    }
    public float workerProductivity() {
        float bookFormula = ((tech.planetology().techLevel() * 3) + 50) / 100;
        return bookFormula * race().workerProductivityMod();
    }
    public float totalIncome()                { return netTradeIncome() + totalPlanetaryIncome(); }
    public float netIncome()                  { return totalIncome() - totalShipMaintenanceCost() - totalStargateCost(); }
    public float empireTaxRevenue()           { return totalTaxablePlanetaryProduction() * empireTaxPct() / 2; }
    public float empireInternalSecurityCost() { return totalTaxablePlanetaryProduction() * internalSecurityCostPct(); }
    public float empireExternalSpyingCost()   { return totalTaxablePlanetaryProduction() * totalSpyCostPct(); }
    
    public boolean incrementEmpireTaxLevel()  { return empireTaxLevel(empireTaxLevel+1); }
    public boolean decrementEmpireTaxLevel()  { return empireTaxLevel(empireTaxLevel-1); }
    public float empireTaxPct()               { return (float) empireTaxLevel / 100; }
    public float maxEmpireTaxPct()            { return (float) maxEmpireTaxLevel()/100; }
    public int empireTaxLevel()               { return empireTaxLevel; }
    public int maxEmpireTaxLevel()            { return 20; }
    public boolean empireTaxLevel(int i)      {
        int prevLevel = empireTaxLevel;
        empireTaxLevel = bounds(0,i,maxEmpireTaxLevel());
        return empireTaxLevel != prevLevel;
    }
    public boolean hasTrade() {
        for (EmpireView v : empireViews()) {
            if ((v != null) && (v.trade().level() > 0))
                return true;
        }
        return false;
    }
    public float netTradeIncome() {
        float trade = totalTradeIncome();
        return trade <= 0 ? trade : trade * (1-tradePiracyRate);
    }
    public float totalTradeIncome() {
        float sum = 0;
        for (EmpireView v : empireViews()) {
            if (v != null)
                sum += v.trade().profit();
        }
        return sum;
    }
    public int totalTradeTreaties() {
        int sum = 0;
        for (EmpireView v : empireViews()) {
            if (v != null)
                sum += v.trade().level();
        }
        return sum;
    }
    public float totalFleetSize(Empire emp) {
        if (this == emp)
            return totalFleetSize();

        float spyPts = 0;
        FleetView fv = viewForEmpire(emp).spies().fleetView();
        if (!fv.noReport()) {
            spyPts += (fv.small()*ShipDesign.hullPoints(ShipDesign.SMALL));
            spyPts += (fv.medium()*ShipDesign.hullPoints(ShipDesign.MEDIUM));
            spyPts += (fv.large()*ShipDesign.hullPoints(ShipDesign.LARGE));
            spyPts += (fv.huge()*ShipDesign.hullPoints(ShipDesign.HUGE));
        }

        float visiblePts = 0;
        for (Ship sh : visibleShips()) {
            if ((sh.empId() == emp.id) && sh instanceof ShipFleet) {
                ShipFleet sh1 = (ShipFleet) sh;
                visiblePts += sh1.hullPoints();
            }
        }
        return max(spyPts, visiblePts);
    }
    public Float totalFleetSize() {
        float pts = 0;
        List<ShipFleet> fleets = galaxy().ships.allFleets(id);
        for (ShipFleet fl: fleets) 
            pts += fl.hullPoints();
        return pts;
    }
    public Float totalArmedFleetSize() {
        float pts = 0;
        int[] counts = galaxy().ships.shipDesignCounts(id);
        for (int i=0;i<ShipDesignLab.MAX_DESIGNS; i++) {
            ShipDesign d = shipLab().design(i);
            if (d.active() && d.isArmed() && !d.isColonyShip()) 
                pts += (counts[i] *d.hullPoints());
        }
        return pts;
    }
    public float totalFleetCost() {
        float pts = 0;
        List<ShipFleet> fleets = galaxy().ships.allFleets(id);
        for (ShipFleet fl: fleets)
            pts += fl.bcValue();
        return pts;
    }
    public Float totalPlanetaryPopulation() {
        float totalPop = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems)
            totalPop += sys.colony().population();
        return totalPop;
    }
    public float totalPlanetaryPopulation(Empire emp) {
        float totalPop = 0;
        if (emp == this) {
            List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
            for (StarSystem sys: systems)
                totalPop += sys.colony().population();
        }
        else {
            for (int n=0; n<sv.count(); n++) {
                if ((sv.empire(n) == emp))
                    totalPop += sv.population(n);
            }
        }
        return totalPop;
    }
    public float totalPlanetaryIncome() {
        float totalProductionBC = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems)
            totalProductionBC += sys.colony().totalIncome();

        return totalProductionBC;
    }
    public float totalTaxablePlanetaryProduction() {
        float totalProductionBC = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems) {
            Colony col = sys.colony();
            if (!col.embargoed())
                totalProductionBC += col.production();
        }
        return totalProductionBC;
    }
    public Float totalPlanetaryProduction() {
        float totalProductionBC = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems)
            totalProductionBC += sys.colony().production();
        return totalProductionBC;
    }
    public float totalPlanetaryProduction(Empire emp) {
        if (emp == this)
            return totalPlanetaryProduction();

        float totalProductionBC = 0;
        for (int i=0; i<sv.count(); i++) {
            if ((sv.empire(i) == emp) && (sv.colony(i) != null))
                totalProductionBC += sv.colony(i).production();
        }
        return totalProductionBC;
    }
    public float totalMissileBaseCostPct() {
        float empireBC = totalPlanetaryProduction();
        float totalCostBC = 0;
        List<StarSystem> allSystems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: allSystems)
            totalCostBC += sys.colony().defense().missileBaseMaintenanceCost();
        return totalCostBC / empireBC;
    }
    public float totalStargateCostPct() {
        float empireBC = totalPlanetaryProduction();
        float totalCostBC = 0;
        List<StarSystem> allSystems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: allSystems)
            totalCostBC += sys.colony().shipyard().stargateMaintenanceCost();
        return totalCostBC / empireBC;
    }
    public float totalPlanetaryIndustrialSpending() {
        float totalIndustrialSpendingBC = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems)
            totalIndustrialSpendingBC += (sys.colony().pct(Colony.INDUSTRY) * sys.colony().totalIncome());
        return totalIndustrialSpendingBC;
    }
    public float totalPlanetaryResearch() {
        float totalResearchBC = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems)
            totalResearchBC += sys.colony().research().totalBCForEmpire(); // some research BC may stay with colony
        return totalResearchBC;
    }
    public float totalEmpireResearch(float totalRp) {
        float total = 0.0f;
        TechTree t = tech();
        total += t.computer().currentResearch(totalRp);
        total += t.construction().currentResearch(totalRp);
        total += t.forceField().currentResearch(totalRp);
        total += t.planetology().currentResearch(totalRp);
        total += t.propulsion().currentResearch(totalRp);
        total += t.weapon().currentResearch(totalRp);
        return total;
    }
    public float totalPlanetaryResearchSpending() {
        float totalResearchBC = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems)
            totalResearchBC += sys.colony().research().totalSpending();
        return totalResearchBC;
    }
    public void allocateReserve(Colony col, int amount) {
        float amt = min(totalReserve, amount);
        totalReserve -= amt;
        col.adjustReserveIncome(amt);
    }
    public void goExtinct() {
        // prevent double notifications
        if (extinct)
            return;
        
        if (lastAttacker instanceof Empire)
            GenocideIncident.create(this, (Empire) lastAttacker);
        GNNGenocideNotice.create(this, lastAttacker);

        extinct = true;
        // iterate over list copy to avoid comodification
        List<ShipFleet> fleets = galaxy().ships.allFleets(id);
        for (ShipFleet fl: fleets) {
            log("disband#1 fleet: ", fl.toString());
            fl.disband();
        }

        for (EmpireView v : empireViews()) {
            if (v != null)
                v.embassy().removeContact();
        }

        Galaxy g = galaxy();
        if (g.council().finalWar()) {
            g.council().removeEmpire(this);
        }
        else { 
            List<Empire> activeEmpires = galaxy().activeEmpires();
            // if only one empire is left...
            if (activeEmpires.size() == 1) {
                if (isPlayer())
                    session().status().loseMilitary();
                else
                    session().status().winMilitary();
            }
            else {
                boolean allAlliedWithPlayer = true;
                int playerId = player().id;
                for (Empire emp: activeEmpires) {
                    if (!emp.alliedWith(playerId))
                        allAlliedWithPlayer = false;
                }
                if (allAlliedWithPlayer) 
                    session().status().winMilitaryAlliance();
            }
        }            
        status.assessTurn();
    }
    public ShipView shipViewFor(ShipDesign d ) {
        if (d == null)
            return null;

        if (d.empire() == this)
            return shipLab.shipViewFor(d);

        EmpireView cv = viewForEmpire(d.empire());
        if (cv != null)
            return cv.spies().shipViewFor(d);

        return null;
    }
    private void detectFleet(ShipFleet fl) {
        EmpireView cv = viewForEmpire(fl.empire());
        if (cv == null)
            return;

        int[] visible = fl.visibleShips(id);
        for (int i=0;i<visible.length;i++) {
            if (visible[i] > 0)
                cv.spies().detectShip(fl.empire().shipLab().design(i));
        }
    }
    public void encounterFleet(ShipFleet fl) {
        if (fl == null)
            return;
        EmpireView cv = viewForEmpire(fl.empire().id);
        if (cv == null)
            return;

        int[] visible = fl.visibleShips(id);
        for (int i=0;i<visible.length;i++) {
            if (visible[i] > 0)
                cv.spies().encounterShip(fl.empire().shipLab().design(i));
        }
    }
    public void scanFleet(ShipFleet fl) {
        EmpireView cv = viewForEmpire(fl.empire());
        if (cv == null)
            return;

        int[] visible = fl.visibleShips(id);
        for (int i=0;i<visible.length;i++) {
            if (visible[i] > 0)
                cv.spies().scanShip(fl.empire().shipLab().design(i));
        }
    }
    public void scanDesign(ShipDesign st, Empire emp) {
        EmpireView cv = viewForEmpire(emp);
        if (cv != null)
            cv.spies().scanShip(st);
    }
    public List<StarSystem> orderedColonies() {
        List<StarSystem> list = new ArrayList<>(allColonizedSystems());
        Collections.sort(list, IMappedObject.MAP_ORDER);
        return list;
    }
    public List<StarSystem> orderedTransportTargetSystems() {
        // we can only send transports to scouted, colonized
        // systems in range, with planets that we have the
        // technology to colonize
        Galaxy gal = galaxy();
        List<StarSystem> list = new ArrayList<>();
        for (int i=0; i<sv.count();i++) {
            StarSystem sys = gal.system(i);
            if (sv.inShipRange(i)
            && sv.isScouted(i)
            && sv.isColonized(i)
            && tech().canColonize(sys.planet()))
                list.add(sys);
        }
        Collections.sort(list, IMappedObject.MAP_ORDER);
        return list;
    }
    public List<StarSystem> orderedFleetTargetSystems(ShipFleet fl) {
        float range = fl.range();
        Galaxy gal = galaxy();
        List<StarSystem> list = new ArrayList<>();
        for (int n=0; n<sv.count();n++) {
            if (sv.withinRange(n, range))
                list.add(gal.system(n));
        }
        Collections.sort(list, IMappedObject.MAP_ORDER);
        return list;
    }
    public List<StarSystem> orderedShipConstructingColonies() {
        List<StarSystem> list = new ArrayList<>(shipBuildingSystems);
        Collections.sort(list, IMappedObject.MAP_ORDER);
        return list;
    }
    public List<ShipFleet> orderedFleets() {
        List<ShipFleet> list = new ArrayList<>();
        List<ShipFleet> fleets = galaxy().ships.allFleets(id);
        for (ShipFleet fl: fleets) {
            if (!fl.isEmpty())
                list.add(fl);
        }
        Collections.sort(list, IMappedObject.MAP_ORDER);
        return list;
    }
    public List<ShipFleet> orderedEnemyFleets() {
        List<ShipFleet> list = new ArrayList<>(enemyFleets());
        Collections.sort(list, IMappedObject.MAP_ORDER);
        return list;
    }
    public List<StarSystem> orderedUnderAttackSystems() {
        List<StarSystem> list = new ArrayList<>();
        Galaxy g = galaxy();
        Empire pl = player();
        for (StarSystem sys: pl.allColonizedSystems()) {
            if (sys.enemyShipsInOrbit(pl))
                list.add(sys);
        }
        if (knowShipETA) {
            List<Transport> ships = g.transports();
            for (Transport ship: ships) {
                if (ship.empId() != pl.id) { // don't care about player ships
                    StarSystem sys = g.system(ship.destSysId());
                    // don't care about ships going to already-added systems or AI systems
                    if (!list.contains(sys) && (sys.empire() == pl)) { 
                        Empire emp = g.empire(ship.empId());
                        // add if incoming fleet is hostile to player
                        if (emp.aggressiveWith(pl.id))
                            list.add(sys);
                    }
                }
            }
            for (ShipFleet ship: g.ships.inTransitFleets()) {
                if (ship.empId() != pl.id) { // don't care about player ships
                    StarSystem sys = g.system(ship.destSysId());
                    // don't care about ships going to already-added systems or AI systems
                    if (!list.contains(sys) && (sys.empire() == pl)) { 
                        Empire emp = g.empire(ship.empId());
                        // add if incoming fleet is hostile to player
                        if (emp.aggressiveWith(pl.id))
                            list.add(sys);
                    }
                }
            }
        }
        return list;
    }
    public static Set<Empire> allContacts(Empire e1, Empire e2) {
        Set<Empire> contacts = new HashSet<>();
        contacts.addAll(e1.contactedEmpires());
        contacts.addAll(e2.contactedEmpires());
        contacts.remove(e1);
        contacts.remove(e2);
        return contacts;
    }
    public static Comparator<Empire> TOTAL_POPULATION = (Empire o1, Empire o2) -> o2.totalPlanetaryPopulation().compareTo(o1.totalPlanetaryPopulation());
    public static Comparator<Empire> TOTAL_PRODUCTION = (Empire o1, Empire o2) -> o2.totalPlanetaryProduction().compareTo(o1.totalPlanetaryProduction());
    public static Comparator<Empire> AVG_TECH_LEVEL   = (Empire o1, Empire o2) -> o2.tech.avgTechLevel().compareTo(o1.tech.avgTechLevel());
    public static Comparator<Empire> TOTAL_FLEET_SIZE = (Empire o1, Empire o2) -> o2.totalFleetSize().compareTo(o1.totalFleetSize());
}
