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

public class NoticeMessage {
    public static String title = "";
    public static String step = "";
    public static int currentStep = 1;
    public static int maxSteps = 1;
    public static boolean dataChanged = false;
    public static long startTime = 0;
    public static long retrievedTime = 0;

    public static String title()  { return title; }
    public static String step() {
        retrievedTime = System.currentTimeMillis();
        if (maxSteps == 1)
            return step;
        else {
            int secs = seconds();
            if (secs > 0)
                return Integer.toString(currentStep)+ " of "+ maxSteps + " "+step+ " - "+Integer.toString(secs)+"s";
            else
                return Integer.toString(currentStep)+ " of "+ maxSteps + " "+step;
        }
    }
    public static int seconds() {
        return (int) ((System.currentTimeMillis() - startTime) / 1000);
    }
    public static boolean redisplay() { return dataChanged || ((System.currentTimeMillis()-retrievedTime) > 1000); }
    public static void setStatus(String s1, String s2, int curr, int max) {
        dataChanged = !s1.equals(title) || !s2.equals(step) || (curr != currentStep) || (max != maxSteps);
        title = s1;
        step = s2;
        currentStep = curr;
        maxSteps = max;
        if (dataChanged)
            startTime = System.currentTimeMillis();
        repaint();
    }
    public static void setStatus(String s1 ) {
        dataChanged = !s1.equals(title);
        title = s1;
        step = "";
        currentStep = 1;
        maxSteps = 1;
        if (dataChanged)
            startTime = System.currentTimeMillis();
        repaint();
    }
    public static void setStatus(String s1, String s2) {
        dataChanged = !s1.equals(title) || !s2.equals(step);
        title = s1;
        step = s2;
        currentStep = 1;
        maxSteps = 1;
        if (dataChanged)
            startTime = System.currentTimeMillis();
        repaint();
    }
    public static void resetSubstatus(String s1 ) {
        dataChanged = !s1.equals(step);
        step = s1;
        currentStep = 1;
        maxSteps = 1;
        if (dataChanged)
            startTime = System.currentTimeMillis();
        repaint();
    }
    public static void setSubstatus(String s1 ) {
        dataChanged = !s1.equals(step);
        step = s1;
        if (dataChanged)
            startTime = System.currentTimeMillis();
        repaint();
    }
    public static void setSubstatus(String s1, int curr, int max) {
        dataChanged = !s1.equals(step) || (curr != currentStep) || (max != maxSteps);
        step = s1;
        currentStep = curr;
        maxSteps = max;
        if (dataChanged)
            startTime = System.currentTimeMillis();
        repaint();
    }
    public static void setStep(int curr) {
        dataChanged = (curr != currentStep);
        currentStep = curr;
        if (dataChanged)
            startTime = System.currentTimeMillis();
        repaint();
    }
    public static void repaint() {
        if (redisplay()) 
            RotPUI.instance().repaint();
    }
}
