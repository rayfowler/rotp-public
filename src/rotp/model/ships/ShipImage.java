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
package rotp.model.ships;

import java.util.ArrayList;
import java.util.List;

public class ShipImage {
    List<String> iconKeys = new ArrayList<>();
    int currentFrame = 0;

    public List<String> icons()   { return iconKeys; }
    public String baseIcon()      { return iconKeys.get(0); }
    public String currentIcon()   { return iconKeys.get(currentFrame); }
    public String nextIcon()  {
        currentFrame = (currentFrame +1) % iconKeys.size();
        return currentIcon();
    }
}
