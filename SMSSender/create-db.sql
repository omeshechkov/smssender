/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@foreign_key_checks, foreign_key_checks = 0 */;

SET NAMES 'utf8';

SET GLOBAL log_bin_trust_function_creators = 1;

USE smssender;

DROP TABLE IF EXISTS `dispatching#state`;
CREATE TABLE `dispatching#state` (
  id int(11) NOT NULL,
  value varchar(30) DEFAULT NULL,

  constraint `pk_dispatching#state` primary key(id)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 3276
CHARACTER SET utf8
COLLATE utf8_general_ci;

DROP TABLE IF EXISTS operation_type;
CREATE TABLE operation_type (
  id int(11) NOT NULL,
  value varchar(30) DEFAULT NULL,
  constraint `pk_operation_type` primary key(id)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 20
CHARACTER SET utf8
COLLATE utf8_general_ci;

DROP TABLE IF EXISTS `service`;
CREATE TABLE `service` (
  id int(11) NOT NULL,
  value varchar(255) DEFAULT NULL,
  constraint pk_service primary key(id)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 3276
CHARACTER SET utf8
COLLATE utf8_general_ci;

DROP TABLE IF EXISTS dispatching;
CREATE TABLE dispatching (
  uid char(36) NOT NULL DEFAULT '',
  operation_type_id int(11) NOT NULL,
  source_number varchar(50) NOT NULL,
  destination_number varchar(50) NOT NULL,
  service_type varchar(6) NOT NULL,
  message text NOT NULL,
  message_id int(11) not null,
  service_id int(11) not null,
  worker int(11) DEFAULT NULL,
  query_state int(11) DEFAULT 0,

  constraint pk_dispatching PRIMARY KEY (uid),
  constraint `fk_dispatching#operation_type` foreign key(operation_type_id) references operation_type(id),
  constraint `fk_dispatching#service` foreign key(service_id) references service(id),

  INDEX `idx_dispatching#operation_type` (operation_type_id),
  INDEX `idx_dispatching#source_number` (source_number),
  INDEX `idx_dispatching#destination_number` (destination_number),
  INDEX `idx_dispatching#query_state` (query_state),
  INDEX `idx_dispatching#worker` (worker)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 8192
CHARACTER SET utf8
COLLATE utf8_general_ci;

DROP TABLE IF EXISTS dispatching_state;
CREATE TABLE dispatching_state (
  uid char(36) NOT NULL DEFAULT '',
  state int(11) not null,
  smpp_status int(11) null,
  smpp_timestamp datetime null,
  `is_actual` bool not null,
  `timestamp` timestamp DEFAULT CURRENT_TIMESTAMP,

  constraint `pk_dispatching_state` primary key(uid, `timestamp`),
  constraint `fk_dispatching_state#uid` foreign key(uid) references dispatching(uid),
  constraint `fk_dispatching_state#state` foreign key(state) references `dispatching#state`(id)
);


DROP TABLE IF EXISTS `daemon-status`;
CREATE TABLE `daemon-status` (
  id varchar(100) NOT NULL,
  daemon varchar(100) NOT NULL,
  heartbeat timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 608
CHARACTER SET utf8
COLLATE utf8_general_ci;

DROP TABLE IF EXISTS ready;
CREATE TABLE ready (
  uid char(36) NOT NULL DEFAULT '',
  operation_type_id int(11) NOT NULL,
  source_number varchar(50) NOT NULL,
  destination_number varchar(50) NOT NULL,
  service_type varchar(6) NOT NULL,
  message text NOT NULL,
  message_id int(11) not null,
  service_id int(11) not null,
  submit_timestamp timestamp NULL DEFAULT NULL,
  delivery_timestamp timestamp NULL DEFAULT NULL,

  constraint pk_ready PRIMARY KEY (uid),
  constraint `fk_ready#operation_type` foreign key(operation_type_id) references operation_type(id),
  constraint `fk_ready#service` foreign key(service_id) references service(id),

  INDEX `idx_ready#operation_type` (operation_type_id),
  INDEX `idx_ready#source_number` (source_number),
  INDEX `idx_ready#destination_number` (destination_number),
  INDEX `idx_ready#service` (service_id),
  INDEX `idx_ready#submit_timestamp` (submit_timestamp),
  INDEX `idx_ready#delivery_timestamp` (delivery_timestamp)
)
ENGINE = INNODB
CHARACTER SET utf8
COLLATE utf8_general_ci;

DROP TABLE IF EXISTS `not-ready`;
CREATE TABLE `not-ready` (
  uid char(36) NOT NULL DEFAULT '',
  operation_type_id int(11) NOT NULL,
  source_number varchar(50) NOT NULL,
  destination_number varchar(50) NOT NULL,
  service_type varchar(6) NOT NULL,
  message text not null,
  message_id int(11) null,
  service_id int(11) not null,
  state_id int(11) not null,
  submit_timestamp timestamp NULL DEFAULT NULL,
  delivery_timestamp timestamp NULL DEFAULT NULL,

  constraint pk_not_ready PRIMARY KEY (uid),
  constraint `fk_not_ready#operation` foreign key(operation_type_id) references operation_type(id),
  constraint `fk_not_ready#service` foreign key(service_id) references service(id),
  constraint `fk_not_ready#state` foreign key(state_id) references `dispatching#state`(id),

  INDEX `idx_ready#operation_type` (operation_type_id),
  INDEX `idx_ready#source_number` (source_number),
  INDEX `idx_ready#destination_number` (destination_number),
  INDEX `idx_ready#service` (service_id),
  INDEX `idx_ready#state` (state_id),
  INDEX `idx_ready#submit_timestamp` (submit_timestamp),
  INDEX `idx_ready#delivery_timestamp` (delivery_timestamp)
)
ENGINE = INNODB
CHARACTER SET utf8
COLLATE utf8_general_ci;

DROP TABLE IF EXISTS missed_call;
CREATE TABLE missed_call (
  uid char(36) NOT NULL DEFAULT '',
  source_number varchar(20) NOT NULL,
  destination_number varchar(20) NOT NULL,
  count int(11) NOT NULL,
  last_call_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  require_send_subscriber_online int(11) DEFAULT NULL,

  constraint pk_missed_call primary key (uid),

  INDEX `idx_missed_call#count` (count),
  INDEX `idx_missed_call#source_number` (source_number),
  INDEX `idx_missed_call#destination_number` (destination_number),
  INDEX `idx_missed_call#last_call_time` (last_call_time)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 4096
CHARACTER SET utf8
COLLATE utf8_general_ci;

DROP TABLE IF EXISTS received_message_type;
CREATE TABLE received_message_type (
  id int(11) not null,
  name varchar(255) not null,

  constraint pk_received_message_type primary key(id)
);

DROP TABLE IF EXISTS received_message;
CREATE TABLE received_message (
  uid char(36) NOT NULL DEFAULT '',
  type_id int(11) not null,
  source_number varchar(50) NOT NULL,
  destination_number varchar(50) NOT NULL,
  message text NOT NULL,
  submit_timestamp datetime null,
  timestamp timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  constraint pk_received_message primary key (uid),
  constraint fk_received_message foreign key (type_id) references received_message_type(id),

  INDEX `idx_received_message#source_number` (source_number),
  INDEX `idx_received_message#destination_number` (destination_number),
  INDEX `idx_received_message#timestamp` (timestamp)
)
ENGINE = INNODB
CHARACTER SET utf8
COLLATE utf8_general_ci;

DROP TABLE IF EXISTS smpp_cs;
CREATE TABLE smpp_cs (
  status int(4) NOT NULL,
  name varchar(100) NOT NULL,
  description varchar(255) NOT NULL,

  constraint pk_smpp_cs primary key (status)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 297
CHARACTER SET utf8
COLLATE utf8_general_ci;

DROP TABLE IF EXISTS sms_texts;
CREATE TABLE sms_texts (
  type varchar(50) NOT NULL,
  rus text NOT NULL,
  kyr text NOT NULL,
  eng text NOT NULL,
  uzb text NOT NULL,
  `desc` text NOT NULL,

  constraint pk_sms_texts PRIMARY KEY (type)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 8192
CHARACTER SET utf8
COLLATE utf8_general_ci;

DROP TABLE IF EXISTS ussd_commands;
CREATE TABLE ussd_commands (
  ussd varchar(50) NOT NULL,
  `desc` text NOT NULL,
  code varchar(15) NOT NULL,
  function varchar(150) NOT NULL,
  active int(1) NOT NULL DEFAULT 1,
  ts timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (ussd),
  INDEX active (active),
  UNIQUE INDEX code (code),
  INDEX function (function)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 1024
CHARACTER SET utf8
COLLATE utf8_general_ci;

DROP TABLE IF EXISTS ussd_texts;
CREATE TABLE ussd_texts (
  type varchar(50) NOT NULL,
  rus text NOT NULL,
  kyr text NOT NULL,
  eng text NOT NULL,
  uzb text NOT NULL,
  `desc` text NOT NULL,
  PRIMARY KEY (type)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 1024
CHARACTER SET utf8
COLLATE utf8_general_ci;

DELIMITER $$

DROP PROCEDURE IF EXISTS schedule_operation$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE schedule_operation (p_operation_type_id int(11),
                              p_source_number varchar(50),
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
    left join dispatching_state ds on ds.uid = t.uid
                                  and ds.is_actual = true
    where t.source_number = p_source_number
      and t.service_type = p_service_type
      and ds.state in (0, 2)
      and t.destination_number = p_destination_number;

    if v_uid is not null then
      update `dispatching` t
        set t.message = p_message,
            t.operation_type_id = 10, -- replace
            t.query_state = 0,
            t.worker = null
        where t.uid = v_uid;

      leave root;
    end if;
  end if;

  insert into `dispatching` (`uid`, `operation_type_id`, `source_number`, `destination_number`, `service_type`, `message`, `service_id`)
    values (p_uid, p_operation_type_id, p_source_number, p_destination_number, p_service_type, p_message, p_service_id);

  insert into dispatching_state(uid, state, is_actual) 
    values(p_uid, 0, true);
END
$$

DROP PROCEDURE IF EXISTS submit_short_message$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE submit_short_message (p_source_number varchar(50),
                                p_destination_number varchar(50),
                                p_message text,
                                p_service_type varchar(6),
                                p_service_id int(11),
                                p_uid char(36))
BEGIN
  call schedule_operation(0, p_source_number, p_destination_number, p_message, p_service_type, p_service_id, p_uid);
END
$$

DROP PROCEDURE IF EXISTS replace_short_message$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE replace_short_message (p_source_number varchar(50),
                                 p_destination_number varchar(50),
                                 p_message text,
                                 p_service_type varchar(6),
                                 p_service_id int(11),
                                 p_uid char(36))
BEGIN
  call schedule_operation(10, p_source_number, p_destination_number, p_message, p_service_type, p_service_id, p_uid);
END
$$

DROP PROCEDURE IF EXISTS cancel_short_message$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE cancel_short_message (p_uid char(36))
BEGIN
  update dispatching d
    set d.operation_type_id = 20
   where d.uid = p_uid;
END
$$

DROP PROCEDURE IF EXISTS submit_ussd$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE submit_ussd (p_source_number varchar(50),
                       p_destination_number varchar(50),
                       p_message text,
                       p_service_type varchar(6),
                       p_service_id int(11),
                       p_uid char(36))
BEGIN
  call schedule_operation(30, p_source_number, p_destination_number, p_message, p_service_type, p_service_id, p_uid);
END
$$

DROP PROCEDURE IF EXISTS change_operation_state$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE change_operation_state (in p_uid char(36),
                                  in p_message_id int,
                                  in p_state int,
                                  in p_smpp_status int,
                                  in p_timestamp timestamp)
BEGIN
  declare v_uid char(36);
  declare v_source_number varchar(50);
  declare v_destination_number varchar(50);
  declare v_message text;
  declare v_service_id int;
  declare v_service_type varchar(6);

  if p_state = 2 then -- submitted
    update dispatching d
     set d.message_id = p_message_id
    where d.uid = p_uid;

    update dispatching_state ds
      set ds.is_actual = false
     where ds.uid = p_uid and ds.is_actual = true;

    insert into dispatching_state(uid, state, smpp_status, smpp_timestamp, is_actual)
      values(p_uid, p_state, p_smpp_status, p_timestamp, true);

    call sms_delivered(p_uid);
  else
    select d.`uid`,
           d.`source_number`,
           d.`destination_number`,
           d.`message`,
           d.`service_id`,
           d.`service_type` into v_uid, v_source_number, v_destination_number, v_message, v_service_id, v_service_type
      from dispatching d
     where d.message_id = p_message_id;

    if p_smpp_status = 12 then
      set p_state = 7; -- delivered
    elseif p_smpp_status = 19 then
      set p_state = 10; -- undeliverable
    end if;

    update dispatching_state ds
      set ds.is_actual = false
     where ds.uid = v_uid
       and ds.is_actual = true;
  
    insert into dispatching_state(uid, state, smpp_status, smpp_timestamp, is_actual) 
      values(v_uid, p_state, p_smpp_status, p_timestamp, true);

    if p_state = 7 then
      call sms_delivered(v_uid);
    end if;
  
    if p_smpp_status = 12 OR p_smpp_status = 19 then
      call submit_short_message(v_source_number,
                                v_destination_number,
                                v_message,
                                v_service_type,
                                v_service_id,
                                MD5(CONCAT(v_uid, v_source_number, v_destination_number, v_message, v_service_type, v_service_id)));
    end if;
  end if;
END
$$

DROP PROCEDURE IF EXISTS notify_message_received$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE notify_message_received (in p_uid varchar(36),
                                   in p_type_id int(11),
                                   in p_source_number varchar(50),
                                   in p_destination_number varchar(50),
                                   in p_message text,
                                   in p_timestamp timestamp)
BEGIN
  -- todo submit_timestamp - не правда
  INSERT INTO `received_message` (uid, type_id, source_number, destination_number, message, submit_timestamp)
    VALUES (p_uid, p_type_id, p_source_number, p_destination_number, p_message, p_timestamp);
END
$$

DROP PROCEDURE IF EXISTS sms_delivered$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE sms_delivered (IN p_sms_uid char(36))
BEGIN
  DECLARE done int DEFAULT 0;
  DECLARE destination_number char(20);
  DECLARE source_number char(20);
  DECLARE missed_call_uid char(36);
  DECLARE thisop int;
  DECLARE require_send_subscriber_online int;
  DECLARE cur1 CURSOR FOR
  SELECT
    IF(mc.`destination_number` LIKE '55%', CONCAT('996', mc.`destination_number`), mc.`destination_number`) AS `destination_number`,
    IF(mc.`source_number` LIKE '55%', CONCAT('996', mc.`source_number`), mc.`source_number`) AS `source_number`,
    mc.`uid` AS `missed_call_uid`,
    mc.`require_send_subscriber_online`,
    IF((mc.`source_number` LIKE '99655_______' OR mc.`source_number` LIKE '55_______'), 1, 0) AS `thisop`
  FROM smssender.`dispatching` AS td
    LEFT JOIN smssender.`missed_call` AS mc
      ON td.`destination_number` = mc.`destination_number`
  WHERE td.`uid` = p_sms_uid
  AND mc.`count` > 0;
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = 1;
  /*
  INSERT INTO `sms_delivery` (`sms_id`, `sms_state`) VALUES (p_sms_id, 0);*/

  OPEN cur1;

  REPEAT
    FETCH cur1 INTO destination_number, source_number, missed_call_uid, require_send_subscriber_online, thisop;
    IF done = 0 THEN
      IF require_send_subscriber_online = 1 AND thisop = 1 THEN
        call submit_short_message('970',
                                  source_number,
                                  CONCAT('Abonent +', destination_number, ' snova na svyazi'),
                                  '', -- service type
                                  109, -- service id
                                  MD5(CONCAT(p_sms_uid, source_number, destination_number, missed_call_uid)));
      END IF;

      DELETE
        FROM `smssender`.`missed_call`
      WHERE `uid` = missed_call_uid
        OR `last_call_time` <= NOW() - INTERVAL 2 DAY;
    END IF;
  UNTIL done = 1
  END REPEAT;

  CLOSE cur1;
END
$$

DROP FUNCTION IF EXISTS ret_ussd_texts$$
CREATE DEFINER = 'vs'@'127.0.0.1'
FUNCTION ret_ussd_texts (p_type varchar(50),
                         p_lang varchar(3))
RETURNS text charset utf8
BEGIN

  DECLARE ret text;
  SET ret = '';

  SELECT
    CASE
      WHEN p_lang = 'rus' THEN rus
      WHEN p_lang = 'kyr' THEN kyr
      WHEN p_lang = 'eng' THEN eng
      WHEN p_lang = 'uzb' THEN uzb ELSE rus
    END INTO ret
  FROM ussd_texts
  WHERE type = p_type;

  RETURN ret;
END
$$

DROP FUNCTION IF EXISTS set_from_ussd$$
CREATE DEFINER = 'vs'@'127.0.0.1'
FUNCTION set_from_ussd (sub varchar(15),
c varchar(15))
RETURNS text charset utf8
BEGIN
  DECLARE ret varchar(100);
  /*
s1|s2
s3|s4
s5|s6
v0|v1
l1|l2|l3|l4  
*/

  SET ret =
    CASE
      WHEN c = 's1' THEN asterisk.replace_sms_mask_params(sub, c, 's2')
      WHEN c = 's2' THEN asterisk.replace_sms_mask_params(sub, c, 's1')
      WHEN c = 's3' THEN asterisk.replace_sms_mask_params(sub, c, 's4')
      WHEN c = 's4' THEN asterisk.replace_sms_mask_params(sub, c, 's3')
      WHEN c = 's5' THEN asterisk.replace_sms_mask_params(sub, c, 's6')
      WHEN c = 's6' THEN asterisk.replace_sms_mask_params(sub, c, 's5')
      WHEN c = 'l1' THEN asterisk.slapi_sub_set_lang(sub, '1')
      WHEN c = 'l2' THEN asterisk.slapi_sub_set_lang(sub, '2')
      WHEN c = 'l3' THEN asterisk.slapi_sub_set_lang(sub, '3')
      WHEN c = 'l4' THEN asterisk.slapi_sub_set_lang(sub, '4')
      WHEN c = 'v0' THEN asterisk.slapi_vm_on_off(sub, 0)
      WHEN c = 'v1' THEN asterisk.slapi_vm_on_off(sub, 1)
    END;

  RETURN ret_ussd_texts(c, asterisk.get_sub_language(sub));
END
$$

DELIMITER ;

INSERT INTO `dispatching#state`
 VALUES (0, 'Scheduled'),
  (1, 'Submitting'),
  (2, 'Submitted'),
  (3, 'Cancelling'),
  (4, 'Canceled'),
  (5, 'Cancelling to replace'),
  (6, 'Canceled to replace'),
  (7, 'Delivered'),
  (8, 'Expired'),
  (9, 'Deleted'),
  (10, 'Undeliverable'),
  (11, 'Accepted'),
  (12, 'Rejected'),
  (13, 'Unknown');

INSERT INTO operation
  VALUES (0, 'SubmitShortMessage'),
  (10, 'ReplaceMessage'),
  (20, 'CancelMessage'),
  (30, 'SubmitUSSD');

insert into received_message_type(id, name)
  values (0, 'SM'), (1, 'USSD');

INSERT INTO smpp_cs
  VALUES (0, 'ESME_ROK', 'No Error'),
  (1, 'ESME_RINVMSGLEN', 'Message too long'),
  (2, 'ESME_RINVCMDLEN', 'Command length is invalid'),
  (3, 'ESME_RINVCMDID', 'Command ID is invalid or not supported'),
  (4, 'ESME_RINVBNDSTS', 'Incorrect bind status for given command'),
  (5, 'ESME_RALYBND', 'Already bound'),
  (6, 'ESME_RINVPRTFLG', 'Invalid Priority Flag'),
  (7, 'ESME_RINVREGDLVFLG', 'Invalid registered delivery flag'),
  (8, 'ESME_RSYSERR', 'System error'),
  (10, 'ESME_RINVSRCADR', 'Invalid source address'),
  (11, 'ESME_RINVDSTADR', 'Invalid destination address'),
  (12, 'ESME_RINVMSGID', 'Message ID is invalid'),
  (13, 'ESME_RBINDFAIL', 'Bind failed'),
  (14, 'ESME_RINVPASWD', 'Invalid password'),
  (15, 'ESME_RINVSYSID', 'Invalid System ID'),
  (17, 'ESME_RCANCELFAIL', 'Canceling message failed'),
  (19, 'ESME_RREPLACEFAIL', 'Message replacement failed'),
  (20, 'ESME_RMSSQFUL', 'Message queue full'),
  (21, 'ESME_RINVSERTYP', 'Invalid service type'),
  (51, 'ESME_RINVNUMDESTS', 'Invalid number of destinations'),
  (52, 'ESME_RINVDLNAME', 'Invalid distribution list name'),
  (64, 'ESME_RINVDESTFLAG', 'Invalid destination flag'),
  (66, 'ESME_RINVSUBREP', 'Invalid submit with replace request'),
  (67, 'ESME_RINVESMCLASS', 'Invalid esm class set'),
  (68, 'ESME_RCNTSUBDL', 'Invalid submit to distribution list'),
  (69, 'ESME_RSUBMITFAIL', 'Submitting message has failed'),
  (72, 'ESME_RINVSRCTON', 'Invalid source address type of number ( TON )'),
  (73, 'ESME_RINVSRCNPI', 'Invalid source address numbering plan ( NPI )'),
  (80, 'ESME_RINVDSTTON', 'Invalid destination address type of number ( TON )'),
  (81, 'ESME_RINVDSTNPI', 'Invalid destination address numbering plan ( NPI )'),
  (83, 'ESME_RINVSYSTYP', 'Invalid system type'),
  (84, 'ESME_RINVREPFLAG', 'Invalid replace_if_present flag'),
  (85, 'ESME_RINVNUMMSGS', 'Invalid number of messages'),
  (88, 'ESME_RTHROTTLED', 'Throttling error'),
  (97, 'ESME_RINVSCHED', 'Invalid scheduled delivery time'),
  (98, 'ESME_RINVEXPIRY', 'Invalid Validity Period value'),
  (99, 'ESME_RINVDFTMSGID', 'Predefined message not found'),
  (100, 'ESME_RX_T_APPN', 'ESME Receiver temporary error'),
  (101, 'ESME_RX_P_APPN', 'ESME Receiver permanent error'),
  (102, 'ESME_RX_R_APPN', 'ESME Receiver reject message error'),
  (103, 'ESME_RQUERYFAIL', 'Message query request failed'),
  (192, 'ESME_RINVTLVSTREAM', 'Error in the optional part of the PDU body'),
  (193, 'ESME_RTLVNOTALLWD', 'TLV not allowed'),
  (194, 'ESME_RINVTLVLEN', 'Invalid parameter length'),
  (195, 'ESME_RMISSINGTLV', 'Expected TLV missing'),
  (196, 'ESME_RINVTLVVAL', 'Invalid TLV value'),
  (254, 'ESME_RDELIVERYFAILURE', 'Transaction delivery failure'),
  (255, 'ESME_RUNKNOWNERR', 'Unknown error'),
  (256, 'ESME_RSERTYPUNAUTH', 'ESME not authorised to use specified servicetype'),
  (257, 'ESME_RPROHIBITED', 'ESME prohibited from using specified operation'),
  (258, 'ESME_RSERTYPUNAVAIL', 'Specified servicetype is unavailable'),
  (259, 'ESME_RSERTYPDENIED', 'Specified servicetype is denied'),
  (260, 'ESME_RINVDCS', 'Invalid data coding scheme'),
  (261, 'ESME_RINVSRCADDRSUBUNIT', 'Invalid source address subunit'),
  (262, 'ESME_RINVSTDADDRSUBUNIR', 'Invalid destination address subunit');

INSERT INTO sms_texts
  VALUES ('sub_a_notify', 'Я снова на связи. Вы можете мне перезвонить', 'Мен кайра байланыштамын. Сиз мага кайрадан чала аласыз', 'I am in touch again. Yоu can call me back.', 'Men yana aloqadaman. Siz menga qayta qongiroq qilishingiz mumkin', 'Информирование абонента А о том, что абонент Б снова на связи'),
  ('sub_b_from_1_a', 'Вам звонили %N% раз, последний звонок в %LAST%', '', '', '', 'Информирование абонента Б о непринятом звонке, если не оставили голосовое сообщение и звонил один абонент');

INSERT INTO ussd_commands
  VALUES ('*200#', 'Чтобы получить краткую информацию об услуге «Кто звонил»', 'sinfo', 'send_info', 0, '2013-05-02 21:31:12'),
  ('*200*1#', 'Чтобы отказаться от получения сообщений о пропущенных звонках', 's1', 'set_from_ussd', 0, '2013-05-02 21:31:14'),
  ('*200*2#', 'Для того чтобы разрешить получение сообщений о пропущенных звонках', 's2', 'set_from_ussd', 0, '2013-05-02 21:31:15'),
  ('*200*3#', 'Чтобы отключить автоматическое оповещение звонивших абонентов', 's3', 'set_from_ussd', 0, '2013-05-02 21:31:16'),
  ('*200*4#', 'Для того чтобы включить автоматическое оповещение звонивших абонентов', 's4', 'set_from_ussd', 0, '2013-05-02 21:31:18'),
  ('*200*5#', 'Чтобы отключить автоматическое оповещение от вызываемых абонентов', 's5', 'set_from_ussd', 0, '2013-05-02 21:31:19'),
  ('*200*6#', 'Чтобы включить автоматическое оповещение от вызываемых абонентов', 's6', 'set_from_ussd', 0, '2013-05-02 21:31:21'),
  ('*200*8#', 'Чтобы узнать состояние услуги «Кто звонил»', 's8', 'send_info_state', 0, '2013-05-02 21:31:23'),
  ('*980#', 'Краткая информация о сервисе «На связи»', 'vinfo', 'send_info', 1, '2013-05-02 21:29:42'),
  ('*980*0#', 'Отключение услуги «На связи»', 'v0', 'set_from_ussd', 1, '2013-05-02 21:30:26'),
  ('*980*1#', 'Подключение услуги «На связи»', 'v1', 'set_from_ussd', 1, '2013-05-02 21:30:04'),
  ('*980*1*1#', 'Установка языка профиля: кыргызский', 'l1', 'set_from_ussd', 1, '2013-05-02 21:34:39'),
  ('*980*1*2#', 'Установка языка профиля: русский', 'l2', 'set_from_ussd', 1, '2013-05-02 21:35:16'),
  ('*980*1*3#', 'Установка языка профиля: узбекский', 'l3', 'set_from_ussd', 1, '2013-05-02 21:35:57'),
  ('*980*1*4#', 'Установка языка профиля: английский', 'l4', 'set_from_ussd', 1, '2013-05-02 21:36:25'),
  ('*980*2#', 'Подключение оповещения о появлении в сети абонента Б', 'v2', 'set_from_ussd', 1, '2013-05-02 21:31:10');

INSERT INTO ussd_texts
  VALUES ('13', 'Rezhim personalizatsii aktiven', 'Персоналдаштыруу режими активдүү', 'Personalization mode is active', 'Rezhim personalizatsii aktiven', 'Чтобы включить режим персонализации уведомления о пропущенных вызовах'),
  ('14', 'Стандартный режим активен', 'Стандарттык режим активдүү', 'Default mode is active', 'Standart rejim faol', 'Чтобы вернуться к стандартному режиму (в сообщении указываются один или несколько номеров звонивших абонентов)'),
  ('error', 'Izvinite, proizoshla sistemnaya oshibka. Pozhaluista, povtorite nabor pozdnee.', 'Kechiresiz, sistemdik kata ketti. Suranych, araketti bir azdan kiyin kaitalanyz.', 'Sorry, a system error has occurred. Please, try again later. ', 'Kechirasiz, sistemada xato ketdi. Iltimoz, talabni tekshiring va bir ozdan keyin qaytaring.', 'Другие системные ошибки'),
  ('s1', 'Usluga informirovaniya o propuschennyh zvonkah NE aktivna', 'Кабыл алынбаган чалуулар тууралуу кызмат активдүү ЭМЕС', 'Missed calls notification is inactive', 'Qabul qilinmagan qongiroqlar tugrisida xizmat faol EMAS', 'Информирование абонента об успешном отключении получения сообщений о пропущенных звонках'),
  ('s2', 'Usluga informirovaniya o propuschennyh zvonkah aktivna', 'Кабыл алынбаган чалуулар тууралуу кызмат активдүү', 'Missed calls notification is active', 'Qabul qilinmagan qongiroqlar tugrisida xizmat faol', 'Информирование абонента об успешном подключении сообщений о пропущенных звонках'),
  ('s3', 'Usluga avtomaticheskogo opovesheniya ot Vas - ne aktivna', 'Сизден автоматтык маалымдоо кызматы – активдүү эмес', 'Automatic notification from you is inactive', 'Usluga avtomaticheskogo opovesheniya ot Vas - ne aktivna Sizdan avtomatik ogohlantirish xizmati - faol EMAS', 'Информирование абонента об успешном отключении автоматического оповещение звонивших абонентов'),
  ('s4', 'Usluga avtomaticheskogo opovesheniya ot Vas - aktivna', 'Сизден автоматтык маалымдоо кызматы – активдүү', 'Automatic notification from you is active', 'Usluga avtomaticheskogo opovesheniya ot Vas - aktivna Sizdan avtomatik ogohlantirish xizmati - faol', 'Информирование абонента об успешном подключении автоматического оповещения звонивших абонентов'),
  ('s5', 'Usluga avtomaticheskogo opovesheniya Vas - ne aktivna', 'Сизди автоматтык маалымдоо кызматы – активдүү эмес', 'Automatic notification is inactive', 'Usluga avtomaticheskogo opovesheniya Вас - ne aktivna Sizni avtomatik ogohlantirish xizmati - faol EMAS', 'Чтобы отключить автоматическое оповещение от вызываемых абонентов'),
  ('s6', 'Usluga avtomaticheskogo opovesheniya Vas - aktivna', 'Сизди автоматтык маалымдоо кызматы – активдүү', 'Automatic notification is active', 'Sizni avtomatik ogohlantirish xizmati - faol', 'Чтобы включить автоматическое оповещение от вызываемых абонентов'),
  ('s8', 'Отправка soobsheniy o propuschennyh zvonkah <%s1%> aktivna, Usluga avtomaticheskogo opovescheniya <%s3%> aktivna, Rezhim personalizatsii <%s5%> aktiven', 'Кабыл алынбаган чалуулар тууралуу билдирүү жөнөтүү активдүү ЭМЕС, Автоматтык маалымдоо кызматы активдүү ЭМЕС, Персоналдаштыруу режими активдүү ЭМЕС', 'Missed calls notification is inactive. Automatic notification is inactive. Personalization mode is inactive', 'Qabul qilinmagan qongiroqlar tugrisida xabar yuborish faol EMAS, Avtomatik malumotlash xizmati faol EMAS, Isimlarni aniqlash rejimi faol EMAS', 'Чтобы узнать состояние услуги «Кто звонил»'),
  ('sinfo', 'Usluga KTO ZVONIL?: *200*X#, gde X=0, 1, 2, 3, 4, 5, 6, 7, 8 – komandy upravleniya uslugoy. Info *500 ili www.megacom.kg.', 'КИМ ЧАЛДЫ? кызматы: *200*X#, бул жерде X=0, 1, 2, 3, 4, 5, 6, 7, 8 – кызматты башкаруу буйруулары. Маалымат *500 же www.megacom.kg', '“Who called?” service: *200*X#, where X=0, 1, 2, 3, 4, 5, 6, 7, 8 – service commands. Info *500 or www.megacom.kg.', 'KIM CHOLDI? xizmati:*200*X#, bu X=0, 1, 2, 3, 4, 5, 6, 7, 8 –xizmatni boshkarish buyruqlari. Malumot *500 yoki www.megacom.kg.', 'Чтобы получить краткую информацию об услуге «Кто звонил»'),
  ('v0', 'Вы отключили услугу «Na svyazi». Ваш голосовой ящик останется доступен еще 7 дней.', 'Сиз “Bailanyshta» кызматын өчүрдүңүз. Сиз үн кутучаңыз дагы 7 күн жеткиликтүү болот', 'You have disabled “In touch” service. Your voice mailbox will be available for 7 more days.', 'Siz «Aloqada» xizmatini uchirdingiz. Sizning ovoz pochtangiz yana 7 kun etkulikta buladi.', '*980*0# отключение услуги'),
  ('v0_d', 'Usluga "Na svyazi" ne podklyuchena. Dlya aktivatsii uslugi naberite *980*1#.', '“Bailanyshta” kyzmaty koshulgan emes. Kyzmatty aktivdeshtiruu *980*1#.', '«In touch» service is disabled. Dial *980*1# to activate.', '“Aloqada” xizmati boglanmagan. Xizmatni faollashtirish uchun *980*1# tering.', 'У абонента не подключена услуга «Na svyazi» и он пытается ее отключить'),
  ('v1', 'Вы подключили услугу «Na svyazi». Инфо *981', 'Сиз “Bailanyshta» кызматын коштуңуз. Маалымат *981', 'You have enabled “In touch” service. Your voice mailbox will be available for 7 more days.', 'Siz «Aloqada» xizmatini bogladingiz. Malumot *981', '*980*1# подключение услуги'),
  ('v1_d', 'Usluga "Na svyazi" uje podklyuchena', '“Bailanyshta” kyzmaty murda koshulgan', '«In touch» service is already enabled.', '«Aloqada» xizmati avval boglangan.', 'У абонента уже подключена услуга «Na svyazi», и им совершена попытка повторного подключения услуги'),
  ('vinfo', 'С услугой «Na svyazi» Вы не пропустите ни одного звонка, если Вы были не доступны. Инфо *981. *980*1# подключить услугу', '«Bailanyshta» кызматы менен Сиз жеткиликтүү эмес болсоңуз да, бардык чалууларды билип турасыз.  Маалымат *981. Кызматты кошуу үчүн *980*1#', 'Do not miss any call with “In touch” service, when you are not available. Dial *980*1# to enable. Info *981.', '«Aloqada» xizmati bilan Siz aloqadan tashqari bulsangiz ham barcha qongiroqlarni bilib turasiz.', '*980# краткая информация об услуге');

/*!40014 SET foreign_key_checks = @OLD_FOREIGN_KEY_CHECKS */;