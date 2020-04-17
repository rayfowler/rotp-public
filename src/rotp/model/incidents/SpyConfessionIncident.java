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
import rotp.model.empires.SpyNetwork;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.notifications.SpyCapturedAlert;

public class SpyConfessionIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int empVictim;
    final int empSpy;
    final int remainingSpies;
    final int missionType;
    final String mission;
    public SpyConfessionIncident(EmpireView ev, SpyNetwork spies) {
        remainingSpies = spies.numActiveSpies();
        empVictim = ev.owner().id;
        empSpy = ev.empire().id;
        
        if (spies.isEspionage()) {
            mission = text("NOTICE_SPYING_MISSION_ESPIONAGE");
            severity = max(-20, -5+ev.embassy().currentSpyIncidentSeverity());
            missionType = 1;
            duration = 5;
        }
        else if (spies.isSabotage()) {
            mission = text("NOTICE_SPYING_MISSION_SABOTAGE");
            severity = max(-40, -10+ev.embassy().currentSpyIncidentSeverity());
            missionType = 2;
            duration = 10;
        }
        else {
            mission = text("NOTICE_SPYING_MISSION_HIDE");
            severity = max(-10, -2+ev.embassy().currentSpyIncidentSeverity());
            missionType = 0;
            duration = 2;
        }

        dateOccurred = galaxy().currentYear();

        if (ev.owner().isPlayer() || ev.empire().isPlayer())
            SpyCapturedAlert.create(ev.empire(), ev.owner(), mission);
    }
    @Override
    public boolean isSpying()           { return missionType > 0; }
    @Override
    public String title()               { return text("INC_SPY_CONFESSION_TITLE"); }
    @Override
    public String description() {
        if (missionType == 2)
            return decode(text("INC_SPY_CONFESS_SABOTAGE_DESC"));
        else
            return decode(text("INC_SPY_CONFESS_ESPIONAGE_DESC"));
    }
    @Override
    public boolean triggersWarning()    { return true; }
    @Override
    public String warningMessageId() {
        if (galaxy().empire(empVictim).isPlayer())
            return "";
        else if (missionType == 2)
            return DialogueManager.WARNING_SABOTAGE;
        else
            return DialogueManager.WARNING_ESPIONAGE;
    }
    @Override
    public String key() {
        return concat(str(dateOccurred), ":SpyConfession");
    }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = s1.replace("[spyrace]",  galaxy().empire(empSpy).raceName());
        s1 = galaxy().empire(empSpy).replaceTokens(s1, "spy");
        s1 = galaxy().empire(empVictim).replaceTokens(s1, "victim");
        s1 = s1.replace("[mission]", mission);
        s1 = s1.replace("[numspies]", str(remainingSpies));
        s1 = s1.replace("[framed]", "");
        return s1;
    }
}
