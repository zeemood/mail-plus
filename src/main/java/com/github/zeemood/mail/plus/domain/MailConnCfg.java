package com.github.zeemood.mail.plus.domain;

import com.github.zeemood.mail.plus.enums.ProxyTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮箱登录配置
 *
 * @author zeemoo
 * @date 2018/12/8
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailConnCfg {

    /**
     * 邮箱
     */
    private String email;
    /**
     * 密码
     */
    private String password;
    /**
     * 服务器地址
     */
    private String host;
    /**
     * 端口号
     */
    private Integer port;
    /**
     * 是否使用加密方式传输
     */
    private boolean ssl;

    /**
     * 代理类型
     */
    private ProxyTypeEnum proxyType;
    /**
     * HTTP代理地址
     */
    private String proxyHost;
    /**
     * HTTP代理端口
     */
    private Integer proxyPort;
    /**
     * HTTP代理用户名
     */
    private String proxyUsername;
    /**
     * HTTP代理密码
     */
    private String proxyPassword;
    /**
     * Socks代理端口
     */
    private String socksProxyHost;
    /**
     * socks代理端口
     */
    private Integer socksProxyPort;

    public MailConnCfg(String email, String password, String host, Integer port) {
        this.email = email;
        this.password = password;
        this.host = host;
        this.port = port;
    }
}
