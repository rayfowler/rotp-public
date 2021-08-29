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
package rotp.model.empires;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import rotp.Rotp;
import rotp.util.AnimationManager;
import rotp.util.Base;
import rotp.util.ImageManager;
import rotp.util.PixelShifter;

public enum RaceFactory implements Base {
    INSTANCE;
    public static RaceFactory current()   { return INSTANCE; }

    private static final String raceListFile = "races/listing.txt";
    private  RaceFactory() {
        loadDataFiles();
    }
    public void loadDataFiles() {
        log("Loading Races: ", raceListFile);
        BufferedReader in = reader(raceListFile);
        if (in == null)
            return;
        try {
            String input;
            while ((input = in.readLine()) != null)
                loadRaceDataFile(input.trim());
            in.close();
        }
        catch (IOException e) {
            err("RaceFactory.loadRaces -- IOException: ", e.toString());
        }
    }
    private void loadRaceDataFile(String raceDirName) {
        if (isComment(raceDirName))
            return;

        // try to open the race file
        String raceDirPath = concat("races/", raceDirName);
        String filename = raceDirPath+"/definition.txt";
        BufferedReader in = reader(filename);
        if (in == null) {
            err("Could not open file: ", filename);
            return;
        }

        Race newRace = new Race(raceDirPath);
        try {
            String input;
            while ((input = in.readLine()) != null)
                loadRaceDataLine(newRace, input);
            in.close();
            ImageManager.current().loadImageList(raceDirPath+"/images.txt");
            AnimationManager.current().loadAnimationList(raceDirPath+"/animations.txt");
        }
        catch (IOException e) {
            err("RaceFactory.loadRaceDataFile(", filename, ") -- IOException: ", e.toString());
        }
        Race.addRace(newRace);
    }
    public void resetRaceLangFiles() {
        for (Race r : Race.races())
            r.raceLabels().resetDialogue();
    }
    public void loadRaceLangFiles(String langDir) {
        for (Race r : Race.races())
            loadRaceLangFiles(r, langDir);
    }
    private void loadRaceLangFiles(Race r, String langDir) {
        String dir = concat("lang/", langDir, "/races/");
        List<String> sNames = readSystemNames(dir+r.langKey+".systems.txt");
        if (sNames != null)
            r.systemNames = sNames;
        r.raceLabels().labelFile(r.langKey+".labels.txt");
        r.raceLabels().dialogueFile(r.langKey+".dialogue.txt");
        r.raceLabels().introFile(r.langKey+".intro.txt");
        r.raceLabels().loadIntroFile(dir);
        r.raceLabels().loadDialogueFile(dir);
        r.raceLabels().loadLabelFile(dir);
        String filename = dir+r.langKey+".names.txt";
        BufferedReader in = reader(filename);
        if (in == null)
            return;

        int wc = 0;
        try {
            String input;
            while ((input = in.readLine()) != null)
                wc += loadRaceLangLine(r, input, langDir);
            in.close();
        }
        catch (IOException e) {
            err("RaceFactory.loadRaceLangFile(", r.directoryName+") -- IOException: ", e.toString());
        }
        
        if (Rotp.countWords)
            log("WORDS - "+filename+": "+wc);
    }
    private void loadRaceDataLine(Race r, String input) {
        if (isComment(input))
            return;

        List<String> vals = substrings(input, ':');
        if (vals.size() < 2)
            return;

        String key = vals.get(0);
        String value = vals.get(1);

        if (key.equalsIgnoreCase("key"))              { r.id = value; return; }
        if (key.equalsIgnoreCase("langKey"))          { r.langKey = value; return; }
        if (key.equalsIgnoreCase("year"))             { r.startingYear = parseInt(value); return; }
        if (key.equalsIgnoreCase("homestarType"))     { r.homeworldStarType = value; return; }
        if (key.equalsIgnoreCase("homeworldType"))    { r.homeworldPlanetType = value; return; }
        if (key.equalsIgnoreCase("homeworldKey"))     { r.homeworldKey(parseInt(value)); return; }
        if (key.equalsIgnoreCase("homeworldSize"))    { r.homeworldSize = parseInt(value); return; }
        if (key.equalsIgnoreCase("mugshot"))          { r.mugshotKey = value; return; }
        if (key.equalsIgnoreCase("diploProfile"))     { r.wideMugshotKey = value; return; }
        if (key.equalsIgnoreCase("setupImage"))       { r.setupImageKey = value; return; }
        if (key.equalsIgnoreCase("spyMug"))           { r.spyFaceKey = value; return; }
        if (key.equalsIgnoreCase("soldierMug"))       { r.soldierFaceKey = value; return; }
        if (key.equalsIgnoreCase("advisorMug"))       { r.advisorFaceKey = value; return; }
        if (key.equalsIgnoreCase("advisorScout"))     { r.advisorScoutKey = value; return; }
        if (key.equalsIgnoreCase("advisorTransport")) { r.advisorTransportKey = value; return; }
        if (key.equalsIgnoreCase("advisorDiplomacy")) { r.advisorDiplomacyKey = value; return; }
        if (key.equalsIgnoreCase("advisorShip"))      { r.advisorShipKey = value; return; }
        if (key.equalsIgnoreCase("advisorRally"))     { r.advisorRallyKey = value; return; }
        if (key.equalsIgnoreCase("advisorMissile"))   { r.advisorMissileKey = value; return; }
        if (key.equalsIgnoreCase("advisorWeapon"))    { r.advisorWeaponKey = value; return; }
        if (key.equalsIgnoreCase("advisorCouncil"))   { r.advisorCouncilKey = value; return; }
        if (key.equalsIgnoreCase("advisorRebellion")) { r.advisorRebellionKey = value; return; }
        if (key.equalsIgnoreCase("advisorResistCouncil")) { r.advisorResistCouncilKey = value; return; }
        if (key.equalsIgnoreCase("advisorCouncilResisted")) { r.advisorCouncilResistedKey = value; return; }
        if (key.equalsIgnoreCase("council"))          { r.councilKey = value; return; }
        if (key.equalsIgnoreCase("lab"))              { r.laboratoryKey = value; return; }
        if (key.equalsIgnoreCase("embassy"))          { r.embassyKey = value; return; }
        if (key.equalsIgnoreCase("holograph"))        { r.holographKey = value; return; }
        if (key.equalsIgnoreCase("diplomat"))         { r.diplomatKey = value; return; }
        if (key.equalsIgnoreCase("scientist"))        { r.scientistKey = value; return; }
        if (key.equalsIgnoreCase("trooper"))          { r.soldierKey = value; return; }
        if (key.equalsIgnoreCase("spy"))              { r.spyKey = value; return; }
        if (key.equalsIgnoreCase("leader"))           { r.leaderKey = value; return; }
        if (key.equalsIgnoreCase("diploTheme"))       { r.diplomacyTheme = value; return; }
        if (key.equalsIgnoreCase("gnn"))              { r.gnnKey = value; return; }
        if (key.equalsIgnoreCase("gnnHost"))          { r.gnnHostKey = value; return; }
        if (key.equalsIgnoreCase("gnnColor"))         { r.gnnColor = value; return; }
        if (key.equalsIgnoreCase("gnnTextColor"))     { r.gnnTextColor = parseColor(value); return; }
        if (key.equalsIgnoreCase("diplomatXform"))    { r.diplomacyTransformer = PixelShifter.createFrom(value); }
        if (key.equalsIgnoreCase("winSplash"))        { r.parseWinSplash(value); return; }
        if (key.equalsIgnoreCase("lossSplash"))       { r.parseLossSplash(value); return; }
        if (key.equalsIgnoreCase("flagSize"))         { r.flagSize(value); return; }
        if (key.equalsIgnoreCase("flagWar"))          { r.flagWarKey = value; return; }
        if (key.equalsIgnoreCase("flagNormal"))       { r.flagNormKey = value; return; }
        if (key.equalsIgnoreCase("flagPact"))         { r.flagPactKey = value; return; }
        if (key.equalsIgnoreCase("dialogWar"))        { r.dlgWarKey = value; return; }
        if (key.equalsIgnoreCase("dialogNormal"))     { r.dlgNormKey = value; return; }
        if (key.equalsIgnoreCase("dialogPact"))       { r.dlgPactKey = value; return; }
        if (key.equalsIgnoreCase("troopIcon"))        { r.troopNormal.iconSpec(value); return; }
        if (key.equalsIgnoreCase("troopFireXY"))      { r.troopNormal.fireXY(value); return; }
        if (key.equalsIgnoreCase("troopScale"))       {
            r.troopNormal.scaling(value);
            r.troopDeath1.scaling(value);
            r.troopDeath2.scaling(value);
            r.troopDeath3.scaling(value);
            r.troopDeath4.scaling(value);
            r.troopHostile.scaling(value);
            r.troopDeath1H.scaling(value);
            r.troopDeath2H.scaling(value);
            r.troopDeath3H.scaling(value);
            r.troopDeath4H.scaling(value);
            return;
        }
        if (key.equalsIgnoreCase("troopHIcon"))    { r.troopHostile.iconSpec(value); return; }
        if (key.equalsIgnoreCase("troopHFireXY"))  { r.troopHostile.fireXY(value); return; }
        if (key.equalsIgnoreCase("troopDeath1"))   { r.troopDeath1.iconSpec(value); return; }
        if (key.equalsIgnoreCase("troopDeath2"))   { r.troopDeath2.iconSpec(value); return; }
        if (key.equalsIgnoreCase("troopDeath3"))   { r.troopDeath3.iconSpec(value); return; }
        if (key.equalsIgnoreCase("troopDeath4"))   { r.troopDeath4.iconSpec(value); return; }
        if (key.equalsIgnoreCase("troopDeath1H"))  { r.troopDeath1H.iconSpec(value); return; }
        if (key.equalsIgnoreCase("troopDeath2H"))  { r.troopDeath2H.iconSpec(value); return; }
        if (key.equalsIgnoreCase("troopDeath3H"))  { r.troopDeath3H.iconSpec(value); return; }
        if (key.equalsIgnoreCase("troopDeath4H"))  { r.troopDeath4H.iconSpec(value); return; }
        if (key.equalsIgnoreCase("landingAudio"))  { r.shipAudioKey = value; return; }
        if (key.equalsIgnoreCase("transport"))     { r.transportKey = value; return; }
        if (key.equalsIgnoreCase("transportDesc")) { r.parseTransportDesc(value); return; }
        if (key.equalsIgnoreCase("transportOpen")) { r.parseTransportOpen(value); return; }
        if (key.equalsIgnoreCase("transportW"))    { r.transportW = parseInt(value); return; }
        if (key.equalsIgnoreCase("transportYOff")) { r.transportYOffset = parseInt(value); return; }
        if (key.equalsIgnoreCase("transportLandingFrames")) { r.transportLandingFrames = parseInt(value); return; }
        if (key.equalsIgnoreCase("colonistWalk"))  { r.colonistWalk(value); return; }
        if (key.equalsIgnoreCase("labFlagX"))      { r.labFlagX(parseFloat(value)); return; }
        if (key.equalsIgnoreCase("spyFactories"))  { r.spyFactoryFrames(parseInt(value)); return; }
        if (key.equalsIgnoreCase("spyMissiles"))   { r.spyMissileFrames(parseInt(value)); return; }
        if (key.equalsIgnoreCase("spyRebellion"))  { r.spyRebellionFrames(parseInt(value)); return; }
        if (key.equalsIgnoreCase("espionageXY"))   { r.espionageXY(substrings(value, '@')); return; }
        if (key.equalsIgnoreCase("dialogTextX"))   { parseDialogTextMargins(r, substrings(value, ',')); return; }
        if (key.equalsIgnoreCase("dialogTextY"))   { r.dialogTopY = parseInt(value); return; }
        if (key.equalsIgnoreCase("fortress"))      { r.parseFortress(value); return; }
        if (key.equalsIgnoreCase("shield"))        { r.shieldKey = value; return; }
        if (key.equalsIgnoreCase("introTextX"))    { r.introTextX = parseInt(value); return; }
        if (key.equalsIgnoreCase("councilDiplo"))  { r.parseCouncilDiplomatLocation(value); return; }
        if (key.equalsIgnoreCase("homeworld"))     { r.homeworldKey(parseInt(value)); return; }
        if (key.equalsIgnoreCase("voice"))         { r.voiceKey = value; return; }
        if (key.equalsIgnoreCase("ambience"))      { r.ambienceKey = value; return; }
        if (key.equalsIgnoreCase("species"))       { parseRaceSpeciesMods(r, substrings(value, ',')); return; }
        if (key.equalsIgnoreCase("preferredship")) { parseRaceShipSize(r, substrings(value,',')); return; }
        if (key.equalsIgnoreCase("shipmod"))       { parseRaceShipMods(r, substrings(value,',')); return; }
        if (key.equalsIgnoreCase("groundmod"))     { r.groundAttackBonus(parseInt(value)); return; }
        if (key.equalsIgnoreCase("spymod"))        { parseRaceSpyMods(r, substrings(value,',')); return; }
        if (key.equalsIgnoreCase("prodmod"))       { parseRaceProdMods(r, substrings(value,',')); return; }
        if (key.equalsIgnoreCase("techmod"))       { parseRaceTechMods(r, substrings(value,',')); return; }
        if (key.equalsIgnoreCase("popmod"))        { parseRacePopMods(r, value); return; }
        if (key.equalsIgnoreCase("diplomod"))      { parseRaceDiploMods(r, substrings(value,',')); return; }
        if (key.equalsIgnoreCase("research"))      { parseRaceResearchMods(r, substrings(value,',')); return; }
        if (key.equalsIgnoreCase("personality"))   { parseRacePersonalityMods(r, value); return; }
        if (key.equalsIgnoreCase("objective"))     { parseRaceObjectiveMods(r, value); return; }
        if (key.equalsIgnoreCase("relations"))     { parseRaceRelationsMods(r, substrings(value,',')); return; }
        if (key.equalsIgnoreCase("shipdesign"))    { parseShipDesignMods(r, substrings(value,',')); return; }
        if (key.equalsIgnoreCase("available"))     { parseRaceAvailableFlags(r, value); return; }

        err("unknown key->", input);
    }

    private int loadRaceLangLine(Race r, String input, String langDir) {
        if (isComment(input))
            return 0;

        List<String> vals = substrings(input, ':');
        if (vals.size() < 2)
            return 0;

        String key = vals.get(0);
        String value = vals.get(1);

        int wc = 0;
        
        if (Rotp.countWords)
            wc = substrings(value,',').size();  // uncomment 
        
        if (key.equalsIgnoreCase("name"))          { r.parseRaceNames(value, langDir); return wc; }
        if (key.equalsIgnoreCase("desc1"))         { r.description1 = value; return wc; }
        if (key.equalsIgnoreCase("desc2"))         { r.description2 = value; return wc; }
        if (key.equalsIgnoreCase("desc3"))         { r.description3 = value; return wc; }
        if (key.equalsIgnoreCase("home"))          { r.homeSystemNames.clear(); r.homeSystemNames.addAll(substrings(value, ',')); return wc; }
        if (key.equalsIgnoreCase("title"))         { r.title(value.trim()); return wc; }
        if (key.equalsIgnoreCase("fulltitle"))     { r.fullTitle(value.trim()); return wc; }
        if (key.equalsIgnoreCase("leader"))        { r.leaderNames.clear(); r.leaderNames.addAll(substrings(value, ',')); return wc; }
        if (key.equalsIgnoreCase("ship1"))         { r.shipNamesSmall.clear(); r.shipNamesSmall.addAll(substrings(value, ',')); return wc; }
        if (key.equalsIgnoreCase("ship2"))         { r.shipNamesMedium.clear(); r.shipNamesMedium.addAll(substrings(value, ',')); return wc; }
        if (key.equalsIgnoreCase("ship3"))         { r.shipNamesLarge.clear(); r.shipNamesLarge.addAll(substrings(value, ',')); return wc; }
        if (key.equalsIgnoreCase("ship4"))         { r.shipNamesHuge.clear(); r.shipNamesHuge.addAll(substrings(value, ',')); return wc; }
        err("unknown key->", input);
        return 0;
    }
    private void parseDialogTextMargins(Race r, List<String> vals) {
        if (vals.size() < 2)
            err("Race ", r.name(), " is missing some dialog text margins");

        r.dialogLeftMargin = parseInt(vals.get(0));
        r.dialogRightMargin = parseInt(vals.get(1));
    }
    private void parseRaceSpeciesMods(Race r, List<String> vals) {
        if (vals.size() < 2)
            err("Race ", r.name(), " is missing some species vars");

        // field #1 is species type (carbon,silicate,robotic) -
        r.speciesType = parseInt(vals.get(0));
        r.ignoresPlanetEnvironment = (parseInt(vals.get(1)) == 1);
    }
    private void parseRaceShipSize(Race r, List<String> vals) {
        r.preferredShipSet = vals.get(0);
        r.preferredShipSize = parseInt(vals.get(1));
    }
    private void parseRaceShipMods(Race r, List<String> vals) {
        r.shipAttackBonus(parseInt(vals.get(0)));
        r.shipDefenseBonus(parseInt(vals.get(1)));
        r.shipInitiativeBonus(parseInt(vals.get(2)));
    }
    private void parseRaceSpyMods(Race r, List<String> vals) {
        r.spyCostMod = (float) parseInt(vals.get(0)) / 100;
        r.internalSecurityAdj = (float) parseInt(vals.get(1)) / 100;
        r.spyInfiltrationAdj = (float) parseInt(vals.get(2)) / 100;
        r.telepathic = parseInt(vals.get(3)) == 1;
        r.masksDiplomacy = parseInt(vals.get(4)) == 1;
    }
    private void parseRaceProdMods(Race r, List<String> vals) {
        if (vals.size() < 3)
            err("Race ", r.name(), " is missing some prod vars. ");
        r.workerProductivityMod = (float) parseInt(vals.get(0)) / 100;
        r.robotControlsAdj = parseInt(vals.get(1));
        r.ignoresFactoryRefit = (parseInt(vals.get(2)) == 1);
    }
    private void parseRaceTechMods(Race r, List<String> vals) {
        if (vals.size() < 2)
            err("Race ", r.name(), " is missing some research vars. ");
        // field #1 is tech discovery pct (only field for now)
        r.techDiscoveryPct = (float) parseInt(vals.get(0)) / 100;
        r.researchBonusPct = (float) parseInt(vals.get(1)) / 100;
    }
    private void parseRacePopMods(Race r, String input) {
        // field #1 is the growth rate mod (only field for now)
        String growthMod = input.trim();
        r.growthRateMod = (float) parseInt(growthMod) / 100;
    }
    private void parseRaceDiploMods(Race r, List<String> vals) {
        // field #1 is trade bonus modifier (as %)
        r.tradePctBonus = (float) parseInt(vals.get(0)) / 100;

        // field #2 is positive DP modifier (as %)
        r.positiveDPMod = (float) parseInt(vals.get(1)) / 100;

        // field #3 is diplomatic relations modifier (as %)
        r.diplomacyBonus = parseInt(vals.get(2));

        // field #4 is council vote modifier (as %)
        r.councilBonus = (float) parseInt(vals.get(3)) / 100;
    }
    private void parseRaceResearchMods(Race r, List<String> vals) {
        // field #1 is computer %
        r.techMod[0] = (float) parseInt(vals.get(0)) / 100;

        // field #2 is construction %
        r.techMod[1] = (float) parseInt(vals.get(1)) / 100;

        // field #3 is force field %
        r.techMod[2] = (float) parseInt(vals.get(2)) / 100;

        // field #4 is planetary %
        r.techMod[3] = (float) parseInt(vals.get(3)) / 100;

        // field #5 is propulsion %
        r.techMod[4] = (float) parseInt(vals.get(4)) / 100;

        // field #6 is weapon %
        r.techMod[5] = (float) parseInt(vals.get(5)) / 100;
    }
    private void parseRacePersonalityMods(Race r, String input) {
        List<String> vals = substrings(input, ',');
        for (int i=0;i<vals.size();i++)
            r.personalityPct[i] = parseInt(vals.get(i))/100.0f;
    }
    private void parseRaceObjectiveMods(Race r, String input) {
        List<String> vals = substrings(input, ',');
        for (int i=0;i<vals.size();i++)
            r.objectivePct[i] = parseFloat(vals.get(i))/100.0f;
    }
    private void parseRaceRelationsMods(Race r, List<String> relations) {
        for (String s : relations) {
            List<String> vals = substrings(s, '=');
            if (vals.get(0).equalsIgnoreCase("DEFAULT"))
                r.defaultRaceRelations(parseInt(vals.get(1)));
            else
                r.baseRelations(vals.get(0),parseInt(vals.get(1)));
        }
    }
    private void parseShipDesignMods(Race r, List<String> mods) {
        int maxMods = r.shipDesignMods.length;
        
        if (mods.size() > maxMods)
            err("Too many ship design mods specified for "+r.name());
        for (int i=0;i<mods.size();i++) {
            float val = parseFloat(mods.get(i));
            if (i < maxMods)
            r.shipDesignMods[i] = val;
        }
    }
    private void parseRaceAvailableFlags(Race r, String input) {
        List<String> vals = this.substrings(input, ',');
        if (vals.size() < 2)
            return;
        // field #1 is available to player
        r.availablePlayer = !vals.get(0).equalsIgnoreCase("no");

        // field #2 is available to AI
        r.availableAI = !vals.get(1).equalsIgnoreCase("no");
    }
}
