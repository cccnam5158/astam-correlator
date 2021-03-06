////////////////////////////////////////////////////////////////////////
//
//     Copyright (C) 2018 Applied Visions - http://securedecisions.com
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
//     This material is based on research sponsored by the Department of Homeland
//     Security (DHS) Science and Technology Directorate, Cyber Security Division
//     (DHS S&T/CSD) via contract number HHSP233201600058C.
//
//     Contributor(s):
//              Secure Decisions, a division of Applied Visions, Inc
//
////////////////////////////////////////////////////////////////////////

package com.denimgroup.threadfix.framework.impl.dotNet.classParsers;

import com.denimgroup.threadfix.framework.util.ScopeStringInterpolationDetector;
import com.denimgroup.threadfix.framework.util.ScopeTracker;

public class CSharpInterpolationDetector implements ScopeStringInterpolationDetector {

    private ScopeTracker scopeTracker;
    private boolean isInterpolatingString = false;
    private int lastToken = 0;
    private boolean isInInterpolatableString = false;

    CSharpInterpolationDetector(ScopeTracker forTracker) {
        scopeTracker = forTracker;
    }

    @Override
    public boolean isInterpolatingString() {
        return isInterpolatingString;
    }

    @Override
    public void parseToken(int token) {
        if (!scopeTracker.isInString()) {
            isInInterpolatableString = false;
            isInterpolatingString = false;
            if (!(lastToken == '$' && token == '@')) {
                lastToken = token;
            }
            return;
        }

        if (lastToken == '$' && scopeTracker.enteredString()) {
            isInInterpolatableString = true;
        }

        if (isInInterpolatableString) {
            if (token == '{') {
                isInterpolatingString = true;
            } else if (token == '}') {
                isInterpolatingString = false;
            }
        }

        //  Capture interpolatable string of format $@"..."
        if (lastToken == '$' && token == '@') {
            return;
        }

        lastToken = token;
    }
}
