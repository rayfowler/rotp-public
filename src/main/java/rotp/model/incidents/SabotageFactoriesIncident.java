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
import rotp.model.empires.SabotageMission;
import rotp.model.galaxy.StarSystem;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.notifications.FactoriesDestroyedAlert;

public class SabotageFactoriesIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int empVictim;
    final int empSpy;
    final int sysId;
    final int destroyed;

    public static void addIncident(SabotageMission m) {
        EmpireView otherView = m.spies().view().otherView();
        otherView.embassy().resetAllianceTimer();
        otherView.embassy().resetPactTimer();
        // no incident if spy not caught
        if (!m.spy().caught()) {
            Empire victim = otherView.owner();
            if (victim.isPlayer()
            && !victim.isAIControlled()
            && (m.factoriesDestroyed() > 0)) {
                StarSystem sys = m.starSystem();
                FactoriesDestroyedAlert.create(null, m.factoriesDestroyed(), sys);
            }
            return;
        }

        // create incident and add to victim's empireView
        otherView.embassy().addIncident(new SabotageFactoriesIncident(otherView, m));
    }
    private SabotageFactoriesIncident(EmpireView ev, SabotageMission m) {
        dateOccurred = galaxy().currentYear();
        duration = 20;

        empVictim = ev.owner().id;
        empSpy = ev.empire().id;
        sysId = m.starSystem().id;
        destroyed = m.factoriesDestroyed();
        severity = max(-60,(-1*destroyed)+ev.embassy().currentSpyIncidentSeverity());

        if (ev.owner().isPlayer()
        && !ev.owner().isAIControlled()
        && (destroyed > 0)) {
            StarSystem sys = m.starSystem();
            FactoriesDestroyedAlert.create(ev.empire(), destroyed, sys);
            if (sys.isColonized() && sys.colony().industry().allocation() == 0)
                session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_SABOTAGE_FACTORIES", systemName(), str(destroyed), ev.empire().raceName()));
        }
    }
    private String systemName()      { return player().sv.name(sysId); }
    @Override
    public boolean isSpying()        { return true; }
    @Override
    public String title()            { return text("INC_DESTROYED_FACTORIES_TITLE"); }
    @Override
    public String description()      { return decode(text("INC_DESTROYED_FACTORIES_DESC")); }
    @Override
    public String warningMessageId() { return galaxy().empire(empVictim).isPlayer() ? "" : DialogueManager.WARNING_SABOTAGE; }
    @Override
    public String key()              { return str(dateOccurred)+":Sabotage:"+sysId; }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empSpy).replaceTokens(s1, "spy");
        s1 = galaxy().empire(empVictim).replaceTokens(s1, "victim");
        s1 = s1.replace("[system]", systemName());
        s1 = s1.replace("[amt]", str(destroyed));
        return s1;
    }
}
