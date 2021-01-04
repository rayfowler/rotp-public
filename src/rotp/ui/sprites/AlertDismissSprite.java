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
import java.awt.Stroke;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;

public class AlertDismissSprite extends MapSprite {
    private int mapX, mapY, buttonW, buttonH;
    private int minMapX, maxButtonW;
    private final MainUI parent;

    protected int mapX()      { return mapX; }
    protected int mapY()      { return mapY; }
    public void mapX(int i)   { mapX = i; }
    public void mapY(int i)   { mapY = i; }

    public void setBounds(int x, int y, int w, int h) {
        mapX = x;
        mapY = y;
        buttonW = w;
        buttonH = h;
    }

    public AlertDismissSprite(MainUI p)  { parent = p; }

    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
        if (!parent.showAlerts())
            return false;
        hovering = x >= mapX
                && x <= mapX+buttonW
                && y >= mapY()
                && y <= mapY()+buttonH;

        return hovering;
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g) {
        if (!parent.showAlerts())
            return;

        int w1 = BasePanel.s10;

        Stroke prev = g.getStroke();
        if (hovering)
            g.setStroke(BasePanel.stroke3);
        else
            g.setStroke(BasePanel.stroke2);

        int x1 = parent.getWidth() - scaled(24);
        int y1 = parent.getHeight() - scaled(162);

        int x2 = x1+w1;
        int y2 = y1+w1;

        g.setColor(Color.black);
        g.drawLine(x1, y1, x2, y2);
        g.drawLine(x2, y1, x1, y2);

        g.setStroke(prev);
    }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        if (!parent.showAlerts())
            return;
        if (click)
            softClick();
        minMapX = min(mapX, minMapX);
        maxButtonW = max(buttonW, maxButtonW);
        hovering = true;

        session().dismissAlert();
        map.repaint();
    }
}