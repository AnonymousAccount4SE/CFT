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

package rf.configtool.data;

import java.util.*;

import rf.configtool.main.Ctx;
import rf.configtool.main.runtime.Value;
import rf.configtool.parser.TokenStream;

public class StmtStdin extends Stmt {

    private List<Expr> exprList;
    
    public StmtStdin (TokenStream ts) throws Exception {
        super(ts);
        ts.matchStr("stdin","expected 'stdin'");
        
        ts.matchStr("(", "expected '(' following stdin");
        if (!ts.matchStr(")")) {
            exprList=new ArrayList<Expr>();
            
            exprList.add(new Expr(ts));
            while (ts.matchStr(",")) {
                exprList.add(new Expr(ts));
            }
            ts.matchStr(")", "expected ')' closing stdin(...)");
        }
    }

    public void execute (Ctx ctx) throws Exception {
        if (exprList != null) {
            for (Expr expr:exprList) {
                String s=expr.resolve(ctx).getValAsString();
                ctx.getStdio().addBufferedInputLine(s);
            }
        } else {
            ctx.outln("Clearing buffered input lines");
            ctx.getStdio().clearBufferedInputLines();
        }
    }

}
