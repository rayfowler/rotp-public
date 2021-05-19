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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.MapSprite;

public class MapOverlaySystemsScouted extends MapOverlay {
    Color maskC  = new Color(40,40,40,160);
    Area mask;
    BufferedImage planetImg;
    MainUI parent;
    float origMapScale;
    List<StarSystem> scoutSystems = new ArrayList<>();
    List<StarSystem> allySystems = new ArrayList<>();
    List<StarSystem> astronomerSystems = new ArrayList<>();
    List<StarSystem> orderedSystems = new ArrayList<>();
    int systemIndex = 0;
    boolean drawSprites = false;
    PreviousSystemButtonSprite prevSystemButton = new PreviousSystemButtonSprite();
    NextSystemButtonSprite nextSystemButton = new NextSystemButtonSprite();
    ContinueButtonSprite continueButton = new ContinueButtonSprite();
    SystemFlagSprite flagButton = new SystemFlagSprite();
    public MapOverlaySystemsScouted(MainUI p) {
        parent = p;
    }
    public void init(HashMap<String, List<StarSystem>> newSystems) {
        parent.hideDisplayPanel();
        origMapScale = parent.map().scaleY();
        parent.map().setScale(20);
        systemIndex = 0;
        drawSprites = true;
        orderedSystems.clear();
        continueButton.reset();
        prevSystemButton.reset();
        nextSystemButton.reset();
        flagButton.reset();
        if (newSystems.isEmpty())
            advanceMap();
        else {
            // create an alphabetized list of systems
            scoutSystems = newSystems.get("Scouts");
            allySystems = newSystems.get("Allies");
            astronomerSystems = newSystems.get("Astronomers");
            orderedSystems.addAll(scoutSystems);
            orderedSystems.addAll(astronomerSystems);
            orderedSystems.addAll(allySystems);
            Collections.sort(orderedSystems, StarSystem.NAME);
            mapSelectIndex(0);
        }
    }
    private void mapSelectIndex(int i) {
        mask = null;
        planetImg = null;
        StarSystem nextSystem = orderedSystems.get(i);
        parent.map().recenterMapOn(nextSystem);
        parent.mapFocus(nextSystem);
        parent.clickedSprite(nextSystem);
        parent.repaint();
    }
    private StarSystem starSystem() {
        return orderedSystems.get(systemIndex);
    }
    public void nextSystem() {
        systemIndex++;
        if (systemIndex >= orderedSystems.size())
            systemIndex = 0;
        mapSelectIndex(systemIndex);
    }
    public void previousSystem() {
        systemIndex--;
        if (systemIndex < 0)
            systemIndex = orderedSystems.size()-1;
        mapSelectIndex(systemIndex);
    }
    private void toggleFlagColor(boolean reverse) {
        StarSystem sys = orderedSystems.get(systemIndex);
        player().sv.toggleFlagColor(sys.id, reverse);
        parent.repaint();
    }
    private void resetFlagColor() {
        StarSystem sys = orderedSystems.get(systemIndex);
        player().sv.resetFlagColor(sys.id);
        parent.repaint();
    }
    @Override
    public void advanceMap() {
        if (drawSprites) {
            drawSprites = false;
            orderedSystems.clear();
            scoutSystems.clear();
            allySystems.clear();
            astronomerSystems.clear();

            if (session().performingTurn())
                parent.resumeTurn();
            else
                parent.resumeOutsideTurn();
        }        
    }
    @Override
    public boolean drawSprites()   { return drawSprites; }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
        if (orderedSystems.isEmpty())
            return;
        StarSystem sys = orderedSystems.get(systemIndex);
        Empire pl = player();

        int s7 = BasePanel.s7;
        int s10 = BasePanel.s10;
        int s15 = BasePanel.s15;
        int s20 = BasePanel.s20;
        int s25 = BasePanel.s25;
        int s30 = BasePanel.s30;
        int s40 = BasePanel.s40;
        int s60 = BasePanel.s60;

        int w = ui.getWidth();
        int h = ui.getHeight();

        int bdrW = s7;
        int boxW = scaled(540);
        int boxH = scaled(240);
        int boxH1 = scaled(68);
        int buttonPaneH = s40;

        int boxX = -s40+(w/2);
        int boxY = s40+(h-boxH)/2;
        
        // dimensions of the shade pane
        int x0 = boxX-bdrW;
        int y0 = boxY-bdrW;
        int w0 = boxW+bdrW+bdrW;
        int h0 = boxH+bdrW+bdrW+buttonPaneH;

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
        g.fillRect(x0, y0, w0, h0);

        // draw Box
        g.setColor(MainUI.paneBackground);
        g.fillRect(boxX, boxY, boxW, boxH1);

        // draw planet image
        if (planetImg == null) {
            if (sys.planet().type().isAsteroids()) {
                planetImg = newBufferedImage(boxW, boxH-boxH1);
                Graphics imgG = planetImg.getGraphics();
                imgG.setColor(Color.black);
                imgG.fillRect(0, 0, boxW, boxH-boxH1);
                drawBackgroundStars(imgG, boxW, boxH-boxH1);
                parent.drawStar((Graphics2D) imgG, sys.starType(), s60, boxW*4/5, (boxH-boxH1)/3);
                imgG.dispose();
            }
            else 
                planetImg = sys.planet().type().panoramaImage();
        }
        g.drawImage(planetImg, boxX, boxY+boxH1, boxW, boxH-boxH1, null);

        // draw header info
        int leftW = boxW * 2/5;
        String yearStr = displayYearOrTurn();
        g.setFont(narrowFont(40));
        int sw = g.getFontMetrics().stringWidth(yearStr);
        int x1 = boxX+((leftW-sw)/2);
        drawBorderedString(g, yearStr, 2, x1, boxY+boxH1-s20, SystemPanel.textShadowC, SystemPanel.orangeText);

        String scoutStr = text("MAIN_SCOUT_TITLE");
        int titleFontSize = scaledFont(g, scoutStr, boxW-leftW-s10, 24, 14);
        g.setFont(narrowFont(titleFontSize));
        drawShadowedString(g, scoutStr, 4, boxX+leftW, boxY+boxH1-s40, SystemPanel.textShadowC, Color.white);

        String detailStr = "";
        if (scoutSystems.contains(sys))
            detailStr = text("MAIN_SCOUT_SUBTITLE_1");
        else if (astronomerSystems.contains(sys))
            detailStr = text("MAIN_SCOUT_SUBTITLE_2");
        else if (allySystems.contains(sys))
            detailStr = text("MAIN_SCOUT_SUBTITLE_3");
            
        if (!detailStr.isEmpty()) {
            g.setColor(Color.darkGray);
            g.setFont(narrowFont(16));
            drawString(g,detailStr, boxX+leftW+s30, boxY+boxH1-s20);
        }

        // draw planet info, from bottom up
        int x2 = boxX+s15;
        int y2 = boxY+boxH-s10;
        int lineH = s20;
        int desiredFont = 18;

        if (pl.sv.isUltraPoor(sys.id)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_ULTRA_POOR_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x2, y2, Color.black, Color.white);
            y2 -= lineH;
        }
        else if (pl.sv.isPoor(sys.id)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_POOR_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x2, y2, Color.black, Color.white);
            y2 -= lineH;
        }
        else if (pl.sv.isRich(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_RICH_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x2, y2, Color.black, Color.white);
            y2 -= lineH;
        }
        else if (pl.sv.isUltraRich(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ULTRA_RICH_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x2, y2, Color.black, Color.white);
            y2 -= lineH;
        }

        if (pl.sv.isOrionArtifact(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ANCIENTS_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x2, y2, Color.black, Color.white);
            y2 -= lineH;
        }
        else if (pl.sv.isArtifact(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ARTIFACTS_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x2, y2, Color.black, Color.white);
            y2 -= lineH;
        }

        if (pl.isEnvironmentHostile(sys)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_HOSTILE_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x2, y2, Color.black, Color.white);
            y2 -= lineH;
        }
        else if (pl.isEnvironmentFertile(sys)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_FERTILE_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x2, y2, Color.black, Color.white);
            y2 -= lineH;
        }
        else if (pl.isEnvironmentGaia(sys)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_GAIA_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x2, y2, Color.black, Color.white);
            y2 -= lineH;
        }

        // classification line
        if (sys.planet().type().isAsteroids()) {
            String s1 = text("MAIN_SCOUT_NO_PLANET");
            g.setFont(narrowFont(desiredFont+3));
            drawBorderedString(g, s1, 1, x2, y2, Color.black, Color.white);
            y2 -= lineH;
        }
        else {
            String s1 = text("MAIN_SCOUT_TYPE", text(sys.planet().type().key()), (int)sys.planet().maxSize());
            g.setFont(narrowFont(desiredFont+3));
            drawBorderedString(g, s1, 1, x2, y2, Color.black, Color.white);
            y2 -= lineH;
        }

        if (pl.sv.isColonized(sys.id)) {
            g.setFont(narrowFont(24));
            String s1 = pl.sv.descriptiveName(sys.id);
            int fontSize = scaledFont(g, s1, boxW-x2-s10, 24, 18);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x2, y2, Color.black, Color.white);
            y2 -= lineH;
            y2 -= scaled(5);
        }
        // planet name
        String sysName = pl.sv.name(sys.id);
        y2 -= scaled(5);
        g.setColor(SystemPanel.orangeText);
        g.setFont(narrowFont(40));
        drawBorderedString(g, sysName, 1, x2, y2, Color.darkGray, SystemPanel.orangeText);
        
        // planet flag
        parent.addNextTurnControl(flagButton);
        flagButton.init(this, g);
        flagButton.mapX(boxX+boxW-flagButton.width()+s10);
        flagButton.mapY(boxY+boxH-flagButton.height()+s10);
        flagButton.draw(parent.map(), g);

        // init and draw continue button sprite
        parent.addNextTurnControl(continueButton);
        continueButton.init(this, g);
        continueButton.mapX(x0+w0-continueButton.width()-s10);
        continueButton.mapY(y0+h0-continueButton.height()-s10);
        if (orderedSystems.size() < 2)
            continueButton.setSelectionBounds(x0,y0,w0,h0);
        continueButton.draw(parent.map(), g);

        if (orderedSystems.size() > 1) {
            parent.addNextTurnControl(prevSystemButton);
            prevSystemButton.init(this,g);
            prevSystemButton.mapX(x0+s10);
            prevSystemButton.mapY(continueButton.mapY());
            prevSystemButton.draw(parent.map(), g);

            // draw notice number
            String notice2Str = text("MAIN_ALLOCATE_BRIEF_NUMBER", str(systemIndex+1), str(orderedSystems.size()));
            g.setFont(narrowFont(16));
            int sw4 = g.getFontMetrics().stringWidth(notice2Str);
            int x4b = prevSystemButton.mapX()+prevSystemButton.width()+s10;
            int y4b = prevSystemButton.mapY()+prevSystemButton.height()-s10;
            g.setColor(SystemPanel.blackText);
            drawString(g,notice2Str, x4b, y4b);

            parent.addNextTurnControl(nextSystemButton);
            nextSystemButton.init(this,g);
            nextSystemButton.mapX(x4b+sw4+s10);
            nextSystemButton.mapY(continueButton.mapY());
            nextSystemButton.draw(parent.map(), g);
        }
    }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        boolean shift = e.isShiftDown();
        switch(e.getKeyCode()) {
            case KeyEvent.VK_N:
                nextSystem();
                break;
            case KeyEvent.VK_P:
                previousSystem();
                break;
            case KeyEvent.VK_C:
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_ESCAPE:
                advanceMap();
                break;
            case KeyEvent.VK_F:
                toggleFlagColor(shift);
                break;
            default:
                misClick(); break;
        }
        return true;
    }
    class PreviousSystemButtonSprite extends MapSprite {
        private LinearGradientPaint background;
        private final Color edgeC = new Color(59,59,59);
        private final Color midC = new Color(93,93,93);
        private int mapX, mapY, buttonW, buttonH;
        private MapOverlaySystemsScouted parent;

        public int mapX()         { return mapX; }
        public int mapY()         { return mapY; }
        public void mapX(int i)   { mapX = i; }
        public void mapY(int i)   { mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        private String label()    { return text("MAIN_ALLOCATE_PREV_SYSTEM"); }
        private Font font()       { return narrowFont(18); }
        public void reset()       { background = null; }

        public void init(MapOverlaySystemsScouted p, Graphics2D g)  {
            parent = p;
            buttonW = BasePanel.s20 + g.getFontMetrics(font()).stringWidth(label());
            buttonH = BasePanel.s30;
        }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
            hovering = x >= mapX
                        && x <= mapX+buttonW
                        && y >= mapY()
                        && y <= mapY()+buttonH;

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
            //if (click)
            //    softClick();
            parent.previousSystem();
        };
    }
     class NextSystemButtonSprite extends MapSprite {
        private LinearGradientPaint background;
        private final Color edgeC = new Color(44,59,30);
        private final Color midC = new Color(70,93,48);
        private int mapX, mapY, buttonW, buttonH;
        private MapOverlaySystemsScouted parent;

        public int mapX()         { return mapX; }
        public int mapY()         { return mapY; }
        public void mapX(int i)   { mapX = i; }
        public void mapY(int i)   { mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        private String label()    { return text("MAIN_ALLOCATE_NEXT_SYSTEM"); }
        private Font font()       { return narrowFont(18); }
        public void reset()       { background = null; }

        public void init(MapOverlaySystemsScouted p, Graphics2D g)  {
            parent = p;
            buttonW = BasePanel.s20 + g.getFontMetrics(font()).stringWidth(label());
            buttonH = BasePanel.s30;
        }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
            hovering = x >= mapX
                        && x <= mapX+buttonW
                        && y >= mapY()
                        && y <= mapY()+buttonH;

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
            //if (click)
            //    softClick();
            parent.nextSystem();
        };
    }
    class ContinueButtonSprite extends MapSprite {
        private LinearGradientPaint background;
        private final Color edgeC = new Color(59,59,59);
        private final Color midC = new Color(93,93,93);
        private int mapX, mapY, buttonW, buttonH;
        private int selectX, selectY, selectW, selectH;

        private MapOverlaySystemsScouted parent;

        protected int mapX()      { return mapX; }
        protected int mapY()      { return mapY; }
        public void mapX(int i)   { selectX = mapX = i; }
        public void mapY(int i)   { selectY = mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        private String label()    { return text("MAIN_ALLOCATE_CLOSE"); }
        private Font font()       { return narrowFont(18); }
        public void reset()       { background = null; }

        public void init(MapOverlaySystemsScouted p, Graphics2D g)  {
            parent = p;
            buttonW = BasePanel.s60 + g.getFontMetrics(font()).stringWidth(label());
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
            //if (click)
            //    softClick();
            parent.advanceMap();
        };
    }
    class SystemFlagSprite extends MapSprite {
        private int mapX, mapY, buttonW, buttonH;
        private int selectX, selectY, selectW, selectH;

        private MapOverlaySystemsScouted parent;

        protected int mapX()      { return mapX; }
        protected int mapY()      { return mapY; }
        public void mapX(int i)   { selectX = mapX = i; }
        public void mapY(int i)   { selectY = mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        public void reset()        { }

        public void init(MapOverlaySystemsScouted p, Graphics2D g)  {
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
    