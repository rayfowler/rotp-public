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

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public enum ImageManager implements Base {
    INSTANCE;
    public static ImageManager current() { return INSTANCE; }
    static final String baseListFile = "data/images.txt";
    static final String gnnListFile = "images/gnn/images.txt";
    static final String planetListFile = "images/planets/images.txt";
    static final String missileListFile = "images/missiles/images.txt";
    static final String flagListFile = "images/flags/images.txt";

    private final HashMap<String, List<String>> imageFiles = new HashMap<>();

    private ImageManager() {
        loadImageList(baseListFile);
        loadImageList(gnnListFile);
        loadImageList(planetListFile);
        loadImageList(missileListFile);
        loadImageList(flagListFile);
    }
    public boolean valid(String key)  {
        return key.equalsIgnoreCase("NULL") || imageFiles.containsKey(key);
    }
    @Override
    public Image image(String key) {
        return key.equalsIgnoreCase("NULL") || key.isEmpty() ? null : icon(random(imageFiles.get(key))).getImage();
    }
    @Override
    public Image scaledImageW(String key, int newW) {
        Image img = image(key);

        int newH = img.getHeight(null)*newW/img.getWidth(null);

        if ((newW <=0) || (newH <=0) || (newW > 2000))
            return img;

        BufferedImage newImg = newBufferedImage(newW, newH);
        Graphics g = newImg.getGraphics();
        g.drawImage(img, 0,0, newW, newH, null);
        g.dispose();
        return newImg;
    }
    public void loadImageList(String filename) {
        log("Loading Images: ", filename);
        BufferedReader in = reader(filename);
        if (in == null)
            return;

        try {
            String input;
            while ((input = in.readLine()) != null)
                loadImageReference(input.trim());
            in.close();
        }
        catch (IOException e) {
            err("ImageManager.loadImageList -- IOException: " + e);
        }
    }
    private void loadImageReference(String line) {
        if (isComment(line))
            return;

        List<String> vals = substrings(line, '|');
        if (vals.size() < 2) {
            err("Not enough fields for image line: ", line);
            return;
        }
        // validation takes too long at startup
        //if (icon(vals.get(1)) == null) {
        //  err("No image found at: ", vals.get(1));
        //  return;
        //}
        String imageKey = vals.get(0);
        if (!imageFiles.containsKey(imageKey))
            imageFiles.put(imageKey, new ArrayList<>());
        imageFiles.get(imageKey).add(vals.get(1));
    }
}
