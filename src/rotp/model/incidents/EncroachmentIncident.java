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

import rotp.model.empires.EmpireView;
import rotp.model.galaxy.StarSystem;
import rotp.ui.diplomacy.DialogueManager;

public class EncroachmentIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int empMe;
    final int empYou;
    final int sysId;
    public static void create(EmpireView ev, StarSystem sys, float sev) {
        ev.owner().diplomatAI().noticeIncident(new EncroachmentIncident(ev,sys,sev), ev.empire());
    }
    public int sysId()                  { return sysId; }
    private EncroachmentIncident(EmpireView ev, StarSystem sys, float sev) {
        empMe = ev.owner().id;
        empYou = ev.empire().id;
        sysId = sys.id;
        dateOccurred = galaxy().currentYear();
        severity = max(-10, sev);
        duration = 3;
        if (ev.owner().leader().isXenophobic())
            duration *= 2;
        else  if (ev.owner().leader().isDiplomat())
            duration /= 2;
    }
    private String systemName()      { return player().sv.name(sysId); }
    @Override
    public String title()            { return text("INC_ENCROACHING_TITLE"); }
    @Override
    public String description()      { return decode(text("INC_ENCROACHING_DESC")); }
    @Override
    public String warningMessageId() { return DialogueManager.WARNING_ENCROACHING; }
    @Override
    public String key() {
        return concat("EmpireGrowth:", str(dateOccurred));
    }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = s1.replace("[system]", systemName());
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        return s1;
    }
}
