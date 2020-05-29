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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.tech.Tech;
import rotp.model.tech.TechCategory;
import rotp.model.tech.TechTree;
import rotp.ui.BasePanel;
import rotp.ui.UserPreferences;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;

public final class RacesIntelligenceUI extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    static Color sliderC = new Color(34,140,142);
    static Color sliderButtonC = new Color(153,0,11);
    static Color sliderButtonHiC = new Color(199,199,11);
    private static final Color selectedC = new Color(178,124,87);
    private static final Color unselectedC = new Color(112,85,68);
    static final Color sliderBoxBlue = new Color(34,140,142);

    private final RacesUI parent;
    private final ManageSpiesUI manageSpiesPane;
    private float internalSecurityCost = 0;
    private float externalSpyingCost = 0;

    private final Polygon spyMaxIncr = new Polygon();
    private final Polygon spyMaxDecr = new Polygon();
    private final Polygon buttonIncr = new Polygon();
    private final Polygon buttonDecr = new Polygon();
    private final Polygon missionIncr = new Polygon();
    private final Polygon missionDecr = new Polygon();
    Rectangle buttonSlider = new Rectangle();
    Shape hoverShape;
    int dragY;
    int[] ptX = new int[3];
    int[] ptY = new int[3];
    
    int[] techY = new int[6];
    int[] techYMax = new int[6];
    Rectangle[] techBoxes = new Rectangle[6];
    Rectangle[] techScrollers = new Rectangle[6];
    
    private final Rectangle manageSpiesBox = new Rectangle();
    private final Rectangle spyMissionBox = new Rectangle();
    private final Rectangle maxSpyBox = new Rectangle();
    
    private final HashMap<Integer, List<String>> knownTechs = new HashMap<>();
    private final HashMap<Integer, List<String>> unknownTechs = new HashMap<>();
    private final HashMap<String, List<Empire>> techOwners = new HashMap<>();

    private LinearGradientPaint backGradient;
    public RacesIntelligenceUI(RacesUI p) {
        parent = p;
        manageSpiesPane = new ManageSpiesUI(p);
        initModel();
    }
    public void init() {
        setValues();
    }
    @Override
    public void drawTexture(Graphics g)      { }
    @Override
    public String textureName()     { return TEXTURE_BROWN; }
    public void changedEmpire()     { setValues(); }
    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        int w = getWidth();
        int h = getHeight();

        if (backGradient == null) {
            Point2D start = new Point2D.Float(0, getHeight() / 2);
            Point2D end = new Point2D.Float(0, getHeight());
            float[] dist = {0.0f, 1.0f};
            Color[] colors = {RacesUI.darkerBrown, RacesUI.gradientBottom};
            backGradient = new LinearGradientPaint(start, end, dist, colors);
        }
        g.setPaint(backGradient);
        g.fillRect(0,h/2,w, h/2);
        if (parent.selectedEmpire().isPlayer()) 
            paintPlayerData(g);
        else
            paintAIData(g);
    }
    private void initModel() {
        for (int i=0;i<techBoxes.length;i++) 
            techBoxes[i] = new Rectangle();
        for (int i=0;i<techScrollers.length;i++) 
            techScrollers[i] = new Rectangle();
        
        setBackground(RacesUI.darkerBrown);
        setBorder(newEmptyBorder(5,5,5,5));
        addMouseMotionListener(this);
        addMouseListener(this);
        addMouseWheelListener(this);        
    }
    private void paintPlayerData(Graphics2D g) {
        // ai data only buttons - clear them
        missionDecr.reset();
        missionIncr.reset();
        spyMissionBox.setBounds(0,0,0,0);
        
        Empire emp = parent.selectedEmpire();
        int w = getWidth();
        int h = getHeight();

        int s200 = scaled(200);
        int s210 = scaled(210);
        int s260 = scaled(260);
        int s310 = scaled(310);
        int s370 = scaled(370);
        int s680 = scaled(680);
        drawRaceIconBase(g, emp, s55, s25, s210, s210);
        drawPlayerBaseInfo(g, emp, s260, s80, s370, scaled(130));
        drawUnknownTechnologyLists(g, emp, s20, s310, w-s40, h-s310-s10);
        drawPlayerIntelligenceBureau(g, emp, s680, s30, w-s680-s30, s200);
        drawTechnologyTitle(g, emp, s80, s260, w-s80-s90, s40);
        if (UserPreferences.textures()) 
            drawTexture(g,0,0,w,h);
        drawRaceIcon(g, emp, s60, s30, s200, s200);
        drawCounterIntelTitle(g, emp, s260, s30, s370, s50);
    }
    private void paintAIData(Graphics2D g) {
        Empire emp = parent.selectedEmpire();
        int w = getWidth();
        int h = getHeight();

        int s200 = scaled(200);
        int s210 = scaled(210);
        int s260 = scaled(260);
        int s310 = scaled(310);
        int s370 = scaled(370);
        int s680 = scaled(680);
        drawRaceIconBase(g, emp, s55, s25, s210, s210);
        drawAIBaseInfo(g, emp, s260, s80, s370, scaled(130));
        drawTechnologyLists(g, emp, s20, s310, w-s40, h-s310-s10);
        drawAISpyOrders(g, emp, s680, s30, w-s680-s30, s200);
        drawTechnologyTitle(g, emp, s80, s260, w-s80-s90, s40);
        if (UserPreferences.textures()) 
            drawTexture(g,0,0,w,h);
        drawRaceIcon(g, emp, s60, s30, s200, s200);
        drawIntelTitle(g, emp, s260, s30, s370, s50);
    }
    private void drawRaceIconBase(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        Shape rect = new RoundRectangle2D.Float(x,y,w,h,w/8, h/8);
        g.fill(rect);  
    }
    private void drawRaceIcon(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(Color.black);
        Shape rect = new RoundRectangle2D.Float(x,y,w,h,w/8, h/8);
        g.fill(rect);
        
        BufferedImage backImg = parent.raceIconBackImg();
        g.drawImage(backImg, x,y, null);
        
        int x1 = x + w/10;
        int w1 = w * 8/10;
        int y1 = y + h/10;
        int h1 = h * 8/10;

        Image img = emp.isPlayer() ? emp.race().flagPact() : player().viewForEmpire(emp).flag();
        int imgH = img.getHeight(null);
        int imgW = img.getWidth(null);
        g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, imgW, imgH, null);
    }
    private void drawIntelTitle(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(SystemPanel.orangeText);
        g.setFont(narrowFont(32));
        g.drawString(text("RACES_INTEL_TITLE", emp.raceName()), x+s10, y+h-s15);
    }
    private void drawCounterIntelTitle(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(SystemPanel.orangeText);
        g.setFont(narrowFont(32));
        g.drawString(text("RACES_COUNTER_INTEL_TITLE"), x+s10, y+h-s15);
    }
    private void drawPlayerBaseInfo(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);

        int lineH = s30;
        int y1 = y+lineH-s5;
        int y2 = y1+lineH;
        int y3 = y2+lineH;
        int x0 = x+s20;
        g.setFont(narrowFont(22));
        Color textC = SystemPanel.whiteText;
        drawShadowedString(g, text("RACES_INTEL_SECURITY_BONUS"), 1, x0, y1, SystemPanel.blackText, textC);
        drawShadowedString(g, text("RACES_INTEL_TOTAL_SPENDING"), 1, x0, y2, SystemPanel.blackText, textC);
        drawShadowedString(g, text("RACES_INTEL_SECURITY_TAX"), 1, x0, y3, SystemPanel.blackText, textC);

        g.setFont(narrowFont(15));
        g.setColor(SystemPanel.blackText);
        String desc = text("RACES_INTEL_TAX_DESC");
        List<String> lines = wrappedLines(g, desc, w-s50);
        int y4 = y3+s20;
        if (lines.size() == 1)
            y4 += s10;
        for (String line: lines) {
            g.drawString(line, x0+20, y4);
            y4 += s16;
        }

        g.setFont(narrowFont(20));
        g.setColor(SystemPanel.blackText);

        int amt = (int) (100*player().totalInternalSecurityPct());
        String s = amt == 0 ? text("RACES_INTEL_SECURITY_BONUS_NONE") : text("RACES_INTEL_SECURITY_BONUS_AMT", str(amt));
        int sw = g.getFontMetrics().stringWidth(s);
        g.drawString(s, x+w-s20-sw, y1);

        amt = (int) internalSecurityCost;
        s = text("RACES_INTEL_SPENDING_ANNUAL", str(amt));
        sw = g.getFontMetrics().stringWidth(s);
        g.drawString(s, x+w-s20-sw, y2);
        
         // draw string on right for pct 
        String cost = text("RACES_INTEL_PERCENT_AMT",(int)(player().internalSecurityCostPct()*100));
        sw = g.getFontMetrics().stringWidth(cost);
        g.drawString(cost, x+w-s20-sw, y3);
        // need maxwidth so slider doesn't move as cost pct changes
        String maxWidthStr = text("RACES_INTEL_PERCENT_AMT",10);
        sw = g.getFontMetrics().stringWidth(maxWidthStr);
       
        int sliderW = s90;
        int sliderH = s16;
        drawSecuritySliderBar(g,x+w-s50-sw-sliderW, y3-sliderH+s3, sliderW, sliderH);
    }
    private void drawAIBaseInfo(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        EmpireView view = player().viewForEmpire(emp);
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);

        int lineH = s30;
        int y1 = y+lineH-s5;
        int y2 = y1+lineH;
        int y3 = y2+lineH;
        int x0 = x+s20;
        g.setFont(narrowFont(22));
        Color textC = SystemPanel.whiteText;
        drawShadowedString(g, text("RACES_INTEL_REPORT_AGE"), 1, x0, y1, SystemPanel.blackText, textC);
        drawShadowedString(g, text("RACES_INTEL_SPY_NETWORK"), 1, x0, y2, SystemPanel.blackText, textC);
        drawShadowedString(g, text("RACES_INTEL_SPENDING"), 1, x0, y3, SystemPanel.blackText, textC);

        g.setFont(narrowFont(15));
        g.setColor(SystemPanel.blackText);
        String desc;
        
        if (view.embassy().unity())
            desc = text("RACES_INTEL_SPENDING_DESC_UNITY");
        else if (view.inEconomicRange())
            desc = text("RACES_INTEL_SPENDING_DESC");
        else
            desc = text("RACES_INTEL_SPENDING_RANGE");
        List<String> lines = wrappedLines(g, desc, w-s50);

        int y4 = y3+s20;
        if (lines.size() == 1)
            y4 += s10;
        for (String line: lines) {
            g.drawString(line, x0+20, y4);
            y4 += s16;
        }

        g.setFont(narrowFont(20));
        g.setColor(SystemPanel.blackText);

        int age = view.spies().reportAge();

        String s;
        if (age < 0)
            s = text("RACES_INTEL_NO_DATA");
        else if (age == 0)
            s = text("RACES_INTEL_CURRENT");
        else
            s = text("RACES_INTEL_YEARS", str(age));
        
        int sw = g.getFontMetrics().stringWidth(s);
        g.drawString(s, x+w-s20-sw, y1);

        // draw arrows to incr/decr desired spy networks
        int mgn = s20;
        if (!view.embassy().unity()) {
            mgn = s40;
            int sz = s10;
            int x1 =x+w-s20;
            // draw incr button
            ptX[0]=x1-s3-sz; ptX[1]=ptX[0]+sz; ptX[2]=ptX[0]+sz/2;
            ptY[0]=y2-s9;  ptY[1]=ptY[0];    ptY[2]=y2-s16;
            spyMaxIncr.reset();
            for (int i=0;i<ptX.length;i++) 
                spyMaxIncr.addPoint(ptX[i], ptY[i]);
            Color buttonC = hoverShape == spyMaxIncr ? SystemPanel.yellowText :  Color.black;
            drawShadedPolygon(g, ptX, ptY, buttonC, s1, -s1);

            // draw lower decr button
            ptX[0]=x1-s3-sz; ptX[1]=ptX[0]+sz; ptX[2]=ptX[0]+sz/2;
            ptY[0]=y2-s6;    ptY[1]=ptY[0];    ptY[2]=y2+s1;
            spyMaxDecr.reset();
            for (int i=0;i<ptX.length;i++) 
                spyMaxDecr.addPoint(ptX[i], ptY[i]);
            buttonC = hoverShape == spyMaxDecr ? SystemPanel.yellowText :  Color.black;
            drawShadedPolygon(g, ptX, ptY, buttonC, s1, -s1);

            maxSpyBox.setBounds(x1-s5-sz, y2-s18, sz+6, s20);
            if ((hoverShape == maxSpyBox) || (hoverShape == spyMaxDecr) || (hoverShape == spyMaxIncr)) {
                Stroke prev = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(SystemPanel.yellowText);
                g.draw(maxSpyBox);
                g.setStroke(prev);
            }       
        }
        
        // draw num & max of spy networks
        int num = view.spies().numActiveSpies();
        int max = view.spies().maxSpies();
        s  = num == max ? str(num) : text("RACES_INTEL_SPIES", str(num), str(max));
        sw = g.getFontMetrics().stringWidth(s);
        g.drawString(s, x+w-mgn-sw, y2);
       
         // draw string on right for pct 
        g.setColor(SystemPanel.blackText);
        String newSpies = view.spies().newSpiesExpected();
        sw = g.getFontMetrics().stringWidth(newSpies);
        g.drawString(newSpies, x+w-s20-sw, y3);
        // need maxwidth so slider doesn't move as cost pct changes
        String maxWidthStr = text("RACES_INTEL_COMPLETION_YEARS",99);
        sw = g.getFontMetrics().stringWidth(maxWidthStr);
       
        // draw security spending bar
        if (!view.embassy().unity()) {
            int sliderW = s90;
            int sliderH = s16;
            drawSecuritySliderBar(g,x+w-s50-sw-sliderW, y3-sliderH+s3, sliderW, sliderH);
        }
    }
    private void drawTechnologyTitle(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(SystemPanel.orangeText);
        g.setFont(narrowFont(30));
        String title = emp.isPlayer() ? text("RACES_INTEL_UNKNOWN_TECHNOLOGY") : text("RACES_INTEL_KNOWN_TECHNOLOGY", emp.raceName());
        int sw = g.getFontMetrics().stringWidth(title);
        int y1 = y + h - s5;
        int x1 = x+s20;
        g.drawString(title, x1, y1);
        
        // don't draw the spy informational message for your New Republic allies
        if (emp.isAI()) {
            EmpireView v = player().viewForEmpire(emp);
            if (v.embassy().unity())
                return;
        }
        
        int x2 = x1+sw+s40;
        int w2 = x+w-x2-s40;
        int y2 = y1 - s10;
        String desc = emp.isPlayer() ? text("RACES_INTEL_UNKNOWN_TECH_DESC") : text("RACES_INTEL_KNOWN_TECH_DESC", emp.raceName());
        g.setColor(SystemPanel.whiteText);
        int fontSize = 15;
        g.setFont(narrowFont(fontSize));
        List<String> lines = this.wrappedLines(g, desc, w2);
        while ((lines.size() > 2) && (fontSize > 10)) {
            fontSize--;
            g.setFont(narrowFont(fontSize));
            lines = this.wrappedLines(g, desc, w2);
        }
        if (lines.size() == 1)
            y2 += s8;
        for (String line: lines) {
            g.drawString(line, x2, y2);
            y2 += s16;
        }
    }
    private void drawUnknownTechnologyLists(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);
        int w1 = (w-s20)/3;
        int h1 = h/2;
        
        int x1 = x+s10;
        int x2 = x1+w1;
        int x3 = x2+w1;
        int y1 = y;
        int y2 = y+h1;
        
        Empire pl = player();
        TechTree tree = emp == pl ? pl.tech() : pl.viewForEmpire(emp).spies().tech();
        drawJnknownTechCategory(g, emp, tree.computer(),    0, x1, y1, w1, h1);
        drawJnknownTechCategory(g, emp, tree.construction(),1, x2, y1, w1, h1);
        drawJnknownTechCategory(g, emp, tree.forceField(),  2, x3, y1, w1, h1);
        drawJnknownTechCategory(g, emp, tree.planetology(), 3, x1, y2, w1, h1);
        drawJnknownTechCategory(g, emp, tree.propulsion(),  4, x2, y2, w1, h1);
        drawJnknownTechCategory(g, emp, tree.weapon(),      5, x3, y2, w1, h1);   
    }
    private void drawTechnologyLists(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);
        int w1 = (w-s20)/3;
        int h1 = h/2;
        
        int x1 = x+s10;
        int x2 = x1+w1;
        int x3 = x2+w1;
        int y1 = y;
        int y2 = y+h1;
        
        Empire pl = player();
        TechTree tree = emp == pl ? pl.tech() : pl.viewForEmpire(emp).spies().tech();
        drawTechCategory(g, emp, tree.computer(),    0, x1, y1, w1, h1);
        drawTechCategory(g, emp, tree.construction(),1, x2, y1, w1, h1);
        drawTechCategory(g, emp, tree.forceField(),  2, x3, y1, w1, h1);
        drawTechCategory(g, emp, tree.planetology(), 3, x1, y2, w1, h1);
        drawTechCategory(g, emp, tree.propulsion(),  4, x2, y2, w1, h1);
        drawTechCategory(g, emp, tree.weapon(),      5, x3, y2, w1, h1);   
    }
    private void drawTechCategory(Graphics2D g, Empire emp, TechCategory cat, int num, int x, int y, int w, int h) {
        g.setColor(SystemPanel.whiteText);
        g.setFont(narrowFont(24));
        String title = text(cat.id());
        int sw = g.getFontMetrics().stringWidth(title);
        int x0 = x+((w-sw)/2);
        int y0 = y+s25;
        drawShadowedString(g, title, 1, x0, y0, SystemPanel.blackText, SystemPanel.whiteText);
    
        int y1 = y0+s10;
        int x1 = x+s10;
        int w1 = w-s20;
        int listH = y+h-y1-s10;
        
        g.setColor(RacesUI.brown);
        g.fillRect(x1,y1,w1,listH);
        
        techBoxes[num].setBounds(x1,y1,w1,listH);
        if (techBoxes[num] == hoverShape) {
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.setColor(Color.yellow);
            g.draw(techBoxes[num]);
            g.setStroke(prev);
        }
        List<String> aiUnknown = unknownTechs.get(cat.index());
        List<String> aiKnown = knownTechs.get(cat.index());
        
        int rowH = s16;
        int x2 = x1+s10;
        int y2 = y1-techY[num];
        g.setFont(narrowFont(15));
        g.setClip(x1+s1,y1+s1,w1-s1,listH-s2);
        g.setColor(SystemPanel.orangeText);
        int rows = 0;
        for (String id: aiUnknown) {
            Tech t = tech(id);
            if ((t.level() > 0) && !t.free) {
                y2 += rowH;
                rows++;
                g.drawString(tech(id).name(), x2, y2);
            }
        }
        g.setColor(SystemPanel.blackText);
        for (String id: aiKnown) {
            Tech t = tech(id);
            if ((t.level() > 0) && !t.free) {
                y2 += rowH;
                rows++;
                g.drawString(tech(id).name(), x2, y2);
            }
        }
        techYMax[num] = max(0, s21+(rowH*rows) - listH);
        if (techYMax[num] == 0) 
            techScrollers[num].setBounds(0,0,0,0);
        else {
            g.setColor(RacesUI.scrollBarC);
            int scrollW = s12;
            int scrollH = (int) ((float)listH*listH/(listH+techYMax[num]));
            int scrollX = x1+w1-scrollW-s2;
            int scrollY =(int) (y1+ (float)listH*techY[num]/(techYMax[num]+listH));
            g.fillRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
            techScrollers[num].setBounds(scrollX, scrollY, scrollW, scrollH);
            if (hoverShape == techScrollers[num]) {
                Stroke prev = g.getStroke();
                g.setColor(Color.yellow);
                g.setStroke(stroke2);
                g.drawRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
                g.setStroke(prev);
            }
        }
        g.setClip(null);
    }
    private void drawJnknownTechCategory(Graphics2D g, Empire emp, TechCategory cat, int num, int x, int y, int w, int h) {
        g.setColor(SystemPanel.whiteText);
        g.setFont(narrowFont(24));
        String title = text(cat.id());
        int sw = g.getFontMetrics().stringWidth(title);
        int x0 = x+((w-sw)/2);
        int y0 = y+s25;
        drawShadowedString(g, title, 1, x0, y0, SystemPanel.blackText, SystemPanel.whiteText);
    
        int y1 = y0+s10;
        int x1 = x+s10;
        int w1 = w-s20;
        int listH = y+h-y1-s10;
        
        g.setColor(RacesUI.brown);
        g.fillRect(x1,y1,w1,listH);
        
        techBoxes[num].setBounds(x1,y1,w1,listH);
        if (techBoxes[num] == hoverShape) {
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.setColor(Color.yellow);
            g.draw(techBoxes[num]);
            g.setStroke(prev);
        }
        
        List<String> aiUnknown = unknownTechs.get(cat.index());
        int rowH = s16;
        int x2 = x1+s10;
        int y2 = y1-techY[num];
        g.setFont(narrowFont(15));
        g.setClip(x1+s1,y1+s1,w1-s1,listH-s2);
        g.setColor(SystemPanel.orangeText);
        int rows = 0;
        for (String id: aiUnknown) {
            Tech t = tech(id);
            if ((t.level() > 0) && !t.free) {
                y2 += rowH;
                rows++;
                String s = tech(id).name();
                g.drawString(s, x2, y2);
                int sw1 = g.getFontMetrics().stringWidth(s)+s5;
                List<Empire> emps = techOwners.get(id);
                int x3 = x2 + sw1;
                g.setColor(SystemPanel.blackText);
                for (Empire emp1: emps) {
                    s = emp1.raceName();
                    sw1 = g.getFontMetrics().stringWidth(s)+s5;
                    g.drawString(s, x3, y2);
                    x3 += sw1;
                }
                g.setColor(SystemPanel.orangeText);
            }
        }
        techYMax[num] = max(0, s21+(rowH*rows) - listH);
        if (techYMax[num] == 0) 
            techScrollers[num].setBounds(0,0,0,0);
        else {
            g.setColor(RacesUI.scrollBarC);
            int scrollW = s12;
            int scrollH = (int) ((float)listH*listH/(listH+techYMax[num]));
            int scrollX = x1+w1-scrollW-s2;
            int scrollY =(int) (y1+ (float)listH*techY[num]/(techYMax[num]+listH));
            g.fillRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
            techScrollers[num].setBounds(scrollX, scrollY, scrollW, scrollH);
            if (hoverShape == techScrollers[num]) {
                Stroke prev = g.getStroke();
                g.setColor(Color.yellow);
                g.setStroke(stroke2);
                g.drawRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
                g.setStroke(prev);
            }
        }
        g.setClip(null);
    }
    private void drawPlayerIntelligenceBureau(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);
        
        g.setColor(SystemPanel.whiteText);
        g.setFont(narrowFont(24));
        String title = text("RACES_INTEL_BUREAU_TITLE");
        int sw = g.getFontMetrics().stringWidth(title);
        int x0 = x+((w-sw)/2);
        int y0 = y+s25;
        drawShadowedString(g, title, 1, x0, y0, SystemPanel.blackText, SystemPanel.whiteText);
        
        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(15));
        String desc = text("RACES_INTEL_BUREAU_DESC");
        List<String> lines = wrappedLines(g, desc, w-s30);
        int y1 = y0+s5;
        for (String line: lines) {
            y1 += s16;
            g.drawString(line, x+s20, y1);
        }
        
        g.setFont(narrowFont(18));
        String spies = text("RACES_INTEL_BUREAU_SPIES");
        int y2 = y0+s80;
        g.drawString(spies, x+s20, y2);
        
        g.setFont(narrowFont(16));
        String str2 = str(player().totalActiveSpies());
        int sw2 = g.getFontMetrics().stringWidth(str2);
        g.drawString(str2, x+w-s20-sw2, y2);
        
        y2 += s25;
        g.setFont(narrowFont(18));
        String spending = text("RACES_INTEL_BUREAU_SPENDING");
        g.drawString(spending, x+s20, y2);
        int amt = (int) externalSpyingCost;
        
        g.setFont(narrowFont(16));
        str2 = amt == 0 ? text("RACES_INTEL_SECURITY_BONUS_NONE") : text("RACES_INTEL_SPENDING_ANNUAL", str(amt));
        sw2 = g.getFontMetrics().stringWidth(str2);
        g.drawString(str2, x+w-s20-sw2, y2);
        
        drawManageSpiesButton(g, x+s20,y+h-s45,w-s40,s25);
    }
    private void drawAISpyOrders(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        EmpireView view = player().viewForEmpire(emp);
        
        // no spy orders for new republic allies
        if (view.embassy().unity())
            return;
        
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);

        g.setColor(SystemPanel.whiteText);
        g.setFont(narrowFont(24));
        String title = text("RACES_INTEL_SPY_ORDERS");
        int sw = g.getFontMetrics().stringWidth(title);
        int x0 = x+((w-sw)/2);
        int y0 = y+s25;
        drawShadowedString(g, title, 1, x0, y0, SystemPanel.blackText, SystemPanel.whiteText);
       
        int y1 = y0+s20;
        int sliderH = s20;
        drawSpiesMissionButton(g, emp, x+s40,y1,w-s80,sliderH);
        
        int y2 = y1+sliderH+s10;

        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(15));
        String desc = text("RACES_INTEL_SPY_ORDERS_DESC", emp.raceName());
        List<String> lines = wrappedLines(g, desc, w-s30);
        for (String line: lines) {
            y2 += s16;
            g.drawString(line, x+s15, y2);
        }
    }
    private void drawSecuritySliderBar(Graphics2D g, int x, int y, int w, int h) {
        // draw spending bar
        int h1 = h; // height of bar
        int w1 = w; // width of bar
        
        EmpireView view = player().viewForEmpire(parent.selectedEmpire());
        float pct = view == null ? player().internalSecurityPct() : view.spies().allocationPct();
        int w1a = (int)(pct * w1); // width of spending

        int y1 = y+((h-h1)/2);
        int x1 = x+s20;

        g.setColor(Color.black);
        g.fillRect(x1,y1,w1,h1);
        g.setColor(sliderC);
        g.fillRect(x1, y1, w1a, h1);
        buttonSlider.setBounds(x1,y1,w1,h1);
        if (hoverShape == buttonSlider) {
            g.setColor(Color.yellow);
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.drawRect(x1,y1,w1,h1);
            g.setStroke(prev);
        }

        int sz = h1/2;
        // draw left/decr button
        ptX[0]=x1-s3-sz; ptX[1]=ptX[0]+sz; ptX[2]=ptX[0]+sz;
        ptY[0]=y+(h/2);  ptY[1]=ptY[0]-sz; ptY[2]=ptY[0]+sz;
        Color buttonC = hoverShape == buttonDecr ? SystemPanel.yellowText :  Color.black;
        buttonDecr.reset();
        for (int i=0;i<ptX.length;i++) 
            buttonDecr.addPoint(ptX[i], ptY[i]);
        drawShadedPolygon(g, ptX, ptY, buttonC, s1, -s1);

        // draw right/incr button
        ptX[0]=x1+w1+sz+s3; ptX[1]=ptX[0]-sz; ptX[2]=ptX[0]-sz;
        ptY[0]=y+(h/2);  ptY[1]=ptY[0]-sz; ptY[2]=ptY[0]+sz;
        buttonIncr.reset();
        for (int i=0;i<ptX.length;i++) 
            buttonIncr.addPoint(ptX[i], ptY[i]);
        buttonC = hoverShape == buttonIncr ? SystemPanel.yellowText :  Color.black;
        drawShadedPolygon(g, ptX, ptY, buttonC, s1, -s1);
        
        if (!parent.selectedEmpire().isPlayer()) {
            g.setColor(SystemPanel.whiteText);
            g.setFont(narrowFont(15));
            String spyCost = text("RACES_INTEL_SPENDING_ANNUAL", (int) externalSpyingCost);
            int sw1 = g.getFontMetrics().stringWidth(spyCost);
            int xa = buttonSlider.x+((buttonSlider.width - sw1)/2);
            int ya = buttonSlider.y+buttonSlider.height-s4;
            g.drawString(spyCost, xa, ya);
        }
    }
    private void drawSpiesMissionButton(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        int leftM = x;
        int rightM = x+w;
        int buttonW = s10;
        int buttonTopY = y+s5;
        int buttonMidY = y+s15;
        int buttonBotY = y+s25;
        
        //left arrow
        ptX[0] = leftM; ptX[1] = leftM+buttonW; ptX[2] = leftM+buttonW;
        ptY[0] = buttonMidY; ptY[1] = buttonTopY; ptY[2] = buttonBotY;
        missionDecr.reset();
        for (int i=0;i<ptX.length;i++) 
           missionDecr.addPoint(ptX[i], ptY[i]);
        if (hoverShape == missionDecr)
            g.setColor(SystemPanel.yellowText);
        else
            g.setColor(Color.black);
        g.fillPolygon(ptX, ptY, 3);
        
        // right arrow
        ptX[0] = rightM; ptX[1] = rightM-buttonW; ptX[2] = rightM-buttonW;
        ptY[0] = buttonMidY; ptY[1] = buttonTopY; ptY[2] = buttonBotY;
        missionIncr.reset();
        for (int i=0;i<ptX.length;i++) 
            missionIncr.addPoint(ptX[i], ptY[i]);
        if (hoverShape == missionIncr)
            g.setColor(SystemPanel.yellowText);
        else
            g.setColor(Color.black);
        g.fillPolygon(ptX, ptY, 3);
        

        // center box
        int barX = x+s12;
        int barW = w-s24;
        int barY = y+s5;
        int barH = h;
        g.setColor(Color.black);
        g.fillRect(barX, barY, barW, barH);
        spyMissionBox.setBounds(barX, barY, barW, barH);

        EmpireView view = player().viewForEmpire(emp);
        g.setColor(sliderBoxBlue);
        String name = view.spies().missionName();
        scaledFont(g, name, barW-s5, 18, 14);
        int sw = g.getFontMetrics().stringWidth(name);
        int x0 = barX+((barW-sw)/2);
        g.drawString(name, x0, barY+s16);

        if (hoverShape == spyMissionBox) {
            Stroke prev = g.getStroke();
            g.setColor(SystemPanel.yellowText);
            g.setStroke(stroke2);
            g.draw(spyMissionBox);
            g.setStroke(prev);
        }
    }
    private void drawManageSpiesButton(Graphics2D g, int x, int y, int w, int h) {
        manageSpiesBox.setBounds(x,y,w,h);
        g.setColor(Color.blue);

        Stroke prev = g.getStroke();
        g.setStroke(stroke3);
        g.setColor(Color.black);
        g.drawRect(x+s2,y+s2,w,h);
        g.setStroke(prev);

        g.setColor(unselectedC);
        g.fillRect(x,y,w,h);

        Color c0 = SystemPanel.whiteText;
        if (hoverShape == manageSpiesBox)
            c0 = Color.yellow;

        String lbl = text("RACES_INTEL_BUREAU_MANAGE");
        int fontSize = scaledFont(g, lbl, w-s10, 18, 14);
        g.setFont(narrowFont(fontSize));
        int sw = g.getFontMetrics().stringWidth(lbl);
        int x0 = x+((w-sw)/2);
        int y0 = y+h-s8;
        drawShadowedString(g, lbl, 2, x0, y0, MainUI.shadeBorderC(), c0);

        prev = g.getStroke();
        g.setStroke(stroke2);
        g.setColor(c0);
        g.drawRect(x,y,w,h);
        g.setStroke(prev);
    }
    private void nextSpyMission() {
        EmpireView view = player().viewForEmpire(parent.selectedEmpire());
        if (view != null)
            view.spies().nextMission();
    }
    private void previousSpyMission() {
        EmpireView view = player().viewForEmpire(parent.selectedEmpire());
        if (view != null)
            view.spies().prevMission();        
    }
    private void increaseMaxSpies() {
        EmpireView view = player().viewForEmpire(parent.selectedEmpire());
        if (view != null) {
            view.spies().increaseMaxSpies();
            setValues();
        }
    }
    private void decreaseMaxSpies() {
        EmpireView view = player().viewForEmpire(parent.selectedEmpire());
        if (view != null) {
            view.spies().decreaseMaxSpies();        
            setValues();
        }
    }
    private boolean increaseSliderValue() {
        if (parent.selectedEmpire().isPlayer()) {
            int oldValue = player().internalSecurity();
            player().increaseInternalSecurity();
            return oldValue != player().internalSecurity();
        }
        else {
            EmpireView view = player().viewForEmpire(parent.selectedEmpire());
            int oldValue = view.spies().allocation();
            view.spies().increaseSpending();
            return oldValue != view.spies().allocation();
        }
    }
    private boolean decreaseSliderValue() {
        if (parent.selectedEmpire().isPlayer()) {
            int oldValue = player().internalSecurity();
            player().decreaseInternalSecurity();
            return oldValue != player().internalSecurity();
        }
        else {
            EmpireView view = player().viewForEmpire(parent.selectedEmpire());
            int oldValue = view.spies().allocation();
            view.spies().decreaseSpending();
            return oldValue != view.spies().allocation();
        }
    }
    private void setValues() {
        for (int i=0; i<techY.length; i++)
            techY[i] = 0;
        
        if (parent.selectedEmpire().isPlayer()) {
            internalSecurityCost = player().empireInternalSecurityCost();
            externalSpyingCost = player().empireExternalSpyingCost();
        }
        else {
            EmpireView view = player().viewForEmpire(parent.selectedEmpire());
            externalSpyingCost = player().totalTaxablePlanetaryProduction() * view.spies().allocationCostPct();
        }
        loadTechMaps();
    }
    private void loadTechMaps() {
        knownTechs.clear();
        unknownTechs.clear();
        techOwners.clear();
        Empire emp = parent.selectedEmpire();
        if (emp.isPlayer()) 
            loadAllUnknownTechs();
        else
            loadAllTechs(emp);
    }
    private void loadAllUnknownTechs() {
        for (int i=0;i<TechTree.NUM_CATEGORIES;i++) {
            knownTechs.put(i, new ArrayList<>());
            unknownTechs.put(i, new ArrayList<>());
        }
        Empire pl = player();
        TechTree plTree = pl.tech();
        List<String> tradedTechs = new ArrayList<>();
        for (String techId: pl.tech().tradedTechs())
            tradedTechs.add(techId);
        List<Empire> empires = pl.contactedEmpires();
        for (Empire emp: empires) {
            TechTree empTree = pl.viewForEmpire(emp).spies().tech();
            for (int i=0;i<TechTree.NUM_CATEGORIES;i++) {
                List<String> aiTechs = new ArrayList<>(empTree.category(i).knownTechs());
                List<String> plTechs = new ArrayList<>(plTree.category(i).knownTechs());
                aiTechs.removeAll(plTechs);
                for (String id : aiTechs) {
                    List<String> currUnknowns = unknownTechs.get(i);
                    if (!currUnknowns.contains(id) && !tradedTechs.contains(id))
                        currUnknowns.add(id);
                    if (!techOwners.containsKey(id))
                        techOwners.put(id, new ArrayList<>());
                    techOwners.get(id).add(emp);
                }
            }        
        }
    }
    private void loadAllTechs(Empire emp) {
        Empire pl = player();
        
        TechTree empTree = pl.viewForEmpire(emp).spies().tech();
        List<String> tradedTechs = new ArrayList<>();
        for (String techId: pl.tech().tradedTechs())
            tradedTechs.add(techId);
        TechTree plTree = pl.tech();
        for (int i=0;i<TechTree.NUM_CATEGORIES;i++) {
            List<String> aiKnown = new ArrayList<>(empTree.category(i).knownTechs());
            List<String> playerKnown = new ArrayList<>(plTree.category(i).knownTechs());
            List<String> aiUnknown = new ArrayList<>();
            for (String id : aiKnown) {
                if (!playerKnown.contains(id) && !tradedTechs.contains(id))
                    aiUnknown.add(id);
            }
            aiKnown.removeAll(aiUnknown);
            Collections.sort(aiUnknown, Tech.REVERSE_LEVEL);
            Collections.sort(aiKnown, Tech.REVERSE_LEVEL);
            knownTechs.put(i, aiKnown);
            unknownTechs.put(i, aiUnknown);
        }
    }
    private void openManageSpiesPane() {
        softClick();
        manageSpiesPane.init();
        enableGlassPane(manageSpiesPane);
        return;
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int x = e.getX();
        int y = e.getY();
        int count = e.getUnitsToScroll();
        for (int i=0;i<techBoxes.length;i++) {
            if ((hoverShape == techBoxes[i])
            || (hoverShape == techScrollers[i])) {
                int prevY = techY[i];
                if (count < 0)
                    techY[i] = max(0,techY[i]-s10);
                else 
                    techY[i] = min(techYMax[i],techY[i]+s10);
                if (techY[i] != prevY)
                    repaint(techBoxes[i]);
                return;
            }
        }
        if (hoverShape == buttonSlider) {
            boolean changed = count < 0 ? increaseSliderValue() : decreaseSliderValue();
            if (changed) {
                setValues();
                repaint();
            }
        }
        else if (hoverShape == spyMissionBox) {
            if (count < 0)
                nextSpyMission();
            else
                previousSpyMission();
            repaint();
        }
        else if (maxSpyBox.contains(x,y)) {
            if (count < 0)
                increaseMaxSpies();
            else
                decreaseMaxSpies();
            repaint();
        }
    }
    @Override
    public void mouseDragged(MouseEvent e) { 
        int x = e.getX();
        int y = e.getY();
        int dY = y-dragY;
        dragY = y;
        for (int i=0;i<techBoxes.length;i++) {
            if (techScrollers[i] == hoverShape) {
                if ((y >= techBoxes[i].y) || (y <= (techBoxes[i].y+techBoxes[i].height))) { 
                    int h = (int) techBoxes[i].getHeight();
                    int dListY = (int)((float)dY*(h+techYMax[i])/h);
                    if (dY < 0)
                        techY[i] = max(0,techY[i]+dListY);
                    else 
                        techY[i] = min(techYMax[i],techY[i]+dListY);
                }
                repaint(techBoxes[i]);
                return;
            }
            else if (techBoxes[i] == hoverShape) {
                if (techBoxes[i].contains(x,y)) { 
                    int h = (int) techBoxes[i].getHeight();
                    int dListY = (int)(-(float)dY*(h+techYMax[i])/h);
                    if (dListY < 0)
                        techY[i] = max(0,techY[i]+dListY);
                    else 
                        techY[i] = min(techYMax[i],techY[i]+dListY);
                }
                repaint(techBoxes[i]);
                return;
            }
        }
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Shape prevHover = hoverShape;
        hoverShape = null;
        for (int i=0;i<techBoxes.length;i++) {
            if (techScrollers[i].contains(x,y)) 
                hoverShape = techScrollers[i];
            else if (techBoxes[i].contains(x,y)) 
                hoverShape = techBoxes[i];
        }
        if (spyMaxIncr.contains(x,y))
            hoverShape = spyMaxIncr;
        else if (spyMaxDecr.contains(x,y))
            hoverShape = spyMaxDecr;
        else if (maxSpyBox.contains(x,y))
            hoverShape = maxSpyBox;
        else if (buttonIncr.contains(x,y))
            hoverShape = buttonIncr;
        else if (buttonDecr.contains(x,y))
            hoverShape = buttonDecr;
        else if (buttonSlider.contains(x,y))
            hoverShape = buttonSlider;
        else if (manageSpiesBox.contains(x,y))
            hoverShape = manageSpiesBox;
        else if (spyMissionBox.contains(x,y))
            hoverShape = spyMissionBox;
        else if (missionDecr.contains(x,y))
            hoverShape = missionDecr;
        else if (missionIncr.contains(x,y))
            hoverShape = missionIncr;

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
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        dragY = 0;
        if (hoverShape == null)
            return;
        if (hoverShape == manageSpiesBox) {
            openManageSpiesPane();
            return;
        }
        else if (hoverShape == spyMaxDecr) {
            decreaseMaxSpies();
            repaint();
            return;
        }
        else if (hoverShape == spyMaxIncr) {
            increaseMaxSpies();
            repaint();
            return;
        }
        else if (hoverShape == missionDecr) {
            previousSpyMission();
            repaint();
            return;
        }
        else if (hoverShape == missionIncr) {
            nextSpyMission();
            repaint();
            return;
        }
        else if (hoverShape == spyMissionBox) {
            nextSpyMission();
            repaint();
            return;
        }
        Empire emp = parent.selectedEmpire();
        EmpireView view = parent.selectedView();
        if (hoverShape == buttonIncr) {
            boolean changed = increaseSliderValue();
            if (changed) {
                setValues();
                softClick();
                repaint();
            }
            else
                misClick();
            return;
        }
        else if (hoverShape == buttonDecr) {
            boolean changed = decreaseSliderValue();
            if (changed) {
                setValues();
                softClick();
                repaint();
            }
            else
                misClick();
            return;
        }
        else if (hoverShape == buttonSlider) {
            float pct = (float) ((e.getX() - buttonSlider.getMinX()) / buttonSlider.getWidth());
            boolean changed;
            if (emp.isPlayer()) {
                int oldSec = emp.internalSecurity();
                emp.securityAllocation(pct);
                changed = oldSec != emp.internalSecurity();
            }
            else {
                int oldAlloc = view.spies().allocation();
                view.spies().allocationPct(pct);
                changed = oldAlloc != view.spies().allocation();
            }
            if (changed) {
                setValues();
                softClick();
                repaint();
            }
            else
                misClick();
            return;
        }
    }
    @Override
    public void mouseEntered(MouseEvent mouseEvent) {}
    @Override
    public void mouseExited(MouseEvent mouseEvent) {
        if (hoverShape != null) {
            hoverShape = null;
            repaint();
        }
    }
}