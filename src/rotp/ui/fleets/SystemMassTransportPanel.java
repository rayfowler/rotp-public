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
import java.awt.geom.Rectangle2D;
import java.util.List;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.main.SystemPanel;
import rotp.ui.main.SystemViewInfoPane;

public class SystemMassTransportPanel  extends SystemPanel {
    private static final long serialVersionUID = 1L;
    FleetUI topParent;

    public SystemMassTransportPanel(FleetUI p) {
        topParent = p;
        initModel(0);
    }
    public void deployTransports() {
        if (!canDeployTransports())
            return;

        log("Deploying Transports");
        topParent.openTransportsDialog();
    }
    public boolean canDeployTransports() {
        StarSystem target = topParent.hoverSystem;
        if (target == null)
            target = topParent.targetSystem;

        if (topParent.filteredSystems.isEmpty())
            return false;
        if (target == null)
            return false;
        if ((topParent.filteredSystems.size() ==1) 
        && topParent.filteredSystems.contains(target))
            return false;
        return player().canSendTransportsTo(target);
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
    protected BasePanel topPane() { return new SystemTransportHeaderPane(); }
    @Override
    protected BasePanel detailPane() { return new SystemViewInfoPane(this); }
    @Override
    protected BasePanel bottomPane() { return new SystemTransportFooterPane(); }
    class SystemTransportHeaderPane extends BasePanel {
        private static final long serialVersionUID = 1L;
        Shape arrow;
        int xPts[];
        int yPts[];
        public SystemTransportHeaderPane() {
            initModel();
        }
        private void initModel() {
            setPreferredSize(new Dimension(getWidth(), scaled(120)));
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

            int y0 = s20;
            g.setColor(SystemPanel.blackText);
            String desc = text("FLEETS_DEPLOYING_TRANSPORTS_DESC");
            List<String> descLines = scaledNarrowWrappedLines(g, desc, w-s20, 4, 18, 16);
            for (String line: descLines) {
                drawString(g,line, s10, y0);
                y0 += s18;
            }
        }
    }
    class SystemTransportFooterPane extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        private Rectangle hoverBox;
        private final Rectangle cancelBox = new Rectangle();
        private final Rectangle startBox = new Rectangle();
        Shape textureClip;
        public SystemTransportFooterPane() {
            initModel();
        }
        private void initModel() {
            setPreferredSize(new Dimension(getWidth(), s70));
            setBackground(FleetUI.backHiC);
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        @Override
        public String textureName()         { return  TEXTURE_BROWN; }
        @Override
        public Shape textureClip()          { return textureClip; }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            int w = getWidth();
            int h = getHeight();

            clearButtons();

            g.setColor(FleetUI.backHiC);
            g.fillRect(0,0,w,h);

            if (canDeployTransports())
                topParent.drawGreenButton(g,text("FLEETS_DEPLOY_TRANSPORTS"), startBox, hoverBox, h-s65);
            else
                topParent.drawGrayButton(g,text("FLEETS_DEPLOY_TRANSPORTS"), null, hoverBox, h-s65);

            topParent.drawBrownButton(g, text("FLEETS_CANCEL"), cancelBox, hoverBox, h-s32);
            textureClip = new Rectangle2D.Float(s3, h-s65, topParent.SIDE_PANE_W-s18, s60);
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
                topParent.clearMapSelections();
                topParent.showQueryPanel();
            }
            else if (startBox.contains(x,y)) {
                deployTransports();
            }
        }
    }
}
