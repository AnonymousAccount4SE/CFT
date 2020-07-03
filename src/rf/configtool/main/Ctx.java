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

package rf.configtool.main;

import java.util.*;

import rf.configtool.main.runtime.Value;
import rf.configtool.main.runtime.ValueList;

public class Ctx {
    
    private Ctx parent;
    private String loopVariableName;
    private Value loopVariableValue;
    
    private FunctionState functionState;

    private OutData outData;
    private OutText outText;
    
    // These two needed to support inline code-blocks ("macros")
    
    private boolean programContainsLooping = false;
    private boolean programContainsLoopingInfoStopsHere=false;

    
    private Stack<Value> stack=new Stack<Value>();
    private ObjGlobal objGlobal;
    
    private boolean abortIterationFlag; // "next"
    private boolean breakLoopFlag;  // "break"
    

    public Ctx(ObjGlobal objGlobal, FunctionState functionState) {
        this(null, new OutData(), new OutText(), objGlobal, functionState);
    }
    
    private Ctx (Ctx parent, OutData outData,  OutText outText, ObjGlobal objGlobal, FunctionState functionState) {
        if (functionState==null) functionState=new FunctionState();
        
        this.parent=parent;
        this.outData=outData;
        this.outText=outText;
        this.objGlobal=objGlobal;
        this.functionState=functionState;
        
    }
    
    public Stdio getStdio() {
        return objGlobal.getStdio();
    }

    public Ctx sub() {
        return new Ctx(this,outData,outText,objGlobal,functionState);
    }
              
    /**
     * Code block is a macro that is invoked immediately, and that has access up the
     * Ctx stack, but with separate OutData object, to avoid mixing result data from environment.
     */
    public Ctx subContextForCodeBlock () {
        Ctx ctx = new Ctx(this,new OutData(),outText,objGlobal,functionState);
        ctx.programContainsLoopingInfoStopsHere=true;
        return ctx;
    }
    
    /**
     * Called from StmtIterate and StmtLoop. Could have used the occurrence of loop variables (which are
     * stored in the Ctx instances) if it wasn't for the "loop" statement, which has no loop variable.
     */
    public void setProgramContainsLooping() {
        // Previously this was implemented as part of the OutData object which made it in effect
        // global.
        Ctx ctx=this;
        for(;;) {
            ctx.programContainsLooping=true;
            if (ctx.programContainsLoopingInfoStopsHere) break;
            if (ctx.parent == null) break;
            ctx=ctx.parent;
        }
        
    }
    

              
    public void outln (String s) {
        objGlobal.outln(s);
    }
    
    public void outln () {
        objGlobal.outln();
    }
    
    public Value getResult() {
        // if program contains looping, then always return data from out(), even
        // if empty
        if (programContainsLooping) {
            return new ValueList(outData.getOutData());
        }
        
        // otherwise return top element on stack
        if (!stack.isEmpty()) {
            return stack.pop();
        }
        // no return value
        return null;
    }
    

    
    /**
     * Aborts current Ctx
     */
    public void setAbortIterationFlag() {
        abortIterationFlag=true;
    }
    
    public boolean hasAbortIterationFlag() {
        return abortIterationFlag;
    }
    
    public void setBreakLoopFlag() {
        breakLoopFlag=true;
    }
    
    public boolean hasBreakLoopFlag() {
        return breakLoopFlag;
    }
    
    public void setLoopVariable (String name, Value value) {
        this.loopVariableName=name;
        this.loopVariableValue=value;
    }
    
    public Value getVariable (String name) {
        Value v=getLoopVariable(name);
        if (v != null) return v;
        
        return functionState.get(name);
    }
    
    private Value getLoopVariable (String name) {
        // first we traverse localVariables up all Ctx, since these will
        // contain loop variables, then we check with FunctionState, where
        // all assigned variables ("=x") are stored in shared scope for the
        // function. 
        
        // This also means one can not redefine loop variables.
        
        if (loopVariableName != null && name.equals(loopVariableName)) {
            return loopVariableValue;
        }
        if (parent != null) return parent.getLoopVariable(name);
        return null;
    }
    
    public Value pop() {
        if (stack.isEmpty()) return null;
        return stack.pop();
    }
    
    public void push (Value v) {
        stack.push(v);
    }

    public OutData getOutData() {
        return outData;
    }
        
    public OutText getOutText() {
        return outText;
    }
    
    public ObjGlobal getObjGlobal() {
        return objGlobal;
    }
    
    public FunctionState getFunctionState() {
        return functionState;
    }
}
