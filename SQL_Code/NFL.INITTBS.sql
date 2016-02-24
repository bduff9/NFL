create procedure NFL.INITTBS (inout USERNAME char(99), inout ACTIVATE char(1))
language sql
modifies sql data
begin
  declare WASACTIVATED char(1);
  declare CNT numeric(3);
  set CNT = 1;
  select ACTIVATED into WASACTIVATED from NFL.PLAYERS where USERID = USERNAME;
  if WASACTIVATED = 'N' and ACTIVATE = 'Y' then
    while CNT < 17 do
      insert into NFL.TIEBREAKER values (CNT, USERNAME, 0, 'N');
      set CNT = CNT + 1;
    end while;
  end if;
end