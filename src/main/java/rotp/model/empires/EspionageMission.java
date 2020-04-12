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
package rotp.model.empires;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import rotp.model.galaxy.StarSystem;
import rotp.model.incidents.EspionageTechIncident;
import rotp.model.tech.Tech;
import rotp.util.Base;

public class EspionageMission implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    private final SpyNetwork spies;
    private final Spy spy;
    private EspionageTechIncident incident;
    private String stolenTech;
    private final StarSystem targetSystem;
    public Empire framedEmpire;
    // map of category ids and id of highest-rated available tech
    private final HashMap<String, String> techChoices = new HashMap<>();
    private final List<Empire> empiresToFrame = new ArrayList<>();

    public StarSystem targetSystem()   { return targetSystem; }

    public EspionageMission(SpyNetwork sn, Spy s, List<Tech> techs, StarSystem target) {
        spies = sn;
        spy = s;
        targetSystem = target;
        for (Tech t: techs) {
            String catId = t.cat.id();
            String currTechId = techChoices.get(catId);
            Tech currTech = tech(currTechId);
            if ((currTech == null) || (currTech.level < t.level))
                techChoices.put(catId, t.id());
        }

        List<Empire> commonContacts = new ArrayList<>();
        for (Empire emp1 : sn.owner().contactedEmpires()) {
            EmpireView  eview2 = sn.empire().viewForEmpire(emp1);
            if ((eview2 != null) && eview2.embassy().contact())
                commonContacts.add(emp1);
        }

        if (commonContacts.size() > 1) {
            Empire frame1 = random(commonContacts);
            commonContacts.remove(frame1);
            empiresToFrame.add(frame1);
            empiresToFrame.add(random(commonContacts));
        }
    }
    public void incident(EspionageTechIncident inc)  { incident = inc; }
    public void stealTech(Tech t)   {
        stolenTech = t.id();
        spies.owner().stealTech(stolenTech);
    }
    public void frameEmpire(Empire e) {
        if (e == null)
            return;
        framedEmpire = e;
        if (incident != null)
            incident.frameEmpire(e);
    }
    public Empire thief()                 { return framedEmpire != null ? framedEmpire : spies.view().owner(); }
    public Empire spyEmpire()             { return spies.view().owner(); }
    public String stolenTech()            { return stolenTech; }
    public Tech choice()                  { return tech(stolenTech); }
    public boolean hasStolenTech()        { return stolenTech != null; }
    public boolean canFrame()             { return spy.canFrame() && (empiresToFrame.size() > 1); }
    public boolean hasFramed()            { return framedEmpire != null; }
    public Tech inCategory(String id)     { return tech(techChoices.get(id)); }
    public List<Empire> empiresToFrame()  { return empiresToFrame; }
}
