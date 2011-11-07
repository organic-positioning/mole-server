# From DrDobbs http://drdobbs.com/database/218100564

Create Procedure setupTmpLog()
BEGIN
     create temporary table if not exists tmplog (msg varchar(512)) engine = memory;
END;


Create Procedure doLog(in logMsg varchar(512))
BEGIN
  Declare continue handler for 1146 -- Table not found
  BEGIN
     call setupTmpLog();
     insert into tmplog values('resetup tmp table');
     insert into tmplog values(logMsg);
  END;

  insert into tmplog values(logMsg);
END;


Create Procedure saveAndLog(in thingId int, in lastMsg varchar(512))
BEGIN
   call dolog(lastMsg);
   insert into log(thingId, msg) (select thingId, msg from tmplog);
   truncate table tmplog;
END;
