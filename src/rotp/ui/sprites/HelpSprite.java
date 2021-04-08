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
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;

public class HelpSprite extends MapSprite {
    private int mapX, mapY, buttonW, buttonH;
    private int minMapX, maxButtonW;
    private final BasePanel parent;

    protected int mapX()      { return mapX; }
    protected int mapY()      { return mapY; }
    public void mapX(int i)   { mapX = i; }
    public void mapY(int i)   { mapY = i; }

    private void setBounds(int x, int y, int w, int h) {
        mapX = x;
        mapY = y;
        buttonW = w;
        buttonH = h;
    }

    public HelpSprite(BasePanel p)  { 
        parent = p; 
        int x0 = BasePanel.s10;
        int y0 = BasePanel.s10;
        int w0 = BasePanel.s20;
        int h0 = BasePanel.s25;

        setBounds(x0,y0,w0,h0);
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
        int x1 = BasePanel.s16;
        int y1 = BasePanel.s30;
        
        g.setColor(new Color(100,100,255,100));
        g.fillOval(mapX, mapY, buttonW, buttonH);
        g.setFont(narrowFont(25));
        if (hovering)
            g.setColor(Color.yellow);
        else
            g.setColor(Color.white);
            
        drawString(g,"?", x1, y1);
    }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        if (click)
            softClick();
        minMapX = min(mapX, minMapX);
        maxButtonW = max(buttonW, maxButtonW);
        hovering = true;
        parent.showHelp();
    }
}

