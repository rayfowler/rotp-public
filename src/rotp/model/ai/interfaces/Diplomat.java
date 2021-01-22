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
package rotp.model.ai.interfaces;

import java.util.List;
import rotp.model.combat.ShipCombatResults;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.GalacticCouncil;
import rotp.model.incidents.DiplomaticIncident;
import rotp.ui.diplomacy.DiplomaticReply;
import rotp.model.tech.Tech;
import rotp.ui.diplomacy.DiplomaticCounterReply;

public interface Diplomat {
    boolean canOfferDiplomaticTreaties(Empire e);
    boolean canOfferTradeTreaty(Empire e);
    boolean canExchangeTechnology(Empire e);
    boolean canOfferAid(Empire e);
    boolean canDeclareWar(Empire e);
    
    List<Integer> offerAidAmounts();
    List<Tech> offerableTechnologies(Empire emp);
    
    void noticeIncident(DiplomaticIncident inc, Empire e);
    void makeDiplomaticOffers(EmpireView ev);
    Empire councilVoteFor(Empire emp1, Empire emp2);
    void acceptCouncilRuling(GalacticCouncil c);
    
    void noticeNoRelationIncident(EmpireView v, List<DiplomaticIncident> incidents);
    void noticeAtWarWithAllyIncidents(EmpireView v, List<DiplomaticIncident> incidents);
    void noticeAlliedWithEnemyIncidents(EmpireView v, List<DiplomaticIncident> incidents);
    void noticeTrespassingIncidents(EmpireView v, List<DiplomaticIncident> incidents);
    void noticeExpansionIncidents(EmpireView v, List<DiplomaticIncident> incidents);
    void noticeBuildupIncidents(EmpireView v, List<DiplomaticIncident> incidents);
    DiplomaticIncident noticeSkirmishIncident(ShipCombatResults res);
    
    DiplomaticReply receiveFinancialAid(Empire e, int amt);
    DiplomaticReply receiveTechnologyAid(Empire e, String techId);
    DiplomaticReply receiveDeclareWar(Empire e);
    DiplomaticReply receiveOfferPeace(Empire e);
    DiplomaticReply receiveOfferTrade(Empire e, int level);
    DiplomaticReply immediateRefusalToTrade(Empire requestor);
    DiplomaticReply receiveOfferPact(Empire e);
    DiplomaticReply receiveOfferAlliance(Empire e);
    DiplomaticReply receiveOfferJointWar(Empire e, Empire target);
    DiplomaticReply receiveBreakTrade(Empire e);
    DiplomaticReply receiveBreakPact(Empire e);
    DiplomaticReply receiveBreakAlliance(Empire e);
    DiplomaticReply receiveCounterJointWar(Empire e, DiplomaticCounterReply reply);
    DiplomaticReply acceptOfferPeace(Empire e);
    DiplomaticReply refuseOfferPeace(Empire e);
    DiplomaticReply acceptOfferTrade(Empire e, int level);
    DiplomaticReply refuseOfferTrade(Empire e, int level);
    DiplomaticReply acceptOfferPact(Empire e);
    DiplomaticReply refuseOfferPact(Empire e);
    DiplomaticReply acceptOfferAlliance(Empire e);
    DiplomaticReply refuseOfferAlliance(Empire e);
    DiplomaticReply acceptOfferJointWar(Empire e, Empire target);
    DiplomaticReply refuseOfferJointWar(Empire e);

    boolean willingToOfferAlliance(Empire e);
    
    List<Tech> techsAvailableForRequest(Empire emp);
    List<Tech> techsRequestedForCounter(Empire emp, Tech t);
    DiplomaticReply receiveRequestTech(Empire emp, Tech t);
    DiplomaticReply receiveCounterOfferTech(Empire e, Tech counter, Tech wanted);
}
