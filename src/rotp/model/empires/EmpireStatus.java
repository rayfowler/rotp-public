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

import java.io.Serializable;
import rotp.util.Base;

public class EmpireStatus implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    private static final int TURNS = 100;
    final public static int FLEET = 0;
    final public static int POPULATION = 1;
    final public static int TECHNOLOGY = 2;
    final public static int PLANETS = 3;
    final public static int PRODUCTION = 4;
    final public static int POWER = 5;

    private final Empire empire;
    private int[] fleetStrength = new int[TURNS];
    private int[] population = new int[TURNS];
    private int[] technology = new int[TURNS];
    private int[] planets = new int[TURNS];
    private int[] production = new int[TURNS];
    private int[] power = new int[TURNS];
    public EmpireStatus (Empire e) {
        empire = e;
    }
    public String title(int cat) {
        switch (cat) {
            case EmpireStatus.FLEET      : return text("RACES_STATUS_FLEET_STRENGTH");
            case EmpireStatus.POPULATION : return text("RACES_STATUS_POPULATION");
            case EmpireStatus.TECHNOLOGY : return text("RACES_STATUS_TECHNOLOGY");
            case EmpireStatus.PLANETS    : return text("RACES_STATUS_PLANETS");
            case EmpireStatus.PRODUCTION : return text("RACES_STATUS_PRODUCTION");
            case EmpireStatus.POWER      : return text("RACES_STATUS_TOTAL_POWER");
        }
        return "";
    }
    public void assessTurn() {
        int turn = galaxy().numberTurns();
        if (turn >= fleetStrength.length)
            growLists();
        
        fleetStrength[turn] = currentFleetStrengthValue();
        population[turn] = currentPopulationValue();
        technology[turn] = currentTechnologyValue();
        planets[turn] = currentPlanetsValue();
        production[turn] = currentProductionValue();
        power[turn] = currentPowerValue();
    }
    public int[] values(int cat) {
        switch (cat) {
            case EmpireStatus.FLEET      : return fleetStrength;
            case EmpireStatus.POPULATION : return population;
            case EmpireStatus.TECHNOLOGY : return technology;
            case EmpireStatus.PLANETS    : return planets;
            case EmpireStatus.PRODUCTION : return production;
            case EmpireStatus.POWER      : return power;
        }
        return null;
    }
    public int age(Empire viewer) {
            return galaxy().numberTurns() - lastViewTurn(viewer);
    }
    public int lastViewValue(Empire viewer, int cat) {
        switch(cat) {
            case FLEET:      return valueFor(fleetStrength, lastViewTurn(viewer));
            case POPULATION: return valueFor(population, lastViewTurn(viewer));
            case TECHNOLOGY: return valueFor(technology, lastViewTurn(viewer));
            case PLANETS:    return valueFor(planets, lastViewTurn(viewer));
            case PRODUCTION: return valueFor(production, lastViewTurn(viewer));
            case POWER:      return valueFor(power, lastViewTurn(viewer));
        }
        return 0;
    }
    private int valueFor(int[] vals, int turn) {
        if (turn < 0)
            return -1;
        if (turn >= vals.length)
            return vals[vals.length-1];
        return vals[turn];
    }
    public int lastViewTurn(Empire viewer) {
        if (empire == viewer)
            return galaxy().numberTurns();

        int lastSpyDate = viewer.viewForEmpire(empire).spies().lastSpyDate();

        return lastSpyDate < 0 ? -1 : lastSpyDate - galaxy().beginningYear();
    }
    private void growLists() {
        fleetStrength = larger(fleetStrength);
        population = larger(population);
        technology = larger(technology);
        planets = larger(planets);
        production = larger(production);
        power = larger(power);
    }
    private int[] larger(int[] list) {
        int[] newList = new int[list.length+100];
        System.arraycopy(list, 0, newList, 0, list.length);
        return newList;
    }
    private int currentFleetStrengthValue() {
        return (int)Math.ceil(empire.totalFleetSize());
    }
    private int currentPlanetsValue() {
        return empire.allColonizedSystems().size();
    }
    private int currentPopulationValue() {
        return (int)Math.ceil(empire.totalEmpirePopulation());
    }
    private int currentProductionValue() {
        return (int)Math.ceil(empire.totalPlanetaryProduction());
    }
    private int currentTechnologyValue() {
        return (int)Math.ceil(empire.tech().avgTechLevel());
    }
    private int currentPowerValue() {
        float tech = empire.tech().avgTechLevel();
        float industrialPower = tech * empire.totalPlanetaryProduction();
        float militaryPower = tech * empire.totalFleetSize();
        return (int)Math.ceil(industrialPower+militaryPower);
    }
}
