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
package rotp.model.events;

import rotp.model.empires.Empire;
import rotp.model.incidents.AssassinationIncident;
import rotp.ui.notifications.GNNNotification;
import rotp.util.Base;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RandomEventAssassination implements Base, Serializable, RandomEvent {
    private static final long serialVersionUID = 1L;
    private int empAssassin, empVictim;
    @Override
    public boolean goodEvent()    		{ return false; }
    @Override
    public boolean repeatable()    		{ return true; }
    @Override
    public String notificationText()    {
        String s1 = text("EVENT_ASSASSINATION");
        s1 = galaxy().empire(empAssassin).replaceTokens(s1, "assassin");
        s1 = galaxy().empire(empVictim).replaceTokens(s1, "victim");
        return s1;
    }
    @Override
    public void trigger(Empire emp) {
        if (galaxy().council().finalWar())
            return;
        
        // find a contacted empire that is not at war
        List<Empire> emps = new ArrayList<>();
        for (Empire e: emp.contactedEmpires()) {
            if (!emp.atWarWith(e.id) && emp.inEconomicRange(e.id))
                emps.add(e);
        }
        if (emps.isEmpty())
            return;

        Empire assassin = emp;
        Empire victim = random(emps);

        empAssassin = assassin.id;
        empVictim = victim.id;

        // if player is assassin, notify immediately since this will trigger a war declaration
        // from the victim and we want the GNN notice to show up first
        if (assassin.isPlayerControlled())
            GNNNotification.notifyImmediateEvent(notificationText(), "GNN_Event_Assassin");
        else if (victim.isPlayerControlled() 
        || player().hasContact(assassin)
        || player().hasContact(victim))
            GNNNotification.notifyRandomEvent(notificationText(), "GNN_Event_Assassin");

        AssassinationIncident.create(assassin, victim);
    }
}
