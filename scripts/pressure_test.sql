insert into orders values (541959,408677,'F',241827.84,'Clerk#000002574','test: check unique');
insert into orders values (541959,408677,'F',241827.84,'Clerk#000002574','test: check unique');

delete from orders where custkey > 190000;

delete from orders where custkey=150000;
insert into orders values (541959,408677,'F',241827.84,'Clerk#000002574','test: check unique');
insert into orders values (541959,408677,'F',241827.84,'Clerk#000002574','test: check unique');
select * from orders where orderstatus='F' and comments='test: check unique';
