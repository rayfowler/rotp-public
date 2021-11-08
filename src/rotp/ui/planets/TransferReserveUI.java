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
package rotp.ui.planets;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
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
import java.awt.geom.Point2D;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.main.SystemPanel;

public final class TransferReserveUI extends BasePanel implements MouseListener, MouseWheelListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    private static final Color backgroundHaze = new Color(0,0,0,160);
    private static final Color yellowText = new Color(255,240,78);
    private static final Color backC = new Color(112,85,68);
    private static final Color borderC = new Color(112,85,68,128);
    private static final Color okButtonBdrC = new Color(158,165,156);
    private static final Color cancelButtonBdrC = new Color(184,165,143);

    private LinearGradientPaint largeRedBackC;
    private LinearGradientPaint largeGreenBackC;

    private static final Color sliderButtonColor = Color.black;
    private static final Color sliderHighlightColor = new Color(255,255,255);
    private static final Color sliderBoxBlue = new Color(34,140,142);
    static final int MAX_TICKS = 50;

    private List<StarSystem> targetSystems;

    private Shape hoverBox;
    private boolean initted = false;

    private final Rectangle cancelButton =  new Rectangle();
    private final Rectangle okButton = new Rectangle();
    private final Rectangle reserveBox = new Rectangle();
    private final Polygon leftArrow = new Polygon();
    private final Polygon rightArrow = new Polygon();

    int boxAreaL, boxAreaW;
    int amt = 0;

    // polygon coordinates for left & right increment buttons
    private final int leftButtonX[] = new int[3];
    private final int leftButtonY[] = new int[3];
    private final int rightButtonX[] = new int[3];
    private final int rightButtonY[] = new int[3];
    Shape textureClip;

    public TransferReserveUI() {
        initModel();
    }
    private void initModel() {
        setOpaque(false);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }
    public void targetSystems(List<StarSystem> syslist)
    {
        targetSystems = syslist;
        if(!syslist.isEmpty())
        {
            setDefaultAmt(syslist.get(0));
        }
    }
    @Override
    public String textureName()     { return TEXTURE_BROWN; }
    @Override
    public Shape textureClip()     { return textureClip; }
    @Override
    public void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;

        super.paintComponent(g);

        int w = getWidth();
        int h = getHeight();

        // draw background "haze"
        g.setColor(backgroundHaze);
        g.fillRect(0, 0, w, h);

        // get length of title and determine box width
        g.setFont(narrowFont(24));
        String title = text("PLANETS_TRANSFER_DESC", player().sv.name(targetSystems.get(0).id));
        if(targetSystems.size() > 1)
        {
            title = text("PLANETS_TRANSFER_DESC", targetSystems.size());
            title +=" "+text("SYSTEMS_TITLE");
        }           

        int titleSW = g.getFontMetrics().stringWidth(title);

        int boxWidth = titleSW+s70;
        int boxHeight = scaled(150);

        int x0 = (w - boxWidth)/2;
        int y0 = h/3;

        // draw box
        g.setColor(borderC);
        g.fillRect(x0, y0, boxWidth, boxHeight);
        g.setColor(backC);
        g.fillRect(x0+s15, y0+s15, boxWidth-s30, boxHeight-s30);

        textureClip = new Rectangle(x0+s15, y0+s15, boxWidth-s30, boxHeight-s30);

        // draw title
        g.setColor(yellowText);
        int y1 = y0+s45;
        int x1 = x0+s30;
        drawShadowedString(g, title, 3, x1, y1, SystemPanel.textShadowC, SystemPanel.whiteText);

        // slider arrow buttons
        int arrowLeftM = x0+s30;
        int arrowRightM = x0+boxWidth-s100;
        int arrowW = s8;
        int arrowTopY = y1+s15;
        int arrowH = s18;
        leftButtonX[0] = arrowLeftM; leftButtonX[1] = arrowLeftM+arrowW; leftButtonX[2] = arrowLeftM+arrowW;
        leftButtonY[0] = arrowTopY+(arrowH/2); leftButtonY[1] = arrowTopY; leftButtonY[2] = arrowTopY+arrowH;
        rightButtonX[0] = arrowRightM; rightButtonX[1] = arrowRightM-arrowW; rightButtonX[2] = arrowRightM-arrowW;
        rightButtonY[0] = arrowTopY+(arrowH/2); rightButtonY[1] = arrowTopY; rightButtonY[2] = arrowTopY+arrowH;
        leftArrow.reset();
        rightArrow.reset();
        for (int i=0;i<3;i++) {
            leftArrow.addPoint(leftButtonX[i], leftButtonY[i]);
            rightArrow.addPoint(rightButtonX[i], rightButtonY[i]);
        }

        if (hoverBox == leftArrow)
            g.setColor(Color.yellow);
        else
            g.setColor(sliderButtonColor);
        g.fillPolygon(leftButtonX, leftButtonY, 3);
        if (hoverBox == rightArrow)
            g.setColor(Color.yellow);
        else
            g.setColor(sliderButtonColor);
        g.fillPolygon(rightButtonX, rightButtonY, 3);

        // slider box
        int boxL = arrowLeftM+arrowW+s4;
        int boxR = arrowRightM-arrowW-s4;
        int boxW = boxR - boxL;
        int boxTopY = arrowTopY;
        int boxH = arrowH;
        int boxBorderW = s2;

        g.setColor(Color.black);
        g.fillRect(boxL, boxTopY, boxW, boxH);
        g.setColor(sliderBoxBlue);
        g.fillRect(boxL, boxTopY+s1, boxW*amt/MAX_TICKS, boxH-s2);

        if (hoverBox == reserveBox) {
            g.setColor(Color.yellow);
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.drawRect(boxL, boxTopY, boxW, boxH);
            g.setStroke(prev);
        }

        boxAreaL = boxL+boxBorderW;
        boxAreaW = boxW-boxBorderW-boxBorderW;
        reserveBox.setBounds(boxAreaL, boxTopY, boxAreaW, boxH);

        // amount string
        g.setFont(narrowFont(20));
        float pct = (float) amt / MAX_TICKS;
        int transferAmt = (int) (pct*player().totalReserve());
        String amtString = text("PLANETS_AMT_BC", transferAmt);
        int sw = g.getFontMetrics().stringWidth(amtString);
        int amtX = x0+boxWidth-s40-sw;
        g.setColor(SystemPanel.blackText);
        drawString(g,amtString, amtX, boxTopY+boxH-s3);

        // button vars
        int buttonM = s30;  // L/R margin
        int buttonM2 = s20; // space between buttons
        int buttonW = (boxWidth-buttonM-buttonM-buttonM2)/2;
        int buttonH = s32;
        int buttonY = y0+boxHeight-buttonH-s30;
        int button1X = x0+buttonM;
        int button2X = x0+buttonM+buttonW+buttonM2;
        g.setFont(narrowFont(20));

        // set up background gradients
        if (!initted)
            init(button1X, button1X+buttonW, button2X, button2X+buttonW);

        // transfer button
        int cnr = s2;
        okButton.setBounds(button1X, buttonY, buttonW, buttonH);
        g.setColor(SystemPanel.textShadowC);
        g.fillRoundRect(button1X+s4, buttonY+s4, buttonW, buttonH, cnr, cnr);
        g.setPaint(largeGreenBackC);
        g.fillRoundRect(button1X, buttonY, buttonW, buttonH, cnr, cnr);
        Stroke prev = g.getStroke();
        g.setStroke(stroke1);
        if (hoverBox == okButton)
            g.setColor(Color.yellow);
        else
            g.setColor(okButtonBdrC);
        g.drawRoundRect(button1X, buttonY, buttonW, buttonH, cnr, cnr);
        g.setStroke(prev);
        Color c1 = hoverBox == okButton ? Color.yellow : SystemPanel.whiteText;
        String acceptText = text("PLANETS_TRANSFER_ACCEPT");
        int sw2 = g.getFontMetrics().stringWidth(acceptText);
        int text2X = button1X+ ((buttonW - sw2) / 2);
        drawShadowedString(g0, acceptText, 3, text2X, buttonY+buttonH-s10, SystemPanel.textShadowC, c1);

        // cancel button
        cancelButton.setBounds(button2X, buttonY, buttonW, buttonH);
        g.setColor(SystemPanel.textShadowC);
        g.fillRoundRect(button2X+s4, buttonY+s4, buttonW, buttonH, cnr, cnr);
        g.setPaint(largeRedBackC);
        g.fillRoundRect(button2X, buttonY, buttonW, buttonH, cnr, cnr);
        prev = g.getStroke();
        g.setStroke(stroke1);
        if (hoverBox == cancelButton)
            g.setColor(Color.yellow);
        else
            g.setColor(cancelButtonBdrC);
        g.drawRoundRect(button2X, buttonY, buttonW, buttonH, cnr, cnr);
        g.setStroke(prev);
        Color c2 = hoverBox == cancelButton ? Color.yellow : SystemPanel.whiteText;
        String cancelText = text("PLANETS_TRANSFER_CANCEL");
        int sw3 = g.getFontMetrics().stringWidth(cancelText);
        int text3X = button2X+ ((buttonW - sw3) / 2);
        drawShadowedString(g0, cancelText, 3, text3X, buttonY+buttonH-s10, SystemPanel.textShadowC, c2);
    }
    private void increment()   { setAmt(amt+1); }
    private void decrement()   { setAmt(amt-1); }
    private void setAmt(int i) { amt = bounds(0, i, MAX_TICKS); }
    private void setDefaultAmt(StarSystem sys) {
        Empire pl = player();
        float neededRsv = pl.sv.colony(sys.id).maxReserveNeeded();
        float totalRsv = pl.sv.empire(sys.id).totalReserve();
        int ticks = (int) Math.ceil(MAX_TICKS*neededRsv/totalRsv);
        setAmt(ticks);
    }
    private void setHoverSprite(int x, int y) {
        hoverBox = null;

        if (cancelButton.contains(x, y))
            hoverBox = cancelButton;
        else if (okButton.contains(x, y))
            hoverBox = okButton;
        else if (leftArrow.contains(x,y))
            hoverBox = leftArrow;
        else if (rightArrow.contains(x,y))
            hoverBox = rightArrow;
        else if (reserveBox.contains(x,y))
            hoverBox = reserveBox;
    }
    private void init(int leftX0, int leftX1, int rightX0, int rightX1) {
        initted = true;
        int w = getWidth();
        Point2D start1 = new Point2D.Float(leftX0, 0);
        Point2D end1 = new Point2D.Float(leftX1, 0);
        Point2D start2 = new Point2D.Float(rightX0, 0);
        Point2D end2 = new Point2D.Float(rightX1, 0);
        float[] dist = {0.0f, 0.2f, 0.5f, 0.8f, 1.0f};

        Color greenEdgeC = new Color(44,59,30);
        Color greenMidC = new Color(71,93,48);
        Color[] greenColors = {greenEdgeC, greenEdgeC, greenMidC, greenEdgeC, greenEdgeC };

        Color redEdgeC = new Color(100,70,50);
        Color redMidC = new Color(161,110,76);
        Color[] redColors = {redEdgeC, redEdgeC, redMidC, redEdgeC, redEdgeC };

        largeGreenBackC = new LinearGradientPaint(start1, end1, dist, greenColors);
        largeRedBackC = new LinearGradientPaint(start2, end2, dist, redColors);
    }
    private void exit() {
        hoverBox = null;
        amt = 0;
        softClick();
        disableGlassPane();
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_ESCAPE) {
            exit();
            return;
        }
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
        int prevAmt = amt;
        if (hoverBox == cancelButton) {
            exit();
            return;
        }
        else if (hoverBox == okButton) {
            float pct = (float) amt / MAX_TICKS;
            int amount = (int) (pct*player().totalReserve());
            for(StarSystem sys : targetSystems)
            {
                player().allocateReserve(sys.colony(), amount);
                if(player().totalReserve() == 0)
                    break;
            }
            exit();
            return;
        }
        else if (hoverBox == leftArrow)
            decrement();
        else if (hoverBox == rightArrow)
            increment();
        else if (hoverBox == reserveBox) 
            setAmt(MAX_TICKS*(e.getX()-boxAreaL)/boxAreaW);

        if (amt != prevAmt) {
            softClick();
            repaint();
        }
        else
            misClick();
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        Shape prevHover = hoverBox;
        setHoverSprite(e.getX(),e.getY());

        if (prevHover != hoverBox)
            repaint();
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        Shape prevHover = hoverBox;
        setHoverSprite(e.getX(),e.getY());

        if (prevHover != hoverBox)
            repaint();
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int count = e.getUnitsToScroll();
        if (hoverBox == reserveBox) {
            int prevAmt = amt;
            if (count < 0)
                increment();
            else if (count > 0)
                decrement();
            if (amt != prevAmt) 
                repaint();   
        }
    }
}
