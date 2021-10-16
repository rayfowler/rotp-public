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
import rotp.model.incidents.TradeIncomeIncident;
import rotp.ui.notifications.TradeTreatyMaturedAlert;
import rotp.util.Base;

public class TradeRoute implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    private static final int UNIT = 25;
    private final int emp1;
    private final int emp2;
    private int level = 0;
    private float profit = 0;
    private float ownerProd = 0;
    private float civProd = 0;

    public int level()               { return level; }
    public float profit()            { return profit; }
    public boolean active()          { return level > 0; }
    public boolean atFullLevel()     { return profit >= level; }
    public TradeRoute (EmpireView v) {
        emp1 = v.owner().id;
        emp2 = v.empire().id;
    }
    private EmpireView view()        { return galaxy().empire(emp1).viewForEmpire(emp2); }
    private EmpireView otherView()   { return galaxy().empire(emp2).viewForEmpire(emp1); }
    public void assessTurn() {
        EmpireView view = view();
        if (!view.inEconomicRange()) {
            stopRoute();
            return;
        }
        float pct = (roll(1,200) + view.embassy().relations() + 25) / 6000.0f;
        civProd = view.empire().totalPlanetaryProduction();
        ownerProd = view.owner().totalPlanetaryProduction();
        float prevProfit = profit;
        
        // anticipate maxLevel() potentially dropping as 
        // empires shrink
        if (level > maxLevel())
            level = maxLevel();
        
        profit = min(maxProfit(), profit + (pct * level) );
        if ((profit == level) && (profit > prevProfit)) {
            if (view.owner().isPlayer())
               TradeTreatyMaturedAlert.create(view.empId(), level);
        }
        if (active())
            TradeIncomeIncident.create(view, profit, profit/ownerProd);
        log(view+" Trade level: ", str(level), "  pct: ", str(pct), "  profit: ", str(profit));
    }
    public void setContact() {
        civProd = galaxy().empire(emp2).totalPlanetaryProduction();
        ownerProd = galaxy().empire(emp1).totalPlanetaryProduction();
    }
    public int maxLevel() {
        float maxLevel = smallerCivProd() / 4;
        return maxLevel < UNIT ? 0 : ((int)(maxLevel / UNIT) * UNIT);
    }
    public void startRoute(int newLevel) {
        float newTrade = newLevel - level;
        if (newTrade <= 0)
            return;
        
        float newPct = ((profit/newTrade) + startPct()) * (newTrade/newLevel);

        profit = newPct * newLevel;
        level = newLevel;
        view().owner().flagColoniesToRecalcSpending();

        //if not done yet, increase the route on the "other" side of the relationship
        EmpireView otherView = otherView();
        if (otherView.trade().level() != newLevel)
            otherView.trade().startRoute(newLevel);
    }
    public void stopRoute() {
        EmpireView otherView = otherView();
        level = 0;
        profit = 0;
        view().owner().flagColoniesToRecalcSpending();
        if (otherView.trade().active())
            otherView.trade().stopRoute();
    }
    private float maxProfit() {
        return (level * (1+galaxy().empire(emp1).tradePctBonus()));
    }
    private float startPct() {
        return (-.3f + galaxy().empire(emp1).tradePctBonus());
    }
    private float smallerCivProd() {
        return min(civProd,ownerProd);
    }
}