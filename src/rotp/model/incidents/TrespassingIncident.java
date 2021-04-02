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
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.notifications.TrespassingAlert;

public class TrespassingIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int sysId;
    final int empMe;
    final int empYou;

    public TrespassingIncident(EmpireView ev, StarSystem sys, ShipFleet fl) {
        sysId = sys.id;
        empMe = ev.owner().id;
        empYou = ev.empire().id;
        dateOccurred = galaxy().currentYear();
        duration = 2;

        float multiplier = -1.0f;
        if (sys.empire().atWarWith(fl.empId()))
            multiplier *= 3;
        if (sys.empire().leader().isXenophobic())
            multiplier *= 2;

        float fleetPower = fl.firepower(sys.colony().defense().shieldLevel())/100.0f;
        severity = multiplier* max(1.0f, fleetPower);
        severity = max(-10, severity);
        // notify player if hostile ships are orbiting his colony
        if (ev.owner().isPlayerControlled())
            TrespassingAlert.create(empMe, empYou, sysId);
        // if it is player's ships in orbit, notify player only if not at war
        else if (ev.empire().isPlayerControlled() && !ev.embassy().anyWar())
            TrespassingAlert.create(empMe, empYou, sysId);
    }
    private String systemName()         { return player().sv.name(sysId); }
    @Override
    public String title()               { return text("INC_TRESPASSING_TITLE", systemName()); }
    @Override
    public String description()         { return decode(text("INC_TRESPASSING_DESC")); }
    @Override
    public String warningMessageId()    { return DialogueManager.WARNING_TRESPASSING; }
    @Override
    public int timerKey()               { return DiplomaticEmbassy.TIMER_ATTACK_WARNING; }
    @Override
    public String key() {
        return concat(systemName(), ":", str(dateOccurred));
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
