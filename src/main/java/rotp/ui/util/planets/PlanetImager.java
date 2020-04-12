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
import javax.swing.ImageIcon;
import rotp.model.planet.Planet;
import rotp.util.Base;
import rotp.util.ColorMap;
import rotp.util.FastImage;

public enum PlanetImager implements Base {
	INSTANCE;
	public static PlanetImager current()  { return INSTANCE; }

	private FastImage terrainBase;

	private final static int LEFT_RIGHT_TOP_BOTTOM = 0;
	private final static int RIGHT_LEFT_TOP_BOTTOM = 1;
	private final static int LEFT_RIGHT_BOTTOM_TOP = 2;
	private final static int RIGHT_LEFT_BOTTOM_TOP = 3;
	private final static int LEFT_RIGHT_TOP_BOTTOM_INVERT = 4;
	private final static int RIGHT_LEFT_TOP_BOTTOM_INVERT = 5;
	private final static int LEFT_RIGHT_BOTTOM_TOP_INVERT = 6;
	private final static int RIGHT_LEFT_BOTTOM_TOP_INVERT = 7;


	private PlanetImager() {
	}
	public FastImage terrainBase() {
		if (terrainBase == null) {
			ImageIcon i = icon("images/planets/terrainMap_tiny.png");
			terrainBase = FastImage.from(i.getImage());
		}
		return terrainBase;
	}
	public void finished() {
		terrainBase = null;
	}
	public FastImage getTerrainSubImage(int w, int h) {
		return getSubImage(terrainBase(), w, h, roll(0,7));
	}
	private static FastImage getSubImage(FastImage baseImg, int w, int h, int orientation) {
		int imgH = baseImg.getHeight();
		int imgW = baseImg.getWidth();
		int imgOffsetX = (int) (Math.random()*(imgW-w));
		int imgOffsetY = (int) (Math.random()*(imgH-h));
		FastImage img = FastImage.sized(w, h);

		switch(orientation) {
			case RIGHT_LEFT_TOP_BOTTOM:
				for (int y=0;y<h;y++)    for (int x0=0;x0<w;x0++) {
					int x = w-x0-1;
					int pixel = baseImg.getRGB(x+imgOffsetX, y+imgOffsetY);
					img.setRGB(x, y, pixel);
				}
				break;
			case RIGHT_LEFT_TOP_BOTTOM_INVERT:
				for (int y=0;y<h;y++)    for (int x0=0;x0<w;x0++) {
					int x = w-x0-1;
					int pixel = 255-baseImg.getRGB(x+imgOffsetX, y+imgOffsetY);
					img.setRGB(x, y, pixel);
				}
				break;
			case RIGHT_LEFT_BOTTOM_TOP:
				for (int y0=0;y0<h;y0++)    for (int x0=0;x0<w;x0++) {
					int y = h-y0-1;
					int x = w-x0-1;
					int pixel = baseImg.getRGB(x+imgOffsetX, y+imgOffsetY);
					img.setRGB(x, y, pixel);
				}
				break;
			case RIGHT_LEFT_BOTTOM_TOP_INVERT:
				for (int y0=0;y0<h;y0++)    for (int x0=0;x0<w;x0++) {
					int y = h-y0-1;
					int x = w-x0-1;
					int pixel = 255-baseImg.getRGB(x+imgOffsetX, y+imgOffsetY);
					img.setRGB(x, y, pixel);
				}
				break;
			case LEFT_RIGHT_BOTTOM_TOP:
				for (int y0=0;y0<h;y0++)    for (int x=0;x<w;x++) {
					int y = h-y0-1;
					int pixel = baseImg.getRGB(x+imgOffsetX, y+imgOffsetY);
					img.setRGB(x, y, pixel);
				}
				break;
			case LEFT_RIGHT_BOTTOM_TOP_INVERT:
				for (int y0=0;y0<h;y0++)    for (int x=0;x<w;x++) {
					int y = h-y0-1;
					int pixel = 255-baseImg.getRGB(x+imgOffsetX, y+imgOffsetY);
					img.setRGB(x, y, pixel);
				}
				break;
			case LEFT_RIGHT_TOP_BOTTOM_INVERT:
				for (int y=0;y<h;y++)    for (int x=0;x<w;x++) {
					int pixel = 255-baseImg.getRGB(x+imgOffsetX, y+imgOffsetY);
					img.setRGB(x, y, pixel);
				}
				break;
			default:
			case LEFT_RIGHT_TOP_BOTTOM:
				for (int y=0;y<h;y++)    for (int x=0;x<w;x++) {
					int pixel = baseImg.getRGB(x+imgOffsetX, y+imgOffsetY);
					img.setRGB(x, y, pixel);
				}
				break;
		}
		return img;
	}
	public static FastImage createCompositeImage(FastImage terrainMap, Planet p) {
		int w = terrainMap.getWidth();
		int h = terrainMap.getHeight();

		ColorMap cMap = p.type().colorMap();

		int minV = (256*4);

		int oceanLevel = p.oceanLevel;
		int iceLevel = p.iceLevel();

		int iceR = p.iceColor.getRed();
		int iceG = p.iceColor.getGreen();
		int iceB = p.iceColor.getBlue();

		//FastImage compositeImg = FastImage.sized(w, h);
		FastImage compositeImg = terrainMap;

		for (int x=0; x < w; x++) {
			for (int y=0; y<h; y++) {
				int landPx = terrainMap.getRGB(x,y);
				int landA = (landPx >> 24) & 0xff;

				// latPct = pct distance away from equator (0=equator, 1=pole)
				float latPct = Math.abs(y-(h/2)) / h;
				// lat = convert latPct to a value from 0 to 255
				int lat = (int) latPct * 255;
				// landLevel is 0-255 value of any channel (RGB) in pixel (it's gray)
				int landLevel = landPx & 0xff;

				int landR = 0;
				int landG = 0;
				int landB = 0;
				// if we have ice, weight the latitude (2), landLevel (2),
				// and iceLevel(4) against minV (4) to see if ice covers
				// this x,y location
				if ((iceLevel > 0)
						&& (((2*lat) + (2*(256-landLevel)) + (4*iceLevel)) > minV)) {
					landR = iceR;
					landG = iceG;
					landB = iceB;
				}
				// else check terrain color map for planet type
				else {
					Color mapColor = cMap.colorAt(landLevel, oceanLevel);
					landR = mapColor.getRed();
					landG = mapColor.getGreen();
					landB = mapColor.getBlue();
				}
				if (landA == 0) {
					// this should never happen... it means the PlanetHeightMap has
					// uninitialized pixels within the sphere image. Make them bright
					// red so we can find and fix them
					landR = 255;
					landG = 0;
					landB = 0;
				}
				// store pixel in composite img
				int newPixel = (255 << 24) | (landR << 16) | (landG << 8) | landB;
				compositeImg.setRGB(x,y,newPixel);
			}
		}
		return compositeImg;
	}
}
