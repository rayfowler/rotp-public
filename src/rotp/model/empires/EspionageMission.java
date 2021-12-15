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
    private final HashMap<String, List<String>> techPossibles = new HashMap<>();

    public StarSystem targetSystem()   { return targetSystem; }

    public EspionageMission(SpyNetwork sn, Spy s, List<Tech> techs, StarSystem target, List<Tech> possibleTechs) {
        spies = sn;
        spy = s;
        targetSystem = target;
        
        // build a list of possible techs to steal by category
        for (Tech t: techs) {
            String catId = t.cat.id();
            if (!techPossibles.containsKey(catId))
                techPossibles.put(catId, new ArrayList<>());
            techPossibles.get(catId).add(t.id());
        }
        
        // pre-select which tech will be stolen for each category
        for (String catId: techPossibles.keySet()) {
            String techToSteal = random(techPossibles.get(catId));
            techChoices.put(catId, techToSteal);
        }

        // build a list of potential empires to frame. Both the spy and victim
        // empire must be in a contact with the framed empire, and the framed
        // empire must be in economic range (i.e. able to spy) of the victim
        List<Empire> potentialFrames = new ArrayList<>();
        Empire spyEmp = sn.owner();
        Empire victimEmp = sn.empire();
        List<Empire> spyContacts = spyEmp.contactedEmpires();
        List<Empire> victimContacts = victimEmp.contactedEmpires();
        
        // a frameable empire must be:
        //  1) in contact with the victim
        //  2) in economic range of the victim
        //  3) not an ally of the victim
        for (Empire framedEmp : spyContacts) {
            if (victimContacts.contains(framedEmp) 
             && victimEmp.inEconomicRange(framedEmp.id) 
             && !victimEmp.alliedWith(framedEmp.id))
                potentialFrames.add(framedEmp);
        }

        // of all of the potential empires we can frame, we 
        // have the opportunity to choose between two to frame
        if (potentialFrames.size() > 1) {
            Empire frame1 = random(potentialFrames);
            potentialFrames.remove(frame1);
            empiresToFrame.add(frame1);
            empiresToFrame.add(random(potentialFrames));
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
    public Empire framedEmpire()          { return framedEmpire; }
    public Tech techChoice(String id)     { return tech(techChoices.get(id)); }
    public List<Empire> empiresToFrame()  { return empiresToFrame; }
    public List<String> possibleTechs(String id)  { return techPossibles.containsKey(id) ? techPossibles.get(id) : new ArrayList<>(); }
}
