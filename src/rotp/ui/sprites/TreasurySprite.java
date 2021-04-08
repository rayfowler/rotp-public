

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
import java.util.List;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.SystemPanel;


public class TreasurySprite extends MapControlSprite {
    public TreasurySprite(int xOff, int yOff, int w, int h) {
        xOffset = scaled(xOff);
        yOffset = scaled(yOff);
        width = scaled(w);
        height = scaled(h);
    }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        RotPUI.instance().selectPlanetsPanel();
        map.parent().hoveringOverSprite(null);
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        if (!map.parent().showTreasuryResearchBar())
            return;
        
        int amt = (int)player().totalReserve();
        int tax = player().empireTaxLevel();
        String label2 = amt >= 100 ? text("MAIN_TECH_RESERVE_BC", "") : "";
        String label = amt < 100 ? text("MAIN_TECH_RESERVE_BC", str(amt)) : shortFmt(amt);

        String detail;
        List<String> detailLines = null;
        int fontSize = 13;
        int labelW;
        
        int w = width;
        if (hovering) {
            if (tax == 0)
                detail = text("MAIN_TECH_RESERVE_TAX_NONE");
            else if (player().empireTaxOnlyDeveloped())
                detail = text("MAIN_TECH_RESERVE_TAX",str(tax));
            else 
                detail = text("MAIN_TECH_RESERVE_TAX_ALL",str(tax));

            g2.setFont(narrowFont(fontSize));
            labelW = g2.getFontMetrics().stringWidth(label);
            int w0 = labelW*3/5;
            detailLines = wrappedLines(g2, detail, w0);
            while (detailLines.size() > 2) {
                w0 += labelW/10;
                detailLines = wrappedLines(g2, detail, w0);
            }
            w = width+BasePanel.s15+w0;
        }
        drawBackground(map,g2,w);

        int cnr = BasePanel.s12;        
        g2.setColor(background);
        g2.fillRoundRect(startX, startY, width, height, cnr, cnr);

        if (amt < 1000)
            g2.setFont(narrowFont(15));
        else if (amt < 10000)
            g2.setFont(narrowFont(14));
        else if (amt < 100000)
            g2.setFont(narrowFont(13));
        else
            g2.setFont(narrowFont(12));
        
        g2.setColor(SystemPanel.orangeText);
        if (label2.isEmpty()) {
            int sw = g2.getFontMetrics().stringWidth(label);
            int x0 = startX+((width-sw)/2);
            drawString(g2,label, x0, startY+height-BasePanel.s10);
        }
        else {
            int sw = g2.getFontMetrics().stringWidth(label);
            int x0 = startX+((width-sw)/2);
            drawString(g2,label, x0, startY+height-BasePanel.s18);
            sw = g2.getFontMetrics().stringWidth(label2);
            x0 = startX+((width-sw)/2);
            drawString(g2,label2, x0, startY+height-BasePanel.s4);
        }
        
        if (hovering) {
            g2.setColor(Color.lightGray);
            g2.setFont(narrowFont(fontSize));
            int y1 = startY+height-BasePanel.s17;
            int x1 = startX+width+BasePanel.s10;
            if (detailLines.size() == 1)
                y1 += BasePanel.s8;
            for (String line: detailLines) {
                drawString(g2,line, x1, y1);
                y1 += BasePanel.s14;
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
        
        
        if (hovering) {
            g2.setStroke(BasePanel.stroke2);
            g2.setColor(SystemPanel.yellowText);
            g2.drawRoundRect(startX, startY, w, height, cnr, cnr);
            g2.setStroke(str0);
        }
    }
}
