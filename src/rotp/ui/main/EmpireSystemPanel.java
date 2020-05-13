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
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import rotp.model.colony.Colony;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.Design;
import rotp.ui.BasePanel;

public class EmpireSystemPanel extends SystemPanel {
    private static final long serialVersionUID = 1L;
    static final Color sliderButtonColor = new Color(153,0,11);
    static final Color sliderHighlightColor = new Color(255,255,255);
    static final Color spendingPaneHighlightColor = new Color(96,96,224);
    static final Color productionGreenColor = new Color(89, 240, 46);

    static final Color darkBrown = new Color(45,14,5);
    static final Color brown = new Color(64,24,13);
    static final Color darkText = new Color(40,40,40);
    static final Color sliderBoxBlue = new Color(34,140,142);

    private SystemViewInfoPane topPane;
    private EmpireColonySpendingPane spendingPane;
    private EmpireShipPane shipPane;

    public EmpireSystemPanel(SpriteDisplayPanel p) {
        parentSpritePanel = p;
        init();
    }
    private void init() {
        initModel();
    }
    @Override
    public String subPanelTextureName()    { return TEXTURE_GRAY; }
    @Override
    protected void showDefaultDetail()  { detailLayout.show(detailCardPane, EMPIRE_DETAIL);  }
    @Override
    protected void showStarDetail()     { detailLayout.show(detailCardPane, STAR_DETAIL); }
    @Override
    protected void showPlanetDetail()   { detailLayout.show(detailCardPane, PLANET_DETAIL); }

    @Override
    public void animate() {
        topPane.animate();
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        switch (k) {
            case KeyEvent.VK_S:
                nextShipDesign();
                return;
            case KeyEvent.VK_1:
            case KeyEvent.VK_2:
            case KeyEvent.VK_3:
            case KeyEvent.VK_4:
            case KeyEvent.VK_5:
                spendingPane.keyPressed(e);
        }
    }
    public void nextShipDesign() {
        StarSystem sys = parentSpritePanel.systemViewToDisplay();
        if (sys == null)
            return;
        Colony c = sys.colony();
        if (c == null)
            return;
        if (!c.shipyard().canCycleDesign()) 
            misClick();
        else {
            softClick();
            c.shipyard().goToNextDesign();
            parentSpritePanel.repaint();
        }
    }
    public void prevShipDesign() {
        StarSystem sys = parentSpritePanel.systemViewToDisplay();
        if (sys == null)
            return;
        Colony c = sys.colony();
        if (c == null)
            return;
        if (!c.shipyard().canCycleDesign()) {
            misClick();
        }
        else {
            softClick();
            c.shipyard().goToPrevDesign();
            parentSpritePanel.repaint();
        }
    }
    @Override
    protected BasePanel topPane() {
        if (topPane == null)
            topPane = new SystemViewInfoPane(this);
        return topPane;
    }
    @Override
    protected BasePanel detailPane() {
        BasePanel detailTopPane = new BasePanel();
        detailTopPane.setOpaque(true);
        detailTopPane.setBackground(dataBorders);

        BorderLayout layout = new BorderLayout();
        layout.setVgap(s1);
        detailTopPane.setLayout(layout);
        detailTopPane.setPreferredSize(new Dimension(getWidth(),scaled(110)));
        detailTopPane.add(new EmpireColonyFoundedPane(this, parentSpritePanel.parent, MainUI.paneBackground()), BorderLayout.NORTH);
        detailTopPane.add(new EmpireColonyInfoPane(this, MainUI.paneBackground(), dataBorders, SystemPanel.yellowText, darkText), BorderLayout.CENTER);
        Color textC = new Color(204,204,204);
        spendingPane = new EmpireColonySpendingPane(this, MainUI.paneBackground(), textC, labelBorderHi, labelBorderLo);
        shipPane = new EmpireShipPane(this);

        BasePanel empireDetailPane = new BasePanel();
        empireDetailPane.setOpaque(false);
        BorderLayout layout0 = new BorderLayout();
        empireDetailPane.setLayout(layout0);
        empireDetailPane.add(detailTopPane, BorderLayout.NORTH);
        empireDetailPane.add(spendingPane, BorderLayout.CENTER);
        empireDetailPane.add(shipPane, BorderLayout.SOUTH);

        detailCardPane = new JPanel();
        detailCardPane.setOpaque(false);
        detailCardPane.setLayout(detailLayout);
        detailCardPane.add(empireDetailPane, EMPIRE_DETAIL);
        detailLayout.show(detailCardPane, EMPIRE_DETAIL);

        BasePanel pane = new BasePanel();
        pane.setLayout(new BorderLayout());
        pane.setBackground(MainUI.paneBackground());
        pane.add(detailCardPane, BorderLayout.CENTER);

        return pane;
    }
    class EmpireShipPane extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        private final EmpireSystemPanel parent;
        // polygon coordinates for left & right increment buttons
        private final int leftButtonX[] = new int[3];
        private final int leftButtonY[] = new int[3];
        private final int rightButtonX[] = new int[3];
        private final int rightButtonY[] = new int[3];

        private final Rectangle shipDesignBox = new Rectangle();
        private final Rectangle rallyPointBox = new Rectangle();
        private final Rectangle transportBox  = new Rectangle();
        private final Rectangle shipNameBox = new Rectangle();
        private final Polygon prevDesign = new Polygon();
        private final Polygon nextDesign = new Polygon();
        private Shape hoverBox;

        Color textColor = newColor(204,204,204);
        Color gray20C = newColor(20,20,20);
        Color darkShadingC = newColor(50,50,50);
        Color buttonC = newColor(110,110,110);
        Color gray70C = newColor(70,70,70);
        Color gray90C = newColor(90,90,90);
        Color gray115C = newColor(115,115,115);
        Color gray150C = newColor(150,150,150);
        Color gray175C = newColor(175,175,175);
        Color gray190C = newColor(190,190,190);
        EmpireShipPane(EmpireSystemPanel p) {
            parent = p;
            init();
        }
        private void init() {
            setBackground(MainUI.paneBackground());
            setPreferredSize(new Dimension(getWidth(), scaled(150)));
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }
        @Override
        public String textureName()            { return TEXTURE_GRAY; }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            int w = getWidth();
            int h = getHeight();

            int midMargin = scaled(105);

            StarSystem sys = parentSpritePanel.systemViewToDisplay();
            Colony col = sys == null ? null : sys.colony();
            if (col == null)
                return;

            drawTitle(g);
            drawShipIcon(g, col, s5,s30,midMargin-s15,s75);
            drawShipCompletion(g, col, midMargin,h-scaled(116),w-s10-midMargin,s30);
            drawNameSelector(g, col, midMargin,h-scaled(103),w-s10-midMargin,s30);
            drawRallyPointButton(g,midMargin,h-s70,w-s10-midMargin,s25);
            drawTransportButton(g,0,h-s35,w,s35);
        }
        private void drawTitle(Graphics g) {
            g.setColor(MainUI.shadeBorderC());
            g.fillRect(0, 0, getWidth(), s3);
            g.setFont(narrowFont(20));
            g.setColor(Color.black);
            String str = text("MAIN_COLONY_SHIPYARD_CONSTRUCTION");
            drawShadowedString(g, str, 2, s5, s22, MainUI.shadeBorderC(), textColor);
        }
        private void drawShipIcon(Graphics2D g,  Colony c, int x, int y, int w, int h) {
            g.setColor(Color.black);
            g.fillRect(x, y, w, h);

            shipDesignBox.setBounds(x,y,w,h);

            g.drawImage(initializedBackgroundImage(w, h), x,y, null);

            if (c == null)
                return;

            // draw design image
            Design d = c.shipyard().design();
            Image img = d.image();
            if (img == null)
                return;
            int w0 = img.getWidth(null);
            int h0 = img.getHeight(null);
            float scale = min((float)w/w0, (float)h/h0);

            int w1 = (int)(scale*w0);
            int h1 = (int)(scale*h0);
            int x1 = x+(w - w1) / 2;
            int y1 = y+(h - h1) / 2;
            g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, w0, h0, this);

            if (hoverBox == shipDesignBox) {
                Stroke prev = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(SystemPanel.yellowText);
                g.drawRect(x, y, w, h);
                g.setStroke(prev);
            }

            // draw expected build count
            String count;
            if (d.scrapped()) {
                count = text("MAIN_COLONY_SHIP_SCRAPPED");
                g.setColor(Color.red);
                g.setFont(narrowFont(20));
            }
            else {
                int i = c.shipyard().upcomingShipCount();
                if (i == 0)
                    return;
                count = Integer.toString(i);
                g.setColor(SystemPanel.yellowText);
                g.setFont(narrowFont(20));
            }

            int sw = g.getFontMetrics().stringWidth(count);
            g.drawString(count, x+w-s5-sw, y+h-s5);
        }
        private void drawShipCompletion(Graphics2D g, Colony c, int x, int y, int w, int h) {
            if (c == null)
                return;

            String result = c.shipyard().shipCompletionResult();
            g.setColor(Color.black);
            g.setFont(narrowFont(16));
            g.drawString(result, x+s12, y+s10);
        }
        private void drawNameSelector(Graphics2D g, Colony c, int x, int y, int w, int h) {
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

            if (c != null) {
                g.setColor(sliderBoxBlue);
                String name = c.shipyard().design().name();
                if (name == null) {
                    int i = 0;
                    name = "";
                }
                scaledFont(g, name, barW-s5, 18, 14);
                int sw = g.getFontMetrics().stringWidth(name);
                int x0 = barX+((barW-sw)/2);
                g.drawString(name, x0, barY+s16);
            }

            if (hoverBox == shipNameBox) {
                Stroke prev = g.getStroke();
                g.setColor(SystemPanel.yellowText);
                g.setStroke(stroke2);
                g.draw(shipNameBox);
                g.setStroke(prev);
            }
        }
        private void drawRallyPointButton(Graphics2D g, int x, int y, int w, int h) {
            StarSystem sys = parentSpritePanel.systemViewToDisplay();
            if (sys == null)
                return;

            rallyPointBox.setBounds(x, y, w, h);
            GradientPaint pt = new GradientPaint(x, y, gray175C, x+w, y, gray115C);
            Paint prevPaint = g.getPaint();
            g.setPaint(pt);
            g.fillRoundRect(x, y, w, h, s10, s10);
            g.setPaint(prevPaint);

            Stroke prevStroke = g.getStroke();
            if ((hoverBox == rallyPointBox)
            && player().canRallyFleetsFrom(id(sys)))
                g.setColor(SystemPanel.yellowText);
            else
                g.setColor(gray175C);

            g.setStroke(stroke2);
            g.drawRoundRect(x,y,w,h,s15,s15);
            g.setStroke(prevStroke);

            String s = text("MAIN_COLONY_RELOCATE_LABEL");
            scaledFont(g, s, w-s10, 17, 15);
            if (!player().canRallyFleetsFrom(id(sys)))
                g.setColor(gray90C);
            else if (hoverBox == rallyPointBox)	
                g.setColor(SystemPanel.yellowText);
            else
                g.setColor(gray20C);
            int sw = g.getFontMetrics().stringWidth(s);
            int x0 = x+((w-sw)/2);
            g.drawString(s, x0, y+h-s7);
        }
        private void drawTransportButton(Graphics2D g, int x, int y, int w, int h) {
            StarSystem sys = parentSpritePanel.systemViewToDisplay();
            if (sys == null)
                return;

            transportBox.setBounds(x, y, w, h);
            g.setColor(MainUI.shadeBorderC());
            g.fillRect(x, y, w, h);
            g.setColor(darkShadingC);
            g.fillRect(x+s2,y+s5,w-s2,h-s5);
            g.setColor(gray190C);
            g.fillRect(x+s2,y+s5, w-s3, h-s7);
            g.setColor(buttonC);
            g.fillRect(x+s3,y+s6, w-s5, h-s9);
            if (!player().canSendTransportsFrom(sys))
                g.setColor(gray70C);
            else if (hoverBox == transportBox)
                g.setColor(SystemPanel.yellowText);
            else
                g.setColor(textColor);
            g.setFont(narrowFont(20));
            String s = text("MAIN_COLONY_TRANSPORTS_LABEL");
            g.drawString(s, x+s10, y+s25);
            if ((hoverBox == transportBox)
            && player().canSendTransportsFrom(sys)) {
                Stroke prevStroke = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(SystemPanel.yellowText);
                g.drawRect(x+s2,y+s5,w-s2,h-s7);
                g.setStroke(prevStroke);
            }
        }
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
        private boolean rallyPointEnabled() { return player().canRallyFleetsFrom(id(parentSpritePanel.systemViewToDisplay())); }
        private boolean transportEnabled() { return player().canSendTransportsFrom(parentSpritePanel.systemViewToDisplay()); }
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
                nextShipDesign();
                parent.repaint();
            }
            else if (shipNameBox.contains(x,y)){
                nextShipDesign();
                parent.repaint();
            }
            else if (nextDesign.contains(x,y)){
                nextShipDesign();
                parent.repaint();
            }
            else if (prevDesign.contains(x,y)){
                prevShipDesign();
                parent.repaint();
            }
            else if (rallyPointBox.contains(x,y)){
                if (rallyPointEnabled()) {
                    StarSystem sys =  parentSpritePanel.systemViewToDisplay();
                    if (sys != null)
                        parentSpritePanel.parent.clickedSprite(sys.rallySprite());
                    parentSpritePanel.repaint();
                }
            }
            else if (transportBox.contains(x,y)){
                if (transportEnabled()) {
                    StarSystem sys =  parentSpritePanel.systemViewToDisplay();
                    if (sys != null)
                        parentSpritePanel.parent.clickedSprite(sys.transportSprite());
                    parentSpritePanel.repaint();
                }
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

            if (prevHover != hoverBox)
                repaint();
        }
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getWheelRotation() < 0)
                nextShipDesign();
            else
                prevShipDesign();

            parent.repaint();
        }
    }
}
