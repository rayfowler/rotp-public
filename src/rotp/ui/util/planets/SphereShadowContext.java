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
package rotp.ui.util.planets;

import java.awt.Color;
import java.awt.PaintContext;
import java.awt.geom.Point2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import rotp.util.Base;

public enum SphereShadowContext implements Base, PaintContext {
	INSTANCE;
	public static SphereShadowContext current()  { return INSTANCE; }
	// mPoint is the center of the sphere we are shading
	// mPoint0 is the center of the sphere that represents the "lit" portion.
	//    if it offset from mPoint so that the uncovered area is fully shaded
	protected Point2D mPoint, mPoint0;
	protected Point2D mRadius, mRadius0;
	protected Color mC1, mC2;
	public void set(Point2D p, Point2D p0, Color c2, Point2D r, Point2D r0) {
		mPoint = p;
		mPoint0 = p0;
		mC2 = c2;
		mC1 = newColor(c2.getRed(), c2.getGreen(), c2.getBlue(), 0);
		mRadius = r;
		mRadius0 = r0;
	}
	@Override
	public void dispose() {
	}

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
				float distance = (float) mPoint.distance(x + i, y + j);
				float distance0 = (float) mPoint0.distance(x + i, y + j);
				float radius = (float) mRadius.distance(0, 0);
				float radius0 = (float) mRadius0.distance(0, 0);
				float ratio = distance0 / radius0;

				// any point completely outside the radius of the sphere gets NO shading as well
				// added a 1-pixel buffer to eliminate pixelation artifacts (a little clunky on smaller circles)
				if (distance > (radius+1))
					ratio = 0;
					// any point outside the light radius gets FULL shadow
				else if (distance0 > radius0)
					ratio = 1;
					// any point that is closer to the light-center than the sphere-center also
					// gets NO shading whatsoever. this avoids gradual shading in the "wrong" direction
//             else if (distance0 <= distance)
//                 ratio = 0;
					// finally shade according to ratio from the light, but sharpen the edge after boundary checks
				else if (ratio < 0.75f)
					ratio = 0;
				else
					ratio = (ratio - 0.75f) * 4;


				int base = (j * w + i) * 4;
				data[base + 0] = (int)(mC1.getRed()   + ratio * (mC2.getRed()   - mC1.getRed()));
				data[base + 1] = (int)(mC1.getGreen() + ratio * (mC2.getGreen() - mC1.getGreen()));
				data[base + 2] = (int)(mC1.getBlue()  + ratio * (mC2.getBlue()  - mC1.getBlue()));
				data[base + 3] = (int)(mC1.getAlpha() + ratio * (mC2.getAlpha() - mC1.getAlpha()));
			}
		}
		raster.setPixels(0, 0, w, h, data);
		return raster;
	}

}