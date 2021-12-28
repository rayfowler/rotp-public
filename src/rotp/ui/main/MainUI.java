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
package rotp.ui.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JLayeredPane;
import javax.swing.border.Border;
import rotp.Rotp;
import rotp.model.Sprite;
import rotp.model.combat.ShipCombatManager;
import rotp.model.empires.Empire;
import rotp.model.empires.EspionageMission;
import rotp.model.empires.SystemView;
import rotp.model.galaxy.IMappedObject;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesign;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.UserPreferences;
import rotp.ui.game.HelpUI;
import rotp.ui.game.HelpUI.HelpSpec;
import rotp.ui.main.overlay.*;
import rotp.ui.map.IMapHandler;
import rotp.ui.notifications.GameAlert;
import rotp.ui.sprites.AlertDismissSprite;
import rotp.ui.sprites.FlightPathSprite;
import rotp.ui.sprites.HelpSprite;
import rotp.ui.sprites.YearDisplaySprite;

public class MainUI extends BasePanel implements IMapHandler {
    private static final long serialVersionUID = 1L;
    public static Color paneBackground = new Color(123,123,123);
    public static Color paneBackgroundDk = new Color(100,100,100);
    public static Color paneShadeC = new Color(123,123,123,128);
    public static Color paneShadeC2 = new Color(100,100,100,192);
    private static final Color shadeBorderC = new Color(80,80,80);
    public static Color darkShadowC = new Color(30,30,30);
    private static final Color namePaneBackgroundHighlight =  new Color(64,64,96);
    private static final Color paneBackgroundHighlight = new Color(96,96,128);

    public static Color textBoxShade0 = new Color(150,150,175);
    public static Color textBoxShade1 = new Color(165,165,202);
    public static Color textBoxShade2 = new Color(112,110,158);
    public static Color textBoxTextColor = new Color(208,208,208);
    public static Color textBoxBackground = new Color(47,46,89);
    public static final Color transC = new Color(0,0,0,0);

    public static final Color greenAlertC  = new Color(0,255,0,192);
    public static final Color redAlertC    = new Color(255,0,0,192);
    public static final Color yellowAlertC = new Color(255,255,0,192);
    
    public static int panelWidth, panelHeight;
    static LinearGradientPaint alertBack;
    static Location center = new Location();
    
    JLayeredPane layers = new JLayeredPane();

    MapOverlayNone overlayNone;
    MapOverlayMemoryLow overlayMemoryLow;
    MapOverlayJava32Bit overlayJava32Bit;
    MapOverlayAutosaveFailed overlayAutosaveFailed;
    MapOverlayShipsConstructed overlayShipsConstructed;
    MapOverlaySpies overlaySpies;
    MapOverlayAllocateSystems overlayAllocateSystems;
    MapOverlaySystemsScouted overlaySystemsScouted;
    MapOverlayEspionageMission overlayEspionageMission;
    MapOverlayColonizePrompt overlayColonizePrompt;
    MapOverlayBombardPrompt overlayBombardPrompt;
    MapOverlayBombardedNotice overlayBombardedNotice;
    MapOverlayShipCombatPrompt overlayShipCombatPrompt;
    MapOverlayAdvice overlayAdvice;
    AlertDismissSprite alertDismissSprite;
    HelpSprite helpSprite;
    MapOverlay overlay;

    private final List<Sprite> nextTurnControls = new ArrayList<>();
    private final List<Sprite> baseControls = new ArrayList<>();

    protected SpriteDisplayPanel displayPanel;
    protected GalaxyMapPanel map;
    protected MainButtonPanel buttonPanel;

    // pre-post next turn state
    private float saveScale;
    private float saveX;
    private float saveY;
    private boolean showAdvice = false;
    private int helpFrame = 0;
    private int numHelpFrames = 0;

    public Border paneBorder()               { return null;   }
    public static Color shadeBorderC()       { return shadeBorderC; }
    public static Color paneBackground()     { return paneBackground; }
    public static Color paneHighlight()      { return paneBackgroundHighlight; }
    public static Color namePaneHighlight()  { return namePaneBackgroundHighlight; }

    public SpriteDisplayPanel displayPanel() { return displayPanel; }
    public void hideDisplayPanel()           {
        displayPanel.setVisible(false); 
    }
    public void showDisplayPanel()           { displayPanel.setVisible(true); }
    public void clearOverlay()               { overlay = showAdvice ? overlayAdvice : overlayNone; }

    private boolean displayPanelMasks(int x, int y) {
        if (!displayPanel.isVisible())
            return false;
        return displayPanel.getBounds().contains(x,y);
    }
    @Override
    public boolean drawMemory()              { return true; }
    @Override
    public GalaxyMapPanel map()              { return map; }

    public MainUI() {
        panelWidth = scaled(250);
        panelHeight = scaled(590);
        initModel();
        addMapControls();
        overlayNone = new MapOverlayNone(this);
        overlayMemoryLow = new MapOverlayMemoryLow(this);
        overlayJava32Bit = new MapOverlayJava32Bit(this);
        overlayAutosaveFailed = new MapOverlayAutosaveFailed(this);
        overlayShipsConstructed = new MapOverlayShipsConstructed(this);
        overlaySpies = new MapOverlaySpies(this);
        overlayAllocateSystems = new MapOverlayAllocateSystems(this);
        overlaySystemsScouted = new MapOverlaySystemsScouted(this);
        overlayEspionageMission = new MapOverlayEspionageMission(this);
        overlayColonizePrompt = new MapOverlayColonizePrompt(this);
        overlayBombardPrompt = new MapOverlayBombardPrompt(this);
        overlayBombardedNotice = new MapOverlayBombardedNotice(this);
        overlayShipCombatPrompt = new MapOverlayShipCombatPrompt(this);
        overlayAdvice = new MapOverlayAdvice(this);
        overlay = overlayNone;
    }
    public void init(boolean pauseNextTurn) {
        map.init();
        if (pauseNextTurn)
            buttonPanel.init();
        
        // if we are being opened not during the next turn process
        // but we have scouted systems (from forming an alliance)
        // bring up the overlay
        if (!session().performingTurn() && session().haveScoutedSystems()) {
            showSystemsScouted(session().systemsScouted());
        }
    }
    @Override
    public void cancel() {
        displayPanel.cancel();
    }
    public void saveMapState() {
        saveScale = map.scaleY();
        saveX = map.centerX();
        saveY = map.centerY();
        sessionVar("MAINUI_SAVE_CLICKED", clickedSprite());
    }
    public void restoreMapState() {
        showDisplayPanel();
        map.setScale(saveScale);
        map.centerX(saveX);
        map.centerY(saveY);
        map.resetRangeAreas();
        map.clearHoverSprite();
        clickedSprite((Sprite) sessionVar("MAINUI_SAVE_CLICKED"));
        showDisplayPanel();
    }
    public void repaintAllImmediately() {
        paintImmediately(0,0,getWidth(),getHeight());
    }
    public void addNextTurnControl(Sprite ms) { nextTurnControls.add(ms); }
    final protected void addMapControls() {
        helpSprite = new HelpSprite(this);
        alertDismissSprite = new AlertDismissSprite(this);
        baseControls.add(new YearDisplaySprite(this));
        baseControls.add(alertDismissSprite);
        baseControls.add(helpSprite);
    }
    @Override
    public boolean showAlerts() {
        return (session().currentAlert() != null) && displayPanel.isVisible();
    }
    @Override
    public boolean showTreasuryResearchBar()       { return overlay != overlayAdvice; }
    @Override
    public boolean showSpyReportIcon()             { return (overlay != overlayAdvice) && session().spyActivity(); }
    @Override
    public boolean showAllCurrentResearch()        { return (overlay == overlayEspionageMission); }
    
    public void setOverlay(MapOverlay lay) {
        overlay = lay;
    }
    public MapOverlay overlay()   { return overlay; }
    public void clearAdvice() {
        if (overlay == overlayAdvice) {
            showAdvice = false;
            overlay = overlayNone;
        }
    }
    public void showAdvice(String key, Empire emp1, String var1, String var2, String var3) {
        overlay = overlayAdvice;
        overlayAdvice.init(key, emp1, var1, var2, var3);
        showAdvice = true;
        repaint();
    }
    @Override
    public void cancelHelp() {
        helpFrame = 0;
        numHelpFrames = 0;
        RotPUI.helpUI().close();
    }
    @Override
    public void showHelp() {
        helpFrame = 1;
        
        numHelpFrames = 1;
        Sprite spr = clickedSprite();
        if (spr instanceof StarSystem) {
            StarSystem sys = (StarSystem) spr;
            if (sys.empire() == player())
                numHelpFrames = 3;
        }
        
        loadHelpUI();
        repaint();   
    }
    @Override 
    public void advanceHelp() {
        if (helpFrame == 0)
            return;
        helpFrame++;
        if (helpFrame > numHelpFrames) 
            cancelHelp();
        loadHelpUI();
        repaint();
    }
    public void showMemoryLowPrompt() {
        overlay = overlayMemoryLow;
        overlayMemoryLow.init();
        repaint();
    }
    public void showJava32BitPrompt() {
        overlay = overlayJava32Bit;
        overlayJava32Bit.init();
        repaint();
    }
    public void showAutosaveFailedPrompt(String err) {
        overlay = overlayAutosaveFailed;
        overlayAutosaveFailed.init(err);
        repaint();
    }
    public void showBombardmentPrompt(int sysId, ShipFleet fl) {
        overlay = overlayBombardPrompt;
        overlayBombardPrompt.init(sysId, fl);
        repaint();
    }
    public void showBombardmentNotice(int sysId, ShipFleet fl) {
        overlayBombardedNotice.init(sysId, fl);
        repaint();
    }
    public void showShipCombatPrompt(ShipCombatManager mgr) {
        overlay = overlayShipCombatPrompt;
        overlayShipCombatPrompt.init(mgr);
        repaint();
    }
    public void showColonizationPrompt(int sysId, ShipFleet fl, ShipDesign d) {
        overlay = overlayColonizePrompt;
        overlayColonizePrompt.init(sysId, fl, d);
        repaint();
    }
    public void showSpyReport() {
        overlay = overlaySpies;
        overlaySpies.init();
        repaint();
    }
    public void showEspionageMission(EspionageMission esp, int empId) {
        overlay = overlayEspionageMission;
        overlayEspionageMission.init(esp, empId);
        repaint();
    }
    public void showShipsConstructed(HashMap<ShipDesign, Integer> ships) {
        overlay = overlayShipsConstructed;
        overlayShipsConstructed.init();
        if (ships.isEmpty())
            resumeTurn();
        else
            repaint();
    }
    public void showSystemsScouted(HashMap<String, List<StarSystem>> newSystems) {
        overlay = overlaySystemsScouted;
        overlaySystemsScouted.init(newSystems);
    }
    public void allocateSystems(HashMap<StarSystem,List<String>> newSystems) {
        overlay = overlayAllocateSystems;
        overlayAllocateSystems.init(newSystems);
    }
    @Override
    public void handleNextTurn()    { displayPanel.handleNextTurn(); }
    private void initModel() {
        int w, h;
        if (!UserPreferences.windowed()) {
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            w = size.width;
            h = size.height;
        }
        else {
            w = scaled(Rotp.IMG_W);
            h = scaled(Rotp.IMG_H);
        }
 
        map = new GalaxyMapPanel(this);
        map.setBounds(0,0,w,h);

        int displayW = panelWidth;
        int displayH = panelHeight;
        displayPanel = new SpriteDisplayPanel(this);
        displayPanel.setBorder(newLineBorder(shadeBorderC,5));
        displayPanel.setBounds(w-displayW-s5,s5,displayW,displayH);

        int buttonH = s60;
        buttonPanel = new MainButtonPanel(this);
        buttonPanel.setBounds(s5,h-s5-buttonH,w-s10,buttonH);

        setLayout(new BorderLayout());
        add(layers, BorderLayout.CENTER);

        layers.add(buttonPanel, JLayeredPane.PALETTE_LAYER);
        layers.add(displayPanel, JLayeredPane.PALETTE_LAYER);
        layers.add(map, JLayeredPane.DEFAULT_LAYER);
        setOpaque(false);
    }
    public boolean enableButtons()   { return !session().performingTurn(); }
    private void selectPlayerHomeSystem() {
        Empire pl = player();
        StarSystem sys = galaxy().system(pl.capitalSysId());

        // main goal here is to trigger sprite click behavior with no click sound
        sys.click(map, 1, false, false);
        hoveringSprite(null);
        clickedSprite(sys);

        Empire emp = player();
        map.centerX(avg(emp.minX(), emp.maxX()));
        map.centerY(avg(emp.minY(), emp.maxY()));
        map.setBounds(emp.minX()-3, emp.maxX()+3, emp.minY()-3, emp.maxY()+3);
        repaint();
    }
    private void loadHelpUI() {
        HelpUI helpUI = RotPUI.helpUI();
        if (helpFrame == 0)
            return;
        
        Sprite spr = this.clickedSprite();
        if (spr instanceof StarSystem) {
            StarSystem sys = (StarSystem) spr;
            if (sys.empire() == player()) {
                switch(helpFrame) {
                    case 1: loadEmpireColonyHelpFrame1(); break;
                    case 2: loadEmpireColonyHelpFrame2(); break;
                    default: loadButtonBarHelpFrame(); break;
                }
            }
            else {
                loadButtonBarHelpFrame();
            }
        }
        else {
            loadButtonBarHelpFrame();          
        }

        helpUI.open(this);
    }
    @Override
    public Color shadeC()                          { return Color.darkGray; }
    @Override
    public Color backC()                           { return Color.gray; }
    @Override
    public Color lightC()                          { return Color.lightGray; }
    @Override
    public boolean hoverOverFleets()               { return displayPanel.hoverOverFleets(); }
    @Override
    public boolean hoverOverSystems()              { return displayPanel.hoverOverSystems(); }
    @Override
    public boolean hoverOverFlightPaths()          { return displayPanel.hoverOverFlightPaths(); }
    @Override
    public boolean masksMouseOver(int x, int y)       { return displayPanelMasks(x, y) || overlay.masksMouseOver(x,y); }
    @Override
    public Color alertColor(SystemView sv)            { 
        if (sv.isAlert())
            return redAlertC;
        return null; 
    }
    @Override
    public boolean displayNextTurnNotice() {
        // don't display notice when updating things
        return (session().performingTurn()
                && !overlay.hideNextTurnNotice());
    }
    @Override
    public List<Sprite> nextTurnSprites()  { return nextTurnControls; }
    @Override
    public void checkMapInitialized() {
        Boolean inited = (Boolean) sessionVar("MAINUI_MAP_INITIALIZED");
        if (inited == null) {
            map.initializeMapData();
            selectPlayerHomeSystem();
            sessionVar("MAINUI_MAP_INITIALIZED", true);
        }
    }
    @Override
    public void clickingNull(int cnt, boolean right) {
        displayPanel.useNullClick(cnt, right);
    };
    @Override
    public void clickingOnSprite(Sprite o, int count, boolean rightClick, boolean click) {
        // if not in normal mode, then NextTurnControls are
        // the only sprites clickable
        if (overlay.consumesClicks(o)) {
            if (nextTurnControls.contains(o)) {
                o.click(map, count, rightClick, click);
                map.repaint();
            }
            return;
        }
        boolean used = (displayPanel != null) && displayPanel.useClickedSprite(o, count, rightClick);
        hoveringOverSprite(null);
        if (!used)  {
            o.click(map, count, rightClick, click);
            if (o.persistOnClick()) {
                hoveringSprite(null);
                clickedSprite(o);
            }
            o.repaint(map);
        }
    }
    @Override
    public void hoveringOverSprite(Sprite o) {
        if (o == lastHoveringSprite())
            return;

        if (lastHoveringSprite() != null)
            lastHoveringSprite().mouseExit(map);
        
        if ((o instanceof StarSystem) 
        && (lastHoveringSprite() instanceof StarSystem)
        && (clickedSprite() instanceof ShipFleet)) {
            lastHoveringSprite().mouseExit(map);
            map.clearHoverSprite();
            lastHoveringSprite(null);
            hoveringSprite(null);
            ((StarSystem)o).repaint(map);
            displayPanel.useClickedSprite(clickedSprite(), 1, false);
        }

        lastHoveringSprite(o);
        if (overlay.hoveringOverSprite(o))
            return;
        
        boolean used = (displayPanel != null) && displayPanel.useHoveringSprite(o);
        if (!used) {
            if (hoveringSprite() != null)
                hoveringSprite().mouseExit(map);
            hoveringSprite(o);
            if (hoveringSprite() != null)
                hoveringSprite().mouseEnter(map);
        }
        repaint();
    }
    @Override
    public boolean shouldDrawSprite(Sprite s) {
        if (s == null)
            return false;
        if (s instanceof FlightPathSprite) {
            FlightPathSprite fp = (FlightPathSprite) s;
            Sprite fpShip = (Sprite) fp.ship();
            if (isClicked(fpShip) || isHovering(fpShip))
                return true;
            if (isClicked((Sprite) fp.destination()))
                return true;
            if (FlightPathSprite.workingPaths().contains(fp))
                return true;
            if (map.showAllFlightPaths())
                return true;
            if (map.showImportantFlightPaths())
                return fp.isPlayer() || fp.aggressiveToPlayer();
            return false;
        }      
        return true;
    }
    @Override
    public Location mapFocus() {
        Location loc = (Location) sessionVar("MAINUI_MAP_FOCUS");
        if (loc == null) {
            loc = new Location();
            sessionVar("MAINUI_MAP_FOCUS", loc);
        }
        return loc;
    }

    public StarSystem lastSystemSelected()    { return (StarSystem) sessionVar("MAINUI_SELECTED_SYSTEM"); }
    public void lastSystemSelected(Sprite s)  { sessionVar("MAINUI_SELECTED_SYSTEM", s); }
    @Override
    public Sprite clickedSprite()            { return (Sprite) sessionVar("MAINUI_CLICKED_SPRITE"); }
    @Override
    public void clickedSprite(Sprite s)      { 
        sessionVar("MAINUI_CLICKED_SPRITE", s); 
        if (s instanceof StarSystem)
            lastSystemSelected(s);
    }
    @Override
    public Sprite hoveringSprite()           { return (Sprite) sessionVar("MAINUI_HOVERING_SPRITE"); }
    public void hoveringSprite(Sprite s)     { 
        sessionVar("MAINUI_HOVERING_SPRITE", s); 
        if (s == null)
           return; 
        if (s.hasDisplayPanel() && !session().performingTurn()) 
            showDisplayPanel(); 
    }
    public Sprite lastHoveringSprite()       { return (Sprite) sessionVar("MAINUI_LAST_HOVERING_SPRITE"); }
    public void lastHoveringSprite(Sprite s) { sessionVar("MAINUI_LAST_HOVERING_SPRITE", s); }
    @Override
    public Border mapBorder()                   { return null; 	}
    @Override
    public boolean canChangeMapScales()         { return overlay.canChangeMapScale(); }
    @Override
    public float startingScalePct()            { return 12.0f / map().sizeX(); }
    @Override
    public List<Sprite> controlSprites()     { return baseControls; }
    @Override
    public void reselectCurrentSystem() {
        clickingOnSprite(lastSystemSelected(), 1, false, true);
        repaint();
    }
    @Override
    public IMappedObject gridOrigin() {
        if (!map.showGridCircular())
            return null;
        Sprite spr = clickedSprite();
        if (spr instanceof IMappedObject) 
            return (IMappedObject) spr;
        return null;        
    }
    @Override
    public void animate() {
        // stop animating while number-crunching during next turn
        if (!displayNextTurnNotice()) {
            map.animate();
            displayPanel.animate();
        }
    }
    @Override
    public void paintOverMap(GalaxyMapPanel ui, Graphics2D g) {
        nextTurnControls.clear();
        overlay.paintOverMap(this, ui, g);
    }
    public void advanceMap() {
        log("Advancing Main UI Map");
        overlay.advanceMap();
        map.hoverSprite = clickedSprite();
    }
    public void resumeTurn() {
        clearOverlay();        
        session().resumeNextTurnProcessing();
        repaint();
    }
    public void resumeOutsideTurn() {
        clearOverlay();        
        showDisplayPanel();
        repaint();
    }
    @Override
    public void drawAlerts(Graphics2D g) {
        if (!showAlerts())
            return;
        GameAlert alert = session().currentAlert();

        int x = getWidth() - scaled(255);
        int y = getHeight() - scaled(168);
        int w = scaled(250);
        int h = s100;

        if (alertBack == null) {
            float[] dist = {0.0f, 1.0f};
            Color topC = new Color(219,135,8);
            Color botC = new Color(254,174,45);
            Point2D start = new Point2D.Float(0, y);
            Point2D end = new Point2D.Float(0, y+h);
            Color[] colors = {topC, botC };
            alertBack = new LinearGradientPaint(start, end, dist, colors);
        }
        g.setPaint(alertBack);
        g.fillRoundRect(x, y, w, h, s5, s5);
        alertDismissSprite.setBounds(x, y, w, h);

        if (alertDismissSprite.hovering()) {
            Stroke prev = g.getStroke();
            g.setColor(Color.yellow);
            g.setStroke(stroke2);
            g.drawRoundRect(x, y, w, h, s5, s5);
            g.setStroke(prev);
        }

        int num = session().numAlerts();
        int count = session().viewedAlerts()+1;
        String title = num == 1 ? text("MAIN_ALERT_TITLE") : text("MAIN_ALERT_TITLE_COUNT", count, num);
        int x1 = x+scaled(10);
        int y1 = y+scaled(20);

        g.setColor(Color.black);
        g.setFont(narrowFont(18));
        drawString(g,title, x1, y1);
        
        String yearStr = displayYearOrTurn();
        g.setFont(narrowFont(16));
        int yearW = g.getFontMetrics().stringWidth(yearStr);
        drawString(g,yearStr, x+w-s5-yearW, y+h-s5);

        g.setFont(narrowFont(16));
        List<String> descLines = wrappedLines(g, alert.description(), scaled(230));
        y1 += scaled(17);
        for (String line: descLines) {
            drawString(g,line, x1, y1);
            y1 += scaled(16);
        }
    }
    private void loadEmpireColonyHelpFrame1() {
        HelpUI helpUI = RotPUI.helpUI();

        int w = getWidth();
        int h = getHeight();
        helpUI.clear();
        HelpSpec s0 = helpUI.addBlueHelpText(s100, s10, scaled(350), 2, text("MAIN_HELP_ALL"));
        s0.setLine(s100, s25, s30, s25);

        int x1 = w-scaled(779);
        int w1 = scaled(430);
        int y1 = scaled(140);
        HelpSpec sp1 = helpUI.addBlueHelpText(x1, y1, w1, 4, text("MAIN_HELP_1A"));
        y1 += (sp1.height()+s10);
        HelpSpec sp2 = helpUI.addBlueHelpText(x1, y1, w1, 2, text("MAIN_HELP_1B"));
        sp2.setLine(x1+w1, y1+(sp2.height()/2), w-scaled(244), scaled(312));
        y1 += (sp2.height()+s5);
        HelpSpec sp3 = helpUI.addBlueHelpText(x1, y1, w1, 2, text("MAIN_HELP_1C"));
        sp3.setLine(x1+w1, y1+(sp3.height()/2), w-scaled(244), scaled(342));
        y1 += (sp3.height()+s5);
        HelpSpec sp4 = helpUI.addBlueHelpText(x1, y1, w1, 2, text("MAIN_HELP_1D"));
        sp4.setLine(x1+w1, y1+(sp4.height()/2), w-scaled(244), scaled(372));
        y1 += (sp4.height()+s5);
        HelpSpec sp5 = helpUI.addBlueHelpText(x1, y1, w1, 2, text("MAIN_HELP_1E"));
        sp5.setLine(x1+w1, y1+(sp5.height()/2), w-scaled(244), scaled(402));
        y1 += (sp5.height()+s5);
        HelpSpec sp6 = helpUI.addBlueHelpText(x1, y1, w1, 2, text("MAIN_HELP_1F"));
        sp6.setLine(x1+w1, y1+(sp6.height()/2), w-scaled(244), scaled(432));

        int x2 = w-scaled(299);
        int y2 = scaled(150);
        int w2 = scaled(280);
        HelpSpec sp7 = helpUI.addBlueHelpText(x2,y2,w2, 6, text("MAIN_HELP_1G"));
        sp7.setLine(x2+(w2/2), y2+sp7.height(), w-scaled(154), scaled(310));

        int x3 = w-scaled(304);
        int y3 = scaled(490);
        int w3 = scaled(300);
        HelpSpec sp8 = helpUI.addBlueHelpText(x3,y3,w3, 4, text("MAIN_HELP_1H"));
        sp8.setLine(x3+(w2*3/4), y3, w-scaled(54), scaled(430));        

        if (showTreasuryResearchBar()) {
            int x12 = scaled(115);
            int y12 = scaled(440);
            int w12 = scaled(220);
            HelpSpec sp12 = helpUI.addBlueHelpText(x12, y12, w12, 3, text("MAIN_HELP_2L"));
            sp12.setLine(x12, y12+(sp12.height()/2), s45, h-scaled(298));

            int x13 = scaled(120);
            int y13 = scaled(540);
            int w13 = scaled(220);
            HelpSpec sp13 = helpUI.addBlueHelpText(x13, y13, w13, 3, text("MAIN_HELP_2M"));
            sp13.setLine(x13, y13+(sp13.height()/2), s45, h-scaled(173));
        }
    }
    private void loadEmpireColonyHelpFrame2() {
        HelpUI helpUI = RotPUI.helpUI();
        
        int w = getWidth();

        helpUI.clear();
        HelpSpec s0 = helpUI.addBlueHelpText(s100, s10, scaled(350), 2, text("MAIN_HELP_ALL"));
        s0.setLine(s100, s25, s30, s25);

        int x1= w-scaled(700);
        int y1 = scaled(175);
        int w1= scaled(400);
        HelpSpec sp1 = helpUI.addBlueHelpText(x1, y1, w1, 2, text("MAIN_HELP_2A"));
        sp1.setLine(x1+w1, y1+sp1.height()-s5, w-scaled(169), y1+sp1.height()-s5, w-scaled(159), scaled(230));

        int x2= x1;
        int y2 = scaled(235);
        int w2= scaled(400);
        HelpSpec sp2 = helpUI.addBlueHelpText(x2, y2, w2, 2, text("MAIN_HELP_2B"));
        sp2.setLine(x2+w2, y2+s15, w-scaled(64), y2+s15, w-scaled(49), scaled(245));

        int x4= x1;
        int y4 = scaled(294);
        int w4= scaled(400);
        HelpSpec sp4 = helpUI.addBlueHelpText(x4, y4, w4, 3, text("MAIN_HELP_2D"));
        sp4.setLine(x4+w4, y4+(sp4.height()/2), w-scaled(69), scaled(265));

        int x3= x1;
        int y3 = scaled(372);
        int w3= scaled(400);
        HelpSpec sp3 = helpUI.addBlueHelpText(x3, y3, w3, 2, text("MAIN_HELP_2C"));
        sp3.setLine(x3+w3, y3+(sp3.height()/2), w-scaled(44), scaled(265));

        int x5= x1;
        int y5 = scaled(433);
        int w5 = scaled(400);
        HelpSpec sp5 = helpUI.addBlueHelpText(x5, y5, w5, 5, text("MAIN_HELP_2E"));
        sp5.setLine(x5+w5, y5+(sp5.height()/2), w-scaled(214), scaled(502));

        int x6 = x1+s50;
        int y6 = scaled(545); int y6a = scaled(555);
        int w6 = scaled(350);
        HelpSpec sp6 = helpUI.addBlueHelpText(x6, y6, w6, 2, text("MAIN_HELP_2F"));
        sp6.setLine(x6+w6, y6a, w-scaled(154), y6a, w-scaled(144), scaled(545));

        int x7 = x1+s50;
        int y7 = scaled(605);
        int w7 = scaled(350);
        HelpSpec sp7 = helpUI.addBlueHelpText(x7,y7,w7, 4, text("MAIN_HELP_2G"));
        sp7.setLine(x7+w7, y7+s10, w-scaled(244), scaled(580));

        int x8 = w-scaled(269);
        int y8 = scaled(605);
        int w8 = scaled(250);
        HelpSpec sp8 = helpUI.addBlueHelpText(x8,y8,w8, 3, text("MAIN_HELP_2H"));
        sp8.setLine(w-scaled(64), y8, w-scaled(64), scaled(582));    

        int x9 = x1;
        int y9 = scaled(115);
        int w9 = scaled(400);
        HelpSpec sp9 = helpUI.addBlueHelpText(x9, y9, w9, 2, text("MAIN_HELP_2I"));
        sp9.setLine(x9+w9, y9+(sp9.height()/2), w-scaled(174), scaled(205));

        int x10 = x1;
        int y10 = scaled(55);
        int w10 = scaled(400);
        HelpSpec sp10 = helpUI.addBlueHelpText(x10, y10, w10, 2, text("MAIN_HELP_2J"));
        sp10.setLine(x10+w10, y10+(sp10.height()/2), w-scaled(49), scaled(205));

        int x11 = w-scaled(179);
        int y11 = scaled(25);
        int w11 = scaled(170);
        HelpSpec sp11 = helpUI.addBlueHelpText(x11, y11, w11, 4, text("MAIN_HELP_2K"));
        sp11.setLine(w-scaled(34), y11+sp11.height(), w-scaled(25), scaled(170));

    }
    private void loadButtonBarHelpFrame() {
        HelpUI helpUI = RotPUI.helpUI();
        helpUI.clear();
        HelpSpec s0 = helpUI.addBlueHelpText(s100, s10, scaled(350), 2, text("MAIN_HELP_ALL"));
        s0.setLine(s100, s25, s30, s25);

        int h = getHeight();
        int w = getWidth();
        
        int buttonW = buttonPanel.buttonW();
        
        int x1 = scaled(25);
        int y1 = scaled(480);
        int w1 = scaled(210);
        int y1a = h - scaled(65);
        int x1a = x1+(w1/4);
        HelpSpec sp1 = helpUI.addBlueHelpText(x1, y1, w1, 4, text("MAIN_HELP_3A"));
        sp1.setLine(x1a, y1+sp1.height(), x1a, y1a);

        int x2 = x1+(buttonW*2/3);
        int y2 = scaled(590);
        int x2a = x2+(w1/2);
        HelpSpec sp2 = helpUI.addBlueHelpText(x2, y2, w1, 4, text("MAIN_HELP_3B"));
        sp2.setLine(x2a, y2+sp2.height(), x2a, y1a);

        int x3 = x2+buttonW;
        HelpSpec sp3 = helpUI.addBlueHelpText(x3, y1, w1, 4, text("MAIN_HELP_3C"));
        sp3.setLine(x3+(w1/2), y1+sp3.height(), x3+(w1/2), y1a);

        int x4 = x3+buttonW;
        HelpSpec sp4 = helpUI.addBlueHelpText(x4, y2, w1, 4, text("MAIN_HELP_3D"));
        sp4.setLine(x4+(w1/2), y2+sp4.height(), x4+(w1/2), y1a);

        int x5 = x4+buttonW;
        HelpSpec sp5 = helpUI.addBlueHelpText(x5, y1, w1, 4, text("MAIN_HELP_3E"));
        sp5.setLine(x5+(w1/2), y1+sp5.height(), x5+(w1/2), y1a);

        int x6 = x5+buttonW;
        HelpSpec sp6 = helpUI.addBlueHelpText(x6, y2, w1, 4, text("MAIN_HELP_3F"));
        sp6.setLine(x6+(w1/2), y2+sp6.height(), x6+(w1/2), y1a);

        int x7 = x6+buttonW;
        HelpSpec sp7 = helpUI.addBlueHelpText(x7, y1, w1, 4, text("MAIN_HELP_3G"));
        sp7.setLine(x7+(w1/2), y1+sp7.height(), x7+(w1/2), y1a);

        int x8 = w-scaled(264);
        int w8 = scaled(150);
        HelpSpec sp8 = helpUI.addBlueHelpText(x8, y2, w8, 3, text("MAIN_HELP_3H"));
        sp8.setLine(x8+(w8/2), y2+sp8.height(), x8+(w8/2), y1a);

        int x9 = w-scaled(220);
        int w9 = scaled(200);
        HelpSpec sp9 = helpUI.addBlueHelpText(x9, y1, w9, 4, text("MAIN_HELP_3I"));
        sp9.setLine(w-scaled(79), y1+sp9.height(), w-scaled(79), h-scaled(100));

        int x10 = scaled(115);
        int y10 = scaled(125);
        int w10 = scaled(250);
        HelpSpec sp10 = helpUI.addBlueHelpText(x10, y10, w10, 3, text("MAIN_HELP_3J"));
        sp10.setLine(x10, y10+(sp10.height()/2), s45, y10+(sp10.height()/2));

    }
    @Override
    public void keyPressed(KeyEvent e) {
        if (!overlay.handleKeyPress(e))
            overlayNone.handleKeyPress(e);
    }
}
