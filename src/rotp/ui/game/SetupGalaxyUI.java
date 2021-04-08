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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
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
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.SwingUtilities;
import rotp.model.empires.Race;
import rotp.model.galaxy.GalaxyShape;
import rotp.model.game.GameSession;
import rotp.ui.BasePanel;
import rotp.ui.NoticeMessage;
import rotp.ui.RotPUI;
import rotp.ui.UserPreferences;
import rotp.ui.main.SystemPanel;

public final class SetupGalaxyUI  extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    public static int MAX_DISPLAY_OPPS = 49;
    BufferedImage backImg, playerRaceImg;
    BufferedImage smBackImg;
    Rectangle backBox = new Rectangle();
    Rectangle startBox = new Rectangle();
    Rectangle settingsBox = new Rectangle();
    Rectangle shapeBox = new Rectangle();
    Polygon shapeBoxL = new Polygon();
    Polygon shapeBoxR = new Polygon();
    Rectangle mapOption1Box = new Rectangle();
    Polygon mapOption1BoxL = new Polygon();
    Polygon mapOption1BoxR = new Polygon();			 
    Rectangle mapOption2Box = new Rectangle();
    Polygon mapOption2BoxL = new Polygon();
    Polygon mapOption2BoxR = new Polygon();			 
    Rectangle sizeBox = new Rectangle();
    Polygon sizeBoxL = new Polygon();
    Polygon sizeBoxR = new Polygon();
    Rectangle diffBox = new Rectangle();
    Polygon  diffBoxL = new Polygon();
    Polygon diffBoxR = new Polygon();
    Rectangle oppBox = new Rectangle();
    Polygon  oppBoxU = new Polygon();
    Polygon oppBoxD = new Polygon();
    Rectangle aiBox = new Rectangle();
    Polygon  aiBoxL = new Polygon();
    Polygon aiBoxR = new Polygon();

    Rectangle[] oppSet = new Rectangle[MAX_DISPLAY_OPPS];
    Rectangle[] oppAI = new Rectangle[MAX_DISPLAY_OPPS];

    Shape hoverBox;
    boolean starting = false;
    int leftBoxX, rightBoxX, boxW, boxY, leftBoxH, rightBoxH;
    int galaxyX, galaxyY, galaxyW, galaxyH;

    public SetupGalaxyUI() {
        init0();
    }
    private void init0() {
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        for (int i=0;i<oppSet.length;i++)
            oppSet[i] = new Rectangle();
        for (int i=0;i<oppAI.length;i++)
            oppAI[i] = new Rectangle();
    }
    public void init() {
    }
    private void release() {
        backImg = null;
        playerRaceImg = null;
    }
    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        int w = getWidth();
        int h = getHeight();

        for (Rectangle rect: oppSet)
            rect.setBounds(0,0,0,0);
        for (Rectangle rect: oppAI)
            rect.setBounds(0,0,0,0);
        // background image
        g.drawImage(backImg(), 0, 0, w, h, this);

        // draw number of opponents
        int maxOpp = newGameOptions().maximumOpponentsOptions();
        int numOpp = newGameOptions().selectedNumberOpponents();
        
        boolean smallImages = maxOpp > 25;
        BufferedImage mugBack = smallImages ? smallRaceBackImg() : SetupRaceUI.raceBackImg();
        int mugW = mugBack.getWidth();
        int mugH = mugBack.getHeight();

        g.setFont(narrowFont(30));
        g.setColor(Color.black);
        String oppStr =str(numOpp);
        int numSW = g.getFontMetrics().stringWidth(oppStr);
        int x0 = oppBox.x + ((oppBox.width-numSW)/2);
        int y0 = oppBox.y + oppBox.height -s5;
        drawString(g,oppStr, x0, y0);

        String randomOppLbl = text("SETUP_OPPONENT_RANDOM");
        int randSW = g.getFontMetrics().stringWidth(randomOppLbl);
        int numRows = smallImages ? 7 : 5;
        int numCols = smallImages ? 7 : 5;
        int spaceW = mugW+(((boxW-s60)-(numCols*mugW))/(numCols-1));
        int spaceH = smallImages ? mugH+s10 : mugH+s15;
        // draw opponent boxes
        Composite raceComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER , 0.5f);
        Composite prevComp = g.getComposite();
        Stroke prevStroke = g.getStroke();
        Color borderC = GameUI.setupFrame();
        boolean selectableAI = newGameOptions().selectableAI();
        int maxDraw = min((numRows*numCols), numOpp, MAX_DISPLAY_OPPS);
        for (int i=0;i<maxDraw;i++) {
            int row = i/numCols;
            int col = i%numCols;
            int y2 = y0+s50+(row*spaceH);
            int x2 = leftBoxX+s30+(col*spaceW);
            oppSet[i].setBounds(x2,y2,mugW,mugH);
            oppAI[i].setBounds(x2,y2+mugH-s20,mugW,s20);
            g.drawImage(mugBack, x2, y2, this);
            String selOpp = newGameOptions().selectedOpponentRace(i);
            if (selOpp == null) {
                int x2b = x2+((mugW-randSW)/2);
                int y2b = smallImages ? y2+mugH-s20 : y2+mugH-s31;
                g.setColor(Color.black);
                g.setFont(narrowFont(30));
                drawString(g,randomOppLbl, x2b, y2b);
            }
            else {
                Race r = Race.keyed(selOpp);
                g.setComposite(raceComp);
                g.drawImage(r.diploMug(), x2, y2, mugW, mugH, this);
                g.setComposite(prevComp);
            }
            if (selectableAI) {
                g.setColor(SystemPanel.whiteText);
                String aiText = text(newGameOptions().specificOpponentAIOption(i+1));
                g.setFont(narrowFont(15));
                int aiSW = g.getFontMetrics().stringWidth(aiText);
                int x2b = x2+(mugW-aiSW)/2;
                drawString(g,aiText, x2b, y2+mugH-s5);
            }
            g.setStroke(stroke1);
            g.setColor(borderC);
            g.drawRect(x2, y2, mugW, mugH);
            g.setStroke(prevStroke);

        }

        // draw galaxy
        drawGalaxyShape(g, newGameOptions().galaxyShape(), galaxyX, galaxyY, galaxyW, galaxyH);

        // draw info under galaxy map
        g.setColor(Color.black);
        g.setFont(narrowFont(16));
        int galaxyBoxW = boxW-s40;
        int y3 = galaxyY+galaxyH+s16;
        String systemsLbl = text("SETUP_GALAXY_NUMBER_SYSTEMS", newGameOptions().numberStarSystems());
        int sw3 = g.getFontMetrics().stringWidth(systemsLbl);
        int x3 = rightBoxX+s20+((galaxyBoxW/2)-sw3)/2;
        drawString(g,systemsLbl, x3,y3);

        String maxOppsLbl = text("SETUP_GALAXY_MAX_OPPONENTS", newGameOptions().maximumOpponentsOptions());
        int sw4 = g.getFontMetrics().stringWidth(maxOppsLbl);
        int x4 = rightBoxX+s20+(galaxyBoxW/2)+((galaxyBoxW/2)-sw4)/2;
        drawString(g,maxOppsLbl, x4,y3);

        // highlight any controls that are hovered
        if ((hoverBox == shapeBoxL) || (hoverBox == shapeBoxR)
            ||  (hoverBox == sizeBoxL)  || (hoverBox == sizeBoxR)
            ||  (hoverBox == diffBoxL)  || (hoverBox == diffBoxR)
            ||  (hoverBox == aiBoxL)  || (hoverBox == aiBoxR)
            ||  (hoverBox == mapOption1BoxL)  || (hoverBox == mapOption1BoxR)
            ||  (hoverBox == mapOption2BoxL)  || (hoverBox == mapOption2BoxR)
            ||  (hoverBox == oppBoxU)   || (hoverBox == oppBoxD)) {
            g.setColor(Color.yellow);
            g.fill(hoverBox);
        }
        else if ((hoverBox == shapeBox) || (hoverBox == sizeBox)
            || (hoverBox == mapOption1Box) || (hoverBox == mapOption2Box) 
            || (hoverBox == aiBox)
            || (hoverBox == diffBox)   || (hoverBox == oppBox)) {
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.setColor(Color.yellow);
            g.draw(hoverBox);
            g.setStroke(prev);
        }
        else {
            if (newGameOptions().selectableAI()) {
                for (int i=0;i<oppAI.length;i++) {
                    if (hoverBox == oppAI[i]) {
                        Stroke prev = g.getStroke();
                        g.setStroke(stroke2);
                        g.setColor(Color.yellow);
                        g.draw(hoverBox);
                        g.setStroke(prev);
                        break;
                    }
                }
            }
            for (int i=0;i<oppSet.length;i++) {
                if (hoverBox == oppSet[i]) {
                    Stroke prev = g.getStroke();
                    g.setStroke(stroke2);
                    g.setColor(Color.yellow);
                    g.draw(hoverBox);
                    g.setStroke(prev);
                    break;
                }
            }
        }
        
        // draw Opponent AI text
        g.setColor(Color.black);
        g.setFont(narrowFont(17));
        String aiLbl = text(newGameOptions().selectedOpponentAIOption());
        int aiSW = g.getFontMetrics().stringWidth(aiLbl);
        int x4b =aiBox.x+((aiBox.width-aiSW)/2);
        int y4b = aiBox.y+aiBox.height-s4;
        drawString(g,aiLbl, x4b, y4b);

        // draw galaxy options text
        int y5 = shapeBox.y+shapeBox.height-s4;
        String shapeLbl = text(newGameOptions().selectedGalaxyShape());
        int shapeSW = g.getFontMetrics().stringWidth(shapeLbl);
        int x5a =shapeBox.x+((shapeBox.width-shapeSW)/2);
        drawString(g,shapeLbl, x5a, y5);
		
        if (newGameOptions().numGalaxyShapeOption1() > 0) {
            String label1 = text(newGameOptions().selectedGalaxyShapeOption1());
            int sw1 = g.getFontMetrics().stringWidth(label1);
            int x5d =mapOption1Box.x+((mapOption1Box.width-sw1)/2);
            drawString(g,label1, x5d, y5+s20);
            if (newGameOptions().numGalaxyShapeOption2() > 0) {
                String label2 = text(newGameOptions().selectedGalaxyShapeOption2());
                int sw2 = g.getFontMetrics().stringWidth(label2);
                int x5e =mapOption2Box.x+((mapOption2Box.width-sw2)/2);
                drawString(g,label2, x5e, y5+s40);           
            }         
        }

        String sizeLbl = text(newGameOptions().selectedGalaxySize());
        int sizeSW = g.getFontMetrics().stringWidth(sizeLbl);
        int x5b =sizeBox.x+((sizeBox.width-sizeSW)/2);
        drawString(g,sizeLbl, x5b, y5);

        String diffLbl = text(newGameOptions().selectedGameDifficulty());
        int diffSW = g.getFontMetrics().stringWidth(diffLbl);
        int x5c =diffBox.x+((diffBox.width-diffSW)/2);
        drawString(g,diffLbl, x5c, y5);
        
        // draw autoplay warning
        if (newGameOptions().isAutoPlay()) {
            g.setFont(narrowFont(16));
            String warning = text("SETTINGS_AUTOPLAY_WARNING");
            List<String> warnLines = this.wrappedLines(g, warning, galaxyW);
            g.setColor(Color.white);
            int warnY = y5+s60;
            for (String line: warnLines) {
                drawString(g,line, galaxyX, warnY);
                warnY += s18;
            }
        }

        // settings button
        int cnr = s5;
        g.setFont(narrowFont(20));
        String text6 = text("SETUP_BUTTON_SETTINGS");
        int sw6 = g.getFontMetrics().stringWidth(text6);
        int x6 = settingsBox.x+((settingsBox.width-sw6)/2);
        int y6 = settingsBox.y+settingsBox.height-s8;
        Color c6 = hoverBox == settingsBox ? Color.yellow : GameUI.borderBrightColor();
        drawShadowedString(g, text6, 2, x6, y6, GameUI.borderDarkColor(), c6);
        Stroke prev = g.getStroke();
        g.setStroke(stroke1);
        g.drawRoundRect(settingsBox.x, settingsBox.y, settingsBox.width, settingsBox.height, cnr, cnr);
        g.setStroke(prev);

        // left button
        g.setFont(narrowFont(30));
        String text1 = text("SETUP_BUTTON_BACK");
        int sw1 = g.getFontMetrics().stringWidth(text1);
        int x1 = backBox.x+((backBox.width-sw1)/2);
        int y1 = backBox.y+backBox.height-s12;
        Color c1 = hoverBox == backBox ? Color.yellow : GameUI.borderBrightColor();
        drawShadowedString(g, text1, 2, x1, y1, GameUI.borderDarkColor(), c1);
        prev = g.getStroke();
        g.setStroke(stroke1);
        g.drawRoundRect(backBox.x, backBox.y, backBox.width, backBox.height, cnr, cnr);
        g.setStroke(prev);

        // right button
        String text2 = text("SETUP_BUTTON_START");
        int sw2= g.getFontMetrics().stringWidth(text2);
        int x2 = startBox.x+((startBox.width-sw2)/2);
        int y2 = startBox.y+startBox.height-s12;
        Color c2 = hoverBox == startBox ? Color.yellow : GameUI.borderBrightColor();
        drawShadowedString(g, text2, 2, x2, y2, GameUI.borderDarkColor(), c2);
        prev = g.getStroke();
        g.setStroke(stroke1);
        g.drawRoundRect(startBox.x, startBox.y, startBox.width, startBox.height, cnr, cnr);
        g.setStroke(prev);

        if (starting) {
            NoticeMessage.setStatus(text("SETUP_CREATING_GALAXY"));
            drawNotice(g, 30);
        }
    }
    private void drawGalaxyShape(Graphics g, GalaxyShape sh, int x, int y, int w, int h) {
        float factor = min((float)h/sh.height(), (float)w/sh.width());
        int dispH = (int) (sh.height()*factor);
        int dispW = (int) (sh.width()*factor);
        int xOff = x+(w-dispW)/2;
        int yOff = y+(h-dispH)/2;
        Point.Float pt = new Point.Float();
        for (int i=0; i<sh.numberStarSystems();i++) {
            sh.coords(i, pt);
            int x0 = xOff + (int) (pt.x*factor);
            int y0 = yOff + (int) (pt.y*factor);
            g.setColor(starColor(i));
            g.fillRect(x0, y0, s2, s2);
        }
    }
    private Color starColor(int i) {
        switch(i % 4) {
            case 0:
            case 1:
                    return Color.lightGray;
            case 2: return Color.gray;
            case 3: return Color.white;
        }
        return Color.gray;
    }
    private BufferedImage playerRaceImg() {
        if (playerRaceImg == null) {
            String selRace = newGameOptions().selectedPlayerRace();
            playerRaceImg = newBufferedImage(Race.keyed(selRace).diploMug());
        }
        return playerRaceImg;
    }
    public void nextGalaxySize(boolean bounded, boolean click) {
        String nextSize = newGameOptions().nextGalaxySize(bounded);
        if (nextSize.equals(newGameOptions().selectedGalaxySize()))
            return;
        if (click) softClick();
        newGameOptions().selectedGalaxySize(newGameOptions().nextGalaxySize(bounded));
        repaint();
    }
    public void prevGalaxySize(boolean bounded, boolean click) {
        String prevSize = newGameOptions().prevGalaxySize(bounded);
        if (prevSize.equals(newGameOptions().selectedGalaxySize()))
            return;
        if (click) softClick();
        newGameOptions().selectedGalaxySize(newGameOptions().prevGalaxySize(bounded));
        int numOpps = newGameOptions().selectedNumberOpponents();
        int maxOpps = newGameOptions().maximumOpponentsOptions();
        if (maxOpps < numOpps) {
            for (int i=maxOpps;i<numOpps;i++)
                newGameOptions().selectedOpponentRace(i,null);
            newGameOptions().selectedNumberOpponents(maxOpps);
        }
        repaint();
    }
    public void nextGalaxyShape(boolean click) {
        if (click) softClick();
        newGameOptions().selectedGalaxyShape(newGameOptions().nextGalaxyShape());
        newGameOptions().galaxyShape().quickGenerate(); 
        backImg = null;
        repaint();
    }
    public void prevGalaxyShape(boolean click) {
        if (click) softClick();
        newGameOptions().selectedGalaxyShape(newGameOptions().prevGalaxyShape());
        newGameOptions().galaxyShape().quickGenerate(); 
        backImg = null;
        repaint();
    }
	
    public void nextMapOption1(boolean click) {
        if (click) softClick();
        newGameOptions().selectedGalaxyShapeOption1(newGameOptions().nextGalaxyShapeOption1());
        newGameOptions().galaxyShape().quickGenerate(); 
        repaint();
    }
    public void prevMapOption1(boolean click) {
        if (click) softClick();
        newGameOptions().selectedGalaxyShapeOption1(newGameOptions().prevGalaxyShapeOption1());
        newGameOptions().galaxyShape().quickGenerate(); 
        repaint();
    }
    public void nextMapOption2(boolean click) {
        if (click) softClick();
        newGameOptions().selectedGalaxyShapeOption2(newGameOptions().nextGalaxyShapeOption2());
        newGameOptions().galaxyShape().quickGenerate(); 
        repaint();
    }
    public void prevMapOption2(boolean click) {
        if (click) softClick();
        newGameOptions().selectedGalaxyShapeOption2(newGameOptions().prevGalaxyShapeOption2());
        newGameOptions().galaxyShape().quickGenerate(); 
        repaint();
    }
    public void nextGameDifficulty(boolean click) {
        if (click) softClick();
        newGameOptions().selectedGameDifficulty(newGameOptions().nextGameDifficulty());
        repaint();
    }
    public void prevGameDifficulty(boolean click) {
        if (click) softClick();
        newGameOptions().selectedGameDifficulty(newGameOptions().prevGameDifficulty());
        repaint();
    }
    public void nextOpponentAI(boolean click) {
        if (click) softClick();
        newGameOptions().selectedOpponentAIOption(newGameOptions().nextOpponentAI());
        repaint();
    }
    public void prevOpponentAI(boolean click) {
        if (click) softClick();
        newGameOptions().selectedOpponentAIOption(newGameOptions().prevOpponentAI());
        repaint();
    }
    public void nextResearchRate(boolean click) {
        if (click) softClick();
        newGameOptions().selectedResearchRate(newGameOptions().nextResearchRate());
        repaint();
    }
    public void increaseOpponents(boolean click) {
        int numOpps = newGameOptions().selectedNumberOpponents();
        if (numOpps >= newGameOptions().maximumOpponentsOptions())
            return;
        if (click) softClick();
        newGameOptions().selectedNumberOpponents(numOpps+1);
        repaint();
    }
    public void decreaseOpponents(boolean click) {
        int numOpps = newGameOptions().selectedNumberOpponents();
        if (numOpps <= 0)
            return;
        if (click) softClick();
        newGameOptions().selectedOpponentRace(numOpps-1,null);
        newGameOptions().selectedNumberOpponents(numOpps-1);
        repaint();
    }
    public void nextSpecificOpponentAI(int i, boolean click) {
        if (click) softClick();
        newGameOptions().nextSpecificOpponentAI(i+1);
        repaint();
    }
    public void prevSpecificOpponentAI(int i, boolean click) {
        if (click) softClick();
        newGameOptions().prevSpecificOpponentAI(i+1);
        repaint();
    }
    public void nextOpponent(int i, boolean click) {
        if (click) softClick();
        newGameOptions().nextOpponent(i);
        repaint();
    }
    public void prevOpponent(int i, boolean click) {
        if (click) softClick();
        newGameOptions().prevOpponent(i);
        repaint();
    }
    public void goToOptions() {
        buttonClick();
        StartOptionsUI optionsUI = RotPUI.startOptionsUI();
        optionsUI.open(this);
        release();
    }
    public void goToRaceSetup() {
        buttonClick();
        RotPUI.instance().selectSetupRacePanel();
        release();
    }
    public void startGame() {
        starting = true;
        Race r = Race.keyed(newGameOptions().selectedPlayerRace());
        GameUI.gameName = r.setupName()+ " - "+text(newGameOptions().selectedGalaxySize())+ " - "+text(newGameOptions().selectedGameDifficulty());
        repaint();
        buttonClick();
        UserPreferences.setForNewGame();
        final Runnable save = () -> {
            long start = System.currentTimeMillis();
            GameSession.instance().startGame(newGameOptions());
            RotPUI.instance().mainUI().checkMapInitialized();
            RotPUI.instance().selectIntroPanel();
            log("TOTAL GAME START TIME:" +(System.currentTimeMillis()-start));
            log("Game Name; "+GameUI.gameName);
            starting = false;
            release();
        };
        SwingUtilities.invokeLater(save);
    }
    private BufferedImage backImg() {
        if (backImg == null)
            initBackImg();
        return backImg;
    }
    private void initBackImg() {
        int w = getWidth();
        int h = getHeight();
        backImg = newOpaqueImage(w, h);
        Graphics2D g = (Graphics2D) backImg.getGraphics();
        setFontHints(g);
        Race race = Race.keyed(newGameOptions().selectedPlayerRace());

        // background image
        Image back = GameUI.defaultBackground;
        int imgW = back.getWidth(null);
        int imgH = back.getHeight(null);
        g.drawImage(back, 0, 0, w, h, 0, 0, imgW, imgH, this);

        // shade Box Dimensions
        leftBoxX = s80;
        rightBoxX = scaled(665);
        boxW = scaled(505);
        boxY = s95;
        leftBoxH = scaled(615);
        rightBoxH = scaled(575);
        // draw opponents title
        String title1 = text("SETUP_SELECT_OPPONENTS");
        g.setFont(narrowFont(50));
        int sw1 = g.getFontMetrics().stringWidth(title1);
        int x1 = leftBoxX+((boxW-sw1)/2);
        int y0 = s80;
        drawBorderedString(g, title1, 2, x1, y0, Color.darkGray, Color.white);

        // draw galaxy title
        String title2 = text("SETUP_SELECT_GALAXY");
        g.setFont(narrowFont(50));
        int sw1b = g.getFontMetrics().stringWidth(title2);
        int x1b = rightBoxX+((boxW-sw1b)/2);
        drawBorderedString(g, title2, 2, x1b, y0, Color.darkGray, Color.white);

        // draw opponents shading
        g.setColor(GameUI.setupShade());
        g.fillRect(leftBoxX, boxY, boxW, leftBoxH);

        // draw opponents back gradient
        g.setPaint(GameUI.opponentsSetupBackground());
        g.fillRect(leftBoxX+s20, boxY+s20, boxW-s40, s92);

        // draw race box for player
        BufferedImage backimg = SetupRaceUI.raceBackImg();
        int mugW = backimg.getWidth();
        int mugH = backimg.getHeight();
        g.drawImage(backimg, leftBoxX+s25, boxY+s25, this);
        g.drawImage(playerRaceImg(), leftBoxX+s25, boxY+s25, mugW, mugH, this);

        // draw player vs opponent text
        int x2 = leftBoxX+s25+mugW+s15;
        int y2 = boxY+s25+mugH-s42;
        g.setFont(narrowFont(28));
        String header1 = text("SETUP_OPPONENTS_HEADER_1", race.setupName());
        String header2 = text("SETUP_OPPONENTS_HEADER_2", race.setupName());
        int swHdr = g.getFontMetrics().stringWidth(header1);
        drawBorderedString(g, header1, 1, x2, y2, Color.black, Color.white);

        // draw opponent count box and arrows
        int x2b = x2+swHdr+s5;
        g.setColor(GameUI.setupFrame());
        oppBox.setBounds(x2b,y2-s30,s30,s35);
        g.fill(oppBox);
        int x2c = x2b+s33;
        int y2c = (int)(oppBox.getY()+(oppBox.getHeight()/2));
        oppBoxD.reset();
        oppBoxD.addPoint(x2c,y2c+s2);
        oppBoxD.addPoint(x2c+s13,y2c+s2);
        oppBoxD.addPoint(x2c+s7,y2c+s17);
        g.fill(oppBoxD);
        oppBoxU.reset();
        oppBoxU.addPoint(x2c,y2c-s1);
        oppBoxU.addPoint(x2c+s13,y2c-s1);
        oppBoxU.addPoint(x2c+s7,y2c-s16);
        g.fill(oppBoxU);

        int x2d = x2c+s20;
        drawBorderedString(g, header2, 1, x2d, y2, Color.black, Color.white);
        
        // draw ai selection
        String header3 = text("SETUP_OPPONENT_AI");
        g.setFont(narrowFont(18));
        int swHdr3 = g.getFontMetrics().stringWidth(header3);
        g.setColor(SystemPanel.blackText);
        int y3 = y2+s32;
        int x3 = x2+s20;
        drawString(g,header3, x3, y3);

        int sliderW = s100+s20;
        int sliderH = s18;
        int sliderY = y3-sliderH+s5;
        int sliderX = x3+swHdr3+s20;
        g.setColor(GameUI.setupFrame());

        aiBoxL.reset();
        aiBoxL.addPoint(sliderX-s4,sliderY+s1);
        aiBoxL.addPoint(sliderX-s4,sliderY+sliderH-s2);
        aiBoxL.addPoint(sliderX-s13,sliderY+(sliderH/2));
        g.fill(aiBoxL);
        aiBoxR.reset();
        aiBoxR.addPoint(sliderX+sliderW+s4,sliderY+s1);
        aiBoxR.addPoint(sliderX+sliderW+s4,sliderY+sliderH-s2);
        aiBoxR.addPoint(sliderX+sliderW+s13,sliderY+(sliderH/2));
        g.fill(aiBoxR);
        aiBox.setBounds(sliderX, sliderY, sliderW, sliderH);
        g.fill(aiBox);

        // draw galaxy shading
        g.setColor(GameUI.setupShade());
        g.fillRect(rightBoxX, boxY, boxW, rightBoxH);

        // draw galaxy background gradient
        g.setPaint(GameUI.galaxySetupBackground());
        g.fillRect(rightBoxX+s20, boxY+s20, boxW-s40, rightBoxH-s40);
        g.setColor(Color.black);
        galaxyX = rightBoxX+s40;
        galaxyY = boxY+s40;
        galaxyW = boxW-s80;
        galaxyH = scaled(325);
        g.fillRect(galaxyX, galaxyY, galaxyW, galaxyH);

        // draw 3 galaxy option labels
        int sectionW = (boxW-s40) / 3;
        int y5 = galaxyY+galaxyH+s45;
        g.setFont(narrowFont(24));
        String shapeLbl = text("SETUP_GALAXY_SHAPE_LABEL");
        int shapeSW = g.getFontMetrics().stringWidth(shapeLbl);
        int x5a = rightBoxX+s20+((sectionW-shapeSW)/2);
        drawBorderedString(g, shapeLbl, 1, x5a, y5, Color.black, Color.white);

        String sizeLbl = text("SETUP_GALAXY_SIZE_LABEL");
        int sizeSW = g.getFontMetrics().stringWidth(sizeLbl);
        int x5b = rightBoxX+s20+sectionW+((sectionW-sizeSW)/2);
        drawBorderedString(g, sizeLbl, 1, x5b, y5, Color.black, Color.white);

        String diffLbl = text("SETUP_GAME_DIFFICULTY_LABEL");
        int diffSW = g.getFontMetrics().stringWidth(diffLbl);
        int x5c = rightBoxX+s20+sectionW+sectionW+((sectionW-diffSW)/2);
        drawBorderedString(g, diffLbl, 1, x5c, y5, Color.black, Color.white);

        sliderW = sectionW*2/3;
        sliderH = s18;
        sliderY = y5+s10;
        sliderX = rightBoxX+s20+(sectionW/6);
        g.setColor(GameUI.setupFrame());

        shapeBoxL.reset();
        shapeBoxL.addPoint(sliderX-s4,sliderY+s1);
        shapeBoxL.addPoint(sliderX-s4,sliderY+sliderH-s2);
        shapeBoxL.addPoint(sliderX-s13,sliderY+(sliderH/2));
        g.fill(shapeBoxL);
        shapeBoxR.reset();
        shapeBoxR.addPoint(sliderX+sliderW+s4,sliderY+s1);
        shapeBoxR.addPoint(sliderX+sliderW+s4,sliderY+sliderH-s2);
        shapeBoxR.addPoint(sliderX+sliderW+s13,sliderY+(sliderH/2));
        g.fill(shapeBoxR);
        shapeBox.setBounds(sliderX, sliderY, sliderW, sliderH);
        g.fill(shapeBox);
		
	mapOption1BoxL.reset();
        mapOption1BoxR.reset();
        mapOption1Box.setBounds(0,0,0,0);
        if (newGameOptions().numGalaxyShapeOption1() > 0) {
            mapOption1BoxL.addPoint(sliderX-s4,sliderY+s1+s20);
            mapOption1BoxL.addPoint(sliderX-s4,sliderY+sliderH-s2+s20);
            mapOption1BoxL.addPoint(sliderX-s13,sliderY+(sliderH/2)+s20);
            g.fill(mapOption1BoxL);
            mapOption1BoxR.addPoint(sliderX+sliderW+s4,sliderY+s1+s20);
            mapOption1BoxR.addPoint(sliderX+sliderW+s4,sliderY+sliderH-s2+s20);
            mapOption1BoxR.addPoint(sliderX+sliderW+s13,sliderY+(sliderH/2)+s20);
            g.fill(mapOption1BoxR);
            mapOption1Box.setBounds(sliderX, sliderY+s20, sliderW, sliderH);
            g.fill(mapOption1Box);
        }

	mapOption2BoxL.reset();
        mapOption2BoxR.reset();
        mapOption2Box.setBounds(0,0,0,0);
        if (newGameOptions().numGalaxyShapeOption2() > 0) {
            mapOption2BoxL.addPoint(sliderX-s4,sliderY+s1+s40);
            mapOption2BoxL.addPoint(sliderX-s4,sliderY+sliderH-s2+s40);
            mapOption2BoxL.addPoint(sliderX-s13,sliderY+(sliderH/2)+s40);
            g.fill(mapOption2BoxL);
            mapOption2BoxR.addPoint(sliderX+sliderW+s4,sliderY+s1+s40);
            mapOption2BoxR.addPoint(sliderX+sliderW+s4,sliderY+sliderH-s2+s40);
            mapOption2BoxR.addPoint(sliderX+sliderW+s13,sliderY+(sliderH/2)+s40);
            g.fill(mapOption2BoxR);
            mapOption2Box.setBounds(sliderX, sliderY+s40, sliderW, sliderH);
            g.fill(mapOption2Box);
        }

        sliderX += sectionW;
        sizeBoxL.reset();
        sizeBoxL.addPoint(sliderX-s4,sliderY+s1);
        sizeBoxL.addPoint(sliderX-s4,sliderY+sliderH-s2);
        sizeBoxL.addPoint(sliderX-s13,sliderY+(sliderH/2));
        g.fill(sizeBoxL);
        sizeBoxR.reset();
        sizeBoxR.addPoint(sliderX+sliderW+s4,sliderY+s1);
        sizeBoxR.addPoint(sliderX+sliderW+s4,sliderY+sliderH-s2);
        sizeBoxR.addPoint(sliderX+sliderW+s13,sliderY+(sliderH/2));
        g.fill(sizeBoxR);
        sizeBox.setBounds(sliderX, sliderY, sliderW, sliderH);
        g.fill(sizeBox);

        sliderX += sectionW;
        diffBoxL.reset();
        diffBoxL.addPoint(sliderX-s4,sliderY+s1);
        diffBoxL.addPoint(sliderX-s4,sliderY+sliderH-s2);
        diffBoxL.addPoint(sliderX-s13,sliderY+(sliderH/2));
        g.fill(diffBoxL);
        diffBoxR.reset();
        diffBoxR.addPoint(sliderX+sliderW+s4,sliderY+s1);
        diffBoxR.addPoint(sliderX+sliderW+s4,sliderY+sliderH-s2);
        diffBoxR.addPoint(sliderX+sliderW+s13,sliderY+(sliderH/2));
        g.fill(diffBoxR);
        diffBox.setBounds(sliderX, sliderY, sliderW, sliderH);
        g.fill(diffBox);

        int cnr = s5;

        // draw settings button
        int smallButtonH = s30;
        int smallButtonW = scaled(180);
        settingsBox.setBounds(scaled(960), scaled(610), smallButtonW, smallButtonH);
        g.setPaint(GameUI.buttonLeftBackground());
        g.fillRoundRect(settingsBox.x, settingsBox.y, smallButtonW, smallButtonH, cnr, cnr);

        int buttonH = s45;
        int buttonW = scaled(220);
        // draw left button
        backBox.setBounds(scaled(710), scaled(685), buttonW, buttonH);
        g.setPaint(GameUI.buttonLeftBackground());
        g.fillRoundRect(backBox.x, backBox.y, buttonW, buttonH, cnr, cnr);

        // draw right button
        startBox.setBounds(scaled(950), scaled(685), buttonW, buttonH);
        g.setPaint(GameUI.buttonRightBackground());
        g.fillRoundRect(startBox.x, startBox.y, buttonW, buttonH, cnr, cnr);

        g.dispose();
    }
    public BufferedImage smallRaceBackImg() {
        if (smBackImg == null)
            initSmallBackImg();
        return smBackImg;
    }
    private void initSmallBackImg() {
        int w = s54;
        int h = s58;
        smBackImg = gc().createCompatibleImage(w, h);

        Point2D center = new Point2D.Float(w/2, h/2);
        float radius = s56;
        float[] dist = {0.0f, 0.1f, 0.5f, 1.0f};
        Color[] colors = {GameUI.raceCenterColor(), GameUI.raceCenterColor(), GameUI.raceEdgeColor(), GameUI.raceEdgeColor()};
        RadialGradientPaint p = new RadialGradientPaint(center, radius, dist, colors);
        Graphics2D g = (Graphics2D) smBackImg.getGraphics();
        g.setPaint(p);
        g.fillRect(0, 0, w, h);
        g.dispose();
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
                goToRaceSetup();
                return;
          case KeyEvent.VK_ENTER:
                startGame();
                return;
        }
    }
    @Override
    public void mouseDragged(MouseEvent e) {  }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Shape prevHover = hoverBox;
        hoverBox = null;
        if (startBox.contains(x,y))
            hoverBox = startBox;
        else if (backBox.contains(x,y))
            hoverBox = backBox;
        else if (settingsBox.contains(x,y))
            hoverBox = settingsBox;
        else if (shapeBoxL.contains(x,y))
            hoverBox = shapeBoxL;
        else if (shapeBoxR.contains(x,y))
            hoverBox = shapeBoxR;
        else if (shapeBox.contains(x,y))
            hoverBox = shapeBox;
	else if (mapOption1BoxL.contains(x,y))
            hoverBox = mapOption1BoxL;
        else if (mapOption1BoxR.contains(x,y))
            hoverBox = mapOption1BoxR;
        else if (mapOption1Box.contains(x,y))
            hoverBox = mapOption1Box;		
	else if (mapOption2BoxL.contains(x,y))
            hoverBox = mapOption2BoxL;
        else if (mapOption2BoxR.contains(x,y))
            hoverBox = mapOption2BoxR;
        else if (mapOption2Box.contains(x,y))
            hoverBox = mapOption2Box;		
        else if (sizeBoxL.contains(x,y))
            hoverBox = sizeBoxL;
        else if (sizeBoxR.contains(x,y))
            hoverBox = sizeBoxR;
        else if (sizeBox.contains(x,y))
            hoverBox = sizeBox;
        else if (aiBoxL.contains(x,y))
            hoverBox = aiBoxL;
        else if (aiBoxR.contains(x,y))
            hoverBox = aiBoxR;
        else if (aiBox.contains(x,y))
            hoverBox = aiBox;
        else if (diffBoxL.contains(x,y))
            hoverBox = diffBoxL;
        else if (diffBoxR.contains(x,y))
            hoverBox = diffBoxR;
        else if (diffBox.contains(x,y))
            hoverBox = diffBox;
        else if (oppBoxU.contains(x,y))
            hoverBox = oppBoxU;
        else if (oppBoxD.contains(x,y))
            hoverBox = oppBoxD;
        else if (oppBox.contains(x,y))
            hoverBox = oppBox;
        else {
            boolean selectable = newGameOptions().selectableAI();
            for (int i=0;i<oppAI.length;i++) {
                if (selectable && oppAI[i].contains(x,y)) {
                    hoverBox = oppAI[i];
                    break;
                }
                else if (oppSet[i].contains(x,y)) {
                    hoverBox = oppSet[i];
                    break;
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
        if (hoverBox == backBox)
            goToRaceSetup();
        else if (hoverBox == settingsBox)
            goToOptions();
        else if (hoverBox == startBox)
            startGame();
        else if (hoverBox == shapeBoxL)
            prevGalaxyShape(true);
        else if (hoverBox == shapeBox)
            nextGalaxyShape(true);
        else if (hoverBox == shapeBoxR)
            nextGalaxyShape(true);		
	else if (hoverBox == mapOption1BoxL)
            prevMapOption1(true);
        else if (hoverBox == mapOption1Box)
            nextMapOption1(true);
        else if (hoverBox == mapOption1BoxR)
            nextMapOption1(true);
	else if (hoverBox == mapOption2BoxL)
            prevMapOption2(true);
        else if (hoverBox == mapOption2Box)
            nextMapOption2(true);
        else if (hoverBox == mapOption2BoxR)
            nextMapOption2(true);
        else if (hoverBox == sizeBoxL)
            prevGalaxySize(false, true);
        else if (hoverBox == sizeBox)
            nextGalaxySize(false, true);
        else if (hoverBox == sizeBoxR)
            nextGalaxySize(false, true);
        else if (hoverBox == aiBoxL)
            prevOpponentAI(true);
        else if (hoverBox == aiBox)
            nextOpponentAI(true);
        else if (hoverBox == aiBoxR)
            nextOpponentAI(true);
        else if (hoverBox == diffBoxL)
            prevGameDifficulty(true);
        else if (hoverBox == diffBox)
            nextGameDifficulty(true);
        else if (hoverBox == diffBoxR)
            nextGameDifficulty(true);
        else if (hoverBox == oppBoxU)
            increaseOpponents(true);
        else if (hoverBox == oppBox)
            increaseOpponents(true);
        else if (hoverBox == oppBoxD)
            decreaseOpponents(true);
        else {
            for (int i=0;i<oppSet.length;i++) {
                if (hoverBox == oppSet[i]) {
                    nextOpponent(i, true);
                    break;
                }
                else if (hoverBox == oppAI[i]) {
                    nextSpecificOpponentAI(i, true);
                    break;
                }
            }
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
    public void mouseWheelMoved(MouseWheelEvent e) {
        boolean up = e.getWheelRotation() > 0;
        if (hoverBox == shapeBox) {
            if (up)
                prevGalaxyShape(false);
            else
                nextGalaxyShape(false);
        }
	else if (hoverBox == mapOption1Box) {
            if (up)
                prevMapOption1(false);
            else
                nextMapOption1(false);
        }
	else if (hoverBox == mapOption2Box) {
            if (up)
                prevMapOption2(false);
            else
                nextMapOption2(false);
        }
        else if (hoverBox == sizeBox) {
            if (up)
                prevGalaxySize(true, false);
            else
                nextGalaxySize(true, false);
        }
        else if (hoverBox == aiBox) {
            if (up)
                prevOpponentAI(false);
            else
                nextOpponentAI(false);
        }
        else if (hoverBox == diffBox) {
            if (up)
                prevGameDifficulty(false);
            else
                nextGameDifficulty(false);
        }
        else if (hoverBox == oppBox) {
            if (up)
                decreaseOpponents(false);
            else
                increaseOpponents(false);
        }
        else {
            for (int i=0;i<oppSet.length;i++) {
                if (hoverBox == oppSet[i]) {
                    if (up)
                        prevOpponent(i, false);
                    else
                        nextOpponent(i, false);
                }
            }
            for (int i=0;i<oppAI.length;i++) {
                if (hoverBox == oppAI[i]) {
                    if (up)
                        prevSpecificOpponentAI(i, false);
                    else
                        nextSpecificOpponentAI(i, false);
                }
            }
        }
    }
}
