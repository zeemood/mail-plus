package com.github.zeemood.mail.plus.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用邮件附件类
 *
 * @author zeemoo
 * @date 2018/12/11
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class UniversalAttachment {
    private String path;
    private String cid;
    private String name;
    private String contentType;
}
