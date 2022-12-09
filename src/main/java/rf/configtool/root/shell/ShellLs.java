package rf.configtool.root.shell;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rf.configtool.lexer.TokenStream;
import rf.configtool.main.Ctx;
import rf.configtool.main.FunctionBody;
import rf.configtool.main.FunctionState;
import rf.configtool.main.ObjGlobal;
import rf.configtool.main.runtime.Obj;
import rf.configtool.main.runtime.Value;
import rf.configtool.main.runtime.ValueList;
import rf.configtool.main.runtime.ValueNull;
import rf.configtool.main.runtime.ValueObj;
import rf.configtool.main.runtime.ValueString;
import rf.configtool.main.runtime.lib.ObjDir;
import rf.configtool.main.runtime.lib.ObjFile;
import rf.configtool.main.runtime.lib.ObjGlob;
import rf.configtool.main.runtime.lib.Protection;

public class ShellLs extends ShellCommand {

	private boolean showFiles;
	private boolean showDirs;

	public ShellLs(List<String> parts) throws Exception {
		super(parts);
		
		String name=getName();
		
		if (name.equals("nls") || name.equals("ls")) {
			showFiles = true;
			showDirs = true;
		} else if (name.equals("nlsd") || name.equals("lsd")) {
			showFiles = false;
			showDirs = true;
		} else if (name.equals("nlsf") || name.equals("lsf")) {
			showFiles = true;
			showDirs = false;
		} else {
			throw new Exception("Expected ls, lsf or lsd");
		}
	}

	public Value execute(Ctx ctx) throws Exception {

		String currDir = ctx.getObjGlobal().getCurrDir();
		boolean noArgs=getArgs().isEmpty();
		boolean enableLimits=noArgs;
		
		FileSet fs=new FileSet(showDirs, showFiles, enableLimits);
		
		if (noArgs) {
			ObjGlob glob=new ObjGlob("*");
			String errMsg = fs.addDirContent(currDir, glob);
			if (errMsg != null) {
				ctx.addSystemMessage(errMsg);
				return new ValueNull();
			}
			
			return generateResultList(fs);
		} 
		
		// process args, some of which may be expressions

		List<ShellCommandArg> args=getArgs();
		
		for (ShellCommandArg arg:args) {
			fs.processArg(currDir, ctx, arg);
		}
		
		if (fs.getFiles().size()==0 && fs.getDirectories().size()==1 && !fs.argsContainGlobbing()) {
			// ls someDir ---> list content inside that dir
			String singleDir=fs.getDirectories().get(0);
			
			fs=new FileSet(showDirs,showFiles, enableLimits);
			fs.addDirContent(singleDir, new ObjGlob("*"));
		}

		return generateResultList(fs);

	}


	
	private Value generateResultList(FileSet fs) throws Exception {
		List<Value> result = new ArrayList<Value>();
		
		if (showDirs) {
			List<String> dirList=fs.getDirectories();
			sort(dirList);
			for (String x : dirList) {
				result.add(new ValueObj(new ObjDir(x, Protection.NoProtection)));
			}
		}
		if (showFiles) {
			List<String> fileList=fs.getFiles();
			sort(fileList);
			for (String x : fileList) {
				result.add(new ValueObj(new ObjFile(x, Protection.NoProtection)));
			}

		}

		return new ValueList(result);

	}

}