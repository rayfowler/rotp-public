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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.border.Border;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;

public class UnexploredGraphicInfoPane extends BasePanel implements MouseListener, MouseWheelListener{
    private static final long serialVersionUID = 1L;
    static Color fontColor = new Color(0,128,0);
    SystemPanel parent;

    public UnexploredGraphicInfoPane(SystemPanel p) {
        this(p, null);
    }
    public UnexploredGraphicInfoPane(SystemPanel p, Border b) {
        parent = p;
        init();
    }
    private void init() {
        setBackground(Color.black);
        addMouseListener(this);
        addMouseWheelListener(this);
    }
    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        int w = getWidth();
        int h = getHeight();
        
        StarSystem sys = parent.systemViewToDisplay();
        if (sys == null)
            return;

        Empire pl = player();
        g.drawImage(pl.sv.starBackground(this), 0, 0, null);
        
        if (sys.inNebula()) {
            g.setColor(SystemPanel.nebulaC);
            g.fillRect(0, 0, w, h);
        }

        int adjW = min(w,h*3/2);
        drawStar(g, sys.starType(), adjW*2/5, w*2/5, h/3);
        
        String name = player().sv.name(sys.id);
        if (!name.isEmpty()) {
            g0.setFont(narrowFont(36*adjW/w));
            int y0 = s42*adjW/w;
            int x0 = s25;
            drawBorderedString(g0, name, 1, x0, y0, Color.black, SystemPanel.orangeText);
        }

        // draw text
        g.setFont(narrowFont(40));
        String text = text("MAIN_UNEXPLORED_SYSTEM");
        int sw = g.getFontMetrics().stringWidth(text);
        int y0 = getHeight()*4/5;
        int x0 = (getWidth()-sw)/2;
        drawBorderedString(g, text, x0, y0, Color.black, fontColor);
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
        if (e.getClickCount() == 2) 
            parent.recenterMap();
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        boolean up = e.getWheelRotation() > 0;
        parent.scrollToNextSystem(up);
    }
}
