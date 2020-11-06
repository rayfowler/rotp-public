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
package rotp.ui.races;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.border.Border;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.empires.Leader;
import rotp.model.empires.Race;
import rotp.model.empires.SabotageMission;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.Nebula;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.main.SystemGraphicPane;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.main.SystemViewInfoPane;
import rotp.ui.main.UnexploredGraphicInfoPane;
import rotp.ui.map.IMapHandler;
import rotp.util.sound.SoundClip;

public final class SabotageUI extends BasePanel implements MouseListener {
    private static final long serialVersionUID = 1L;
    private static final String MAP_PANEL = "Map";
    private static final String RESULT_PANEL = "Result";

    static SabotageUI instance;
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

    static final int REQUEST_MISSION = 1;
    static final int SHOW_ANIMATION = 2;
    static final int SHOW_RESULTS = 3;

    private BasePanel cardPane;
    private final CardLayout cardLayout = new CardLayout();
    private SabotageMission mission;
    private GalaxyMapPane mapPane;
    private GalaxyMapPanel map;
    SpyDetailPane spyDetailPanel;
    SpySystemPanel spySystemPanel;
    SabotageButtonsPanel spyButtonsPanel;
    BasePanel titlePanel;
    BasePanel promptPanel;
    SabotageResultPanel resultPanel;
    private final List<Sprite> controls = new ArrayList<>();
    int currentState;
    int destroyCount = 0;
    boolean inRebellion = false;
    int explosionFrame = 0;
    boolean inciteAudioPlayed = false;
    boolean exited = false;
    SoundClip audioClip = null;

    @Override
    public boolean drawMemory()            { return true; }

    public void init(SabotageMission sm, int sysId)       {
        mission = sm;
        exited = false;
        currentState = REQUEST_MISSION;
        destroyCount = 0;
        explosionFrame = 0;
        inciteAudioPlayed = false;
        // reset map everytime we open
        removeSessionVar("SABOTAGEUI_MAP_INITIALIZED");
        mapPane.checkMapInitialized();
        mapPane.selectTargetSystem(galaxy().system(sysId));
        audioClip = null;
        selectMapPanel();
    }
    public StarSystem systemToDisplay() {
        if (mapPane.clickedSprite() instanceof StarSystem)
            return (StarSystem) mapPane.clickedSprite();
        else
            return galaxy().system(player().capitalSysId());
    }
    public SabotageUI() {
        instance = this;
        setBackground(Color.black);
        setOpaque(true);
        initModel();
    }
    private void advanceToNextState() {
        switch(currentState) {
            case REQUEST_MISSION:
                stopAmbience();
                resultPanel.init();
                currentState = SHOW_ANIMATION;
                if (!playAnimations()) {
                    advanceToNextState();
                    return;
                }
                break;
            case SHOW_ANIMATION:
                currentState = SHOW_RESULTS;
                playAmbience();
                break;
            case SHOW_RESULTS:
                if (audioClip != null)
                    audioClip.endPlaying();
                exited = true;
                repaint();
                session().resumeNextTurnProcessing();
                return;
        }
        if ((currentState == REQUEST_MISSION))
            selectMapPanel();
        else
            selectResultPanel();

        repaint();
    }
    private void initModel() {
        mapPane = new GalaxyMapPane();
        resultPanel = new SabotageResultPanel();
        titlePanel = new TitlePanel();
 
        promptPanel = new BasePanel();
        promptPanel.setLayout(new BorderLayout());
        promptPanel.add(titlePanel, BorderLayout.NORTH);
        promptPanel.add(mapPane, BorderLayout.CENTER);

        cardPane = new BasePanel();
        cardPane.setOpaque(false);
        cardPane.setLayout(cardLayout);
        cardPane.add(resultPanel, RESULT_PANEL);
        cardPane.add(promptPanel, MAP_PANEL);

        setLayout(new BorderLayout());
        add(cardPane, BorderLayout.CENTER);
        addMouseListener(this);
    }
    public void selectMapPanel()     { cardLayout.show(cardPane, MAP_PANEL); }
    public void selectResultPanel()  { cardLayout.show(cardPane, RESULT_PANEL); }
    @Override
    public void animate() {
        if ((currentState == REQUEST_MISSION))
            map.animate();
        else if (currentState == SHOW_ANIMATION)
            resultPanel.animate();
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_ESCAPE) {
            if (currentState == REQUEST_MISSION)
                misClick();
            else {
                softClick();
                advanceToNextState();
            }
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
        if (e.getButton() > 3)
            return;
        if (currentState != REQUEST_MISSION) {
            softClick();
            advanceToNextState();
        }
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
            String title = text("SABOTAGE_TITLE", mission.target().raceName());
            g.setFont(narrowFont(35));
            int sw = g.getFontMetrics().stringWidth(title);
            g.drawString(title, (w-sw)/2, s40);
        }
    }
    class SpySystemPanel extends SystemPanel {
        private static final long serialVersionUID = 1L;
        UnexploredGraphicInfoPane unexploredPane;
        SystemGraphicPane exploredPane;
        BasePanel cardPanel;
        final String UNEXPLORED = "Unexplored";
        final String EXPLORED = "Explored";
        public SpySystemPanel() {
            init();
        }
        private void init() {
            setPreferredSize(new  Dimension(getWidth(), scaled(120)));
            setOpaque(true);

            unexploredPane = new UnexploredGraphicInfoPane(this);
            exploredPane = new SystemGraphicPane(this, null);

            cardPanel = new BasePanel();
            cardPanel.setLayout(detailLayout);
            cardPanel.add(unexploredPane, UNEXPLORED);
            cardPanel.add(exploredPane, EXPLORED);

            setLayout(new BorderLayout());
            add(cardPanel, BorderLayout.CENTER);
            showUnexplored();
        }
        @Override
        public void paint(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            if (player().sv.isScouted(systemViewToDisplay().id))
                showExplored();
            else
                showUnexplored();
            super.paint(g);
            int w = getWidth();
            int h = getHeight();
        } 
        @Override
        public void animate() {
            exploredPane.animate();
        }
        @Override
        public StarSystem systemViewToDisplay() {
            return instance.systemToDisplay();
        }
	@Override
	protected BasePanel topPane()    { return new SystemViewInfoPane(this); }
	@Override
	protected BasePanel bottomPane()    { return null; }
	@Override
	protected BasePanel detailPane() { return null; }
        protected void showExplored()     { detailLayout.show(cardPanel, EXPLORED); }
        protected void showUnexplored()   { detailLayout.show(cardPanel, UNEXPLORED); }
    }

    final class SpyDetailPane extends BasePanel {
        private static final long serialVersionUID = 1L;
        SystemPanel parent;
        Shape textureClip;

        SpyDetailPane(SystemPanel p) {
            parent = p;
            setOpaque(false);
        }
        @Override
        public String textureName()            { return TEXTURE_GRAY; }
        @Override
        public Shape textureClip()      { return textureClip; }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;

            int id = sys.id;
            Empire pl = player();
            Empire sysEmp = pl.sv.empire(id);
            if (sysEmp == null)
                return;

            boolean spied = pl.sv.isSpied(id);

            super.paintComponent(g);
            int h = getHeight();
            int w = getWidth();

            int topH1 = s40;
            int topH = s90;
            // draw colony info box
            g.setColor(MainUI.paneBackground());
            g.fillRect(0, 0, w, topH-s5);
            GradientPaint back = new GradientPaint(0,0,sysEmp.color(),w, 0,MainUI.transC);
            g.setPaint(back);
            g.fillRect(0, 0, w, topH1-s5);
            g.setPaint(null);
            g.setColor(MainUI.shadeBorderC());
            g.fillRect(0, topH-s5, w, s6);

            textureClip = new Rectangle2D.Float(0,0,w,topH-s5);

            //  colony name
            g.setFont(narrowFont(24));
            drawShadowedString(g, pl.sv.descriptiveName(id), 2, s10, topH1-s15, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);

            // colony data
            String unknown = text("RACES_UNKNOWN_DATA");
            String factLbl = text("MAIN_COLONY_FACTORIES");
            String baseLbl = text("MAIN_COLONY_BASES");
            String shieldLbl = text("MAIN_COLONY_SHIELD");
            String popLbl = text("MAIN_COLONY_POPULATION");

            int x0 = s5;
            int x1 = w/2;
            int y0 = topH-s37;
            int y1 = topH-s12;

            g.setFont(narrowFont(16));
            g.setColor(SystemPanel.blackText);
            g.drawString(popLbl, x0, y0);
            g.drawString(factLbl, x1, y0);
            g.drawString(shieldLbl, x0, y1);
            g.drawString(baseLbl, x1, y1);

            String s = spied ? str(pl.sv.population(id)) : unknown;
            int sw1 = g.getFontMetrics().stringWidth(s);
            g.drawString(s, x1-sw1-s10, y0);
            s = spied ? str(pl.sv.factories(id)) : unknown;
            int sw2 = g.getFontMetrics().stringWidth(s);
            g.drawString(s, w-s10-sw2, y0);
            s = spied ? str(pl.sv.shieldLevel(id)) : unknown;
            int sw3 = g.getFontMetrics().stringWidth(s);
            g.drawString(s, x1-s10-sw3, y1);
            s = spied ? str(pl.sv.bases(id)) : unknown;
            int sw4 = g.getFontMetrics().stringWidth(s);
            g.drawString(s, w-s10-sw4, y1);

            // draw borders around data
            g.setColor(dataBorders);
            Stroke prevStroke = g.getStroke();
            g.setStroke(stroke1);
            //g.drawLine(0, y0-s18, w, y0-s18);
            g.drawLine(0, y1-s18, w, y1-s18);
            g.drawLine(x1-s5, y0-s18, x1-s5, topH-s6);
            g.setStroke(prevStroke);

            String desc;
            if (pl.sv.isScouted(id)) {
                BufferedImage img = pl.sv.planetTerrain(id);
                g.drawImage(img, 0, topH, w, h, 0, 0, img.getWidth(), img.getHeight(), null);
                desc = pl.sv.planetType(id).description(pl);
            }
            else {    
                g.setColor(Color.black);
                g.fillRect(0,topH,w,h);
                drawStar(g, sys.starType(), s40, w/2, h/2);
                desc = text(sys.starType().description());
            }

            g.setFont(narrowFont(16));
            g.setColor(SystemPanel.grayText);
            List<String> descLines =  wrappedLines(g, text(desc), getWidth()-s12);

            int ydelta = s18;
            int y2=h-s8-(ydelta*(descLines.size()-1));
            for (String line: descLines) {
                drawBorderedString(g, line, s8, y2, Color.black, SystemPanel.whiteText);
                y2 += ydelta;
            }
        }
    }
    final class SabotageButtonsPanel extends BasePanel implements MouseListener, MouseMotionListener {
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
        private final Rectangle basesBox = new Rectangle();
        private final Rectangle factoriesBox = new Rectangle();
        private final Rectangle rebellionBox = new Rectangle();
        private final Rectangle noActionBox = new Rectangle();
        private Shape hoverTarget;
        
        public SabotageButtonsPanel() {
            init();
            setPreferredSize(new Dimension(getWidth(),scaled(140)));
        }
        private void init() {
            setBackground(MainUI.paneBackground());
            setOpaque(true);
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);

            int w = getWidth();
            int h = getHeight();
            
            int buttonW = w-s3;
            int buttonH = (h-s25)/4; // -s25 is because 4 buttons at -s5 spacing/button
            int buttonX = s1;
            int buttonY = s5;
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
            
            g.setFont(narrowFont(18));
            
            // draw factories button
            factoriesBox.setBounds(buttonX, buttonY, buttonW, buttonH);
            String label = text("SABOTAGE_BUTTON_FACTORIES");
            int sw = g.getFontMetrics().stringWidth(label);
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(buttonX+s3, buttonY+s3, buttonW, buttonH, s8, s8);           
            boolean hovering = hoverTarget == factoriesBox;
            boolean enabled = canSabotageFactories();
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
          
            // draw missile bases button
            buttonY += (buttonH+s5);
            basesBox.setBounds(buttonX, buttonY, buttonW, buttonH);
            label = text("SABOTAGE_BUTTON_BASES");
            sw = g.getFontMetrics().stringWidth(label);
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(buttonX+s3, buttonY+s3, buttonW, buttonH, s8, s8);           
            hovering = hoverTarget == basesBox;
            enabled = canSabotageBases();
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
          
            // draw incite rebellion button
            buttonY += (buttonH+s5);
            rebellionBox.setBounds(buttonX, buttonY, buttonW, buttonH);
            label = text("SABOTAGE_BUTTON_REBELLION");
            sw = g.getFontMetrics().stringWidth(label);
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(buttonX+s3, buttonY+s3, buttonW, buttonH, s8, s8);           
            hovering = hoverTarget == rebellionBox;
            enabled = canInciteRebellion();
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
          
            // draw no action button
            buttonY += (buttonH+s5);
            noActionBox.setBounds(buttonX, buttonY, buttonW, buttonH);
            label = text("SABOTAGE_BUTTON_NO_ACTION");
            sw = g.getFontMetrics().stringWidth(label);
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(buttonX+s3, buttonY+s3, buttonW, buttonH, s8, s8);           
            hovering = hoverTarget == noActionBox;
            g.setPaint(redBackground);
            g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, s8, s8);
            prevStr = g.getStroke();
            if (hovering) {
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
        }
        private boolean canSabotageFactories() {
            return player().sv.canSabotageFactories(systemToDisplay().id);
        }
        private boolean canSabotageBases() {
            return player().sv.canSabotageBases(systemToDisplay().id);
        }
        private boolean canInciteRebellion() {
            return player().sv.canInciteRebellion(systemToDisplay().id);
        }
        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mousePressed(MouseEvent e) { }
        @Override
        public void mouseReleased(MouseEvent e) {
           if ((hoverTarget == factoriesBox) && canSabotageFactories()) {
                softClick(); 
                mission.destroyFactories(systemToDisplay());
                advanceToNextState();
                return;
            }
            else if ((hoverTarget == basesBox) && canSabotageBases()) {
                softClick(); 
                mission.destroyMissileBases(systemToDisplay());
                advanceToNextState();
                return;
            }
            else if ((hoverTarget == rebellionBox) && canInciteRebellion()) {
                softClick(); 
                StarSystem sys = systemToDisplay();
                Leader prevLeader = sys.empire().leader();
                mission.inciteRebellion(sys);
                inRebellion = (sys.colony().inRebellion() || (sys.empire().leader() != prevLeader));
                advanceToNextState();
                return;
            }
            else if (hoverTarget == noActionBox) {
                mission.cancelMission();
                currentState = SHOW_RESULTS;
                advanceToNextState();
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

            if (factoriesBox.contains(x,y))
                hoverTarget = factoriesBox;
            else if (basesBox.contains(x,y))
                hoverTarget = basesBox;
            else if (rebellionBox.contains(x,y))
                hoverTarget = rebellionBox;
            else if (noActionBox.contains(x,y))
                hoverTarget = noActionBox;

            if (prevHover != hoverTarget)
                repaint();
        }
    }
    class SabotageResultPanel extends BasePanel {
        private static final long serialVersionUID = 1L;
        private Image panelBuffer;
        private List<Image> animationFrames;
        private int animationIndex = 0;
        public void init() {
            Race r = mission.target().race();
            if (mission.isDestroyBases()) 
                animationFrames = r.sabotageMissileFrames();
            else if (mission.isDestroyFactories()) 
                animationFrames = r.sabotageFactoryFrames();
            else if (mission.isInciteRebellion()) 
                animationFrames = r.sabotageRebellionFrames();
            
            // if no animation, just show a star background
            if (animationFrames.isEmpty()) 
                animationFrames.add(GalaxyMapPanel.sharedStarBackground);
            
            animationIndex = 0;
        }
        @Override
        public void paintComponent(Graphics g) {
            paintSabotageResult(panelBuffer());
            g.drawImage(panelBuffer(),0,0,null);
        }
        private void paintSabotageResult(Image buffer) {
            Graphics2D g = (Graphics2D) buffer.getGraphics();
            int w = getWidth();
            int h = getHeight();
            g.setColor(Color.black);
            g.fillRect(0,0,w,h);        
            int index = min(animationIndex, animationFrames.size()-1);
            Image img = animationFrames.get(index);
            g.drawImage(img, 0, 0, w, h, 0, 0, img.getWidth(null), img.getHeight(null), null);
          
            if ((currentState == SHOW_RESULTS)) {
                String msg;
                if (mission.isDestroyFactories())
                    msg = text("SABOTAGE_FACTORIES_RESULT", mission.factoriesDestroyed());
                else if (mission.isDestroyBases())
                    msg = text("SABOTAGE_BASES_RESULT", mission.missileBasesDestroyed());
                else {
                    if (inRebellion)
                        msg = text("SABOTAGE_REBELS_REVOLT");
                    else {
                        int pct = (int) (systemToDisplay().colony().rebellionPct()*100);
                        msg = text("SABOTAGE_REBELS_TOTAL", mission.rebelsIncited(), pct);
                    }
                }

                g.setFont(narrowFont(32));
                int sw = g.getFontMetrics().stringWidth(msg);
                int x0 = (w-sw)/2;
                int y0 = h-s50;
                drawBorderedString(g, msg, x0, y0, Color.black, Color.white);
            }
            drawSkipText(g, (currentState == SHOW_RESULTS));

            g.dispose();
        }
        private Image panelBuffer() {
            if ((panelBuffer == null)
            || (panelBuffer.getWidth(null) != getWidth())
            || (panelBuffer.getHeight(null) != getHeight())) {
                panelBuffer = createImage(getWidth(), getHeight());
            }
            return panelBuffer;
        }
        @Override
        public void animate() {
            if (!playAnimations())
                return;

            if (currentState == SHOW_ANIMATION) {
                if (animationIndex == 1) {
                    sleep(1000);  // pause on the opening scene before the explosion
                    if (mission.isInciteRebellion())
                        audioClip = playAudioClip("InciteRebellion");
                    else
                        audioClip = playAudioClip("ExplosionFactories");
                }
                if (animationIndex < animationFrames.size())
                    repaint();
                animationIndex++;
                if (animationIndex >= animationFrames.size()) {
                    player().sv.refreshSpyScan(systemToDisplay().id);
                    advanceToNextState();
                    return;
                }
            }
            else if (currentState == SHOW_RESULTS) {
                if (animationCount() % 3 == 0)
                    repaint();
            }
        }
    }
    class SpyParentPanel extends BasePanel {
        private static final long serialVersionUID = 1L;
        LinearGradientPaint backGradient;
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            if (backGradient == null) {
                Point2D start = new Point2D.Float(0, 0);
                Point2D end = new Point2D.Float(0, h);
                float[] dist = {0.0f, 1.0f};
                Color[] colors = {shadeBorderC, MainUI.paneBackground};
                backGradient = new LinearGradientPaint(start, end, dist, colors);
            }
            g.setPaint(backGradient);
            g.fillRect(0,0,w, h);     
        }
    }
    class GalaxyMapPane extends BasePanel implements IMapHandler {
        private static final long serialVersionUID = 1L;
        private LinearGradientPaint backGradient;
        public GalaxyMapPane() {
            init();
        }
        private void init() {
            setOpaque(true);
            setBackground(Color.black);
            spySystemPanel = new SpySystemPanel();
            spyDetailPanel = new SpyDetailPane(spySystemPanel);
            spyButtonsPanel = new SabotageButtonsPanel();
            BasePanel spyPanel = new BasePanel();
            spyPanel.setOpaque(false);
            spyPanel.setLayout(new BorderLayout());
            spyPanel.setPreferredSize(new Dimension(scaled(250), getHeight()));
            //spyPanel.setBorder(newLineBorder(shadeBorderC,5));
            spyPanel.setBorder(newEmptyBorder(5,5,5,5));
            spyPanel.add(spySystemPanel, BorderLayout.NORTH);
            spyPanel.add(spyDetailPanel, BorderLayout.CENTER);
            spyPanel.add(spyButtonsPanel, BorderLayout.SOUTH);
            
            BasePanel spyParentPanel = new SpyParentPanel();
            spyParentPanel.setLayout(new BorderLayout());
            spyParentPanel.add(spyPanel, BorderLayout.CENTER);

            setBorder(newEmptyBorder(0,15,15,10));
            setLayout(new BorderLayout(s10,s10));
            map = new GalaxyMapPanel(this);
            add(map, BorderLayout.CENTER);
            add(spyParentPanel, BorderLayout.EAST);
        }
        @Override
        public GalaxyMapPanel map()         { return map; }
        @Override
        public void drawTitle(Graphics2D g) { 
            int w = getWidth();
            g.setFont(narrowFont(24));
            String title = text("SABOTAGE_SELECT_TARGET");
            int sw = g.getFontMetrics().stringWidth(title);
            g.setColor(SystemPanel.whiteText);
            g.drawString(title, (w-sw)/2, s24);
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
        public float systemClickRadius()             { return 1.0f; }
        @Override
        public boolean canChangeMapScales()          { return true; }
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
        public Empire empireBoundaries()    { return mission.target(); }
        @Override
        public boolean showOwnerReach(StarSystem spr) {
            return false;
        }
        @Override
        public boolean showOwnership(StarSystem sys) {
            return player().sv.empire(sys.id) == mission.target();
        }
        @Override
        public boolean shouldDrawSprite(Sprite s) {
            return (s instanceof StarSystem)
                || (s instanceof Nebula)
                || controls.contains(s);
        }
        @Override
        public void checkMapInitialized() {
            Boolean inited = (Boolean) sessionVar("SABOTAGEUI_MAP_INITIALIZED");
            if (inited == null) {
                map.initializeMapData();
                // init appropriate scale and bounds
                Empire emp = mission.target();
                map.centerX(avg(emp.minX(), emp.maxX()));
                map.centerY(avg(emp.minY(), emp.maxY()));
                map.setBounds(emp.minX()-3, emp.maxX()+6, emp.minY()-6, emp.maxY());
                sessionVar("SABOTAGEUI_MAP_INITIALIZED", true);
            }
        }
        @Override
        public Location mapFocus() {
            Location loc = (Location) sessionVar("RACEUI_MAP_FOCUS");
            if (loc == null) {
                loc = new Location();
                sessionVar("RACEUI_MAP_FOCUS", loc);
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
            if (o instanceof StarSystem) {
                StarSystem sys = (StarSystem) o;
                if (sys.empire() == mission.target()) {
                    clickedSprite(o);
                    instance.repaint();
                }
            }
        }
        @Override
        public Sprite hoveringSprite() { return null; }
        @Override
        public Sprite clickedSprite()      { return (Sprite) sessionVar("SABOTAGEUI_CLICKED_SPRITE"); }
        @Override
        public void clickedSprite(Sprite s) { sessionVar("SABOTAGEUI_CLICKED_SPRITE", s); }
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
