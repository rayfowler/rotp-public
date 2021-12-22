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
import rotp.model.tech.Tech;

public class DiplomacyTechRequestMenu extends DiplomaticMessage {
    private List<Tech> choices;
    public DiplomacyTechRequestMenu(String  s) {
        messageType = s;
    }
    @Override
    public void diplomat(Empire v)               { 
        super.diplomat(v); 
        choices = player().diplomatAI().techsAvailableForRequest(diplomat());
    }
    @Override
    public boolean showTalking()        { return false; }
    @Override
    public int numReplies()       		{ return choices.size()+1; }
    @Override
    public String reply(int i)          { 
        if (i < choices.size()){
            Tech tech = choices.get(i);
            return  text(tech.name());
        }
        if (i == choices.size())
            return text("DIPLOMACY_MENU_FORGET_IT");
        return ""; 
    }
    @Override
    public String replyDetail(int i)          {
        if (i < choices.size()){
            Tech tech = choices.get(i);
            return text("TECH_TRADE_TIER_COST_INFO", str(tech.quintile), str((int) tech.researchCost()));
        }
        return "";
    }
    @Override
    public boolean enabled(int i) { 
        return i <= choices.size();
    }
    @Override
    public void select(int i) {
        if (!enabled(i))
            return;

        log("DiplomacyTechRequestMenu - selected: ", str(i));

        if (i == choices.size()) {
            escape();
            return;
        }
        // get the reply which contains text response from AI
        Tech requestedTech = choices.get(i);
        DiplomaticReply reply = diplomat().diplomatAI().receiveRequestTech(player(), requestedTech);

        if (reply.accepted()) {
            DiplomacyRequestReply menu = DiplomacyTechCounterMenu.create(player(), diplomat(), reply, requestedTech);
            DiplomaticMessage.reply(menu);
        }
        else {
            reply.returnMenu(DialogueManager.DIPLOMACY_MAIN_MENU);
            DiplomaticMessage.reply(DiplomacyRequestReply.create(diplomat(), reply));	
            return;
        }
        // a null reply means we need to create a follow-up menu of counter-offer techs to choose from
    }
    @Override
    public void escape() {
        DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_MAIN_MENU);
    }
}