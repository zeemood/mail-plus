package com.github.zeemood.mail.plus.service.impl;

import com.github.zeemood.mail.plus.service.IMailService;
import com.github.zeemood.mail.plus.service.exception.MailPlusException;
import com.sun.mail.pop3.POP3Folder;
import com.sun.mail.pop3.POP3Message;
import com.sun.mail.pop3.POP3Store;
import com.github.zeemood.mail.plus.domain.MailConn;
import com.github.zeemood.mail.plus.domain.MailConnCfg;
import com.github.zeemood.mail.plus.domain.MailItem;
import com.github.zeemood.mail.plus.domain.UniversalMail;
import com.github.zeemood.mail.plus.enums.ProxyTypeEnum;
import com.github.zeemood.mail.plus.utils.MailItemParser;

import javax.mail.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * pop3协议邮箱收件类
 *
 * @author zeemoo
 * @date 2019/01/18
 */
public class Pop3Service implements IMailService {
    /**
     * Session properties的键名
     */
    private static final String PROPS_HOST = "mail.pop3.host";
    private static final String PROPS_PORT = "mail.pop3.port";
    private static final String PROPS_SSL = "mail.pop3.ssl.enable";
    private static final String PROPS_AUTH = "mail.pop3.auth";
    private static final String PROPS_SOCKS_PROXY_HOST = "mail.pop3.socks.host";
    private static final String PROPS_SOCKS_PROXY_PORT = "mail.pop3.socks.port";
    private static final String PROPS_HTTP_PROXY_HOST = "mail.pop3.proxy.host";
    private static final String PROPS_HTTP_PROXY_PORT = "mail.pop3.proxy.port";
    private static final String PROPS_HTTP_PROXY_USER = "mail.pop3.proxy.user";
    private static final String PROPS_HTTP_PROXY_PASSWORD = "mail.pop3.proxy.password";
    /**
     * POP3只能打开INBOX文件夹，也就是收件箱
     */
    private static final String FOLDER_INBOX = "INBOX";
    /**
     * 一次性最多能同步的数量
     */
    private static final int MAX_SYNCHRO_SIZE = 80;

    /**
     * 解析邮件
     *
     * @param mailItem      需要解析的邮件列表项
     * @param localSavePath 本地存储路径
     * @return
     * @throws MailPlusException
     */
    @Override
    public UniversalMail parseEmail(MailItem mailItem, String localSavePath) throws MailPlusException {
        //使用通用的邮件解析工具类解析邮件
        return MailItemParser.parseMail(mailItem, localSavePath);
    }

    /**
     * 列举需要被同步的邮件
     *
     * @param mailConn  邮箱连接，也可以做成字段
     * @param existUids 已存在的邮件uid
     * @return
     * @throws MailPlusException
     */
    @Override
    public List<MailItem> listAll(MailConn mailConn, List<String> existUids) throws MailPlusException {
        POP3Store pop3Store = mailConn.getPop3Store();
        try {
            //获取文件夹，POP3只能获取收件箱的邮件
            POP3Folder folder = (POP3Folder) pop3Store.getFolder(FOLDER_INBOX);
            //文件夹必须打开才可以获取邮件
            folder.open(Folder.READ_ONLY);
            Message[] messages = folder.getMessages();
            List<MailItem> mailItems = new ArrayList<>();
            //进行去重筛选需要同步的邮件
            for (int i = messages.length - 1; i >= 0; i--) {
                String uid = folder.getUID(messages[i]);
                if (!existUids.contains(uid)) {
                    POP3Message pop3Message = (POP3Message) messages[i];
                    mailItems.add(MailItem.builder().pop3Message(pop3Message).build());
                }
                //到一定数量停止
                if (mailItems.size() == MAX_SYNCHRO_SIZE) {
                    break;
                }
            }
            return mailItems;
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new MailPlusException(String.format("【POP3服务】打开文件夹/获取邮件列表失败，错误信息【{}】"));
        }
    }

    /**
     * 连接服务器
     *
     * @param mailConnCfg 连接配置
     * @param proxy       是否设置代理
     * @return 返回连接
     */
    @Override
    public MailConn createConn(MailConnCfg mailConnCfg, boolean proxy) throws MailPlusException {
        //构建Session Properties
        Properties properties = new Properties();
        properties.put(PROPS_HOST, mailConnCfg.getHost());
        properties.put(PROPS_PORT, mailConnCfg.getPort());
        properties.put(PROPS_SSL, mailConnCfg.isSsl());
        properties.put(PROPS_AUTH, true);

        //设置代理
        if (proxy && mailConnCfg.getProxyType() != null) {
            ProxyTypeEnum proxyType = mailConnCfg.getProxyType();
            if (proxyType.equals(ProxyTypeEnum.HTTP)) {
                properties.put(PROPS_HTTP_PROXY_HOST, mailConnCfg.getProxyHost());
                properties.put(PROPS_HTTP_PROXY_PORT, mailConnCfg.getProxyPort());
                properties.put(PROPS_HTTP_PROXY_USER, mailConnCfg.getProxyUsername());
                properties.put(PROPS_HTTP_PROXY_PASSWORD, mailConnCfg.getProxyPassword());
            } else if (proxyType.equals(ProxyTypeEnum.SOCKS)) {
                //java mail里socks代理是不支持用户名密码验证的
                properties.put(PROPS_SOCKS_PROXY_HOST, mailConnCfg.getSocksProxyHost());
                properties.put(PROPS_SOCKS_PROXY_PORT, mailConnCfg.getSocksProxyPort());
            }
        }
        //构建session
        Session session = Session.getInstance(properties);
        try {
            //连接
            Store store = session.getStore("pop3");
            store.connect(mailConnCfg.getEmail(), mailConnCfg.getPassword());
            return MailConn.builder().pop3Store((POP3Store) store).build();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
            throw new MailPlusException(e.getMessage());
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new MailPlusException(e.getMessage());
        }
    }
}
