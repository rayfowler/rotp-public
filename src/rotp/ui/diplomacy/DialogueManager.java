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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.incidents.DiplomaticIncident;
import rotp.util.Base;

public class DialogueManager implements Base {
    public static final String DIALOG_FILENAME = "data/dialogue.txt";
    // these values are used in the dialogue.txt file to identify message types
    public static final String CONTACT_DIPLOMATIC       = "Contact-Diplomatic";
    public static final String CONTACT_PACIFIST         = "Contact-Pacifist";
    public static final String CONTACT_HONORABLE        = "Contact-Honorable";
    public static final String CONTACT_RUTHLESS         = "Contact-Ruthless";
    public static final String CONTACT_AGGRESSIVE       = "Contact-Aggressive";
    public static final String CONTACT_XENOPHOBIC       = "Contact-Xenophobic";
    public static final String CONTACT_ERRATIC          = "Contact-Erratic";
    public static final String DIPLOMACY_MAIN_MENU      = "DiplomacyMainMenu";
    public static final String DIPLOMACY_TRADE_MENU     = "DiplomacyTradeMenu";
    public static final String DIPLOMACY_OFFER_TECH_MENU  = "DiplomacyOfferTechMenu";
    public static final String DIPLOMACY_TECH_REQ_MENU  = "DiplomacyTechRequestMenu";
    public static final String DIPLOMACY_TECH_CTR_MENU  = "DiplomacyTechCounterMenu";
    public static final String DIPLOMACY_TREATY_MENU    = "DiplomacyTreatyMenu";
    public static final String DIPLOMACY_THREATEN_MENU  = "DiplomacyThreatenMenu";
    public static final String DIPLOMACY_DECLARE_WAR_MENU = "DiplomacyDeclareWarMenu";
    public static final String DIPLOMACY_JOINT_WAR_MENU = "DiplomacyJointWarMenu";
    public static final String DIPLOMACY_OFFER_AID_MENU = "DiplomacyOfferAidMenu";
    public static final String DECLINE_ANNOYED          = "DeclineAnnoyed";
    public static final String DECLINE_OFFER            = "DeclineOffer";
    public static final String DECLINE_TECH_TRADE       = "DeclineTechTrade";
    public static final String DECLINE_OATHBREAKER      = "DeclineOathbreaker";
    public static final String DECLINE_ESPIONAGE        = "DeclineEspionage";
    public static final String DECLINE_SABOTAGE         = "DeclineSabotage";
    public static final String DECLINE_BUILDUP          = "DeclineBuildup";
    public static final String DECLINE_ENCROACH         = "DeclineEncroach";
    public static final String DECLINE_SKIRMISH         = "DeclineSkirmish";
    public static final String DECLINE_ATTACK           = "DeclineAttack";
    public static final String DECLINE_INVASION         = "DeclineInvasion";
    public static final String DECLINE_BIOWEAPONS       = "DeclineBioweapons";
    public static final String DECLINE_ENEMY_ALLY       = "DeclineEnemyAlly";
    public static final String DECLINE_ALREADY_ALLIED   = "DeclineAlreadyAllied";
    public static final String DECLINE_NO_WAR_ON_ALLY   = "DeclineNoWarOnAlly";
    public static final String DECLINE_PEACE_TREATY     = "DeclinePeaceTreaty";
    public static final String OFFER_TRADE              = "OfferTrade";
    public static final String ANNOUNCE_TRADE           = "AnnounceTrade";
    public static final String ACCEPT_TRADE             = "AcceptTrade";
    public static final String BREAK_TRADE              = "BreakTrade";
    public static final String OFFER_TECH_EXCHANGE      = "OfferTechExchange";
    public static final String ACCEPT_TECH_EXCHANGE     = "AcceptTechExchange";
    public static final String ACCEPT_FINANCIAL_AID     = "AcceptFinancialAid";
    public static final String ACCEPT_TECHNOLOGY_AID    = "AcceptTechnologyAid";
    public static final String OFFER_PEACE              = "OfferPeace";
    public static final String ANNOUNCE_PEACE           = "AnnouncePeace";
    public static final String ACCEPT_PEACE             = "AcceptPeace";
    public static final String OFFER_PACT               = "OfferPact";
    public static final String ANNOUNCE_PACT            = "AnnouncePact";
    public static final String ACCEPT_PACT              = "AcceptPact";
    public static final String BREAK_PACT               = "BreakPact";
    public static final String OFFER_ALLIANCE           = "OfferAlliance";
    public static final String ANNOUNCE_ALLIANCE        = "AnnounceAlliance";
    public static final String ACCEPT_ALLIANCE          = "AcceptAlliance";
    public static final String BREAK_ALLIANCE           = "BreakAlliance";
    public static final String OFFER_JOINT_WAR          = "OfferJointWar";
    public static final String ACCEPT_JOINT_WAR         = "AcceptJointWar";
    public static final String COUNTER_JOINT_WAR        = "CounterJointWar";
    public static final String RESPOND_CLOSE_EMBASSY    = "RespondCloseEmbassy";
    public static final String RESPOND_BREAK_TRADE      = "RespondBreakTrade";
    public static final String RESPOND_BREAK_PACT       = "RespondBreakPact";
    public static final String RESPOND_BREAK_ALLIANCE   = "RespondBreakAlliance";
    public static final String RESPOND_IGNORE_THREAT    = "RespondIgnoreThreat";
    public static final String RESPOND_STOP_SPYING      = "RespondStopSpying";
    public static final String RESPOND_STOP_ATTACKING   = "RespondStopAttacking";
    
    public static final String TRANSPORTS_PERISHED      = "TransportsPerished";
    public static final String TRANSPORTS_KILLED        = "TransportsKilled";
    public static final String ENEMY_TRANSPORTS_KILLED  = "EnemyTransportsKilled";
    public static final String PRAISE_COUNCIL_VOTE      = "Praise-CouncilVote";
    public static final String PRAISE_ATTACKED_ENEMY    = "Praise-AttackedEnemy";
    public static final String PRAISE_TRADE             = "Praise-Trade";
    public static final String PRAISE_REBELLING_WITH    = "Praise-RebellingWith";
    public static final String WARNING_COUNCIL_VOTE     = "Warning-CouncilVote";
    public static final String WARNING_OATHBREAKER      = "Warning-Oathbreaker";
    public static final String WARNING_ATTACKED_ALLY    = "Warning-AttackedAlly";
    public static final String WARNING_SKIRMISH         = "Warning-Skirmish";
    public static final String WARNING_TRESPASSING      = "Warning-Trespassing";
    public static final String WARNING_ENCROACHING      = "Warning-Encroaching";
    public static final String WARNING_EXPANSION        = "Warning-Expansion";
    public static final String WARNING_BUILDUP          = "Warning-Buildup";
    public static final String WARNING_ESPIONAGE        = "Warning-Espionage";
    public static final String WARNING_SABOTAGE         = "Warning-Sabotage";
    public static final String WARNING_COLONY_ATTACKED  = "Warning-ColonyAttacked";
    public static final String WARNING_COLONY_INVADED   = "Warning-ColonyInvaded";
    public static final String WARNING_GENOCIDE         = "Warning-Genocide";
    public static final String WARNING_FINAL_WAR        = "Warning-FinalWar";
    public static final String WARNING_BIOWEAPON        = "Warning-Bioweapon";
    public static final String WARNING_REBELLING_AGAINST = "Warning-RebellingAgainst";
    public static final String DECLARE_ASSASSIN_WAR     = "DeclareWar-Assassin";
    public static final String DECLARE_ATTACKED_WAR     = "DeclareWar-Attack";
    public static final String DECLARE_HATE_WAR         = "DeclareWar-Hate";
    public static final String DECLARE_OPPORTUNITY_WAR  = "DeclareWar-Opportunity";
    public static final String DECLARE_SPYING_WAR       = "DeclareWar-Spying";
    public static final String DECLARE_ERRATIC_WAR      = "DeclareWar-Erratic"; 
    public static final String DECLARE_MILITARY_WAR     = "DeclareWar-Military"; 
    public static final String DECLARE_ALLIANCE_WAR     = "DeclareWar-Alliance"; 

    public HashMap<String, DiplomaticMessage> messages = new HashMap<>();
    public List<DialogString> strings = new ArrayList<>();
    private static final DialogueManager instance = new DialogueManager(DIALOG_FILENAME);
    public static DialogueManager current()       { return instance; }
    public DialogueManager(String filename) {
        loadDialogStrings(filename);
        loadMessages();
    }
    public DiplomaticMessage message(String type, DiplomaticIncident inc, Empire diplomat) {
        if (!messages.containsKey(type))
            return null;
        DiplomaticMessage msg = messages.get(type);
        msg.init();
        msg.diplomat(diplomat);
        msg.incident(inc);
        return msg;
    }
    public DiplomaticMessage message(String type, DiplomaticIncident inc, Empire diplomat, Empire target) {
        if (!messages.containsKey(type))
            return null;
        DiplomaticMessage msg = messages.get(type);
        msg.init();
        msg.diplomat(diplomat);
        msg.target(target);
        msg.incident(inc);
        return msg;
    }
    public String randomMessage(String type, Empire speaker) {
        // find all dialog strings of proper type
        EmpireView view = speaker.isPlayer() ? null : speaker.viewForEmpire(player());
        List<DialogString> matchingStrings = new ArrayList<>();
        for (DialogString str: strings) {
            if (str.matchesType(type) && str.fitsContext(view))
                matchingStrings.add(str); 
        }
        if (matchingStrings.isEmpty())
            return concat("Message not found. type:", type);
        else 
            return speaker.race().dialogue(random(matchingStrings).key());
    }
    public String randomMessage(String type, EmpireView view) {
        // find all dialog strings of proper type
        Empire diplomat = view.empire();
        List<DialogString> matchingStrings = new ArrayList<>();
        for (DialogString str: strings) {
            if (str.matchesType(type) && str.fitsContext(view))
                matchingStrings.add(str); 
        }
        if (matchingStrings.isEmpty())
            return concat("Message not found. type:", type);
        else 
            return diplomat.race().dialogue(random(matchingStrings).key());
    }
    private void loadMessages() {
        addMessage(new TurnNotificationMessage(CONTACT_DIPLOMATIC));
        addMessage(new TurnNotificationMessage(CONTACT_PACIFIST));
        addMessage(new TurnNotificationMessage(CONTACT_HONORABLE));
        addMessage(new TurnNotificationMessage(CONTACT_RUTHLESS));
        addMessage(new TurnNotificationMessage(CONTACT_AGGRESSIVE));
        addMessage(new TurnNotificationMessage(CONTACT_XENOPHOBIC));
        addMessage(new TurnNotificationMessage(CONTACT_ERRATIC));
        addMessage(new DiplomacyMainMenu(DIPLOMACY_MAIN_MENU));
        addMessage(new DiplomacyTradeMenu(DIPLOMACY_TRADE_MENU));
        addMessage(new DiplomacyTechRequestMenu(DIPLOMACY_TECH_REQ_MENU));
        addMessage(new DiplomacyTechCounterMenu(DIPLOMACY_TECH_CTR_MENU));
        addMessage(new DiplomacyTreatyMenu(DIPLOMACY_TREATY_MENU));
        addMessage(new DiplomacyThreatenMenu(DIPLOMACY_THREATEN_MENU));
        addMessage(new DiplomacyDeclareWarMenu(DIPLOMACY_DECLARE_WAR_MENU));
        addMessage(new DiplomacyJointWarMenu(DIPLOMACY_JOINT_WAR_MENU));
        addMessage(new DiplomacyOfferAidMenu(DIPLOMACY_OFFER_AID_MENU));
        addMessage(new DiplomacyOfferTechMenu(DIPLOMACY_OFFER_TECH_MENU));
        addMessage(new OfferTradeMessage(OFFER_TRADE));
        addMessage(new DiplomacyTechOfferMenu(OFFER_TECH_EXCHANGE));
        addMessage(new OfferPeaceMessage(OFFER_PEACE));
        addMessage(new OfferPactMessage(OFFER_PACT));
        addMessage(new OfferAllianceMessage(OFFER_ALLIANCE));
        addMessage(new OfferJointWarMessage(OFFER_JOINT_WAR));
        addMessage(new SimpleMessage(DECLINE_ANNOYED));
        addMessage(new SimpleMessage(DECLINE_OFFER));
        addMessage(new SimpleMessage(DECLINE_TECH_TRADE));
        addMessage(new SimpleMessage(DECLINE_OATHBREAKER));
        addMessage(new SimpleMessage(DECLINE_ESPIONAGE));
        addMessage(new SimpleMessage(DECLINE_SABOTAGE));
        addMessage(new SimpleMessage(DECLINE_BUILDUP));
        addMessage(new SimpleMessage(DECLINE_ENCROACH));
        addMessage(new SimpleMessage(DECLINE_SKIRMISH));
        addMessage(new SimpleMessage(DECLINE_ATTACK));
        addMessage(new SimpleMessage(DECLINE_INVASION));
        addMessage(new SimpleMessage(DECLINE_BIOWEAPONS));
        addMessage(new SimpleMessage(DECLINE_ENEMY_ALLY));
        addMessage(new SimpleMessage(DECLINE_NO_WAR_ON_ALLY));
        addMessage(new SimpleMessage(DECLINE_PEACE_TREATY));
        addMessage(new SimpleMessage(ACCEPT_TRADE));
        addMessage(new SimpleMessage(ACCEPT_TECH_EXCHANGE));
        addMessage(new SimpleMessage(ACCEPT_FINANCIAL_AID));
        addMessage(new SimpleMessage(ACCEPT_TECHNOLOGY_AID));
        addMessage(new SimpleMessage(ACCEPT_PEACE));
        addMessage(new SimpleMessage(ACCEPT_PACT));
        addMessage(new SimpleMessage(ACCEPT_ALLIANCE));
        addMessage(new SimpleMessage(ACCEPT_JOINT_WAR));
        addMessage(new SimpleMessage(COUNTER_JOINT_WAR));
        addMessage(new SimpleMessage(RESPOND_CLOSE_EMBASSY));
        addMessage(new SimpleMessage(RESPOND_BREAK_TRADE));
        addMessage(new SimpleMessage(RESPOND_BREAK_PACT));
        addMessage(new SimpleMessage(RESPOND_BREAK_ALLIANCE));
        addMessage(new TurnNotificationMessage(ANNOUNCE_TRADE));
        addMessage(new TurnNotificationMessage(ANNOUNCE_PEACE));
        addMessage(new TurnNotificationMessage(ANNOUNCE_PACT));
        addMessage(new TurnNotificationMessage(ANNOUNCE_ALLIANCE));
        addMessage(new TurnNotificationMessage(BREAK_TRADE));
        addMessage(new TurnNotificationMessage(BREAK_PACT));
        addMessage(new TurnNotificationMessage(BREAK_ALLIANCE));
        addMessage(new TurnNotificationMessage(TRANSPORTS_PERISHED));
        addMessage(new TurnNotificationMessage(TRANSPORTS_KILLED));
        addMessage(new TurnNotificationMessage(ENEMY_TRANSPORTS_KILLED));
        addMessage(new TurnNotificationMessage(PRAISE_COUNCIL_VOTE));
        addMessage(new TurnNotificationMessage(PRAISE_ATTACKED_ENEMY));
        addMessage(new TurnNotificationMessage(PRAISE_TRADE));
        addMessage(new TurnNotificationMessage(PRAISE_REBELLING_WITH));
        addMessage(new TurnNotificationMessage(WARNING_COUNCIL_VOTE));
        addMessage(new TurnNotificationMessage(WARNING_OATHBREAKER));
        addMessage(new TurnNotificationMessage(WARNING_ATTACKED_ALLY));
        addMessage(new ThreatForAttack(WARNING_TRESPASSING));
        addMessage(new TurnNotificationMessage(WARNING_EXPANSION));
        addMessage(new TurnNotificationMessage(WARNING_BUILDUP));
        addMessage(new ThreatForAttack(WARNING_SKIRMISH));
        addMessage(new ThreatForSpying(WARNING_ESPIONAGE));
        addMessage(new ThreatForSpying(WARNING_SABOTAGE));
        addMessage(new ThreatForAttack(WARNING_COLONY_INVADED));
        addMessage(new ThreatForAttack(WARNING_COLONY_ATTACKED));
        addMessage(new TurnNotificationMessage(WARNING_BIOWEAPON));
        addMessage(new TurnNotificationMessage(WARNING_GENOCIDE));
        addMessage(new TurnNotificationMessage(WARNING_FINAL_WAR));
        addMessage(new TurnNotificationMessage(WARNING_REBELLING_AGAINST));
        addMessage(new TurnNotificationMessage(DECLARE_ERRATIC_WAR));
        addMessage(new TurnNotificationMessage(DECLARE_HATE_WAR));
        addMessage(new TurnNotificationMessage(DECLARE_OPPORTUNITY_WAR));
        addMessage(new TurnNotificationMessage(DECLARE_SPYING_WAR));
        addMessage(new TurnNotificationMessage(DECLARE_ATTACKED_WAR));
        addMessage(new TurnNotificationMessage(DECLARE_ASSASSIN_WAR));
    }
    private void addMessage(DiplomaticMessage msg) {
        messages.put(msg.messageType, msg);
    }
    private void loadDialogStrings(String filename) {
        // loads dialog strings from dialog.txt file... which does not have language-specific data
        log("Loading Dialog Strings...");
        BufferedReader in = reader(filename);
        if (in == null) {
            err("Cannot find dialog file: ", filename);
            return;
        }
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (!isComment(line)) 
                    strings.add(new DialogString(line));
            }
        }
        catch (IOException e) {
            throw new RuntimeException(concat("load.load -- IOException: ", e.toString()));
        }
        finally {
            try { in.close(); }
            catch(IOException e2) { }
        }
    }
}