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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.border.Border;
import rotp.util.ThickBevelBorder;

public abstract class ButtonPanel extends BasePanel implements MouseListener {
    private static final long serialVersionUID = 1L;
    // GRAY THEME
    public static Color grButtonLighter = new Color(192,192,192);
    public static Color grButtonLight = new Color(156,156,156);
    public static Color grButtonColor = new Color(123,123,123);
    public static Color grButtonDepressed = new Color(96,96,96);
    public static Color grButtonDark = new Color(83,83,83);
    public static Color grButtonDarker = new Color(63,63,63);
    private static Border grButtonBevelBorder, grButtonDepressedBorder;

    // BLUE THEME
    public static Color buttonLighter = new Color(163,162,204);
    public static Color buttonLight = new Color(143,142,184);
    public static Color buttonColor = new Color(100,98,145);
    public static Color buttonDepressed = new Color(96,96,96);
    public static Color buttonDark = new Color(81,79,126);
    public static Color buttonDarker = new Color(63,63,63);

    private static Border buttonBevelBorder, buttonDepressedBorder;

    public static Color grayColor()  { return grButtonColor; }
    public static Border grayBevelBorder(int w) {
        return new ThickBevelBorder(w, 1, grButtonLighter, grButtonLight, grButtonDarker, grButtonDark, grButtonDark, grButtonDarker, grButtonLight, grButtonLighter);
    }
    public static Border grayBevelBorder() {
        if (grButtonBevelBorder == null)
            grButtonBevelBorder =   new ThickBevelBorder(4, 1, grButtonLighter, grButtonLight, grButtonDarker, grButtonDark, grButtonDark, grButtonDarker, grButtonLight, grButtonLighter);
        return grButtonBevelBorder;
    }
    public static Border grayDepressedBorder() {
        if (grButtonDepressedBorder == null)
            grButtonDepressedBorder =   new ThickBevelBorder(4, 1, grButtonDarker, grButtonDark, grButtonLighter, grButtonLight, grButtonLight, grButtonLighter, grButtonDark, grButtonDarker);
        return grButtonDepressedBorder;
    }
    protected Shape boundingShape;
    protected boolean hovering = false;
    protected boolean depressed = false;
    @Override
    public Border buttonBevelBorder() {
        if (usingGrayTheme())
            return grayBevelBorder();
        else {
            if (buttonBevelBorder == null)
                buttonBevelBorder =  new ThickBevelBorder(4, 1, buttonLighter, buttonLight, buttonDarker, buttonDark, buttonDark, buttonDarker, buttonLight, buttonLighter);
            return buttonBevelBorder;
        }
    }
    public Border buttonDepressedBorder() {
        if (usingGrayTheme())
            return grayDepressedBorder();
        else {
            if (buttonDepressedBorder == null)
                buttonDepressedBorder = new ThickBevelBorder(4, 1, buttonDarker, buttonDark, buttonLighter, buttonLight, buttonLight, buttonLighter, buttonDark, buttonDarker);
            return buttonDepressedBorder;
        }
    }
    public boolean isDepressed() { return depressed; }
    public boolean isHovering()  { return hovering; }
    public void depressed(boolean b) {
        if (depressed != b) {
            depressed = b;
            repaint();
        }
    }
    abstract public boolean isButtonEnabled();
    abstract public String buttonLabel();
    abstract public Font textFont();
    abstract public void buttonClicked(int cnt);
    public boolean usingGrayTheme()   { return false; }

    public ButtonPanel(int w, int h) {
        this();
        setBackground(new Color(0,0,0,0));
        setPreferredSize(new Dimension(w,h));
    }
    public ButtonPanel() {
        setBackground(new Color(0,0,0,0));
        addMouseListener(this);
    }
    public Shape boundingShape() {
        if (boundingShape == null)
            boundingShape = new Rectangle(0,0,getWidth(),getHeight());
        return boundingShape;
    }
    public void paintBackground(Graphics2D g) {
        g.setColor(backgroundColor());
        g.fill(boundingShape());
    }
    public void paintBorder(Graphics2D g) {
        if (depressed)
            buttonDepressedBorder().paintBorder(this,g,0,0,getWidth(),getHeight());
        else
            buttonBevelBorder().paintBorder(this,g,0,0,getWidth(),getHeight());
    }
    @Override
    public void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        super.paintComponent(g);

        paintBackground(g);
        paintBorder(g);
        g.setFont(textFont());
        g.setColor(textColor());
        String text = buttonLabel();
        int sw = g.getFontMetrics().stringWidth(text);
        int sh = g.getFontMetrics().getHeight();
        int x0 = (getWidth() - sw) / 2;
        int y0 = getHeight() - bottomMargin(sh);
        drawString(g,text, x0, y0);
    }
    public int bottomMargin(int fontHeight) {
        return s4+(getHeight()-(fontHeight*9/10))/2;
    }
    public Color backgroundColor() {
        if (depressed)
            return usingGrayTheme() ? grButtonDepressed : buttonDepressed;
        else
            return usingGrayTheme() ? grButtonColor : buttonColor;
    }
    public Color textColor() {
        if (isButtonEnabled()) {
            if (isHovering())
                return hoveringTextColor();
            else
                return enabledTextColor();
        }
        else
            return disabledTextColor();
    }
    public Color hoveringTextColor()    { return Color.white; }
    public Color enabledTextColor()     { return Color.black; }
    public Color disabledTextColor()    { return Color.darkGray; }
    public void makeClickSound()   { buttonClick(); }
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {
        hovering = true;
        repaint();
    }
    @Override
    public void mouseExited(MouseEvent e) {
        hovering = false;
        depressed(false);
    }
    @Override
    public void mousePressed(MouseEvent e) {
        if (isButtonEnabled())
            depressed(true);
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        if (depressed && isButtonEnabled()) {
            depressed(false);
            buttonClicked(e.getClickCount());
        }
    }
}
