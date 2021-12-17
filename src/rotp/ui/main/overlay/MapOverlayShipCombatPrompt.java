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
package rotp.ui.main.overlay;

import rotp.model.Sprite;
import rotp.model.combat.ShipCombatManager;
import rotp.model.empires.Empire;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import rotp.ui.combat.ShipBattleUI;

public class MapOverlayShipCombatPrompt extends MapOverlay {
    static final Color destroyedTextC = new Color(255,32,32,192);
    static final Color destroyedMaskC = new Color(0,0,0,160);
    Color maskC  = new Color(40,40,40,160);
    Area mask;
    BufferedImage planetImg;
    MainUI parent;
    int sysId;
    ShipFleet fleet;
    int pop, bases, fact, shield;
    public int boxX, boxY, boxW, boxH;
    boolean drawSprites = false;
    public ShipCombatManager mgr;
    AutoResolveBattleSprite resolveButton = new AutoResolveBattleSprite();
    RetreatAllBattleSprite retreatButton = new RetreatAllBattleSprite();
    EnterBattleSprite battleButton = new EnterBattleSprite();
    SystemFlagSprite flagButton = new SystemFlagSprite();
    public MapOverlayShipCombatPrompt(MainUI p) {
        parent = p;
    }
    public void init(ShipCombatManager m) {
        mgr = m;
        sysId = mgr.system().id;
        Empire pl = player();
        flagButton.reset();
        StarSystem sys = galaxy().system(sysId);
        fleet = null;
        planetImg = null;
        drawSprites = true;
        pop = pl.sv.population(sysId);
        bases = pl.sv.bases(sysId);
        fact = pl.sv.factories(sysId);
        shield = pl.sv.shieldLevel(sysId);
        parent.hideDisplayPanel();
        parent.map().setScale(20);
        parent.map().recenterMapOn(sys);
        parent.mapFocus(sys);
        parent.clickedSprite(sys);
        parent.repaint();
    }
    public void startCombat(int combatFlag) {
        drawSprites = false;
        parent.clearOverlay();
        parent.repaintAllImmediately();
        RotPUI.instance().selectShipBattlePanel(mgr, combatFlag);
    }
    private StarSystem starSystem() {
        return galaxy().system(sysId);
    }
    private void toggleFlagColor(boolean reverse) {
        player().sv.toggleFlagColor(sysId, reverse);
        parent.repaint();
    }
    private void resetFlagColor() {
        player().sv.resetFlagColor(sysId);
        parent.repaint();
    }
    @Override
    public boolean drawSprites()   { return drawSprites; }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public void advanceMap() {
        startCombat(ShipBattleUI.ENTER_COMBAT);
    }
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
        StarSystem sys = galaxy().system(sysId);
        Empire pl = player();

        int s7 = BasePanel.s7;
        int s10 = BasePanel.s10;
        int s15 = BasePanel.s15;
        int s20 = BasePanel.s20;
        int s25 = BasePanel.s25;
        int s30 = BasePanel.s30;
        int s40 = BasePanel.s40;
        int s50 = BasePanel.s50;
        int s60 = BasePanel.s60;

        int w = ui.getWidth();
        int h = ui.getHeight();

        int bdrW = s7;
        boxW = scaled(540);
        int boxH1 = BasePanel.s68;
        int boxH2 = scaled(172);
        int buttonPaneH = scaled(35);
        boxH = boxH1 + boxH2 + buttonPaneH;
        
        boxX = -s40+(w/2);
        boxY = -s40+(h-boxH)/2;

        // draw map mask
        if (mask == null) {
            int r = s60;
            int centerX = w*2/5;
            int centerY = h*2/5;
            Ellipse2D window = new Ellipse2D.Float();
            window.setFrame(centerX-r, centerY-r, r+r, r+r);
            Area st1 = new Area(window);
            Rectangle blackout  = new Rectangle();
            blackout.setFrame(0,0,w,h);
            mask = new Area(blackout);
            mask.subtract(st1);
        }
        g.setColor(maskC);
        g.fill(mask);
        // draw border
        g.setColor(MainUI.paneShadeC);
        g.fillRect(boxX-bdrW, boxY-bdrW, boxW+bdrW+bdrW, boxH+bdrW+bdrW);

        // draw Box
        g.setColor(MainUI.paneBackground);
        g.fillRect(boxX, boxY, boxW, boxH1);

        boolean scouted = player().sv.isScouted(sys.id);
        // draw planet image
        if (planetImg == null) {
            if (!scouted || sys.planet().type().isAsteroids()) {
                planetImg = newBufferedImage(boxW, boxH2);
                Graphics imgG = planetImg.getGraphics();
                imgG.setColor(Color.black);
                imgG.fillRect(0, 0, boxW, boxH2);
                drawBackgroundStars(imgG, boxW, boxH2);
                parent.drawStar((Graphics2D) imgG, sys.starType(), s60, boxW*4/5, boxH2/3);
                imgG.dispose();
            }
            else {
                planetImg = sys.planet().type().panoramaImage();
                int planetW = planetImg.getWidth();
                int planetH = planetImg.getHeight();
                Graphics imgG = planetImg.getGraphics();
                Empire emp = sys.empire();
                if (emp != null) {
                    BufferedImage fortImg = emp.race().fortress(sys.colony().fortressNum());
                    int fortW = scaled(fortImg.getWidth());
                    int fortH = scaled(fortImg.getHeight());
                    int fortScaleW = fortW*planetW/w;
                    int fortScaleH = fortH*planetW/w;
                    int fortX = planetImg.getWidth()-fortScaleW;
                    int fortY = planetImg.getHeight()-fortScaleH+(planetH/5);
                    imgG.drawImage(fortImg, fortX, fortY, fortX+fortScaleW, fortY+fortScaleH, 0, 0, fortImg.getWidth(), fortImg.getHeight(), null);
                    imgG.dispose();
                }
            }
        }
        g.drawImage(planetImg, boxX, boxY+boxH1, boxW, boxH2, null);

        // draw header info
        int leftW = boxW * 2/5;
        String yearStr = displayYearOrTurn();
        g.setFont(narrowFont(40));
        int sw = g.getFontMetrics().stringWidth(yearStr);
        int x0 = boxX+((leftW-sw)/2);
        drawBorderedString(g, yearStr, 2, x0, boxY+boxH1-s20, SystemPanel.textShadowC, SystemPanel.orangeText);

        
        
        Empire aiEmpire = mgr.results().aiEmpire();
        String titleStr;
        if (aiEmpire == null)
            titleStr = text("SHIP_COMBAT_TITLE_MONSTER_DESC", mgr.results().aiRaceName());
        else {
            titleStr = text("SHIP_COMBAT_TITLE_DESC");
            titleStr = aiEmpire.replaceTokens(titleStr, "alien");
        }
        g.setColor(Color.black);
        int titleFontSize = scaledFont(g, titleStr, boxW-leftW, 20, 14);
        g.setFont(narrowFont(titleFontSize));
        drawString(g,titleStr, boxX+leftW, boxY+s20);

        // print prompt string
        String sysName = player().sv.name(sys.id);
        String promptStr = scouted ? text("SHIP_COMBAT_TITLE_SYSTEM", sysName) : text("SHIP_COMBAT_TITLE_UNSCOUTED");
        int promptFontSize = scaledFont(g, promptStr, boxW-leftW-s30, 24, 20);
        g.setFont(narrowFont(promptFontSize));
        drawShadowedString(g, promptStr, 4, boxX+leftW, boxY+s50, SystemPanel.textShadowC, Color.white);

        // init and draw battle and resolve buttons
        parent.addNextTurnControl(battleButton);
        battleButton.init(this, g);
        battleButton.mapX(boxX+boxW-battleButton.width());
        battleButton.mapY(boxY+boxH-battleButton.height());
        battleButton.draw(parent.map(), g);
        
        if (aiEmpire != null) {
            parent.addNextTurnControl(resolveButton);
            resolveButton.init(this, g);
            resolveButton.mapX(boxX);
            resolveButton.mapY(battleButton.mapY());
            resolveButton.draw(parent.map(), g);

            parent.addNextTurnControl(retreatButton);
            retreatButton.init(this, g);
            retreatButton.mapX(resolveButton.mapX()+resolveButton.width()+s10);
            retreatButton.mapY(battleButton.mapY());
            retreatButton.draw(parent.map(), g);
        }
        // if unscouted, no planet info
        if (!scouted)
            return;
        
        // draw planet info, from bottom up
        int x1 = boxX+s15;
        int y1 = boxY+boxH1+boxH2-s10;
        int lineH = s20;
        int desiredFont = 18;

        if (pl.sv.isUltraPoor(sys.id)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_ULTRA_POOR_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isPoor(sys.id)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_POOR_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isRich(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_RICH_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isUltraRich(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ULTRA_RICH_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        if (pl.sv.isOrionArtifact(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ANCIENTS_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isArtifact(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ARTIFACTS_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        if (pl.isEnvironmentHostile(sys)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_HOSTILE_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.isEnvironmentFertile(sys)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_FERTILE_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.isEnvironmentGaia(sys)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_GAIA_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        // classification line
        if (sys.planet().type().isAsteroids()) {
            String s1 = text("MAIN_SCOUT_NO_PLANET");
            g.setFont(narrowFont(desiredFont+3));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else {
            String s1 = text("MAIN_SCOUT_TYPE", text(sys.planet().type().key()), (int)sys.planet().maxSize());
            g.setFont(narrowFont(desiredFont+3));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        // planet name
        y1 -= scaled(5);
        g.setFont(narrowFont(40));
        drawBorderedString(g, sysName, 1, x1, y1, Color.darkGray, SystemPanel.orangeText);
        
        // planet flag
        parent.addNextTurnControl(flagButton);
        flagButton.init(this, g);
        flagButton.mapX(boxX+boxW-flagButton.width()+s10);
        flagButton.mapY(boxY+boxH-buttonPaneH-flagButton.height()+s10);
        flagButton.draw(parent.map(), g);
    }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        boolean shift = e.isShiftDown();
        Empire aiEmpire = mgr.results().aiEmpire();
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_E:
                startCombat(ShipBattleUI.ENTER_COMBAT);
                break;
            case KeyEvent.VK_A:
                if (aiEmpire != null)
                    startCombat(ShipBattleUI.AUTO_RESOLVE);
                break;
            case KeyEvent.VK_R:
                if (aiEmpire != null)
                    startCombat(ShipBattleUI.RETREAT_ALL);
                break;
            case KeyEvent.VK_F:
                toggleFlagColor(shift);
                break;
            default:
                misClick();
                break;
        }
        return true;
    }
    class AutoResolveBattleSprite extends MapSprite {
        private LinearGradientPaint background;
        private final Color edgeC = new Color(59,59,59);
        private final Color midC = new Color(93,93,93);
        private int mapX, mapY, buttonW, buttonH;
        private int selectX, selectY, selectW, selectH;

        private MapOverlayShipCombatPrompt parent;

        protected int mapX()      { return mapX; }
        protected int mapY()      { return mapY; }
        public void mapX(int i)   { selectX = mapX = i; }
        public void mapY(int i)   { selectY = mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        private String label()    { return text("SHIP_COMBAT_AUTO_RESOLVE"); }
        private Font font()       { return narrowFont(18); }
        public void reset()       { background = null; }

        public void init(MapOverlayShipCombatPrompt p, Graphics2D g)  {
            parent = p;
            buttonW = BasePanel.s40 + g.getFontMetrics(font()).stringWidth(label());
            buttonH = BasePanel.s30;
            selectW = buttonW;
            selectH = buttonH;
        }
        public void setSelectionBounds(int x, int y, int w, int h) {
            selectX = x;
            selectY = y;
            selectW = w;
            selectH = h;
        }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
            hovering = x >= selectX
                        && x <= selectX+selectW
                        && y >= selectY
                        && y <= selectY+selectH;
            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            if (!parent.drawSprites())
                return;
            if (background == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D start = new Point2D.Float(mapX, 0);
                Point2D end = new Point2D.Float(mapX+buttonW, 0);
                Color[] colors = {edgeC, midC, edgeC };
                background = new LinearGradientPaint(start, end, dist, colors);
            }
            int s3 = BasePanel.s3;
            int s5 = BasePanel.s5;
            int s10 = BasePanel.s10;
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(mapX+s3, mapY+s3, buttonW,buttonH,s10,s10);
            g.setPaint(background);
            g.fillRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
            Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
            g.setColor(c0);
            Stroke prevStr =g.getStroke();
            g.setStroke(BasePanel.stroke2);
            g.drawRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
            g.setStroke(prevStr);
            g.setFont(font());

            String str = label();
            int sw = g.getFontMetrics().stringWidth(str);
            int x2a = mapX+((buttonW-sw)/2);
            drawBorderedString(g, str, x2a, mapY+buttonH-s10, SystemPanel.textShadowC, c0);
        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
            startCombat(ShipBattleUI.AUTO_RESOLVE);
        };
    }
    class RetreatAllBattleSprite extends MapSprite {
        private LinearGradientPaint background;
        private final Color edgeC = new Color(59,59,59);
        private final Color midC = new Color(93,93,93);
        private int mapX, mapY, buttonW, buttonH;
        private int selectX, selectY, selectW, selectH;

        private MapOverlayShipCombatPrompt parent;

        protected int mapX()      { return mapX; }
        protected int mapY()      { return mapY; }
        public void mapX(int i)   { selectX = mapX = i; }
        public void mapY(int i)   { selectY = mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        private String label()    { return text("SHIP_COMBAT_RETREAT_ALL"); }
        private Font font()       { return narrowFont(18); }
        public void reset()       { background = null; }

        public void init(MapOverlayShipCombatPrompt p, Graphics2D g)  {
            parent = p;
            buttonW = BasePanel.s40 + g.getFontMetrics(font()).stringWidth(label());
            buttonH = BasePanel.s30;
            selectW = buttonW;
            selectH = buttonH;
        }
        public void setSelectionBounds(int x, int y, int w, int h) {
            selectX = x;
            selectY = y;
            selectW = w;
            selectH = h;
        }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
            hovering = x >= selectX
                        && x <= selectX+selectW
                        && y >= selectY
                        && y <= selectY+selectH;
            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            if (!parent.drawSprites())
                return;
            if (background == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D start = new Point2D.Float(mapX, 0);
                Point2D end = new Point2D.Float(mapX+buttonW, 0);
                Color[] colors = {edgeC, midC, edgeC };
                background = new LinearGradientPaint(start, end, dist, colors);
            }
            int s3 = BasePanel.s3;
            int s5 = BasePanel.s5;
            int s10 = BasePanel.s10;
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(mapX+s3, mapY+s3, buttonW,buttonH,s10,s10);
            g.setPaint(background);
            g.fillRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
            Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
            g.setColor(c0);
            Stroke prevStr =g.getStroke();
            g.setStroke(BasePanel.stroke2);
            g.drawRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
            g.setStroke(prevStr);
            g.setFont(font());

            String str = label();
            int sw = g.getFontMetrics().stringWidth(str);
            int x2a = mapX+((buttonW-sw)/2);
            drawBorderedString(g, str, x2a, mapY+buttonH-s10, SystemPanel.textShadowC, c0);
        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
            startCombat(ShipBattleUI.RETREAT_ALL);
        };
    }
    class EnterBattleSprite extends MapSprite {
        private LinearGradientPaint background;
        private final Color edgeC = new Color(44,59,30);
        private final Color midC = new Color(70,93,48);
        private int mapX, mapY, buttonW, buttonH;
        private int selectX, selectY, selectW, selectH;

        private MapOverlayShipCombatPrompt parent;

        protected int mapX()      { return mapX; }
        protected int mapY()      { return mapY; }
        public void mapX(int i)   { selectX = mapX = i; }
        public void mapY(int i)   { selectY = mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        private String label()    { return text("SHIP_COMBAT_ENTER_BATTLE"); }
        private Font font()       { return narrowFont(18); }
        public void reset()       { background = null; }

        public void init(MapOverlayShipCombatPrompt p, Graphics2D g)  {
            parent = p;
            buttonW = BasePanel.s40 + g.getFontMetrics(font()).stringWidth(label());
            buttonH = BasePanel.s30;
            selectW = buttonW;
            selectH = buttonH;
        }
        public void setSelectionBounds(int x, int y, int w, int h) {
            selectX = x;
            selectY = y;
            selectW = w;
            selectH = h;
        }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
            hovering = x >= selectX
                        && x <= selectX+selectW
                        && y >= selectY
                        && y <= selectY+selectH;
            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            if (!parent.drawSprites())
                return;
            if (background == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D start = new Point2D.Float(mapX, 0);
                Point2D end = new Point2D.Float(mapX+buttonW, 0);
                Color[] colors = {edgeC, midC, edgeC };
                background = new LinearGradientPaint(start, end, dist, colors);
            }
            int s3 = BasePanel.s3;
            int s5 = BasePanel.s5;
            int s10 = BasePanel.s10;
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(mapX+s3, mapY+s3, buttonW,buttonH,s10,s10);
            g.setPaint(background);
            g.fillRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
            Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
            g.setColor(c0);
            Stroke prevStr =g.getStroke();
            g.setStroke(BasePanel.stroke2);
            g.drawRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
            g.setStroke(prevStr);
            g.setFont(font());

            String str = label();
            int sw = g.getFontMetrics().stringWidth(str);
            int x2a = mapX+((buttonW-sw)/2);
            drawBorderedString(g, str, x2a, mapY+buttonH-s10, SystemPanel.textShadowC, c0);
        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
            startCombat(ShipBattleUI.ENTER_COMBAT);
        };
    }
    class SystemFlagSprite extends MapSprite {
        private int mapX, mapY, buttonW, buttonH;
        private int selectX, selectY, selectW, selectH;

        private MapOverlayShipCombatPrompt parent;

        protected int mapX()      { return mapX; }
        protected int mapY()      { return mapY; }
        public void mapX(int i)   { selectX = mapX = i; }
        public void mapY(int i)   { selectY = mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        public void reset()       {  }

        public void init(MapOverlayShipCombatPrompt p, Graphics2D g)  {
            parent = p;
            buttonW = BasePanel.s70;
            buttonH = BasePanel.s70;
            selectW = buttonW;
            selectH = buttonH;
        }
        public void setSelectionBounds(int x, int y, int w, int h) {
            selectX = x;
            selectY = y;
            selectW = w;
            selectH = h;
        }
        @Override
        public boolean acceptDoubleClicks()         { return true; }
        @Override
        public boolean acceptWheel()                { return true; }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
            hovering = x >= selectX
                        && x <= selectX+selectW
                        && y >= selectY
                        && y <= selectY+selectH;
            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            if (!parent.drawSprites())
                return;
            StarSystem sys = parent.starSystem();
            Image flagImage = parent.parent.flagImage(sys);
            Image flagHaze = parent.parent.flagHaze(sys);
            g.drawImage(flagHaze, mapX, mapY, buttonW, buttonH, null);
            if (hovering) {
                Image flagHover = parent.parent.flagHover(sys);
                g.drawImage(flagHover, mapX, mapY, buttonW, buttonH, null);
            }
            g.drawImage(flagImage, mapX, mapY, buttonW, buttonH, null);
        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
            if (rightClick)
                parent.resetFlagColor();
            else
                parent.toggleFlagColor(false);
        };
        @Override
        public void wheel(GalaxyMapPanel map, int rotation, boolean click) {
            if (rotation < 0)
                parent.toggleFlagColor(true);
            else
                parent.toggleFlagColor(false);
        };
    }
}
