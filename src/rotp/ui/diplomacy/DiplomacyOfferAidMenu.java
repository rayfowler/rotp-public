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

import java.util.ArrayList;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.tech.Tech;

public class DiplomacyOfferAidMenu extends DiplomaticMessage {
    public List<Integer> amounts;
    public List<Tech> techs;
    public List<Integer> options = new ArrayList<>();
    public DiplomacyOfferAidMenu(String  s) {
        messageType = s;
    }
    @Override
    public void diplomat(Empire emp) { 
        super.diplomat(emp); 
        
        options.clear();
        int maxOffers = 5;
        amounts = player().diplomatAI().offerAidAmounts();
        techs = player().diplomatAI().offerableTechnologies(emp);
        for (int amt: amounts)
            options.add(OFFER_MONEY);
        
        maxOffers -= options.size();
        if (techs.size() > maxOffers) {
            techs.clear();
            options.add(OFFER_TECH);
        }
        else {
            for (Tech t: techs)
                options.add(OFFER_TECH);          
        }
        options.add(EXIT);
    }
    @Override
    public boolean showTalking()        { return false; }
    @Override
    public int numReplies()       		{ return options.size(); }
    @Override
    public String reply(int i)          { 
        if (options.get(i) == OFFER_MONEY)
            return text("DIPLOMACY_MENU_AMT", amounts.get(i));
        else if (options.get(i) == EXIT)
            return text("DIPLOMACY_MENU_FORGET_IT");
        else if (options.get(i) == OFFER_TECH) {
            if (techs.isEmpty())
                return text("DIPLOMACY_MENU_OFFER_TECHS");
            else {
                int techIndex = i - amounts.size();
                return techs.get(techIndex).name();
            }
        }
        else
            return ""; 
    }
    @Override
    public boolean enabled(int i)      { return i < options.size(); }
    @Override
    public void select(int i) {
        if (!enabled(i))
            return;

        log("DiplomacyOfferAidgitMenu - selected: ", str(i));
        DiplomaticReply reply;
        if (options.get(i) == OFFER_MONEY)
            reply = diplomat().diplomatAI().receiveFinancialAid(player(), amounts.get(i));  
        else if (options.get(i) == EXIT) {
             escape(); return;
        }
        else if (options.get(i) == OFFER_TECH) {
            if (techs.isEmpty()) {
                DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_OFFER_TECH_MENU); 
                return;
            }
            else {
                int techIndex = i-amounts.size();
                reply = diplomat().diplomatAI().receiveTechnologyAid(player(), techs.get(techIndex).id);
            }
        }
        else {
            escape(); return;
        }

        EmpireView diplomatView = diplomat().viewForEmpire(player());
        // get return menu for reply (after it's clicked)
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