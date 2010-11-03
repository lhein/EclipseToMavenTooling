/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.lhein.tooling;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author lhein
 */
public class EclipsePluginParser {

    public static final String BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName";
	public static final String BUNDLE_VERSION = "Bundle-Version";
    public static final int MAX_VERSION_POINTS = 2;

	public static enum GOALS {
		SOURCES,
        PLUGINS,
        ALL
	}

	private boolean markAsOptional = false;
	private boolean includeVersionsTag = true;
	private boolean includeSymbolicsComment = true;
	private String targetEclipseFolder;
	private GOALS pluginsGoal;
	private String outputTag;

	/**
	 * parses the plugins folder of the eclipseDir
	 */
	public String parsePlugins() {
		File eclipseFolder = new File(targetEclipseFolder);
		if (!eclipseFolder.exists() || !eclipseFolder.isDirectory()) {
			return "The specified Eclipse folder is not accessible...Aborting.";
		}

		File pluginsFolder = new File(targetEclipseFolder, "plugins");
		if (!pluginsFolder.exists() || !pluginsFolder.isDirectory()) {
			return "The specified Eclipse folder does not contain a plugins folder...Aborting.";
    	}

		File[] plugins = pluginsFolder.listFiles(new FileFilter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.io.FileFilter#accept(java.io.File)
			 */
			public boolean accept(File f) {
				if (!f.exists())
					return false;

				if (f.isDirectory() || (f.isFile() && f.getName().toLowerCase().endsWith(".jar"))) {
					if (getPluginsGoal() == GOALS.PLUGINS) {
						return f.getName().toLowerCase().indexOf(".source_") == -1;
					} else if (getPluginsGoal() == GOALS.SOURCES) {
						return f.getName().toLowerCase().indexOf(".source_") != -1;
					} else {
						return true;
					}
				}
				return false;
			}
		});

		StringBuffer result = new StringBuffer();
		result.append(String.format("<%s>\n", getMulti(this.outputTag)));

		// loop plugin folder content
		for (File f : plugins) {
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
					continue;
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
					continue;
				}
			}

			if (version != null) {
				String osgiVersion = convertToMavenLogic(version);

				// now parse the rest of the name
				int pos = f.getName().indexOf("_" + version);
				if (pos == -1) {
					continue;
				}
				String coreName = f.getName().substring(0, pos);
				String groupId = coreName.substring(0,
						coreName.lastIndexOf('.'));
				String artifactId = coreName.substring(coreName
						.lastIndexOf('.') + 1);

				// all info gathered...now put to output
				result.append(String.format("\t<%s>\n", outputTag));
				result.append(String.format("\t\t<groupId>%s</groupId>\n", groupId));
				result.append(String.format("\t\t<artifactId>%s</artifactId>\n", artifactId));
				if (includeVersionsTag) {
					result.append(String.format("\t\t<version>%s</version>\n", osgiVersion));
				}
				if (markAsOptional) {
					result.append("\t\t<optional>true</optional>\n");
				}
				if (symbolicName != null && includeSymbolicsComment) {
					result.append(String.format("\t\t<!-- Bundle-SymbolicName: %s -->\n", symbolicName));
				}
				result.append(String.format("\t</%s>\n", outputTag));
			}
		}

		result.append(String.format("</%s>\n", getMulti(outputTag)));

        return result.toString();
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
			String symbolicName = mf.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
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
			public boolean accept(File p) {
				return p.getName().equalsIgnoreCase("META-INF") && p.isDirectory();
			}
		});

		if (fx == null || fx.length <= 0) {
			return null;
		}

		File metainffolder = fx[0];
		File[] metainfcontent = metainffolder.listFiles(new FileFilter() {
			public boolean accept(File x) {
				return x.isFile() && x.getName().equalsIgnoreCase("manifest.mf");
			}
		});

		if (metainfcontent == null || metainfcontent.length <= 0) {
			return null;
		}

		File manifestFile = metainfcontent[0];
		Manifest mf = new Manifest(new FileInputStream(manifestFile));
		String symbolicName = mf.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
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

    private String getMulti(String single) {
        if (single.trim().endsWith("y")) {
            return String.format("%sies", single.trim().substring(0, single.trim().length()-1));
        } else {
            return String.format("%ss", single.trim());
        }
    }

    public boolean isMarkAsOptional() {
        return markAsOptional;
    }

    public void setMarkAsOptional(boolean markAsOptional) {
        this.markAsOptional = markAsOptional;
    }

    public boolean isIncludeVersionsTag() {
        return includeVersionsTag;
    }

    public void setIncludeVersionsTag(boolean includeVersionsTag) {
        this.includeVersionsTag = includeVersionsTag;
    }

    public boolean isIncludeSymbolicsComment() {
        return includeSymbolicsComment;
    }

    public void setIncludeSymbolicsComment(boolean includeSymbolicsComment) {
        this.includeSymbolicsComment = includeSymbolicsComment;
    }

    public String getTargetEclipseFolder() {
        return targetEclipseFolder;
    }

    public void setTargetEclipseFolder(String targetEclipseFolder) {
        this.targetEclipseFolder = targetEclipseFolder;
    }

    public GOALS getPluginsGoal() {
        return pluginsGoal;
    }

    public void setPluginsGoal(GOALS pluginsGoal) {
        this.pluginsGoal = pluginsGoal;
    }

    public String getOutputTag() {
        return outputTag;
    }

    public void setOutputTag(String outputTag) {
        this.outputTag = outputTag;
    }
}
