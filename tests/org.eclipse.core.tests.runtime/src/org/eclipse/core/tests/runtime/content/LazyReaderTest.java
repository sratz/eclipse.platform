/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.content;

import java.io.*;
import junit.framework.*;
import org.eclipse.core.internal.content.LazyReader;

public class LazyReaderTest extends TestCase {

	/**
	 * Opens up protected methods from LazyInputStream.
	 */
	private static class OpenLazyReader extends LazyReader {

		public OpenLazyReader(Reader in, int blockCapacity) {
			super(in, blockCapacity);
		}

		public int getBlockCount() {
			return super.getBlockCount();
		}

		public int getBufferSize() {
			return super.getBufferSize();
		}

		public int getMark() {
			return super.getMark();
		}

		public int getOffset() {
			return super.getOffset();
		}
	}

	private final static String DATA = "012345678901234567890123456789";

	public LazyReaderTest(String name) {
		super(name);
	}

	public void testReadSingleChar() throws UnsupportedEncodingException, IOException {
		CharArrayReader underlying = new CharArrayReader(DATA.toCharArray());
		OpenLazyReader stream = new OpenLazyReader(underlying, 7);
		assertEquals("1.0", '0', stream.read());
		assertEquals("1.1", '1', stream.read());
		stream.skip(10);
		assertEquals("1.2", '2', stream.read());
		assertEquals("1.3", 13, stream.getOffset());
	}

	public void testReadBlock() throws UnsupportedEncodingException, IOException {
		CharArrayReader underlying = new CharArrayReader(DATA.toCharArray());
		OpenLazyReader stream = new OpenLazyReader(underlying, 7);
		stream.skip(4);
		char[] buffer = new char[7];
		int read = stream.read(buffer);
		assertEquals("1.0", buffer.length, read);
		assertEquals("1.1", DATA.substring(4, 4 + buffer.length), new String(buffer));
		assertEquals("1.2", 11, stream.getOffset());
		read = stream.read(buffer, 3, 4);
		assertEquals("2.0", 4, read);
		assertEquals("2.1", DATA.substring(11, 11 + read), new String(buffer, 3, read));
		assertEquals("2.2", 15, stream.getOffset());
		buffer = new char[100];
		read = stream.read(buffer);
		assertEquals("3.0", DATA.length() - 15, read);
		assertEquals("3.1", DATA.substring(15, 15 + read), new String(buffer, 0, read));
	}

	public void testMarkAndReset() throws UnsupportedEncodingException, IOException {
		CharArrayReader underlying = new CharArrayReader(DATA.toCharArray());
		OpenLazyReader stream = new OpenLazyReader(underlying, 7);
		assertTrue("0.1", stream.ready());
		stream.skip(13);
		assertTrue("0.2", stream.ready());
		stream.mark(0);
		assertEquals("2.0", 13, stream.getMark());
		assertEquals("2.1", '3', stream.read());
		assertEquals("2.2", '4', stream.read());
		assertEquals("2.3", 15, stream.getOffset());
		assertTrue("2.4", stream.ready());
		stream.reset();
		assertTrue("2.5", stream.ready());
		assertEquals("2.6", 13, stream.getOffset());
		assertEquals("2.7", 17, stream.skip(1000));
		assertFalse("2.8", stream.ready());
		stream.reset();
		assertEquals("2.9", 0, stream.getOffset());
		assertTrue("2.10", stream.ready());
	}

	public static Test suite() {
		return new TestSuite(LazyReaderTest.class);
	}
}
