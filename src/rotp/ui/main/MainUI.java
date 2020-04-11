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
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLayeredPane;
import javax.swing.border.Border;
import rotp.Rotp;
import rotp.model.Sprite;
import rotp.model.combat.ShipCombatManager;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.EspionageMission;
import rotp.model.empires.SystemView;
import rotp.model.galaxy.IMappedObject;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesign;
import rotp.ui.BasePanel;
import rotp.ui.main.overlay.*;
import rotp.ui.map.IMapHandler;
import rotp.ui.notifications.GameAlert;
import rotp.ui.sprites.AlertDismissSprite;
import rotp.ui.sprites.ClickToContinueSprite;
import rotp.ui.sprites.FlightPathSprite;
import rotp.ui.sprites.YearDisplaySprite;
import rotp.util.ThickBevelBorder;

public class MainUI extends BasePanel implements IMapHandler {
    private static final long serialVersionUID = 1L;
    public static Color paneBackground = new Color(123,123,123);
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

    public static final Color greenAlertC  = new Color(0,255,0);
    public static final Color redAlertC    = new Color(255,0,0);
    public static final Color yellowAlertC = new Color(255,255,0);
    
    public static int panelWidth, panelHeight;
    static LinearGradientPaint alertBack;
    static Location center = new Location();
    
    JLayeredPane layers = new JLayeredPane();

    MapOverlayNone overlayNone;
    MapOverlayMemoryLow overlayMemoryLow;
    MapOverlayShipsConstructed overlayShipsConstructed;
    MapOverlay overlaySpiesCaptured;
    MapOverlayAllocateSystems overlayAllocateSystems;
    MapOverlaySystemsScouted overlaySystemsScouted;
    MapOverlayEspionageMission overlayEspionageMission;
    MapOverlayColonizePrompt overlayColonizePrompt;
    MapOverlayBombardPrompt overlayBombardPrompt;
    MapOverlayBombardedNotice overlayBombardedNotice;
    MapOverlayShipCombatPrompt overlayShipCombatPrompt;
    MapOverlayAdvice overlayAdvice;
    AlertDismissSprite alertDismissSprite;
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

    public Border paneBorder()               { return null;   }
    public static Color shadeBorderC()       { return shadeBorderC; }
    public static Color paneBackground()     { return paneBackground; }
    public static Color paneHighlight()      { return paneBackgroundHighlight; }
    public static Color namePaneHighlight()  { return namePaneBackgroundHighlight; }

    public SpriteDisplayPanel displayPanel() { return displayPanel; }
    public void hideDisplayPanel()           { displayPanel.setVisible(false); }
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
        overlayShipsConstructed = new MapOverlayShipsConstructed(this);
        overlaySpiesCaptured = new MapOverlaySpiesCaptured(this);
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
        map.clearRangeMap();
        if (pauseNextTurn)
            buttonPanel.init();
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
        map.clearRangeMap();
        clickedSprite((Sprite) sessionVar("MAINUI_SAVE_CLICKED"));
        showDisplayPanel();
    }
    public void repaintAllImmediately() {
        paintImmediately(0,0,getWidth(),getHeight());
    }
    public void addNextTurnControl(Sprite ms) { nextTurnControls.add(ms); }
    final protected void addMapControls() {
        alertDismissSprite = new AlertDismissSprite(this);
        baseControls.add(new YearDisplaySprite(this));
        baseControls.add(alertDismissSprite);
    }
    public boolean showAlerts() {
        return (session().currentAlert() != null) && displayPanel.isVisible();
    }
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
    public void showAdvice(String key, String var1, String var2, String var3) {
        overlay = overlayAdvice;
        overlayAdvice.init(key, var1, var2, var3);
        showAdvice = true;
        repaint();
    }
    public void showMemoryLowPrompt() {
        overlay = overlayMemoryLow;
        overlayMemoryLow.init();
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
    public void showSpiesCaptured() {
        overlay = overlaySpiesCaptured;
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
    public void showSystemsScouted(List<StarSystem> newSystems) {
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
        int w = scaled(Rotp.IMG_W);
        int h = scaled(Rotp.IMG_H);

        int displayW = panelWidth;
        int displayH = panelHeight;
        displayPanel = new SpriteDisplayPanel(this);
        displayPanel.setBorder(newLineBorder(shadeBorderC,5));
        displayPanel.setBounds(w-displayW-s25,s15,displayW,displayH);

        map = new GalaxyMapPanel(this);
        map.setBorder(paneBorder());
        map.setBounds(0,0,w,h);

        int buttonH = s60;
        buttonPanel = new MainButtonPanel(this);
        buttonPanel.setBounds(0,h-s15-buttonH,w,buttonH);

        setLayout(new BorderLayout());
        add(layers, BorderLayout.CENTER);

        layers.add(buttonPanel, 0);
        layers.add(displayPanel, 0);
        layers.add(map, -1);
        Border line1 = newLineBorder(newColor(60,60,60),2);
        Border line2 = newLineBorder(newColor(0,0,0),8);
        Border compound1 = BorderFactory.createCompoundBorder(line2, line1);
        setBorder(compound1);
        setOpaque(false);
    }
    public boolean enableButtons()   { return true; }
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
    public void hoveringSprite(Sprite s)     { sessionVar("MAINUI_HOVERING_SPRITE", s); }
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
    }
    public void resumeTurn() {
        clearOverlay();
        session().resumeNextTurnProcessing();
        repaint();
    }
    @Override
    public void drawAlerts(Graphics2D g) {
        if (!showAlerts())
            return;
        GameAlert alert = session().currentAlert();

        int x = getWidth() - scaled(282);
        int y = getHeight() - scaled(168);
        int w = scaled(250);
        int h = s80;

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
        g.drawString(title, x1, y1);

        g.setFont(narrowFont(16));
        List<String> descLines = wrappedLines(g, alert.description(), scaled(240));
        y1 += scaled(17);
        for (String line: descLines) {
            g.drawString(line, x1, y1);
            y1 += scaled(16);
        }
    }
    @Override
    public void keyPressed(KeyEvent e) {
        if (!overlay.handleKeyPress(e))
            overlayNone.handleKeyPress(e);
    }
    class MapOverlaySpiesCaptured extends MapOverlay {
        MainUI parent;
        ClickToContinueSprite clickSprite;
        Border shipBoxOuterBorder;
        final Color spyTitleC = new Color(143,142,184);
        final Color shipBoxBackground = new Color(61,48,28);
        final Color shipLightC = new Color(76,57,41);
        final Color shipLighterC = new Color(94,71,53);
        final Color shipDarkC = new Color(42,26,19);
        final Color shipDarkerC = new Color(22,14,4);
        public MapOverlaySpiesCaptured(MainUI p) {
            parent = p;
            shipBoxOuterBorder = new ThickBevelBorder(5,shipLighterC,shipLightC,shipDarkerC, shipDarkC,shipDarkerC, shipDarkC,shipLighterC,shipLightC);
            clickSprite = new ClickToContinueSprite(parent);
        }
        @Override
        public boolean hoveringOverSprite(Sprite o) { return false; }
        @Override
        public void advanceMap() {
            resumeTurn();
        }
        @Override
        public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
            List<Empire> empires = player().contactedEmpires();
            int lineH = s25;
            int spyBoxW = scaled(400);
            int spyBoxH = scaled(100)+(lineH*empires.size());
            int cellMargin = s20;
            int borderW = s5;

            int x0 = (ui.getWidth()-spyBoxW)/2;
            int y0 = (ui.getHeight()-spyBoxH)*2/5;

            g.setColor(shipBoxBackground);
            g.fillRect(x0, y0, spyBoxW, spyBoxH);

            shipBoxOuterBorder.paintBorder(ui, g, x0, y0, spyBoxW, spyBoxH);
            g.setColor(Color.white);
            g.setFont(narrowFont(26));

            int w1 = spyBoxW-(2*(borderW+cellMargin));
            g.setColor(spyTitleC);

            String title = text("MAIN_SPIES_CAUGHT_TITLE");
            int x1 = x0+borderW+cellMargin;
            int y1 = y0+borderW+cellMargin+s10;
            drawBorderedString(g, title, 2, x1, y1, Color.black, Color.white);

            String yours = text("MAIN_SPIES_CAUGHT_YOURS");
            int sw2 = g.getFontMetrics().stringWidth(yours);
            int x2 = x1 + w1/2;
            int x2b = x2 + (sw2/2);
            int y2 = y1;
            drawBorderedString(g, yours, 2, x2, y2, Color.black, Color.white);

            String theirs = text("MAIN_SPIES_CAUGHT_THEIRS");
            int sw3 = g.getFontMetrics().stringWidth(theirs);
            int x3 = x1 + w1*3/4;
            int x3b = x3+(sw3/2);
            int y3 = y1;
            drawShadowedString(g, theirs, 2, x3, y3, Color.black, Color.white);

            g.setFont(narrowFont(24));
            int y4 = y3+s10+lineH;
            g.setColor(spyTitleC);
            for (Empire emp: empires) {
                EmpireView v = player().viewForEmpire(emp);
                g.drawString(v.empire().raceName(), x1, y4);
                g.drawString(str(v.spies().spiesLost()), x2b, y4);
                g.drawString(str(v.otherView().spies().spiesLost()), x3b, y4);
                y4 += lineH;
            }

            g.setFont(narrowFont(20));
            g.setColor(spyTitleC);
            String cont = text("CLICK_CONTINUE");
            int sw4 = g.getFontMetrics().stringWidth(cont);
            int x4 = (ui.getWidth()-sw4)/2;
            g.drawString(cont, x4, y0+spyBoxH-s11);

            parent.addNextTurnControl(clickSprite);
        }
        @Override
        public boolean handleKeyPress(KeyEvent e) {
            switch(e.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:
                    softClick();
                    advanceMap();
                    return true;
                default:
                    return false;
            }
        }
    }
}
