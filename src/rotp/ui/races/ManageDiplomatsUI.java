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
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.diplomacy.DiplomaticMessage;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;

public class ManageDiplomatsUI  extends BasePanel implements MouseListener, MouseWheelListener, MouseMotionListener {
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
    private int dragY = 0;
    private final Rectangle okButton = new Rectangle();
    final Rectangle listBox = new Rectangle();
    final Rectangle listScroller = new Rectangle();
    int listY, listYMax;
    int button1W, button2W, maxNameW;
    private LinearGradientPaint smallGreenBackC, largeGreenBackC;
    Shape textureClip;
    List<Rectangle> audienceBoxes = new ArrayList<>();
    List<Rectangle> diploBoxes = new ArrayList<>();
    List<EmpireView> empireViews = new ArrayList<>();

    public ManageDiplomatsUI(RacesUI p) {
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
        for (Rectangle r: audienceBoxes)
            r.setBounds(0,0,0,0);
        for (Rectangle r: diploBoxes)
            r.setBounds(0,0,0,0);
        
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
        
        if (maxNameW == 0) { // don't do this every repaint. c'mon man.
            empireViews.clear();
            for (EmpireView v: player().contacts()) {
                if (!v.embassy().finalWar() && v.inEconomicRange())
                    empireViews.add(v);
            }
            Collections.sort(empireViews, EmpireView.BY_RACENAME);
            String button1A = text("RACES_DIPLOMACY_AUDIENCE");
            String button1B = text("RACES_MANAGE_OUR_RECALLED");
            String button1C = text("RACES_MANAGE_THEIR_RECALLED");
            String button1D = text("RACES_MANAGE_BOTH_RECALLED");
            String button2A = text("RACES_MANAGE_REINSTATE_DIPLOMAT");
            String button2B = text("RACES_MANAGE_RECALL_DIPLOMAT");
            g.setFont(narrowFont(20));
            maxNameW = s60;
            for (EmpireView view: player().contacts()) 
                maxNameW = max(maxNameW, s30+g.getFontMetrics().stringWidth(view.empire().raceName()));       
            g.setFont(narrowFont(18));
            button1W = max(s80, s30+g.getFontMetrics().stringWidth(button1A));
            button2W = galaxy().council().finalWar() ? 0 : max(s80, s30+g.getFontMetrics().stringWidth(button2A), s20+g.getFontMetrics().stringWidth(button2B));
            g.setFont(narrowFont(16));
            button1W = max(button1W, g.getFontMetrics().stringWidth(button1B), g.getFontMetrics().stringWidth(button1C), g.getFontMetrics().stringWidth(button1D));
        }

        // draw background "haze"
        g.setColor(backgroundHaze);
        g.fillRect(0, 0, w, h);
        

        // get length of title and rows to determine box width
        g.setFont(narrowFont(24));
        String title = text("RACES_DIPLOMACY_BUREAU");
        int titleSW = g.getFontMetrics().stringWidth(title);
        
        int scrollBoxW = s20;
        
        int maxDisplayRows = 9;
        int topM = s15;
        int bottomM = s30;
        int buttonH = s32;
        int titleH = s45;
        int rowH = s35;
        int numRows = min(maxDisplayRows, empireViews.size());
        int padding = button2W == 0 ? s90 : scaled(110);
        int boxWidth = maxNameW+button1W+button2W+padding;
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
        int rowx2 = rowx1+maxNameW+s20;
        int rowx3 = rowx2+button1W+s20;
        int rowx4 = rowx3+button2W+s20;
        
        int listH = h2;
        int fullListH = empireViews.size()* rowH;
        
        g.setClip(x2,y2,w2, h2);
        int y3 = y2-listY;
        for (int i=0;i<empireViews.size();i++) {
            EmpireView view = empireViews.get(i);
            drawEmpireInformation(g, view, i, x2, y3, w2, rowH, rowx2, rowx3, rowx4);
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
            int scrollX = rowx4+s3;
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
        if (empireViews.size() > maxDisplayRows)
            g.drawLine(rowx4, y2, rowx4, y2+h2);     
        
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
    private void  drawEmpireInformation(Graphics2D g, EmpireView view, int index, int x, int y, int w, int h, int x2, int x3, int x4) {
        int y1 = y+h-s12;
        
        if (audienceBoxes.size() <= index)
            audienceBoxes.add(new Rectangle());
        if (diploBoxes.size() <= index)
            diploBoxes.add(new Rectangle());
        
        g.setColor(SystemPanel.blackText);
        
        // race name in column 1
        g.setFont(narrowFont(20));
        String s = view.empire().raceName();
        int w1 = x2-x;
        int sw = g.getFontMetrics().stringWidth(s);
        drawString(g,s, x+(w1-sw)/2, y1);
        
        // audience button or recalled text in column 2
        int w2 = x3-x2;
        boolean ourRecalled = view.embassy().diplomatGone();
        boolean theirRecalled = view.otherView().embassy().diplomatGone();
        String label = null;
        if (ourRecalled && theirRecalled)
            label = text("RACES_MANAGE_BOTH_RECALLED");
        else if (ourRecalled)
            label = text("RACES_MANAGE_OUR_RECALLED");
        else if (theirRecalled)
            label = text("RACES_MANAGE_THEIR_RECALLED");
        
        // set up background gradients
        if (smallGreenBackC == null) {  // don't do this every repaint
            Point2D start1 = new Point2D.Float(x+s15, 0);
            Point2D end1 = new Point2D.Float(x+w-s15, 0);
            float[] dist = {0.0f, 0.2f, 0.5f, 0.8f, 1.0f};
            Color[] greenColors = {greenEdgeC, greenEdgeC, greenMidC, greenEdgeC, greenEdgeC };
            smallGreenBackC = new LinearGradientPaint(start1, end1, dist, greenColors);
        }

        if (label != null) {
            g.setFont(narrowFont(16));
            sw = g.getFontMetrics().stringWidth(label);
            drawString(g,label, x2+(w2-sw)/2, y1);
        }
        else {
            // AUDIENCE BUTTON HERE
            g.setFont(narrowFont(20));
            label = text("RACES_DIPLOMACY_AUDIENCE");
            Rectangle rect = audienceBoxes.get(index);
            drawButton(g, rect, label, smallGreenBackC, x2+s15,y+s5, w2-s30, h-s10);
        }
        
        // recall or reinstate button in column 3
        if (button2W > 0) {
            int w3 = x4-x3;
            g.setFont(narrowFont(20));
            if (view.embassy().alliance()) {
                Rectangle rect2 = diploBoxes.get(index);
                rect2.setBounds(0,0,0,0);
                String label2 = text("RACES_ALLY");
                g.setFont(narrowFont(18));
                int sw2 = g.getFontMetrics().stringWidth(label2);
                int x0 = x3+((w3-sw2)/2);
                int y0 = y+h-s10;
                g.setColor(SystemPanel.whiteText);
                drawString(g, label2, x0, y0);
            }
            else {
                String label2 = ourRecalled ? text("RACES_MANAGE_REINSTATE_DIPLOMAT") : text("RACES_MANAGE_RECALL_DIPLOMAT");
                Rectangle rect2 = diploBoxes.get(index);

                drawButton(g, rect2, label2, null, x3+s15, y+s5, w3-s30, h-s10);
                g.setColor(brownDividerC);
                g.drawLine(x, y+h, x+w-s20, y+h);
            }
        }

        
    }
    private void drawButton(Graphics2D g, Rectangle buttonBox, String label, LinearGradientPaint backC, int x, int y, int w, int h) {
        buttonBox.setBounds(x,y,w,h);
        g.setColor(Color.blue);

        Stroke prev = g.getStroke();
        g.setStroke(stroke3);
        g.setColor(Color.black);
        g.drawRect(x+s2,y+s2,w,h);
        g.setStroke(prev);

        if (backC != null)
            g.setPaint(backC);
        else
            g.setColor(unselectedC);
        g.fillRect(x,y,w,h);

        Color c0 = SystemPanel.whiteText;
        if (hoverButton == buttonBox)
            c0 = Color.yellow;

        g.setFont(narrowFont(18));
        int sw = g.getFontMetrics().stringWidth(label);
        int x0 = x+((w-sw)/2);
        int y0 = y+h-s8;
        drawShadowedString(g, label, 2, x0, y0, MainUI.shadeBorderC(), c0);

        prev = g.getStroke();
        g.setStroke(stroke2);
        g.setColor(c0);
        g.drawRect(x,y,w,h);
        g.setStroke(prev);
    }
    private void recallReinstateDiplomat(EmpireView view) {
        if (view == null)
            return;
        
        if (view.embassy().diplomatGone())
            view.embassy().reopenEmbassy();
        else
            view.embassy().closeEmbassy();
                
        parent.repaint();
    }
    private void openEmbassy(EmpireView view) {
        if (view == null)
            return;
                
        if (view.embassy().diplomatGone()) {
            view.embassy().reopenEmbassy();
            repaint();
        }
        else {
            DiplomaticMessage.show(view.empire().viewForEmpire(player()), DialogueManager.DIPLOMACY_MAIN_MENU);   
            parent.repaint();
        }
    }
    private void exit() {
        hoverBox = null;
        largeGreenBackC = null;
        maxNameW = 0;
        audienceBoxes.clear();
        diploBoxes.clear();
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
        for (int i=0;i<audienceBoxes.size();i++) {
            if (hoverButton == audienceBoxes.get(i)) {
                exit();
                openEmbassy(empireViews.get(i));
                return;
            }         
        }
        for (int i=0;i<diploBoxes.size();i++) {
            if (hoverButton == diploBoxes.get(i)) {
                recallReinstateDiplomat(empireViews.get(i));
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
        
        for (Rectangle r: audienceBoxes) {
            if (r.contains(x,y)) {
                hoverButton = r;
                break;
            }
        }
        for (Rectangle r: diploBoxes) {
            if (r.contains(x,y)) {
                hoverButton = r;
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
