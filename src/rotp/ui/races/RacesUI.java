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
import rotp.model.empires.DiplomaticTreaty;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.TreatyAlliance;
import rotp.ui.*;
import static rotp.ui.BasePanel.s10;
import static rotp.ui.BasePanel.s20;
import static rotp.ui.BasePanel.s3;
import static rotp.ui.BasePanel.s5;
import rotp.ui.game.HelpUI;
import rotp.ui.main.SystemPanel;
import rotp.util.AnimationManager;

public class RacesUI extends BasePanel {
    private static final long serialVersionUID = 1L;
    public static RacesUI instance;
    
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
    Rectangle diplomacyBox = new Rectangle();
    Rectangle intelligenceBox = new Rectangle();
    Rectangle militaryBox = new Rectangle();
    Rectangle statusBox = new Rectangle();

    private final List<Empire> empires = new ArrayList<>();

    BasePanel cardPanel;
    MainTitlePanel titlePanel;
    RacesDiplomacyUI diploPanel;
    RacesIntelligenceUI intelPanel;
    RacesMilitaryUI militaryPanel;
    RacesStatusUI statusPanel;
    RacePlayerRelationsPane raceListingPanel;
    RacesSetColorUI setColorPanel;
    private final CardLayout cardLayout = new CardLayout();
    private int pad = 10;
    private int helpFrame = 0;
    private LinearGradientPaint backGradient;
    private final HashMap<Empire, Shape> colorIcons = new HashMap<>();

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
    @Override
    public void cancelHelp() {
        helpFrame = 0;
        RotPUI.helpUI().close();
    }
    @Override
    public void showHelp() {
        helpFrame = 1;
        loadHelpUI();
        repaint();   
    }
    @Override 
    public void advanceHelp() {
        if (helpFrame == 0)
            return;
        helpFrame++;
        if (helpFrame > 2) 
            cancelHelp();
        loadHelpUI();
        repaint();
    }
    private void loadHelpUI() {
        HelpUI helpUI = RotPUI.helpUI();
        helpUI.clear();

        if (helpFrame == 0)
            return;

        switch(helpFrame) {
            case 1: loadHelpUI0(); break;
            case 2:
                Empire emp = selectedEmpire();
                switch(selectedPanel) {
                    case DIPLOMACY_PANEL:
                        if (emp.isPlayer())
                            loadHelpUI1();
                        else
                            loadHelpUI2(); 
                        break;
                    case INTELLIGENCE_PANEL:
                        if (emp.isPlayer())
                            loadHelpUI3();
                        else
                            loadHelpUI4(); 
                        break;
                    case MILITARY_PANEL:
                        loadHelpUI5(); 
                        break;
                    case STATUS_PANEL: 
                        loadHelpUI6(); 
                        break;
                }
                break;
        }
        helpUI.open(this);
    }
    private void loadHelpUI0() {
        int w = getWidth();
        HelpUI helpUI = RotPUI.helpUI();

        int x1 = scaled(150);
        int w1 = scaled(400);
        int y1 = scaled(270);
        HelpUI.HelpSpec sp1 = helpUI.addBrownHelpText(x1, y1, w1, 4, text("RACES_HELP_0A"));

        int x2 = diplomacyBox.x-s30;
        int w2 = scaled(160);
        int y2 = s80;
        int x2a = x2+(w2/2)+s15;
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 4, text("RACES_HELP_0B"));
        sp2.setLine(x2a, y2, x2a, s55);
        
        int x3 = intelligenceBox.x-s20;
        int x3a = x3+(w2/2)+s10;
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y2, w2, 4, text("RACES_HELP_0C"));
        sp3.setLine(x3a, y2, x3a, s55);
        
        int x4 = militaryBox.x-s10;
        int x4a = x4+(w2/2)+s5;
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y2, w2, 4, text("RACES_HELP_0D"));
        sp4.setLine(x4a, y2, x4a, s55);
        
        int x5 = statusBox.x;
        int x5a = x5+(w2/2);
        HelpUI.HelpSpec sp5 = helpUI.addBrownHelpText(x5, y2, w2, 4, text("RACES_HELP_0E"));
        sp5.setLine(x5a, y2, x5a, s55);
        
        int x6 = w-scaled(490);
        int w6 = scaled(210);
        int y6 = scaled(180);
        int x6a = w-scaled(240);
        HelpUI.HelpSpec sp6 = helpUI.addBrownHelpText(x6, y6, w6, 4, text("RACES_HELP_0F"));
        sp6.setLine(x6+w6, y6, x6a, scaled(140));
        
        if (empires.size() > 1) {
            int y7 = scaled(290);
            HelpUI.HelpSpec sp7 = helpUI.addBrownHelpText(x6,y7,w6, 4, text("RACES_HELP_0G"));
            sp7.setLine(x6+w6, y7, x6a, scaled(240));
        }
    }
    private void loadHelpUI1() {
        int w = getWidth();
        HelpUI helpUI = RotPUI.helpUI();

        int x2 = scaled(30);
        int w2 = scaled(200);
        int y2 = scaled(140);
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 3, text("RACES_HELP_1A"));
        sp2.setLine(x2+w2, y2+(sp2.height()/2), scaled(280), y2+(sp2.height()/2));
        
        int x3 = w-scaled(780);
        int w3 = scaled(200);
        int y3 = scaled(65);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 3, text("RACES_HELP_1B"));
        sp3.setLine(x3+w3-s30, y3+sp3.height(), w-scaled(525), scaled(225));
        
        int y4 = scaled(450);
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x3, y4, w3, 3, text("RACES_HELP_1C"));
        sp4.setLine(x3+w3, y4+(sp4.height()/2), w-scaled(470), scaled(470));
        
        int y5 = scaled(630);
        HelpUI.HelpSpec sp5 = helpUI.addBrownHelpText(x3, y5, w3, 3, text("RACES_HELP_1D"));
        sp5.setLine(x3+w3, y5+(sp5.height()/2), w-scaled(555),  y5+(sp5.height()/2));
        
        int x6 = w-scaled(470);
        int w6 = scaled(210);
        int y6 = scaled(55);
        int x6a = w-scaled(65);
        HelpUI.HelpSpec sp6 = helpUI.addBrownHelpText(x6, y6, w6, 3, text("RACES_HELP_1F"));
        sp6.setLine(x6+w6, y6+(sp6.height()/2), x6a, scaled(85));
        
        int x7 = scaled(150);
        int w7 = scaled(300);
        int y7 = scaled(460);
        HelpUI.HelpSpec sp7 = helpUI.addBrownHelpText(x7, y7, w7, 5, text("RACES_HELP_1E"));
        sp7.setLine(x7+(w7/2), y7, x7+(w7/2), scaled(400));
    }
    private void loadHelpUI2() {
        int w = getWidth();
        HelpUI helpUI = RotPUI.helpUI();

        int x2 = scaled(30);
        int w2 = scaled(200);
        int y2 = scaled(140);
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 3, text("RACES_HELP_2A"));
        sp2.setLine(x2+w2, y2+(sp2.height()/2), scaled(280), y2+(sp2.height()/2));
        
        int x3 = w-scaled(780);
        int w3 = scaled(200);
        int y3 = scaled(65);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 3, text("RACES_HELP_2B"));
        sp3.setLine(x3+w3, y3+sp3.height(), w-scaled(525), scaled(165));
        
        int y4 = scaled(350);
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x3, y4, w3, 3, text("RACES_HELP_2C"));
        sp4.setLine(x3+w3, y4+(sp4.height()/2), w-scaled(480), scaled(370));
        
        int y5 = scaled(450);
        HelpUI.HelpSpec sp5 = helpUI.addBrownHelpText(x3, y5, w3, 3, text("RACES_HELP_2D"));
        sp5.setLine(x3+w3, y5+(sp5.height()/2),  w-scaled(555),  y5+(sp5.height()/2));
        
        int x6 = scaled(150);
        int w6 = scaled(300);
        int y6 = scaled(460);
        HelpUI.HelpSpec sp6 = helpUI.addBrownHelpText(x6, y6, w6, 5, text("RACES_HELP_2E"));
        sp6.setLine(x6+(w6/2), y6, x6+(w6/2), scaled(400));
        
        int x7 = scaled(340);
        int w7 = scaled(300);
        int y7 = scaled(250);
        HelpUI.HelpSpec sp7 = helpUI.addBrownHelpText(x7,y7,w7, 2, text("RACES_HELP_2F"));
        sp7.setLine(x7+(w7/2), y7+sp7.height(), x7+(w7/2), scaled(315));
    }
    private void loadHelpUI3() {
        int w = getWidth();
        HelpUI helpUI = RotPUI.helpUI();

        int x2 = scaled(50);
        int w2 = scaled(200);
        int y2 = scaled(165);
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 3, text("RACES_HELP_3A"));
        sp2.setLine(x2+w2, y2+(sp2.height()/2), scaled(430), y2+(sp2.height()/2), scaled(490), scaled(220));
        
        int x3 = w-scaled(760);
        int w3 = scaled(190);
        int y3 = scaled(65);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 3, text("RACES_HELP_3B"));
        sp3.setLine(x3+(w3*2/3), y3+sp3.height(), w-scaled(525), scaled(255));
        
        int x4 = scaled(255);
        int w4 = scaled(360);
        int y4 = scaled(445);
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y4, w4, 3, text("RACES_HELP_3C"));
    }
    private void loadHelpUI4() {
        int w = getWidth();
        HelpUI helpUI = RotPUI.helpUI();

        int x2 = scaled(30);
        int w2 = scaled(230);
        int y2 = scaled(160);
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 4, text("RACES_HELP_4A"));
        sp2.setLine(x2+w2, y2+(sp2.height()/2), scaled(390), y2+(sp2.height()/2), scaled(450), scaled(220));
        
        int x3 = w-scaled(525);
        int w3 = scaled(220);
        int y3 = scaled(250);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 7, text("RACES_HELP_4B"));
        sp3.setLine(x3+(w3/2), y3, x3+(w3/2), scaled(150));
        
        int x4 = scaled(255);
        int w4 = scaled(360);
        int y4 = scaled(445);
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y4, w4, 3, text("RACES_HELP_4C"));
    }
    private void loadHelpUI5() {
        int w = getWidth();
        HelpUI helpUI = RotPUI.helpUI();

        int x2 = scaled(490);
        int w2 = scaled(250);
        int y2 = s50;
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 6, text("RACES_HELP_5A"));
        sp2.setLine(x2, y2+(sp2.height()*2/3), scaled(460), scaled(170));
        
        int x3 = scaled(30);
        int w3 = scaled(240);
        int y3 = scaled(190);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 5, text("RACES_HELP_5B"));
        sp3.setLine(x3+(w3/2), y3+sp3.height(), scaled(180), scaled(350));

        int x4 = w-scaled(470);
        int y4 = s60;
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y4, w2, 3, text("RACES_HELP_5C"));
        sp4.setLine(x4+(w2/2), y4+sp4.height(), x4+(w2/3), y4+sp4.height()+s30);
    }
    private void loadHelpUI6() {
        int w = getWidth();
        int h = getHeight();
        HelpUI helpUI = RotPUI.helpUI();

        int barW = (w-scaled(410))/3;
        int barH = (h-scaled(350))/2;
        boolean playerSelected = selectedEmpire().isPlayer();
        if (playerSelected) {
            int x2 = scaled(350);
            int w2 = scaled(300);
            int y2 = scaled(100);
            HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 5, text("RACES_HELP_6A"));
        }
        else {
            int x2 = scaled(350);
            int w2 = scaled(300);
            int y2 = scaled(100);
            HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 5, text("RACES_HELP_6A2"));
        }
        
        int x3 = playerSelected ? scaled(140) : scaled(115);
        int w3 = scaled(190);
        int y3 = scaled(405);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 3, text("RACES_HELP_6B"));
        
        int x4 = x3+barW;
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y3, w3, 3, text("RACES_HELP_6C"));
        
        int x5 = x4+barW;
        HelpUI.HelpSpec sp5 = helpUI.addBrownHelpText(x5, y3, w3, 3, text("RACES_HELP_6D"));
        
        int y6 = y3+barH;
        HelpUI.HelpSpec sp6 = helpUI.addBrownHelpText(x3, y6, w3, 3, text("RACES_HELP_6E"));
        
        HelpUI.HelpSpec sp7 = helpUI.addBrownHelpText(x4, y6, w3, 3, text("RACES_HELP_6F"));
        
        HelpUI.HelpSpec sp8 = helpUI.addBrownHelpText(x5, y6, w3, 3, text("RACES_HELP_6G"));
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
    public Empire selectedIconEmpire() {
        Empire e = (Empire) sessionVar("RACESUI_ICON_EMPIRE");
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
    public void selectedIconEmpire(Empire e)  {
        if (e != selectedIconEmpire()) 
            sessionVar("RACESUI_ICON_EMPIRE", e);
    }
    public Shape selectedIconShape() {
        Empire e = selectedIconEmpire();
        return colorIcons.get(e);
    }
    private boolean selectPrevEmpire() {
        int index = empires.indexOf(selectedEmpire());
        if (index <= 0)
            return false;
        selectedEmpire(empires.get(index-1));
        return true;    
    }
    private boolean selectNextEmpire() {
        int index = empires.indexOf(selectedEmpire());
        if ((index + 1) >= empires.size())
            return false;
        selectedEmpire(empires.get(index+1));
        return true;    
    }
    public EmpireView selectedView() {
        Empire selectedEmpire = selectedEmpire();
        if (selectedEmpire == player())
            return null;
        return player().viewForEmpire(selectedEmpire);
    }
    public void selectDiplomacyTab()  { diploPanel.init(); titlePanel.selectTab(DIPLOMACY_PANEL); }
    public void selectIntelligenceTab()  { titlePanel.selectTab(INTELLIGENCE_PANEL); }
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
        Border emptyBorder = newEmptyBorder(0, 8, pad, pad);
        setBorder(emptyBorder);

        diploPanel = new RacesDiplomacyUI(this);
        intelPanel = new RacesIntelligenceUI(this);
        militaryPanel = new RacesMilitaryUI(this);
        statusPanel = new RacesStatusUI(this);
        setColorPanel = new RacesSetColorUI(this);

        // create center panel
        titlePanel = new MainTitlePanel(this, "RACES_TITLE");
        
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
        mainPanel.setBorder(newEmptyBorder(10,0,10,0));
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(cardPanel, BorderLayout.CENTER);

        // create design slot panel on right side of UI
        raceListingPanel = new RacePlayerRelationsPane();
        EmpireTitlePanel slotsTitlePanel = new EmpireTitlePanel("RACES_SELECTOR_TITLE");
        BasePanel rightPanel = new BasePanel();
        rightPanel.setPreferredSize(new Dimension(rightPaneW, getHeight()));
        rightPanel.setBorder(newEmptyBorder(10,0,0,0));
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
    public void drawAllianceStars(Graphics2D g, int x, int y, int val, int imgW) {
        int[] stars = new int[5];
        Image[] img = new Image[3];
        img[0] = image("Star_Empty");
        img[1] = image("Star_Half");
        img[2] = image("Star_Full");

        // 90+ = 2 2 2 2 2
        // 80+ = 2 2 2 2 1
        // 70+ = 2 2 2 2 0
        // 60+ = 2 2 2 1 0
        // 50+ = 2 2 2 0 0
        // 40+ = 2 2 1 0 0
        // 30+ = 2 2 0 0 0
        // 20+ = 2 1 0 0 0
        // 10+ = 2 0 0 0 0
        // 0+  = 1 0 0 0 0
        // <0  = 0 0 0 0 0

        if (val >= 0)  stars[0]=1;
        if (val >= 10) stars[0]=2;
        if (val >= 20) stars[1]=1;
        if (val >= 30) stars[1]=2;
        if (val >= 40) stars[2]=1;
        if (val >= 50) stars[2]=2;
        if (val >= 60) stars[3]=1;
        if (val >= 70) stars[3]=2;
        if (val >= 80) stars[4]=1;
        if (val >= 90) stars[4]=2;

        for (int i=0;i<stars.length;i++) {
            g.drawImage(img[stars[i]], x, y-imgW, imgW, imgW, this);
            x = x+imgW;
        }
    }
    @Override
    public void keyPressed(KeyEvent e) {
        if (frame().getGlassPane().isVisible()) {
            BasePanel selectionPane = (BasePanel) frame().getGlassPane();
            selectionPane.keyPressed(e);
            return;
        }
        boolean shift = e.isShiftDown();
        int k = e.getKeyCode();
        switch(k) {
            case KeyEvent.VK_F1:
                showHelp();
                return;
            case KeyEvent.VK_A:
                if (selectedPanel.equals(DIPLOMACY_PANEL)) {
                    if (!selectedEmpire().isPlayer())
                        diploPanel.openEmbassy();
                }
                return;
            case KeyEvent.VK_D:
                if (selectedPanel.equals(DIPLOMACY_PANEL)) {
                    if (selectedEmpire().isPlayer())
                        diploPanel.openManageDiplomatsPane();
                }
                return;
            case KeyEvent.VK_S:
                if (selectedPanel.equals(DIPLOMACY_PANEL)) {
                    if (selectedEmpire().isPlayer())
                        diploPanel.openManageSpiesPane();
                }
                return;
            case KeyEvent.VK_ESCAPE:
                exit(false);
                return;
            case KeyEvent.VK_TAB:
                if (!shift)
                    titlePanel.selectNextTab();
                else
                    titlePanel.selectPreviousTab();
                return;
            case KeyEvent.VK_UP:
                if (selectPrevEmpire())
                    repaint();
                return;
            case KeyEvent.VK_DOWN:
                if (selectNextEmpire())
                    repaint();
                return;
        }
    }
    private void exit(boolean pauseNextTurn) {
        buttonClick();
        RotPUI.instance().selectMainPanel(pauseNextTurn);
    }
    class MainTitlePanel extends BasePanel implements MouseMotionListener, MouseListener {
        private static final long serialVersionUID = 1L;
        RacesUI parent;
        String titleKey;
        public MainTitlePanel(RacesUI p, String s) {
            parent = p;
            titleKey = s;
            initModel();
        }
        Rectangle hoverBox;
        Rectangle helpBox = new Rectangle();
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
            int helpW = s30;
            int x0 = gap+helpW;
            int y0 = h - s10;
            String title = text(titleKey);
            String dipLabel = text("RACES_TAB_DIPLOMACY");
            String intLabel = text("RACES_TAB_INTELLIGENCE");
            String milLabel = text("RACES_TAB_MILITARY");
            String statusLabel = text("RACES_TAB_STATUS");

            drawHelpButton(g);

            g.setColor(SystemPanel.orangeText);
            g.setFont(narrowFont(32));
            int titleW = g.getFontMetrics().stringWidth(title);
            int titleSpacing = s60+s60;
            drawString(g,title, x0,y0);

            int tabW = (w-titleW-helpW-titleSpacing-(5*gap))/4;

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
        private void drawHelpButton(Graphics2D g) {
            helpBox.setBounds(s10,s10,s20,s25);
            g.setColor(darkBrown);
            g.fillOval(s10, s10, s20, s25);
            g.setFont(narrowFont(25));
            if (helpBox == hoverBox)
                g.setColor(Color.yellow);
            else
                g.setColor(Color.white);

            drawString(g,"?", s16, s30);
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
            if (panel.equals(selectedPanel))
                return;

            softClick();
            selectedPanel = panel;
            cardLayout.show(cardPanel, panel);

            switch(panel) {
                case DIPLOMACY_PANEL: diploPanel.open(); break;
                case INTELLIGENCE_PANEL: intelPanel.open(); break;
                case MILITARY_PANEL: militaryPanel.open(); break;
                case STATUS_PANEL: statusPanel.open(); break;
            }
            instance.repaint();
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
                else if (hoverBox == helpBox)
                    parent.showHelp();
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
           else if (helpBox.contains(x,y))
                hoverBox = helpBox;

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
            drawString(g,title, s10, s35);
        }
    }
    final class RacePlayerRelationsPane extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        private final HashMap<Empire, Rectangle> contactBoxes = new HashMap<>();
        private Empire hoverEmp;
        private boolean hoveringIcon;
        int dragY;
        int contactsY, contactsYMax;    
        int contactH, listH;
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
            contactH = s100;
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
            if (UserPreferences.texturesInterface()) 
                drawTexture(g,0,0,w,h);
            for (Rectangle r: contactBoxes.values())
                r.setBounds(0,0,0,0);

            listH = h-y1-s10;
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
            Color whiteC = (emp == hoverEmp) && !hoveringIcon ? Color.yellow : selected ? Color.white : SystemPanel.whiteText;
             //empire name
            int x1 = x0+w1+mgn+s5;
            int y1 = y0+s25;
            g.setFont(narrowFont(22));
            drawShadowedString(g, emp.raceName(), 1, x1, y1, blackC, whiteC);
            Shape sh = emp.drawShape(g,x+w-s30,y0+s10,s20,s20);
            setColorIcon(emp, sh);
            if ((hoverEmp == emp) && hoveringIcon) {
                Stroke prev = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(Color.yellow);
                g.draw(sh);
                g.setStroke(prev);
            }
            boolean inRange = true;
            if (emp.isPlayer()) {
                List<EmpireView> views = player().contacts();
                int n = views.size();
                g.setFont(narrowFont(16));
                g.setColor(blackC);
                y1 += s20;
                String line1 = text("RACES_KNOWN_EMPIRES", n);
                scaledFont(g, line1, scaled(130), 16, 12);
                drawString(g,line1, x1, y1);
                if (n > 0) {
                    int r = 0;
                    for (EmpireView v : views) {
                        if (!v.diplomats())
                            r++;
                    }
                    y1 += s16;
                    String line2 = text("RACES_RECALLED_DIPLOMATS", r);
                    scaledFont(g, line2, scaled(130), 16, 12);
                    drawString(g,line2, x1, y1);
                }
            }
            else {
                EmpireView view = player().viewForEmpire(emp);
                inRange = view.inEconomicRange();
                g.setColor(blackC);
                g.setFont(narrowFont(16));

                if (inRange) {
                    // treaty status
                    y1 += s20;
                    DiplomaticTreaty treaty = view.embassy().treaty();
                    boolean isAlly = treaty.isAlliance();
                    int starW = s8;
                    String s = treaty.status(player());
                    int sw = g.getFontMetrics().stringWidth(s);
                    scaledFont(g, s, scaled(130), 16, 12);
                    drawString(g,s, x1, y1);
                    if (isAlly) {
                        TreatyAlliance alliance = (TreatyAlliance) treaty;
                        drawAllianceStars(g,x1+sw+s5,y1-s3,alliance.standing(player()),starW);
                    }
                    // trade
                    y1 += s16;
                    int level = view.trade().level();
                    String tradeStr = (level == 0) ? text("RACES_TRADE_NONE") : text("RACES_TRADE_LEVEL", level);
                    scaledFont(g, tradeStr, scaled(130), 16, 12);
                    drawString(g,tradeStr, x1, y1);
                }
                else {
                    y1 += s20;
                    String line = text("RACES_OUT_OF_RANGE");
                    scaledFont(g, line, scaled(130), 16, 12);
                    drawString(g,line, x1, y1);
                }
                if (!emp.masksDiplomacy()) 
                    drawRelationsBar(g, emp, x1, y0+h0-s25, w0-x1-s10, s10, s10, s5);
            }
            
            if (UserPreferences.texturesInterface()) 
                drawTextureWithExistingClip(g, x0,y0,w0,h0);
            drawRaceImage(g, emp, back, x0, y0, h0, inRange);
        }
        public void drawRaceImage(Graphics2D g, Empire emp, BufferedImage back, int x, int y, int h, boolean inRange) {
            BufferedImage img = emp.race().diploMugshotQuiet();
            
            int w1 = back.getWidth();
            int h1 = back.getHeight();
            int mgn = (h-h1)/2;
            
            g.drawImage(back, x+mgn, y+mgn, null);
            g.drawImage(img, x+mgn, y+mgn, w1, h1, null);

            if (emp.isPlayer())
                return;
            
            String text = "";
            EmpireView view = player().viewForEmpire(emp);
            boolean dipGone = view.embassy().diplomatGone();
            boolean otherDipGone = view.otherView().embassy().diplomatGone();
            if (!inRange) 
                text = text("RACES_OUT_OF_RANGE");
            else if (dipGone && otherDipGone)
                text = text("RACES_DIPLOMATS_RECALLED");
            else if (dipGone) {
                text = text("RACES_DIPLOMAT_RECALLED");
                text = player().replaceTokens(text, "alien");
            }
            else if (otherDipGone) {
                text = text("RACES_DIPLOMAT_RECALLED", emp.raceName());
                text = emp.replaceTokens(text, "alien");
            }
                        
            if (!text.isEmpty()) {
                g.setFont(narrowFont(14));
                Composite prevC = g.getComposite();
                Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f);
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
                    drawString(g,line, x2, y2);
                    y2 += s16;
                }
            }
        }
        public BufferedImage raceBackImg() {
            if (raceBackImg == null)
                initRaceBackImg();
            return raceBackImg;
        }
        public void changedEmpire() {
            Empire emp = selectedEmpire();
            int empIndex = empires.indexOf(emp);
            
            int numEmpires = (getHeight()-s10)/contactH;
            if (empIndex <= numEmpires) {
                contactsY = 0;
                return;
            }
            int offset = min(empIndex, empires.size()-numEmpires);
            contactsY = contactH*offset;
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
        public void setColorIcon(Empire emp, Shape sh) {
            colorIcons.put(emp, sh);
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
                    //ail: dragging inside of the list should not be accelerated
                    int dListY = -dY;
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
            hoveringIcon = false;
            for (Empire emp: colorIcons.keySet()) {
                Shape rec = colorIcons.get(emp);
                if (rec.contains(x,y)) {
                    hoverShape = rec;
                    hoverEmp = emp;
                    hoveringIcon = true;
                    break;
                }
            }
            if (hoverShape == null) {
                for (Empire emp: contactBoxes.keySet()) {
                    Rectangle rec = contactBoxes.get(emp);
                    if (rec.contains(x,y)) {
                        hoverShape = rec;
                        hoverEmp = emp;
                        break;
                    }
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

            if (hoverEmp != null) {
                softClick();
                if (hoveringIcon) {
                    hoveringIcon = false;
                    selectedIconEmpire(hoverEmp);
                    enableGlassPane(setColorPanel);
                }
                else if (hoverEmp != selectedEmpire())
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