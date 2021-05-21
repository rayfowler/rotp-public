

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
import rotp.ui.RotPUI;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.SystemPanel;


public class SpyReportSprite extends MapControlSprite {
    static LinearGradientPaint alertBack;
    public SpyReportSprite(int xOff, int yOff, int w, int h) {
        xOffset = scaled(xOff);
        yOffset = scaled(yOff);
        width = scaled(w);
        height = scaled(h);
    }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        RotPUI.instance().showSpyReport();
        map.parent().hoveringOverSprite(null);
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        if (!map.parent().showSpyReportIcon())
            return;
        
        String label = text("MAIN_SPY_REPORT");
     
        drawBackground(map,g2,width);

//        int cnr = BasePanel.s12;        
//        g2.setColor(background);
//        g2.fillRoundRect(startX, startY, width, height, cnr, cnr);
        g2.setFont(narrowFont(15)); 
        if (session().spyActivity())
            g2.setColor(SystemPanel.blackText);
        else
            g2.setColor(SystemPanel.blackText);

        int sw = g2.getFontMetrics().stringWidth(label);
        int x0 = startX+((width-sw)/2);
        drawString(g2,label, x0, startY+height-BasePanel.s10);

///        drawBorder(map,g2,width);
    }
    private void drawBackground(GalaxyMapPanel map, Graphics2D g2, int w) {
        startX = xOffset >= 0 ? xOffset : map.getWidth()+xOffset;
        startY = yOffset >= 0 ? yOffset : map.getHeight()+yOffset;
        int s5 = scaled(5);
        if (alertBack == null) {
            float[] dist = {0.0f, 1.0f};
            Color topC = new Color(219,135,8);
            Color botC = new Color(254,174,45);
            Point2D start = new Point2D.Float(0, startY);
            Point2D end = new Point2D.Float(0, startY+height);
            Color[] colors = {topC, botC };
            alertBack = new LinearGradientPaint(start, end, dist, colors);
        }
        g2.setPaint(alertBack);
        g2.fillRect(startX, startY, w, height);
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
