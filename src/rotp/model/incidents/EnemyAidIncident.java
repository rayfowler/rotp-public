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

import rotp.model.empires.DiplomaticEmbassy;
import rotp.model.empires.Empire;
import rotp.model.tech.Tech;

public class EnemyAidIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    public final int empMe;
    public final int empYou;
    public final int empEnemy;
    private int amount;
    private String techId;
    public static EnemyAidIncident create(Empire emp, Empire enemy, Empire donor, int amt) {
        // it's possible to give aid to someone who is not at war with you but is
        // on the enemies list because undeclared war footing
        if (emp == donor)
            return null;

        DiplomaticEmbassy emb = emp.viewForEmpire(donor).embassy();
        
        EnemyAidIncident inc = new EnemyAidIncident(emp, enemy, donor, amt);
        // if we already have an enemy aid incident with this key, then add it to
        // the existing one. Note: severity is eventually capped with no more ROE
        DiplomaticIncident prev = emb.getIncidentWithKey(inc.key());
        if (prev != null) {
            EnemyAidIncident prevF = (EnemyAidIncident) prev;
            prevF.setAmount(emp, prevF.amount+inc.amount);
        }
        else
            emb.addIncident(inc);
        return inc;
    }
    public static EnemyAidIncident create(Empire emp, Empire enemy, Empire donor, String tId) {
        // it's possible to give aid to someone who is not at war with you but is
        // on the enemies list because undeclared war footing
        if (emp == donor)
            return null;

        EnemyAidIncident inc = new EnemyAidIncident(emp, enemy, donor, tId);
        emp.viewForEmpire(donor).embassy().addIncident(inc);
        return inc;
    }
    private EnemyAidIncident(Empire emp, Empire enemy, Empire donor, int amt) {
        empMe = emp.id;
        empYou = donor.id;
        empEnemy = enemy.id;
        setAmount(emp, amt);
        techId = null;
        dateOccurred = galaxy().currentYear();
        duration = 5;
    }
    private void setAmount(Empire emp, int amt) {
        float pct = (float) amt / emp.totalPlanetaryProduction();
        float sev = min(5,5*pct);
        severity = -sev;
        amount = amt;
    }
    private EnemyAidIncident(Empire emp, Empire enemy, Empire donor, String tId) {
        log("creating enemy aid incident: "+enemy.raceName()+"  tech:"+tech(tId).name());
        empMe = emp.id;
        empYou = donor.id;
        empEnemy = enemy.id;
        techId = tId;
        amount = 0;
        Tech tech = tech(tId);
        float rpValue = enemy.ai().scientist().warTradeBCValue(tech);
        float pct = rpValue / enemy.totalPlanetaryProduction();
        float sev = min(10,15*pct);
        severity = -sev;
        dateOccurred = galaxy().currentYear();
        duration = 5;
    }
    @Override
    public String title()        { return text("INC_ENEMY_AID_TITLE"); }
    @Override
    public String description()  { return techId == null ? decode(text("INC_ENEMY_AID_MONEY_DESC")) :  decode(text("INC_ENEMY_AID_TECH_DESC")); }
    @Override
    public String key()          { return techId == null ? "Enemy Aid" : "Enemy Aid: "+techId; }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        s1 = galaxy().empire(empEnemy).replaceTokens(s1, "enemy");
        if (amount > 0)
            s1 = s1.replace("[amt]", str(amount));
        if (techId != null)
            s1 = s1.replace("[tech]", tech(techId).name());
        return s1;
    }
}
