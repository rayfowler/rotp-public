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
package rotp.model.incidents;

import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.galaxy.StarSystem;
import rotp.ui.diplomacy.DialogueManager;

public class ColonyCapturedIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int sysId;
    final int empDefender;
    final int empAttacker;
    final int popLost;
    public static void create(Empire attacker, Empire defender, StarSystem sys, int popLost) {
        if (defender.extinct())
            return;
        ColonyCapturedIncident inc = new ColonyCapturedIncident(attacker, defender, sys, popLost);
        EmpireView ev = defender.viewForEmpire(attacker);
        if (ev != null)
            ev.embassy().addIncident(inc);
        else
            System.err.println("Null embassy for defender:"+defender.raceName()+" attacker:"+attacker.raceName());
    }
    public ColonyCapturedIncident(Empire att, Empire def, StarSystem sys, int p) {
        empAttacker = att.id;
        empDefender = def.id;
        sysId = sys.id;
        popLost = p;

        severity = -20 + max(-130, -5*popLost);
        dateOccurred = galaxy().currentYear();
        duration = 60;
    }
    private String systemName() { return player().sv.name(sysId); }
    @Override
    public String title()         { return text("INC_CAPTURED_COLONY_TITLE", systemName()); }
    @Override
    public String description()   { return  decode(text("INC_CAPTURED_COLONY_DESC")); }
    @Override
    public String declareWarId()  { return DialogueManager.DECLARE_ATTACKED_WAR; }
    @Override
    public String key() {
        return concat(systemName(), ":", str(dateOccurred));
    }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = s1.replace("[system]", systemName());
        s1 = s1.replace("[amt]", str(popLost));
        s1 = galaxy().empire(empAttacker).replaceTokens(s1, "attacker");
        s1 = galaxy().empire(empDefender).replaceTokens(s1, "defender");
        return s1;
    }
}
