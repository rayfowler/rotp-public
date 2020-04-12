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

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rotp.model.colony.Colony;
import rotp.model.combat.ShipCombatManager;
import rotp.model.empires.Empire;
import rotp.model.empires.GalacticCouncil;
import rotp.model.empires.Race;
import rotp.model.events.RandomEvents;
import rotp.model.galaxy.GalaxyShape.EmpireSystem;
import rotp.model.game.GameSession;
import rotp.ui.NoticeMessage;
import rotp.ui.notifications.AdviceNotification;
import rotp.ui.notifications.BombardSystemNotification;
import rotp.util.Base;

public class Galaxy implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public static Galaxy current()   { return GameSession.instance().galaxy(); }
    private static final float TIME_PER_TURN = 1;

    private float currentTime = 0;
    private final GalacticCouncil council = new GalacticCouncil();
    private final RandomEvents events = new RandomEvents();
    public final Ships ships = new Ships();
    private final StarSystem[] starSystems;
    private List<Nebula> nebulas;
    private final Empire[] empires;
    private final List<String> adviceGiven = new ArrayList<>();
    private final List<Transport> transports = new ArrayList<>();

    private Empire playerEmpire;
    private Empire orionEmpire;
    private final int widthLY;
    private final int heightLY;
    private float maxScaleAdj = 1.0f;
    public int systemCount = 0;

    private transient ShipCombatManager shipCombat = new ShipCombatManager();
    private transient Map<String, List<String>> raceSystemNames = new HashMap<>();
    private transient Map<String, Integer> raceSystemCtr = new HashMap<>();

    public int beginningYear()               { return player().race().startingYear; }
    public float currentTime()               { return currentTime; }
    public GalacticCouncil council()         { return council; }
    public RandomEvents events()       		 { return events; }
    public List<Nebula> nebulas()            { return nebulas; }
    public Empire[] empires()                { return empires; }
    public int maxNumStarSystems()           { return starSystems.length; }
    public int numStarSystems()              { return systemCount; }
    public StarSystem[] starSystems()        { return starSystems; }
    public void addStarSystem(StarSystem s)  {
        starSystems[systemCount] = s;
        systemCount++;
    }
    public StarSystem system(int i)          { return (i < 0) || (i >= numStarSystems()) ? null : starSystems[i]; }

    public int width()                       { return widthLY; }
    public int height()                      { return heightLY; }
    public float maxScaleAdj()              { return maxScaleAdj; }

    public void player(Empire d)             { playerEmpire = d; }
    @Override
    public Empire player()                   { return playerEmpire; }
    @Override
    public boolean isPlayer(Empire d)        { return playerEmpire == d; }
    public void orionEmpire(Empire d)        { orionEmpire = d; }
    public Empire orionEmpire()              { return orionEmpire; }
    public void initNebulas(int size)        { nebulas = new ArrayList<>(size); }
    public ShipCombatManager shipCombat() {
        if (shipCombat == null)
            shipCombat = new ShipCombatManager();
        return shipCombat;
    }
    private Map<String, List<String>> raceSystemNames() {
        if (raceSystemNames == null)
            raceSystemNames = new HashMap<>();
        return raceSystemNames;
    }
    private Map<String, Integer> raceSystemCtr() {
        if (raceSystemCtr == null)
            raceSystemCtr = new HashMap<>();
        return raceSystemCtr;
    }
    public Galaxy(GalaxyShape sh) {
        widthLY = sh.width();
        heightLY = sh.height();
        maxScaleAdj = sh.maxScaleAdj();
        starSystems = new StarSystem[sh.totalStarSystems()];
        empires = new Empire[options().selectedNumberOpponents()+1];
    }
    public void advanceTime() { currentTime += TIME_PER_TURN; }
    public void addNebula(GalaxyShape shape) {
        // each nebula creates a buffered image for display
        // after we have created 5 nebulas, start cloning
        // existing nebulas (add their images) when making
        // new nebulas
        int MAX_UNIQUE_NEBULAS = 8;

        float buffer = 2.0f;
        float rightBuffer = buffer+6;
        float bottomBuffer = buffer+6;

        Point.Float pt = new Point.Float();
        shape.setRandom(pt);
        if (shape.valid(pt)) {
            Nebula neb;
            if (nebulas.size() < MAX_UNIQUE_NEBULAS)
                neb = new Nebula(true);
            else
                neb = random(nebulas).copy();
            neb.setXY(pt.x, pt.y);
            boolean containsSystem = false;
            for (EmpireSystem sys : shape.empSystems) {
                if (sys.inNebula(neb))
                    containsSystem = true;
            }
            if (!containsSystem)
                nebulas.add(neb);
            //log("Adding nebula: ", fmt(neb.x(),2), "@", fmt(neb.y(),2), "    ", fmt(neb.width(),2), "x", fmt(neb.height(),2));
        }
    }
    public List<StarSystem> systemsNamed(String name) {
        List<StarSystem> systems = new ArrayList<>();
        for (StarSystem sys: starSystems) {
            if (sys.name().equals(name))
                systems.add(sys);
        }
        return systems;
    }
    private float randomLocation(float max, float leftBuff, float rightBuff) {
        return leftBuff + (random() * (max-leftBuff-rightBuff));
    }
    public void addAdviceGiven(String key) {
        if (!adviceGiven.contains(key))
            adviceGiven.add(key);
    }
    public boolean adviceAlreadyGiven(String key) {
        return adviceGiven.contains(key);
    }
    public void giveAdvice(String key) {
        if (!adviceAlreadyGiven(key)) {
            addAdviceGiven(key);
            AdviceNotification.create(key);
        }
    }
    public void giveAdvice(String key, String s1) {
        if (!adviceAlreadyGiven(key)) {
            addAdviceGiven(key);
            AdviceNotification.create(key, s1);
        }
    }
    public void giveAdvice(String key, String s1, String s2) {
        if (!adviceAlreadyGiven(key)) {
            addAdviceGiven(key);
            AdviceNotification.create(key, s1, s2);
        }
    }
    public void giveAdvice(String key, String s1, String s2, String s3) {
        if (!adviceAlreadyGiven(key)) {
            addAdviceGiven(key);
            AdviceNotification.create(key, s1, s2, s3);
        }
    }
    public boolean inNebula (float x, float y) {
        for (Nebula neb: nebulas) {
            if (neb.contains(x,y))
                return true;
        }
        return false;
    }
    public void preNextTurn() {
        NoticeMessage.resetSubstatus(text("TURN_LAUNCHING_FLEETS"));
        for (StarSystem sys: starSystems)
            sys.launchTransports();
        ships.disembarkFleets();
        ships.reloadBombs();
    }
    public void postNextTurn1() {
        // check ship combat & invasions at each system
        NoticeMessage.resetSubstatus(text("TURN_SHIP_COMBAT"));
        Galaxy gal = galaxy();
        for (int i=0; i<gal.numStarSystems(); i++) {
            gal.system(i).resolveAnyShipConflict();
            gal.ships.disembarkRalliedFleets();
        }

        // update system views
        for (Empire e: empires)
            e.refreshViews();
    }
    public void postNextTurn2() {
        // check bombardment
        NoticeMessage.resetSubstatus(text("TURN_BOMBARDMENT"));
        checkForPlanetaryBombardment();

        // land transports
        Galaxy gal = galaxy();
        NoticeMessage.resetSubstatus(text("TURN_TRANSPORTS"));
        for (int i=0; i<gal.numStarSystems(); i++)
            gal.system(i).resolvePendingTransports();

        NoticeMessage.resetSubstatus(text("TURN_COLONIES"));
        // after bombardments, check for any possible colonizations
        checkForColonization();

        NoticeMessage.resetSubstatus(text("TURN_SPIES"));
        for (Empire e: empires)
            e.postNextTurn();

        NoticeMessage.resetSubstatus(text("TURN_REBELLION"));
        for (Empire e: empires)
            e.checkForRebellionSpread();

        NoticeMessage.resetSubstatus(text("TURN_COUNCIL"));
        council().checkIfDisband();
    }
    private void checkForPlanetaryBombardment() {
        for (StarSystem sys: starSystems) {
            Empire home = sys.empire();
            List<ShipFleet> fleets = sys.orbitingFleets();
            if ((home != null) && !fleets.isEmpty()){
                for (ShipFleet fl: fleets) {
                    if (fl.inOrbit() && !fl.retreating()) {
                        Empire fleetEmp = fl.empire();
                        // cannot bombard if alliance or unity
                        if (fleetEmp.aggressiveWith(home.id)) {
                            if (fleetEmp.ai().promptForBombardment(sys, fl))
                                BombardSystemNotification.create(sys.id, fl);
                        }
                    }                
                }
            }
        }
    }
    public void checkForColonization() {
        for (StarSystem sys: starSystems) {
            Empire home = sys.empire();
            List<ShipFleet> fleets = sys.orbitingFleets();
            if ((home == null) && !fleets.isEmpty()){
                for (ShipFleet fl: fleets) 
                    fl.checkColonize();
            }
        }
    }
    public void assessTurn() {
        NoticeMessage.resetSubstatus(text("TURN_RESEARCH"));
        for (Empire e: empires)
            e.completeResearch();
        // everything that can have happened in the turn has happened
        // now it's time for empires to decide what to do about it
        // warn, praise, break treaties or declare war, generally
        NoticeMessage.resetSubstatus(text("TURN_ASSESS"));
        for (Empire e: empires)
            e.assessTurn();
        NoticeMessage.resetSubstatus(text("TURN_DIPLOMACY"));
        for (Empire e: empires)
            e.makeDiplomaticOffers();
        NoticeMessage.resetSubstatus(text("TURN_ACQUIRE_TECHS"));
        for (Empire e: empires)
            e.acquireTradedTechs();
    }
    public void makeNextTurnDecisions() {
        int num = empires.length;
        for (int i=0;i<num;i++) {
            NoticeMessage.setSubstatus(text("TURN_MAKE_DECISIONS"), (i+1), num);
            Empire emp = empires[i];
            emp.makeNextTurnDecisions();
        }
    }
    
    public void validate() {
       for (Empire emp: empires())
            emp.validate();
    }
    public int currentYear() {
        return beginningYear() + (int) currentTime;
    }
    public int numberTurns() { return (int) currentTime; }
    public int currentTurn() { return (int) currentTime+1; }

    public Empire empire(int i)     {
        return (i < 0) || (i >= empires.length) ? null : empires[i];
    }
    public void addEmpire(Empire e) {
        empires[e.id] = e;
    }
    public Empire empireForRace(Race r) {
        for (Empire e: empires) {
            if (e.race() == r)
                return e;
        }
        return null;
    }
    public void startGame() {
        giveAdvice("MAIN_ADVISOR_SCOUT");
        session().processNotifications();
    }
    public void moveShipsInTransit() {
        // move transports
        List<Transport> arrivingTransports = new ArrayList<>();
        List<Transport> incoming = new ArrayList<>(transports);
        Collections.sort(incoming, Ship.ARRIVAL_TIME);
        for (Transport sh: incoming) {
            if (sh.arrivalTime() > currentTime)
                break;
            arrivingTransports.add(sh);
            sh.arrive();
        }
        transports.removeAll(arrivingTransports);
        
        //move fleets
        List<ShipFleet> incomingFleets = ships.inTransitFleets();
        Collections.sort(incomingFleets, Ship.ARRIVAL_TIME);
        for (ShipFleet sh: incomingFleets) {
            if (sh.arrivalTime() > currentTime)
                break;
            galaxy().ships.arriveFleet(sh);
        }
    }
    public List<Transport> transports()       { return transports; }
    public void removeShipInTransit(Transport sh) { transports.remove(sh); }
    public void addShipInTransit(Transport sh)    { transports.add(sh); }
    public void nextEmpireTurns() {
        for (Empire e: empires) {
            if (!e.extinct())
                e.nextTurn();
        }
    }
    public int numColonizedSystems() {
        int num = 0;
        for (Empire e: empires) 
            num += e.numColonizedSystems();       
        return num;
    }
    public int friendlyPopApproachingSystem(StarSystem sys) {
        int pop = 0;
        Galaxy gal = galaxy();

        for (Transport tr: gal.transports()) {
            if (tr.empId() == sys.empire().id) {
                if (tr.destSysId() == sys.id)
                    pop += tr.size();
            }
        }
        for (int i=0; i<gal.numStarSystems(); i++) {
            StarSystem system = gal.system(i);
            if (system.planet().isColonized()) {
                Colony col = system.planet().colony();
                if ((col.empire() == sys.empire()) && col.transporting() && (col.transport().destSysId() == sys.id))
                    pop += col.inTransport();
            }
        }
        return pop;
    }
    public int enemyPopApproachingSystem(StarSystem sys) {
        int pop = 0;
        for (Transport sh: transports) {
            if ( (sh.destSysId() == sys.id)
            && (sh.empId() != sys.empire().id))
                pop += sh.size();
        }
        for (int i=0; i<numStarSystems(); i++) {
            StarSystem system = system(i);
            if (system.planet().isColonized()) {
                Colony col = system.planet().colony();
                if ((col.empire() != sys.empire()) && col.transporting() && (col.transport().destSysId() == sys.id))
                    pop += col.inTransport();
            }
        }
        return pop;
    }
    public int popApproachingSystem(StarSystem sys, Empire emp) {
        int pop = 0;

        for (Transport tr: transports()) {
            if ((tr.empire() == emp) && (tr.destSysId() == sys.id))
               pop += tr.size();
        }
        for (StarSystem system : emp.allColonizedSystems()) {
            Colony col = system.colony();
            if (col != null) {
                if (col.transporting() && (col.transport().destSysId() == sys.id))
                    pop += col.inTransport();
            }
        }
        return pop;
    }
    public int numEmpires()       { return empires.length; }
    public int numActiveEmpires() {
        int emps = 0;
        for (Empire e : empires) {
            if (!e.extinct())
                emps++;
        }
        return emps;
    }
    public List<Empire> activeEmpires() {
        List<Empire> emps = new ArrayList<>();
        for (Empire e : empires) {
            if (!e.extinct())
                emps.add(e);
        }
        return emps;
    }
    public List<StarSystem> systemsInRange(IMappedObject xyz, float radius) {
        List<StarSystem> systems = new ArrayList<>();
        for (int i=0;i<numStarSystems();i++) {
            StarSystem s = system(i);
            if (s.distanceTo(xyz) <= radius)
                systems.add(s);
        }
        return systems;
    }
    public String nextSystemName(String rId) {
        if (!raceSystemNames().containsKey(rId))
            loadRaceNames(rId, 0);

        List<String> remainingNames = raceSystemNames().get(rId);
        if (remainingNames.isEmpty()) {
            int nextSeq = raceSystemCtr().get(rId) + 1;
            loadRaceNames(rId, nextSeq);
            remainingNames = raceSystemNames().get(rId);
        }

        String nextName = remainingNames.remove(0);
        int seq = raceSystemCtr().get(rId);
        if (seq > 1)
            nextName = nextName + " " + Base.letter[seq];

        return nextName;
    }
    private void loadRaceNames(String rId, int i) {
        Race r = Race.keyed(rId);
        List<String> names = new ArrayList<>(r.systemNames);
        Collections.shuffle(names);
        raceSystemNames().put(rId, names);
        raceSystemCtr().put(rId, i);
    }
}
