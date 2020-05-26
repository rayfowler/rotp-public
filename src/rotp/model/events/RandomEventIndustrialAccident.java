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

public class RandomEventIndustrialAccident implements Base, Serializable, RandomEvent {
    private static final long serialVersionUID = 1L;
    private int empId;
    private int sysId;
    @Override
    public boolean goodEvent()    		{ return false; }
    @Override
    public boolean repeatable()    		{ return true; }
    @Override
    public String notificationText()    {
        String s1 = text("EVENT_ACCIDENT");
        s1 = s1.replace("[system]", galaxy().empire(empId).sv.name(sysId));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    @Override
    public void trigger(Empire emp) {
        // find a random colony that has at least 30 factories
        List<StarSystem> systems = new ArrayList<>();
        for (StarSystem sys : emp.allColonizedSystems()) {
            Colony col = sys.colony();
            if (col.industry().factories() >= 30)
                systems.add(sys);
        }
        if (systems.isEmpty())
            return;

        StarSystem targetSystem = random(systems);
        empId = emp.id;
        sysId = targetSystem.id;
        
        targetSystem.addEvent(new SystemRandomEvent("SYSEVENT_ACCIDENT"));
        targetSystem.planet().irradiateEnvironment();
        float maxWaste = targetSystem.planet().maxWaste(); // calc max waste after irradiation
        targetSystem.planet().addWaste(maxWaste);
        targetSystem.planet().removeExcessWaste();
        if (player().knowsOf(empId))
            GNNNotification.notifyRandomEvent(notificationText(), "GNN_Event_Accident");
    }
}
