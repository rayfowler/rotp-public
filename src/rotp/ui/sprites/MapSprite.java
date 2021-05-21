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
import rotp.model.Sprite;
import rotp.model.galaxy.IMappedObject;
import rotp.model.galaxy.StarSystem;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.map.IMapHandler;
import rotp.util.Base;

public abstract class MapSprite implements Base, Sprite {
    private IMappedObject source;
    protected boolean hovering;

    @Override
    public boolean hovering()                   { return hovering; }
    @Override
    public void hovering(boolean b)             { hovering = b; }
    @Override
    public boolean persistOnClick()             { return false; }
    @Override
    public IMappedObject source()               { return source; }
    public void source(IMappedObject o)         { source = o; }
    @Override
    public void repaint(GalaxyMapPanel map)     { map.repaint(); }
    @Override
    public int mapX(GalaxyMapPanel map)         { return map.mapX(source().x()); }
    @Override
    public int mapY(GalaxyMapPanel map)         { return map.mapY(source().y()); }
    @Override
    public int centerMapX(GalaxyMapPanel map)   { return mapX(map); }
    @Override
    public int centerMapY(GalaxyMapPanel map)   { return mapY(map); }
    public void use(MapSprite s, IMapHandler ui)        { }

    public MapSprite() { }

    @Override
    public StarSystem starSystem()   { return null; }
    @Override
    public abstract boolean isSelectableAt(GalaxyMapPanel map, int mapX, int mapY);
    @Override
    public abstract void draw(GalaxyMapPanel map, Graphics2D g2);
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean sound)        { }
    @Override
    public void wheel(GalaxyMapPanel map, int count, boolean sound)        { }
}
