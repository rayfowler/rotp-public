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

public class DiplomacyCounterMenu extends DiplomacyRequestReply {
    public DiplomacyCounterMenu(String  s) {
        messageType = s;
    }
    public static DiplomacyCounterMenu create(Empire player, Empire requestor, DiplomaticCounterReply r) {
        DiplomacyCounterMenu msg = new DiplomacyCounterMenu();
        msg.diplomat(requestor);
        msg.reply(r);
        return msg;
    }
    private DiplomacyCounterMenu() { }
    @Override
    public void diplomat(Empire v)               {
        super.diplomat(v);
    }
    public DiplomaticCounterReply reply()  { return (DiplomaticCounterReply) reply; }
    @Override
    public boolean showTalking()        { return false; }
    @Override
    public int numDataLines()  {
        int lines = reply().techs.size();
        if (reply().bribe >= 100)
            lines++;
        return lines;
    }
    @Override
    public int numReplies()       	{ return 2; }
    @Override
    public String reply(int i)          {
        switch(i) {
            case 0: 
                if (player().totalReserve() < reply().bribe)
                    return text("DIPLOMACY_MENU_ACCEPT_NEED_BC");
                else
                    return text("DIPLOMACY_MENU_ACCEPT");
            case 1: return text("DIPLOMACY_MENU_FORGET_IT");
        }
        return "";
    }
    @Override
    public String dataLine(int i) { 
        if (i >= numDataLines())
            return ""; 
        List<String> techs = reply().techs;
        int bribe = reply().bribe;
        if (i < techs.size())
            return tech(techs.get(i)).name();
        if (bribe > 0)
            return text("RACES_DIPLOMACY_TRADE_AMT", bribe);
        return "";
    }
    @Override
    public boolean enabled(int i) {
        switch(i) {
            case 0: return player().totalReserve() >= reply().bribe;
            case 1: return true;
            default: return false;
        }
    }
    @Override
    public void select(int i) {
        if (!enabled(i))
            return;
        log("DiplomacyCounterMenu - selected: ", str(i));
        switch(i) {
            case 0: 
                // get the reply which contains text response from AI
                DiplomaticReply reply1 = diplomat().diplomatAI().receiveCounterJointWar(player(), reply());
                reply1.returnMenu(DialogueManager.DIPLOMACY_MAIN_MENU);
                DiplomaticMessage.reply(DiplomacyRequestReply.create(diplomat(), reply1));
                break;
            case 1:
                escape();
                return;
        }
    }
    @Override
    public void escape() {
        DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_MAIN_MENU);
    }
    @Override
    public String decode(String encodedMessage) {
        String s1 = super.decode(encodedMessage);
        return s1;
    }
}