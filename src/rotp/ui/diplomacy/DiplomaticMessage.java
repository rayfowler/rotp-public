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
import rotp.model.empires.EmpireView;
import rotp.model.incidents.DiplomaticIncident;
import rotp.ui.RotPUI;
import rotp.ui.notifications.DiplomaticNotification;
import rotp.util.Base;

public abstract class DiplomaticMessage implements Base {
    protected static final int EXIT = -1;
    protected static final int PROPOSE_PEACE = 0;
    protected static final int PROPOSE_TRADE = 1;
    protected static final int PROPOSE_PACT = 2;
    protected static final int PROPOSE_ALLIANCE = 3;
    protected static final int PROPOSE_JOINT_WAR = 4;
    protected static final int PROPOSE_BREAK_ALLIANCE = 5;
    protected static final int BREAK_TRADE = 6;
    protected static final int BREAK_PACT = 7;
    protected static final int BREAK_ALLIANCE = 8;
    protected static final int DECLARE_WAR = 9;
    protected static final int OFFER_MONEY = 10;
    protected static final int OFFER_TECH = 11;
    protected static final int STOP_ATTACKING = 12;
    protected static final int STOP_SPYING = 13;
    protected static final int EVICT_SPIES = 14;
    protected static final int TREATY_MENU = 91;
    protected static final int TRADE_MENU = 92;
    protected static final int TECHNOLOGY_MENU = 93;
    protected static final int AID_MENU = 94;
    protected static final int THREATEN_MENU = 95;
    protected static final int WAR_MENU = 96;

    public static final int MAX_SELECTIONS = 6;
    private Empire diplomat;
    private DiplomaticIncident incident;
    protected String messageType = "";
    protected String remark;
    protected boolean returnToMap = false;

    public int numReplies()                      { return 1; }
    public int numDataLines()                    { return 0; }
    public String reply(int i)                   { return text("DIPLOMACY_MENU_CONTINUE"); }
    public String replyDetail(int i)             { return ""; }
    public String requestDetail()                { return ""; }
    public String dataLine(int i)                { return ""; }
    public boolean enabled(int i)                { return true; }
    public void escape()                         { }
    public void select(int i)                    { escape(); }

    public void init() {
        // clear vars used for individual messages
        remark = null;
        incident = null;
        diplomat = null;
        returnToMap = false;
    }
    public void diplomat(Empire v)               { diplomat = v; }
    public Empire diplomat()                     { return diplomat; }
    public void target(Empire v)                 { }
    public void incident(DiplomaticIncident v)   { incident = v; }
    public DiplomaticIncident incident()         { return incident; }
    public EmpireView view()                     { return diplomat.viewForEmpire(player()); }

    public boolean showTalking()                 { return true; }
    public DialogueManager manager()             { return DialogueManager.current(); }
    public void returnToMap(boolean b)           { returnToMap = b; }
    public boolean returnToMap()                 { return returnToMap; }
    public String remark() { 
        if (remark == null) 
            remark = decode(manager().randomMessage(messageType, diplomat()));
        return remark;
    }
    public String remark(Empire target) { 
        if (remark == null) {
            if (target == null)
               remark = decode(manager().randomMessage(messageType, diplomat()));
            else
               remark = decode(manager().randomMessage(messageType, diplomat()), target);
        }
        return remark;
    }
    public void remark(String s)                 { remark = s; }

    public static DiplomaticNotification show(EmpireView v, String type) {
        return show(v,type,false);
    }
    public static DiplomaticNotification show(EmpireView v, String type, boolean returnToMap) {
        DiplomaticNotification notif = new DiplomaticNotification(v, type);
        if (returnToMap)
            notif.setReturnToMap();
        RotPUI.instance().selectDiplomaticDialoguePanel(notif);
        return notif;
    }
    public static void reply(DiplomacyRequestReply reply) {
        RotPUI.instance().selectDiplomaticReplyPanel(reply);
    }
    public static void replyModal(DiplomacyRequestReply reply) {
        RotPUI.instance().selectDiplomaticReplyModalPanel(reply);
    }
    public String decode(String encodedMessage) { 
        String s1 = diplomat.decode(encodedMessage, player()); 
        if (incident != null)
            s1 = incident.decode(s1);

        return s1;
    }
    public String decode(String encodedMessage, Empire target) { 
        String s1 = diplomat.decode(encodedMessage, player(), target); 
        if (incident != null)
            s1 = incident.decode(s1);

        return s1;
    }
}