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
package rotp.model.game;

import rotp.Rotp;
import rotp.model.empires.*;
import rotp.model.galaxy.*;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipManeuver;
import rotp.model.ships.ShipSpecial;
import rotp.model.ships.ShipWeapon;
import rotp.model.tech.Tech;
import rotp.model.tech.TechTree;
import rotp.ui.NoticeMessage;
import rotp.ui.RotPUI;
import rotp.ui.UserPreferences;
import rotp.ui.notifications.*;
import rotp.ui.sprites.FlightPathSprite;
import rotp.util.Base;

import java.io.*;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class GameSession implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public static final int CURRENT_SAVE_VERSION = 1;
    public static final String SAVEFILE_DIRECTORY = ".";
    public static final String SAVEFILE_EXTENSION = ".rotp";
    public static final String RECENT_SAVEFILE = "recent.rotp";
    public static final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final Object ONE_GAME_AT_A_TIME = new Object();
    private static GameSession instance = new GameSession();
    public static GameSession instance()  { return instance; }

    private static final int MINIMUM_NEXT_TURN_TIME = 500;
    private static Thread nextTurnThread;
    private static volatile boolean suspendNextTurn = false;
    private static final ThreadFactory minThreadFactory = GameSession.minThreadFactory();
    private static ExecutorService smallSphereService = Executors.newSingleThreadExecutor(minThreadFactory);

    private static HashMap<String,Object> vars;
    private static boolean performingTurn;
    private static final List<TurnNotification> notifications = new ArrayList<>();
    private static HashMap<StarSystem, List<String>> systemsToAllocate;
    private static List<StarSystem> systemsScouted;
    private static HashMap<ShipDesign, Integer> shipsConstructed;
    private static final List<GameAlert> alerts = new ArrayList<>();
    private static int viewedAlerts;

    private IGameOptions options;
    private Galaxy galaxy;
    private final GameStatus status = new GameStatus();
    private long id;
    private transient List<GameListener> gameListeners;
    public GameStatus status()                   { return status; }
    public long id()                             { return id; }
    public ExecutorService smallSphereService()  { return smallSphereService; }

    public List<GameListener> gameListeners() {
        if (gameListeners == null)
            gameListeners = new ArrayList<>();
        return gameListeners;
    }
    public void addGameListener(GameListener gameListener) {
        gameListeners().add(gameListener);
    }
    public void removeGameListener(GameListener gameListener) {
        gameListeners().remove(gameListener);
    }

    public void pauseNextTurnProcessing(String s)   {
        log("Pausing Next Turn: ", s);
        suspendNextTurn = true;
    }
    public void resumeNextTurnProcessing()  {
        log("Resuming Next Turn");
        suspendNextTurn = false;
    }
    public HashMap<ShipDesign, Integer> shipsConstructed() {
        if (shipsConstructed == null)
            shipsConstructed = new HashMap<>();
        return shipsConstructed;
    }
    public HashMap<StarSystem, List<String>> systemsToAllocate() {
        if (systemsToAllocate == null)
            systemsToAllocate = new HashMap<>();
        return systemsToAllocate;
    }
    public List<StarSystem> systemsScouted() {
        if (systemsScouted == null)
            systemsScouted = new ArrayList<>();
        return systemsScouted;
    }
    private List<TurnNotification> notifications() {
        return notifications;
    }
    private HashMap<String,Object> vars() {
        if (vars == null)
            vars = new HashMap<>();
        return vars;
    }
    public GameAlert currentAlert() {
        if (viewedAlerts >= alerts.size())
            return null;
        return alerts.get(viewedAlerts);
    }
    public int viewedAlerts()    { return viewedAlerts; }
    public int numAlerts()       { return alerts.size(); }
    public void addAlert(GameAlert a)  { alerts.add(a); }
    private void clearAlerts() {
        alerts.clear();
        viewedAlerts = 0;
    }
    public void dismissAlert() { viewedAlerts++; }

    public boolean performingTurn()      { return performingTurn; }
    @Override
    public IGameOptions options()        { return options; }
    public void options(IGameOptions o)  { options = o; }
    @Override
    public Galaxy galaxy()               { return galaxy; }
    public void galaxy(Galaxy g)         { galaxy = g; }

    public float populationBonus()      { return 1.0f; }
    public float propulsionBonus()      { return 1.0f; }
    public float damageBonus()          { return 1.0f; }
    public float researchBonus()        { return 1.0f; }
    public float researchMapSizeAdjustment() {
        float stars = galaxy().numStarSystems();
        int races = options().selectedNumberOpponents()+2;
        float targetRatio = 12.0f;
        return sqrt(stars/races/targetRatio);
    }
    public void addShipsConstructed(ShipDesign design, int newCount) {
        if (!design.active()) 
            throw new RuntimeException("Constructed an inactive ship design");
        
        if (shipsConstructed().isEmpty())
            addTurnNotification(new ShipConstructionNotification());

        int existingCount = shipsConstructed().containsKey(design) ? shipsConstructed().get(design) : 0;
        shipsConstructed().put(design, existingCount+newCount);
    }
    public void addSystemScouted(StarSystem sys) {
        systemsScouted().add(sys);
    }
    public void addSystemToAllocate(StarSystem sys, String reason) {
        log("Re-allocate: ", sys.name(), " :", reason);
        if (!systemsToAllocate().containsKey(sys))
            systemsToAllocate().put(sys, new ArrayList<>());

        if (!systemsToAllocate().get(sys).contains(reason))
            systemsToAllocate().get(sys).add(reason);
    }
    public boolean awaitingAllocation(StarSystem sys) {
        return systemsToAllocate().containsKey(sys);
    }
    public void addTurnNotification(TurnNotification notif) {
        notifications().add(notif);
    }
    public void removePendingNotification(String key) {
        List<TurnNotification> notifs = new ArrayList<>(notifications());
        for (TurnNotification notif: notifs) {
            if (notif.key().equals(key))
                notifications.remove(notif);
        }
            
    }
    public GameSession() {
        options(RulesetManager.current().defaultRuleset());
    }
    public void startGame() {
        stopCurrentGame();
        startExecutors();
        
        synchronized(ONE_GAME_AT_A_TIME) {
            id = (long) (Long.MAX_VALUE*random());
            GalaxyFactory.current().newGalaxy();
            log("Galaxy complete");
            status().startGame();
            systemsScouted().clear();
            systemsToAllocate().clear();
            shipsConstructed().clear();
            galaxy().startGame();
            saveRecentSession(false);
        }
    }
    private void  startExecutors() {
        smallSphereService = Executors.newSingleThreadExecutor();
    }
    private void stopCurrentGame() {
        gameListeners().forEach(gl -> gl.clearAdvice());
        vars().clear();
        clearAlerts();
        // shut down any threads running from previous game
        smallSphereService().shutdownNow();
    }
    public void exit()                        { System.exit(0); }
    public Object var(String key)             { return vars().get(key); }
    public void var(String key, Object value) { vars().put(key, value); }
    public void removeVar(String key)         { vars().remove(key); }
    public void replaceVarValue(Object prevValue, Object newValue) {
        List<String> keys = new ArrayList<>();
        keys.addAll(vars().keySet());
        for (String key: keys) {
            if (var(key) == prevValue) {
                log("replacing value for session var: ", key);
                var(key, newValue);
            }
        }
    }
    public void removeVarValue(Object value) {
        List<String> keys = new ArrayList<>();
        keys.addAll(vars().keySet());
        for (String key: keys) {
            if (var(key) == value)
                vars().remove(key);
        }
    }
    public void nextTurn() {
        if (performingTurn())
            return;

        performingTurn = true;
        nextTurnThread = new Thread(nextTurnProcess());
        nextTurnThread.start();
    }
    public void waitUntilNextTurnCanProceed() {
        while(suspendNextTurn)
            sleep(200);
    }
    public boolean sufficientHeapSpace(IGameOptions opts) {
        long maxHeap = Rotp.maxHeapMemory;
        long reqHeap = 200;
        return maxHeap > reqHeap;
    }
    public boolean inProgress()  { return status().inProgress(); }
    private Runnable nextTurnProcess() {
        return () -> {
            try {
                performingTurn = true;
                Galaxy gal = galaxy();
                String turnTitle = nextTurnTitle();
                NoticeMessage.setStatus(turnTitle, text("TURN_SAVING"));
                FlightPathSprite.clearWorkingPaths();
                RotPUI.instance().mainUI().saveMapState();
                log("Next Turn - BEGIN: ", str(galaxy.currentYear()));
                log("Autosaving pre-turn");
                instance.saveRecentSession(false);
                
                long startMs = timeMs();
                systemsToAllocate().clear();
                systemsScouted().clear();
                shipsConstructed().clear();
                clearAlerts();
                RotPUI.instance().repaint();
                processNotifications();
                gal.preNextTurn();
                
                // REMOVE THIS CODE
                //markSpiesCaptured();
                
                // all intra-empire events: civ turns, ship movement, etc
                gal.advanceTime();
                gal.moveShipsInTransit();
                
                gal.events().nextTurn();

                gal.council().nextTurn();
                GNNRankingNoticeCheck.nextTurn();
                GNNExpansionEvent.nextTurn();
                gal.refreshAllEmpireViews();
                gal.nextEmpireTurns();
                
                // test game over conditions
                //randomlyEndGame();
                
                if (!inProgress())
                    return;

                if (processNotifications()) {
                    log("Notifications processed 1 - back to MainPanel");
                    //RotPUI.instance().selectMainPanel();
                }
                gal.postNextTurn1();
                if (!inProgress())
                    return;

                processNotifications();

                RotPUI.instance().selectMainPanel();
                log("Notifications processed 2 - back to MainPanel");
                gal.postNextTurn2();

                if (!inProgress())
                    return;
                if (processNotifications()) {
                    log("Notifications processed 3 - back to MainPanel");
                    //RotPUI.instance().selectMainPanel();
                }
                // all diplomatic fallout: praise, warnings, treaty offers, war declarations
                gal.assessTurn();

                processNotifications();
                gal.refreshAllEmpireViews();

                gal.makeNextTurnDecisions();

                if (!systemsToAllocate().isEmpty())
                    gameListeners().forEach(l -> l.allocateSystems());

                log("Refreshing Player Views");
                NoticeMessage.resetSubstatus(text("TURN_REFRESHING"));
                validate();
                gal.refreshEmpireViews(player());

                log("Autosaving post-turn");
                log("NEXT TURN PROCESSING TIME: ", str(timeMs()-startMs));
                NoticeMessage.resetSubstatus(text("TURN_SAVING"));
                instance.saveRecentSession(true);

                log("Reselecting main panel");
                RotPUI.instance().mainUI().showDisplayPanel();
                RotPUI.instance().selectMainPanel();
                notifications().clear();
                // ensure Next Turn takes at least a minimum time
                long spentMs = timeMs() - startMs;
                if (spentMs < MINIMUM_NEXT_TURN_TIME) {
                    try { Thread.sleep(MINIMUM_NEXT_TURN_TIME - spentMs);
                    } catch (InterruptedException e) { }
                }
                RotPUI.instance().repaint();
                log("Next Turn - END: ", str(galaxy.currentYear()));
            }
            catch(Exception e) {
                err("Unexpected error during Next Turn:", e.toString());
                exception(e);
            }
            finally {
                RotPUI.instance().mainUI().restoreMapState();
                if (Rotp.memoryLow())
                    RotPUI.instance().mainUI().showMemoryLowPrompt();
                // handle game over possibility
                if (!session().status().inProgress())
                    RotPUI.instance().selectGameOverPanel();
                performingTurn = false;
            }
        };
    }
    public boolean processNotifications() {
        log("Processing player notifications: ", str(notifications().size()));
        if (!session().systemsScouted().isEmpty()) 
            session().addTurnNotification(new SystemsScoutedNotification());

        // received a concurrent modification here... iterate over temp array
        List<TurnNotification> notifs = new ArrayList<>(notifications());
        Collections.sort(notifs);
        notifications().clear();

        gameListeners().forEach(l -> l.processNotifications(notifs));
        systemsScouted().clear();
        return true;
    }
    public void startGroundCombat() {
        for (EmpireView v : player().empireViews()) {
            if ((v!= null) && !v.embassy().contact()) {
                v.embassy().makeFirstContact();
                v.embassy().declareWar();
                break;
            }
        }

        if (galaxy().currentTurn() > 2)
            return;
        Empire pl = player();
        if (pl.hostiles().isEmpty())
            return;
        Empire emp = random(pl.hostiles()).empire();
        StarSystem sys = galaxy().system(pl.homeSysId());

        Transport tr = emp.allColonizedSystems().get(0).colony().transport();
        tr.setDest(sys);
        tr.size(30); // better hope you're playing the Bulrathi
        tr.launch();
        tr.arrive();
    }
    public void startShipCombat() {
        if (galaxy().currentTurn() > 2)
            return;
        Empire pl = player();
        if (pl.hostiles().isEmpty())
            return;
        Empire emp = random(pl.hostiles()).empire();
        StarSystem sys = galaxy().system(pl.homeSysId());
        sys.colony().defense().bases(3);

        ShipDesign plSc = pl.shipLab().scoutDesign();
        ShipDesign plSh = pl.shipLab().fighterDesign();
        ShipFleet plFl = sys.orbitingFleetForEmpire(pl);
        if (plFl != null) {
            plFl.addShips(plSh.id(), 2);
        }
        else {
            plFl = new ShipFleet(pl.id, sys);
            plFl.addShips(plSh.id(), 2);
            sys.acceptFleet(plFl);
        }

        ShipDesign enSh = emp.shipLab().fighterDesign();
        ShipDesign enSh2 = emp.shipLab().bomberDesign();
        ShipFleet enFl = new ShipFleet(emp.id, sys);
        enFl.addShips(enSh.id(), 5);
        enFl.addShips(enSh2.id(), 3);
        sys.acceptFleet(enFl);
    }
    public void startShipCombat2() {
        if (galaxy().currentTurn() > 2)
            return;
        Empire pl = player();
        if (pl.hostiles().isEmpty())
            return;
        Empire emp = random(pl.hostiles()).empire();
        StarSystem sys = galaxy().system(pl.homeSysId());
        sys.colony().defense().bases(1);
        ShipDesign plCo= pl.shipLab().colonyDesign();
        ShipDesign plSc = pl.shipLab().scoutDesign();
        ShipDesign plSh = pl.shipLab().fighterDesign();
        ShipFleet plFl = sys.orbitingFleetForEmpire(pl);
        if (plFl != null) {
            plFl.removeShips(plSc.id(), 2, false);
            plFl.removeShips(plCo.id(), 1, false);
            //plFl.addShips(plSh, 2);
        }
        else {
            plFl = new ShipFleet(pl.id, sys);
            plFl.addShips(plSh.id(), 2);
            sys.acceptFleet(plFl);
        }

        ShipDesign enSh = emp.shipLab().fighterDesign();
        ShipWeapon miss = emp.shipLab().missileWeapon(0, 5);
        enSh.addWeapon(miss, 20);
        ShipFleet enFl = new ShipFleet(emp.id, sys);
        enFl.addShips(enSh.id(), 5);
        //enFl.addShips(enSh2, 3);
        sys.acceptFleet(enFl);
    }
    public void startGalacticCouncil() {
        if (galaxy().currentTurn() == 2) {
            for (Empire emp: galaxy().empires())
                emp.makeFullContact();
        }
    }
    public void startGNNNotification() {
        (new GNNRankingNoticeCheck()).showRanking();
    }
    public void randomlyEndGame() {
        if (galaxy().numberTurns() < 2)
            return;
        galaxy().council().leader(random(galaxy().empires()));
        player().lastAttacker(random(galaxy().empires()));

        int r = roll(0,11);
        switch(r) {
            case 0: session().status().loseOverthrown(); break;
            case 1: session().status().loseMilitary(); break;
            case 2: session().status().loseDiplomatic(); break;
            case 3: session().status().loseNewRepublic(); break;
            case 4: session().status().loseRebellion(); break;
            case 5: session().status().winDiplomatic(); break;
            case 6: session().status().winMilitary(); break;
            case 7: session().status().winMilitaryAlliance(); break;
            case 8: session().status().winNewRepublic(); break;
            case 9: session().status().winRebellion(); break;
            case 10: session().status().winRebellionAlliance(); break;
            case 11: session().status().wonCouncilAlliance(); break;
        }
    }
    public void formAllianceWithRandomContact() {
        int empId = random(player().contactedEmpires()).id;
        if (!player().alliedWith(empId)) {
            EmpireView v = player().viewForEmpire(empId);
            v.embassy().signAlliance();
        }
    }
    public void formAlliancesWithContacts() {
        for (Empire e: player().contactedEmpires()) {
            if (!player().alliedWith(e.id)) {
                EmpireView v = player().viewForEmpire(e.id);
                v.embassy().signAlliance();
            }
        }
    }
    public void randomlyLearnTechs() {
        if (galaxy().numberTurns() == 2) {
            err("Each empire randomly learning 10 unknown techs to facilitate TechExchange testing");
            for (Empire emp: galaxy().empires()) {
                for (int i=0;i<10;i++)
                    emp.tech().learnTech(emp.tech().randomUnknownTech(1,20).id());
            }
            err("Each empire spying on each other");
            for (Empire emp1: galaxy().empires()) {
                for (Empire emp2: galaxy().empires()) {
                    if (emp1 != emp2) {
                        EmpireView v = emp1.viewForEmpire(emp2);
                        v.spies().updateTechList();
                    }
                }
            }
        }
    }
    public void startHighTechShipCombat() {
        if (galaxy().numberTurns() > 2)
            return;

        // make enemies
        for (EmpireView v : player().empireViews()) {
            if ((v!= null) && !v.embassy().contact()) {
                v.embassy().makeFirstContact();
                v.embassy().declareWar();
                break;
            }
        }

        // learn everything
        for (Empire emp: galaxy().empires())
            emp.tech().learnAll();

        Empire pl = player();
        if (pl.hostiles().isEmpty())
            return;
        Empire emp = random(pl.hostiles()).empire();
        StarSystem sys = galaxy().system(pl.homeSysId());
        sys.colony().defense().bases(3);

        ShipDesign plSc = pl.shipLab().scoutDesign();
        ShipDesign plSh = pl.shipLab().fighterDesign();
        ShipDesign plBo = pl.shipLab().bomberDesign();
        ShipDesign plCo = pl.shipLab().colonyDesign();
        ShipFleet plFl = sys.orbitingFleetForEmpire(pl);
        if (plFl == null) {
            plFl = new ShipFleet(pl.id, sys);
            sys.acceptFleet(plFl);
        }

        plFl.addShips(plSc.id(), 20);
        plFl.addShips(plSh.id(), 2);
        plFl.addShips(plBo.id(), 2);
        plFl.addShips(plCo.id(), 2);

        ShipSpecial sp1  = pl.shipLab().specialTeleporter();
        ShipSpecial sp2  = pl.shipLab().specialCloak();
        ShipSpecial sp3  = pl.shipLab().specialNamed("Stasis Field");
        ShipSpecial sp4  = pl.shipLab().specialNamed("High Energy Focus");
        ShipSpecial sp5  = pl.shipLab().specialNamed("Ionic Pulsar");
        ShipSpecial sp6  = pl.shipLab().specialNamed("Black Hole Generator");
        ShipSpecial sp7  = pl.shipLab().specialNamed("Warp Dissipator");
        ShipSpecial sp8  = pl.shipLab().specialNamed("Technology Nullifier");
        ShipSpecial sp9  = pl.shipLab().specialNamed("Displacement Device");

        ShipManeuver manv = pl.shipLab().maneuvers().get(pl.shipLab().maneuvers().size()-1);
        ShipWeapon wpn1 = pl.shipLab().beamWeapon(0, true);

        plSc.maneuver(manv);
        plSc.special(0, sp1);
        plSc.special(1, sp7);
        plSc.special(2, sp5);
        pl.shipViewFor(plSc).scan();

        plSh.maneuver(manv);
        plSh.weapon(0,wpn1);
        plSh.special(0,sp2);
        plSh.special(1,sp6);
        plSh.special(2,sp4);
        pl.shipViewFor(plSh).scan();

        plBo.maneuver(manv);
        plBo.special(0,sp3);
        plBo.special(1, sp8);
        plBo.special(2, sp9);
        pl.shipViewFor(plBo).scan();


        ShipDesign enSh = emp.shipLab().fighterDesign();
        ShipDesign enSh2 = emp.shipLab().bomberDesign();
        ShipFleet enFl = new ShipFleet(emp.id, sys);
        enFl.addShips(enSh.id(), 100);
        enFl.addShips(enSh2.id(), 30);
        sys.acceptFleet(enFl);
    }
    public void randomlyStealATech() {
        EmpireView view = random(player().empireViews());
        if (view != null) {
            StarSystem espionageSystem = galaxy().system(view.empire().homeSysId());
            Empire espionageEmpire = view.empire();

            List<Tech> techs = new ArrayList<>();
            for (int i=0;i<TechTree.NUM_CATEGORIES;i++)
                techs.add(tech(random(view.empire().tech().category(i).allTechs())));
            techs.remove(random(techs)); // one blank category
            Spy spy = (new Spy(view.spies())).makeSuper();
            EspionageMission mission = new EspionageMission(view.spies(), spy, techs,espionageSystem);
            StealTechNotification.create(mission, espionageEmpire.id);
            for (EmpireView v: player().empireViews())
                if ((v != null) && (v.empire() != view.empire()))
                    mission.empiresToFrame().add(v.empire());
        }
    }
    public void randomlyCommitSabotage() {
        EmpireView view = random(player().empireViews());
        if (view != null) {
            view.embassy().makeFirstContact();
            StarSystem sys = random(view.empire().allColonizedSystems());
            player().sv.refreshFullScan(sys.id);
            Spy spy = (new Spy(view.spies())).makeSuper();
            SabotageMission mission = new SabotageMission(view.spies(), spy);
            SabotageNotification.addMission(mission, sys.id);
        }
    }
    private String nextTurnTitle() {
        if (UserPreferences.displayYear())
            return text("MAIN_ADVANCING_YEAR", galaxy().currentYear()+1);
        else
            return text("MAIN_ADVANCING_TURN", galaxy().currentTurn()+1);
    }
    public void saveSession(String filename) throws Exception {
        log("Saving game as file: ", filename);
        GameSession currSession = GameSession.instance();        
        log("Session started");
        File saveFile = saveFileNamed(filename);
        OutputStream fileOut = new FileOutputStream(saveFile);
        OutputStream buffer = new BufferedOutputStream(fileOut);
        ObjectOutput output = new ObjectOutputStream(buffer);

        output.writeObject(currSession);
        output.flush();
        output.close();
        buffer.close();
        fileOut.close();
    }
    private void loadPreviousSession(GameSession gs, boolean startUp) {
        stopCurrentGame();
        instance = gs;
        startExecutors();
        RotPUI.instance().mainUI().checkMapInitialized();
        if (!startUp) {
            RotPUI.instance().selectMainPanel();
        }
    }
    public File saveFileNamed(String fileName) {
        return new File(Rotp.jarPath(), fileName);
    }
    public File recentSaveFile() {
        return new File(Rotp.jarPath(), GameSession.RECENT_SAVEFILE);
    }
    public void saveRecentSession(boolean showWarning) {
        try {
            autoBackup();
            saveSession(RECENT_SAVEFILE);
        }
        catch(Exception e) {
            err("Error saving: ", RECENT_SAVEFILE, " - ", e.getMessage());
            if (showWarning)
                RotPUI.instance().mainUI().showAutosaveFailedPrompt(e.getMessage());
        }
    }

    private void autoBackup() {
        File recentSaveFile = recentSaveFile();
        if (recentSaveFile.exists() && (galaxy.currentTurn() % 5 == 0)) {
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss'Z'");
            df.setTimeZone(tz);
            String timestampTemplate = String.format("autosave-%s.rotp", df.format(new Date()));
            File backup = new File(Rotp.jarPath(), timestampTemplate);
            try {
                Files.copy(recentSaveFile.toPath(), backup.toPath());
                log("'", recentSaveFile.getName(), "' copied to '", backup.getName(), "'");
            } catch (IOException e) {
                err("Failed to create an autosave file: ", e.getMessage());
            }
        }
    }

    public boolean hasRecentSession() {
        try {
            InputStream file = new FileInputStream(RECENT_SAVEFILE);
            file.close();
        } catch (IOException ex) {
            return false;
        }
        return true;
    }
    public void loadRecentSession(boolean startUp) {
        loadSession(RECENT_SAVEFILE, startUp);
    }
    public void loadSession(String filename, boolean startUp) {
        try {
            log("Loading game from file: ", filename);
            File saveFile = saveFileNamed(filename);
            InputStream file = new FileInputStream(saveFile);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);
            GameSession newSession = (GameSession) input.readObject();
            GameSession.instance = newSession;
            newSession.validate();
            newSession.validateOnLoadOnly();
            input.close();
            buffer.close();
            file.close();
            loadPreviousSession(newSession, startUp);
            saveRecentSession(false);
        }
        catch(IOException | ClassNotFoundException e) {
            throw new RuntimeException(text("LOAD_GAME_BAD_VERSION", filename));
        }
    }
    private void validate() {
        galaxy().validate();
    }
    private void validateOnLoadOnly() {
        GNNExpansionEvent.instance().validate(galaxy());

        // check for invalid colonies with too much waste & negative pop
        for (StarSystem sys: galaxy().starSystems()) {
            if (sys.isColonized())
                sys.colony().validateOnLoad();
        }
        // check for council last-vote init issue
        boolean allVotedOnlyForPlayer = true;
        for (Empire emp: galaxy().empires()) {
            if (emp.lastCouncilVoteEmpId() != 0)
                allVotedOnlyForPlayer = false;
        }

        if (allVotedOnlyForPlayer) {
            for (Empire emp: galaxy().empires())
                emp.lastCouncilVoteEmpId(Empire.NULL_ID);
        }

        Galaxy gal = this.galaxy();
        Empire pl = player();
        float minX = gal.width();
        float minY = gal.height();
        float maxX = 0;
        float maxY = 0;

        List<StarSystem> alliedSystems = pl.allColonizedSystems();
        for (StarSystem sys : alliedSystems) {
            minX = min(minX,sys.x());
            maxX = max(maxX,sys.x());
            minY = min(minY,sys.y());
            maxY = max(maxY,sys.y());
        }
        float r = pl.scoutReach(6);
        minX = max(0,minX-r);
        maxX = min(gal.width(), maxX+r);
        minY = max(0,minY-r);
        maxY = min(gal.height(), maxY+r);
        pl.setBounds(minX, maxX, minY, maxY);
        pl.setVisibleShips();
    }
    static ThreadFactory minThreadFactory() {
        return (Runnable r) -> {
            Thread t = new Thread(r);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        };
    }
}
