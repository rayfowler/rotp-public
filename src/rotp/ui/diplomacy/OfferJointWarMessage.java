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

import rotp.model.empires.Empire;

public class OfferJointWarMessage extends TurnNotificationMessage {
    Empire target;
    public OfferJointWarMessage(String  s) {
        messageType = s;
    }
    @Override
    public void diplomat(Empire e)  {
        super.diplomat(e);
    }
    @Override
    public void target(Empire e)        { target = e; }
    @Override
    public int numReplies()       	{ return 2; }
    @Override
    public boolean enabled(int i)       { return true; }
    @Override
    public String reply(int i)          { 
        switch (i) {
            case 0 : return text("DIPLOMACY_ACCEPT_JOIN_WAR");
            case 1 :
                if (player().alliedWith(diplomat().id) && diplomat().atWarWith(target.id))
                    return text("DIPLOMACY_DECLINE_BREAK_ALLIANCE");
                else
                    return text("DIPLOMACY_DECLINE_OFFER");
        }
        return ""; 
    }
    @Override
    public void select(int i) {
        log("OfferPactMessage - selected: ", str(i));
        switch(i) {
        case 0: 
            DiplomaticReply reply = player().diplomatAI().acceptOfferJointWar(diplomat(), target);
            reply.resumeTurn(true);
            DiplomaticMessage.reply(DiplomacyRequestReply.create(diplomat(), reply));	
            break;
        case 1: 
        default:
            escape(); break;
        }
    }
    @Override
    public void escape() {
        DiplomaticReply reply = player().diplomatAI().refuseOfferJointWar(diplomat(), target);
        if (reply == null) {
            session().resumeNextTurnProcessing();
            return;
        }
            
        reply.resumeTurn(true);
        DiplomaticMessage.reply(DiplomacyRequestReply.create(diplomat(), reply));	
    }
    @Override
    public String decode(String encodedMessage) { 
        
        String s1 = diplomat().decode(encodedMessage, player(), target); 
        return s1;
    }
}