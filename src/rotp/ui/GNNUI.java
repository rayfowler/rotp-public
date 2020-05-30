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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.border.Border;
import rotp.model.empires.Empire;
import rotp.ui.main.SystemPanel;
import rotp.util.Base;
import rotp.util.ThickBevelBorder;

public class GNNUI extends FadeInPanel implements Base, MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    static final Color innerTextBackC = new Color(73,163,163);
    static final Color outerTextAreaC = new Color(92,208,208);
    static final Color textBorderLo1 = new Color(73,163,163);
    static final Color textBorderLo2 = new Color(52,126,126);
    static final Color textBorderHi1  = new Color(110,240,240);
    static final Color textBorderHi2  = new Color(115,252,252);
    static final Color textC = new Color(114,155,201);

    static Border outerTextAreaBorder, innerTextAreaBorder;

    private final static int EVENT_FADE_IN_FRAME = 30;
    private final static int NUM_EVENT_FADE_FRAMES = 5;
    private boolean exited = false;
    private String messageText;
    private String eventId;
    private Image eventImg;
    private int frameCtr = 0;
    private List<Empire> empires;

    public GNNUI() {
        outerTextAreaBorder = new ThickBevelBorder(8, textBorderHi2, textBorderHi1, textBorderHi2, textBorderHi1, textBorderLo2, textBorderLo1, textBorderLo2, textBorderLo1);
        innerTextAreaBorder = new ThickBevelBorder(8, textBorderLo1, textBorderLo2, textBorderLo1, textBorderLo2, textBorderHi1, textBorderHi2, textBorderHi1, textBorderHi2);
        init();
    }
    private void init() {
        addMouseListener(this);
        addMouseMotionListener(this);
        setBackground(Color.red);
    }
    public void init(String title, String id, List<Empire> empireList) {
        clearBuffer();
        messageText = title;
        eventId = id;
        empires = empireList;
        exited = false;
        frameCtr = 0;
        eventImg = player().race().gnnEvent(eventId);
        player().race().resetGNN(id);
        startFadeTimer();
        if (!playAnimations())
            frameCtr = EVENT_FADE_IN_FRAME + NUM_EVENT_FADE_FRAMES + 1;
    }
    @Override
    public String ambienceSoundKey() { return "NewsAmbience"; }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Image img0 = paintToImage();
        g.drawImage(img0,0,0,null);
        drawOverlay(g);
    }
    public Image paintToImage() {
        int w = getWidth();
        int h = getHeight();

        BufferedImage backImg = player().race().gnn();
        BufferedImage hostImg = player().race().gnnHost();

        float resizePct = (float) w / backImg.getWidth();

        int resizedW = w;
        int resizedH = (int) (resizePct * backImg.getHeight());

        //paint background
        Image screenImg = this.screenBuffer();
        Graphics2D g = (Graphics2D) screenImg.getGraphics();
        super.paintComponent(g);
        // resize background image to screen size
        g.drawImage(backImg, 0, 0, resizedW, resizedH, 0, 0, backImg.getWidth(), backImg.getHeight(), null);
        // fade in event image
        if (frameCtr > EVENT_FADE_IN_FRAME) {
            Composite prevComposite = g.getComposite();
            if (frameCtr > (EVENT_FADE_IN_FRAME + NUM_EVENT_FADE_FRAMES)) 
                g.drawImage(eventImg, 0, 0, resizedW, resizedH, 0, 0, eventImg.getWidth(null), eventImg.getHeight(null), null);
            else {
                float fluxPct = 0.3f + (float) (frameCtr - 30) / 10;
                if (fluxPct > 1.0f)
                    fluxPct = 1.0f;
                AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fluxPct);
                g.setComposite(ac);
                g.drawImage(eventImg, 0, 0, resizedW, resizedH, 0, 0, eventImg.getWidth(null), eventImg.getHeight(null), null);
            }
            drawText(g, w, h);
            g.setComposite(prevComposite);
        }
        g.drawImage(hostImg, 0, 0, resizedW, resizedH, 0, 0, hostImg.getWidth(), hostImg.getHeight(), null);
        drawTitle(g, w, h);
        g.dispose();
        return screenImg;
    }
    private void drawTitle(Graphics g, int w, int h) {
        g.setFont(narrowFont(36));
        String title = text("GNN_TITLE", player().raceName());
        int titleW = g.getFontMetrics().stringWidth(title);
        drawBorderedString(g, title, 2, (w-titleW)/2, s36, Color.black, SystemPanel.orangeText);
    }
    private void drawText(Graphics g, int w, int h) {
        Color c0 = player().race().gnnTextColor;
        g.setColor(c0);
        g.setFont(dlgFont(28));
        int lineH = s28;
        int y1 = h*27/100;
        int x1 = w*65/100;
        List<String> remarkLines = wrappedLines(g, messageText, w-x1-s10);
        for (String line: remarkLines) {
            y1 += lineH;
            drawBorderedString(g, line, 2, x1, y1, Color.black, c0);
        }
        if (empires == null)
            return;

        y1 += s20;
        x1 += s15;
        int rows = min(5, empires.size());

        for (int i=0;i<rows;i++) {
            Empire e = empires.get(i);
            y1 += lineH;
            String line = text("GNN_EMPIRE_RANKING", str(i+1), e.raceName());
            drawShadowedString(g, line, 1, x1, y1, Color.black, c0);
        }
    }
    private void advance() {
        if (frameCtr < (EVENT_FADE_IN_FRAME + NUM_EVENT_FADE_FRAMES)) {
            frameCtr = EVENT_FADE_IN_FRAME + NUM_EVENT_FADE_FRAMES;
            repaint();
            return;
        }
        softClick();
        exited = true;
        repaint();
        session().resumeNextTurnProcessing();
    }
    @Override
    public void mouseDragged(MouseEvent e) { }
    @Override
    public void mouseMoved(MouseEvent e) { }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        advance();
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();

        switch(k) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_SPACE:
                advance();
                return;
        }
    }
    @Override
    public void animate() {
        frameCtr++;
        if (!playAnimations())
            return;
        advanceFade();
        repaint();
    }
}
