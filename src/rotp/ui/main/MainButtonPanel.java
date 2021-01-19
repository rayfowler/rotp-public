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

import java.awt.Color;
import java.awt.GradientPaint;
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
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.UserPreferences;

public final class MainButtonPanel extends BasePanel implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    private LinearGradientPaint buttonBackground;
    private GradientPaint nextTurnBackground;
    private GradientPaint nextTurnDisableBackground;
    private GradientPaint nextTurnHoverBackground;
    private GradientPaint nextTurnDepressedBackground;

    private final Color buttonEdgeC = new Color(34,53,102);
    private final Color buttonMidC = new Color(78,101,155);
    private final Color buttonTextC = new Color(167,169,172);
    private final Color textShadowC = new Color(50,50,50,50);
    private final Color nextTurnDisableLightC = new Color(128,128,128);
    private final Color nextTurnDisableDarkC = new Color(64,64,64);
    private final Color nextTurnLightC = new Color(143,174,76);
    private final Color nextTurnDarkC = new Color(26,56,0);
    private final Color nextTurnBorderC = new Color(231,231,231);
    private final Color nextTurnTextC = Color.white;

    private boolean allowNextTurn = true;
    int leftM;
    int midM = -1;
    int rightM = -1;
    int botM;
    int buttonW;
    String[] buttons = { "MAIN_NAVIGATION_GAME",
                    "MAIN_NAVIGATION_SYSTEMS",
                    "MAIN_NAVIGATION_FLEETS",
                    "MAIN_NAVIGATION_DESIGN",
                    "MAIN_NAVIGATION_RACES",
                    "MAIN_NAVIGATION_COLONIES",
                    "MAIN_NAVIGATION_TECH" };

    Rectangle[] buttonBox = new Rectangle[buttons.length];
    Rectangle nextTurnBox = new Rectangle();
    Shape hoverBox, depressedBox;

    private final MainUI parent;

    public MainButtonPanel(MainUI p) {
        parent = p;
        leftM = s1;
        botM = s2;

        for (int i=0;i<buttonBox.length;i++)
            buttonBox[i] = new Rectangle();

        addMouseListener(this);
        addMouseMotionListener(this);
    }
    public int buttonW()      { return buttonW; }
    private void initGradients(int w) {
        midM = w-scaled(273);
        rightM = w-s2;

        Point2D start = new Point2D.Float(leftM, 0);
        Point2D end = new Point2D.Float(midM, 0);
        float[] dist = {0.0f, 0.5f, 1.0f};
        Color[] colors = {buttonEdgeC, buttonMidC, buttonEdgeC };
        buttonBackground = new LinearGradientPaint(start, end, dist, colors);
        nextTurnBackground = new GradientPaint(midM,0,nextTurnLightC,rightM,0,nextTurnDarkC);
        nextTurnDisableBackground = new GradientPaint(midM,0,nextTurnDisableLightC,rightM,0,nextTurnDisableDarkC);
        nextTurnHoverBackground = new GradientPaint(midM,0,nextTurnLightC,rightM,0,hoverC);
        nextTurnDepressedBackground = new GradientPaint(midM,0,nextTurnLightC,rightM,0,depressedC);        
    }
    @Override
    public String textureName()            { return TEXTURE_GRAY; }
    public void init() {
        allowNextTurn = false;
        new Thread(slowEnableNextTurn()).start();
    }
    private Runnable slowEnableNextTurn() {
        return () -> {
                sleep(750);
                allowNextTurn = true;
                repaint();
        };
    }
    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        int w = getWidth();
        int h = getHeight();
        
        Graphics2D g = (Graphics2D) g0;
        
        if (buttonBackground == null) 
            initGradients(w);
        g.setColor(Color.black);
        g.fillRect(0, 0, w, h);

        buttonW = (midM-leftM)/buttons.length;

        for (int i=0;i<buttons.length;i++)
            drawButton(g, i, leftM+(i*buttonW), s2, buttonW-s2, h-s2-botM);

        drawNextTurn(g, midM, s2, rightM-midM, h-s3-botM);
    }
    private void drawButton(Graphics2D g, int i, int x, int y, int w, int h) {
        g.setPaint(buttonBackground);
        g.fillRect(x,y,w,h);

        Color c0 = buttonTextC;
        if (depressedBox == buttonBox[i])
            c0 = depressedC;
        else if (hoverBox == buttonBox[i])
            c0 = hoverC;

        buttonBox[i].setBounds(x, y, w, h);
        String label = text(buttons[i]);
        g.setFont(narrowFont(28));
        int sw = g.getFontMetrics().stringWidth(label);
        int x0 = x+((w-sw)/2);
        drawShadowedString(g, label, 3, x0, y+h-s18, textShadowC, c0);

        if ((hoverBox == buttonBox[i])
        || (depressedBox == buttonBox[i])) {
            g.setColor(c0);
            Stroke prevS = g.getStroke();
            g.setStroke(stroke2);
            g.drawRect(x,y,w,h);
            g.setStroke(prevS);
        }
    }
    private void drawNextTurn(Graphics2D g, int x, int y, int w, int h) {
        if (!allowNextTurn)
            g.setPaint(nextTurnDisableBackground);
        else if (depressedBox == nextTurnBox)
            g.setPaint(nextTurnDepressedBackground);
        else if (hoverBox == nextTurnBox)
            g.setPaint(nextTurnHoverBackground);
        else
            g.setPaint(nextTurnBackground);
        g.fillRoundRect(x,y,w,h,s10,s10);
        nextTurnBox.setBounds(x, y, w, h);

        Stroke prevS = g.getStroke();
        g.setStroke(stroke2);
        g.setColor(nextTurnBorderC);
        g.drawRoundRect(x,y,w,h,s10,s10);
        g.setStroke(prevS);

        String label = UserPreferences.displayYear() ? text("MAIN_NAVIGATION_NEXT_YEAR") : text("MAIN_NAVIGATION_NEXT_TURN");
        g.setFont(narrowFont(28));
        int sw = g.getFontMetrics().stringWidth(label);
        int x0 = x+((w-sw)/2);
        g.setColor(buttonTextC);
        drawShadowedString(g, label, 3, x0, y+h-s18, textShadowC, nextTurnTextC);
    }
    private void clickButton(int i) {
        RotPUI.instance().mainUI().cancel();
        switch(i) {
            case 0: RotPUI.instance().selectGamePanel();    break;
            case 1: RotPUI.instance().selectSystemsPanel(); break;
            case 2: RotPUI.instance().selectFleetPanel();   break;
            case 3: RotPUI.instance().selectDesignPanel();  break;
            case 4: RotPUI.instance().selectRacesPanel();   break;
            case 5: RotPUI.instance().selectPlanetsPanel(); break;
            case 6: RotPUI.instance().selectTechPanel();    break;
            default: break;
        }
    }
    @Override
    public void mouseClicked(MouseEvent arg0) { }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) {
        if ((hoverBox != null)
        || (depressedBox != null)) {
            depressedBox = null;
            hoverBox = null;
            repaint();
        }
    }
    @Override
    public void mousePressed(MouseEvent e) {
        if (hoverBox != null) {
            depressedBox = hoverBox;
            repaint();
        }
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        int x = e.getX();
        int y = e.getY();

        depressedBox = null;

        if (!parent.enableButtons())
            return;

        if (session().performingTurn()) {
            misClick();
            return;
        }

        int click = 0;
        if (allowNextTurn && nextTurnBox.contains(x,y)) {
            click = 1;
            parent.handleNextTurn();
            session().nextTurn();
        }
        for (int i=0;i<buttonBox.length;i++) {
            if (buttonBox[i].contains(x, y)) {
                clickButton(i);
                click = 2;
            }
        }
        if (click == 2)
            buttonClick();
        else if (click == 1)
            buttonClick();
    }
    @Override
    public void mouseDragged(MouseEvent arg0) { }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        if (!parent.enableButtons())
            return;
        Shape prevHover = hoverBox;
        hoverBox = null;

        if (nextTurnBox.contains(x,y))
            hoverBox = nextTurnBox;

        for (int i=0;i<buttonBox.length;i++) {
            if (buttonBox[i].contains(x, y))
                hoverBox = buttonBox[i];
        }

        if (prevHover != hoverBox)
            repaint();
    }
}
