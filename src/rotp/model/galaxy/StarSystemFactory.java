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

import rotp.model.empires.Race;
import rotp.model.game.GameSession;
import rotp.model.game.IGameOptions;
import rotp.model.planet.PlanetFactory;
import rotp.util.Base;

public class StarSystemFactory implements Base {
    static StarSystemFactory instance = new StarSystemFactory();
    public static StarSystemFactory current()   { return instance; }

    public StarSystem newSystem(Galaxy gal) {
        IGameOptions opts = GameSession.instance().options();
        String type = opts.randomStarType();
        StarSystem sys = StarSystem.create(type, gal);
        return sys;
    }
    public StarSystem newOrionSystem(Galaxy gal) {
        IGameOptions opts = GameSession.instance().options();
        String type = opts.randomOrionStarType();
        StarSystem sys = StarSystem.create(type, gal);
        sys.planet(PlanetFactory.createOrion(sys, session().populationBonus()));
        sys.monster(new OrionGuardianShip());
        sys.name(text("PLANET_ORION"));
        return sys;
    }
    public StarSystem newSystemForRace(Race r, Galaxy gal) {
        IGameOptions opts = GameSession.instance().options();
        String type = opts.randomRaceStarType(r);
        StarSystem sys = StarSystem.create(type, gal);
        sys.planet(PlanetFactory.createHomeworld(r, sys, session().populationBonus()));
        return sys;
    }
    public StarSystem newSystemForPlayer(Race r, Galaxy gal) {
        IGameOptions opts = GameSession.instance().options();
        String type = opts.randomPlayerStarType(r);
        StarSystem sys = StarSystem.create(type, gal);
        sys.planet(PlanetFactory.createHomeworld(r, sys, session().populationBonus()));
        return sys;
    }
}
