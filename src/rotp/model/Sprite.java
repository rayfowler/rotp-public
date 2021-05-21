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
package rotp.model;

import java.awt.Graphics2D;
import rotp.model.galaxy.IMappedObject;
import rotp.model.galaxy.StarSystem;
import rotp.ui.main.GalaxyMapPanel;

public interface Sprite {
    IMappedObject source();
    boolean hovering();
    void hovering(boolean b);
    void draw(GalaxyMapPanel map, Graphics2D g2);
    boolean isSelectableAt(GalaxyMapPanel map, int mapX, int mapY);
    default float selectDistance(GalaxyMapPanel map, int mapX, int mapY)  { return 0.0f; }

    default int displayPriority()                { return 10; }
    default boolean persistOnClick()             { return false; }
    default void repaint(GalaxyMapPanel map)     { map.repaint(); }
    default StarSystem starSystem()              { return null; }
    default boolean hasDisplayPanel()            { return false; }

    default boolean acceptDoubleClicks()         { return false; }
    default void click(GalaxyMapPanel map, int count, boolean rightClick, boolean sound)        { }
    default void wheel(GalaxyMapPanel map, int count, boolean sound)        { }
    default boolean acceptWheel()                { return false; }

    default int mapX(GalaxyMapPanel map)         { return map.mapX(source().x()); }
    default int mapY(GalaxyMapPanel map)         { return map.mapY(source().y()); }

    default int centerMapX(GalaxyMapPanel map)   { return mapX(map); }
    default int centerMapY(GalaxyMapPanel map)   { return mapY(map); }

    default void mouseEnter(GalaxyMapPanel map) {
        if (!hovering()) 
            hovering(true);
    }
    default void mouseExit(GalaxyMapPanel map) {
        if (hovering()) 
            hovering(false);
    }
}
