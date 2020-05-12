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
package rotp.ui.combat;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import rotp.model.combat.*;
import rotp.model.empires.Empire;
import rotp.model.empires.ShipView;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.*;
import rotp.ui.FadeInPanel;
import rotp.ui.main.SystemPanel;
import rotp.util.Base;
import javax.swing.*;
import javax.swing.border.Border;

public class ShipBattleUI extends FadeInPanel implements Base, MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    private enum Display { INTRO, RESULT }
    static final Color shipCountTextC = new Color(255,240,78);
    static final Color grayBackC = new Color(123,123,123);
    static final Color hostileBorderC = new Color(151,18,23);
    public static final Color currentBorderC = new Color(163,123,0);
    static final Color friendlyBorderC = new Color(79,79,79);
    static final Color redButtonCenterC = new Color(120,45,42);
    static final Color redButtonEdgeC = new Color(92,20,21);
    static final Color greenButtonCenterC = new Color(70,93,47);
    static final Color greenButtonEdgeC = new Color(44,59,30);
    static final Color grayButtonCenterC = new Color(128,128,128);
    static final Color grayButtonEdgeC = new Color(92,92,92);
    static final Color greenQuickButtonC = new Color(70,93,47);
    static final Color grayQuickButtonC = new Color(92,92,92,128);
    static final Color lineColor = new Color(150,150,150);
    private static final Color grayLeftC = new Color(173,173,173);
    private static final Color grayRightC = new Color(115,115,115);
    private static final Color grayBorderC = new Color(210,210,210);

    public static final Color spaceBlue = new Color(0,0,32);
    private static final int GRID_COUNT_X = 10;
    private static final int GRID_COUNT_Y = 8;
    int ptX[] = new int[GRID_COUNT_X+1];
    int ptY[] = new int[GRID_COUNT_Y+1];
    public int boxH = -1;
    public int boxW = -1;

    private int mouseGridX = 0;
    private int mouseGridY = 0;
    int planetX = 250;
    int planetY = 200;
    int planetR = 400;
    int pad = s10;
    int barH = s45;
    float planetRotateSpeed = 0.1f;
    private boolean drawingPlanet = false;
    private boolean planetDrawn = false;
    boolean showPlanet = false;
    boolean exited = false;
    BufferedImage renderedPlanetImage;
    Image[] asteroids = new Image[16];
    public int[][] asteroidRoll = new int[GRID_COUNT_X][GRID_COUNT_Y];

    private LinearGradientPaint menuBackC;
    private LinearGradientPaint moveBackC;
    private LinearGradientPaint resolveButtonBackC;
    private LinearGradientPaint retreatButtonBackC;
    private LinearGradientPaint nextShipButtonBackC;
    private LinearGradientPaint exitBackC;
    private LinearGradientPaint playPauseBackC;
    private LinearGradientPaint leftGreenActionBackC;
    private LinearGradientPaint leftGrayActionBackC;
    private LinearGradientPaint rightGreenActionBackC;
    private LinearGradientPaint rightGrayActionBackC;
    Map<ShipDesign, Integer> leftFleet = new LinkedHashMap<>();
    Map<ShipDesign, Integer> rightFleet = new LinkedHashMap<>();
    final Map<ShipDesign, Integer> counts = new LinkedHashMap<>();
    Empire leftEmpire;
    Empire rightEmpire;

    Display mode;
    ShipCombatManager  mgr;

    Rectangle planetBox = new Rectangle();
    Rectangle hoverBox;
    Rectangle currentGrid;
    Rectangle resolveBox = new Rectangle();
    Rectangle retreatBox = new Rectangle();
    Rectangle exitBox = new Rectangle();
    Rectangle playPauseBox = new Rectangle();
    Rectangle nextBox = new Rectangle();
    public Rectangle[][] combatGrids = new Rectangle[GRID_COUNT_X][GRID_COUNT_Y];
    List<ShipActionButton> shipActionButtons = new ArrayList<>();
    Rectangle shipButtonOverlay = new Rectangle();
    FlightPath shipTravelPath = null;
    ShipDoneButton shipDoneButton = new ShipDoneButton();
    ShipMoveButton shipMoveButton = new ShipMoveButton();
    ShipFireAllButton shipFireAllButton = new ShipFireAllButton();
    ShipTeleportButton shipTeleportButton = new ShipTeleportButton();
    ShipRetreatButton shipRetreatButton = new ShipRetreatButton();
    ShipWeaponButton[] shipWeaponButton = new ShipWeaponButton[ShipDesign.maxWeapons];
    ShipSpecialButton[] shipSpecialButton = new ShipSpecialButton[ShipDesign.maxSpecials];
    Robot robot;

    Color[] redColors = new Color[3];
    Color[] greenColors = new Color[3];
    Color[] yellowColors = new Color[3];
    Color[] grayColors = new Color[3];
    BufferedImage combatBackground;

    public ShipBattleUI() {
        init();
    }
    private void init() {
        try {
            robot = new Robot();
        } catch (AWTException ex) {  err("Could not create robot in ShipBattleUI");  }
        
        for (int i=0;i<shipWeaponButton.length;i++)
            shipWeaponButton[i] = new ShipWeaponButton();
        for (int i=0;i<shipSpecialButton.length;i++)
            shipSpecialButton[i] = new ShipSpecialButton();

        setBackground(Color.black);
        Border bord = BorderFactory.createLineBorder(Color.black, pad);
        setBorder(bord);
        addMouseListener(this);
        addMouseMotionListener(this);

        asteroids[0] = image("COMBAT_ASTEROIDS_01");
        asteroids[1] = image("COMBAT_ASTEROIDS_02");
        asteroids[2] = image("COMBAT_ASTEROIDS_03");
        asteroids[3] = image("COMBAT_ASTEROIDS_04");
        asteroids[4] = image("COMBAT_ASTEROIDS_05");
        asteroids[5] = image("COMBAT_ASTEROIDS_06");
        asteroids[6] = image("COMBAT_ASTEROIDS_07");
        asteroids[7] = image("COMBAT_ASTEROIDS_08");
        asteroids[8] = image("COMBAT_ASTEROIDS_09");
        asteroids[9] = image("COMBAT_ASTEROIDS_10");
        asteroids[10] = image("COMBAT_ASTEROIDS_11");
        asteroids[11] = image("COMBAT_ASTEROIDS_12");
        asteroids[12] = image("COMBAT_ASTEROIDS_13");
        asteroids[13] = image("COMBAT_ASTEROIDS_14");
        asteroids[14] = image("COMBAT_ASTEROIDS_15");
        asteroids[15] = image("COMBAT_ASTEROIDS_16");
    }
    @Override
    public void drawBackgroundStars(BufferedImage img, Graphics g, int w0, int h0, int minDist, int varDist) {
        // background stars need to be dimmer for this UI
        super.drawBackgroundStars(img, g, w0, h0, minDist, varDist);
        g.setColor(new Color(0,0,0,160));
        g.fillRect(0,0,w0,h0);

        int w = w0-pad-pad;
        int h = h0-pad-pad-barH;
        int x = pad;
        int y = pad;
        for (int i=0;i<ptX.length;i++)
            ptX[i] = x+(int) ((float)w*i/GRID_COUNT_X);
        for (int i=0;i<ptY.length;i++)
            ptY[i] = y+(int) ((float)h*i/GRID_COUNT_Y);

        // on first time through, initialize graphical bounds for each combat grid
        if (boxH < 0) {
            boxH = h / GRID_COUNT_Y;
            boxW = w / GRID_COUNT_X;
            for (int y0 = 0; y0 < GRID_COUNT_Y; y0++) {
                for (int x0 = 0; x0 < GRID_COUNT_X; x0++) {
                    int thisBoxW = ptX[x0+1]-ptX[x0];
                    int thisBoxH = ptY[y0+1]-ptY[y0];
                    Rectangle rect = new Rectangle(ptX[x0],ptY[y0],thisBoxW, thisBoxH);
                    combatGrids[x0][y0] = rect;
                }
            }
        }
        int variations = asteroids.length * 4;
        for (int x0=0;x0<GRID_COUNT_X;x0++) {
            for (int y0=0;y0<GRID_COUNT_Y;y0++) {
                if (mgr.asteroidMap[x0][y0])
                    asteroidRoll[x0][y0] = roll(0,variations-1);
                else
                    asteroidRoll[x0][y0] = -1;
            }
        }
    }
    private void paintAsteroids(Graphics g) {
        for (int x0=0;x0<GRID_COUNT_X;x0++) {
            for (int y0=0;y0<GRID_COUNT_Y;y0++) {
                if (mgr.asteroidMap[x0][y0]) {
                    Rectangle rect = combatGrids[x0][y0];
                    int index = asteroidRoll[x0][y0] / 4;
                    int orientation = asteroidRoll[x0][y0] % 4;
                    int w1 = rect.width;
                    int h1 = rect.height;
                    switch(orientation) {
                        case 0:
                            g.drawImage(asteroids[index], rect.x+w1, rect.y, -w1, h1, null); break;
                        case 1:
                            g.drawImage(asteroids[index], rect.x+w1, rect.y+h1, -w1, -h1, null); break;
                        case 2:
                            g.drawImage(asteroids[index], rect.x, rect.y, w1, h1, null); break;
                        case 3: default:
                            g.drawImage(asteroids[index], rect.x, rect.y+h1, w1, -h1, null); break;
                    }
                }
            }
        }
    }
    public void init(ShipCombatManager m) {
        mgr = m;
        mgr.ui(this);
        mgr.setInitialPause();
        exited = false;
        mode = Display.INTRO;
        renderedPlanetImage = mgr.system().planet().image(planetR, 135);

        planetDrawn = false;
        showPlanet = false;

        Color redEdgeC = new Color(92,20,20);
        Color redMidC = new Color(117,42,42);
        redColors[0]=redEdgeC; redColors[1]=redMidC; redColors[2]=redEdgeC;
        Color greenEdgeC = new Color(20,92,20);
        Color greenMidC = new Color(42,117,42);
        greenColors[0]=greenEdgeC; greenColors[1]=greenMidC; greenColors[2]=greenEdgeC;
        Color yellowEdgeC = new Color(90,90,20);
        Color yellowMidC = new Color(160,160,42);
        yellowColors[0]=yellowEdgeC; yellowColors[1]=yellowMidC; yellowColors[2]=yellowEdgeC;
        Color grayEdgeC = new Color(44,44,44);
        Color grayMidC = new Color(67,67,67);
        grayColors[0]=grayEdgeC; grayColors[1]=grayMidC; grayColors[2]=grayEdgeC;

        List<Empire> emps = mgr.results().empires();
        if  (emps.size() != 2)
            throw new RuntimeException("Not exactly 2 empires in ship combat");

        Empire aiEmpire = emps.get(0).isPlayer() ? emps.get(1) : emps.get(0);

        if (mgr.system().empire() == player()) {
            showPlanet = true;
            leftEmpire = player();
            rightEmpire = aiEmpire;
        }
        else if (mgr.system().empire() != player()) {
            showPlanet = true;
            rightEmpire = aiEmpire;
            leftEmpire = player();
        }
        else {
            rightEmpire = aiEmpire;
            leftEmpire = player();
        }

        initFleetStacks(leftEmpire, leftFleet);
        if (!mgr.results().isMonsterAttack())
            initFleetStacks(rightEmpire, rightFleet);
        if (mgr.currentStack().usingAI()) {
            final Runnable aiShips = () -> {
                nextStack();
            };
            invokeLater(aiShips);
        }
        rotateAndRenderPlanet();
    }
    @Override
    public String ambienceSoundKey()     { return "ShipCombatAmbience"; }
    @Override
    public boolean hasStarBackground()   { return false; }
    @Override
    public void animate() {
        if (this.stillFading()) {
            advanceFade();
            repaint();
        }
    }
    protected Image combatBackground() {
        if (combatBackground == null) {
            combatBackground = newOpaqueImage(getWidth(), getHeight());
            combatBackground.getGraphics().drawImage(starBackground(),0,0,null);
        }
        return combatBackground;
    }
    protected void resetCombatBackground() {
        BufferedImage newCombatBackground = newOpaqueImage(getWidth(), getHeight());
        newCombatBackground.getGraphics().drawImage(starBackground(),0,0,null);
        combatBackground = newCombatBackground;
    }
    public void paintAllImmediately() {
        paintImmediately(s10,s10,getWidth()-s20,getHeight()-s20);
    }
    public void paintAllImmediately(int minTime) {
        long t0 = System.currentTimeMillis();
        paintImmediately(s10,s10,getWidth()-s20,getHeight()-s20);
        long dur = System.currentTimeMillis() - t0;
        if (dur < minTime)
            sleep(minTime-dur);
    }
    public void paintCellImmediately(int x, int y) {
        paintImmediately(ptX[x], ptY[y], boxW, boxH);
    }
    public void paintCellsImmediately(int x0, int x1, int y0, int y1) {
        int x = min(ptX[min(x0,x1)]);
        int w = abs(ptX[x0]-ptX[x1])+boxW;
        int y = min(ptY[min(y0,y1)]);
        int h = abs(ptY[y0]-ptY[y1])+boxH;
        paintImmediately(x,y,w,h);
    }
    public void repaintButtonArea() {
        int x = pad;
        int y = pad;
        int w = getWidth()-pad-pad;
        int h = getHeight()-pad-pad;
        repaint(x,y+h-barH,w,barH);
    }
    @Override
    public void paintComponent(Graphics g0) {
        int x = pad;
        int y = pad;
        int w = getWidth()-pad-pad;
        int h = getHeight()-pad-pad;

        if (mgr.redrawMap) {
            resetCombatBackground();
            paintAsteroids(combatBackground().getGraphics());
            mgr.redrawMap = false;
        }

        Graphics2D g = (Graphics2D) screenBuffer().getGraphics();
        super.paintComponent(g);
        g.drawImage(combatBackground, 0, 0, null);

        // find the x,y of the grid being currently hovered
        int hoveringX = -1;
        int hoveringY = -1;
        NestedLoop:
        for (int x0=0;x0<GRID_COUNT_X;x0++) {
            for (int y0=0;y0<GRID_COUNT_Y;y0++) {
                if (currentGrid == combatGrids[x0][y0]) {
                    hoveringX = x0;
                    hoveringY = y0;
                    break NestedLoop;
                }
            }
        }

        paintShipsToImage(g,x,y,w,h-barH, hoveringX, hoveringY);
        paintMenuBarToImage(g,x,y+h-barH,w,barH);
        
        if (mode != Display.RESULT)
            paintStackActions(g, hoveringX, hoveringY);

        Stroke prev = g.getStroke();
        g.setStroke(stroke1);
        g.setColor(grayBackC);
        g.drawRect(x,y,w-s1,h-s1);
        g.setStroke(prev);

        drawMapBuffer(g0);
    }
    @Override
    protected void drawStars(Graphics g) {
        super.drawStars(g, getWidth(), getHeight());
    }
    private void initFleetStacks(Empire e, Map<ShipDesign,Integer> map) {
        map.clear();
        ShipFleet orbitingFleet = mgr.system().orbitingFleetForEmpire(e);
        List<ShipFleet> allFleets = new ArrayList<>();
        if (orbitingFleet != null)
            allFleets.add(mgr.system().orbitingFleetForEmpire(e));

        if (allFleets.size() > 1)
            throw new RuntimeException(concat("Too many ", e.name(), " fleets in combat"));

        // no fleets if only colony bases for defense
        if (allFleets.isEmpty())
            return;
        ShipFleet fl = allFleets.get(0);
        // add uncloaked stacks first
        for (int i=0;i<ShipDesignLab.MAX_DESIGNS;i++) {
            if (fl.num(i) > 0) {
                ShipDesign d = fl.empire().shipLab().design(i);
                counts.put(d, fl.num(i));
                if (!d.allowsCloaking())
                    map.put(d, fl.num(i));
            }
        }
        // add cloaked stacks last
        for (int i=0;i<ShipDesignLab.MAX_DESIGNS;i++) {
            if (fl.num(i) > 0) {
                ShipDesign d = fl.empire().shipLab().design(i);
                if (d.allowsCloaking())
                    map.put(d, fl.num(i));
            }
        }
    }
    private void drawMapBuffer(Graphics g) {
        g.drawImage(screenBuffer(),0,0,null);
        drawOverlay(g);
    }
    private void paintShipsToImage(Graphics2D g, int x, int y, int w, int h, int hoveringX, int hoveringY) {
        CombatStack currStack = mgr.currentStack();
        
        // unless we are in auto-complete mode, color grid squares blue that are in reach
        if ((!mgr.performingStackTurn) && (mode != Display.RESULT)) {
            g.setColor(new Color(0, 0, 255, 64));
            if (!currStack.usingAI()) {
                for (int y0 = 0; y0 < GRID_COUNT_Y; y0++) {
                    for (int x0 = 0; x0 < GRID_COUNT_X; x0++) {
                        if (mgr.canTacticallyMoveTo(currStack, x0, y0)) 
                                g.fill(combatGrids[x0][y0]);
                    }
                }
            }
        }
        
        if ((currentGrid != null) && !mgr.performingStackTurn && (mode == Display.INTRO)) {
            g.setColor(new Color(255,255,255,64));
            g.fill(currentGrid);
        }

        // draw grid bars
        Stroke prev = g.getStroke();
        g.setStroke(stroke1);
        g.setColor(Color.darkGray);
        for (int i=1;i<GRID_COUNT_Y;i++)
            g.drawLine(x,ptY[i],x+w,ptY[i]);
        for (int i=1;i<GRID_COUNT_X;i++)
            g.drawLine(ptX[i],y,ptX[i],y+h);
        g.setStroke(prev);

        // draw travel path
        paintTravelPathToImage(g, currStack, hoveringX, hoveringY);

        // draw ship missiles
        List<CombatStack> stacks = new ArrayList<>(mgr.activeStacks());
        for (CombatStack st: mgr.activeStacks())
            stacks.addAll(st.missiles());

        // draw ship stacks
        showPlanet = false;
        for (CombatStack stack: stacks) {
            if (stack.visible) {
                Rectangle rect = combatGrids[stack.x][stack.y];
                drawStack(g, stack, stackX(stack), stackY(stack), rect.width, rect.height);
            }
        }

        // draw any overlying messages
        if (mode != Display.INTRO)
            drawResults(g, 0, 0, w, h);
        if (exited)
            drawNextTurnNotice(g);
    }
    private void paintStackActions(Graphics2D g, int  hoveringX, int hoveringY) {
        if (mgr.performingStackTurn)
            return;
        if (currentGrid == null)
            return;

        shipActionButtons.clear();
        CombatStack currentStack = mgr.currentStack();
        if (currentStack.usingAI() || currentStack.inStasis)
            return;

        CombatStack targetStack = mgr.stackAt(hoveringX, hoveringY);
        
        // build list of action buttons
        if (targetStack == null) {
            shipMoveButton.reset();
            shipTeleportButton.reset();
            if (mgr.canTacticallyMoveTo(currentStack, hoveringX, hoveringY)) {
                if (shipTravelPath != null) 
                    shipMoveButton.setData(currentStack, hoveringX, hoveringY);
            }
            if (mgr.canTeleportTo(currentStack, hoveringX, hoveringY)) 
                shipTeleportButton.setData(currentStack, hoveringX, hoveringY);
            int buttons = shipActionButtons.size();
            for (int i=0;i<buttons;i++)
                shipActionButtons.get(i).draw(g, i+1, buttons);
        }
        else if (targetStack == currentStack) {
            shipDoneButton.setData(currentStack, hoveringX, hoveringY);
            if (targetStack.canRetreat()) 
                shipRetreatButton.setData(currentStack, hoveringX, hoveringY);
        }
        else if (targetStack.inStasis) {
            
        }
        else if (targetStack.isMonster() || (targetStack.empire != currentStack.empire)) {
            int wpnI = 0;
            int spcI = 0;
            shipFireAllButton.reset();
            if ((currentStack.numWeapons() > 0) && !currentStack.isColony())
                shipFireAllButton.setData(targetStack, hoveringX, hoveringY);
            for (int i=0;i<currentStack.numWeapons();i++) {
                ShipComponent wpn = currentStack.weapon(i);
                if (wpn.isSpecial()) {
                    shipSpecialButton[spcI].setData(targetStack, wpnI, hoveringX, hoveringY);
                    spcI++;
                    wpnI++;
                }
                else {
                    shipWeaponButton[wpnI].setData(targetStack, wpnI, hoveringX, hoveringY);
                    wpnI++;
                }
            }
        }

        if (targetStack != null) {
            if (targetStack.isColony())
                drawColonyButtonOverlay(g, (CombatStackColony) targetStack, shipActionButtons);
            else if (targetStack.isMonster())
                drawMonsterButtonOverlay(g, targetStack, shipActionButtons);
            else
                drawShipButtonOverlay(g, targetStack, shipActionButtons);
        }
    }
    private void drawColonyButtonOverlay(Graphics2D g, CombatStackColony target, List<ShipActionButton> actions) {
        CombatStack currentStack = mgr.currentStack();
        
        Rectangle grid = combatGrids[target.x][target.y];
       
        List<Rectangle> quickButtons = new ArrayList<>();
        for (int i=0;i<actions.size();i++) {
            int gridW = grid.width;
            int gridH = grid.height;
            int buttonW = gridW*9/20;
            int buttonX = i%2==0 ? grid.x+(gridW/20) : grid.x+(gridW*10/20);
            int buttonH = (gridH-s21)/4;
            int buttonY = grid.y+s6+(((i/2)%4)*(buttonH+s3));
            quickButtons.add(new Rectangle(buttonX,buttonY,buttonW, buttonH));
        }
        
        boolean friendly = target.empire.isPlayer();
        boolean drawUpper = target.y > 3;
        boolean drawLeft = target.reversed ? target.x > 6 : target.x > 2;
        int buttonRows = (actions.size() + 1) / 2;
        int buttonH = s25;
        int buttonGap = s5;
        int overlayHeaderH = actions.isEmpty() ? s10 : s30;
        int overlayButtonH = buttonRows * (buttonH + buttonGap);
        int overlayFooterH = 0;
        int overlayScanH = 0;

        int numWeapons = target.numWeapons();
        int numSpecials = 1;  // Battle Scanner
        if (target.empire.tech().subspaceInterdiction())
            numSpecials++;

        overlayScanH = scaled(105); // space for title and combat stats
        overlayScanH += s8;
        overlayScanH += (numWeapons * s13);
        overlayScanH += s8;
        overlayScanH += (numSpecials * s13);

        int w = grid.width + grid.width + s50;
        int h = overlayHeaderH + overlayButtonH + overlayScanH + overlayFooterH;
        int y = drawUpper ? grid.y + grid.height - h - (grid.height / 5) : grid.y + (grid.height / 5);
        int x = drawLeft ? grid.x - w : grid.x + grid.width;
        shipButtonOverlay.setBounds(x, y, w, h);

        Color borderColor = friendlyBorderC;
        if (target.empire != currentStack.empire)
            borderColor = hostileBorderC;
        else if (target == currentStack)
            borderColor = currentBorderC;

        g.setColor(grayBackC);
        g.fill(shipButtonOverlay);
        if (target.empire != currentStack.empire)
            g.setColor(borderColor);
        else
            g.setColor(borderColor);
        Stroke prev = g.getStroke();
        g.setStroke(stroke7);
        g.setClip(shipButtonOverlay);
        g.draw(shipButtonOverlay);
        g.setClip(null);
        g.setStroke(stroke2);
        g.draw(combatGrids[target.x][target.y]);
        g.setStroke(prev);


        int x0 = x + s10;
        int y0 = y + s20;
        String titleText;
        if (target == currentStack)
            titleText = text("SHIP_COMBAT_ALLY_COLONY");
        else if (target.empire == currentStack.empire)
            titleText = text("SHIP_COMBAT_ALLY_COLONY");
        else
            titleText = text("SHIP_COMBAT_ENEMY_COLONY");
        g.setFont(narrowFont(14));
        g.setColor(SystemPanel.blackText);
        g.drawString(titleText, x0, y0);

        int y1 = y0 + s20;
        if (!actions.isEmpty()) {
            String line = friendly ? text("SHIP_COMBAT_SELECT_ACTION") : text("SHIP_COMBAT_SELECT_WEAPON");
            g.setFont(narrowFont(20));
            drawShadowedString(g, line, 3, x0, y1, SystemPanel.textShadowC, SystemPanel.whiteText);
            y1 += s10;
            for (int i = 0; i < actions.size(); i++) {
                boolean isLeftColumn = i % 2 == 0;
                int x1 = isLeftColumn ? x + s10 : x + (w / 2);
                int w1 = (w - s20 - s5) / 2;
                actions.get(i).draw(g, currentStack, target, isLeftColumn, x1, y1, w1, buttonH, actions.size(), quickButtons.get(i));
                if (i % 2 == 1)
                    y1 = y1 + buttonH + buttonGap;
            }
        }

        int x2 = x + s10;
        int y2 = y0 + overlayHeaderH + overlayButtonH;
        g.setColor(borderColor);
        g.fillRect(x, y2, w, s4);

        y2 += s25;
        String scanTitle = friendly ? text("SHIP_COMBAT_COLONY_LABEL") : text("SHIP_COMBAT_COLONY_SCAN_LABEL");
        g.setFont(narrowFont(20));
        drawShadowedString(g, scanTitle, 3, x2, y2, SystemPanel.textShadowC, SystemPanel.whiteText);

        y2 += s10;
        int y2a = y2;
        int x1 = x + s4;
        int w1 = w - s8;
        int x1a = x1 + s4;
        int x1b = x1 + (w1 / 2) + s8;
        Color textColor = SystemPanel.blackText;
        g.setColor(lineColor);
        g.fillRect(x1, y2, w1, s1);

        g.setFont(narrowFont(12));
        String lbl1 = text("SHIP_COMBAT_COLONY_SHIELD");
        String lbl2 = text("SHIP_COMBAT_COLONY_BASES");
        String val1 = str((int)target.shieldLevel());
        String val2 = str(target.num);
        int sw1 = g.getFontMetrics().stringWidth(val1);
        int sw2 = g.getFontMetrics().stringWidth(val2);
        g.setColor(textColor);
        g.drawString(lbl1, x1a, y2 + s12);
        g.drawString(val1, x1b - s10 - sw1, y2 + s12);
        g.drawString(lbl2, x1b, y2 + s12);
        g.drawString(val2, x1 + w1 - sw2 - s5, y2 + s12);

        y2 += s15;
        g.setColor(lineColor);
        g.fillRect(x1, y2, w1, s1);

        lbl1 = text("SHIP_COMBAT_COLONY_POPULATION");
        lbl2 = text("SHIP_COMBAT_COLONY_BASE_HITS");
        g.setFont(narrowFont(12));
        g.setColor(textColor);
        g.drawString(lbl1, x1a, y2 + s12);
        g.drawString(lbl2, x1b, y2 + s12);
        val1 = str((int) target.colony.population());
        val2 = str((int) target.maxHits);
        sw1 = g.getFontMetrics().stringWidth(val1);
        sw2 = g.getFontMetrics().stringWidth(val2);
        g.setColor(textColor);
        g.drawString(lbl1, x1a, y2 + s12);
        g.drawString(val1, x1b - s10 - sw1, y2 + s12);
        g.drawString(lbl2, x1b, y2 + s12);
        g.drawString(val2, x1 + w1 - sw2 - s5, y2 + s12);

        y2 += s15;
        g.setColor(lineColor);
        g.fillRect(x1, y2, w1, s1);

        lbl1 = text("SHIP_COMBAT_COLONY_FACTORIES");
        lbl2 = text("SHIP_COMBAT_COLONY_BASE_ATTACK");
        g.setFont(narrowFont(12));
        g.setColor(textColor);
        g.drawString(lbl1, x1a, y2 + s12);
        g.drawString(lbl2, x1b, y2 + s12);
        val1 = str((int) target.colony.industry().factories());
        val2 = str((int) target.attackLevel());
        sw1 = g.getFontMetrics().stringWidth(val1);
        sw2 = g.getFontMetrics().stringWidth(val2);
        g.setColor(textColor);
        g.drawString(lbl1, x1a, y2 + s12);
        g.drawString(val1, x1b - s10 - sw1, y2 + s12);
        g.drawString(lbl2, x1b, y2 + s12);
        g.drawString(val2, x1 + w1 - sw2 - s5, y2 + s12);

        y2 += s15;
        g.setColor(lineColor);
        g.fillRect(x1, y2, w1, s1);

        g.setColor(lineColor);
        g.fillRect(x1 + (w / 2), y2a, s1, y2 - y2a);

        // weapon - missile type for bases
        if (numWeapons > 0) {
            y2 += s3;
            lbl1 = text("SHIP_COMBAT_SCAN_WEAPONS");
            g.setFont(narrowFont(12));
            g.setColor(textColor);
            g.drawString(lbl1, x1a, y2 + s12);
            for (int i=0;i<numWeapons;i++) {
                val2 = target.wpnName(i);
                sw2 = g.getFontMetrics().stringWidth(val2);
                g.drawString(val2, x1 + w1 - sw2 - s5, y2 + s12);
                y2 += s13;
            }
            y2 += s5;
            g.setColor(lineColor);
            g.fillRect(x1, y2, w1, s1);
        }

        if (numSpecials > 0) {
            y2 += s3;
            lbl1 = text("SHIP_COMBAT_SCAN_DEVICES");
            g.setFont(narrowFont(12));
            g.setColor(textColor);
            g.drawString(lbl1, x1a,y2+s12);
            val2 = target.empire.shipLab().specialBattleScanner().name();
            sw2 = g.getFontMetrics().stringWidth(val2);
            g.drawString(val2, x1+w1-sw2-s5, y2+s12);
            y2 += s13;
            if (target.empire.tech().subspaceInterdiction()) {
                val2 = target.empire.tech().topSubspaceInterdictorTech().name();
                sw2 = g.getFontMetrics().stringWidth(val2);
                g.drawString(val2, x1 + w1 - sw2 - s5, y2 + s12);
                y2 += s13;
            }
            y2 += s5;
            g.setColor(lineColor);
            g.fillRect(x1,y2,w1,s1);
        }
    }
    private void drawShipButtonOverlay(Graphics2D g, CombatStack target, List<ShipActionButton> actions) {
        CombatStack currentStack = mgr.currentStack();
        
        if (!currentStack.empire.isPlayer())
            actions.clear();
         
        Rectangle grid = combatGrids[target.x][target.y];
        int gridW = grid.width;
        int gridH = grid.height;
        int buttonW = gridW*9/20;
        int buttonH = (gridH-s21)/4;
       
        List<Rectangle> quickButtons = new ArrayList<>();
        for (int i=0;i<actions.size();i++) {
            int buttonX = i%2==0 ? grid.x+(gridW/20) : grid.x+(gridW*10/20);
            int buttonY = grid.y+s6+(((i/2)%4)*(buttonH+s3));
            quickButtons.add(new Rectangle(buttonX,buttonY,buttonW, buttonH));
        }

        boolean friendly = currentStack.empire == target.empire;
        boolean drawUpper = target.y > 5;
        boolean drawLeft = target.reversed ? target.x > 6 : target.x > 2;
        int buttonRows = (actions.size()+1)/2;
        buttonH = s25;
        int buttonGap = s5;
        int overlayHeaderH = actions.isEmpty() ? s10 : s30;
        int overlayButtonH = buttonRows*(buttonH+buttonGap);
        int overlayFooterH = 0;
        int overlayScanH = 0;

        ShipView view = player().shipViewFor(target.design());
        if (view != null) {
            overlayScanH = s100+s5; // space for title and combat stats
            if (!view.weapons().isEmpty()) {
                overlayScanH += s8;
                overlayScanH += (view.weapons().size() * s13);
            }
            if (!view.specials().isEmpty()) {
                overlayScanH += s8;
                overlayScanH += (view.specials().size() * s13);
            }
        }

        int w = grid.width+grid.width+s100+s20;
        int h = overlayHeaderH+overlayButtonH+overlayScanH+overlayFooterH;
        int y = drawUpper ? grid.y + grid.height - h - (grid.height/5) : grid.y + (grid.height/5);
        int x =  drawLeft ? grid.x - w : grid.x + grid.width;

        Color borderColor = friendlyBorderC;
        if (target.empire != currentStack.empire)
            borderColor = hostileBorderC;
        else if (target == currentStack)
            borderColor = currentBorderC;

        Rectangle shipOverlay = new Rectangle(x,y,w,h);
        g.setColor(grayBackC);
        g.fill(shipOverlay);
        if (target.empire != currentStack.empire)
            g.setColor(borderColor);
        else
            g.setColor(borderColor);
        Stroke prev = g.getStroke();
        g.setStroke(stroke7);
        g.setClip(shipOverlay);
        g.draw(shipOverlay);
        g.setClip(null);
        g.setStroke(stroke2);
        Rectangle r = combatGrids[target.x][target.y];
        g.drawRect(r.x+s1,r.y+s1, r.width-s1,r.height-s1);
        g.setStroke(prev);


        int x0 = x+s10;
        int y0 = drawUpper? y+overlayScanH: y+s20;

        // draw ship info and action buttons
        String titleText;
        if (target == currentStack)
            titleText = text("SHIP_COMBAT_CURRENT_TARGET");
        else if (target.empire == currentStack.empire)
            titleText = text("SHIP_COMBAT_ALLY_TARGET");
        else
            titleText = text("SHIP_COMBAT_ENEMY_TARGET");
        g.setFont(narrowFont(14));
        g.setColor(SystemPanel.blackText);
        g.drawString(titleText, x0, y0);

        int y1 = y0+s20;
        if (!actions.isEmpty()) {
            String line;
            if (friendly)
                line = text("SHIP_COMBAT_SELECT_ACTION");
            else if (actions.size() == 1)
                line = text("SHIP_COMBAT_FIRE_WEAPON");
            else
                line = text("SHIP_COMBAT_SELECT_WEAPON");
            g.setFont(narrowFont(20));
            drawShadowedString(g,line,3, x0,y1, SystemPanel.textShadowC, SystemPanel.whiteText);
            y1 += s10;
            for (int i=0;i<actions.size();i++) {
                boolean isLeftColumn = i%2 == 0;
                int x1 = isLeftColumn ? x+s10 : x+(w/2);
                int w1 = (w-s20-s5)/2;
                actions.get(i).draw(g,currentStack, target, isLeftColumn, x1, y1, w1, buttonH, actions.size(), quickButtons.get(i));
                if (i%2 == 1)
                    y1 = y1+buttonH+buttonGap;
            }
        }

        shipButtonOverlay.setBounds(x,y0-s20,w,overlayHeaderH+overlayButtonH+s20);

        if (view == null)
            return;

        int x2 = x+s10;
        int y2 = drawUpper ? y+s25 : y+s20+overlayHeaderH+overlayButtonH;

        // draw scan info

        if (!drawUpper) {
            g.setColor(borderColor);
            g.fillRect(x, y2, w, s4);
            y2 += s25;
        }
        String scanTitle = friendly ? text("SHIP_COMBAT_INFO_LABEL") : text("SHIP_COMBAT_SCAN_LABEL");
        g.setFont(narrowFont(20));
        drawShadowedString(g, scanTitle, 3, x2, y2, SystemPanel.textShadowC, SystemPanel.whiteText);

        y2 += s10;
        int y2a = y2;
        int x1 = x+s4;
        int w1 = w-s8;
        int x1a = x1+s4;
        int x1b = x1 + (w1/2)+s8;
        Color textColor = SystemPanel.blackText;
        g.setColor(lineColor);
        g.fillRect(x1,y2,w1,s1);

        String unk = text("SHIP_COMBAT_SCAN_UNSCANNED_VALUE");

        g.setFont(narrowFont(12));
        String lbl1 = text("SHIP_COMBAT_SCAN_HIT_POINTS");
        String lbl2 = text("SHIP_COMBAT_SCAN_SHIELD_CLASS");
        int currHits = (int) Math.ceil(target.hits);
        int maxHits = (int) Math.ceil(target.maxHits);
        String val1 = currHits == maxHits ? "" + maxHits : ""+currHits+"/"+maxHits;
        String val2 = view.shieldKnown() ? ""+target.shieldLevel() : unk;
        int sw1 = g.getFontMetrics().stringWidth(val1);
        int sw2 = g.getFontMetrics().stringWidth(val2);
        g.setColor(textColor);
        g.drawString(lbl1, x1a,y2+s12);
        g.drawString(val1, x1b-s10-sw1, y2+s12);
        g.drawString(lbl2, x1b,y2+s12);
        g.drawString(val2, x1+w1-sw2-s5, y2+s12);

        y2 += s15;
        g.setColor(lineColor);
        g.fillRect(x1,y2,w1,s1);

        lbl1 = text("SHIP_COMBAT_SCAN_MISSILE_DEF");
        lbl2 = text("SHIP_COMBAT_SCAN_ATTACK_LEVEL");
        g.setFont(narrowFont(12));
        g.setColor(textColor);
        g.drawString(lbl1, x1a,y2+s12);
        g.drawString(lbl2, x1b,y2+s12);
        val1 = view.missileDefenseKnown() ? "" +target.missileDefense() : unk;
        val2 = view.attackLevelKnown() ? ""+target.attackLevel() : unk;
        sw1 = g.getFontMetrics().stringWidth(val1);
        sw2 = g.getFontMetrics().stringWidth(val2);
        g.setColor(textColor);
        g.drawString(lbl1, x1a,y2+s12);
        g.drawString(val1, x1b-s10-sw1, y2+s12);
        g.drawString(lbl2, x1b,y2+s12);
        g.drawString(val2, x1+w1-sw2-s5, y2+s12);

        y2 += s15;
        g.setColor(lineColor);
        g.fillRect(x1,y2,w1,s1);

        lbl1 = text("SHIP_COMBAT_SCAN_BEAM_DEF");
        lbl2 = text("SHIP_COMBAT_SCAN_SPEED");
        g.setFont(narrowFont(12));
        g.setColor(textColor);
        g.drawString(lbl1, x1a,y2+s12);
        g.drawString(lbl2, x1b,y2+s12);
        val1 = view.beamDefenseKnown() ? ""+target.beamDefense() : unk;
        val2 = view.combatSpeedKnown() ? ""+target.maxMove() : unk;
        sw1 = g.getFontMetrics().stringWidth(val1);
        sw2 = g.getFontMetrics().stringWidth(val2);
        g.setColor(textColor);
        g.drawString(lbl1, x1a,y2+s12);
        g.drawString(val1, x1b-s10-sw1, y2+s12);
        g.drawString(lbl2, x1b,y2+s12);
        g.drawString(val2, x1+w1-sw2-s5, y2+s12);

        y2 += s15;
        g.setColor(lineColor);
        g.fillRect(x1,y2,w1,s1);

        g.setColor(lineColor);
        g.fillRect(x1+(w/2),y2a,s1,y2-y2a);

        if (!view.weapons().isEmpty()) {
            y2 += s3;
            lbl1 = text("SHIP_COMBAT_SCAN_WEAPONS");
            g.setFont(narrowFont(12));
            g.setColor(textColor);
            g.drawString(lbl1, x1a,y2+s12);
            List<ShipWeapon> wpns = view.weapons();
            for (int i=0; i<wpns.size(); i++) {
                ShipWeapon wpn = wpns.get(i);
                val2 = text("SHIP_COMBAT_SCAN_WEAPON_CNT", str(view.wpnCount(i)), wpn.name());
                sw2 = g.getFontMetrics().stringWidth(val2);
                g.drawString(val2, x1+w1-sw2-s5, y2+s12);
                y2 += s13;
            }
            y2 += s5;
            g.setColor(lineColor);
            g.fillRect(x1,y2,w1,s1);
        }

        if (!view.specials().isEmpty()) {
            y2 += s3;
            lbl1 = text("SHIP_COMBAT_SCAN_DEVICES");
            g.setFont(narrowFont(12));
            g.setColor(textColor);
            g.drawString(lbl1, x1a,y2+s12);
            List<ShipSpecial> specials = view.specials();
            for (int i=0; i<specials.size(); i++) {
                ShipSpecial spec = specials.get(i);
                val2 = spec.name();
                sw2 = g.getFontMetrics().stringWidth(val2);
                g.drawString(val2, x1+w1-sw2-s5, y2+s12);
                y2 += s13;
            }
            y2 += s5;
            g.setColor(lineColor);
            g.fillRect(x1,y2,w1,s1);
        }

        // draw dividing line
        if (drawUpper) {
            g.setColor(borderColor);
            g.fillRect(x, y2+s5, w, s4);
        }
    }
    private void drawMonsterButtonOverlay(Graphics2D g, CombatStack target, List<ShipActionButton> actions) {
        CombatStack currentStack = mgr.currentStack();
        
        if (currentStack.usingAI())
            actions.clear();
         
        Rectangle grid = combatGrids[target.x][target.y];
       
        List<Rectangle> quickButtons = new ArrayList<>();
        for (int i=0;i<actions.size();i++) {
            int gridW = grid.width;
            int gridH = grid.height;
            int buttonW = gridW*9/20;
            int buttonX = i%2==0 ? grid.x+(gridW/20) : grid.x+(gridW*10/20);
            int buttonH = (gridH-s21)/4;
            int buttonY = grid.y+s6+(((i/2)%4)*(buttonH+s3));
            quickButtons.add(new Rectangle(buttonX,buttonY,buttonW, buttonH));
        }

        boolean drawUpper = target.y > 5;
        boolean drawLeft = target.reversed ? target.x > 6 : target.x > 2;
        int buttonRows = (actions.size()+1)/2;
        int buttonH = s25;
        int buttonGap = s5;
        int overlayHeaderH = actions.isEmpty() ? s10 : s30;
        int overlayButtonH = buttonRows*(buttonH+buttonGap);
        int overlayFooterH = 0;
        int overlayScanH = s76;

        int w = grid.width+grid.width+s100+s20;
        int h = overlayHeaderH+overlayButtonH+overlayScanH+overlayFooterH;
        int y = drawUpper ? grid.y + grid.height - h - (grid.height/5) : grid.y + (grid.height/5);
        int x =  drawLeft ? grid.x - w : grid.x + grid.width;

        Color borderColor = hostileBorderC;

        Rectangle shipOverlay = new Rectangle(x,y,w,h);
        g.setColor(grayBackC);
        g.fill(shipOverlay);
        g.setColor(borderColor);
        Stroke prev = g.getStroke();
        g.setStroke(stroke7);
        g.setClip(shipOverlay);
        g.draw(shipOverlay);
        g.setClip(null);
        g.setStroke(stroke2);
        Rectangle r = combatGrids[target.x][target.y];
        g.drawRect(r.x+s1,r.y+s1, r.width-s1,r.height-s1);
        g.setStroke(prev);


        int x0 = x+s10;
        int y0 = drawUpper? y+overlayScanH: y+s20;

        // draw ship info and action buttons
        String titleText = text("SHIP_COMBAT_MONSTER_TARGET");
        g.setFont(narrowFont(14));
        g.setColor(SystemPanel.blackText);
        g.drawString(titleText, x0, y0);

        int y1 = y0+s20;
        if (!actions.isEmpty()) {
            String line;
            if (actions.size() == 1)
                line = text("SHIP_COMBAT_FIRE_WEAPON");
            else
                line = text("SHIP_COMBAT_SELECT_WEAPON");
            g.setFont(narrowFont(20));
            drawShadowedString(g,line,3, x0,y1, SystemPanel.textShadowC, SystemPanel.whiteText);
            y1 += s10;
            for (int i=0;i<actions.size();i++) {
                boolean isLeftColumn = i%2 == 0;
                int x1 = isLeftColumn ? x+s10 : x+(w/2);
                int w1 = (w-s20-s5)/2;
                actions.get(i).draw(g,currentStack, target, isLeftColumn, x1, y1, w1, buttonH, actions.size(), quickButtons.get(i));
                if (i%2 == 1)
                    y1 = y1+buttonH+buttonGap;
            }
        }

        shipButtonOverlay.setBounds(x,y0-s20,w,overlayHeaderH+overlayButtonH+s20);
        int x2 = x+s10;
        int y2 = drawUpper ? y+s25 : y+s20+overlayHeaderH+overlayButtonH;

        // draw scan info

        if (!drawUpper) {
            g.setColor(borderColor);
            g.fillRect(x, y2, w, s4);
            y2 += s25;
        }
        String scanTitle = text("SHIP_COMBAT_SCAN_LABEL");
        g.setFont(narrowFont(20));
        drawShadowedString(g, scanTitle, 3, x2, y2, SystemPanel.textShadowC, SystemPanel.whiteText);

        y2 += s10;
        int y2a = y2;
        int x1 = x+s4;
        int w1 = w-s8;
        int x1a = x1+s4;
        int x1b = x1 + (w1/2)+s8;
        Color textColor = SystemPanel.blackText;
        g.setColor(lineColor);
        g.fillRect(x1,y2,w1,s1);

        String unk = text("SHIP_COMBAT_SCAN_UNSCANNED_VALUE");

        g.setFont(narrowFont(12));
        String lbl1 = text("SHIP_COMBAT_SCAN_HIT_POINTS");
        String lbl2 = text("SHIP_COMBAT_SCAN_SPEED");
        int currHits = (int) Math.ceil(target.hits);
        int maxHits = (int) target.maxHits;
        String val1 = currHits == maxHits ? "" + maxHits : ""+currHits+"/"+maxHits;
        String val2 = target.maxMove == 0 ? unk : str((int)target.maxMove);
        int sw1 = g.getFontMetrics().stringWidth(val1);
        int sw2 = g.getFontMetrics().stringWidth(val2);
        g.setColor(textColor);
        g.drawString(lbl1, x1a,y2+s12);
        g.drawString(val1, x1b-s10-sw1, y2+s12);
        g.drawString(lbl2, x1b,y2+s12);
        g.drawString(val2, x1+w1-sw2-s5, y2+s12);

        y2 += s15;
        g.setColor(lineColor);
        g.fillRect(x1,y2,w1,s1);

    }
    private void paintTravelPathToImage(Graphics2D g, CombatStack stack, int hoveringX, int hoveringY) {
        if (mgr.autoComplete || mgr.performingStackTurn)
            return;
        if (mode == Display.RESULT)
            return;
        if (stack.usingAI())
            return;
        if (!mgr.canTacticallyMoveTo(stack, hoveringX, hoveringY))
            return;

        shipTravelPath = stack.pathTo(hoveringX, hoveringY);
        if (shipTravelPath == null)
            return;

        Stroke prev = g.getStroke();
        g.setStroke(stroke2);
        g.setColor(Color.green);

        Rectangle rect = combatGrids[stack.x][stack.y];
        int prevX = rect.x + (rect.width/2);
        int prevY = rect.y + (rect.height /2);

        for (int i=0;i<shipTravelPath.size();i++) {
            int gridX = shipTravelPath.mapX(i);
            int gridY = shipTravelPath.mapY(i);
            rect = combatGrids[gridX][gridY];
            int ctrX = rect.x + (rect.width/2);
            int ctrY = rect.y + (rect.height /2);
            g.drawLine(prevX, prevY, ctrX, ctrY);
            prevX = ctrX;
            prevY = ctrY;
        }
        g.setStroke(prev);
    }
    public int stackX(CombatStack st)   {
        Rectangle rect = combatGrids[st.x][st.y];
        return (int) (rect.x+ (st.offsetX*rect.width));
    }
    public int stackY(CombatStack st)   {
        Rectangle rect = combatGrids[st.x][st.y];
        return (int) (rect.y+ (st.offsetY*rect.height));
    }
    public int stackW()                 { return boxW; }
    public int stackH()                 { return boxH; }

    private void paintMenuBarToImage(Graphics2D g, int x, int y, int w, int h) {
        if (menuBackC == null) {
            Color transP = new Color(0,0,0,0);
            // always create background for create button since config changes to change button color
            float[] dist = {0.0f,  1.0f};
            Point2D ptStart = new Point2D.Float(x, y);
            Point2D ptEnd = new Point2D.Float(x, y+h-s5);
            Color[] colors = {transP, grayBackC };
            menuBackC = new LinearGradientPaint(ptStart, ptEnd, dist, colors);
        }
        g.setPaint(menuBackC);
        g.fillRect(x+s5, y, w-s10, h-s5);

        int buttW = scaled(220);
        int buttH = barH-s15;
        int buttY = y+h-s10-buttH;

        boolean performingTurn = mgr.performingStackTurn;
        boolean autoCompleting = mgr.autoComplete || mgr.autoResolve;
        if (mgr.combatIsFinished()) {
            nextBox.setBounds(0,0,0,0);
            String exitText = text("SHIP_COMBAT_EXIT");
            int buttX = x+w-buttW-s10;
            drawButton(g, exitBackC, exitText, exitBox, redButtonEdgeC, redButtonCenterC, buttX, buttY, buttW, buttH, false);
        }
        else {
            exitBox.setBounds(0,0,0,0);
            String resolveText = mgr.autoResolve? text("SHIP_COMBAT_AUTO_RESOLVING") : text("SHIP_COMBAT_AUTO_RESOLVE");
            int buttX = x + s10;
            drawButton(g, resolveButtonBackC, resolveText, resolveBox, redButtonEdgeC, redButtonCenterC, buttX, buttY, buttW, buttH, performingTurn);


            if (!mgr.autoResolve) {
                String retreatText = text("SHIP_COMBAT_RETREAT_ALL");
                buttX = buttX+s10+buttW;
                drawButton(g, retreatButtonBackC, retreatText, retreatBox, redButtonEdgeC, redButtonCenterC, buttX, buttY, buttW, buttH, performingTurn);

                String nextText = text("SHIP_COMBAT_NEXT");
                buttX = x + w - buttW - s10;
                drawButton(g, nextShipButtonBackC, nextText, nextBox, greenButtonEdgeC, greenButtonCenterC, buttX, buttY, buttW, buttH, performingTurn || autoCompleting);

                buttX = buttX - buttW - s10;
                if (mgr.autoComplete) {
                    String autoText = text("SHIP_COMBAT_PAUSE_BATTLE");
                    drawButton(g, playPauseBackC, autoText, playPauseBox, redButtonEdgeC, redButtonCenterC, buttX, buttY, buttW, buttH, false);
                } else {
                    String autoText = text("SHIP_COMBAT_PLAY_BATTLE");
                    drawButton(g, playPauseBackC, autoText, playPauseBox, greenButtonEdgeC, greenButtonCenterC, buttX, buttY, buttW, buttH, performingTurn);
                }
            }
        }
    }
    public void drawButton(Graphics2D g, LinearGradientPaint backC, String label, Rectangle bounds, Color edgeC, Color midC, int x, int y, int w, int h, boolean shouldBeGray) {
        bounds.setBounds(x,y,w,h);
        if (backC == null) {
            float[] dist = {0.0f, 0.5f, 1.0f};
            Point2D ptStart = new Point2D.Float(x, y);
            Point2D ptEnd = new Point2D.Float(x+w, y);
            Color[] colors = {edgeC, midC, edgeC};
            backC = new LinearGradientPaint(ptStart, ptEnd, dist, colors);
        }

        Stroke prev = g.getStroke();
        if (shouldBeGray)
            g.setColor(Color.gray);
        else
            g.setPaint(backC);
        g.fillRoundRect(x, y, w, h, s5, s5);
        Color c0 = SystemPanel.whiteText;
        if ((hoverBox == bounds) && !shouldBeGray)
            c0 = Color.yellow;
        g.setColor(c0);
        g.setStroke(stroke2);
        g.drawRoundRect(x, y, w, h, s5, s5);
        g.setStroke(prev);

        g.setFont(narrowFont(18));
        int labelW = g.getFontMetrics().stringWidth(label);
        int x1 = x+((w-labelW)/2);
        int y1 = y+h-s10;
        drawShadowedString(g, label, 3, x1, y1, SystemPanel.textShadowC, c0);
    }
    private void drawStack(Graphics2D g, CombatStack st, int x, int y, int w, int h) {
        if (st.isShip()) {
            CombatStackShip sh = (CombatStackShip) st;
            ShipDesign d = sh.design;
            int count = counts.containsKey(d) ? counts.get(d) : 0;
            if (count > 0)
                sh.drawStack(this, g, count, x, y, w, h);
        }
        else if (st.isMissile()) {
            CombatStackMissile sh = (CombatStackMissile) st;
            sh.drawStack(this, g, 0, x, y, w, h);
        }
        else if (st.isMonster()) {
            st.drawStack(this, g, 0, x, y, w, h);
        }
        else if (st.isColony()) {
            showPlanet = true;
            planetX = x+((w-h)/2);
            planetY = y+(h/12);
            planetR = h*5/6;
            planetBox.setBounds(x,y,w,h);
            if (mgr.currentStack().isColony() && !mgr.performingStackTurn) {
                Stroke prev = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(currentBorderC);
                g.drawRect(x+s1,y+s1,w-s2,h-s2);
                g.setStroke(prev);
            }
            drawPlanet(g, (CombatStackColony) st, x, y, w, h);
        }
    }
    private void drawPlanet(Graphics2D g, CombatStackColony st, int x, int y, int w, int h) {
        if (renderedPlanetImage != null) {
            planetDrawn = true;
            int imgW = renderedPlanetImage.getWidth();
            int imgH = renderedPlanetImage.getHeight();
            g.drawImage(renderedPlanetImage, planetX, planetY, planetX+planetR, planetY+planetR, 0, 0, imgW, imgH, null);
        }
        int y0 = y+h-s5;
        g.setFont(narrowFont(18));
        g.setColor(Color.white);
        int sysId = st.colony.starSystem().id;
        if (player().sv.isScouted(sysId)) {
            String sname = player().sv.name(sysId);
            if (sname == null)
                sname = "";    
            String dispName = st.num > 0 ? ""+st.num+"  " + sname : sname;
            int sw = g.getFontMetrics().stringWidth(dispName);
            int x0 = x+(w-sw)/2;
            g.drawString(dispName, x0, y0);
        }
    }
    private void rotateAndRenderPlanet() {
        if (!showPlanet)
            return;
        if (drawingPlanet)
            return;

        Runnable drawRunnable = () -> {
            if (!drawingPlanet) {
                drawingPlanet = true;
                mgr.system().planet().rotate(planetRotateSpeed);
                try {
                    renderedPlanetImage = mgr.system().planet().image(planetR, 135);
                }
                catch (Exception e) { }
                if (!mgr.playAnimations())
                    repaint();
                drawingPlanet = false;
            }
        };
        Thread thread = new Thread(drawRunnable);
        thread.start();
    }
    private void drawResults(Graphics g, int x, int y, int w, int h) {
        g.setFont(narrowFont(40));
        g.setColor(Color.lightGray);
        String sysName =  player().sv.name(mgr.system().id);
        String victorName = mgr.results().victorName();
        String prompt;
        if (sysName.isEmpty())
            prompt = text("SHIP_COMBAT_RESULTS_UNNAMED", victorName);
        else if (mgr.results().isMonsterVictory())
            prompt = text("SHIP_COMBAT_RESULTS_MONSTER", victorName, sysName);
        else 
            prompt = text("SHIP_COMBAT_RESULTS", victorName, sysName);
        int sw = g.getFontMetrics().stringWidth(prompt);

        int x0 = (w - sw) / 2;
        int y0 = s80;
        drawBorderedString(g, prompt, x0, y0, Color.black, Color.white);

        drawSkipText(g, true);
    }
    private void togglePlayPause() {
        if (mode != Display.INTRO)
            return;
        mgr.toggleAutoComplete();
        paintAllImmediately();
    }
    private void nextStack() {
        if (mode != Display.INTRO)
            return;
       
        mgr.continueToNextPlayerStack();
        paintAllImmediately();
        if (mgr.combatIsFinished())
            showResult();
        newTargetGridCell();
        
        if (mouseGridX >= 0)
            currentGrid = combatGrids[mouseGridX][mouseGridY];
        
        
        repaint();
    }
    private void moveMouseToCurrentStack() {
        if (robot == null) {
            log("robot == null");
            return;
        }
        
        CombatStack st = mgr.currentStack();
        mouseGridX = st.x;
        mouseGridY = st.y;
        currentGrid = combatGrids[mouseGridX][mouseGridY];
        Point p = MouseInfo.getPointerInfo().getLocation();
        Point frame = this.getLocationOnScreen();
        int destX = currentGrid.x+frame.x+s30;
        int destY = currentGrid.y+frame.y+s15;
        int currX = p.x-frame.x;
        int currY = p.y-frame.y;
        for (int i=0;i<=10;i++) {
            int x = currX+ ((destX-currX)*i/10);
            int y = currY+ ((destY-currY)*i/10);
            robot.mouseMove(x, y);
            mouseMovedTo(destX, destY);
            sleep(25);
        }
        paintCellImmediately(mouseGridX, mouseGridY);
    }
    public void showResult() {
        mode = Display.RESULT;
        repaint();
    }
    private void retreatStack(CombatStack stack) {
        if (!stack.canRetreat() || !stack.empire.isPlayer())
            return;
        StarSystem dest = player().ai().shipCaptain().retreatSystem(mgr.system());
        mgr.retreatStack((CombatStackShip)stack, dest);
        mgr.continueToNextPlayerStack();
    }
    public void finish() {
        if (mgr.combatIsFinished()) {
            renderedPlanetImage = null;
            mode = Display.RESULT;
            exited = true;
            repaint();
            session().resumeNextTurnProcessing();
        }
        else {
            mgr.showAnimations = false;
            mgr.resolveAllCombat();
            mode = Display.RESULT;
            repaint();
        }
    }
    private void newTargetGridCell() {
        shipMoveButton.reset();
        shipTeleportButton.reset();
        shipRetreatButton.reset();
        for (int i=0;i<shipWeaponButton.length;i++)
            shipWeaponButton[i].reset();
        for (int i=0;i<shipSpecialButton.length;i++)
            shipSpecialButton[i].reset();

        shipActionButtons.clear();
        shipButtonOverlay.setBounds(0,0,0,0);
        leftGreenActionBackC = null;
        leftGrayActionBackC = null;
        rightGreenActionBackC = null;
        rightGrayActionBackC = null;
        moveBackC = null;
    }
    @Override
    public void keyPressed(KeyEvent e) {
        if (stillFading())
            return;
        int k = e.getKeyCode();
        boolean rightClick = false;
        switch(k) {
            case KeyEvent.VK_1:   clickActionButton(1, rightClick);  return;
            case KeyEvent.VK_2:   clickActionButton(2, rightClick);  return;
            case KeyEvent.VK_3:   clickActionButton(3, rightClick);  return;
            case KeyEvent.VK_4:   clickActionButton(4, rightClick);  return;
            case KeyEvent.VK_5:   clickActionButton(5, rightClick);  return;
            case KeyEvent.VK_6:   clickActionButton(6, rightClick);  return;
            case KeyEvent.VK_7:   clickActionButton(7, rightClick);  return;
            case KeyEvent.VK_8:   clickActionButton(8, rightClick);  return;
            case KeyEvent.VK_LEFT: 
                log("left");
                mouseGridX = max(0,mouseGridX-1);
                hoverBox = currentGrid = combatGrids[mouseGridX][mouseGridY];
                repaint();
                return;
            case KeyEvent.VK_RIGHT: 
                log("right");
                mouseGridX = min(GRID_COUNT_X-1,mouseGridX+1);
                hoverBox = currentGrid = combatGrids[mouseGridX][mouseGridY];
                newTargetGridCell();
                repaint();
                return;
            case KeyEvent.VK_UP: 
                log("up");
                mouseGridY = max(0,mouseGridY-1);
                hoverBox = currentGrid = combatGrids[mouseGridX][mouseGridY];
                newTargetGridCell();
                repaint();
                return;
            case KeyEvent.VK_DOWN: 
                log("down");
                mouseGridY = min(GRID_COUNT_Y-1,mouseGridY+1);
                hoverBox = currentGrid = combatGrids[mouseGridX][mouseGridY];
                newTargetGridCell();
                repaint();
                return;
            case KeyEvent.VK_A:
                togglePlayPause();
                return;
            case KeyEvent.VK_N:
                if (mgr.combatIsFinished())
                    finish();
                else
                    nextStack();
                return;
            case KeyEvent.VK_R:
                retreatStack(mgr.currentStack());
                return;
            case KeyEvent.VK_E:
            case KeyEvent.VK_ESCAPE:
                finish();
        }
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
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
    public void mousePressed(MouseEvent e) {
        if (e.getButton() > 3)
            return;
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        boolean rightClick = SwingUtilities.isRightMouseButton(e);
        if (mgr.combatIsFinished())
            finish();
        
        if (rightClick) {
            moveMouseToCurrentStack();
            return;
        }
        for (ShipActionButton butt : shipActionButtons) {
            if (hoverBox == butt) {
                butt.clickAction(rightClick);
                return;
            }
        }

        if (hoverBox == resolveBox) {
            mgr.autoComplete = true;
            mgr.autoResolve = true;
            paintAllImmediately();
            finish();
        }
        else if (hoverBox == retreatBox) {
            // retreat all player ships  - TODO
            List<CombatStack> stacks = new ArrayList<>(mgr.activeStacks());
            for (CombatStack stack: stacks) 
                retreatStack(stack);
            mgr.autoComplete = true;
            hoverBox = null;
            paintAllImmediately();
            finish();
        }
        if (hoverBox == playPauseBox)
            togglePlayPause();
        else if (hoverBox == exitBox)
            finish();
        else if (hoverBox == nextBox)
            nextStack();
    }
    @Override
    public void mouseDragged(MouseEvent e) {  }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        mouseMovedTo(x,y);
    }   
    private void mouseMovedTo(int x, int y) {
        Rectangle prevHover = hoverBox;
        Rectangle prevGrid = currentGrid;
        hoverBox = null;
        currentGrid = null;

        // determine what grid we are over. that may become the hoverBox if we are
        // not also hovering over a button
        NestedLoop:
        for (int x0 = 0; x0 < GRID_COUNT_X; x0++) {
            for (int y0 = 0; y0 < GRID_COUNT_Y; y0++) {
                Rectangle grid = combatGrids[x0][y0];
                if ((grid != null) && grid.contains(x, y)) {
                    mouseGridX = x0;
                    mouseGridY = y0;
                    currentGrid = grid;
                    break NestedLoop;
                }
            }
        }

        if (resolveBox.contains(x,y)) {
            hoverBox = resolveBox;
            mouseGridX = -1;
            currentGrid = null;
        }
        else if (retreatBox.contains(x,y)) {
            hoverBox = retreatBox;
            mouseGridX = -1;
            currentGrid = null;
        }
        else if (exitBox.contains(x,y)) {
            hoverBox = exitBox;
            mouseGridX = -1;
            currentGrid = null;
        }
        else if (playPauseBox.contains(x,y)) {
            hoverBox = playPauseBox;
            mouseGridX = -1;
            currentGrid = null;
        }
        else if (nextBox.contains(x,y)) {
            hoverBox = nextBox;
            mouseGridX = -1;
            currentGrid = null;
        }
        else if (!shipActionButtons.isEmpty()) {
            List<Rectangle> buttons = new ArrayList<>(shipActionButtons);
            for (Rectangle butt : buttons) {
                if (butt.contains(x, y))
                    hoverBox = butt;
            }
        }

        // if not hovering over any buttons, then use current Grid
        if (hoverBox == null)
            hoverBox = currentGrid;

        if (currentGrid != prevGrid) 
            newTargetGridCell();

        if ((hoverBox != prevHover) || (currentGrid != prevGrid))
            repaint();
    }
    private void clickActionButton(int i, boolean rightClick) {
        if (shipActionButtons.size() >= i)
            shipActionButtons.get(i-1).clickAction(rightClick);
    }
    abstract class ShipActionButton extends Rectangle {
        private static final long serialVersionUID = 1L;
        CombatStack ship;
        boolean draw = false;
        String numKey;
        int x, y;
        private boolean canUse;
        Rectangle quick = new Rectangle();
        public void reset() {
            draw = false;
            setBounds(0,0,0,0);
            quick.setBounds(0,0,0,0);
        }
        abstract String label();
        @Override
        public boolean contains(int x, int y) {
            return super.contains(x, y) || quick.contains(x,y);
        }
        String hoverLabel()    { return label(); }
        boolean hovering()     { return hoverBox == this; }
        boolean canUse()       { return canUse; }
        void canUse(boolean b) { canUse = b; }
        public void setData(CombatStack st, int x0, int y0) {
            draw = true;
            canUse = true;
            ship = st;
            x = x0;
            y = y0;
            shipActionButtons.add(this);
            numKey = str(shipActionButtons.indexOf(this)+1);
            quick.setBounds(0,0,0,0);

            Rectangle grid = combatGrids[x][y];
            int x2 = grid.x+(grid.width/10);
            int w2 = grid.width*4/5;
            int h2 = grid.height/5;
            int y2 = grid.y+(grid.height*1/5);
            setBounds(x2,y2,w2,h2);
        }
        public void clickAction(boolean rightClick) {  }
        public void clickAllAction(boolean rightClick) {  }
        public void draw(Graphics2D g, int i, int n) {
            if (!draw)
                return;

            Rectangle grid = combatGrids[x][y];
            int vbdr = grid.height / 10;
            int h0 = n == 1 ? grid.height / 3 : (grid.height - ((n+1)*vbdr))/ n;
            int y0 = n == 1 ? grid.y + (grid.height/3) : grid.y + (i*vbdr) + ((i-1)*h0);
            int x0 = grid.x + (grid.width/20);
            int w0 = grid.width*9/10;
            setBounds(x0, y0, w0, h0);

            // if there is only 1 button in the grid, always treat is as hovering
            if (n == 1)
                hoverBox = this;

            if (moveBackC == null) {
                float[] dist = {0.0f, 1.0f};
                Point2D ptStart = new Point2D.Float(x0, y0);
                Point2D ptEnd = new Point2D.Float(x0 + w0, y0);
                Color[] colors = {grayLeftC, grayRightC};
                moveBackC = new LinearGradientPaint(ptStart, ptEnd, dist, colors);
            }
            int bdr = s10;
            Stroke prev = g.getStroke();
            g.setPaint(moveBackC);
            g.fillRoundRect(x0, y0, w0, h0, bdr, bdr);
            g.setStroke(stroke1);
            Color c0 = hovering() ? SystemPanel.yellowText : grayBorderC;
            g.setColor(c0);
            g.drawRoundRect(x0, y0, w0, h0, bdr, bdr);
            g.setStroke(prev);
            g.setFont(narrowFont(16));
            String label = text("SHIP_COMBAT_ACTION_FMT", numKey, label());
            int sw = g.getFontMetrics().stringWidth(label);
            int x1 = x0+((w0-sw)/2);
            int y1 = y0+h0-s10;
            Color c1 = hovering() ? SystemPanel.yellowText : SystemPanel.blackText;
            g.setColor(c1);
            g.drawString(label, x1, y1);
        }
        public void draw(Graphics2D g, CombatStack current, CombatStack target, boolean isLeftColumn, int x, int y, int w, int h, int numActions, Rectangle q) {
            setBounds(x,y,w,h);

            quick.setBounds(q.x, q.y, q.width, q.height);
            String label = text("SHIP_COMBAT_ACTION_FMT", numKey, label());
            String hoverLabel = text("SHIP_COMBAT_ACTION_FMT", numKey, hoverLabel());
            canUse(label.equals(hoverLabel));

            LinearGradientPaint backC;
            // create background gradient if needed
            if (isLeftColumn) {
                if (canUse())
                    backC = createLeftGreenGradient(x,y,w,h);
                else
                    backC = createLeftGrayGradient(x,y,w,h);
            }
            else {
                if (canUse())
                    backC = createRightGreenGradient(x,y,w,h);
                else
                    backC = createRightGrayGradient(x,y,w,h);
            }

            // draw button
            int cnr = s3;
            Stroke prev = g.getStroke();
            g.setColor(SystemPanel.textShadowC);
            g.setStroke(stroke4);
            g.drawRoundRect(x+s1,y+s1,w,h,cnr,cnr);
            g.setStroke(prev);
            g.setPaint(backC);
            g.fillRoundRect(x,y,w,h,cnr,cnr);

            if (canUse() && (numActions == 1) && (current != target))
                hoverBox = this;
            Color c0 = hovering() ? Color.yellow : SystemPanel.whiteText;
            Color c1 = canUse() && hovering() ? Color.yellow : SystemPanel.whiteText;
            String text = hovering() ? hoverLabel : label;
            g.setColor(c0);
            g.setStroke(stroke1);
            g.drawRoundRect(x,y,w,h,cnr,cnr);
            g.setStroke(prev);

            g.setFont(narrowFont(14));
            int sw = g.getFontMetrics().stringWidth(text);
            int x1 = x+((w-sw)/2);
            int y1 = y+h-s8;
            drawShadowedString(g, text, 3, x1, y1, SystemPanel.textShadowC, c1);
            
            Color c2 = canUse() ? greenQuickButtonC : grayQuickButtonC;
            Color c4 = canUse() && hovering() ? Color.yellow : SystemPanel.whiteText;
            g.setStroke(stroke1);
            g.setColor(c2);
            g.fill(quick);
            g.setColor(c4);
            g.draw(quick);
            g.setStroke(prev);
            g.setFont(narrowFont(14));
            sw = g.getFontMetrics().stringWidth(numKey);
            x1 = quick.x+((quick.width-sw)/2);
            y1 = quick.y+quick.height-s4;
            drawShadowedString(g, numKey, 3, x1, y1, SystemPanel.textShadowC, c1);
        }
        protected LinearGradientPaint createLeftGreenGradient(int x, int y, int w, int h) {
            if (leftGreenActionBackC == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D ptStart = new Point2D.Float(x, y);
                Point2D ptEnd = new Point2D.Float(x + w, y);
                Color[] colors = {greenButtonEdgeC, greenButtonCenterC, greenButtonEdgeC};
                leftGreenActionBackC = new LinearGradientPaint(ptStart, ptEnd, dist, colors);
            }
            return leftGreenActionBackC;
        }
        protected LinearGradientPaint createLeftGrayGradient(int x, int y, int w, int h) {
            if (leftGrayActionBackC == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D ptStart = new Point2D.Float(x, y);
                Point2D ptEnd = new Point2D.Float(x + w, y);
                Color[] colors = {grayButtonEdgeC, grayButtonCenterC, grayButtonEdgeC};
                leftGrayActionBackC = new LinearGradientPaint(ptStart, ptEnd, dist, colors);
            }
            return leftGrayActionBackC;
        }
        protected LinearGradientPaint createRightGreenGradient(int x, int y, int w, int h) {
            if (rightGreenActionBackC == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D ptStart = new Point2D.Float(x, y);
                Point2D ptEnd = new Point2D.Float(x + w, y);
                Color[] colors = {greenButtonEdgeC, greenButtonCenterC, greenButtonEdgeC};
                rightGreenActionBackC = new LinearGradientPaint(ptStart, ptEnd, dist, colors);
            }
            return rightGreenActionBackC;
        }
        protected LinearGradientPaint createRightGrayGradient(int x, int y, int w, int h) {
            if (rightGrayActionBackC == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D ptStart = new Point2D.Float(x, y);
                Point2D ptEnd = new Point2D.Float(x + w, y);
                Color[] colors = {grayButtonEdgeC, grayButtonCenterC, grayButtonEdgeC};
                rightGrayActionBackC = new LinearGradientPaint(ptStart, ptEnd, dist, colors);
            }
            return rightGrayActionBackC;
        }
    }
    class ShipDoneButton extends ShipActionButton {
        private static final long serialVersionUID = 1L;
        @Override
        String label()   { return text("SHIP_COMBAT_ACTION_TURN_DONE"); }
        @Override
        public void clickAction(boolean rightClick) {
            nextStack();
        }
    }
    class ShipMoveButton extends ShipActionButton {
        private static final long serialVersionUID = 1L;
        @Override
        String label()   { return text("SHIP_COMBAT_ACTION_MOVE"); }
        @Override
        public void clickAction(boolean rightClick) {
            if (!canUse())
                return;
            if (mgr.performingStackTurn)
                return;
            mgr.performingStackTurn = true;
            FlightPath fp = shipTravelPath;
            CombatStack curr = mgr.currentStack();
            for (int i=0; i<fp.size(); i++)
                curr.moveTo(fp.mapX(i),fp.mapY(i));
            
            // possible for the stack we are moving to die during the move 
            // (missiles) so if it's complete and the new current Stack is
            // the AI then continue until we get to another player stack
            if (curr.isTurnComplete() || curr.destroyed()) {
                if (mgr.currentStack().usingAI())
                    mgr.continueToNextPlayerStack();
                else
                    nextStack();
            }
            mgr.performingStackTurn = false;
            repaint();
        }
    }
    class ShipTeleportButton extends ShipActionButton {
        private static final long serialVersionUID = 1L;
        @Override
        String label()   { return text("SHIP_COMBAT_ACTION_TELEPORT"); }
        @Override
        public void clickAction(boolean rightClick) {
            if (!canUse())
                return;
            if (mgr.performingStackTurn)
                return;
            mgr.performingStackTurn = true;
            mgr.currentStack().teleportTo(x,y,0.1f);
            if (mgr.currentStack().isTurnComplete())
                nextStack();
            mgr.performingStackTurn = false;
            repaint();
        }
    }
    class ShipRetreatButton extends ShipActionButton {
        private static final long serialVersionUID = 1L;
        @Override
        String label()   { return text("SHIP_COMBAT_ACTION_RETREAT"); }
        @Override
        public void clickAction(boolean rightClick) {
            retreatStack(mgr.currentStack());
        }
    }
    class ShipFireAllButton extends ShipActionButton {
        private static final long serialVersionUID = 1L;
        @Override
        String label()   { return mgr.currentStack().canFireWeaponAtTarget(ship) ? text("SHIP_COMBAT_ACTION_FIRE_ALL") : text("SHIP_COMBAT_ACTION_TURN_DONE"); }
        @Override
        boolean canUse() { return true; }
        @Override
        public void clickAction(boolean rightClick) {
            if (!mgr.currentStack().canFireWeaponAtTarget(ship)) {
                nextStack();
                return;
            }
            List<ShipActionButton> buttons = new ArrayList<>(shipActionButtons);
            for (ShipActionButton butt: buttons) {
                if (butt != this)
                    butt.clickAllAction(rightClick);
            }
        }
    }
    class ShipWeaponButton extends ShipActionButton {
        private static final long serialVersionUID = 1L;
        int index;
        boolean allShots;
        public void setData(CombatStack st, int i, int x0, int y0) {
            super.setData(st,x0,y0);
            index = i;
        }
        @Override
        String label()   {
            CombatStack current = mgr.currentStack();
            int shots = current.shotsRemaining(index);
            if (shots < 2 )
                return text("SHIP_COMBAT_ACTION_WPN_COUNT", current.wpnName(index), str(current.wpnCount(index)));
            else
                return text("SHIP_COMBAT_ACTION_WPN_COUNT_SHOTS", current.wpnName(index), str(current.wpnCount(index)), str(shots));            
        }
        @Override
        String hoverLabel()   {
            CombatStack current = mgr.currentStack();
            if (!current.shipComponentValidTarget(index,ship))
                return text("SHIP_COMBAT_ACTION_RANGE");
            else if (current.shipComponentIsOutOfMissiles(index)) 
                return text("SHIP_COMBAT_ACTION_NO_MISSILES");
            else if (current.shipComponentIsOutOfBombs(index)) 
                return text("SHIP_COMBAT_ACTION_NO_BOMBS");
            else if (current.shipComponentIsUsed(index))
                return text("SHIP_COMBAT_ACTION_FIRED");
            else if (!current.shipComponentInRange(index,ship))
                return text("SHIP_COMBAT_ACTION_RANGE");
            else
                return label();
        }
        @Override
        boolean hovering() {
            return super.hovering() || (hoverBox == shipFireAllButton); 
        }
        @Override
        public void clickAction(boolean rightClick) {
            if (!canUse())
                return;
            if (mgr.performingStackTurn)
                return;
            boolean prevAnimations = mgr.showAnimations;
            if (rightClick)
                mgr.showAnimations = false;
            mgr.performingStackTurn = true;
            paintAllImmediately();
            CombatStack current = mgr.currentStack();
            current.fireWeapon(ship, index, allShots);
            mgr.performingStackTurn = false;
            mgr.showAnimations = prevAnimations;
            repaint();
            if (mgr.currentStack().isTurnComplete())
                nextStack();
        }
        @Override
        public void clickAllAction(boolean rightClick) {
            allShots = true;
            clickAction(rightClick);
            allShots = false;
        }
    }
    class ShipSpecialButton extends ShipActionButton {
        private static final long serialVersionUID = 1L;
        int weaponIndex;
        public void setData(CombatStack st, int i, int x0, int y0) {
            super.setData(st,x0,y0);
            weaponIndex = i;
        }
        @Override
        String label()   {
            return mgr.currentStack().wpnName(weaponIndex);
        }
        @Override
        String hoverLabel()   {
            CombatStack current = mgr.currentStack();
            if (!current.shipComponentValidTarget(weaponIndex,ship))
                return text("SHIP_COMBAT_ACTION_BAD_TARGET");
            else if (current.shipComponentIsOutOfMissiles(weaponIndex))
                return text("SHIP_COMBAT_ACTION_NO_MISSILES");
            else if (current.shipComponentIsOutOfBombs(weaponIndex))
                return text("SHIP_COMBAT_ACTION_NO_BOMBS");
            else if (current.shipComponentIsUsed(weaponIndex))
                return text("SHIP_COMBAT_ACTION_FIRED");
            else if (!current.shipComponentInRange(weaponIndex,ship))
                return text("SHIP_COMBAT_ACTION_RANGE");
            else
                return label();
        }
        @Override
        boolean hovering() {
            return super.hovering() || (hoverBox == shipFireAllButton); 
        }
        @Override
        public void clickAction(boolean rightClick) {
            if (!canUse())
                return;
            if (mgr.performingStackTurn)
                return;
            boolean prevAnimations = mgr.showAnimations;
            if (rightClick)
                mgr.showAnimations = false;
            mgr.performingStackTurn = true;
            CombatStack current = mgr.currentStack();
            current.fireWeapon(ship, weaponIndex);
            mgr.performingStackTurn = false;
            mgr.showAnimations = prevAnimations;
            repaint();
            if (mgr.currentStack().isTurnComplete())
                nextStack();
        }
        @Override
        public void clickAllAction(boolean rightClick) {
            // specials are not used with the fire all weapons action
        }
    }
}
