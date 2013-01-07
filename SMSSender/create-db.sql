use smssender;
set charset utf8;

drop table if exists `message_state`;
create table `message_state` (
  `id` int not null,
  `value` varchar(30),
  index `id_index` (`id`)
) engine=MyISAM default charset=utf8;

insert into `message_state` (`id`, `value`) values (0, 'Scheduled');
insert into `message_state` (`id`, `value`) values (1, 'Submited');
insert into `message_state` (`id`, `value`) values (2, 'Delivered');
insert into `message_state` (`id`, `value`) values (3, 'Replaced');
insert into `message_state` (`id`, `value`) values (4, 'Undelivered');

drop table if exists `short_message_state`;
create table `short_message_state` (
  `id` int not null,
  `value` varchar(30),
  index `id_index` (`id`)
) engine=MyISAM default charset=utf8;

insert into `short_message_state` (`id`, `value`) values (0, 'Submited');
insert into `short_message_state` (`id`, `value`) values (1, 'Delivered');
insert into `short_message_state` (`id`, `value`) values (2, 'Expired');
insert into `short_message_state` (`id`, `value`) values (3, 'Deleted');
insert into `short_message_state` (`id`, `value`) values (4, 'Undeliverable');
insert into `short_message_state` (`id`, `value`) values (5, 'Accepted');
insert into `short_message_state` (`id`, `value`) values (6, 'Unknown');
insert into `short_message_state` (`id`, `value`) values (7, 'Rejected');

drop table if exists `dispatching`;
create table `dispatching` (
  `id` binary(36) unique,
  `source_number` varchar(50) not null,
  `destination_number` varchar(50) not null,
  `message` varchar(999) not null,
  `message_type` int not null,
  `state` int default 0, /*scheduled*/
  `worker` int default null,
  `query_state` int default 0,
  primary key(`id`)
) engine=InnoDB default charset=utf8 auto_increment=1;

drop table if exists `message`;
create table `message` (
  `id` binary(36) unique,
  `session_id` binary(36) not null,
  `message_id` int not null,
  `message` varchar(160) not null,
  `state` int default 0, /*message submited*/
  `submit_timestamp` timestamp null,
  `delivery_timestamp` timestamp null
) engine=InnoDB default charset=utf8 auto_increment=1;

drop table if exists `received`;
create table `received` (
  `id` int not null auto_increment,
  `source_number` varchar(50) not null,
  `destination_number` varchar(50) not null,
  `message` varchar(999) not null,
  `timestamp` timestamp,
  primary key(`id`)
) engine = MyISAM default charset=utf8 auto_increment=1;

delimiter $$
drop function if exists schedule;
$$
create function schedule(p_source_number varchar(50),
                         p_destination_number varchar(50),
                         p_message varchar(999),
                         p_message_type int,
                         p_replace int)
returns int
begin
  declare v_id int;
  declare v_state int;

  if p_replace != 0 then
    select t.id, t.state into v_id, v_state
      from `dispatching` t
     where t.source_number = p_source_number
           and t.message_type = p_message_type
           and t.state in (0, 1, 3) /*scheduled, submited or replaced*/
           and t.destination_number = p_destination_number;

    if v_id is not null then
      if v_state = 1 then /*if submited*/
        set v_state = 3; /*changing to replaced*/
      end if;

      update `dispatching` t
        set t.message = p_message,
            t.state = v_state,
            t.query_state = 0 /*scheduled*/
       where t.id = v_id;

      return v_id;
    end if;
  end if;

  insert into `dispatching` (`id`, `source_number`, `destination_number`, `message`, `message_type`)
    values (uuid(), p_source_number, p_destination_number, p_message, p_message_type);

  return last_insert_id();
end
$$

drop procedure if exists query_messages;
$$
create procedure query_messages()
begin
	update `dispatching` t
	  set t.worker = connection_id(),
	      t.query_state = 1 /*locked*/
	 where t.query_state = 0
	 limit 30;

  drop table if exists `batch`;

  create temporary table `batch` (
    `id` binary(36),
    `source_number` varchar(50),
    `destination_number` varchar(50),
    `message` varchar(999),
    `state` int
  )
  engine=HEAP default charset=utf8
    select t.id,
           t.source_number,
           t.destination_number,
           t.message,
           t.state
      from `dispatching` t
     where t.worker = connection_id()
           and t.query_state = 1;

  update `dispatching` t
    set t.query_state = 2 /*dispatched*/
  where t.worker = connection_id()
        and t.query_state = 1;
end
$$

drop procedure if exists notify_received;
$$
create procedure notify_received(in p_source_number varchar(50),
                                 in p_destination_number varchar(50),
                                 in p_message varchar(160),
                                 in p_timestamp timestamp)
begin
  insert into `received` (`source_number`, `destination_number`, `message`, `timestamp`)
    values (p_source_number, p_destination_number, p_message, p_timestamp);
end
$$

drop procedure if exists submit_short_message;
$$
create procedure submit_short_message(in p_session_id binary(36),
                                      in p_message_id int,
                                      in p_message varchar(160),
                                      in p_timestamp timestamp)
begin
  insert into `message` (`id`, `session_id`, `message_id`, `message`, `submit_timestamp`)
    values (uuid(), p_session_id, p_message_id, p_message, p_timestamp);

  update `dispatching` t
    set t.state = 1  /*submited*/
   where t.id = p_session_id;
end
$$

drop procedure if exists replace_short_message;
$$
create procedure replace_short_message(in p_message_id int,
                                       in p_message varchar(160))
begin
  update `message` t
    set t.message = p_message
   where t.message_id = p_message_id;
end
$$

drop procedure if exists change_short_message_state;
$$
create procedure change_short_message_state(in p_message_id int,
                                            in p_timestamp timestamp,
                                            in p_state int,
                                            out p_session_id int)
begin
  update `message` t
    set t.state = p_state,
        t.delivery_timestamp = p_timestamp
   where t.message_id = p_message_id;

  select t.session_id into p_session_id from `message` t
    where t.message_id = p_message_id;
end
$$

drop procedure if exists change_message_state;
$$
create procedure change_message_state(in p_session_id int,
                                      in p_submit_timestamp timestamp,
                                      in p_delivery_timestamp timestamp,
                                      in p_state int)
begin
  update `dispatching` t
    set t.state = p_state
   where t.id = p_session_id;

   if p_state = 2 then /*delivered*/
     call sms_delivered(p_session_id);
   end if;
end
$$

delimiter ;