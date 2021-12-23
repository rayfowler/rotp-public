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
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.Design;
import rotp.ui.BasePanel;
import rotp.ui.main.SystemPanel;

public class SystemMassQueryPanel extends BasePanel {
    private static final long serialVersionUID = 1L;
    FleetUI topParent;
    private final SystemQuery sysQueryPane;
    private final SystemAction sysActionPane;

    public SystemMassQueryPanel(FleetUI p) {
        topParent = p;
        sysActionPane = new SystemAction();
        sysQueryPane = new SystemQuery(this);
        initModel();
    }
    private void initModel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        add(sysQueryPane, BorderLayout.CENTER);
        add(sysActionPane, BorderLayout.SOUTH);
    }
    class SystemQuery extends BasePanel implements MouseMotionListener, MouseListener {
        private static final long serialVersionUID = 1L;
        SystemMassQueryPanel parent;
        Rectangle selectAllBox = new Rectangle();
        Rectangle deselectAllBox = new Rectangle();
        Rectangle hoverBox;
        public SystemQuery(SystemMassQueryPanel p) {
            parent = p;
            initModel();
        }
        private void initModel() {
            addMouseMotionListener(this);
            addMouseListener(this);
        }
        private FleetUI.ShipDesignFilter[] designFilters()     { return topParent.systemDesignFilters; }
        private FleetUI.ShipDesignFilter designFilter(int i)   { return topParent.systemDesignFilters[i]; }
        private FleetUI.SystemRallyPointFilter rallyFilter()   { return topParent.rallyPointFilter; }
        private FleetUI.SystemStargateFilter stargateFilter()  { return topParent.stargateFilter; }
        private FleetUI.SystemTransportsFilter transportsFilter()  { return topParent.transportsFilter; }
        private FleetUI.SystemResourcesFilter resourceFilter() { return topParent.resourceFilter; }
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
            String sys1 = text("FLEETS_SELECT_SYSTEMS");
            String cnt1 = text("FLEETS_SYSTEMS_SELECTED", topParent.filteredSystems.size());
            String fr1 = text("FLEETS_SELECT_ALL");
            String fr2 = text("FLEETS_DESELECT_ALL");
            String filt = text("FLEETS_FILTER_BY");
            String cons = text("FLEETS_CONSTRUCTING_DESIGN");

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
            y0 += s20;
            g.setColor(Color.black);
            g.setFont(narrowFont(17));
            drawString(g,filt, x0, y0);
            y0 += s3;

            // draw filters
            g.setColor(Color.darkGray);
            g.setFont(narrowFont(14));

            if (resourceFilter().isVisible()) {
                y0 += s14;
                FleetUI.drawFilter(g, x0,y0, w, resourceFilter());
            }
            if (rallyFilter().isVisible()) {
                y0 += s14;
                FleetUI.drawFilter(g, x0,y0, w, rallyFilter());
            }
            if (stargateFilter().isVisible()) {
                y0 += s14;
                FleetUI.drawFilter(g, x0,y0, w, stargateFilter());
            }
            if (transportsFilter().isVisible()) {
                y0 += s14;
                FleetUI.drawFilter(g, x0,y0, w, transportsFilter());
            }
            y0 += s19;
            g.setFont(narrowFont(17));
            g.setColor(SystemPanel.blackText);
            drawString(g,cons, x0+s5, y0);
            g.setColor(Color.darkGray);
            g.setFont(narrowFont(14));
            y0 += s2;
            int half = designFilters().length / 2;
            for (int i=0;i<half;i++) {
                y0 += s14;
                FleetUI.QueryFilter filt1 = designFilter(i);
                if (filt1.isVisible())
                    FleetUI.drawFilter(g, x0, y0, x1, filt1);
                FleetUI.QueryFilter filt2 = designFilter(i+half);
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
            for (FleetUI.QueryFilter fl : designFilters())
                repaint = repaint || fl.mouseMoved(x, y);

            repaint = repaint || resourceFilter().mouseMoved(x,y);
            repaint = repaint || stargateFilter().mouseMoved(x,y);
            repaint = repaint || transportsFilter().mouseMoved(x,y);
            repaint = repaint || rallyFilter().mouseMoved(x,y);

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
            for (FleetUI.QueryFilter fl : designFilters())
                validClick = validClick || fl.click(x, y);

            validClick = validClick || resourceFilter().click(x, y);
            validClick = validClick || stargateFilter().click(x, y);
            validClick = validClick || transportsFilter().click(x, y);
            validClick = validClick || rallyFilter().click(x, y);

            if (selectAllBox.contains(x,y) && topParent.selectAllSystems())
                validClick = true;
            else if (deselectAllBox.contains(x,y) && topParent.deselectAllSystems())
                validClick = true;

            if (validClick) {
                softClick();
                topParent.systemFiltersChanged();
                topParent.repaint();
            }
            else
                misClick();
        }
        @Override
        public void mouseEntered(MouseEvent e) {  }
        @Override
        public void mouseExited(MouseEvent e) {
            boolean repaint = false;
            for (FleetUI.QueryFilter fl : designFilters())
                repaint = repaint || fl.mouseExited();

            repaint = repaint || resourceFilter().mouseExited();
            repaint = repaint || stargateFilter().mouseExited();
            repaint = repaint || transportsFilter().mouseExited();
            repaint = repaint || rallyFilter().mouseExited();

            if (hoverBox != null) {
                hoverBox = null;
                repaint = true;
            }

            if (repaint)
                parent.repaint();
        }
    }
    class SystemAction extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        // polygon coordinates for left & right increment buttons
        private final int leftButtonX[] = new int[3];
        private final int leftButtonY[] = new int[3];
        private final int rightButtonX[] = new int[3];
        private final int rightButtonY[] = new int[3];

        private final Rectangle shipDesignBox = new Rectangle();
        private final Rectangle rallyPointBox = new Rectangle();
        private final Rectangle stopRalliesBox = new Rectangle();
        private final Rectangle stopTransportsBox = new Rectangle();
        private final Rectangle transportBox  = new Rectangle();
        private final Rectangle spendingBox  = new Rectangle();
        private final Rectangle shipNameBox = new Rectangle();
        private final Polygon prevDesign = new Polygon();
        private final Polygon nextDesign = new Polygon();
        private Shape hoverBox;
        Area textureArea;

        public SystemAction() {
            initModel();
        }
        private void initModel() {
            setPreferredSize(new Dimension(getWidth(), s100+s35));
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }
        @Override
        public String textureName()            { return TEXTURE_BROWN; }
        @Override
        public Area textureArea()             { return textureArea; }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            int w = getWidth();
            int h = getHeight();
            g.setColor(FleetUI.backLoC);
            g.fillRect(0,0,w,h);

            if (topParent.showingQueryPanel()) {
                int midMargin = s90;
                drawShipIcon(g,s5,s5,midMargin-s10,s60);
                drawNameSelector(g,midMargin+s5,h-s100-s28,w-midMargin-s15,s30);
                drawRallyPointButton(g,midMargin+s5,h-s93,w-s15-midMargin,s25);
                drawShipSpendingButton(g,0,h-s67,w,s35);
                drawTransportButton(g,0,h-s35,w,s35);
                textureArea = null;
            }
            else if (topParent.showingRallyPanel())
                drawRallyPointHeader(g,w,h);
            else if (topParent.showingTransportPanel())
                drawTransportHeader(g,w,h);
            else if (topParent.showingSpendingPanel())
                drawShipSpendingHeader(g,w,h);
        }
        private void drawRallyPointHeader(Graphics2D g, int w, int h) {
            g.setFont(narrowFont(25));
            String title = text("FLEETS_SET_RALLY_POINTS");
            drawShadowedString(g, title, 3, s5, h-s75, SystemPanel.textShadowC, SystemPanel.orangeText);

            g.setColor(FleetUI.backHiC);
            g.fillRect(0, h-s70, w, s70);

            g.setColor(FleetUI.backLoC);
            g.fillRect((w/2)-s30, h-s65, s60, s65);

            int rallyCount = 0;
            Empire pl = player();
            for (StarSystem sys: topParent.filteredSystems) {
                if (pl.sv.hasRallyPoint(sys.id))
                    rallyCount++;
            }
            textureArea = new Area(new Rectangle2D.Float(0,0,w,h-s70));

            stopRalliesBox.setBounds(0,0,0,0);
            if (rallyCount == 0)
                return;

            int y0 = h-s52;
            g.setColor(SystemPanel.blackText);
            String desc = text("FLEETS_STOP_RALLIES_DESC", str(rallyCount));
            List<String> descLines = scaledNarrowWrappedLines(g, desc, w-s20, 2, 16, 14);
            for (String line: descLines) {
                drawString(g,line, s10, y0);
                y0 += s16;
            }

            topParent.drawRedButton(g, text("FLEETS_STOP_RALLIES"), stopRalliesBox, hoverBox, h-s30);
            Area buttonArea = new Area(new Rectangle2D.Float(s3, h-s30, topParent.SIDE_PANE_W-s18, s27));
            textureArea.add(buttonArea);
        }
        private void drawTransportHeader(Graphics2D g, int w, int h) {
            g.setFont(narrowFont(25));
            String title = text("FLEETS_SEND_TRANSPORTS");
            drawShadowedString(g, title, 3, s5, h-s75, SystemPanel.textShadowC, SystemPanel.orangeText);

            g.setColor(FleetUI.backHiC);
            g.fillRect(0, h-s70, w, s70);

            g.setColor(FleetUI.backLoC);
            g.fillRect((w/2)-s30, h-s65, s60, s65);

            int transportCount = 0;
            Empire pl = player();
            for (StarSystem sys: topParent.filteredSystems) {
                if (pl.sv.hasActiveTransport(sys.id))
                    transportCount++;
            }

            textureArea = new Area(new Rectangle2D.Float(0,0,w,h-s70));
            stopTransportsBox.setBounds(0,0,0,0);
            if (transportCount == 0)
                return;

            int y0 = h-s52;
            g.setColor(SystemPanel.blackText);
            String desc = text("FLEETS_CANCEL_TRANSPORTS_DESC", str(transportCount));
            List<String> descLines = scaledNarrowWrappedLines(g, desc, w-s20, 2, 18, 14);
            for (String line: descLines) {
                drawString(g,line, s10, y0);
                y0 += s16;
            }

            topParent.drawRedButton(g, text("FLEETS_CANCEL_TRANSPORTS"), stopTransportsBox, hoverBox, h-s30);
            Area buttonArea = new Area(new Rectangle2D.Float(s3, h-s30, topParent.SIDE_PANE_W-s18, s27));
            textureArea.add(buttonArea);
        }
        private void drawShipSpendingHeader(Graphics2D g, int w, int h) {
            g.setFont(narrowFont(25));
            String title = text("FLEETS_ADJUST_SPENDING");
            drawShadowedString(g, title, 3, s5, h-s75, SystemPanel.textShadowC, SystemPanel.orangeText);

            g.setColor(FleetUI.backHiC);
            g.fillRect(0, h-s70, w, s70);

            int y0 = h-s45;
            g.setColor(SystemPanel.blackText);
            
            int num = topParent.filteredSystems.size();
            String desc = num == 0 ? text("FLEETS_ADJUST_SPENDING_SELECT") : text("FLEETS_ADJUST_SPENDING_DESC", str(num));
            List<String> descLines = scaledNarrowWrappedLines(g, desc, w-s20, 2, 18, 14);
            for (String line: descLines) {
                drawString(g,line, s10, y0);
                y0 += s16;
            }

            textureArea = new Area(new Rectangle2D.Float(0,0,w,h-s70));
        }
        private void drawShipIcon(Graphics2D g, int x, int y, int w, int h) {
            g.setColor(Color.black);
            g.fillRect(x, y, w, h);

            shipDesignBox.setBounds(x,y,w,h);

            g.drawImage(initializedBackgroundImage(w, h), x,y, null);

            // draw design image
            if (topParent.currDesign != null) {
                Image img = topParent.currDesign.image();
                int w0 = img.getWidth(null);
                int h0 = img.getHeight(null);
                float scale = min((float)w/w0, (float)h/h0);

                int w1 = (int)(scale*w0);
                int h1 = (int)(scale*h0);
                int x1 = x+(w - w1) / 2;
                int y1 = y+(h - h1) / 2;
                g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, w0, h0, this);
            }
            if (hoverBox == shipDesignBox) {
                Stroke str1 = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(SystemPanel.yellowText);
                g.drawRect(x, y, w, h);
                g.setStroke(str1);
            }
        }
        private void drawNameSelector(Graphics2D g, int x, int y, int w, int h) {
            int leftM = x;
            int rightM = x+w;
            int buttonW = s10;
            int buttonTopY = y+s5;
            int buttonMidY = y+s15;
            int buttonBotY = y+s25;
            leftButtonX[0] = leftM; leftButtonX[1] = leftM+buttonW; leftButtonX[2] = leftM+buttonW;
            leftButtonY[0] = buttonMidY; leftButtonY[1] = buttonTopY; leftButtonY[2] = buttonBotY;

            rightButtonX[0] = rightM; rightButtonX[1] = rightM-buttonW; rightButtonX[2] = rightM-buttonW;
            rightButtonY[0] = buttonMidY; rightButtonY[1] = buttonTopY; rightButtonY[2] = buttonBotY;
            prevDesign.reset();
            nextDesign.reset();
            for (int i=0;i<leftButtonX.length;i++) {
                prevDesign.addPoint(leftButtonX[i], leftButtonY[i]);
                nextDesign.addPoint(rightButtonX[i], rightButtonY[i]);
            }

            if (hoverBox == prevDesign)
                g.setColor(SystemPanel.yellowText);
            else
                g.setColor(Color.black);
            g.fillPolygon(leftButtonX, leftButtonY, 3);

            if (hoverBox == nextDesign)
                g.setColor(SystemPanel.yellowText);
            else
                g.setColor(Color.black);
            g.fillPolygon(rightButtonX, rightButtonY, 3);

            int barX = x+s12;
            int barW = w-s24;
            int barY = y+s5;
            int barH = h-s10;
            g.setColor(Color.black);
            g.fillRect(barX, barY, barW, barH);
            shipNameBox.setBounds(barX, barY, barW, barH);

            g.setColor(FleetUI.sliderBoxBlue);
            g.setFont(narrowFont(18));
            String name = topParent.currDesign == null ? text("FLEETS_MULTIPLE_DESIGNS") : topParent.currDesign.name();
            scaledFont(g,name,barW-s5,18,8);
            int sw = g.getFontMetrics().stringWidth(name);
            int x0 = barX+((barW-sw)/2);
            drawString(g,name, x0, barY+s16);

            if (hoverBox == shipNameBox) {
                Stroke prev = g.getStroke();
                g.setColor(SystemPanel.yellowText);
                g.setStroke(stroke2);
                g.draw(shipNameBox);
                g.setStroke(prev);
            }
        }
        private void drawRallyPointButton(Graphics2D g, int x, int y, int w, int h) {
            rallyPointBox.setBounds(x, y, w, h);
            GradientPaint pt = new GradientPaint(x, y, FleetUI.rallyBackHiC, x+w, y, FleetUI.rallyBackLoC);
            Paint prevPaint = g.getPaint();
            g.setPaint(pt);
            g.fillRoundRect(x, y, w, h, s10, s10);
            g.setPaint(prevPaint);

            Stroke prevStroke = g.getStroke();
            if ((hoverBox == rallyPointBox)
            && player().canRallyFleets())
                g.setColor(SystemPanel.yellowText);
            else
                g.setColor(FleetUI.rallyBorderC);

            g.setStroke(stroke2);
            g.drawRoundRect(x,y,w,h,s15,s15);
            g.setStroke(prevStroke);

            String s = text("FLEETS_RELOCATE_LABEL");
            g.setFont(narrowFont(17));
            this.scaledFont(g, s, scaled(120), 17, 14);
            if (!player().canRallyFleets())
                g.setColor(SystemPanel.grayText);
            else if (hoverBox == rallyPointBox)	
                g.setColor(SystemPanel.yellowText);
            else
                g.setColor(SystemPanel.blackText);
            int sw = g.getFontMetrics().stringWidth(s);
            int x0 = x+((w-sw)/2);
            drawString(g,s, x0, y+h-s7);
        }
        private void drawTransportButton(Graphics2D g, int x, int y, int w, int h) {
            transportBox.setBounds(x, y, w, h);
            g.setColor(FleetUI.backLoC);
            g.fillRect(x, y, w, h);
            g.setColor(FleetUI.darkShadingC);
            g.fillRect(x+s2,y+s5,w-s2,h-s5);
            g.setColor(SystemPanel.whiteText);
            g.fillRect(x+s2,y+s5, w-s3, h-s7);
            g.setColor(FleetUI.backLoC);
            g.fillRect(x+s3,y+s6, w-s5, h-s9);
            if (!player().canSendTransports())
                g.setColor(SystemPanel.grayText);
            else if (hoverBox == transportBox)
                g.setColor(SystemPanel.yellowText);
            else
                g.setColor(SystemPanel.whiteText);
            g.setFont(narrowFont(20));
            String s = text("FLEETS_TRANSPORTS_LABEL");
            drawString(g,s, x+s10, y+s25);
            if ((hoverBox == transportBox)
            && player().canSendTransports()) {
                Stroke prevStroke = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(SystemPanel.yellowText);
                g.drawRect(x+s2,y+s5,w-s2,h-s7);
                g.setStroke(prevStroke);
            }
        }
        private void drawShipSpendingButton(Graphics2D g, int x, int y, int w, int h) {
            spendingBox.setBounds(x, y, w, h);
            g.setColor(FleetUI.backLoC);
            g.fillRect(x, y, w, h);
            g.setColor(FleetUI.darkShadingC);
            g.fillRect(x+s2,y+s5,w-s2,h-s5);
            g.setColor(SystemPanel.whiteText);
            g.fillRect(x+s2,y+s5, w-s3, h-s7);
            g.setColor(FleetUI.backLoC);
            g.fillRect(x+s3,y+s6, w-s5, h-s9);
            if (hoverBox == spendingBox)
                g.setColor(SystemPanel.yellowText);
            else
                g.setColor(SystemPanel.whiteText);
            g.setFont(narrowFont(20));
            String s = text("FLEETS_SPENDING_LABEL");
            drawString(g,s, x+s10, y+s25);
            if (hoverBox == spendingBox) {
                Stroke prevStroke = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(SystemPanel.yellowText);
                g.drawRect(x+s2,y+s5,w-s2,h-s7);
                g.setStroke(prevStroke);
            }
        }
        private void nextShipDesign(boolean click) {
            Design prev = topParent.currDesign;
            topParent.currDesign = player().shipLab().nextDesignFrom(topParent.currDesign, false);
            if (topParent.currDesign != prev) {
                if (click)
                    softClick();
                setAllShipDesigns();
                repaint();
            }
        }
        private void prevShipDesign(boolean click) {
            Design prev = topParent.currDesign;
            topParent.currDesign = player().shipLab().prevDesignFrom(topParent.currDesign, false);
            if (topParent.currDesign != prev) {
                if (click)
                    softClick();
                setAllShipDesigns();
                repaint();
            }
        }
        private void setAllShipDesigns() {
            for (StarSystem sys: topParent.filteredSystems) 
                sys.colony().shipyard().switchToDesign(topParent.currDesign);
        }
        private boolean rallyPointEnabled() { return player().canRallyFleets(); }
        private boolean transportEnabled() { return player().canSendTransports(); }
        private Image initializedBackgroundImage(int w, int h) {
            if ((starBackground == null)
            || (starBackground.getWidth() != w)
            || (starBackground.getHeight() != h)) {
                starBackground = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics g = starBackground.getGraphics();
                drawBackgroundStars(g,w,h);
                g.dispose();
            }
            return starBackground;
        }
        @Override
        public void mouseClicked(MouseEvent arg0) { }
        @Override
        public void mouseEntered(MouseEvent arg0) { }
        @Override
        public void mouseExited(MouseEvent arg0) {
            if (hoverBox != null) {
                hoverBox = null;
                repaint();
            }
        }
        @Override
        public void mousePressed(MouseEvent arg0) { }
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                return;
            int x = e.getX();
            int y = e.getY();

            if (shipDesignBox.contains(x,y)){
                nextShipDesign(true);
                topParent.repaint();
            }
            else if (shipNameBox.contains(x,y)){
                nextShipDesign(true);
                topParent.repaint();
            }
            else if (nextDesign.contains(x,y)){
                nextShipDesign(true);
                topParent.repaint();
            }
            else if (prevDesign.contains(x,y)){
                prevShipDesign(true);
                topParent.repaint();
            }
            else if (stopRalliesBox.contains(x,y)) {
                player().stopRallies(topParent.filteredSystems);
                topParent.repaint();
            }
            else if (stopTransportsBox.contains(x,y)) {
                player().cancelTransports(topParent.filteredSystems);
                topParent.repaint();
            }
            else if (rallyPointBox.contains(x,y)){
                if (rallyPointEnabled()) {
                    topParent.showRallyPanel();
                    repaint();
                }
            }
            else if (transportBox.contains(x,y)){
                if (transportEnabled()) {
                    topParent.showTransportPanel();
                    repaint();
                }
            }
            else if (spendingBox.contains(x,y)){
                topParent.showSpendingPanel();
                repaint();
            }
        }
        @Override
        public void mouseDragged(MouseEvent arg0) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Shape prevHover = hoverBox;

            hoverBox = null;

            if (shipDesignBox.contains(x,y))
                hoverBox = shipDesignBox;
            else if (shipNameBox.contains(x,y))
                hoverBox = shipNameBox;
            else if (nextDesign.contains(x,y))
                hoverBox = nextDesign;
            else if (prevDesign.contains(x,y))
                hoverBox = prevDesign;
            else if (rallyPointBox.contains(x,y))
                hoverBox = rallyPointBox;
            else if (transportBox.contains(x,y))
                hoverBox = transportBox;
            else if (spendingBox.contains(x,y))
                hoverBox = spendingBox;
            else if (stopRalliesBox.contains(x,y))
                hoverBox = stopRalliesBox;
            else if (stopTransportsBox.contains(x,y))
                hoverBox = stopTransportsBox;

            if (prevHover != hoverBox)
                repaint();
        }
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (!topParent.showingQueryPanel())
                    return;
            if (e.getWheelRotation() < 0)
                nextShipDesign(false);
            else
                prevShipDesign(false);

            topParent.repaint();
        }
    }
}
