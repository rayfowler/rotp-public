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
package rotp.ui.design;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.List;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipImage;
import rotp.ui.BasePanel;
import rotp.ui.main.SystemPanel;

public final class ConfirmScrapUI extends BasePanel implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    private static final Color backgroundHaze = new Color(0,0,0,160);

    private static final int DIALOG_W = 310;
    private static final int DIALOG_H = 375;
    private static final Color borderC = new Color(112,85,68,128);
    private static final Color backC = new Color(112,85,68);
    private static final Color redEdgeC = new Color(72,14,14);
    private static final Color redMidC = new Color(126,28,28);
    private static final Color brownEdgeC = new Color(59,44,30);
    private static final Color brownMidC = new Color(93,70,48);

    private LinearGradientPaint cancelBackground;
    private LinearGradientPaint scrapBackground;
    private Shape hoverTarget;
    private final Rectangle cancelButtonArea = new Rectangle();
    private final Rectangle scrapButtonArea = new Rectangle();

    private ShipDesign targetDesign;

    public ConfirmScrapUI() {
        setOpaque(false);
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    public void targetDesign(ShipDesign d) {
        targetDesign = d;
    }
    @Override
    public void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        super.paintComponent(g);

        int w = getWidth();
        int h = getHeight();
        int bdr = s10;
        int mgn = s10;

        // draw background "haze"
        g.setColor(backgroundHaze);
        g.fillRect(0, 0, w, h);


        // draw border
        int dlgW = scaled(DIALOG_W);
        int dlgH = scaled(DIALOG_H);
        int dlgX = (w-dlgW)/2;
        int dlgY = (h-dlgH)/2;
        g.setColor(borderC);
        g.fillRect(dlgX,dlgY,dlgW,dlgH);

        // draw Box
        int boxX = dlgX+bdr;
        int boxY = dlgY+bdr;
        int boxW = dlgW-bdr-bdr;
        int boxH = dlgH-bdr-bdr;
        g.setColor(backC);
        g.fillRect(boxX, boxY, boxW, boxH);

        // draw ship
        int shipW = boxW-mgn-mgn;
        int shipH = shipW*3/4;
        drawShipIcon(g, boxX+mgn, boxY+mgn, shipW, shipH);
        int count = galaxy().ships.shipDesignCount(player().id, targetDesign.id());

        int y0 = boxY+mgn+shipH;
        int x0 = boxX+mgn+mgn;
        int w0 = shipW-mgn-mgn;
        String titleString = text("SHIP_DESIGN_SCRAP_CONFIRM1", targetDesign.name());

        g.setFont(narrowFont(20));
        List<String> titleLines = wrappedLines(g, titleString, w0);

        int y1 = y0 + s25;
        g.setColor(SystemPanel.whiteText);
        for (String line: titleLines) {
            int sw = g.getFontMetrics().stringWidth(line);
            int x1 = (w-sw)/2;
            drawString(g,line, x1, y1);
            y1 += s20;
        }

        g.setFont(narrowFont(16));
        int y2 = y1 + s5;
        String amtString = count == 0 ? text("SHIP_DESIGN_SCRAP_CONFIRM3") : text("SHIP_DESIGN_SCRAP_CONFIRM2", count, (int) targetDesign.scrapValue(count));
        List<String> amtLines = wrappedLines(g, amtString,  w0);
        g.setColor(SystemPanel.blackText);
        for (String line: amtLines) {
            int sw = g.getFontMetrics().stringWidth(line);
            int x1 = (w-sw)/2;
            drawString(g,line, x1, y2);
            y2 += s16;
        }

        int y3 = y2+s10;
        // button vars
        int buttonH = s25;
        int buttonW = (boxW-(2*bdr)-(3*mgn))/2;
        int buttonY = y3;
        int button1X = boxX+s10;
        int button2X = boxX+boxW-buttonW-s10;
        g.setFont(narrowFont(20));

        if (scrapBackground == null) {
            float[] dist = {0.0f, 0.5f, 1.0f};
            Point2D ptStart = new Point2D.Float(button2X, 0);
            Point2D ptEnd = new Point2D.Float(button2X + buttonW, 0);
            Color[] yesColors = {redEdgeC, redMidC, redEdgeC};
            scrapBackground = new LinearGradientPaint(ptStart, ptEnd, dist, yesColors);
        }
        scrapButtonArea.setBounds(button1X, buttonY, buttonW, buttonH);
        boolean hovering = hoverTarget == scrapButtonArea;
        g.setPaint(scrapBackground);
        g.fillRoundRect(button1X, buttonY, buttonW, buttonH, s3, s3);
        Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
        g.setColor(c0);
        Stroke prevStr = g.getStroke();
        g.setStroke(BasePanel.stroke1);
        g.drawRoundRect(button1X, buttonY, buttonW, buttonH, s3, s3);
        g.setStroke(prevStr);
        String str = text("SHIP_DESIGN_SCRAP_OK");
        int sw = g.getFontMetrics().stringWidth(str);
        int x2a = button1X + ((buttonW - sw) / 2);
        drawBorderedString(g, str, x2a, buttonY + buttonH - s7, SystemPanel.textShadowC, c0);


        if (cancelBackground == null) {
            float[] dist = {0.0f, 0.5f, 1.0f};
            Point2D ptStart = new Point2D.Float(button2X, 0);
            Point2D ptEnd = new Point2D.Float(button2X + buttonW, 0);
            Color[] yesColors = {brownEdgeC, brownMidC, brownEdgeC};
            cancelBackground = new LinearGradientPaint(ptStart, ptEnd, dist, yesColors);
        }
        cancelButtonArea.setBounds(button2X, buttonY, buttonW, buttonH);
        hovering = hoverTarget == cancelButtonArea;
        g.setPaint(cancelBackground);
        g.fillRoundRect(button2X, buttonY, buttonW, buttonH, s3, s3);
        c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
        g.setColor(c0);
        prevStr = g.getStroke();
        g.setStroke(stroke1);
        g.drawRoundRect(button2X, buttonY, buttonW, buttonH, s3, s3);
        g.setStroke(prevStr);
        str = text("SHIP_DESIGN_SCRAP_CANCEL");
        sw = g.getFontMetrics().stringWidth(str);
        x2a = button2X + ((buttonW - sw) / 2);
        drawBorderedString(g, str, x2a, buttonY + buttonH - s7, SystemPanel.textShadowC, c0);
    }
    private void drawShipIcon(Graphics g, int x, int y, int w, int h) {
        g.setColor(Color.black);
        g.fillRect(x, y, w, h);

        Image img = targetDesign.image();

        int w0 = img.getWidth(null);
        int h0 = img.getHeight(null);
        float scale = min((float)w/w0, (float)(h-s20)/h0);

        int w1 = (int)(scale*w0);
        int h1 = (int)(scale*h0);

        int x1 = x+((w-w1)/2);
        int y1 = y+((h-s20-h1)/2);
        g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, w0, h0, this);
    }
    private void cancelAction() {
        hoverTarget = null;
        disableGlassPane();
    }
    private void scrapAction() {
        player().shipLab().scrapDesign(targetDesign);
        
        // mark the player's empire economic stats to be
        // recalculated since ship maint costs may change
        player().recalcPlanetaryProduction();
        DesignUI.instance.init();
        hoverTarget = null;
        disableGlassPane();
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if ((k == KeyEvent.VK_ESCAPE)
        ||  (k == KeyEvent.VK_C)){
            cancelAction();
            return;
        }
        else if (k == KeyEvent.VK_S) {
            scrapAction();
            return;
        }
    }
    @Override
    public void mouseClicked(MouseEvent arg0) { }
    @Override
    public void mouseEntered(MouseEvent arg0) { }
    @Override
    public void mouseExited(MouseEvent arg0) { }
    @Override
    public void mousePressed(MouseEvent arg0) { }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        if (hoverTarget == cancelButtonArea) {
            softClick();
            cancelAction();
            return;
        }
        else if (hoverTarget == scrapButtonArea) {
            softClick();
            scrapAction();
            return;
        }
    }
    @Override
    public void mouseDragged(MouseEvent e) { }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Shape prevHover = hoverTarget;
        hoverTarget = null;
        if (cancelButtonArea.contains(x,y))
            hoverTarget = cancelButtonArea;
        else if (scrapButtonArea.contains(x,y))
            hoverTarget = scrapButtonArea;

        if (prevHover != hoverTarget)
            repaint();
    }
}
