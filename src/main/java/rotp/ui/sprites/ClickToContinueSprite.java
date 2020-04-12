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

import java.awt.Graphics2D;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;

public class ClickToContinueSprite extends MapSprite {
    private MainUI parent;
    public void parent(MainUI p)  { parent = p; }
    public ClickToContinueSprite(MainUI p) {
        parent = p;
    }
    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) { return true; }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) { }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        parent.advanceMap();
    }
}
