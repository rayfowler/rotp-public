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

import rotp.model.empires.DiplomaticEmbassy;
import rotp.model.empires.EmpireView;
import rotp.model.empires.SpyNetwork;
import rotp.ui.diplomacy.DialogueManager;

public class SpyConfessionIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    public final int empVictim;
    public final int empSpy;
    public final int remainingSpies;
    public final int missionType;
    public final String mission;
    public SpyConfessionIncident(EmpireView ev, SpyNetwork spies) {
        remainingSpies = spies.numActiveSpies();
        empVictim = ev.owner().id;
        empSpy = ev.empire().id;
        
        if (spies.isEspionage()) {
            mission = text("NOTICE_SPYING_MISSION_ESPIONAGE");
            missionType = 1;
            
            if (ev.owner().diplomatAI().setSeverityAndDuration(this, ev.embassy().currentSpyIncidentSeverity()))
                return;
            severity = max(-20, -5+ev.embassy().currentSpyIncidentSeverity());
            duration = 5;
        }
        else if (spies.isHide() && ev.owner().diplomatAI().leaderHatesAllSpies()) {
            mission = text("NOTICE_SPYING_MISSION_SABOTAGE");
            severity = max(-20, -10+ev.embassy().currentSpyIncidentSeverity());
            missionType = 0;
            duration = 10;
        }
        else if (spies.isSabotage()) {
            mission = text("NOTICE_SPYING_MISSION_SABOTAGE");
            severity = max(-20, -10+ev.embassy().currentSpyIncidentSeverity());
            missionType = 2;
            duration = 10;
        }
        else {
            mission = text("NOTICE_SPYING_MISSION_HIDE");
            severity = max(-5, -2+ev.embassy().currentSpyIncidentSeverity());
            missionType = 0;
            duration = 2;
        }

        if (ev.owner().diplomatAI().leaderHatesAllSpies())
            duration *= 2;
        
        dateOccurred = galaxy().currentYear();
    }
    @Override
    public boolean isSpying()           { return (missionType > 0) || galaxy().empire(empVictim).diplomatAI().leaderHatesAllSpies() ; }
    @Override
    public int timerKey()               { return DiplomaticEmbassy.TIMER_SPY_WARNING; }
    @Override
    public String title()               { return text("INC_SPY_CONFESSION_TITLE"); }
    @Override
    public String description() {
        switch(missionType) {
            case 0: return decode(text("INC_SPY_CAPTURED_DESC"));
            case 1: return decode(text("INC_SPY_CONFESS_ESPIONAGE_DESC"));
            case 2: return decode(text("INC_SPY_CONFESS_SABOTAGE_DESC"));
            default: return decode(text("INC_SPY_CAPTURED_DESC"));
        }
    }
    @Override
    public boolean triggersWar()        { return false; } // war is only triggered after a warning
    @Override
    public boolean triggersWarning()    { return true; }
    @Override
    public String warningMessageId() {
        if (galaxy().empire(empVictim).isPlayerControlled())
            return "";
        else if (missionType == 2)
            return DialogueManager.WARNING_SABOTAGE;
        else if (missionType == 1)
            return DialogueManager.WARNING_ESPIONAGE;
        else
            return DialogueManager.WARNING_SABOTAGE;        
    }
    @Override
    public String declareWarId()     { return DialogueManager.DECLARE_SPYING_WAR; }
    @Override
    public String key() {
        return concat(str(dateOccurred), ":SpyConfession");
    }
    @Override
    public String decode(String s) {
        String forceMessage = missionType > 0 ? "" : text("SPY_FORCED_CONFESSION");
        String s1 = super.decode(s);
        s1 = s1.replace("[spyrace]",  galaxy().empire(empSpy).raceName());
        s1 = galaxy().empire(empSpy).replaceTokens(s1, "spy");
        s1 = galaxy().empire(empVictim).replaceTokens(s1, "victim");
        s1 = s1.replace("[mission]", mission);
        s1 = s1.replace("[numspies]", str(remainingSpies));
        s1 = s1.replace("[framed]", "");
        s1 = s1.replace("[forced]", forceMessage);
        return s1;
    }
}
