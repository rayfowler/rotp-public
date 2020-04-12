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
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JPanel;
import rotp.model.colony.Colony;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.SystemViewer;

public class EmpireColonyInfoPane extends BasePanel {
    private static final long serialVersionUID = 1L;
    static final Color enabledArrowColor = Color.black;
    static final Color disabledArrowColor = new Color(65,65,65);
    static final Color sliderHighlightColor = new Color(255,255,255);
    static final Color productionGreenColor = new Color(89, 240, 46);
    static final Color dataBorders = new Color(160,160,160);
    static final Color textColor = new Color(20,20,20);

    Color borderC;
    Color darkC;
    Color textC;
    Color backC;
    SystemViewer parentUI;
    public EmpireColonyInfoPane(SystemViewer p, Color backColor, Color borderColor, Color textColor, Color darkTextColor) {
        parentUI = p;
        borderC = borderColor;
        darkC = darkTextColor;
        textC = textColor;
        backC = backColor;
        init(borderColor);
    }
    private void init(Color c0) {
        setBackground(c0);

        setOpaque(true);
        JPanel popFactoriesPane = new JPanel();
        popFactoriesPane.setOpaque(false);

        GridLayout layout1 = new GridLayout(0,2);
        layout1.setHgap(s1);
        popFactoriesPane.setLayout(layout1);
        popFactoriesPane.add(new EmpirePopPane());
        popFactoriesPane.add(new EmpireFactoriesPane());

        JPanel shieldBasesPane = new JPanel();
        shieldBasesPane.setOpaque(false);

        GridLayout layout2 = new GridLayout(0,2);
        layout2.setHgap(s1);
        shieldBasesPane.setLayout(layout2);
        shieldBasesPane.add(new EmpireShieldPane());
        shieldBasesPane.add(new EmpireBasesPane());

        GridLayout layout0 = new GridLayout(3,0);
        layout0.setVgap(s1);
        setLayout(layout0);
        add(popFactoriesPane);
        add(shieldBasesPane);
        add(new EmpireProductionPane());
    }
    abstract class EmpireDataPane extends BasePanel {
        private static final long serialVersionUID = 1L;
        protected Shape hoverBox;
        protected Rectangle basesBox = new Rectangle();
        EmpireDataPane() {
            init();
        }
        private void init() {
            setOpaque(true);
            setBackground(backC);
        }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            StarSystem sys = parentUI.systemViewToDisplay();
            if (sys == null)
                return;
            Colony c = sys.colony();
            if (c == null)
                return;
            super.paintComponent(g);

            String strTitle = titleString();
            String strDataLabel = dataLabelString(c);
            String strData1 = valueString(c);
            String strData2 = value(c) == maxValue(c) ? "" : concat("/", maxValueString(c));

            int x0 = s5;
            int y0 = getHeight()-s6;

            g.setFont(narrowFont(16));
            int sw0 = strDataLabel == null ? 0 : g.getFontMetrics().stringWidth(strDataLabel);
            int sw1 = g.getFontMetrics().stringWidth(strData1)+s1;
            int sw2 = g.getFontMetrics().stringWidth(strData2);
            int x2 = getWidth()-rightMargin()-sw2;
            int x1 = x2-sw1;
            if (sw0 > 0)
                x1 = getWidth()-rightMargin()-sw0;

            // calc max width for label and try to get largest font (from 13-16) in it
            int titleMaxW = x1-x0-s2;
            int fontSize = 16;
            g.setFont(narrowFont(fontSize));
            while ((g.getFontMetrics().stringWidth(strTitle) > titleMaxW) && (fontSize > 12)) {
                fontSize--;
                g.setFont(narrowFont(fontSize));
            }
            g.setColor(textColor);
            g.drawString(strTitle, x0, y0);

            if (strDataLabel != null) {
                drawShadowedString(g, strDataLabel, 1, x1, y0, darkC, textC);
            }
            else {
                drawShadowedString(g, strData1, 1, x1, y0, darkC, textC);
                g.setColor(darkC);
                g.drawString(strData2, x2, y0);
                basesBox.setBounds(x1-s3,y0-s12,(x2-x1)+sw2+s6,s15);
                if (hoverBox == basesBox) {
                    Stroke prevStroke = g.getStroke();
                    g.setStroke(stroke2);
                    g.setColor(SystemPanel.yellowText);
                    g.drawRect(x1-s3,y0-s12,(x2-x1)+sw2+s4,s15);
                    g.setStroke(prevStroke);
                }
            }
        }
        protected int rightMargin()   { return s5; }
        protected String valueString(Colony c)  { return str(value(c)); }
        protected String maxValueString(Colony c) { return str(maxValue(c)); }
        protected String dataLabelString(Colony c)   { return null; }
        abstract protected String titleString();
        abstract int value(Colony c);
        abstract int maxValue(Colony c);
    }
    class EmpirePopPane extends EmpireDataPane {
        private static final long serialVersionUID = 1L;
        @Override
        public String textureName()            { return parentUI.subPanelTextureName(); }
        @Override
        protected String titleString()      { return text("MAIN_COLONY_POPULATION"); }
        @Override
        protected int value(Colony c)       { return (int) (c == null ? 0 : c.displayPopulation()); }
        @Override
        protected int maxValue(Colony c)    { return (int) (c == null ? 0 : c.maxSize()); }
    }
    class EmpireFactoriesPane extends EmpireDataPane {
        private static final long serialVersionUID = 1L;
        @Override
        public String textureName()            { return parentUI.subPanelTextureName(); }
        @Override
        protected String titleString()   { return text("MAIN_COLONY_FACTORIES"); }
        @Override
        protected int value(Colony c)    { return (int) (c == null ? 0 : c.industry().factories()); }
        @Override
        protected int maxValue(Colony c) { return (int) (c == null ? 0 : c.industry().maxFactories()); }
    }
    class EmpireShieldPane extends EmpireDataPane {
        private static final long serialVersionUID = 1L;
        @Override
        public String textureName()            { return parentUI.subPanelTextureName(); }
        @Override
        protected String titleString()   { return text("MAIN_COLONY_SHIELD"); }
        @Override
        protected int value(Colony c)    { return c == null ? 0 : c.defense().shieldLevel(); }
        @Override
        protected int maxValue(Colony c) { return (int) (c == null ? 0 : c.defense().maxShieldLevel()); }
        @Override
        protected String dataLabelString(Colony c)   { return (c != null) && c.starSystem().inNebula() ? text("MAIN_COLONY_NO_SHIELD") : null; }
    }
    class EmpireBasesPane extends EmpireDataPane  implements MouseListener, MouseMotionListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        private final Polygon upArrow = new Polygon();
        private final Polygon downArrow = new Polygon();
        public EmpireBasesPane() {
            super();
            init();
        }
        private void init() {
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }
        private final int upButtonX[] = new int[3];
        private final int upButtonY[] = new int[3];
        private final int downButtonX[] = new int[3];
        private final int downButtonY[] = new int[3];
        private void incrementBases() {
            StarSystem sys = parentUI.systemViewToDisplay();
            if (sys == null)
                return;
            Colony colony = sys.colony();
            if  (colony == null)
                return;
            boolean updated = colony.defense().incrementMaxBases();
            if (updated)
                softClick();
            else
                misClick();
            repaint();
        }
        private void decrementBases() {
            StarSystem sys = parentUI.systemViewToDisplay();
            if (sys == null)
                return;
            Colony colony = sys.colony();
            if  (colony == null)
                return;
            boolean updated = colony.defense().decrementMaxBases();
            if (updated)
                softClick();
            else
                misClick();
            repaint();
        }
        @Override
        public String textureName()      { return parentUI.subPanelTextureName(); }
        @Override
        protected int rightMargin()      { return s20; }
        @Override
        protected String titleString()   { return text("MAIN_COLONY_BASES"); }
        @Override
        protected int value(Colony c)    { return c == null ? 0 : (int)c.defense().bases(); }
        @Override
        protected int maxValue(Colony c) { return c == null ? 0 : c.defense().maxBases(); }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);

            StarSystem sys = parentUI.systemViewToDisplay();
            if (sys == null)
                return;
            Colony colony = sys.colony();
            if  (colony == null)
                return;

            int w = getWidth();
            int h = getHeight();

            upButtonX[0] = w-s11; upButtonX[1] = w-s17; upButtonX[2] = w-s5;
            upButtonY[0] = s3; upButtonY[1] = s11; upButtonY[2] = s11;

            downButtonX[0] = w-s11; downButtonX[1] = w-s17; downButtonX[2] = w-s5;
            downButtonY[0] = h-s3; downButtonY[1] = h-s11; downButtonY[2] = h-s11;

            g.setColor(enabledArrowColor);
            g.fillPolygon(upButtonX, upButtonY, 3);

            if (colony.defense().maxBases() == 0)
                g.setColor(disabledArrowColor);
            else
                g.setColor(enabledArrowColor);
            g.fillPolygon(downButtonX, downButtonY, 3);

            upArrow.reset();
            downArrow.reset();
            for (int i=0;i<upButtonX.length;i++) {
                upArrow.addPoint(upButtonX[i], upButtonY[i]);
                downArrow.addPoint(downButtonX[i], downButtonY[i]);
            }
            Stroke prevStroke = g.getStroke();
            g.setStroke(stroke2);
            if (hoverBox == upArrow) {
                g.setColor(SystemPanel.yellowText);
                g.drawPolygon(upArrow);
            }
            else if ((hoverBox == downArrow)
                && (colony.defense().maxBases() > 0)) {
                g.setColor(SystemPanel.yellowText);
                g.drawPolygon(downArrow);
            }
            g.setStroke(prevStroke);
        }
        @Override
        public void mouseClicked(MouseEvent arg0) {}
        @Override
        public void mouseEntered(MouseEvent arg0) {}
        @Override
        public void mouseExited(MouseEvent arg0) {
            if (hoverBox != null) {
                hoverBox = null;
                repaint();
            }
        }
        @Override
        public void mousePressed(MouseEvent arg0) {}
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                return;
            int x = e.getX();
            int y = e.getY();
            if (upArrow.contains(x,y)) {
                StarSystem sys = parentUI.systemViewToDisplay();
                if (sys == null)
                    return;
                Colony colony = sys.colony();
                if (colony == null)
                    return;
                boolean updated = colony.defense().incrementMaxBases();
                if (updated)
                    softClick();
                else
                    misClick();
                repaint();
            }
            else if (downArrow.contains(x,y)) {
                StarSystem sys = parentUI.systemViewToDisplay();
                if (sys == null)
                    return;
                Colony colony = sys.colony();
                if (colony == null)
                    return;
                boolean updated = colony.defense().decrementMaxBases();
                if (updated)
                    softClick();
                else
                    misClick();
                repaint();
            }
        }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            Shape newHover = null;
            if (upArrow.contains(x,y))
                newHover = upArrow;
            else if (downArrow.contains(x,y))
                newHover = downArrow;
            else if (basesBox.contains(x,y))
                newHover = basesBox;

            if (newHover != hoverBox) {
                hoverBox = newHover;
                repaint();
            }
        }
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getWheelRotation() < 0)
                incrementBases();
            else
                decrementBases();
        }
    }
    class EmpireProductionPane extends BasePanel {
        private static final long serialVersionUID = 1L;
        EmpireProductionPane() {
            init();
        }
        private void init() {
            setBackground(backC);
            setOpaque(true);
        }
        @Override
        public String textureName()            { return parentUI.subPanelTextureName(); }
        @Override
        public void paintComponent(Graphics g) {
            StarSystem sys = parentUI.systemViewToDisplay();
            if (sys == null)
                return;
            Colony c = sys.colony();
            if (c == null)
                return;

            super.paintComponent(g);

            String str1 = text("MAIN_COLONY_PRODUCTION");
            String str2 = str((int)c.totalIncome());
            String str3 = concat("(", str((int)c.production()), ")");

            int y0 = getHeight()-s6;
            g.setColor(textColor);
            g.setFont(narrowFont(16));
            g.drawString(str1, s5, y0);
            int sw2 = g.getFontMetrics().stringWidth(str2);
            int sw3 = g.getFontMetrics().stringWidth(str3);

            g.setFont(narrowFont(15));
            drawShadowedString(g, str3, 1, getWidth()-sw3-s10, y0, darkC, textC);
            g.setColor(textC);
            drawShadowedString(g, str2, 1, getWidth()-sw2-sw3-s15, y0, darkC, textC);
        }
    }
}
