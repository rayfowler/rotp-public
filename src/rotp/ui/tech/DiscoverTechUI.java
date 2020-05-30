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
package rotp.ui.tech;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.empires.EspionageMission;
import rotp.model.galaxy.Galaxy;
import rotp.model.galaxy.StarSystem;
import rotp.model.tech.Tech;
import rotp.ui.FadeInPanel;
import rotp.ui.main.SystemPanel;

public class DiscoverTechUI extends FadeInPanel implements MouseListener, MouseMotionListener, ActionListener {
    private static final long serialVersionUID = 1L;
    static final int SCIENTIST_VIEW = 0;
    static final int SPY_VIEW = 1;
    static final int TROOPER_VIEW = 2;

    static final int MODE_SHOW_TECH = 0;
    static final int MODE_FRAME_EMPIRE = 1;
    static final int MODE_REALLOCATE = 2;

    static final int BACKGROUND_LABORATORY = 0;
    static final int BACKGROUND_ALIEN_LAB = 1;
    static final int BACKGROUND_RUINS = 2;
    static final int BACKGROUND_DERELICT = 3;

    private int view;
    private int mode;
    private int background;
    String title;
    private Tech tech;
    private StarSystem system;
    private Empire sourceEmpire;
    private Empire frameEmpire1;
    private Empire frameEmpire2;
    private EspionageMission mission;

    static Color yellowText = new Color(226,173,26);
    static Color backShadingC = new Color(0,0,0,192);

    static Color dimWhite = new Color(225,225,255);
    static Color gray2 = new Color(123,123,123);
    static Color grayShade = new Color(123,123,123,160);
    static Color buttonShadowC = new Color(33,33,33);

    final static Color dialogBackC = new Color(104,104,104);
    final static Color dialogTextBackC = new Color(19,19,19);
    static Color dialogTextFore =new Color(233,233,233);

    private LinearGradientPaint frameBackC1, frameBackC2, allocateBackC;

    Rectangle button1 = new Rectangle();
    Rectangle button2 = new Rectangle();
    Rectangle button3 = new Rectangle();
    Rectangle button4 = new Rectangle();
    Rectangle frameButton1 = new Rectangle();
    Rectangle frameButton2 = new Rectangle();
    Rectangle hoverBox;

    boolean finished = false;
    float holoPct = 0f;
    int talkTimeMs = 5000;
    long startTimeMs;

    private Tech tech()               { return tech; }

    private boolean shouldReallocate()  { return player().isPlayerControlled() && tech.promptToReallocate() && !tech.isObsolete(player()); }
    private boolean shouldFrameEmpire() { return (mission != null) && (frameEmpire1 != null) && (frameEmpire2 != null); }

    public DiscoverTechUI() {
        init();
    }
    private void init() {
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    @Override
    public String ambienceSoundKey() { return "ResearchAmbience"; }

    public void discoverTech(String techId) {
        clearBuffer();
        startFadeTimer();
        startTimeMs = System.currentTimeMillis();
        holoPct = 0;
        view = SCIENTIST_VIEW;
        mode = MODE_SHOW_TECH;
        background = BACKGROUND_LABORATORY;
        tech = tech(techId);
        sourceEmpire = player();
        player().race().resetScientist();
        title = text("TECH_DISCOVERY_TITLE", text(player().raceName()), text(tech().cat.key()));
        finished = false;
        mission = null;
        frameEmpire1 = null;
        frameEmpire2 = null;
    }
    public void tradeTech(String techId, int empId) {
        Galaxy gal = galaxy();
        clearBuffer();
        startFadeTimer();
        startTimeMs = System.currentTimeMillis();
        holoPct = 0;
        view = SCIENTIST_VIEW;
        mode = MODE_SHOW_TECH;
        background = BACKGROUND_ALIEN_LAB;
        tech = tech(techId);
        sourceEmpire = gal.empire(empId);
        player().race().resetScientist();
        title = text("TECH_TRADED_TITLE", text(player().raceName()), sourceEmpire.raceName());
        finished = false;
        mission = null;
        frameEmpire1 = null;
        frameEmpire2 = null;
    }
    public void plunderTech(String techId, int sysId, int empId) {
        Galaxy gal = galaxy();
        clearBuffer();
        startFadeTimer();
        startTimeMs = System.currentTimeMillis();
        holoPct = 0;
        view = TROOPER_VIEW;
        mode = MODE_SHOW_TECH;
        background = empId == Empire.NULL_ID ? BACKGROUND_RUINS : BACKGROUND_ALIEN_LAB;
        tech = tech(techId);
        system = gal.system(sysId);
        sourceEmpire = empId == Empire.NULL_ID ? null : gal.empire(empId);  // could be null for artifact planets
        player().race().resetSoldier();
        if (sourceEmpire == null)
            title = text("TECH_SCOUTED_TITLE", player().sv.name(sysId));
        else
            title = text("TECH_PLUNDERED_TITLE", player().sv.name(sysId));
        finished = false;
        mission = null;
        frameEmpire1 = null;
        frameEmpire2 = null;
    }
    public void plunderShipTech(String techId, int empId) {
        // if emp <0, then this is a precursor ship (e.g. Derelict Random Event)
        clearBuffer();
        startFadeTimer();
        startTimeMs = System.currentTimeMillis();
        holoPct = 0;
        view = TROOPER_VIEW;
        mode = MODE_SHOW_TECH;
        background = BACKGROUND_DERELICT;
        tech = tech(techId);
        mission = null;
        sourceEmpire = null;
        player().race().resetScientist();
        title = empId == -2 ? text("TECH_GUARDIAN_TITLE", text(player().raceName())) :  text("TECH_DERELICT_TITLE", text(player().raceName()));
        finished = false;
        frameEmpire1 = null;
        frameEmpire2 = null;
    }
    public void stealTech(EspionageMission m, int empId) {
        Galaxy gal = galaxy();
        clearBuffer();
        startFadeTimer();
        startTimeMs = System.currentTimeMillis();
        holoPct = 0;
        view = SPY_VIEW;
        mode = MODE_SHOW_TECH;
        background = BACKGROUND_ALIEN_LAB;
        tech = m.choice();
        system = m.targetSystem();
        sourceEmpire = gal.empire(empId);
        player().race().resetSpy();
        mission = m;
        title = text("TECH_STOLEN_TITLE", text(sourceEmpire.raceName()), player().sv.name(system.id));
        finished = false;
        frameEmpire1 = mission.canFrame() ? mission.empiresToFrame().get(0) : null;
        frameEmpire2 = mission.canFrame() ? mission.empiresToFrame().get(1) : null;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintToImage(screenBuffer());

        g.drawImage(screenBuffer(),0,0,null);
        drawOverlay(g);
    }
    public void paintToImage(Image dataImg) {
        Graphics2D g = (Graphics2D) dataImg.getGraphics();

        Image techImg = null;
        Image raceImg;
        BufferedImage labImg;
        BufferedImage holoImg;
        boolean talking = (System.currentTimeMillis() - startTimeMs) < talkTimeMs;
        Empire pl = player();
        
        switch(background) {
            case BACKGROUND_ALIEN_LAB:
                labImg = sourceEmpire.race().laboratory();
                holoImg = sourceEmpire.race().holograph();
                break;
            case BACKGROUND_DERELICT:
                labImg = currentFrame("DERELICT_SHIP");
                holoImg = null;
                break;
            case BACKGROUND_RUINS:
                labImg = currentFrame(system.ruinsKey());
                holoImg = null;
                break;
            case BACKGROUND_LABORATORY:
            default:
                labImg = pl.race().laboratory();
                holoImg = pl.race().holograph();
                break;
        }
        switch(view) {
            case SPY_VIEW:
                raceImg = talking ? pl.race().spyTalking() :  pl.race().spyQuiet();
                break;
            case TROOPER_VIEW:
                raceImg = talking ? pl.race().soldierTalking() : pl.race().soldierQuiet();
                break;
            case SCIENTIST_VIEW:
            default:
                if (sourceEmpire == null)
                    raceImg = talking ? pl.race().scientistTalking() : pl.race().scientistQuiet();
                else
                    raceImg = sourceEmpire.race().scientistQuiet();
                break;
        }
        int w = getWidth();
        int h = getHeight();

        int fX = (int) (player().race().labFlagX()*w);
        int fY = h*25/100;
        int fW = scaled(210);
        int fH = scaled(140);
        g.drawImage(labImg, 0, 0, w, h, null);
        if (holoImg != null) {
            Composite prevComposite = g.getComposite();
            float fluxPct = holoPct;
            if (fluxPct > 1) {
                fluxPct = (fluxPct % 1) / 2;
                if (fluxPct <= 0.25)
                    fluxPct = 1 - fluxPct;
                else
                    fluxPct = 0.5f + fluxPct;
            }
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fluxPct);
            g.setComposite(ac);
            g.drawImage(holoImg, 0, 0, w, h, null);
            g.setComposite(prevComposite);
        }
        if ((holoPct > 2) && (techImg != null)) {
            float flagPct = (float) min(0.3f, (holoPct - 2));
            Composite prevComposite = g.getComposite();
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, flagPct);
            g.setComposite(ac);
            g.drawImage(techImg, fX, fY, fX+fW, fY+fH, 0, 0, techImg.getWidth(null), techImg.getHeight(null), null);
            g.setComposite(prevComposite);
        }
        g.drawImage(raceImg, 0, 0, w, h, 0, 0, raceImg.getWidth(null), raceImg.getHeight(null), null);

        if (mode == MODE_SHOW_TECH)
            drawTechDiscovery(g, title);
        else if (mode == MODE_REALLOCATE)
            drawReallocation(g);
        else if (mode == MODE_FRAME_EMPIRE)
            drawFrameEmpire(g);

        g.dispose();
    }
    private void drawTechDiscovery(Graphics2D g, String title) {
        int w = getWidth();
        int h = getHeight();
        // draw title - centered
        g.setFont(narrowFont(40));
        int sw0 = g.getFontMetrics().stringWidth(title);
        int x0 = (w-sw0)/2;
        int y0 = s50;
        drawBorderedString(g, title, 2, x0, y0, Color.black, yellowText);

        // draw tech name (caps) - centered in right half
        String techName = tech.name();
        g.setFont(narrowFont(40));
        int sw1 = g.getFontMetrics().stringWidth(techName);
        int x1 =  (w*3/4) - (sw1/2);
        // ensure at least 20px right margin
        if ((x1 + sw1+ s20) > w)
                x1 = w - sw1 - s20;
        int y1 = scaled(160);
        drawBorderedString(g, techName, 2, x1, y1, Color.black, Color.white);

        // draw tech description?
        g.setFont(narrowFont(24));
        List<String> wrappedLines = wrappedLines(g, tech.detail(), scaled(500));
        int lineHeight = s28;
        y1 = h - scaled(200);
        for (String line: wrappedLines) {
            sw1 = g.getFontMetrics().stringWidth(line);
            x1 = (w*3/4)-(sw1/2);
            drawBorderedString(g, line, 2, x1, y1, Color.black, Color.white);
            y1 += lineHeight;
        }
    }
    private void drawFrameEmpire(Graphics2D g) {
        int w = getWidth();
        int h = getHeight();
        int mgn = s15;
        int buttonPad = s30;
        int textBoxW = scaled(450);
        int boxW = textBoxW+(2*mgn);
        int lineH = s28;
        int buttonW = (textBoxW-(3*buttonPad))/2;
        int buttonH = s35;

        g.setColor(backShadingC);
        g.fillRect(0, 0, w, h);

        String frameTitle = text("NOTICE_ESPIONAGE_FRAME_TITLE");
        String subtitle = text("NOTICE_ESPIONAGE_FRAME_SUBTITLE");

        g.setFont(narrowFont(26));
        List<String> titleLines = wrappedLines(g, frameTitle, textBoxW-(2*mgn));

        int boxH = (2*mgn)+(lineH*titleLines.size())+s60+buttonH;
        int x0 = (w-boxW)/2;
        int y0 = (h-boxH)/2;

        g.setColor(grayShade);
        g.fillRect(x0,y0,boxW,boxH);
        g.setColor(gray2);
        g.fillRect(x0+mgn,y0+mgn,textBoxW,boxH-mgn-mgn);

        g.setColor(Color.white);
        int y1 = y0+mgn;
        for (String line: titleLines) {
            y1 += lineH;
            int sw = g.getFontMetrics().stringWidth(line);
            int x1 = x0+mgn+((textBoxW-sw)/2);
            drawShadowedString(g, line, 3, x1, y1, SystemPanel.textShadowC, Color.white);
        }

        g.setFont(narrowFont(18));
        g.setColor(Color.black);
        int sw = g.getFontMetrics().stringWidth(subtitle);
        int x2 = x0+(boxW-sw)/2;
        int y2 = y1+s30;
        g.drawString(subtitle, x2, y2);

        int x3a = x0+mgn+buttonPad;
        int x3b = x3a+buttonW+buttonPad;
        int y3 = y2+s10;

        if (frameBackC1 == null)
            initFrameButtonGradients(x3a, x3b, buttonW);

        if (frameEmpire1 != null)
            drawButton(g, frameBackC1, frameEmpire1.name(), frameButton1, x3a, y3, buttonW, buttonH);
        if (frameEmpire2 != null)
            drawButton(g, frameBackC2, frameEmpire2.name(), frameButton2, x3b, y3, buttonW, buttonH);
    }
    private void drawReallocation(Graphics2D g) {
        int w = getWidth();
        int h = getHeight();

        int bdr = s10;
        int titleLineH = s25;
        int optionH = s35;
        int NUM_OPTIONS = 4;

        String title2 = text(tech().followup().toString());

        int sideMgn = scaled(150);
        // get width of text box
        int boxLeftX = (getWidth()/2)+sideMgn;
        int boxRightX = getWidth() - sideMgn;
        int boxWidth = boxRightX - boxLeftX;

        // break title into multiple lines based on width
        g.setFont(narrowFont(20));
        List<String> titleLines = wrappedLines(g, title2, boxWidth-s40);
        int titleH = titleLines.size()*titleLineH;

        // calculate vertical size of text box
        int boxTopY = scaled(200);
        int boxBottomY = boxTopY+s20+titleH+s20+(optionH*NUM_OPTIONS)+s20+s70;
        int boxHeight = boxBottomY - boxTopY;

        // draw main box with shaded borders
        g.setColor(grayShade);
        g.fillRect(boxLeftX-bdr, boxTopY-bdr, boxWidth+bdr+bdr, boxHeight+bdr+bdr);
        g.setColor(gray2);
        g.fillRect(boxLeftX, boxTopY, boxWidth, boxHeight);

        // draw title for main box
        int y0 = boxTopY+s10;
        int x0 = boxLeftX+s20;
        g.setFont(narrowFont(22));
        for (String titleLine: titleLines) {
            y0 += titleLineH;
            drawShadowedString(g, titleLine, 4, x0, y0, SystemPanel.textShadowC, dimWhite);
        }

        // draw buttons
        int buttonW = boxWidth-s40;  //
        if (allocateBackC == null)
            initAllocateButtonGradients(x0, buttonW);

        int amt = (int) (tech().baseReallocateAmount()*100);

        y0 = y0+s30;
        drawButton(g, allocateBackC, text("TECH_ALLOCATE_NO"), button1, x0, y0, buttonW, optionH);

        y0 += (optionH+s15);
        drawButton(g, allocateBackC, text("TECH_ALLOCATE_PCT", str(amt)), button2, x0, y0, buttonW, optionH);

        y0 += (optionH+s15);
        drawButton(g, allocateBackC, text("TECH_ALLOCATE_PCT", str(2*amt)), button3, x0, y0, buttonW, optionH);

        y0 += (optionH+s15);
        drawButton(g, allocateBackC, text("TECH_ALLOCATE_PCT", str(3*amt)), button4, x0, y0, buttonW, optionH);
    }
    private void drawButton(Graphics2D g, LinearGradientPaint gradient, String label, Rectangle actionBox, int x1, int y, int w, int h) {
        if (actionBox != null)
            actionBox.setBounds(x1,y,w,h);
        int cnr = s2;
        g.setColor(buttonShadowC);
        Stroke prev = g.getStroke();
        g.setStroke(stroke2);
        g.drawRoundRect(x1+s3,y+s2,w-s2,h,cnr,cnr);
        g.setStroke(prev);

        g.setPaint(gradient);
        g.fillRoundRect(x1,y,w,h,cnr,cnr);

        boolean hovering = (actionBox != null) && (actionBox == hoverBox);
        Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;

        g.setFont(narrowFont(20));
        int sw = g.getFontMetrics().stringWidth(label);
        int x0 = x1+((w-sw)/2);
        drawShadowedString(g, label, 3, x0, y+h-s11, SystemPanel.textShadowC, c0);

        g.setColor(c0);
        Stroke prev2 = g.getStroke();
        g.setStroke(stroke2);
        g.drawRoundRect(x1+s1,y,w-s2,h,cnr,cnr);
        g.setStroke(prev2);
    }
    private void initFrameButtonGradients(int x1, int x2, int w) {
        Point2D left1 = new Point2D.Float(x1, 0);
        Point2D left2 = new Point2D.Float(x2, 0);
        Point2D end1 = new Point2D.Float(x1+w, 0);
        Point2D end2 = new Point2D.Float(x2+w, 0);
        float[] dist = {0.0f, 0.5f, 1.0f};

        Color greenEdgeC = new Color(44,59,30);
        Color greenMidC = new Color(71,93,48);
        Color[] greenColors = {greenEdgeC, greenMidC, greenEdgeC };

        frameBackC1 = new LinearGradientPaint(left1, end1, dist, greenColors);
        frameBackC2 = new LinearGradientPaint(left2, end2, dist, greenColors);
    }
    private void initAllocateButtonGradients(int x1, int w) {
        Point2D left1 = new Point2D.Float(x1, 0);
        Point2D end1 = new Point2D.Float(x1+w, 0);
        float[] dist = {0.0f, 0.5f, 1.0f};

        Color grayEdgeC = new Color(59,59,59);
        Color grayMidC = new Color(90,90,90);
        Color[] grayColors = {grayEdgeC, grayMidC, grayEdgeC };

        allocateBackC = new LinearGradientPaint(left1, end1, dist, grayColors);
    }
    public float scaleFactor(int imgWidth, int imgHeight, int uiWidth, int uiHeight) {
        float widthRatio = (float)uiWidth/imgWidth;
        float heightRatio = (float)uiHeight/imgHeight;

        if (widthRatio > 1)
            return min(widthRatio, heightRatio);
        else
            return max(widthRatio, heightRatio);
    }
    private void finish() {
        finished = true;
        repaint();
        session().resumeNextTurnProcessing();
    }
    private void handleShowTechAction() {
        softClick();
        if (shouldFrameEmpire()) {
            mode = MODE_FRAME_EMPIRE;
            repaint();
            return;
        }
        else if (shouldReallocate()) {
            mode = MODE_REALLOCATE;
            repaint();
            return;
        }
        finish();
    }
    private void handleFrameEmpireAction(int buttonId) {
        if (buttonId == 1) {
            mission.frameEmpire(frameEmpire1);
        }
        else if (buttonId == 2) {
            mission.frameEmpire(frameEmpire2);
        }
        if (mission.hasFramed()) {
            if (shouldReallocate()) {
                mode = MODE_REALLOCATE;
                repaint();
                return;
            }
            finish();
        }
    }
    private void handleReallocateSystemsAction(int buttonId) {
        float amt = tech().baseReallocateAmount();
        if (buttonId == 1) {
            finish();
        }
        else if (buttonId == 2) {
            player().addColonyOrder(tech().followup(), amt);
            finish();
        }
        else if (buttonId == 3) {
            player().addColonyOrder(tech().followup(), amt*2);
            finish();
        }
        else if (buttonId == 4) {
            player().addColonyOrder(tech().followup(), amt*3);
            finish();
        }
    }
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
        if (e.getClickCount() > 1)
            return;
        if (stillFading())
            return;

        int x = e.getX();
        int y = e.getY();

        if (mode == MODE_SHOW_TECH)
            handleShowTechAction();
        else if (mode == MODE_FRAME_EMPIRE) {
            if (frameButton1.contains(x,y))
                handleFrameEmpireAction(1);
            else if (frameButton2.contains(x,y))
                handleFrameEmpireAction(2);
        }
        else if (mode == MODE_REALLOCATE) {
            if (button1.contains(x,y))
                handleReallocateSystemsAction(1);
            else if (button2.contains(x,y))
                handleReallocateSystemsAction(2);
            else  if (button3.contains(x,y))
                handleReallocateSystemsAction(3);
            else if (button4.contains(x,y))
                handleReallocateSystemsAction(4);
        }
    }
    @Override
    public void mouseDragged(MouseEvent arg0) { }
    @Override
    public void mouseMoved(MouseEvent e) {
        if (stillFading())
            return;

        int x = e.getX();
        int y = e.getY();
        Rectangle prevHover = hoverBox;
        hoverBox = null;
        if (button1.contains(x,y))
            hoverBox = button1;
        else if (button2.contains(x,y))
            hoverBox = button2;
        else if (button3.contains(x,y))
            hoverBox = button3;
        else if (button4.contains(x,y))
            hoverBox = button4;
        else if (frameButton1.contains(x,y))
            hoverBox = frameButton1;
        else if (frameButton2.contains(x,y))
            hoverBox = frameButton2;

        if (prevHover != hoverBox)
            repaint();
    }
    @Override
    public void animate() {
        if (!playAnimations())
            return;

        advanceFade();
        holoPct += .1;
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (stillFading())
            return;

        int k = e.getKeyCode();
        switch(k) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_SPACE:
                if (mode == MODE_SHOW_TECH)
                    handleShowTechAction();
                return;
        }
    }
}
