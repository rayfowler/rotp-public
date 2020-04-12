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
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;

public class SystemNameDisplaySprite  extends MapControlSprite  {
    static Color greenC = Color.green;
    static Color darkGreenC = new Color(0,128,0);
    static FlightPathSprite sprite = new FlightPathSprite();

    Color extColor, normColor;

    public SystemNameDisplaySprite(int xOff, int yOff, int w, int h) {
        xOffset = scaled(xOff);
        yOffset = scaled(yOff);
        width = scaled(w);
        height = scaled(h);
        extColor =  newColor(0,0,192,64);
        normColor = newColor(32,32,192,128);
    }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        map.toggleSystemNameDisplay();
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        StarSystem home = galaxy().system(player().capitalSysId());
        drawBackground(map,g2);
        int x2 = startX+width/2;
        int y2 = startY+scaled(5);

        Color c0 = g2.getColor();
        int cnr = BasePanel.s12;        
        g2.setColor(background);
        g2.fillRoundRect(startX, startY, width, height, cnr, cnr);

        int s2 = scaled(2);
        int s4 = scaled(4);
        g2.setClip(startX+s2,startY+s2,width-s4,height-s4);
        home.drawStar(map, g2, x2, y2);

        g2.setFont(narrowFont(16));

        Color textC = map.showSystemNames() ? greenC : darkGreenC;

        g2.setColor(textC);
        String name = map.parent().systemLabel(home);
        int sw = g2.getFontMetrics().stringWidth(name);

        g2.drawString(name, x2-(sw/2), y2+scaled(22));
        g2.setClip(null);
        g2.setColor(c0);
        drawBorder(map, g2);
    }
}
