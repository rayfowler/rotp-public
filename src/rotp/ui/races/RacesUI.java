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

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.Border;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.swing.BorderFactory;
import rotp.Rotp;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.ui.*;
import rotp.ui.main.SystemPanel;
import rotp.util.AnimationManager;

public class RacesUI extends BasePanel {
    private static final long serialVersionUID = 1L;
    private static RacesUI instance;
    
    private static final String DIPLOMACY_PANEL = "Diplomacy";
    private static final String INTELLIGENCE_PANEL = "Intelligence";
    private static final String MILITARY_PANEL = "Military";
    private static final String STATUS_PANEL = "Status";
    private static String selectedPanel = DIPLOMACY_PANEL;

    public static final Color scrollBarC = new Color(211,166,125);
    public static final Color lightBrown = new Color(178,124,87);
    public static final Color brown = new Color(141,101,76);
    public static final Color darkBrown = new Color(112,85,68);
    public static final Color darkerBrown = new Color(75,55,39);
    public static final Color gradientBottom = new Color(110,79,56);
    private static final Color raceEdgeColor = new Color(114,75,49);
    private static final Color raceCenterColor = new Color(179,117,77);
    static Color[] relationsC = new Color[40];
    public static BufferedImage raceBackImg;
    public static BufferedImage raceIconBackImg;

    private final List<Empire> empires = new ArrayList<>();

    BasePanel cardPanel;
    RacesDiplomacyUI diploPanel;
    RacesIntelligenceUI intelPanel;
    RacesMilitaryUI militaryPanel;
    RacesStatusUI statusPanel;
    RacePlayerRelationsPane raceListingPanel;
    private final CardLayout cardLayout = new CardLayout();
    private int pad = 10;
    private LinearGradientPaint backGradient;

    public RacesUI() {
        instance = this;
        pad = s10;
        initModel();
        initRelationsColors();
    }
    @Override
    public boolean drawMemory()            { return true; }
    @Override
    public void animate() {
        if (!AnimationManager.current().playAnimations())
            return;
        if (frame().getGlassPane().isVisible())
            return;
        if (animationCount() % 3 != 0)
            return;
        diploPanel.animate();
    }
    public void init() {
        diploPanel.init();
        intelPanel.init();
        statusPanel.init();
        raceListingPanel.init();
        empires.clear();
        empires.add(player());
        List<EmpireView> contacts = player().contacts();
        Collections.sort(contacts, EmpireView.PLAYER_LIST_ORDER);
        for (EmpireView v: contacts) 
            empires.add(v.empire());
        
        // auto-select the last selected empire
        // this will automatically weed out empires that have become extinct
        // since the last time this UI was displayed
        selectedEmpire(selectedEmpire());
    }
    private void initRelationsColors() {
        // do red.. 0-19, bright to dark
        for (int i=0;i<20;i++) {
            int r = 255-(i*12);
            int gb = 38-(i*2);
            relationsC[i] = newColor(r,gb,gb);
        }
        // do green.. 20-39, dark to bright
        for (int i=0;i<20;i++) {
            int g = 17+(i*12);
            int rb = i*2;
            relationsC[20+i] = newColor(rb,g,rb);
        }
    }
    public Empire selectedEmpire() {
        Empire e = (Empire) sessionVar("RACESUI_EMPIRE");
        return (e == null) || e.extinct() ? player() : e;
    }
    public void selectedEmpire(Empire e)  {
        if (e != selectedEmpire()) {
            sessionVar("RACESUI_EMPIRE", e);
            diploPanel.changedEmpire();
            intelPanel.changedEmpire();
            militaryPanel.changedEmpire();
            statusPanel.changedEmpire();
        }
    }
    public EmpireView selectedView() {
        Empire selectedEmpire = selectedEmpire();
        if (selectedEmpire == player())
            return null;
        return player().viewForEmpire(selectedEmpire);
    }
    public BufferedImage raceIconBackImg() {
        if (raceIconBackImg == null) {
            int w = scaled(200);
            raceIconBackImg = newStarBackground(this,w,w);
        }
        return raceIconBackImg;
    }
    private void initModel() {
        int w = scaled(Rotp.IMG_W);
        int h = scaled(Rotp.IMG_H);
        int rightPaneW = scaled(250);

        setBackground(Color.black);
        Border emptyBorder = newEmptyBorder(0, pad, pad, pad);
        setBorder(emptyBorder);

        diploPanel = new RacesDiplomacyUI(this);
        intelPanel = new RacesIntelligenceUI(this);
        militaryPanel = new RacesMilitaryUI(this);
        statusPanel = new RacesStatusUI(this);

        // create center panel
        MainTitlePanel titlePanel = new MainTitlePanel("RACES_TITLE");
        
        cardPanel = new BasePanel();
        cardPanel.setBorder(BorderFactory.createMatteBorder(scaled(5), 0,0,0, lightBrown));
        cardPanel.setLayout(cardLayout);
        cardPanel.add(diploPanel, DIPLOMACY_PANEL);
        cardPanel.add(intelPanel, INTELLIGENCE_PANEL);
        cardPanel.add(militaryPanel, MILITARY_PANEL);
        cardPanel.add(statusPanel, STATUS_PANEL);
        cardLayout.show(cardPanel, DIPLOMACY_PANEL);       
        
        BasePanel mainPanel = new BasePanel();
        mainPanel.setOpaque(false);
        mainPanel.setBorder(newEmptyBorder(20,10,10,0));
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(cardPanel, BorderLayout.CENTER);

        // create design slot panel on right side of UI
        raceListingPanel = new RacePlayerRelationsPane();
        EmpireTitlePanel slotsTitlePanel = new EmpireTitlePanel("RACES_SELECTOR_TITLE");
        BasePanel rightPanel = new BasePanel();
        rightPanel.setPreferredSize(new Dimension(rightPaneW, getHeight()));
        rightPanel.setBorder(newEmptyBorder(20,0,0,0));
        rightPanel.setOpaque(false);
        rightPanel.setLayout(new BorderLayout(0, s5));
        rightPanel.add(slotsTitlePanel, BorderLayout.NORTH);
        rightPanel.add(raceListingPanel, BorderLayout.CENTER);
        rightPanel.add(new ExitDesignButton(getWidth(), s60, s10, s2), BorderLayout.SOUTH);

        BorderLayout layout0 = new BorderLayout();
        layout0.setHgap(pad);
        setLayout(layout0);
        add(mainPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }
    @Override
    public boolean hasStarBackground()     { return true; }
    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        // draw the gradient background for the header row
        if (backGradient == null) {
            Color c0 = new Color(71,53,39,0);
            Color c1 = new Color(71,53,39);
            Point2D start = new Point2D.Float(s10, getHeight()-scaled(200));
            Point2D end = new Point2D.Float(s10, getHeight()-s20);
            float[] dist = {0.0f, 1.0f};
            Color[] colors = {c0, c1 };
            backGradient = new LinearGradientPaint(start, end, dist, colors);
        }
        g.setPaint(backGradient);
        g.fillRect(s10,getHeight()-scaled(200),getWidth()-s20, scaled(190));
    }
    public void drawRelationsBar(Graphics2D g, Empire emp, int x, int y, int w, int h, int arrowW, int arrowH) {
        int cats = relationsC.length;
        int h0 = h*3/4;
        int w0 = w/cats;
        int x0 = x+(w-(w0*cats))/2;
        int x0b = x0;

        for (int i=0;i<cats;i++) {
            g.setColor(relationsC[i]);
            g.fillRect(x0b, y,  w0,  h0);
            x0b += w0;
        }
        
        g.setColor(Color.lightGray);
        Stroke prev = g.getStroke();
        g.setStroke(stroke1);
        g.drawRect(x0,y,w0*cats,h0);
        g.setStroke(prev);

        EmpireView view = emp.viewForEmpire(player());
        float relations = view.embassy().relations();
 
        // draw pointer under bar showing exact relation
        float pct = (relations+100)/200;
        int x2 = x0+(int)(pct*w0*cats);
        int y2 = y+h0+s1;

        int ptX[] = new int[3];
        int ptY[] = new int[3];
        ptX[0]=x2; ptX[1] = x2-(arrowW/2); ptX[2]=x2+(arrowW/2);
        ptY[0]=y2; ptY[1] = y2+arrowH; ptY[2]=y2+arrowH;
        g.setColor(Color.white);
        g.fillPolygon(ptX, ptY, 3);
    }
    @Override
    public void keyPressed(KeyEvent e) {
        if (frame().getGlassPane().isVisible()) {
            BasePanel selectionPane = (BasePanel) frame().getGlassPane();
            selectionPane.keyPressed(e);
            return;
        }
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_ESCAPE) {
            exit(false);
            return;
        }
    }
    private void exit(boolean pauseNextTurn) {
        buttonClick();
        RotPUI.instance().selectMainPanel(pauseNextTurn);
    }
    class MainTitlePanel extends BasePanel implements MouseMotionListener, MouseListener {
        private static final long serialVersionUID = 1L;
        String titleKey;
        public MainTitlePanel(String s) {
            titleKey = s;
            initModel();
        }
        Rectangle hoverBox;
        Rectangle diplomacyBox = new Rectangle();
        Rectangle intelligenceBox = new Rectangle();
        Rectangle militaryBox = new Rectangle();
        Rectangle statusBox = new Rectangle();
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
            String dipLabel = text("RACES_TAB_DIPLOMACY");
            String intLabel = text("RACES_TAB_INTELLIGENCE");
            String milLabel = text("RACES_TAB_MILITARY");
            String statusLabel = text("RACES_TAB_STATUS");

            g.setColor(SystemPanel.orangeText);
            g.setFont(narrowFont(32));
            int titleW = g.getFontMetrics().stringWidth(title);
            int titleSpacing = s60+s60;
            g.drawString(title, x0,y0);

            int tabW = (w-titleW-titleSpacing-(5*gap))/4;

            x0 += (titleW+titleSpacing);
            drawTab(g,x0,0,tabW,h,dipLabel, diplomacyBox, selectedPanel.equals(DIPLOMACY_PANEL));
            textureArea = new Area(new RoundRectangle2D.Float(x0,s10,tabW,h-s10,h/4,h/4));

            x0 += (tabW+gap);
            drawTab(g,x0,0,tabW,h,intLabel, intelligenceBox, selectedPanel.equals(INTELLIGENCE_PANEL));
            Area tab2Area = new Area(new RoundRectangle2D.Float(x0,s10,tabW,h-s10,h/4,h/4));
            textureArea.add(tab2Area);

            x0 += (tabW+gap);
            drawTab(g,x0,0,tabW,h,milLabel, militaryBox, selectedPanel.equals(MILITARY_PANEL));
            Area tab3Area = new Area(new RoundRectangle2D.Float(x0,s10,tabW,h-s10,h/4,h/4));
            textureArea.add(tab3Area);

            x0 += (tabW+gap);
            drawTab(g,x0,0,tabW,h,statusLabel, statusBox, selectedPanel.equals(STATUS_PANEL));
            Area tab4Area = new Area(new RoundRectangle2D.Float(x0,s10,tabW,h-s10,h/4,h/4));
            textureArea.add(tab4Area);
        }
        private void drawTab(Graphics2D g, int x, int y, int w, int h, String label, Rectangle box, boolean selected) {
            g.setFont(narrowFont(22));
            if (selected)
                g.setColor(lightBrown);
            else
                g.setColor(darkBrown);

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
            switch(selectedPanel) {
                case DIPLOMACY_PANEL: selectTab(INTELLIGENCE_PANEL); break;
                case INTELLIGENCE_PANEL: selectTab(MILITARY_PANEL); break;
                case MILITARY_PANEL: selectTab(STATUS_PANEL); break;
                case STATUS_PANEL: selectTab(DIPLOMACY_PANEL); break;
            }
        }
        public void selectPreviousTab() {
            switch(selectedPanel) {
                case DIPLOMACY_PANEL: selectTab(STATUS_PANEL); break;
                case INTELLIGENCE_PANEL: selectTab(DIPLOMACY_PANEL); break;
                case MILITARY_PANEL: selectTab(INTELLIGENCE_PANEL); break;
                case STATUS_PANEL: selectTab(MILITARY_PANEL); break;
            }
        }
        private void selectTab(String panel) {
            if (!panel.equals(selectedPanel)) {
                softClick();
                selectedPanel = panel;
                cardLayout.show(cardPanel, panel);
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
                if (hoverBox == diplomacyBox)
                    selectTab(DIPLOMACY_PANEL);
                else if (hoverBox == intelligenceBox)
                    selectTab(INTELLIGENCE_PANEL);
                else if (hoverBox == militaryBox)
                    selectTab(MILITARY_PANEL);
                else if (hoverBox == statusBox)
                    selectTab(STATUS_PANEL);
            }
        }
        @Override
        public void mouseDragged(MouseEvent e) {}
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Rectangle prevHover = hoverBox;
            if (diplomacyBox.contains(x,y))
                hoverBox = diplomacyBox;
            else if (intelligenceBox.contains(x,y))
                hoverBox = intelligenceBox;
            else if (militaryBox.contains(x,y))
                hoverBox = militaryBox;
           else if (statusBox.contains(x,y))
                hoverBox = statusBox;

            if (hoverBox != prevHover)
                repaint();
        }
    }
    final class EmpireTitlePanel extends BasePanel {
        private static final long serialVersionUID = 1L;
        String titleKey;
        public EmpireTitlePanel(String s) {
            titleKey = s;
            setPreferredSize(new Dimension(getWidth(), s45));
            setOpaque(false);
        }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            String title = text(titleKey);
            g.setFont(narrowFont(32));
            g.setColor(SystemPanel.orangeText);
            g.drawString(title, s10, s35);
        }
    }
    final class RacePlayerRelationsPane extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        private final HashMap<Empire, Rectangle> contactBoxes = new HashMap<>();
        private Empire hoverEmp;
        int dragY;
        int contactsY, contactsYMax;
        Rectangle contactsListBox = new Rectangle();
        Rectangle contactsScroller = new Rectangle();
        Shape hoverShape;

        public RacePlayerRelationsPane() {
            setBackground(lightBrown);
            initModel();
        }
        private void initModel() {
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }
        public void init() { 
            contactsY = 0;
        }
        @Override
        public String textureName()            { return TEXTURE_BROWN; }
        @Override
        public void drawTexture(Graphics g0) {  }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;

            int w = getWidth();
            int h = getHeight();

            // data area
            g.setColor(brown);
            int y1 = 0;
            int x1 = 0;
            int w1 = w;
            int h1 = h-y1-s10;
            g.fillRect(0,0,w,h);
            if (UserPreferences.textures()) 
                drawTexture(g,0,0,w,h);
            for (Rectangle r: contactBoxes.values())
                r.setBounds(0,0,0,0);

            int contactH = s100;
            int listH = h-y1-s10;
            int fullListH = empires.size()*contactH;
            int rightM = listH >= fullListH ? 0 : s20;
            int w2 = w1-rightM;
            int bottomY = y1+listH;
            int y2 = y1 - contactsY;
            
            contactsListBox.setBounds(x1,y1,w1,listH);
            for (Empire e: empires) {
                Rectangle contactClip = new Rectangle(x1, y2, w2, contactH+s3).intersection(contactsListBox);
                g.setClip(contactClip);
                drawContact(g, e, x1, y2, w2, contactH);
                if (e == selectedEmpire()) {
                    Stroke prev = g.getStroke();
                    g.setStroke(stroke4);
                    g.setColor(SystemPanel.yellowText);
                    g.drawRect(x1+s4,y2+s4,w2-s8,contactH-s4);
                    g.setStroke(prev);
                }
                y2 += contactH;
                if (y2 >= bottomY)
                    break;
            }
            contactsYMax = max(0, fullListH-listH);
            if (rightM == 0)
                contactsScroller.setBounds(0,0,0,0);
            else {
                int scrollW = s12;
                int scrollH = (int) ((float)listH*listH/(listH+contactsYMax));
                int scrollX = x1+w1-scrollW-s6;
                int scrollY =(int) (y1+s5+(float)listH*contactsY/(contactsYMax+listH));
                g.setColor(darkBrown);
                g.fillRect(scrollX-s2, y1+s5, scrollW+s4, h1);
                g.setClip(scrollX,y1+s5,scrollW,h1-s5);
                g.setColor(RacesUI.scrollBarC);
                g.fillRect(scrollX, scrollY, scrollW, scrollH);
                contactsScroller.setBounds(scrollX, scrollY, scrollW, scrollH);
                if (hoverShape == contactsScroller) {
                    Stroke prev = g.getStroke();
                    g.setColor(Color.yellow);
                    g.setStroke(stroke2);
                    g.drawRect(scrollX, scrollY, scrollW, scrollH);
                    g.setStroke(prev);
                }
            }
            g.setClip(null);
        }
        private void drawContact(Graphics2D g, Empire emp, int x, int y, int w, int h) {
            int x0 = x+s5;
            int y0 = y+s5;
            int w0 = w-s10;
            int h0 = h-s5;
            boxFor(emp).setBounds(x0,y0,w0,h0);
            g.setColor(darkBrown);
            g.fillRect(x0, y0, w0, h0);
            BufferedImage back = raceBackImg();
            int w1 = back.getWidth();
            int h1 = back.getHeight();
            int mgn = (h-h1)/2;
            
            boolean selected = emp == selectedEmpire();

            Color blackC = selected ? Color.black : SystemPanel.blackText;
            Color whiteC = emp == hoverEmp ? Color.yellow : selected ? Color.white : SystemPanel.whiteText;
             //empire name
            int x1 = x0+w1+mgn+s10;
            int y1 = y0+s25;
            g.setFont(narrowFont(22));
            drawShadowedString(g, emp.raceName(), 1, x1, y1, blackC, whiteC);

            if (emp.isPlayer()) {
                List<EmpireView> views = player().contacts();
                int n = views.size();
                g.setFont(narrowFont(16));
                g.setColor(blackC);
                y1 += s20;
                g.drawString(text("RACES_KNOWN_EMPIRES", n), x1, y1);
                if (n > 0) {
                    int r = 0;
                    for (EmpireView v : views) {
                        if (!v.diplomats())
                            r++;
                    }
                    y1 += s16;
                    g.drawString(text("RACES_RECALLED_DIPLOMATS", r), x1, y1);
                }
            }
            else {
                EmpireView view = player().viewForEmpire(emp);
                boolean inRange = view.inEconomicRange();
                g.setColor(blackC);
                g.setFont(narrowFont(16));

                if (inRange) {
                    // treaty status
                    y1 += s20;
                    g.drawString(view.embassy().treaty().status(), x1, y1);
                    // trade
                    y1 += s16;
                    int level = view.trade().level();
                    String tradeStr = (level == 0) ? text("RACES_TRADE_NONE") : text("RACES_TRADE_LEVEL", level);
                    g.drawString(tradeStr, x1, y1);
                }
                else {
                    y1 += s20;
                    g.drawString(text("RACES_OUT_OF_RANGE"), x1, y1);
                }
                if (!emp.race().masksDiplomacy) 
                    drawRelationsBar(g, emp, x1, y0+h0-s25, w0-x1-s10, s10, s10, s5);
            }
            
            if (UserPreferences.textures()) 
                drawTextureWithExistingClip(g, x0,y0,w0,h0);
            drawRaceImage(g, emp, back, x0, y0, h0);
        }
        public void drawRaceImage(Graphics2D g, Empire emp, BufferedImage back, int x, int y, int h) {
            BufferedImage img = emp.race().diploMugshotQuiet();
            int w1 = back.getWidth();
            int h1 = back.getHeight();
            int mgn = (h-h1)/2;
            g.drawImage(back, x+mgn, y+mgn, null);

            //Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER , 0.3f);
            //if (comp != null)
            //    g.setComposite(comp);
            g.drawImage(img, x+mgn, y+mgn, w1, h1, null);
            
            if (emp.isPlayer())
                return;
            
            String text = "";
            EmpireView view = player().viewForEmpire(emp);
            boolean dipGone = view.embassy().diplomatGone();
            boolean otherDipGone = view.otherView().embassy().diplomatGone();
            if (dipGone && otherDipGone)
                text = text("RACES_DIPLOMATS_RECALLED");
            else if (dipGone)
                text = text("RACES_DIPLOMAT_RECALLED", player().raceName());
            else if (otherDipGone) 
                text = text("RACES_DIPLOMAT_RECALLED", emp.raceName());
                        
            if (!text.isEmpty()) {
                g.setFont(narrowFont(14));
                Composite prevC = g.getComposite();
                Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER , 0.6f);
                g.setComposite(comp);
                g.setColor(Color.black);
                g.fillRect(x+mgn, y+mgn, w1, h1);
                g.setComposite(prevC);
                g.setColor(Color.white);
                List<String> lines = wrappedLines(g, text, w1-s10);
                int y2 = y+mgn+s50-(lines.size()*s8);
                for (String line: lines) {
                    int sw = g.getFontMetrics().stringWidth(line);
                    int x2 = x+mgn+((w1-sw)/2);
                    g.drawString(line, x2, y2);
                    y2 += s16;
                }
            }
        }
        public BufferedImage raceBackImg() {
            if (raceBackImg == null)
                initRaceBackImg();
            return raceBackImg;
        }
        private void initRaceBackImg() {
            int w = s76;
            int h = s82;
            raceBackImg = gc().createCompatibleImage(w, h);

            Point2D center = new Point2D.Float(w/2, h/2);
            float radius = s78;
            float[] dist = {0.0f, 0.1f, 0.5f, 1.0f};
            Color[] colors = {raceCenterColor, raceCenterColor, raceEdgeColor, raceEdgeColor};
            RadialGradientPaint p = new RadialGradientPaint(center, radius, dist, colors);
            Graphics2D g = (Graphics2D) raceBackImg.getGraphics();
            g.setPaint(p);
            g.fillRect(0, 0, w, h);
            g.dispose();
        }
        private Rectangle boxFor(Empire emp) {
            if (!contactBoxes.containsKey(emp))
                contactBoxes.put(emp, new Rectangle());
            return contactBoxes.get(emp);
        }
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            int count = e.getUnitsToScroll();           
            boolean hoveringEmp = false;
            for (Rectangle rect: contactBoxes.values()) {
                if (hoverShape == rect) {
                    hoveringEmp = true;
                    break;
                }
            }
            if (hoveringEmp || (hoverShape == contactsScroller)) {
                int prevY = contactsY;
                if (count < 0)
                    contactsY = max(0,contactsY-s10);
                else 
                    contactsY = min(contactsYMax,contactsY+s10);
                if (contactsY != prevY)
                    repaint(contactsListBox);
                return;
            }
        }
        @Override
        public void mouseDragged(MouseEvent e) { 
            int x = e.getX();
            int y = e.getY();
            int dY = y-dragY;
            dragY = y;
            if (contactsScroller == hoverShape) {
                if ((y >= contactsListBox.y) || (y <= (contactsListBox.y+contactsListBox.height))) { 
                    int h = (int) contactsListBox.getHeight();
                    int dListY = (int)((float)dY*(h+contactsYMax)/h);
                    if (dY < 0)
                        contactsY = max(0,contactsY+dListY);
                    else 
                        contactsY = min(contactsYMax,contactsY+dListY);
                }
                repaint(contactsListBox);
                return;
            }
            else if (hoverEmp != null) {
                if (contactsListBox.contains(x,y)) { 
                    int h = (int) contactsListBox.getHeight();
                    int dListY = (int)(-(float)dY*(h+contactsYMax)/h);
                    if (dListY < 0)
                        contactsY = max(0,contactsY+dListY);
                    else 
                        contactsY = min(contactsYMax,contactsY+dListY);
                }
                repaint(contactsListBox);
                return;
            }
        }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Shape prevHover = hoverShape;
            hoverShape = null;
            hoverEmp = null;
            for (Empire emp: contactBoxes.keySet()) {
                Rectangle rec = contactBoxes.get(emp);
                if (rec.contains(x,y)) {
                    hoverShape = rec;
                    hoverEmp = emp;
                    break;
                }
            }
            if (contactsScroller.contains(x,y)) 
                hoverShape = contactsScroller;
 
            if (hoverShape != prevHover) 
                repaint();     
        }
        @Override
        public void mouseClicked(MouseEvent mouseEvent) { }
        @Override
        public void mousePressed(MouseEvent e) { 
            dragY = e.getY();
        }
        @Override
        public void mouseExited(MouseEvent mouseEvent) {
            if (hoverShape != null) {
                hoverShape = null;
                repaint();
            }
        }
        @Override
        public void mouseEntered(MouseEvent e) {   }
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                return;
            dragY = 0;
            if (hoverShape == null)
                return;

            if ((hoverEmp != null) && (hoverEmp != selectedEmpire())) {
                softClick();
                selectedEmpire(hoverEmp);
                instance.repaint();
            }
        }
    }
    class ExitDesignButton extends ExitButton {
        private static final long serialVersionUID = 1L;
        public ExitDesignButton(int w, int h, int vMargin, int hMargin) {
            super(w, h, vMargin, hMargin);
        }
        @Override
        protected void clickAction(int numClicks) {
            // force recalcuate map bounds when returning
            exit(true);
        }
    }
}