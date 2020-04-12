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
import rotp.model.empires.DiplomaticTreaty;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;

public class DiplomacyTreatyMenu extends DiplomaticMessage {
    private final List<Integer> options = new ArrayList<>();
    public DiplomacyTreatyMenu(String  s) {
        messageType = s;
    }
    @Override
    public void diplomat(Empire emp) { 
        super.diplomat(emp); 
        
        Empire pl = player();
        Empire dip = diplomat();
        EmpireView v = pl.viewForEmpire(dip);
        DiplomaticTreaty tr = v.embassy().treaty();
        
        // build a list of empires that the diplomat can declare war on
        List<Empire> nonEnemies = dip.nonEnemiesKnownBy(player());
        
        // build a list of empires that the diplomat can break an alliance with
        List<Empire> allies = dip.allies();
        allies.remove(pl);
        
        options.clear();
        if (tr.isWar())
            options.add(PROPOSE_PEACE);
        else if (!v.trade().active())
            options.add(PROPOSE_TRADE);
        else if (tr.isNoTreaty())
            options.add(PROPOSE_PACT);
        else if (tr.isPact())
            options.add(PROPOSE_ALLIANCE);
        
        if (tr.isPact())
            options.add(BREAK_PACT);
        else if (tr.isAlliance())
            options.add(BREAK_ALLIANCE);
        else if (v.trade().active())
            options.add(BREAK_TRADE);

        if (!tr.isWar() && !nonEnemies.isEmpty())
            options.add(PROPOSE_JOINT_WAR);
        
        options.add(EXIT);
    }
    @Override
    public boolean showTalking()                { return false; }
    @Override
    public int numReplies()       		{ return options.size(); }
    @Override
    public String reply(int i) {
        if (i >= options.size())
            return "";
        
        int choice = options.get(i);
        switch(choice) {
            case PROPOSE_PEACE          : return text("DIPLOMACY_MENU_PEACE");
            case PROPOSE_TRADE          : return text("DIPLOMACY_MENU_TRADE");
            case PROPOSE_PACT           : return text("DIPLOMACY_MENU_PACT");
            case PROPOSE_ALLIANCE       : return text("DIPLOMACY_MENU_ALLIANCE");
            case PROPOSE_JOINT_WAR      : return text("DIPLOMACY_MENU_JOINT_WAR");
            case BREAK_TRADE            : return text("DIPLOMACY_MENU_BREAK_TRADE");
            case BREAK_PACT             : return text("DIPLOMACY_MENU_BREAK_PACT");
            case BREAK_ALLIANCE         : return text("DIPLOMACY_MENU_BREAK_ALLIANCE");
            case EXIT                   : return text("DIPLOMACY_MENU_FORGET_IT"); 
        }
        return "";
    }
    @Override
    public boolean enabled(int i)  { return i < options.size(); }
    @Override
    public void select(int i) {
        if (!enabled(i))
            return;
        if (i >= options.size())
            return;

        int choice = options.get(i);
        DiplomaticReply reply;
        switch(choice) {
            case PROPOSE_PEACE          : reply = diplomat().diplomatAI().receiveOfferPeace(player());    break;
            case PROPOSE_TRADE          : DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_TRADE_MENU); return;
            case PROPOSE_PACT           : reply = diplomat().diplomatAI().receiveOfferPact(player());     break;
            case PROPOSE_ALLIANCE       : reply = diplomat().diplomatAI().receiveOfferAlliance(player()); break;
            case PROPOSE_JOINT_WAR      : DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_JOINT_WAR_MENU); return;
            case BREAK_TRADE            : reply = diplomat().diplomatAI().receiveBreakTrade(player());    break;
            case BREAK_PACT             : reply = diplomat().diplomatAI().receiveBreakPact(player());     break;
            case BREAK_ALLIANCE         : reply = diplomat().diplomatAI().receiveBreakAlliance(player()); break;
            case EXIT                   : DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_MAIN_MENU); return;
            default                     : DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_MAIN_MENU); return;
        }

        EmpireView diplomatView = diplomat().viewForEmpire(player());
        // get return menu for reply (after it's clicked)
        if (diplomatView.embassy().diplomatGone())
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