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
package rotp.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import rotp.Rotp;
import rotp.util.LanguageManager;
import rotp.util.sound.SoundManager;

public class UserPreferences {
    private static final String WINDOW_MODE = "GAME_SETTINGS_WINDOWED";
    private static final String BORDERLESS_MODE = "GAME_SETTINGS_BORDERLESS";
    private static final String FULLSCREEN_MODE = "GAME_SETTINGS_FULLSCREEN";
    private static final String GRAPHICS_LOW = "GAME_SETTINGS_GRAPHICS_LOW";
    private static final String GRAPHICS_MEDIUM = "GAME_SETTINGS_GRAPHICS_MED";
    private static final String GRAPHICS_HIGH = "GAME_SETTINGS_GRAPHICS_HIGH";
    private static final String TEXTURES_YES = "GAME_SETTINGS_TEXTURES_YES";
    private static final String TEXTURES_NO = "GAME_SETTINGS_TEXTURES_NO";
    private static final String AUTOCOLONIZE_YES = "GAME_SETTINGS_AUTOCOLONIZE_YES";
    private static final String AUTOCOLONIZE_NO = "GAME_SETTINGS_AUTOCOLONIZE_NO";
    
    private static final String PREFERENCES_FILE = "Remnants.cfg";
    private static final String keyFormat = "%-20s: ";
    private static boolean showMemory = false;
    private static boolean playMusic = true;
    private static boolean playSounds = true;
    private static boolean displayYear = true;
    private static boolean textures = true;
    private static boolean autoColonize = false;
    private static String displayMode = WINDOW_MODE;
    private static String graphicsMode = GRAPHICS_HIGH;
    private static float uiTexturePct = 0.20f;
    private static int screenSizePct = 93;
    private static final HashMap<String, String> raceNames = new HashMap<>();

    public static boolean showMemory()      { return showMemory; }
    public static void toggleMemory()       { showMemory = !showMemory; save(); }
    public static boolean fullScreen()      { return displayMode.equals(FULLSCREEN_MODE); }
    public static boolean windowed()        { return displayMode.equals(WINDOW_MODE); }
    public static boolean borderless()      { return displayMode.equals(BORDERLESS_MODE); }
    public static String displayMode()      { return displayMode; }
    public static void toggleDisplayMode()   { 
        switch(displayMode) {
            case WINDOW_MODE:     displayMode = BORDERLESS_MODE; break;
            case BORDERLESS_MODE: displayMode = FULLSCREEN_MODE; break;
            case FULLSCREEN_MODE: displayMode = WINDOW_MODE; break;
            default:              displayMode = WINDOW_MODE; break;
        }
        save();
    }
    public static String graphicsMode()     { return graphicsMode; }
    public static void toggleGraphicsMode()   { 
        switch(graphicsMode) {
            case GRAPHICS_HIGH:   graphicsMode = GRAPHICS_MEDIUM; break;
            case GRAPHICS_MEDIUM: graphicsMode = GRAPHICS_LOW; break;
            case GRAPHICS_LOW:    graphicsMode = GRAPHICS_HIGH; break;
            default :             graphicsMode = GRAPHICS_HIGH; break;
        }
        save();
    }
    public static String texturesMode()     { return textures ? TEXTURES_YES : TEXTURES_NO; }
    public static void toggleTextures()     { textures = !textures; save();  }
    public static boolean textures()        { return textures; }
    
    public static String autoColonizeMode()     { return autoColonize ? AUTOCOLONIZE_YES : AUTOCOLONIZE_NO; }
    public static void toggleAutoColonize()     { autoColonize = !autoColonize; save();  }
    public static boolean autoColonize()        { return autoColonize; }
    
    public static boolean playAnimations()  { return !graphicsMode.equals(GRAPHICS_LOW); }
    public static boolean antialiasing()    { return graphicsMode.equals(GRAPHICS_MEDIUM); }
    public static boolean playSounds()      { return playSounds; }
    public static void toggleSounds()       { playSounds = !playSounds;	save(); }
    public static boolean playMusic()       { return playMusic; }
    public static void toggleMusic()        { playMusic = !playMusic; save();  }
    public static int screenSizePct()       { return screenSizePct; }
    public static void screenSizePct(int i) { setScreenSizePct(i); }

    public static void toggleYearDisplay()    { displayYear = !displayYear; save(); }
    public static boolean displayYear()       { return displayYear; }
    public static void uiTexturePct(int i)    { uiTexturePct = i / 100.0f; }
    public static float uiTexturePct()        { return uiTexturePct; }

    
    public static void loadAndSave() {
        load();
        save();
    }
    public static void load() {
        String path = Rotp.jarPath();
        File configFile = new File(path, PREFERENCES_FILE);
		// modnar: change to InputStreamReader, force UTF-8
		try ( BufferedReader in = new BufferedReader( new InputStreamReader( new FileInputStream(configFile), "UTF-8"));) {
            String input;
            if (in != null) {
                while ((input = in.readLine()) != null)
                    loadPreferenceLine(input.trim());
            }
        }
        catch (FileNotFoundException e) {
            System.err.println(path+PREFERENCES_FILE+" not found.");
        }
        catch (IOException e) {
            System.err.println("UserPreferences.load -- IOException: "+ e.toString());
        }
    }
    public static void save() {
        String path = Rotp.jarPath();
        List<String> raceKeys = new ArrayList<>(raceNames.keySet());
        Collections.sort(raceKeys);
        try (FileOutputStream fout = new FileOutputStream(new File(path, PREFERENCES_FILE));
            // modnar: change to OutputStreamWriter, force UTF-8
            PrintWriter out = new PrintWriter(new OutputStreamWriter(fout, "UTF-8")); ) {
            out.println(keyFormat("DISPLAY_MODE")+displayModeToSettingName(displayMode));
            out.println(keyFormat("GRAPHICS")+graphicsModeToSettingName(graphicsMode));
            out.println(keyFormat("MUSIC")+ yesOrNo(playMusic));
            out.println(keyFormat("SOUNDS")+ yesOrNo(playSounds));
            out.println(keyFormat("MUSIC_VOLUME")+ SoundManager.musicLevel());
            out.println(keyFormat("SOUND_VOLUME")+ SoundManager.soundLevel());
            out.println(keyFormat("SHOW_MEMORY")+ yesOrNo(showMemory));
            out.println(keyFormat("DISPLAY_YEAR")+ yesOrNo(displayYear));
            out.println(keyFormat("SCREEN_SIZE_PCT")+ screenSizePct());
            out.println(keyFormat("UI_TEXTURES")+ yesOrNo(textures));
            out.println(keyFormat("UI_TEXTURE_LEVEL")+(int) (uiTexturePct()*100));
            out.println(keyFormat("LANGUAGE")+ languageDir());
            for (String raceKey: raceKeys) 
              out.println(keyFormat(raceKey)+raceNames.get(raceKey));
        }
        catch (IOException e) {
            System.err.println("UserPreferences.save -- IOException: "+ e.toString());
        }
    }
    private static String keyFormat(String s)  { return String.format(keyFormat, s); }
    
    private static void loadPreferenceLine(String line) {
        if (line.isEmpty())
            return;

        String[] args = line.split(":");
        if (args.length < 2)
            return;

        String key = args[0].toUpperCase().trim();
        String val = args[1].trim();
        if (key.isEmpty() || val.isEmpty())
                return;

        if (Rotp.logging)
            System.out.println("Key:"+key+"  value:"+val);
        switch(key) {
            case "DISPLAY_MODE":  displayMode = displayModeFromSettingName(val); return;
            case "GRAPHICS":     graphicsMode = graphicsModeFromSettingName(val); return;
            case "MUSIC":        playMusic = yesOrNo(val); return;
            case "SOUNDS":       playSounds = yesOrNo(val); return;
            case "MUSIC_VOLUME": SoundManager.musicLevel(Integer.valueOf(val)); return;
            case "SOUND_VOLUME": SoundManager.soundLevel(Integer.valueOf(val)); return;
            case "SHOW_MEMORY":  showMemory = yesOrNo(val); return;
            case "DISPLAY_YEAR": displayYear = yesOrNo(val); return;
            case "SCREEN_SIZE_PCT": screenSizePct(Integer.valueOf(val)); return;
            case "UI_TEXTURES":  textures = yesOrNo(val); return;
            case "UI_TEXTURE_LEVEL": uiTexturePct(Integer.valueOf(val)); return;
            case "LANGUAGE":     selectLanguage(val); return;
            default:
                raceNames.put(key, val); break;
        }
    }
    private static String yesOrNo(boolean b) {
        return b ? "YES" : "NO";
    }
    private static boolean yesOrNo(String s) {
        return s.equalsIgnoreCase("YES");
    }
    private static void selectLanguage(String s) {
        LanguageManager.selectLanguage(s);
    }
    private static String languageDir() {
        return LanguageManager.selectedLanguageDir();
    }
    private static void setScreenSizePct(int i) {
        screenSizePct = Math.max(50,Math.min(i,100));
    }
    public static boolean shrinkFrame() {
        int oldSize = screenSizePct;
        setScreenSizePct(screenSizePct-5);
        return oldSize != screenSizePct;
    }
    public static boolean expandFrame() {
        int oldSize = screenSizePct;
        setScreenSizePct(screenSizePct+5);
        return oldSize != screenSizePct;
    }
    public static String raceNames(String id, String defaultNames) {
        String idUpper = id.toUpperCase();
        if (raceNames.containsKey(idUpper))
            return raceNames.get(idUpper);
        
        raceNames.put(idUpper, defaultNames);
        return defaultNames;
    }
    public static String displayModeToSettingName(String s) {
        switch(s) {
            case WINDOW_MODE:     return "Windowed";
            case BORDERLESS_MODE: return "Borderless";
            case FULLSCREEN_MODE: return "Fullscreen";
        }
        return "Windowed";
    }
    public static String displayModeFromSettingName(String s) {
        switch(s) {
            case "Windowed":   return WINDOW_MODE;
            case "Borderless": return BORDERLESS_MODE;
            case "Fullscreen": return FULLSCREEN_MODE;
        }
        return WINDOW_MODE;
    }
    public static String graphicsModeToSettingName(String s) {
        switch(s) {
            case GRAPHICS_LOW:    return "Low";
            case GRAPHICS_MEDIUM: return "Medium";
            case GRAPHICS_HIGH:   return "High";
        }
        return "High";
    }
    public static String graphicsModeFromSettingName(String s) {
        switch(s) {
            case "Low":    return GRAPHICS_LOW;
            case "Medium": return GRAPHICS_MEDIUM;
            case "High":   return GRAPHICS_HIGH;
        }
        return GRAPHICS_HIGH;
    }
}
