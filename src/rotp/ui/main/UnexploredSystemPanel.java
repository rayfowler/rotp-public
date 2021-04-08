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
import java.awt.Graphics2D;
import java.util.List;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import static rotp.ui.main.SystemPanel.grayText;

public class UnexploredSystemPanel extends SystemPanel {
    private static final long serialVersionUID = 1L;

    public UnexploredSystemPanel(SpriteDisplayPanel p) {
        parentSpritePanel = p;
        init();
    }
    private void init() {
        initModel();
    }
    @Override
    protected BasePanel topPane() { return null; }
    @Override
    protected BasePanel bottomPane() {
        return new SystemRangePane(this);
    }
    @Override
    protected BasePanel detailPane() {
        return new UnexploredDetailPane(parentSpritePanel);
    }
    public class UnexploredDetailPane  extends BasePanel {
        private static final long serialVersionUID = 1L;
        SpriteDisplayPanel parent;
        UnexploredDetailPane(SpriteDisplayPanel p) {
            parent = p;
            init();
        }
        private void init() {
            setOpaque(true);
            setBackground(Color.black);
        }
        @Override
        public Color starBackgroundC()  { return SystemPanel.starBackgroundC; }
        @Override
        public boolean hasStarBackground()     { return true; }
        @Override
        public void paintComponent(Graphics g) {
            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;

            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();

            
            if (sys.inNebula()) {
                g.setColor(SystemPanel.nebulaC);
                g.fillRect(0,0,w,h);
            }
                
            Graphics2D g2 = (Graphics2D) g;
            drawStar(g2, sys.starType(), s40, getWidth()/2, getHeight()/2);

            g.setFont(narrowFont(36));
            String label = text("MAIN_UNEXPLORED_SYSTEM");
            drawBorderedString(g, label, 2, s25, s42, Color.black, SystemPanel.orangeText);
            
            if (sys.inNebula()) {
                g.setFont(narrowFont(16));
                g.setColor(grayText);
                List<String> nebLines =  wrappedLines(g, text("MAIN_NEBULA_DESC"), getWidth()-s12);
                int ydelta = s18;
                int y0=s70;
                for (String line: nebLines) {
                    drawString(g,line, s8, y0);
                    y0 += ydelta;
                }
            } 
           

            g.setFont(narrowFont(16));
            g.setColor(grayText);
            List<String> descLines =  wrappedLines(g, text(sys.starType().description()), getWidth()-s12);

            int ydelta = s18;
            int y0=h-s8-(ydelta*(descLines.size()-1));
            for (String line: descLines) {
                drawString(g,line, s8, y0);
                y0 += ydelta;
            }
        }
    }
}
