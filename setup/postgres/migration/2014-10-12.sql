alter table FEEDENTRIES add column originalContent_id int8;

    alter table FEEDENTRIES 
        add constraint FK_keat5o9lup78w62xu86p328h1 
        foreign key (originalContent_id) 
        references FEEDENTRYCONTENTS;
