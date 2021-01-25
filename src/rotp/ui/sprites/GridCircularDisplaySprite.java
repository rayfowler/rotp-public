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

public class GridCircularDisplaySprite extends MapControlSprite  {
    Color extColor, normColor;

    public GridCircularDisplaySprite(int xOff, int yOff, int w, int h) {
        xOffset = scaled(xOff);
        yOffset = scaled(yOff);
        width = scaled(w);
        height = scaled(h);
        extColor =  newColor(0,0,192,64);
        normColor = newColor(32,32,192,128);
    }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        map.toggleGridCircularDisplay();
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        drawBackground(map,g2);

        int cnr = BasePanel.s12;        
        g2.setColor(background);
        g2.fillRoundRect(startX, startY, width, height, cnr, cnr);

        Stroke str0 = g2.getStroke();
        g2.setStroke(BasePanel.stroke1);
        if (map.showGridCircular())
            g2.setColor(GalaxyMapPanel.gridLight);
        else
            g2.setColor(GalaxyMapPanel.gridDark);

        g2.setClip(startX, startY, width, height);
        int centerX= startX+(width/2);
        int centerY = startY+(height/2);

        float max = 1.4f * max(width, height);

        int s5 = scaled(5);
        for (int i=s5; i<=max; i+=s5)
            g2.drawOval(centerX-i, centerY-i, i+i, i+i);

        g2.setClip(null);
        g2.setStroke(str0);

        drawBorder(map,g2);
    }
}