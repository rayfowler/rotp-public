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
package rotp.ui.sprites;

import java.awt.Color;
import java.awt.PaintContext;
import java.awt.geom.Point2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import rotp.util.Base;

public class RoundGradientContext implements Base, PaintContext {
    protected Point2D mPoint;
    protected Point2D mRadius;
    protected Color mC1, mC2;
    protected int flareLevel;

    public RoundGradientContext(Point2D p,  Color c1, Point2D r, Color c2, int f) {
        set(p, c1, r, c2, f);
    }
    public void set(Point2D p, Color c1, Point2D r, Color c2, int f) {
        mPoint = p;
        mC1 = c1;
        mRadius = r;
        mC2 = c2;
        flareLevel = f;
    }
    @Override
    public void dispose() { }
    @Override
    public ColorModel getColorModel() {
        return ColorModel.getRGBdefault();
    }
    @Override
    public Raster getRaster(int x, int y, int w, int h) {
        WritableRaster raster = getColorModel().createCompatibleWritableRaster(w, h);

        int[] data = new int[w * h * 4];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                boolean onFlareAxis = flareLevel > 0 && ((mPoint.getX() == (x+i)) || (mPoint.getY() == (y+j)));
                float distance = (float) mPoint.distance(x + i, y + j);
                float radius = (float) mRadius.distance(0, 0)+flareLevel;

                // because of flares, the draw radius can be greater than
                // the star radius on the x/y axes
                float drawRadius = radius;
                if (onFlareAxis)
                    drawRadius = radius * sqrt(flareLevel+1);

                float ratio = distance / drawRadius;
                float coreEdge = radius / 3;
                float ratio1 = ratio;
                float ratio2 = ratio;

                if (radius <= 18) {
                    if (distance > drawRadius)
                        ratio2 = ratio1 = 1.0f;
                    else if (distance <= coreEdge)
                        ratio2 = ratio1 = 0;
                    else 
                        ratio2 = ratio1 = (distance - coreEdge) / (drawRadius -coreEdge);
                }
                else {
                    float radiusPct = 0.15f;
                    if (ratio > 1.0)
                        // outside the star display radius, complete transparency
                        ratio2 = ratio1 = 1.0f;
                    else if (ratio < radiusPct)
                        // in the star core... no opacity, use core color
                        ratio2 = ratio1 = 0;
                    else {
                        // linear from 0 (full star) to 1 (no star)
                        ratio = (ratio - radiusPct) / (1 - radiusPct);
                        ratio2 = sqrt(ratio);
                        ratio1 = ratio*ratio;
                        // get darker faster
                    }
                }
                // ratio1 controls the transition from the star core color (mc1),
                // typically white, to the star's spectal color (mc0), typically
                // red/yellow/green/blue/etc. This seems to work better as a slow
                // transition from 0 to 1, thus ratio1 is the ratio squared
                // ratio2 controls the opacity... how quickly the starlight
                // becomes transparent as it moves away from the star. This seems
                // to work better as a quick transition from 0 to 1, therefore
                // ratio2 is the square root of the ratio
                int base = (j * w + i) * 4;
                data[base + 0] = (int)(mC1.getRed()   + ratio1 * (mC2.getRed()   - mC1.getRed()));
                data[base + 1] = (int)(mC1.getGreen() + ratio1 * (mC2.getGreen() - mC1.getGreen()));
                data[base + 2] = (int)(mC1.getBlue()  + ratio1 * (mC2.getBlue()  - mC1.getBlue()));
                data[base + 3] = (int)(mC1.getAlpha() + ratio2 * (mC2.getAlpha() - mC1.getAlpha()));
            }
        }
        raster.setPixels(0, 0, w, h, data);
        return raster;
    }
}

