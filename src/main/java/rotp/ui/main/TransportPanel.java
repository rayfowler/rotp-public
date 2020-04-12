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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import rotp.model.empires.Empire;
import rotp.model.empires.Race;
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.Transport;
import rotp.ui.BasePanel;

public class TransportPanel extends BasePanel {
    private static final long serialVersionUID = 1L;
    private final SpriteDisplayPanel parent;
    protected BasePanel topPane;
    protected BasePanel detailPane;
    protected BasePanel bottomPane;

    public TransportPanel(SpriteDisplayPanel p) {
        parent = p;
        initModel();
    }
    public Transport transport() { return parent.transportToDisplay(); }
    @Override
    public void animate() {
        topPane.animate();
        detailPane.animate();
    }
    private void initModel() {
        setBackground(MainUI.paneBackground());

        topPane = new TransportGraphicPane(this);
        detailPane = new TransportDetailPane(this);
        bottomPane = new TransportButtonPane(this);

        setLayout(new BorderLayout());
        if (topPane != null) {
            topPane.setPreferredSize(new Dimension(getWidth(),scaled(145)));
            add(topPane, BorderLayout.NORTH);
        }
        add(detailPane, BorderLayout.CENTER);
        if (bottomPane != null) {
            bottomPane.setPreferredSize(new Dimension(getWidth(),s40));
            add(bottomPane, BorderLayout.SOUTH);
        }
    }
    public class TransportGraphicPane extends BasePanel {
        private static final long serialVersionUID = 1L;
        private final TransportPanel parent;
        private Race displayedRace;
        private Image shipImg;
        public TransportGraphicPane(TransportPanel p){
            parent = p;
            init();
        }
        private void init() {
            setBackground(Color.black);
        }
        @Override
        public boolean hasStarBackground()     { return true; }
        @Override
        public Color starBackgroundC()         { return SystemPanel.starBackgroundC; }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();

            Empire pl = player();
            Transport tr = parent.transport();
            if (tr == null)
                return;

            // draw ship image
            if (displayedRace != tr.empire().race()) {
                displayedRace = tr.empire().race();
                shipImg = tr.empire().race().transport();
            }
            int imgW = shipImg.getWidth(null);
            int imgH = shipImg.getHeight(null);
            float scale = (float) s80 / max(imgW, imgH);
            int shipW = (int) (scale*imgW);
            int shipH = (int) (scale*imgH);
            int shipX = s40;
            int shipY = h-shipH-s10;
            g.drawImage(shipImg, shipX,shipY,shipX+shipW,shipY+shipH, 0,0,imgW,imgH, null);

            // draw title
            g.setFont(narrowFont(32));
            String str1 = text("MAIN_TRANSPORTS_EMPIRE", tr.empire().raceName());
            drawBorderedString(g, str1, 2, s15, s37, Color.black, SystemPanel.orangeText);

            // draw orbiting data, bottom up
            int y0 = h-s12;
            g.setColor(SystemPanel.whiteText);
            g.setFont(narrowFont(20));

            if (pl.knowETA(tr)) {
                String dest =  pl.sv.name(tr.destination().id);
                String str2 = dest.isEmpty() ? text("MAIN_FLEET_DEST_UNSCOUTED") : text("MAIN_FLEET_DESTINATION", dest);
                int sw2 = g.getFontMetrics().stringWidth(str2);
                g.drawString(str2, w-sw2-s10, y0);
                y0 -= s25;
                StarSystem sys = tr.from();
                String str3 = text("MAIN_FLEET_ORIGIN", pl.sv.name(sys.id));
                int sw3 = g.getFontMetrics().stringWidth(str3);
                g.drawString(str3, w-sw3-s10, y0);
                y0 -= s25;
            }
            String str4 = text("MAIN_FLEET_IN_TRANSIT");
            int sw4 = g.getFontMetrics().stringWidth(str4);
            g.drawString(str4, w-sw4-s10, y0);

            g.setColor(MainUI.shadeBorderC());
            g.fillRect(0, h-s5, w, s5);
        }
    }
    public class TransportDetailPane extends BasePanel  {
        private static final long serialVersionUID = 1L;
        final int MAX_DISPLAY = 30;
        int randX[] = { 48,344,393,229,534,599,586,536,286,368,460, 67,414,355,296,466,405,653,777,794,290,645,281,145,  2,498,290,391,603,151 };
        int randY[] = {615,306,855,601,576,330,587,528,820,468,366,406,462,578,311,329,273,491,454,559,686,691,723,360,345,229,634,826,608,505 };
        private final TransportPanel parent;
        public TransportDetailPane(TransportPanel p) {
            parent = p;
        }
        @Override
        public boolean hasStarBackground()     { return true; }
        @Override
        public Color starBackgroundC()         { return SystemPanel.starBackgroundC; }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            Transport tr = parent.transport();
            if (tr == null)
                return;

            drawTransports(g,tr,0,0,w,h);

            // draw title
            g.setFont(narrowFont(20));
            String str1 = text("MAIN_TRANSPORTS_COUNT", tr.size());
            int sw = g.getFontMetrics().stringWidth(str1);
            int x0 = (w-sw)/2;
            drawBorderedString(g, str1, 2, x0, s24, Color.black, SystemPanel.orangeText);
        }
        private void drawTransports(Graphics2D g, Transport tr, int x, int y, int w, int h) {
            Image img = tr.empire().race().transport();
            int n = min(tr.size(),MAX_DISPLAY);
            for (int i=n-1;i>=0;i--)
                drawTransport(g, img, w, h, i);
        }
        private void drawTransport(Graphics2D g, Image img, int w, int h, int n) {
            int imgW = img.getWidth(null);
            int imgH = img.getHeight(null);

            int dispW = w*4/(n+5);
            int dispH = imgH*dispW/imgW;
            int y0 = (int) (h*randY[n]/1000.0);
            int x0 = (int) (w*randX[n]/1000.0);
            g.drawImage(img, x0, y0, x0+dispW, y0+dispH, 0, 0, imgW, imgH, null);
        }
    }
    public class TransportButtonPane extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        private final TransportPanel parent;
        private final Color buttonShadowC = new Color(33,33,33);
        int leftM, midM1, midM2, rightM;
        private LinearGradientPaint fullGrayBackC;
        private boolean initted = false;

        private Shape hoverBox;
        private final Rectangle destBox = new Rectangle();
        private Shape textureClip;
        public TransportButtonPane(TransportPanel p) {
            parent = p;
            init();
        }
        private void init() {
            addMouseListener(this);
            addMouseMotionListener(this);
            setBackground(MainUI.shadeBorderC());
        }
        private void initGradients() {
            initted = true;
            int w = getWidth();
            leftM = s2;
            midM1 = (w*3/5)-s2;
            midM2 = midM1+s4;
            rightM = w-s2;
            Point2D start = new Point2D.Float(leftM, 0);
            Point2D end = new Point2D.Float(rightM, 0);
            float[] dist = {0.0f, 0.5f, 1.0f};

            Color grayEdgeC = new Color(59,59,59);
            Color grayMidC = new Color(92,92,92);
            Color[] grayColors = {grayEdgeC, grayMidC, grayEdgeC };

            fullGrayBackC = new LinearGradientPaint(start, end, dist, grayColors);
        }
        @Override
        public String textureName()            { return TEXTURE_GRAY; }
        @Override
        public Shape textureClip()     { return textureClip; }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);

            if (!initted)
                initGradients();

            clearButtons();

            Transport tr = parent.transport();
            if (player().knowETA(tr)) {
                StarSystem sys = tr.destination();
                drawShowDestButton(g, tr, sys);
            }
            else
                drawCancelButton(g);
        }
        private void clearButtons() {
            destBox.setBounds(0,0,0,0);
        }
        private void drawShowDestButton(Graphics2D g, Transport tr, StarSystem sys) {
            drawButton(g,fullGrayBackC,text("MAIN_TRANSPORTS_SHOW_DEST",player().sv.name(sys.id), tr.travelTurnsRemaining()), destBox, leftM, rightM);
        }
        private void drawCancelButton(Graphics2D g) {
            drawButton(g,fullGrayBackC,text("MAIN_TRANSPORT_CANCEL"), destBox, leftM, rightM);
        }
        private void drawButton(Graphics2D g, LinearGradientPaint gradient, String label, Rectangle actionBox, int x1, int x2) {
            int y = s4;
            int h = getHeight()-s7;
            int w = x2 - x1;
            if (actionBox != null)
                actionBox.setBounds(x1,y,w,h);
            g.setColor(buttonShadowC);
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.drawRoundRect(x1+s3,y+s2,w-s2,h,s10,s10);
            g.setStroke(prev);

            g.setPaint(gradient);
            g.fillRoundRect(x1,y,w,h,s10,s10);

            textureClip = new RoundRectangle2D.Float(x1,y,w,h,s10,s10);

            boolean hovering = (actionBox != null) && (actionBox == hoverBox);
            Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;

            g.setFont(narrowFont(20));
            int sw = g.getFontMetrics().stringWidth(label);
            int x0 = x1+((w-sw)/2);
            drawShadowedString(g, label, 3, x0, y+h-s11, SystemPanel.textShadowC, c0);

            g.setColor(c0);
            Stroke prev2 = g.getStroke();
            g.setStroke(stroke2);
            g.drawRoundRect(x1+s1,y,w-s2,h,s10,s10);
            g.setStroke(prev2);
        }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            Shape prevHover = hoverBox;
            hoverBox = null;
            if (destBox.contains(x,y))
                hoverBox = destBox;

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

            if (destBox.contains(x,y)) {
                StarSystem sys = parent.transport().destination();
                parent.parent.parent.clickedSprite(sys);
            }
        }
    }
}
