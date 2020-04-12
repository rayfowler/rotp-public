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
package rotp.ui.sprites;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.SystemPanel;
import rotp.ui.main.overlay.MapOverlayAdvice;

public class AdvisorOKSprite extends MapSprite {
    private LinearGradientPaint background;
    private final Color edgeC = new Color(59,59,59);
    private final Color midC = new Color(93,93,93);
    private int mapX, mapY, buttonW, buttonH;
    private int selectX, selectY, selectW, selectH;
    private MapOverlayAdvice parent;
    private boolean draw;

    public void draw(boolean b)   { draw = b; }
    public void setBounds(int x, int y, int w, int h) {
        // if w changes due to language change, then recreate gradient background
        if (w != buttonW)
            background = null;
        mapX = x;
        mapY = y;
        buttonW = w;
        buttonH = h;
    }
    public void setSelectionBounds(int x, int y, int w, int h) {
        selectX = x;
        selectY = y;
        selectW = w;
        selectH = h;
    }
    protected int mapX()      { return mapX; }
    protected int mapY()      { return mapY; }
    public void mapX(int i)   { mapX = i; }
    public void mapY(int i)   { mapY = i; }

    public void parent(MapOverlayAdvice p)  { parent = p; }

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
        if (!draw)
            return;
        if (background == null) {
            float[] dist = {0.0f, 0.5f, 1.0f};
            Point2D yesStart = new Point2D.Float(mapX, 0);
            Point2D yesEnd = new Point2D.Float(mapX+buttonW, 0);
            Color[] yesColors = {edgeC, midC, edgeC };
            background = new LinearGradientPaint(yesStart, yesEnd, dist, yesColors);
        }
        int s2 = BasePanel.s2;
        int s10 = BasePanel.s10;
        g.setFont(narrowFont(20));
        String str = text("MAIN_ADVISOR_BUTTON_OK");
        int sw = g.getFontMetrics().stringWidth(str);
        g.setColor(Color.black);
        g.fillRoundRect(mapX+s2, mapY+s2, buttonW,buttonH,s10,s10);
        g.setPaint(background);
        g.fillRoundRect(mapX, mapY, buttonW,buttonH,s10,s10);
        Color c0 = hovering ? SystemPanel.yellowText : Color.white;
        g.setColor(c0);
        Stroke prevStr = g.getStroke();
        g.setStroke(BasePanel.stroke2);
        g.drawRoundRect(mapX, mapY, buttonW,buttonH,s10,s10);
        g.setStroke(prevStr);
        int x2a = mapX+((buttonW-sw)/2);
        drawBorderedString(g, str, x2a, mapY+buttonH-s10, SystemPanel.textShadowC, c0);
    }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        if (click)
            softClick();
        parent.advanceMap();
    }
}
