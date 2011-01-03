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

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.*;

public class MainWindow extends JFrame
    {
    private JPanel mainPanel;
    private JTextField txtEclipseFolder;
    private JButton btnEclipseFolder;
    private JRadioButton radioGoalsPluginAll;
    private JRadioButton radioGoalsPlugins;
    private JRadioButton radioGoalsPluginSources;
    private JRadioButton radioGoalsFeatureAll;
    private JRadioButton radioGoalsFeatures;
    private JComboBox comboTargetTag;
    private JCheckBox checkIncludeVersions;
    private JCheckBox checkIncludeSymbolics;
    private JCheckBox checkMarkOptional;
    private JButton btnAnalyze;
    private JTextArea textAreaResult;
    private JButton btnCopyToClipboard;
    private JButton btnSaveToFile;
    private JPanel panel;
    private JLabel lblEclipseFolder;
    private JLabel lblGoal;
    private JLabel lblTargetTag;
    private JPanel panel_results;
    private JScrollPane outputscrollpane;
    private JPanel goalPanel;
    private JRadioButton radioGoalsFeatureSources;

    private File lastEclipseDir = new File(".");
    private File lastOutputFile = new File("./output.xml");

    private EclipsePluginParser parser = new EclipsePluginParser();

    public MainWindow()
        {
        super("Eclipse to Maven Tooling");
        initialize();
        }

    private void initialize()
        {
        setContentPane(this.mainPanel);

        btnAnalyze.addActionListener(new ActionListener()
        {
        @Override
        public void actionPerformed(ActionEvent e)
            {
            parser.setIncludeSymbolicsComment(checkIncludeSymbolics.isSelected());
            parser.setIncludeVersionsTag(checkIncludeVersions.isSelected());
            parser.setMarkAsOptional(checkMarkOptional.isSelected());
            parser.setOutputTag(comboTargetTag.getSelectedItem().toString());
            EclipsePluginParser.GOALS goal;
            if (radioGoalsPluginAll.isSelected())
                {
                goal = EclipsePluginParser.GOALS.PLUGIN_ALL;
                }
            else if (radioGoalsPlugins.isSelected())
                {
                goal = EclipsePluginParser.GOALS.PLUGINS;
                }
            else if (radioGoalsPluginSources.isSelected())
                {
                goal = EclipsePluginParser.GOALS.PLUGIN_SOURCES;
                }
            else if (radioGoalsFeatureAll.isSelected())
                {
                goal = EclipsePluginParser.GOALS.FEATURE_ALL;
                }
            else if (radioGoalsFeatures.isSelected())
                {
                goal = EclipsePluginParser.GOALS.FEATURES;
                }
            else
                {
                goal = EclipsePluginParser.GOALS.FEATURE_SOURCES;
                }
            parser.setPluginsGoal(goal);
            parser.setTargetEclipseFolder(txtEclipseFolder.getText());
            textAreaResult.setText(parser.parsePlugins());
            }
        });
        btnEclipseFolder.addActionListener(new ActionListener()
        {
        @Override
        public void actionPerformed(ActionEvent e)
            {
            JFileChooser jfc = new JFileChooser();
            jfc.setDialogTitle("Select Eclipse Folder...");
            jfc.setDialogType(JFileChooser.OPEN_DIALOG);
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (lastEclipseDir != null)
                {
                jfc.setSelectedFile(lastEclipseDir);
                }
            if (JFileChooser.APPROVE_OPTION == jfc.showOpenDialog(MainWindow.this))
                {
                lastEclipseDir = jfc.getSelectedFile();
                if (lastEclipseDir != null)
                    {
                    txtEclipseFolder.setText(lastEclipseDir.getAbsolutePath());
                    }
                }
            }
        });
        btnCopyToClipboard.addActionListener(new ActionListener()
        {
        @Override
        public void actionPerformed(ActionEvent e)
            {
            String selection = textAreaResult.getText();
            StringSelection data = new StringSelection(selection);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(data, data);
            }
        });
        btnSaveToFile.addActionListener(new ActionListener()
        {
        @Override
        public void actionPerformed(ActionEvent e)
            {
            JFileChooser jfc = new JFileChooser();
            if (lastOutputFile != null)
                {
                jfc.setSelectedFile(lastOutputFile);
                }
            jfc.setDialogTitle("Save to file...");
            jfc.setDialogType(JFileChooser.SAVE_DIALOG);
            if (JFileChooser.APPROVE_OPTION == jfc.showSaveDialog(MainWindow.this))
                {
                lastOutputFile = jfc.getSelectedFile();
                if (lastOutputFile != null)
                    {
                    saveToFile();
                    }
                }
            }
        });

        textAreaResult.setTabSize(4);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(600, 400));
        pack();
        setVisible(true);
        }

    private void saveToFile()
        {
        BufferedOutputStream bos = null;
        try
            {
            bos = new BufferedOutputStream(new FileOutputStream(lastOutputFile));
            bos.write(this.textAreaResult.getText().getBytes());
            bos.flush();
            JOptionPane.showMessageDialog(this,
                                          "The result was successfully saved to file " + lastOutputFile.getAbsolutePath(),
                                          "Saved file...", JOptionPane.INFORMATION_MESSAGE);
            }
        catch (IOException ex)
            {
            JOptionPane.showMessageDialog(this,
                                          "Unable to save to file " + lastOutputFile.getAbsolutePath() + " due to: \n" + ex
                                                  .getMessage(), "Unable to save file...", JOptionPane.ERROR_MESSAGE);
            }
        finally
            {
            if (bos != null)
                {
                try
                    {
                    bos.close();
                    }
                catch (IOException e)
                    {
                    // ignore
                    }
                }
            }

        }

    public static void main(String[] args)
        {
        new MainWindow();
        }

    private void createUIComponents()
        {
        // TODO: place custom component creation code here
        }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
    $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$()
    {
    mainPanel = new JPanel();
    mainPanel.setLayout(new GridBagLayout());
    panel = new JPanel();
    panel.setLayout(new GridBagLayout());
    panel.setMinimumSize(new Dimension(600, 241));
    panel.setPreferredSize(new Dimension(600, 539));
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 2.0;
    gbc.weighty = 1.0;
    mainPanel.add(panel, gbc);
    lblEclipseFolder = new JLabel();
    lblEclipseFolder.setText("Eclipse Folder:");
    lblEclipseFolder.setToolTipText("Specify the eclipse installation folder...");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.EAST;
    panel.add(lblEclipseFolder, gbc);
    txtEclipseFolder = new JTextField();
    txtEclipseFolder.setEditable(false);
    txtEclipseFolder.setEnabled(false);
    txtEclipseFolder.setToolTipText("The Eclipse installation folder...");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(txtEclipseFolder, gbc);
    btnEclipseFolder = new JButton();
    btnEclipseFolder.setText("Browse");
    btnEclipseFolder.setToolTipText("Click here to select the installation folder..");
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(btnEclipseFolder, gbc);
    lblGoal = new JLabel();
    lblGoal.setText("Target Plugins:");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.EAST;
    panel.add(lblGoal, gbc);
    lblTargetTag = new JLabel();
    lblTargetTag.setText("Target Tag:");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.EAST;
    panel.add(lblTargetTag, gbc);
    comboTargetTag = new JComboBox();
    comboTargetTag.setEditable(true);
    final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
    defaultComboBoxModel1.addElement("");
    defaultComboBoxModel1.addElement("artifactItem");
    defaultComboBoxModel1.addElement("dependency");
    comboTargetTag.setModel(defaultComboBoxModel1);
    comboTargetTag.setToolTipText("Choose the tag to embed the result in...");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(comboTargetTag, gbc);
    checkIncludeVersions = new JCheckBox();
    checkIncludeVersions.setSelected(true);
    checkIncludeVersions.setText("Include Version");
    checkIncludeVersions.setToolTipText("Check this to create a version tag for each item...");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 4;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    panel.add(checkIncludeVersions, gbc);
    checkIncludeSymbolics = new JCheckBox();
    checkIncludeSymbolics.setSelected(true);
    checkIncludeSymbolics.setText("Include Symbolic Names");
    checkIncludeSymbolics.setToolTipText(
            "Check this to generate a comment for each item which contains the bundle symbolic name...");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 5;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    panel.add(checkIncludeSymbolics, gbc);
    checkMarkOptional = new JCheckBox();
    checkMarkOptional.setText("Mark Optional");
    checkMarkOptional.setToolTipText("Check this if you want to mark the items optional...");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 6;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    panel.add(checkMarkOptional, gbc);
    panel_results = new JPanel();
    panel_results.setLayout(new BorderLayout(0, 0));
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 7;
    gbc.gridwidth = 4;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    panel.add(panel_results, gbc);
    outputscrollpane = new JScrollPane();
    panel_results.add(outputscrollpane, BorderLayout.CENTER);
    textAreaResult = new JTextArea();
    textAreaResult.setEditable(false);
    textAreaResult.setLineWrap(false);
    textAreaResult.setRows(20);
    outputscrollpane.setViewportView(textAreaResult);
    btnAnalyze = new JButton();
    btnAnalyze.setText("Generate");
    btnAnalyze.setToolTipText("Click here to start analyzer...");
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 6;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(btnAnalyze, gbc);
    btnCopyToClipboard = new JButton();
    btnCopyToClipboard.setText("Copy to Clipboard");
    btnCopyToClipboard.setToolTipText("Copies the contents of the result field into the clipboard...");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 8;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(btnCopyToClipboard, gbc);
    btnSaveToFile = new JButton();
    btnSaveToFile.setText("Save");
    btnSaveToFile.setToolTipText("Saves the content of the result field into a file...");
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 8;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(btnSaveToFile, gbc);
    goalPanel = new JPanel();
    goalPanel.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.BOTH;
    panel.add(goalPanel, gbc);
    radioGoalsPluginAll = new JRadioButton();
    radioGoalsPluginAll.setSelected(true);
    radioGoalsPluginAll.setText("Plugins and Sources");
    radioGoalsPluginAll.setToolTipText("Examine all plugins of the installation...");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    goalPanel.add(radioGoalsPluginAll, gbc);
    radioGoalsPlugins = new JRadioButton();
    radioGoalsPlugins.setText("Plugins");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    goalPanel.add(radioGoalsPlugins, gbc);
    radioGoalsPluginSources = new JRadioButton();
    radioGoalsPluginSources.setText("Sources");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    goalPanel.add(radioGoalsPluginSources, gbc);
    radioGoalsFeatureAll = new JRadioButton();
    radioGoalsFeatureAll.setText("Features and Sources");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    goalPanel.add(radioGoalsFeatureAll, gbc);
    radioGoalsFeatures = new JRadioButton();
    radioGoalsFeatures.setText("Features");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    goalPanel.add(radioGoalsFeatures, gbc);
    radioGoalsFeatureSources = new JRadioButton();
    radioGoalsFeatureSources.setText("Sources");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    goalPanel.add(radioGoalsFeatureSources, gbc);
    ButtonGroup buttonGroup;
    buttonGroup = new ButtonGroup();
    buttonGroup.add(radioGoalsPluginAll);
    buttonGroup.add(radioGoalsPlugins);
    buttonGroup.add(radioGoalsPluginSources);
    buttonGroup.add(radioGoalsFeatureAll);
    buttonGroup.add(radioGoalsFeatures);
    buttonGroup.add(radioGoalsFeatureSources);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
    return mainPanel;
    }
    }
