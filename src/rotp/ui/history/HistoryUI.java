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
import java.util.Collections;
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
import rotp.ui.sprites.ZoomInWidgetSprite;
import rotp.ui.sprites.ZoomOutWidgetSprite;

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
    static final Color sliderBoxC = new Color(34,140,142);
    static final Color sliderBackC = Color.black;
    static final Color sliderBorderC = Color.lightGray;

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
    int numEmps = 1;
    byte[] sysData;
    int[] sysCount;
    float[] xMin;
    float[] xMax;
    float[] xSum;
    float[] ySum;
    List<Empire> sortedEmpires = new ArrayList<>();

    @Override
    public boolean drawMemory()            { return true; }
    public void init(int empId, boolean all) {
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
        numEmps = galaxy().numEmpires();
        maxTurn = galaxy().numberTurns();
        empire = galaxy().empire(empId);
        showAll = all;
        paused = true;
        
        sortedEmpires.clear();
        sortedEmpires.addAll(Arrays.asList(galaxy().empires()));
        
        initOwnershipData();
        sortEmpireList();
    }
    public void sortEmpireList() {
        for (Empire emp: sortedEmpires) 
            emp.numColoniesHistory = sysCount(emp.id, turn);
        
        Collections.sort(sortedEmpires, Empire.HISTORICAL_SIZE);
    }
    public int empIndex(int emp, int t) {
        return (t*numEmps)+emp;
    }
    public byte sysData(int sys, int t) {
        return sysData[(t*numSystems)+sys];
    }
    public int sysCount(int emp, int t) {
        return sysCount[(t*numEmps)+emp];
    }
    public void setData(StarSystem sys, int t, int empId) {
        sysData[(t*numSystems)+sys.id] = (byte)empId;
        
        int i = (t*numEmps)+empId;
        float sysX = sys.x();
        float sysY = sys.y();
        sysCount[i]++;
        xMin[i] = min(xMin[i],sysX);
        xMax[i] = max(xMax[i],sysX);
        xSum[i] += sysX;
        ySum[i] += sysY;
    }
    private void initOwnershipData() {
        int i = numSystems*(maxTurn+1);
        sysData = new byte[i];
        sysCount = new int[i];
        xMin =  new float[i];
        xMax =  new float[i];
        xSum =  new float[i];
        ySum =  new float[i];
        Arrays.fill(sysData, (byte)Empire.NULL_ID);
        Arrays.fill(sysCount, 0);
        Arrays.fill(xMin, Float.MAX_VALUE);
        Arrays.fill(xMax, 0);
        Arrays.fill(xSum, 0);
        Arrays.fill(ySum, 0);
        
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
                        setData(sys,t,prevOwner);
                }
                prevOwner = (byte) event.owner();
                prevTurn = eventTurn;
            }
        }
        if (prevOwner != nullOwner) {
            for (int t=prevTurn;t<=maxTurn;t++)
                setData(sys,t,prevOwner);
        }
    }
    public HistoryUI() {
        instance = this;
        setBackground(Color.black);
        setOpaque(true);
        controls.add(new ZoomOutWidgetSprite(10,60,30,30));
        controls.add(new ZoomInWidgetSprite(10,25,30,30));
        initModel();
    }
    public void setTurn(int n) {
        if ((n < 0) || (n > maxTurn))
            return;
        turn = n;
        sortEmpireList();
        map.clearRangeMap();
        repaint();
    }
    public void nextTurn() {
        softClick();
        setTurn(turn+1);
    }
    public void previousTurn() {
        softClick();
        setTurn(turn-1);
    }
    public void playPause() {
        softClick();
        paused = !paused;
        repaint();
    }
    public void reset() {
        softClick();
        setTurn(0);      
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
    public boolean canReset() {
        return turn >= maxTurn;
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
            setTurn(turn+1);
        else
            paused = true;
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        switch(k) {
            case KeyEvent.VK_EQUALS:
                if (e.isShiftDown())  
                    map.adjustZoom(-1);
                break;
            case KeyEvent.VK_MINUS:
                map.adjustZoom(1);
                break;
            case KeyEvent.VK_UP:
                map.dragMap(0, s40);
                break;
            case KeyEvent.VK_DOWN:
                map.dragMap(0, -s40);
                break;
            case KeyEvent.VK_LEFT:
                map.dragMap(s40, 0);
                break;
            case KeyEvent.VK_RIGHT:
                map.dragMap(-s40, 0);
                break;
            case KeyEvent.VK_F:
                nextTurn();
                break;
            case KeyEvent.VK_B:
                previousTurn();
                break;
            case KeyEvent.VK_P:
                if (!canReset())
                    playPause();
                break;
            case KeyEvent.VK_R:
                if (canReset())
                    reset();
                break;
            case KeyEvent.VK_E:
            case KeyEvent.VK_ESCAPE:
                exit();
                break;
        }
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
            else 
                title = text("HISTORY_TITLE", empire.name());
                
            g.setFont(narrowFont(40));
            int sw = g.getFontMetrics().stringWidth(title);
            drawString(g,title, (w-sw)/2, s50);
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
        private final Rectangle sliderBox = new Rectangle();
        private Shape hoverTarget;
        Shape textureClip;
        int sliderX, sliderW;
        
        public HistoryButtonsPanel() {
            init();
        }
        private void init() {
            setBackground(MainUI.paneBackground());
            setOpaque(false);
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

            int numButtons = 4;
            int buttonW = s90;
            int buttonH = s30; // -s25 is because 4 buttons at -s5 spacing/button
            
            int totalButtonSpacing = numButtons*(buttonW+s10);
            int buttonX = (w-totalButtonSpacing)/2;
            int buttonY = h-buttonH-s20;
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
            String label = text("HISTORY_FORWARD");
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
          
            // draw previous button
            buttonX = buttonX+buttonW+s10;
            g.setFont(narrowFont(18));
            prevTurnBox.setBounds(buttonX, buttonY, buttonW, buttonH);
            label = text("HISTORY_BACK");
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
          
            // draw play/pause button
            buttonX = buttonX+buttonW+s10;
            g.setFont(narrowFont(18));
            playBox.setBounds(buttonX, buttonY, buttonW, buttonH);
            if (canReset())
                label = text("HISTORY_RESET");
            else if (paused)
                label = text("HISTORY_PLAY");
            else
                label = text("HISTORY_PAUSE");
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
          
            // draw exit button
            buttonX = buttonX+buttonW+s10;
            g.setFont(narrowFont(18));
            exitBox.setBounds(buttonX, buttonY, buttonW, buttonH);
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
            
            // draw slider bar
            int sliderH = s10;
            sliderW = scaled(500);
            sliderX = (w-sliderW)/2;
            int sliderY = (h-sliderH-s5);
            sliderBox.setBounds(sliderX, sliderY, sliderW, sliderH);
            int sliderW0 = min(sliderW, sliderW*turn/maxTurn);
            if (sliderW > sliderW0) {
                g.setColor(sliderBackC);
                g.fill(sliderBox);
            }
            if (sliderW0 > 0) {
                g.setColor(greenMidC);
                g.fillRect(sliderX, sliderY, sliderW0, sliderH);
            }       
            if (hoverTarget == sliderBox) {
                prevStr = g.getStroke();
                g.setColor(Color.yellow);
                g.setStroke(stroke2);
                g.draw(sliderBox);
                g.setStroke(prevStr);
            }
            else {
                g.setColor(sliderBorderC);
                g.draw(sliderBox);
            }
            
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
                if (canReset())
                    reset();
                else
                    playPause();
                return;
            }
            else if ((hoverTarget == exitBox)) {
                softClick(); 
                exit();
                return;
            }
            else if (hoverTarget == sliderBox) {
                int newTurn = maxTurn * (e.getX()-sliderX)/sliderW;
                setTurn(newTurn);
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
            else if (sliderBox.contains(x,y))
                hoverTarget = sliderBox;

            if (prevHover != hoverTarget) 
               repaint();
        }
    }
    class GalaxyMapPane extends BasePanel implements IMapHandler {
        private static final long serialVersionUID = 1L;
        private LinearGradientPaint backGradient;
        public GalaxyMapPane() {
            init0();
        }
        private void init0() {
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

            int bpW = scaled(600);
            int bpH = scaled(60);
            buttonsPanel = new HistoryButtonsPanel();
            buttonsPanel.setBounds((w-bpW)/2,h-bpH-s15,bpW,bpH);

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
            else 
                title = text("HISTORY_TITLE");
            int sw = g.getFontMetrics().stringWidth(title);
            g.setColor(SystemPanel.whiteText);
            drawString(g,title, (w-sw)/2, s24);
        }
        @Override
        public void drawYear(Graphics2D g) { 
            int w = getWidth();
            int h = getHeight();
            g.setFont(narrowFont(30));
            String title = text("HISTORY_TURN_DESC",str(turn), str(maxTurn));
            int sw = g.getFontMetrics().stringWidth(title);
            g.setColor(SystemPanel.whiteText);
            drawString(g,title, w-sw-s35, h-s25);
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
        public boolean drawStar(StarSystem s)          { return sysData(s.id, turn) == Empire.NULL_ID; }
        @Override
        public boolean showSystemName(StarSystem s)    { return true; }
        @Override
        public boolean drawFlag(StarSystem s)          { return false; }
        @Override
        public boolean drawShield(StarSystem s)        { return false; }
        @Override
        public boolean drawStargate(StarSystem s)      { return false; }
        @Override
        public boolean drawShips()                     { return false; }
        @Override
        public boolean drawBackgroundStars()           { return false; }
        @Override
        public boolean shouldDrawEmpireName(Empire e, float scale)  { 
            if (scale == 0)
                return false;
            int i = empIndex(e.id, turn);
            return (sysCount[i] > 0);
        }
        @Override
        public void drawEmpireName(Empire e, GalaxyMapPanel ui, Graphics2D g)  { 
            int i = empIndex(e.id, turn);
            float xAvg = sysCount[i] == 0 ? 0 : xSum[i]/sysCount[i];
            float yAvg = sysCount[i] == 0 ? 0 : ySum[i]/sysCount[i];
            e.draw(ui,g,xMin[i],xMax[i],xAvg,yAvg); 
        }
        @Override
        public Color systemLabelColor(StarSystem s)    { 
            int i = sysData(s.id, turn);
            if (i == Empire.NULL_ID)
                return Color.gray;
            else
                return galaxy().empire(i).color();
        }
        @Override
        public boolean showAlerts()                    { return false; }
        @Override
        public boolean showShipRanges()                { return false; }
        @Override
        public Empire knownEmpire(int sysId, Empire emp)    { 
            int id = sysData(sysId, turn);
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
                map.init();
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
        public void hoveringOverSprite(Sprite o) {
            if (o == lastHoveringSprite())
                return;

            if (lastHoveringSprite() != null)
                lastHoveringSprite().mouseExit(map);
            lastHoveringSprite(o);

            if (hoveringSprite() != null)
                hoveringSprite().mouseExit(map);
            hoveringSprite(o);
            if (hoveringSprite() != null)
                hoveringSprite().mouseEnter(map);
            repaint();
        }
        @Override
        public void clickingOnSprite(Sprite o, int cnt, boolean rightClick, boolean click) {
            if (controls.contains(o)) {
                o.click(map, cnt, rightClick, click);
                map.repaint();
            }
        }
        @Override
        public boolean isClicked(Sprite s)             { return false; }
        @Override
        public boolean isHovering(Sprite s)            { return false; }
        @Override
        public Sprite hoveringSprite()           { return (Sprite) sessionVar("HISTORYUI_HOVERING_SPRITE"); }
        public void hoveringSprite(Sprite s)     { 
            sessionVar("HISTORYUI_HOVERING_SPRITE", s); 
        }
        public Sprite lastHoveringSprite()       { return (Sprite) sessionVar("HISTORYUI_LAST_HOVERING_SPRITE"); }
        public void lastHoveringSprite(Sprite s) { sessionVar("HISTORYUI_LAST_HOVERING_SPRITE", s); }
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
        @Override
        public void paintOverMap(GalaxyMapPanel ui, Graphics2D g) { 
            // empire list
            int lineH = s22;
            int y1 = s40;
            int x1 = getWidth()-scaled(120);
            g.setFont(narrowFont(24));
            g.setColor(Color.white);
            String sysTitle = text("HISTORY_SYSTEMS");
            String empTitle = text("HISTORY_EMPIRE");
            String ext = text("HISTORY_EXTINCT");
            int sw0 = g.getFontMetrics().stringWidth(sysTitle);
            
            g.setColor(Color.white);
            drawString(g,empTitle, x1, y1);
            drawString(g,sysTitle, x1-sw0-s10, y1);
            
            g.setFont(narrowFont(20));
            for (Empire emp: sortedEmpires) {
                int num = emp.numColoniesHistory;
                if (showAll || (num > 0)) {
                    y1 += lineH;
                    String amt = num == 0 ? ext : str(num);
                    sw0 = g.getFontMetrics().stringWidth(amt);
                    if (num == 0)
                        g.setColor(SystemPanel.redText);
                    else
                        g.setColor(SystemPanel.whiteText);
                    drawString(g,amt, x1-sw0-s10, y1);
                    drawString(g,emp.raceName(), x1, y1);
                }
            }
        }
    }
}
