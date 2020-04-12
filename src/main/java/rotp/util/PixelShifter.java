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

import java.util.List;

public class PixelShifter implements Base, ImageTransformer {
    int r_shift = 50;
    int g_shift = 50;
    int b_shift = 50;
    int a_shift = 50;

    public static ImageTransformer createFrom(String s) {
        if (s.equalsIgnoreCase("Darlok"))
            return new DarlokViewer();
        return new PixelShifter(s);
    }
    private PixelShifter(String s) {
        List<String> vals = this.substrings(s, ',');
        if (vals.size() > 0) 
            r_shift = bounds(0, parseInt(vals.get(0)), 255);
        if (vals.size() > 1) 
            g_shift = bounds(0, parseInt(vals.get(1)), 255);
        if (vals.size() > 2) 
            b_shift = bounds(0, parseInt(vals.get(2)), 255);
        if (vals.size() > 3) 
            a_shift = bounds(0, parseInt(vals.get(3)), 255);
    }
    @Override
    public int transformPixel(int pixel) {
        int a = pixel >> 24 & 0xff;
        int r = pixel >> 16 & 0xff;
        int g = pixel >> 8 & 0xff;
        int b = pixel >> 0 & 0xff;
        
        if (r_shift < 50) 
            r = max(0,r*r_shift/50);
        else if (r_shift > 50)
            r  = min(255, r + ((100-r_shift)/50*(255-r)));
        if (g_shift < 50) 
            g = max(0,g*g_shift/50);
        else if (g_shift > 50)
            g  = min(255, g + ((100-g_shift)/50*(255-g)));
        if (b_shift < 50) 
            b = max(0,b*b_shift/50);
        else if (b_shift > 50)
            b  = min(255, b + ((100-b_shift)/50*(255-b)));
        if (a_shift < 50) 
            a = max(0,a*a_shift/50);
        else if (a_shift > 50)
            a  = min(255, a + ((100-a_shift)/50*(255-a)));
        
        return (a << 24)+(r << 16)+(g << 8)+b;
    }
}
