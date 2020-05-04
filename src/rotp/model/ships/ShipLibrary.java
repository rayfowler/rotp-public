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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import rotp.ui.BasePanel;
import rotp.util.Base;

public class ShipLibrary implements Base {
    static ShipLibrary instance = new ShipLibrary();
    public static ShipLibrary current()   { return instance; }

    public static final int sizes = 4;
    public static final int designsPerSize = 6;
    public static final String imageDir = "images/ships/";
    public static final String setFilename = "listing.txt";

    public ImageIcon stargate;
    public List<String> styles = new ArrayList<>();
    public List<String> unchosenStyles = new ArrayList<>();
    public List<ShipStyle> shipStyles = new ArrayList<>();

    private static final String[] sizeKey = { "A", "B", "C", "D" };
    private static final String[] designKey = { "01", "02", "03", "04", "05", "06" };
    private static final String[] frameKey = { "a", "b", "c", "d", "e", "f", "g", "h"};

    static {
        current().loadData();
    }
    public int selectRandomUnchosenSet() {
        if (unchosenStyles.isEmpty())
            resetUnchosenStyles();

        String setName = random(unchosenStyles);
        unchosenStyles.remove(setName);
        return styles.indexOf(setName);
    }
    public BufferedImage scoutImage(Integer colorId) {
        int destH = BasePanel.s10;
        int destW = BasePanel.s17;
        int[] pX = new int[3];
        int[] pY = new int[3];
        
        pX[0] = BasePanel.s4;
        pX[1] = BasePanel.s4;
        pX[2] = BasePanel.s15;
        pY[0] = BasePanel.s1;
        pY[1] = BasePanel.s9;
        pY[2] = BasePanel.s5;
        
        BufferedImage destImg = newBufferedImage(destW, destH);
        Graphics2D g = (Graphics2D) destImg.getGraphics();
        setFontHints(g);
        Color c0 = options().color(colorId);
        g.setColor(c0);
        g.fillPolygon(pX, pY, 3);
        g.setStroke(BasePanel.stroke2);
        g.setColor(Color.black);
        g.drawPolygon(pX, pY, 3);
        g.dispose();
        return destImg;
    }
    public BufferedImage shipImage(Integer colorId) {
        int destH = BasePanel.s12;
        int destW = BasePanel.s20;
        int[] pX = new int[3];
        int[] pY = new int[3];
        
        int s1 = BasePanel.s1;
        int s2 = BasePanel.s2;
        int s3 = BasePanel.s3;
        int s6 = BasePanel.s6;
        int s9 = BasePanel.s9;
        
        pX[0] = BasePanel.s4;
        pX[1] = BasePanel.s4;
        pX[2] = BasePanel.s18;
        pY[0] = BasePanel.s1;
        pY[1] = BasePanel.s11;
        pY[2] = BasePanel.s6;
        
        BufferedImage destImg = newBufferedImage(destW, destH);
        Graphics2D g = (Graphics2D) destImg.getGraphics();
        setFontHints(g);
        Color c0 = options().color(colorId);
        g.setColor(c0);
        g.fillPolygon(pX, pY, 3);
        g.setStroke(BasePanel.stroke2);
        g.setColor(Color.yellow);
        g.fillRect( 0, s6, s3, s1);
        g.setColor(Color.orange);
        g.fillRect(s1, s3, s2, s1);
        g.fillRect(s1, s9, s2, s1);
        g.setColor(Color.black);
        g.drawPolygon(pX, pY, 3);
        g.dispose();
        return destImg;
    }
    public BufferedImage shipImageLarge(Integer colorId) {
        int destH = BasePanel.s16;
        int destW = BasePanel.s25;
        int[] pX = new int[3];
        int[] pY = new int[3];
        
        int s1 = BasePanel.s1;
        int s2 = BasePanel.s2;
        int s3 = BasePanel.s3;
        int s8 = BasePanel.s8;
        int s13 = BasePanel.s13;
        
        pX[0] = BasePanel.s4;
        pX[1] = BasePanel.s4;
        pX[2] = BasePanel.s23;
        pY[0] = BasePanel.s1;
        pY[1] = BasePanel.s15;
        pY[2] = BasePanel.s8;
        
        BufferedImage destImg = newBufferedImage(destW, destH);
        Graphics2D g = (Graphics2D) destImg.getGraphics();
        setFontHints(g);
        Color c0 = options().color(colorId);
        g.setColor(c0);
        g.fillPolygon(pX, pY, 3);
        g.setStroke(BasePanel.stroke2);
        g.setColor(Color.yellow);
        g.fillRect( 0, s8, s3, s1);
        g.setColor(Color.orange);
        g.fillRect(s1, s3, s2, s1);
        g.fillRect(s1, s13, s2, s1);
        g.setColor(Color.black);
        g.drawPolygon(pX, pY, 3);
        g.dispose();
        return destImg;
    }
    public BufferedImage shipImageHuge(Integer colorId) {
         int destH = BasePanel.s20;
        int destW = BasePanel.s30;
        int[] pX = new int[3];
        int[] pY = new int[3];
        
        int s1 = BasePanel.s1;
        int s2 = BasePanel.s2;
        int s3 = BasePanel.s3;
        int s6 = BasePanel.s6;
        int s10 = BasePanel.s10;
        int s14 = BasePanel.s14;
        int s17 = BasePanel.s17;
        
        pX[0] = BasePanel.s4;
        pX[1] = BasePanel.s4;
        pX[2] = BasePanel.s28;
        pY[0] = BasePanel.s1;
        pY[1] = BasePanel.s19;
        pY[2] = BasePanel.s10;
        
        BufferedImage destImg = newBufferedImage(destW, destH);
        Graphics2D g = (Graphics2D) destImg.getGraphics();
        setFontHints(g);
        Color c0 = options().color(colorId);
        g.setColor(c0);
        g.fillPolygon(pX, pY, 3);
        g.setStroke(BasePanel.stroke2);
        g.setColor(Color.yellow);
        g.fillRect( 0, s10, s3, s1);
        g.setColor(Color.orange);
        g.fillRect(s1, s3, s2, s1);
        g.fillRect(s1, s6, s2, s1);
        g.fillRect(s1, s14, s2, s1);
        g.fillRect(s1, s17, s2, s1);
        g.setColor(Color.black);
        g.drawPolygon(pX, pY, 3);
        g.dispose();
        return destImg;
    }
    public BufferedImage transportImage(Integer colorId) {
        int destH = BasePanel.s7;
        int destW = BasePanel.s16;
        int s1 = BasePanel.s1;
        int s2 = BasePanel.s2;
        int crv = BasePanel.s4;
        BufferedImage destImg = newBufferedImage(destW, destH);
        Graphics2D g = (Graphics2D) destImg.getGraphics();
        setFontHints(g);
        Color c0 = options().color(colorId);
        g.setColor(c0);
        g.fillRoundRect(s1,s1,destW-s2,destH-s2,crv,crv);
        g.setStroke(BasePanel.stroke2);
        g.setColor(Color.black);
        g.drawRoundRect(s1,s1,destW-s2,destH-s2,crv,crv);
        g.dispose();
        return destImg;
    }
    public ShipImage shipImage(int styleNum, int size, int num) {
        int shipSeq = (size * designsPerSize) + num;
        List<ShipImage> images = shipStyles.get(styleNum).images;
        return images.get(shipSeq);
    }
    public String shipKey(int i, int size, int num) {
        int shipSeq = (size * designsPerSize) + num;
        List<ShipImage> images = shipStyles.get(i).images;
        return images.get(shipSeq).nextIcon();
    }

    public List<String> validIconKeys(int labNum, int size) {
        List<String> validIconKeys = new ArrayList<>();

        List<ShipImage> images = shipStyles.get(labNum).images;
        for (int i=0;i<designsPerSize;i++) {
            int index = (size*designsPerSize)+i;
            if (index >= images.size()) {
                err("ERROR: icon index:"+index+"  iconSize:"+images.size()+"   labnum:"+labNum+"  size:"+size);
            }
            ShipImage image = images.get((size*designsPerSize)+i);
            validIconKeys.add(image.currentIcon());
        }
        return validIconKeys;
    }
    private void loadData() {
        log("Loading Ship Sets...");
        styles.clear();
        unchosenStyles.clear();
        shipStyles.clear();

        stargate = icon("images/ships/stargate_icon.png");

        loadSetFile();
        resetUnchosenStyles();

        for (int i=0;i<styles.size();i++) {
            ShipStyle style = new ShipStyle(styles.get(i));
            shipStyles.add(style);
            for (int j=0;j<sizes;j++) {
                for (int k=0;k<designsPerSize;k++) {
                    ShipImage styleImage = new ShipImage();
                    style.images.add(styleImage);
                    String shipIconKey = fileName(i,j,k);
                    if (url(shipIconKey) != null)
                        styleImage.iconKeys.add(shipIconKey);
                    else {
                        for (String f: frameKey) {
                            shipIconKey = fileName(i,j,k,f);
                            if (url(shipIconKey) != null) 
                                styleImage.iconKeys.add(shipIconKey);
                        }
                    }
                }
            }
        }
    }
    private void resetUnchosenStyles() {
        unchosenStyles.clear();
        for (int i=0;i<styles.size();i++)
            unchosenStyles.add(styles.get(i));
    }
    private String fileName(int i, int j, int k) {
        String setName = styles.get(i);
        return imageDir+setName+"/"+sizeKey[j]+designKey[k]+".png";
    }
    private String fileName(int i, int j, int k, String f) {
        String setName = styles.get(i);
        return imageDir+setName+"/"+sizeKey[j]+designKey[k]+f+".png";
    }
    private void loadSetFile() {
        BufferedReader in = reader(imageDir+setFilename);
        if (in == null)
                return;

        try {
            String input;
            while ((input = in.readLine()) != null)
                loadSetLine(input);
            in.close();
        }
        catch (IOException e) {
            System.err.println("ShipLibrary.loadSetFile -- IOException: " + e);
        }
    }
    private void loadSetLine(String input) {
        if (isComment(input))
            return;

        int mark = input.indexOf(',', 0);
        String setName = input.substring(0, mark).trim();
        styles.add(setName);
    }
}
