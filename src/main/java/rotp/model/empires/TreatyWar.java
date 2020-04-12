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

import rotp.util.Base;

public class TreatyWar extends DiplomaticTreaty implements Base {
    private static final long serialVersionUID = 1L;
    int[] coloniesStart = new int[2];
    float[] populationStart = new float[2];
    float[] productionStart = new float[2];
    float[] fleetSizeStart = new float[2];
    int[] coloniesNow = new int[2];
    float[] populationNow = new float[2];
    float[] productionNow = new float[2];
    float[] fleetSizeNow = new float[2];
    
    public TreatyWar(Empire e1, Empire e2) {
        super(e1,e2,"RACES_AT_WAR");
        initValues(e1, e2);
    }    
    @Override
    public boolean isWar()                      { return true; }
    @Override
    public int listOrder()                      { return 2; }
    public int coloniesStart(Empire e)          { return coloniesStart[index(e)]; }
    public float populationStart(Empire e)      { return populationStart[index(e)]; }
    public float productionStart(Empire e)      { return productionStart[index(e)]; }
    public float fleetSizeStart(Empire e)       { return fleetSizeStart[index(e)]; }
    public int coloniesNow(Empire e)            { return coloniesNow[index(e)]; }
    public float populationNow(Empire e)        { return populationNow[index(e)]; }
    public float productionNow(Empire e)        { return productionNow[index(e)]; }
    public float fleetSizeNow(Empire e)         { return fleetSizeNow[index(e)]; }
    
    public float colonyChange(Empire e)         { return (float) coloniesNow[index(e)]/coloniesStart[index(e)]; }
    public float populationChange(Empire e)     { return populationNow[index(e)]/populationStart[index(e)]; }
    public float productionChange(Empire e)     { return productionNow[index(e)]/productionStart[index(e)]; }
    public float fleetSizeChange(Empire e)      { return fleetSizeNow[index(e)]/fleetSizeStart[index(e)]; }
    
           
    @Override
    public void nextTurn(Empire emp) {
        // this will be called separately for each empire from their diplomatic
        // embassy for the other empire
        coloniesNow[index(emp)] = emp.numColonizedSystems();
        populationNow[index(emp)] = emp.totalPlanetaryPopulation();     
        productionNow[index(emp)] = emp.totalPlanetaryProduction();
        fleetSizeNow[index(emp)] = emp.totalFleetSize();     
    }
    private int index(Empire e)  { return e.id == empire1 ? 0 : 1; }    
    private void initValues(Empire e1, Empire e2) {
        coloniesStart[0] = coloniesNow[0] = e1.numColonizedSystems();
        coloniesStart[1] = coloniesNow[1] = e2.numColonizedSystems();
        
        populationStart[0] = populationNow[0] = e1.totalPlanetaryPopulation();
        populationStart[1] = populationNow[1] = e2.totalPlanetaryPopulation();     
         
        productionStart[0] = productionNow[0] = e1.totalPlanetaryProduction();
        productionStart[1] = productionNow[1] = e2.totalPlanetaryProduction();
       
        // minimum 1 to avoid potential /0 when going to war with no ships
        fleetSizeStart[0] = fleetSizeNow[0] = max(1, e1.totalFleetSize());
        fleetSizeStart[1] = fleetSizeNow[1] = max(1, e2.totalFleetSize());     
    }    
  
}
