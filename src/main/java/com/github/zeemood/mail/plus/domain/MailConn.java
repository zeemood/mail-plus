package com.github.zeemood.mail.plus.domain;

import com.sun.mail.imap.IMAPStore;
import com.sun.mail.pop3.POP3Store;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import microsoft.exchange.webservices.data.core.ExchangeService;

/**
 * 邮件服务器连接
 *
 * @author zeemoo
 * @date 2018/12/8
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailConn {

    private POP3Store pop3Store;
    private IMAPStore imapStore;
    private ExchangeService exchangeService;
}
