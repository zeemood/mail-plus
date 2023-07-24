package com.github.zeemood.mail.plus.service.exception;

import lombok.NoArgsConstructor;

/**
 * 邮箱开发包自定义异常
 *
 * @author zeemoo
 * @date 2018/12/8
 */
@NoArgsConstructor
public class MailPlusException extends Exception {
    private static final long serialVersionUID = -8014572218613182580L;

    public MailPlusException(String message) {
        super(message);
    }
}
