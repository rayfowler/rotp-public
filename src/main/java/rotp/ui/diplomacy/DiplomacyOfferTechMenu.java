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
import rotp.model.tech.Tech;

public class DiplomacyOfferTechMenu extends DiplomaticMessage {
    private List<Tech> choices = new ArrayList<>();
    public DiplomacyOfferTechMenu(String  s) {
        messageType = s;
    }
    @Override
    public void diplomat(Empire emp)               { 
        super.diplomat(emp); 
        choices = player().diplomatAI().offerableTechnologies(emp);
    }
    @Override
    public boolean showTalking()            { return false; }
    @Override
    public int numReplies()          	    { return choices.size()+1; }
    @Override
    public String reply(int i)          { 
        if (i < choices.size())
            return text(choices.get(i).name());
        if (i == choices.size())
            return text("DIPLOMACY_MENU_FORGET_IT");
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

        log("DiplomacyOfferTechMenu - selected: ", str(i));

        if (i == choices.size()) {
            escape();
            return;
        }
        // get the reply which contains text response from AI
        DiplomaticReply reply = diplomat().diplomatAI().receiveTechnologyAid(player(),  choices.get(i).id);

        reply.returnMenu(DialogueManager.DIPLOMACY_MAIN_MENU);
        DiplomaticMessage.reply(DiplomacyRequestReply.create(diplomat(), reply));	
    }
    @Override
    public void escape() {
        DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_MAIN_MENU);
    }
}