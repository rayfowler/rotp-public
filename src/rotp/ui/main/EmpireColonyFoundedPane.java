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
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.SwingUtilities;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import static rotp.ui.BasePanel.s10;
import static rotp.ui.BasePanel.s20;
import static rotp.ui.BasePanel.s70;
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

        // draw system banner
        int sz = s70;
        if (hoverBox == flagBox) {
            Image hoverImage = player().sv.flagHover(sys.id);
            g.drawImage(hoverImage, w-sz+s15, h-sz+s15, sz, sz, null);
        }
        Image flagImage = player().sv.flagImage(sys.id);
        g.drawImage(flagImage, w-sz+s15, h-sz+s15, sz, sz, null);
        flagBox.setBounds(w-sz+s25,h-sz+s15,sz-s20,sz-s10);
    }
    public void toggleFlagColor(boolean rightClick) {
        StarSystem sys = parent.systemViewToDisplay();
        player().sv.view(sys.id).toggleFlagColor(rightClick);
        if (topParent != null)
            topParent.repaint();
        else
            parent.repaint();
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
            toggleFlagColor(rightClick);
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