/**
 * Logback: the generic, reliable, fast and flexible logging framework.
 * 
 * Copyright (C) 1999-2008, QOS.ch
 * 
 * This library is free software, you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation.
 */
package ch.qos.logback.core.pattern;

public class SpacePadder {

  final static String[] SPACES = { " ", "  ", "    ", "        ", // 1,2,4,8
      // spaces
      "                ", // 16 spaces
      "                                " }; // 32 spaces

  final static public void leftPad(StringBuffer buf, String s, int desiredLength) {
    int actualLen = 0;
    if (s != null) {
      actualLen = s.length();
    }
    if (actualLen < desiredLength) {
      spacePad(buf, desiredLength - actualLen);
    }
    if (s != null) {
      buf.append(s);
    }
  }

  final static public void rightPad(StringBuffer buf, String s, int desiredLength) {
    int actualLen = 0;
    if (s != null) {
      actualLen = s.length();
    }
    if (s != null) {
      buf.append(s);
    }
    if (actualLen < desiredLength) {
      spacePad(buf, desiredLength - actualLen);
    }
  }
  
  /**
   * Fast space padding method.
   */
  final static public void spacePad(StringBuffer sbuf, int length) {
    while (length >= 32) {
      sbuf.append(SPACES[5]);
      length -= 32;
    }

    for (int i = 4; i >= 0; i--) {
      if ((length & (1 << i)) != 0) {
        sbuf.append(SPACES[i]);
      }
    }
  }
}