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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import rotp.model.galaxy.ShipFleet;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.ui.BasePanel;
import rotp.ui.main.SystemPanel;

public class FleetMassQueryPanel extends BasePanel {
    private static final long serialVersionUID = 1L;
    FleetUI topParent;
    private final FleetQuery fleetQueryPane;
    private final FleetAction fleetActionPane;
    public FleetMassQueryPanel(FleetUI p) {
        topParent = p;
        fleetActionPane = new FleetAction();
        fleetQueryPane = new FleetQuery(this);
        initModel();
    }
    private void initModel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        add(fleetQueryPane, BorderLayout.CENTER);
        add(fleetActionPane, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(getWidth(), scaled(300)));
    }
    class FleetQuery extends BasePanel implements MouseMotionListener, MouseListener {
        private static final long serialVersionUID = 1L;
        FleetMassQueryPanel parent;
        Rectangle selectAllBox = new Rectangle();
        Rectangle deselectAllBox = new Rectangle();
        Rectangle hoverBox;
        public FleetQuery(FleetMassQueryPanel p) {
            parent = p;
            init();
        }
        private void init() {
            addMouseMotionListener(this);
            addMouseListener(this);
        }
        @Override
        public String textureName()            { return TEXTURE_BROWN; }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            int w = getWidth();
            int h = getHeight();
            g.setColor(FleetUI.backLoC);
            g.fillRect(0,0,w,h);
            String sys1 = text("FLEETS_SELECT_FLEETS");
            String cnt1 = text("FLEETS_FLEETS_SELECTED", topParent.filteredFleets.size());
            String fr1 = text("FLEETS_SELECT_ALL");
            String fr2 = text("FLEETS_DESELECT_ALL");
            String filt = text("FLEETS_FILTER_BY");
            String cons = text("FLEETS_INCLUDES_SHIP_TYPE");

            int y0 = s23;
            int x0 = s5;
            int x1 = w/2;
            g.setFont(narrowFont(25));
            drawShadowedString(g, sys1, 3, x0, y0, SystemPanel.textShadowC, SystemPanel.orangeText);

            g.setFont(narrowFont(20));
            y0 += s22;
            drawShadowedString(g, cnt1, 3, x0, y0, SystemPanel.textShadowC, SystemPanel.limeText);

            y0 += s30;
            g.setFont(narrowFont(15));

            // draw Select All box
            int boxMgn = s15;
            int boxL = boxMgn;
            int boxW = (w- (3*boxMgn))/2;
            int boxH = s20;
            int sw1 = g.getFontMetrics().stringWidth(fr1);
            int fr1X = boxL+((boxW-sw1)/2);
            selectAllBox.setBounds(boxL, y0-boxH, boxW, boxH);
            g.setColor(SystemPanel.blackText);
            g.fillRect(boxL+s1, y0-boxH+s1, boxW, boxH);
            g.setColor(FleetUI.backLoC);
            g.fillRect(boxL, y0-boxH, boxW, boxH);
            if (hoverBox == selectAllBox)
                g.setColor(Color.yellow);
            else
                g.setColor(SystemPanel.whiteText);
            drawString(g,fr1, fr1X, y0-s5);
            Stroke prev = g.getStroke();
            g.setStroke(stroke1);
            g.drawRect(boxL, y0-boxH, boxW, boxH);
            g.setStroke(prev);

            // draw Deselect All box
            int boxL2 = w-boxMgn-boxW;
            int sw2 = g.getFontMetrics().stringWidth(fr2);
            int fr2X = boxL2+((boxW-sw2)/2);
            deselectAllBox.setBounds(boxL2, y0-boxH, boxW, boxH);
            g.setColor(SystemPanel.blackText);
            g.fillRect(boxL2+s1, y0-boxH+s1, boxW, boxH);
            g.setColor(FleetUI.backLoC);
            g.fillRect(boxL2, y0-boxH, boxW, boxH);
            if (hoverBox == deselectAllBox)
                g.setColor(Color.yellow);
            else
                g.setColor(SystemPanel.whiteText);
            drawString(g,fr2, fr2X, y0-s5);
            prev = g.getStroke();
            g.setStroke(stroke1);
            g.drawRect(boxL2, y0-boxH, boxW, boxH);
            g.setStroke(prev);

            // draw filter title
            y0 += s25;
            g.setColor(Color.black);
            g.setFont(narrowFont(17));
            drawString(g,filt, x0, y0);
            y0 += s3;

            // draw filters
            g.setColor(Color.darkGray);
            g.setFont(narrowFont(14));

            if (topParent.inTransitFilter.isVisible()) {
                y0 += s14;
                FleetUI.drawFilter(g, x0,y0, w, topParent.inTransitFilter);
            }

            // design filters
            y0 += s25;
            g.setFont(narrowFont(17));
            g.setColor(SystemPanel.blackText);
            drawString(g,cons, x0+s5, y0);
            g.setColor(Color.darkGray);
            g.setFont(narrowFont(14));
            y0 += s4;
            int half = topParent.fleetDesignFilters.length / 2;
            for (int i=0;i<half;i++) {
                y0 += s15;
                FleetUI.QueryFilter filt1 = topParent.fleetDesignFilters[i];
                if (filt1.isVisible())
                    FleetUI.drawFilter(g, x0, y0, x1, filt1);
                FleetUI.QueryFilter filt2 = topParent.fleetDesignFilters[i+half];
                if (filt2.isVisible())
                    FleetUI.drawFilter(g, x1, y0, w, filt2);
            }
        }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            boolean repaint = false;
            for (FleetUI.QueryFilter fl : topParent.fleetDesignFilters)
                repaint = repaint || fl.mouseMoved(x, y);

            repaint = repaint || topParent.inTransitFilter.mouseMoved(x,y);

            Rectangle prevHover = hoverBox;
            hoverBox = null;
            if (selectAllBox.contains(x,y))
                hoverBox = selectAllBox;
            else if (deselectAllBox.contains(x,y))
                hoverBox = deselectAllBox;

            repaint = repaint || (hoverBox != prevHover);
            if (repaint)
                parent.repaint();
        }
        @Override
        public void mouseClicked(MouseEvent e) {  }
        @Override
        public void mousePressed(MouseEvent e) { }
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                return;
            int x = e.getX();
            int y = e.getY();
            boolean validClick = false;
            for (FleetUI.QueryFilter fl : topParent.fleetDesignFilters)
                validClick = validClick || fl.click(x, y);

            validClick = validClick || topParent.inTransitFilter.click(x, y);

            if (selectAllBox.contains(x,y) && topParent.selectAllFleets())
                validClick = true;
            else if (deselectAllBox.contains(x,y) && topParent.deselectAllFleets())
                validClick = true;

            if (validClick) {
                softClick();
                topParent.fleetFiltersChanged();
            }
            else
                misClick();
        }
        @Override
        public void mouseEntered(MouseEvent e) {  }
        @Override
        public void mouseExited(MouseEvent e) {
            boolean repaint = false;
            for (FleetUI.QueryFilter fl : topParent.fleetDesignFilters)
                repaint = repaint || fl.mouseExited();

            repaint = repaint || topParent.inTransitFilter.mouseExited();

            if (hoverBox != null) {
                hoverBox = null;
                repaint = true;
            }

            if (repaint)
                parent.repaint();
        }
    }
    class FleetAction extends BasePanel implements MouseMotionListener, MouseListener {
        private static final long serialVersionUID = 1L;
        private final Rectangle deployBox  = new Rectangle();
        private Rectangle hoverBox;
        public FleetAction() {
            init();
        }
        private void init() {
            setPreferredSize(new Dimension(getWidth(), scaled(110)));
            addMouseMotionListener(this);
            addMouseListener(this);
        }
        @Override
        public String textureName()            { return TEXTURE_BROWN; }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            int w = getWidth();
            int h = getHeight();
            g.setColor(FleetUI.backLoC);
            g.fillRect(0,0,w,h);

            String ships = text("FLEETS_SELECTED_SHIPS");
            g.setFont(narrowFont(18));
            drawShadowedString(g0, ships, 3, s10, s25, SystemPanel.textShadowC, SystemPanel.whiteText);
            drawFleetsToDeploy(g, 0, h-s88, w);
            if (topParent.showingQueryPanel())
                drawDeployFleetsButton(g,0,h-s35,w,s35);
            else {
                g.setFont(narrowFont(25));
                String title = text("FLEETS_FLEET_DEPLOYMENT");
                drawShadowedString(g, title, 3, s5, h-s10, SystemPanel.textShadowC, SystemPanel.orangeText);
            }
        }
        private void drawFleetsToDeploy(Graphics2D g, int x0, int y0, int w) {
            int x1 = x0 + w/2;
            y0 += s5;
            g.setFont(narrowFont(15));
            int maxCount = 0;
            for (int i=0;i<ShipDesignLab.MAX_DESIGNS;i++) 
                maxCount = max(maxCount,shipCount(i));
            int swMax = g.getFontMetrics().stringWidth(str(maxCount));
            g.setColor(SystemPanel.blackText);
            int half = ShipDesignLab.MAX_DESIGNS / 2;
            for (int i=0;i<half;i++) {
                y0 += s15;
                ShipDesign d1 = player().shipLab().design(i);
                if (d1.active()) {
                    g.setFont(narrowFont(15));
                    String str1 = str(shipCount(i));
                    int sw1 = g.getFontMetrics().stringWidth(str1);
                    drawString(g,str1, x0+s5+swMax-sw1, y0);
                    String str2 = d1.name();
                    scaledFont(g,str2,w/2-swMax-s10,15,7);
                    drawString(g,str2, x0+swMax+s10, y0);
                }
                ShipDesign d2 = player().shipLab().design(i+half);
                if (d2.active()) {
                    g.setFont(narrowFont(15));
                    String str1 = str(shipCount(i+half));
                    int sw1 = g.getFontMetrics().stringWidth(str1);
                    drawString(g,str1, x1+s5+swMax-sw1, y0);
                    String str2 = d2.name();
                    scaledFont(g,str2,w/2-swMax-s10,15,7);
                    drawString(g,str2, x1+swMax+s10, y0);
                }
            }
        }
        private int shipCount(int dNum) {
            if (!topParent.allowedDesign(dNum))
                return 0;
            int n = 0;
            for (ShipFleet fl: topParent.filteredFleets)
                n += fl.num(dNum);
            return n;
        }
        private void drawDeployFleetsButton(Graphics2D g, int x, int y, int w, int h) {
            deployBox.setBounds(x, y, w, h);
            g.setColor(FleetUI.backLoC);
            g.fillRect(x, y, w, h);
            g.setColor(FleetUI.darkShadingC);
            g.fillRect(x+s2,y+s5,w-s2,h-s5);
            g.setColor(SystemPanel.whiteText);
            g.fillRect(x+s2,y+s5, w-s3, h-s7);
            g.setColor(FleetUI.backLoC);
            g.fillRect(x+s3,y+s6, w-s5, h-s9);
            if (topParent.filteredFleets.isEmpty())
                g.setColor(SystemPanel.grayText);
            else if (hoverBox == deployBox)
                g.setColor(SystemPanel.yellowText);
            else
                g.setColor(SystemPanel.whiteText);
            g.setFont(narrowFont(20));
            String s = text("FLEETS_DEPLOY_FLEETS");
            drawString(g,s, x+s10, y+s25);
            if ((hoverBox == deployBox)
            && player().canSendTransports()) {
                Stroke prevStroke = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(SystemPanel.yellowText);
                g.drawRect(x+s2,y+s5,w-s2,h-s7);
                g.setStroke(prevStroke);
            }
        }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Rectangle prevHover = hoverBox;
            hoverBox = null;
            if (deployBox.contains(x,y))
                hoverBox = deployBox;

            if (hoverBox != prevHover)
                repaint();
        }
        @Override
        public void mouseClicked(MouseEvent e) {  }
        @Override
        public void mousePressed(MouseEvent e) { }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                return;
            int x = e.getX();
            int y = e.getY();

            if (deployBox.contains(x,y)) {
                softClick();
                topParent.showDeployPanel();
                topParent.repaint();
                return;
            }
            misClick();
        }
        @Override
        public void mouseEntered(MouseEvent e) {  }
        @Override
        public void mouseExited(MouseEvent e) {
            if (hoverBox != null) {
                hoverBox = null;
                repaint();
            }
        }
    }
}
