/*
CFT - an interactive programmable shell for automation 
Copyright (C) 2020 Roar Foshaug

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, version 3 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>
*/

package rf.configtool.parser;

public class CharSource  {
    private String s;
    private int pos=0;

    public CharSource (String s) { this.s=s; }
    public int getPos() {
        return pos;
    }
    public void setPos (int pos) {
    	this.pos=pos;
    }
    public char getChar() {
        if (eol()) throw new RuntimeException("end of line");
        return s.charAt(pos++);
    }
    public void ungetChar() {
        pos--;
    }
    public void ungetChar(int count) {
        pos-=count;
    }
    public boolean eol () {
        return (pos >= s.length());
    }
}
