////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2002  Oliver Burn
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////
package com.puppycrawl.tools.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.Utils;

/**
 * Abstract class for checks that verify the placement of a left curly
 * brace. It provides the support for setting the maximum line length and the
 * policy to verify. The default policy is EOL. It also provides the method to
 * verify the placement.
 *
 * @author <a href="mailto:checkstyle@puppycrawl.com">Oliver Burn</a>
 * @version 1.0
 */
public abstract class AbstractLeftCurlyCheck
    extends AbstractOptionCheck
{
    /** TODO: replace this ugly hack **/
    private int mMaxLineLength = 80;

    /**
     * Creates a default instance and sets the policy to EOL.
     */
    public AbstractLeftCurlyCheck()
    {
        super(LeftCurlyOption.EOL);
    }

    /**
     * Sets the maximum line length used in calculating the placement of the
     * left curly brace.
     * @param aMaxLineLength the max allowed line length
     */
    public void setMaxLineLength(int aMaxLineLength)
    {
        mMaxLineLength = aMaxLineLength;
    }

    /**
     * Verifies that a specified left curly brace is placed correctly
     * according to policy.
     * @param aBrace token for left curly brace
     * @param aStartToken token for start of expression
     */
    protected void verifyBrace(final DetailAST aBrace,
                               final DetailAST aStartToken)
    {
        final String braceLine = getLines()[aBrace.getLineNo() - 1];

        // calculate the previous line length without trailing whitespace. Need
        // to handle the case where there is no previous line, cause the line
        // being check is the first line in the file.
        final int prevLineLen = (aBrace.getLineNo() == 1)
            ? mMaxLineLength
            : Utils.lengthMinusTrailingWhitespace(
                getLines()[aBrace.getLineNo() - 2]);

        // Check for being told to ignore, or have '{}' which is a special case
        if ((braceLine.length() > (aBrace.getColumnNo() + 1))
            && (braceLine.charAt(aBrace.getColumnNo() + 1) == '}'))
        {
            ; // ignore
        }
        else if (getAbstractOption() == LeftCurlyOption.NL) {
            if (!Utils.whitespaceBefore(aBrace.getColumnNo(), braceLine)) {
                log(aBrace.getLineNo(), aBrace.getColumnNo(),
                    "line.new", "{");
            }
        }
        else if (getAbstractOption() == LeftCurlyOption.EOL) {
            if (Utils.whitespaceBefore(aBrace.getColumnNo(), braceLine)
                && ((prevLineLen + 2) <= mMaxLineLength))
            {
                log(aBrace.getLineNo(), aBrace.getColumnNo(),
                    "line.previous", "{");
            }
        }
        else if (getAbstractOption() == LeftCurlyOption.NLOW) {
            if (aStartToken.getLineNo() == aBrace.getLineNo()) {
                ; // all ok as on the same line
            }
            else if ((aStartToken.getLineNo() + 1) == aBrace.getLineNo()) {
                if (!Utils.whitespaceBefore(aBrace.getColumnNo(), braceLine)) {
                    log(aBrace.getLineNo(), aBrace.getColumnNo(),
                        "line.new", "{");
                }
                else if ((prevLineLen + 2) <= mMaxLineLength) {
                    log(aBrace.getLineNo(), aBrace.getColumnNo(),
                        "line.previous", "{");
                }
            }
            else if (!Utils.whitespaceBefore(aBrace.getColumnNo(), braceLine)) {
                log(aBrace.getLineNo(), aBrace.getColumnNo(),
                    "line.new", "{");
            }
        }
    }
}
