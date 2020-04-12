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

import java.awt.event.KeyEvent;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

public class BaseTextField extends JTextField {
    private static final long serialVersionUID = 1L;
    BasePanel tabNotifier;
    private int limit = 30;
    public BaseTextField(BasePanel c) {
        super("");
        tabNotifier = c;
    }
    public BaseTextField() {
        super();
    }
    public BaseTextField(int i) {
        super(i);
    }
    public BaseTextField(String s) {
        super(s);
    }
    public void setLimit(int i )   { limit = i; }
    @Override
    protected void processKeyEvent(KeyEvent k) {
        if ((k.getKeyCode() == KeyEvent.VK_TAB) && (tabNotifier != null)) {
            System.out.println("Tab for: "+this.getSelectedText());
            tabNotifier.keyPressed(k);
        }
        else
            super.processKeyEvent(k);
    }
    @Override
    protected Document createDefaultModel() {
        return new LimitDocument();
    }
    private class LimitDocument extends PlainDocument {
        private static final long serialVersionUID = 1L;
        @Override
        public void insertString( int offset, String  str, AttributeSet attr ) throws BadLocationException {
            if (str == null) return;
            if ((getLength() + str.length()) <= limit) 
                    super.insertString(offset, str, attr);
        }
    }
}
