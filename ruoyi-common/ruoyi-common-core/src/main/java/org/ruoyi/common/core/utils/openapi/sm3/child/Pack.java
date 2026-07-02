package org.ruoyi.common.core.utils.openapi.sm3.child;

public class Pack
{
  public static int bigEndianToInt(byte[] bs, int off)
  {
    int n = bs[off] << 24;
    n |= (bs[(++off)] & 0xFF) << 16;
    n |= (bs[(++off)] & 0xFF) << 8;
    n |= bs[(++off)] & 0xFF;
    return n;
  }

  public static void bigEndianToInt(byte[] bs, int off, int[] ns)
  {
    for (int i = 0; i < ns.length; i++)
    {
      ns[i] = bigEndianToInt(bs, off);
      off += 4;
    }
  }

  public static void intToBigEndian(int n, byte[] bs, int off)
  {
    bs[off] = ((byte)(n >>> 24));
    bs[(++off)] = ((byte)(n >>> 16));
    bs[(++off)] = ((byte)(n >>> 8));
    bs[(++off)] = ((byte)n);
  }

  public static void intToBigEndian(int[] ns, byte[] bs, int off)
  {
    for (int i = 0; i < ns.length; i++)
    {
      intToBigEndian(ns[i], bs, off);
      off += 4;
    }
  }

  public static long bigEndianToLong(byte[] bs, int off)
  {
    int hi = bigEndianToInt(bs, off);
    int lo = bigEndianToInt(bs, off + 4);
    return (hi & 0xFFFFFFFF) << 32 | lo & 0xFFFFFFFF;
  }

  public static void longToBigEndian(long n, byte[] bs, int off)
  {
    intToBigEndian((int)(n >>> 32), bs, off);
    intToBigEndian((int)(n & 0xFFFFFFFF), bs, off + 4);
  }

  public static int littleEndianToInt(byte[] bs, int off)
  {
    int n = bs[off] & 0xFF;
    n |= (bs[(++off)] & 0xFF) << 8;
    n |= (bs[(++off)] & 0xFF) << 16;
    n |= bs[(++off)] << 24;
    return n;
  }

  public static void littleEndianToInt(byte[] bs, int off, int[] ns)
  {
    for (int i = 0; i < ns.length; i++)
    {
      ns[i] = littleEndianToInt(bs, off);
      off += 4;
    }
  }

  public static void intToLittleEndian(int n, byte[] bs, int off)
  {
    bs[off] = ((byte)n);
    bs[(++off)] = ((byte)(n >>> 8));
    bs[(++off)] = ((byte)(n >>> 16));
    bs[(++off)] = ((byte)(n >>> 24));
  }

  public static void intToLittleEndian(int[] ns, byte[] bs, int off)
  {
    for (int i = 0; i < ns.length; i++)
    {
      intToLittleEndian(ns[i], bs, off);
      off += 4;
    }
  }

  public static long littleEndianToLong(byte[] bs, int off)
  {
    int lo = littleEndianToInt(bs, off);
    int hi = littleEndianToInt(bs, off + 4);
    return (hi & 0xFFFFFFFF) << 32 | lo & 0xFFFFFFFF;
  }

  public static void longToLittleEndian(long n, byte[] bs, int off)
  {
    intToLittleEndian((int)(n & 0xFFFFFFFF), bs, off);
    intToLittleEndian((int)(n >>> 32), bs, off + 4);
  }
}
