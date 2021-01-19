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

public class AtWarWithAllyIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int empMe;
    final int empYou;
    final int empOther;

    public AtWarWithAllyIncident(EmpireView ev, Empire other) {
        empMe = ev.owner().id;
        empYou = ev.empire().id;
        empOther = other.id;
        dateOccurred = galaxy().currentYear();
        duration = 1;
        severity = -10;
    }
    @Override
    public String title()            { return text("INC_AT_WAR_WITH_ALLY_TITLE"); }
    @Override
    public String description()      { return  decode(text("INC_AT_WAR_WITH_ALLY_DESC")); }
    @Override
    public boolean triggeredByAction()   { return false; }
    @Override
    public String key()              { return "At War with ally: "+empOther; }
    @Override
    public String declareWarId()     { return DialogueManager.DECLARE_ALLIANCE_WAR; }
    @Override
    public boolean triggersWar()     { 
        Galaxy g = galaxy();
        return !g.empire(empMe).alliedWith(empYou); 
    }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empOther).replaceTokens(s1, "other");
        return s1;
    }
}
