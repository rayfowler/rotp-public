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
package rotp.ui.main.overlay;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import rotp.model.Sprite;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.util.Base;

public abstract class MapOverlay implements Base {
    abstract public boolean hoveringOverSprite(Sprite o);
    abstract public void advanceMap();
    abstract public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g2);
    abstract public boolean handleKeyPress(KeyEvent e);

    public boolean consumesClicks(Sprite spr)      { return true; }
    public boolean masksMouseOver(int x, int y)    { return false; }
    public boolean canChangeMapScale()             { return false; }
    public boolean hideNextTurnNotice()            { return true; }
    public boolean drawSprites()                   { return true; }
}
