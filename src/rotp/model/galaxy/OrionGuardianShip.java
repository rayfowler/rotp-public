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

import java.awt.Image;
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
        super("ORION_GUARDIAN");
        techs.add("ShipWeapon:16");  // death ray
    }
    @Override
    public void initCombat() {
        combatStacks().clear();
        addCombatStack(new CombatStackOrionGuardian());       
    }
    @Override
    public Image image()  { return image("ORION_GUARDIAN"); }
    @Override
    public void plunder() { 
        super.plunder();
        Empire emp = this.lastAttacker();
        for (String techId: techs)
            emp.plunderShipTech(tech(techId), -2); 
        
        // find the system with this monster and remove it
        int sysId = StarSystem.NULL_ID;
        for (StarSystem sys: galaxy().starSystems()) {
            if (sys.planet().isOrionArtifact()) {
                sys.monster(null);
                sysId = sys.id;
                break;
            }
        }
        // all empires now know this system is no longer guarded
        for (Empire emp1: galaxy().empires()) 
            emp1.sv.view(sysId).refreshSystemEntryScan();
    } 
    @Override
    protected DiplomaticIncident killIncident(Empire emp) { return KillGuardianIncident.create(emp.id, lastAttackerId, nameKey); }

}
