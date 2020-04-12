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
package rotp.ui.main;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;

public class UnexploredAlienSystemPanel extends SystemPanel {
    private static final long serialVersionUID = 1L;
    static final Color textColor = new Color(20,20,20);
    static final Color dataBorders = new Color(160,160,160);
    static final Color transC = new Color(0,0,0,0);

    public UnexploredAlienSystemPanel(SpriteDisplayPanel p) {
        parentSpritePanel = p;
        init();
    }
    private void init() {
        initModel();
    }
    @Override
    public void animate()            { overviewPane.animate(); }
    @Override
    protected BasePanel topPane()    { return new SystemViewInfoPane(this); }
    @Override
    protected BasePanel bottomPane() { return new SystemRangePane(this); }
    @Override
    protected BasePanel detailPane() {
        return new DetailPane(this);
    }
    private class DetailPane extends BasePanel {
        private static final long serialVersionUID = 1L;
        SystemPanel parent;
        Shape textureClip;

        DetailPane(SystemPanel p) {
            parent = p;
            init();
        }
        private void init() {
            setOpaque(true);
        }
        @Override
        public String textureName()     { return TEXTURE_GRAY; }
        @Override
        public Shape textureClip()      { return textureClip; }
        @Override
        public Color starBackgroundC()  { return SystemPanel.starBackgroundC; }
        @Override
        public boolean hasStarBackground()     { return true; }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;
            int id = sys.id;
            Empire pl = player();

            super.paintComponent(g);
            int h = getHeight();
            int w = getWidth();

            int topH1 = s40;
            int topH = s90;
            // draw colony info box
            g.setColor(MainUI.paneBackground());
            g.fillRect(0, 0, w, topH-s5);
            GradientPaint back = new GradientPaint(0,0,pl.sv.empire(id).color(),w, 0,transC);
            g.setPaint(back);
            g.fillRect(0, 0, w, topH1-s5);
            g.setPaint(null);
            g.setColor(MainUI.shadeBorderC());
            g.fillRect(0, topH-s5, w, s6);

            textureClip = new Rectangle2D.Float(0,0,w,topH-s5);

            //  colony name
            g.setFont(narrowFont(24));
            drawShadowedString(g, pl.sv.descriptiveName(id), 2, s10, topH1-s15, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);

            // colony data
            String unknown = text("RACES_UNKNOWN_DATA");
            String factLbl = text("MAIN_COLONY_FACTORIES");
            String baseLbl = text("MAIN_COLONY_BASES");
            String shieldLbl = text("MAIN_COLONY_SHIELD");
            String popLbl = text("MAIN_COLONY_POPULATION");

            boolean spied = pl.sv.isSpied(id);

            int x0 = s5;
            int x1 = w/2;
            int y0 = topH-s37;
            int y1 = topH-s12;

            g.setFont(narrowFont(16));
            g.setColor(textColor);
            g.drawString(popLbl, x0, y0);
            g.drawString(factLbl, x1, y0);
            g.drawString(shieldLbl, x0, y1);
            g.drawString(baseLbl, x1, y1);

            String str1 = spied ? str(pl.sv.population(id)) : unknown;
            int sw1 = g.getFontMetrics().stringWidth(str1);
            g.drawString(str1, x1-sw1-s10, y0);
            String str2 = spied ? str(pl.sv.factories(id)) : unknown;
            int sw2 = g.getFontMetrics().stringWidth(str2);
            g.drawString(str2, w-s10-sw2, y0);
            String str3 = spied ? str(pl.sv.shieldLevel(id)) : unknown;
            int sw3 = g.getFontMetrics().stringWidth(str3);
            g.drawString(str3, x1-s10-sw3, y1);
            String str4 = spied ? str(pl.sv.bases(id)) : unknown;
            int sw4 = g.getFontMetrics().stringWidth(str4);
            g.drawString(str4, w-s10-sw4, y1);

            // draw borders around data
            g.setColor(dataBorders);
            Stroke prevStroke = g.getStroke();
            g.setStroke(stroke1);
            //g.drawLine(0, y0-s18, w, y0-s18);
            g.drawLine(0, y1-s18, w, y1-s18);
            g.drawLine(x1-s5, y0-s18, x1-s5, topH-s6);
            g.setStroke(prevStroke);

            // draw planet terrain background
            drawStar(g, sys.starType(), s40, w/2, h/2);

            g.setFont(narrowFont(16));
            g.setColor(grayText);
            List<String> descLines =  wrappedLines(g, text(sys.starType().description()), getWidth()-s12);

            int ydelta = s18;
            y0=h-s8-(ydelta*(descLines.size()-1));
            for (String line: descLines) {
                g.drawString(line, s8, y0);
                y0 += ydelta;
            }
        }
    }
}
