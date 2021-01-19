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

public class FinancialAidIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int empMe;
    final int empYou;
    private final int amount;
    public static FinancialAidIncident create(Empire emp, Empire donor, int amt) {
        FinancialAidIncident inc = new FinancialAidIncident(emp, donor, amt);
        emp.viewForEmpire(donor).embassy().addIncident(inc);
        for (Empire enemy: emp.enemies()) 
            EnemyAidIncident.create(enemy, emp, donor, amt);

        return inc;
    }
    private FinancialAidIncident(Empire emp, Empire donor, int amt) {
        empYou = donor.id;
        empMe = emp.id;
        float pct = (float) amt / emp.totalPlanetaryProduction();
        severity = min(15,20*pct);
        amount = amt;
        dateOccurred = galaxy().currentYear();
        duration = 3;
    }
    @Override
    public String title()        { return text("INC_FINANCIAL_AID_TITLE"); }
    @Override
    public String description()  { return decode(text("INC_FINANCIAL_AID_DESC")); }
    @Override
    public String key()          { return "Financial Aid"; }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        s1 = s1.replace("[amt]", str(amount));
        return s1;
    }
}
