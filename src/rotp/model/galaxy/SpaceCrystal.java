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
package rotp.model.galaxy;

import rotp.model.colony.Colony;
import rotp.model.combat.CombatStackSpaceCrystal;
import rotp.model.events.RandomEventSpaceCrystal;
import rotp.model.planet.PlanetType;

public class SpaceCrystal extends SpaceMonster {
    private static final long serialVersionUID = 1L;
    public SpaceCrystal() {
        super("SPACE_CRYSTAL");
    }
    @Override
    public void initCombat() {
        combatStacks().clear();
        addCombatStack(new CombatStackSpaceCrystal());       
    }
    public void destroyColony(StarSystem sys) {
        Colony col = sys.colony();
        if (col != null) {
            sys.empire().lastAttacker(RandomEventSpaceCrystal.monster);
            sys.planet().degradeToType(PlanetType.DEAD);
            float maxWaste = sys.planet().maxWaste();
            sys.planet().addWaste(maxWaste);
            sys.planet().removeExcessWaste();
            col.destroy();  
        }        
    }
}
