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
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import static rotp.ui.BasePanel.s10;
import static rotp.ui.BasePanel.s20;
import static rotp.ui.BasePanel.s70;
import rotp.ui.RotPUI;
import rotp.ui.SystemViewer;
import rotp.ui.map.IMapHandler;

public class EmpireColonyFoundedPane extends BasePanel implements MouseMotionListener, MouseListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    SystemViewer parent;
    Rectangle flagBox = new Rectangle();
    Rectangle nameBox = new Rectangle();
    Shape hoverBox;
    IMapHandler topParent;
    public BasePanel repainter;
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
        addMouseWheelListener(this);
        addMouseListener(this);
    }
    @Override
    public String textureName()            { return parent.subPanelTextureName(); }
    @Override
    public void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        super.paintComponent(g);
        
        List<StarSystem> systems = parent.systemsToDisplay();
        nameBox.setBounds(0,0,0,0);
        int w = getWidth();
        int h = getHeight();
        StarSystem sys = parent.systemViewToDisplay();
        if (sys == null)
            return;
        int id = sys.id;
        String name = systems != null ? text("PLANETS_AGGREGATE_VALUES") : player().sv.descriptiveName(id);
        int sw = g.getFontMetrics().stringWidth(name);
        Color c0 = nameBox == hoverBox ? Color.yellow : SystemPanel.whiteLabelText;
        scaledFont(g, name, w-s50, 24, 20);
        drawShadowedString(g, name, 2, s10, s30, MainUI.shadeBorderC(), c0);
        nameBox.setBounds(s10, s5, sw+s5,s25);

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
    public void toggleFlagColor(boolean reverse) {
        List<StarSystem> systems = parent.systemsToDisplay();
        if (systems == null) {
            systems = new ArrayList<>();
            StarSystem sys = parent.systemViewToDisplay();
            if (sys != null)
                systems.add(sys);
        }
        
        for (StarSystem sys1: systems) 
            player().sv.toggleFlagColor(sys1.id, reverse);

                if (repainter != null)
            repainter.repaint();
        else if (topParent != null)
            topParent.repaint();
        else
            parent.repaintAll();
    }
    public void resetFlagColor() {
        List<StarSystem> systems = parent.systemsToDisplay();
        if (systems == null) {
            systems = new ArrayList<>();
            StarSystem sys = parent.systemViewToDisplay();
            if (sys != null)
                systems.add(sys);
        }
        
        for (StarSystem sys1: systems) 
            player().sv.resetFlagColor(sys1.id);
        
        if (repainter != null)
            repainter.repaint();
        else if (topParent != null)
            topParent.repaint();
        else
            parent.repaintAll();
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
        else if (nameBox.contains(x,y))
            hoverBox = nameBox;
        
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
        else if (hoverBox == nameBox) {
            RotPUI.instance().selectRacesPanel();
            RotPUI.instance().racesUI().selectDiplomacyTab();
            RotPUI.instance().racesUI().selectedEmpire(player());              
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
                toggleFlagColor(true);
        }
    }
}