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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.ui.diplomacy.DialogueManager;

public class BioweaponIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    public final int empAttacker;
    public final int empVictim;
    public final int empMe;
    public final int sysId;

    public static void create(Empire victim, Empire attacker, StarSystem sys) {
        // build list of races that were in contact with victim & attacker
        List<Empire> knowsVictim = victim.contactedEmpires();
        List<Empire> knowsAttacker = attacker.contactedEmpires();

        Set<Empire> notifyList = new HashSet<>();
        notifyList.addAll(knowsVictim);
        notifyList.addAll(knowsAttacker);

        // notify all of genocide exc. victim & attacker
        notifyList.remove(victim);
        notifyList.remove(attacker);
        for (Empire emp: notifyList) {
            BioweaponIncident inc = new BioweaponIncident(emp, attacker, victim, sys);
            emp.diplomatAI().noticeIncident(inc, attacker);
        }
    }
    private BioweaponIncident(Empire n, Empire a, Empire v, StarSystem sys) {
        empMe = n.id;
        empAttacker = a.id;
        empVictim = v.id;
        sysId = sys.id;
        dateOccurred = galaxy().currentYear();
        
        // no penalty in final war
        if (galaxy().council().finalWar()) {
            duration = 1;
            severity = 0;
            return;
        }

        if (n.diplomatAI().setSeverityAndDuration(this))
            return;
              
        severity = max(-30, -20*n.diplomatAI().leaderBioweaponMod());
        duration = 50;
    }
    private String systemName() { return player().sv.name(sysId); }
    @Override
    public String title()         { return text("INC_USED_BIOWEAPONS_TITLE"); }
    @Override
    public String description()      { return  decode(text("INC_USED_BIOWEAPONS_DESC")); }
    @Override
    public String warningMessageId() { return DialogueManager.WARNING_BIOWEAPON; }
    @Override
    public String key() {
        return concat("Bioweapon:", galaxy().empire(empAttacker).race().id);
    }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = s1.replace("[system]", systemName());
        s1 = galaxy().empire(empAttacker).replaceTokens(s1, "attacker");
        s1 = galaxy().empire(empVictim).replaceTokens(s1, "victim");
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        return s1;
    }
}