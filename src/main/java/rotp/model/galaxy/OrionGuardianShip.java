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

import java.util.ArrayList;
import java.util.List;
import rotp.model.combat.CombatStackOrionGuardian;
import rotp.model.empires.Empire;
import rotp.model.incidents.DiplomaticIncident;
import rotp.model.incidents.KillGuardianIncident;

public class OrionGuardianShip extends SpaceMonster {
    private static final long serialVersionUID = 1L;
    private final List<String> techs = new ArrayList<>();
    public OrionGuardianShip() {
        super("PLANET_ORION_GUARDIAN");
        techs.add("ShipWeapon:16");  // death ray
    }
    @Override
    public void initCombat() {
        combatStacks().clear();
        addCombatStack(new CombatStackOrionGuardian());       
    }
    @Override
    public void plunder() { 
        super.plunder();
        Empire emp = this.lastAttacker();
        for (String techId: techs)
            emp.plunderShipTech(tech(techId), -2); 
    } 
    @Override
    protected DiplomaticIncident killIncident(Empire emp) { return KillGuardianIncident.create(emp.id, lastAttackerId, nameKey); }

}
