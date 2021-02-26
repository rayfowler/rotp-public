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
import java.util.List;
import rotp.util.Base;

public class SpyReport implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    private int spiesLost = 0;
    private int spiesCaptured = 0;
    private SpyNetwork.Mission confessedMission;
    private String stolenTech;
    public int framedEmpire;
    public boolean wasFramed;
    public int sabotageMission;
    public int sabotageSystem;
    public int sabotageCount;
    public List<String> techsLearned = new ArrayList<>();
 
    public int spiesLost()                       { return spiesLost; }
    public void addSpiesLost()                   { spiesLost++; }
    public int spiesCaptured()                   { return spiesCaptured; }
    public void addSpiesCaptured()               { spiesCaptured++; }
    public SpyNetwork.Mission confessedMission() { return confessedMission; }
    public void confessedMission(SpyNetwork.Mission m) { confessedMission = m; }
    public String stolenTech()                   { return stolenTech; }
    public void stolenTech(String id)            { stolenTech = id; }
    public void framedEmpire(Empire e)           { 
        framedEmpire = e == null ? Empire.NULL_ID : e.id; 
    }
    public Empire framedEmpire() { 
        return framedEmpire == Empire.NULL_ID ? null : galaxy().empire(framedEmpire); 
    }
    public void frame()                         { wasFramed = true; }
    public boolean wasFramed()                  { return wasFramed; }
    public int sabotageCount()                  { return sabotageCount; }
    public int sabotageMission()                { return sabotageMission; }
    public int sabotageSystem()                 { return sabotageSystem; }
    public List<String> techsLearned()          { return techsLearned; }
    public void recordSabotage(int mission, int system, int count) {
        sabotageMission = mission;
        sabotageSystem = system;
        sabotageCount = count;
    }
    public void recordTechsLearned(List<String> techs) {
        techsLearned.addAll(techs);
    }
    
    public boolean hasActivity() {
        return (spiesLost > 0) 
            || (spiesCaptured > 0)
            || (stolenTech != null)
            || (sabotageCount > 0)
            || !techsLearned.isEmpty()
            || wasFramed;
    }
    public void clear() {
        techsLearned.clear();
        spiesCaptured = 0;
        spiesLost = 0;
        wasFramed = false;
        confessedMission = null;
        stolenTech = null;
        framedEmpire = Empire.NULL_ID;
        sabotageMission = SabotageMission.NO_ACTION;
        sabotageCount = 0;
    }

}
