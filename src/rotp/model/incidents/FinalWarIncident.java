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
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.notifications.DiplomaticNotification;

public class FinalWarIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int empNotified;
    final int empRebel;

    public static void create(Empire ally, Empire leader, Empire rebel) {
        FinalWarIncident inc = new FinalWarIncident(ally, leader, rebel);
        ally.diplomatAI().noticeIncident(inc, rebel);
        if (rebel.isPlayerControlled() && (ally == leader))
            DiplomaticNotification.create(ally.viewForEmpire(rebel), inc, inc.warningMessageId());
    }
    private FinalWarIncident(Empire n, Empire a, Empire v) {
        empNotified = n.id;
        empRebel = v.id;
        severity = -200;
        dateOccurred = galaxy().currentYear();
        duration = 9999999;
    }
    @Override
    public String title()            { return text("INC_FINAL_WAR_TITLE"); }
    @Override
    public String description()      { return decode(text("INC_FINAL_WAR_DESC")); }
    @Override
    public float currentSeverity()   { return severity; }
    @Override
    public String warningMessageId() {  return DialogueManager.WARNING_FINAL_WAR; }
    @Override
    public String key() {
        return concat("FinalWar:", galaxy().empire(empRebel).race().id);
    }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empRebel).replaceTokens(s1, "rebel");
        return s1;
    }
}
