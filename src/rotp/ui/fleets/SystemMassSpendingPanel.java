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
package rotp.ui.fleets;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import rotp.model.colony.Colony;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.main.SystemPanel;

public class SystemMassSpendingPanel  extends BasePanel implements MouseMotionListener, MouseListener {
    private static final long serialVersionUID = 1L;
    FleetUI topParent;
    Rectangle hoverBox;

    Rectangle spending0Box = new Rectangle();
    Rectangle spending25Box = new Rectangle();
    Rectangle spending50Box = new Rectangle();
    Rectangle spending75Box = new Rectangle();
    Rectangle spendingMaxBox = new Rectangle();
    Rectangle cancelBox = new Rectangle();
    public SystemMassSpendingPanel(FleetUI p) {
        topParent = p;
        init();
    }
    private void init() {
        addMouseMotionListener(this);
        addMouseListener(this);
        setPreferredSize(new Dimension(getWidth(),scaled(300)));
    }
    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        int w = getWidth();
        int h = getHeight();
        g.setColor(FleetUI.backHiC);
        g.fillRect(0,0,w,h);
        
        int buttonH = s35;
        int y0 = 0;
        topParent.drawGreenButton(g,text("FLEETS_SPENDING_0"), spending0Box, hoverBox, y0);
        y0 += buttonH;
        topParent.drawGreenButton(g,text("FLEETS_SPENDING_25"), spending25Box, hoverBox, y0);
        y0 += buttonH;
        topParent.drawGreenButton(g,text("FLEETS_SPENDING_50"), spending50Box, hoverBox, y0);
        y0 += buttonH;
        topParent.drawGreenButton(g,text("FLEETS_SPENDING_75"), spending75Box, hoverBox, y0);
        y0 += buttonH;
        topParent.drawGreenButton(g,text("FLEETS_SPENDING_MAX"), spendingMaxBox, hoverBox, y0);
        y0 += buttonH;

        y0 += s15;
        g.setColor(SystemPanel.blackText);            
        String desc = text("FLEETS_ADJUST_SPENDING_DESC2");
        if (!player().ignoresPlanetEnvironment())
            desc = desc + " " + text("FLEETS_ADJUST_SPENDING_DESC3");
        List<String> descLines = scaledNarrowWrappedLines(g, desc, w-s20, 4, 18, 14);
        for (String line: descLines) {
            drawString(g,line, s10, y0);
            y0 += s16;
        }
        
        topParent.drawBrownButton(g, text("FLEETS_CANCEL"), cancelBox, hoverBox, h-s32);
    }
    private void setShipSpendingLevel(float pct) {
        for (StarSystem sys: topParent.filteredSystems) {
            Colony c = sys.colony();
            if (c != null) {
                c.forcePct(Colony.SHIP, pct);
                c.ensureMinimumCleanup();
            }
        }
        close();
    }
    private void close() {
        topParent.clearMapSelections();
        topParent.showQueryPanel();
    }
    @Override
    public void mouseDragged(MouseEvent arg0) { }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        Rectangle prevHover = hoverBox;
        hoverBox = null;
        
        if (spending0Box.contains(x,y))
            hoverBox = spending0Box;
        else if (spending25Box.contains(x,y))
            hoverBox = spending25Box;
        else if (spending50Box.contains(x,y))
            hoverBox = spending50Box;
        else if (spending75Box.contains(x,y))
            hoverBox = spending75Box;
        else if (spendingMaxBox.contains(x,y))
            hoverBox = spendingMaxBox;

        if (hoverBox != prevHover)
            repaint();
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
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
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        int x = e.getX();
        int y = e.getY();

        if (cancelBox.contains(x,y)) 
            close();
        if (spending0Box.contains(x,y)) 
            setShipSpendingLevel(0);
        else if (spending25Box.contains(x,y)) 
            setShipSpendingLevel(0.25f);
        else if (spending50Box.contains(x,y)) 
            setShipSpendingLevel(0.5f);
        else if (spending75Box.contains(x,y)) 
            setShipSpendingLevel(0.75f);
        else if (spendingMaxBox.contains(x,y)) 
            setShipSpendingLevel(1);
    }
}
