package com.github.zeemood.mail.plus.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 通用邮件实体类
 *
 * @author zeemoo
 * @date 2018/12/11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversalMail {

    private String uid;
    private String fromer;
    private String receiver;
    private String bcc;
    private String cc;
    private String folder;
    private Boolean hasRead;
    private Boolean hasAttachment;
    private Date sendDate;
    private String title;
    private String emlPath;
    private String content;
    private String email;
    private List<UniversalAttachment> attachments;
}
