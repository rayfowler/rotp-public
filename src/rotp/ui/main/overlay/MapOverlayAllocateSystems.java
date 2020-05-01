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
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import rotp.model.Sprite;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.MapSprite;

public class MapOverlayAllocateSystems extends MapOverlay {
    MainUI parent;
    HashMap<StarSystem,List<String>> systemsToAllocate = new HashMap<>();
    List<StarSystem> orderedSystems = new ArrayList<>();
    private LinearGradientPaint arrowBack;
    int systemIndex = 0;
    int x[] = new int[9];
    int y[] = new int[9];
    boolean drawSprites = false;
    PreviousSystemButtonSprite prevSystemButton = new PreviousSystemButtonSprite();
    NextSystemButtonSprite nextSystemButton = new NextSystemButtonSprite();
    ContinueButtonSprite continueButton = new ContinueButtonSprite();
    public MapOverlayAllocateSystems(MainUI p) {
        parent = p;
    }
    public void init(HashMap<StarSystem,List<String>> newSystems) {
        drawSprites = true;
        systemsToAllocate = newSystems;
        orderedSystems.clear();
        systemIndex = 0;
        continueButton.reset();
        prevSystemButton.reset();
        nextSystemButton.reset();
        if (newSystems.isEmpty())
            advanceMap();
        else {
            // create an alphabetized list of systems
            orderedSystems.addAll(newSystems.keySet());
            Collections.sort(orderedSystems, StarSystem.NAME);
            mapSelectIndex(0);
        }
    }
    private void mapSelectIndex(int i) {
        StarSystem nextSystem = orderedSystems.get(i);
        parent.map().recenterMapOn(nextSystem);
        parent.mapFocus(nextSystem);
        parent.clickedSprite(nextSystem);
        parent.showDisplayPanel();
        parent.repaint();
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
    @Override
    public boolean drawSprites()   { return drawSprites; }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public void advanceMap() {
        drawSprites = false;
        if (!systemsToAllocate.isEmpty()) {
            systemsToAllocate.clear();
            orderedSystems.clear();
        }
        parent.hideDisplayPanel();
        parent.resumeTurn();
    }
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
        int s3 = BasePanel.s3;
        int s7 = BasePanel.s7;
        int s10 = BasePanel.s10;
        int s15 = BasePanel.s15;
        int s20 = BasePanel.s20;
        int s25 = BasePanel.s25;
        int s30 = BasePanel.s30;
        int s35 = BasePanel.s35;

        int x0 = scaled(530);
        int y0 = scaled(215);
        int w0 = scaled(420);
        int h0 = scaled(235);
        g.setColor(MainUI.paneShadeC2);
        g.fillRect(x0, y0, w0, h0);

        int x1 = x0 + s7;
        int y1 = y0 + s7;
        int w1 = w0 - s7 - s7;
        int h1 = scaled(65);
        g.setColor(MainUI.paneBackground);
        g.fillRect(x1, y1, w1, h1);

        int x2 = x1;
        int y2 = y1+h1+s3;
        int w2 = w1;
        int h2 = scaled(112);
        g.setColor(MainUI.paneBackground);
        g.fillRect(x2, y2, w2, h2);

        // draw year/turn info
        String yearStr = displayYearOrTurn();
        g.setFont(narrowFont(40));
        int sw = g.getFontMetrics().stringWidth(yearStr);
        int leftW = w1*4/9;
        int x1a = x1+((leftW-sw)/2);
        drawBorderedString(g, yearStr, 2, x1a, y1+h1-s20, SystemPanel.textShadowC, SystemPanel.orangeText);

        // draw title
        String titleStr = text("MAIN_ALLOCATE_TITLE");
        g.setFont(narrowFont(22));
        int sw1 = g.getFontMetrics().stringWidth(titleStr);
        int rightW = w1-leftW;
        int x1b = x1+leftW+((rightW-sw1)/2);
        drawShadowedString(g, titleStr, 3, x1b, y1+h1-s35, SystemPanel.textShadowC, SystemPanel.whiteText);

        // draw notice number
        String noticeStr = text("MAIN_ALLOCATE_NOTICE_NUMBER", str(systemIndex+1), str(orderedSystems.size()));
        g.setFont(narrowFont(16));
        int x1c = x1b+scaled(20);
        g.setColor(SystemPanel.blackText);
        g.drawString(noticeStr, x1c, y1+h1-s15);

        //draw arrow
        int r1 = x1+w1;
        int b1 = y2+h2;
        x[0]=r1;    x[1]=x[0]-s30;x[2]=x[1]-s25;x[3]=x[2]+s20;x[4]=x1;  x[5]=x[4];  x[6]=x[3];x[7]=x[6]-s10;x[8]=x[7]+s25;
        y[0]=b1-s20;y[1]=y[0]-s30;y[2]=y[1];    y[3]=b1-s30;  y[4]=y[3];y[5]=b1-s10;y[6]=y[5];y[7]=b1;      y[8]=b1;

        if (arrowBack == null) {
            float[] dist = {0.0f, 0.1f, 0.6f, 1.0f};
            Point2D arrowL = new Point2D.Float(x1, 0);
            Point2D arrowR = new Point2D.Float(r1, 0);
            Color[] arrowColors = {SystemPanel.orangeClear, SystemPanel.orangeClear, SystemPanel.orangeText, SystemPanel.orangeText };
            arrowBack = new LinearGradientPaint(arrowL, arrowR, dist, arrowColors);
        }
        g.setPaint(arrowBack);
        g.fillPolygon(x,y,x.length);

        //draw text in arrow
        String actionStr = text("MAIN_ALLOCATE_CHANGE_SPENDING");
        g.setFont(narrowFont(14));
        int sw2 = g.getFontMetrics().stringWidth(actionStr);
        int x1d = x[6]-sw2;
        int y1d = y[6]-scaled(5);
        g.setColor(SystemPanel.blackText);
        g.drawString(actionStr, x1d, y1d);

        // draw reasons info
        StarSystem sv = orderedSystems.get(systemIndex);
        List<String> reasons = systemsToAllocate.get(sv);
        int lineH = BasePanel.s18;
        int x2a = x2+s10;
        int y2a = y2+s20;

        //g.setFont(narrowFont(16));
        int textW = w2+x2-x2a-s10;
        g.setFont(narrowFont(16));
        for (String reason: reasons) {
            List<String> lines = this.wrappedLines(g, reason, textW);
            for (String line: lines) {
                //scaledFont(g, line, textW, 16, 12);
                g.drawString(line, x2a, y2a);
                y2a += lineH;
            }
        }

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
                prevSystemButton.mapY(y0+h0-prevSystemButton.height()-s10);
                prevSystemButton.draw(parent.map(), g);

                // draw notice number
                String notice2Str = text("MAIN_ALLOCATE_BRIEF_NUMBER", str(systemIndex+1), str(orderedSystems.size()));
                g.setFont(narrowFont(16));
                int sw4 = g.getFontMetrics().stringWidth(notice2Str);
                int x4b = prevSystemButton.mapX()+prevSystemButton.width()+s10;
                int y4b = prevSystemButton.mapY()+prevSystemButton.height()-s10;
                g.setColor(SystemPanel.blackText);
                g.drawString(notice2Str, x4b, y4b);

                parent.addNextTurnControl(nextSystemButton);
                nextSystemButton.init(this,g);
                nextSystemButton.mapX(x4b+sw4+s10);
                nextSystemButton.mapY(prevSystemButton.mapY());
                nextSystemButton.draw(parent.map(), g);
        }
    }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_N:
                //softClick();
                nextSystem();
                break;
            case KeyEvent.VK_P:
                //softClick();
                previousSystem();
                break;
            case KeyEvent.VK_F:
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_ESCAPE:
                //softClick();
                advanceMap();
                break;
            default:
                misClick();
                break;
        }
        return true;
    }
}
class PreviousSystemButtonSprite extends MapSprite {
    private LinearGradientPaint background;
    private final Color edgeC = new Color(59,59,59);
    private final Color midC = new Color(93,93,93);
    private int mapX, mapY, buttonW, buttonH;
    private MapOverlayAllocateSystems parent;

    public int mapX()         { return mapX; }
    public int mapY()         { return mapY; }
    public void mapX(int i)   { mapX = i; }
    public void mapY(int i)   { mapY = i; }

    public int width()        { return buttonW; }
    public int height()       { return buttonH; }
    private String label()    { return text("MAIN_ALLOCATE_PREV_SYSTEM"); }
    private Font font()       { return narrowFont(18); }
    public void reset()       { background = null; }

    public void init(MapOverlayAllocateSystems p, Graphics2D g)  {
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
    private MapOverlayAllocateSystems parent;

    public int mapX()         { return mapX; }
    public int mapY()         { return mapY; }
    public void mapX(int i)   { mapX = i; }
    public void mapY(int i)   { mapY = i; }

    public int width()        { return buttonW; }
    public int height()       { return buttonH; }
    private String label()    { return text("MAIN_ALLOCATE_NEXT_SYSTEM"); }
    private Font font()       { return narrowFont(18); }
    public void reset()       { background = null; }

    public void init(MapOverlayAllocateSystems p, Graphics2D g)  {
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

    private MapOverlayAllocateSystems parent;

    protected int mapX()      { return mapX; }
    protected int mapY()      { return mapY; }
    public void mapX(int i)   { selectX = mapX = i; }
    public void mapY(int i)   { selectY = mapY = i; }

    public int width()        { return buttonW; }
    public int height()       { return buttonH; }
    private String label()    { return text("MAIN_ALLOCATE_FINISHED"); }
    private Font font()       { return narrowFont(18); }
    public void reset()       { background = null; }

    public void init(MapOverlayAllocateSystems p, Graphics2D g)  {
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
