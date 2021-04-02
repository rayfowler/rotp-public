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
import rotp.ui.RotPUI;

public class DiplomacyDeclareWarMenu extends DiplomaticMessage {
    private final List<Integer> options = new ArrayList<>();
    public DiplomacyDeclareWarMenu(String  s) {
        messageType = s;
    }
    @Override
    public void diplomat(Empire emp)               { 
        super.diplomat(emp); 
        EmpireView v = player().viewForEmpire(emp);
        options.clear();
        options.add(DECLARE_WAR);
        options.add(EXIT);
    }
    @Override
    public boolean showTalking()        { return false; }
    @Override
    public int numReplies()             { return 2; }
    @Override
    public String reply(int i) { 
        if (i >= options.size())
            return "";
        
        int choice = options.get(i);
        switch(choice) {
            case DECLARE_WAR          : return text("DIPLOMACY_MENU_DECLARE_WAR");
            case EXIT                 : return text("DIPLOMACY_MENU_FORGET_IT"); 
        }
        return "";
    }
    @Override
    public boolean enabled(int i)        { return i < options.size();  }
    @Override
    public void select(int i) {
        if (!enabled(i))
            return;
        if (i >= options.size())
            return;

        int choice = options.get(i);
        DiplomaticReply reply;
        switch(choice) {
            case DECLARE_WAR          : reply = diplomat().diplomatAI().receiveDeclareWar(player());    break;
            case EXIT                 : escape(); return;
            default                   : escape(); return;
        }

        // get return menu for reply (after it's clicked)
        EmpireView diplomatView = diplomat().viewForEmpire(player());
        if (!diplomatView.diplomats())
            reply.returnMenu(null);
        else
            reply.returnMenu(DialogueManager.DIPLOMACY_MAIN_MENU);
        
        reply.returnToMap(returnToMap());

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