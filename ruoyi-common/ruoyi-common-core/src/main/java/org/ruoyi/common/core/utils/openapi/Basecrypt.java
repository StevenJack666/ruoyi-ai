package org.ruoyi.common.core.utils.openapi;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.openapi.sm3.SM3Digest;

/**
 * 基础加解密库
 * 注：口令加密过程，如：
 * 明文：admin123
 * 密文：sm3(admin123) 得到16进制字符串hex，然后hash(hex)得到数据库存储密文。（sm3和hash下面的方法）
 */
@Slf4j
public class Basecrypt {

    /**
     * SM3 10000000次8秒,SM3加密长度32位(国密摘要)
     * 只用于口令存储，传输防篡改不适用SM3,避免其它客户端无法实现该算法引起问题
     * @param plain
     * @return
     * @throws Exception
     */
    public static byte[] sm3(byte[] plain){
        try{
            SM3Digest sm3 = new SM3Digest();
            sm3.update(plain, 0, plain.length);
            byte[] c3 = new byte[sm3.getDigestSize()];
            sm3.doFinal(c3, 0);

            return c3;
        }catch(Exception e){
            throw new RuntimeException("sm3 error",e);
        }
    }

    /**
     * sm3 摘要，生成16进制字符串
     * @param password
     * @return
     */
    public static String sm3(String password){
        byte[] result = sm3(StrUtil.bytes(password, CharsetUtil.CHARSET_UTF_8));
        return HexUtil.encodeHexStr(result);
    }

    /**
     * 加密口令(采用单向算法)
     * 注：这次两次hash避免字典库碰撞，加盐因子为自己
     * 注：前端到后端的请求不是原始密码，而是sm3摘要的16进制串（这里需要了解）
     * @param password 口令
     */
    public static String hash(String password) {
        try{
            String data = password + "{" + password + "}";
            byte[] result = Basecrypt.sm3(Basecrypt.sm3(data.getBytes()));
            return HexUtil.encodeHexStr(result);
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new RuntimeException("password encrypt error",e);
        }
    }

    public static void main(String[] args) {

        String str="4dff9c9ac07ebbc967493cc3874795802c04169462b2d2a026bc7e2a4beadc59";
        String hash = hash(str);
    }

}
