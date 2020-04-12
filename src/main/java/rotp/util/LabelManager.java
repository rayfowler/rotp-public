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

public class LabelManager implements Base {
    static LabelManager instance = new LabelManager();
    public static LabelManager current()  { return instance; }

    private String labelFile = "labels.txt";
    private String dialogueFile = "dialogue.txt";
    private final String techsFile = "techs.txt";
    private String introFile = "intro.txt";
    private final HashMap<String,byte[]> labelMap = new HashMap<>();
    private final HashMap<String,List<String>> enDialogueMap = new HashMap<>();
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
        if (value == null) 
            value = enDialogueMap.get(key);
        return (value == null) || value.isEmpty() ? key : random(value);
    }
    public void load(String dir) {
    	loadIntroFile(dir);
    	loadLabelFile(dir);
    	loadTechsFile(dir);
    	loadDialogueFile(dir);
    }
    public void loadIntroFile(String dir) {
        log("loading Intro: ", dir, introFile);
        BufferedReader in = reader(dir+introFile);
        if (in == null) {
            err("can't find intro file! ", dir, introFile);
            return;
        }

        // intro file found... reset list of intro lines
        introLines.clear();
        try {
            String input;
            while ((input = in.readLine()) != null) {
            	if (!isComment(input))
            		introLines.add(input);
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
    }
    public void loadLabelFile(String dir) {
        log("loading Labels: ", dir, labelFile);
        BufferedReader in = reader(dir+labelFile);
        if (in == null) {
            err("can't find label file! ", dir, labelFile);
            return;
        }

        try {
            String input;
            while ((input = in.readLine()) != null)
                loadLabelLine(input);
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
    }
    public void resetDialogue() {
        enDialogueMap.clear();
        dialogueMap.clear();
    }
    public void loadDialogueFile(String dir) {
        log("loading Dialogue: ", dir, dialogueFile);
        HashMap thisMap = dir.equals("lang/en/") ? enDialogueMap : dialogueMap;
        
        BufferedReader in = reader(dir+dialogueFile);
        if (in == null) {
            err("can't find dialogue file! ", dir, dialogueFile);
            return;
        }

        try {
            String input;
            while ((input = in.readLine()) != null)
                loadDialogueLine(input, thisMap);
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
    }
    public void loadTechsFile(String dir) {
        log("loading Techs: ", dir, techsFile);
        
        BufferedReader in = reader(dir+techsFile);
        if (in == null) {
            err("can't find techs file! ", dir, techsFile);
            return;
        }

        try {
            String input;
            while ((input = in.readLine()) != null)
                loadLabelLine(input);
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
    }
    private void loadLabelLine(String input) {
    	if (isComment(input))
    		return;
 
        List<String> vals = substrings(input, '|');
        if (vals.size() < 2)
        	return;
        
        try {
        labelMap.put(vals.get(0), vals.get(1).getBytes("UTF-8"));
        }
        catch(UnsupportedEncodingException e) { }
    }
    private void loadDialogueLine(String input, HashMap<String,List<String>> map) {
    	if (isComment(input))
    		return;
 
        List<String> vals = substrings(input, '|');
        if (vals.size() < 2)
        	return;
        
        String key = vals.get(0);
        if (!map.containsKey(key))
        	map.put(key, new ArrayList<>());
        	
       map.get(key).add(vals.get(1));
    }
}
