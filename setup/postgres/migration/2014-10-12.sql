alter table FEEDENTRIES add column originalContent_id int8;

    alter table FEEDENTRIES 
        add constraint FK_keat5o9lup78w62xu86p328h1 
        foreign key (originalContent_id) 
        references FEEDENTRYCONTENTS;


alter table FEEDENTRIES alter column url set not null;

alter table FEEDENTRIES add feed_id int8;

-- this only works "correctly" if there is exactly one FEED_FEEDENTRIES row per FEEDENTRIES row...
update FEEDENTRIES fe
set feed_id = ffe.feed_id
from FEED_FEEDENTRIES ffe
where ffe.FEEDENTRY_ID = fe.id;

alter table FEEDENTRIES alter column feed_id set not null;

    alter table FEEDENTRIES 
        add constraint FK_6hyvlrj242ajqvjsy25uc8h9d 
        foreign key (feed_id) 
        references FEEDS;

drop table FEED_FEEDENTRIES;

drop index UK_rdbgxqktl8ribogimricm4xxt;

    alter table FEEDENTRIES 
        add constraint UK_m5ro6vxmo0jynl3t2gf7hjaj6  unique (guidHash, feed_id, url);

    create index UK_bx7u9d4nyge272bjsaerca6j8 on FEEDENTRIES (feed_id, updated);
