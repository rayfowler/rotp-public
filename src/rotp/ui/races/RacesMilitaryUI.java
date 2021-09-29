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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.ShipView;
import rotp.model.empires.SpyNetwork;
import rotp.model.ships.ShipDesign;
import rotp.model.tech.TechTree;
import rotp.ui.BasePanel;
import rotp.ui.UserPreferences;
import rotp.ui.main.SystemPanel;

public final class RacesMilitaryUI extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    private final Color brownDividerC = new Color(136,115,96);

    private final RacesUI parent;
    private LinearGradientPaint backGradient;
    private List<ShipView> ships;
    int dragY;
    int shipY, shipYMax;
    private final Rectangle shipListBox = new Rectangle();
    private final Rectangle shipScroller = new Rectangle();
    public static BufferedImage shipIconBackImg;
    private final RacesShipRenameUI renameShipUI;

    private final Map<ShipDesign,Rectangle> renameBoxes = new HashMap<>();
    private Rectangle hoverNameBox;
    private Shape hoverShape;
    private final Polygon maxBasesIncr = new Polygon();
    private final Polygon maxBasesDecr = new Polygon();
    private final Rectangle maxBasesBox = new Rectangle();
    int[] ptX = new int[3];
    int[] ptY = new int[3];

    public RacesMilitaryUI(RacesUI p) {
        parent = p;
        renameShipUI = new RacesShipRenameUI();
        initModel();
    }
    @Override
    public void drawTexture(Graphics g)      { }
    @Override
    public String textureName()     { return TEXTURE_BROWN; }
    public void changedEmpire()     { 
        renameBoxes.clear();      
        ships = null;
        shipY = 0;
    }
    private List<ShipView> ships() {
        if (ships == null) {
            Empire emp = parent.selectedEmpire();
            if (emp.isPlayer())
                ships = player().shipLab().designHistory();
            else
                ships = player().viewForEmpire(emp).spies().ships();
        }
        return ships;
    }
    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        int w = getWidth();
        int h = getHeight();
        // draw the gradient background for the header row

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
    public BufferedImage shipIconBackImg() {
        if (shipIconBackImg == null) {
            int w = scaled(100);
            shipIconBackImg = newStarBackground(this,w,w);
        }
        return shipIconBackImg;
    }
    private void paintPlayerData(Graphics2D g) {
        Empire emp = parent.selectedEmpire();
        int w = getWidth();
        int h = getHeight();

        int s200 = scaled(200);
        int s210 = scaled(210);
        int s220 = scaled(220);
        int s260 = scaled(260);
        int s265 = scaled(265);
        int baseW = scaled(290);
        int defX = w-scaled(275);
        int defW = scaled(255);
        drawRaceIconBase(g, emp, s55, s25, s210, s210);
        drawPlayerBaseInfo(g, emp, s260, s80, baseW, scaled(130));
        drawPlayerDefenseInfo(g, emp, defX, s80, defW, scaled(140));
        drawShipDesignTitle(g, emp, s20, s220, w-s20-s20, s40);
        drawShipDesignListing(g, emp, s20, s265, w-s20-s20, h-s265-s10);
        if (UserPreferences.texturesInterface()) 
            drawTexture(g,0,0,w,h);
        drawRaceIcon(g, emp, s60, s30, s200, s200);
        drawFleetTitle(g, emp, s260, s30, baseW, s50);
        drawDefenseTitle(g, emp, defX, s30, defW, s50);
    }
    private void paintAIData(Graphics2D g) {
        Empire emp = parent.selectedEmpire();
        int w = getWidth();
        int h = getHeight();

        int s200 = scaled(200);
        int s210 = scaled(210);
        int s220 = scaled(220);
        int s260 = scaled(260);
        int s265 = scaled(265);
        int baseW = scaled(290);
        int defX = w-scaled(275);
        int defW = scaled(255);
        drawRaceIconBase(g, emp, s55, s25, s210, s210);
        drawAIBaseInfo(g, emp, s260, s80, baseW, scaled(130));
        drawAIDefenseInfo(g, emp, defX, s80, defW, scaled(140));
        drawShipDesignTitle(g, emp, s20, s220, w-s20-s20, s40);
        drawShipDesignListing(g, emp, s20, s265, w-s20-s20, h-s265-s10);
        if (UserPreferences.texturesInterface()) 
            drawTexture(g,0,0,w,h);
        drawRaceIcon(g, emp, s60, s30, s200, s200);
        drawFleetTitle(g, emp, s260, s30, baseW, s50);
        drawDefenseTitle(g, emp, defX, s30, defW, s50);
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
    private void drawFleetTitle(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(SystemPanel.orangeText);
        g.setFont(narrowFont(32));
        String title = text("RACES_MILITARY_TITLE");
        title = emp.replaceTokens(title, "alien");
        drawString(g,title, x+s10, y+h-s15);
    }
    private void drawDefenseTitle(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(SystemPanel.orangeText);
        g.setFont(narrowFont(32));
        drawString(g,text("RACES_MILITARY_DEFENSE"), x+s10, y+h-s15);
    }
    private void drawPlayerBaseInfo(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);
        int x0 = x+s20;
        int y0 = y + s26;
        String title2 = text("RACES_MILITARY_SUBTITLE");
        g.setFont(narrowFont(20));
        drawShadowedString(g, title2, 1, x0, y0, SystemPanel.blackText, Color.white);

        Empire pl = player();
        int leftM = 0;
        int rightM = s40;
        int centerSpace = s30;
        y0 += s15;
        int y1 = y0+s25;
        int x1 = x0 + leftM;
        int boxW = (w-leftM-rightM-centerSpace) / 2;
        int x2 = x1 + boxW + centerSpace;
        drawSizeBox(g, text("RACES_MILITARY_SMALL"),  pl.shipCount(ShipDesign.SMALL),  x1, y0, boxW, s25, false);
        drawSizeBox(g, text("RACES_MILITARY_LARGE"),  pl.shipCount(ShipDesign.LARGE),  x2, y0, boxW, s25, false);
        drawSizeBox(g, text("RACES_MILITARY_MEDIUM"), pl.shipCount(ShipDesign.MEDIUM), x1, y1, boxW, s25, false);
        drawSizeBox(g, text("RACES_MILITARY_HUGE"),   pl.shipCount(ShipDesign.HUGE),   x2, y1, boxW, s25, false); 
    }
    private void drawAIBaseInfo(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);
        int x0 = x+s20;
        int y0 = y + s26;
        String title2 = text("RACES_MILITARY_SUBTITLE");
        g.setFont(narrowFont(20));
        drawShadowedString(g, title2, 1, x0, y0, SystemPanel.blackText, Color.white);

        Empire pl = player();
        EmpireView view = pl.viewForEmpire(emp);
        SpyNetwork.FleetView fv = view.spies().fleetView();

        int leftM = 0;
        int rightM = s40;
        int centerSpace = s30;
        y0 += s15;
        int y1 = y0+s25;
        int x1 = x0 + leftM;
        int boxW = (w-leftM-rightM-centerSpace) / 2;
        int x2 = x1 + boxW + centerSpace;
        drawSizeBox(g, text("RACES_MILITARY_SMALL"),  fv.small(),  x1, y0, boxW, s25, fv.noReport());
        drawSizeBox(g, text("RACES_MILITARY_LARGE"),  fv.large(),  x2, y0, boxW, s25, fv.noReport());
        drawSizeBox(g, text("RACES_MILITARY_MEDIUM"), fv.medium(), x1, y1, boxW, s25, fv.noReport());
        drawSizeBox(g, text("RACES_MILITARY_HUGE"),   fv.huge(),   x2, y1, boxW, s25, fv.noReport()); 

        int y2 = y+h-s10;
        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(16));
        int age = fv.reportAge();
        if (fv.noReport())
            drawString(g,text("RACES_MILITARY_REPORT_NONE"), x0, y2);
        else if (age == 0)
            drawString(g,text("RACES_MILITARY_REPORT_CURRENT"), x0, y2);
        else
            drawString(g,text("RACES_MILITARY_REPORT_OLD", str(age)), x0, y2);
    }
    private void drawPlayerDefenseInfo(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h-s40);
        int y0 = y + s20;

        Empire pl = player();
        TechTree tech = pl.tech();
        int x0 = x + s10;
        
        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(16));
        drawString(g,text("RACES_MILITARY_DEF_PLANET_SHIELD"), x0, y0);
        g.setFont(narrowFont(15));
        int shieldLvl = tech.topPlanetaryShieldTech() == null ? 0 : tech.topPlanetaryShieldTech().damage;
        String shieldLvlStr = shieldLvl > 0 ? str(shieldLvl) : text("RACES_MILITARY_NO_SHIELD");
        int sw = g.getFontMetrics().stringWidth(shieldLvlStr);
        drawString(g,shieldLvlStr, x+w-s10-sw, y0);
            
        y0 += s17;
        g.setFont(narrowFont(16));
        drawString(g,text("RACES_MILITARY_DEF_DEFL_SHIELD"), x0, y0);
        g.setFont(narrowFont(15));
        shieldLvl = tech.topDeflectorShieldTech() == null ? 0 : tech.topDeflectorShieldTech().damage;
        shieldLvlStr = shieldLvl > 0 ? str(shieldLvl) : text("RACES_MILITARY_NO_SHIELD");
        sw = g.getFontMetrics().stringWidth(shieldLvlStr);
        drawString(g,shieldLvlStr, x+w-s10-sw, y0);
        
        y0 += s17;
        g.setFont(narrowFont(16));
        drawString(g,text("RACES_MILITARY_DEF_ARMOR"), x0, y0);
        g.setFont(narrowFont(15));
        String armor = tech.topArmorTech() == null ? "" : tech.topArmorTech().shortName();
        sw = g.getFontMetrics().stringWidth(armor);
        drawString(g,armor, x+w-s10-sw, y0);
        
        y0 += s17;
        g.setFont(narrowFont(16));
        drawString(g,text("RACES_MILITARY_DEF_MISSILE"), x0, y0);
        g.setFont(narrowFont(15));
        String miss = tech.topBaseMissileTech() == null ? "" : tech.topBaseMissileTech().name();
        sw = g.getFontMetrics().stringWidth(miss);
        drawString(g,miss, x+w-s10-sw, y0);
        
        y0 += s17;
        g.setFont(narrowFont(16));
        drawString(g,text("RACES_MILITARY_TROOP_BONUS"), x0, y0);
        g.setFont(narrowFont(15));
        String bonus = concat("+",str((int)tech.troopCombatAdj(false)));
        sw = g.getFontMetrics().stringWidth(bonus);
        drawString(g,bonus, x+w-s10-sw, y0);      
        
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y+h-s35, w, s35);
        
        y0 += s32;        
        int y1 = y0+s8;
        int x1 = x0+s35;
        String maxBaseStr = text("RACES_MILITARY_DEF_MISSILE_MAX");
        List<String> maxBaseLines = wrappedLines(g, maxBaseStr, w-s60);
        for (String line: maxBaseLines) {
            drawShadowedString(g, line, x0+s40, y0, Color.black, SystemPanel.whiteText);
            y0 += s16;
        }
        
        // draw arrows to incr/decr default missile bases
        g.setFont(narrowFont(20));
        g.setColor(SystemPanel.blackText);
        String numBases = str(player().defaultMaxBases());
        int sw1 = g.getFontMetrics().stringWidth(numBases);
        drawString(g,numBases, x0+s18-sw1, y1);
        
        
        int sz = s10;
        // draw incr button
        ptX[0]=x1-s3-sz; ptX[1]=ptX[0]+sz; ptX[2]=ptX[0]+sz/2;
        ptY[0]=y1-s9;  ptY[1]=ptY[0];    ptY[2]=y1-s16;
        maxBasesIncr.reset();
        for (int i=0;i<ptX.length;i++) 
            maxBasesIncr.addPoint(ptX[i], ptY[i]);
        Color buttonC = hoverShape == maxBasesIncr ? SystemPanel.yellowText :  Color.black;
        drawShadedPolygon(g, ptX, ptY, buttonC, s1, -s1);

        // draw lower decr button
        ptX[0]=x1-s3-sz; ptX[1]=ptX[0]+sz; ptX[2]=ptX[0]+sz/2;
        ptY[0]=y1-s6;    ptY[1]=ptY[0];    ptY[2]=y1+s1;
        maxBasesDecr.reset();
        for (int i=0;i<ptX.length;i++) 
            maxBasesDecr.addPoint(ptX[i], ptY[i]);
        buttonC = hoverShape == maxBasesDecr ? SystemPanel.yellowText :  Color.black;
        drawShadedPolygon(g, ptX, ptY, buttonC, s1, -s1);

        maxBasesBox.setBounds(x1-s5-sz, y1-s18, sz+6, s20);
        if ((hoverShape == maxBasesBox) || (hoverShape == maxBasesDecr) || (hoverShape == maxBasesIncr)) {
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.setColor(SystemPanel.yellowText);
            g.draw(maxBasesBox);
            g.setStroke(prev);
        }       
    }
    private void drawAIDefenseInfo(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h-s40);
        int y0 = y + s20;

        Empire pl = player();
        EmpireView v = player().viewForEmpire(emp.id);
        if (v == null)
            return;
        TechTree tech = v.spies().tech();
        int x0 = x + s10;
        
        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(16));
        drawString(g,text("RACES_MILITARY_DEF_PLANET_SHIELD"), x0, y0);
        g.setFont(narrowFont(15));
        int shieldLvl = tech.topPlanetaryShieldTech() == null ? 0 : tech.topPlanetaryShieldTech().damage;
        String shieldLvlStr = shieldLvl > 0 ? str(shieldLvl) : text("RACES_MILITARY_NO_SHIELD");
        int sw = g.getFontMetrics().stringWidth(shieldLvlStr);
        drawString(g,shieldLvlStr, x+w-s10-sw, y0);
            
        y0 += s17;
        g.setFont(narrowFont(16));
        drawString(g,text("RACES_MILITARY_DEF_DEFL_SHIELD"), x0, y0);
        g.setFont(narrowFont(15));
        shieldLvl = tech.topDeflectorShieldTech() == null ? 0 : tech.topDeflectorShieldTech().damage;
        shieldLvlStr = shieldLvl > 0 ? str(shieldLvl) : text("RACES_MILITARY_NO_SHIELD");
        sw = g.getFontMetrics().stringWidth(shieldLvlStr);
        drawString(g,shieldLvlStr, x+w-s10-sw, y0);
        
        y0 += s17;
        g.setFont(narrowFont(16));
        drawString(g,text("RACES_MILITARY_DEF_ARMOR"), x0, y0);
        g.setFont(narrowFont(15));
        String armor = tech.topArmorTech() == null ? "" : tech.topArmorTech().shortName();
        sw = g.getFontMetrics().stringWidth(armor);
        drawString(g,armor, x+w-s10-sw, y0);
        
        y0 += s17;
        g.setFont(narrowFont(16));
        drawString(g,text("RACES_MILITARY_DEF_MISSILE"), x0, y0);
        g.setFont(narrowFont(15));
        String miss = tech.topBaseMissileTech() == null ? "" : tech.topBaseMissileTech().name();
        sw = g.getFontMetrics().stringWidth(miss);
        drawString(g,miss, x+w-s10-sw, y0);
        
        y0 += s17;
        g.setFont(narrowFont(16));
        drawString(g,text("RACES_MILITARY_TROOP_BONUS"), x0, y0);
        g.setFont(narrowFont(15));
        String bonus = concat("+",str((int)tech.troopCombatAdj(false)));
        sw = g.getFontMetrics().stringWidth(bonus);
        drawString(g,bonus, x+w-s10-sw, y0);
    }
    private void drawShipDesignTitle(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        int y0 = y+h-s5;
        // these x values should match those in method drawShipDesign(
        int x1 = x+scaled(245)+s10; 
        int x3 = x+scaled(550)+s10;
        int x4 = x+scaled(735)+s10;
        
        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(18));
        drawString(g,text("RACES_MILITARY_TACTICAL"), x1, y0);
        drawString(g,text("RACES_MILITARY_WEAPONS"), x3, y0);
        drawString(g,text("RACES_MILITARY_SPECIALS"), x4, y0);
    }
    private void drawShipDesignListing(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);
        
        for (Rectangle r: renameBoxes.values())
            r.setBounds(0,0,0,0);
        
        if (ships().isEmpty()) {
            drawNoShipData(g,emp,x,y,w,h);
            return;
        }
        
        int listH = h;
        int rowH = s80;
        int fullListH = ships().size()*rowH;
        shipListBox.setBounds(x,y,w,listH);
        shipYMax = max(0, fullListH-listH);
        
        List<ShipView> ships1 = ships();
        if (emp.isPlayer())
            Collections.sort(ships1, ShipView.VIEW_ACTIVE);
        int y1 = y-shipY;
        g.setClip(x,y+s1,w,h-s2);
        for (ShipView view: ships1) {
            drawShipDesign(g, emp, view, x, y1, w, rowH);
            y1 += rowH;
        }
        if (shipYMax == 0)
            shipScroller.setBounds(0,0,0,0);
        else {
            g.setColor(RacesUI.scrollBarC);
            int scrollW = s12;
            int scrollH = (int) ((float)listH*listH/(listH+shipYMax));
            int scrollX = x+w-scrollW-s4;
            int scrollY =(int) (y+ (float)listH*shipY/(shipYMax+listH));
            g.fillRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
            shipScroller.setBounds(scrollX, scrollY, scrollW, scrollH);
            if (hoverShape == shipScroller) {
                Stroke prev = g.getStroke();
                g.setColor(Color.yellow);
                g.setStroke(stroke2);
                g.drawRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
                g.setStroke(prev);
            }
        }
        g.setClip(null);
        if ((hoverShape == shipListBox) 
        && (fullListH > listH)) {
            g.setColor(Color.yellow);
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.draw(shipListBox);
            g.setStroke(prev);
        }
    }
    private void drawNoShipData(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        String str = text("RACES_MILITARY_NO_SHIPS");
        g.setFont(narrowFont(32));
        int sw = g.getFontMetrics().stringWidth(str);

        int x0 = x+((w-sw)/2);
        drawShadowedString(g, str, 2, x0, y+(h/4)-s15, Color.black, SystemPanel.whiteText);
    }
    private void  drawShipDesign(Graphics2D g, Empire emp, ShipView view, int x, int y, int w, int h) { 
        int x0 = x;
        int x1 = x+scaled(245);
        int x2 = x+scaled(395);
        int x3 = x+scaled(550);
        int x4 = x+scaled(735);
        int x5 = x+w-scaled(20);
        
        drawShipNameAndIcon(g, view, x0, y, x1-x0, h);
        drawShipTactical1(g, view, x1, y, x2-x1, h);
        drawShipTactical2(g, view, x2, y, x3-x2, h);
        drawShipWeapons(g, view, x3, y, x4-x3, h);
        drawShipSpecials(g, view, x4, y, x5-x4, h);
                
        Stroke prev = g.getStroke();
        g.setStroke(stroke1);
                g.setColor(brownDividerC);
        g.drawLine(x, y+h, x5, y+h);
        g.drawLine(x1, y, x1, y+h);
        g.drawLine(x2, y, x2, y+h);
        g.drawLine(x3, y, x3, y+h);
        g.drawLine(x4, y, x4, y+h);
        g.drawLine(x5, y, x5, y+h);
        g.setStroke(prev);       
    }
    private void  drawShipNameAndIcon(Graphics2D g, ShipView view, int x, int y, int w, int h) {     
        int x0 = x+w*11/20;
        int w0 = x+w-x0-s5;
        int y0 = y+s5;
        int h0 = h-s10;
        
        ShipDesign d = view.design();
        
        if (!renameBoxes.containsKey(d))
            renameBoxes.put(d, new Rectangle());
        Rectangle nameBox = renameBoxes.get(d);
        
        Color c0 = hoverNameBox == nameBox ? SystemPanel.yellowText : SystemPanel.whiteText;
        
        g.setFont(narrowFont(20));
        String s = d.name();
        scaledFont(g,s,w0-s10,20,10);
        int sw = g.getFontMetrics().stringWidth(s);
        int x1 = x+((x0-x-sw)/2);
        int y1 = y+(h/2)-s10;
        drawShadowedString(g, s, 1, x1, y1, SystemPanel.blackText, c0);
        
        nameBox.setBounds(x1-s5,y1-s18,sw+s10,s22);
        
        String status, status2 = null;
        if (view.empire().isPlayer()) {
            if (d.scrapped()) 
                status = text("RACES_MILITARY_INACTIVE");
            else
                status = text("RACES_MILITARY_ACTIVE");
        }
        else {
            int age = galaxy().currentYear() - view.lastViewDate();
            if (age < 1) 
                status = text("RACES_MILITARY_ACTIVE");
            else {
                status = text("RACES_MILITARY_LAST_SEEN");
                status2 = text("RACES_MILITARY_SCAN_AGE", str(age));               
            }
        }
        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(16));
        sw = g.getFontMetrics().stringWidth(status);
        x1 = x+((x0-x-sw)/2);
        y1 += s20;
        drawString(g,status, x1, y1);
        
        if (status2 != null) {
            g.setFont(narrowFont(15));
            sw = g.getFontMetrics().stringWidth(status2);
            x1 = x+((x0-x-sw)/2);
            y1 += s16;
            drawString(g,status2, x1, y1);          
        }
        drawShipIcon(g, view, x0, y0, w0, h0); 
    }
    private void  drawShipIcon(Graphics2D g, ShipView view, int x, int y, int w, int h) { 
        g.setColor(Color.black);
        RoundRectangle2D roundRect = new RoundRectangle2D.Float(x,y,w,h,s20,s20);
        g.fill(roundRect);

        Rectangle rect = new Rectangle(x,y,w,h).intersection(shipListBox);
        Shape prevClip = g.getClip();
        g.setClip(rect);
        g.drawImage(shipIconBackImg(), x, y, this);
        g.setClip(prevClip);

        Image img = view.design().image();

        int w0 = img.getWidth(null);
        int h0 = img.getHeight(null);
        float scale = min((float)w/w0, (float)h/h0);

        int w1 = (int)(scale*w0);
        int h1 = (int)(scale*h0);

        int x1 = x+((w-w1)/2);
        int y1 = y+((h-h1)/2);
        g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, w0, h0, this);
    }
    private void  drawShipTactical1(Graphics2D g, ShipView view, int x, int y, int w, int h) { 
        String unk = text("RACES_MILITARY_UNSCANNED");

        ShipDesign d = view.design();
        g.setColor(SystemPanel.blackText);
        
        int lineH = s18;
        int x0 = x+s10;
        int y0 = y+lineH;
        int y1 = y0+lineH;
        int y2 = y1+lineH;
        int y3 = y2+lineH;
        g.setFont(narrowFont(16));
        String label = text("RACES_MILITARY_HULL");
        drawString(g,label, x0, y0);
        label = text("RACES_MILITARY_ARMOR");
        drawString(g,label,  x0, y1);
        label = text("RACES_MILITARY_SHIELD");
        drawString(g,label, x0, y2);
        label = text("RACES_MILITARY_TRAVEL_SPEED");
        drawString(g,label,  x0, y3);
             
        g.setFont(narrowFont(15));
        String val = d.sizeDesc();
        int sw = g.getFontMetrics().stringWidth(val);
        drawString(g,val, x+w-sw-s10, y0);
        val = view.armorKnown() ? str((int)d.hits()) : unk;
        sw = g.getFontMetrics().stringWidth(val);
        drawString(g,val, x+w-sw-s10, y1);
        val =  view.shieldKnown() ? str((int)d.shieldLevel()) : unk;
        sw = g.getFontMetrics().stringWidth(val);
        drawString(g,val, x+w-sw-s10, y2);
        val = str(d.warpSpeed());
        sw = g.getFontMetrics().stringWidth(val);
        drawString(g,val, x+w-sw-s10, y3);
    }
    private void  drawShipTactical2(Graphics2D g, ShipView view, int x, int y, int w, int h) { 
        String unk = text("RACES_MILITARY_UNSCANNED");
 
        ShipDesign d = view.design();
        g.setColor(SystemPanel.blackText);
        
        int lineH = s18;
        int x0 = x+s10;
        int y0 = y+lineH;
        int y1 = y0+lineH;
        int y2 = y1+lineH;
        int y3 = y2+lineH;
        g.setFont(narrowFont(16));
        String label = text("RACES_MILITARY_ATTACK_LEVEL");
        drawString(g,label,  x0, y0);
        label = text("RACES_MILITARY_MISSILE_DEFENSE");
        drawString(g,label,  x0, y1);
        label = text("RACES_MILITARY_BEAM_DEFENSE");
        drawString(g,label,  x0, y2);
        label = text("RACES_MILITARY_COMBAT_SPEED");
        drawString(g,label,  x0, y3);
             
        g.setFont(narrowFont(15));
        String val = view.computerKnown() ? str((int)d.attackLevel()) : unk;
        int sw = g.getFontMetrics().stringWidth(val);
        drawString(g,val, x+w-sw-s10, y0);
        val = view.maneuverKnown() ? str(d.missileDefense()) : unk;
        sw = g.getFontMetrics().stringWidth(val);
        drawString(g,val, x+w-sw-s10, y1);
        val = view.maneuverKnown() ? str(d.beamDefense()) : unk;
        sw = g.getFontMetrics().stringWidth(val);
        drawString(g,val, x+w-sw-s10, y2);
        val = view.maneuverKnown() ? str(d.combatSpeed()) : unk;
        sw = g.getFontMetrics().stringWidth(val);
        drawString(g,val, x+w-sw-s10, y3);

    }
    private void  drawShipWeapons(Graphics2D g, ShipView view, int x, int y, int w, int h) { 
        ShipDesign d = view.design();
        g.setFont(narrowFont(15));

        int lineH = s18;
        int y0 = y;
        int x0 = x+s10;
        g.setColor(SystemPanel.blackText);
        int totalWpnCount = 0;
        boolean wpnScanned = false;
        for (int i=0;i<ShipDesign.maxWeapons();i++) {
            wpnScanned = wpnScanned || view.weaponKnown(i);
            totalWpnCount += d.wpnCount(i);
        }
        
        y0 += lineH;
        if (!wpnScanned)
            drawString(g,text("RACES_MILITARY_UNSCANNED_LONG"), x0, y0);
        else if (totalWpnCount == 0)
            drawString(g,text("RACES_MILITARY_NO_WEAPONS"), x0, y0);
        else {
            for (int i=0;i<ShipDesign.maxWeapons();i++) {
                if (view.weaponKnown(i)) {
                    if (view.hasWeapon(i)) 
                        drawString(g,text("RACES_MILITARY_WEAPON_CNT", str(d.wpnCount(i)), d.weapon(i).name()), x0, y0);
                }
                y0 += lineH;
            }    
        }
    }
    private void  drawShipSpecials(Graphics2D g, ShipView view, int x, int y, int w, int h) { 
        ShipDesign d = view.design();
        g.setFont(narrowFont(15));

        int lineH = s18;
        int y0 = y;
        int x0 = x+s10;
        g.setColor(SystemPanel.blackText);
        for (int i=0;i<ShipDesign.maxSpecials();i++) {
            y0 += lineH;
            if (view.specialKnown(i)) {
                if (view.hasSpecial(i)) {
                    drawString(g,d.special(i).name(), x0, y0);
                }
                else if (i == 0)
                    drawString(g,text("RACES_MILITARY_NO_SPECIALS"), x0, y0);
            }
            else if (i == 0)
                drawString(g,text("RACES_MILITARY_UNSCANNED_LONG"), x0, y0);
        }   
    }
    private void drawSizeBox(Graphics g, String size, int count, int x, int y, int w, int h, boolean hideVal) {
        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(20));
        drawString(g,size, x, y+h-s10);

        if (!hideVal) {
            String c = str(count);
            int sw = g.getFontMetrics().stringWidth(c);
            drawString(g,c, x+w-sw, y+h-s10);
        }
    }
    private void openRenameDialog(ShipDesign d) {
        renameShipUI.targetDesign(d);
        enableGlassPane(renameShipUI);
        return;
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int dY = y-dragY;
        dragY = y;
        if (shipScroller == hoverShape) {
            if ((y >= shipListBox.y) || (y <= (shipListBox.y+shipListBox.height))) { 
                int h = (int) shipListBox.getHeight();
                int dListY = (int)((float)dY*(h+shipYMax)/h);
                if (dListY < 0)
                    shipY = max(0,shipY+dListY);
                else 
                    shipY = min(shipYMax,shipY+dListY);
            }
            repaint(shipListBox);
            return;
        }
        else if (shipListBox == hoverShape) {
            if (shipListBox.contains(x,y)) { 
                int h = (int) shipListBox.getHeight();
                int dListY = (int)(-(float)dY*(h+shipYMax)/h);
                if (dListY < 0)
                    shipY = max(0,shipY+dListY);
                else 
                    shipY = min(shipYMax,shipY+dListY);
            }
            repaint(shipListBox);
            return;
        }
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Shape prevHover = hoverShape;
        Rectangle prevNameBox = hoverNameBox;
        hoverShape = null;
        hoverNameBox = null;
        if (maxBasesIncr.contains(x,y))
            hoverShape = maxBasesIncr;
        else if (maxBasesDecr.contains(x,y))
            hoverShape = maxBasesDecr;
        else if (maxBasesBox.contains(x,y))
            hoverShape = maxBasesBox;
        else if (shipScroller.contains(x,y))
            hoverShape = shipScroller;
        else if (shipListBox.contains(x,y))
            hoverShape = shipListBox;

        for (Rectangle r: renameBoxes.values()) {
            if (r.contains(x,y)) {
                hoverNameBox = r;
                break;
            }
        }
        if ((hoverShape != prevHover) 
        || (hoverNameBox != prevNameBox))
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
        if (hoverShape == maxBasesDecr) {
            if (player().decrDefaultMaxBases())
               repaint();
            return;
        }
        else if (hoverShape == maxBasesIncr) {
            if (player().incrDefaultMaxBases())
                repaint();
            return;
        }
        else if (hoverNameBox != null) {
            for (ShipDesign des: renameBoxes.keySet()) {
                if (renameBoxes.get(des) == hoverNameBox) {
                    openRenameDialog(des);
                    break;
                }
            }
        }

    }
    
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {
        if (hoverShape != null) {
            hoverShape = null;
            hoverNameBox = null;
            repaint();
        }
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int count = e.getUnitsToScroll();
        int x = e.getX();
        int y = e.getY();
        if ((hoverShape == shipListBox)
        || (hoverShape == shipScroller)) {
            int prevY = shipY;
            if (count < 0)
                shipY = max(0,shipY-s10);
            else 
                shipY = min(shipYMax,shipY+s10);
            if (shipY != prevY)
                repaint(shipListBox);
            return;
        }
        else if (maxBasesBox.contains(x,y)) {
            boolean repaint;
            if (count < 0)
                repaint = player().incrDefaultMaxBases();
            else
                repaint = player().decrDefaultMaxBases();
            if (repaint)
                repaint();
        }
    }
}