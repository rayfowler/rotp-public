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
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.border.Border;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.model.planet.PlanetType;
import rotp.ui.BasePanel;

public class ExploredSystemPanel extends SystemPanel {
    private static final long serialVersionUID = 1L;
    static Border topPaneBorder;
    static Border detailPaneBorder;

    static final Color paneBackground = new Color(123,123,123);

    @Override
    protected void showDefaultDetail() { }
    @Override
    protected void showStarDetail()   { }
    @Override
    protected void showPlanetDetail()   { }

    public ExploredSystemPanel(SpriteDisplayPanel p) {
        parentSpritePanel = p;
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
        return new ExploredDetailPane(this);
    }
    private class ExploredDetailPane extends BasePanel {
        private static final long serialVersionUID = 1L;
        SystemPanel parent;
        private Shape textureClip;

        ExploredDetailPane(SystemPanel p) {
            parent = p;
            init();
        }
        private void init() {
            setOpaque(false);
        }
        @Override
        public String textureName()            { return TEXTURE_GRAY; }
        @Override
        public Shape textureClip()    { return textureClip; }
        @Override
        public void paintComponent(Graphics g) {
            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;
            Empire pl = player();

            super.paintComponent(g);
            int h = getHeight();
            int w = getWidth();

            int topH = s45;
            // draw "UNCOLONIZED" box
            g.setColor(MainUI.paneBackground());
            g.fillRect(0, 0, w, topH-s5);
            g.setColor(MainUI.shadeBorderC());
            g.fillRect(0, topH-s5, w, s6);

            textureClip = new Rectangle2D.Float(0,0,w,topH-s5);

            String label;
            
            if (sys.planet().isEnvironmentNone())
                label = text("MAIN_NO_PLANETS");
            else if (sys.abandoned())
                label = text("MAIN_ABANDONED");
            else
                label = text("MAIN_NO_COLONIES");
            g.setFont(narrowFont(24));
            drawShadowedString(g, label, 2, s10, topH-s15, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);

            // draw planet terrain background
            PlanetType pt = sys.planet().type();

            if (pt != null) {
                BufferedImage img = pt.terrainImage();
                if (img != null)
                    g.drawImage(img, 0, topH, w, h, 0, 0, img.getWidth(), img.getHeight(), null);
                g.setFont(narrowFont(16));
                g.setColor(grayText);
                String desc = pt.description(player());
                List<String> descLines =  wrappedLines(g, text(desc), getWidth()-s12);

                int ydelta = s18;
                int y0=h-s8-(ydelta*(descLines.size()-1));
                for (String line: descLines) {
                    drawBorderedString(g, line, s8, y0, Color.black, whiteText);
                    y0 += ydelta;
                }
            }
        }
    }
}
