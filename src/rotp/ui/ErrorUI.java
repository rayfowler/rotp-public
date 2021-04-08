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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import rotp.Rotp;

public class ErrorUI extends BasePanel implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    private Throwable exception;
    public ErrorUI() {
        init();
    }
    private void init() {
        setBackground(Color.black);
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    public void init(Throwable e) {
        exception = e;
        e.printStackTrace();
    }
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        int w = getWidth();
        int h = getHeight();

        g.setColor(Color.lightGray);

        g.setFont(narrowFont(40));
        String title = "An Error has occurred  :(";
        int sw0 = g.getFontMetrics().stringWidth(title);
        drawString(g,title, (w-sw0)/2, BasePanel.s50);

        int x0 = w/10;
        int w0 = w*4/5;
        int y0 = BasePanel.s80;
        g.setFont(narrowFont(30));
        String desc = "If you would like to help fix this problem, please send a screen shot of this UI plus the 'recent.rotp' save game file to Ray Fowler, or bring it to his attention on the ROTP subreddit.";
        List<String> lines = wrappedLines(g, desc, w0);
        int lineCount = 0;
        for (String line : lines) {
            y0 += BasePanel.s35;
            if (lineCount < 10)
                drawString(g,line, x0, y0);
            lineCount++;
        }

        g.setFont(narrowFont(24));
        y0 += BasePanel.s60;
        drawString(g,"Email: rayfowler@fastmail.com", x0, y0);
        y0 += BasePanel.s30;
        drawString(g,"Reddit: www.Reddit.com/r/rotp", x0, y0);


        g.setFont(narrowFont(24));
        y0 += BasePanel.s70;
        drawString(g,exception.toString(), x0, y0);
        for (StackTraceElement line : exception.getStackTrace()) {
            y0 += BasePanel.s27;
            drawString(g,line.toString(), x0, y0);
        }

        g.setFont(narrowFont(20));
        String ver = "Version:"+ Rotp.releaseId;
        int sw = g.getFontMetrics().stringWidth(ver);
        drawString(g,ver, getWidth()-sw-s20, getHeight()-s30);

        drawMemory(g);
        drawSkipText(g, true);
    }
    @Override
    public boolean displayMemory() {
        return true;
    }
    private void advanceMode() {
        RotPUI.instance().selectGamePanel();
    }
    @Override
    public void mouseDragged(MouseEvent e) { }
    @Override
    public void mouseMoved(MouseEvent e) { }
    @Override
    public void mouseClicked(MouseEvent arg0) { }
    @Override
    public void mouseEntered(MouseEvent arg0) { }
    @Override
    public void mouseExited(MouseEvent arg0) { }
    @Override
    public void mousePressed(MouseEvent arg0) {}
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        advanceMode();
    }
    @Override
    public void keyPressed(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                advanceMode();
                return;
        }
    }
}
