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

import java.util.ArrayList;
import java.util.List;
import rotp.model.empires.DiplomaticEmbassy;
import rotp.model.empires.Empire;
import rotp.model.tech.Tech;

public class TechnologyAidIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    public final int empMe;
    public final int empYou;
    private final String techId;
    private List<String> techIds = new ArrayList<>();
    public static TechnologyAidIncident create(Empire emp, Empire donor, String techId) {
        DiplomaticEmbassy emb = emp.viewForEmpire(donor).embassy();
        TechnologyAidIncident inc = new TechnologyAidIncident(emp, donor, techId);
        
        // if we already have a financial incident with this key, then add it to
        // the existing one. Note: severity is eventually capped with no more ROE
        DiplomaticIncident prev = emb.getIncidentWithKey(inc.key());
        if (prev != null) {
            TechnologyAidIncident prevF = (TechnologyAidIncident) prev;
            prevF.addTech(emp, techId);
        }
        else
            emb.addIncident(inc);
        
        for (Empire enemy: emp.warEnemies()) 
            EnemyAidIncident.create(enemy, emp, donor, techId);

        return inc;
    }
    private TechnologyAidIncident(Empire emp, Empire donor, String tId) {
        empYou = donor.id;
        empMe = emp.id;
        techId = tId;
        addTech(emp, tId);
        dateOccurred = galaxy().currentYear();
        duration = 5;
    }
    private List<String> techIds() {
        if (techIds == null)
            techIds = new ArrayList<>();
        return techIds;
    }
    private void addTech(Empire emp, String tId) {
        // handle legacy instances
        if (techIds().isEmpty() && (techId != null))
            techIds.add(techId);

        if (!techIds.contains(tId))
            techIds.add(tId);
        
        int rpValue = 0;
        for (String id: techIds) {
            Tech tech = tech(id);
            rpValue += emp.ai().scientist().researchBCValue(tech);
        }
        float pct = rpValue / emp.totalPlanetaryProduction();
        severity = min(15,25*pct); 
    }
    @Override
    public String title()        { return text("INC_TECHNOLOGY_AID_TITLE"); }
    @Override
    public String description()  { 
        if (techIds().size() < 2)
            return decode(text("INC_TECHNOLOGY_AID_DESC")); 
        else
            return decode(text("INC_TECHNOLOGY_AID_DESC_MULT")); 
    }
    @Override
    public String key()          { return "Technology Aid:"+dateOccurred; }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        if ((techIds().size() < 2) && (techId != null))
            s1 = s1.replace("[tech]", tech(techId).name());
        else {
            String comma = text("INC_TECHNOLOGY_TECH_LIST_COMMA")+ " ";
            String list = "";
            for (int i=0;i<techIds.size();i++) {
                String id = techIds.get(i);
                if (i > 0)
                    list = list + comma;
                list = list+tech(id).name();
            }
            s1 = s1.replace("[techs]", list);
        }
            
        return s1;
    }
}
