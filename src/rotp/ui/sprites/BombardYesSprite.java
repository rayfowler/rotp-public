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
import rotp.ui.main.overlay.MapOverlayBombardPrompt;

public class BombardYesSprite extends MapSprite {
    private LinearGradientPaint background;
    private final Color edgeC = new Color(44,59,30);
    private final Color midC = new Color(70,93,48);
    private int mapX, mapY, buttonW, buttonH;
    private MapOverlayBombardPrompt parent;

    public void setBounds(int x, int y, int w, int h) {
        mapX = x;
        mapY = y;
        buttonW = w;
        buttonH = h;
    }
    protected int mapX()      { return mapX; }
    protected int mapY()      { return mapY; }
    public void mapX(int i)   { mapX = i; }
    public void mapY(int i)   { mapY = i; }
    public void reset()       { background = null; }

    public void parent(MapOverlayBombardPrompt p)  { parent = p; }

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
            Point2D yesStart = new Point2D.Float(mapX, 0);
            Point2D yesEnd = new Point2D.Float(mapX+buttonW, 0);
            Color[] yesColors = {edgeC, midC, edgeC };
            background = new LinearGradientPaint(yesStart, yesEnd, dist, yesColors);
        }
        int s5 = scaled(5);
        int s10 = scaled(10);
        g.setFont(narrowFont(20));
        String str = text("MAIN_BOMBARD_YES");
        int sw = g.getFontMetrics().stringWidth(str);
        g.setPaint(background);
        g.fillRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
        Color c0 = hovering ? SystemPanel.yellowText : Color.white;
        g.setColor(c0);
        Stroke prevStr = g.getStroke();
        g.setStroke(BasePanel.stroke2);
        g.drawRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
        g.setStroke(prevStr);
        int x2a = mapX+((buttonW-sw)/2);
        drawBorderedString(g, str, x2a, mapY+buttonH-s10, SystemPanel.textShadowC, c0);
    }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        if (click)
            softClick();
        parent.bombardYes();
    }
}
