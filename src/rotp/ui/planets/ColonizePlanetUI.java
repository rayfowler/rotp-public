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
package rotp.ui.planets;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import rotp.model.empires.Race;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.planet.PlanetType;
import rotp.model.ships.ShipDesign;
import rotp.ui.BasePanel;
import rotp.ui.BaseTextField;
import rotp.ui.FadeInPanel;
import rotp.ui.RotPUI;
import rotp.ui.main.SystemPanel;
import rotp.util.sound.SoundClip;

public class ColonizePlanetUI extends FadeInPanel implements MouseListener, MouseMotionListener, ActionListener {
    private static final long serialVersionUID = 1L;
    private static final Color edgeC = new Color(44,59,30);
    private static final Color midC = new Color(70,93,48);

    private enum Display {  LANDING, CLAIMING, NAMING }
    private Display displayMode;

    private int startLandingX = s70;
    private final int startLandingY = scaled(-100);
    private int stopLandingX = s70;
    private int stopLandingY = scaled(375);
    private int landingShipW = scaled(200);
    private int numLandingFrames = 150;

    StarSystem system;
    // claimXY values based on race (offset from their transport ship)
    private int claimingDelay, startClaimingX, startClaimingY, stopClaimingX, stopClaimingY;
    private int numClaimingFrames = 200;

    private final Color grayBdr = new Color(189,189,189);
    private final Color dlgBack = new Color(123,123,123);
    private final Color dlgBdr = new Color(123,123,123,128);

    private ShipFleet fleet;

    private Image landscapeImg;
    private Image shipImg;
    private Image flagPole;
    private BufferedImage raceImg;
    private boolean exited = false;
    private int landingX = 0;
    private int landingY = 0;
    private int landingFrame = 0;
    private int claimingX = 0;
    private int claimingY = 0;
    private int claimingFrame = 0;
    private boolean showFlag = false;
    private boolean showAstronaut = false;
    private final BaseTextField nameField;
    private SoundClip shipLanding;
    private SoundClip claimingJingle;
    final static String CANCEL_ACTION = "cancel-input";

    private final List<Image> descendingFrames = new ArrayList<>();
    private final List<Integer> descendingFrameRefs = new ArrayList<>();
    private final List<Image> openingFrames = new ArrayList<>();
    private final List<Integer> openingFrameRefs = new ArrayList<>();
    int frameIndex = 0;
    private LinearGradientPaint okBackground;
    int okButtonX, okButtonY, okButtonW, okButtonH;
    boolean okHovering;
    Rectangle okBox;

    public ColonizePlanetUI() {
        super();
        nameField = new BaseTextField("");
        okBox = new Rectangle();
        init();
    }
    private void init() {
        addMouseListener(this);
        addMouseMotionListener(this);

        Border fieldBdr = BorderFactory.createCompoundBorder(newLineBorder(grayBdr, 1), newEmptyBorder(0,10,0,0));
        nameField.setBackground(new Color(59,59,59));
        nameField.setBorder(fieldBdr);
        nameField.setFont(narrowFont(26));
        nameField.setForeground(SystemPanel.whiteText);
        nameField.setCaretColor(SystemPanel.whiteText);
        nameField.putClientProperty("caretWidth", s2);
        nameField.addMouseListener(this);
        InputMap im = nameField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = nameField.getActionMap();
        im.put(KeyStroke.getKeyStroke("ESCAPE"), CANCEL_ACTION);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), CANCEL_ACTION);
        am.put(CANCEL_ACTION, new CancelAction());

        setLayout(null);
        add(nameField);
    }
    public void init(int sysId, ShipFleet fl, ShipDesign des) {
        claimingJingle = null;
        okBackground = null;
        okHovering = false;
        okBox.setBounds(0,0,0,0);
        setFPS(30);
        screenBuffer();
        Race pr = player().race();

        raceImg = asBufferedImage(pr.troopHostile.firingFrames().get(0));

        // hide nameField so keyFocus can be regained by main UI
        nameField.setVisible(false);
        displayMode = Display.LANDING;
        frameIndex = 0;
        
        descendingFrames.clear();
        descendingFrameRefs.clear();        
        allFrames(pr.transportDescKey, pr.transportDescFrames, 0, descendingFrames, descendingFrameRefs);
        openingFrames.clear();
        openingFrameRefs.clear();
        allFrames(pr.transportOpenKey, pr.transportOpenFrames, 0, openingFrames, openingFrameRefs);

        system = galaxy().system(sysId);

        PlanetType pType = system.planet().type();
        fleet = fl;
        exited = false;
        int w = getWidth();
        int h = getHeight();

        landscapeImg = newOpaqueImage(w,h);
        Graphics2D g = (Graphics2D) landscapeImg.getGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, w, h);
        super.paintComponent(g);
        g.drawImage(pType.atmosphereImage(), 0, 0, w, h, null);
        //drawStar(g);
        g.drawImage(pType.randomCloudImage(), 0, 0, w, h, null);
        g.drawImage(system.planet().landscapeImage(), 0, 0, w, h, null);
        drawTitle(g,w);
        g.dispose();

        // scale the needed width of the ship with race-specific modifiers
        landingShipW = scaled(pType.shipW(0)*pr.transportW/100);
        startLandingX = scaled(pType.shipX(0));
        stopLandingX = startLandingX;
        stopLandingY = scaled(pType.shipY(0)-pr.transportYOffset);
        numLandingFrames = pr.transportLandingFrames;
        numClaimingFrames = pr.colonistWalkingFrames;

        // reset animation vars
        landingX = startLandingX;
        landingY = startLandingY;
        landingFrame = 0;
        claimingDelay = pr.colonistDelay();
        startClaimingX = pr.colonistStartX();
        startClaimingY = pr.colonistStartY();
        stopClaimingX = pr.colonistStopX();
        stopClaimingY = pr.colonistStopY();
        claimingX = startClaimingX;
        claimingY = startClaimingY;
        claimingFrame = 0;
        showFlag = false;
        nameField.setLimit(24);
        nameField.setText(player().sv.name(sysId));
        nameField.setCaretPosition(nameField.getText().length());
        nameField.setVisible(false);
        nameField.setBounds((w/2)-scaled(150),(h/2)-s100, scaled(300), s40);
        stopAmbience();
        shipLanding = playAudioClip(pr.shipAudioKey);

        if (!playAnimations()) {
            frameIndex = 5000;
            landingFrame = 5000;
            landingX = stopLandingX;
            landingY = stopLandingY;
            showAstronaut = true;
            claimingX = stopClaimingX;
            claimingY = stopClaimingY;
            showFlag = true;
            nameField.setVisible(true);
            nameField.requestFocus();
            displayMode = Display.NAMING;
        }
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        //log("Action Performed");
    }
    @Override
    public void paintComponent(Graphics g) {
        //long tm0 = System.currentTimeMillis();
        switch (displayMode) {
            case LANDING:
            case CLAIMING:
            case NAMING:
                paintPlanetSceneToImage(screenBuffer); break;
        }

        drawMapBuffer(g);

        // draw name box over image if necesary
        if (displayMode == Display.NAMING) {
            nameField.repaint();
            nameField.requestFocus();
        }
    }
    public void drawMapBuffer(Graphics g) {
        g.drawImage(screenBuffer,0,0,getWidth(), getHeight(), null);
        drawOverlay(g);
    }
    private Image flagPole() {
        if (flagPole == null)
            flagPole = currentFrame("FlagPole");
        return flagPole;
    }
    private Image nextDescendingShipImage() {
        int frame = frameIndex++;
        for (int i=0;i<descendingFrameRefs.size();i++) {
            if (frame < descendingFrameRefs.get(i))
                return descendingFrames.get(i);
            frame -= descendingFrameRefs.get(i);
        }      
        return descendingFrames.get(descendingFrames.size()-1);
    }
    private Image nextOpeningShipImage() {
        int frame = frameIndex++;
        for (int i=0;i<openingFrameRefs.size();i++) {
            if (frame < openingFrameRefs.get(i))
                return openingFrames.get(i);
            frame -= openingFrameRefs.get(i);
        }      
        return openingFrames.get(openingFrames.size()-1);
    }
    private void paintPlanetSceneToImage(Image img) {
        int w = getWidth();
        int h = getHeight();
        Graphics2D g = (Graphics2D) img.getGraphics();

        g.drawImage(landscapeImg, 0, 0, null);

        // draw landing ship
        if (displayMode == Display.LANDING)
            shipImg = nextDescendingShipImage();
        else
            shipImg = nextOpeningShipImage();

        //long tm3 = System.currentTimeMillis();

        //log("displayMode: ", str(displayMode), "  frame:", str(frameIndex));

        int shipW = shipImg.getWidth(null);
        int shipH = shipImg.getHeight(null);
        int dispW = landingShipW;
        float shipScale = (float)dispW/shipW;
        int dispH = (int)(shipH*shipScale);
        g.drawImage(shipImg, landingX, landingY-dispH, landingX+dispW, landingY, 0,0,shipW,shipH, null);

        // draw walking astronaut
        if ((displayMode == Display.CLAIMING)
        || (displayMode == Display.NAMING)) {
            if (showAstronaut) {
                int claimX = (int)(stopLandingX+(shipScale*claimingX));
                int claimY = (int)(stopLandingY-dispH+(shipScale*claimingY));
                int raceW = raceImg.getWidth(null);
                int raceH = raceImg.getHeight(null);
                dispH = s40;
                dispW = raceW*dispH/raceH;
                g.drawImage(raceImg, claimX, claimY-dispH, claimX+dispW, claimY, 0,0,raceW,raceH, null);
                // draw flag
                if (showFlag) {
                    int fX = claimX+dispW;
                    int fY = claimY-dispH;
                    Image flg = flagPole();
                    g.drawImage(flg, fX+s5, fY, fX+s15, fY+s30, 0, 0, flg.getWidth(null), flg.getHeight(null), null);
                    Image flagImg = player().race().flagNorm();
                    g.drawImage(flagImg, fX, fY-s18, fX+s20, fY+s2, 0, 0, flagImg.getWidth(null), flagImg.getHeight(null), null);
                }
            }
        }

        // draw prompt box for new name
        if ((displayMode == Display.NAMING) && !exited) {
            int pad = s10;
            String promptText = text("MAIN_COLONIZE_NAME_PROMPT");
            String okStr = text("MAIN_COLONIZE_NAME_OK");
            g.setFont(narrowFont(20));
            int swOK = g.getFontMetrics().stringWidth(okStr);
            okButtonW = swOK+s20;
            g.setFont(narrowFont(32));
            int sw = g.getFontMetrics().stringWidth(promptText);
            int boxW = sw + s60+ okButtonW+scaled(200)+pad+pad;
            int boxH = s70+pad+pad;
            int x2 = (w-boxW)/2;
            int y2 = (h/2)-s100;
            g.setColor(dlgBdr);
            g.fillRect(x2, y2, boxW, boxH);
            g.setColor(dlgBack);
            g.fillRect(x2+pad, y2+pad, boxW-pad-pad, boxH-pad-pad);
            int x3 = x2+s30;
            int y3 = y2+s55;
            g.setColor(Color.black);
            drawShadowedString(g, promptText, 4, x3, y3, SystemPanel.textShadowC, SystemPanel.whiteText);
            nameField.setBounds(x3+sw+s10, y3-s25, scaled(200), s32);
            // draw ok button
            okButtonX = x2+boxW-okButtonW-s30;
            okButtonY = y2+pad+s22;
            okButtonH = s28;
            if (okBackground == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D yesStart = new Point2D.Float(okButtonX, 0);
                Point2D yesEnd = new Point2D.Float(okButtonX+okButtonW, 0);
                Color[] yesColors = {edgeC, midC, edgeC };
                okBackground = new LinearGradientPaint(yesStart, yesEnd, dist, yesColors);
            }
            g.setPaint(okBackground);
            g.fillRoundRect(okButtonX, okButtonY, okButtonW,okButtonH,s5,s5);
            Color c0 = okHovering ? SystemPanel.yellowText : Color.white;
            g.setColor(c0);
            Stroke prevStr = g.getStroke();
            g.setStroke(BasePanel.stroke2);
            g.drawRoundRect(okButtonX, okButtonY, okButtonW,okButtonH,s5,s5);
            g.setStroke(prevStr);
            okBox.setBounds(okButtonX, okButtonY, okButtonW,okButtonH);
            int x2a = okButtonX+((okButtonW-swOK)/2);
            g.setFont(narrowFont(20));
            drawBorderedString(g, okStr, x2a, okButtonY+okButtonH-s8, SystemPanel.textShadowC, c0);
        }

        //draw skip text at bottom
        drawSkipText(g, displayMode == Display.NAMING);
        g.dispose();
    }
    private void drawTitle(Graphics2D g, int w) {
        // draw title last (so it overlays any ship)
        String title = text("MAIN_COLONIZE_ANIMATION_TITLE", str(galaxy().currentYear()), player().name());
        g.setFont(narrowFont(36));
        List<String> lines = wrappedLines(g, title, w*5/6);
        int y1 = s10;
        for (String line: lines) {
            y1 += s40;
            int sw = g.getFontMetrics().stringWidth(line);
            int x1 = (w-sw)/2;
            drawBorderedString(g, line, 2, x1, y1, Color.black, SystemPanel.orangeText);
        }
    }
    private void playClaimingJingle() {
        if (claimingJingle != null)
            return;
                    
        if (system.planet().isArtifact())
            claimingJingle = playAudioClip("ColonizeRuinsAmbience");
        else if (system.planet().isEnvironmentHostile())
            claimingJingle = playAudioClip("ColonizeHostileAmbience");
        else     
            claimingJingle = playAudioClip("ColonizeNormalAmbience");
    }
    public void advanceMode() {
        if (stillFading())
            return;

        switch(displayMode) {
            case LANDING:
                frameIndex = 0;
                landingX = stopLandingX;
                landingY = stopLandingY;
                showAstronaut = false;
                displayMode = Display.CLAIMING;
                if (shipLanding != null)
                    shipLanding.endPlaying();
                break;
            case CLAIMING:
                claimingX = stopClaimingX;
                claimingY = stopClaimingY;
                showFlag = true;
                nameField.setVisible(true);
                nameField.requestFocus();
                displayMode = Display.NAMING;
                break;
            case NAMING:
                finish();
                return;
        }
        repaint();
    }
    public void finish() {
        if (claimingJingle != null)
            claimingJingle.endPlaying();
        if (!nameField.getText().trim().isEmpty()) {
            String name = nameField.getText().trim();
            player().sv.name(system.id, name);
        }

        nameField.setVisible(false);
        exited = true;
        repaint();

        fleet.colonizeSystem(system);
        resetFPS();
        RotPUI.instance().requestFocus();
        RotPUI.instance().selectMainPanel();
        session().resumeNextTurnProcessing();
    }
    @Override
    public void playAmbience() {
        // no background ambience on this screen
        return;
    }
    @Override
    public String ambienceSoundKey() {
        switch(displayMode) {
            case LANDING:
            case CLAIMING:
            case NAMING:
            default:
                if (system.planet().isArtifact())
                    return "ColonizeRuinsAmbience";
                else if (system.planet().isEnvironmentHostile())
                    return "ColonizeHostileAmbience";
                else     
                    return "ColonizeNormalAmbience";
        }
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        switch(displayMode) {
            case LANDING:
            case CLAIMING:
                advanceMode(); break;
            case NAMING:
                if (e.getSource() == nameField)
                    nameField.requestFocus();
                else if ((e.getButton() <= 3) && okHovering)
                    advanceMode();
                break;
        }
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }
    @Override
    public void mouseDragged(MouseEvent e) { }

    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        boolean prevHovering = okHovering;
        okHovering = okBox.contains(x,y);
        if (okHovering != prevHovering) {
            repaint(okBox);
            return;
        }
    }


    @Override
    public void animate() {
        if (displayMode == Display.NAMING)
            return;
        if (playAnimations()) {
            advanceFade();
            switch(displayMode) {
                case LANDING    : moveLandingShip(); break;
                case CLAIMING   : moveClaimingSoldier(); break;
                default: break;
            }
            repaint();
        }
    }
    @Override
    public void keyPressed(KeyEvent e) {
        if (stillFading())
            return;
        int k = e.getKeyCode();

        switch(k) {
            case KeyEvent.VK_ESCAPE:
                escapeAction();
        }
    }
    protected void escapeAction() {
        if (stillFading())
            return;

        switch(displayMode) {
            case LANDING:
                landingX = stopLandingX;
                landingY = stopLandingY;
                showAstronaut = false;
                displayMode = Display.CLAIMING;
                if (shipLanding != null)
                    shipLanding.endPlaying();
                break;
            case CLAIMING:
                claimingX = stopClaimingX;
                claimingY = stopClaimingY;
                showFlag = true;
                nameField.setVisible(true);
                nameField.requestFocus();
                // do not switch mode. When nameField is given keyFocus
                // the escape key will be passed to him before thread
                // completes. If mode is naming, it will jump to finish()
                //displayMode = Display.NAMING;
                break;
            case NAMING:
                finish();
                return;
        }
        repaint();
    }
    public void escapeAction2() {
        switch(displayMode) {
            case CLAIMING:
                displayMode = Display.NAMING;
                break;
            case NAMING:
                advanceMode();
                break;
            default:
                break;
        }
    }
    private void moveLandingShip() {
        landingFrame++;
        if (landingFrame > numLandingFrames) {
            if (landingFrame > numLandingFrames)
                advanceMode();
            return;
        }
        landingX = startLandingX + ((stopLandingX - startLandingX) * landingFrame / numLandingFrames);
        landingY = startLandingY + ((stopLandingY - startLandingY) * landingFrame / numLandingFrames);
    }
    private void moveClaimingSoldier() {
        claimingFrame++;
        int transportOpenDelay = claimingDelay;
        if (claimingFrame < transportOpenDelay) {
            showAstronaut = false;
            return;
        }
        int walkFrame = claimingFrame - transportOpenDelay;
        playClaimingJingle();
        showAstronaut = true;
        if (claimingFrame > (numClaimingFrames+transportOpenDelay)) {
            showFlag = true;
            if (claimingFrame > (numClaimingFrames+transportOpenDelay+5))
                advanceMode();
            return;
        }
        claimingX = startClaimingX + ((stopClaimingX - startClaimingX) * walkFrame / numClaimingFrames);
        claimingY = startClaimingY + ((stopClaimingY - startClaimingY) * walkFrame / numClaimingFrames);
    }
    class CancelAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        @Override
        public void actionPerformed(ActionEvent ev) {
            escapeAction2();
        }
    }
}
