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
import rotp.model.galaxy.Galaxy;
import rotp.ui.diplomacy.DialogueManager;

public class AssassinationIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    private final int empAssassin;
    private final int empVictim;
    public static void create(Empire assassin, Empire victim) {
        AssassinationIncident inc = new AssassinationIncident(assassin, victim);

        EmpireView ev = victim.viewForEmpire(assassin);
        ev.embassy().recallAmbassador(); // technically, he was recalled by the assassin..
        victim.diplomatAI().noticeIncident(inc, assassin);
        
        for (Empire vicEnemy: victim.warEnemies()) {
            if (vicEnemy != assassin) {
                AssassinationIncident inc2 = new AssassinationIncident(assassin, victim, vicEnemy);
                EmpireView ev2 = vicEnemy.viewForEmpire(assassin);
                ev2.embassy().openEmbassy();
                vicEnemy.diplomatAI().noticeIncident(inc2, assassin);     
            }
        }
    }
    public AssassinationIncident(Empire ass, Empire vic) {
        empAssassin = ass.id;
        empVictim = vic.id;
        severity = -50;
        dateOccurred = galaxy().currentYear();
        duration = 20;
    }
    public AssassinationIncident(Empire ass, Empire vic, Empire vicEnemy) {
        empAssassin = ass.id;
        empVictim = vic.id;
        severity = 50;
        dateOccurred = galaxy().currentYear();
        duration = 10;
    }
    @Override
    public String title()            { return text("INC_ASSASSINATION_TITLE"); }
    @Override
    public String description()      { return  decode(text("INC_ASSASSINATION_DESC")); }
    @Override
    public String declareWarId()     { return DialogueManager.DECLARE_ASSASSIN_WAR; }
    @Override
    public boolean triggersWar()     { return severity < 0; } // only trigger war for the negative version of this incident
    @Override
    public boolean triggersImmediateWar() { return true; }
    @Override
    public boolean triggersPraise()   { return severity > 0; }
    @Override
    public String praiseMessageId()   { return DialogueManager.PRAISE_ATTACKED_ENEMY; }
    @Override
    public String key() {
        return concat("Assassination:", str(empVictim));
    }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        Galaxy g = galaxy();
        s1 = g.empire(empAssassin).replaceTokens(s1, "assassin"); // used by DECLARE_ASSASSIN_WAR
        s1 = g.empire(empVictim).replaceTokens(s1, "victim");     // used by DECLARE_ASSASSIN_WAR
        s1 = g.empire(empAssassin).replaceTokens(s1, "attacker"); // used by PRAISE_ATTACKED_ENEMY
        s1 = g.empire(empVictim).replaceTokens(s1, "defender");   // used by PRAISE_ATTACKED_ENEMY
        return s1;
    }
}
