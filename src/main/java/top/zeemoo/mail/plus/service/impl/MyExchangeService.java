package top.zeemoo.mail.plus.service.impl;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.WebProxy;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.enumeration.search.SortDirection;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.ItemSchema;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.credential.WebProxyCredentials;
import microsoft.exchange.webservices.data.search.FindFoldersResults;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import microsoft.exchange.webservices.data.search.FolderView;
import microsoft.exchange.webservices.data.search.ItemView;
import top.zeemoo.mail.plus.domain.MailConn;
import top.zeemoo.mail.plus.domain.MailConnCfg;
import top.zeemoo.mail.plus.domain.MailItem;
import top.zeemoo.mail.plus.domain.UniversalMail;
import top.zeemoo.mail.plus.service.IMailService;
import top.zeemoo.mail.plus.service.exception.MailPlusException;
import top.zeemoo.mail.plus.utils.MailItemParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Exchange服务
 *
 * @author zeemoo
 * @date 2019/01/18
 */
public class MyExchangeService implements IMailService {

    /**
     * 最大同步数量
     */
    private static final int MAX_SYNCHRO_SIZE = 80;

    /**
     * 解析邮件
     *
     * @param mailItem
     * @param localSavePath
     * @return
     * @throws MailPlusException
     */
    @Override
    public UniversalMail parseEmail(MailItem mailItem, String localSavePath) throws MailPlusException {
        return MailItemParser.parseMail(mailItem, localSavePath);
    }

    /**
     * 列举需要被同步的邮件
     *
     * @param mailConn
     * @param existUids
     * @return
     * @throws MailPlusException
     */
    @Override
    public List<MailItem> listAll(MailConn mailConn, List<String> existUids) throws MailPlusException {
        ExchangeService exchangeService = mailConn.getExchangeService();
        try {
            Folder msgFolderRoot = Folder.bind(exchangeService, WellKnownFolderName.MsgFolderRoot);
            int childFolderCount = msgFolderRoot.getChildFolderCount();
            FolderView folderView = new FolderView(childFolderCount);
            FindFoldersResults folders = msgFolderRoot.findFolders(folderView);
            ArrayList<Folder> folderList = folders.getFolders();
            List<MailItem> mailItems = new ArrayList<>();
            //判断是否达到一定数量的标志（使用双层循环）
            boolean flag = false;
            for (int i = 0; i < folderList.size(); i++) {
                Folder folder = folderList.get(i);
                String displayName = folder.getDisplayName();
                //排除已知的非邮件格式的文件夹（是EmailMessage类型但是不是标准邮件）
                if (
                        displayName.equals("Files")
                                || displayName.equals("文件")
                                || displayName.equals("檔案")
                                || displayName.equals("記事")
                        ) {
                    continue;
                }
                if (folder.getTotalCount() > 0) {
                    ItemView itemView = new ItemView(folder.getTotalCount());
                    itemView.getOrderBy().add(ItemSchema.DateTimeReceived, SortDirection.Descending);
                    FindItemsResults<Item> items = exchangeService.findItems(folder.getId(), itemView);
                    ArrayList<Item> itemList = items.getItems();
                    for (Item item :
                            itemList) {
                        if (item instanceof EmailMessage && !existUids.contains(item.getId().getUniqueId())) {
                            EmailMessage message = (EmailMessage) item;
                            mailItems.add(MailItem.builder().exchangeMessage(message).build());
                        }
                        flag = mailItems.size() >= MAX_SYNCHRO_SIZE;
                        if (flag) {
                            break;
                        }
                    }
                    if (flag) {
                        break;
                    }
                }
            }
            return mailItems;
        } catch (Exception e) {
            e.printStackTrace();
            throw new MailPlusException(e.getMessage());
        }
    }

    /**
     * 连接服务器
     *
     * @param mailConnCfg 连接配置
     * @param proxy       是否代理
     * @return 返回连接
     */
    @Override
    public MailConn createConn(MailConnCfg mailConnCfg, boolean proxy) throws MailPlusException {
        ExchangeService service = new ExchangeService();
        //配置代理
        if (proxy) {
            WebProxy webProxy = new WebProxy(
                    mailConnCfg.getHost()
                    , mailConnCfg.getProxyPort()
                    , new WebProxyCredentials(
                    mailConnCfg.getProxyUsername()
                    , mailConnCfg.getProxyPassword()
                    , ""
            )
            );
            service.setWebProxy(webProxy);
        }
        service.setCredentials(
                new WebCredentials(
                        mailConnCfg.getEmail()
                        , mailConnCfg.getPassword()
                )
        );
        //设置超时时间，在拉取邮件的时候保证不中断
        service.setTimeout(600000);
        try {
            if (mailConnCfg.isSsl()) {
                //我怀疑exchange的都是https方式
                service.autodiscoverUrl(mailConnCfg.getEmail(), redirectionUrl -> {
                    return redirectionUrl.toLowerCase().startsWith("https://");
                });
            } else {
                service.autodiscoverUrl(mailConnCfg.getEmail());
            }
            return MailConn.builder().exchangeService(service).build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new MailPlusException(e.getMessage());
        }
    }
}
