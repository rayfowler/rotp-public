
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
import rotp.model.tech.Tech;
import rotp.model.tech.TechCategory;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.SystemPanel;

public class TechStatusSprite extends MapControlSprite {
    private static  final Color blueBucketC = new Color(100,100,255);
    private static final Color blueBucketBackC = new Color(50,50,128);
    private final int category;
    public TechStatusSprite(int catNum, int xOff, int yOff, int w, int h) {
        category = catNum;
        xOffset = scaled(xOff);
        yOffset = scaled(yOff);
        width = scaled(w);
        height = scaled(h);
    }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        RotPUI.instance().selectTechPanel();
        RotPUI.instance().techUI().selectTechCategory(category);
        map.parent().hoveringOverSprite(null);
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        if (!map.parent().showTreasuryResearchBar())
            return;
        
        boolean show = hovering || map.parent().showAllCurrentResearch();
        
        TechCategory cat = player().tech().category(category);
        Tech tech = tech(cat.currentTech());
        String label = "";
        String label2 = "";
        int fontSize1 = 14;
        int fontSize2 = 11;
        int labelW1 = 0;
        int labelW2 = 0;
        
        int w = width;
        if (show) {
            if (tech == null)
                label = text("MAIN_TECH_NONE");
            else if (cat.researchCompleted())
                label = text("MAIN_TECH_COMPLETED");
            else {
                label = tech.name(); 
                int costRP = (int) Math.ceil(cat.costForTech(tech) - cat.totalBC());
                if (costRP > 0)
                    label2 = text("MAIN_TECH_RP_REMAINING",costRP); 
                else 
                    label2 = text("MAIN_TECH_RP_DISCOVERY");
            }
            g2.setFont(narrowFont(fontSize1));
            labelW1 = g2.getFontMetrics().stringWidth(label);
            g2.setFont(narrowFont(fontSize2));
            labelW2 = g2.getFontMetrics().stringWidth(label2);            
            w = width+BasePanel.s15+max(labelW1, labelW2);
        }
        drawBackground(map,g2,w);

        int cnr = BasePanel.s12;        
        g2.setColor(background);
        g2.fillRoundRect(startX, startY, width, height, cnr, cnr);

        
        if (tech != null)
            RotPUI.instance().techUI().drawResearchBubble(g2, cat, true, Color.lightGray, blueBucketC, blueBucketBackC, startX+BasePanel.s1+(width/2), startY+BasePanel.s7+(height/2));

        if (show) {
            g2.setColor(Color.lightGray);
            int y1 = startY+height-BasePanel.s18;
            int x1 = startX+width+BasePanel.s10;
            if (label2.isEmpty())
                y1 += BasePanel.s7;
            g2.setFont(narrowFont(fontSize1));
            drawString(g2,label, x1, y1);
            if (!label2.isEmpty()) {
                int y2 = startY+height-BasePanel.s5;
                int x2 = labelW2 >= labelW1 ? x1 : x1 + ((labelW1-labelW2)/2);
                g2.setFont(narrowFont(fontSize2));
                drawString(g2,label2, x2, y2);
            }
        }
        drawBorder(map,g2,w);
    }
    private void drawBackground(GalaxyMapPanel map, Graphics2D g2, int w) {
        startX = xOffset >= 0 ? xOffset : map.getWidth()+xOffset;
        startY = yOffset >= 0 ? yOffset : map.getHeight()+yOffset;
        int s5 = scaled(5);
        g2.setColor(map.parent().shadeC());
        g2.fillRect(startX-s5, startY-s5, w+s5+s5, height+s5+s5);
    }
    private void drawBorder(GalaxyMapPanel map, Graphics2D g2, int w) {
        Stroke str0 = g2.getStroke();

        int cnr = BasePanel.s12;
        
        g2.setStroke(BasePanel.stroke1);
        g2.setColor(map.parent().backC());
        g2.drawRoundRect(startX, startY, width, height, cnr, cnr);
        
        boolean show = hovering || map.parent().showAllCurrentResearch();
        
        if (show) {
            g2.setStroke(BasePanel.stroke2);
            g2.setColor(SystemPanel.yellowText);
            g2.drawRoundRect(startX, startY, w, height, cnr, cnr);
            g2.setStroke(str0);
        }
    }
}
