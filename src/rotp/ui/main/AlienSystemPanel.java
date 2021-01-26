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
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.SwingUtilities;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;

public class AlienSystemPanel extends SystemPanel {
    private static final long serialVersionUID = 1L;
    static final Color textColor = new Color(20,20,20);
    static final Color dataBorders = new Color(160,160,160);

    public AlienSystemPanel(SpriteDisplayPanel p) {
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
        return new DetailPane(this);
    }
    private class DetailPane extends BasePanel implements MouseMotionListener, MouseListener {
        private static final long serialVersionUID = 1L;
        SystemPanel parent;
        Shape textureClip;
        Rectangle flagBox = new Rectangle();
        Shape hoverBox;

        DetailPane(SystemPanel p) {
            parent = p;
            init();
        }
        private void init() {
            setOpaque(false);
            addMouseMotionListener(this);
            addMouseListener(this);
        }
        @Override
        public String textureName()            { return TEXTURE_GRAY; }
        @Override
        public Shape textureClip()      { return textureClip; }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;

            int id = sys.id;
            Empire pl = player();
            Empire sysEmp = pl.sv.empire(id);
            if (sysEmp == null)
                return;

            boolean spied = pl.sv.isSpied(id);

            super.paintComponent(g);
            int h = getHeight();
            int w = getWidth();

            int topH1 = s40;
            int topH = s90;
            // draw colony info box
            g.setColor(MainUI.paneBackground());
            g.fillRect(0, 0, w, topH-s5);
            GradientPaint back = new GradientPaint(0,0,sysEmp.color(),w, 0,MainUI.transC);
            g.setPaint(back);
            g.fillRect(0, 0, w, topH1-s5);
            g.setPaint(null);
            g.setColor(MainUI.shadeBorderC());
            g.fillRect(0, topH-s5, w, s6);

            textureClip = new Rectangle2D.Float(0,0,w,topH-s5);

            //  colony name
            g.setFont(narrowFont(24));
            drawShadowedString(g, pl.sv.descriptiveName(id), 2, s10, topH1-s15, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);

            // draw system banner
            Color flagC = parentSpritePanel.parent.flagColor(sys);
            if (hoverBox == flagBox) 
                sys.drawBanner(g, flagC, SystemPanel.yellowText, w-s10,topH1);
            else {
                Color c1 = flagC == null ? SystemPanel.blackText : SystemPanel.whiteText;
                sys.drawBanner(g, flagC, c1, w-s10,topH1);
            }
            flagBox.setBounds(w-s30,topH1-s60,s20,s60);
            
            // colony data
            String unknown = text("RACES_UNKNOWN_DATA");
            String factLbl = text("MAIN_COLONY_FACTORIES");
            String baseLbl = text("MAIN_COLONY_BASES");
            String shieldLbl = text("MAIN_COLONY_SHIELD");
            String popLbl = text("MAIN_COLONY_POPULATION");

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

            String str = spied ? str(pl.sv.population(id)) : unknown;
            int sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x1-sw-s10, y0);
            str = spied ? str(pl.sv.factories(id)) : unknown;
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, w-s10-sw, y0);
            str = spied ? str(pl.sv.shieldLevel(id)) : unknown;
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x1-s10-sw, y1);
            str = spied ? str(pl.sv.bases(id)) : unknown;
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, w-s10-sw, y1);

            // draw borders around data
            g.setColor(dataBorders);
            Stroke prevStroke = g.getStroke();
            g.setStroke(stroke1);
            //g.drawLine(0, y0-s18, w, y0-s18);
            g.drawLine(0, y1-s18, w, y1-s18);
            g.drawLine(x1-s5, y0-s18, x1-s5, topH-s6);
            g.setStroke(prevStroke);

            // draw planet terrain background
            BufferedImage img = pl.sv.planetTerrain(id);
            g.drawImage(img, 0, topH, w, h, 0, 0, img.getWidth(), img.getHeight(), null);
            g.setFont(narrowFont(16));
            g.setColor(grayText);

            String desc = pl.sv.planetType(id).description(pl);
            List<String> descLines =  wrappedLines(g, text(desc), getWidth()-s12);

            int ydelta = s18;
            int y2=h-s8-(ydelta*(descLines.size()-1));
            for (String line: descLines) {
                drawBorderedString(g, line, s8, y2, Color.black, whiteText);
                y2 += ydelta;
            }
        }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Shape prevHover = hoverBox;
            hoverBox = null;
            if (flagBox.contains(x,y))
                hoverBox = flagBox;

            if (prevHover != hoverBox)
                repaint();
        }
        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mousePressed(MouseEvent e) { }
        @Override
        public void mouseReleased(MouseEvent e) {
            boolean rightClick = SwingUtilities.isRightMouseButton(e);
            if (hoverBox == flagBox) {
                StarSystem sys = parentSpritePanel.systemViewToDisplay();
                player().sv.view(sys.id).toggleFlagColor(rightClick);
                parentSpritePanel.parent.repaint();
            }
        }
        @Override
        public void mouseEntered(MouseEvent e) { }
        @Override
        public void mouseExited(MouseEvent e) { 
            if (hoverBox != null) {
                hoverBox = null;
                repaint();
            }
        }
    }
}
