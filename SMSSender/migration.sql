/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@foreign_key_checks, foreign_key_checks = 0 */;

SET NAMES 'utf8';

SET GLOBAL log_bin_trust_function_creators = 1;

USE smssender2;

DROP TABLE IF EXISTS sequence;
CREATE TABLE sequence (
	id int(11) not null,
	name varchar(255),
	value int(11) not null
) ENGINE = INNODB;

DROP TABLE IF EXISTS `state#short_message`;
CREATE TABLE `state#short_message` (
  id int(11) NOT NULL,
  value varchar(30) DEFAULT NULL,

  constraint `pk_state#short_message` primary key(id)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 3276
CHARACTER SET utf8
COLLATE utf8_general_ci;

INSERT INTO `state#short_message`(id, value) values (1, 'Submitting');
INSERT INTO `state#short_message`(id, value) values (2, 'Submitted');
INSERT INTO `state#short_message`(id, value) values (3, 'Cancelling');
INSERT INTO `state#short_message`(id, value) values (4, 'Canceled');
INSERT INTO `state#short_message`(id, value) values (7, 'Delivered');
INSERT INTO `state#short_message`(id, value) values (8, 'Expired');
INSERT INTO `state#short_message`(id, value) values (9, 'Deleted');
INSERT INTO `state#short_message`(id, value) values (10, 'Undeliverable');
INSERT INTO `state#short_message`(id, value) values (11, 'Accepted');
INSERT INTO `state#short_message`(id, value) values (12, 'Rejected');
INSERT INTO `state#short_message`(id, value) values (13, 'Unknown');
COMMIT;

DROP TABLE IF EXISTS `short_message`;
CREATE TABLE `short_message` (
  id int(11) not null,
	dispatching_uid char(36) NOT NULL,
	sequence int(11) not null,
	state   int(11) not null,
	message_id int(11) null,
	end_position int(11) not null,
  smpp_status int(11) NULL,

	constraint `fk_short_message#dispatching_uid` foreign key(dispatching_uid) references `dispatching`(uid),
	constraint `fk_short_message#state` foreign key(`state`) references `state#short_message`(id),

  index `ind_short_message#id` (id),
	index `ind_short_message#dispatching_uid` (dispatching_uid),
	index `ind_short_message#dispatching_uid#sequence` (dispatching_uid, sequence)
) ENGINE = INNODB;

DROP TABLE IF EXISTS short_message_state;
CREATE TABLE `short_message_state` (
  id  int(11) not null,
	state int(11) not null,
	message_id int(11) null,
  smpp_status int(11) null,
  `timestamp` timestamp DEFAULT CURRENT_TIMESTAMP,

	constraint `fk_short_message_state#id` foreign key(`id`) references `short_message`(id),
	constraint `fk_short_message_state#state` foreign key(`state`) references `state#short_message`(id)
) ENGINE = INNODB;


alter table dispatching add current_seq int(11);
alter table dispatching add total_messages int(11);
alter table dispatching add messages_submitted int(11);
alter table dispatching add messages_delivered int(11);

update dispatching set current_seq = 0;
COMMIT;

SET @seq = 0;
insert into short_message(`id`, dispatching_uid, sequence, state, message_id, end_position)
  SELECT (@seq := @seq + 1) AS `id`, d.uid, 0, CASE d.state when 5 then 3 WHEN 6 THEN 4 ELSE d.state END, d.message_id, char_length(d.message) - 1 from dispatching d;
COMMIT;

insert into short_message_state(id, state, message_id, smpp_status, timestamp)
	select sm.id, CASE d.state when 5 then 3 WHEN 6 THEN 4 ELSE d.state END, ds.message_id, ds.smpp_status, ds.`timestamp` from dispatching_state ds
    LEFT JOIN short_message sm ON sm.dispatching_uid = ds.uid
   WHERE ds.state != 0;
COMMIT;

SET @seq := 0;
select max(id) + 1 INTO @seq from short_message;
INSERT INTO sequence (id, name, value)
  VALUES (1, 'Short message sequence', @seq);
COMMIT;

alter table dispatching drop message_id;

alter table dispatching_state drop message_id;
alter table dispatching_state drop smpp_status;
alter table dispatching_state drop smpp_timestamp;

delimiter $$

drop function if exists `get_sequence_next_value`;
create function `get_sequence_next_value`(p_sequence_id int(11))
returns int(11)
begin
    declare v_value int(11);

    update sequence s
      set s.value = s.value + 1
     where s.id = p_sequence_id;

    select s.value into v_value
      from sequence s
     where s.id = p_sequence_id;

    return v_value;
end
$$

DROP PROCEDURE IF EXISTS change_operation_state$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE change_operation_state(in p_uid            char(36),
                                 in p_state          int,
                                 in p_total_messages int,
                                 in p_timestamp      timestamp)
BEGIN
  if p_state = 2 then -- Submitted
    UPDATE dispatching d
      SET d.state = p_state,
          d.total_messages = p_total_messages,
          d.messages_submitted = 0,
          d.messages_delivered = 0
     WHERE d.uid = p_uid;
  elseif p_state = 6 then -- Canceled To Replace
    UPDATE dispatching d
      SET d.state = 0,
          d.worker = NULL,
          d.query_state = 0,
          d.current_seq = d.current_seq + 1,
          d.messages_submitted = 0,
          d.messages_delivered = 0
     WHERE d.uid = p_uid;
  else
    UPDATE dispatching d
      SET d.state = p_state
     WHERE d.uid = p_uid;
  end if;

  INSERT INTO dispatching_state (uid, state, `timestamp`)
    VALUES (p_uid, p_state, p_timestamp);
end
$$

DROP PROCEDURE IF EXISTS `short_message#on_submitting`$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE `short_message#on_submitting`(in p_id            int(11),
                                        in p_operation_uid char(36),
                                        in p_sequence      int(11),
                                        in p_end_position  int(11),
                                        in p_timestamp     timestamp)
BEGIN
  -- submitting state
  insert into short_message(`id`, dispatching_uid, sequence, state, end_position)
    values(p_id, p_operation_uid, p_sequence, 1, p_end_position);

  insert into short_message_state(`id`, state, `timestamp`) values(p_id, 1, p_timestamp);
end
$$

DROP PROCEDURE IF EXISTS `short_message#on_submitted`$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE `short_message#on_submitted`(in p_id          int(11),
                                       in p_message_id  int(11),
                                       in p_smpp_status int(11),
                                       in p_timestamp   timestamp)
BEGIN
  declare v_operation_uid char(36);
  declare v_short_message_current_sequence int(11);
  declare v_current_sequence int(11);

  select sm.dispatching_uid, sm.sequence into v_operation_uid, v_short_message_current_sequence
    from short_message sm
   where sm.`id` = p_id;

  select d.current_seq into v_current_sequence
    from dispatching d
   where d.uid = v_operation_uid;

  update short_message sm
    set sm.state = 2, -- Submitted
        sm.message_id = p_message_id,
        sm.smpp_status = p_smpp_status
   where sm.`id` = p_id;

  if v_short_message_current_sequence = v_current_sequence and p_smpp_status = 0 then
    update dispatching d
      set d.messages_submitted = d.messages_submitted + 1
     where d.uid = v_operation_uid;
  end if;

  insert into short_message_state(`id`, message_id, state, smpp_status, `timestamp`)
    values(p_id, p_message_id, 2, p_smpp_status, p_timestamp);
end
$$

DROP PROCEDURE IF EXISTS `short_message#on_cancelling`$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE `short_message#on_cancelling`(in p_id          int(11),
                                        in p_timestamp   timestamp)
BEGIN
  declare v_message_id int(11);

  select sm.message_id into v_message_id
    from short_message sm
   where sm.`id` = p_id;

  update short_message sm
    set sm.state = 3 -- Cancelling
   where sm.`id` = p_id;

  insert into short_message_state(`id`, message_id, state, `timestamp`)
    values(p_id, v_message_id, 3, p_timestamp);
end
$$

DROP PROCEDURE IF EXISTS `short_message#on_cancelled`$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE `short_message#on_cancelled`(in p_id          int(11),
                                       in p_smpp_status int(11),
                                       in p_timestamp   timestamp)
BEGIN
  declare v_message_id int(11);

  select sm.message_id into v_message_id
    from short_message sm
   where sm.`id` = p_id;

  update short_message sm
    set sm.state = 4, -- Cancelled
        sm.smpp_status = p_smpp_status
   where sm.`id` = p_id;

  insert into short_message_state(`id`, message_id, state, smpp_status, `timestamp`)
    values(p_id, v_message_id, 4, p_smpp_status, p_timestamp);
end
$$

DROP PROCEDURE IF EXISTS `short_message#on_delivered`$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE `short_message#on_delivered`(in p_message_id  int(11),
                                       in p_state       int(11),
                                       in p_timestamp   timestamp)
BEGIN
  declare v_operation_uid char(36);
  declare v_short_message_id int(11);
  declare v_short_message_current_sequence int(11);
  declare v_current_sequence int(11);
  declare v_total_messages int(11);
  declare v_messages_delivered int(11);
  declare v_message_id int(11);

  select sm.dispatching_uid, sm.`id`, sm.message_id, sm.sequence into v_operation_uid, v_short_message_id, v_message_id, v_short_message_current_sequence
    from short_message sm
   where sm.message_id = p_message_id;

  select d.current_seq, d.total_messages, d.messages_delivered into v_current_sequence, v_total_messages, v_messages_delivered
    from dispatching d
   where d.uid = v_operation_uid;

  update short_message sm
    set sm.state = p_state,
        sm.smpp_status = null
   where sm.`id` = v_short_message_id;

  if v_short_message_current_sequence = v_current_sequence and p_state = 7 then
     if v_messages_delivered = v_total_messages - 1 then
       update dispatching d
         set d.messages_delivered = d.messages_delivered + 1,
             d.state = 7 -- Delivered
        where d.uid = v_operation_uid;

        call sms_delivered(v_operation_uid);
     else
       update dispatching d
         set d.messages_delivered = d.messages_delivered + 1
        where d.uid = v_operation_uid;
     end if;
  end if;

  insert into short_message_state(`id`, message_id, state, `timestamp`)
    values(v_short_message_id, v_message_id, p_state, p_timestamp);
end
$$

DROP PROCEDURE IF EXISTS `schedule_operation`$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE `schedule_operation`(p_operation_type_id int(11),
                               p_source_number varchar(50),
                               p_source_number_ton int(11),
                               p_source_number_npi int(11),
                               p_destination_number varchar(50),
                               p_message text,
                               p_service_type varchar(6),
                               p_service_id int(11),
                               p_uid char(36))
root:BEGIN
  DECLARE v_uid char(36);

  if p_operation_type_id = 10 then -- replace
    select t.uid into v_uid
    from `dispatching` t
    where t.destination_number = p_destination_number
      and t.service_id = p_service_id
      and ((t.state = 0) or (t.state = 2 AND t.messages_submitted > 0 and t.total_messages != t.messages_delivered))
    order by `timestamp` DESC LIMIT 1;

    if v_uid is not null then
      update `dispatching` t
        set t.message = p_message,
            t.operation_type_id = 10, -- replace
            t.query_state = 0,
            t.worker = NULL,
            t.source_number = p_source_number,
            t.source_number_ton = p_source_number_ton,
            t.source_number_npi = p_source_number_npi
        where t.uid = v_uid;

      leave root;
    end if;
  end if;

  insert into `dispatching` (`uid`, `operation_type_id`, `source_number`, `source_number_ton`, `source_number_npi`, `destination_number`, `service_type`, `message`, `service_id`, `state`, `current_seq`)
    values (p_uid, CASE p_operation_type_id WHEN 10 then 0 ELSE p_operation_type_id END, p_source_number, p_source_number_ton, p_source_number_npi, p_destination_number, p_service_type, p_message, p_service_id, 0, 0);

  insert into dispatching_state(uid, state) values(p_uid, 0);
END
$$

delimiter ;