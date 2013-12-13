////////////////////////////////////////////////////////////////////////
//
//     Copyright (c) 2009-2013 Denim Group, Ltd.
//
//     The contents of this file are subject to the Mozilla Public License
//     Version 2.0 (the "License"); you may not use this file except in
//     compliance with the License. You may obtain a copy of the License at
//     http://www.mozilla.org/MPL/
//
//     Software distributed under the License is distributed on an "AS IS"
//     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//     License for the specific language governing rights and limitations
//     under the License.
//
//     The Original Code is ThreadFix.
//
//     The Initial Developer of the Original Code is Denim Group, Ltd.
//     Portions created by Denim Group, Ltd. are Copyright (C)
//     Denim Group, Ltd. All Rights Reserved.
//
//     Contributor(s): Denim Group, Ltd.
//
////////////////////////////////////////////////////////////////////////
package com.denimgroup.threadfix.plugins.intellij.toolwindow;

import com.denimgroup.threadfix.plugins.intellij.markers.WorkspaceUtils;
import com.denimgroup.threadfix.plugins.intellij.properties.Constants;
import com.denimgroup.threadfix.plugins.intellij.rest.VulnerabilityMarker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class VulnerabilitiesTableModel extends DefaultTableModel {

    public VulnerabilitiesTableModel() {
        super(initialObjects, headers);
        clear();
    }

    /////////////////////
    //       SETUP
    /////////////////////

    private static final String[][] initialObjects = new String[][] { VulnerabilityMarker.getHeaders() };
    private static final String[] headers = VulnerabilityMarker.getHeaders();
    private VirtualFile[] files;

    private Project project = null;

    VirtualFile getVirtualFileAt(int row) {
        return files[row];
    }

    public void setVirtualFileAt(int row, VirtualFile file) {
        files[row] = file;
    }

    public void initVirtualFiles(int size) {
        files = new VirtualFile[size];
    }

    public void clear() {
        setRowCount(0);
    }

    public void setProject(Project project) {
        this.project = project;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    /////////////////////
    // Action Handlers
    /////////////////////

    public void doAction(int cellRow, int cellColumn) {
        if (cellRow != -1 && cellColumn != -1) {
            switch (cellColumn) {
                case VulnerabilityMarker.CWE_ID_INDEX:
                case VulnerabilityMarker.CWE_TEXT_INDEX: openCwe(cellRow); break;
                case VulnerabilityMarker.DEFECT_URL_INDEX: openDefect(cellRow); break;
                default: openFile(cellRow);
            }
        }
    }

    void openFile(int cellRow) {
        VirtualFile file = getVirtualFileAt(cellRow);

        String stringLineNumber = getValueAt(cellRow, VulnerabilityMarker.LINE_NUMBER_INDEX).toString();

        int lineNumber = 0;

        if (stringLineNumber.matches("^[0-9]+$")) {
            try {
                lineNumber = Integer.valueOf(getValueAt(cellRow, VulnerabilityMarker.LINE_NUMBER_INDEX).toString());
            } catch (NumberFormatException e) {
                System.out.println("Got NumberFormatException for String " + stringLineNumber);
            }
        } else {
            System.out.println("Line number was not numeric.");
        }

        WorkspaceUtils.openFile(project, file, lineNumber);
    }

    void openCwe(int cellRow) {
        String address = Constants.CWE_ADDRESS_START + getValueAt(cellRow, VulnerabilityMarker.CWE_ID_INDEX);
        openAddressInWebBrowser(address);
    }

    void openDefect(int cellRow) {
        String address = getValueAt(cellRow, VulnerabilityMarker.DEFECT_URL_INDEX).toString();

        // TODO maybe more checking, but URI should throw IllegalArgumentException for problematic URLs
        if (address != null && !address.trim().equals("")) {
            openAddressInWebBrowser(address);
        }
    }

    void openAddressInWebBrowser(String address) {
        Desktop desktop = Desktop.getDesktop();

        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                URI uri = URI.create(address);
                desktop.browse(uri);
            } catch (IllegalArgumentException e) {
                System.out.println("Encountered IllegalArgumentException passing " + address + "to the operating system.");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("Encountered IOException trying to open " + address);
                e.printStackTrace();
            }
        } else {
            System.out.println("Desktop was null or browsing is not supported.");
        }
    }
}