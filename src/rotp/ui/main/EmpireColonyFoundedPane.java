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
import java.awt.Dimension;
import java.awt.Graphics;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.SystemViewer;

public class EmpireColonyFoundedPane extends BasePanel {
    private static final long serialVersionUID = 1L;
    SystemViewer parent;
    public EmpireColonyFoundedPane(SystemViewer p, Color c0) {
        parent = p;
        init(c0);
    }
    private void init(Color c0) {
        setOpaque(true);
        setBackground(c0);
        setPreferredSize(new Dimension(getWidth(), s40));
    }
    @Override
    public String textureName()            { return parent.subPanelTextureName(); }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        StarSystem sys = parent.systemViewToDisplay();
        if (sys == null)
            return;
        int id = sys.id;
        String name = player().sv.descriptiveName(id);
        g.setFont(narrowFont(24));
        drawShadowedString(g, name, 2, s10, s30, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);
    }
}