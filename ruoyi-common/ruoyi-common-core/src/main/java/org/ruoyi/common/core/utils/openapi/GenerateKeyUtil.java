package org.ruoyi.common.core.utils.openapi;

import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.NoIvGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @Description:
 * @Author: xyn
 * @date: 2021/3/10 17:21
 */
@Slf4j
public class GenerateKeyUtil {

    public static final String SECURITY_KEY_ENCRYPTOR = "Lwk73+4cd7LV5g9Ef220=";
    /**
     *
     * @Description 生成appId和securityKey
     * @Author xyn
     * @param num 生成appId和securityKey的个数
     * @return
     */
    public static Map<String,String> generateAppIdAndSecurityKey(){
        Map<String,String> rstMap = new HashMap<String,String>();

        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setIvGenerator(new NoIvGenerator());
        encryptor.setAlgorithm("PBEWithMD5AndDES");
        encryptor.setPassword(GenerateKeyUtil.SECURITY_KEY_ENCRYPTOR);
        String appid=UUID.randomUUID().toString().replaceAll("-","");
        rstMap.put("appId",appid);
        System.out.println("appid:"+ appid);
        UUID uuid = UUID.randomUUID();
        String unSecurityKey = uuid.toString().replaceAll("-", "").substring(0,20);
        String encrypt = encryptor.encrypt(unSecurityKey).replaceAll("=","");
        System.out.println("securityKey原始值："+encrypt);
        rstMap.put("unSecurityKey",encrypt);
        //加密值用于保存数据库
        String securityKey = encryptor.encrypt(encrypt);
        System.out.println("securityKey加密值："+securityKey);
        rstMap.put("securityKey",securityKey);
        log.info("=======================================");
        return rstMap;
    }
    /**
     *
     * @Description 生成appId和securityKey
     * @Author xyn
     * @param num 生成appId和securityKey的个数
     * @return
     */
    public static void generateAppIdAndSecurityKey(int num){
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setIvGenerator(new NoIvGenerator());
        encryptor.setAlgorithm("PBEWithMD5AndDES");
        encryptor.setPassword(GenerateKeyUtil.SECURITY_KEY_ENCRYPTOR);

        for(int i = 0;i<num;i++){
            String appid=UUID.randomUUID().toString().replaceAll("-","");
//            log.info("appid:"+ appid);
            System.out.println("appid:"+ appid);
            UUID uuid = UUID.randomUUID();
            String securityKey = uuid.toString().replaceAll("-", "").substring(0,20);
            String encrypt = encryptor.encrypt(securityKey).replaceAll("=","");
//            log.info("securityKey原始值："+encrypt);
            System.out.println("securityKey原始值："+encrypt);
            //加密值用于保存数据库
            String securityKey1 = encryptor.encrypt(encrypt);
//            log.info("securityKey加密值："+securityKey1);
            System.out.println("securityKey加密值："+securityKey1);
            log.info("=======================================");
        }
    }


    /**
     *通过数据库原始key获取接入方key
     */
    public static String getInnerKey(String originKey){
        StandardPBEStringEncryptor standardPBEStringEncryptor = new StandardPBEStringEncryptor();
        standardPBEStringEncryptor.setIvGenerator(new NoIvGenerator());
        standardPBEStringEncryptor.setAlgorithm("PBEWithMD5AndDES");
        standardPBEStringEncryptor.setPassword(GenerateKeyUtil.SECURITY_KEY_ENCRYPTOR);
        //根据数据库key获取接入方sk
        return standardPBEStringEncryptor.decrypt(originKey);
    }
}
