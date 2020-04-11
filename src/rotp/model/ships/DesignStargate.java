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
package rotp.model.ships;

import javax.swing.ImageIcon;
import rotp.model.tech.TechStargate;

public class DesignStargate extends Design {
    private static final long serialVersionUID = 1L;
    private static ImageIcon icon;
    private String techId;

    // handle bad save game files that don't have tech id assigned?
    private String techId() { return techId == null ? "Stargate:0" : techId; }

    public TechStargate tech()         { return (TechStargate) tech(techId()); }
    public void tech(TechStargate t)   { techId = t.id(); }
    @Override
    public String name()               { return text("SHIP_DESIGN_STARGATE_NAME"); }
    @Override
    public ImageIcon icon() {
        if (icon == null)
            icon =  icon("images/ships/special/stargate.png");
        return icon;
    }
    @Override
    public int cost()       { return (int) tech().cost; }
}
