/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.io;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A <code>BufferedInputStream</code> adds
 * functionality to another input stream-namely,
 * the ability to buffer the input and to
 * support the <code>mark</code> and <code>reset</code>
 * methods. When  the <code>BufferedInputStream</code>
 * is created, an internal buffer array is
 * created. As bytes  from the stream are read
 * or skipped, the internal buffer is refilled
 * as necessary  from the contained input stream,
 * many bytes at a time. The <code>mark</code>
 * operation  remembers a point in the input
 * stream and the <code>reset</code> operation
 * causes all the  bytes read since the most
 * recent <code>mark</code> operation to be
 * reread before new bytes are  taken from
 * the contained input stream.
 *
 * BufferedInputStream 是 Java I/O 库中为了提高数据读取效率而设计的一个装饰器类。
 * 其主要思路是减少直接与底层数据源（如磁盘、网络等）的交互次数，而代之以从内存缓冲区中读取数据。这样做的好处在于：
 * 减少系统调用：每次从磁盘或网络读取数据都涉及到系统调用，这些调用的开销是比较大的。通过预先读取更多的数据到内存缓冲区，可以减少这些调用的次数。
 * 利用内存的高速性能：相对于磁盘和网络，内存的读写速度要快得多。通过将数据缓存到内存中，可以大大加快数据的读取速度。
 * 支持 mark 和 reset：BufferedInputStream 还提供了标记和重置的功能，允许程序在流中向前和向后移动，这在直接从磁盘或网络流中是难以实现的。
 * 所以，当你知道将会进行大量的小读取操作时，使用 BufferedInputStream 是非常有益的，因为它可以大大减少实际的物理读取操作，从而提高整体的 I/O 性能。
 *
 * @author  Arthur van Hoff
 * @since   JDK1.0
 */
public
class BufferedInputStream extends FilterInputStream {

    private static int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    /**
     * The internal buffer array where the data is stored. When necessary,
     * it may be replaced by another array of
     * a different size.
     */
    protected volatile byte buf[];

    /**
     * Atomic updater to provide compareAndSet for buf. This is
     * necessary because closes can be asynchronous. We use nullness
     * of buf[] as primary indicator that this stream is closed. (The
     * "in" field is also nulled out on close.)
     * 原子更新器 JVM保证
     */
    private static final
        AtomicReferenceFieldUpdater<BufferedInputStream, byte[]> bufUpdater =
        AtomicReferenceFieldUpdater.newUpdater
        (BufferedInputStream.class,  byte[].class, "buf");

    /**
     * The index one greater than the index of the last valid byte in
     * the buffer.
     * This value is always
     * in the range <code>0</code> through <code>buf.length</code>;
     * elements <code>buf[0]</code>  through <code>buf[count-1]
     * </code>contain buffered input data obtained
     * from the underlying  input stream.
     */
    protected int count;

    /**
     * The current position in the buffer. This is the index of the next
     * character to be read from the <code>buf</code> array.
     * <p>
     * This value is always in the range <code>0</code>
     * through <code>count</code>. If it is less
     * than <code>count</code>, then  <code>buf[pos]</code>
     * is the next byte to be supplied as input;
     * if it is equal to <code>count</code>, then
     * the  next <code>read</code> or <code>skip</code>
     * operation will require more bytes to be
     * read from the contained  input stream.
     *
     * @see     java.io.BufferedInputStream#buf
     */
    protected int pos;

    /**
     * The value of the <code>pos</code> field at the time the last
     * <code>mark</code> method was called.
     * <p>
     * This value is always
     * in the range <code>-1</code> through <code>pos</code>.
     * If there is no marked position in  the input
     * stream, this field is <code>-1</code>. If
     * there is a marked position in the input
     * stream,  then <code>buf[markpos]</code>
     * is the first byte to be supplied as input
     * after a <code>reset</code> operation. If
     * <code>markpos</code> is not <code>-1</code>,
     * then all bytes from positions <code>buf[markpos]</code>
     * through  <code>buf[pos-1]</code> must remain
     * in the buffer array (though they may be
     * moved to  another place in the buffer array,
     * with suitable adjustments to the values
     * of <code>count</code>,  <code>pos</code>,
     * and <code>markpos</code>); they may not
     * be discarded unless and until the difference
     * between <code>pos</code> and <code>markpos</code>
     * exceeds <code>marklimit</code>.
     *
     * pos（位置指针）: 当你从流中读取数据时，pos指针表示你当前正在读取的数据在buf数组中的位置。每次读取一个字节，pos都会增加1。
     * markpos（标记位置）: 当你想在特定的位置"标记"一个点，以便稍后可以回到这个点并重新开始读取，你会使用mark方法。调用此方法时，当前的pos值会被存储到markpos中。
     * 想象以下场景：
     * 你正在从buf数组中读取数据，并且pos当前的值是5，表示你已经读取了前五个字节。
     * 现在，你决定标记这个位置，因为你认为可能需要再次从这个位置读取。所以，你调用了mark方法。此时，markpos的值设置为5，即当前的pos值。
     * 你继续从流中读取更多的数据。假设你读取了另外10个字节，现在pos的值是15。
     * 基于某种原因，你决定你需要回到之前标记的位置（也就是pos值为5的地方），所以你调用了reset方法。
     * reset方法会将pos的值设置回markpos的值，即5。这样，下次你从流中读取数据时，你会从你之前标记的位置开始，而不是从你最后停下的位置。
     * 这就是markpos的基本作用：它帮助你记住你在哪里放置了一个标记，以便你可以在将来回到那个位置并重新开始读取
     *
     *
     * @see     java.io.BufferedInputStream#mark(int)
     * @see     java.io.BufferedInputStream#pos
     */
    protected int markpos = -1;

    /**
     * The maximum read ahead allowed after a call to the
     * <code>mark</code> method before subsequent calls to the
     * <code>reset</code> method fail.
     * Whenever the difference between <code>pos</code>
     * and <code>markpos</code> exceeds <code>marklimit</code>,
     * then the  mark may be dropped by setting
     * <code>markpos</code> to <code>-1</code>.
     *
     * marklimit 不是用来控制从底层 InputStream 读取数据的速率的。它是用来指定在调用 mark() 方法后，
     * 你可以安全地从 BufferedInputStream 读取多少字节数据，而不使之前的标记失效。
     * 当你在 BufferedInputStream 上调用 mark(readlimit) 方法时，这意味着你想在当前位置设置一个标记，
     * 并希望在接下来的 readlimit 字节数内，都可以通过调用 reset() 来返回到标记的位置。如果在调用 mark() 之后读取的数据超过了 readlimit，则标记可能会失效。
     * 举个例子，假设你有一个 BufferedInputStream，其内部缓冲区大小为 100 字节。现在，你调用 mark(50)，
     * 表示你想在当前位置标记，并在接下来读取 50 字节数据后，仍然可以返回到标记的位置。但是，如果你读取了 51 字节或更多的数据，那么标记可能会失效，调用 reset() 可能会失败。
     * 重点是，marklimit 是用来控制从标记位置开始，可以安全地读取多少数据，而不失去返回到标记位置的能力。它并不控制从底层 InputStream 读取数据的速率。
     *
     * markpos 记录了标记的位置。
     * marklimit 记录了从标记位置开始可以安全读取的字节数。
     * 当从标记位置读取的字节数超过 marklimit 时，标记可能会失效。
     * 当调用 reset() 时，如果标记仍然有效，它会使用 markpos 来恢复到标记的位置。
     * @see     java.io.BufferedInputStream#mark(int)
     * @see     java.io.BufferedInputStream#reset()
     */
    protected int marklimit;

    /**
     * Check to make sure that underlying input stream has not been
     * nulled out due to close; if not return it;
     */
    private InputStream getInIfOpen() throws IOException {
        InputStream input = in;
        if (input == null)
            throw new IOException("Stream closed");
        return input;
    }

    /**
     * Check to make sure that buffer has not been nulled out due to
     * close; if not return it;
     */
    private byte[] getBufIfOpen() throws IOException {
        byte[] buffer = buf;
        if (buffer == null)
            throw new IOException("Stream closed");
        return buffer;
    }

    /**
     * Creates a <code>BufferedInputStream</code>
     * and saves its  argument, the input stream
     * <code>in</code>, for later use. An internal
     * buffer array is created and  stored in <code>buf</code>.
     *
     * @param   in   the underlying input stream.
     */
    public BufferedInputStream(InputStream in) {
        this(in, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Creates a <code>BufferedInputStream</code>
     * with the specified buffer size,
     * and saves its  argument, the input stream
     * <code>in</code>, for later use.  An internal
     * buffer array of length  <code>size</code>
     * is created and stored in <code>buf</code>.
     *
     * @param   in     the underlying input stream.
     * @param   size   the buffer size.
     * @exception IllegalArgumentException if {@code size <= 0}.
     */
    public BufferedInputStream(InputStream in, int size) {
        super(in);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
    }

    /**
     * Fills the buffer with more data, taking into account
     * shuffling and other tricks for dealing with marks.
     * Assumes that it is being called by a synchronized method.
     * This method also assumes that all data has already been read in,
     * hence pos > count.
     */
    private void fill() throws IOException {
        byte[] buffer = getBufIfOpen();
        if (markpos < 0) // 如果没有mark 就直接重置buf
            pos = 0;            /* no mark: throw away the buffer */
        else if (pos >= buffer.length)  /* no room left in buffer */  //已经读完了 对于使用bufferedInput而言
            if (markpos > 0) {  /* can throw away early part of the buffer */
                /**
                 * 当 markpos = 0 时，表示标记在缓冲区的起始位置。
                 * 因此，整个缓冲区的内容都可能需要被 reset() 方法重新读取，所以不能丢弃任何数据。
                 * 当 markpos > 0 时，表示标记在缓冲区的某个中间位置。
                 * 那么，从缓冲区的起始位置到 markpos-1 的数据是可以被丢弃的，因为这部分数据在 mark() 之前已经被读取了。但 markpos 位置及其之后的数据应该被保留。
                 */
                int sz = pos - markpos; // pos 一定会大于markpos 因为markpos是途中标记 获得离标记点的已读的长度
                System.arraycopy(buffer, markpos, buffer, 0, sz);// 重置buf 但把从标记点起已读的数据 复制到buf头
                pos = sz; //重新定位 但这里是从buf[0]开始
                markpos = 0; //markpos 是0 证明是打了标识的 和-1(NULL mark)又本质的区别
            } else if (buffer.length >= marklimit) { // 当这个 else if 被满足 markpos = 0(只有这么一个可能) 那么重置buf是最简洁的 因为不管保留不保留都是从0开始
                markpos = -1;   /* buffer got too big, invalidate mark */ // 置NULL Mark
                pos = 0;        /* drop buffer contents */ //重置buf
            } else if (buffer.length >= MAX_BUFFER_SIZE) { //当前buf已经达到最大容量
                throw new OutOfMemoryError("Required array size too large");
            } else {            /* grow buffer */
                /**
                 * 同时满足以下条件时buf 会执行扩容
                 * 已经读完了 pos大于length
                 * 没有打上mark(或者说mark 是0 也就是新buf)
                 * marklimit(这里你可以理解成你申请的大小已经超过了length) buffer.length已经不够了
                 */
                int nsz = (pos <= MAX_BUFFER_SIZE - pos) ? //如果 max_buffer_size还剩余的大小 大于pos的两倍
                        pos * 2 : MAX_BUFFER_SIZE; // 直接调整成 2 倍  ,不足2倍就直接 使用Max_buffer_size
                if (nsz > marklimit) // 如果计算出来的新大小 大于 marklimit(也就是标记有效的最大的限量) 那么就同步 。？？？？
                    nsz = marklimit;
                byte nbuf[] = new byte[nsz];
                System.arraycopy(buffer, 0, nbuf, 0, pos); //copy 数据到头
                if (!bufUpdater.compareAndSet(this, buffer, nbuf)) { //原子更新
                    // Can't replace buf if there was an async close.
                    // Note: This would need to be changed if fill()
                    // is ever made accessible to multiple threads.
                    // But for now, the only way CAS can fail is via close.
                    // assert buf == null;
                    throw new IOException("Stream closed");
                }
                buffer = nbuf;
            }
        count = pos;  // 记录旧的指针 也就是已读数据
        int n = getInIfOpen().read(buffer, pos, buffer.length - pos); //填满buf(也可以是新buf)
        if (n > 0)
            count = n + pos;
    }

    /**
     * See
     * the general contract of the <code>read</code>
     * method of <code>InputStream</code>.
     *
     * @return     the next byte of data, or <code>-1</code> if the end of the
     *             stream is reached.
     * @exception  IOException  if this input stream has been closed by
     *                          invoking its {@link #close()} method,
     *                          or an I/O error occurs.
     * @see        java.io.FilterInputStream#in
     */
    public synchronized int read() throws IOException {
        if (pos >= count) { //已经读完当前的
            fill();
            if (pos >= count) //整个inputStread全部被已读
                return -1;
        }
        return getBufIfOpen()[pos++] & 0xff;
    }

    /**
     * Read characters into a portion of an array, reading from the underlying stream at most once if necessary.
     */
    private int read1(byte[] b, int off, int len) throws IOException {
        int avail = count - pos; //还能从缓冲区读多少
        if (avail <= 0) { // 如果没有可读的了
            /* If the requested length is at least as large as the buffer, and
               if there is no mark/reset activity, do not bother to copy the
               bytes into the local buffer.  In this way buffered streams will
               cascade harmlessly. */
            if (len >= getBufIfOpen().length && markpos < 0) { //如果需要的长度大于缓存区大小 并且没有标记 那么就一次性从input中读出来
                return getInIfOpen().read(b, off, len);
            }
            fill();
            avail = count - pos;
            if (avail <= 0) return -1;
        }
        int cnt = (avail < len) ? avail : len;
        System.arraycopy(getBufIfOpen(), pos, b, off, cnt);
        pos += cnt;
        return cnt;
    }

    /**
     * Reads bytes from this byte-input stream into the specified byte array,
     * starting at the given offset.
     *
     * <p> This method implements the general contract of the corresponding
     * <code>{@link InputStream#read(byte[], int, int) read}</code> method of
     * the <code>{@link InputStream}</code> class.  As an additional
     * convenience, it attempts to read as many bytes as possible by repeatedly
     * invoking the <code>read</code> method of the underlying stream.  This
     * iterated <code>read</code> continues until one of the following
     * conditions becomes true: <ul>
     *
     *   <li> The specified number of bytes have been read,
     *
     *   <li> The <code>read</code> method of the underlying stream returns
     *   <code>-1</code>, indicating end-of-file, or
     *
     *   <li> The <code>available</code> method of the underlying stream
     *   returns zero, indicating that further input requests would block.
     *
     * </ul> If the first <code>read</code> on the underlying stream returns
     * <code>-1</code> to indicate end-of-file then this method returns
     * <code>-1</code>.  Otherwise this method returns the number of bytes
     * actually read.
     *
     * <p> Subclasses of this class are encouraged, but not required, to
     * attempt to read as many bytes as possible in the same fashion.
     *
     * @param      b     destination buffer.
     * @param      off   offset at which to start storing bytes.
     * @param      len   maximum number of bytes to read.
     * @return     the number of bytes read, or <code>-1</code> if the end of
     *             the stream has been reached.
     * @exception  IOException  if this input stream has been closed by
     *                          invoking its {@link #close()} method,
     *                          or an I/O error occurs.
     */
    public synchronized int read(byte b[], int off, int len)
        throws IOException
    {
        getBufIfOpen(); // Check for closed stream
        if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int n = 0;
        for (;;) {
            int nread = read1(b, off + n, len - n);
            if (nread <= 0)
                return (n == 0) ? nread : n;
            n += nread;
            if (n >= len)
                return n;
            // if not closed but no bytes available, return
            InputStream input = in;
            if (input != null && input.available() <= 0)
                return n;
        }
    }

    /**
     * See the general contract of the <code>skip</code>
     * method of <code>InputStream</code>.
     *
     * @exception  IOException  if the stream does not support seek,
     *                          or if this input stream has been closed by
     *                          invoking its {@link #close()} method, or an
     *                          I/O error occurs.
     */
    public synchronized long skip(long n) throws IOException {
        getBufIfOpen(); // Check for closed stream
        if (n <= 0) {
            return 0;
        }
        long avail = count - pos;

        if (avail <= 0) {
            // If no mark position set then don't keep in buffer
            if (markpos <0)
                return getInIfOpen().skip(n);

            // Fill in buffer to save bytes for reset
            fill();
            avail = count - pos;
            if (avail <= 0)
                return 0;
        }

        long skipped = (avail < n) ? avail : n;
        pos += skipped;
        return skipped;
    }

    /**
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream. The next invocation might be
     * the same thread or another thread.  A single read or skip of this
     * many bytes will not block, but may read or skip fewer bytes.
     * <p>
     * This method returns the sum of the number of bytes remaining to be read in
     * the buffer (<code>count&nbsp;- pos</code>) and the result of calling the
     * {@link java.io.FilterInputStream#in in}.available().
     *
     * @return     an estimate of the number of bytes that can be read (or skipped
     *             over) from this input stream without blocking.
     * @exception  IOException  if this input stream has been closed by
     *                          invoking its {@link #close()} method,
     *                          or an I/O error occurs.
     */
    public synchronized int available() throws IOException {
        int n = count - pos;
        int avail = getInIfOpen().available();
        return n > (Integer.MAX_VALUE - avail)
                    ? Integer.MAX_VALUE
                    : n + avail;
    }

    /**
     * See the general contract of the <code>mark</code>
     * method of <code>InputStream</code>.
     *
     * @param   readlimit   the maximum limit of bytes that can be read before
     *                      the mark position becomes invalid.
     * @see     java.io.BufferedInputStream#reset()
     */
    public synchronized void mark(int readlimit) {
        marklimit = readlimit;
        markpos = pos;
    }

    /**
     * See the general contract of the <code>reset</code>
     * method of <code>InputStream</code>.
     * <p>
     * If <code>markpos</code> is <code>-1</code>
     * (no mark has been set or the mark has been
     * invalidated), an <code>IOException</code>
     * is thrown. Otherwise, <code>pos</code> is
     * set equal to <code>markpos</code>.
     *
     * @exception  IOException  if this stream has not been marked or,
     *                  if the mark has been invalidated, or the stream
     *                  has been closed by invoking its {@link #close()}
     *                  method, or an I/O error occurs.
     * @see        java.io.BufferedInputStream#mark(int)
     */
    public synchronized void reset() throws IOException {
        getBufIfOpen(); // Cause exception if closed
        if (markpos < 0)
            throw new IOException("Resetting to invalid mark");
        pos = markpos;
    }

    /**
     * Tests if this input stream supports the <code>mark</code>
     * and <code>reset</code> methods. The <code>markSupported</code>
     * method of <code>BufferedInputStream</code> returns
     * <code>true</code>.
     *
     * @return  a <code>boolean</code> indicating if this stream type supports
     *          the <code>mark</code> and <code>reset</code> methods.
     * @see     java.io.InputStream#mark(int)
     * @see     java.io.InputStream#reset()
     */
    public boolean markSupported() {
        return true;
    }

    /**
     * Closes this input stream and releases any system resources
     * associated with the stream.
     * Once the stream has been closed, further read(), available(), reset(),
     * or skip() invocations will throw an IOException.
     * Closing a previously closed stream has no effect.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public void close() throws IOException {
        byte[] buffer;
        while ( (buffer = buf) != null) {
            if (bufUpdater.compareAndSet(this, buffer, null)) {
                InputStream input = in;
                in = null;
                if (input != null)
                    input.close();
                return;
            }
            // Else retry in case a new buf was CASed in fill()
        }
    }
}
