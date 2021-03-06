
    create table APPLICATIONSETTINGS (
        id int8 not null,
        allowRegistrations boolean not null,
        announcement varchar(255),
        backgroundThreads int4 not null,
        crawlingPaused boolean not null,
        databaseUpdateThreads int4 not null,
        feedbackButton boolean not null,
        googleAnalyticsTrackingCode varchar(255),
        imageProxyEnabled boolean not null,
        publicUrl varchar(255),
        queryTimeout int4 not null,
        smtpHost varchar(255),
        smtpPassword varchar(255),
        smtpPort int4 not null,
        smtpTls boolean not null,
        smtpUserName varchar(255),
        primary key (id)
    );

    create table FEEDCATEGORIES (
        id int8 not null,
        collapsed boolean not null,
        name varchar(128) not null,
        position int4,
        parent_id int8,
        user_id int8 not null,
        primary key (id)
    );

    create table FEEDENTRIES (
        id int8 not null,
        author varchar(128),
        guid varchar(2048) not null,
        guidHash varchar(40) not null,
        inserted timestamp,
        updated timestamp,
        url varchar(2048) not null,
        content_id int8 not null,
        feed_id int8 not null,
        originalContent_id int8,
        primary key (id)
    );

    create table FEEDENTRYCONTENTS (
        id int8 not null,
        content text,
        enclosureType varchar(255),
        enclosureUrl varchar(2048),
        searchText text,
        title varchar(2048),
        primary key (id)
    );

    create table FEEDENTRYSTATUSES (
        id int8 not null,
        entryInserted timestamp,
        entryUpdated timestamp,
        read_status boolean,
        starred boolean not null,
        entry_id int8 not null,
        subscription_id int8 not null,
        user_id int8 not null,
        primary key (id)
    );

    create table FEEDS (
        id int8 not null,
        disabledUntil timestamp,
        errorCount int4 not null,
        etagHeader varchar(255),
        lastContentHash varchar(40),
        lastEntryDate timestamp,
        lastModifiedHeader varchar(64),
        lastPublishedDate timestamp,
        lastUpdateSuccess timestamp,
        lastUpdated timestamp,
        link varchar(2048),
        message varchar(1024),
        normalizedUrl varchar(2048),
        normalizedUrlHash varchar(40),
        url varchar(2048) not null,
        urlHash varchar(40) not null,
        primary key (id)
    );

    create table FEEDSUBSCRIPTIONS (
        id int8 not null,
        position int4,
        title varchar(128) not null,
        category_id int8,
        feed_id int8 not null,
        user_id int8 not null,
        primary key (id)
    );

    create table USERROLES (
        id int8 not null,
        roleName varchar(255) not null,
        user_id int8 not null,
        primary key (id)
    );

    create table USERS (
        id int8 not null,
        apiKey varchar(40),
        created timestamp,
        disabled boolean not null,
        email varchar(255),
        lastLogin timestamp,
        name varchar(32) not null,
        password bytea not null,
        recoverPasswordToken varchar(40),
        recoverPasswordTokenDate timestamp,
        salt bytea not null,
        primary key (id)
    );

    create table USERSETTINGS (
        id int8 not null,
        customCss text,
        user_lang varchar(4),
        readingMode varchar(255) not null,
        readingOrder varchar(255) not null,
        scrollMarks boolean not null,
        showRead boolean not null,
        socialButtons boolean not null,
        theme varchar(32),
        viewMode varchar(255) not null,
        user_id int8 not null,
        primary key (id)
    );

    alter table FEEDENTRIES 
        add constraint UK_meqpfqilqcjxfd9lwdhqo4ncg  unique (content_id);

    alter table FEEDENTRIES 
        add constraint UK_m5ro6vxmo0jynl3t2gf7hjaj6  unique (guidHash, feed_id, url);

    create index UK_bx7u9d4nyge272bjsaerca6j8 on FEEDENTRIES (feed_id, updated);

    create index UK_p5nmay0paseebxmlwgw9l8ufo on FEEDENTRIES (inserted);

    create index UK_ji594pog5wx9lvh9b73bbw5xt on FEEDENTRIES (updated);

    create index UK_tb5npe7leh9gos12r8wso9d5 on FEEDENTRYSTATUSES (subscription_id, entry_id);

    create index UK_avw2s5g0b5s7oocvvh9nc6ye on FEEDENTRYSTATUSES (subscription_id, read_status, entryUpdated);

    create index UK_oiog75k2bs9ob92t6kxk9irf7 on FEEDENTRYSTATUSES (user_id, read_status, entryUpdated);

    create index UK_luovps3dqgcvcxb26bie1e3c1 on FEEDENTRYSTATUSES (user_id, read_status, subscription_id);

    create index UK_ovvnv8yae726mkp2ccgmpr7ax on FEEDS (disabledUntil, lastUpdated);

    create index UK_jf9ce2w2mrmnb1uc4c6w6x6xq on FEEDS (lastUpdated);

    create index UK_br5ya6ovoro4v4d7xvchuabji on FEEDS (urlHash);

    create index UK_ju09vfqn1ekxei4n2gtemtu4j on FEEDS (normalizedUrlHash);

    create index UK_2cy3jecqr5dqimh3prsgto37s on FEEDS (lastContentHash);

    alter table USERS 
        add constraint UK_s7wep93120xdlalhu7mmiuj2h  unique (apiKey);

    alter table USERS 
        add constraint UK_avh1b2ec82audum2lyjx2p1ws  unique (email);

    alter table USERS 
        add constraint UK_kby09mn7e5oe95v5ykdm0c0lq  unique (name);

    alter table USERSETTINGS 
        add constraint UK_esotfv5fyogf38xpwgmiojv3f  unique (user_id);

    alter table FEEDCATEGORIES 
        add constraint FK_pvp3785abm8mbssomvls5uk2x 
        foreign key (parent_id) 
        references FEEDCATEGORIES;

    alter table FEEDCATEGORIES 
        add constraint FK_if0iww92y9qakaunluuke9u4d 
        foreign key (user_id) 
        references USERS;

    alter table FEEDENTRIES 
        add constraint FK_meqpfqilqcjxfd9lwdhqo4ncg 
        foreign key (content_id) 
        references FEEDENTRYCONTENTS;

    alter table FEEDENTRIES 
        add constraint FK_6hyvlrj242ajqvjsy25uc8h9d 
        foreign key (feed_id) 
        references FEEDS;

    alter table FEEDENTRIES 
        add constraint FK_keat5o9lup78w62xu86p328h1 
        foreign key (originalContent_id) 
        references FEEDENTRYCONTENTS;

    alter table FEEDENTRYSTATUSES 
        add constraint FK_nrv3nmfwdyex9aws8ri6ydqvr 
        foreign key (entry_id) 
        references FEEDENTRIES;

    alter table FEEDENTRYSTATUSES 
        add constraint FK_f48vit8hk72q2m921w7vrxc7k 
        foreign key (subscription_id) 
        references FEEDSUBSCRIPTIONS;

    alter table FEEDENTRYSTATUSES 
        add constraint FK_26rjrqay7qp7o3m6ygv8fr46y 
        foreign key (user_id) 
        references USERS;

    alter table FEEDSUBSCRIPTIONS 
        add constraint FK_t4o12t00vn4uryeee93qnm4yg 
        foreign key (category_id) 
        references FEEDCATEGORIES;

    alter table FEEDSUBSCRIPTIONS 
        add constraint FK_tlukons7kshi11274og0fvwc7 
        foreign key (feed_id) 
        references FEEDS;

    alter table FEEDSUBSCRIPTIONS 
        add constraint FK_649sh9xukf1mwkmdb4urip304 
        foreign key (user_id) 
        references USERS;

    alter table USERROLES 
        add constraint FK_nrrxoaoog5dxgn8c02rhaej60 
        foreign key (user_id) 
        references USERS;

    alter table USERSETTINGS 
        add constraint FK_esotfv5fyogf38xpwgmiojv3f 
        foreign key (user_id) 
        references USERS;

    create table hibernate_sequences (
         sequence_name varchar(255) not null ,
        sequence_next_hi_value int8,
        primary key ( sequence_name ) 
    );
