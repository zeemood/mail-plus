# POP3/IMAP4/Exchange协议的邮件收取工具包

> 最近在做一个邮件相关的项目，网上找资料时发现，关于发送邮件的资料倒是很多，收取邮件的就很少了。写下个人开发经验，供别人参考吧。具体的实施内容可以看我的慕课网手记
[https://www.imooc.com/article/274209](https://www.imooc.com/article/274209)
> 版本号已经修改成1.1.0。复制的时候注意一下就好（2023.07.24）

添加工具包可以使用maven的方式

首先添加仓库

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

然后添加依赖

```xml
<dependency>
    <groupId>com.github.zeemood</groupId>
    <artifactId>mail-plus</artifactId>
    <version>1.1.0</version>
</dependency>
```
