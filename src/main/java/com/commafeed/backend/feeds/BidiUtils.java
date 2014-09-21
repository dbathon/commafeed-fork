/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.commafeed.backend.feeds;

import java.util.regex.Pattern;

/**
 * Adapted from com.google.gwt.i18n.shared.BidiUtils.
 * <p>
 * Utility functions for performing common Bidi tests on strings.
 */
public class BidiUtils {

  /**
   * A practical pattern to identify strong LTR characters. This pattern is not completely correct
   * according to the Unicode standard. It is simplified for performance and small code size.
   */
  private static final String LTR_CHARS =
      "A-Za-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02B8\u0300-\u0590\u0800-\u1FFF"
          + "\u2C00-\uFB1C\uFDFE-\uFE6F\uFEFD-\uFFFF";

  /**
   * A practical pattern to identify strong RTL characters. This pattern is not completely correct
   * according to the Unicode standard. It is simplified for performance and small code size.
   */
  private static final String RTL_CHARS = "\u0591-\u07FF\uFB1D-\uFDFD\uFE70-\uFEFC";

  /**
   * Regular expression to check if the first strongly directional character in a string is RTL.
   */
  private static final Pattern FIRST_STRONG_IS_RTL_RE = Pattern.compile("^[^" + LTR_CHARS + "]*["
      + RTL_CHARS + "]", Pattern.UNIX_LINES);

  /**
   * Regular expression to check if a string contains any LTR characters.
   */
  private static final Pattern HAS_ANY_LTR_RE = Pattern.compile("[" + LTR_CHARS + "]",
      Pattern.UNIX_LINES);

  /**
   * This constant defines the threshold of RTL directionality.
   */
  private static final float RTL_DETECTION_THRESHOLD = 0.40f;

  /**
   * Regular expression to split a string into "words" for directionality estimation based on
   * relative word counts.
   */
  private static final Pattern WORD_SEPARATOR_RE = Pattern.compile("\\s+", Pattern.UNIX_LINES);

  /**
   * Not instantiable.
   */
  private BidiUtils() {}

  public static boolean estimateIsRtl(String str) {
    int rtlCount = 0;
    int total = 0;
    for (final String token : WORD_SEPARATOR_RE.split(str)) {
      if (FIRST_STRONG_IS_RTL_RE.matcher(token).find()) {
        rtlCount++;
        total++;
      }
      else if (HAS_ANY_LTR_RE.matcher(token).find()) {
        total++;
      }
    }

    return total > 0 && ((float) rtlCount / total > RTL_DETECTION_THRESHOLD);
  }

}
