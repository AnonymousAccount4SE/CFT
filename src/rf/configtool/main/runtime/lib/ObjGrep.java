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

package rf.configtool.main.runtime.lib;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import rf.configtool.main.Ctx;
import rf.configtool.main.OutText;
import rf.configtool.main.runtime.*;
import rf.configtool.util.TabUtil;


public class ObjGrep extends Obj {
    
    abstract class Match {
        private boolean mode; // match=true, reject=false
        public Match (boolean mode) {
            this.mode=mode;
        }
        public final boolean match (String line) {
            return isPositiveMatch(line)==mode;
        }
        public abstract boolean isPositiveMatch (String line);
    }
    
    class ReMatch extends Match {
        
        private ObjRegex regex;
        public ReMatch (boolean match, ObjRegex regex) {
            super(match);
            this.regex=regex;
        }
        public boolean isPositiveMatch (String line) {
            return regex.matchesPartial(line);
        }
    }
    
    class StrMatch extends Match {
        private List<String> values=new ArrayList<String>();
        
        public StrMatch (List<String> values) {
            super(true);
            this.values=values;
        }
        public StrMatch(boolean match, List<Value> params) {
            super(match);
            for (Value p:params) {
                if (p instanceof ValueList) {
                    List<Value> list=((ValueList) p).getVal();
                    for (Value x:list) values.add(x.getValAsString());
                } else {
                    values.add(p.getValAsString());
                }
            }
        }
        
        public boolean isPositiveMatch (String line) {
            for (String v:values) {
                boolean found=line.contains(v);
                if (found) {
                    return true;
                }
            }
            return false;
        }
    }
    
    public static final int DEFAULT_LIMIT = 1000;
    
    private static final int ModeDefault = 0;
    private static final int ModeCount = 1;
    private static final int ModeCheck = 2;
    
    
    private List<Match> matchList=new ArrayList<Match>();
    private int mode=ModeDefault;
    private int limit=DEFAULT_LIMIT; 
    private boolean limitKeepFirst=true;
        // if limit is N > 0, if true, keep first N lines, otherwise last N lines
    
    private Obj self() {
        return this;
    }
    
    public ObjGrep(List<String> matchParts) {
        if (matchParts.size() > 0) {
            matchList.add(new StrMatch(matchParts));
        }
        add(new FunctionMatch());
        add(new FunctionReject());
        add(new FunctionFile());
        add(new FunctionLines());
        add(new FunctionModeCheck());
        add(new FunctionModeCount());
        
        add(new FunctionMatchRegex());
        add(new FunctionRejectRegex());
        
        add(new FunctionLimitFirst());
        add(new FunctionLimitLast());
        
    }
    
    @Override
    public boolean eq(Obj x) {
        return x==this;
    }


    
    public String getTypeName() {
        return "Grep";
    }
    

    public ColList getContentDescription() {
        return ColList.list().regular("Grep");
    }

    class FunctionMatch extends Function {
        public String getName() {
            return "match";
        }
        public String getShortDesc() {
            return "match(...) - match lines which contain one of the values, including content of lists - return self";
        }
        public Value callFunction (Ctx ctx, List<Value> params) throws Exception {
            matchList.add(new StrMatch(true, params));
            return new ValueObj(self());
        }
    }
    
    class FunctionReject extends Function {
        public String getName() {
            return "reject";
        }
        public String getShortDesc() {
            return "reject(...) - reject lines which contain one of the values, including content of lists - returns self";
        }
        public Value callFunction (Ctx ctx, List<Value> params) throws Exception {
            matchList.add(new StrMatch(false, params));
            return new ValueObj(self());
        }
    }
    
    class FunctionFile extends Function {
        public String getName() {
            return "file";
        }
        public String getShortDesc() {
            return "file(File) - search file for lines which match the Grep object";
        }
        public Value callFunction (Ctx ctx, List<Value> params) throws Exception {
            if (params.size() != 1 || !(params.get(0) instanceof ValueObj)) {
                throw new Exception("Expected File parameter");
            }
            Obj o1=((ValueObj) (params.get(0))).getVal();
            if (!(o1 instanceof ObjFile)) {
                throw new Exception("Expected File parameter");
            }
            ObjFile f=(ObjFile) o1;
            
            List<Value> result=new ArrayList<Value>();
            BufferedReader br = new BufferedReader(
      			   new InputStreamReader(
      	                      new FileInputStream(f.getPath()), f.getEncoding()));

            int count=0;
            boolean reachedLimit = false;
            
            try {
                long lineNo=0;
                for (;;) {
                    String line=br.readLine();
                    if (line==null) break;
                    lineNo++;
                    
                    boolean keep=true;
                    for (Match m:matchList) {
                        if (!m.match(line)) {
                            keep=false;
                            break;
                        }
                    }
                    
                    if (keep) {
                        if (mode == ModeCheck) return new ValueBoolean(true);
                    
                        if (mode == ModeCount) {
                        	count++;
                        } else {
	                        String deTabbed=TabUtil.substituteTabs(line,4);
	                        result.add(new ValueObjFileLine(deTabbed, lineNo, f));
	                        
	                        if (limit > 0) {
	                        	if (limitKeepFirst) {
	                        		if (result.size()==limit) {
	                        			ctx.getObjGlobal().addSystemMessage("WARNING: grep limit " + limit + " reached " + f.getPath());
	                        			return new ValueList(result);
	                        		}
	                        	} else {
	                        		// keep tail
	                        		if (result.size() > limit) {
	                        			reachedLimit=true;
	                        			result.remove(0);
	                        		}
	                        	}
	                        }
	                    }
                    }
                        
                }
            } finally {
                br.close();
            }
            if (mode==ModeCount) {
            	return new ValueInt(count);
            }
            
            if (reachedLimit) {
    			ctx.getObjGlobal().addSystemMessage("WARNING: grep limit " + limit + " reached " + f.getPath());
            }
            return new ValueList(result);
        }
    }
    
    
    class FunctionLines extends Function {
        public String getName() {
            return "lines";
        }
        public String getShortDesc() {
            return "lines(list) - identify lines that match";
        }
        public Value callFunction (Ctx ctx, List<Value> params) throws Exception {
            if (params.size() != 1 || !(params.get(0) instanceof ValueList)) {
                throw new Exception("Expected list parameter");
            }
            
            List<Value> lines=((ValueList) params.get(0)).getVal();
            List<Value> result=new ArrayList<Value>();

            int count=0;
            boolean reachedLimit=false;
            
            for (Value lineObj:lines) {
                String line=lineObj.getValAsString();
                if (line==null) break;
                
                boolean keep=true;
                for (Match m:matchList) {
                    if (!m.match(line)) {
                        keep=false;
                        break;
                    }
                }
                
                if (keep) {
                    if (mode==ModeCheck) return new ValueBoolean(true);
                    if (mode == ModeCount) {
                    	count++;
                    } else {
	                    result.add(lineObj);
	                    
	                    if (limit > 0) {
		                	if (limitKeepFirst) {
		                		if (result.size()==limit) {
		                			ctx.getObjGlobal().addSystemMessage("WARNING: grep limit " + limit + " reached");
		                			return new ValueList(result);
		                		}
		                	} else {
		                		// keep last
		                		if (result.size() > limit) {
		                			reachedLimit=true;
		                			result.remove(0);
		                		}
		                	}
	                    }
                    }
                    
                }
            }
            if (mode==ModeCount) {
            	return new ValueInt(count);
            }
            if (reachedLimit) {
    			ctx.getObjGlobal().addSystemMessage("WARNING: grep limit " + limit + " reached");
            }
            return new ValueList(result);
        }
    }
    
    
    class FunctionModeCheck extends Function {
        public String getName() {
            return "modeCheck";
        }
        public String getShortDesc() {
            return "modeCheck() - changes output of file() and lines() to boolean - returns self";
        }
        public Value callFunction (Ctx ctx, List<Value> params) throws Exception {
            mode=ModeCheck;
            return new ValueObj(self());
        }
    }
    
    class FunctionModeCount extends Function {
        public String getName() {
            return "modeCount";
        }
        public String getShortDesc() {
            return "modeCount() - changes output of file() and lines() to int - returns self";
        }
        public Value callFunction (Ctx ctx, List<Value> params) throws Exception {
            mode=ModeCount;
            return new ValueObj(self());
        }
    }
    
    class FunctionMatchRegex extends Function {
        public String getName() {
            return "matchRegex";
        }
        public String getShortDesc() {
            return "matchRegex(Regex) - match lines which partially match regex";
        }
        public Value callFunction (Ctx ctx, List<Value> params) throws Exception {
            if (params.size() != 1) throw new Exception("Expected parameter regex");
            Obj obj=getObj("regex", params, 0);
            if (!(obj instanceof ObjRegex)) throw new Exception("Expected parameter regex");
            ObjRegex regex=(ObjRegex) obj;
            
            matchList.add(new ReMatch(true, regex));
            return new ValueObj(self());
        }
    }
    
    class FunctionRejectRegex extends Function {
        public String getName() {
            return "rejectRegex";
        }
        public String getShortDesc() {
            return "rejectRegex(Regex) - match lines which don't partially match regex";
        }
        public Value callFunction (Ctx ctx, List<Value> params) throws Exception {
            if (params.size() != 1) throw new Exception("Expected parameter regex");
            Obj obj=getObj("regex", params, 0);
            if (!(obj instanceof ObjRegex)) throw new Exception("Expected parameter regex");
            ObjRegex regex=(ObjRegex) obj;
            
            matchList.add(new ReMatch(false, regex));
            return new ValueObj(self());
        }
    }
    
    
    class FunctionLimitFirst extends Function {
        public String getName() {
            return "limitFirst";
        }
        public String getShortDesc() {
            return "limitFirst(count) - return first N matches only";
        }
        public Value callFunction (Ctx ctx, List<Value> params) throws Exception {
            if (params.size() != 1) throw new Exception("Expected parameter count");
            int count=(int) getInt("count", params, 0);
            limit=count;
            limitKeepFirst=true;
            return new ValueObj(self());
        }
    }
    
 
    class FunctionLimitLast extends Function {
        public String getName() {
            return "limitLast";
        }
        public String getShortDesc() {
            return "limitLast(count) - return last N matches only";
        }
        public Value callFunction (Ctx ctx, List<Value> params) throws Exception {
            if (params.size() != 1) throw new Exception("Expected parameter count");
            int count=(int) getInt("count", params, 0);
            limit=count;
            limitKeepFirst=false;
            return new ValueObj(self());
        }
    }
    
 

}
    