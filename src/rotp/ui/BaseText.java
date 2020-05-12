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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import rotp.util.Base;
import rotp.util.LanguageManager;

public class BaseText implements Base {
    static int ST_NORMAL = 0;
    static int ST_LOCKED = 1;
    static int ST_HOVER = 1;
    static int ST_PRESSED = 1;


    private final BasePanel panel;
    private final Color enabledC, disabledC, hoverC, depressedC, shadeC;
    private final int topLBdr, btmRBdr, bdrStep;
    private final Rectangle bounds = new Rectangle();
    private String text, hoverText;
    private int x,y;
    private boolean disabled;
    private boolean depressed = false;
    private boolean hovered = false;
    private BufferedImage textShadow;
    private int bufferedLanguage = -1;
    private boolean logoFont = false;
    private int fontSize = 10;
    private int xOrig;
    private int yOrig;
    BaseText preceder;

    public BaseText(BasePanel p, boolean logo, int fSize, int x1, int y1, Color c1, Color c2, Color c3, Color c4, Color c5, int i1, int i2, int i3) {
        panel = p;
        
        logoFont = logo;
        fontSize = fSize;
        xOrig = x1;
        yOrig = y1;
        x = scaled(x1);
        y = scaled(y1);
        enabledC = c1;
        disabledC = c2;
        hoverC = c3;
        depressedC = c4;
        shadeC = c5;
        bdrStep = i1;
        topLBdr = i2;
        btmRBdr = i3;
    }
    public void setScaledXY(int x1, int y1) {
        xOrig = unscaled(x1);
        yOrig = unscaled(y1);
        x = x1;
        y = y1;
    }
    private Font font() {
        return logoFont ? logoFont(fontSize) : narrowFont(fontSize);
    }
    private boolean centered() {  return (xOrig == 0) && (preceder == null); }
    @Override
    public String toString()  { return concat("Text:", text, "  at:", bounds.toString()); }
    public void displayText(String s) { text = s; }
    public void hoverText(String s)   { hoverText = s; }
    public int x()                    { return bounds.x; }
    public int y()                    { return bounds.y; }
    public int w()                    { return bounds.width; }
    public int h()                    { return bounds.height; }
    public int bottomY()              { return bounds.y + bounds.height; }
    public Rectangle bounds()         { return bounds; }
    public void disabled(boolean b)   { disabled = b; }
    public void preceder(BaseText t)  { preceder = t; }
    public void setBounds(int x, int y, int w, int h) {
        bounds.setBounds(x,y,w,h);
    }
    public void reset() { 
        bounds.setBounds(0,0,0,0); 
    }
    public void rescale() {
        x = scaled(xOrig);
        y = scaled(yOrig);
    }
    public boolean contains(int x, int y) {
        return bounds.contains(x, y);
    }
    public void repaint(String s) {
        Graphics g = panel.getGraphics();
        int oldW = stringWidth(g);
        displayText(s);
        int newW = stringWidth(g);
        g.dispose();
        bounds.width = max(oldW, newW)+scaled(5);
        repaint();
    }
    public void repaint(String s1, String s2) {
        Graphics g = panel.getGraphics();
        int oldW = stringWidth(g);
        displayText(s1);
        hoverText(s2);
        int newW = stringWidth(g);
        g.dispose();
        bounds.width = max(oldW, newW)+scaled(5);
        repaint();
    }
    public void repaint() {
        panel.repaint(bounds);
    }
    public void mousePressed() {
        depressed = true;
        if (centered())
            drawCentered();
        else
            draw();
        repaint();
    }
    public void mouseReleased() {
        depressed = false;
        if (centered())
            drawCentered();
        else
            draw();
        repaint();
    }
    public void mouseEnter() {
        hovered = true;
        if (centered())
            drawCentered();
        else
            draw();
        repaint();
    }
    public void mouseExit() {
        hovered = false;
        depressed = false;
        if (centered())
            drawCentered();
        else
            draw();
        repaint();
    }
    public int stringWidth(Graphics g) {
        int sw1 = g.getFontMetrics(font()).stringWidth(text);
        int sw2 = hoverText == null ? sw1 : g.getFontMetrics(font()).stringWidth(hoverText);
        return max(sw1,sw2);
    }
    private Color textColor() {
        if (disabled)
            return disabledC;
        else if (depressed)
            return depressedC;
        else if (hovered)
            return hoverC;
        else
            return enabledC;
    }
    private String displayText() {
        if (hovered && (hoverText != null))
            return hoverText;
        else
            return text;
    }
    public int draw() {
        return draw(panel.getGraphics());
    }
    public int drawCentered() {
        return drawCentered(panel.getGraphics());
    }
    public int draw(Graphics g) {
        int x1 = x >= 0 ? x : panel.getWidth()+x;
        int y1 = y >= 0 ? y : panel.getHeight()+y;
        
        if ((preceder != null) && (x>= 0)) {
            x1 = preceder.x() + preceder.w() + x;
        }
        g.setFont(font());
        g.setColor(textColor());
        int sw = stringWidth(g);
        int fontH = g.getFontMetrics().getHeight();
        setBounds(x1,y1-fontH,sw+scaled(5),fontH+(fontH/5));
        g.drawString(displayText(), x1, y1);
        return x1+sw;
    }
    public int drawCentered(Graphics g) {
        g.setFont(font());
        int w = panel.getWidth();
        int sw = stringWidth(g);
        int x1 = (w-sw)/2;
        int y1 = y > 0 ? y : panel.getHeight()+y;
        int fontH = g.getFontMetrics().getHeight();
        int sp = fontH/4;
        int hPad = sw/20;
        int vPad = fontH/5;
        setBounds(x1,y1+sp-fontH,sw+scaled(5),fontH-sp/2);
        
        int shadowImgW = sw+hPad+hPad;
        if ((textShadow == null)                                      // first time through?
        || (bufferedLanguage != LanguageManager.selectedLanguage())   // language changed?
        || (shadowImgW != textShadow.getWidth())) {                   // font resized?
            bufferedLanguage = LanguageManager.selectedLanguage();
            textShadow = newBufferedImage(shadowImgW, fontH+vPad);
            Graphics g0 = textShadow.getGraphics();
            g0.setFont(font());
            g0.setColor(shadeC);
            int topThick = scaled(topLBdr);
            int thick = btmRBdr;
            for (int x0=(0-topThick);x0<=thick;x0++) {
                int x0s = scaled(x0);
                for (int y0=(0-topThick);y0<=thick;y0++) {
                    int y0s = scaled(y0);
                    g0.drawString(text, hPad+x0s, fontH+y0s);
                }
            }
            g0.dispose();
        }
        Color c0 = textColor();
        g.setColor(c0);
        g.drawImage(textShadow, x1-hPad, y1-fontH, null);
        g.drawString(displayText(), x1, y1);
        return x1+sw;
    }
}
