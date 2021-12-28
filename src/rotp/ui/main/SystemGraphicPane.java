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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import javax.swing.border.Border;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipLibrary;
import rotp.ui.BasePanel;
import rotp.util.AnimationManager;

public class SystemGraphicPane extends BasePanel implements MouseMotionListener, MouseListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    private boolean showSpyData = false;
    SystemPanel parent;
    Ellipse2D starCircle = new Ellipse2D.Float();
    Ellipse2D planetCircle = new Ellipse2D.Float();
    Rectangle stargateBox = new Rectangle();
    int currentHover = 0;
    public boolean showPopulation = false;
    public SystemGraphicPane(SystemPanel p, Border coloredBorder) {
        parent = p;
        init();
    }
    private void init() {
        setBackground(Color.black);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }
    public void showSpyData()      { showSpyData = true; }
    public void showPopulation()   { showPopulation = true; }

    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;

        int w = getWidth();
        int h = getHeight();

        StarSystem sys = parent.systemViewToDisplay();
        if (sys == null) {
            drawStars(g, w, h);
            return;
        }

        Empire pl = player();
        int adjW = min(w,h*3/2);
        g.drawImage(pl.sv.starBackground(this), 0, 0, null);
        
        if (sys.inNebula()) {
            g.setColor(SystemPanel.nebulaC);
            g.fillRect(0,0,w,h);
        }
        drawStar(g, sys.starType(), adjW*2/5, w*2/5, h/3);
        starCircle.setFrame((w/3)-s20, s10, s40, s40);

        String str = player().sv.name(sys.id);
        scaledFont(g,str,w-s30,36,24);
        int y0 = s42*adjW/w;
        int x0 = s25;
        drawBorderedString(g0, str, 1, x0, y0, Color.black, SystemPanel.orangeText);

        //log("graphic h:", str(unscaled(h)));
        int x1 = s20;
        int y1 = s70*adjW/w;
        int r = s40;
        sys.planet().draw(g0, w, h, x1, y1, (r+r)*adjW/w, 45);
        planetCircle.setFrame(x1, y1, r+r, r+r);

        if (sys.hasStargate(player())) {
            int x2 = s10;
            int y2 = y1;
            Image img = ShipLibrary.current().stargate.getImage();
            int w2 = img.getWidth(null);
            int h2 = img.getHeight(null);
            g.drawImage(img, x2, y2, x2+s14, y2+s14, 0, 0, w2, h2, null);  
            stargateBox.setBounds(x2,y2,s14,s14);
            if (currentHover == 3) {
                Stroke prevStroke = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(Color.red);
                g.drawLine(x2, y2, x2+s14, y2+s14);
                g.drawLine(x2, y2+s14, x2+s14, y2);
                g.setStroke(prevStroke);
            }
        }
        parent.drawPlanetInfo(g, sys, showSpyData, showPopulation, s40, w-s3, h-s12);
    }
    @Override
    public void animate() {
        if (!AnimationManager.current().playAnimations())
            return;
        StarSystem sys = parent.systemViewToDisplay();
        if ((sys != null) && (animationCount() % 1 == 0)) {
            sys.planet().rotate(1);
            repaint();
        }
    }
    @Override
    public void mouseDragged(MouseEvent arg0) {}
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int prevHover = currentHover;
        if (stargateBox.contains(x,y))
            currentHover = 3;
        else if (planetCircle.contains(x,y))
            currentHover = 1;
        else if (starCircle.contains(x,y))
            currentHover = 2;
        else
            currentHover = 0;

        if (currentHover != prevHover) {
            switch(currentHover) {
                case 0: parent.showDefaultDetail(); break;
                case 1: parent.showPlanetDetail(); break;
                case 2: parent.showStarDetail(); break;
            }
            parent.repaint();
        }
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getClickCount() == 2) {
            parent.recenterMap();
            return;
        }
        StarSystem sys = parent.systemViewToDisplay();
        if (sys == null)
            return;
        if (currentHover == 3) {
            currentHover = 0;
            player().sv.removeStargate(sys.id);
            player().recalcPlanetaryProduction();
            repaint();
        }
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        boolean up = e.getWheelRotation() > 0;
        parent.scrollToNextSystem(up);
    }
}
