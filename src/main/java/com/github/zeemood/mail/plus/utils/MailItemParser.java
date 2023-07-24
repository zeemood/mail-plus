package com.github.zeemood.mail.plus.utils;

import com.alibaba.fastjson.JSON;
import com.github.zeemood.mail.plus.domain.UniversalAttachment;
import com.github.zeemood.mail.plus.domain.UniversalRecipient;
import com.github.zeemood.mail.plus.service.exception.MailPlusException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.pop3.POP3Folder;
import com.sun.mail.pop3.POP3Message;
import com.vdurmont.emoji.EmojiParser;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.EmailMessageSchema;
import microsoft.exchange.webservices.data.core.service.schema.ItemSchema;
import microsoft.exchange.webservices.data.property.complex.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import com.github.zeemood.mail.plus.domain.MailItem;
import com.github.zeemood.mail.plus.domain.UniversalMail;

import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static jdk.nashorn.internal.codegen.OptimisticTypesPersistence.load;

/**
 * 通用邮件解析工具
 *
 * @author zeemoo
 * @date 2018/12/11
 */
public class MailItemParser {

    public static final String IMPORT_FOLDER = "手动导入";
    /**
     * eml文件后缀
     */
    private static final String EML_SUFFIX = ".eml";

    /**
     * 解析通用邮件内容
     *
     * @param mailItem
     * @param targetDir
     * @return
     */
    public static UniversalMail parseMail(MailItem mailItem, String targetDir) throws MailPlusException {
        UniversalMail universalMail = null;
        if (mailItem.getPop3Message() != null) {
            POP3Message pop3Message = mailItem.getPop3Message();
            universalMail = parseMimeMessage(pop3Message, targetDir);
            if (universalMail.getHasAttachment()) {
                List<UniversalAttachment> universalAttachments = parseAttachment(pop3Message, targetDir + "/" + universalMail.getUid());
                universalMail.setAttachments(universalAttachments);
            }
            String emlPath = saveMimiMessageAsLocalEml(pop3Message, targetDir + "/" + universalMail.getUid());
            universalMail.setEmlPath(emlPath);
        } else if (mailItem.getImapMessage() != null) {
            IMAPMessage imapMessage = mailItem.getImapMessage();
            universalMail = parseMimeMessage(imapMessage, targetDir);
            if (universalMail.getHasAttachment()) {
                List<UniversalAttachment> universalAttachments = parseAttachment(imapMessage, targetDir + "/" + universalMail.getUid());
                universalMail.setAttachments(universalAttachments);
            }
            String emlPath = saveMimiMessageAsLocalEml(imapMessage, targetDir + "/" + universalMail.getUid());
            universalMail.setEmlPath(emlPath);
        } else if (mailItem.getExchangeMessage() != null) {
            universalMail = parseExchangeMail(mailItem.getExchangeMessage());
            //解析附件
            if (universalMail.getHasAttachment()) {
                try {
                    EmailMessage exchangeMessage = mailItem.getExchangeMessage();
                    exchangeMessage.load(new PropertySet(EmailMessageSchema.MimeContent, EmailMessageSchema.AllowedResponseActions, EmailMessageSchema.Attachments));
                    List<UniversalAttachment> attachments = parseAttachment(exchangeMessage.getAttachments(), targetDir + "/" + universalMail.getUid());
                    universalMail.setAttachments(attachments);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new MailPlusException(e.getMessage());
                }
            }
            //保存到本地
            String emlPath = saveExchangeMailAsLocalEml(mailItem.getExchangeMessage(), targetDir + "/" + universalMail.getUid());
            universalMail.setEmlPath(emlPath);
        }
        return universalMail;
    }

    /**
     * 保存邮件到本地的eml中
     *
     * @param mimeMessage
     * @param targetDir
     * @return
     */
    public static String saveMimiMessageAsLocalEml(MimeMessage mimeMessage, String targetDir) throws MailPlusException {
        try {
            String subject = mimeMessage.getSubject();
            subject = StringUtils.isEmpty(subject) ? "无主题" + System.currentTimeMillis() : subject;
            File file = new File(targetDir.concat("/")
                    .concat(mimeMessage.getSession() == null ? RandomStringUtils.random(40, true, true) : DigestUtils.md5Hex(subject))
                    .concat(EML_SUFFIX));
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
                mimeMessage.writeTo(new FileOutputStream(file));
            }
            return file.getAbsolutePath();
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new MailPlusException(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new MailPlusException(e.getMessage());
        }
    }

    /**
     * 解析邮件附件
     *
     * @param mimeMessage
     * @param targetDir
     * @return
     */
    public static List<UniversalAttachment> parseAttachment(MimeMessage mimeMessage, String targetDir) throws MailPlusException {
        try {
            MimeMessageParser parser = new MimeMessageParser(mimeMessage).parse();
            List<UniversalAttachment> list = new ArrayList<>();

            File dir = new File(targetDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            List<DataSource> attachmentList = parser.getAttachmentList();
            Collection<String> contentIds = parser.getContentIds();
            Set<String> cidFile = new HashSet<>();
            for (String cid : contentIds) {
                DataSource attachmentByCid = parser.findAttachmentByCid(cid);
                File file = new File(targetDir + "/" + attachmentByCid.getName());
                if (!file.exists()) {
                    file.createNewFile();
                    FileUtils.copyInputStreamToFile(attachmentByCid.getInputStream(), file);
                }
                UniversalAttachment universalAttachment = UniversalAttachment.builder()
                        .cid(cid)
                        .path(file.getAbsolutePath())
                        .name(attachmentByCid.getName())
                        .contentType(attachmentByCid.getContentType())
                        .build();
                list.add(universalAttachment);
                cidFile.add(attachmentByCid.getName());
            }
            for (DataSource dataSource :
                    attachmentList) {
                if (cidFile.contains(dataSource.getName())) {
                    continue;
                }
                File file = new File(targetDir + "/" + dataSource.getName());
                if (!file.exists()) {
                    file.createNewFile();
                    FileUtils.copyInputStreamToFile(dataSource.getInputStream(), file);
                }
                UniversalAttachment universalAttachment = UniversalAttachment.builder()
                        .cid(null)
                        .path(file.getAbsolutePath())
                        .name(dataSource.getName())
                        .contentType(dataSource.getContentType())
                        .build();
                list.add(universalAttachment);
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            throw new MailPlusException(e.getMessage());
        }
    }

    /**
     * 解析mime邮件
     *
     * @param mimeMessage
     * @param targetDir
     * @return
     * @throws MailPlusException
     */
    public static UniversalMail parseMimeMessage(MimeMessage mimeMessage, String targetDir) throws MailPlusException {
        try {
            MimeMessageParser parser = new MimeMessageParser(mimeMessage).parse();
            javax.mail.Folder folder = mimeMessage.getFolder();
            String uid = "";
            //组装uid
            if (mimeMessage instanceof IMAPMessage) {
                uid = folder.getName() + ((IMAPFolder) folder).getUID(mimeMessage);
            } else if (mimeMessage instanceof POP3Message) {
                uid = ((POP3Folder) folder).getUID(mimeMessage);
            } else {
                uid = UUID.randomUUID().toString();
            }
            if (StringUtils.isEmpty(uid)) {
                throw new MailPlusException("【MIME邮件解析】解析uid失败");
            }
            String subject = parser.getSubject();
            String body = parser.hasHtmlContent() ? parser.getHtmlContent() : parser.getPlainContent();
            UniversalMail universalMail = UniversalMail.builder()
                    .content(StringUtils.isEmpty(body) ? "" : EmojiParser.parseToAliases(body))
                    .uid(uid)
                    .receiver(getMimeMessageAddressJson(parser.getTo()))
                    .title(StringUtils.isEmpty(subject) ? "<无主题>" : EmojiParser.parseToAliases(subject))
                    .sendDate(mimeMessage.getSentDate())
                    .hasRead(mimeMessage.getFlags().equals(Flags.Flag.SEEN))
                    .hasAttachment(parser.hasAttachments())
                    .fromer(parser.getFrom())
                    .folder(folder != null ? folder.getName() : IMPORT_FOLDER)
                    .cc(getMimeMessageAddressJson(parser.getCc()))
                    .bcc(getMimeMessageAddressJson(parser.getBcc()))
                    .build();
            return universalMail;
        } catch (Exception e) {
            e.printStackTrace();
            throw new MailPlusException(e.getMessage());
        }
    }

    /**
     * 获取JSON格式的邮件地址
     *
     * @param address
     * @return
     */
    public static String getMimeMessageAddressJson(List<Address> address) {
        List<UniversalRecipient> recipients = new ArrayList<>();
        for (int i = 0; i < address.size(); i++) {
            InternetAddress internetAddress = (InternetAddress) address.get(i);
            UniversalRecipient build = UniversalRecipient.builder()
                    .name(StringUtils.isNotEmpty(internetAddress.getPersonal()) ? EmojiParser.parseToAliases(internetAddress.getPersonal()) : internetAddress.getAddress())
                    .email(internetAddress.getAddress())
                    .build();
            recipients.add(build);
        }
        return JSON.toJSONString(recipients);
    }

    /**
     * 保存到本地，必须在解析完邮件之后再保存，不然会报错，加载的头不一样了
     *
     * @param exchangeMessage
     * @param targetDir
     * @return
     * @throws MailPlusException
     */
    public static String saveExchangeMailAsLocalEml(EmailMessage exchangeMessage, String targetDir) throws MailPlusException {
        try {
            File dir = new File(targetDir);
            if (dir.exists()) {
                dir.mkdirs();
            }
            exchangeMessage.load();
            String subject = exchangeMessage.getSubject();
            subject = StringUtils.isEmpty(subject) ? "<无主题>" + System.currentTimeMillis() : subject;
            File eml = new File(targetDir.concat("/").concat(DigestUtils.md5Hex(subject).concat(EML_SUFFIX)));
            exchangeMessage.load(
                    new PropertySet(
                            EmailMessageSchema.MimeContent
                            , EmailMessageSchema.AllowedResponseActions
                    )
            );
            if (!eml.exists()) {
                File parentFile = eml.getParentFile();
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }
                eml.createNewFile();
                byte[] content = exchangeMessage.getMimeContent().getContent();
                FileUtils.writeByteArrayToFile(eml, content);
            }
            return eml.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            throw new MailPlusException(e.getMessage());
        }
    }

    /**
     * 解析Exchange附件
     *
     * @param attachments
     * @param targetDir
     * @return
     */
    public static List<UniversalAttachment> parseAttachment(AttachmentCollection attachments, String targetDir) {
        List<UniversalAttachment> universalAttachments = new ArrayList<>();
        List<Attachment> items = attachments.getItems();
        File dir = new File(targetDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        for (Attachment attachment :
                items) {
            try {
                String path = targetDir + "/" + attachment.getName();
                File file = new File(path);
                if (!file.exists()) {
                    file.createNewFile();
                    if (attachment instanceof FileAttachment) {
                        ((FileAttachment) attachment).load(file.getAbsolutePath());
                    } else if (attachment instanceof ItemAttachment) {
                        ItemAttachment itemAttachment = (ItemAttachment) attachment;
                        itemAttachment.load(ItemSchema.MimeContent);
                        Item item = itemAttachment.getItem();
                        FileUtils.writeByteArrayToFile(file, item.getMimeContent().getContent());
                    }
                }
                universalAttachments.add(
                        UniversalAttachment.builder()
                                .cid(attachment.getContentId())
                                .contentType(attachment.getContentType())
                                .name(attachment.getName())
                                .path(path)
                                .build()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return universalAttachments;
    }

    /**
     * 解析exchange邮件
     *
     * @param message
     * @return
     */
    public static UniversalMail parseExchangeMail(EmailMessage message) throws MailPlusException {
        try {
            message.load();
            String subject = message.getSubject();
            EmailAddress from = message.getFrom();
            String body = message.getBody().toString();
            Date dateTimeSent = null;
            try {
                dateTimeSent = message.getDateTimeSent();
            } catch (ServiceLocalException e) {
                e.printStackTrace();
            }
            UniversalMail.UniversalMailBuilder builder = UniversalMail.builder()
                    .bcc(getExchangeAddressJson(message.getBccRecipients()))
                    .cc(getExchangeAddressJson(message.getCcRecipients()))
                    .folder(Folder.bind(message.getService(), message.getParentFolderId()).getDisplayName())
                    .fromer(from == null ? "<无发件人>" : from.getAddress())
                    .hasAttachment(message.getHasAttachments())
                    .hasRead(message.getIsRead())
                    .sendDate(dateTimeSent)
                    //处理emoji
                    .title(StringUtils.isAnyEmpty(message.getSubject()) ? "<无主题>" : EmojiParser.parseToAliases(subject))
                    .receiver(getExchangeAddressJson(message.getToRecipients())).uid(message.getId().getUniqueId())
                    .content(StringUtils.isEmpty(body) ? "" : EmojiParser.parseToAliases(body));
            UniversalMail universalMail = builder.build();
            return universalMail;
        } catch (ServiceLocalException e) {
            e.printStackTrace();
            throw new MailPlusException(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new MailPlusException(e.getMessage());
        }
    }

    /**
     * 解析exchange地址为键值对并转换成JSON字符串
     *
     * @param recipients
     * @return
     */
    public static String getExchangeAddressJson(EmailAddressCollection recipients) {
        List<EmailAddress> items = recipients.getItems();
        List<UniversalRecipient> list = new ArrayList<>();
        for (EmailAddress emailAddress :
                items) {
            list.add(
                    UniversalRecipient.builder()
                            .email(emailAddress.getAddress())
                            .name(StringUtils.isNotEmpty(emailAddress.getName()) ? EmojiParser.parseToAliases(emailAddress.getName()) : emailAddress.getAddress())
                            .build()
            );
        }
        return JSON.toJSONString(list);
    }
}
