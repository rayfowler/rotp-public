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
import rotp.model.empires.SabotageMission;
import rotp.ui.diplomacy.DialogueManager;

public class SabotageRebellionIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int empVictim;
    final int empSpy;
    final int sysId;
    final int incited;

    public static void addIncident(SabotageMission m) {
        // no incident if spy not caught
        if (!m.spy().caught())
            return;

        // create incident and add to victim's empireView
        EmpireView otherView = m.spies().view().otherView();
        otherView.embassy().addIncident(new SabotageRebellionIncident(otherView, m));
        otherView.embassy().resetAllianceTimer();
        otherView.embassy().resetPactTimer();
    }
    private SabotageRebellionIncident(EmpireView ev, SabotageMission m) {
        dateOccurred = galaxy().currentYear();
        duration = ev.empire().leader().isXenophobic()? 20 : 10;

        empVictim = ev.owner().id;
        empSpy = ev.empire().id;
        sysId = m.starSystem().id;
        incited = m.rebelsIncited();
        severity = max(-25,(-2*incited)+ev.embassy().currentSpyIncidentSeverity());
    }
    private String systemName()      { return player().sv.name(sysId); }
    @Override
    public boolean isSpying()        { return true; }
    @Override
    public int timerKey()          { return DiplomaticEmbassy.TIMER_SPY_WARNING; }
    @Override
    public String title()            { return text("INC_INCITED_REBELLION_TITLE"); }
    @Override
    public String description()      { return decode(text("INC_INCITED_REBELLION_DESC")); }
    @Override
    public String warningMessageId() { return galaxy().empire(empVictim).isPlayerControlled() ? "" : DialogueManager.WARNING_SABOTAGE; }
    @Override
    public String declareWarId()     { return DialogueManager.DECLARE_SPYING_WAR; }
    @Override
    public boolean triggersWar()        { return false; } // war is only triggered after a warning
    @Override
    public String key()              { return  str(dateOccurred)+":Rebellion:"+sysId; }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empSpy).replaceTokens(s1, "spy");
        s1 = galaxy().empire(empVictim).replaceTokens(s1, "victim");
        s1 = s1.replace("[system]", systemName());
        s1 = s1.replace("[amt]", str(incited));
        return s1;
    }
}
