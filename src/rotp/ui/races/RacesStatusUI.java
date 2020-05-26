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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
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
import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireStatus;
import rotp.ui.BasePanel;
import rotp.ui.UserPreferences;
import rotp.ui.main.SystemPanel;

public final class RacesStatusUI extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    static Color sliderC = new Color(34,140,142);
    static Color sliderButtonC = new Color(153,0,11);
    static Color sliderButtonHiC = new Color(199,199,11);
    static final Color sliderBoxBlue = new Color(34,140,142);
    static Stroke dashedLineStroke;

    private final RacesUI parent;
    Shape hoverShape;
    int dragY;
    List<RaceValue> vals = new ArrayList<>();

    int[] dataY = new int[6];
    int[] dataYMax = new int[6];
    Rectangle[] fullBoxes = new Rectangle[6];
    Rectangle[] dataBoxes = new Rectangle[6];
    Rectangle[] dataScrollers = new Rectangle[6];
    
    private LinearGradientPaint backGradient;
    public RacesStatusUI(RacesUI p) {
        parent = p;
        dashedLineStroke = new BasicStroke(s2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3}, 0);
        initModel();
    }
    public void init() {
        for (int i=0; i<dataY.length; i++)
            dataY[i] = 0;
    }
    @Override
    public void drawTexture(Graphics g)      { }
    @Override
    public String textureName()     { return TEXTURE_BROWN; }
    public void changedEmpire()     { init(); }
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
        for (int i=0;i<fullBoxes.length;i++) 
            fullBoxes[i] = new Rectangle();
        for (int i=0;i<dataBoxes.length;i++) 
            dataBoxes[i] = new Rectangle();
        for (int i=0;i<dataScrollers.length;i++) 
            dataScrollers[i] = new Rectangle();
        
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
        int s260 = scaled(260);
        int s370 = scaled(370);
        drawRaceIconBase(g, emp, s55, s25, s210, s210);
        drawRaceIconBase(g, emp, w-s65-s200, s25, s210, s210);
        drawAllRankingsLists(g, emp, s55, s260, w-s55-s55, h-s260-s10);
        if (UserPreferences.textures()) 
            drawTexture(g,0,0,w,h);
        drawRaceIcon(g, emp, s60, s30, s200, s200);
        drawOpponentIcon(g, null, w-s60-s200, s30, s200, s200);
        drawPlayerTitle(g, emp, s260, s30, s370, s50);
        drawVS(g, emp, s260, s90, s370, s50);
        drawKnownEmpiresTitle(g, emp, w-s60-s200, s30+s200, 0, s50);
    }
    private void paintAIData(Graphics2D g) {
        Empire emp = parent.selectedEmpire();
        int w = getWidth();
        int h = getHeight();

        int s200 = scaled(200);
        int s210 = scaled(210);
        int s260 = scaled(260);
        int s370 = scaled(370);
        drawRaceIconBase(g, emp, s55, s25, s210, s210);
        drawRaceIconBase(g, emp, w-s65-s200, s25, s210, s210);
        drawVsRankingsLists(g, emp, s55, s260, w-s55-s55, h-s260-s10);
        if (UserPreferences.textures()) 
            drawTexture(g,0,0,w,h);
        drawRaceIcon(g, emp, s60, s30, s200, s200);
        drawOpponentIcon(g, emp, w-s60-s200, s30, s200, s200);
        drawPlayerTitle(g, emp, s260, s30, s370, s50);
        drawVS(g, emp, s260, s90, s370, s50);
        drawAITitle(g, emp, w-s60-s200, s30+s200, 0, s50);
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

        Image img = player().race().flagPact();
        int imgH = img.getHeight(null);
        int imgW = img.getWidth(null);
        g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, imgW, imgH, null);
    }
    private void drawOpponentIcon(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(Color.black);
        Shape rect = new RoundRectangle2D.Float(x,y,w,h,w/8, h/8);
        g.fill(rect);
        
        BufferedImage backImg = parent.raceIconBackImg();
        g.drawImage(backImg, x,y, null);
        
        if (emp == null)
            return;
        
        int x1 = x + w/10;
        int w1 = w * 8/10;
        int y1 = y + h/10;
        int h1 = h * 8/10;

        Image img = player().viewForEmpire(emp).flag();
        int imgH = img.getHeight(null);
        int imgW = img.getWidth(null);
        g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, imgW, imgH, null);
    }
    private void drawPlayerTitle(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(SystemPanel.orangeText);
        g.setFont(narrowFont(32));
        g.drawString(text("RACES_STATUS_THE_EMPIRE", player().name()), x+s10, y+h-s15);
    }
    private void drawVS(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(SystemPanel.orangeText);
        g.setFont(narrowFont(32));
        String vs = text("RACES_STATUS_VS");
        int sw = g.getFontMetrics().stringWidth(vs);
        int x0 = x+(w-sw)/2;
        g.drawString(vs, x0, y+h-s2);
    }
    private void drawKnownEmpiresTitle(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(SystemPanel.orangeText);
        g.setFont(narrowFont(32));
        String title = text("RACES_STATUS_KNOWN_EMPIRES", player().contacts().size());
        int sw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, x-sw-s10, y-s15);
    }
    private void drawAITitle(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(SystemPanel.orangeText);
        g.setFont(narrowFont(32));
        String title = text("RACES_STATUS_THE_EMPIRE", emp.name());
        int sw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, x-sw-s10, y-s15);
    }
    private void drawAllRankingsLists(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);
        int w1 = (w-s20)/3;
        int h1 = h/2;
        
        int x1 = x+s10;
        int x2 = x1+w1;
        int x3 = x2+w1;
        int y1 = y;
        int y2 = y+h1;
        
        drawRankingPane(g, x1, y1, w1, h1, EmpireStatus.FLEET);
        drawRankingPane(g, x2, y1, w1, h1, EmpireStatus.POPULATION);
        drawRankingPane(g, x3, y1, w1, h1, EmpireStatus.TECHNOLOGY);
        drawRankingPane(g, x1, y2, w1, h1, EmpireStatus.PLANETS);
        drawRankingPane(g, x2, y2, w1, h1, EmpireStatus.PRODUCTION);
        drawRankingPane(g, x3, y2, w1, h1, EmpireStatus.POWER);
    }
    private void drawRankingPane(Graphics2D g, int x, int y, int w, int h, int num) {
        int numDisplayEmps = bounds(4, galaxy().numActiveEmpires(), 8);
        
        g.setFont(font(16));
        int maxSW =  g.getFontMetrics().stringWidth(player().raceName());
        for (Empire emp: player().contactedEmpires()) {
            maxSW = max(maxSW, g.getFontMetrics().stringWidth(emp.raceName()));
        }
        int mgnL = maxSW+s5;
        int mgnR = s5;
        int mgnT = s40;
        int mgnB = s10;

        g.setFont(font(20));
        String title = player().status().title(num);
        int sw = g.getFontMetrics().stringWidth(title);
        int x0 = x+mgnL+(w-sw-mgnL)/2;
        int y0 = y+s35;
        drawShadowedString(g, title, 1, x0, y0, SystemPanel.blackText, SystemPanel.whiteText);

        int x1 = x+mgnL;
        int w1 = w-mgnL-mgnR;
        int y1 = y+mgnT;
        int listH = h-mgnT-mgnB;

        // recalculate h1 so bars fit in evenly
        int vSpacing = s5;
        int tSpacing = s5;
        int bSpacing = s5;
        int lSpacing = s5;
        int rSpacing = s40;
        int barH = (listH-((numDisplayEmps-1)*vSpacing)-tSpacing-bSpacing)/numDisplayEmps;
        listH = (barH*numDisplayEmps)+((numDisplayEmps-1)*vSpacing)+tSpacing+bSpacing;
        int yAdj = (barH-s10)/2;

        g.setColor(RacesUI.darkerBrown);
        g.fillRect(x1, y1, w1, listH);
        fullBoxes[num].setBounds(x1-s5-maxSW,y1,w1+s5+maxSW,listH);
        dataBoxes[num].setBounds(x1,y1,w1,listH);
        if (dataBoxes[num] == hoverShape) {
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.setColor(Color.yellow);
            g.draw(dataBoxes[num]);
            g.setStroke(prev);
        }
             
        getEmpireListing(num);

        float maxValue = vals.get(0).value;
        float sumValues = 0;
        for (RaceValue rv: vals) {
            if (rv.value > 0)
                sumValues += rv.value;
        }
        int x2 = x1 + lSpacing;
        int y2 = y1 + tSpacing-dataY[num];
        int w2 = w1 - lSpacing - rSpacing;
        g.setFont(font(16));
        g.setClip(fullBoxes[num]);
        int rows = vals.size();
        for (RaceValue rv: vals) {
            if (rv.value < 0) {
                g.setColor(RacesUI.darkBrown);
                String s = text("RACES_STATUS_NO_DATA");
                g.drawString(s, x2, y2+barH-yAdj);
            }
            else {
                g.setColor(rv.emp.color());
                int barW = (int) (w2 * (rv.value / maxValue));
                g.fillRect(x2, y2, barW, barH);
                g.setColor(RacesUI.brown);
                float pct = (float)100*rv.value/sumValues;
                String val = pct >= 10 ? str(Math.round(pct)) : df1.format(pct); 
                g.drawString(val, x2+barW+s5, y2+barH-yAdj);
            }
            String name = rv.emp.raceName();
            int sw2 = g.getFontMetrics().stringWidth(name);
            g.setColor(SystemPanel.blackText);
            g.drawString(name, x1-s5-sw2, y2+barH-yAdj);
            y2 += (barH+vSpacing);
        }
        
        dataYMax[num] = max(0, s21+(barH*rows) - listH);
        if (dataYMax[num] == 0) 
            dataScrollers[num].setBounds(0,0,0,0);
        else {
            g.setColor(RacesUI.scrollBarC);
            int scrollW = s12;
            int scrollH = (int) ((float)listH*listH/(listH+dataYMax[num]));
            int scrollX = x1+w1-scrollW-s2;
            int scrollY =(int) (y1+ (float)listH*dataY[num]/(dataYMax[num]+listH));
            g.fillRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
            dataScrollers[num].setBounds(scrollX, scrollY, scrollW, scrollH);
            if (hoverShape == dataScrollers[num]) {
                Stroke prev = g.getStroke();
                g.setColor(Color.yellow);
                g.setStroke(stroke2);
                g.drawRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
                g.setStroke(prev);
            }
        }
        g.setClip(null);    
    }
    private void getEmpireListing(int cat) {
        vals.clear();
        for (Empire emp: player().contactedEmpires()) {
            int val = emp.status().lastViewValue(player(), cat);
            int age=  emp.status().age(player());
            vals.add(new RaceValue(emp, val, age));
        }
        int val = player().status().lastViewValue(player(), cat);
        vals.add(new RaceValue(player(), val, 0));
        Collections.sort(vals);
    }
    private void drawVsRankingsLists(Graphics2D g, Empire emp, int x, int y, int w, int h) {
        g.setColor(RacesUI.darkBrown);
        g.fillRect(x, y, w, h);
        int w1 = (w-s20)/3;
        int h1 = h/2;
        
        int x1 = x+s10;
        int x2 = x1+w1;
        int x3 = x2+w1;
        int y1 = y;
        int y2 = y+h1;
        
        drawHistoryPane(g, x1, y1, w1, h1, EmpireStatus.FLEET);
        drawHistoryPane(g, x2, y1, w1, h1, EmpireStatus.POPULATION);
        drawHistoryPane(g, x3, y1, w1, h1, EmpireStatus.TECHNOLOGY);
        drawHistoryPane(g, x1, y2, w1, h1, EmpireStatus.PLANETS);
        drawHistoryPane(g, x2, y2, w1, h1, EmpireStatus.PRODUCTION);
        drawHistoryPane(g, x3, y2, w1, h1, EmpireStatus.POWER);
    }
    private void drawHistoryPane(Graphics2D g, int x, int y, int w, int h, int cat) {
        int mgnL = s20;
        int mgnR = s20;
        int mgnT = s50;
        int mgnB = s10;

        g.setFont(font(20));
        String title = player().status().title(cat);
        int sw = g.getFontMetrics().stringWidth(title);
        int x0 = x+mgnL+(w-sw-mgnL)/2;
        int y0 = y+s35;
        drawShadowedString(g, title, 1, x0, y0, SystemPanel.blackText, SystemPanel.whiteText);

        int x1 = x+mgnL;
        int w1 = w-mgnL-mgnR;
        int y1 = y+mgnT;
        int h1 = h-mgnT-mgnB;

        // recalculate h1 so bars fit in evenly
        int tSpacing = s20;
        int bSpacing = s10;
        int lSpacing = s5;
        int rSpacing = s25;

        g.setColor(RacesUI.darkerBrown);
        g.fillRect(x1, y1, w1, h1);

        int[] playerVals = player().status().values(cat);
        int[] empireVals = parent.selectedEmpire().status().values(cat);
        int totalTurns = galaxy().numberTurns();
        int empireTurns = parent.selectedEmpire().status().lastViewTurn(player());
        int displayW = w1-rSpacing-lSpacing;
        int displayH = h1-tSpacing-bSpacing;

        // find maximum Y value to display
        int maxYValue = 0;
        for (int i=0;i<totalTurns;i++)
            maxYValue = Math.max(playerVals[i], maxYValue);
        for (int i=0;i<empireTurns;i++)
            maxYValue = Math.max(empireVals[i], maxYValue);

        int startX = x1+lSpacing;    // x-location for pt values in turn 0
        int startY = y1+h1-bSpacing; // y-location for pt values = 0
        int maxPtSpacing = s20;
        Stroke prevStroke = g.getStroke();
        g.setStroke(stroke2);
        g.setFont(font(18));

        // DRAW SELECTED EMPIRE
        g.setColor(parent.selectedEmpire().color());
        String name = parent.selectedEmpire().raceName();
        int nameW = g.getFontMetrics().stringWidth(name);
        g.drawString(name, startX+displayW+rSpacing-s5-nameW, startY+s5);
        int prevX = startX;
        int prevY = -1;
        for (int i=0;i<empireTurns;i++) {
            int ptX = startX+(displayW*i/totalTurns);
            if ((ptX - prevX) > maxPtSpacing)
                ptX = prevX + maxPtSpacing;
            int ptY = startY-(displayH*empireVals[i]/maxYValue);
            if (prevY >= 0)
                g.drawLine(prevX, prevY, ptX, ptY);
            prevX = ptX;
            prevY = ptY;
        }

        // DRAW PLAYER
        g.setColor(player().color());
        g.drawString(player().raceName(), startX, startY-displayH);
        prevX = startX;
        prevY = -1;
        for (int i=0;i<totalTurns;i++) {
            int ptX = startX+(displayW*i/totalTurns);
            if ((ptX - prevX) > maxPtSpacing)
                ptX = prevX + maxPtSpacing;
            int ptY = maxYValue == 0 ? 0 : startY-(displayH*playerVals[i]/maxYValue);
            if (prevY >= 0)
                g.drawLine(prevX, prevY, ptX, ptY);
            prevX = ptX;
            prevY = ptY;
        }

       if (empireTurns < 0) {
            g.setFont(narrowFont(24));
            g.setColor(RacesUI.darkBrown);
            String s = text("RACES_STATUS_NO_DATA");
            int sw1 = g.getFontMetrics().stringWidth(s);
            g.drawString(s, x1+(w1-sw1)/2, y1+(h1+s24)/2);            
        }
        // DRAW SELECTED EMPIRE AGAIN, with dashed lines
        g.setStroke(dashedLineStroke);
        g.setColor(parent.selectedEmpire().color());
        prevX = startX;
        prevY = -1;
        for (int i=0;i<empireTurns;i++) {
            int ptX = startX+(displayW*i/totalTurns);
            if ((ptX - prevX) > maxPtSpacing)
                ptX = prevX + maxPtSpacing;
            int ptY = maxYValue == 0 ? 0 : startY-(displayH*empireVals[i]/maxYValue);
            if (prevY >= 0)
                g.drawLine(prevX, prevY, ptX, ptY);
            prevX = ptX;
            prevY = ptY;
        }
        g.setStroke(prevStroke);
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int count = e.getUnitsToScroll();
        for (int i=0;i<dataBoxes.length;i++) {
            if ((hoverShape == dataBoxes[i])
            || (hoverShape == dataScrollers[i])) {
                int prevY = dataY[i];
                if (count < 0)
                    dataY[i] = max(0,dataY[i]-s10);
                else 
                    dataY[i] = min(dataYMax[i],dataY[i]+s10);
                if (dataY[i] != prevY)
                    repaint(fullBoxes[i]);
                return;
            }
        }
    }
    @Override
    public void mouseDragged(MouseEvent e) { 
        int x = e.getX();
        int y = e.getY();
        int dY = y-dragY;
        dragY = y;
        for (int i=0;i<dataBoxes.length;i++) {
            if (dataScrollers[i] == hoverShape) {
                if ((y >= dataBoxes[i].y) || (y <= (dataBoxes[i].y+dataBoxes[i].height))) { 
                    int h = (int) dataBoxes[i].getHeight();
                    int dListY = (int)((float)dY*(h+dataYMax[i])/h);
                    if (dY < 0)
                        dataY[i] = max(0,dataY[i]+dListY);
                    else 
                        dataY[i] = min(dataYMax[i],dataY[i]+dListY);
                }
                repaint(fullBoxes[i]);
                return;
            }
            else if (dataBoxes[i] == hoverShape) {
                if (dataBoxes[i].contains(x,y)) { 
                    int h = (int) dataBoxes[i].getHeight();
                    int dListY = (int)(-(float)dY*(h+dataYMax[i])/h);
                    if (dListY < 0)
                        dataY[i] = max(0,dataY[i]+dListY);
                    else 
                        dataY[i] = min(dataYMax[i],dataY[i]+dListY);
                }
                repaint(fullBoxes[i]);
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
        for (int i=0;i<dataBoxes.length;i++) {
            if (dataScrollers[i].contains(x,y)) 
                hoverShape = dataScrollers[i];
            else if (dataBoxes[i].contains(x,y)) 
                hoverShape = dataBoxes[i];
        }
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
    class RaceValue  implements Comparable<RaceValue> {
        int value = 0;
        int age = 0;
        Empire emp;
        public RaceValue(Empire e, int v, int a) {
            value = v;
            age  = a;
            emp = e;
        }
        @Override
        public int compareTo(RaceValue rv) {
            return value > rv.value ? -1 : value < rv.value ? +1 : 0;
        }
    }
}