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

import java.util.List;
import rotp.model.incidents.AttackedAllyIncident;
import rotp.model.incidents.AttackedEnemyIncident;
import rotp.model.incidents.DiplomaticIncident;
import rotp.model.incidents.EnemyAidIncident;
import rotp.model.incidents.FinancialAidIncident;
import rotp.model.incidents.TechnologyAidIncident;
import rotp.ui.notifications.AllianceBreakingAlert;
import rotp.ui.notifications.AllianceWeakeningAlert;
import rotp.util.Base;

public class TreatyAlliance extends DiplomaticTreaty implements Base {
    private static final long serialVersionUID = 1L;
    private int standing = 50; // unused
    private int[] standings;
    private boolean[] wantToBreak = new boolean[2];
    public TreatyAlliance(Empire e1, Empire e2) {
        super(e1,e2,"RACES_ALLY");
        initStandings();
    }    
    @Override
    public boolean isAlliance()               { return true; }
    @Override
    public int listOrder()                    { return 3; }
    @Override
    public boolean wantToBreak(Empire emp)    { 
        int index = emp.id == empire1 ? 0 : 1;
        return wantToBreak[index];
    }
    public int standing(Empire e)            { 
        initStandings(); // backwards-save compatibility
        int index = e.id == empire1 ? 1 : 0;
        return standings[index];
    }
    @Override
    public void nextTurn(Empire emp)      { 
        initStandings();  // backwards-save compatibility
        int index = emp.id == empire1 ? 0 : 1;
        int prevStanding = standings[index];
        boolean isPlayerStanding = false;
        switch(index) {
            case 0: isPlayerStanding = empire2 == Empire.PLAYER_ID; break;
            case 1: isPlayerStanding = empire1 == Empire.PLAYER_ID; break;
        }

       
        Empire emp1 = galaxy().empire(empire1);
        Empire emp2 = galaxy().empire(empire2);
        List<Empire> enemies1 = emp1.warEnemies();
        List<Empire> enemies2 = emp2.warEnemies();

        int jointEnemies = 0;
        for (Empire e: enemies1) {
            if (enemies2.contains(e)) 
                jointEnemies++;
        }
        
        // now additionally degrade based on war responsibilities
        switch(jointEnemies) {
            case 0 : 
                int v = standings[index];
                if (v > 50) {
                    // if > 50, standing decays 10% towards 50 each turn
                    standings[index] = v - max(1, (100-v)/10);
                };
                break;
            case 1 : standings[index] -= 2; break;
            case 2 : standings[index] -= 4; break;
            default: standings[index] -= 5; break;
        }    
        
        //normalize so that max stnading is at least 50
        //this ensures that alliances break up only when there
        //is an imbalance in effort
        int maxStanding = max(standings[0], standings[1]);
        if (maxStanding < 50) {
            int adj = 50-maxStanding;
            standings[0] += adj;
            standings[1] += adj;
        }
        
        if (isPlayerStanding) {
            Empire nonPlayerEmpire = null;
            switch(index) {
                case 0: nonPlayerEmpire = emp1; break;
                case 1: nonPlayerEmpire = emp2; break;
            }
            if ((prevStanding >= 0) && (standings[index] < 0)) {
                AllianceBreakingAlert.create(nonPlayerEmpire);
            }
            else if ((prevStanding >= 25) && (standings[index] < 25)) {
                AllianceWeakeningAlert.create(nonPlayerEmpire);
            }
        }
        
        float chance = chanceBreak(index);
        wantToBreak[index] = random() < chance;
    }
    @Override
    public void noticeIncident(DiplomaticIncident inc) { 
        initStandings();
        if (inc instanceof TechnologyAidIncident) 
            handleTechnologyAid((TechnologyAidIncident) inc);
        else if (inc instanceof FinancialAidIncident) 
            handleFinancialAid((FinancialAidIncident) inc);
        else if (inc instanceof EnemyAidIncident) 
            handleEnemyAid((EnemyAidIncident) inc);
        else if (inc instanceof AttackedEnemyIncident) 
            handleAttackedEnemy((AttackedEnemyIncident) inc);
        else if (inc instanceof AttackedAllyIncident) 
            handleAttackedAlly((AttackedAllyIncident) inc);
    }
    private void handleTechnologyAid(TechnologyAidIncident inc) {
        if ((inc.empYou != empire1) && (inc.empYou != empire2))
            return;
        if ((inc.empMe != empire1) && (inc.empMe != empire2))
            return;
        
        int giver = inc.empYou == empire1 ? 1 : 0;
        standings[giver] += (int) inc.currentSeverity();
        
    }
    private void handleFinancialAid(FinancialAidIncident inc) {
        if ((inc.empYou != empire1) && (inc.empYou != empire2))
            return;
        if ((inc.empMe != empire1) && (inc.empMe != empire2))
            return;
        
        int giver = inc.empYou == empire1 ? 1 : 0;
        standings[giver] += (int) inc.currentSeverity();
    }
    private void handleEnemyAid(EnemyAidIncident inc) {
        if ((inc.empYou != empire1) && (inc.empYou != empire2))
            return;
        if ((inc.empMe != empire1) && (inc.empMe != empire2))
            return;
        
        int giver = inc.empYou == empire1 ? 1 : 0;
        standings[giver] += (int) inc.currentSeverity();
    }
    private void handleAttackedEnemy(AttackedEnemyIncident inc) {
        if ((inc.empAttacker != empire1) && (inc.empAttacker != empire2))
            return;
        if ((inc.empMe != empire1) && (inc.empMe != empire2))
            return;
        
        int giver = inc.empAttacker == empire1 ? 1 : 0;
        standings[giver] += (int) inc.currentSeverity();
    }
    private void handleAttackedAlly(AttackedAllyIncident inc) {
        if ((inc.empAttacker != empire1) && (inc.empAttacker != empire2))
            return;
        if ((inc.empMe != empire1) && (inc.empMe != empire2))
            return;
        
        int giver = inc.empAttacker == empire1 ? 1 : 0;
        standings[giver] += (int) inc.currentSeverity();
    }
    private void initStandings() {
        if (standings == null) {
            standings = new int[2];
            standings[0] = 50;
            standings[1] = 50;
        }
        if (wantToBreak == null) {
            wantToBreak = new boolean[2];
            wantToBreak[0] = false;
            wantToBreak[1] = false;
        }
    }
    private float chanceBreak(int i) {
        // won't check for break until at least 5% chance 
        if (standings[i] >= -5)
            return 0f;
        return -standings[i]/100f;
    }
}
