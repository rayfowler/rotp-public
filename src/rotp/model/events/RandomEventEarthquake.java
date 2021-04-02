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

import rotp.model.colony.Colony;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.ui.notifications.GNNNotification;
import rotp.util.Base;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RandomEventEarthquake implements Base, Serializable, RandomEvent {
    private static final long serialVersionUID = 1L;
    private int empId;
    private int sysId;
    private int popKilled = 0;
    @Override
    public boolean goodEvent()    		{ return false; }
    @Override
    public boolean repeatable()    		{ return true; }
    @Override
    public String notificationText()    {
        String s1 = text("EVENT_EARTHQUAKE");
        s1 = s1.replace("[amt]", str(popKilled));
        s1 = s1.replace("[system]", galaxy().empire(empId).sv.name(sysId));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    @Override
    public void trigger(Empire emp) {
        // find a random colony that has at least 30 population
        List<StarSystem> systems = new ArrayList<>();
        for (StarSystem sys : emp.allColonizedSystems()) {
            Colony col = sys.colony();
            if (col.population() >= 30)
                systems.add(sys);
        }
        if (systems.isEmpty())
            return;

        StarSystem targetSystem = random(systems);
        empId = emp.id;
        sysId = targetSystem.id;
        
        Colony targetColony = targetSystem.colony();

        float popLossPct = 0.2f + (random()*0.1f);
        float factLossPct = 0.3f + (random()*0.5f);

        float prevPop = targetColony.population();
        float prevFact = targetColony.industry().factories();

        float newPop = prevPop * (1-popLossPct);
        float newFact = prevFact * (1-factLossPct);

        popKilled = max(1, (int) prevPop - (int) newPop);

        targetColony.setPopulation(newPop);
        targetColony.industry().factories(newFact);
        if (player().knowsOf(empId)
        && !player().sv.name(sysId).isEmpty())
            GNNNotification.notifyRandomEvent(notificationText(), "GNN_Event_Earthquake");
    }
}
