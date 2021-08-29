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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import rotp.Rotp;

public class LabelManager implements Base {
    static LabelManager instance = new LabelManager();
    public static LabelManager current()  { return instance; }

    private String labelFile = "labels.txt";
    private String dialogueFile = "dialogue.txt";
    private final String techsFile = "techs.txt";
    private String introFile = "intro.txt";
    private final HashMap<String,byte[]> labelMap = new HashMap<>();
    private final HashMap<String,List<String>> dialogueMap = new HashMap<>();
    private final List<String> introLines = new ArrayList<>();

    public boolean hasLabel(String key)    { return labelMap.containsKey(key); }
    public boolean hasDialogue(String key) { return dialogueMap.containsKey(key); }
    public boolean hasIntroduction()       { return !introLines.isEmpty(); }
    public List<String> introduction()     { return introLines; }
    
    public void dialogueFile(String s)    { dialogueFile = s; }
    public void labelFile(String s)       { labelFile = s; }
    public void introFile(String s)       { introFile = s; }
    
    public String label(String key) {
        byte[] value = labelMap.get(key);
        try {
        return (value == null) ? key : new String(value, "UTF-8");
        }
        catch(UnsupportedEncodingException e) { return key; }
    }
    public String realLabel(String key) {
        byte[] value = labelMap.get(key);
        try {
        return (value == null) ? null : new String(value, "UTF-8");
        }
        catch(UnsupportedEncodingException e) { return null; }
    }
    public String dialogue(String key) {
        List<String> value = dialogueMap.get(key);
        //if (value == null) 
        //    value = enDialogueMap.get(key);
        return (value == null) || value.isEmpty() ? key : random(value);
    }
    public void load(String dir) {
    	loadLabelFile(dir);
    	loadTechsFile(dir);
    	loadDialogueFile(dir);
    }
    public void loadIntroFile(String dir) {
        log("loading Intro: ", dir, introFile);
        String filename = dir+introFile;
        BufferedReader in = reader(filename);
        if (in == null) {
            err("can't find intro file! ", dir, introFile);
            return;
        }

        // intro file found... reset list of intro lines
        introLines.clear();
        int wc = 0;
        try {
            String input;
            while ((input = in.readLine()) != null) {
            	if (!isComment(input)) {
                    introLines.add(input);
                    if (Rotp.countWords)
                        wc += substrings(input, ' ').size();
                }
            }
        }
        catch (IOException e) { 
        	err("LabelManager.loadIntroFile -- IOException: " + e); 
        }
        finally {
        	try {
                        in.close();
			} catch (IOException e) {
	        	err("LabelManager.loadIntroFile2 -- IOException: " + e); 
			}
        }
        if (Rotp.countWords)
            log("WORDS - "+filename+": "+wc);
            
    }
    public void loadLabelFile(String dir) {
        log("loading Labels: ", dir, labelFile);
        String filename = dir+labelFile;
        BufferedReader in = reader(filename);
        if (in == null) {
            err("can't find label file! ", dir, labelFile);
            return;
        }

        int wc = 0;
        try {
            String input;
            while ((input = in.readLine()) != null)
                wc += loadLabelLine(input);
        }
        catch (IOException e) { 
        	err("LabelManager.loadLabelFile -- IOException: ", e.toString()); 
        }
        finally {
        	try {
                        in.close();
                    } catch (IOException e) {
                        err("LabelManager.loadLabelFile2 -- IOException: " + e); 
                    }
        }
        if (Rotp.countWords)
            log("WORDS - "+filename+": "+wc);
    }
    public void resetDialogue() {
        dialogueMap.clear();
    }
    public void loadDialogueFile(String dir) {
        log("loading Dialogue: ", dir, dialogueFile);
        
        String filename = dir+dialogueFile;
        BufferedReader in = reader(filename);
        if (in == null) {
            err("can't find dialogue file! ", dir, dialogueFile);
            return;
        }

        int wc = 0;
        try {
            String input;
            while ((input = in.readLine()) != null)
                wc += loadDialogueLine(input, dialogueMap);
        }
        catch (IOException e) { 
        	err("LabelManager.loadDialogueFile -- IOException: ", e.toString()); 
        }
        finally {
            try {
                    in.close();
                } catch (IOException e) {
                    err("LabelManager.loadDialogueFile2 -- IOException: " + e); 
                }
        }
        if (Rotp.countWords)
            log("WORDS - "+filename+": "+wc);
    }
    public void loadTechsFile(String dir) {
        log("loading Techs: ", dir, techsFile);
        
        String filename = dir+techsFile;
        BufferedReader in = reader(filename);
        if (in == null) {
            err("can't find techs file! ", dir, techsFile);
            return;
        }

        int wc = 0;
        try {
            String input;
            while ((input = in.readLine()) != null)
                wc += loadLabelLine(input);
        }
        catch (IOException e) { 
            err("LabelManager.loadTechsFile -- IOException: ", e.toString()); 
        }
        finally {
            try {
                in.close();
            } catch (IOException e) {
                err("LabelManager.loadTechsFile2 -- IOException: " + e); 
            }
        }
        if (Rotp.countWords)
            log("WORDS - "+filename+": "+wc);
    }
    private int loadLabelLine(String input) {
    	if (isComment(input))
            return 0;
 
        List<String> vals = substrings(input, '|');
        if (vals.size() < 2)
            return 0;
        
        int wc = 0;
        try {      
            labelMap.put(vals.get(0), vals.get(1).getBytes("UTF-8"));
            if (Rotp.countWords)
                wc = substrings(vals.get(1), ' ').size();
        }
        catch(UnsupportedEncodingException e) { }
        return wc;
    }
    private int loadDialogueLine(String input, HashMap<String,List<String>> map) {
    	if (isComment(input))
            return 0;
 
        List<String> vals = substrings(input, '|');
        if (vals.size() < 2)
            return 0;
        
        String key = vals.get(0);
        if (!map.containsKey(key))
            map.put(key, new ArrayList<>());
        	
        map.get(key).add(vals.get(1));
        
        if (Rotp.countWords)
            return substrings(vals.get(1), ' ').size();
        else
            return 0;
    }
}
