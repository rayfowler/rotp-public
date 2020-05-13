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
package rotp.ui.map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLayeredPane;
import javax.swing.border.Border;
import rotp.Rotp;
import rotp.model.Sprite;
import rotp.model.colony.Colony;
import rotp.model.empires.Empire;
import rotp.model.empires.SystemView;
import rotp.model.events.RandomEvent;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.Ship;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.Transport;
import rotp.model.tech.Tech;
import rotp.ui.BasePanel;
import rotp.ui.ExitButton;
import rotp.ui.RotPUI;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.FlightPathSprite;
import rotp.ui.sprites.MapControlSprite;

public final class SystemsUI extends BasePanel implements IMapHandler, ActionListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    public static final Color backHiC = new Color(178,124,87);
    public static final Color backLoC = new Color(112,85,68);
    public static final Color rallyBackHiC = new Color(192,139,105);
    public static final Color rallyBackLoC = new Color(77,55,34);
    public static final Color rallyBorderC = new Color(208,172,148);
    public static final Color darkShadingC = new Color(50,50,50);
    public static final Color darkBrown = new Color(45,14,5);
    public static final Color brown = new Color(64,24,13);
    public static final Color sliderBoxBlue = new Color(34,140,142);
    public static final Color mapMask = new Color(0,0,0,192);
    public static final Color unselectedTabC = new Color(112,85,68);
    public static final Color selectedTabC = new Color(178,124,87);

    private static final String exploreTab = "Explore";
    private static final String expandTab = "Expand";
    private static final String exploitTab = "Exploit";
    private static final String exterminateTab = "Exterminate";
    private String selectedTab = exploreTab;

    private MainTitlePanel titlePanel;
    public static SystemsUI instance;

    private int pad = 10;
    private GalaxyMapPanel map;
    private LinearGradientPaint backGradient;
    private SystemInfoPanel displayPanel;
    private final List<Sprite> controls = new ArrayList<>();

    // public for all
    public StarSystem hoverSystem;
    public StarSystem targetSystem;

    JLayeredPane layers = new JLayeredPane();
    public boolean animate = true;

    public int SIDE_PANE_W;
    private LinearGradientPaint grayBackC;
    private LinearGradientPaint redBackC;
    private LinearGradientPaint greenBackC;
    private LinearGradientPaint brownBackC;

    public SystemsUI() {
        instance = this;
        pad = s10;
        initModel();
    }
    public void init() {
        if (grayBackC == null)
            initGradients();

        // reset map everytime we open
        sessionVar("SYSTEMUI_MAP_INITIALIZED", false);
        targetSystem = null;
        map.clearRangeMap();
    }
    public void clickSystem(StarSystem v, int count) {

    }
    public void clickFleet(ShipFleet fl) {
    }
    public void drawBrownButton(Graphics2D g, String label, Rectangle actionBox, Shape hoverBox, int y) {
        drawButton(g, brownBackC, label, actionBox, hoverBox, y);
    }
    public void drawGrayButton(Graphics2D g, String label, Rectangle actionBox, Shape hoverBox, int y) {
        drawButton(g, grayBackC, label, null, hoverBox, y);
    }
    public void drawGreenButton(Graphics2D g, String label, Rectangle actionBox, Shape hoverBox, int y) {
        drawButton(g, greenBackC, label, actionBox, hoverBox, y);
    }
    public void drawRedButton(Graphics2D g, String label, Rectangle actionBox, Shape hoverBox, int y) {
        drawButton(g, redBackC, label, actionBox, hoverBox, y);
    }
    public void drawButton(Graphics2D g, LinearGradientPaint gradient, String label, Rectangle actionBox, Shape hoverBox, int y) {
        int buttonH = s27;
        int x1 = s3;
        int w1 = SIDE_PANE_W-s18;
        if (actionBox != null)
            actionBox.setBounds(x1,y,w1,buttonH);
        g.setColor(SystemPanel.buttonShadowC);
        g.fillRoundRect(x1+s1,y+s3,w1,buttonH,s8,s8);
        g.fillRoundRect(x1+s2,y+s4,w1,buttonH,s8,s8);

        g.setPaint(gradient);
        g.fillRoundRect(x1,y,w1,buttonH,s5,s5);

        boolean hovering = (actionBox != null) && (actionBox == hoverBox);
        Color c0 = (actionBox == null) ? SystemPanel.grayText : hovering ? SystemPanel.yellowText : SystemPanel.whiteText;

        g.setFont(narrowFont(18));
        int sw = g.getFontMetrics().stringWidth(label);
        int x0 = x1+((w1-sw)/2);
        drawShadowedString(g, label, 3, x0, y+buttonH-s7, SystemPanel.textShadowC, c0);

        g.setColor(c0);
        Stroke prev2 = g.getStroke();
        g.setStroke(stroke1);
        g.drawRoundRect(x1+s1,y,w1-s2,buttonH,s5,s5);
        g.setStroke(prev2);
    }
    private void initGradients() {
        SIDE_PANE_W = scaled(250);
        int w = getWidth();
        int leftM = s2;
        int rightM = w-s2;
        Point2D start = new Point2D.Float(leftM, 0);
        Point2D end = new Point2D.Float(rightM, 0);
        float[] dist = {0.0f, 0.5f, 1.0f};

        Color brownEdgeC = new Color(100,70,50);
        Color brownMidC = new Color(161,110,76);
        Color[] brownColors = {brownEdgeC, brownMidC, brownEdgeC };

        Color grayEdgeC = new Color(59,59,59);
        Color grayMidC = new Color(92,92,92);
        Color[] grayColors = {grayEdgeC, grayMidC, grayEdgeC };

        Color greenEdgeC = new Color(44,59,30);
        Color greenMidC = new Color(71,93,48);
        Color[] greenColors = {greenEdgeC, greenMidC, greenEdgeC };

        Color redEdgeC = new Color(92,20,20);
        Color redMidC = new Color(117,42,42);
        Color[] redColors = {redEdgeC, redMidC, redEdgeC };

        brownBackC = new LinearGradientPaint(start, end, dist, brownColors);
        grayBackC = new LinearGradientPaint(start, end, dist, grayColors);
        greenBackC = new LinearGradientPaint(start, end, dist, greenColors);
        redBackC = new LinearGradientPaint(start, end, dist, redColors);
    }
    @Override
    public void paint(Graphics g) {
        checkMapInitialized();
        super.paint(g);
    }
    @Override
    public void paintOverMap(GalaxyMapPanel ui, Graphics2D g) {
        int w = ui.getWidth();
        int h = ui.getHeight();
                           
        if (backGradient == null) {
            Color c0 = Color.black;
            Color c1 = new Color(71,53,39);
            Point2D start = new Point2D.Float(s10, h-scaled(200));
            Point2D end = new Point2D.Float(s10, h-s20);
            float[] dist = {0.0f, 0.7f, 1.0f};
            Color[] colors = {c0, c0, c1 };
            backGradient = new LinearGradientPaint(start, end, dist, colors);
        }
        g.setPaint(backGradient);
        Area a = new Area(new Rectangle(0,0,w,h));
        a.subtract(new Area(new Rectangle(s10, s40,w-scaled(295), h-s70)));
        g.fill(a);
    }
    @Override
    public Color shadeC()                          { return rallyBackLoC; }
    @Override
    public Color backC()                           { return rallyBackHiC; }
    @Override
    public Color lightC()                          { return rallyBorderC; }
    @Override
    public boolean drawMemory()            { return true; }
    @Override
    public Color flagColor(StarSystem s)             { return s.notes().isEmpty() ? null : Color.yellow; }
    @Override
    public GalaxyMapPanel map()         { return map; }
    private void initModel() {
        int w = scaled(Rotp.IMG_W);
        int h = scaled(Rotp.IMG_H);
        int rightPaneW = scaled(250);

        setBackground(Color.black);
        Border emptyBorder = newEmptyBorder(0,pad,pad,pad);
        setBorder(emptyBorder);

        map = new GalaxyMapPanel(this);
        map.setBorder(null);
        map.setBounds(0,0,w,h);

        titlePanel = new MainTitlePanel("SYSTEMS_TITLE");
        titlePanel.setBounds(0,0,w-rightPaneW-s25, s45);
        
        displayPanel = new SystemInfoPanel(this);
        
        BasePanel rightPanel = new BasePanel();
        rightPanel.setBounds(w-rightPaneW-s25,0,rightPaneW,h-s20);
        rightPanel.setOpaque(false);
        rightPanel.setLayout(new BorderLayout(0, pad));
        rightPanel.add(displayPanel, BorderLayout.CENTER);
        rightPanel.add(new ExitFleetsButton(w, s60, s10, s2), BorderLayout.SOUTH);
        
        setLayout(new BorderLayout());
        add(layers, BorderLayout.CENTER);
        
        layers.add(titlePanel, new Integer(3));
        layers.add(rightPanel,new Integer(2));
        layers.add(map, new Integer(0));
        Border line1 = newLineBorder(newColor(60,60,60),2);
        Border line2 = newLineBorder(newColor(0,0,0),8);
        Border compound1 = BorderFactory.createCompoundBorder(line2, line1);
        setBorder(compound1);
        setOpaque(false);

        addMouseWheelListener(this);
    }
    @Override
    public boolean animating()    { return animate; }
    public void clearMapSelections() {
        targetSystem = null;
        hoverSystem = null;
        List<FlightPathSprite> paths = FlightPathSprite.workingPaths();
        paths.clear();
    }
    @Override
    public void checkMapInitialized() {
        Boolean inited = (Boolean) sessionVar("SYSTEMSUI_MAP_INITIALIZED");
        if ((inited == null) || (inited == false)) {
            // init appropriate scale and bounds
            //map.toggleFlightPathDisplay(false);
            sessionVar("SYSTEMSUI_MAP_INITIALIZED", true);
        }
    }
    @Override
    public Color alertColor(SystemView sv) { 
        switch(selectedTab) {
            case exploreTab:     return exploreAlertColor(sv);
            case expandTab:      return expandAlertColor(sv);
            case exploitTab:     return exploitAlertColor(sv);
            case exterminateTab: return exterminateAlertColor(sv);
        }
        return null; 
    }
    public StarSystem lastSystemSelected()    { return (StarSystem) sessionVar("MAINUI_SELECTED_SYSTEM"); }
    public void lastSystemSelected(Sprite s)  { sessionVar("MAINUI_SELECTED_SYSTEM", s); }
    public StarSystem systemToDisplay() {
        Sprite spr = hoveringSprite();
        if (spr instanceof StarSystem)
            return (StarSystem) spr;
        else
            return lastSystemSelected();
    }
    @Override
    public boolean canChangeMapScales()                 { return true; }
    @Override
    public void clickingNull(int cnt, boolean right) {
        displayPanel.useNullClick(cnt, right);
    };
    @Override
    public void clickingOnSprite(Sprite o, int count, boolean rightClick, boolean click) {
        if ((o instanceof ShipFleet) || (o instanceof Transport) || (o instanceof FlightPathSprite))
            return;
        
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

        if ((o == null) || (o instanceof StarSystem) || (o instanceof MapControlSprite))
            lastHoveringSprite(o);
        else
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
    public List<Sprite> controlSprites()      { return controls; }
    @Override
    public Border mapBorder()       { return null; }
    @Override
    public float startingScalePct() {
        return (player().maxY()-player().minY()) / map().sizeY();
    }
    @Override
    public void animate() {
        if (animate)
            map.animate();
    }
    private String randomEventStatus(SystemView sv) {
        if (!sv.scouted())
            return "";
        StarSystem sys = sv.system();
        if (sys.hasEvent()) {
            RandomEvent ev = galaxy().events().activeEventForKey(sys.eventKey());
            if (ev != null)
                return ev.statusMessage();
        }
        return "";
    }
    private Color exploreAlertColor(SystemView sv) { 
        String eventMessage = randomEventStatus(sv);
        if (!eventMessage.isEmpty()) {
            if (sv.empire() == player())
                return MainUI.redAlertC;
            else
                return MainUI.yellowAlertC;
        }
        
        if (sv.scouted())
            return null;
        
        if (sv.isGuarded()) 
            return MainUI.redAlertC;
        
        float sysDistance = sv.distance();
        Empire pl = player();
        if (sysDistance <= pl.scoutRange()) {
            if (!sv.isColonized() || pl.alliedWith(sv.empId()))
                return MainUI.greenAlertC;
            else if (pl.atWarWith(sv.empId()))
                return MainUI.redAlertC;
            else
                return MainUI.yellowAlertC;
        }
        
        String neededTechId = pl.rangeTechNeededToScout(sv.sysId);
        if (neededTechId != null) 
            return MainUI.yellowAlertC;
        
        return null; 
    }
    private Color expandAlertColor(SystemView sv) { 
        if (!sv.scouted())
            return null;   
                
        String eventMessage = randomEventStatus(sv);
        if (!eventMessage.isEmpty()) {
            if (sv.empire() == player())
                return MainUI.redAlertC;
        }

        if (sv.isColonized()) 
            return null;
        
        float sysDistance = sv.distance();
        Empire pl = player();
        if ((sysDistance <= pl.shipRange()) && pl.canColonize(sv.sysId)) 
            return MainUI.greenAlertC;
        
        String rangeTech = pl.rangeTechNeededToReach(sv.sysId);
        String envTech = pl.environmentTechNeededToColonize(sv.sysId);   
        if ((rangeTech != null) && (envTech != null))
            return MainUI.yellowAlertC;
        if ((envTech != null) && (sysDistance <= pl.shipRange()))
            return MainUI.yellowAlertC;
        if ((rangeTech != null) && pl.canColonize(sv.sysId)) 
            return MainUI.yellowAlertC;
        return null; 
    }
    private Color exploitAlertColor(SystemView sv) { 
        if (sv.empire() != player())
            return null;
        
        String eventMessage = randomEventStatus(sv);
        if (!eventMessage.isEmpty()) {
            if (sv.empire() == player())
                return MainUI.redAlertC;
        }
        
        Colony col = sv.system().colony();
        
        if (col.inRebellion())
            return MainUI.redAlertC;
        
        if (col.creatingWaste()) 
            return MainUI.redAlertC;
        
        int pct = (int) (100*col.currentProductionCapacity());
        if (pct < 34)
            return MainUI.redAlertC;
        else if (pct < 67)
            return MainUI.yellowAlertC;
        else if (pct < 100)
            return MainUI.greenAlertC;
        return null; 
    }
    private Color exterminateAlertColor(SystemView sv)       { 
        Empire pl = player();
        if (sv.distance() > pl.scoutRange())
            return null;
        
        // show enemy colonies as yellow
        Empire sysEmp = sv.empire();
        int sysEmpId = sv.empId();
        if (pl.atWarWith(sysEmpId))
            return MainUI.yellowAlertC;
        
        // deal with enemy fleets orbiting systems around us
        List<ShipFleet> fleets = sv.orbitingFleets();
        for (ShipFleet fl: fleets) {
            if (fl.isPotentiallyArmed(pl)) {
                if (pl.atWarWith(fl.empId())) { 
                    if (sysEmp == null)
                        return MainUI.yellowAlertC; // enemy fleets around empty systems          
                    if (sysEmp.isPlayer())
                        return MainUI.redAlertC;    // enemy fleets around player colonies
                    else if (pl.alliedWith(sysEmpId))
                        return MainUI.yellowAlertC; // enemy fleets around ally systems 
                    else
                        return MainUI.yellowAlertC; // enemy fleets around other systems 
                }  
            }
        }
        
        // if we can see ship ETAs, look for armed enemy ships approaching
        // player or allied colonies
        if (pl.knowShipETA() && pl.alliedWith(sysEmpId)) {
            for (Ship sh: pl.visibleShips()) {
                if ((sh.destSysId() == sv.sysId) && pl.atWarWith(sh.empId()) && sh.isPotentiallyArmed(pl)) {
                    if (sysEmp.isPlayer())
                        return MainUI.redAlertC; // enemy fleets approaching player colonies
                    else 
                        return MainUI.yellowAlertC; // enemy fleets approaching player allied colonies
                }
            }            
        }
        // for player systems, highlight those with no bases or insufficient shields
        if (sysEmp == null)
            return null;
        if (!sysEmp.isPlayer())
            return null;

        if (!sv.colony().defense().isCompleted())
            return MainUI.greenAlertC;
        
        return null; 
    } 
    public String alertDescription(SystemView sv) { 
        switch(selectedTab) {
            case exploreTab:     return exploreAlertDescription(sv);
            case expandTab:      return expandAlertDescription(sv);
            case exploitTab:     return exploitAlertDescription(sv);
            case exterminateTab: return exterminateAlertDescription(sv);
        }
        return null; 
    }
    private String exploreAlertDescription(SystemView sv) { 
        String eventMessage = randomEventStatus(sv);
        if (!eventMessage.isEmpty()) 
             return eventMessage; 
        
        if (sv.scouted())
            return null;      
        if (sv.isGuarded()) 
            return text("SYSTEMS_UNSCOUTED_GUARDED");
        
        float sysDistance = sv.distance();
        Empire pl = player();
        if (sysDistance <= pl.scoutRange()) {
            if (!sv.isColonized() || pl.alliedWith(sv.empId()))
                return text("SYSTEMS_UNSCOUTED");
            else if (pl.atWarWith(sv.empId()))
                return text("SYSTEMS_UNSCOUTED_ENEMY");
            else
                return text("SYSTEMS_UNSCOUTED_COLONIZED");
        }
        
        String neededTechId = pl.rangeTechNeededToScout(sv.sysId);
        if (neededTechId != null) {
            Tech t = tech(neededTechId);
            return text("SYSTEMS_UNSCOUTED_NEED_TECH", t.name());
        }     
        return text("SYSTEMS_UNSCOUTED_UNREACHABLE"); 
    }
    private String expandAlertDescription(SystemView sv) { 
        if (!sv.scouted())
            return null;      
        String eventMessage = randomEventStatus(sv);
        if (!eventMessage.isEmpty()) {
             if (sv.empire() == player())
                 return eventMessage;
        }

        if (sv.isColonized()) 
            return null;
        
        float sysDistance = sv.distance();
        Empire pl = player();
        if ((sysDistance <= pl.shipRange()) && pl.canColonize(sv.sysId)) 
            return text("SYSTEMS_CAN_COLONIZE");
        
        String rangeTech = pl.rangeTechNeededToReach(sv.sysId);
        String envTech = pl.environmentTechNeededToColonize(sv.sysId);  
        
        if ((rangeTech != null) && (envTech != null)) {
            Tech t1 = tech(envTech);
            Tech t2 = tech(rangeTech);
            return text("SYSTEMS_UNCOLONIZED_NEED_TECHS", t1.name(), t2.name());
        }
        if ((envTech != null) && (sysDistance <= pl.shipRange())){
            Tech t1 = tech(envTech);
            return text("SYSTEMS_UNCOLONIZED_NEED_TECH", t1.name());            
        }
        if ((rangeTech != null) && pl.canColonize(sv.sysId)) {
            Tech t1 = tech(rangeTech);
            return text("SYSTEMS_UNCOLONIZED_NEED_TECH", t1.name());            
        }
        return text("SYSTEMS_UNCOLONIZEABLE"); 
    }
    private String exploitAlertDescription(SystemView sv) { 
        Empire sysEmp = sv.empire();
        if ((sysEmp == null) || !sysEmp.isPlayer())
            return null;
       
        String eventMessage = randomEventStatus(sv);
        if (!eventMessage.isEmpty()) {
             if (sv.empire() == player())
                 return eventMessage;
        }
        
        Colony col = sv.system().colony();
        if (col.inRebellion())
            return text("SYSTEMS_STATUS_REBELLION");
        
        if (col.creatingWaste()) 
            return text("SYSTEMS_EXPLOIT_WASTE");
        
        int pct = (int) (100*col.currentProductionCapacity());
        if (pct < 34)
            return text("SYSTEMS_EXPLOIT_PCT", pct); 
        else if (pct < 67)
            return text("SYSTEMS_EXPLOIT_PCT", pct); 
        else if (pct < 100)
            return text("SYSTEMS_EXPLOIT_PCT", pct); 
        return text("SYSTEMS_EXPLOIT_COMPLETE"); 
    }
    private String exterminateAlertDescription(SystemView sv) { 
        Empire pl = player();
        if (sv.distance() > pl.scoutRange())
            return null;
        
        // show enemy colonies as yellow
        Empire sysEmp = sv.empire();
        if (pl.atWarWith(sv.empId()))
            return text("SYSTEMS_EXT_ENEMY");
        
        // deal with enemy fleets orbiting systems around us
        List<ShipFleet> fleets = sv.orbitingFleets();
        for (ShipFleet fl: fleets) {
            if (fl.isPotentiallyArmed(pl)) {
                if (pl.atWarWith(fl.empId())) { 
                    if (sysEmp == null)
                        return text("SYSTEMS_EXT_ENEMY_FLEET");
                    if (sysEmp.isPlayer())
                        return text("SYSTEMS_EXT_ENEMY_FLEET_PLAYER");
                    else if (pl.alliedWith(sv.empId()))
                        return text("SYSTEMS_EXT_ENEMY_FLEET_ALLY");
                    else
                        return text("SYSTEMS_EXT_ENEMY_FLEET");
                }  
            }
        }
        
        // if we can see ship ETAs, look for armed enemy ships approaching
        // player or allied colonies
        if (pl.knowShipETA() && pl.alliedWith(sv.empId())) {
            for (Ship sh: pl.visibleShips()) {
                if ((sh.destSysId() == sv.sysId) && pl.atWarWith(sh.empId()) && sh.isPotentiallyArmed(pl)) {
                    if (sysEmp.isPlayer())
                        return text("SYSTEMS_EXT_INC_FLEET_PLAYER");
                    else 
                        return text("SYSTEMS_EXT_INC_FLEET_ALLY");
                }
            }               
        }
        // for player systems, highlight those with no bases or insufficient shields
        if ((sysEmp == null) || !sysEmp.isPlayer())
            return null;

        if (!sv.colony().defense().isCompleted())
            return text("SYSTEMS_EXT_NEED_DEFENSE");
        
        return null; 
    }
    @Override
    public String systemLabel2(StarSystem sys) {
        Empire pl = player();
        int rng = pl.scoutRange();
        SystemView sv = pl.sv.view(sys.id);
        switch(selectedTab) {
            case exploreTab:
                int dist = (int) Math.ceil(sv.distance());
                return (dist > 0) && (dist < rng+rng) ? text("SYSTEMS_RANGE", str(dist)) : "";
            case expandTab:
                if (!sv.scouted())
                    return "";
                else if (sv.currentSize() == 0)
                    return text("SYSTEMS_ENVIRONMENT_SIZE", sv.planetType().name(), "");
                else
                    return text("SYSTEMS_ENVIRONMENT_SIZE", sv.planetType().name(), str(sv.currentSize()));
            case exploitTab: 
                return text(sv.resourceType());
            case exterminateTab: 
                int bases = sv.bases();
                int shield = sv.shieldLevel();
                if ((bases == 0) && (shield == 0))
                    return "";
                String str1 = shield == 0 ? "" : text("SYSTEMS_SHIELD", str(shield));
                String str2 = bases == 0 ? "" : text("SYSTEMS_BASES", str(bases));
                return shield == 0 ? str2 : str1+" "+str2;
        }
        return "";     }
    @Override
    public void keyPressed(KeyEvent e) {
        switch(e.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:
                    clearMapSelections();
                    buttonClick();
                    RotPUI.instance().selectMainPanel(false);
                    return;
                case KeyEvent.VK_EQUALS:
                    if (e.isShiftDown())  {
                        softClick();
                        map().adjustZoom(-1);
                    }
                    return;
                case KeyEvent.VK_MINUS:
                    softClick();
                    map().adjustZoom(1);
                    return;
                case KeyEvent.VK_UP:
                    softClick();
                    map().dragMap(0, s40);
                    return;
                case KeyEvent.VK_DOWN:
                    softClick();
                    map().dragMap(0, -s40);
                    return;
                case KeyEvent.VK_LEFT:
                    softClick();
                    map().dragMap(s40, 0);
                    return;
                case KeyEvent.VK_RIGHT:
                    softClick();
                    map().dragMap(-s40, 0);
                    return;
        }
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

    }
    class MainTitlePanel extends BasePanel implements MouseMotionListener, MouseListener {
        private static final long serialVersionUID = 1L;
        String titleKey;
        public MainTitlePanel(String s) {
            titleKey = s;
            initModel();
        }
        Rectangle hoverBox;
        Rectangle exploreBox = new Rectangle();
        Rectangle expandBox = new Rectangle();
        Rectangle exploitBox = new Rectangle();
        Rectangle exterminateBox = new Rectangle();
        Area textureArea;

        private void initModel() {
            setOpaque(false);
            setPreferredSize(new Dimension(getWidth(),s45));
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        @Override
        public Area textureArea()       { return textureArea; }
        @Override
        public String textureName()     { return TEXTURE_BROWN; }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;

            int w = getWidth();
            int h = getHeight();
            int gap = s10;
            int x0 = gap;
            int y0 = h - s10;
            String title = text(titleKey);
            String dipLabel = text("SYSTEMS_TAB_EXPLORE");
            String intLabel = text("SYSTEMS_TAB_EXPAND");
            String milLabel = text("SYSTEMS_TAB_EXPLOIT");
            String statusLabel = text("SYSTEMS_TAB_EXTERMINATE");

            g.setColor(SystemPanel.orangeText);
            g.setFont(narrowFont(32));
            int titleW = g.getFontMetrics().stringWidth(title);
            int titleSpacing = s60+s60;
            g.drawString(title, x0,y0);

            int tabW = (w-titleW-titleSpacing-(6*gap))/4;

            x0 += (titleW+titleSpacing);
            drawTab(g,x0,0,tabW,h,dipLabel, exploreBox, selectedTab.equals(exploreTab));
            textureArea = new Area(new RoundRectangle2D.Float(x0,s10,tabW,h-s10,h/4,h/4));

            x0 += (tabW+gap);
            drawTab(g,x0,0,tabW,h,intLabel, expandBox, selectedTab.equals(expandTab));
            Area tab2Area = new Area(new RoundRectangle2D.Float(x0,s10,tabW,h-s10,h/4,h/4));
            textureArea.add(tab2Area);

            x0 += (tabW+gap);
            drawTab(g,x0,0,tabW,h,milLabel, exploitBox, selectedTab.equals(exploitTab));
            Area tab3Area = new Area(new RoundRectangle2D.Float(x0,s10,tabW,h-s10,h/4,h/4));
            textureArea.add(tab3Area);

            x0 += (tabW+gap);
            drawTab(g,x0,0,tabW,h,statusLabel, exterminateBox, selectedTab.equals(exterminateTab));
            Area tab4Area = new Area(new RoundRectangle2D.Float(x0,s10,tabW,h-s10,h/4,h/4));
            textureArea.add(tab4Area);
            
            g.setColor(selectedTabC);
            g.fillRect(s10, h-s5, w-s20, s5);
        }
        private void drawTab(Graphics2D g, int x, int y, int w, int h, String label, Rectangle box, boolean selected) {
            g.setFont(narrowFont(22));
            if (selected)
                g.setColor(selectedTabC);
            else
                g.setColor(unselectedTabC);

            box.setBounds(x, y+s10, w, h-s10);
            g.fillRoundRect(x, y+s10, w, h-s10, h/4, h/4);
            g.fillRect(x, h-s5, w, s5);

            if (box == hoverBox) {
                Stroke prev = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(Color.yellow);
                g.setClip(x, y, w, h*2/3);
                g.drawRoundRect(x, y+s10, w, h-s10, h/4, h/4);
                g.setClip(x, y+h/2, w, h/2);
                g.drawRect(x, y+s10, w, h);
                g.setClip(null);
                g.setStroke(prev);
            }
            int sw = g.getFontMetrics().stringWidth(label);
            int x0 = x+((w-sw)/2);
            int y0 = y+h-s10;

            Color c0 = (box == hoverBox) ? Color.yellow : SystemPanel.whiteLabelText;
            drawShadowedString(g, label, 3, x0, y0, SystemPanel.textShadowC, c0);
        }
        public void selectNextTab() {
            switch(selectedTab) {
                case exploreTab:     selectTab(expandTab); break;
                case expandTab:      selectTab(exploitTab); break;
                case exploitTab:     selectTab(exterminateTab); break;
                case exterminateTab: selectTab(exploreTab); break;
            }
        }
        public void selectPreviousTab() {
            switch(selectedTab) {
                case exploreTab :    selectTab(exterminateTab); break;
                case expandTab:      selectTab(exploreTab); break;
                case exploitTab:     selectTab(expandTab); break;
                case exterminateTab: selectTab(exploitTab); break;
            }
        }
        private void selectTab(String s) {
            if (!selectedTab.equals(s)) {
                softClick();
                selectedTab = s;
                instance.repaint();
            }
        }
        @Override
        public void mouseClicked(MouseEvent e) {}
        @Override
        public void mouseEntered(MouseEvent e) {}
        @Override
        public void mouseExited(MouseEvent e) {
            if (hoverBox != null) {
                hoverBox = null;
                repaint();
            }
        }
        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                return;
            int x = e.getX();
            int y = e.getY();
            if (hoverBox == null)
                misClick();
            else {
                if (hoverBox == exploreBox)
                    selectTab(exploreTab);
                else if (hoverBox == expandBox)
                    selectTab(expandTab);
                else if (hoverBox == exploitBox)
                    selectTab(exploitTab);
                else if (hoverBox == exterminateBox)
                    selectTab(exterminateTab);
            }
        }
        @Override
        public void mouseDragged(MouseEvent e) {}
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Rectangle prevHover = hoverBox;
            if (exploreBox.contains(x,y))
                hoverBox = exploreBox;
            else if (expandBox.contains(x,y))
                hoverBox = expandBox;
            else if (exploitBox.contains(x,y))
                hoverBox = exploitBox;
           else if (exterminateBox.contains(x,y))
                hoverBox = exterminateBox;

            if (hoverBox != prevHover)
                repaint();
        }
    }
    class ExitFleetsButton extends ExitButton {
        private static final long serialVersionUID = 1L;
        public ExitFleetsButton(int w, int h, int vMargin, int hMargin) {
            super(w, h, vMargin, hMargin);
        }
        @Override
        protected void clickAction(int numClicks) {
            // force recalcuate map bounds when returning
            buttonClick();
            RotPUI.instance().selectMainPanel(true);
        }
    }
}
