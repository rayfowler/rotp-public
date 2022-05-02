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
import java.awt.LinearGradientPaint;import java.awt.Polygon;
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
import rotp.model.empires.DiplomaticTreaty;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.TreatyAlliance;
import rotp.model.incidents.DiplomaticIncident;
import rotp.ui.BasePanel;
import rotp.ui.UserPreferences;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.diplomacy.DiplomaticMessage;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import static rotp.ui.races.RacesIntelligenceUI.sliderC;

public final class RacesDiplomacyUI extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    public static final String[] relationIds = new String[] { "RELATIONS_FEUD", "RELATIONS_HATE", "RELATIONS_DISCORD",
        "RELATIONS_TROUBLED", "RELATIONS_TENSE", "RELATIONS_RESTLESS", "RELATIONS_WARY", "RELATIONS_UNEASE",
        "RELATIONS_NEUTRAL", "RELATIONS_RELAXED", "RELATIONS_AMIABLE", "RELATIONS_CALM", "RELATIONS_AFFABLE",
        "RELATIONS_PEACEFUL", "RELATIONS_FRIENDLY", "RELATIONS_UNITY", "RELATIONS_HARMONY" };
    private static final Color unselectedC = new Color(112,85,68);
    private final Color greenEdgeC = new Color(44,59,30);
    private final Color greenMidC = new Color(70,93,48);
    private final Color brownEdgeC = new Color(101,70,50);
    private final Color brownMidC = new Color(161,111,78);
    private final Color brownDividerC = new Color(136,115,96);
    private final Color incidentRedC = new Color(112,0,0);
    private final Color incidentGreenC = new Color(0,63,32);

    private final RacesUI parent;
    private final ManageDiplomatsUI manageDiplomatsPane;
    private final ManageSpiesUI manageSpiesPane;
    int incidentY, incidentYMax;
    int relationsY, relationsYMax;
    int dragY;
    private LinearGradientPaint incidentTitleBackground;
    private LinearGradientPaint backGradient;
    private LinearGradientPaint embassyBackground, embassyDisabledBackground;
    private final Rectangle embassyBox = new Rectangle();
    private final Rectangle incidentListBox = new Rectangle();
    private final Rectangle incidentScroller = new Rectangle();
    private final Rectangle relationsListBox = new Rectangle();
    private final Rectangle relationsScroller = new Rectangle();
    private final Rectangle manageDiplomatsBox = new Rectangle();
    private final Rectangle manageSpiesBox = new Rectangle();
    Polygon buttonIncr = new Polygon();
    Polygon buttonDecr = new Polygon();
    Rectangle buttonSlider = new Rectangle();
    private float internalSecurityCost = 0;
    private float externalSpyingCost = 0;
    int[] ptX = new int[3];
    int[] ptY = new int[3];
    private final HashMap<DiplomaticIncident, Empire> incidentMap = new HashMap<>();

    private Shape hoverShape;
    
    public RacesDiplomacyUI(RacesUI p) {
        parent = p;
        manageDiplomatsPane = new ManageDiplomatsUI(p);
        manageSpiesPane = new ManageSpiesUI(p);

        initModel();
    }
    @Override
    public void drawTexture(Graphics g)      { }
    @Override
    public String textureName()     { return TEXTURE_BROWN; }
    public void init()              { setValues(); }
    public void changedEmpire()     { setValues(); }
    @Override
    public void open()              { setValues(); }
    private void setValues() {
        incidentY = 0;
        relationsY = 0;
        if (parent.selectedEmpire().isPlayer()) {
            internalSecurityCost = player().empireInternalSecurityCost();
            externalSpyingCost = player().empireExternalSpyingCost();
        }
    }
    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        int w = getWidth();
        int h = getHeight();

        relationsListBox.setBounds(0,0,0,0);
        relationsScroller.setBounds(0,0,0,0);
        manageDiplomatsBox.setBounds(0,0,0,0);
        manageSpiesBox.setBounds(0,0,0,0);
        buttonIncr.reset();
        buttonDecr.reset();
        
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
        setBackground(RacesUI.darkerBrown);
        setBorder(newEmptyBorder(5,5,5,5));
        addMouseMotionListener(this);
        addMouseListener(this);
        addMouseWheelListener(this);        
    }
    private void paintPlayerData(Graphics2D g) {
        Empire emp = parent.selectedEmpire();
        int w = getWidth();
        int h = getHeight();

        int s200 = scaled(200);
        int s210 = scaled(210);
        int s215 = scaled(215);
        int s245 = scaled(245);
        int s260 = scaled(260);
        int s370 = scaled(370);
        int s435 = scaled(435);
        int x0 = s20;
        int x1 = w-scaled(299);
        int w0 = x1-x0-s20;
        int w1 = scaled(279);

        drawRaceIconBase(g, emp, s55, s25, s210, s210);
        drawPlayerBaseInfo(g, emp, s260, s80, s370, scaled(130));
        drawPlayerDiplomaticEvents(g, emp, x0, s245, w0, h-s245-s10);
        drawPlayerDiplomacyBureau(g, emp, x1, s10, w1, s200);
        drawPlayerCounterIntelligenceBureau(g, emp, x1, s215, w1, s215);
        drawPlayerIntelligenceBureau(g, emp, x1, s435, w1, s200);
        if (UserPreferences.texturesInterface()) 
            drawTexture(g,0,0,w,h);
        drawRaceIcon(g, emp, s60, s30, s200, s200);
        drawEmpireName(g, emp, s260, s30, s370, s50);
    }
    private void paintAIData(Graphics2D g) {
        Empire emp = parent.selectedEmpire();
        int w = getWidth();
        int h = getHeight();

        int s200 = scaled(200);
        int s135 = scaled(135);
        int s150 = scaled(150);
        int s175 = scaled(175);
        int s210 = scaled(210);
        int s245 = scaled(245);
        int s260 = scaled(260);
        int s295 = scaled(295);
        int s330 = scaled(330);
        int s370 = scaled(370);
        
        int x0 = s20;
        int x1 = w-scaled(299);
        int w0 = x1-x0-s20;
        int w1 = scaled(279);

        drawRaceIconBase(g, emp, s55, s25, s210, s210);
        drawAIBaseInfo(g, emp, s260, s80, s370, scaled(130));
        drawRelationsMeter(g, emp, x0, s245, w0, s40);
        drawAIDiplomaticEvents(g, emp, x0, s295, w0, h-s295-s10);
        drawAIDiplomacyBureau(g, emp, x1, s10, w1, s135);
        drawAITradeSummary(g, emp, x1, s150, w1, s175);
        drawAIForeignRelations(g, emp, x1, s330, w1, h-s330-s10);          
        if (UserPreferences.texturesInterface()) 
            drawTexture(g,0,0,w,h);
        drawRaceIcon(g, emp, s60, s30, s200, s200);
        drawEmpireName(g, emp, s260, s30, s370, s50);
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
    private void drawEmpireName(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(SystemPanel.orangeText);
        g.setFont(narrowFont(32));
        drawString(g,emp.name(), x+s10, y+h-s15);
    }
    private void drawPlayerBaseInfo(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);

        int lineH = s30;
        int y1 = y+lineH-s5;
        int y2 = y1+lineH;
        int y3 = y2+lineH;
        int y4 = y3+lineH;
        int x0 = x+s20;
        g.setFont(narrowFont(22));
        Color textC = SystemPanel.whiteText;
        drawShadowedString(g, text("RACES_DIPLOMACY_HOMEWORLD"), 1, x0, y1, SystemPanel.blackText, textC);
        drawShadowedString(g, text("RACES_DIPLOMACY_LEADER"), 1, x0, y2, SystemPanel.blackText, textC);
        drawShadowedString(g, text("RACES_DIPLOMACY_CURRENT_TRADE"), 1, x0, y3, SystemPanel.blackText, textC);
        drawShadowedString(g, text("RACES_DIPLOMACY_TOTAL_TRADE"), 1, x0, y4, SystemPanel.blackText, textC);

        g.setFont(narrowFont(20));
        g.setColor(SystemPanel.blackText);
        String s = emp.sv.name(emp.capitalSysId());
        int sw = g.getFontMetrics().stringWidth(s);
        drawString(g,s, x+w-s20-sw, y1);

        s = text("TITLE_LEADERNAME", emp.labels().text("_nameTitle"), emp.leader().name());
        s = emp.replaceTokens(s, "alien");
        sw = g.getFontMetrics().stringWidth(s);
        drawString(g,s, x+w-s20-sw, y2);

        int amt = (int) player().totalTradeIncome();
        s = text("RACES_DIPLOMACY_TRADE_AMT", str(amt));
        sw = g.getFontMetrics().stringWidth(s);
        drawString(g,s, x+w-s20-sw, y3);

        amt = player().totalTradeTreaties();
        s = text("RACES_DIPLOMACY_TRADE_AMT", str(amt));
        sw = g.getFontMetrics().stringWidth(s);
        drawString(g,s, x+w-s20-sw, y4);
    }
    private void drawAIBaseInfo(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);

        int lineH = s30;
        int y1 = y+lineH-s5;
        int y2 = y1+lineH;
        int y3 = y2+lineH;
        int y4 = y3+lineH;
        int x0 = x+s20;
        g.setFont(narrowFont(22));
        Color textC = SystemPanel.whiteText;
        drawShadowedString(g, text("RACES_DIPLOMACY_HOMEWORLD"), 1, x0, y1, SystemPanel.blackText, textC);
        drawShadowedString(g, text("RACES_DIPLOMACY_LEADER"), 1, x0, y2, SystemPanel.blackText, textC);
        drawShadowedString(g, text("RACES_DIPLOMACY_CHARACTER"), 1, x0, y3, SystemPanel.blackText, textC);
        drawShadowedString(g, text("RACES_DIPLOMACY_STATUS"), 1, x0, y4, SystemPanel.blackText, textC);

        g.setFont(narrowFont(20));
        g.setColor(SystemPanel.blackText);
        String s = emp.sv.name(emp.capitalSysId());
        int sw = g.getFontMetrics().stringWidth(s);
        drawString(g,s, x+w-s20-sw, y1);

        s = text("TITLE_LEADERNAME", emp.race().text("_nameTitle"), emp.leader().name());
        s = emp.replaceTokens(s, "alien");
        sw = g.getFontMetrics().stringWidth(s);
        drawString(g,s, x+w-s20-sw, y2);

        s =  text("LEADER_PERSONALITY_FORMAT", emp.leader().personality(),emp.leader().objective());
        sw = g.getFontMetrics().stringWidth(s);
        drawString(g,s, x+w-s20-sw, y3);

        EmpireView view = player().viewForEmpire(emp);
        DiplomaticTreaty treaty = view.embassy().treaty();
        boolean isAlly = treaty.isAlliance();
        int starW = s10;
        int offset = isAlly ? (starW*5)+s5 : 0;
        s = treaty.status(player());
        sw = g.getFontMetrics().stringWidth(s);
        drawString(g,s, x+w-s20-offset-sw, y4);
        if (isAlly) {
            TreatyAlliance alliance = (TreatyAlliance) treaty;
            int x1 = x+w-s20-offset+s5;
            parent.drawAllianceStars(g,x1,y4-s3,alliance.standing(player()),starW);
        }
    }
    private void drawRelationsMeter(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);
        
        EmpireView view = player().viewForEmpire(emp);
        if (view == null)
            return;

        int x0 = x+s10;
        int y0 = y+h-s12;
        g.setFont(narrowFont(24));
        String str = text("RACES_DIPLOMACY_RELATIONS_METER");
        this.drawShadowedString(g, str, 1,  x0, y0, SystemPanel.blackText, SystemPanel.whiteText);
        int leftW = g.getFontMetrics().stringWidth(str);
        
        if (emp.masksDiplomacy()) {
            g.setFont(narrowFont(15));
            g.setColor(SystemPanel.blackText);
            String str2 = text("RACES_DIPLOMACY_RELATIONS_UNKNOWN");
            str2 = emp.replaceTokens(str2, "alien");
            List<String> lines = this.wrappedLines(g, str2, w-s60-leftW);
            int x1 = x0+leftW+s40;
            int y1 = lines.size() == 1 ? y0 : y0-s10;
            for (String line: lines) {
                drawString(g,line, x1, y1);
                y1 += s15;
            }
            return;
        }
        g.setFont(narrowFont(20));
        g.setColor(SystemPanel.blackText);
        float relations = view.otherView().embassy().relations();
        // which relation key display to use
        float pctRel = (relations+100)/200.0f;
        float sizeRelId = 1.0f/relationIds.length;
        int i = (int) (pctRel/sizeRelId);
        i = bounds(0,i,relationIds.length-1);
        String rel = text(relationIds[i]);
        int rightW = g.getFontMetrics().stringWidth(rel);
        
        int x1 = x+w-rightW-s20;
        drawString(g,rel, x1, y0);
        
        int x2 = x0+leftW+s30;
        int w2 = x+w-x2-rightW-s30;
        int y2 = y+s12;
        int h2 = h-s15;
        parent.drawRelationsBar(g, emp, x2, y2, w2, h2, s16, s8);
    }
    private void drawPlayerDiplomaticEvents(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        // title area
        int x0 = x;
        int w0 = w;
        int y0 = y;
        int h0 = s30;
        if (incidentTitleBackground == null) {
            float[] dist = {0.0f, 0.5f, 1.0f};
            Point2D ptStart = new Point2D.Float(x0, 0);
            Point2D ptEnd = new Point2D.Float(x0 + w0, 0);
            Color[] yesColors = {brownEdgeC, brownMidC, brownEdgeC};
            incidentTitleBackground = new LinearGradientPaint(ptStart, ptEnd, dist, yesColors);
        }
        g.setPaint(incidentTitleBackground);
        g.fillRect(x0, y0, w0, h0);

        //year or turn
        g.setFont(narrowFont(20));
        int x1 = x0;
        int w1 = s60;         
        String year = UserPreferences.displayYear() ? text("RACES_DIPLOMACY_EVENT_YEAR") :  text("RACES_DIPLOMACY_EVENT_TURN");
        int sw = g.getFontMetrics().stringWidth(year);
        drawShadowedString(g, year, 1, x0+(w1-sw)/2, y0+s23, Color.black, SystemPanel.whiteText);
        
        //race
        g.setFont(narrowFont(20));
        int x2 = x1+w1;
        int w2 = s100;
        String race = text("RACES_DIPLOMACY_EVENT_RACE");
        sw = g.getFontMetrics().stringWidth(race);
        drawShadowedString(g, race, 1, x2+(w2-sw)/2, y0+s23, Color.black, SystemPanel.whiteText);
        
        // scrollboar
        int w5 = s20;
        int x5 = x0+w0-w5;
        
        // effect
        g.setFont(narrowFont(20));
        String effect = text("RACES_DIPLOMACY_EVENT_EFFECT");
        sw = g.getFontMetrics().stringWidth(effect);
        int w4 = s60;
        int x4 = x5-w4;
        drawShadowedString(g, effect, 1, x4+(w4-sw)/2, y0+s23, Color.black, SystemPanel.whiteText);
        
        g.setFont(narrowFont(22));
        int x3 = x2+w2;
        int w3 = x4-x3;
        String title = text("RACES_DIPLOMACY_EVENT_TITLE");
        sw = g.getFontMetrics().stringWidth(title);
        drawShadowedString(g, title, 1, x3+(w3-sw)/2, y0+s23, Color.black, SystemPanel.whiteText);
        
        
        // data area
        g.setColor(RacesUI.darkBrown);
        int y1 = y0+h0;
        int listH = h-h0;
        g.fillRect(x0, y1, w0, listH);

        g.setClip(x0, y1+s1, w0, listH-s2);
        // draw vertical divider lines
        Stroke prev = g.getStroke();
        g.setStroke(stroke1);
        g.setColor(brownDividerC);
        g.drawLine(x2, y1, x2, y1+listH);
        g.drawLine(x3, y1, x3, y1+listH);
        g.drawLine(x4, y1, x4, y1+listH);
        g.drawLine(x5, y1, x5, y1+listH);
        g.setStroke(prev);
        List<DiplomaticIncident> incidents = new ArrayList<>();
        incidentMap.clear();
        for (EmpireView view: player().contacts()) {
            for (DiplomaticIncident inc : view.otherView().embassy().allIncidents()) {
                if ((inc.currentSeverity() != 0) && inc.triggeredByAction()) {
                    incidents.add(inc);
                    incidentMap.put(inc, view.empire());
                }
            }
        }
        Collections.sort(incidents, DiplomaticIncident.DATE);
        int y2 = y1 - incidentY;
        int fullListH = 0;
        for (int i=0;i<incidents.size();i++) {
            DiplomaticIncident inc = incidents.get(i);
            int incidentH = drawPlayerIncident(g, inc, x0, y2, w0, w1, w2, w3, w4);
            //box(i).setBounds(x1,y1,w1,incidentH);
            fullListH += incidentH;
            y2 += incidentH;
        }
        incidentYMax = max(0, fullListH-listH);
 
        g.setColor(RacesUI.scrollBarC);
        int scrollW = s12;
        int scrollH = (int) ((float)listH*listH/(listH+incidentYMax));
        int scrollX = x+w-scrollW-s4;
        int scrollY =(int) (y1+ (float)listH*incidentY/(incidentYMax+listH));
        g.fillRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
        incidentScroller.setBounds(scrollX, scrollY, scrollW, scrollH);
        if (hoverShape == incidentScroller) {
            prev = g.getStroke();
            g.setColor(Color.yellow);
            g.setStroke(stroke2);
            g.drawRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
            g.setStroke(prev);
        }

        g.setClip(null);
        
        incidentListBox.setBounds(x0,y1,w0,listH);
        if ((hoverShape == incidentListBox) 
        && (fullListH > listH)) {
            g.setColor(Color.yellow);
            prev = g.getStroke();
            g.setStroke(stroke2);
            g.draw(incidentListBox);
            g.setStroke(prev);
        }
    }
    private int drawPlayerIncident(Graphics g, DiplomaticIncident inc, int x, int y, int w, int w1, int w2, int w3, int w4) {
        String title = inc.title()+":";
        String sev = fmt(inc.currentSeverity(),1);
        String desc = inc.description();
        g.setFont(narrowFont(18));
        int indent = g.getFontMetrics().stringWidth(title)+s5;
        g.setFont(narrowFont(15));
        List<String> descLines = wrappedLines(g, desc, w3-s15, indent);
        int h0 = s7+(s22*descLines.size());

        // title
        int x1 = x;
        int x2 = x1+w1;
        int x3 = x2+w2;
        int x4 = x3+w3;
        
        int y1 = y+s8;

        // year or turn
        g.setFont(narrowFont(18));
        g.setColor(Color.black);
        String year = UserPreferences.displayYear() ? str(inc.dateOccurred()) :  str(inc.turnOccurred());
        int sw = g.getFontMetrics().stringWidth(year);
        int x0 = x1+(w1-sw)/2;
        int y0 = y+s25;
        drawString(g,year, x0, y0);
       
        // race
        g.setFont(narrowFont(18));
        g.setColor(Color.black);
        Empire otherEmpire = incidentMap.get(inc);
        String race = otherEmpire.raceName();
        sw = g.getFontMetrics().stringWidth(race);
        x0 = x2+(w2-sw)/2;
        y0 = y+s25;
        drawString(g,race, x0, y0);

        // description lines, field #3
        boolean firstLine = true;
        g.setFont(narrowFont(18));
        for (String line: descLines) {
            y1 += s17;
            g.setColor(Color.black);
            if (firstLine) {
                drawString(g,title, x3+s10, y1);
                g.setFont(narrowFont(15));
                drawString(g,line, x3+s10+indent, y1);
                firstLine = false;
            }
            else
                drawString(g,line, x3+s10, y1);
        }
 

        // severity
        if (!otherEmpire.masksDiplomacy()) {
            g.setFont(narrowFont(18));
            sw = g.getFontMetrics().stringWidth(sev);
            x0 = x4+(w4-sw)/2;
            if (inc.currentSeverity() < 0)
                g.setColor(incidentRedC);
            else
                g.setColor(incidentGreenC);
            drawString(g,sev, x0, y0);
        }

        g.setColor(brownDividerC);
        g.drawLine(x1, y1+s12, x+w-s20, y1+s12);
        return h0;
    }
    private void drawPlayerDiplomacyBureau(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);
        
        List<EmpireView> views = player().contacts();
        int recalls = 0;
        for (EmpireView v: views) {
            if (v.embassy().diplomatGone())
                recalls++;
        }
        
        g.setColor(SystemPanel.whiteText);
        g.setFont(narrowFont(24));
        String title = text("RACES_DIPLOMACY_BUREAU");
        int sw = g.getFontMetrics().stringWidth(title);
        int x0 = x+((w-sw)/2);
        int y0 = y+s25;
        drawShadowedString(g, title, 1, x0, y0, SystemPanel.blackText, SystemPanel.whiteText);
        
        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(15));
        String desc = text("RACES_DIPLOMACY_BUREAU_DESC1");
        List<String> lines = wrappedLines(g, desc, w-s30);
        int y1 = y0+s5;
        for (String line: lines) {
            y1 += s16;
            drawString(g,line, x+s20, y1);
        }
        
        g.setFont(narrowFont(18));
        String spies = text("RACES_DIPLOMACY_KNOWN_EMPIRES");
        int y2 = y0+s85;
        drawString(g,spies, x+s20, y2);
        
        g.setFont(narrowFont(16));
        String str2 = str(views.size());
        int sw2 = g.getFontMetrics().stringWidth(str2);
        drawString(g,str2, x+w-s20-sw2, y2);
        
        y2 += s20;
        g.setFont(narrowFont(18));
        String spending = text("RACES_DIPLOMACY_RECALLED_DIPLOMAT");
        drawString(g,spending, x+s20, y2);
        int amt = (int) 0;
        
        g.setFont(narrowFont(16));
        str2 = str(recalls);
        sw2 = g.getFontMetrics().stringWidth(str2);
        drawString(g,str2, x+w-s20-sw2, y2);
        
        drawManageDiplomatsButton(g, x+s20,y+h-s45,w-s40,s25);
    }
    private void drawPlayerCounterIntelligenceBureau(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);
        
        g.setColor(SystemPanel.whiteText);
        g.setFont(narrowFont(24));
        String title = text("RACES_DIPLOMACY_COUNTER_BUREAU");
        int fontSize = scaledFont(g, title, w-s20, 24, 20);
        g.setFont(narrowFont(fontSize));
        int sw = g.getFontMetrics().stringWidth(title);
        int x0 = x+((w-sw)/2);
        int y0 = y+s25;
        drawShadowedString(g, title, 1, x0, y0, SystemPanel.blackText, SystemPanel.whiteText);
        
        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(15));
        String desc = text("RACES_DIPLOMACY_COUNTER_DESC");
        List<String> lines = wrappedLines(g, desc, w-s30);
        int y1 = y0+s5;
        for (String line: lines) {
            y1 += s16;
            drawString(g,line, x+s20, y1);
        }
        
        g.setFont(narrowFont(18));
        String spies = text("RACES_INTEL_SECURITY_BONUS");
        int y2 = y0+s70;
        drawString(g,spies, x+s20, y2);
        
        g.setFont(narrowFont(16));
        int amt = (int) (100*player().totalInternalSecurityPct());
        String s = amt == 0 ? text("RACES_INTEL_SECURITY_BONUS_NONE") : text("RACES_INTEL_SECURITY_BONUS_AMT", str(amt));
        sw = g.getFontMetrics().stringWidth(s);
        drawString(g,s, x+w-s20-sw, y2);
        
        y2 += s20;
        g.setFont(narrowFont(18));
        String spending = text("RACES_INTEL_TOTAL_SPENDING");
        drawString(g,spending, x+s20, y2);
        
        g.setFont(narrowFont(16));
        amt = (int) internalSecurityCost;
        s = text("RACES_INTEL_SPENDING_ANNUAL", str(amt));
        sw = g.getFontMetrics().stringWidth(s);
        drawString(g,s, x+w-s20-sw, y2);
        
        g.setFont(narrowFont(15));
        desc = text("RACES_DIPLOMACY_COUNTER_DESC2");
        lines = wrappedLines(g, desc, w-s30);
        
        int y3 = y+h-s20-(s16*3);
        for (String line: lines) {
            drawString(g,line, x+s20, y3);
            y3 += s16;
        }
        
        g.setFont(narrowFont(16));
        int y4 = y+h-s20;
        String taxStr = text("RACES_INTEL_SECURITY_TAX");
        drawString(g,taxStr, x+s20, y4);
        int sw4 = g.getFontMetrics().stringWidth(taxStr);

         // draw string on right for pct 
        String cost = text("RACES_INTEL_PERCENT_AMT",(int)(player().internalSecurityCostPct()*100));
        sw = g.getFontMetrics().stringWidth(cost);
        drawString(g,cost, x+w-s20-sw, y4);
        // need maxwidth so slider doesn't move as cost pct changes
        String maxWidthStr = text("RACES_INTEL_PERCENT_AMT",10);
        sw = g.getFontMetrics().stringWidth(maxWidthStr);

        int sliderW = w-sw-sw4-s80;
        int sliderH = s16;
        drawSecuritySliderBar(g,x+s20+sw4, y4-sliderH+s3, sliderW, sliderH);
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
            drawString(g,spyCost, xa, ya);
        }
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
            drawString(g,line, x+s20, y1);
        }
        
        g.setFont(narrowFont(18));
        String spies = text("RACES_INTEL_BUREAU_SPIES");
        int y2 = y+h-s85;
        drawString(g,spies, x+s20, y2);
        
        g.setFont(narrowFont(16));
        String str2 = str(player().totalActiveSpies());
        int sw2 = g.getFontMetrics().stringWidth(str2);
        drawString(g,str2, x+w-s20-sw2, y2);
        
        y2 += s20;
        g.setFont(narrowFont(18));
        String spending = text("RACES_INTEL_BUREAU_SPENDING");
        drawString(g,spending, x+s20, y2);
        int amt = (int) externalSpyingCost;
        
        g.setFont(narrowFont(16));
        str2 = amt == 0 ? text("RACES_INTEL_SECURITY_BONUS_NONE") : text("RACES_INTEL_SPENDING_ANNUAL", str(amt));
        sw2 = g.getFontMetrics().stringWidth(str2);
        drawString(g,str2, x+w-s20-sw2, y2);
        
        y2 += s20;
        drawManageSpiesButton(g, x+s20,y2,w-s40,s25);
    }
    private void drawManageDiplomatsButton(Graphics2D g, int x, int y, int w, int h) {
        manageDiplomatsBox.setBounds(x,y,w,h);

        Stroke prev = g.getStroke();
        g.setStroke(stroke3);
        g.setColor(Color.black);
        g.drawRect(x+s2,y+s2,w,h);
        g.setStroke(prev);

        g.setColor(unselectedC);
        g.fillRect(x,y,w,h);

        Color c0 = SystemPanel.whiteText;
        if (hoverShape == manageDiplomatsBox)
            c0 = Color.yellow;

        String lbl = text("RACES_DIPLOMACY_MANAGE_DIPLOMATS");
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
    private void drawAIDiplomaticEvents(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        // title area
        int x0 = x;
        int w0 = w;
        int y0 = y;
        int h0 = s30;
        if (incidentTitleBackground == null) {
            float[] dist = {0.0f, 0.5f, 1.0f};
            Point2D ptStart = new Point2D.Float(x0, 0);
            Point2D ptEnd = new Point2D.Float(x0 + w0, 0);
            Color[] yesColors = {brownEdgeC, brownMidC, brownEdgeC};
            incidentTitleBackground = new LinearGradientPaint(ptStart, ptEnd, dist, yesColors);
        }
        g.setPaint(incidentTitleBackground);
        g.fillRect(x0, y0, w0, h0);

       //title
        g.setFont(narrowFont(20));
        String year = UserPreferences.displayYear() ? text("RACES_DIPLOMACY_EVENT_YEAR") :  text("RACES_DIPLOMACY_EVENT_TURN");
        drawShadowedString(g, year, 1, x0+s10, y0+s23, Color.black, SystemPanel.whiteText);
        
        g.setFont(narrowFont(22));
        String title = text("RACES_DIPLOMACY_EVENT_TITLE");
        int sw = g.getFontMetrics().stringWidth(title);
        drawShadowedString(g, title, 1, x0+(w0-sw)/2, y0+s23, Color.black, SystemPanel.whiteText);

        g.setFont(narrowFont(20));
        String effect = text("RACES_DIPLOMACY_EVENT_EFFECT");
        int sw2 = g.getFontMetrics().stringWidth(effect);
        drawShadowedString(g, effect, 1, x0+w0-sw2-s30, y0+s23, Color.black, SystemPanel.whiteText);
        
        // data area
        g.setColor(RacesUI.darkBrown);
        int y1 = y0+h0;
        int x1 = x0;
        int w1 = w0;
        int listH = h-h0;
        g.fillRect(x1, y1, w1, listH);

        g.setClip(x1, y1+s1, w1, listH-s2);
        // draw vertical divider lines
        int x1a = x1+s60;
        int x1b = x1+w1-s80;
        int x1c = x1+w1-s20;
        Stroke prev = g.getStroke();
        g.setStroke(stroke1);
        g.setColor(brownDividerC);
        g.drawLine(x1a, y1, x1a, y1+listH);
        g.drawLine(x1b, y1, x1b, y1+listH);
        g.drawLine(x1c, y1, x1c, y1+listH);
        g.setStroke(prev);
        List<DiplomaticIncident> incidents = new ArrayList<>();
        for (DiplomaticIncident inc : parent.selectedView().otherView().embassy().allIncidents()) {
            if (inc.currentSeverity() != 0)
                incidents.add(inc);
        }
        Collections.sort(incidents, DiplomaticIncident.DATE);
        int y2 = y1 - incidentY;
        int fullListH = 0;
        for (int i=0;i<incidents.size();i++) {
            DiplomaticIncident inc = incidents.get(i);
            int incidentH = drawAIIncident(g, inc, x1, y2, w1, s60, s80);
            //box(i).setBounds(x1,y1,w1,incidentH);
            fullListH += incidentH;
            y2 += incidentH;
        }
        incidentYMax = max(0, fullListH-listH);
        if (incidentYMax == 0)
            incidentScroller.setBounds(0,0,0,0);
        else {
            g.setColor(RacesUI.scrollBarC);
            int scrollW = s12;
            int scrollH = (int) ((float)listH*listH/(listH+incidentYMax));
            int scrollX = x1+w1-scrollW-s2;
            int scrollY =(int) (y1+ (float)listH*incidentY/(incidentYMax+listH));
            g.fillRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
            incidentScroller.setBounds(scrollX, scrollY, scrollW, scrollH);
            if (hoverShape == incidentScroller) {
                prev = g.getStroke();
                g.setColor(Color.yellow);
                g.setStroke(stroke2);
                g.drawRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
                g.setStroke(prev);
            }
        }
        g.setClip(null);
        
        incidentListBox.setBounds(x1,y1,w1,listH);
        if ((hoverShape == incidentListBox) 
        && (fullListH > listH)) {
            g.setColor(Color.yellow);
            prev = g.getStroke();
            g.setStroke(stroke2);
            g.draw(incidentListBox);
            g.setStroke(prev);
        }
    }
    private int drawAIIncident(Graphics g, DiplomaticIncident inc, int x, int y, int w, int leftM, int rightM) {
        String title = inc.title()+":";
        String sev = fmt(inc.currentSeverity(),1);
        String desc = inc.description();
        g.setFont(narrowFont(18));
        int indent = g.getFontMetrics().stringWidth(title)+s5;
        g.setFont(narrowFont(15));
        List<String> descLines = wrappedLines(g, desc, w-s20-leftM-rightM, indent);
        int h0 = s7+(s22*descLines.size());

        // title
        int x1 = x+leftM+s10;
        int y1 = y+s8;
        g.setFont(narrowFont(22));
        g.setColor(Color.black);

        // description lines
        boolean firstLine = true;
        g.setFont(narrowFont(18));
        for (String line: descLines) {
            y1 += s17;
            g.setColor(Color.black);
            if (firstLine) {
                drawString(g,title, x1, y1);
                g.setFont(narrowFont(15));
                drawString(g,line, x1+indent, y1);
                firstLine = false;
            }
            else
                drawString(g,line, x1, y1);
        }
        
        // year
        g.setFont(narrowFont(18));
        g.setColor(Color.black);
        String year = UserPreferences.displayYear() ? str(inc.dateOccurred()) :  str(inc.turnOccurred());
        int sw0 = g.getFontMetrics().stringWidth(year);
        int x0 = x+(leftM-sw0)/2;
        int y0 = y+s25;
        drawString(g,year, x0, y0);

        // severity
        int x2 = x+w-rightM;
        int w2 = s60;
        if (!parent.selectedEmpire().masksDiplomacy()) {
            g.setFont(narrowFont(18));
            int sw = g.getFontMetrics().stringWidth(sev);
            int x2a = x2+(w2-sw)/2;
            if (inc.currentSeverity() < 0)
                g.setColor(incidentRedC);
            else
                g.setColor(incidentGreenC);
            drawString(g,sev, x2a, y0);
        }

        g.setColor(brownDividerC);
        g.drawLine(x, y1+s8, x+w-s20, y1+s8);
        return h0;
    }
    private void drawAIDiplomacyBureau(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);
        g.setColor(SystemPanel.whiteText);
        g.setFont(narrowFont(24));
        String title = text("RACES_DIPLOMACY_BUREAU");
        int sw = g.getFontMetrics().stringWidth(title);
        int x0 = x+((w-sw)/2);
        int y0 = y+s25;
        drawShadowedString(g, title, 1, x0, y0, SystemPanel.blackText, SystemPanel.whiteText);
        
        boolean finalWar = player().viewForEmpire(emp).embassy().finalWar();
        boolean outOfRange = !player().viewForEmpire(emp).inEconomicRange();
        
        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(15));
        String desc;
        if (finalWar)
            desc = text("RACES_DIPLOMACY_BUREAU_DESC_FINAL");
        else if (outOfRange)
            desc = text("RACES_DIPLOMACY_BUREAU_DESC_RANGE");
        else {
            desc = text("RACES_DIPLOMACY_BUREAU_DESC");
            desc = emp.replaceTokens(desc, "alien");
        }
        
        List<String> lines = wrappedLines(g, desc, w-s30);
        int y1 = y0+s7;
        for (String line: lines) {
            y1 += s16;
            drawString(g,line, x+s15, y1);
        }
        
        embassyBox.setBounds(0,0,0,0);
        if (!finalWar && !outOfRange)
            drawEmbassyButton(g, x+s20,y+h-s38,w-s40,s28);
    }
    private void drawAITradeSummary(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);
        g.setColor(SystemPanel.whiteText);
        g.setFont(narrowFont(24));
        String title = text("RACES_DIPLOMACY_TRADE_SUMMARY");
        int sw = g.getFontMetrics().stringWidth(title);
        int x0 = x+((w-sw)/2);
        int y0 = y+s25;
        drawShadowedString(g, title, 1, x0, y0, SystemPanel.blackText, SystemPanel.whiteText);
        
        boolean finalWar = player().viewForEmpire(emp).embassy().finalWar();
        boolean outOfRange = !player().viewForEmpire(emp).inEconomicRange();
        
        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(15));
        String desc;
        
        if (finalWar)
            desc = text("RACES_DIPLOMACY_TRADE_DESC_FINAL");
        else if (outOfRange)
            desc = text("RACES_DIPLOMACY_TRADE_DESC_RANGE");
        else {
            desc = text("RACES_DIPLOMACY_TRADE_DESC");
            desc = emp.replaceTokens(desc, "alien");
        }
        
        List<String> lines = wrappedLines(g, desc, w-s30);
        int y1 = y0+s10;
        for (String line: lines) {
            y1 += s16;
            drawString(g,line, x+s15, y1);
        }
        
        EmpireView view = player().viewForEmpire(emp);
        if (view == null)
            return;
        
        g.setFont(narrowFont(18));
        String spies = text("RACES_DIPLOMACY_TRADE_TREATY");
        int y2 = lines.size() <= 5 ? y0+h-s65 : y0+h-s55;
        drawString(g,spies, x+s20, y2);
        
        g.setFont(narrowFont(16));
        String s2 = text("RACES_DIPLOMACY_TRADE_AMT", view.trade().level());
        int sw2 = g.getFontMetrics().stringWidth(s2);
        drawString(g,s2, x+w-s20-sw2, y2);
        
        y2 += s22;
        g.setFont(narrowFont(18));
        String spending = text("RACES_DIPLOMACY_CURRENT_TRADE");
        drawString(g,spending, x+s20, y2);
        int amt = (int) view.trade().profit();
        
        g.setFont(narrowFont(16));
        s2 = text("RACES_DIPLOMACY_TRADE_AMT", str(amt));
        sw2 = g.getFontMetrics().stringWidth(s2);
        drawString(g,s2, x+w-s20-sw2, y2);
    }
    private void drawAIForeignRelations(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);
        
        g.setColor(SystemPanel.whiteText);
        g.setFont(narrowFont(24));
        String title = text("RACES_DIPLOMACY_FOREIGN_RELATIONS");
        int sw = g.getFontMetrics().stringWidth(title);
        int x0 = x+((w-sw)/2);
        int y0 = y+s25;
        drawShadowedString(g, title, 1, x0, y0, SystemPanel.blackText, SystemPanel.whiteText);

        boolean outOfRange = !player().viewForEmpire(emp).inEconomicRange();

        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(15));
        String desc;
        if (outOfRange)
            desc = text("RACES_DIPLOMACY_FOREIGN_RANGE");
        else
            desc = text("RACES_DIPLOMACY_FOREIGN_DESC");
        desc = emp.replaceTokens(desc, "alien");
        List<String> lines = wrappedLines(g, desc, w-s30);
        int y1 = y0+s10;
        for (String line: lines) {
            y1 += s16;
            drawString(g,line, x+s15, y1);
        }
        
        if (outOfRange)
            return;
        
        int x2 = x+s10;
        int y2 = y1+s10;
        int w2 = w-s20;
        int listH = y+h-y2-s20;
        g.setColor(RacesUI.brown);
        g.fillRect(x2,y2,w2,listH);
        relationsListBox.setBounds(x2,y2,w2,listH);
        
        g.setClip(x2,y2+s1,w2,listH-s2);
        int y3 = y2-relationsY;
        int x3 = x2+s10;
        g.setFont(narrowFont(18));
        g.setColor(SystemPanel.blackText);
        List<EmpireView> contacts = emp.contacts();
        Collections.sort(contacts, EmpireView.PLAYER_LIST_ORDER);
        int rowH = s18;
        int fullListH = (contacts.size()*rowH)+s5;
        int rightM = fullListH <= listH ? s10 : s20;
        for (EmpireView contact: contacts) {
            if (contact.inEconomicRange()) {
                y3 += rowH;
                g.setFont(narrowFont(18));
                drawString(g,contact.empire().raceName(), x3, y3);
                g.setFont(narrowFont(15));
                String treaty = contact.embassy().treaty().status(player());
                int sw1 = g.getFontMetrics().stringWidth(treaty);
                int x3b = x2+w2-sw1-rightM;
                drawString(g,treaty, x3b, y3);
            }
        }      
        relationsYMax = max(0, fullListH-listH);
        if (relationsYMax == 0)
            relationsScroller.setBounds(0,0,0,0);
        else {
            g.setColor(RacesUI.scrollBarC);
            int scrollW = s12;
            int scrollH = (int) ((float)listH*listH/(listH+relationsYMax));
            int scrollX = x2+w2-scrollW-s2;
            int scrollY =(int) (y2+ (float)listH*relationsY/(relationsYMax+listH));
            g.fillRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
            relationsScroller.setBounds(scrollX, scrollY, scrollW, scrollH);
            if (hoverShape == relationsScroller) {
                Stroke prev = g.getStroke();
                g.setColor(Color.yellow);
                g.setStroke(stroke2);
                g.drawRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
                g.setStroke(prev);
            }
        }
        g.setClip(null);
        if ((hoverShape == relationsListBox) 
        && (fullListH > listH)) {
            g.setColor(Color.yellow);
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.draw(relationsListBox);
            g.setStroke(prev);
        }
    }
    private void drawEmbassyButton(Graphics2D g, int x, int y, int w, int h) {
        EmpireView view = player().viewForEmpire(parent.selectedEmpire());
        if (view == null)
            return;
        embassyBox.setBounds(x,y,w,h);

        if (embassyBackground == null) {
            float[] dist = {0.0f, 0.5f, 1.0f};
            Point2D ptStart = new Point2D.Float(x, 0);
            Point2D ptEnd = new Point2D.Float(x+w, 0);
            Color[] colors = {greenEdgeC, greenMidC, greenEdgeC};
            embassyBackground = new LinearGradientPaint(ptStart, ptEnd, dist, colors);
            Color[] colors2 = {brownEdgeC, brownMidC, brownEdgeC};
            embassyDisabledBackground = new LinearGradientPaint(ptStart, ptEnd, dist, colors2);
        }
        
        Stroke prev = g.getStroke();
        g.setStroke(stroke3);
        g.setColor(Color.black);
        g.drawRect(x+s2,y+s2,w,h);
        g.setStroke(prev);

        if (view.diplomats())
            g.setPaint(embassyBackground);
        else
            g.setPaint(embassyDisabledBackground);
        
        g.fillRect(x,y,w,h);

        Color c0 = SystemPanel.whiteText;
        if ((hoverShape == embassyBox) && view.diplomats())
            c0 = Color.yellow;

        String lbl = view.embassy().diplomatGone()? text("RACES_DIPLOMACY_CONTACT") : text("RACES_DIPLOMACY_AUDIENCE");
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
    public void openEmbassy() {
        EmpireView view = player().viewForEmpire(parent.selectedEmpire());
        if (view == null)
            return;
                
        if (view.embassy().diplomatGone()) {
            view.embassy().reopenEmbassy();
            parent.repaint();
            return;
        }
        
        if (!view.diplomats())
            return;
        
        DiplomaticMessage.show(parent.selectedEmpire().viewForEmpire(player()), DialogueManager.DIPLOMACY_MAIN_MENU);     
    }
    public void openManageDiplomatsPane() {
        softClick();
        manageDiplomatsPane.init();
        enableGlassPane(manageDiplomatsPane);
        return;
    }
    public void openManageSpiesPane() {
        softClick();
        manageSpiesPane.init();
        enableGlassPane(manageSpiesPane);
        return;
    }
    private boolean increaseSliderValue() {
        int oldValue = player().internalSecurity();
        player().increaseInternalSecurity();
        return oldValue != player().internalSecurity();
    }
    private boolean decreaseSliderValue() {
        int oldValue = player().internalSecurity();
        player().decreaseInternalSecurity();
        return oldValue != player().internalSecurity();
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int count = e.getUnitsToScroll();
        if ((hoverShape == incidentListBox)
        || (hoverShape == incidentScroller)) {
            int prevY = incidentY;
            if (count < 0)
                incidentY = max(0,incidentY-s10);
            else 
                incidentY = min(incidentYMax,incidentY+s10);
            if (incidentY != prevY)
                repaint(incidentListBox);
            return;
        }
        else if (hoverShape == buttonSlider) {
            boolean changed = count < 0 ? increaseSliderValue() : decreaseSliderValue();
            if (changed) {
                setValues();
                repaint();
            }
        }
        else if ((hoverShape == relationsListBox)
        || (hoverShape == relationsScroller)) {
            int prevY = relationsY;
            if (count < 0)
                relationsY = max(0,relationsY-s10);
            else 
                relationsY = min(relationsYMax,relationsY+s10);
            if (relationsY != prevY)
                repaint(relationsListBox);
            return;
        }
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int dY = y-dragY;
        dragY = y;
        if (incidentScroller == hoverShape) {
            if ((y >= incidentListBox.y) || (y <= (incidentListBox.y+incidentListBox.height))) { 
                int h = (int) incidentListBox.getHeight();
                int dListY = (int)((float)dY*(h+incidentYMax)/h);
                if (dY < 0)
                    incidentY = max(0,incidentY+dListY);
                else 
                    incidentY = min(incidentYMax,incidentY+dListY);
            }
            repaint(incidentListBox);
            return;
        }
        else if (incidentListBox == hoverShape) {
            if (incidentListBox.contains(x,y)) { 
                int h = (int) incidentListBox.getHeight();
                int dListY = (int)(-(float)dY*(h+incidentYMax)/h);
                if (dListY < 0)
                    incidentY = max(0,incidentY+dListY);
                else 
                    incidentY = min(incidentYMax,incidentY+dListY);
            }
            repaint(incidentListBox);
            return;
        }
        else if (relationsScroller == hoverShape) {
            if ((y >= relationsListBox.y) || (y <= (relationsListBox.y+relationsListBox.height))) { 
                int h = (int) relationsListBox.getHeight();
                int dListY = (int)((float)dY*(h+relationsYMax)/h);
                if (dY < 0)
                    relationsY = max(0,relationsY+dListY);
                else 
                    relationsY = min(relationsYMax,relationsY+dListY);
            }
            repaint(relationsListBox);
            return;
        }
        else if (relationsListBox == hoverShape) {
            if (relationsListBox.contains(x,y)) { 
                int h = (int) relationsListBox.getHeight();
                int dListY = (int)(-(float)dY*(h+relationsYMax)/h);
                if (dListY < 0)
                    relationsY = max(0,relationsY+dListY);
                else 
                    relationsY = min(relationsYMax,relationsY+dListY);
            }
            repaint(relationsListBox);
            return;
        }
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Shape prevHover = hoverShape;
        hoverShape = null;
        if (buttonIncr.contains(x,y))
            hoverShape = buttonIncr;
        else if (buttonDecr.contains(x,y))
            hoverShape = buttonDecr;
        else if (buttonSlider.contains(x,y))
            hoverShape = buttonSlider;
        else if (embassyBox.contains(x,y))
            hoverShape = embassyBox;
        else if (incidentScroller.contains(x,y))
            hoverShape = incidentScroller;
        else if (incidentListBox.contains(x,y))
            hoverShape = incidentListBox;
        else if (relationsScroller.contains(x,y))
            hoverShape = relationsScroller;
        else if (relationsListBox.contains(x,y))
            hoverShape = relationsListBox;
        else if (manageSpiesBox.contains(x,y))
            hoverShape = manageSpiesBox;
        else if (manageDiplomatsBox.contains(x,y))
            hoverShape = manageDiplomatsBox;

        if (hoverShape != prevHover) 
            repaint();
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) {
        dragY = e.getY();
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        dragY = 0;
        if (e.getButton() > 3)
            return;
        if (hoverShape == null)
            return;
        Empire emp = parent.selectedEmpire();
        if (hoverShape == embassyBox) {
            openEmbassy();
            return;
        }
        else if (hoverShape == manageSpiesBox) {
            openManageSpiesPane();
            return;
        }
        else if (hoverShape == manageDiplomatsBox) {
            openManageDiplomatsPane();
            return;
        }
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
            boolean changed = false;
            if (emp.isPlayer()) {
                int oldSec = emp.internalSecurity();
                emp.securityAllocation(pct);
                changed = oldSec != emp.internalSecurity();
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
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {
        if (hoverShape != null) {
            hoverShape = null;
            repaint();
        }
    }
}