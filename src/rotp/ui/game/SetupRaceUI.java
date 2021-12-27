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
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JTextField;
import rotp.model.empires.Race;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;

public final class SetupRaceUI extends BasePanel implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    static final int MAX_RACES = 10;
    int MAX_COLORS = 10;
    int FIELD_W;
    int FIELD_H;
    BufferedImage backImg;
    public static BufferedImage raceBackImg;
    BufferedImage raceImg;
    Rectangle hoverBox;
    Rectangle cancelBox = new Rectangle();
    Rectangle nextBox = new Rectangle();
    Rectangle leaderBox = new Rectangle();
    Rectangle homeWorldBox = new Rectangle();
    Rectangle[] raceBox = new Rectangle[MAX_RACES];
    Rectangle[] colorBox = new Rectangle[MAX_COLORS];

    public static BufferedImage[] racemugs = new BufferedImage[MAX_RACES];
    JTextField leaderName = new JTextField("");
    JTextField homeWorld = new JTextField("");

    public SetupRaceUI() {
        init0();
    }
    private void init0() {
        addMouseListener(this);
        addMouseMotionListener(this);
        for (int i=0;i<raceBox.length;i++)
            raceBox[i] = new Rectangle();
        for (int i=0;i<colorBox.length;i++)
            colorBox[i] = new Rectangle();

        FIELD_W = scaled(160);
        FIELD_H = s24;
        initTextField(homeWorld);
        initTextField(leaderName);
    }
    public void init() {
        leaderName.setFont(narrowFont(20));
        homeWorld.setFont(narrowFont(20));

        createNewGameOptions();
        newGameOptions().copyOptions(options());
        raceChanged();
   }
    @Override
    public void paintComponent(Graphics g0) {
        int x = colorBox[0].x;
        int y = colorBox[0].y;
        leaderName.setCaretPosition(leaderName.getText().length());
        leaderName.setLocation(x, y-s58);
        leaderBox.setBounds(x-s1, y-s59, FIELD_W+s2, FIELD_H+s2);
        homeWorld.setCaretPosition(homeWorld.getText().length());
        homeWorld.setLocation(x, y-s100-s10);
        homeWorldBox.setBounds(x-s1, y-s100-s10, FIELD_W+s2, FIELD_H+s2);

        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        int w = getWidth();
        int h = getHeight();

        // background image
        g.drawImage(backImg(), 0, 0, w, h, this);

        // selected race center img
        g.drawImage(raceImg(), scaled(425), scaled(108), scaled(385), scaled(489), null);

        // selected race box
        List<String> races = newGameOptions().startingRaceOptions();
        String selRace = newGameOptions().selectedPlayerRace();
        for (int i=0;i<races.size();i++) {
            if (races.get(i).equals(selRace)) {
                Rectangle box = raceBox[i];
                drawRaceBox(g, i, box.x, box.y, null);
                Stroke prev = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(GameUI.setupFrame());
                g.draw(raceBox[i]);
                g.setStroke(prev);
                break;
            }
        }

        // hovering race b`ox outline
        for (int i=0;i<raceBox.length;i++) {
            if (raceBox[i] == hoverBox) {
                Stroke prev = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(Color.yellow);
                g.draw(raceBox[i]);
                g.setStroke(prev);
                break;
            }
        }

        int x0 = colorBox[0].x;

        // race icon
        Race race = Race.keyed(newGameOptions().selectedPlayerRace());
        int iconH = scaled(115);
        BufferedImage icon = newBufferedImage(race.flagNorm());
        int imgX = scaled(855);
        int imgY = scaled(120);
        //g.drawImage(icon, imgX, imgY, iconH, iconH, null);
        g.drawImage(icon, imgX, imgY, imgX+iconH, imgY+iconH, 0, 0, icon.getWidth(), icon.getHeight(), null);

        // draw race name
        int y0 = scaled(260);
        g.setFont(font(30));
        this.drawBorderedString(g0, race.setupName(), 1, x0, y0, Color.black, Color.white);

        // draw race desc #1
        int maxLineW = scaled(170);
        y0 += s25;
        g.setFont(narrowFont(16));
        g.setColor(Color.black);
        List<String> desc1Lines = this.wrappedLines(g, race.description1, maxLineW);
        g.fillOval(x0, y0-s8, s5, s5);
        for (String line: desc1Lines) {
            drawString(g,line, x0+s8, y0);
            y0 += s16;
        }

        // draw race desc #2
        y0 += s3;
        List<String> desc2Lines = wrappedLines(g, race.description2, maxLineW);
        g.fillOval(x0, y0-s8, s5, s5);
        for (String line: desc2Lines) {
            drawString(g,line, x0+s8, y0);
            y0 += s16;
        }

        // draw race desc #3
        y0 += s12;
        String desc3 = race.description3.replace("[race]", race.setupName());
        List<String> desc3Lines = scaledNarrowWrappedLines(g0, desc3, maxLineW+s8, 5, 16, 13);
        for (String line: desc3Lines) {
            drawString(g,line, x0, y0);
            y0 += s16;
        }

        // draw homeword label
        String homeLbl = text("SETUP_HOMEWORLD_NAME_LABEL");
        int x3 = colorBox[0].x;
        int y3 = colorBox[0].y-s100-s14;
        g.setFont(narrowFont(20));
        g.setColor(Color.black);
        drawString(g,homeLbl, x3, y3);

        if (hoverBox == homeWorldBox) {
            Stroke prev = g.getStroke();
            g.setStroke(stroke4);
            g.setColor(Color.yellow);
            g.draw(hoverBox);
            g.setStroke(prev);
        }

        // draw leader name label
        String nameLbl = text("SETUP_LEADER_NAME_LABEL");
        x3 = colorBox[0].x;
        y3 = colorBox[0].y-s64;
        g.setFont(narrowFont(20));
        g.setColor(Color.black);
        drawString(g,nameLbl, x3, y3);

        if (hoverBox == leaderBox) {
            Stroke prev = g.getStroke();
            g.setStroke(stroke4);
            g.setColor(Color.yellow);
            g.draw(hoverBox);
            g.setStroke(prev);
        }

        // draw empire color label
        String colorLbl = text("SETUP_RACE_COLOR");
        x3 = colorBox[0].x;
        y3 = colorBox[0].y-s8;
        g.setFont(narrowFont(20));
        g.setColor(Color.black);
        drawString(g,colorLbl, x3, y3);

        // draw selected & hovering colors
        for (int i=0;i<colorBox.length;i++) {
            int xC = colorBox[i].x;
            int yC = colorBox[i].y;
            int wC = colorBox[i].width;
            int hC = colorBox[i].height;
            Color c = newGameOptions().color(i);
            if (hoverBox == colorBox[i]) {
                Stroke prev = g.getStroke();
                g.setStroke(BasePanel.stroke2);
                g.setColor(Color.yellow);
                g.drawRect(xC, yC, wC, hC);
                g.setStroke(prev);
            }
            if (newGameOptions().selectedPlayerColor() == i) {
                g.setColor(c);
                g.fillRect(xC, yC, wC, hC);
                Stroke prev = g.getStroke();
                g.setStroke(BasePanel.stroke2);
                g.setColor(GameUI.setupFrame());
                g.drawRect(xC, yC, wC, hC);
                g.setStroke(prev);
            }
        }

        // left button
        int cnr = s5;
        g.setFont(narrowFont(30));
        String text1 = text("SETUP_BUTTON_CANCEL");
        int sw1 = g.getFontMetrics().stringWidth(text1);
        int x1 = cancelBox.x+((cancelBox.width-sw1)/2);
        int y1 = cancelBox.y+cancelBox.height-s12;
        Color c1 = hoverBox == cancelBox ? Color.yellow : GameUI.borderBrightColor();
        drawShadowedString(g, text1, 2, x1, y1, GameUI.borderDarkColor(), c1);
        Stroke prev = g.getStroke();
        g.setStroke(stroke1);
        g.drawRoundRect(cancelBox.x, cancelBox.y, cancelBox.width, cancelBox.height, cnr, cnr);
        g.setStroke(prev);

        // right button
        String text2 = text("SETUP_BUTTON_NEXT");
        int sw2= g.getFontMetrics().stringWidth(text2);
        int x2 = nextBox.x+((nextBox.width-sw2)/2);
        int y2 = nextBox.y+nextBox.height-s12;
        Color c2 = hoverBox == nextBox ? Color.yellow : GameUI.borderBrightColor();
        drawShadowedString(g, text2, 2, x2, y2, GameUI.borderDarkColor(), c2);
        prev = g.getStroke();
        g.setStroke(stroke1);
        g.drawRoundRect(nextBox.x, nextBox.y, nextBox.width, nextBox.height, cnr, cnr);
        g.setStroke(prev);
    }
    public void goToMainMenu() {
        buttonClick();
        RotPUI.instance().selectGamePanel();
        backImg = null;
        raceImg = null;
    }
    public void goToGalaxySetup() {
        buttonClick();
        RotPUI.instance().selectSetupGalaxyPanel();
        backImg = null;
        raceImg = null;
    }
    public void selectRace(int i) {
        String selRace = newGameOptions().selectedPlayerRace();
        List<String> races = newGameOptions().startingRaceOptions();
        if (i <= races.size()) {
            if (!selRace.equals(races.get(i))) {
                newGameOptions().selectedPlayerRace(races.get(i));
                raceChanged();
                repaint();
            }
        }
    }
    public void raceChanged() {
        Race r =  Race.keyed(newGameOptions().selectedPlayerRace());
        r.resetMugshot();
        r.resetSetupImage();
        leaderName.setText(r.randomLeaderName());
        newGameOptions().selectedLeaderName(leaderName.getText());
        homeWorld.setText(r.defaultHomeworldName());
        newGameOptions().selectedHomeWorldName(homeWorld.getText());
        raceImg = null;
    }
    public void selectColor(int i) {
        int selColor = newGameOptions().selectedPlayerColor();
        if (selColor != i) {
            newGameOptions().selectedPlayerColor(i);
            repaint();
        }
    }
    private void initTextField(JTextField value) {
        value.setBackground(GameUI.setupFrame());
        value.setBorder(newEmptyBorder(5,5,0,0));
        value.setPreferredSize(new Dimension(FIELD_W, FIELD_H));
        value.setFont(narrowFont(20));
        value.setForeground(Color.black);
        value.setCaretColor(Color.black);
        value.putClientProperty("caretWidth", s3);
        value.setVisible(true);
        value.addMouseListener(this);
        add(value);
    }
    private BufferedImage raceImg() {
        if (raceImg == null) {
            String selRace = newGameOptions().selectedPlayerRace();
            raceImg = newBufferedImage(Race.keyed(selRace).setupImage());
        }
        return raceImg;
    }
    public static BufferedImage raceBackImg() {
        if (raceBackImg == null)
            initRaceBackImg();
        return raceBackImg;
    }
    private BufferedImage backImg() {
        if (backImg == null)
            initBackImg();
        return backImg;
    }
    private static void initRaceBackImg() {
        int w = s76;
        int h = s82;
        raceBackImg = gc().createCompatibleImage(w, h);

        Point2D center = new Point2D.Float(w/2, h/2);
        float radius = s78;
        float[] dist = {0.0f, 0.1f, 0.5f, 1.0f};
        Color[] colors = {GameUI.raceCenterColor(), GameUI.raceCenterColor(), GameUI.raceEdgeColor(), GameUI.raceEdgeColor()};
        RadialGradientPaint p = new RadialGradientPaint(center, radius, dist, colors);
        Graphics2D g = (Graphics2D) raceBackImg.getGraphics();
        g.setPaint(p);
        g.fillRect(0, 0, w, h);
        g.dispose();
    }
    private void initBackImg() {
        int w = getWidth();
        int h = getHeight();
        backImg = newOpaqueImage(w, h);
        Graphics2D g = (Graphics2D) backImg.getGraphics();
        setFontHints(g);

        // background image
        Image back = GameUI.defaultBackground;
        int imgW = back.getWidth(null);
        int imgH = back.getHeight(null);
        g.drawImage(back, 0, 0, w, h, 0, 0, imgW, imgH, this);

        // draw title
        String title = text("SETUP_SELECT_RACE");
        g.setFont(narrowFont(50));
        int sw = g.getFontMetrics().stringWidth(title);
        int x0 = (w - sw) / 2;
        int y0 = s80;
        drawBorderedString(g, title, 2, x0, y0, Color.darkGray, Color.white);

        // draw shading
        g.setColor(GameUI.setupShade());
        g.fillRect(scaled(205), s95, scaled(825), scaled(515));

        // draw race frame
        g.setColor(GameUI.setupFrame());
        g.fillRect(scaled(420), scaled(103), scaled(395), scaled(499));

        // draw race left gradient
        g.setPaint(GameUI.raceLeftBackground());
        g.fillRect(scaled(220), scaled(115), scaled(200), scaled(475));

        // draw race right gradient
        g.setPaint(GameUI.raceRightBackground());
        g.fillRect(scaled(815), scaled(115), scaled(200), scaled(475));

        int cnr = s5;
        int buttonH = s45;
        int buttonW = scaled(220);

        int xL = scaled(234);
        int xR = scaled(325);

        Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER , 0.3f);
        drawRaceBox(g, 0, xL, scaled(125), comp);
        drawRaceBox(g, 1, xR, scaled(125), comp);
        drawRaceBox(g, 2, xL, scaled(218), comp);
        drawRaceBox(g, 3, xR, scaled(218), comp);
        drawRaceBox(g, 4, xL, scaled(311), comp);
        drawRaceBox(g, 5, xR, scaled(311), comp);
        drawRaceBox(g, 6, xL, scaled(403), comp);
        drawRaceBox(g, 7, xR, scaled(403), comp);
        drawRaceBox(g, 8, xL, scaled(496), comp);
        drawRaceBox(g, 9, xR, scaled(496), comp);

        // draw color buttons on right panel
        int xC = scaled(830);
        int yC = scaled(550);
        int wC = s25;
        int hC = s15;
        for (int i=0;i<MAX_COLORS;i++) {
            int yC1 = i%2 == 0 ? yC : yC+hC+s5;
            Color c = newGameOptions().color(i);
            Color c0 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 128);
            g.setColor(c0);
            g.fillRect(xC, yC1, wC, hC);
            colorBox[i].setBounds(xC, yC1, wC, hC);
            if (i%2 == 1)
                xC += (wC+s10);
        }

        // draw left button
        cancelBox.setBounds(scaled(710), scaled(685), buttonW, buttonH);
        g.setPaint(GameUI.buttonLeftBackground());
        g.fillRoundRect(cancelBox.x, cancelBox.y, buttonW, buttonH, cnr, cnr);

        // draw right button
        nextBox.setBounds(scaled(950), scaled(685), buttonW, buttonH);
        g.setPaint(GameUI.buttonRightBackground());
        g.fillRoundRect(nextBox.x, nextBox.y, buttonW, buttonH, cnr, cnr);

        g.dispose();
    }
    private void drawRaceBox(Graphics2D g, int num, int x, int y, Composite comp) {
        raceBox[num].setBounds(0,0,0,0);
        BufferedImage back = raceBackImg();
        g.drawImage(back, x, y, null);

        List<String> races = newGameOptions().startingRaceOptions();
        if (num >= races.size())
            return;

        int w = back.getWidth();
        int h = back.getHeight();

        raceBox[num].setBounds(x,y,w,h);
        Race r = Race.keyed(races.get(num));
        if (racemugs[num] == null)
            racemugs[num] = newBufferedImage(r.diploMugshotQuiet());

        BufferedImage mug = racemugs[num];

        Composite prevC = g.getComposite();
        if (comp != null)
            g.setComposite(comp);
        g.drawImage(mug, x,y,w,h, null);
        g.setComposite(prevC);
    }
    @Override
    public String ambienceSoundKey() { 
        return GameUI.AMBIENCE_KEY;
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        switch(k) {
            case KeyEvent.VK_ESCAPE:
                goToMainMenu();
                return;
            case KeyEvent.VK_ENTER:
                goToGalaxySetup();
                return;
        }
    }
    @Override
    public void mouseDragged(MouseEvent e) {  }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Rectangle prevHover = hoverBox;
        hoverBox = null;
        search:
        if (nextBox.contains(x,y))
            hoverBox = nextBox;
        else if (cancelBox.contains(x,y))
            hoverBox = cancelBox;
        else if (leaderBox.contains(x,y))
            hoverBox = leaderBox;
        else if (homeWorldBox.contains(x,y))
            hoverBox = homeWorldBox;
        else {
            for (int i=0;i<raceBox.length;i++) {
                if (raceBox[i].contains(x,y)) {
                    hoverBox = raceBox[i];
                    break search;
                }
            }
            for (int i=0;i<colorBox.length;i++) {
                if (colorBox[i].contains(x,y)) {
                    hoverBox = colorBox[i];
                    break search;
                }
            }
        }
        if (hoverBox != prevHover)
            repaint();
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        if (hoverBox == null)
            return;
        int x = e.getX();
        int y = e.getY();
        search:
        if (hoverBox == cancelBox)
            goToMainMenu();
        else if (hoverBox == nextBox)
            goToGalaxySetup();
        else {
            for (int i=0;i<raceBox.length;i++) {
                if (hoverBox == raceBox[i]) {
                    selectRace(i);
                    break search;
                }
            }
            for (int i=0;i<colorBox.length;i++) {
                if (hoverBox == colorBox[i]) {
                    selectColor(i);
                    break search;
                }
            }
        }
    }
    @Override
    public void mouseEntered(MouseEvent e) {
        if (e.getComponent() == leaderName) {
            leaderName.requestFocus();
            hoverBox = leaderBox;
            repaint();
        }
        else if (e.getComponent() == homeWorld) {
            homeWorld.requestFocus();
            hoverBox = homeWorldBox;
            repaint();
        }
    }
    @Override
    public void mouseExited(MouseEvent e) {
        if (e.getComponent() == leaderName) {
            newGameOptions().selectedLeaderName(leaderName.getText());
            RotPUI.instance().requestFocus();
        }
        else if (e.getComponent() == homeWorld) {
            newGameOptions().selectedHomeWorldName(homeWorld.getText());
            RotPUI.instance().requestFocus();
        }
        if (hoverBox != null) {
            hoverBox = null;
            repaint();
        }
    }
}
