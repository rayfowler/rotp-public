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
package rotp.model.tech;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import rotp.util.Base;

public final class TechLibrary implements Base {
    private static final String techDataFile = "data/techs.txt";
    private static final TechLibrary instance;
    public static TechLibrary current()   { return instance; }
    public static TechCategory[] baseCategory = new TechCategory[TechTree.NUM_CATEGORIES];
    private static TechCategory loadingCat;
    private static Tech loadingTech;

    private final HashMap <String, Tech> techMap = new HashMap<>();

    static {
        instance = new TechLibrary();
        instance.loadTechDataFile(techDataFile);
        instance.loadTechFiles();
    }
    public static String techMatching(int type, int seq) {
        for (Tech t : current().techMap.values()) {
            if ((t.techType == type) && (t.typeSeq == seq))
                    return t.id();
        }
        return null;
    }
    @Override 
    public Tech tech(String id)               { return techMap.get(id); }
    private void loadTechDataFile(String filename) {
        log("Loading Techs...");
        BufferedReader in = reader(filename);
        if (in == null) 
            return;

        try {
            String input;
            while ((input = in.readLine()) != null) 
                loadTechDataLine(input);
            in.close();
        } 
        catch (IOException e) {
            err("TechTree.loadTechDataFiles -- IOException: " + e);
        }
    }
    private void loadTechDataLine(String input) {
        String line = input.trim();
        if (isComment(input))
            return;

        List<String> vals = this.substrings(input, ':');
        if (vals.size() < 2)
            return;

        String key = vals.get(0);
        String value = vals.get(1);
        if (key.equalsIgnoreCase("cat"))           { parseCategoryLine(value); return; }
        if (key.equalsIgnoreCase("tech"))          { parseTechLine(value); return; }

        err("unknown tech key->", line);
    }
    private void parseCategoryLine(String input) {
        // field #1 is category index (only field for now)
        int index = parseInt(input);
        TechCategory newCat = new TechCategory();
        newCat.index(index);
        loadingCat = newCat;
        baseCategory[index] = newCat;
    }
    private void parseTechLine(String input) {
        List<String> fields = substrings(input, ',');
        if (fields.size() < 5)
            err("Invalid tech line, <5 fields: ", input);

        int researchLevel = parseInt(fields.get(0));
        String techType = fields.get(1);
        int techSeq = parseInt(fields.get(2));
        boolean techFree = parseInt(fields.get(3)) == 1;
        String iconName = fields.get(4);
        String effect = fields.size() > 5 ? fields.get(5) : "";

        Tech newTech = newLoadedTech(researchLevel, techType, techSeq, techFree);
        if (newTech == null) 
            log("Error: couldn't create tech for: ", input);
        else {
            newTech.iconFilename = iconName;
            newTech.effectKey = effect;
            loadingCat.addPossibleTech(newTech.id());
            techMap.put(newTech.id(), newTech);
        }
    }
    private Tech newLoadedTech(int level, String type, int seq, boolean free) {
        if (type.equalsIgnoreCase("Scanner"))              { return new TechScanner(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("BattleComputer"))       { return new TechBattleComputer(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ECMJammer"))            { return new TechECMJammer(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("RoboticControls"))      { return new TechRoboticControls(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("HyperspaceComm"))       { return new TechHyperspaceComm(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("BeamFocus"))            { return new TechBeamFocus(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ShipNullifier"))        { return new TechShipNullifier(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("Armor"))                { return new TechArmor(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ReserveFuelRange"))     { return new TechReserveFuelRange(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ImprovedIndustrial"))   { return new TechImprovedIndustrial(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("IndustrialWaste"))      { return new TechIndustrialWaste(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("BattleSuit"))           { return new TechBattleSuit(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("AutomatedRepair"))      { return new TechAutomatedRepair(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("DeflectorShield"))      { return new TechDeflectorShield(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("Cloaking"))             { return new TechCloaking(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("Repulsor"))             { return new TechRepulsor(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("PersonalShield"))       { return new TechPersonalShield(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("PlanetaryShield"))      { return new TechPlanetaryShield(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("MissileShield"))        { return new TechMissileShield(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("StasisField"))          { return new TechStasisField(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("BlackHole"))            { return new TechBlackHole(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ControlEnvironment"))   { return new TechControlEnvironment(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("EcoRestoration"))       { return new TechEcoRestoration(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ImprovedTerraforming")) { return new TechImprovedTerraforming(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("BiologicalWeapon"))     { return new TechBiologicalWeapon(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("SoilEnrichment"))       { return new TechSoilEnrichment(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("BiologicalAntidote"))   { return new TechBiologicalAntidote(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("Cloning"))              { return new TechCloning(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("AtmosphereEnrichment")) { return new TechAtmosphereEnrichment(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("EngineWarp"))           { return new TechEngineWarp(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("FuelRange"))            { return new TechFuelRange(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ShipInertial"))         { return new TechShipInertial(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("EnergyPulsar"))         { return new TechEnergyPulsar(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("Stargate"))             { return new TechStargate(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("Teleporter"))           { return new TechTeleporter(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("SubspaceInterdictor"))  { return new TechSubspaceInterdictor(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("CombatTransporter"))    { return new TechCombatTransporter(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("Displacement"))         { return new TechDisplacement(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("BombWeapon"))           { return new TechBombWeapon(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("MissileWeapon"))        { return new TechMissileWeapon(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ShipWeapon"))           { return new TechShipWeapon(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("HandWeapon"))           { return new TechHandWeapon(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("StreamProjector"))      { return new TechStreamProjector(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("TorpedoWeapon"))        { return new TechTorpedoWeapon(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("FutureComputer"))       { return new TechFutureComputer(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("FutureConstruction"))   { return new TechFutureConstruction(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("FutureForceField"))     { return new TechFutureForceField(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("FuturePlanetology"))    { return new TechFuturePlanetology(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("FuturePropulsion"))     { return new TechFuturePropulsion(type, level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("FutureWeapon"))         { return new TechFutureWeapon(type, level, seq, free, loadingCat); }

        return null;
    }
    public void loadTechFiles() {
        String dataDir = "data/";
        log("Loading tech files from dir: ",dataDir);
        
    	loadTechLangFile(Tech.ARMOR, "Armor.txt", dataDir);
    	loadTechLangFile(Tech.ATMOSPHERE_ENRICHMENT, "AtmosphereEnrichment.txt", dataDir);
    	loadTechLangFile(Tech.AUTOMATED_REPAIR, "AutomatedRepair.txt", dataDir);
    	loadTechLangFile(Tech.BATTLE_COMPUTER, "BattleComputer.txt", dataDir);
    	loadTechLangFile(Tech.BATTLE_SUIT, "BattleSuit.txt", dataDir);
    	loadTechLangFile(Tech.BEAM_FOCUS, "BeamFocus.txt", dataDir);
    	loadTechLangFile(Tech.BIOLOGICAL_ANTIDOTE, "BiologicalAntidote.txt", dataDir);
    	loadTechLangFile(Tech.BIOLOGICAL_WEAPON, "BiologicalWeapon.txt", dataDir);
    	loadTechLangFile(Tech.BLACK_HOLE, "BlackHole.txt", dataDir);
    	loadTechLangFile(Tech.BOMB_WEAPON, "BombWeapon.txt", dataDir);
    	loadTechLangFile(Tech.CLOAKING, "Cloaking.txt", dataDir);
    	loadTechLangFile(Tech.CLONING, "Cloning.txt", dataDir);
    	loadTechLangFile(Tech.COMBAT_TRANSPORTER, "CombatTransporter.txt", dataDir);
    	loadTechLangFile(Tech.CONTROL_ENVIRONMENT, "ControlEnvironment.txt", dataDir);
    	loadTechLangFile(Tech.DEFLECTOR_SHIELD, "DeflectorShield.txt", dataDir);
    	loadTechLangFile(Tech.DISPLACEMENT, "Displacement.txt", dataDir);
    	loadTechLangFile(Tech.ECM_JAMMER, "ECMJammer.txt", dataDir);
    	loadTechLangFile(Tech.ECO_RESTORATION, "EcoRestoration.txt", dataDir);
    	loadTechLangFile(Tech.ENERGY_PULSAR, "EnergyPulsar.txt", dataDir);
    	loadTechLangFile(Tech.ENGINE_WARP, "EngineWarp.txt", dataDir);
    	loadTechLangFile(Tech.FUEL_RANGE, "FuelRange.txt", dataDir);
    	loadTechLangFile(Tech.HAND_WEAPON, "HandWeapon.txt", dataDir);
    	loadTechLangFile(Tech.HYPERSPACE_COMM, "HyperspaceComm.txt", dataDir);
    	loadTechLangFile(Tech.IMPROVED_INDUSTRIAL, "ImprovedIndustrial.txt", dataDir);
    	loadTechLangFile(Tech.IMPROVED_TERRAFORMING, "ImprovedTerraforming.txt", dataDir);
    	loadTechLangFile(Tech.INDUSTRIAL_WASTE, "IndustrialWaste.txt", dataDir);
    	loadTechLangFile(Tech.MISSILE_SHIELD, "MissileShield.txt", dataDir);
    	loadTechLangFile(Tech.MISSILE_WEAPON, "MissileWeapon.txt", dataDir);
    	loadTechLangFile(Tech.PERSONAL_SHIELD, "PersonalShield.txt", dataDir);
    	loadTechLangFile(Tech.PLANETARY_SHIELD, "PlanetaryShield.txt", dataDir);
    	loadTechLangFile(Tech.REPULSOR, "Repulsor.txt", dataDir);
    	loadTechLangFile(Tech.RESERVE_FUEL_RANGE, "ReserveFuelRange.txt", dataDir);
    	loadTechLangFile(Tech.ROBOTIC_CONTROLS, "RoboticControls.txt", dataDir);
    	loadTechLangFile(Tech.SCANNER, "Scanner.txt", dataDir);
    	loadTechLangFile(Tech.SHIP_INERTIAL, "ShipInertial.txt", dataDir);
    	loadTechLangFile(Tech.SHIP_NULLIFIER, "ShipNullifier.txt", dataDir);
    	loadTechLangFile(Tech.SHIP_WEAPON, "ShipWeapon.txt", dataDir);
    	loadTechLangFile(Tech.SOIL_ENRICHMENT, "SoilEnrichment.txt", dataDir);
    	loadTechLangFile(Tech.STARGATE, "Stargate.txt", dataDir);
    	loadTechLangFile(Tech.STASIS_FIELD, "StasisField.txt", dataDir);
    	loadTechLangFile(Tech.STREAM_PROJECTOR, "StreamProjector.txt", dataDir);
    	loadTechLangFile(Tech.SUBSPACE_INTERDICTOR, "SubspaceInterdictor.txt", dataDir);
    	loadTechLangFile(Tech.TELEPORTER, "Teleporter.txt", dataDir);
    	loadTechLangFile(Tech.TORPEDO_WEAPON, "TorpedoWeapon.txt", dataDir);
    	loadTechLangFile(Tech.FUTURE_COMPUTER, "FutureComputer.txt", dataDir);
    	loadTechLangFile(Tech.FUTURE_CONSTRUCTION, "FutureConstruction.txt", dataDir);
    	loadTechLangFile(Tech.FUTURE_FORCE_FIELD, "FutureForceField.txt", dataDir);
    	loadTechLangFile(Tech.FUTURE_PLANETOLOGY, "FuturePlanetology.txt", dataDir);
    	loadTechLangFile(Tech.FUTURE_PROPULSION, "FuturePropulsion.txt", dataDir);
    	loadTechLangFile(Tech.FUTURE_WEAPON, "FutureWeapon.txt", dataDir);
    }
    private void loadTechLangFile(int techType, String filename, String langDir) {
        // try to open the race file
        BufferedReader in = reader(concat(langDir, "tech/", filename));
        if (in == null) 
            return;

        try {
            String input;
            loadingTech = null;
            log("Loading tech file:",filename);
            while ((input = in.readLine()) != null) 
                loadTechLangLine(techType, input.trim());
            in.close();
        } 
        catch (IOException e) {
            err("TechTree.loadTechLangFile(", filename, ") -- IOException: ", e.toString());
        }
    }
    private void loadTechLangLine(int techType, String input) {
    	if (isComment(input))
            return;

        if (input.equalsIgnoreCase("[tech]"))
            return;

        int techSeq = 0;
        List<String> strings1 = substrings(input, ':');

        if (strings1.size() < 2) 
            return;

        String key = strings1.get(0);
        String value = strings1.get(1);

        // if a tech sequence number, then retrieve the tech we are loading into & load the default description
        if (key.equalsIgnoreCase("seq")) { 
            try { techSeq = parseInt(value); }
            catch (NumberFormatException e) {
                err("TechTree.loadTechLangLine -- NumberFormatException for tech seq: " + value);
            }
            loadingTech = tech(TechLibrary.techMatching(techType, techSeq));
            if (loadingTech == null)
                err("Could not find a tech matching type:", str(techType), " and seq:", str(techSeq));
            return; 
        }

        // if no matching tech has been found to load into, ignore the remaining keys
        if (loadingTech == null)
            return;

        switch(key) {
            case "name"   : loadingTech.name = value;    break;
            case "brief"  : loadingTech.shDesc = value;  break;
            case "brief2" : loadingTech.shDesc2 = value; break;
            case "detail" : loadingTech.detail = value;  break;
            case "item"   : loadingTech.item = value;    break;
            case "item2"  : loadingTech.item2 = value;   break;
            default       : log("unknown key->", input);  return;
        }
    }
}
