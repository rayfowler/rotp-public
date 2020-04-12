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

import java.awt.Font;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import rotp.Rotp;

public enum FontManager implements Base {
    INSTANCE;
    public static FontManager current()  { return INSTANCE; }

    private Font logoFont;
    private Font introFont;
    private Font dlgFont;
    private Font narrowFont;
    private Font plainFont;
    private int logoSize, introSize, dlgSize, narrowSize, plainSize;
    private Font[] createdLogoFont;
    private Font[] createdDlgFont;
    private Font[] createdNarrowFonts;
    private Font[] createdPlainFonts;
    private Font[] createdIntroFonts;

    @Override
    public Font dlgFont(int i) {
        if (createdDlgFont[i] == null)
            createdDlgFont[i] = dlgFont.deriveFont((float) scaled(i*dlgSize/100));
        return createdDlgFont[i];
    }
    @Override
    public Font narrowFont(int i) {
        if (createdNarrowFonts[i] == null)
            createdNarrowFonts[i] = narrowFont.deriveFont((float) scaled(i*narrowSize/100));
        return createdNarrowFonts[i];
    }
    @Override
    public Font plainFont(int i) {
        if (createdPlainFonts[i] == null)
            createdPlainFonts[i] = plainFont.deriveFont((float) scaled(i*plainSize/100));
        return createdPlainFonts[i];
    }
    @Override
    public Font font(int i) {
        if (createdIntroFonts[i] == null)
            createdIntroFonts[i] = introFont.deriveFont((float) scaled(i*introSize/100));
        return createdIntroFonts[i];
    }
    @Override
    public Font logoFont(int i) {
        if (createdLogoFont[i] == null)
            createdLogoFont[i] = logoFont.deriveFont((float) scaled(i*logoSize/100));
        return createdLogoFont[i];
    }
    private void initFonts() {
        createdLogoFont = new Font[100];
        createdDlgFont = new Font[100];
        createdNarrowFonts = new Font[100];
        createdPlainFonts = new Font[100];
        createdIntroFonts = new Font[100];
    }
    public void resetFonts() {
        initFonts();
    }
    public void loadFonts(String baseDir, String langDir) {
        String fontDir = baseDir+"fonts/";
        String dir = baseDir+langDir+"/";
        log("Loading fonts - baseDir: ", baseDir, "  fontDir: ", fontDir, "  langDir: ", dir);
        initFonts();
        String dataFile = "fonts.txt";

        BufferedReader in = reader(dir+dataFile);
        if (in == null) {
            err("can't find fonts file! ", dir, dataFile);
            return;
        }
        try {
            String input;
            while ((input = in.readLine()) != null)
                loadFontLine(input, fontDir);
            in.close();
        }
        catch (IOException e) {
            err("FontManager.loadFonts -- IOException: ", e.toString());
        }
    }
    private void loadFontLine(String input, String fontDir) {
        if (isComment(input))
            return;

        List<String> fields = substrings(input, ',');
        String fontName = fields.get(0);
        String filename = fields.size() > 1 ? fields.get(1) : fields.get(0);
        int fontSizing = fields.size() > 2 ? parseInt(fields.get(2)): 100;

        InputStream is = Rotp.class.getResourceAsStream(fontDir+filename);
        if (is == null) {
            err("FontManager.loadFont: could not get inputStream for:"+fontDir+filename);
            return;
        }

        try {
            Font newFont = Font.createFont(Font.TRUETYPE_FONT, is);
            if (fontName.equalsIgnoreCase("dialogue")) {
                dlgFont = newFont;
                dlgSize = fontSizing;
            }
            else if (fontName.equalsIgnoreCase("narrow")) {
                narrowFont = newFont;
                narrowSize = fontSizing;
            }
            else if (fontName.equalsIgnoreCase("plain")) {
                plainFont = newFont;
                plainSize = fontSizing;
            }
            else if (fontName.equalsIgnoreCase("normal")) {
                introFont = newFont;
                introSize = fontSizing;
            }
            else if (fontName.equalsIgnoreCase("logo")) {
                logoFont = newFont;
                logoSize = fontSizing;
            }
        }
        catch (Exception e) {
            err("FontManager.loadFont -- Exception: " + e.getMessage());
        }
    }
}
