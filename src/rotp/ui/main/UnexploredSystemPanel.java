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
package rotp.ui.main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;
import javax.swing.SwingUtilities;
import rotp.model.empires.SystemInfo;
import rotp.model.empires.SystemView;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.map.IMapHandler;

public class UnexploredSystemPanel extends SystemPanel implements MouseMotionListener, MouseListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    Rectangle flagBox = new Rectangle();
    Shape hoverBox;

    public UnexploredSystemPanel(SpriteDisplayPanel p) {
        parentSpritePanel = p;
        init();
    }
    private void init() {
        initModel();
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }
    @Override
    protected BasePanel topPane() { return null; }
    @Override
    protected BasePanel bottomPane() {
        return new SystemRangePane(this);
    }
    @Override
    protected BasePanel detailPane() {
        return new UnexploredDetailPane(parentSpritePanel);
    }
    public void toggleFlagColor(boolean reverse) {
        StarSystem sys = parentSpritePanel.systemViewToDisplay();
        player().sv.toggleFlagColor(sys.id, reverse);
        spritePanel().repaint();
    }
    public void resetFlagColor() {
        StarSystem sys = parentSpritePanel.systemViewToDisplay();
        player().sv.resetFlagColor(sys.id);
        IMapHandler topPanel = spritePanel().parent;
        topPanel.repaint();
    }
    @Override
    public void mouseDragged(MouseEvent e) { }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Shape prevHover = hoverBox;
        hoverBox = null;
        if (flagBox.contains(x,y))
            hoverBox = flagBox;
        
        if (prevHover != hoverBox)
            repaint();
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) {
        boolean rightClick = SwingUtilities.isRightMouseButton(e);
        if (hoverBox == flagBox) {
            if (rightClick)
                resetFlagColor();
            else
                toggleFlagColor(false);
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
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (hoverBox == flagBox) {
            if (e.getWheelRotation() < 0)
                toggleFlagColor(true);
            else
                toggleFlagColor(false);
        }
    }
    public class UnexploredDetailPane  extends BasePanel {
        private static final long serialVersionUID = 1L;
        SpriteDisplayPanel parent;
        UnexploredDetailPane(SpriteDisplayPanel p) {
            parent = p;
            init();
        }
        private void init() {
            setOpaque(true);
            setBackground(Color.black);
        }
        @Override
        public Color starBackgroundC()  { return SystemPanel.starBackgroundC; }
        @Override
        public boolean hasStarBackground()     { return true; }
        @Override
        public void paintComponent(Graphics g0) {
            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;

            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();

            
            if (sys.inNebula()) {
                g.setColor(SystemPanel.nebulaC);
                g.fillRect(0,0,w,h);
            }
                
            Graphics2D g2 = (Graphics2D) g;
            drawStar(g2, sys.starType(), s40, getWidth()/2, getHeight()/2);

            int sz = s60;
            String label = text("MAIN_UNEXPLORED_SYSTEM");
            scaledFont(g, label, w-sz, 36, 24);
            drawBorderedString(g, label, 2, s10, s40, Color.black, SystemPanel.orangeText);
            
            // draw system banner
            SystemInfo sv = player().sv;
            if (hoverBox == flagBox) {
                Image hoverImage = sv.flagHover(sys.id);
                g.drawImage(hoverImage, w-sz+s5, 0, sz, sz, null);
            }
            else if (sv.flagColorId(sys.id) == SystemView.FLAG_NONE){
                Image hoverImage = sv.flagHover(sys.id);
                g.drawImage(hoverImage, w-sz+s5, 0, sz, sz, null);
                Composite prevC = g.getComposite();
                Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f);
                g.setComposite(comp);
                g.setColor(Color.black);
                g.fillRect(w-sz+s15, 0, sz-s10, sz-s10);
                g.setComposite(prevC);
            }
            
            Image flagImage = sv.mapFlagImage(sys.id);
            g.drawImage(flagImage, w-sz+s5, 0, sz, sz, null);
            flagBox.setBounds(w-sz+s5,0,sz-s20,sz-s10);         
            
            //g.setColor(Color.red);
            //g.fillRect(w-sz+s25,15,sz-s20,sz-s10); 
            
            if (sys.inNebula()) {
                g.setFont(narrowFont(16));
                g.setColor(grayText);
                List<String> nebLines =  wrappedLines(g, text("MAIN_NEBULA_DESC"), getWidth()-s12);
                int ydelta = s18;
                int y0=s70;
                for (String line: nebLines) {
                    drawString(g,line, s8, y0);
                    y0 += ydelta;
                }
            } 
           

            g.setFont(narrowFont(16));
            g.setColor(grayText);
            List<String> descLines =  wrappedLines(g, text(sys.starType().description()), getWidth()-s12);

            int ydelta = s18;
            int y0=h-s8-(ydelta*(descLines.size()-1));
            for (String line: descLines) {
                drawString(g,line, s8, y0);
                y0 += ydelta;
            }
        }
    }
}
