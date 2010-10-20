package de.lhein.tooling;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author lhein
 */
public class E2MParser {
	public static final String OPTION_TAG = "-o";
	public static final String OPTION_VALUE_SEPARATOR = "=";

	public static final String OPTION_ECLIPSE_FOLDER = "eclipseDir";
	public static final String OPTION_PARSE_GOAL = "goal";
	public static final String OPTION_TARGET_TAG = "targetTag";
	public static final String OPTION_OUTFILE = "outputfile";
	public static final String OPTION_OUTPUTVERSION = "includeVersions";
	public static final String OPTION_OUTPUTSYMBOLIC = "includeSymbolics";
	public static final String OPTION_MARK_OPTIONAL = "markOptional";

	public static final String BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName";
	public static final String BUNDLE_VERSION = "Bundle-Version";

	public static final int MAX_VERSION_POINTS = 2;

	public static enum GOALS {
		sources, plugins, all
	}

	public static enum TARGET_TAG {
		artifactItem("artifactItem", "artifactItems"), dependency("dependency",
				"dependencies");

		private String single;
		private String multi;

		private TARGET_TAG(String single, String multi) {
			this.single = single;
			this.multi = multi;
		}

		/**
		 * @return the multi
		 */
		public String getMulti() {
			return this.multi;
		}

		/**
		 * @return the single
		 */
		public String getSingle() {
			return this.single;
		}
	}

	private boolean markOptional = false;
	private boolean includeVersions = true;
	private boolean includeSymbolics = true;
	private String eclipseDir;
	private GOALS goal;
	private TARGET_TAG outTag;
	private String outFile;

	/**
	 * creates a new parser
	 * 
	 * @param args
	 *            the command line arguments
	 */
	public E2MParser(String[] args) {
		try {
			initialize(args);
		} catch (IllegalArgumentException e) {
			printHelp();
		}
	}

	/**
	 * initializes all parameters and checks for validity
	 * 
	 * @param args
	 *            the command line arguments
	 * @throws IllegalArgumentException
	 *             on invalid arguments
	 */
	private void initialize(String[] args) throws IllegalArgumentException {
		// parsing the arguments
		for (String s : args) {
			if (s.toLowerCase().startsWith(OPTION_TAG)) {
				s = s.substring(OPTION_TAG.length());

				// found a valid option
				if (s.toLowerCase().startsWith(
						OPTION_ECLIPSE_FOLDER.toLowerCase())) {
					if (s.indexOf(OPTION_VALUE_SEPARATOR) != -1) {
						// valid notation
						this.eclipseDir = s.substring(s
								.indexOf(OPTION_VALUE_SEPARATOR) + 1);
						System.out
								.println("Eclipse-Folder: " + this.eclipseDir);
					} else {
						System.err
								.println("Invalid option notation. Use notation -o<optionName>=<optionValue> to specify options.");
					}
				} else if (s.toLowerCase().startsWith(
						OPTION_OUTFILE.toLowerCase())) {
					if (s.indexOf(OPTION_VALUE_SEPARATOR) != -1) {
						// valid notation
						this.outFile = s.substring(s
								.indexOf(OPTION_VALUE_SEPARATOR) + 1);
						System.out.println("Output-File: " + this.outFile);
					} else {
						System.err
								.println("Invalid option notation. Use notation -o<optionName>=<optionValue> to specify options.");
					}
				} else if (s.toLowerCase().startsWith(
						OPTION_PARSE_GOAL.toLowerCase())) {
					if (s.indexOf(OPTION_VALUE_SEPARATOR) != -1) {
						// valid notation
						this.setGoal(GOALS.valueOf(s.substring(s
								.indexOf(OPTION_VALUE_SEPARATOR) + 1)));
						System.out.println("Goal: " + this.getGoal());
					} else {
						System.err
								.println("Invalid option notation. Use notation -o<optionName>=<optionValue> to specify options.");
					}
				} else if (s.toLowerCase().startsWith(
						OPTION_TARGET_TAG.toLowerCase())) {
					if (s.indexOf(OPTION_VALUE_SEPARATOR) != -1) {
						// valid notation
						this.outTag = TARGET_TAG.valueOf(s.substring(s
								.indexOf(OPTION_VALUE_SEPARATOR) + 1));
						System.out.println("Target-Tag: " + this.outTag);
					} else {
						System.err
								.println("Invalid option notation. Use notation -o<optionName>=<optionValue> to specify options.");
					}
				} else if (s.toLowerCase().startsWith(
						OPTION_OUTPUTVERSION.toLowerCase())) {
					if (s.indexOf(OPTION_VALUE_SEPARATOR) != -1) {
						// valid notation
						this.includeVersions = Boolean
								.parseBoolean(s.substring(s
										.indexOf(OPTION_VALUE_SEPARATOR) + 1));
						System.out.println("Include-Versions: "
								+ this.includeVersions);
					} else {
						System.err
								.println("Invalid option notation. Use notation -o<optionName>=<optionValue> to specify options.");
					}
				} else if (s.toLowerCase().startsWith(
						OPTION_OUTPUTSYMBOLIC.toLowerCase())) {
					if (s.indexOf(OPTION_VALUE_SEPARATOR) != -1) {
						// valid notation
						this.includeSymbolics = Boolean
								.parseBoolean(s.substring(s
										.indexOf(OPTION_VALUE_SEPARATOR) + 1));
						System.out.println("Include-Symbolics: "
								+ this.includeSymbolics);
					} else {
						System.err
								.println("Invalid option notation. Use notation -o<optionName>=<optionValue> to specify options.");
					}
				} else if (s.toLowerCase().startsWith(
						OPTION_MARK_OPTIONAL.toLowerCase())) {
					if (s.indexOf(OPTION_VALUE_SEPARATOR) != -1) {
						// valid notation
						this.markOptional = Boolean
								.parseBoolean(s.substring(s
										.indexOf(OPTION_VALUE_SEPARATOR) + 1));
						System.out.println("Mark Optional: "
								+ this.markOptional);
					} else {
						System.err
								.println("Invalid option notation. Use notation -o<optionName>=<optionValue> to specify options.");
					}
				} else {
					// invalid option
					System.out
							.println("Unknown option " + s + " is ignored...");
				}
			}
		}

		// final check
		if (eclipseDir == null || eclipseDir.trim().length() < 1
				|| getGoal() == null || outTag == null || outFile == null
				|| outFile.trim().length() < 1) {
			throw new IllegalArgumentException(
					"Not all needed parameters where provided.");
		}
	}

	/**
	 * display a command line help
	 */
	private void printHelp() {
		System.out.println("E2MParser -o<name>=<value> ...");
		System.out
				.println("Possible options: (all mandatory, except the ones with a default value!)");
		System.out
				.println("\t"
						+ OPTION_ECLIPSE_FOLDER
						+ " : The exact path to the eclipse installation to parse (Escape possible whitespaces!)");
		System.out.println("\t" + OPTION_PARSE_GOAL
				+ " : The goal to run. (parse for: sources, plugins or all)");
		System.out
				.println("\t"
						+ OPTION_TARGET_TAG
						+ " : The tags to use for the output of the parsing result. (artifactItem or dependency)");
		System.out
				.println("\t"
						+ OPTION_OUTFILE
						+ " : The exact path to the file used to save the parsing result");
		System.out
				.println("\t"
						+ OPTION_OUTPUTVERSION
						+ " : Flag if the artifact version should be printed as well (default: true)");
		System.out
				.println("\t"
						+ OPTION_OUTPUTSYMBOLIC
						+ " : Flag if a comment with the bundle-symbolicname should be printed as well (default: true)");
		System.out
				.println("\t"
						+ OPTION_MARK_OPTIONAL
						+ " : Flag if the optional tag should be added (default: false)");
	}

	/**
	 * parses the plugins folder of the eclipseDir
	 */
	public void parse() {
		File eclipseFolder = new File(eclipseDir);
		if (!eclipseFolder.exists() || !eclipseFolder.isDirectory()) {
			System.err
					.println("The specified Eclipse folder is not accessible...Aborting.");
			return;
		}

		File pluginsFolder = new File(eclipseDir, "plugins");
		if (!pluginsFolder.exists() || !pluginsFolder.isDirectory()) {
			System.err
					.println("The specified Eclipse folder does not contain a plugins folder...Aborting.");
			return;
		}

		File out = new File(outFile);
		try {
			out.createNewFile();
		} catch (IOException ioex) {
			System.err.println("Unable to create output file...Aborting.");
			return;
		}

		File[] content = pluginsFolder.listFiles(new FileFilter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.io.FileFilter#accept(java.io.File)
			 */
			public boolean accept(File f) {
				if (!f.exists())
					return false;

				if (f.isDirectory()
						|| (f.isFile() && f.getName().toLowerCase()
								.endsWith(".jar"))) {
					if (getGoal() == GOALS.plugins) {
						return f.getName().toLowerCase().indexOf(".source_") == -1;
					} else if (getGoal() == GOALS.sources) {
						return f.getName().toLowerCase().indexOf(".source_") != -1;
					} else {
						return true;
					}
				}
				return false;
			}
		});

		StringBuffer result = new StringBuffer();
		result.append(String.format("<%s>\n", outTag.getMulti()));

		// loop plugin folder content
		for (File f : content) {
			String version = null;
			String symbolicName = null;

			if (f.getName().toLowerCase().endsWith(".jar")) {
				// extract version info from manifest
				try {
					String[] infos = extractInfoFromJAR(f);
					if (infos != null) {
						symbolicName = infos[0];
						version = infos[1];
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			} else {
				// must be a folder - grab META-INF/MANIFEST.MF
				try {
					String[] infos = extractInfoFromFolder(f);
					if (infos != null) {
						symbolicName = infos[0];
						version = infos[1];
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}

			if (version != null) {
				String osgiVersion = convertToMavenLogic(version);

				// now parse the rest of the name
				int pos = f.getName().indexOf("_" + version);
				if (pos == -1) {
					System.err.println("Error processing " + f.getName()
							+ "...Skipped.");
					continue;
				}
				String coreName = f.getName().substring(0, pos);
				String groupId = coreName.substring(0,
						coreName.lastIndexOf('.'));
				String artifactId = coreName.substring(coreName
						.lastIndexOf('.') + 1);

				// all info gathered...now put to output
				result.append(String.format("\t<%s>\n", outTag.getSingle()));
				result.append(String.format("\t\t<groupId>%s</groupId>\n",
						groupId));
				result.append(String.format(
						"\t\t<artifactId>%s</artifactId>\n", artifactId));
				if (includeVersions) {
					result.append(String.format("\t\t<version>%s</version>\n",
							osgiVersion));
				}
				if (markOptional) {
					result.append("\t\t<optional>true</optional>\n");
				}
				if (symbolicName != null && includeSymbolics) {
					result.append(String.format(
							"\t\t<!-- Bundle-SymbolicName: %s -->\n",
							symbolicName));
				}
				result.append(String.format("\t</%s>\n", outTag.getSingle()));
			}
		}

		result.append(String.format("</%s>\n", outTag.getMulti()));

		// now write file
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(out);
			fos.write(result.toString().getBytes());
		} catch (IOException ex) {
			System.err.println("Error saving results to "
					+ out.getAbsolutePath());
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * extracts the version and the symbolic name from the bundle manifest
	 * 
	 * @param f
	 *            the manifest.mf file
	 * @return a string array with symbolicname at 0 and bundle version at 1 or
	 *         null on illegal arguments
	 * @throws IOException
	 *             on errors processing the jar
	 */
	private String[] extractInfoFromJAR(File f) throws IOException {
		JarFile jar = new JarFile(f);
		Manifest mf = jar.getManifest();
		if (mf != null) {
			String symbolicName = mf.getMainAttributes().getValue(
					BUNDLE_SYMBOLICNAME);
			String version = mf.getMainAttributes().getValue(BUNDLE_VERSION);
			return new String[] { symbolicName, version };
		}
		return null;
	}

	/**
	 * extracts the version and the symbolic name from the bundle manifest
	 * 
	 * @param f
	 *            the bundle root folder
	 * @return a string array with symbolicname at 0 and bundle version at 1 or
	 *         null on illegal arguments
	 * @throws IOException
	 *             on errors processing the jar
	 */
	private String[] extractInfoFromFolder(File f) throws IOException {
		File[] fx = f.listFiles(new FileFilter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.io.FileFilter#accept(java.io.File)
			 */
			public boolean accept(File p) {
				return p.getName().equalsIgnoreCase("META-INF")
						&& p.isDirectory();
			}
		});

		if (fx == null || fx.length <= 0) {
			return null;
		}

		File metainffolder = fx[0];
		File[] metainfcontent = metainffolder.listFiles(new FileFilter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.io.FileFilter#accept(java.io.File)
			 */
			public boolean accept(File x) {
				return x.isFile()
						&& x.getName().equalsIgnoreCase("manifest.mf");
			}
		});

		if (metainfcontent == null || metainfcontent.length <= 0) {
			return null;
		}

		File manifestFile = metainfcontent[0];
		Manifest mf = new Manifest(new FileInputStream(manifestFile));
		String symbolicName = mf.getMainAttributes().getValue(
				BUNDLE_SYMBOLICNAME);
		String version = mf.getMainAttributes().getValue(BUNDLE_VERSION);
		return new String[] { symbolicName, version };
	}

	/**
	 * converts the version number in a suitable format
	 * 
	 * @param version
	 *            the version from the manifest
	 * @return
	 */
	private String convertToMavenLogic(String version) {
		StringBuffer osgiVersion = new StringBuffer();
		int pointsGiven = 0;

		StringTokenizer stok = new StringTokenizer(version, ".");

		while (stok.hasMoreTokens()) {
			String part = stok.nextToken();
			if (pointsGiven < MAX_VERSION_POINTS) {
				osgiVersion.append(part);
				osgiVersion.append('.');
				pointsGiven++;
			} else {
				// no more points possible
				if (pointsGiven == MAX_VERSION_POINTS) {
					// now the - char
					osgiVersion.append(part);
					osgiVersion.append('-');
					pointsGiven++;
				} else {
					osgiVersion.append(part);
					pointsGiven++;
				}
			}
		}

		return osgiVersion.toString();
	}

	/**
	 * @param goal
	 *            the goal to set
	 */
	public void setGoal(GOALS goal) {
		this.goal = goal;
	}

	/**
	 * @return the goal
	 */
	public GOALS getGoal() {
		return goal;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new E2MParser(args).parse();
	}
}
