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
import rotp.model.ai.interfaces.Diplomat;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.ui.RotPUI;

public class DiplomacyThreatenMenu extends DiplomaticMessage {
    private final List<Integer> options = new ArrayList<>();
    public DiplomacyThreatenMenu(String  s) {
        messageType = s;
    }
    @Override
    public void diplomat(Empire emp) { 
        super.diplomat(emp); 
        
        Diplomat plAI = player().diplomatAI();
        Empire dip = diplomat();
        
        options.clear();
        if (plAI.canEvictSpies(dip))
            options.add(EVICT_SPIES);
        if (plAI.canThreatenSpying(dip))
            options.add(STOP_SPYING);
        if (plAI.canThreatenAttacking(dip))
            options.add(STOP_ATTACKING);
        if (plAI.canDeclareWar(dip))
            options.add(WAR_MENU);
        
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
            case EVICT_SPIES      : return text("DIPLOMACY_MENU_EVICT_SPIES");
            case STOP_SPYING      : return text("DIPLOMACY_MENU_STOP_SPYING");
            case STOP_ATTACKING   : return text("DIPLOMACY_MENU_STOP_ATTACKING");
            case WAR_MENU         : return text("DIPLOMACY_MENU_DECLARE_WAR");
            case EXIT             : return text("DIPLOMACY_MENU_FORGET_IT"); 
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
            case EVICT_SPIES     : reply = diplomat().diplomatAI().receiveThreatEvictSpies(player());    break;
            case STOP_SPYING     : reply = diplomat().diplomatAI().receiveThreatStopSpying(player());    break;
            case STOP_ATTACKING  : reply = diplomat().diplomatAI().receiveThreatStopAttacking(player());     break;
            case WAR_MENU        : DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_DECLARE_WAR_MENU, returnToMap()); return;
            case EXIT            : escape(); return;
            default              : escape(); return;
        }
        // get return menu for reply (after it's clicked)
        EmpireView diplomatView = diplomat().viewForEmpire(player());
        if (!diplomatView.diplomats())
            reply.returnMenu(null);
        else
            reply.returnMenu(DialogueManager.DIPLOMACY_MAIN_MENU);

        reply.returnToMap(returnToMap);
        // show reply
        DiplomaticMessage.reply(DiplomacyRequestReply.create(diplomat(), reply));	
    }
    @Override
    public void escape() {
        if (returnToMap)
            RotPUI.instance().selectMainPanel();
        else
            DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_MAIN_MENU);
    }
}