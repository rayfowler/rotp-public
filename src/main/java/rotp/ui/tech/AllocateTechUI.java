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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import rotp.model.tech.Tech;
import rotp.model.tech.TechCategory;
import rotp.model.tech.TechTree;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
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
    static final Color tierBackC = new Color(24,18,14);
    static final Color currentTechC = new Color(217,164,0);
    static final Color unknownTechC = new Color(145,102,72);
    static final Color knownTechC = new Color(75,99,51);
    static final Color techUnderscoreC = new Color(255,255,255,90);

    private LinearGradientPaint backGradient;
    private ExitTechButton exitButton;
    private final Rectangle equalizeButton = new Rectangle();
    private Shape hoverBox;
    private final Rectangle[] catBox = new Rectangle[TechTree.NUM_CATEGORIES];
    private final Polygon[] leftArrow = new Polygon[TechTree.NUM_CATEGORIES];
    private final Polygon[] rightArrow = new Polygon[TechTree.NUM_CATEGORIES];
    private final Rectangle[] labelBox = new Rectangle[TechTree.NUM_CATEGORIES];
    private final Rectangle[] sliderBox = new Rectangle[TechTree.NUM_CATEGORIES];
    private final Rectangle treeBox = new Rectangle();
    private final Rectangle catArea = new Rectangle();
    private BufferedImage visualTree;
    int treeX, treeY;
    int dragX, dragY;
    float totalPlanetaryResearch = 0;
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
        totalPlanetaryResearch = player().totalPlanetaryResearch();
        totalPlanetaryResearchSpending = player().totalPlanetaryResearchSpending();
        resetData();
    }
    @Override
    public String textureName()     { return TEXTURE_BROWN; }
    @Override
    public void drawTexture(Graphics g0) {
        // this UI will manually override
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintToImage(screenBuffer());
        g.drawImage(screenBuffer(), 0,0, this);
        exitButton.setBounds(scaled(980),scaled(695),scaled(235),s50);
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

        String title = text("TECH_RESEARCH");
        g.setFont(narrowFont(40));
        g.setColor(yellowTextC);
        g.drawString(title, s30, s60);

        int cats = TechTree.NUM_CATEGORIES;
        int gap = s5;
        int catH = s22;
        int topM = s70;
        int bottomM = s25;
        int leftM = s30;
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
        // draw right-side panel
        int subPanelX = scaled(980);
        int subPanelW = scaled(233);
        int subPanelY = s100;
        int subPanelH = scaled(535);
        String subtitle = text("TECH_RESEARCH_POINTS");
        g.setFont(narrowFont(28));
        int subSW = g.getFontMetrics().stringWidth(subtitle);
        g.setColor(yellowTextC);

        int x4 = subPanelX+((subPanelW-subSW)/2);
        g.drawString(subtitle, x4, subPanelY-s8);

        g.setColor(subpanelBackC);
        g.fillRect(subPanelX, subPanelY, subPanelW, subPanelH);

        g.setColor(darkBrownC);
        int topSubPaneH = scaled(123);
        g.fillRect(subPanelX+s10, subPanelY+s10, subPanelW-s20, topSubPaneH);

        int botSubPaneY = subPanelY+topSubPaneH+s18;
        int botSubPaneH = subPanelY+subPanelH-s10-botSubPaneY;
        g.fillRect(subPanelX+s10, botSubPaneY, subPanelW-s20, botSubPaneH);

        // fill in top right panel
        String title2 = text("TECH_EMPIRE_SPENDING");
        g.setFont(narrowFont(20));
        drawShadowedString(g, title2, 1, subPanelX+s20, subPanelY+s30, Color.black, Color.white);

        g.setFont(plainFont(16));
        g.setColor(Color.black);
        int y5 = subPanelY+s35;
        List<String> descLines = wrappedLines(g, text("TECH_EMPIRE_SPENDING_DESC"), scaled(200));
        if (descLines.size() == 1)
            y5 += s10;
        for (String descLine: descLines) {
            y5 += s15;
            g.drawString(descLine, subPanelX+s20, y5);
        }

        TechTree tree = player().tech();
        int totalSpending = (int) totalPlanetaryResearchSpending;
        int totalResearch = 0;       
        for (int i=0;i<cats;i++)
             totalResearch += tree.category(i).currentResearch(totalPlanetaryResearch);

        int y6 = subPanelY+s93;
        g.setFont(plainFont(25));
        String detailLine1 = text("TECH_EMPIRE_DETAIL_1", str(totalSpending), str(totalResearch));
        int detSW = g.getFontMetrics().stringWidth(detailLine1);
        int x6 = subPanelX+((subPanelW-detSW)/2);
        g.drawString(detailLine1, x6, y6);
        String detailLine2 = text("TECH_EMPIRE_DETAIL_2", str(totalSpending), str(totalResearch));
        detSW = g.getFontMetrics().stringWidth(detailLine2);
        y6 += s28;
        x6 = subPanelX+((subPanelW-detSW)/2);
        g.drawString(detailLine2, x6, y6);

        // draw tech spending area
        String allocateTitle = text("TECH_ALLOCATE_POINTS");
        g.setFont(narrowFont(20));
        drawShadowedString(g, allocateTitle, 1, subPanelX+s20, botSubPaneY+s20, Color.black, Color.white);

        // draw equalize spending hihit
        g.setFont(plainFont(16));
        g.setColor(Color.black);
        y6 = botSubPaneY+s25;
        List<String> eqLines = wrappedLines(g, text("TECH_EMPIRE_BALANCED_DESC"), scaled(200));
        for (String line: eqLines) {
            y6 += s15;
            g.drawString(line, subPanelX+s20, y6);
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
        Stroke prev = g.getStroke();
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
        g.drawString(name, x+s30, y+s15);
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

        String rpText = text("TECH_TOTAL_RP",(int)cat.currentResearch(totalPlanetaryResearch));
        g.setColor(Color.black);
        g.setFont(plainFont(18));
        int rpSW = g.getFontMetrics().stringWidth(rpText);
        int x8 = x+w-rpSW-s10;
        int y8 = y+s32;
        g.drawString(rpText, x8, y8);
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

        float chance = cat.upcomingDiscoveryChance(totalPlanetaryResearch);
        int x0 = x+scaled(180);
        int r = s19;
        if (chance <= 1) {
            g.setColor(Color.black);
            g.fillOval(x0-s10, y0-s16, r, r);
            int lvl = (int)(chance*r);
            g.setColor(blueBucketC);
            g.setClip(x0-s10,y0+s4-lvl,r+r,r);
            g.fillOval(x0-s10, y0-s16, r, r);
            g.setClip(null);
        }
        else {
            int pct = (int) (100* (chance -1));
            String strPct = text("TECH_DISCOVERY_PCT",pct);
            g.setFont(narrowFont(18));
            g.setColor(Color.black);
            int swPct = g.getFontMetrics().stringWidth(strPct);
            g.drawString(strPct, x0-(swPct/2), y0);
        }

        int fontSize = 18;
        g.setColor(Color.black);
        g.setFont(narrowFont(fontSize));
        String researching = text("TECH_RESEARCHING");
        int sw1 = g.getFontMetrics().stringWidth(researching);
        int x1 =x+scaled(200);
        g.drawString(researching, x1, y0);

        g.setFont(plainFont(fontSize));
        String techName = tech == null ? text("TECH_NONE_RESEARCHED") : tech.name();
        g.drawString(techName, x1+sw1+s5, y0);

        g.setFont(narrowFont(fontSize));
        String detail1Key = cat.techDescription1(true);
        int sw2 = g.getFontMetrics().stringWidth(detail1Key);
        int x2 = x+scaled(500);
        g.drawString(detail1Key, x2, y0);

        g.setFont(plainFont(fontSize));
        String detail1Value = cat.techDescription1(false);
        int x2b = x2+sw2+s10;
        g.drawString(detail1Value, x2b, y0);

        String detail2Key = cat.techDescription2(true);
        if (!detail2Key.isEmpty()) {
            g.setFont(narrowFont(fontSize));
            int sw3 = g.getFontMetrics().stringWidth(detail2Key);
            int x3 = x+scaled(720);
            g.drawString(detail2Key, x3, y0);
            String detail2Value = cat.techDescription2(false);
            int x3b = x3+sw3+s10;
            g.setFont(plainFont(fontSize));
            g.drawString(detail2Value, x3b, y0);
        }
    }
    private BufferedImage visualTree() {
        if (visualTree == null) 
            initVisualTree();        
        return visualTree;
    }
    private void initVisualTree() {
        TechCategory cat = player().tech().category(selectedCategory);
        String currentT = cat.currentTech();
        int maxQ = currentT == null ? 0 : cat.maxResearchableQuintile();
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
            if (i > 0)
                drawTierArrow(g,x0,tierGap,h);
            x0 += tierGap;
            int minLevel=(5*i)+1;
            int maxLevel=minLevel+4;
            if (i==maxQ)
                drawUnknownTechTier(g,cat, i, x0,tierW,h);
            else
                drawTechTier(g,techs,knownT,currentT,minLevel,maxLevel,x0,tierW,h);
            x0 += tierW;
        }
        g.dispose();
    }
    private void drawUnknownTechTier(Graphics g, TechCategory cat, int tier, int x, int w, int h) {
        String title = text("TECH_NEXT_TIER_TITLE");
        String desc;
        
        if (tier == 0)
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
        g.drawString(title,x0,y0);

        y0 += s10;
        List<String> descLines = this.scaledPlainWrappedLines(g, desc, w*2/3, 5, 20, 16);
        for (String line: descLines) {
            int lineSW = g.getFontMetrics().stringWidth(line);
            x0 = x+((w-lineSW)/2);
            y0 += s20;
            g.drawString(line,x0,y0);
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
    private void drawTechTier(Graphics2D g, Tech[] allT, List<String> knownT, String currentT, int minLevel, int maxLevel, int x, int w, int h) {
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
            drawTechBox(g, tech, knownT, currentT, boxX, boxY, boxW, boxH);
            boxY += boxH;
        }
    }
    private void drawTechBox(Graphics2D g, Tech tech, List<String> knownT, String currentT, int x, int y, int w, int h) {
        TechCategory cat =  player().tech().category(selectedCategory);
        Color backC = unknownTechC;
        Color textC = Color.white;

        boolean known = knownT.contains(tech.id);

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
        g.setPaint(backGradient1);
        g.fillRoundRect(x, y, w, realH, cnr, cnr);

        g.setColor(textC);
        String name = tech.name();

        int fontSize = scaledFont(g, name, w-s80, 20, 16);
        int y0 = y+hdr1;
        if (known)
            g.drawString(name, x+s7, y0);
        else
            drawShadowedString(g, name, 2, x+s7, y0, SystemPanel.buttonShadowC, textC);

        if (!known) {
            String costLbl;
            float costRP = cat.costForTech(tech);
            if ((cat.currentTech() != null) && cat.currentTech().equals(tech.id))
                costRP -= cat.totalBC();
            int cost = (int) max(0,costRP);
            if (cost > 0)
                costLbl = text("TECH_TOTAL_RP",cost);
            else {
                float chance = cat.upcomingDiscoveryChance();
                int pct = (int) (100* (chance -1));
                costLbl = text("TECH_DISCOVERY_PCT",pct);
            }
            g.setFont(narrowFont(fontSize));
            int costSW = g.getFontMetrics().stringWidth(costLbl);
            g.setColor(Color.black);
            g.drawString(costLbl, x+w-s10-costSW, y0);
        }

        g.setColor(techUnderscoreC);
        g.fillRect(x,y0+s7,w,s2);

        y0 = y0 + hdr2;
        g.setColor(Color.black);
        descLines = this.scaledPlainWrappedLines(g, tech.detail(), w-s14, 4, 16, 13);
        for (String desc: descLines) {
            g.drawString(desc, x+s7, y0);
            y0+= lineH;
        }
    }
    private void selectTechCategory(int i) {
        if ((i < 0) || (i >= catBox.length))
            return;
        if (selectedCategory == i)
            return;
        selectedCategory = i;
        resetData();
        repaint();
    }
    private void resetData() {
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
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        int mods = e.getModifiersEx();
        switch (k) {
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
                dragTree(0,s10);
                return;
            case KeyEvent.VK_DOWN:
                dragTree(0,-s10);
                return;
            case KeyEvent.VK_TAB:
                return;
            case KeyEvent.VK_1:
                if (mods == 0)
                    increment(0, true);
                else if (mods == 1)
                    decrement(0, true);
                else if (mods == 2)
                    toggleCategoryLock(0);
                return;
            case KeyEvent.VK_2:
                if (mods == 0)
                    increment(1, true);
                else if (mods == 1)
                    decrement(1, true);
                else if (mods == 2)
                    toggleCategoryLock(1);
                return;
            case KeyEvent.VK_3:
                if (mods == 0)
                    increment(2, true);
                else if (mods == 1)
                    decrement(2, true);
                else if (mods == 2)
                    toggleCategoryLock(2);
                return;
            case KeyEvent.VK_4:
                if (mods == 0)
                    increment(3, true);
                else if (mods == 1)
                    decrement(3, true);
                else if (mods == 2)
                    toggleCategoryLock(3);
                return;
            case KeyEvent.VK_5:
                if (mods == 0)
                    increment(4, true);
                else if (mods == 1)
                    decrement(4, true);
                else if (mods == 2)
                    toggleCategoryLock(4);
                return;
            case KeyEvent.VK_6:
                if (mods == 0)
                    increment(5, true);
                else if (mods == 1)
                    decrement(5, true);
                else if (mods == 2)
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
        if (equalizeButton.contains(x,y))
            return equalizeButton;
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
        public ExitTechButton(int w, int h, int vMargin, int hMargin) {
            super(w, h, vMargin, hMargin);
        }
        @Override
        protected void clickAction(int numClicks) {
            exit(true);
        }
    }
}
