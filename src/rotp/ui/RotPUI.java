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
package rotp.ui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.Timer;
import rotp.Rotp;
import rotp.model.colony.Colony;
import rotp.model.combat.ShipCombatManager;
import rotp.model.empires.Empire;
import rotp.model.empires.EspionageMission;
import rotp.model.empires.SabotageMission;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.Transport;
import rotp.model.game.GameListener;
import rotp.model.game.GameSession;
import rotp.model.planet.PlanetFactory;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipLibrary;
import rotp.model.tech.TechCategory;
import rotp.model.tech.TechLibrary;
import rotp.ui.combat.ShipBattleUI;
import rotp.ui.design.DesignUI;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.diplomacy.DiplomacyRequestReply;
import rotp.ui.fleets.FleetUI;
import rotp.ui.map.SystemsUI;
import rotp.ui.game.GameOverUI;
import rotp.ui.game.GameUI;
import rotp.ui.game.LoadGameUI;
import rotp.ui.game.RaceIntroUI;
import rotp.ui.game.SaveGameUI;
import rotp.ui.game.SetupGalaxyUI;
import rotp.ui.game.SetupRaceUI;
import rotp.ui.main.MainUI;
import rotp.ui.notifications.DiplomaticNotification;
import rotp.ui.notifications.TurnNotification;
import rotp.ui.planets.ColonizePlanetUI;
import rotp.ui.planets.GroundBattleUI;
import rotp.ui.planets.PlanetsUI;
import rotp.ui.races.RacesUI;
import rotp.ui.races.SabotageUI;
import rotp.ui.tech.AllocateTechUI;
import rotp.ui.tech.DiplomaticMessageUI;
import rotp.ui.tech.DiscoverTechUI;
import rotp.ui.tech.SelectNewTechUI;
import rotp.ui.util.planets.PlanetImager;
import rotp.util.AnimationManager;
import rotp.util.ImageManager;
import rotp.util.LanguageManager;
import rotp.util.Logger;
import rotp.util.sound.SoundManager;

public class RotPUI extends BasePanel implements ActionListener, KeyListener, GameListener {
    private static final long serialVersionUID = 1L;
    private static int FPS = 10;
    private static int ANIMATION_TIMER = 100;
    private boolean drawNextTurnNotice = false;
    private static Throwable startupException;
    static {
        Logger.registerLogListener(Logger::logToFile);
        // needed for opening ui
        try { UserPreferences.load(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: UserPreferences init "+t.getMessage()); }
        try { TechLibrary.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: TechLibrary init: "+t.getMessage()); }
        try { LanguageManager.current().selectedLanguageName(); }
        catch (Throwable t) { System.out.println("Err: LanguageManager init: "+t.getMessage()); }

        try { SoundManager.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: SoundManager init: "+t.getMessage()); }

        try { ImageManager.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: ImageManager init: "+t.getMessage()); }

        try { AnimationManager.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: AnimationManager init: "+t.getMessage()); }

        try { DialogueManager.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: DialogueManager init: "+t.getMessage()); }

        try { ShipLibrary.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: ShipLibrary init: "+t.getMessage()); }

        try { PlanetImager.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: PlanetImager init: "+t.getMessage()); }

        try { PlanetFactory.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: PlanetFactory init: "+t.getMessage()); }

        try { UserPreferences.loadAndSave(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: PlanetFactory init: "+t.getMessage()); }
    }

    public static boolean useDebugFile = false;

    private static final String SETUP_RACE_PANEL = "SetupRace";
    private static final String SETUP_GALAXY_PANEL = "SetupGalaxy";
    private static final String LOAD_PANEL = "Load";
    private static final String SAVE_PANEL = "Save";
    private static final String INTRO_PANEL = "Intro";
    private static final String MAIN_PANEL = "Main";
    private static final String GAME_PANEL = "Game";
    private static final String DESIGN_PANEL = "Design";
    private static final String FLEET_PANEL = "Fleet";
    private static final String SYSTEMS_PANEL = "Systems";
    private static final String RACES_PANEL = "Races";
    private static final String PLANETS_PANEL = "Planets";
    private static final String TECH_PANEL = "Tech";
    private static final String SELECT_NEW_TECH_PANEL = "NewTech";
    private static final String DISCOVER_TECH_PANEL = "DiscoverTech";
    private static final String COLONIZE_PROMPT_PANEL = "PromptColonize";
    private static final String DIPLOMATIC_MESSAGE_PANEL = "DiplomaticMessage";
    private static final String SHIP_BATTLE_PANEL = "ShipBattle";
    private static final String GROUND_BATTLE_PANEL = "GroundBattle";
    private static final String SABOTAGE_PANEL = "Sabotage";
    private static final String GNN_PANEL = "GNN";
    private static final String COUNCIL_PANEL = "GalacticCouncil";
    private static final String GAME_OVER_PANEL = "GameOver,Man,GameOver";
    private static final String CREDITS_PANEL = "Credits";
    private static final String ERROR_PANEL = "Error";

    private static final RotPUI instance = new RotPUI();

    private static PrintWriter debugFile = null;

    public static void fps(int fps) {
        // bound arg between 10 & 60
        int actualFPS = Math.min(60, Math.max(10,fps));
        if (FPS == actualFPS)
            return;

        FPS = actualFPS;
        ANIMATION_TIMER = 1000/FPS;
        instance.resetTimer();
    }
    public static int scaledSize(int i) {
        if (i < 1)
            return (int) Math.ceil(Rotp.resizeAmt()*i);
        else if (i > 1)
            return (int) Math.floor(Rotp.resizeAmt()*i);
        else
            return i;
    }
    public static int unscaledSize(int i) {
        return (int) Math.max(0, Math.ceil(i/Rotp.resizeAmt()));
    }
    public static PrintWriter debugFile() {
        if (!useDebugFile)
            return null;

        if (debugFile == null) {
            try {
                FileOutputStream fout = new FileOutputStream(new File("rotp_log.txt"));
                debugFile = new PrintWriter(fout, true);
            }
            catch (FileNotFoundException e) {
                System.err.println("RotpUI.static<> -- Unable to open debug file:  FileNotFoundException: " + e);
            }
        }
        return debugFile;
    }

    private final GameUI gameUI = new GameUI();
    private final LoadGameUI loadGameUI = new LoadGameUI();
    private final SaveGameUI saveGameUI = new SaveGameUI();
    private final SetupRaceUI setupRaceUI = new SetupRaceUI();
    private final SetupGalaxyUI setupGalaxyUI = new SetupGalaxyUI();
    private final RaceIntroUI raceIntroUI = new RaceIntroUI();
    private MainUI mainUI;
    private final DesignUI designUI = new DesignUI();
    private final FleetUI fleetUI = new FleetUI();
    private final SystemsUI systemsUI = new SystemsUI();
    private final RacesUI racesUI = new RacesUI();
    private final PlanetsUI planetsUI = new PlanetsUI();
    private final AllocateTechUI allocateTechUI = new AllocateTechUI();
    private final SelectNewTechUI selectNewTechUI = new SelectNewTechUI();
    private final DiscoverTechUI discoverTechUI = new DiscoverTechUI();
    private final ShipBattleUI shipBattleUI = new ShipBattleUI();
    private final GroundBattleUI groundBattleUI = new GroundBattleUI();
    private final SabotageUI sabotageUI = new SabotageUI();
    private final GNNUI gnnUI = new GNNUI();
    private final ColonizePlanetUI colonizePlanetUI = new ColonizePlanetUI();
    private final DiplomaticMessageUI diplomaticMessageUI = new DiplomaticMessageUI();
    private final GalacticCouncilUI galacticCouncilUI = new GalacticCouncilUI();
    private final GameOverUI gameOverUI = new GameOverUI();
    private final ErrorUI errorUI = new ErrorUI();

    private final CardLayout layout = new CardLayout();
    private String currentPane = GAME_PANEL;
    private BasePanel selectedPanel;

    private Timer timer;
    private int animationCount = 0;
    private long animationMs = 0;
    public MainUI mainUI() {
        if (mainUI == null)
            mainUI = new MainUI();
        return mainUI;
    }
    public RaceIntroUI raceIntroUI()  { return raceIntroUI; }
    @Override
    public int animationCount()     { return animationCount; }
    @Override
    public long animationMs()       { return animationMs; }

    public RotPUI() {
        timer = new Timer(ANIMATION_TIMER, this);
        init();
        registerOnSession(session());
    }
    // should be called ONCE on any time new game session is created/loaded
    public final void registerOnSession(GameSession gameSession) {
        gameSession.removeGameListener(this);
        gameSession.addGameListener(this);
    }
    @Override
    public void clearAdvice() {
        RotPUI.this.mainUI().clearAdvice();
    }
    @Override
    public void processNotifications(List<TurnNotification> notifications) {
        for (TurnNotification tn: notifications) {
            try {
                drawNextTurnNotice = false;
                tn.notifyPlayer();
            } finally {
                drawNextTurnNotice = true;
            }
        }
    }

    private void resetTimer() {
        if (timer != null)
            timer.stop();
        timer = new Timer(ANIMATION_TIMER, this);
        timer.start();
    }
    private void init() {
        initModel();
        addKeyListener(this);
        setDefaultCursor();
        if (startupException != null)
            selectErrorPanel(startupException);
        else
            selectCurrentPanel();

        timer.start();
        //toggleAnimations();
        repaint();
    }

    public void toggleAnimations() {
        if (playAnimations())
            timer.start();
        else
            timer.stop();
    }
     public static RotPUI instance()            { return instance; }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (drawNextTurnNotice && session().performingTurn()) {
            drawNotice(g, 28, -s100);
        }
        requestFocusInWindow();
    }

    public void selectCurrentPanel()   { selectPanel(currentPane, selectedPanel); }

    // PLAYER-TRIGGERED ACTIONS
    public void selectSetupRacePanel() { setupRaceUI.init(); selectPanel(SETUP_RACE_PANEL, setupRaceUI);  }
    public void selectSetupGalaxyPanel() { setupGalaxyUI.init(); selectPanel(SETUP_GALAXY_PANEL, setupGalaxyUI);  }
    public void selectLoadGamePanel()  { loadGameUI.init(); selectPanel(LOAD_PANEL, loadGameUI);  }
    public void selectSaveGamePanel()  { saveGameUI.init(); selectPanel(SAVE_PANEL, saveGameUI);  }
    public void selectIntroPanel()     { mainUI.init(false); selectPanel(MAIN_PANEL, mainUI()); enableGlassPane(raceIntroUI); repaint(); }
    public void selectMainPanel()      { selectMainPanel(false); }
    public void selectMainPanel(boolean pauseNextTurn)      {
        disableGlassPane();
        mainUI.init(pauseNextTurn);
        selectPanel(MAIN_PANEL, mainUI());
        repaint();
    }
    public void selectGamePanel()      { selectPanel(GAME_PANEL,  gameUI); }
    public void selectDesignPanel()    { designUI.init(); selectPanel(DESIGN_PANEL, designUI); }
    public void selectFleetPanel()     { fleetUI.init(); selectPanel(FLEET_PANEL, fleetUI); }
    public void selectSystemsPanel()   { systemsUI.init(); selectPanel(SYSTEMS_PANEL, systemsUI); }
    public void selectRacesPanel()     { racesUI.init(); selectPanel(RACES_PANEL, racesUI); }
    public void selectPlanetsPanel()   { planetsUI.init(); selectPanel(PLANETS_PANEL, planetsUI); }
    public void selectTechPanel()      { allocateTechUI.init(); selectPanel(TECH_PANEL, allocateTechUI); }
    public void selectCouncilPanel()   {
        session().pauseNextTurnProcessing("Show Council");
        galacticCouncilUI.init();
        selectPanel(COUNCIL_PANEL, galacticCouncilUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectGameOverPanel()  {
        gameOverUI.init();
        selectPanel(GAME_OVER_PANEL, gameOverUI);
    }
    public void selectErrorPanel(Throwable e)  {
        // ignore low-level mp3 errors from the Java FX library
        for (StackTraceElement line : e.getStackTrace()) {
            if (line.toString().contains("com.sun.media.jfxmediaimpl.NativeMediaPlayer.sendWarning")) {
                err("IGNORED JAVA MEDIA WARNING: ");
                e.printStackTrace();
                return;
            }
            if (line.toString().contains("com.sun.media.sound.DirectAudioDevice")) {
                err("IGNORED JAVA MEDIA WARNING: ");
                e.printStackTrace();
                SoundManager.loadSounds();
                return;
            }
        }
        //e.printStackTrace();
        errorUI.init(e);
        selectPanel(ERROR_PANEL, errorUI);
    }
    public void selectGNNPanel(String title, String id, List<Empire> empires) {
        session().pauseNextTurnProcessing("Show GNN");
        gnnUI.init(title, id, empires);
        selectPanel(GNN_PANEL, gnnUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectSabotagePanel(SabotageMission m, int sysId) {
        session().pauseNextTurnProcessing("Show Sabotage");
        sabotageUI.init(m, sysId);
        selectPanel(SABOTAGE_PANEL, sabotageUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectGroundBattlePanel(Colony c, Transport tr) {
        session().pauseNextTurnProcessing("Show Ground Battle");
        groundBattleUI.init(c, tr);
        selectPanel(GROUND_BATTLE_PANEL, groundBattleUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void showAdvice(String key, String var1, String var2, String var3) {
        mainUI().showAdvice(key, var1, var2, var3);
        selectMainPanel();
    }
    public void showBombardmentNotice(int sysId, ShipFleet fl) {
        session().pauseNextTurnProcessing("Show Bombard Notice");
        mainUI().showBombardmentNotice(sysId, fl);
        selectMainPanel();
        session().waitUntilNextTurnCanProceed();
    }
    public void promptForBombardment(int sysId, ShipFleet fl) {
        session().pauseNextTurnProcessing("Show Bombard Prompt");
        mainUI().showBombardmentPrompt(sysId, fl);
        selectMainPanel();
        session().waitUntilNextTurnCanProceed();
    }
    public void promptForShipCombat(ShipCombatManager mgr) {
        try {
            drawNextTurnNotice = false;
            session().pauseNextTurnProcessing("Show Ship Combat Prompt");
            mainUI().showShipCombatPrompt(mgr);
            selectMainPanel();
            session().waitUntilNextTurnCanProceed();
        } finally {
            drawNextTurnNotice = true;
        }
    }
    public void selectShipBattlePanel(ShipCombatManager mgr) {
        shipBattleUI.init(mgr);
        selectPanel(SHIP_BATTLE_PANEL, shipBattleUI);
    }
public void promptForColonization(int sysId, ShipFleet fl, ShipDesign d) {
        session().pauseNextTurnProcessing("Show Colonize Prompt");
        mainUI().showColonizationPrompt(sysId, fl, d);
        selectMainPanel();
        session().waitUntilNextTurnCanProceed();
    }
    public void selectColonizationPanel(int sysId, ShipFleet fl, ShipDesign d) {
        colonizePlanetUI.init(sysId, fl, d);
        selectPanel(COLONIZE_PROMPT_PANEL, colonizePlanetUI);
    }
    public void selectSelectNewTechPanel(TechCategory cat) {
        session().pauseNextTurnProcessing("Show Select Tech");
        selectNewTechUI.category(cat);
        selectPanel(SELECT_NEW_TECH_PANEL, selectNewTechUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectPlunderShipTechPanel(String techId, int empId) {
        session().pauseNextTurnProcessing("Show Plunder Tech");
        discoverTechUI.plunderShipTech(techId, empId);
        selectPanel(DISCOVER_TECH_PANEL, discoverTechUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectPlunderTechPanel(String techId, int sysId, int empId) {
        session().pauseNextTurnProcessing("Show Plunder Tech");
        discoverTechUI.plunderTech(techId, sysId, empId);
        selectPanel(DISCOVER_TECH_PANEL, discoverTechUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectDiscoverTechPanel(String techId) {
        session().pauseNextTurnProcessing("Show Discover Tech");
        discoverTechUI.discoverTech(techId);
        selectPanel(DISCOVER_TECH_PANEL, discoverTechUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectTradeTechPanel(String techId, int empId) {
        session().pauseNextTurnProcessing("Show Trade Tech");
        discoverTechUI.tradeTech(techId, empId);
        selectPanel(DISCOVER_TECH_PANEL, discoverTechUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectEspionageMissionPanel(EspionageMission mission, int empId) {
        session().pauseNextTurnProcessing("Show Espionage");
        log("==MAIN UI==   espionage mission");
        mainUI().showEspionageMission(mission, empId);
        selectMainPanel();
        session().waitUntilNextTurnCanProceed();
    }
    public void selectStealTechPanel(EspionageMission mission, int empId) {
        discoverTechUI.stealTech(mission, empId);
        selectPanel(DISCOVER_TECH_PANEL, discoverTechUI);
    }
    public void selectDiplomaticMessagePanel(DiplomaticNotification notif) {
        session().pauseNextTurnProcessing("Show Diplomatic Message");
        log("==MAIN UI==   selectDiplomaticMessagePanel");
        diplomaticMessageUI.init(notif);
        selectPanel(DIPLOMATIC_MESSAGE_PANEL, diplomaticMessageUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectDiplomaticDialoguePanel(DiplomaticNotification notif) {
        log("==MAIN UI==   selectDiplomaticDialoguePanel");
        diplomaticMessageUI.init(notif);
        diplomaticMessageUI.endFade();
        selectPanel(DIPLOMATIC_MESSAGE_PANEL, diplomaticMessageUI);
    }
    public void selectDiplomaticReplyPanel(DiplomacyRequestReply reply) {
        log("==MAIN UI==   selectDiplomaticReplyPanel");
        diplomaticMessageUI.initReply(reply);
        diplomaticMessageUI.endFade();
        selectPanel(DIPLOMATIC_MESSAGE_PANEL, diplomaticMessageUI);
    }
    public void selectDiplomaticReplyModalPanel(DiplomacyRequestReply reply) {
        session().pauseNextTurnProcessing("Show Diplomatic Reply");
        log("==MAIN UI==   selectDiplomaticReplyModalPanel");
        diplomaticMessageUI.initReply(reply);
        diplomaticMessageUI.endFade();
        selectPanel(DIPLOMATIC_MESSAGE_PANEL, diplomaticMessageUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void showTransportAlert(String title, String subtitle, String text) {  }
    public void showSpyAlert(String title, String subtitle, String text) {  }
    public void showRandomEventAlert(String title, String subtitle, String text, ImageIcon splash) { }
    @Override
    public void allocateSystems() {
        try {
            drawNextTurnNotice = false;
            session().pauseNextTurnProcessing("Show Allocate Systems");
            log("==MAIN UI==   allocate systems");
            mainUI().allocateSystems(session().systemsToAllocate());
            selectMainPanel();
            session().waitUntilNextTurnCanProceed();
        } finally {
            drawNextTurnNotice = true;
        }
    }
    public void showSystemsScouted() {
        session().pauseNextTurnProcessing("Show Systems Scouted");
        log("==MAIN UI==   show systems scouted");
        mainUI().showSystemsScouted(session().systemsScouted());
        selectMainPanel();
        session().waitUntilNextTurnCanProceed();
    }
    public void showSpiesCaptured() {
        log("==MAIN UI==   show spies captured");
        mainUI().showSpiesCaptured();
        selectMainPanel();
    }
    public void showShipConstruction() {
        session().pauseNextTurnProcessing("Show Ship Construction");
        log("==MAIN UI==   show ship construction");
        mainUI().showShipsConstructed(session().shipsConstructed());
        selectMainPanel();
        session().waitUntilNextTurnCanProceed();
    }
    private void initModel() {
        setFocusTraversalKeysEnabled(false);
        setBackground(Color.CYAN);
        setLayout(layout);

        add(gameUI, GAME_PANEL);
        add(setupRaceUI, SETUP_RACE_PANEL);
        add(setupGalaxyUI, SETUP_GALAXY_PANEL);
        add(loadGameUI, LOAD_PANEL);
        add(saveGameUI, SAVE_PANEL);
        add(raceIntroUI, INTRO_PANEL);
        add(mainUI(), MAIN_PANEL);
        add(designUI, DESIGN_PANEL);
        add(fleetUI, FLEET_PANEL);
        add(systemsUI, SYSTEMS_PANEL);
        add(racesUI, RACES_PANEL);
        add(planetsUI, PLANETS_PANEL);
        add(allocateTechUI, TECH_PANEL);
        add(selectNewTechUI, SELECT_NEW_TECH_PANEL);
        add(discoverTechUI, DISCOVER_TECH_PANEL);
        add(shipBattleUI, SHIP_BATTLE_PANEL);
        add(groundBattleUI, GROUND_BATTLE_PANEL);
        add(sabotageUI, SABOTAGE_PANEL);
        add(gnnUI, GNN_PANEL);
        add(colonizePlanetUI, COLONIZE_PROMPT_PANEL);
        add(diplomaticMessageUI, DIPLOMATIC_MESSAGE_PANEL);
        add(galacticCouncilUI, COUNCIL_PANEL);
        add(gameOverUI, GAME_OVER_PANEL);
        add(errorUI, ERROR_PANEL);

        selectGamePanel();
    }
    private void selectPanel(String panelName, BasePanel panel)   {
        currentPane = panelName;
        selectedPanel = panel;
        selectedPanel.playAmbience();
        log("showing panel: ", panelName);
        layout.show(this, panelName);
    }
    @Override
    public void enableGlassPane(BasePanel panel)   {
        super.enableGlassPane(panel);
        panel.playAmbience();
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        long newTime  = timeMs();
        animationMs = newTime;
        animationCount++;
        animate();
    }
    @Override
    public void animate() {
        try {
            AnimationManager.reclaimImages();
            if (playAnimations()) {
                if (glassPane() != null)
                    glassPane().animate();
                if (selectedPanel != null)
                    selectedPanel.animate();
            }
        }
        catch (Exception e) {
            // we have to catch all errors or else the
            // animation timer stops completely
            e.printStackTrace();
        }
    }
    @Override
    public void keyPressed(KeyEvent e) {
        if (glassPane() != null)
            glassPane().keyPressed(e);
        else if (selectedPanel != null)
            selectedPanel.keyPressed(e);
    }
    @Override
    public void keyReleased(KeyEvent e) {
        if (glassPane() != null)
            glassPane().keyReleased(e);
        else if (selectedPanel != null)
            selectedPanel.keyReleased(e);
    }
    @Override
    public void keyTyped(KeyEvent e) {
        if (glassPane() != null)
            glassPane().keyTyped(e);
        else if (selectedPanel != null)
            selectedPanel.keyTyped(e);
    }
}
