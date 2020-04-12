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
import rotp.ui.main.SystemPanel;

public abstract class MapControlSprite extends MapSprite {
    protected static Color background = new Color(0,0,0);
    protected int xOffset, yOffset, width, height;
    protected int startX, startY;

    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int mapX, int mapY) {
        int s5 = scaled(3);
        hovering = mapX >= startX-s5
            && mapX <= startX+width+s5+s5
            && mapY >= startY-s5
            && mapY <= startY+height+s5+s5;

        return hovering;
    }
    public void drawBackground(GalaxyMapPanel map, Graphics2D g2) {
        startX = xOffset >= 0 ? xOffset : map.getWidth()+xOffset;
        startY = yOffset >= 0 ? yOffset : map.getHeight()+yOffset;
        int s5 = scaled(5);
        g2.setColor(map.parent().shadeC());
        g2.fillRect(startX-s5, startY-s5, width+s5+s5, height+s5+s5);
    }
    public void drawBorder(GalaxyMapPanel map, Graphics2D g2) {
        Stroke str0 = g2.getStroke();

        if (hovering) {
            g2.setStroke(BasePanel.stroke2);
            g2.setColor(SystemPanel.yellowText);
        }
        else {
            g2.setStroke(BasePanel.stroke1);
            g2.setColor(map.parent().backC());
        }
        int cnr = BasePanel.s12;
        g2.drawRoundRect(startX, startY, width, height, cnr, cnr);
        g2.setStroke(str0);
    }
}