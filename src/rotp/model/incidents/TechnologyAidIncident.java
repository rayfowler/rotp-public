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
import rotp.model.tech.Tech;

public class TechnologyAidIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int empMe;
    final int empYou;
    private final String techId;
    public static TechnologyAidIncident create(Empire emp, Empire donor, String techId) {
        TechnologyAidIncident inc = new TechnologyAidIncident(emp, donor, techId);
        emp.viewForEmpire(donor).embassy().addIncident(inc);
        for (Empire enemy: emp.enemies()) 
            EnemyAidIncident.create(enemy, emp, donor, techId);

        return inc;
    }
    private TechnologyAidIncident(Empire emp, Empire donor, String tId) {
        empYou = donor.id;
        empMe = emp.id;
        techId = tId;
        Tech tech = tech(tId);
        float rpValue = emp.ai().scientist().researchValue(tech);
        float pct = rpValue / emp.totalPlanetaryProduction();
        severity = min(25,100*pct);
        dateOccurred = galaxy().currentYear();
        duration = 10;
    }
    @Override
    public String title()        { return text("INC_TECHNOLOGY_AID_TITLE"); }
    @Override
    public String description()  { return decode(text("INC_TECHNOLOGY_AID_DESC")); }
    @Override
    public String key()          { return "Technology Aid: "+techId; }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        s1 = s1.replace("[tech]", tech(techId).name());
        return s1;
    }
}
