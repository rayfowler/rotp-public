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
import rotp.model.tech.Tech;
import rotp.model.tech.TechArmor;
import rotp.model.tech.TechAtmosphereEnrichment;
import rotp.model.tech.TechBattleComputer;
import rotp.model.tech.TechBiologicalAntidote;
import rotp.model.tech.TechBiologicalWeapon;
import rotp.model.tech.TechBlackHole;
import rotp.model.tech.TechBombWeapon;
import rotp.model.tech.TechCategory;
import rotp.model.tech.TechCloning;
import rotp.model.tech.TechCombatTransporter;
import rotp.model.tech.TechControlEnvironment;
import rotp.model.tech.TechDeflectorShield;
import rotp.model.tech.TechECMJammer;
import rotp.model.tech.TechEcoRestoration;
import rotp.model.tech.TechEngineWarp;
import rotp.model.tech.TechFuelRange;
import rotp.model.tech.TechHandWeapon;
import rotp.model.tech.TechImprovedIndustrial;
import rotp.model.tech.TechImprovedTerraforming;
import rotp.model.tech.TechIndustrialWaste;
import rotp.model.tech.TechMissileShield;
import rotp.model.tech.TechMissileWeapon;
import rotp.model.tech.TechPersonalShield;
import rotp.model.tech.TechPlanetaryShield;
import rotp.model.tech.TechRoboticControls;
import rotp.model.tech.TechShipInertial;
import rotp.model.tech.TechShipWeapon;
import rotp.model.tech.TechSoilEnrichment;
import rotp.model.tech.TechStargate;
import rotp.model.tech.TechSubspaceInterdictor;
import rotp.model.tech.TechTeleporter;

public interface Scientist {
    void setTechTreeAllocations();
    void setDefaultTechTreeAllocations();
    void setTechToResearch(TechCategory cat);
    Tech mostDesirableTech(List<Tech> techs);
    
    float warTradeValue(Tech t);
    float researchValue(Tech t);
    float researchPriority(Tech t);
    float baseValue(TechArmor t);
    float baseValue(TechAtmosphereEnrichment t);
    float baseValue(TechBattleComputer t);
    float baseValue(TechBiologicalAntidote t);
    float baseValue(TechBiologicalWeapon t);
    float baseValue(TechBlackHole t);
    float baseValue(TechBombWeapon t);
    float baseValue(TechCloning t);
    float baseValue(TechCombatTransporter t);
    float baseValue(TechControlEnvironment t);
    float baseValue(TechDeflectorShield t);
    float baseValue(TechECMJammer t);
    float baseValue(TechEcoRestoration t);
    float baseValue(TechEngineWarp t);
    float baseValue(TechFuelRange t);
    float baseValue(TechHandWeapon t);
    float baseValue(TechImprovedIndustrial t);
    float baseValue(TechImprovedTerraforming t);
    float baseValue(TechIndustrialWaste t);
    float baseValue(TechMissileShield t);
    float baseValue(TechMissileWeapon t);
    float baseValue(TechPersonalShield t);
    float baseValue(TechPlanetaryShield t);
    float baseValue(TechRoboticControls t);
    float baseValue(TechShipInertial t);
    float baseValue(TechShipWeapon t);
    float baseValue(TechSoilEnrichment t);
    float baseValue(TechStargate t);
    float baseValue(TechSubspaceInterdictor t);
    float baseValue(TechTeleporter t);
}
