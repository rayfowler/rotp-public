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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;

public class SystemViewInfoPane extends BasePanel {
    private static final long serialVersionUID = 1L;
    private static final String EMPIRE_SYSTEM = "Empire";
    private static final String ALIEN_FRIENDLY_SYSTEM = "Alien-Friendly";
    private static final String ALIEN_NEUTRAL_SYSTEM = "Alien-Neutral";
    private static final String ALIEN_HOSTILE_SYSTEM = "Alien-Hostile";
    private static final String UNEXPLORED_SYSTEM = "Unexplored";
    private static final String UNCOLONIZED_SYSTEM = "Uncolonized";

    CardLayout topLayout;
    SystemPanel parent;

    EmpireColonyTopPanel empireTopPanel;
    AlienFriendlyColonyTopPanel alienFriendlyTopPanel;
    AlienNeutralColonyTopPanel alienNeutralTopPanel;
    AlienHostileColonyTopPanel alienHostileTopPanel;
    UnexploredGraphicInfoPane unexploredTopPanel;
    UncolonizedTopPanel uncolonizedTopPanel;
    BasePanel currentTopPanel;

    public SystemViewInfoPane(SystemPanel p) {
        parent = p;
        topLayout = new CardLayout();

        empireTopPanel = new EmpireColonyTopPanel(parent, false);
        alienFriendlyTopPanel = new AlienFriendlyColonyTopPanel(parent);
        alienNeutralTopPanel = new AlienNeutralColonyTopPanel(parent);
        alienHostileTopPanel = new AlienHostileColonyTopPanel(parent);
        unexploredTopPanel = new UnexploredGraphicInfoPane(parent, parent.topPaneBorderUncolonized());
        uncolonizedTopPanel = new UncolonizedTopPanel(parent);
        init();
    }
    public SystemViewInfoPane(SystemPanel p, boolean showPop) {
        parent = p;
        topLayout = new CardLayout();

        empireTopPanel = new EmpireColonyTopPanel(parent, showPop);
        alienFriendlyTopPanel = new AlienFriendlyColonyTopPanel(parent);
        alienNeutralTopPanel = new AlienNeutralColonyTopPanel(parent);
        alienHostileTopPanel = new AlienHostileColonyTopPanel(parent);
        unexploredTopPanel = new UnexploredGraphicInfoPane(parent, parent.topPaneBorderUncolonized());
        uncolonizedTopPanel = new UncolonizedTopPanel(parent);
        init();
    }
    private void init() {
        setPreferredSize(new Dimension(getWidth(),scaled(145)));
        setLayout(topLayout);
        add(empireTopPanel, EMPIRE_SYSTEM);
        add(alienFriendlyTopPanel, ALIEN_FRIENDLY_SYSTEM);
        add(alienNeutralTopPanel, ALIEN_NEUTRAL_SYSTEM);
        add(alienHostileTopPanel, ALIEN_HOSTILE_SYSTEM);
        add(unexploredTopPanel, UNEXPLORED_SYSTEM);
        add(uncolonizedTopPanel, UNCOLONIZED_SYSTEM);
        topLayout.show(this, UNEXPLORED_SYSTEM);
    }
    @Override
    public void paint(Graphics g) {
        selectBestPane();
        super.paint(g);
    }
    private void selectBestPane() {
        StarSystem sys = parent.systemViewToDisplay();
        if (sys == null)
            return;
        Empire pl = player();

        if (!pl.sv.isScouted(sys.id)) {
            currentTopPanel = unexploredTopPanel;
            topLayout.show(this, UNEXPLORED_SYSTEM);
        } else if (!player().sv.isColonized(sys.id)) {
            currentTopPanel = uncolonizedTopPanel;
            topLayout.show(this, UNCOLONIZED_SYSTEM);
        } else {
            int ownerId = pl.sv.empId(sys.id);
            if (ownerId == pl.id) {
                currentTopPanel = empireTopPanel;
                topLayout.show(this, EMPIRE_SYSTEM);
            } else if (pl.friendlyWith(ownerId)) {
                currentTopPanel = alienFriendlyTopPanel;
                topLayout.show(this, ALIEN_FRIENDLY_SYSTEM);
            } else if (pl.atWarWith(ownerId)) {
                currentTopPanel = alienHostileTopPanel;
                topLayout.show(this, ALIEN_HOSTILE_SYSTEM);
            } else {
                currentTopPanel = alienNeutralTopPanel;
                topLayout.show(this, ALIEN_NEUTRAL_SYSTEM);
            }
        }
    }
    @Override
    public void animate() {
        if (currentTopPanel != null)
            currentTopPanel.animate();
    }
    final class EmpireColonyTopPanel extends BasePanel {
        private static final long serialVersionUID = 1L;
        SystemGraphicPane graphicPane;
        public EmpireColonyTopPanel(SystemPanel p, boolean showPop) {
            graphicPane = new SystemGraphicPane(p, p.topPaneBorderEmpire());
            graphicPane.showPopulation = showPop;
            setLayout(new BorderLayout());
            add(graphicPane, BorderLayout.CENTER);
        }
        @Override
        public void animate()     { graphicPane.animate(); }
    }
    final class AlienFriendlyColonyTopPanel extends BasePanel {
        private static final long serialVersionUID = 1L;
        SystemGraphicPane graphicPane;
        public AlienFriendlyColonyTopPanel(SystemPanel p) {
            graphicPane = new SystemGraphicPane(p, p.topPaneBorderFriendly());
            setLayout(new BorderLayout());
            add(graphicPane, BorderLayout.CENTER);
        }
        @Override
        public void animate()     { graphicPane.animate(); }
    }
    final class AlienNeutralColonyTopPanel extends BasePanel {
        private static final long serialVersionUID = 1L;
        SystemGraphicPane graphicPane;
        public AlienNeutralColonyTopPanel(SystemPanel p) {
            graphicPane = new SystemGraphicPane(p, p.topPaneBorderNeutral());
            setLayout(new BorderLayout());
            add(graphicPane, BorderLayout.CENTER);
        }
        @Override
        public void animate()     { graphicPane.animate(); }
    }

    final class AlienHostileColonyTopPanel extends BasePanel {
        private static final long serialVersionUID = 1L;
        SystemGraphicPane graphicPane;
        public AlienHostileColonyTopPanel(SystemPanel p) {
            graphicPane = new SystemGraphicPane(p, p.topPaneBorderHostile());
            setLayout(new BorderLayout());
            add(graphicPane, BorderLayout.CENTER);
        }
        @Override
        public void animate()     { graphicPane.animate(); }
    }
    final class UncolonizedTopPanel extends BasePanel {
        private static final long serialVersionUID = 1L;
        SystemGraphicPane graphicPane;
        public UncolonizedTopPanel(SystemPanel p) {
            graphicPane = new SystemGraphicPane(p, p.topPaneBorderUncolonized());
            setLayout(new BorderLayout());
            add(graphicPane, BorderLayout.CENTER);
        }
        @Override
        public void animate()     { graphicPane.animate(); }
    }
}
