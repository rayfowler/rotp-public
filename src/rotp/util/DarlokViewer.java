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

public class DarlokViewer implements Base, ImageTransformer {
    @Override
    public int transformPixel(int pixel) {
        int a = pixel >> 24 & 0xff;
        int r = pixel >> 16 & 0xff;
        int g = pixel >> 8 & 0xff;
        int b = pixel >> 0 & 0xff;

        int avg = (r+g+b)/3;

        if (avg < 96) 
            avg = avg / 2;
        else 
            avg = min(255, ((255-avg) / 2)+avg);

        return (a << 24)+(avg << 16)+(avg << 8)+avg;
    }
}
