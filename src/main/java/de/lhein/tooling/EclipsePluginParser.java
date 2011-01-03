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
import java.io.InputStream;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author lhein
 */
public class EclipsePluginParser
    {

    public static final String BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName";
    public static final String BUNDLE_VERSION = "Bundle-Version";
    public static final int MAX_VERSION_POINTS = 2;

    public static enum GOALS
        {
            PLUGIN_SOURCES,
            PLUGINS,
            PLUGIN_ALL,
            FEATURE_SOURCES,
            FEATURES,
            FEATURE_ALL
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
    public String parsePlugins()
        {
        File eclipseFolder = new File(targetEclipseFolder);
        if (!eclipseFolder.exists() || !eclipseFolder.isDirectory())
            {
            return "The specified Eclipse folder is not accessible...Aborting.";
            }


        File[] plugins = new File[0];
        try
            {
            plugins = getFolderContents();
            }
        catch (IOException e)
            {
            return e.getMessage();
            }


        StringBuffer result = new StringBuffer();
        result.append(String.format("<%s>\n", getMulti(this.outputTag)));

        // loop plugin folder content
        for (File f : plugins)
            {
            String version = null;
            String symbolicName = null;

            if (f.getName().toLowerCase().endsWith(".jar"))
                {
                // extract version info from manifest
                try
                    {
                    String[] infos = extractInfoFromJAR(f);
                    if (infos != null)
                        {
                        symbolicName = infos[0];
                        version = infos[1];
                        }
                    }
                catch (IOException ex)
                    {
                    }
                // try feature
                if (version == null &&
                        (getPluginsGoal() == GOALS.FEATURE_ALL ||
                        getPluginsGoal() == GOALS.FEATURE_SOURCES ||
                        getPluginsGoal() == GOALS.FEATURES))
                    {
                    try
                        {
                        String[] infos = extractInfoFromFeatureJAR(f);
                        if (infos != null)
                            {
                            symbolicName = infos[0];
                            version = infos[1];
                            }
                        }
                    catch (Exception e)
                        {
                        continue;
                        }
                    }
                }
            else
                {
                // must be a folder - grab META-INF/MANIFEST.MF
                try
                    {
                    String[] infos = extractInfoFromFolder(f);
                    if (infos != null)
                        {
                        symbolicName = infos[0];
                        version = infos[1];
                        }
                    }
                catch (IOException ex)
                    {
                    continue;
                    }

                 // try feature
                if (version == null &&
                        (getPluginsGoal() == GOALS.FEATURE_ALL ||
                        getPluginsGoal() == GOALS.FEATURE_SOURCES ||
                        getPluginsGoal() == GOALS.FEATURES))
                    {
                    FileInputStream fis = null;
                    try
                        {
                        fis = new FileInputStream(new File(f, "feature.xml"));
                        String[] infos = extractInfoFromFeatureXML(fis);
                        if (infos != null)
                            {
                            symbolicName = infos[0];
                            version = infos[1];
                            }
                        }
                    catch (Exception e)
                        {
                        e.printStackTrace();
                        continue;
                        }
                    finally
                        {
                        if(fis != null)
                            {
                            try
                                {
                                fis.close();
                                }
                            catch (IOException e)
                                {
                                }
                            }
                        }
                    }
                }

            if (version != null)
                {
                String osgiVersion = convertToMavenLogic(version);

                // now parse the rest of the name
                int pos = f.getName().indexOf("_" + version);
                if (pos == -1)
                    {
                    continue;
                    }
                String coreName = f.getName().substring(0, pos);
                int separatorIdx = coreName.lastIndexOf('.');
                String groupId = coreName.substring(0,
                                                    separatorIdx == -1 ? coreName.length() : separatorIdx);
                String artifactId = coreName.substring(separatorIdx == -1 ? 0 : separatorIdx + 1);

                // all info gathered...now put to output
                result.append(String.format("\t<%s>\n", outputTag));
                result.append(String.format("\t\t<groupId>%s</groupId>\n", groupId));
                result.append(String.format("\t\t<artifactId>%s</artifactId>\n", artifactId));
                if (includeVersionsTag)
                    {
                    result.append(String.format("\t\t<version>%s</version>\n", osgiVersion));
                    }
                if (markAsOptional)
                    {
                    result.append("\t\t<optional>true</optional>\n");
                    }
                if (symbolicName != null && includeSymbolicsComment)
                    {
                    result.append(String.format("\t\t<!-- Bundle-SymbolicName: %s -->\n", symbolicName));
                    }
                result.append(String.format("\t</%s>\n", outputTag));
                }
            }

        result.append(String.format("</%s>\n", getMulti(outputTag)));

        return result.toString();

    }

    private File[] getFolderContents() throws IOException
        {
        if (getPluginsGoal() == GOALS.PLUGINS ||
                getPluginsGoal() == GOALS.PLUGIN_ALL ||
                getPluginsGoal() == GOALS.PLUGIN_SOURCES)
            {

            File pluginsFolder = new File(targetEclipseFolder, "plugins");
            if (!pluginsFolder.exists() || !pluginsFolder.isDirectory())
                {
                throw new IOException("The specified Eclipse folder does not contain a plugins folder...Aborting.");
                }
            return pluginsFolder.listFiles(new FileFilter()
                {
                /*
                * (non-Javadoc)
                *
                * @see java.io.FileFilter#accept(java.io.File)
                */
                public boolean accept(File f)
                    {
                        if (!f.exists())
                            {
                            return false;
                            }

                        if (f.isDirectory() || (f.isFile() && f.getName().toLowerCase().endsWith(".jar")))
                            {
                            if (getPluginsGoal() == GOALS.PLUGINS)
                                {
                                return f.getName().toLowerCase().indexOf(".source_") == -1;
                                }
                            else if (getPluginsGoal() == GOALS.PLUGIN_SOURCES)
                                {
                                return f.getName().toLowerCase().indexOf(".source_") != -1;
                                }
                            else
                                {
                                return true;
                                }
                            }
                        return false;
                    }}
                );
            }
        else
            {
            File pluginsFolder = new File(targetEclipseFolder, "features");
            if (!pluginsFolder.exists() || !pluginsFolder.isDirectory())
                {
                throw new IOException("The specified Eclipse folder does not contain a features folder...Aborting.");
                }
            return pluginsFolder.listFiles(new FileFilter()
                {
                /*
                * (non-Javadoc)
                *
                * @see java.io.FileFilter#accept(java.io.File)
                */
                public boolean accept(File f)
                    {
                        if (!f.exists())
                            {
                            return false;
                            }

                        if (f.isDirectory() || (f.isFile() && f.getName().toLowerCase().endsWith(".jar")))
                            {
                            if (getPluginsGoal() == GOALS.FEATURES)
                                {
                                return f.getName().toLowerCase().indexOf(".source_") == -1;
                                }
                            else if (getPluginsGoal() == GOALS.FEATURE_SOURCES)
                                {
                                return f.getName().toLowerCase().indexOf(".source_") != -1;
                                }
                            else
                                {
                                return true;
                                }
                            }
                        return false;
                    }}
                );
            }
        }


    /**
     * extracts the version and the symbolic name from the bundle manifest
     *
     * @param f the manifest.mf file
     * @return a string array with symbolicname at 0 and bundle version at 1 or
     *         null on illegal arguments
     * @throws IOException on errors processing the jar
     */
    private String[] extractInfoFromJAR(File f) throws IOException
    {
        JarFile jar = new JarFile(f);
        Manifest mf = jar.getManifest();
        if (mf != null)
            {
            String symbolicName = mf.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
            String version = mf.getMainAttributes().getValue(BUNDLE_VERSION);
            return new String[]{symbolicName, version};
            }
        return null;
    }

    /**
	 * extracts the version and the symbolic name from the feature.xml in the given feature jar
	 *
	 * @param f
	 *            the feature jar file
	 * @return a string array with symbolicname at 0 and bundle version at 1 or
	 *         null on illegal arguments
	 * @throws IOException
	 *             on errors processing the jar
	 */
	private String[] extractInfoFromFeatureJAR(File f) throws Exception {
        JarFile jar = new JarFile(f);
        JarEntry entry = jar.getJarEntry("feature.xml");
        return entry == null ? null : extractInfoFromFeatureXML(jar.getInputStream(entry));
	}

    /**
     *  extracts the version and the symbolic name from the feature.xml
     * @param is the inputstream of the feature xml
     * @return a string array with symbolicname at 0 and bundle version at 1 or
	 *         null on illegal arguments
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private String[] extractInfoFromFeatureXML(InputStream is)
            throws ParserConfigurationException, SAXException, IOException
        {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(is);
        doc.getDocumentElement().normalize();
        return new String[]{null, doc.getDocumentElement().getAttribute("version")};
        }


    /**
     * extracts the version and the symbolic name from the bundle manifest
     *
     * @param f the bundle root folder
     * @return a string array with symbolicname at 0 and bundle version at 1 or
     *         null on illegal arguments
     * @throws IOException on errors processing the jar
     */
    private String[] extractInfoFromFolder(File f) throws IOException
    {
        File[] fx = f.listFiles(new FileFilter()
        {
        public boolean accept(File p)
            {
                return p.getName().equalsIgnoreCase("META-INF") && p.isDirectory();
            }
        });

        if (fx == null || fx.length <= 0)
            {
            return null;
            }

        File metainffolder = fx[0];
        File[] metainfcontent = metainffolder.listFiles(new FileFilter()
        {
        public boolean accept(File x)
            {
                return x.isFile() && x.getName().equalsIgnoreCase("manifest.mf");
            }
        });

        if (metainfcontent == null || metainfcontent.length <= 0)
            {
            return null;
            }

        File manifestFile = metainfcontent[0];
        Manifest mf = new Manifest(new FileInputStream(manifestFile));
        String symbolicName = mf.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
        String version = mf.getMainAttributes().getValue(BUNDLE_VERSION);

        return new String[]{symbolicName, version};
    }

    /**
     * converts the version number in a suitable format
     *
     * @param version the version from the manifest
     * @return
     */
    private String convertToMavenLogic(String version)
    {
        StringBuffer osgiVersion = new StringBuffer();
        int pointsGiven = 0;

        StringTokenizer stok = new StringTokenizer(version, ".");

        while (stok.hasMoreTokens())
            {
            String part = stok.nextToken();
            if (pointsGiven < MAX_VERSION_POINTS)
                {
                osgiVersion.append(part);
                osgiVersion.append('.');
                pointsGiven++;
                }
            else
                {
                // no more points possible
                if (pointsGiven == MAX_VERSION_POINTS)
                    {
                    // now the - char
                    osgiVersion.append(part);
                    osgiVersion.append('-');
                    pointsGiven++;
                    }
                else
                    {
                    osgiVersion.append(part);
                    pointsGiven++;
                    }
                }
            }

        return osgiVersion.toString();
    }

    private String getMulti(String single)
        {
            if (single.trim().endsWith("y"))
                {
                return String.format("%sies", single.trim().substring(0, single.trim().length() - 1));
                }
            else
                {
                return String.format("%ss", single.trim());
                }
        }

    public boolean isMarkAsOptional()
        {
            return markAsOptional;
        }

    public void setMarkAsOptional(boolean markAsOptional)
        {
            this.markAsOptional = markAsOptional;
        }

    public boolean isIncludeVersionsTag()
        {
            return includeVersionsTag;
        }

    public void setIncludeVersionsTag(boolean includeVersionsTag)
        {
            this.includeVersionsTag = includeVersionsTag;
        }

    public boolean isIncludeSymbolicsComment()
        {
            return includeSymbolicsComment;
        }

    public void setIncludeSymbolicsComment(boolean includeSymbolicsComment)
        {
            this.includeSymbolicsComment = includeSymbolicsComment;
        }

    public String getTargetEclipseFolder()
        {
            return targetEclipseFolder;
        }

    public void setTargetEclipseFolder(String targetEclipseFolder)
        {
            this.targetEclipseFolder = targetEclipseFolder;
        }

    public GOALS getPluginsGoal()
        {
            return pluginsGoal;
        }

    public void setPluginsGoal(GOALS pluginsGoal)
        {
            this.pluginsGoal = pluginsGoal;
        }

    public String getOutputTag()
        {
            return outputTag;
        }

    public void setOutputTag(String outputTag)
        {
            this.outputTag = outputTag;
        }
    }
