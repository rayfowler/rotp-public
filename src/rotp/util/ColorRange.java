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
package rotp.util;

import java.awt.Color;

public class ColorRange implements Base {
    Color color1, color2;
    public ColorRange(Color c1, Color c2) {
        color1 = c1;
        color2 = c2;
    }
    public Color randomColor() {
        return color(random());
    }
    public Color color(float r) {
        if (r == 0)
            return color1;
        else if (r == 1)
            return color2;

        int red = color1.getRed() + (int) (r*(color2.getRed()-color1.getRed()));
        int green = color1.getGreen() + (int) (r*(color2.getGreen()-color1.getGreen()));
        int blue = color1.getBlue() + (int) (r*(color2.getBlue()-color1.getBlue()));
        return newColor(red,green,blue);
    }
}
