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
import rotp.model.galaxy.StarSystem;
import rotp.ui.notifications.GNNNotification;
import rotp.util.Base;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RandomEventRebellion implements Base, Serializable, RandomEvent {
    private static final long serialVersionUID = 1L;
    private int empId;
    private int sysId;
    @Override
    public boolean goodEvent()    		{ return false; }
    @Override
    public boolean repeatable()    		{ return true; }
    @Override
    public String notificationText()    {
        String s1 = text("EVENT_REBELLION");
        s1 = s1.replace("[system]", galaxy().empire(empId).sv.name(sysId));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    @Override
    public void trigger(Empire emp) {
        // find a random colony that is not the homeworld and not already in rebellion
        // and is not uncolonizable by the owning empire (e.g. if the env has degraded)
        List<StarSystem> systems = new ArrayList<>();
        for (StarSystem sys : emp.allColonizedSystems()) {
            if (!emp.isCapital(sys) && emp.canColonize(sys) && !sys.colony().inRebellion())
                systems.add(sys);
        }
        if (systems.isEmpty())
            return;

        StarSystem targetSystem = random(systems);
        empId = emp.id;
        sysId = targetSystem.id;

        targetSystem.colony().inciteRebels(0.5f, "GNN_PLAYER_REBELLION");
        String systemName = player().sv.name(sysId);

        // if a player colony rebels, he already gets a GNN notice. Don't need to send another
        if (!emp.isPlayerControlled() && !systemName.isEmpty())
            GNNNotification.notifyRandomEvent(notificationText(), "GNN_Event_Rebellion");
    }
}
