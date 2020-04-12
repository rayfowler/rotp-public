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

public class ZoomInWidgetSprite extends MapControlSprite {
    public ZoomInWidgetSprite(int xOff, int yOff, int w, int h) {
        xOffset = scaled(xOff);
        yOffset = scaled(yOff);
        width = scaled(w);
        height = scaled(h);
    }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        map.adjustZoom(-1);
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        drawBackground(map,g2);

        int cnr = BasePanel.s12;        
        g2.setColor(map.parent().backC());
        g2.fillRoundRect(startX, startY, width, height, cnr, cnr);
        
        Stroke str0 = g2.getStroke();
        g2.setStroke(str0);
        g2.setColor(Color.lightGray);

        int th = scaled(4);
        int len = width/2;
        g2.fillRect(startX+(width/2)-(th/2), startY+(height-len)/2, th, len);
        g2.fillRect(startX+(width-len)/2, startY+(height/2)-(th/2), len, th);

        g2.setStroke(str0);
        drawBorder(map,g2);
    }
}
