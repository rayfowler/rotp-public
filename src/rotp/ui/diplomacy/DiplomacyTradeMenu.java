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
package rotp.ui.diplomacy;

import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;

public class DiplomacyTradeMenu extends DiplomaticMessage {
    private List<Integer> choices;
    public DiplomacyTradeMenu(String  s) {
        messageType = s;
    }
    @Override
    public void diplomat(Empire emp)               { 
        super.diplomat(emp); 
        EmpireView v = player().viewForEmpire(emp);
        choices = v.nominalTradeLevels();
    }
    @Override
    public boolean showTalking()        { return false; }
    @Override
    public int numReplies()             { return choices.size()+1; }
    @Override
    public String reply(int i) { 
        int maxIndex = choices.size();
        if (i < choices.size())
            return text("DIPLOMACY_TRADE_LEVEL",str(choices.get(i)));
        if (i == maxIndex)
            return text("DIPLOMACY_MENU_FORGET_IT");
        return ""; 
    }
    @Override
    public boolean enabled(int i)        { return i <= choices.size();  }
    @Override
    public void select(int i) {
        if (!enabled(i))
            return;

        log("DiplomacyTradeMenu - selected: ", str(i));
        int maxIndex = choices.size();
        if (i > maxIndex)
            return;
        if (i == maxIndex) {
            escape();
            return;
        }
        DiplomaticReply reply = diplomat().diplomatAI().receiveOfferTrade(player(), choices.get(i));

        // get return menu for reply (after it's clicked)
        EmpireView diplomatView = diplomat().viewForEmpire(player());
        if (!diplomatView.diplomats())
            reply.returnMenu(null);
        else
            reply.returnMenu(DialogueManager.DIPLOMACY_MAIN_MENU);

        // show reply
        DiplomaticMessage.reply(DiplomacyRequestReply.create(diplomat(), reply));	
    }
    @Override
    public void escape() {
        DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_MAIN_MENU);
    }
}