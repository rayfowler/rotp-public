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
import rotp.model.empires.EmpireView;
import rotp.ui.diplomacy.DialogueManager;

public class TradeIncomeIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    public final int empMe;
    public final int empYou;
    private final float profit;

    public static void create(EmpireView ev, float profit, float pct) {
        ev.owner().diplomatAI().noticeIncident(new TradeIncomeIncident(ev, profit, pct), ev.empire());
    }
    public TradeIncomeIncident(EmpireView ev, float p, float pct) {
        profit = p;
        severity = min(30, max(0,pct*50));
        dateOccurred = galaxy().currentYear();
        empMe = ev.owner().id;
        empYou = ev.empire().id;
        
        if (ev.owner().diplomatAI().setSeverityAndDuration(this))
            return;
        duration = 3;
    }
    private DiplomaticEmbassy embassy() {
        return galaxy().empire(empMe).viewForEmpire(empYou).embassy();
    }
    @Override
    public String title()             { return text("INC_TRADE_INCOME_TITLE"); }
    @Override
    public String description()       { return decode(text("INC_TRADE_INCOME_DESC")); }
    @Override
    public void notifyOfPraise()      { embassy().tradePraised(true); }
    @Override
    public boolean triggersPraise()   { return super.triggersPraise() && !embassy().tradePraised(); }
    @Override
    public String praiseMessageId()   { return DialogueManager.PRAISE_TRADE; }
    @Override
    public String key() {
        return str(dateOccurred);
    }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        if ((profit < 10) && (profit > -10))
            s1 = s1.replace("[amt]", fmt(profit,1));
        else
            s1 = s1.replace("[amt]", str((int)profit));
        return s1;
    }
}
