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

public class OfferPeaceMessage extends TurnNotificationMessage {
    public OfferPeaceMessage(String  s) {
        messageType = s;
    }
    @Override
    public void diplomat(Empire e)  {
        super.diplomat(e);
    }
    @Override
    public int numReplies()       		{ return 2; }
    @Override
    public boolean enabled(int i)       { return true; }
    @Override
    public String reply(int i)          { 
        switch (i) {
            case 0 : return text("DIPLOMACY_ACCEPT_END_WAR");
            case 1 : return text("DIPLOMACY_DECLINE_OFFER");
        }
        return ""; 
    }
    @Override
    public void select(int i) {
        log("OfferPeaceMessage - selected: ", str(i));
        switch(i) {
        case 0: 
            DiplomaticReply reply = player().diplomatAI().acceptOfferPeace(diplomat());
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
        player().diplomatAI().refuseOfferPeace(diplomat());
        session().resumeNextTurnProcessing();
    }
    @Override
    public String decode(String encodedMessage) { 
        String s1 = super.decode(encodedMessage); 
        return s1;
    }
}