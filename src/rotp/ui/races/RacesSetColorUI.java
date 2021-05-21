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
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.ui.BasePanel;
import static rotp.ui.BasePanel.s15;
import static rotp.ui.BasePanel.s30;
import static rotp.ui.BasePanel.s70;
import rotp.ui.main.SystemPanel;

public class RacesSetColorUI extends BasePanel implements MouseListener, MouseMotionListener  {
    private static final long serialVersionUID = 1L;
    private static final Color backgroundHaze = new Color(0,0,0,160);
    private static final Color backC = new Color(112,85,68);
    private static final Color borderC = new Color(112,85,68,128);

    Shape textureClip;
    private int hoverIndex = -1;
    private Shape hoverBox;
    private RacesUI parent;
    private final List<Shape> hoverShapes = new ArrayList<>();

    public RacesSetColorUI(RacesUI p) {
        parent = p;
        initModel();
    }
    private void initModel() {
        setOpaque(false);
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    @Override
    public void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;

        super.paintComponent(g);

        int w = getWidth();
        int h = getHeight();

        boolean firstPass = hoverShapes.isEmpty();
        
        // draw background "haze"
        g.setColor(backgroundHaze);
        g.fillRect(0, 0, w, h);

        Empire emp = parent.selectedIconEmpire();
        // get length of title and determine box width
        g.setFont(narrowFont(24));
        String title = text("RACES_SET_COLOR_TITLE");
        title = emp.replaceTokens(title, "alien");
        int titleSW = g.getFontMetrics().stringWidth(title);

        int n = options().numColors();

        int boxWidth = max(n*s30, titleSW+s70);

        int x0 = (w - boxWidth)/2;
        int y0 = h/3;

        int paneW = boxWidth-s30;
        String desc = text("RACES_SET_COLOR_DESC");
        g.setFont(narrowFont(16));
        List<String> lines = this.wrappedLines(g, desc, paneW-s20);

        int boxHeight = scaled(130)+(lines.size()*s18);
        // draw box
        g.setColor(borderC);
        g.fillRect(x0, y0, boxWidth, boxHeight);
        g.setColor(backC);
        g.fillRect(x0+s15, y0+s15, boxWidth-s30, boxHeight-s30);

        textureClip = new Rectangle(x0+s15, y0+s15, paneW, boxHeight-s30);

        // draw title
        int y1 = y0+s45;
        int x1 = x0+s30;
        g.setFont(narrowFont(24));
        drawShadowedString(g, title, 3, x1, y1, SystemPanel.textShadowC, SystemPanel.whiteText);

        // draw descriptive subtitle
        y1 += s10;
        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(16));
        for (String line: lines) {
            y1 += s18;
            drawString(g,line, x0+s25, y1);
        }
       
        
        // draw colored shapes
        int y2 = y1+s10;
        int shapeW = paneW/n;
        int iconW = s20;
        int buff = (shapeW-iconW)/2;
        for (int i=0;i<n;i++) {
            int x2 = x0 + s15 + buff + (paneW*i/n);
            Color c0 = options().color(i);
            Shape sh = emp.drawShape(g,x2,y2,iconW,iconW,c0);
            if (firstPass)
                hoverShapes.add(sh);
            if (i == hoverIndex) {
                Stroke prev = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(Color.yellow);
                g.draw(sh);
                g.setStroke(prev);
            }          
        }
        
        //draw exit string
        String exit = text("CLICK_EXIT");
        g.setFont(narrowFont(16));
        int sw = g.getFontMetrics().stringWidth(exit);
        g.setColor(SystemPanel.blackText);
        int x3 = x0+(boxWidth-sw)/2;
        int y3 = y2 +s40;
        drawString(g,exit, x3, y3);
    }
    private void exit() {
        hoverBox = null;
        hoverIndex = -1;
        hoverShapes.clear();
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
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        
        if (hoverIndex >= 0) 
            parent.selectedIconEmpire().changeColorId(hoverIndex);
        exit();
    }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }
    @Override
    public void mouseDragged(MouseEvent e) { }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        
        int prevHover = hoverIndex;
        hoverIndex = -1;
        for (int i=0;i<hoverShapes.size();i++) {
            Shape sh = hoverShapes.get(i);
            if (sh.contains(x,y)) {
                hoverIndex = i;
                break;
            }
        }
        
        if (hoverIndex != prevHover)
            repaint();
    }    
}
