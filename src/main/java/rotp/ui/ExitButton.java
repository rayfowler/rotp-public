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
package rotp.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import rotp.ui.main.SystemPanel;

public class ExitButton extends BasePanel implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    private LinearGradientPaint buttonBack;
    private final Color buttonEdgeC = new Color(41,25,12);
    private final Color buttonMidC = new Color(187,122,80);
    private final Color buttonBorderC = new Color(194,181,155);
    Rectangle hoverBox;
    Rectangle buttonBox = new Rectangle();
    int hPad = 0;
    int vPad = 0;
    Shape textureClip;

    public ExitButton(int w, int h, int vMargin, int hMargin) {
        hPad = hMargin;
        vPad = vMargin;
        init(w,h);
    }
    private void init(int w, int h) {
        setPreferredSize(new Dimension(w, h));
        setOpaque(false);
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    @Override
    public String textureName()            { return TEXTURE_BROWN; }
    @Override
    public Shape textureClip()             { return textureClip; }
    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;

        int w = getWidth();
        int h = getHeight();

        int hp = hPad();
        int vp = vPad();

        if (buttonBack == null) {
            Point2D start = new Point2D.Float(0, 0);
            Point2D end = new Point2D.Float(w, 0);
            float[] dist = {0.0f, 0.2f, 0.5f, 0.8f, 1.0f};
            Color[] colors = {buttonEdgeC, buttonEdgeC, buttonMidC, buttonEdgeC, buttonEdgeC };
            buttonBack = new LinearGradientPaint(start, end, dist, colors);
        }

        int arc = min(w, h) / 4;

        buttonBox.setBounds(hp, vp, w-hp-hp, h-vp-vp);
        g.setPaint(buttonBack);
        g.fillRoundRect(hp, vp, w-hp-hp, h-vp-vp, arc, arc);
        Stroke prev = g.getStroke();
        g.setStroke(borderStroke());
        Color c0 = hoverBox == buttonBox ? Color.yellow : buttonBorderC;
        g.setColor(c0);
        g.drawRoundRect(hp, vp, w-hp-hp, h-vp-vp,arc,arc);
        g.setStroke(prev);

        textureClip = new RoundRectangle2D.Float(hp, vp, w-hp-hp, h-vp-vp,arc,arc);

        g.setFont(narrowFont(unscaled((h-vp-vp)*2/3)));
        String s = label();
        int sw = g.getFontMetrics().stringWidth(s);
        int x0 = (w-sw)/2;
        int y0 = vp+((h-vp-vp)*3/4);
        Color c1 = hoverBox == buttonBox ? Color.yellow : SystemPanel.whiteText;
        drawShadowedString(g, s, 3, x0, y0, SystemPanel.textShadowC, c1);
    }
    protected Stroke borderStroke()  { return stroke2; }
    protected int hPad()             { return hPad; }
    protected int vPad()             { return vPad; }
    protected String label()         { return text("BUTTON_EXIT"); }
    protected void clickAction(int numClicks) {
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        int x = e.getX();
        int y = e.getY();
        if (buttonBox.contains(x,y)) {
            clickAction(e.getClickCount());
            return;
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
    public void mouseDragged(MouseEvent e) { }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Rectangle prevHover = hoverBox;
        hoverBox = null;
        if (buttonBox.contains(x,y))
            hoverBox = buttonBox;

        if (hoverBox != prevHover)
            repaint();
    }
}
