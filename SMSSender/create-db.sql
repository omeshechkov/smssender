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
  name varchar(255) DEFAULT NULL,
  constraint pk_service primary key(id)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 3276
CHARACTER SET utf8
COLLATE utf8_general_ci;

DROP TABLE IF EXISTS `ton`;
CREATE TABLE `ton` (
  id int(11) NOT NULL,
  name varchar(255) DEFAULT NULL,
  constraint pk_ton primary key(id)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 3276
CHARACTER SET utf8
COLLATE utf8_general_ci;

DROP TABLE IF EXISTS `npi`;
CREATE TABLE `npi` (
  id int(11) NOT NULL,
  name varchar(255) DEFAULT NULL,
  constraint pk_npi primary key(id)
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
  source_number_ton int(11) NOT NULL,
  source_number_npi int(11) NOT NULL,
  destination_number varchar(50) NOT NULL,
  service_type varchar(6) NOT NULL,
  message text NOT NULL,
  message_id int(11) not null,
  service_id int(11) not null,
  state int(11) not null,
  worker int(11) DEFAULT NULL,
  query_state int(11) DEFAULT 0,

  constraint pk_dispatching PRIMARY KEY (uid),
  constraint `fk_dispatching#operation_type` foreign key(operation_type_id) references operation_type(id),
  constraint `fk_dispatching#service` foreign key(service_id) references service(id),
  constraint `fk_dispatching#source_number_ton` foreign key(source_number_ton) references ton(id),
  constraint `fk_dispatching#source_number_npi` foreign key(source_number_npi) references npi(id),

  INDEX `idx_dispatching#operation_type` (operation_type_id),
  INDEX `idx_dispatching#source_number` (source_number),
  INDEX `idx_dispatching#destination_number` (destination_number),
  INDEX `idx_dispatching#state` (state),
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
  `timestamp` timestamp DEFAULT CURRENT_TIMESTAMP,

  constraint `fk_dispatching_state#uid` foreign key(uid) references dispatching(uid),
  constraint `fk_dispatching_state#state` foreign key(state) references `dispatching#state`(id),

  index `ind_dispatching_state#uid` (uid)
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
  timestamp datetime not null,

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

DELIMITER $$

DROP PROCEDURE IF EXISTS schedule_operation$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE schedule_operation(p_operation_type_id int(11),
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
      and t.state in (0, 2);

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

  insert into `dispatching` (`uid`, `operation_type_id`, `source_number`, `source_number_ton`, `source_number_npi`, `destination_number`, `service_type`, `message`, `service_id`, `state`)
    values (p_uid, p_operation_type_id, p_source_number, p_source_number_ton, p_source_number_npi, p_destination_number, p_service_type, p_message, p_service_id, 0);

  insert into dispatching_state(uid, state) values(p_uid, 0);
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

DROP PROCEDURE IF EXISTS lock_sms_for_query$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE lock_sms_for_query ()
BEGIN
  UPDATE dispatching d
    SET d.worker = connection_id(),
        d.query_state = 1
     WHERE d.query_state = 0
      and (d.operation_type_id in (0, 10, 20) or (d.operation_type_id = 10 and d.state in (0, 2, 4, 6)))
      LIMIT 10;
END
$$

DROP PROCEDURE IF EXISTS lock_ussd_for_query$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE lock_ussd_for_query ()
BEGIN
  UPDATE dispatching d
    SET d.worker = connection_id(),
        d.query_state = 1
     WHERE d.query_state = 0 and d.operation_type_id = 30
      LIMIT 10;
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
  declare v_source_number_ton int(11);
  declare v_source_number_npi int(11);
  declare v_destination_number varchar(50);
  declare v_message text;
  declare v_service_id int;
  declare v_service_type varchar(6);

  -- submitting, submitted, cancelling, cancelled, cancelling to replace
  if p_state in (1, 2, 3, 4, 5) then
    update dispatching d
     set d.message_id = p_message_id,
         d.state = p_state
    where d.uid = p_uid;

    insert into dispatching_state(uid, state, smpp_status, smpp_timestamp)
      values(p_uid, p_state, p_smpp_status, p_timestamp);
  elseif p_state = 6 then -- cancelled to replace
    update dispatching d
     set d.message_id = p_message_id,
         d.state = p_state,
         d.worker = null,
         d.query_state = 0
    where d.uid = p_uid;

    insert into dispatching_state(uid, state, smpp_status, smpp_timestamp)
      values(p_uid, p_state, p_smpp_status, p_timestamp);
  else
    select d.`uid`,
           d.`source_number`,
           d.`source_number_ton`,
           d.`source_number_npi`,
           d.`destination_number`,
           d.`message`,
           d.`service_id`,
           d.`service_type` into v_uid, v_source_number, v_source_number_ton, v_source_number_npi, v_destination_number, v_message, v_service_id, v_service_type
      from dispatching d
     where d.message_id = p_message_id;

    if p_smpp_status = 12 then -- message id is invalid
      set p_state = 7; -- delivered
    end if;

    update dispatching d
     set d.state = p_state
    where d.uid = v_uid;

    insert into dispatching_state(uid, state, smpp_status, smpp_timestamp)
      values(v_uid, p_state, p_smpp_status, p_timestamp);

    if p_state = 7 then -- delivered
      call sms_delivered(v_uid);
    end if;
  
    if p_smpp_status = 12 then -- message id is invalid
      call submit_short_message(v_source_number,
                                v_source_number_ton,
                                v_source_number_npi,
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
  INSERT INTO `received_message` (uid, type_id, source_number, destination_number, message, `timestamp`)
    VALUES (p_uid, p_type_id, p_source_number, p_destination_number, p_message, p_timestamp);
END
$$

DROP PROCEDURE IF EXISTS sms_delivered$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE sms_delivered (IN p_sms_uid char(36))
BEGIN
  -- dispatch sms delivered event
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

INSERT INTO operation_type
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

insert into ton(id, name) values
(0, 'Unknown'),
(1, 'International'),
(2, 'National'),
(3, 'Network specific'),
(4, 'Subscriber number'),
(5, 'Alphanumeric'),
(6, 'Abbreviated');

insert into npi(id, name) values
(0, 'Unknown'),
(1, 'ISDN (E163/E164)'),
(3, 'Data (X.121)'),
(4, 'Telex (F.69)'),
(6, 'Land Mobile (E.212)'),
(8, 'National'),
(9, 'Private'),
(10, 'ERMES'),
(14, 'Internet (IP)'),
(18, 'WAP Client Id');

/*!40014 SET foreign_key_checks = @OLD_FOREIGN_KEY_CHECKS */