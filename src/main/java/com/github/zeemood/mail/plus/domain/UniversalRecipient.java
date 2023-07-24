package com.github.zeemood.mail.plus.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用邮件收件人对象
 *
 * @author zeemoo
 * @date 2018/12/11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UniversalRecipient {
    private String name;
    private String email;
}
