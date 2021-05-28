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

import java.awt.ComponentOrientation;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import rotp.model.empires.RaceFactory;
import rotp.ui.UserPreferences;

public class LanguageManager implements Base {
    static LanguageManager instance = new LanguageManager();
    public static LanguageManager current() { return instance; }

    public static int DEFAULT_LANGUAGE = 0;
    private static final String baseDir = "lang/";
    private static final String languageFile = "languages.txt";
    private static final List<Language> languages = new ArrayList<>();
    private static int selectedLanguage = LanguageManager.DEFAULT_LANGUAGE;

    public static void selectDefaultLanguage() { instance.selectLanguage(LanguageManager.DEFAULT_LANGUAGE); }
    public static int selectedLanguage()        { return selectedLanguage; }
    public static void selectedLanguage(int i)  { selectedLanguage = i; }

    private List<Language> languages()  {
        if (languages.isEmpty()) {
            loadLanguageFile();
            selectedLanguage(-1);
            selectLanguage(DEFAULT_LANGUAGE);
        }
        return languages;
    }
    public List<String> languageCodes() {
        List<String> names = new ArrayList<>();
        for (Language lang: languages)
            names.add(lang.directory);
        return names;
    }
    public List<String> languageNames() {
        List<String> names = new ArrayList<>();
        for (Language lang: languages)
            names.add(lang.name);
        return names;
    }
    public static int languageNumber(String dir) {
        for (int i=0;i<languages.size();i++) {
            Language lang = languages.get(i);
            if (lang.directory.equalsIgnoreCase(dir))
                return i;
        }
        return DEFAULT_LANGUAGE;
    }
    public static void selectLanguage(String dir) {
        for (int i=0;i<languages.size();i++) {
            Language lang = languages.get(i);
            if (lang.directory.equalsIgnoreCase(dir)) {
                current().selectLanguage(i);
                return;
            }
        }
    }
    public static String selectedLanguageDir() { return languages.get(selectedLanguage).directory; }
    public static String languageDir(int i)    { return languages.get(i).directory; }
    public String selectedLanguageName() {
        return language(selectedLanguage());
    }
    public String defaultLanguageFullPath()   { return baseDir+languages().get(DEFAULT_LANGUAGE).directory; }
    public String selectedLanguageFullPath()  { return baseDir+languages().get(selectedLanguage()).directory; }

    public String language(int i)   { return languages().get(i).name; }
    public String langDir(int i)    { return languages().get(i).directory; }
    public String langSubdir(int i) { return languages().get(i).subdirectory;    }
    public String fontName(int i)   { return languages().get(i).font; }
    public Locale locale(int i)     { return languages().get(i).locale; }
    public ComponentOrientation orientation(int i) { return languages().get(i).orientation; }
    public void cycleLanguage(boolean up) {
        int i = selectedLanguage();
        i += (up?1:-1);
        if (i < 0)
            i = languages().size() - 1;
        else if (i >= languages().size())
            i = 0;

        selectLanguage(i);
        UserPreferences.save();
    }
    public void selectLanguage(int i) {
        if (selectedLanguage() == i)
            return;

        //Language defLang = languages().get(DEFAULT_LANGUAGE);
        Language newLang = languages().get(i);

        // load fonts for selected lanage
        FontManager.current().loadFonts(baseDir, newLang.directory);

        // reset dialogue maps in label managers
        labels().resetDialogue();
        RaceFactory.current().resetRaceLangFiles();

        // reload default labels, since that is assured of completeness
        //String currDir = baseDir+defLang.directory+"/";
        //labels().loadLabelFile(currDir);
        //labels().loadDialogueFile(currDir);
        //labels().loadTechsFile(currDir);
        //RaceFactory.current().loadRaceLangFiles(defLang.directory);

        // now overwrite those with labels for the selected language
        selectedLanguage(i);

        //if (i != DEFAULT_LANGUAGE) {
            String currDir = baseDir+newLang.directory+"/";
            labels().load(currDir);
            RaceFactory.current().loadRaceLangFiles(newLang.directory);
        //}
    } 
    public String defaultLangDir()    { return langDir(DEFAULT_LANGUAGE); }
    public String currentLanguage()   { return language(selectedLanguage()); }
    public String currentLangDir()    { return langDir(selectedLanguage()); }
    public String currentLangSubdir() { return langSubdir(selectedLanguage()); }
    public String currentFont()       { return fontName(selectedLanguage()); }
    public Locale currentLocale()     { return locale(selectedLanguage()); }
    public ComponentOrientation currentOrientation()  { return orientation(selectedLanguage()); }

    protected void loadLanguageFile() {
        BufferedReader in = reader(baseDir+languageFile);
        if (in == null) {
            err("LanguageManager.loadLanguageFile() - can't find language file! ", baseDir, languageFile);
            return;
        }
        try {
            String input;
            while ((input = in.readLine()) != null)
                loadLanguageLine(input);
            in.close();
        }
        catch (IOException e) {
            err("LanguageManager.loadLanguageFile() -- IOException: ", e.toString());
        }
    }
    protected void loadLanguageLine(String input) {
        if (isComment(input))
            return;

        List<String> strings = substrings(input, ',',5);
        String dirString = strings.get(0);
        String subdirString = strings.get(1);
        String nameString = strings.get(2);
        String orientString = strings.get(3);
        String fontString = strings.get(4);
        languages.add(new Language(dirString, subdirString, nameString, orientString, fontString));
        // load fonts for selected lanage
        FontManager.current().loadLanguageFonts(baseDir, dirString);
    }
    class Language {
        String directory;
        String subdirectory;
        Locale locale;
        ComponentOrientation orientation;
        String name;
        String font;
        public Language(String dir, String sub, String n, String o, String f) {
            directory = dir;
            subdirectory = sub;
            name = n;
            font = f;
            locale = new Locale(dir);
            if (o.trim().equalsIgnoreCase("RT"))
                orientation = ComponentOrientation.RIGHT_TO_LEFT;
            else
                orientation = ComponentOrientation.LEFT_TO_RIGHT;
        }
    }
}
