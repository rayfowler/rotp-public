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

public class OfferTradeMessage extends TurnNotificationMessage {
    int tradeAmount = 0;
    public OfferTradeMessage(String  s) {
        messageType = s;
    }
    @Override
    public void diplomat(Empire e)  {
        super.diplomat(e);
    }
    public int tradeAmount()   {
        if (tradeAmount == 0)
            tradeAmount = view().trade().maxLevel();
        return tradeAmount;
    }
    @Override
    public int numReplies()       		{ return 2; }
    @Override
    public boolean enabled(int i)       { return true; }
    @Override
    public String reply(int i)          {
        switch (i) {
            case 0 : return text("DIPLOMACY_ACCEPT_ESTABLISH_TRADE");
            case 1 : return text("DIPLOMACY_DECLINE_OFFER");
        }
        return "";
    }
    @Override
    public void select(int i) {
        log("OfferTradeMessage - selected: ", str(i));
        switch(i) {
            case 0:
                DiplomaticReply reply = player().diplomatAI().acceptOfferTrade(diplomat(), tradeAmount());
                reply.resumeTurn(true);
                DiplomaticMessage.reply(DiplomacyRequestReply.create(diplomat(), reply));
                break;
            case 1:
            default:
                escape(); break;
        }
        // reset trade amount back to 0 so it will be re-initted for the next request
        tradeAmount = 0;
    }
    @Override
    public void escape() {
        player().diplomatAI().refuseOfferTrade(diplomat(), tradeAmount);
        session().resumeNextTurnProcessing();
    }
    @Override
    public String decode(String encodedMessage) {
        String s1 = super.decode(encodedMessage);
        s1 = s1.replace("[amt]", str(tradeAmount()));
        return s1;
    }
}