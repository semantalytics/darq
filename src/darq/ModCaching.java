package darq;

import arq.cmd.CmdException;
import arq.cmdline.ArgDecl;
import arq.cmdline.ArgModuleGeneral;
import arq.cmdline.CmdArgModule;
import arq.cmdline.CmdGeneral;

import java.io.File;
import java.lang.String;

public class ModCaching implements ArgModuleGeneral {

	protected final ArgDecl argDarqCache = new ArgDecl(ArgDecl.HasValue, "cache");
	/* return a value if cache parameter was given by commandline */
	private String cacheConfig;
	private String cacheConfigFile;
	private File file;

	/*
	 * (non-Javadoc)
	 * 
	 * @see arq.cmdline.ArgModuleGeneral#registerWith(arq.cmdline.CmdGeneral)
	 *      Output for help
	 */
	public void registerWith(CmdGeneral cmdLine) {
		cmdLine.add(argDarqCache, "--cache 'enable'|<file>", "enable cache (with configuration file)");

	}

	/**
	 * returns String from command line otherwise throws exception
	 */
	public void processArgs(CmdArgModule cmdLine) throws IllegalArgumentException {
		cacheConfig = cmdLine.getValue(argDarqCache);
		if (cacheConfig == null) {
			cacheConfigFile = null;
		} else {
			if (cacheConfig.equals("enable")) {

				String path = System.getProperty("user.dir") + "\\DarqCacheConfigurationMemory.xml";
				// FRAGE Do it work on Linux?
				cacheConfigFile = path;
				file = new File(path);
			} else if (cacheConfig != null) {
				file = new File(cacheConfig);
				cacheConfigFile = cacheConfig;
			}
			if (!file.exists())
				throw new CmdException("Error [MODCACHING]: Cache file " + file.toString() + " not found.");
		}
	}

	public String getCacheConfig() {
		return cacheConfigFile;
	}

}
