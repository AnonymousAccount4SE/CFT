package rf.configtool.main.runtime.lib.ddd;

import java.awt.Color;
import java.util.List;

import rf.configtool.main.Ctx;
import rf.configtool.main.runtime.ColList;
import rf.configtool.main.runtime.Function;
import rf.configtool.main.runtime.Obj;
import rf.configtool.main.runtime.Value;
import rf.configtool.main.runtime.ValueObj;
import rf.configtool.main.runtime.lib.ObjColor;
import rf.configtool.main.runtime.lib.ddd.core.Brush;
import rf.configtool.main.runtime.lib.ddd.core.Triangle;
import rf.configtool.main.runtime.lib.ddd.core.TriangleReceiver;
import rf.configtool.main.runtime.lib.ddd.core.Vector3d;
import rf.configtool.main.runtime.lib.ddd.core.VisibleAttributes;

/**
 * 
 *
 */
public class DDDBrush extends Obj {
	
	private TriangleReceiver triRecv;
	
	private Brush brush;
	
    public DDDBrush (TriangleReceiver triRecv) {
    	this.triRecv=triRecv;
    	this.brush=new Brush(triRecv);
    	
    	this.add(new FunctionSetColor());

    	this.add(new FunctionAddPoint());
    	this.add(new FunctionAddTerminationTriangle());
    	
    	this.add(new FunctionBox());
    	//this.add(new FunctionCircle());
    	
    	this.add(new FunctionPenDown());
    	this.add(new FunctionPenUp());
    	    	
    }

    @Override
    public boolean eq(Obj x) {
        return x == this;
    }

    public String getTypeName() {
        return "DDD.Brush";
    }

    public ColList getContentDescription() {
        return ColList.list().regular(getDesc());
    }

    private String getDesc() {
        return "DDD.Brush";
    }

    private DDDBrush self() {
        return this;
    }

    
    // -----------------------------------
    // brush logic
    // -----------------------------------

  
    
    class FunctionSetColor extends Function {
        public String getName() {
            return "setColor";
        }

        public String getShortDesc() {
            return "setColor(Color) - set color - returns self";
        }

        public Value callFunction(Ctx ctx, List<Value> params) throws Exception {
        	if (params.size() != 1) throw new RuntimeException("Expected Color parameter");
        	Obj x=getObj("color",params,0);
        	if (x instanceof ObjColor) {
        		Color color=((ObjColor) x).getAWTColor();
            	self().brush.setAttr(new VisibleAttributes(color));
            	return new ValueObj(self());
        	} else {
        		throw new RuntimeException("Expected Color parameter");
        	}
        }
    }
    
    
    class FunctionAddPoint extends Function {
        public String getName() {
            return "addPoint";
        }

        public String getShortDesc() {
            return "addPoint(Vector) - add brush point as (relative) vector - returns self";
        }

        public Value callFunction(Ctx ctx, List<Value> params) throws Exception {
        	if (params.size() != 1) throw new RuntimeException("Expected DDD.Vector parameter");
        	Obj obj=getObj("vector",params,0);
        	if (obj instanceof DDDVector) {
        		Vector3d vec=((DDDVector) obj).getVec();
        		self().brush.addPoint(vec);
        	}
        	return new ValueObj(self());
        }
    }
    
    
    class FunctionAddTerminationTriangle extends Function {
        public String getName() {
            return "addTerminationTriangle";
        }

        public String getShortDesc() {
            return "addTerminationTriangle(DDD.Triangle) - triangles rendered at first penDown and at penUp";
        }

        public Value callFunction(Ctx ctx, List<Value> params) throws Exception {
        	if (params.size() != 1) throw new RuntimeException("Expected DDD.Triangle parameter");
        	Obj obj=getObj("triangle",params,0);
        	if (obj instanceof DDDTriangle) {
        		Triangle tri=((DDDTriangle) obj).getTri();
        		self().brush.addTerminatorTriangle(tri);
            	return new ValueObj(self());
        	} else {
        		throw new RuntimeException("Expected DDD.Triangle parameter");
        	}
        }
    }
    
    
    class FunctionPenDown extends Function {
        public String getName() {
            return "penDown";
        }

        public String getShortDesc() {
            return "penDown(Ref) - brush pen down";
        }

        public Value callFunction(Ctx ctx, List<Value> params) throws Exception {
        	if (params.size() != 1) throw new RuntimeException("Expected Ref parameter");
        	Obj x=getObj("ref",params,0);
        	if (x instanceof DDDRef) {
        		self().brush.penDown(  ((DDDRef) x).getRef() );
            	return new ValueObj(self());
        	} else {
        		throw new RuntimeException("Expected Ref parameter");
        	}
        }
    }
    
    class FunctionPenUp extends Function {
        public String getName() {
            return "penUp";
        }

        public String getShortDesc() {
            return "penUp() - brush pen up";
        }

        public Value callFunction(Ctx ctx, List<Value> params) throws Exception {
        	if (params.size() != 0) throw new RuntimeException("Expected no parameters");
        	self().brush.penUp();
        	return new ValueObj(self());
        }
    }
    
    private Vector3d RU (double right, double up) {
    	return new Vector3d(0, -right, up);
    }
    
    class FunctionBox extends Function {
        public String getName() {
            return "box";
        }

        public String getShortDesc() {
            return "box(width,height,color) - creates box brush, to be dragged along Fwd direction)";
        }
        
        public Value callFunction(Ctx ctx, List<Value> params) throws Exception {
        	if (params.size() != 3) throw new RuntimeException("Expected parameters, width, height, color");
        	double width=getFloat("width",params,0);
        	double height=getFloat("height",params,1);
        	Obj col1=getObj("color",params,2);
        	if (col1 instanceof ObjColor) {
        		Color col=((ObjColor) col1).getAWTColor();
        		VisibleAttributes attr = new VisibleAttributes(col);

        		brush.setAttr(attr);
        		
        		Vector3d a=RU(-width/2,-height/2);
        		Vector3d b=RU(+width/2,-height/2);
        		Vector3d c=RU(+width/2,+height/2);
        		Vector3d d=RU(-width/2,+height/2);
        		brush.addPoint(a);
        		brush.addPoint(b);
        		brush.addPoint(c);
        		brush.addPoint(d);
        		brush.addPoint(a);
        		Triangle t1=new Triangle(a,b,c,attr);
        		Triangle t2=new Triangle(a,c,d,attr);
        		brush.addTerminatorTriangle(t1);
        		brush.addTerminatorTriangle(t2);
        		
        		brush.setSplitBothWays(false);
        		return new ValueObj(self());
        	} else {
        		throw new RuntimeException("Expected parameters, width, height, color");
        	}
        }
    }
    
    
    
    
    
}
