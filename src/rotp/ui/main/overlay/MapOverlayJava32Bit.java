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
import java.util.List;
import rotp.model.Sprite;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.MapSprite;

public class MapOverlayJava32Bit extends MapOverlay {
    final Color edgeC = new Color(44,59,30);
    final Color midC = new Color(70,93,48);
    boolean showSprite = true;
    MainUI parent;
    OKButtonSprite okButton = new OKButtonSprite();
    public MapOverlayJava32Bit(MainUI p) {
        parent = p;
    }
    public void init() {
        okButton.reset();
        showSprite = true;
    }
    public void ok() {
        showSprite = false;
        parent.clearOverlay();
    }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public void advanceMap() { }
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
        if (!showSprite)
            return;
        int s3 = BasePanel.s3;
        int s7 = BasePanel.s7;
        int s10 = BasePanel.s10;
        int s20 = BasePanel.s20;
        int s25 = BasePanel.s25;
        int s35 = BasePanel.s35;

        int x0 = scaled(330);
        int y0 = scaled(165);
        int w0 = scaled(420);
        int h0 = scaled(335);
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
        int h2 = scaled(212);
        g.setColor(MainUI.paneBackground);
        g.fillRect(x2, y2, w2, h2);

        // draw title
        String titleStr = text("MAIN_JAVA_32BIT_TITLE");
        g.setFont(narrowFont(22));
        int sw1 = g.getFontMetrics().stringWidth(titleStr);
        int x1a = x1+(w1-sw1)/2;
        drawShadowedString(g, titleStr, 3, x1a, y1+h1-s35, SystemPanel.textShadowC, SystemPanel.whiteText);

        int lineH = BasePanel.s18;
        int x2a = x2+s10;
        int y2a = y2+s20;

        int textW = w2+x2-x2a-s10;
        String desc1 = text("MAIN_JAVA_32BIT_DESC");
        g.setFont(narrowFont(16));
        List<String> lines = wrappedLines(g, desc1, textW);
        for (String line: lines) {
            drawString(g,line, x2a, y2a);
            y2a += lineH;
        }

        y2a += s10;
        String desc2 = text("MAIN_JAVA_32BIT_DESC_2");
        g.setFont(narrowFont(16));
        lines = wrappedLines(g, desc2, textW);
        for (String line: lines) {
            drawString(g,line, x2a, y2a);
            y2a += lineH;
        }


        y2a += s10;
        String desc3 = text("MAIN_JAVA_32BIT_DESC_3");
        g.setFont(narrowFont(16));
        lines = wrappedLines(g, desc3, textW);
        for (String line: lines) {
            drawString(g,line, x2a, y2a);
            y2a += lineH;
        }


        // init and draw continue button sprite
        parent.addNextTurnControl(okButton);
        okButton.init(this,g);
        okButton.mapX(x0+s10);
        okButton.mapY(y0+h0-okButton.height()-s10);
        okButton.draw(parent.map(), g);
    }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                //softClick();
                ok();
                break;
            default:
                misClick();
                break;
        }
        return true;
    }
    class OKButtonSprite extends MapSprite {
        private LinearGradientPaint background;
        private int mapX, mapY, buttonW, buttonH;
        private MapOverlayJava32Bit parent;

        public int mapX()         { return mapX; }
        public int mapY()         { return mapY; }
        public void mapX(int i)   { mapX = i; }
        public void mapY(int i)   { mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        private String label()    { return text("MAIN_AUTOSAVE_FAILED_OK"); }
        private Font font()       { return narrowFont(18); }
        public void reset()       { background = null; }

        public void init(MapOverlayJava32Bit p, Graphics2D g)  {
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
            if (!parent.showSprite)
                return;
            if (background == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D start = new Point2D.Float(mapX, 0);
                Point2D end = new Point2D.Float(mapX+buttonW, 0);
                Color[] colors = {parent.edgeC, parent.midC, parent.edgeC };
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
            parent.ok();
        };
    }
}