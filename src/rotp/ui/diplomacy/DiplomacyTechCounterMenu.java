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

public class DiplomacyTechCounterMenu extends DiplomacyRequestReply {
    private Tech requestedTech;
    private List<Tech> counterOffers;
    public DiplomacyTechCounterMenu(String  s) {
        messageType = s;
    }
    public static DiplomacyTechCounterMenu create(Empire player, Empire requestor, DiplomaticReply r, Tech t) {
        DiplomacyTechCounterMenu msg = new DiplomacyTechCounterMenu();
        msg.requestedTech = t;
        msg.diplomat(requestor);
        msg.reply(r);
        //msg.counterOffers = requestor.ai().techsRequestedForCounter(msg.requestee,t);
        return msg;
    }
    private DiplomacyTechCounterMenu() { }
    @Override
    public void diplomat(Empire v)               {
        super.diplomat(v);
        counterOffers = diplomat().diplomatAI().techsRequestedForCounter(player(), requestedTech);
    }
    @Override
    public boolean showTalking()        { return false; }
    @Override
    public int numReplies()       		{ return counterOffers.size()+1; }
    @Override
    public String reply(int i)          {
        if (i < counterOffers.size()){
            Tech tech = counterOffers.get(i);
            return  text(tech.name());
        }

        if (i == counterOffers.size())
            return text("DIPLOMACY_MENU_FORGET_IT");

        return "";
    }
    @Override
    public String replyDetail(int i)          {
        if (i < counterOffers.size()){
            Tech tech = counterOffers.get(i);
            return text("TECH_TRADE_TIER_COST_INFO", str(tech.quintile), str((int) tech.researchCost()));
        }
        return "";
    }
    @Override
    public boolean enabled(int i) {
        return i <= counterOffers.size();
    }
    @Override
    public void select(int i) {
        if (!enabled(i))
                return;

        log("DiplomacyTechCounterMenu - selected: ", str(i));

        if (i > counterOffers.size())
            return;

        if (i == counterOffers.size()) {
            escape();
            return;
        }
        // get the reply which contains text response from AI
        DiplomaticReply reply1 = diplomat().diplomatAI().receiveCounterOfferTech(player(), counterOffers.get(i), requestedTech);

        // get return menu for reply (after it's clicked)
        reply1.returnMenu(DialogueManager.DIPLOMACY_MAIN_MENU);

        // show reply
        DiplomaticMessage.reply(DiplomacyRequestReply.create(diplomat(), reply1));
    }
    @Override
    public void escape() {
        DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_MAIN_MENU);
    }
    @Override
    public String decode(String encodedMessage) {
        String s1 = super.decode(encodedMessage);
        s1 = s1.replace("[tech]", requestedTech.name());
        return s1;
    }
}