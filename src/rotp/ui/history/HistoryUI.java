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
package rotp.ui.history;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JLayeredPane;
import javax.swing.border.Border;
import rotp.Rotp;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.empires.SystemInfo;
import rotp.model.events.StarSystemEvent;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.Nebula;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.UserPreferences;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.map.IMapHandler;

public final class HistoryUI extends BasePanel implements MouseListener {
    private static final long serialVersionUID = 1L;

    static HistoryUI instance;
    static Color uiBackground = new Color(132,98,77);
    static Color dataBackground = new Color(94,71,53);
    static Color titleColor = new Color(114,155,201);
    private static final Color shadeBorderC = new Color(80,80,80);
	static final Color dataBorders = new Color(160,160,160);

    static final Color paneBorderDarker = new Color(61,41,28);
    static final Color paneBorderDark = new Color(76,57,41);
    static final Color paneBorderLighter = new Color(169,127,99);
    static final Color paneBorderLight = new Color(151,112,90);
    static final Color borderLight0 = new Color(169,127,99);
    static final Color borderLight1 = new Color(151,112,90);
    static final Color borderShade0 = new Color(85,64,47);
    static final Color borderShade1 = new Color(62,60,108);

    LinearGradientPaint backGradient ;
    private GalaxyMapPane mapPane;
    private GalaxyMapPanel map;
    HistoryButtonsPanel buttonsPanel;
    JLayeredPane layers = new JLayeredPane();
    private final List<Sprite> controls = new ArrayList<>();
    int animationIndex = 0;

    Empire empire;
    boolean showAll = false;
    boolean exited = false;
    boolean paused = true;
    int turn = 0;
    int maxTurn = 0;
    int numSystems = 1;
    byte[] data;
    

    @Override
    public boolean drawMemory()            { return true; }
    public void init(int empId, boolean all)       {
        exited = false;
        backGradient = null;
        // reset map everytime we open
        removeSessionVar("SABOTAGEUI_MAP_INITIALIZED");
        RotPUI.instance().mainUI().saveMapState();
        int homeSysId = galaxy().empire(empId).homeSysId();
        StarSystem homeSys = galaxy().system(homeSysId);
        mapPane.checkMapInitialized();
        mapPane.map().setScale(20);
        mapPane.selectTargetSystem(galaxy().system(homeSysId));
        map.centerX(homeSys.x());
        map.centerY(homeSys.y());
        animationIndex = 0;
        turn = 0;
        numSystems = galaxy().numStarSystems();
        maxTurn = galaxy().numberTurns()-1;
        empire = galaxy().empire(empId);
        showAll = all;
        paused = true;
        
        initOwnershipData();
    }
    public byte data(int sys, int turn) {
        return data[(turn*numSystems)+sys];
    }
    public void setData(int sys, int turn, int empId) {
        data[(turn*numSystems)+sys] = (byte)empId;
    }
    private void initOwnershipData() {
        data = new byte[numSystems*(maxTurn+1)];
        Arrays.fill(data, (byte)Empire.NULL_ID);
        
        if (showAll) {
            for (int sysId=0;sysId<numSystems;sysId++) 
                loadOwnershipData(sysId);
        }
        else {
            SystemInfo sv = player().sv;
            for (int sysId=0;sysId<numSystems;sysId++) {
                if (!sv.name(sysId).isEmpty())
                    loadOwnershipData(sysId);
            }
        }        
    }
    private void loadOwnershipData(int sysId) {
        StarSystem sys = galaxy().system(sysId);
        List<StarSystemEvent> events = sys.events();
        int prevTurn = 0;
        byte nullOwner = (byte) Empire.NULL_ID;
        byte prevOwner = nullOwner;
        // load up owners for system based on its events
        for (StarSystemEvent event: events) {
            if (event.changesOwnership()) {
                int eventTurn=event.turn();
                if (prevOwner != nullOwner) {
                    for (int t=prevTurn;t<eventTurn;t++) 
                        setData(sysId,t,prevOwner);
                }
                prevOwner = (byte) event.owner();
                prevTurn = eventTurn;
            }
        }
        if (prevOwner != nullOwner) {
            for (int t=prevTurn;t<=maxTurn;t++)
                setData(sysId,t,prevOwner);
        }
    }
    public StarSystem systemToDisplay() {
        if (mapPane.clickedSprite() instanceof StarSystem)
            return (StarSystem) mapPane.clickedSprite();
        else
            return galaxy().system(player().capitalSysId());
    }
    public HistoryUI() {
        instance = this;
        setBackground(Color.black);
        setOpaque(true);
        initModel();
    }
    public void nextTurn() {
        if (turn >= maxTurn)
            return;
        turn++;
        map.clearRangeMap();
        repaint();
    }
    public void previousTurn() {
        if (turn <= 0)
            return;
        turn--;
        map.clearRangeMap();
        repaint();
    }
    public void playPause() {
        softClick();
        paused = !paused;
        repaint();
    }
    public void exit() {
        softClick();
        RotPUI.instance().mainUI().restoreMapState();
        disableGlassPane();
    }
    private boolean canNextTurn() {
        return turn < maxTurn;
    }
    private boolean canPreviousTurn() {
        return turn > 0;
    }
    private void initModel() {
        mapPane = new GalaxyMapPane();
 
        setLayout(new BorderLayout());
        add(mapPane, BorderLayout.CENTER);
        addMouseListener(this);
    }
    @Override
    public void animate() {
        if (paused)
            return;
        
        if (canNextTurn())
            nextTurn();
        else
            paused = true;
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_1)
            nextTurn();
        else if (k == KeyEvent.VK_2)
            previousTurn();
        else if (k == KeyEvent.VK_3)
            playPause();
        else if (k == KeyEvent.VK_4)
            exit();
        else if (k == KeyEvent.VK_ESCAPE) 
            exit();
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) {
        if ((e.getButton() > 3) || e.getClickCount() > 1)
            return;
    }
    final class TitlePanel extends BasePanel {
        private static final long serialVersionUID = 1L;
        public TitlePanel () {
            setPreferredSize(new Dimension(getWidth(), s60));
            setBackground(Color.black);
        }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            int w = getWidth();            
            g.setColor(SystemPanel.orangeText);
            String title;
            if (showAll)
                title = text("HISTORY_TITLE_ALL");
            else if (empire.isPlayer())
                title = text("HISTORY_TITLE_PLAYER", empire.name());
            else
                title = text("HISTORY_TITLE_AI", empire.name());
                
            g.setFont(narrowFont(35));
            int sw = g.getFontMetrics().stringWidth(title);
            g.drawString(title, (w-sw)/2, s40);
        }
    }
    final class HistoryButtonsPanel extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        private final Color grayEdgeC = new Color(59,59,59);
        private final Color grayMidC = new Color(93,93,93);
        private final Color greenEdgeC = new Color(44,59,30);
        private final Color greenMidC = new Color(70,93,48);
        private final Color redEdgeC = new Color(72,14,14);
        private final Color redMidC = new Color(126,28,28);
        private LinearGradientPaint greenBackground;
        private LinearGradientPaint redBackground;
        private LinearGradientPaint grayBackground;
        private final Rectangle prevTurnBox = new Rectangle();
        private final Rectangle nextTurnBox = new Rectangle();
        private final Rectangle playBox = new Rectangle();
        private final Rectangle exitBox = new Rectangle();
        private Shape hoverTarget;
        Shape textureClip;
        
        public HistoryButtonsPanel() {
            init();
            setPreferredSize(new Dimension(scaled(400),scaled(50)));
        }
        private void init() {
            setBackground(MainUI.paneBackground());
            setOpaque(true);
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        @Override
        public String textureName()            { return TEXTURE_GRAY; }
        @Override
        public Shape textureClip()             { return textureClip; }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);

            int w = getWidth();
            int h = getHeight();
            
            textureClip = new Rectangle2D.Float(0, 0, w, h);

            int buttonW = s90;
            int buttonH = s30; // -s25 is because 4 buttons at -s5 spacing/button
            int buttonX = s5;
            int buttonY = h-buttonH-s10;
            if (greenBackground == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D ptStart = new Point2D.Float(buttonX, 0);
                Point2D ptEnd = new Point2D.Float(buttonX + buttonW, 0);
                Color[] greenColors = {greenEdgeC, greenMidC, greenEdgeC};
                greenBackground = new LinearGradientPaint(ptStart, ptEnd, dist, greenColors);                
                Color[] redColors = {redEdgeC, redMidC, redEdgeC};
                redBackground = new LinearGradientPaint(ptStart, ptEnd, dist, redColors);                
                Color[] grayColors = {grayEdgeC, grayMidC, grayEdgeC};
                grayBackground = new LinearGradientPaint(ptStart, ptEnd, dist, grayColors);                
            }
            
            
            // draw next turn
            g.setFont(narrowFont(18));
            nextTurnBox.setBounds(buttonX, buttonY, buttonW, buttonH);
            String key = "1";
            String label = text("HISTORY_NEXT_TURN");
            int sw = g.getFontMetrics().stringWidth(label);
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(buttonX+s3, buttonY+s3, buttonW, buttonH, s8, s8);           
            boolean hovering = hoverTarget == nextTurnBox;
            boolean enabled = canNextTurn();
            if (enabled)
                g.setPaint(greenBackground);
            else
                g.setPaint(grayBackground);
            g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, s8, s8);
            Stroke prevStr = g.getStroke();
            Color c0;
            if (hovering && enabled) {
                c0 = SystemPanel.yellowText;
                g.setStroke(stroke2);
            }
            else {
                c0 = SystemPanel.whiteText;
                g.setStroke(BasePanel.stroke1);              
            }
            g.setColor(c0);
            g.drawRoundRect(buttonX, buttonY, buttonW, buttonH, s8, s8);
            g.setStroke(prevStr);
            int x2a = buttonX + ((buttonW - sw) / 2);
            drawShadowedString(g, label, x2a, buttonY + buttonH - s8, Color.black, c0);
            g.setFont(narrowFont(15));
            drawShadowedString(g, key, buttonX+s10, buttonY + buttonH - s8, Color.black, c0);
          
            // draw previous button
            buttonX = buttonX+buttonW+s10;
            g.setFont(narrowFont(18));
            prevTurnBox.setBounds(buttonX, buttonY, buttonW, buttonH);
            key = "2";
            label = text("HISTORY_PREV_TURN");
            sw = g.getFontMetrics().stringWidth(label);
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(buttonX+s3, buttonY+s3, buttonW, buttonH, s8, s8);           
            hovering = hoverTarget == prevTurnBox;
            enabled = canPreviousTurn();
            if (enabled)
                g.setPaint(greenBackground);
            else
                g.setPaint(grayBackground);
            g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, s8, s8);
            prevStr = g.getStroke();
            if (hovering && enabled) {
                c0 = SystemPanel.yellowText;
                g.setStroke(stroke2);
            }
            else {
                c0 = SystemPanel.whiteText;
                g.setStroke(BasePanel.stroke1);              
            }
            g.setColor(c0);
            g.drawRoundRect(buttonX, buttonY, buttonW, buttonH, s8, s8);
            g.setStroke(prevStr);
            x2a = buttonX + ((buttonW - sw) / 2);
            drawShadowedString(g, label, x2a, buttonY + buttonH - s8, Color.black, c0);
            g.setFont(narrowFont(15));
            drawShadowedString(g, key, buttonX+s10, buttonY + buttonH - s8, Color.black, c0);
          
            // draw play/pause button
            buttonX = buttonX+buttonW+s10;
            g.setFont(narrowFont(18));
            playBox.setBounds(buttonX, buttonY, buttonW, buttonH);
            key = "3";
            label = paused ? text("HISTORY_PLAY") : text("HISTORY_PAUSE");
            sw = g.getFontMetrics().stringWidth(label);
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(buttonX+s3, buttonY+s3, buttonW, buttonH, s8, s8);           
            hovering = hoverTarget == playBox;
            enabled = true;
            if (enabled)
                g.setPaint(greenBackground);
            else
                g.setPaint(grayBackground);
            g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, s8, s8);
            prevStr = g.getStroke();
            if (hovering && enabled) {
                c0 = SystemPanel.yellowText;
                g.setStroke(stroke2);
            }
            else {
                c0 = SystemPanel.whiteText;
                g.setStroke(BasePanel.stroke1);              
            }
            g.setColor(c0);
            g.drawRoundRect(buttonX, buttonY, buttonW, buttonH, s8, s8);
            g.setStroke(prevStr);
            x2a = buttonX + ((buttonW - sw) / 2);
            drawShadowedString(g, label, x2a, buttonY + buttonH - s8, Color.black, c0);
            g.setFont(narrowFont(15));
            drawShadowedString(g, key, buttonX+s10, buttonY + buttonH - s8, Color.black, c0);
          
            // draw exit button
            buttonX = buttonX+buttonW+s10;
            g.setFont(narrowFont(18));
            exitBox.setBounds(buttonX, buttonY, buttonW, buttonH);
            key = "4";
            label = text("HISTORY_EXIT");
            sw = g.getFontMetrics().stringWidth(label);
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(buttonX+s3, buttonY+s3, buttonW, buttonH, s8, s8);           
            hovering = hoverTarget == exitBox;
            enabled = true;
            if (enabled)
                g.setPaint(greenBackground);
            else
                g.setPaint(grayBackground);
            g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, s8, s8);
            prevStr = g.getStroke();
            if (hovering && enabled) {
                c0 = SystemPanel.yellowText;
                g.setStroke(stroke2);
            }
            else {
                c0 = SystemPanel.whiteText;
                g.setStroke(BasePanel.stroke1);              
            }
            g.setColor(c0);
            g.drawRoundRect(buttonX, buttonY, buttonW, buttonH, s8, s8);
            g.setStroke(prevStr);
            x2a = buttonX + ((buttonW - sw) / 2);
            drawShadowedString(g, label, x2a, buttonY + buttonH - s8, Color.black, c0);
            g.setFont(narrowFont(15));
            drawShadowedString(g, key, buttonX+s10, buttonY + buttonH - s8, Color.black, c0);
        }
        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mousePressed(MouseEvent e) { }
        @Override
        public void mouseReleased(MouseEvent e) {
           if ((hoverTarget == nextTurnBox) && canNextTurn()) {
                softClick(); 
                nextTurn();
                return;
            }
            else if ((hoverTarget == prevTurnBox) && canPreviousTurn()) {
                softClick(); 
                previousTurn();
                return;
            }
            else if ((hoverTarget == playBox)) {
                softClick(); 
                playPause();
                return;
            }
            else if ((hoverTarget == exitBox)) {
                softClick(); 
                exit();
                return;
            }
        }
        @Override
        public void mouseEntered(MouseEvent e) { }
        @Override
        public void mouseExited(MouseEvent e) {
            if (hoverTarget != null) {
                hoverTarget = null;
                repaint();
            }
        }
        @Override
        public void mouseDragged(MouseEvent e) {  }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            Shape prevHover = hoverTarget;
            hoverTarget = null;

            if (nextTurnBox.contains(x,y))
                hoverTarget = nextTurnBox;
            else if (prevTurnBox.contains(x,y))
                hoverTarget = prevTurnBox;
            else if (playBox.contains(x,y))
                hoverTarget = playBox;
            else if (exitBox.contains(x,y))
                hoverTarget = exitBox;

            if (prevHover != hoverTarget) 
               repaint();
        }
    }
    class GalaxyMapPane extends BasePanel implements IMapHandler {
        private static final long serialVersionUID = 1L;
        private LinearGradientPaint backGradient;
        public GalaxyMapPane() {
            init();
        }
        private void init() {
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
            
            setBorder(newEmptyBorder(0,15,15,10));
            setLayout(new BorderLayout(s10,s10));
            map = new GalaxyMapPanel(this);
            map.setBounds(0,0,w,h);

            int bpW = scaled(400);
            int bpH = scaled(50);
            buttonsPanel = new HistoryButtonsPanel();
            buttonsPanel.setBounds((w-bpW)/2,h-bpH-s25,bpW,bpH);

            setLayout(new BorderLayout());
            add(layers, BorderLayout.CENTER);

            layers.add(buttonsPanel, JLayeredPane.PALETTE_LAYER);
            layers.add(map, JLayeredPane.DEFAULT_LAYER);
            setOpaque(false);
        }
        @Override
        public GalaxyMapPanel map()         { return map; }
        @Override
        public void drawTitle(Graphics2D g) { 
            int w = getWidth();
            g.setFont(narrowFont(30));
            String title;
            if (showAll)
                title = text("HISTORY_TITLE_ALL");
            else if (empire.isPlayer())
                title = text("HISTORY_TITLE_PLAYER", empire.name());
            else
                title = text("HISTORY_TITLE_AI", empire.name());
            int sw = g.getFontMetrics().stringWidth(title);
            g.setColor(SystemPanel.whiteText);
            g.drawString(title, (w-sw)/2, s24);
        }
        @Override
        public void drawYear(Graphics2D g) { 
            int w = getWidth();
            int h = getHeight();
            g.setFont(narrowFont(30));
            String title = text("HISTORY_TURN_DESC",str(turn), str(maxTurn));
            int sw = g.getFontMetrics().stringWidth(title);
            g.setColor(SystemPanel.whiteText);
            g.drawString(title, w-sw-s35, h-s25);
        }
        @Override
        public boolean suspendAnimationsDuringNextTurn()    { return false; }
        @Override
        public Color shadeC()                          { return Color.darkGray; }
        @Override
        public Color backC()                           { return Color.gray; }
        @Override
        public Color lightC()                          { return Color.lightGray; }
        @Override
        public float systemClickRadius()               { return 1.0f; }
        @Override
        public boolean canChangeMapScales()            { return true; }
        @Override
        public boolean showSystemName(StarSystem s)    { return true; }
        @Override
        public boolean drawShield(StarSystem s)        { return false; }
        @Override
        public boolean drawStargate(StarSystem s)      { return false; }
        @Override
        public boolean showAlerts()                    { return false; }
        @Override
        public boolean showShipRanges()                { return false; }
        @Override
        public Empire knownEmpire(int sysId, Empire emp)    { 
            int id = data(sysId, turn);
            return id == Empire.NULL_ID ? null : galaxy().empire(id);
        }
        @Override
        public List<Sprite> controlSprites()      { return controls; }
        @Override
        public float ownerReach(StarSystem sys) {
            if (sys.isColonized())
                return sys.empire().tech().topEngineWarpTech().warp();
            else
                return 0;
        }
        @Override
        public boolean showOwnerReach(StarSystem spr) {
            return false;
        }
        @Override
        public boolean shouldDrawSprite(Sprite s) {
            return (s instanceof StarSystem)
                || (s instanceof Nebula)
                || controls.contains(s);
        }
        @Override
        public void checkMapInitialized() {
            Boolean inited = (Boolean) sessionVar("HISTORYUI_MAP_INITIALIZED");
            if (inited == null) {
                map.initializeMapData();
                // init appropriate scale and bounds
                Empire emp = player();
                map.centerX(avg(emp.minX(), emp.maxX()));
                map.centerY(avg(emp.minY(), emp.maxY()));
                map.setBounds(emp.minX()-3, emp.maxX()+6, emp.minY()-6, emp.maxY());
                sessionVar("HISTORYUI_MAP_INITIALIZED", true);
            }
        }
        @Override
        public Location mapFocus() {
            Location loc = (Location) sessionVar("HISTORYUI_MAP_FOCUS");
            if (loc == null) {
                loc = new Location();
                sessionVar("HISTORYUI_MAP_FOCUS", loc);
            }
            return loc;
        }
        @Override
        public void hoveringOverSprite(Sprite o) { }
        @Override
        public void clickingOnSprite(Sprite o, int cnt, boolean rightClick, boolean click) {
            if (controls.contains(o)) {
                o.click(map, cnt, rightClick, click);
                map.repaint();
            }
        }
        @Override
        public Sprite hoveringSprite() { return null; }
        @Override
        public Sprite clickedSprite()      { return (Sprite) sessionVar("HISTORYUI_CLICKED_SPRITE"); }
        @Override
        public void clickedSprite(Sprite s) { sessionVar("HISTORYUI_CLICKED_SPRITE", s); }
        @Override
        public void reselectCurrentSystem() {}
        @Override
        public Border mapBorder() { return shadedBorder(); }
        @Override
        public float startingScalePct() { return galaxy().maxScaleAdj(); }
        private void selectTargetSystem(StarSystem sys) {
            clickingOnSprite(sys, 1, false, false);
            repaint();
        }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            if (backGradient == null) {
                Point2D start = new Point2D.Float(0, h / 2);
                Point2D end = new Point2D.Float(0, h);
                float[] dist = {0.0f, 1.0f};
                Color[] colors = {Color.black, MainUI.paneBackground};
                backGradient = new LinearGradientPaint(start, end, dist, colors);
            }
            g.setPaint(backGradient);
            g.fillRect(0,h/2,w, h/2);
        }
    }
}
