package com.github.zeemood.mail.plus.domain;

import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.pop3.POP3Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;

/**
 * 邮件同化类
 *
 * @author zeemoo
 * @date 2018/12/10
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MailItem {
    private IMAPMessage imapMessage;
    private POP3Message pop3Message;
    private EmailMessage exchangeMessage;
}
