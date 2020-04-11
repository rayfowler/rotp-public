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

public class DiplomacyJointWarMenu extends DiplomaticMessage {
    private static int MAX_CHOICES = 5;
    private List<Empire> choices;
    public DiplomacyJointWarMenu(String  s) {
        messageType = s;
    }
    @Override
    public void diplomat(Empire v) { 
        super.diplomat(v); 
        choices = diplomat().nonEnemiesKnownBy(player());
        while (choices.size() > MAX_CHOICES) {
            Empire rando = random(choices);
            choices.remove(rando);
        }
    }
    @Override
    public boolean showTalking()        { return false; }
    @Override
    public int numReplies()       	{ return choices.size()+1; }
    @Override
    public String reply(int i)          { 
        if (i < choices.size())
            return choices.get(i).raceName();
        if (i == choices.size())
            return text("DIPLOMACY_MENU_FORGET_IT");
        return ""; 
    }
    @Override
    public boolean enabled(int i)       { return i <= choices.size(); }

    @Override
    public void select(int i) {
        if (!enabled(i))
            return;

        log("DiplomacyJointWarMenu - selected: ", str(i));
        if (i > choices.size())
            return;

        if (i == choices.size()) {
            escape();
            return;
        }
        // get the reply which contains text response from AI
        DiplomaticReply reply = diplomat().diplomatAI().receiveOfferJointWar(player(), choices.get(i));

        if (reply.accepted() && (reply instanceof DiplomaticCounterReply)) {
            DiplomaticCounterReply counter = (DiplomaticCounterReply) reply;
            DiplomacyRequestReply menu = DiplomacyCounterMenu.create(player(), diplomat(), counter);
            DiplomaticMessage.reply(menu);              
        }
        else {
            reply.returnMenu(DialogueManager.DIPLOMACY_MAIN_MENU);
            DiplomaticMessage.reply(DiplomacyRequestReply.create(diplomat(), reply));	
        }       
    }
    @Override
    public void escape() {
        DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_TREATY_MENU);
    }
}