create table student (
		sno char(8),
		sname char(16) unique,
		sage int,
		sgender char(1),
		primary key ( sno )
);
insert into student values ('12345678','wy',22,'M');
insert into student values ('00000001','ab',20,'M');
insert into student values ('00000002','bc',21,'F');

select sage from student where sname = 'ab';

create index stunameidx on student ( sname );

select * from student where sname = 'wy';

insert into student values ('00000101','aa',22,'F');
select * from student where sname = 'aa';

delete from student where sname = 'aa';
select * from student where sname = 'aa';

drop index stunameidx;
select * from student where sname = 'wy';
delete from student;

select * from student;

