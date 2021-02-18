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
import rotp.ui.RotPUI;

public class DiplomacyMainMenu extends DiplomaticMessage {
    private final List<Integer> options = new ArrayList<>();
    public DiplomacyMainMenu(String  s) {
        messageType = s;
    }
    @Override
    public void diplomat(Empire emp) { 
        super.diplomat(emp); 
        
        Diplomat plAI = player().diplomatAI();
        Empire dip = diplomat();
        
        options.clear();
        if (plAI.canOfferDiplomaticTreaties(dip))
            options.add(TREATY_MENU);
        if (plAI.canOfferTradeTreaty(dip))
            options.add(TRADE_MENU);
        if (plAI.canExchangeTechnology(dip))
            options.add(TECHNOLOGY_MENU);
        if (plAI.canOfferAid(dip))
            options.add(AID_MENU);
        // only add war menu if threaten not available
        if (plAI.canThreaten(dip))
            options.add(THREATEN_MENU);
        else if (plAI.canDeclareWar(dip))
            options.add(WAR_MENU);
        
        options.add(EXIT);
    }
    @Override
    public boolean showTalking()        { return false; }
    @Override
    public int numReplies()             { return options.size(); }
    @Override
    public boolean enabled(int i)       { return i < options.size(); }
    @Override
    public String reply(int i)          { 
        if (i >= options.size())
            return "";
        
        int choice = options.get(i);
        switch(choice) {
            case TREATY_MENU      : return text("DIPLOMACY_MENU_TREATIES");
            case TRADE_MENU       : return text("DIPLOMACY_MENU_TRADE");
            case TECHNOLOGY_MENU  : return text("DIPLOMACY_MENU_TECHNOLOGY");
            case AID_MENU         : return text("DIPLOMACY_MENU_OFFER_AID");
            case THREATEN_MENU    : return text("DIPLOMACY_MENU_THREATEN");
            case WAR_MENU         : return text("DIPLOMACY_MENU_DECLARE_WAR");
            case EXIT             : return text("DIPLOMACY_MENU_GOODBYE"); 
        }
        return ""; 
    }
    @Override
    public void select(int i) {
        if (!enabled(i))
            return;
        if (i >= options.size())
            return;

        Empire pl = player();
        log("DiplomacyMainMenu - selected: ", str(i));
        int choice = options.get(i);

        switch(choice) {
            case TREATY_MENU     : DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_TREATY_MENU); break;
            case TRADE_MENU      : 
                DiplomaticReply refusal = diplomat().diplomatAI().immediateRefusalToTrade(pl);
                if (refusal != null) {
                    refusal.returnMenu(DialogueManager.DIPLOMACY_MAIN_MENU);
                    DiplomaticMessage.reply(DiplomacyRequestReply.create(diplomat(), refusal));
                }
                else
                    DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_TRADE_MENU); 
                break;
            case TECHNOLOGY_MENU :
                if (pl.diplomatAI().techsAvailableForRequest(diplomat()).isEmpty()) {
                    DiplomaticReply reply = diplomat().viewForEmpire(pl.id).refuse(DialogueManager.DECLINE_TECH_TRADE);
                    reply.returnMenu(DialogueManager.DIPLOMACY_MAIN_MENU);
                    DiplomaticMessage.reply(DiplomacyRequestReply.create(diplomat(), reply));
                }
                else
                     DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_TECH_REQ_MENU); 
                break;
            case AID_MENU        : DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_OFFER_AID_MENU); break;
            case THREATEN_MENU   : DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_THREATEN_MENU); break;
            case WAR_MENU        : DiplomaticMessage.show(view(), DialogueManager.DIPLOMACY_DECLARE_WAR_MENU); break;
            case EXIT            : 
            default              : escape(); break;
        }
    }
    @Override
    public void escape() {
        RotPUI.instance().selectRacesPanel();		
    }
}