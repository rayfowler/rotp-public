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
import java.awt.LinearGradientPaint;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.model.empires.EmpireView;
import rotp.ui.BasePanel;
import rotp.ui.main.SystemPanel;
import static rotp.ui.races.RacesIntelligenceUI.sliderBoxBlue;
import static rotp.ui.races.RacesIntelligenceUI.sliderC;

public class ManageSpiesUI extends BasePanel implements MouseListener, MouseWheelListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    private static final Color backgroundHaze = new Color(0,0,0,160);
    private static final Color okButtonBdrC = new Color(158,165,156);
    private static final Color borderC = new Color(112,85,68,128);
    private final Color brownDividerC = new Color(136,115,96);
    private static final Color greenEdgeC = new Color(44,59,30);
    private static final Color greenMidC = new Color(71,93,48);
    private static final Color unselectedC = new Color(112,85,68);

    final RacesUI parent;
    private Shape hoverBox, hoverButton;
    int dragY;
    int[] ptX = new int[3];
    int[] ptY = new int[3];
    private final Rectangle okButton = new Rectangle();
    final Rectangle listBox = new Rectangle();
    final Rectangle listScroller = new Rectangle();
    int listY, listYMax;
    int col1W, col2W, col3W, col4W;
    private LinearGradientPaint largeGreenBackC;
    Shape textureClip;
    List<Rectangle> spendingBoxes = new ArrayList<>();
    List<Rectangle> missionBoxes = new ArrayList<>();
    List<Polygon> spendingIncr = new ArrayList<>();
    List<Polygon> spendingDecr = new ArrayList<>();
    List<Polygon> missionIncr = new ArrayList<>();
    List<Polygon> missionDecr = new ArrayList<>();
    List<EmpireView> empireViews = new ArrayList<>();
    float totalPlanetaryProduction;

    public ManageSpiesUI(RacesUI p) {
        parent = p;
        initModel();
    }
    private void initModel() {
        setOpaque(false);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }
    public void init() {
        listY = 0;
        for (Rectangle r: spendingBoxes)
            r.setBounds(0,0,0,0);
        for (Rectangle r: missionBoxes)
            r.setBounds(0,0,0,0);
        for (Polygon p: spendingDecr)
            p.reset();
        for (Polygon p: spendingIncr)
            p.reset();
        for (Polygon p: missionDecr)
            p.reset();
        for (Polygon p: missionIncr)
            p.reset();
        totalPlanetaryProduction = player().totalTaxablePlanetaryProduction();
    }
    @Override
    public String textureName()     { return TEXTURE_BROWN; }
    @Override
    public Shape textureClip()     { return textureClip; }
    @Override
    public void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;

        super.paintComponent(g);

        int w = getWidth();
        int h = getHeight();
        
        if (col1W == 0) { // don't do this every repaint. c'mon man.
            List<EmpireView> allContacts = player().contacts();
            empireViews.clear();
            for (EmpireView v: allContacts) {
                if (!v.embassy().unity() && v.inEconomicRange())
                    empireViews.add(v);
            }
            Collections.sort(empireViews, EmpireView.BY_RACENAME);
            g.setFont(narrowFont(20));
            col1W = s60;
            for (EmpireView view: player().contacts()) 
                col1W = max(col1W, s30+g.getFontMetrics().stringWidth(view.empire().raceName()));       
            col2W = scaled(180);
            col3W = s70;
            col4W = scaled(120);
        }

        // draw background "haze"
        g.setColor(backgroundHaze);
        g.fillRect(0, 0, w, h);
        

        // get length of title and rows to determine box width
        g.setFont(narrowFont(24));
        String title = text("RACES_INTEL_BUREAU_TITLE");
        int titleSW = g.getFontMetrics().stringWidth(title);
        
        int scrollBoxW = s20;
        
        int maxDisplayRows = 9;
        int topM = s15;
        int bottomM = s30;
        int buttonH = s32;
        int titleH = s45;
        int rowH = s35;
        int numRows = min(maxDisplayRows, empireViews.size());
        int boxWidth = col1W+col2W+col3W+col4W+scaled(130);
        if (empireViews.size() > maxDisplayRows)
            boxWidth += scrollBoxW;
        int boxHeight = (rowH*numRows)+topM+bottomM+titleH+buttonH;

        int x0 = (w - boxWidth)/2;
        int y0 = h/3;

        // draw box
        g.setColor(borderC);
        g.fillRect(x0, y0, boxWidth, boxHeight);
        g.setColor(RacesUI.lightBrown);
        g.fillRect(x0+s15, y0+s15, boxWidth-s30, boxHeight-s30);

        textureClip = new Rectangle(x0+s15, y0+s15, boxWidth-s30, boxHeight-s30);

        // draw title
        int y1 = y0+s45;
        int x1 = (w-titleSW)/2;
        g.setFont(narrowFont(24));
        drawShadowedString(g, title, 3, x1, y1, SystemPanel.textShadowC, SystemPanel.whiteText);
        
        // fill data area
        int x2 = x0+s25;
        int y2 = y0+s55;
        int w2 = boxWidth-s50;
        int h2 = numRows*rowH;
        g.setColor(RacesUI.darkBrown);
        
        g.fillRect(x2, y2, w2, h2); 

        int rowx1 = x2;
        int rowx2 = rowx1+col1W+s20;
        int rowx3 = rowx2+col2W+s20;
        int rowx4 = rowx3+col3W+s20;
        int rowx5 = rowx4+col4W+s20;
        
        int listH = h2;
        int fullListH = empireViews.size()* rowH;
        
        g.setClip(x2,y2,w2, h2);
        int y3 = y2-listY;
        for (int i=0;i<empireViews.size();i++) {
            EmpireView view = empireViews.get(i);
            drawEmpireInformation(g, view, i, x2, y3, w2, rowH, rowx2, rowx3, rowx4, rowx5);
            y3 += rowH;
        }
        listBox.setBounds(x2,y2,w2,h2);
        listYMax = max(0, fullListH-listH);
        if (listYMax == 0)
            listScroller.setBounds(0,0,0,0);
        else {
            g.setColor(RacesUI.scrollBarC);
            int scrollW = scrollBoxW-s5;
            int scrollH = (int) ((float)listH*listH/(listH+listYMax));
            int scrollX = rowx5+s3;
            int scrollY =(int) (y2+ (float)listH*listY/(listYMax+listH));
            g.fillRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
            listScroller.setBounds(scrollX, scrollY, scrollW, scrollH);
            if (hoverBox == listScroller) {
                Stroke prev = g.getStroke();
                g.setColor(Color.yellow);
                g.setStroke(stroke2);
                g.drawRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
                g.setStroke(prev);
            }
        }
        g.setClip(null);
        
        // draw vertical lines
        g.setColor(brownDividerC);
        g.drawLine(rowx2, y2, rowx2, y2+h2);
        g.drawLine(rowx3, y2, rowx3, y2+h2);
        g.drawLine(rowx4, y2, rowx4, y2+h2);
        if (empireViews.size() > maxDisplayRows)
            g.drawLine(rowx5, y2, rowx5, y2+h2);     
        
        // OK button vars
        int buttonM = s30;  // L/R margin
        int buttonW = (boxWidth-buttonM-buttonM)/2;
        int buttonY = y0+boxHeight-buttonH-s25;
        int button1X = (w-buttonW)/2;
        g.setFont(narrowFont(20));

        // set up background gradients
        if (largeGreenBackC == null) {  // don't do this every repaint
            Point2D start1 = new Point2D.Float(button1X, 0);
            Point2D end1 = new Point2D.Float(button1X+buttonW, 0);
            float[] dist = {0.0f, 0.2f, 0.5f, 0.8f, 1.0f};
            Color[] greenColors = {greenEdgeC, greenEdgeC, greenMidC, greenEdgeC, greenEdgeC };
            largeGreenBackC = new LinearGradientPaint(start1, end1, dist, greenColors);
        }

        // ok button
        int cnr = s2;
        okButton.setBounds(button1X, buttonY, buttonW, buttonH);
        g.setColor(SystemPanel.textShadowC);
        g.fillRoundRect(button1X+s4, buttonY+s4, buttonW, buttonH, cnr, cnr);
        g.setPaint(largeGreenBackC);
        g.fillRoundRect(button1X, buttonY, buttonW, buttonH, cnr, cnr);
        Stroke prev = g.getStroke();
        g.setStroke(stroke1);
        if (hoverBox == okButton)
            g.setColor(Color.yellow);
        else
            g.setColor(okButtonBdrC);
        g.drawRoundRect(button1X, buttonY, buttonW, buttonH, cnr, cnr);
        g.setStroke(prev);
        Color c1 = hoverBox == okButton ? Color.yellow : SystemPanel.whiteText;
        String acceptText = text("RACES_MANAGE_DIALOG_OK");
        int sw2 = g.getFontMetrics().stringWidth(acceptText);
        int text2X = button1X+ ((buttonW - sw2) / 2);
        drawShadowedString(g0, acceptText, 3, text2X, buttonY+buttonH-s10, SystemPanel.textShadowC, c1);

    }
    private void  drawEmpireInformation(Graphics2D g, EmpireView view, int index, int x, int y, int w, int h, int x2, int x3, int x4, int x5) {
        int y1 = y+h-s12;
        
        if (spendingBoxes.size() <= index)
            spendingBoxes.add(new Rectangle());
        if (missionBoxes.size() <= index)
            missionBoxes.add(new Rectangle());
        
        g.setColor(SystemPanel.blackText);
        
        // race name in column 1
        g.setFont(narrowFont(20));
        String s = view.empire().raceName();
        int w1 = x2-x;
        int sw = g.getFontMetrics().stringWidth(s);
        drawString(g,s, x+(w1-sw)/2, y1);
        
        // spending slider in column 2
        int w2 = x3-x2;
        drawSpySpendingButton(g, view, index, x2+s5, y+s2, w2-s10, h-s17);

        
        // num spies button in column 3
        g.setFont(narrowFont(16));
        g.setColor(SystemPanel.blackText);
        int w3 = x4-x3;
        s = view.spies().numSpiesLabel();
        sw = g.getFontMetrics().stringWidth(s);
        drawString(g,s, x3+(w3-sw)/2, y1);
 
        // mission box in column 4
        int w4 = x5-x4;
        drawSpiesMissionButton(g, view, index, x4+s5, y+s2, w4-s10, h-s17);
        
        g.setColor(brownDividerC);
        g.drawLine(x, y+h, x+w-s20, y+h);
        
    }
    private void drawSpySpendingButton(Graphics2D g, EmpireView view ,int index, int x, int y, int w, int h) {
        if (spendingDecr.size() <= index)
            spendingDecr.add(new Polygon());
        if (spendingIncr.size() <= index)
            spendingIncr.add(new Polygon());
        if (spendingBoxes.size() <= index)
            spendingBoxes.add(new Rectangle());
       
        Polygon left = spendingDecr.get(index);
        Polygon right = spendingIncr.get(index);
        Rectangle box = spendingBoxes.get(index);
        float cost = totalPlanetaryProduction * view.spies().allocationCostPct()*view.owner().spySpendingModifier();
        
        int labelW = s60;
        int leftM = x;
        int rightM = x+w-labelW;
        int buttonW = s10;
        int buttonTopY = y+s5;
        int buttonMidY = y+s14;
        int buttonBotY = y+s23;

        //left arrow
        ptX[0] = leftM; ptX[1] = leftM+buttonW; ptX[2] = leftM+buttonW;
        ptY[0] = buttonMidY; ptY[1] = buttonTopY; ptY[2] = buttonBotY;
        left.reset();
        for (int i=0;i<ptX.length;i++) 
           left.addPoint(ptX[i], ptY[i]);
        if (hoverButton == left)
            g.setColor(SystemPanel.yellowText);
        else
            g.setColor(Color.black);
        g.fillPolygon(ptX, ptY, 3);
        
        // right arrow
        ptX[0] = rightM; ptX[1] = rightM-buttonW; ptX[2] = rightM-buttonW;
        ptY[0] = buttonMidY; ptY[1] = buttonTopY; ptY[2] = buttonBotY;
        right.reset();
        for (int i=0;i<ptX.length;i++) 
            right.addPoint(ptX[i], ptY[i]);
        if (hoverButton == right)
            g.setColor(SystemPanel.yellowText);
        else
            g.setColor(Color.black);
        g.fillPolygon(ptX, ptY, 3);
        

        // center box
        int barX = x+s12;
        int barW = w-s24-labelW;
        int barY = y+s5;
        int barH = h;
        int w1a = (int)(barW * view.spies().allocationPct()); // width of spending
        g.setColor(Color.black);
        g.fillRect(barX, barY, barW, barH);
        box.setBounds(barX, barY, barW, barH);
        g.setColor(sliderC);
        g.fillRect(barX, barY, w1a, barH);

        g.setColor(SystemPanel.whiteText);
        g.setFont(narrowFont(15));
        String spyCost = text("RACES_INTEL_SPENDING_ANNUAL", (int) cost);
        int sw = g.getFontMetrics().stringWidth(spyCost);
        int x0 = barX+((barW-sw)/2);
        drawString(g,spyCost, x0, barY+s14);

        if (hoverButton == box) {
            Stroke prev = g.getStroke();
            g.setColor(SystemPanel.yellowText);
            g.setStroke(stroke2);
            g.draw(box);
            g.setStroke(prev);
        }
        
        // draw string on right for pct 
        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(16));
        String newSpies = view.spies().newSpiesExpected();
        drawString(g,newSpies, x+w-labelW+s10, barY+s14);
    }   
    private void drawSpiesMissionButton(Graphics2D g, EmpireView view ,int index, int x, int y, int w, int h) {
        if (missionDecr.size() <= index)
            missionDecr.add(new Polygon());
        if (missionIncr.size() <= index)
            missionIncr.add(new Polygon());
        if (missionBoxes.size() <= index)
            missionBoxes.add(new Rectangle());
        
        Polygon left = missionDecr.get(index);
        Polygon right = missionIncr.get(index);
        Rectangle box = missionBoxes.get(index);
        
        int leftM = x;
        int rightM = x+w;
        int buttonW = s10;
        int buttonTopY = y+s5;
        int buttonMidY = y+s14;
        int buttonBotY = y+s23;

        //left arrow
        ptX[0] = leftM; ptX[1] = leftM+buttonW; ptX[2] = leftM+buttonW;
        ptY[0] = buttonMidY; ptY[1] = buttonTopY; ptY[2] = buttonBotY;
        left.reset();
        for (int i=0;i<ptX.length;i++) 
           left.addPoint(ptX[i], ptY[i]);
        if (hoverButton == left)
            g.setColor(SystemPanel.yellowText);
        else
            g.setColor(Color.black);
        g.fillPolygon(ptX, ptY, 3);
        
        // right arrow
        ptX[0] = rightM; ptX[1] = rightM-buttonW; ptX[2] = rightM-buttonW;
        ptY[0] = buttonMidY; ptY[1] = buttonTopY; ptY[2] = buttonBotY;
        right.reset();
        for (int i=0;i<ptX.length;i++) 
            right.addPoint(ptX[i], ptY[i]);
        if (hoverButton == right)
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
        box.setBounds(barX, barY, barW, barH);

        g.setColor(sliderBoxBlue);
        String name = view.spies().missionName();
        g.setFont(narrowFont(16));
        int sw = g.getFontMetrics().stringWidth(name);
        int x0 = barX+((barW-sw)/2);
        drawString(g,name, x0, barY+s14);

        if (hoverButton == box) {
            Stroke prev = g.getStroke();
            g.setColor(SystemPanel.yellowText);
            g.setStroke(stroke2);
            g.draw(box);
            g.setStroke(prev);
        }
    }   
    private boolean increaseSliderValue(int index) {
        EmpireView view = empireViews.get(index);
        int oldValue = view.spies().allocation();
        view.spies().increaseSpending();
        return oldValue != view.spies().allocation();
    }
    private boolean decreaseSliderValue(int index) {
        EmpireView view = empireViews.get(index);
        int oldValue = view.spies().allocation();
        view.spies().decreaseSpending();
        return oldValue != view.spies().allocation();
    }
    private void nextSpyMission(int index) {
        EmpireView view = empireViews.get(index);
        view.spies().nextMission();
    }
    private void previousSpyMission(int index) {
        EmpireView view = empireViews.get(index);
        view.spies().prevMission();        
    }
    private void exit() {
        hoverBox = null;
        largeGreenBackC = null;
        col1W = 0;
        spendingBoxes.clear();
        missionBoxes.clear();
        missionIncr.clear();
        missionDecr.clear();
        spendingIncr.clear();
        spendingDecr.clear();
        softClick();
        disableGlassPane();
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_ESCAPE) {
            exit();
            return;
        }
    }
    @Override
    public void mouseClicked(MouseEvent e) {
    }
    @Override
    public void mousePressed(MouseEvent e) {
        dragY = e.getY();
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        dragY = 0;
        if (e.getButton() > 3)
            return;
        if (hoverBox == okButton) {
            exit();
            return;
        }
        for (int i=0;i<spendingBoxes.size();i++) {  
            if (hoverButton == spendingDecr.get(i)) {
                if (decreaseSliderValue(i))
                    repaint();
                return;
            }
            if (hoverButton == spendingIncr.get(i)) {
                if (increaseSliderValue(i))
                    repaint();
                return;
            }
            if (hoverButton == missionBoxes.get(i)) {
                nextSpyMission(i);
                repaint();
                return;
            }    
            if (hoverButton == missionIncr.get(i)) {
                nextSpyMission(i);
                repaint();
                return;
            }
            if (hoverButton == missionDecr.get(i)) {
                previousSpyMission(i);
                repaint();
                return;
            }
            if (hoverButton == spendingBoxes.get(i)) {           
                Rectangle box = spendingBoxes.get(i);
                float pct = (float) ((e.getX() - box.getMinX()) / box.getWidth());
                EmpireView view = empireViews.get(i);
                int oldAlloc = view.spies().allocation();
                view.spies().allocationPct(pct);
                if (oldAlloc != view.spies().allocation()) 
                    repaint();
                return;
            }
        }
    }
    @Override
    public void mouseEntered(MouseEvent e) {
    }
    @Override
    public void mouseExited(MouseEvent e) {
        if (hoverBox != null) {
            hoverBox = null;
            repaint();
        }
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int count = e.getUnitsToScroll();
        for (int i=0;i<spendingBoxes.size(); i++) {
            if (hoverButton == spendingBoxes.get(i)) {
                boolean changed = count < 0 ? increaseSliderValue(i) : decreaseSliderValue(i);
                if (changed) 
                    repaint();
                return;
            }
            else if (hoverButton == missionBoxes.get(i)) {
                if (count < 0)
                    nextSpyMission(i);
                else
                    previousSpyMission(i);
                repaint();
                return;
            }
        }
        if ((hoverBox == listBox)
        || (hoverBox == listScroller)) {
            int prevY = listY;
            if (count < 0)
                listY = max(0,listY-s10);
            else 
                listY = min(listYMax,listY+s10);
            if (listY != prevY) 
                repaint(listBox);
            return;
        }
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int dY = y-dragY;
        dragY = y;
        if (listScroller == hoverBox) {
            if ((y >= listBox.y) || (y <= (listBox.y+listBox.height))) { 
                int h = (int) listBox.getHeight();
                int dListY = (int)((float)dY*(h+listYMax)/h);
                if (dY < 0)
                    listY = max(0,listY+dListY);
                else 
                    listY = min(listYMax,listY+dListY);
            }
            repaint(listBox);
            return;
        }
        else if (listBox == hoverBox) {
            if (listBox.contains(x,y)) { 
                int h = (int) listBox.getHeight();
                int dListY = (int)(-(float)dY*(h+listYMax)/h);
                if (dListY < 0)
                    listY = max(0,listY+dListY);
                else 
                    listY = min(listYMax,listY+dListY);
            }
            repaint(listBox);
            return;
        }
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Shape prevHoverBox = hoverBox;
        Shape prevHoverButton = hoverButton;
        hoverBox = null;
        hoverButton = null;
        
        for (int i=0;i<spendingBoxes.size(); i++) {
            if (spendingBoxes.get(i).contains(x,y)) {
                hoverButton = spendingBoxes.get(i);
                break;
            }
            if (missionBoxes.get(i).contains(x,y)) {
                hoverButton = missionBoxes.get(i);
                break;
            }
            if (missionDecr.get(i).contains(x,y)) {
                hoverButton = missionDecr.get(i);
                break;
            }
            if (missionIncr.get(i).contains(x,y)) {
                hoverButton = missionIncr.get(i);
                break;
            }
            if (spendingDecr.get(i).contains(x,y)) {
                hoverButton = spendingDecr.get(i);
                break;
            }
            if (spendingIncr.get(i).contains(x,y)) {
                hoverButton = spendingIncr.get(i);
                break;
            }
        }

        if (okButton.contains(x, y))
            hoverBox = okButton;
        else if (listScroller.contains(x,y))
            hoverBox = listScroller;
        else if (listBox.contains(x,y))
            hoverBox = listBox;
        
        if ((prevHoverBox != hoverBox) 
        || (prevHoverButton != hoverButton))
            repaint();
    }
}
