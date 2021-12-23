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
package rotp.ui.tech;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rotp.model.tech.Tech;
import rotp.model.tech.TechCategory;
import rotp.model.tech.TechTree;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.game.HelpUI;
import rotp.ui.main.SystemPanel;

public class AllocateTechUI extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    private int selectedCategory = 0;
    private final Color yellowTextC = new Color(255,192,0);
    private final Color whiteTextC = new Color(239,239,239);
    private final Color darkBrownC = new Color(112,85,68);
    private final Color blueBucketC = new Color(32,132,132);
    private final Color subpanelBackC = new Color(178,124,87);
    static final Color sliderHighlightColor = new Color(255,255,255);
    static final Color sliderBoxEnabled = new Color(34,140,142);
    static final Color sliderBoxDisabled = new Color(102,137,137);
    static final Color sliderBackEnabled = Color.black;
    static final Color sliderBackDisabled = new Color(65,65,65);
    static final Color sliderTextEnabled = Color.black;
    static final Color sliderTextDisabled = new Color(65,65,65);
    static final Color eqButtonBorderC = new Color(166,153,145);
    public static final Color tierBackC = new Color(24,18,14);
    public static final Color tierNumC = new Color(48,36,28);
    public static final Color currentTechC = new Color(217,164,0);
    public static final Color unknownTechC = new Color(145,102,72);
    static final Color knownTechC = new Color(75,99,51);
    static final Color techUnderscoreC = new Color(255,255,255,90);

    private LinearGradientPaint backGradient;
    private ExitTechButton exitButton;
    private final Rectangle equalizeButton = new Rectangle();
    private Shape hoverBox;
    Rectangle techBox = new Rectangle();
    Rectangle helpBox = new Rectangle();
    private final Rectangle[] catBox = new Rectangle[TechTree.NUM_CATEGORIES];
    private final Polygon[] leftArrow = new Polygon[TechTree.NUM_CATEGORIES];
    private final Polygon[] rightArrow = new Polygon[TechTree.NUM_CATEGORIES];
    private final Rectangle[] labelBox = new Rectangle[TechTree.NUM_CATEGORIES];
    private final Rectangle[] sliderBox = new Rectangle[TechTree.NUM_CATEGORIES];
    private final Rectangle treeBox = new Rectangle();
    private final Rectangle catArea = new Rectangle();
    private final Map<RoundRectangle2D.Float,String> techSelections = new HashMap<>();
    private Point2D.Float[] currentTechs = new Point2D.Float[TechTree.NUM_CATEGORIES];
    private BufferedImage visualTree;
    int treeX, treeY;
    int dragX, dragY;
    float totalPlanetaryResearch = -1;
    float totalPlanetaryResearchSpending = 0;
    
    
    public AllocateTechUI() {
        initModel();
    }

    @Override
    public boolean hasStarBackground()  { return true; }
    private void initModel() {
        for (int i=0;i<catBox.length;i++)
            catBox[i] = new Rectangle();
        for (int i=0;i<labelBox.length;i++)
            labelBox[i] = new Rectangle();
        for (int i=0;i<sliderBox.length;i++)
            sliderBox[i] = new Rectangle();
        for (int i=0;i<leftArrow.length;i++)
            leftArrow[i] = new Polygon();
        for (int i=0;i<rightArrow.length;i++)
            rightArrow[i] = new Polygon();

        exitButton = new ExitTechButton(scaled(235), s50, s2, s2);
        add(exitButton);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }
    public void init() {
        totalPlanetaryResearch = -1;
        totalPlanetaryResearchSpending = player().totalPlanetaryResearchSpending();
        resetData();
    }
    public void adjustPlanetaryResearch(float amt) { 
        if (totalPlanetaryResearch != -1)
            totalPlanetaryResearch += amt;
    }
    public void resetPlanetaryResearch() { 
        totalPlanetaryResearch = -1;
    }
    public float totalPlanetaryResearch() {
        if (totalPlanetaryResearch < 0)
            totalPlanetaryResearch = player().totalPlanetaryResearch();
        return totalPlanetaryResearch;
    }
    @Override
    public String textureName()     { return TEXTURE_BROWN; }
    @Override
    public void drawTexture(Graphics g0) {
        // this UI will manually override
    }
    @Override
    public void cancelHelp() {
        RotPUI.helpUI().close();
    }
    @Override
    public void showHelp() {
        loadHelpUI();
        repaint();   
    }
    @Override 
    public void advanceHelp() {
        cancelHelp();
    }
    private void loadHelpUI() {
        int w = getWidth();
        HelpUI helpUI = RotPUI.helpUI();
        helpUI.clear();

        int x1 = scaled(150);
        int w1 = scaled(400);
        int y1 = scaled(330);
        HelpUI.HelpSpec sp1 = helpUI.addBrownHelpText(x1, y1, w1, 5, text("TECH_HELP_1A"));

        int x2 = scaled(10);
        int w2 = scaled(160);
        int y2 = scaled(100);
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 5, text("TECH_HELP_1B"));
        sp2.setLine(scaled(60), y2, s60, s70);
        
        int x3 = scaled(180);
        int w3 = scaled(190);
        int y3 = scaled(100);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 6, text("TECH_HELP_1C"));
        sp3.setLine(scaled(200), y3, scaled(200), s70);
        
        int x4 = scaled(380);
        int w4 = scaled(190);
        int y4 = scaled(100);
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y4, w4, 5, text("TECH_HELP_1D"));
        sp4.setLine(scaled(390), y4, scaled(340), s70);
        
        int x5 = scaled(580);
        int w5 = scaled(190);
        int y5 = scaled(100);
        HelpUI.HelpSpec sp5 = helpUI.addBrownHelpText(x5, y5, w5, 5, text("TECH_HELP_1E"));
        sp5.setLine(scaled(575), s70, scaled(675), y5, scaled(755), s70);
        
        int x6 = w-scaled(450);
        int w6 = scaled(190);
        int y6 = scaled(80);
        HelpUI.HelpSpec sp6 = helpUI.addBrownHelpText(x6, y6, w6, 5, text("TECH_HELP_1F"));
        sp6.setLine(x6+w6, scaled(140), w-scaled(190), scaled(140));
        
        int y7 = scaled(210);
        HelpUI.HelpSpec sp7 = helpUI.addBrownHelpText(x6,y7,w6, 5, text("TECH_HELP_1G"));
        sp7.setLine(x6+w6, y7, w-scaled(235), scaled(195));
        
        int y8 = scaled(360);
        HelpUI.HelpSpec sp8 = helpUI.addBrownHelpText(x6,y8,w6, 6, text("TECH_HELP_1H"));
        sp8.setLine(x6+w6, y8+(sp8.height()/2), w-scaled(235), y8+(sp8.height()/2));

        int x9 = w-scaled(490);
        int y9 = scaled(580);
        HelpUI.HelpSpec sp9 = helpUI.addBrownHelpText(x9,y9,w6, 4, text("TECH_HELP_1I"));
        sp9.setLine(x9+w6, scaled(610), w-scaled(240), scaled(610));

        helpUI.open(this);
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        paintToImage(screenBuffer());
        g.drawImage(screenBuffer(), 0,0, this);
        exitButton.setBounds(w-scaled(250),h-s73,scaled(235),s50);
    }
    private void paintToImage(Image img) {
        Graphics2D g = (Graphics2D) img.getGraphics();

        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();

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

        drawHelpButton(g);
        
        String title = text("TECH_RESEARCH");
        g.setFont(narrowFont(32));
        g.setColor(yellowTextC);
        drawString(g,title,s50, s40);

        int cats = TechTree.NUM_CATEGORIES;
        int gap = s5;
        int catH = s22;
        int topM = s50;
        int bottomM = s25;
        int leftM = s20;
        int rightM = scaled(265);
        int openCatH = h-topM-bottomM-((cats-1)*gap)-((cats-1)*catH);

        int y0= topM;
        int x0= leftM;
        int catW = w-rightM-leftM;
        for (int i=0;i<cats;i++) {
            if (i == selectedCategory) {
                drawOpenTechCategory(g, i, x0, y0, catW, openCatH, catH);
                y0 += openCatH;
            }
            else {
                drawClosedTechCategory(g, i, x0, y0, catW, catH);
                y0 += catH;
            }
            y0 += gap;
        }
        catArea.setBounds(leftM,topM,catW,y0-topM);
        
        if (techSelections.keySet().contains(hoverBox)) {
            RoundRectangle2D rect = (RoundRectangle2D) hoverBox;
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.setColor(Color.yellow);
            g.setClip(leftM+s10,topM+s20,catW-s20,y0-topM-s40);
            g.drawRoundRect((int)(rect.getX()-treeX+treeBox.x), (int)(rect.getY()-treeY+treeBox.y), (int) rect.getWidth(),(int) rect.getHeight(),(int)  rect.getArcWidth(), (int) rect.getArcHeight());
            g.setClip(null);
            g.setStroke(prev);
        }

        // draw RP remaining or % success for current tech
        Point2D.Float techPt = currentTechs[selectedCategory];
        if (techPt != null) {
            int y1 = (int) techPt.y-treeY+treeBox.y;
            int x1 = (int) techPt.x-treeX+treeBox.x;       
            TechCategory cat = player().tech().category(selectedCategory);
            Tech tech = tech(cat.currentTech());
            String costLbl = techCostString(cat, tech);
            g.setFont(narrowFont(20));
            int costSW = g.getFontMetrics().stringWidth(costLbl);
            g.setColor(Color.black);
            drawString(g,costLbl, x1-costSW, y1);        
        }
        
        // draw right-side panel
        int subPanelX = w-scaled(250);
        int subPanelW = scaled(233);
        int subPanelY = s50;
        int subPanelH = scaled(585);
        String subtitle = text("TECH_RESEARCH_POINTS");
        g.setFont(narrowFont(28));
        int subSW = g.getFontMetrics().stringWidth(subtitle);
        g.setColor(yellowTextC);

        int x4 = subPanelX+((subPanelW-subSW)/2);
        drawString(g,subtitle, x4, subPanelY-s8);

        g.setColor(subpanelBackC);
        g.fillRect(subPanelX, subPanelY, subPanelW, subPanelH);

        g.setColor(darkBrownC);
        int topSubPaneH = scaled(173);
        g.fillRect(subPanelX+s10, subPanelY+s10, subPanelW-s20, topSubPaneH);

        int botSubPaneY = subPanelY+topSubPaneH+s18;
        int botSubPaneH = subPanelY+subPanelH-s10-botSubPaneY;
        g.fillRect(subPanelX+s10, botSubPaneY, subPanelW-s20, botSubPaneH);

        // fill in top right panel
        String title2 = text("TECH_EMPIRE_SPENDING");
        g.setFont(narrowFont(20));
        this.scaledFont(g, title2, scaled(200), 20, 15);
        drawShadowedString(g, title2, 1, subPanelX+s20, subPanelY+s30, Color.black, Color.white);

        g.setFont(plainFont(16));
        g.setColor(Color.black);
        int y5 = subPanelY+s35;
        List<String> descLines = wrappedLines(g, text("TECH_EMPIRE_SPENDING_DESC"), scaled(200));
        if (descLines.size() == 1)
            y5 += s10;
        for (String descLine: descLines) {
            y5 += s15;
            drawString(g,descLine, subPanelX+s20, y5);
        }

        TechTree tree = player().tech();
        int totalSpending = (int) totalPlanetaryResearchSpending;
        int totalResearch = 0;       
        for (int i=0;i<cats;i++)
             totalResearch += tree.category(i).currentResearch(totalPlanetaryResearch());

        int y6 = subPanelY+s93;
        g.setFont(plainFont(25));
        String spending = shortFmt(totalSpending);
        String research = shortFmt(totalResearch);
        String detailLine1 = text("TECH_EMPIRE_DETAIL_1", spending, research);
        int detSW = g.getFontMetrics().stringWidth(detailLine1);
        int x6 = subPanelX+((subPanelW-detSW)/2);
        drawString(g,detailLine1, x6, y6);
        String detailLine2 = text("TECH_EMPIRE_DETAIL_2", spending, research);
        detSW = g.getFontMetrics().stringWidth(detailLine2);
        y6 += s28;
        x6 = subPanelX+((subPanelW-detSW)/2);
        drawString(g,detailLine2, x6, y6);
        
        int y6a = y6+s10;
        g.setFont(plainFont(16));
        String divertLine = text("TECH_EMPIRE_TECH_RESERVE_OPT");
        int indent = s18;
        List<String> divertLines = wrappedLines(g, divertLine, subPanelW-s30-indent);
        for (String line: divertLines) {
            y6a += s15;
            drawString(g,line, subPanelX+s20+indent, y6a);
        }
     
        int checkW = s12;
        int checkX= subPanelX+s20;
            
        int y6b = y6+s18+(max(2,divertLines.size())*s15/2);
        techBox.setBounds(checkX, y6b-checkW, checkW, checkW);
        Stroke prev = g.getStroke();
        g.setStroke(stroke2);
        g.setColor(subpanelBackC);
        g.fill(techBox);
        if (hoverBox == techBox) {
            g.setColor(Color.yellow);
            g.draw(techBox);
        }
        if (player().divertColonyExcessToResearch()) {
            g.setColor(SystemPanel.whiteText);
            g.drawLine(checkX-s1, y6b-s6, checkX+s3, y6b-s3);
            g.drawLine(checkX+s3, y6b-s3, checkX+checkW, y6b-s12);
        }
        g.setStroke(prev);
    
        // draw tech spending area
        String allocateTitle = text("TECH_ALLOCATE_POINTS");
        g.setFont(narrowFont(20));
        this.scaledFont(g, allocateTitle, scaled(200), 20, 15);
        drawShadowedString(g, allocateTitle, 1, subPanelX+s20, botSubPaneY+s20, Color.black, Color.white);

        // draw equalize spending hihit
        g.setFont(plainFont(16));
        g.setColor(Color.black);
        y6 = botSubPaneY+s25;
        List<String> eqLines = wrappedLines(g, text("TECH_EMPIRE_BALANCED_DESC"), scaled(200));
        for (String line: eqLines) {
            y6 += s15;
            drawString(g,line, subPanelX+s20, y6);
        }

        int x7 = subPanelX+s10;
        int y7 = botSubPaneY+s70;
        int w7 = subPanelW-s20;
        int h7 = s45;
        for (int i=0;i<cats;i++)
            drawCategorySlider(g, i, x7, y7+(i*h7), w7, h7);

         // draw equalize button
        int eqH = s30;
        int eqY = subPanelY+subPanelH-eqH-s10;
        int eqX = subPanelX+s10;
        int eqW = subPanelW-s22;
        equalizeButton.setBounds(eqX, eqY, eqW, eqH);
        Color c9 = hoverBox == equalizeButton ? Color.yellow : eqButtonBorderC;
        prev = g.getStroke();
        g.setStroke(stroke2);
        g.setColor(darkBrownC.darker());
        g.drawRect(eqX+s2, eqY+s2, eqW, eqH);
        g.setColor(darkBrownC);
        g.fill(equalizeButton);
        g.setColor(c9);
        g.draw(equalizeButton);
        g.setStroke(prev);

        String eqText = text("TECH_EQUALIZE");
        g.setFont(narrowFont(20));
        c9 = hoverBox == equalizeButton ? Color.yellow : Color.white;
        int sw9 = g.getFontMetrics().stringWidth(eqText);
        int x9 = eqX+((eqW-sw9)/2);
        int y9 = eqY+eqH-s8;
        drawShadowedString(g, eqText, 1, x9, y9, Color.black, c9);
        drawTexture(g, subPanelX, subPanelY,subPanelW, subPanelH);
        g.dispose();
    }
    private void drawHelpButton(Graphics2D g) {
        helpBox.setBounds(s20,s20,s20,s25);
        g.setColor(darkBrownC);
        g.fillOval(s20, s20, s20, s25);
        g.setFont(narrowFont(25));
        if (helpBox == hoverBox)
            g.setColor(Color.yellow);
        else
            g.setColor(Color.white);

        drawString(g,"?", s26, s40);
    }
    private String techCostString(TechCategory cat, Tech tech) {
        if (tech == null)
            return "";
        float costRP = cat.costForTech(tech);
        float chance = 1;
        if ((cat.currentTech() != null) && cat.currentTech().equals(tech.id)) {
            costRP -= cat.totalBC();
            chance = cat.upcomingDiscoveryChance();
        }
        int cost = max(0,(int)Math.ceil(costRP));
        if (chance > 1) {
            int pct = (int) (100* (chance -1));
            return text("TECH_DISCOVERY_PCT",pct);
        }
        else if (cost > 10000)
            return text("TECH_TOTAL_RP",shortFmt(cost));
        else 
            return text("TECH_TOTAL_RP",cost);
    }
    private void drawCategorySlider(Graphics2D g, int catNum, int x, int y, int w, int h) {
        TechCategory cat = player().tech().category(catNum);
        boolean locked = cat.locked();
        float pct = cat.allocationPct();
        String name = text(cat.id());

        // cat name
        g.setFont(narrowFont(16));
        Color textC;
        if (hoverBox == labelBox[catNum])
            textC = SystemPanel.yellowText;
        else if (locked)
            textC = sliderTextDisabled;
        else
            textC = sliderTextEnabled;
        g.setColor(textC);
        int sw = g.getFontMetrics().stringWidth(name);
        drawString(g,name, x+s30, y+s15);
        labelBox[catNum].setBounds(x+s30, y, sw, s15);

        // cat slider box
        int boxX = x+s20;
        int boxY = y+s19;
        int boxW = w-s100;
        int boxH = s16;
        Color c1 = locked ? sliderBoxDisabled : sliderBoxEnabled;
        Color c2 = locked ? sliderBackDisabled : sliderBackEnabled;
        Color c3;

        g.setColor(c2);
        sliderBox[catNum].setBounds(boxX, boxY, boxW, boxH);
        g.fill(sliderBox[catNum]);
        g.setColor(c1);
        if (pct == 1)
            g.fillRect(boxX, boxY+s1, boxW, boxH-s2);
        else
            g.fillRect(boxX, boxY+s1, (int) (pct*(boxW)), boxH-s2);

        if (hoverBox == sliderBox[catNum]) {
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.setColor(Color.yellow);
            g.draw(sliderBox[catNum]);
            g.setStroke(prev);
        }

        int[] bX = new int[3];
        int[] bY = new int[3];
        // left arrow
        leftArrow[catNum].reset();
        bX[0] = boxX-s11;      bX[1] = boxX-s3; bX[2] = boxX-s3;
        bY[0] = boxY+(boxH/2); bY[1] = boxY;    bY[2] = boxY+boxH;
        for (int i=0;i<bX.length;i++)
            leftArrow[catNum].addPoint(bX[i], bY[i]);
        c3 = hoverBox == leftArrow[catNum] ? SystemPanel.yellowText : c2;
        g.setColor(c3);
        g.fillPolygon(leftArrow[catNum]);

        // right arrow
        rightArrow[catNum].reset();
        bX[0] = boxX+boxW+s11;      bX[1] = boxX+boxW+s3; bX[2] = boxX+boxW+s3;
        bY[0] = boxY+(boxH/2);      bY[1] = boxY;         bY[2] = boxY+boxH;
        for (int i=0;i<bX.length;i++)
            rightArrow[catNum].addPoint(bX[i], bY[i]);
        c3 = hoverBox == rightArrow[catNum] ? SystemPanel.yellowText : c2;
        g.setColor(c3);
        g.fillPolygon(rightArrow[catNum]);

        String rpText = text("TECH_TOTAL_RP",shortFmt(cat.currentResearch(totalPlanetaryResearch())));
        g.setColor(Color.black);
        g.setFont(plainFont(18));
        int rpSW = g.getFontMetrics().stringWidth(rpText);
        int x8 = x+w-rpSW-s10;
        int y8 = y+s32;
        drawString(g,rpText, x8, y8);
    }
    private void drawOpenTechCategory(Graphics2D g, int catNum, int x, int y, int w, int h, int titleH) {
        catBox[catNum].setBounds(0,0,0,0);
        g.setColor(darkBrownC);
        g.fillRect(x, y, w, h);

        drawCategorySummary(g, catNum, x,y,w,titleH);

        int x0[] = { x+s3, x+s17, x+s10 };
        int y0[] = { y+(titleH/2)-s2, y+(titleH/2)-s2, y+titleH-s5 };
        g.setColor(Color.black);
        g.fillPolygon(x0, y0, x0.length);

        int x1 = x+s10;
        int w1 = w-s20;
        int y1 = y+titleH;
        int h1 = h-s10-titleH;
        treeBox.setBounds(x1,y1,w1,h1);
        
        drawTexture(g, x,y,w,h);
        paintTree(g);
    }
    private void drawClosedTechCategory(Graphics2D g, int catNum, int x, int y, int w, int h) {
        catBox[catNum].setBounds(x,y,w,h);
        g.setColor(darkBrownC);
        g.fillRect(x, y, w, h);
        drawCategorySummary(g, catNum, x,y,w,h);

        int x0[] = { x+s5, x+s5, x+s12 };
        int y0[] = { y+s3, y+s17, y+(h/2) };
        g.setColor(Color.black);
        g.fillPolygon(x0, y0, x0.length);

        if (hoverBox == catBox[catNum]) {
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.setColor(Color.yellow);
            g.drawRect(x,y,w,h);
            g.setStroke(prev);
        }
        drawTexture(g, x,y,w,h);
    }
    private void drawCategorySummary(Graphics2D g, int catNum, int x, int y, int w, int h) {
        TechCategory cat = player().tech().category(catNum);
        Tech tech = tech(cat.currentTech());

        int y0 = y+h-s5;
        g.setFont(narrowFont(20));
        String title = text(TechCategory.id(catNum));
        drawShadowedString(g, title, 1, x+s20, y0, Color.black, whiteTextC);

        drawResearchBubble(g, cat, false, Color.black, blueBucketC, Color.black, x+scaled(180), y0);

        int fontSize = 18;
        g.setColor(Color.black);
        g.setFont(narrowFont(fontSize));
        String researching = cat.researchCompleted() ? text("TECH_RESEARCH_COMPLETED") : text("TECH_RESEARCHING");
        int sw1 = g.getFontMetrics().stringWidth(researching);
        int x1 =x+scaled(200);
        drawString(g,researching, x1, y0);

        if (!cat.researchCompleted()) {
            g.setFont(plainFont(fontSize));
            String techName = tech == null ? text("TECH_NONE_RESEARCHED") : tech.name();
            drawString(g,techName, x1+sw1+s5, y0);
        }

        g.setFont(narrowFont(fontSize));
        String detail1Key = cat.techDescription1(true);
        int sw2 = g.getFontMetrics().stringWidth(detail1Key);
        int x2 = x+scaled(500);
        drawString(g,detail1Key, x2, y0);

        g.setFont(plainFont(fontSize));
        String detail1Value = cat.techDescription1(false);
        int x2b = x2+sw2+s10;
        drawString(g,detail1Value, x2b, y0);

        String detail2Key = cat.techDescription2(true);
        if (!detail2Key.isEmpty()) {
            g.setFont(narrowFont(fontSize));
            int sw3 = g.getFontMetrics().stringWidth(detail2Key);
            int x3 = x+scaled(720);
            drawString(g,detail2Key, x3, y0);
            String detail2Value = cat.techDescription2(false);
            int x3b = x3+sw3+s10;
            g.setFont(plainFont(fontSize));
            drawString(g,detail2Value, x3b, y0);
        }
    }
    public void drawResearchBubble(Graphics2D g, TechCategory cat, boolean showMinimum, Color textC, Color backC1, Color backC2, int x, int y) {
        if (!cat.researchCompleted()) {
            float chance = cat.upcomingDiscoveryChance(totalPlanetaryResearch());
            if (showMinimum)
                chance = max(.15f, chance);
            int r = s19;
            if (chance <= 1) {
                g.setColor(backC2);
                g.fillOval(x-s10, y-s16, r, r);
                int lvl = (int)(chance*r);
                g.setColor(backC1);
                g.setClip(x-s10,y+s4-lvl,r+r,r);
                g.fillOval(x-s10, y-s16, r, r);
                g.setClip(null);
            }
            else {
                int pct = (int) (100* (chance -1));
                String strPct = text("TECH_DISCOVERY_PCT",pct);
                g.setFont(narrowFont(18));
                g.setColor(textC);
                int swPct = g.getFontMetrics().stringWidth(strPct);
                drawString(g,strPct, x-(swPct/2), y);
            }
        }

    }
    private BufferedImage visualTree() {
        if (visualTree == null) 
            initVisualTree();        
        return visualTree;
    }
    private void initVisualTree() {
        TechCategory cat = player().tech().category(selectedCategory);
        techSelections.clear();
        boolean newResearch = cat.researchStarted();
        String currentT = cat.currentTech();

        int maxQ = cat.maxResearchableQuintile();
        
        // if we haven't started any research yet (currentT == null)
        // but have acquired a tech through other means (e.g. artifact planet),
        // show all of the known tiers but not the next tier
        if (currentT == null)
            maxQ--;
        
        int maxTechLvl = maxQ*5;
        List<String> knownT = cat.knownTechs();
        List<String> allT = new ArrayList<>(cat.possibleTechs());
        for (String techId: knownT) {
            if (!tech(techId).free)
                allT.add(techId);
        }

        int tierW = scaled(300);
        int tierGap = s100;
        int w = max(treeBox.width, (tierW*(maxQ+1))+(tierGap*(maxQ+2)));
        int h = treeBox.height;
        visualTree = newOpaqueImage(w, h);
        Graphics2D g = (Graphics2D) visualTree.createGraphics();
        setFontHints(g);
        Tech[] techs = new Tech[maxTechLvl+1];
        for (String techId: allT) {
            Tech tech = tech(techId);
            if ((tech.level > 0) && (tech.level <= maxTechLvl))
                techs[tech.level] = tech;
        }
        g.setColor(Color.black);
        g.fillRect(0,0,w,h);
        // draw quintiles from 1 to maxQ
        int x0=0;
        for (int i=0;i<=maxQ;i++) {
            drawTierLevel(g,x0,tierGap,i);
            if (i > 0)
                drawTierArrow(g,x0,tierGap,h);
            x0 += tierGap;
            int minLevel=(5*i)+1;
            int maxLevel=minLevel+4;
            if (i==maxQ)
                drawUnknownTechTier(g,cat,currentT, i, x0,tierW,h);
            else
                drawTechTier(g,techs,newResearch,knownT,currentT,minLevel,maxLevel,x0,tierW,h);
            x0 += tierW;
        }
        g.dispose();
    }
    private void drawUnknownTechTier(Graphics g, TechCategory cat, String currentT, int tier, int x, int w, int h) {
        String title = text("TECH_NEXT_TIER_TITLE");
        String desc;
        
        if (currentT == null)
            desc = text("TECH_FIRST_TIER_DESC");
        else if (tier < 10)
            desc = text("TECH_NEXT_TIER_DESC");
        else 
            desc = text("TECH_FUTURE_TIER_DESC");

        g.setColor(tierBackC);
        g.fillRect(x,0,w,h);

        scaledFont(g, title, w-s20, 20, 16);
        g.setColor(Color.white);
        int titleSW = g.getFontMetrics().stringWidth(title);
        int x0 = x+((w-titleSW)/2);
        int y0 = h*4/9;
        drawString(g,title,x0,y0);

        y0 += s10;
        List<String> descLines = this.scaledPlainWrappedLines(g, desc, w*2/3, 5, 20, 16);
        for (String line: descLines) {
            int lineSW = g.getFontMetrics().stringWidth(line);
            x0 = x+((w-lineSW)/2);
            y0 += s20;
            drawString(g,line,x0,y0);
        }
    }
    private void drawTierArrow(Graphics2D g, int x0, int w, int h) {
        g.setColor(tierBackC);
        int mid = h/2;
        g.fillRect(x0, mid-s30, w/2, s60);
        int x[] = { x0+(w/2), x0+(w/2), x0+w };
        int y[] = { mid-s90, mid+s90, mid };
        g.fillPolygon(x, y, 3);
    }
    private void drawTierLevel(Graphics2D g, int x0, int w, int tierNum) {
        g.setColor(tierNumC);
        g.setFont(narrowFont(30));
        String numStr = str(tierNum+1);
        int sw = g.getFontMetrics().stringWidth(numStr);
        drawString(g,numStr, x0+w-sw-s5, s25);
    }
    private void drawTechTier(Graphics2D g, Tech[] allT, boolean newResearch, List<String> knownT, String currentT, int minLevel, int maxLevel, int x, int w, int h) {
        g.setColor(tierBackC);
        g.fillRect(x,0,w,h);
        // get available techs in this tier
        List<Tech> displayT = new ArrayList<>();
        for (int j=minLevel;j<=maxLevel;j++) {
            if (allT[j] != null)
                displayT.add(allT[j]);
        }
        int n = displayT.size();
        int boxX = x+s10;
        int boxW = w-s20;
        int boxH = (h-s90)/5;
        int boxG = (h-(n*boxH))/(n+1);
        int boxY = 0;
        for (Tech tech: displayT) {
            boxY += boxG;
            drawTechBox(g, tech, newResearch, knownT, currentT, boxX, boxY, boxW, boxH);
            boxY += boxH;
        }
    }
    private void drawTechBox(Graphics2D g, Tech tech, boolean newResearch, List<String> knownT, String currentT, int x, int y, int w, int h) {
        TechCategory cat =  player().tech().category(selectedCategory);
        Color backC = unknownTechC;
        Color textC = Color.white;
        
        boolean known = knownT.contains(tech.id);
        boolean allowSelect = !known && newResearch && !tech.id().equals(currentT);

        if(known) {
            backC = knownTechC;
            textC = Color.black;
        }
        else if (tech.id.equals(currentT)) 
            backC = currentTechC;
        
        Color c0 = backC;
        Color c1 = new Color(backC.getRed(), backC.getGreen(), backC.getBlue(), 80);
        Point2D start = new Point2D.Float(x, y);
        Point2D end = new Point2D.Float(x, y+h);
        float[] dist = {0.0f, 0.2f, 1.0f};
        Color[] colors = {c0, c0, c1 };
        Paint backGradient1 = new LinearGradientPaint(start, end, dist, colors);

        List<String> descLines = this.scaledPlainWrappedLines(g, tech.detail(), w-s14, 4, 16, 13);
        int numLines = descLines.size();
        int hdr1 = numLines <= 4 ? s20 : s15;
        int hdr2 = numLines <= 4 ? s24 : s20;
        int lineH = numLines <= 4 ? s16 : s14;

        int realH = descLines.size() < 4 ? h : h +s12;

        int cnr = min(w,realH)/5;
        RoundRectangle2D.Float techDetailBox = new RoundRectangle2D.Float(x,y,w,realH,cnr,cnr);
        g.setPaint(backGradient1);
        g.fill(techDetailBox);

        g.setColor(textC);
        String name = tech.name();

        int fontSize = scaledFont(g, name, w-s80, 20, 16);
        int y0 = y+hdr1;
        if (known)
            drawString(g,name, x+s7, y0);
        else
            drawShadowedString(g, name, 2, x+s7, y0, SystemPanel.buttonShadowC, textC);
        
        if (!known) {
            // for current techs, the cost label is drawn dynamically over the visualTree
            // image instead of within the image. This allows adjustments of the tech
            // sliders to update the current tech cost label without the costly effort
            // of recreating the visual tree image. To do this, for each current tech
            // we save off the x/y positioning of the cost label for later reference.
            if (tech.id.equals(currentT)) {
                int catId = tech.cat.index();
                Point2D.Float pt = new Point2D.Float(x+w-s10,y0);
                currentTechs[catId] = pt;
            }
            else {
                String costLbl = techCostString(cat, tech);
                g.setFont(narrowFont(fontSize));
                int costSW = g.getFontMetrics().stringWidth(costLbl);
                g.setColor(Color.black);
                drawString(g,costLbl, x+w-s10-costSW, y0);
            }            
        }

        g.setColor(techUnderscoreC);
        g.fillRect(x,y0+s7,w,s2);

        y0 = y0 + hdr2;
        g.setColor(Color.black);
        descLines = this.scaledPlainWrappedLines(g, tech.detail(), w-s14, 4, 16, 13);
        for (String desc: descLines) {
            drawString(g,desc, x+s7, y0);
            y0+= lineH;
        }
        
        if (allowSelect) {
            techSelections.put(techDetailBox, tech.id);
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.setColor(Color.yellow);
            String select = text("TECH_CHANGE_RESEARCH");
            g.setFont(narrowFont(16));
            int sw1 = g.getFontMetrics().stringWidth(select);
            int x1 = x+(w-sw1-s20)/2;
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(x1, y+hdr1+s45, sw1+s20, s20, s5, s5);
            g.setColor(Color.yellow);
            g.drawRoundRect(x1, y+hdr1+s45, sw1+s20, s20, s5, s5);
            g.setStroke(prev);
            g.setColor(SystemPanel.whiteText);
            drawString(g,select, x1+s10, y+hdr1+s60);
        }
    }
    public void selectTechCategory(int i) {
        if ((i < 0) || (i >= catBox.length))
            return;
        if (selectedCategory == i)
            return;
        selectedCategory = i;
        resetData();
        repaint();
    }
    private void resetData() {
        currentTechs = new Point2D.Float[TechTree.NUM_CATEGORIES];
        visualTree = null;
        hoverBox = null;
        treeX = 0;
        treeY = 0;

        Tech currentTech = tech(player().tech().category(selectedCategory).currentTech());
        if (currentTech == null)
            return;

        int quintile = currentTech.quintile;
        // 400px per quintile, 1 starts at 0.  2 & up start at visual 200
        if (quintile > 1) 
            treeX = scaled((400*quintile)-600);
    }
    private void equalize() {
        softClick();
        player().tech().equalizeAllocations();
        repaint();
    }
    private void toggleOverflowSpending() {
        softClick();
        player().toggleColonyExcessToResearch();
        totalPlanetaryResearch = player().totalPlanetaryResearch();
        totalPlanetaryResearchSpending = player().totalPlanetaryResearchSpending();
        repaint();
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        int mods = e.getModifiersEx();
        switch (k) {
            case KeyEvent.VK_F1:
                showHelp();
                return;
            case KeyEvent.VK_EQUALS:
                equalize();
                return;
            case KeyEvent.VK_LEFT:
                dragTree(-s10,0);
                return;
            case KeyEvent.VK_RIGHT:
                dragTree(s10,0);
                return;
            case KeyEvent.VK_UP:
                selectTechCategory(selectedCategory-1);
                return;
            case KeyEvent.VK_DOWN:
                selectTechCategory(selectedCategory+1);
                return;
            case KeyEvent.VK_TAB:
                return;
            case KeyEvent.VK_1:
                if (mods == 0)
                    increment(0, true);
                else if (mods == 64)
                    decrement(0, true);
                else if (mods == 128)
                    toggleCategoryLock(0);
                return;
            case KeyEvent.VK_2:
                if (mods == 0)
                    increment(1, true);
                else if (mods == 64)
                    decrement(1, true);
                else if (mods == 128)
                    toggleCategoryLock(1);
                return;
            case KeyEvent.VK_3:
                if (mods == 0)
                    increment(2, true);
                else if (mods == 64)
                    decrement(2, true);
                else if (mods == 128)
                    toggleCategoryLock(2);
                return;
            case KeyEvent.VK_4:
                if (mods == 0)
                    increment(3, true);
                else if (mods == 64)
                    decrement(3, true);
                else if (mods == 128)
                    toggleCategoryLock(3);
                return;
            case KeyEvent.VK_5:
                if (mods == 0)
                    increment(4, true);
                else if (mods == 64)
                    decrement(4, true);
                else if (mods == 128)
                    toggleCategoryLock(4);
                return;
            case KeyEvent.VK_6:
                if (mods == 0)
                    increment(5, true);
                else if (mods == 64)
                    decrement(5, true);
                else if (mods == 128)
                    toggleCategoryLock(5);
                return;
            case KeyEvent.VK_ESCAPE:
                exit(false);
                return;
        }
    }
    public void toggleCategoryLock(int i) {
        softClick();
        TechCategory cat = player().tech().category(i);
        cat.toggleLock();
        repaint();
    }
    public void decrement(int i, boolean click) {
        if (player().tech().adjustTechAllocation(i, -1)) {
            if (click)
                softClick();
            repaint();
        }
        else if (click)
            misClick();
    }
    public void increment(int i, boolean click) {
        if (player().tech().adjustTechAllocation(i, 1)) {
            if (click)
                softClick();
            repaint();
        }
        else if (click)
            misClick();
    }
    private void dragTree(int x1, int y1) {
        int oldX = treeX;
        int oldY = treeY;

        if (visualTree == null)
            return;
        int maxX = max(0,visualTree.getWidth()-treeBox.width);
        int maxY = max(0,visualTree.getHeight()-treeBox.height);

        int newX = min(maxX, max(0,treeX-x1));
        int newY = min(maxY, max(0,treeY-y1));

        if ((oldX != newX) || (oldY != newY)) {
            treeX = newX;
            treeY = newY;
            repaint();
        }
    }
    private void paintTree(Graphics g) {
        g.setClip(treeBox);
        BufferedImage img = visualTree();
        int w = img.getWidth();
        int h = img.getHeight();
        g.drawImage(img, treeBox.x-treeX,treeBox.y-treeY,treeBox.x-treeX+w,treeBox.y-treeY+h, 0,0,w,h, null);
        g.setClip(null);
    }
    private void exit(boolean disableNextTurn) {
        resetData();
        buttonClick();
        RotPUI.instance().selectMainPanel(disableNextTurn);
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) {
        dragX = e.getX();
        dragY = e.getY();
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if (hoverBox == helpBox) {
            showHelp();
            return;
        }
        
        if (treeBox.contains(x,y)) {
            if (techSelections.keySet().contains(hoverBox)) {
                String techId = techSelections.get(hoverBox);
                player().tech().category(selectedCategory).currentTech(tech(techId));
                visualTree = null;
                repaint();
                return;
            }
        }

        if (hoverBox == techBox) {
            toggleOverflowSpending();
            return;
        }
        if (hoverBox == equalizeButton) {
            equalize();
            return;
        }
        for (int i=0;i<catBox.length;i++) {
            if (hoverBox == catBox[i]) {
                selectTechCategory(i);
                return;
            }
        }
        for (int i=0;i<labelBox.length;i++) {
            if (labelBox[i].contains(x,y)) {
                toggleCategoryLock(i);
                return;
           }
        }
        for (int i=0;i<leftArrow.length;i++) {
            if (leftArrow[i].contains(x,y)) {
                decrement(i,true);
                return;
            }
        }
        for (int i=0;i<rightArrow.length;i++) {
            if (rightArrow[i].contains(x,y)) {
                increment(i,true);
                return;
            }
        }
        for (int i=0;i<sliderBox.length;i++) {
            if (sliderBox[i].contains(x,y)) {
                float pct = (float)(x-sliderBox[i].x)/sliderBox[i].width;
                if (pct >= 0) {
                    // clicks near the edge of the box are typically trying
                    // to zero or max them out. Assume that.
                    if (pct < .05)
                        pct = 0;
                    else if (pct > .95)
                        pct = 1;
                    TechCategory cat = player().tech().category(i);
                    int oldAllocation = cat.allocation();
                    cat.allocationPct(pct);
                    int newAllocation = cat.allocation();
                    int delta = newAllocation - oldAllocation;
                    cat.allocation(oldAllocation);
                    if (player().tech().adjustTechAllocation(i, delta)) {
                        softClick();
                        repaint();
                    }
                    else
                        misClick();
                }
            }
        }
    }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) {
        if (hoverBox != null) {
            hoverBox = null;
            repaint();
        }
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if (treeBox.contains(x,y))
            dragTree(x-dragX, y-dragY);

        dragX = x;
        dragY = y;
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        dragX = dragY = 0;
        Shape prevHover = hoverBox;
        hoverBox = hoverShape(x,y);

        if (hoverBox != prevHover)
            repaint();
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int x = e.getX();
        int y = e.getY();
        int rot = e.getWheelRotation();
        for (int i=0;i<sliderBox.length;i++) {
            if (hoverBox == sliderBox[i]) {
                if (rot > 0)
                    decrement(i, false);
                else if (rot  < 0)
                    increment(i, false);
            }
        }
        if (catArea.contains(x,y)) {
            if (rot > 0)
                selectTechCategory(selectedCategory+1);
            else if (rot  < 0)
                selectTechCategory(selectedCategory-1);
        }
    }
    private Shape hoverShape(int x, int y) {
        if (treeBox.contains(x,y)) {
            for (RoundRectangle2D.Float box: techSelections.keySet()) {
                if (box.contains(x+treeX-treeBox.x,y+treeY-treeBox.y)) 
                    return box;
            }
        }
        if (equalizeButton.contains(x,y))
            return equalizeButton;
        if (helpBox.contains(x,y))
            return helpBox;
        else if (techBox.contains(x,y))
            return techBox;                    
        for (int i=0;i<catBox.length;i++) {
            if (catBox[i].contains(x,y))
                return catBox[i];
        }
        for (int i=0;i<sliderBox.length;i++) {
            if (sliderBox[i].contains(x,y))
                return sliderBox[i];
        }
        for (int i=0;i<labelBox.length;i++) {
            if (labelBox[i].contains(x,y))
                return labelBox[i];
        }
        for (int i=0;i<leftArrow.length;i++) {
            if (leftArrow[i].contains(x,y))
                return leftArrow[i];
        }
        for (int i=0;i<rightArrow.length;i++) {
            if (rightArrow[i].contains(x,y))
                return rightArrow[i];
        }
        return null;
    }
    class ExitTechButton extends rotp.ui.ExitButton {
        private static final long serialVersionUID = 1L;
        public ExitTechButton(int w, int h, int vMargin, int hMargin) {
            super(w, h, vMargin, hMargin);
        }
        @Override
        protected void clickAction(int numClicks) {
            exit(true);
        }
    }
}
