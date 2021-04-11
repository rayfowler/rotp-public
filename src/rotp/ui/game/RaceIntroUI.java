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
package rotp.ui.game;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import rotp.model.empires.Empire;
import rotp.ui.BasePanel;
import rotp.ui.ButtonPanel;
import rotp.ui.RotPUI;
import rotp.ui.main.SystemPanel;

public class RaceIntroUI extends BasePanel implements MouseListener {
    private static final long serialVersionUID = 1L;
    BeginButton button;
    float opacity = 1.0f;
    float opacityDecr = 0.0f;
    BufferedImage introBack = null;
    BufferedImage fadeLab = null;
    LinearGradientPaint backGradient;
    Color textColor = Color.white;
    public RaceIntroUI() {
        initModel();
    }
    @Override
    public void animate() {
        if (opacity <= 0) {
            opacity = 1.0f;
            opacityDecr = 0;
            introBack = null;
            fadeLab = null;
            button.reset();
            RotPUI.instance().selectMainPanelNewGame();
        }
        galaxy().system(player().homeSysId()).planet().rotate(1);
        repaint();
    }
    @Override
    public boolean hasStarBackground()     { return true; }
    @Override
    public String ambienceSoundKey() { return player().race().diplomacyTheme; }
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        paintToBuffer(screenBuffer());

        opacity -= opacityDecr;
        // accelerate the fade as we get closer to transparency
        if (opacity <= 0.9)
            opacityDecr = 0.10f;
        if (opacity <= 0.4)
            opacityDecr = 0.2f;
        if (opacity < 0)
            opacity = 0;

        Composite prevComp = g2.getComposite();
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
        g2.setComposite(ac);
        g2.drawImage(screenBuffer(),0,0,null);
        g2.setComposite(prevComp);
    }
    public void paintToBuffer(Image img) {
        Graphics2D g = (Graphics2D) img.getGraphics();
        setFontHints(g);
        g.drawImage(introBack(), 0, 0, getWidth(), getHeight(), this);
        drawHomePlanet(g);
        jPanelPaintComponent(g);

        g.dispose();
    }
    public void drawHomeStar(Graphics2D g) {
        int graphicPaneW = scaled(247);
        int x0 = getWidth()-graphicPaneW+s13;
        int y0 = s29;
        drawStar(g, galaxy().system(player().homeSysId()).starType(), s80, x0+(graphicPaneW/3), y0+s30);
    }
    public void drawHomePlanet(Graphics2D g) {
        //w & h of graphic pane
        int w = scaled(247);
        int h = scaled(120);

        int x0 = getWidth()-w+s14;
        int y0 = s42+(h/4);
        int r = s38;
        galaxy().system(player().homeSysId()).planet().draw(g, w, h, x0, y0, r+r, 45);
    }
    public void drawSystemName(Graphics2D g) {
        Empire pl = player();
        String str = pl.sv.name(pl.homeSysId());
        g.setFont(narrowFont(36));

        int y0 = s46;
        int x0 = getWidth()-scaled(225);

        drawBorderedString(g, str, 1, x0, y0, Color.black, SystemPanel.orangeText);
    }
    private void drawIntroductionTitle(Graphics2D g) {
        Empire pl = player();
        String title = pl.race().introduction().get(0);
        title = title.replace("[race]", pl.raceName());
        g.setColor(textColor);
        g.setFont(font(24));
        int x0 = scaled(pl.race().introTextX);
        int y0 = scaled(230);
        drawString(g,title, x0, y0);
    }
    private void drawIntroductionText(Graphics2D g) {
        int w = getWidth();
        Empire pl = player();
        List<String> text = pl.race().introduction();
        g.setColor(textColor);
        g.setFont(font(14));
        int x0 = scaled(pl.race().introTextX);
        int y0 = scaled(250);
        int lineW = w-x0-s40;
        int lineH = s18;
        int paraSpacing = s10;
        for (int i=0;i<text.size();i++)  {
            if (i > 0) {
                String paragraph = text.get(i).replace("[race]", pl.raceName());
                List<String> lines = scaledWrappedLines(g, paragraph, lineW, 30, 16, 12);
                for (String line: lines) {
                    drawString(g,line, x0, y0);
                    y0 += lineH;
                }
                y0 += paraSpacing;
            }
        }
    }
    private BufferedImage introBack() {
        if (introBack == null)
            initIntroBack();
        return introBack;
    }
    private BufferedImage fadeLab(int w, int h) {
        if (fadeLab == null) {
            fadeLab = newBufferedImage(w, h);
            Graphics2D g = (Graphics2D) fadeLab.getGraphics();
            g.drawImage(player().race().laboratory(), 0, 0, w, h, this);
            g.setPaint(backGradient(w));
            g.setComposite(AlphaComposite.DstOut);
            g.fillRect(0,0,w,h);
            g.dispose();
        }
        return fadeLab;
    }
    private LinearGradientPaint backGradient(int w) {
        if (backGradient == null) {
            Point2D start = new Point2D.Float(0, 0);
            Point2D end = new Point2D.Float(w, 0);
            float[] dist = {0.0f, 0.4f, 0.75f, 1.0f};
            Color[] colors = new Color[] { newColor(0,0,0,0), newColor(0,0,0,0), newColor(0,0,0,255), newColor(0,0,0,255) };
            backGradient = new LinearGradientPaint(start, end, dist, colors);
        }
        return backGradient;
    }
    private void initIntroBack() {
        int w = getWidth();
        int h = getHeight();
        
        // non-stretched values for lab and diplomat image
        int h1 = min(h,w*5/8);
        int w1 = h*8/5;
        introBack = this.newBufferedImage(w, h);
        Graphics2D g = (Graphics2D) introBack.getGraphics();
        setFontHints(g);
        g.setColor(Color.black);
        g.fillRect(0,0, w, h);
        drawStars(g);
        g.drawImage(fadeLab(w1,h1), 0, 0, w1, h1, this);
        drawHomeStar(g);
        drawSystemName(g);
        drawIntroductionTitle(g);
        drawIntroductionText(g);

        Image raceImg = player().race().diplomatQuiet();
        g.drawImage(player().race().diplomatQuiet(), 0, h1/10, w1*9/10, h, 0, 0, raceImg.getWidth(null), raceImg.getHeight(null), this);
        g.dispose();
    }
    private void initModel() {
        setBackground(Color.black);
        button = new BeginButton();
        int buttonW = scaled(200);
        int buttonH = s45;
        button.setPreferredSize(new Dimension(buttonW,buttonH));

        BasePanel eastPanel = new BasePanel();
        eastPanel.setPreferredSize(new Dimension(scaled(250),getHeight()));
        eastPanel.setOpaque(false);
        eastPanel.setLayout(new BorderLayout());
        eastPanel.add(button, BorderLayout.SOUTH);
        eastPanel.setBorder(newEmptyBorder(0,20,10,10));

        setOpaque(false);
        setLayout(new BorderLayout());
        add(eastPanel, BorderLayout.EAST);

        // same outer border as MainUI
        Border line1 = newLineBorder(newColor(192,192,192),2);
        Border line2 = newLineBorder(newColor(160,160,160),2);
        Border line3 = newLineBorder(newColor(128,128,128),2);
        Border line4 = newLineBorder(newColor(96,96,96),2);
        Border line5 = newLineBorder(newColor(80,80,80),2);
        Border compound1 = BorderFactory.createCompoundBorder(line2, line1);
        Border compound2 = BorderFactory.createCompoundBorder(line3, compound1);
        Border compound3 = BorderFactory.createCompoundBorder(line4, compound2);
        Border compound4 = BorderFactory.createCompoundBorder(line5, compound3);
        setBorder(compound4);
        addMouseListener(this);
    }
    private void finish() {
        if (!playAnimations()) {
            opacity = 0.0f;
            animate();
        }
        opacityDecr = 0.05f;
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        switch(k) {
            case KeyEvent.VK_B:
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_ENTER:
                finish();
                return;
        }
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) { }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }
    class BeginButton extends ButtonPanel {
        private static final long serialVersionUID = 1L;
        private GradientPaint nextTurnBackground;
        private GradientPaint nextTurnHoverBackground;
        private GradientPaint nextTurnDepressedBackground;
        private final Color nextTurnLightC = new Color(143,174,76);
        private final Color nextTurnDarkC = new Color(26,56,0);
        private final Color nextTurnBorderC = new Color(231,231,231);
        private final Color nextTurnTextC = Color.white;
        private final Color backColor = newColor(159,235,180);

        public void reset() {
            nextTurnBackground = null;
            nextTurnHoverBackground = null;
            nextTurnDepressedBackground = null;
        }
        @Override
        public boolean isButtonEnabled() {  return true; }
        @Override
        public String buttonLabel() { return text("INTRO_BEGIN"); }
        @Override
        public Font textFont() { return font(24); }
        @Override
        public void buttonClicked(int cnt) {
            finish();
        }
        @Override
        public Shape boundingShape() {
            if (boundingShape == null)
                boundingShape = new RoundRectangle2D.Float(s3,s3,getWidth()-s6,getHeight()-s6,s5,s5);
            return boundingShape;
        }
        @Override
        public void paintBackground(Graphics2D g) {
            g.setPaint(backgroundGradient());
            g.fill(boundingShape());
        }
        @Override
        public void paintBorder(Graphics2D g) {
            Color c = nextTurnBorderC;
            if (opacity < 1) {
                int alpha = bounds(0,(int)(opacity*255),255);
                c = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            }

            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.setColor(c);
            g.draw(boundingShape());
            g.setStroke(prev);
        }
        @Override
        public Color enabledTextColor()     { return textColor; }
        @Override
        public Color backgroundColor() {
            Color c = isDepressed() ? backColor.darker() : backColor;
            if (opacity < 1) {
                int alpha = bounds(0,(int)(opacity*255),255);
                Color c0 = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                return c0;
            }       
            return c;
        }
        @Override
        public Color textColor() {
            Color c = nextTurnTextC;
            if (opacity < 1) {
                int alpha = bounds(0,(int)(opacity*255),255);
                Color c0 = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                return c0;
            }
            return c;
        }
        private Color backLightC() {
            Color c = nextTurnLightC;
            if (opacity < 1) {
                int alpha = bounds(0,(int)(opacity*255),255);
                Color c0 = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                return c0;
            }
            return c;
        }
        private Color backDarkC() {
            Color c = nextTurnDarkC;
            if (opacity >= 1)
                return c;

            int alpha = bounds(0,(int)(opacity*255),255);
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }
        private Color backHoverDarkC() {
            Color c = hoverC;
            if (opacity >= 1)
                return c;

            int alpha = bounds(0,(int)(opacity*255),255);
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }
        private Color backDepressedDarkC() {
            Color c = depressedC;
            if (opacity >= 1)
                return c;

            int alpha = bounds(0,(int)(opacity*255),255);
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }
        private GradientPaint backgroundGradient() {
            if ((nextTurnBackground == null) || (opacity < 1)) {
                int w = getWidth();
                nextTurnBackground = new GradientPaint(0,0,backLightC(),w,0,backDarkC());
                nextTurnHoverBackground = new GradientPaint(0,0,backLightC(),w,0,backHoverDarkC());
                nextTurnDepressedBackground = new GradientPaint(0,0,backLightC(),w,0,backDepressedDarkC());
            }
            if (depressed)
                return nextTurnDepressedBackground;
            else if (hovering)
                return nextTurnHoverBackground;
            else
                return nextTurnBackground;
        }
    }
}
