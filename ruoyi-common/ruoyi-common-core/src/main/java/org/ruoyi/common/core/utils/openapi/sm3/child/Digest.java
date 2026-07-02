package org.ruoyi.common.core.utils.openapi.sm3.child;

public abstract interface Digest
{

  /**
   * 获取算法名称
   * @return
   */
  public abstract String getAlgorithmName();

  /**
   * 获取digest的长度
   * @return
   */
  public abstract int getDigestSize();

  /**
   * 更新
   * @param paramByte
   */
  public abstract void update(byte paramByte);

  /**
   * 更新
   * @param paramArrayOfByte
   * @param paramInt1
   * @param paramInt2
   */
  public abstract void update(byte[] paramArrayOfByte, int paramInt1, int paramInt2);

  /**
   * 执行
   * @param paramArrayOfByte
   * @param paramInt
   * @return
   */
  public abstract int doFinal(byte[] paramArrayOfByte, int paramInt);

  /**
   * 重置
   */
  public abstract void reset();
}
