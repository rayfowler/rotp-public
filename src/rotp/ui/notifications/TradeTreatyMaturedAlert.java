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
package rotp.ui.notifications;

import rotp.model.empires.Empire;
import rotp.model.game.GameSession;

public class TradeTreatyMaturedAlert extends GameAlert {
    private final int empId;
    private final int amt;
     public static void create(int id, int tradeAmt) {
        GameSession.instance().addAlert(new TradeTreatyMaturedAlert(id, tradeAmt));
    }
    @Override
    public String description() {
        Empire emp = galaxy().empire(empId);
        String str1 = text("MAIN_ALERT_TRADE_MATURED", emp.name(), str(amt));
        str1 = emp.replaceTokens(str1, "alien");
        return str1;
    }
    private TradeTreatyMaturedAlert(int id, int tradeAmt) {
        empId = id;
        amt = tradeAmt;
    }
}
