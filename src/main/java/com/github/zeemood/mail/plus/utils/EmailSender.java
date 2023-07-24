package com.github.zeemood.mail.plus.utils;

import com.github.zeemood.mail.plus.service.exception.MailPlusException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;

/**
 * 邮件发送工具类
 *
 * @author zeemoo
 * @date 2019/1/8
 */
public class EmailSender {

    /**
     * 发送邮件
     *
     * @param username
     * @param password
     * @param title
     * @param receiver
     * @param content
     * @param mailSploitName
     * @param mailSploitEmail
     * @param file
     * @param host
     * @param port
     */
    public static void sendEmailWithAttachment(
            String username, String password
            , String title, String receiver
            , String content, String mailSploitName
            , String mailSploitEmail, File file
            , String host, Integer port
            , String socksProxyHost, Integer socksProxyPort
    ) throws MailPlusException {
        try {
            HtmlEmail multiPartEmail = new HtmlEmail();
            multiPartEmail.setBoolHasAttachments(true);
            multiPartEmail.setAuthentication(username, password);
            multiPartEmail.setCharset("UTF-8");
            multiPartEmail.addTo(receiver);
            multiPartEmail.setSmtpPort(port);
            multiPartEmail.setHostName(host);
            multiPartEmail.setSSLOnConnect(true);
            multiPartEmail.setSentDate(new Date());
            multiPartEmail.setSubject(title);
            multiPartEmail.setHtmlMsg(content);

            if (StringUtils.isAnyEmpty(mailSploitEmail, mailSploitName)) {
                multiPartEmail.setFrom(username);
            } else {
                Base64.Encoder encoder = Base64.getEncoder();
                multiPartEmail.setFrom(username, "=?utf-8?b?" + encoder.encodeToString(mailSploitName.getBytes("utf-8")) + "?==?utf-8?Q?=00?==?utf-8?b?" + encoder.encodeToString(String.format("(%s)", mailSploitEmail).getBytes("utf-8")) + "?=" + String.format("<%s>", mailSploitEmail));
                multiPartEmail.addReplyTo(mailSploitEmail);
            }
            multiPartEmail.attach(file);
            Properties properties = multiPartEmail.getMailSession().getProperties();
            properties.setProperty("mail.smtp.socks.host", socksProxyHost);
            properties.put("mail.smtp.socks.port", socksProxyPort);
            multiPartEmail.send();
        } catch (EmailException e) {
            e.printStackTrace();
            throw new MailPlusException(String.format("【发送邮件】失败，原始信息:【%s】", e.getMessage()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
