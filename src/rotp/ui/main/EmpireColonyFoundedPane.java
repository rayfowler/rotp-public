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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.SystemViewer;
import rotp.ui.map.IMapHandler;

public class EmpireColonyFoundedPane extends BasePanel implements MouseMotionListener, MouseListener {
    private static final long serialVersionUID = 1L;
    SystemViewer parent;
    Rectangle flagBox = new Rectangle();
    Shape hoverBox;
    IMapHandler topParent;
    public EmpireColonyFoundedPane(SystemViewer p, IMapHandler top, Color c0) {
        parent = p;
        topParent = top;
        init(c0);
    }
    private void init(Color c0) {
        setOpaque(true);
        setBackground(c0);
        setPreferredSize(new Dimension(getWidth(), s40));
        addMouseMotionListener(this);
        addMouseListener(this);
    }
    @Override
    public String textureName()            { return parent.subPanelTextureName(); }
    @Override
    public void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        super.paintComponent(g);
        
        int w = getWidth();
        int h = getHeight();
        StarSystem sys = parent.systemViewToDisplay();
        if (sys == null)
            return;
        int id = sys.id;
        String name = player().sv.descriptiveName(id);
        g.setFont(narrowFont(24));
        drawShadowedString(g, name, 2, s10, s30, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);
        if (topParent == null)
            return;

        Color flagC = topParent.flagColor(sys);
        if (hoverBox == flagBox) 
            sys.drawBanner(g, flagC, SystemPanel.yellowText, w-s10,h);
        else {
            Color c1 = flagC == null ? SystemPanel.blackText : SystemPanel.whiteText;
            sys.drawBanner(g, flagC, c1, w-s10,h);
        }
        
        flagBox.setBounds(w-s30,h-s60,s20,s60);
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
        if (hoverBox == flagBox) {
            StarSystem sys = parent.systemViewToDisplay();
            player().sv.view(sys.id).toggleFlagColor();
            topParent.repaint();
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
}