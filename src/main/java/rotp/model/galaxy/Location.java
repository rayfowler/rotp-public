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

import java.io.Serializable;
import rotp.util.Base;

public class Location implements IMappedObject, Base, Serializable {
    private static final long serialVersionUID = 1L;
    private float x;
    private float y;

    @Override
    public float x()        { return x; }
    public void x(float d)  { x = d; }
    @Override
    public float y()        { return y;  }
    public void y(float d)  { y = d; }

    public void setXY(float d1, float d2) {
        x(d1);
        y(d2);
    }
    public Location() {
        x = 0; y = 0; 
    }
    public Location(float x1, float y1) {
        x = x1; y = y1; 
    }
    public Location(IMappedObject loc) {
        x = loc.x(); y = loc.y();
    }
}
