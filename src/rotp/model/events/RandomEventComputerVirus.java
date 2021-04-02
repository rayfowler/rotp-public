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
import rotp.model.tech.TechCategory;
import rotp.model.tech.TechTree;
import rotp.ui.notifications.GNNNotification;
import rotp.util.Base;
import java.io.Serializable;

public class RandomEventComputerVirus implements Base, Serializable, RandomEvent {
    private static final long serialVersionUID = 1L;
    private int empId;
    private String techId;
    private int lostRP;
    @Override
    public boolean goodEvent()    		{ return false; }
    @Override
    public boolean repeatable()    		{ return true; }
    @Override
    public String notificationText()    {
        String s1 = text("EVENT_VIRUS");
        s1 = s1.replace("[amt]", str(lostRP));
        s1 = s1.replace("[technology]", tech(techId).name());
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    @Override
    public void trigger(Empire emp) {
        TechCategory targetCat = null;
        TechTree empTech = emp.tech();
        lostRP = 0;
        if (empTech.computer().totalBC() > lostRP) {
            lostRP = (int) empTech.computer().totalBC();
            targetCat = empTech.computer();
        }
        if (empTech.forceField().totalBC() > lostRP) {
            lostRP = (int) empTech.forceField().totalBC();
            targetCat = empTech.forceField();
        }
        if (empTech.propulsion().totalBC() > lostRP) {
            lostRP = (int) empTech.propulsion().totalBC();
            targetCat = empTech.propulsion();
        }
        if (empTech.weapon().totalBC() > lostRP) {
            lostRP = (int) empTech.weapon().totalBC();
            targetCat = empTech.weapon();
        }

        if (targetCat == null)
            return;

        empId = emp.id;
        techId = targetCat.currentTech();
        targetCat.resetResearchBC();
        if (emp.isPlayerControlled() || player().hasContact(emp))
            GNNNotification.notifyRandomEvent(notificationText(), "GNN_Event_Virus");
    }
}
