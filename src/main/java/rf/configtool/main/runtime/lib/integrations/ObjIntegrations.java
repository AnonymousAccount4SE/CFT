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

package rf.configtool.main.runtime.lib.integrations;

import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.security.MessageDigest;
import java.util.*;

import rf.configtool.main.Ctx;
import rf.configtool.main.CtxCloseHook;
import rf.configtool.main.SoftErrorException;
import rf.configtool.main.OutText;
import rf.configtool.main.PropsFile;
import rf.configtool.main.Version;
import rf.configtool.main.runtime.ColList;
import rf.configtool.main.runtime.Function;
import rf.configtool.main.runtime.Obj;
import rf.configtool.main.runtime.Value;
import rf.configtool.main.runtime.ValueBinary;
import rf.configtool.main.runtime.ValueBoolean;
import rf.configtool.main.runtime.ValueFloat;
import rf.configtool.main.runtime.ValueInt;
import rf.configtool.main.runtime.ValueList;
import rf.configtool.main.runtime.ValueBlock;
import rf.configtool.main.runtime.ValueNull;
import rf.configtool.main.runtime.ValueObj;
import rf.configtool.main.runtime.ValueString;
import rf.configtool.main.runtime.lib.db2.ObjDb2;
import rf.configtool.parsetree.Expr;
import io.sentry.Sentry;

public class ObjIntegrations extends Obj {

    public ObjIntegrations () {       
        this.add(new FunctionSentry());
        this.add(new FunctionMSSql());
    }
    
    private ObjIntegrations self() {
        return this;
    }
    
    @Override
    public boolean eq(Obj x) {
        return false;
    }

    
    @Override
    public String getTypeName() {
        return "Integrations";
    }
    
    @Override
    public ColList getContentDescription() {
        return ColList.list().regular("Integrations");
    }
   
    class FunctionSentry extends Function {
        public String getName() {
            return "Sentry";
        }
        public String getShortDesc() {
            return "Sentry() - create Sentry object for testing Sentry.io installs";
        }
        public Value callFunction (Ctx ctx, List<Value> params) throws Exception {
            if (params.size() != 0) throw new Exception("Expected no parameters");
            return new ValueObj(new ObjSentry());
        }
    } 
    
    class FunctionMSSql extends Function {
        public String getName() {
            return "MSSql";
        }
        public String getShortDesc() {
            return "MSSql() - create MSSql object for interfacing MS SQL Server database";
        }
        public Value callFunction (Ctx ctx, List<Value> params) throws Exception {
            if (params.size() != 0) throw new Exception("Expected no parameters");
            return new ValueObj(new ObjMSSql());
        }
    } 
    

}