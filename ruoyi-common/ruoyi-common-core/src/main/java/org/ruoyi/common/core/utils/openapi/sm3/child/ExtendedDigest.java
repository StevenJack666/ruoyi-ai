package org.ruoyi.common.core.utils.openapi.sm3.child;

public abstract interface ExtendedDigest extends Digest
{
  /**
   * 获取字节长度
   * @return
   */
  public abstract int getByteLength();
}
