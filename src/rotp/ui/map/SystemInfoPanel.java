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
package rotp.ui.map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JTextArea;
import rotp.model.empires.Empire;
import rotp.model.empires.SystemView;
import rotp.model.events.StarSystemEvent;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.util.Palette;

public class SystemInfoPanel extends SystemPanel implements MouseMotionListener {
    private static final long serialVersionUID = 1L;
    private static final Color selectedC = new Color(178,124,87);
    private static final Color unselectedC = new Color(112,85,68);
    private static Palette palette;
    EmpireInfoGraphicPane graphicPane;
    SystemsUI parent;
    public SystemInfoPanel(SystemsUI p) {
        parent = p;
        init();
    }
    private void init() {
        palette = Palette.named("Brown");
        setOpaque(true);
        setBackground(selectedC);
        setBorder(newEmptyBorder(6,6,6,6));
        setPreferredSize(new Dimension(scaled(250), getHeight()));

        graphicPane = new EmpireInfoGraphicPane(this);
        graphicPane.setPreferredSize(new Dimension(getWidth(),scaled(140)));

        detailPane = detailPane();
        BorderLayout layout = new BorderLayout();
        layout.setVgap(s6);
        setLayout(layout);
        add(topPane(), BorderLayout.NORTH);
        add(detailPane, BorderLayout.CENTER);
        
        addMouseMotionListener(this);
    }
    @Override
    public String subPanelTextureName()    { return TEXTURE_BROWN; }
    @Override
    public StarSystem systemViewToDisplay() { return selectedSystem(); }
    @Override
    public void animate() { 
        graphicPane.animate(); 
        detailPane.animate();
    }
    @Override
    protected BasePanel topPane() {
        return graphicPane;
    }
    @Override
    protected BasePanel detailPane() {
        BasePanel summaryPane = new SystemSummaryPane();

        BasePanel systemFlagsPane = new SystemFlagsPane();

        BasePanel systemEventsPanel = new SystemHistoryPane();
        systemEventsPanel.setPreferredSize(new Dimension(getWidth(), scaled(250)));
        
        BasePanel empireDetailPane = new BasePanel();
        empireDetailPane.setOpaque(false);
        empireDetailPane.setLayout(new BorderLayout(0,s5));
        empireDetailPane.add(summaryPane, BorderLayout.NORTH);
        empireDetailPane.add(systemFlagsPane, BorderLayout.CENTER);
        empireDetailPane.add(systemEventsPanel, BorderLayout.SOUTH);
        return empireDetailPane;
    }
    StarSystem selectedSystem() {
        return parent.systemToDisplay();
    }

    @Override
    public void mouseDragged(MouseEvent e) { }
    @Override
    public void mouseMoved(MouseEvent e) {}
    class EmpireInfoGraphicPane extends BasePanel implements ActionListener {
        private static final long serialVersionUID = 1L;
        SystemPanel parent;
        Ellipse2D starCircle = new Ellipse2D.Float();
        Ellipse2D planetCircle = new Ellipse2D.Float();
        int currentHover = 0;
        EmpireInfoGraphicPane(SystemPanel p) {
            parent = p;
            init();
        }
        private void init() {
            setBackground(palette.black);
        }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;

            Empire pl = player();
            int w = getWidth();
            int h = getHeight();

            Graphics2D g2 = (Graphics2D) g;
            g2.drawImage(pl.sv.starBackground(this), 0, 0, null);
            drawStar(g2, selectedSystem().starType(), s80, getWidth()/3, s70);
            starCircle.setFrame((getWidth()/3)-s20, s10, s40, s40);

            g.setFont(narrowFont(36));
            String str = player().sv.name(sys.id);
            int y0 = s42;
            int x0 = s25;
            drawBorderedString(g, str, 2, x0, y0, Color.black, SystemPanel.orangeText);

            if (!player().sv.isScouted(sys.id))
                return;
            int x1 = s20;
            int y1 = s70;
            int r = s40;
            selectedSystem().planet().draw(g, w, h, x1, y1, r+r, 45);
            planetCircle.setFrame(x1, y1, r+r, r+r);
            parent.drawPlanetInfo(g2, selectedSystem(), false, false, s40, getWidth(), getHeight()-s12);
        }
        @Override
        public void animate() {
            if (animationCount() % 3 == 0) {
                try {
                    selectedSystem().planet().rotate(1);
                    repaint();
                }
                catch (Exception e) { }
            }
        }
    }
    final class SystemSummaryPane extends BasePanel {
        private static final long serialVersionUID = 1L;
        SystemSummaryPane() {
            setBackground(unselectedC);
            setPreferredSize(new Dimension(getWidth(), s40));
        }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            StarSystem sys = systemViewToDisplay();
            if (sys == null)
                return;
            int id = sys.id;
            String name = player().sv.descriptiveName(id);
            g.setFont(narrowFont(24));
            drawShadowedString(g, name, 2, s10, s30, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);
        }
    }
    final class SystemFlagsPane extends BasePanel implements MouseListener {
        private static final long serialVersionUID = 1L;
        JTextArea comments = new JTextArea();
        StarSystem sys;
        String emptyNotes;
        boolean commentLocSet = false;
        SystemFlagsPane() {
            setBackground(unselectedC);
            add(comments);
            comments.setPreferredSize(new Dimension(getWidth()-s20,s100));
            comments.setBackground(selectedC);
            comments.setForeground(SystemPanel.blackText);
            comments.setFont(narrowFont(17));
            comments.setCaretColor(SystemPanel.blackText);
            comments.putClientProperty("caretWidth", s3);
            comments.setFocusTraversalKeysEnabled(false);
            comments.addMouseListener(this);
            emptyNotes = text("SYSTEMS_ENTER_NOTES");
        }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            StarSystem prevSys = sys;
            sys = systemViewToDisplay();
            
            // if we are displaying a different system, updating 
            // the notes in the comments field
            if (sys != prevSys) {
                if (sys == null)
                    comments.setText("");
                else if (sys.notes().isEmpty())
                    comments.setText(emptyNotes);
                else
                    comments.setText(sys.notes());
            }
            if (sys == null)
                return;
            
            int id = sys.id;
            SystemView sv = player().sv.view(id);
            String title = text("SYSTEMS_ALERT");
            g.setFont(narrowFont(22));
            drawShadowedString(g, title, 2, s10, s25, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);
            int sw = g.getFontMetrics().stringWidth(title);
            Color c0 = parent.alertColor(sv);
            if (c0 != null) {
                g.setColor(c0);
                int x0 = s10+sw+s20;
                int y0 = s15;
                g.fillRect(x0-s11, y0-s2, s7, s4);
                g.fillRect(x0+s5, y0-s2, s7, s4);
                g.fillRect(x0-s2, y0-s10, s4, s7);
                g.fillRect(x0-s2, y0+s5, s4, s7);
            }
            
            String desc = parent.alertDescription(sv);
            if (desc == null)
                desc = text("SYSTEMS_NO_ALERTS");
            
            g.setFont(narrowFont(17));
            g.setColor(SystemPanel.blackText);
            List<String> lines = wrappedLines(g, desc, getWidth()-s20);
            int y0 = s47;
            for (String line: lines) {
                g.drawString(line, s10, y0);
                y0 += s18;
            }
            
            comments.setBounds(s5, getHeight()-s100, getWidth()-s10, s95);
        }
        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mousePressed(MouseEvent e) { }
        @Override
        public void mouseReleased(MouseEvent e) { }
        @Override
        public void mouseEntered(MouseEvent e) {
            parent.animate = false;
            comments.setCaretPosition(comments.getText().length());
            comments.setBounds(s5, getHeight()-s100, getWidth()-s10, s95);
            comments.requestFocus();
            comments.repaint();
        }
        @Override
        public void mouseExited(MouseEvent e) {
            parent.animate = true;
            // ensure we update the system's notes when we leave the field
            String notes = comments.getText().trim();
            if (notes.equals(emptyNotes))
                notes = "";
            sys.notes(notes);
            if (notes.isEmpty())
                comments.setText(emptyNotes);
        }
    }
    final class SystemHistoryPane extends BasePanel implements MouseListener {
        private static final long serialVersionUID = 1L;
        StarSystem sys;
        Rectangle eventsBox = new Rectangle();
        SystemHistoryPane() {
            setBackground(unselectedC);
        }
        @Override
        public void paintComponent(Graphics g0) {
            int w = getWidth();
            int h = getHeight();
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);
            sys = systemViewToDisplay();
            
            if (sys == null)
                return;
            
            int id = sys.id;
            SystemView sv = player().sv.view(id);
            String title = text("SYSTEMS_HISTORY");
            g.setFont(narrowFont(22));
            drawShadowedString(g, title, 2, s10, s25, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);
            
            eventsBox.setBounds(s5, s35, w-s10, h-s40);
            //g.setColor(selectedC);
            //g.fill(eventsBox);

            if (!sv.scouted())
                return;
            
            g.setClip(eventsBox);
            
            g.setFont(narrowFont(16));
            g.setColor(SystemPanel.blackText);
            int y0 = eventsBox.y+s20;
            int x0 = eventsBox.x+s10;
            int w0 = eventsBox.width-s20;
            int lineH = s16;
            List<StarSystemEvent> reverseEvents = new ArrayList<>(sv.system().events());
            Collections.reverse(reverseEvents);
            for (StarSystemEvent ev: reverseEvents) {
                String yr= ev.year();
                String desc = ev.description();
                List<String> descLines = this.wrappedLines(g, desc, w0-s40);
                int yrOffset = (descLines.size()-1)*(lineH/2);
                g.drawString(yr, x0, y0+yrOffset);
                for (String line: descLines) {
                    g.drawString(line, x0+s40, y0);
                    y0 += lineH;
                }
                y0 += s3;
            }
            g.setClip(null);
        }
        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mousePressed(MouseEvent e) { }
        @Override
        public void mouseReleased(MouseEvent e) { }
        @Override
        public void mouseEntered(MouseEvent e) { }
        @Override
        public void mouseExited(MouseEvent e) { }
    }
}