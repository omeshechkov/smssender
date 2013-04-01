-- Скрипт сгенерирован Devart dbForge Studio for MySQL, Версия 5.0.97.1
-- Домашняя страница продукта: http://www.devart.com/ru/dbforge/mysql/studio
-- Дата скрипта: 07.01.2013 14:02:21
-- Версия сервера: 5.1.66
-- Версия клиента: 4.1

-- 
-- Отключение внешних ключей
-- 
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;

-- 
-- Установка кодировки, с использованием которой клиент будет посылать запросы на сервер
--
SET NAMES 'utf8';

-- 
-- Установка базы данных по умолчанию
--
USE smssender;

--
-- Описание для таблицы `daemon-status`
--
DROP TABLE IF EXISTS `daemon-status`;
CREATE TABLE `daemon-status` (
  id VARCHAR(100) NOT NULL,
  daemon VARCHAR(100) NOT NULL,
  heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
)
ENGINE = MEMORY
AVG_ROW_LENGTH = 608
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы `not-ready`
--
DROP TABLE IF EXISTS `not-ready`;
CREATE TABLE `not-ready` (
  id INT(11) NOT NULL AUTO_INCREMENT,
  source_number VARCHAR(50) NOT NULL,
  destination_number VARCHAR(50) NOT NULL,
  message VARCHAR(999) NOT NULL,
  message_type INT(11) NOT NULL,
  service_type INT(11) NOT NULL,
  state INT(11) NOT NULL,
  submit_timestamp TIMESTAMP NULL DEFAULT NULL,
  delivery_timestamp TIMESTAMP NULL DEFAULT NULL,
  parts INT(3) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  INDEX delivery_timestamp (delivery_timestamp),
  INDEX destination_number (destination_number),
  INDEX message (message(255)),
  INDEX service_type (service_type),
  INDEX parts (parts),
  INDEX source_number (source_number),
  INDEX state (state),
  INDEX submit_timestamp (submit_timestamp)
)
ENGINE = INNODB
AUTO_INCREMENT = 1
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы batch
--
DROP TABLE IF EXISTS batch;
CREATE TABLE batch (
  id INT(11) DEFAULT NULL,
  source_number VARCHAR(50) DEFAULT NULL,
  destination_number VARCHAR(50) DEFAULT NULL,
  message VARCHAR(999) DEFAULT NULL,
  state INT(11) DEFAULT NULL,
  worker INT(11) DEFAULT NULL,
  uid CHAR(36) NOT NULL DEFAULT ''
)
ENGINE = MEMORY
AVG_ROW_LENGTH = 3422
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы dispatching
--
DROP TABLE IF EXISTS dispatching;
CREATE TABLE dispatching (
  id INT(11) NOT NULL AUTO_INCREMENT,
  uid CHAR(36) NOT NULL DEFAULT '',
  source_number VARCHAR(50) NOT NULL,
  destination_number VARCHAR(50) NOT NULL,
  message VARCHAR(999) NOT NULL,
  message_type INT(11) NOT NULL,
  service_type INT(11) NOT NULL,
  state INT(11) DEFAULT 0,
  worker INT(11) DEFAULT NULL,
  query_state INT(11) DEFAULT 0,
  record_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX destination_number (destination_number),
  INDEX service_type (service_type),
  INDEX query_state (query_state),
  INDEX record_timestamp (record_timestamp),
  INDEX source_number (source_number),
  INDEX state (state),
  UNIQUE INDEX uid_2 (uid),
  INDEX worker (worker)
)
ENGINE = INNODB
AUTO_INCREMENT = 1
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы message
--
DROP TABLE IF EXISTS message;
CREATE TABLE message (
  id INT(11) NOT NULL AUTO_INCREMENT,
  uid CHAR(36) NOT NULL DEFAULT '',
  session_id INT(11) NOT NULL,
  session_uid CHAR(36) NOT NULL DEFAULT '',
  message_id INT(11) NOT NULL,
  message VARCHAR(160) NOT NULL,
  state INT(11) DEFAULT 0,
  submit_timestamp TIMESTAMP NULL DEFAULT NULL,
  delivery_timestamp TIMESTAMP NULL DEFAULT NULL,
  command_status INT(4) DEFAULT NULL,
  command_timestamp TIMESTAMP NULL DEFAULT NULL,
  INDEX command_status (command_status, command_timestamp),
  INDEX delivery_timestamp (delivery_timestamp),
  UNIQUE INDEX id (id),
  INDEX message (message),
  INDEX message_id (message_id),
  INDEX session_id (session_id),
  INDEX session_uid (session_uid),
  INDEX state (state),
  INDEX submit_timestamp (submit_timestamp)
)
ENGINE = INNODB
AUTO_INCREMENT = 1
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы message_state
--
DROP TABLE IF EXISTS message_state;
CREATE TABLE message_state (
  id INT(11) NOT NULL,
  value VARCHAR(30) DEFAULT NULL,
  INDEX id_index (id)
)
ENGINE = MYISAM
AVG_ROW_LENGTH = 20
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы message_state
--
DROP TABLE IF EXISTS message_type;
CREATE TABLE message_type (
  id INT(11) NOT NULL,
  value VARCHAR(30) DEFAULT NULL,
  INDEX id_index (id)
)
ENGINE = MYISAM
AVG_ROW_LENGTH = 20
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы missed_call
--
DROP TABLE IF EXISTS missed_call;
CREATE TABLE missed_call (
  id INT(11) NOT NULL AUTO_INCREMENT,
  uid CHAR(36) NOT NULL DEFAULT '',
  source_number VARCHAR(20) NOT NULL,
  destination_number VARCHAR(20) NOT NULL,
  count INT(11) NOT NULL,
  last_call_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  require_send_subscriber_online INT(11) DEFAULT NULL,
  PRIMARY KEY (id),
  INDEX count (count),
  INDEX destination_number (destination_number),
  INDEX last_call_time (last_call_time),
  INDEX source_number (source_number),
  UNIQUE INDEX uid_2 (uid)
)
ENGINE = INNODB
AUTO_INCREMENT = 1
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы ready
--
DROP TABLE IF EXISTS ready;
CREATE TABLE ready (
  id INT(11) NOT NULL AUTO_INCREMENT,
  source_number VARCHAR(50) NOT NULL,
  destination_number VARCHAR(50) NOT NULL,
  message VARCHAR(999) NOT NULL,
  message_type INT(11) NOT NULL,
  service_type INT(11) NOT NULL,
  submit_timestamp TIMESTAMP NULL DEFAULT NULL,
  delivery_timestamp TIMESTAMP NULL DEFAULT NULL,
  parts INT(3) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  INDEX delivery_timestamp (delivery_timestamp),
  INDEX destination_number (destination_number),
  INDEX message (message(255)),
  INDEX service_type (service_type),
  INDEX parts (parts),
  INDEX source_number (source_number),
  INDEX submit_timestamp (submit_timestamp)
)
ENGINE = INNODB
AUTO_INCREMENT = 1
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы received
--
DROP TABLE IF EXISTS received;
CREATE TABLE received (
  id INT(11) NOT NULL AUTO_INCREMENT,
  source_number VARCHAR(50) NOT NULL,
  destination_number VARCHAR(50) NOT NULL,
  message VARCHAR(999) NOT NULL,
  `timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX destination_number (destination_number),
  INDEX message (message(255)),
  INDEX source_number (source_number),
  INDEX `timestamp` (`timestamp`)
)
ENGINE = INNODB
AUTO_INCREMENT = 1
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы short_message_state
--
DROP TABLE IF EXISTS short_message_state;
CREATE TABLE short_message_state (
  id INT(11) NOT NULL,
  value VARCHAR(30) DEFAULT NULL,
  INDEX id_index (id)
)
ENGINE = MYISAM
AVG_ROW_LENGTH = 20
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы smpp_cs
--
DROP TABLE IF EXISTS smpp_cs;
CREATE TABLE smpp_cs (
  status INT(4) NOT NULL,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(255) NOT NULL,
  PRIMARY KEY (status),
  INDEX description (description),
  INDEX name (name)
)
ENGINE = MYISAM
AVG_ROW_LENGTH = 54
CHARACTER SET utf8
COLLATE utf8_general_ci;

DELIMITER $$

--
-- Описание для процедуры change_message_state_ex
--
DROP PROCEDURE IF EXISTS change_message_state_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE change_message_state_ex(IN p_session_uid        CHAR(36),
                                  IN p_submit_timestamp   TIMESTAMP,
                                  IN p_delivery_timestamp TIMESTAMP,
                                  IN p_state              INT)
BEGIN
  UPDATE `dispatching` t
  SET
    t.state = p_state
  WHERE
    t.uid = p_session_uid;

  IF p_state = 2 THEN
    CALL sms_delivered_ex(p_session_uid);
  END IF;
END
$$

--
-- Описание для процедуры change_short_message_state_ex
--
DROP PROCEDURE IF EXISTS change_short_message_state_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE change_short_message_state_ex(IN  p_message_id  INT,
                                        IN  p_timestamp   TIMESTAMP,
                                        IN  p_state       INT,
                                        OUT p_session_uid CHAR(36))
BEGIN
  DECLARE msg_count INT;

  UPDATE `message` t
  SET
    t.state = p_state, t.delivery_timestamp = p_timestamp
  WHERE
    t.message_id = p_message_id;

  SELECT t.session_uid
  INTO
    p_session_uid
  FROM
    `message` t
  WHERE
    t.message_id = p_message_id;

  IF p_state = 1 THEN
    SELECT count(`message_id`)
    INTO
      msg_count
    FROM
      `message`
    WHERE
      `session_uid` = p_session_uid;

    IF msg_count = 1 THEN
      UPDATE dispatching
      SET
        `state` = 2
      WHERE
        uid = p_session_uid;
    END IF;
  END IF;
END
$$

--
-- Описание для процедуры change_sm_command_status_ex
--
DROP PROCEDURE IF EXISTS change_sm_command_status_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE change_sm_command_status_ex(IN p_message_id INT,
                                      IN c_state      INT,
                                      IN c_timestamp  TIMESTAMP)
BEGIN
  DECLARE p_session_uid        CHAR(36);
  DECLARE p_source_number      VARCHAR(50);
  DECLARE p_destination_number VARCHAR(50);
  DECLARE p_message            VARCHAR(999);
  DECLARE p_service_type       INT;

  UPDATE `message` t
  SET
    t.command_status = c_state, t.command_timestamp = c_timestamp
  WHERE
    t.message_id = p_message_id;

  IF c_state = 12 OR c_state = 19 THEN
    SELECT t.session_uid
    INTO
      p_session_uid
    FROM
      `message` t
    WHERE
      t.message_id = p_message_id;
    SELECT `source_number`
         , `destination_number`
         , `message`
         , `service_type`
    INTO
      p_source_number, p_destination_number, p_message, p_service_type
    FROM
      dispatching
    WHERE
      uid = p_session_uid;
    SELECT schedule_ex(p_source_number, p_destination_number, p_message, p_service_type, 0, 0, md5(concat(p_session_uid, p_source_number, p_destination_number, p_message, p_service_type)));
  END IF;
  IF c_state = 12 THEN
    UPDATE dispatching
    SET
      state = 2
    WHERE
      uid = p_session_uid;
  END IF;
  IF c_state = 19 THEN
    UPDATE dispatching
    SET
      state = 4
    WHERE
      uid = p_session_uid;
  END IF;
/*Исправлено 26.08.2012*/
END
$$

--
-- Описание для процедуры deliver_message_ex
--
DROP PROCEDURE IF EXISTS deliver_message_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE deliver_message_ex(IN p_session_uid        CHAR(36),
                                                                  IN p_submit_timestamp   TIMESTAMP,
                                                                  IN p_delivery_timestamp TIMESTAMP
                                                                  )
BEGIN
  UPDATE `dispatching` t
  SET
    t.state = 2
  WHERE
    t.id = p_session_uid;

  CALL sms_delivered_ex(p_session_uid);
END
$$

--
-- Описание для процедуры deliver_short_message_ex
--
DROP PROCEDURE IF EXISTS deliver_short_message_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE deliver_short_message_ex(IN  p_message_id  INT,
                                   IN  p_timestamp   TIMESTAMP,
                                   OUT p_session_uid CHAR(36))
BEGIN
  UPDATE `message` t
  SET
    t.state = 1, t.delivery_timestamp = p_timestamp
  WHERE
    t.message_id = p_message_id;

  SELECT t.session_uid
  INTO
    p_session_uid
  FROM
    `message` t
  WHERE
    t.message_id = p_message_id;
END
$$

--
-- Описание для процедуры notify_received_ex
--
DROP PROCEDURE IF EXISTS notify_received_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE notify_received_ex(IN p_source_number      VARCHAR(50),
                             IN p_destination_number VARCHAR(50),
                             IN p_message            VARCHAR(160),
                             IN p_timestamp          TIMESTAMP)
BEGIN
  INSERT INTO `received` (`source_number`, `destination_number`, `message`, `timestamp`) VALUES (p_source_number, p_destination_number, p_message, p_timestamp);
END
$$

--
-- Описание для процедуры query_messages
--
DROP PROCEDURE IF EXISTS query_messages$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE query_messages()
BEGIN
  DECLARE do_query INT;

  SELECT if((`heartbeat` <= now() - INTERVAL 120 SECOND), 1, 0)
  INTO
    do_query
  FROM
    `daemon-status`
  WHERE
    `id` = 'SMSSenderHeartbeat-dtg';
  IF do_query = 1 OR do_query = 0 THEN

    UPDATE `dispatching` t
    SET
      t.worker = connection_id(), t.query_state = 1
    WHERE
      t.query_state = 0 LIMIT 30;
    /*10 строк, работает каждые 100 мс*/

    CREATE TABLE IF NOT EXISTS `smssender`.`batch`(
      `id` INT,
      `source_number` VARCHAR(50),
      `destination_number` VARCHAR(50),
      `message` VARCHAR(999),
      `message_type` INT,
      `state` INT,
      `worker` INT,
      `uid` CHAR(36)
    )
    ENGINE = HEAP DEFAULT CHARSET = utf8;
    DELETE
    FROM
      `batch`
    WHERE
      `worker` = connection_id();

    REPLACE INTO `smssender`.`batch` (`id`, `source_number`, `destination_number`, `message`, `message_type`, `state`, `worker`, `uid`)
      SELECT t.id
           , t.source_number
           , t.destination_number
           , t.message
           , t.message_type
           , t.state
           , connection_id() AS `worker`
           , t.uid
      FROM
        `dispatching` t
     WHERE t.query_state = 1;

    UPDATE `dispatching` t
      SET t.query_state = 2
     WHERE t.query_state = 1;
  END IF;
END
$$

--
-- Описание для процедуры replace_short_message_ex
--
DROP PROCEDURE IF EXISTS replace_short_message_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE replace_short_message_ex(IN p_message_id INT,
                                   IN p_message    VARCHAR(160))
BEGIN
  UPDATE `message` t
  SET
    t.message = p_message
  WHERE
    t.message_id = p_message_id;
END
$$

--
-- Описание для процедуры sms_delivered_ex
--
DROP PROCEDURE IF EXISTS sms_delivered_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE sms_delivered_ex(IN p_sms_uid CHAR(36))
BEGIN DECLARE done                           INT DEFAULT 0;
  DECLARE destination_number             CHAR(20);
  DECLARE source_number                  CHAR(20);
  DECLARE missed_call_uid                CHAR(36);
  DECLARE thisop                        INT;
  DECLARE require_send_subscriber_online INT;
  DECLARE cur1 CURSOR FOR
  SELECT if(mc.`destination_number` LIKE '55%', concat('996', mc.`destination_number`), mc.`destination_number`) AS `destination_number`
       , if(mc.`source_number` LIKE '55%', concat('996', mc.`source_number`), mc.`source_number`) AS `source_number`
       , mc.`uid` AS `missed_call_uid`
       , mc.`require_send_subscriber_online`
       , if((mc.`source_number` LIKE '99655_______' OR mc.`source_number` LIKE '55_______'), 1, 0) AS `thisop`
  FROM
    smssender.`dispatching` AS td
  LEFT JOIN smssender.`missed_call` AS mc
  ON td.`destination_number` = mc.`destination_number`
  WHERE
    td.`uid` = p_sms_uid
    AND mc.`count` > 0;
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = 1;
  /*
  INSERT INTO `sms_delivery` (`sms_id`, `sms_state`) VALUES (p_sms_id, 0);*/

  OPEN cur1;

  REPEAT
    FETCH cur1 INTO destination_number, source_number, missed_call_uid, require_send_subscriber_online, thisop;
    IF done = 0 THEN
      IF require_send_subscriber_online = 1 AND thisop = 1 THEN
        SELECT schedule_ex('388', source_number, concat('Abonent +', destination_number, ' snova na svyazi'), 109, 0, 0, md5(concat(p_sms_uid, source_number, destination_number, missed_call_uid)));
      END
      IF;
      DELETE
      FROM
        `smssender`.`missed_call`
      WHERE
        `uid` = missed_call_uid
        OR `last_call_time` <= now() - INTERVAL 2 DAY;
    END IF;
  UNTIL done = 1
  END REPEAT;

  CLOSE cur1;
END
$$

--
-- Описание для процедуры submit_short_message_ex
--
DROP PROCEDURE IF EXISTS submit_short_message_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE submit_short_message_ex(IN p_session_uid CHAR(36),
                                  IN p_message_id  INT,
                                  IN p_message     VARCHAR(160),
                                  IN p_timestamp   TIMESTAMP)
BEGIN
  INSERT INTO `message` (`message_id`, `message`, `submit_timestamp`, `session_uid`) VALUES (p_message_id, p_message, p_timestamp, p_session_uid);

  UPDATE `dispatching` t
  SET
    t.state = 1
  WHERE
    t.uid = p_session_uid;
END
$$

--
-- Описание для процедуры update_status_ex
--
DROP PROCEDURE IF EXISTS update_status_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE update_status_ex(IN p_session_uid CHAR(36),
                           IN p_state       TINYINT,
                           IN p_message_id  INT,
                           IN p_message     VARCHAR(160),
                           IN p_timestamp   TIMESTAMP)
BEGIN
  IF p_state = 1 THEN
    UPDATE `dispatching`
    SET
      `state` = p_state, `message_id` = p_message_id, `submit_timestamp` = p_timestamp
    WHERE
      `session_uid` = p_session_uid;
  ELSEIF p_state = 2 THEN
    UPDATE `dispatching`
    SET
      `state` = p_state, `message_id` = p_message_id, `delivery_timestamp` = p_timestamp
    WHERE
      `session_uid` = p_session_uid;
  ELSEIF p_state = 3 THEN
    UPDATE `dispatching`
    SET
      `state` = p_state, `message` = p_message, `replace_timestamp` = p_timestamp
    WHERE
      `message_uid` = p_message_uid;
  ELSE
    UPDATE `dispatching`
    SET
      `state` = p_state, `message_id` = p_message_id
    WHERE
      `session_uid` = p_session_uid;
  END IF;
END
$$

--
-- Описание для функции schedule_ex
--
DROP FUNCTION IF EXISTS schedule_ex$$
CREATE DEFINER = 'sms'@'localhost'
FUNCTION schedule_ex(p_source_number      VARCHAR(50),
                     p_destination_number VARCHAR(50),
                     p_message            VARCHAR(999),
                     p_service_type       INT,
                     p_message_type       INT,
                     p_replace            INT,
                     p_uid                CHAR(36))
  RETURNS int(11)
BEGIN
  DECLARE v_id    INT;
  DECLARE v_uid   CHAR(36);
  DECLARE v_state INT;

  IF p_replace != 0 && p_message_type = 0 THEN
    SELECT t.uid
         , t.id
         , t.state
    INTO
      v_uid, v_id, v_state
    FROM
      `dispatching` t
    WHERE
      t.source_number = p_source_number
      AND t.service_type = p_service_type
      AND t.state IN (0, 1, 3)
      AND t.destination_number = p_destination_number;

    IF v_uid IS NOT NULL THEN
      IF v_state = 1 THEN
        SET v_state = 3;
      END IF;

      UPDATE `dispatching` t
      SET
        t.message = p_message, t.state = v_state, t.query_state = 0
      WHERE
        t.uid = v_uid;

      RETURN v_id;
    END IF;
  END IF;

  REPLACE INTO `dispatching` (`source_number`, `destination_number`, `message`, `service_type`, `message_type`, `uid`)
    VALUES (p_source_number, p_destination_number, p_message, p_service_type, p_message_type, p_uid);

  RETURN LAST_INSERT_ID();
END
$$

DELIMITER ;

--
-- Описание для представления dispatching_count_grouped
--
DROP VIEW IF EXISTS dispatching_count_grouped CASCADE;
CREATE OR REPLACE 
	DEFINER = 'root'@'localhost'
VIEW dispatching_count_grouped
AS
	select `s`.`value` AS `value`,count(`m`.`state`) AS `statecnt` from (`message` `m` join `short_message_state` `s` on((`m`.`state` = `s`.`id`))) group by `m`.`state` order by `m`.`state`;

--
-- Описание для представления message_count_grouped
--
DROP VIEW IF EXISTS message_count_grouped CASCADE;
CREATE OR REPLACE 
	DEFINER = 'root'@'localhost'
VIEW message_count_grouped
AS
	select concat(`s`.`value`,_utf8' (',`d`.`state`,_utf8')') AS `state`,count(`d`.`state`) AS `statecnt` from (`dispatching` `d` join `message_state` `s` on((`d`.`state` = `s`.`id`))) group by `d`.`state` order by `d`.`state`;

--
-- Описание для представления message_count_uid
--
DROP VIEW IF EXISTS message_count_uid CASCADE;
CREATE OR REPLACE 
	DEFINER = 'root'@'localhost'
VIEW message_count_uid
AS
	select `m`.`session_uid` AS `session_uid`,`d`.`destination_number` AS `destination_number`,max(`m`.`submit_timestamp`) AS `submit_timestamp`,count(`m`.`session_uid`) AS `count`,count(distinct `m`.`message`) AS `parts`,`d`.`state` AS `d_state`,max(`m`.`state`) AS `m_state` from (`message` `m` join `dispatching` `d` on((`m`.`session_uid` = `d`.`uid`))) group by `m`.`session_uid` order by count(`m`.`session_uid`) desc;

--
-- Описание для представления queue_replace_error
--
DROP VIEW IF EXISTS queue_replace_error CASCADE;
CREATE OR REPLACE 
	DEFINER = 'root'@'localhost'
VIEW queue_replace_error
AS
	select `m`.`session_uid` AS `session_uid`,`m`.`message_id` AS `message_id`,concat(`t4`.`value`,_utf8' (',`m`.`state`,_utf8')') AS `message_state`,concat(`t3`.`value`,_utf8' (',`d`.`state`,_utf8')') AS `dispatching_state`,`d`.`state` AS `d_state`,`m`.`submit_timestamp` AS `submit_timestamp`,`m`.`delivery_timestamp` AS `delivery_timestamp`,concat(`cs`.`description`,_utf8' (',`cs`.`status`,_utf8')') AS `command_status`,`m`.`command_timestamp` AS `command_timestamp`,`d`.`destination_number` AS `destination_number` from ((((`message` `m` join `dispatching` `d` on((`m`.`session_uid` = `d`.`uid`))) left join `message_state` `t3` on((`d`.`state` = `t3`.`id`))) left join `short_message_state` `t4` on((`m`.`state` = `t4`.`id`))) left join `smpp_cs` `cs` on((`m`.`command_status` = `cs`.`status`))) where ((`m`.`command_status` <> 0) and (`d`.`state` <> 2) and (`d`.`state` <> 4)) order by `m`.`session_uid`,`t4`.`value`;

--
-- Описание для представления sms_in_second_last_hour
--
DROP VIEW IF EXISTS sms_in_second_last_hour CASCADE;
CREATE OR REPLACE 
	DEFINER = 'root'@'localhost'
VIEW sms_in_second_last_hour
AS
	select `message`.`submit_timestamp` AS `submit_timestamp`,count(`message`.`submit_timestamp`) AS `count(``submit_timestamp``)` from `message` where (`message`.`submit_timestamp` >= (now() - interval 1 hour)) group by `message`.`submit_timestamp` order by 1;

--
-- Описание для представления message_duplicates
--
DROP VIEW IF EXISTS message_duplicates CASCADE;
CREATE OR REPLACE 
	DEFINER = 'root'@'localhost'
VIEW message_duplicates
AS
	select `message_count_uid`.`session_uid` AS `session_uid`,`message_count_uid`.`destination_number` AS `destination_number`,`message_count_uid`.`submit_timestamp` AS `submit_timestamp`,`message_count_uid`.`count` AS `count`,`message_count_uid`.`parts` AS `parts` from `message_count_uid` where (`message_count_uid`.`count` <> `message_count_uid`.`parts`) order by `message_count_uid`.`count` desc,`message_count_uid`.`submit_timestamp`;

--
-- Вывод данных для таблицы message_state
--
INSERT INTO message_state VALUES 
  (0, 'Scheduled'),
  (1, 'Submited'),
  (2, 'Delivered'),
  (3, 'Replaced'),
  (4, 'Undelivered');

-- 
-- Вывод данных для таблицы missed_call
--
-- Таблица smssender.missed_call не содержит данных

--
-- Вывод данных для таблицы short_message_state
--
INSERT INTO short_message_state VALUES 
  (0, 'Submited'),
  (1, 'Delivered'),
  (2, 'Expired'),
  (3, 'Deleted'),
  (4, 'Undeliverable'),
  (5, 'Accepted'),
  (6, 'Unknown'),
  (7, 'Rejected');


--
-- Вывод данных для таблицы message_type
--
INSERT INTO message_type VALUES
  (0, 'SM'),
  (1, 'USSD');

--
-- Вывод данных для таблицы smpp_cs
--
INSERT INTO smpp_cs VALUES 
  (0, 'ESME_ROK', 'No Error'),
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

-- 
-- Включение внешних ключей
-- 
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;