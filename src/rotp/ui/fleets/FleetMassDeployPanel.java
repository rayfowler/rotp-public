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
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesign;
import rotp.ui.BasePanel;
import rotp.ui.main.SystemPanel;
import rotp.ui.main.SystemViewInfoPane;
import rotp.ui.sprites.FlightPathSprite;

public class FleetMassDeployPanel extends BasePanel {
    private static final long serialVersionUID = 1L;
    FleetUI topParent;
    int leftM, midM1, midM2, rightM;

    public FleetMassDeployPanel(FleetUI p) {
        topParent = p;
        initModel();
    }
    public void deploySelectedFleets() {
        if (!canDeployFleets())
            return;
        StarSystem dest = topParent.targetSystem;
        List<ShipDesign> designs = topParent.selectedFleetDesigns();
        for (ShipFleet fl: topParent.filteredFleets) {
            if (designs.isEmpty())
                galaxy().ships.deployFleet(fl, dest.id);
            else
                galaxy().ships.deploySubfleet(fl, designs, dest.id);
        }
        topParent.showQueryPanel();
        topParent.repaint();
    }
    public boolean canDeployFleets() {
        StarSystem target = topParent.hoverSystem;
        if (target == null)
            target = topParent.targetSystem;
        if (topParent.filteredFleets.isEmpty()
        || (target == null))
            return false;

        boolean canReach = true;
        for (ShipFleet fl: topParent.filteredFleets)
            canReach = canReach && fl.empire().isPlayer() && fl.canReach(target) && fl.canMassDeployTo(target);

        return canReach;
    }
    private void initModel() {
        setOpaque(true);
        setPreferredSize(new Dimension(getWidth(),scaled(340)));
        setBackground(FleetUI.backHiC);
        BasePanel bottomPanel = new FleetDeployPane();

        setLayout(new BorderLayout());
        add(new FleetUndeployPane(), BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.CENTER);
    }
    class FleetUndeployPane extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        private Rectangle hoverBox;
        private final Rectangle cancelBox = new Rectangle();
        private final Rectangle stopBox = new Rectangle();

        public FleetUndeployPane() {
            initModel();
        }
        private void initModel() {
            setPreferredSize(new Dimension(getWidth(), s77));
            setBackground(FleetUI.backHiC);
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            int w = getWidth();
            int h = getHeight();

            clearButtons();

            int undeployCount = 0;
            for (ShipFleet fl: topParent.filteredFleets) {
                if (fl.canUndeploy())
                    undeployCount++;
            }

            g.setColor(FleetUI.backHiC);
            g.fillRect(0,0,w,h);

            int x1 = w/2-s30;
            g.setColor(FleetUI.backLoC);
            g.fillRect(x1,0,s60,h);

            if (undeployCount == 0)
                return;

            int y0 = s5;

            g.setColor(SystemPanel.blackText);
            String desc = text("FLEETS_UNDEPLOYING_FLEETS", str(undeployCount));
            List<String> descLines = scaledNarrowWrappedLines(g, desc, w-s20, 2, 16, 14);
            for (String line: descLines) {
                y0 += s16;
                drawString(g,line, s10, y0);
            }

            if (undeployCount == 0)
                topParent.drawGrayButton(g, text("FLEETS_CANCEL_DEPLOYMENTS"), null, hoverBox, h-s32);
            else
                topParent.drawRedButton(g, text("FLEETS_CANCEL_DEPLOYMENTS"), stopBox, hoverBox, h-s32);
        }
        private void clearButtons() {
            cancelBox.setBounds(0,0,0,0);
            stopBox.setBounds(0,0,0,0);
        }
        @Override
        public void mouseDragged(MouseEvent arg0) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            Rectangle prevHover = hoverBox;
            hoverBox = null;
            if (cancelBox.contains(x,y))
                hoverBox = cancelBox;
            else if (stopBox.contains(x,y))
                hoverBox = stopBox;

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

            if (cancelBox.contains(x,y)) {
                topParent.clearMapSelections();
                topParent.showQueryPanel();
            }
            else if (stopBox.contains(x,y)) {
                List<ShipDesign> designs = topParent.selectedFleetDesigns();
                for (ShipFleet fl: topParent.filteredFleets) {
                    if (designs.isEmpty())
                        galaxy().ships.undeployFleet(fl);
                    else
                        galaxy().ships.undeployFleet(fl,designs);
                }
                topParent.clearMapSelections();
                topParent.repaint();
            }
        }
    }
    class FleetDeployPane extends SystemPanel {
        private static final long serialVersionUID = 1L;
        public FleetDeployPane() {
            init();
        }
        private void init() {
            initModel(0);
        }
        @Override
        protected Color backgroundColor()   { return FleetUI.backLoC; }
        @Override
        public StarSystem systemViewToDisplay() {
            if (topParent.hoverSystem != null)
                return topParent.hoverSystem;
            else
                return topParent.targetSystem;
        }
        @Override
        protected BasePanel topPane() { return new FleetDeployHeaderPane(); }
        @Override
        protected BasePanel detailPane() { return new SystemViewInfoPane(this); }
        @Override
        protected BasePanel bottomPane() { return new FleetDeployFooterPane(); }
    }
    class FleetDeployHeaderPane extends BasePanel {
        private static final long serialVersionUID = 1L;
        Shape arrow;
        int xPts[];
        int yPts[];
        public FleetDeployHeaderPane() {
            initModel();
        }
        private void initModel() {
            setPreferredSize(new Dimension(getWidth(), s65));
            setBackground(FleetUI.backHiC);
        }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            int w = getWidth();
            int h = getHeight();
            g.setColor(FleetUI.backHiC);
            g.fillRect(0,0,w,h);

            if (arrow == null) {
                xPts = new int[7];
                yPts = new int[7];
                xPts[0] = w/2-s30;
                xPts[1] = w/2-s30;
                xPts[2] = w/2-s60;
                xPts[3] = w/2;
                xPts[4] = w/2+s60;
                xPts[5] = w/2+s30;
                xPts[6] = w/2+s30;
                yPts[0] = 0;
                yPts[1] = h-s50;
                yPts[2] = h-s50;
                yPts[3] = h;
                yPts[4] = h-s50;
                yPts[5] = h-s50;
                yPts[6] = 0;
                arrow = new Polygon(xPts,yPts,xPts.length);
            }
            g.setColor(FleetUI.backLoC);
            g.fill(arrow);
            g.setColor(SystemPanel.blackText);
            String desc = text("FLEETS_DEPLOYING_FLEETS_DESC");
            g.setFont(narrowFont(18));
            List<String> descLines = wrappedLines(g, desc, w-s20);
            int y0 = h-s10-(descLines.size()*s20);
            for (String line: descLines) {
                y0 += s18;
                drawString(g,line, s10, y0);
            }
        }
    }
    class FleetDeployFooterPane extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        private Rectangle hoverBox;
        private final Rectangle cancelBox = new Rectangle();
        private final Rectangle startBox = new Rectangle();
        public FleetDeployFooterPane() {
            initModel();
        }
        private void initModel() {
            setPreferredSize(new Dimension(getWidth(), s72));
            setBackground(FleetUI.backHiC);
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            int w = getWidth();
            int h = getHeight();

            clearButtons();

            g.setColor(FleetUI.backHiC);
            g.fillRect(0,0,w,h);

            if (canDeployFleets())
                topParent.drawGreenButton(g, text("FLEETS_DEPLOY_FLEETS"), startBox, hoverBox, h-s65);
            else
                topParent.drawGrayButton(g, text("FLEETS_DEPLOY_FLEETS"), null, hoverBox, h-s65);

            topParent.drawBrownButton(g, text("FLEETS_CANCEL"), cancelBox, hoverBox, h-s32);
        }
        private void clearButtons() {
            cancelBox.setBounds(0,0,0,0);
            startBox.setBounds(0,0,0,0);
        }
        @Override
        public void mouseDragged(MouseEvent arg0) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            Rectangle prevHover = hoverBox;
            hoverBox = null;
            if (cancelBox.contains(x,y))
                hoverBox = cancelBox;
            else if (startBox.contains(x,y))
                hoverBox = startBox;

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

            if (cancelBox.contains(x,y)) {
                FlightPathSprite.clearWorkingPaths();
                topParent.showQueryPanel();
            }
            else if (startBox.contains(x,y))
                deploySelectedFleets();
        }
    }
}
