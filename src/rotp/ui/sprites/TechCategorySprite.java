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
import java.awt.Rectangle;
import rotp.model.empires.EspionageMission;
import rotp.model.tech.Tech;
import rotp.model.tech.TechCategory;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.overlay.MapOverlayEspionageMission;

public class TechCategorySprite extends MapSprite {
    private final Rectangle selectBox = new Rectangle();
    private final int categoryNum;
    private EspionageMission mission;
    private final MapOverlayEspionageMission parent;

    public TechCategorySprite(MapOverlayEspionageMission p, int i) {
        parent = p;
        categoryNum = i;
    }
    public int categoryNum()   { return categoryNum;  }
    public String categoryId() { return TechCategory.id(categoryNum); }
    public Rectangle box()     { return selectBox; }

    public void setBounds(int x, int y, int w, int h) {
        selectBox.setBounds(x,y,w,h);
    }
    public void espionage(EspionageMission ct) { mission = ct; }
    private Tech tech()   { return mission.techChoice(categoryId()); }

    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
        return (tech() != null) && selectBox.contains(x,y);
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) { }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        if (click)
            softClick();
        mission.stealTech(tech());
        parent.espionageCategorySelected();
    }
}
