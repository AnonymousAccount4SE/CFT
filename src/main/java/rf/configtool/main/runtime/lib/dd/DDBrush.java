package rf.configtool.main.runtime.lib.dd;

import java.awt.Color;
import java.util.*;

import rf.configtool.main.Ctx;
import rf.configtool.main.runtime.ColList;
import rf.configtool.main.runtime.Function;
import rf.configtool.main.runtime.Obj;
import rf.configtool.main.runtime.Value;
import rf.configtool.main.runtime.ValueObj;
import rf.configtool.main.runtime.lib.ObjColor;


/**
 * The 2D brush is simply for drawing lines, nothing fancy
 */
public class DDBrush extends Obj {

	private ViewReceiver recv;
	private Color color;
	private Vector2d offsetA, offsetB;
	
	private Vector2d prevA, prevB;

	
	public DDBrush (ViewReceiver recv, Vector2d offsetA, Vector2d offsetB) {
		this.recv=recv;
		this.offsetA=offsetA;
		this.offsetB=offsetB;
		this.color=new Color(0,0,0);
		
		this.add(new FunctionPenDown());
		this.add(new FunctionPenUp());
		this.add(new FunctionSetColor());
		
	}
	@Override
	public boolean eq(Obj x) {
		return x == this;
	}

	@Override
	public String getTypeName() {
		return "DD.Brush";
	}

	@Override
	public ColList getContentDescription() {
		return ColList.list().regular(getDesc());
	}

	private String getDesc() {
		return "DD.Brush";
	}

	private DDBrush self() {
		return this;
	}

	
	   class FunctionPenDown extends Function {
	        public String getName() {
	            return "penDown";
	        }

	        public String getShortDesc() {
	            return "penDown(Ref) - draw area from last penDown";
	        }

	        public Value callFunction(Ctx ctx, List<Value> params) throws Exception {
	        	if (params.size() != 1) throw new RuntimeException("Expected Ref parameter");
	        	
	        	Obj ref1=getObj("Ref",params,0);
	        	if (ref1 instanceof DDRef) {
	        		Ref ref=((DDRef) ref1).getRef();
	        		// calculate new global points a and b
	        		Vector2d a=ref.transformLocalToGlobal(offsetA);
	        		Vector2d b=ref.transformLocalToGlobal(offsetB);
	        		
	        		if (prevA != null && prevB != null) {
	        			List<Vector2d> points=new ArrayList<Vector2d>();
	        			points.add(prevA);
	        			points.add(prevB);
	        			points.add(b);
	        			points.add(a);
	        			points.add(prevA); // closing polygon
	        			recv.addPolygon(new Polygon(points,color));
	        			
//	        			StringBuffer sb=new StringBuffer();
//	        			for (Vector2d p:points) {
//	        				sb.append(" " + p.toString());
//	        			}
//	        			System.out.println(sb.toString());
	        		}
	        		prevA=a;
	        		prevB=b;
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
	            return "penUp() - stop drawing";
	        }

	        public Value callFunction(Ctx ctx, List<Value> params) throws Exception {
	        	if (params.size() != 0) throw new RuntimeException("Expected no parameters");
	        	prevA=prevB=null;
	        	return new ValueObj(self());
	        }
	    }

	
	   class FunctionSetColor extends Function {
	        public String getName() {
	            return "setColor";
	        }

	        public String getShortDesc() {
	            return "setColor(Color) - set color";
	        }

	        public Value callFunction(Ctx ctx, List<Value> params) throws Exception {
	        	if (params.size() != 1) throw new RuntimeException("Expected Color parameter");
	        	Obj col1=getObj("Color",params,0);
	        	if (col1 instanceof ObjColor) {
	        		Color color=((ObjColor) col1).getAWTColor();
	        		self().color=color;
	        		return new ValueObj(self());
	        	} else {
	        		throw new RuntimeException("Expected Color parameter");
	        	}
	        }
	    }
}
