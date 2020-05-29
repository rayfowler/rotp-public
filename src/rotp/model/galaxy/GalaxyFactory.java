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
package rotp.model.galaxy;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.empires.Race;
import rotp.model.galaxy.GalaxyShape.EmpireSystem;
import rotp.model.game.GameSession;
import rotp.model.game.IGameOptions;
import rotp.model.planet.Planet;
import rotp.ui.util.planets.PlanetImager;
import rotp.util.Base;

public class GalaxyFactory implements Base {
    static GalaxyFactory instance = new GalaxyFactory();
    public static GalaxyFactory current()   { return instance; }

    public Galaxy newGalaxy() {
        for (Race r: Race.races()) {
            r.loadNameList();
            r.loadLeaderList();
            r.loadHomeworldList();
        }

        IGameOptions opts = GameSession.instance().options();
        opts.randomizeColors();
        GalaxyShape shape = opts.galaxyShape();

        // for extremely large maps, shape is not fully generated on Setup UI
        if (!shape.fullyInit())
            shape.fullGenerate();

        Galaxy g = new Galaxy(shape);
        GameSession.instance().galaxy(g);
        Race playerRace = Race.keyed(opts.selectedPlayerRace());

        List<String> alienRaces = buildAlienRaces();

        log("Creating Galaxy size: ", fmt(g.width(),2), "@", fmt(g.height(),2));
        long tm0 = System.currentTimeMillis();

        addNebulas(g, shape);
        long tm1 = System.currentTimeMillis();
        log(str(g.nebulas().size()) +" Nebulas: "+(tm1-tm0)+"ms");

        List<String> systemNames = playerRace.systemNames;
        Collections.shuffle(systemNames);

        List<EmpireSystem> empires = shape.empireSystems();
        addPlayerSystemForGalaxy(g, 0, empires);
        empires.remove(empires.get(0));
        addAlienRaceSystemsForGalaxy(g, 1, empires, alienRaces);
        addUnsettledSystemsForGalaxy(g, shape);

        long tm2 = System.currentTimeMillis();
        log(str(g.numStarSystems()) ," Systems, ",str(Planet.COUNT)," Planets: "+(tm2-tm1)+"ms");

        // after systems created, add system views for each emp
        for (Empire e: g.empires()) {
            e.loadStartingTechs();
        }
        long tm3 = System.currentTimeMillis();
        log("load starting techs: "+(tm3-tm2)+"ms");

        for (Empire e: g.empires()) {
            e.loadStartingShipDesigns();
        }
        long tm3b = System.currentTimeMillis();
        log("load ship designs: "+(tm3b-tm3)+"ms");

        for (Empire e: g.empires()) {
            e.colonizeHomeworld();
        }
        long tm3c = System.currentTimeMillis();
        log("colonize homeworld: "+(tm3c-tm3b)+"ms");

        // after all is done, set playerCiv
        g.player(g.empire(0));
        player().setBeginningColonyAllocations();

        for (Empire e : g.empires())
            e.ai().scientist().setDefaultTechTreeAllocations();

        for (Empire e1 : g.empires()) {
            for (Empire e2 : g.empires())
                e1.addViewFor(e2);
        }

        // this takes the longest time
        for (Empire e: g.empires()) {
            e.sv.refreshFullScan(e.homeSysId());
            e.setVisibleShips(e.homeSysId());
        }

        g.council().init();

        long tm4 = System.currentTimeMillis();
        log("Other inits: "+(tm4-tm3c)+"ms");

        g.player().makeNextTurnDecisions();
        g.player().refreshViews();
        long tm5 = System.currentTimeMillis();
        log("Next Turn Decision: "+(tm5-tm4)+"ms");

        PlanetImager.current().finished();
        return g;
    }
    private List<String> buildAlienRaces() {
        List<String> raceList = new ArrayList<>();
        List<String> allRaceOptions = new ArrayList<>();
        List<String> options = options().startingRaceOptions();
        int maxRaces = options().selectedNumberOpponents();
        int mult = IGameOptions.MAX_OPPONENT_TYPE;

        // first, build randomized list of opponent races
        for (int i=0;i<mult;i++) {
            Collections.shuffle(options);
            allRaceOptions.addAll(options);
        }

        // next, remove from that list the player and any selected opponents
        String[] selectedOpponents = options().selectedOpponentRaces();
        allRaceOptions.remove(options().selectedPlayerRace());
        
        for (int i=0;i<maxRaces;i++) {
            if (selectedOpponents[i] != null)
                allRaceOptions.remove(selectedOpponents[i]);
        }
        // build alien race list, replacing unselected opponents (null)
        // with remaining options
        for (int i=0;i<maxRaces;i++) {
            if (selectedOpponents[i] == null)
                raceList.add(allRaceOptions.remove(0));
            else
                raceList.add(selectedOpponents[i]);
        }
        return raceList;
    }
    private void addPlayerSystemForGalaxy(Galaxy g, int id, List<EmpireSystem> empSystems) {
        EmpireSystem empSystem = empSystems.get(id);
        // creates a star system for player, using selected options
        IGameOptions opts = GameSession.instance().options();
        String raceKey = opts.selectedPlayerRace();
        Race playerRace = Race.keyed(raceKey);
        String defaultName = playerRace.nextAvailableHomeworld();
        String systemName = options().selectedHomeWorldName();
        if (systemName.isEmpty())
            systemName = defaultName;
        String leaderName = opts.selectedLeaderName();
        Integer color = options().selectedPlayerColor();

        // create home system for player
        StarSystem sys = StarSystemFactory.current().newSystemForPlayer(playerRace, g);
        sys.setXY(empSystem.colonyX(), empSystem.colonyY());
        sys.name(systemName);
        g.addStarSystem(sys);

        // add Empire to galaxy
        Empire emp = new Empire(g, id, raceKey, sys, color, leaderName);
        g.addEmpire(emp);

        //log("Adding star system: ", sys.name(), " - ", playerRace.id, " : ", fmt(sys.x(),2), "@", fmt(sys.y(),2));

        // add other systems in this EmpireSystem
        // ensure 1st nearby system is colonizable
        boolean needHabitable = true;
        for (int i=1;i<empSystem.numSystems();i++) {
            StarSystem sys0 = StarSystemFactory.current().newSystem(g);
            if (needHabitable) {
                while ((sys0 == null) || !sys0.planet().isEnvironmentFriendly())
                    sys0 = StarSystemFactory.current().newSystem(g);
                needHabitable = false;
            }
            sys0.setXY(empSystem.x(i), empSystem.y(i));
            g.addStarSystem(sys0);
        }
    }
    private void addAlienRaceSystemsForGalaxy(Galaxy g, int startId, List<EmpireSystem> empSystems, List<String> alienRaces) {
        IGameOptions opts = GameSession.instance().options();
        // creates a star system for each race, and then additional star
        // systems based on the galaxy size selected at startup

        // get possible banner colors, remove player's color, then randomize
        List<Integer> raceColors = opts.possibleColors();

        // possible the galaxy shape could not fit in all of the races
        int maxRaces = min(alienRaces.size(), empSystems.size());

        int empId = startId;
        for (int h=0;h<maxRaces;h++) {
            Race r = Race.keyed(alienRaces.get(h));
            EmpireSystem empSystem = empSystems.get(h);
            Integer colorId = raceColors.remove(0);
            if (raceColors.isEmpty())
                raceColors =  opts.possibleColors();
            if (colorId == options().selectedPlayerColor()) {
                colorId = raceColors.remove(0);
                if (raceColors.isEmpty())
                    raceColors = opts.possibleColors();
            }
            StarSystem sys = StarSystemFactory.current().newSystemForRace(r,g);
            sys.setXY(empSystem.colonyX(), empSystem.colonyY());
            sys.name(r.nextAvailableHomeworld());
            g.addStarSystem(sys);
            Empire emp = new Empire(g, empId, r.id, sys, colorId, null);
            //log("Adding star system: ", sys.name(), " - ", r.id, " : ", fmt(sys.x(),2), "@", fmt(sys.y(),2));
            g.addEmpire(emp);
            empId++;
            // create two nearby system within 3 light-years (required to be at least 1 habitable)
            boolean needHabitable = true;
            for (int i=1;i<empSystem.numSystems();i++) {
                StarSystem sys0 = StarSystemFactory.current().newSystem(g);
                if (needHabitable) {
                    while ((sys0 == null) || !sys0.planet().isEnvironmentFriendly())
                        sys0 = StarSystemFactory.current().newSystem(g);
                    needHabitable = false;
                }
                sys0.setXY(empSystem.x(i), empSystem.y(i));
                g.addStarSystem(sys0);
            }
        }
    }
    private void addUnsettledSystemsForGalaxy(Galaxy g, GalaxyShape sh) {
        Point.Float pt = new Point.Float();
        // add Orion, index =0;
        StarSystem orion = StarSystemFactory.current().newOrionSystem(g);
        sh.coords(0, pt);
        orion.setXY(pt.x, pt.y);
        orion.name(text("PLANET_ORION"));
        g.addStarSystem(orion);
        
        // add all other systems, starting at index 1
        for (int i=1;i<sh.numberStarSystems();i++) {
            StarSystem sys = StarSystemFactory.current().newSystem(g);
            sh.coords(i, pt);
            sys.setXY(pt.x, pt.y);
            g.addStarSystem(sys);
        }
        log("total systems created: ", str(g.numStarSystems()));
    }
    private void addNebulas(Galaxy g, GalaxyShape shape) {
        IGameOptions opts = GameSession.instance().options();
        // creates a star system for each race, and then additional star
        // systems based on the galaxy size selected at startup
        int numNebula= opts.numberNebula();
        g.initNebulas(numNebula);
        for (int i=0;i<numNebula;i++)
            g.addNebula(shape);
    }
}
