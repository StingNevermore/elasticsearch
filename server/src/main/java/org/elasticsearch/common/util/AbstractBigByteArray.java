/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.util;

import org.elasticsearch.common.recycler.Recycler;

abstract class AbstractBigByteArray extends AbstractBigArray {

    protected byte[][] pages;

    protected AbstractBigByteArray(int pageSize, BigArrays bigArrays, boolean clearOnResize, long size) {
        super(pageSize, bigArrays, clearOnResize);
        this.size = size;
        pages = new byte[numPages(size)][];
        for (int i = 0; i < pages.length; ++i) {
            pages[i] = newBytePage(i);
        }
    }

    protected final byte[] newBytePage(int page) {
        if (recycler != null) {
            final Recycler.V<byte[]> v = recycler.bytePage(clearOnResize);
            return registerNewPage(v, page, PageCacheRecycler.BYTE_PAGE_SIZE);
        } else {
            return new byte[PageCacheRecycler.BYTE_PAGE_SIZE];
        }
    }

    /**
     * Fills an array with a value by copying it to itself, increasing copy ranges in each iteration
     */
    protected static void fillBySelfCopy(byte[] page, int fromBytes, int toBytes, int initialCopyBytes) {
        for (int pos = fromBytes + initialCopyBytes; pos < toBytes;) {
            int sourceBytesLength = pos - fromBytes; // source bytes available to be copied
            int copyBytesLength = Math.min(sourceBytesLength, toBytes - pos); // number of bytes to actually copy
            System.arraycopy(page, fromBytes, page, pos, copyBytesLength);
            pos += copyBytesLength;
        }
    }

    /**
     * Bulk copies array to paged array
     */
    protected void set(long index, byte[] buf, int offset, int len, byte[][] pages, int shift) {
        assert index + len <= size();
        int pageIndex = pageIndex(index);
        final int indexInPage = indexInPage(index);
        if (indexInPage + len <= pageSize()) {
            System.arraycopy(buf, offset << shift, pages[pageIndex], indexInPage << shift, len << shift);
        } else {
            int copyLen = pageSize() - indexInPage;
            System.arraycopy(buf, offset << shift, pages[pageIndex], indexInPage, copyLen << shift);
            do {
                ++pageIndex;
                offset += copyLen;
                len -= copyLen;
                copyLen = Math.min(len, pageSize());
                System.arraycopy(buf, offset << shift, pages[pageIndex], 0, copyLen << shift);
            } while (len > copyLen);
        }
    }
}
