-- Скрипт сгенерирован Devart dbForge Studio for MySQL, Версия 6.0.568.0
-- Домашняя страница продукта: http://www.devart.com/ru/dbforge/mysql/studio
-- Дата скрипта: 26.11.2013 17:09:53
-- Версия сервера: 5.6.14-log
-- Версия клиента: 4.1

-- 
-- Отключение внешних ключей
-- 
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@foreign_key_checks, foreign_key_checks = 0 */;

-- 
-- Установка кодировки, с использованием которой клиент будет посылать запросы на сервер
--
SET NAMES 'utf8';

SET GLOBAL log_bin_trust_function_creators = 1;
-- 
-- Установка базы данных по умолчанию
--
USE smssender;

--
-- Описание для таблицы `daemon-status`
--
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

--
-- Описание для таблицы `not-ready`
--
DROP TABLE IF EXISTS `not-ready`;
CREATE TABLE `not-ready` (
  id int(11) NOT NULL AUTO_INCREMENT,
  source_number varchar(50) NOT NULL,
  destination_number varchar(50) NOT NULL,
  message varchar(999) NOT NULL,
  message_type int(11) NOT NULL,
  state int(11) NOT NULL,
  submit_timestamp timestamp NULL DEFAULT NULL,
  delivery_timestamp timestamp NULL DEFAULT NULL,
  parts int(3) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  INDEX delivery_timestamp (delivery_timestamp),
  INDEX destination_number (destination_number),
  INDEX message (message (255)),
  INDEX message_type (message_type),
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
  id int(11) DEFAULT NULL,
  source_number varchar(50) DEFAULT NULL,
  destination_number varchar(50) DEFAULT NULL,
  message varchar(999) DEFAULT NULL,
  message_type int(1) DEFAULT NULL,
  state int(11) DEFAULT NULL,
  worker int(11) DEFAULT NULL,
  uid char(36) NOT NULL DEFAULT ''
)
ENGINE = INNODB
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы dispatching
--
DROP TABLE IF EXISTS dispatching;
CREATE TABLE dispatching (
  id int(11) NOT NULL AUTO_INCREMENT,
  uid char(36) NOT NULL DEFAULT '',
  source_number varchar(50) NOT NULL,
  destination_number varchar(50) NOT NULL,
  message varchar(999) NOT NULL,
  message_type int(11) NOT NULL,
  service_type int(11) NOT NULL,
  state int(11) DEFAULT 0,
  worker int(11) DEFAULT NULL,
  query_state int(11) DEFAULT 0,
  record_timestamp timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX destination_number (destination_number),
  INDEX message_type (message_type),
  INDEX query_state (query_state),
  INDEX record_timestamp (record_timestamp),
  INDEX source_number (source_number),
  INDEX state (state),
  UNIQUE INDEX uid_2 (uid),
  INDEX worker (worker)
)
ENGINE = INNODB
AUTO_INCREMENT = 3
AVG_ROW_LENGTH = 8192
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы message
--
DROP TABLE IF EXISTS message;
CREATE TABLE message (
  id int(11) NOT NULL AUTO_INCREMENT,
  uid char(36) NOT NULL DEFAULT '',
  session_id int(11) NOT NULL,
  session_uid char(36) NOT NULL DEFAULT '',
  message_id int(11) NOT NULL,
  message varchar(160) NOT NULL,
  state int(11) DEFAULT 0,
  submit_timestamp timestamp NULL DEFAULT NULL,
  delivery_timestamp timestamp NULL DEFAULT NULL,
  command_status int(4) DEFAULT NULL,
  command_timestamp timestamp NULL DEFAULT NULL,
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
AUTO_INCREMENT = 3
AVG_ROW_LENGTH = 16384
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы message_state
--
DROP TABLE IF EXISTS message_state;
CREATE TABLE message_state (
  id int(11) NOT NULL,
  value varchar(30) DEFAULT NULL,
  INDEX id_index (id)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 3276
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы message_type
--
DROP TABLE IF EXISTS message_type;
CREATE TABLE message_type (
  id int(11) NOT NULL,
  value varchar(30) DEFAULT NULL,
  INDEX id_index (id)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 20
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы missed_call
--
DROP TABLE IF EXISTS missed_call;
CREATE TABLE missed_call (
  id int(11) NOT NULL AUTO_INCREMENT,
  uid char(36) NOT NULL DEFAULT '',
  source_number varchar(20) NOT NULL,
  destination_number varchar(20) NOT NULL,
  count int(11) NOT NULL,
  last_call_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  require_send_subscriber_online int(11) DEFAULT NULL,
  PRIMARY KEY (id),
  INDEX count (count),
  INDEX destination_number (destination_number),
  INDEX last_call_time (last_call_time),
  INDEX source_number (source_number),
  UNIQUE INDEX uid_2 (uid)
)
ENGINE = INNODB
AUTO_INCREMENT = 1
AVG_ROW_LENGTH = 4096
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы ready
--
DROP TABLE IF EXISTS ready;
CREATE TABLE ready (
  id int(11) NOT NULL AUTO_INCREMENT,
  source_number varchar(50) NOT NULL,
  destination_number varchar(50) NOT NULL,
  message varchar(999) NOT NULL,
  service_type int(11) NOT NULL,
  message_type int(11) NOT NULL,
  submit_timestamp timestamp NULL DEFAULT NULL,
  delivery_timestamp timestamp NULL DEFAULT NULL,
  parts int(3) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  INDEX delivery_timestamp (delivery_timestamp),
  INDEX destination_number (destination_number),
  INDEX message (message (255)),
  INDEX message_type (message_type),
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
  id int(11) NOT NULL AUTO_INCREMENT,
  source_number varchar(50) NOT NULL,
  destination_number varchar(50) NOT NULL,
  message varchar(999) NOT NULL,
  timestamp timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX destination_number (destination_number),
  INDEX message (message (255)),
  INDEX source_number (source_number),
  INDEX timestamp (timestamp)
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
  id int(11) NOT NULL,
  value varchar(30) DEFAULT NULL,
  INDEX id_index (id)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 2048
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы smpp_cs
--
DROP TABLE IF EXISTS smpp_cs;
CREATE TABLE smpp_cs (
  status int(4) NOT NULL,
  name varchar(100) NOT NULL,
  description varchar(255) NOT NULL,
  PRIMARY KEY (status),
  INDEX description (description),
  INDEX name (name)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 297
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы sms_texts
--
DROP TABLE IF EXISTS sms_texts;
CREATE TABLE sms_texts (
  type varchar(50) NOT NULL,
  rus text NOT NULL,
  kyr text NOT NULL,
  eng text NOT NULL,
  uzb text NOT NULL,
  `desc` text NOT NULL,
  PRIMARY KEY (type)
)
ENGINE = INNODB
AVG_ROW_LENGTH = 8192
CHARACTER SET utf8
COLLATE utf8_general_ci;

--
-- Описание для таблицы ussd_commands
--
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

--
-- Описание для таблицы ussd_texts
--
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

--
-- Описание для процедуры change_message_state
--
DROP PROCEDURE IF EXISTS change_message_state$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE change_message_state (IN p_session_id int,
IN p_submit_timestamp timestamp,
IN p_delivery_timestamp timestamp,
IN p_state int)
BEGIN
  UPDATE `dispatching` t
  SET t.state = p_state
  WHERE t.id = p_session_id;

  IF p_state = 2 THEN
    CALL sms_delivered(p_session_id);
  END IF;
END
$$

--
-- Описание для процедуры change_message_state_ex
--
DROP PROCEDURE IF EXISTS change_message_state_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE change_message_state_ex (IN p_session_uid char(36),
IN p_submit_timestamp timestamp,
IN p_delivery_timestamp timestamp,
IN p_state int)
BEGIN
  UPDATE `dispatching` t
  SET t.state = p_state
  WHERE t.uid = p_session_uid;

  IF p_state = 2 THEN
    CALL sms_delivered_ex(p_session_uid);
  END IF;
END
$$

--
-- Описание для процедуры change_short_message_state
--
DROP PROCEDURE IF EXISTS change_short_message_state$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE change_short_message_state (IN p_message_id int,
IN p_timestamp timestamp,
IN p_state int,
OUT p_session_id int)
BEGIN
  UPDATE `message` t
  SET t.state = p_state,
      t.delivery_timestamp = p_timestamp
  WHERE t.message_id = p_message_id;

  SELECT
    t.session_id INTO p_session_id
  FROM `message` t
  WHERE t.message_id = p_message_id;
END
$$

--
-- Описание для процедуры change_short_message_state_ex
--
DROP PROCEDURE IF EXISTS change_short_message_state_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE change_short_message_state_ex (IN p_message_id int,
IN p_timestamp timestamp,
IN p_state int,
OUT p_session_uid char(36))
BEGIN
  DECLARE msg_count int;

  UPDATE `message` t
  SET t.state = p_state,
      t.delivery_timestamp = p_timestamp
  WHERE t.message_id = p_message_id;

  SELECT
    t.session_uid INTO p_session_uid
  FROM `message` t
  WHERE t.message_id = p_message_id;

  IF p_state = 1 THEN
    SELECT
      COUNT(`message_id`) INTO msg_count
    FROM `message`
    WHERE `session_uid` = p_session_uid;

    IF msg_count = 1 THEN
      UPDATE dispatching
      SET `state` = 2
      WHERE uid = p_session_uid;
    END IF;
  END IF;
END
$$

--
-- Описание для процедуры change_sm_command_status_ex
--
DROP PROCEDURE IF EXISTS change_sm_command_status_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE change_sm_command_status_ex (IN p_message_id int,
IN c_state int,
IN c_timestamp timestamp/*,
                                                OUT p_session_uid CHAR(36)*/
)
BEGIN
  DECLARE p_session_uid char(36);
  DECLARE p_source_number varchar(50);
  DECLARE p_destination_number varchar(50);
  DECLARE p_message varchar(999);
  DECLARE p_message_type int;

  UPDATE `message` t
  SET t.command_status = c_state,
      t.command_timestamp = c_timestamp
  WHERE t.message_id = p_message_id;
  /*Исправлено 26.08.2012*/
  IF c_state = 12 OR c_state = 19 THEN
    SELECT
      t.session_uid INTO p_session_uid
    FROM `message` t
    WHERE t.message_id = p_message_id;
    SELECT
      `source_number`,
      `destination_number`,
      `message`,
      `message_type` INTO p_source_number, p_destination_number, p_message, p_message_type
    FROM dispatching
    WHERE uid = p_session_uid;
    SELECT
      schedule_ex(p_source_number, p_destination_number, p_message, p_message_type, 0, MD5(CONCAT(p_session_uid, p_source_number, p_destination_number, p_message, p_message_type)));
  END IF;
  IF c_state = 12 THEN
    UPDATE dispatching
    SET state = 2
    WHERE uid = p_session_uid;
  END IF;
  IF c_state = 19 THEN
    UPDATE dispatching
    SET state = 4
    WHERE uid = p_session_uid;
  END IF;
/*Исправлено 26.08.2012*/
END
$$

--
-- Описание для процедуры deliver_message
--
DROP PROCEDURE IF EXISTS deliver_message$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE deliver_message (IN p_session_id int,
IN p_submit_timestamp timestamp,
IN p_delivery_timestamp timestamp)
BEGIN
  UPDATE `dispatching` t
  SET t.state = 2
  WHERE t.id = p_session_id;

  CALL sms_delivered(p_session_id);
END
$$

--
-- Описание для процедуры deliver_message_ex
--
DROP PROCEDURE IF EXISTS deliver_message_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE deliver_message_ex (IN p_session_uid char(36),
IN p_submit_timestamp timestamp,
IN p_delivery_timestamp timestamp)
BEGIN
  UPDATE `dispatching` t
  SET t.state = 2
  WHERE t.id = p_session_uid;

  CALL sms_delivered_ex(p_session_uid);
END
$$

--
-- Описание для процедуры deliver_short_message
--
DROP PROCEDURE IF EXISTS deliver_short_message$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE deliver_short_message (IN p_message_id int,
IN p_timestamp timestamp,
OUT p_session_id int)
BEGIN
  UPDATE `message` t
  SET t.state = 1,
      t.delivery_timestamp = p_timestamp
  WHERE t.message_id = p_message_id;

  SELECT
    t.session_id INTO p_session_id
  FROM `message` t
  WHERE t.message_id = p_message_id;
END
$$

--
-- Описание для процедуры deliver_short_message_ex
--
DROP PROCEDURE IF EXISTS deliver_short_message_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE deliver_short_message_ex (IN p_message_id int,
IN p_timestamp timestamp,
OUT p_session_uid char(36))
BEGIN
  UPDATE `message` t
  SET t.state = 1,
      t.delivery_timestamp = p_timestamp
  WHERE t.message_id = p_message_id;

  SELECT
    t.session_uid INTO p_session_uid
  FROM `message` t
  WHERE t.message_id = p_message_id;
END
$$

--
-- Описание для процедуры notify_received
--
DROP PROCEDURE IF EXISTS notify_received$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE notify_received (IN p_source_number varchar(50),
IN p_destination_number varchar(50),
IN p_message varchar(160),
IN p_timestamp timestamp)
BEGIN
  INSERT INTO `received` (`source_number`, `destination_number`, `message`, `timestamp`)
    VALUES (p_source_number, p_destination_number, p_message, p_timestamp);
END
$$

--
-- Описание для процедуры notify_received_ex
--
DROP PROCEDURE IF EXISTS notify_received_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE notify_received_ex (IN p_source_number varchar(50),
IN p_destination_number varchar(50),
IN p_message varchar(160),
IN p_timestamp timestamp)
BEGIN
  INSERT INTO `received` (`source_number`, `destination_number`, `message`, `timestamp`)
    VALUES (p_source_number, p_destination_number, p_message, p_timestamp);
END
$$

--
-- Описание для процедуры query_messages
--
DROP PROCEDURE IF EXISTS query_messages$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE query_messages ()
BEGIN
  DECLARE do_query int;

  SELECT
    IF((`heartbeat` <= NOW() - INTERVAL 120 SECOND), 1, 0) INTO do_query
  FROM `daemon-status`
  WHERE `id` = 'SMSSenderHeartbeat-v-node1';
  IF do_query = 1 OR do_query = 0 THEN

    UPDATE `dispatching` t
    SET t.worker = CONNECTION_ID(),
        t.query_state = 1
    WHERE t.query_state = 0 LIMIT 30;
    /*10 строк, работает каждые 100 мс*/

    CREATE TABLE IF NOT EXISTS `smssender`.`batch` (
      `id` int,
      `source_number` varchar(50),
      `destination_number` varchar(50),
      `message` varchar(999),
      `message_type` int,
      `state` int,
      `worker` int,
      `uid` char(36)
    )
    ENGINE = HEAP DEFAULT charset = utf8;
    DELETE
      FROM `batch`
    WHERE `worker` = CONNECTION_ID();


    REPLACE INTO `smssender`.`batch` (`id`, `source_number`, `destination_number`, `message`, `state`, `worker`, `uid`)
    SELECT
      t.id,
      t.source_number,
      t.destination_number,
      t.message,
      t.state,
      CONNECTION_ID() AS `worker`,
      t.uid
    FROM `dispatching` t
    WHERE /*t.worker = connection_id()
      AND */ t.query_state = 1;

    UPDATE `dispatching` t
    SET t.query_state = 2
    WHERE /*t.worker = connection_id()
      AND */ t.query_state = 1;
  END IF;
END
$$

--
-- Описание для процедуры replace_short_message
--
DROP PROCEDURE IF EXISTS replace_short_message$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE replace_short_message (IN p_message_id int,
IN p_message varchar(160))
BEGIN
  UPDATE `message` t
  SET t.message = p_message
  WHERE t.message_id = p_message_id;
END
$$

--
-- Описание для процедуры replace_short_message_ex
--
DROP PROCEDURE IF EXISTS replace_short_message_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE replace_short_message_ex (IN p_message_id int,
IN p_message varchar(160))
BEGIN
  UPDATE `message` t
  SET t.message = p_message
  WHERE t.message_id = p_message_id;
END
$$

--
-- Описание для процедуры sms_delivered
--
DROP PROCEDURE IF EXISTS sms_delivered$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE sms_delivered (IN p_sms_id int)
BEGIN
  DECLARE done int DEFAULT 0;
  DECLARE destination_number char(20);
  DECLARE source_number char(20);
  DECLARE missed_call_id int;
  DECLARE thisop int;
  DECLARE require_send_subscriber_online int;
  DECLARE cur1 CURSOR FOR
  SELECT
    IF(mc.`destination_number` LIKE '55%', CONCAT('996', mc.`destination_number`), mc.`destination_number`) AS `destination_number`,
    IF(mc.`source_number` LIKE '55%', CONCAT('996', mc.`source_number`), mc.`source_number`) AS `source_number`,
    mc.`id` AS `missed_call_id`,
    mc.`require_send_subscriber_online`,
    IF((mc.`source_number` LIKE '99655_______' OR mc.`source_number` LIKE '55_______'), 1, 0) AS `thisop`
  FROM smssender.`dispatching` AS td
    LEFT JOIN smssender.`missed_call` AS mc
      ON td.`destination_number` = mc.`destination_number`
  WHERE td.`id` = p_sms_id
  AND mc.`count` > 0;
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = 1;
  /*
  INSERT INTO `sms_delivery` (`sms_id`, `sms_state`) VALUES (p_sms_id, 0);*/

  OPEN cur1;

  REPEAT
    FETCH cur1 INTO destination_number, source_number, missed_call_id, require_send_subscriber_online, thisop;
    IF done = 0 THEN
      IF require_send_subscriber_online = 1 AND thisop = 1 THEN
        SELECT
          schedule_ex('970', source_number, CONCAT('Abonent +', destination_number, ' snova na svyazi'), 109, 0, MD5(CONCAT(p_sms_id, source_number, destination_number)));
      /*INSERT INTO smssender.test_sms_send (send_to, recipient, missed_call_id, message) VALUES (source_number, destination_number, missed_call_id, concat('Abonent +', destination_number, ' snova na svyazi'));*/
      END
      IF;
      DELETE
        FROM `smssender`.`missed_call`
      WHERE `id` = missed_call_id
        OR `last_call_time` <= NOW() - INTERVAL 2 DAY;
    END IF;
  UNTIL done = 1
  END REPEAT;

  CLOSE cur1;
END
$$

--
-- Описание для процедуры sms_delivered_ex
--
DROP PROCEDURE IF EXISTS sms_delivered_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE sms_delivered_ex (IN p_sms_uid char(36))
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
        SELECT
          schedule_ex('970', source_number, CONCAT('Abonent +', destination_number, ' snova na svyazi'), 109, 0, 0, MD5(CONCAT(p_sms_uid, source_number, destination_number, missed_call_uid)));
      END
      IF;
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

--
-- Описание для процедуры submit_short_message
--
DROP PROCEDURE IF EXISTS submit_short_message$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE submit_short_message (IN p_session_id int,
IN p_message_id int,
IN p_message varchar(160),
IN p_timestamp timestamp)
BEGIN
  DECLARE p_session_uid char(36);

  SELECT
    `uid` INTO p_session_uid
  FROM `dispatching`
  WHERE `id` = p_session_id
  LIMIT
  1;
  INSERT INTO `message` (`session_id`, `message_id`, `message`, `submit_timestamp`, `session_uid`)
    VALUES (p_session_id, p_message_id, p_message, p_timestamp, p_session_uid);

  UPDATE `dispatching` t
  SET t.state = 1
  WHERE t.id = p_session_id;
END
$$

--
-- Описание для процедуры submit_short_message_ex
--
DROP PROCEDURE IF EXISTS submit_short_message_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE submit_short_message_ex (IN p_session_uid char(36),
IN p_message_id int,
IN p_message varchar(160),
IN p_timestamp timestamp)
BEGIN
  INSERT INTO `message` (`message_id`, `message`, `submit_timestamp`, `session_uid`)
    VALUES (p_message_id, p_message, p_timestamp, p_session_uid);

  UPDATE `dispatching` t
  SET t.state = 1
  WHERE t.uid = p_session_uid;
END
$$

--
-- Описание для процедуры update_status
--
DROP PROCEDURE IF EXISTS update_status$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE update_status (IN p_session_id int,
IN p_state tinyint,
IN p_message_id int,
IN p_message varchar(160),
IN p_timestamp timestamp)
BEGIN
  IF p_state = 1 THEN
    UPDATE `dispatching`
    SET `state` = p_state,
        `message_id` = p_message_id,
        `submit_timestamp` = p_timestamp
    WHERE `session_id` = p_session_id;
  ELSEIF p_state = 2 THEN
    UPDATE `dispatching`
    SET `state` = p_state,
        `message_id` = p_message_id,
        `delivery_timestamp` = p_timestamp
    WHERE `session_id` = p_session_id;
  ELSEIF p_state = 3 THEN
    UPDATE `dispatching`
    SET `state` = p_state,
        `message` = p_message,
        `replace_timestamp` = p_timestamp
    WHERE `message_id` = p_message_id;
  ELSE
    UPDATE `dispatching`
    SET `state` = p_state,
        `message_id` = p_message_id
    WHERE `session_id` = p_session_id;
  END IF;
END
$$

--
-- Описание для процедуры update_status_ex
--
DROP PROCEDURE IF EXISTS update_status_ex$$
CREATE DEFINER = 'sms'@'localhost'
PROCEDURE update_status_ex (IN p_session_uid char(36),
IN p_state tinyint,
IN p_message_id int,
IN p_message varchar(160),
IN p_timestamp timestamp)
BEGIN
  IF p_state = 1 THEN
    UPDATE `dispatching`
    SET `state` = p_state,
        `message_id` = p_message_id,
        `submit_timestamp` = p_timestamp
    WHERE `session_uid` = p_session_uid;
  ELSEIF p_state = 2 THEN
    UPDATE `dispatching`
    SET `state` = p_state,
        `message_id` = p_message_id,
        `delivery_timestamp` = p_timestamp
    WHERE `session_uid` = p_session_uid;
  ELSEIF p_state = 3 THEN
    UPDATE `dispatching`
    SET `state` = p_state,
        `message` = p_message,
        `replace_timestamp` = p_timestamp
    WHERE `message_uid` = p_message_uid;
  ELSE
    UPDATE `dispatching`
    SET `state` = p_state,
        `message_id` = p_message_id
    WHERE `session_uid` = p_session_uid;
  END IF;
END
$$

--
-- Описание для функции ret_ussd_texts
--
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

--
-- Описание для функции schedule
--
DROP FUNCTION IF EXISTS schedule$$
CREATE DEFINER = 'sms'@'localhost'
FUNCTION schedule (p_source_number varchar(50),
p_destination_number varchar(50),
p_message varchar(999),
p_message_type int,
p_replace int)
RETURNS int(11)
BEGIN
  DECLARE v_id int;
  DECLARE v_state int;

  IF p_replace != 0 THEN
    SELECT
      t.id,
      t.state INTO v_id, v_state
    FROM `dispatching` t
    WHERE t.source_number = p_source_number
    AND t.message_type = p_message_type
    AND t.state IN (0, 1, 3)
    AND t.destination_number = p_destination_number;

    IF v_id IS NOT NULL THEN
      IF v_state = 1 THEN
        SET v_state = 3;
      END IF;

      UPDATE `dispatching` t
      SET t.message = p_message,
          t.state = v_state,
          t.query_state = 0
      WHERE t.id = v_id;

      RETURN v_id;
    END IF;
  END IF;

  INSERT INTO `dispatching` (`source_number`, `destination_number`, `message`, `message_type`)
    VALUES (p_source_number, p_destination_number, p_message, p_message_type);

  RETURN LAST_INSERT_ID();
END
$$

--
-- Описание для функции schedule_ex
--
DROP FUNCTION IF EXISTS schedule_ex$$
CREATE DEFINER = 'sms'@'localhost'
FUNCTION schedule_ex (p_source_number varchar(50),
p_destination_number varchar(50),
p_message varchar(999),
p_service_type int,
p_message_type int,
p_replace int,
p_uid char(36))
RETURNS int(11)
BEGIN
  DECLARE v_id int;
  DECLARE v_uid char(36);
  DECLARE v_state int;

  IF p_replace != 0 && p_message_type = 0 THEN
    SELECT
      t.uid,
      t.id,
      t.state INTO v_uid, v_id, v_state
    FROM `dispatching` t
    WHERE t.source_number = p_source_number
    AND t.service_type = p_service_type
    AND t.state IN (0, 1, 3)
    AND t.destination_number = p_destination_number;

    IF v_uid IS NOT NULL THEN
      IF v_state = 1 THEN
        SET v_state = 3;
      END IF;

      UPDATE `dispatching` t
      SET t.message = p_message,
          t.state = v_state,
          t.query_state = 0
      WHERE t.uid = v_uid;

      RETURN v_id;
    END IF;
  END IF;

  REPLACE INTO `dispatching` (`source_number`, `destination_number`, `message`, `service_type`, `message_type`, `uid`)
  VALUES (p_source_number, p_destination_number, p_message, p_service_type, p_message_type, p_uid);

  RETURN LAST_INSERT_ID();
END
$$

--
-- Описание для функции set_from_ussd
--
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

--
-- Описание для представления dispatching_count_grouped
--
DROP VIEW IF EXISTS dispatching_count_grouped CASCADE;
CREATE OR REPLACE
DEFINER = 'root'@'localhost'
VIEW dispatching_count_grouped
AS
SELECT
  `s`.`value` AS `value`,
  COUNT(`m`.`state`) AS `statecnt`
FROM (`message` `m`
  JOIN `short_message_state` `s`
    ON ((`m`.`state` = `s`.`id`)))
GROUP BY `m`.`state`
ORDER BY `m`.`state`;

--
-- Описание для представления message_count_grouped
--
DROP VIEW IF EXISTS message_count_grouped CASCADE;
CREATE OR REPLACE
DEFINER = 'root'@'localhost'
VIEW message_count_grouped
AS
SELECT
  CONCAT(`s`.`value`, _utf8 ' (', `d`.`state`, _utf8 ')') AS `state`,
  COUNT(`d`.`state`) AS `statecnt`
FROM (`dispatching` `d`
  JOIN `message_state` `s`
    ON ((`d`.`state` = `s`.`id`)))
GROUP BY `d`.`state`
ORDER BY `d`.`state`;

--
-- Описание для представления message_count_uid
--
DROP VIEW IF EXISTS message_count_uid CASCADE;
CREATE OR REPLACE
DEFINER = 'root'@'localhost'
VIEW message_count_uid
AS
SELECT
  `m`.`session_uid` AS `session_uid`,
  `d`.`destination_number` AS `destination_number`,
  MAX(`m`.`submit_timestamp`) AS `submit_timestamp`,
  COUNT(`m`.`session_uid`) AS `count`,
  COUNT(DISTINCT `m`.`message`) AS `parts`,
  `d`.`state` AS `d_state`,
  MAX(`m`.`state`) AS `m_state`
FROM (`message` `m`
  JOIN `dispatching` `d`
    ON ((`m`.`session_uid` = `d`.`uid`)))
GROUP BY `m`.`session_uid`
ORDER BY COUNT(`m`.`session_uid`) DESC;

--
-- Описание для представления queue_replace_error
--
DROP VIEW IF EXISTS queue_replace_error CASCADE;
CREATE OR REPLACE
DEFINER = 'root'@'localhost'
VIEW queue_replace_error
AS
SELECT
  `m`.`session_uid` AS `session_uid`,
  `m`.`message_id` AS `message_id`,
  CONCAT(`t4`.`value`, _utf8 ' (', `m`.`state`, _utf8 ')') AS `message_state`,
  CONCAT(`t3`.`value`, _utf8 ' (', `d`.`state`, _utf8 ')') AS `dispatching_state`,
  `d`.`state` AS `d_state`,
  `m`.`submit_timestamp` AS `submit_timestamp`,
  `m`.`delivery_timestamp` AS `delivery_timestamp`,
  CONCAT(`cs`.`description`, _utf8 ' (', `cs`.`status`, _utf8 ')') AS `command_status`,
  `m`.`command_timestamp` AS `command_timestamp`,
  `d`.`destination_number` AS `destination_number`
FROM ((((`message` `m`
  JOIN `dispatching` `d`
    ON ((`m`.`session_uid` = `d`.`uid`)))
  LEFT JOIN `message_state` `t3`
    ON ((`d`.`state` = `t3`.`id`)))
  LEFT JOIN `short_message_state` `t4`
    ON ((`m`.`state` = `t4`.`id`)))
  LEFT JOIN `smpp_cs` `cs`
    ON ((`m`.`command_status` = `cs`.`status`)))
WHERE ((`m`.`command_status` <> 0) AND (`d`.`state` <> 2) AND (`d`.`state` <> 4))
ORDER BY `m`.`session_uid`, `t4`.`value`;

--
-- Описание для представления sms_in_second_last_hour
--
DROP VIEW IF EXISTS sms_in_second_last_hour CASCADE;
CREATE OR REPLACE
DEFINER = 'root'@'localhost'
VIEW sms_in_second_last_hour
AS
SELECT
  `message`.`submit_timestamp` AS `submit_timestamp`,
  COUNT(`message`.`submit_timestamp`) AS `count(``submit_timestamp``)`
FROM `message`
WHERE (`message`.`submit_timestamp` >= (NOW() - INTERVAL 1 HOUR))
GROUP BY `message`.`submit_timestamp`
ORDER BY 1;

--
-- Описание для представления message_duplicates
--
DROP VIEW IF EXISTS message_duplicates CASCADE;
CREATE OR REPLACE
DEFINER = 'root'@'localhost'
VIEW message_duplicates
AS
SELECT
  `message_count_uid`.`session_uid` AS `session_uid`,
  `message_count_uid`.`destination_number` AS `destination_number`,
  `message_count_uid`.`submit_timestamp` AS `submit_timestamp`,
  `message_count_uid`.`count` AS `count`,
  `message_count_uid`.`parts` AS `parts`
FROM `message_count_uid`
WHERE (`message_count_uid`.`count` <> `message_count_uid`.`parts`)
ORDER BY `message_count_uid`.`count` DESC, `message_count_uid`.`submit_timestamp`;

-- 
-- Вывод данных для таблицы `daemon-status`
--

-- 
-- Вывод данных для таблицы `not-ready`
--

-- Таблица smssender.`not-ready` не содержит данных

-- 
-- Вывод данных для таблицы batch
--

-- Таблица smssender.batch не содержит данных

-- 
-- Вывод данных для таблицы dispatching
--

-- 
-- Вывод данных для таблицы message
--

-- 
-- Вывод данных для таблицы message_state
--
INSERT INTO message_state
  VALUES (0, 'Scheduled'),
  (1, 'Submited'),
  (2, 'Delivered'),
  (3, 'Replaced'),
  (4, 'Undelivered');

-- 
-- Вывод данных для таблицы message_type
--
INSERT INTO message_type
  VALUES (0, 'SM'),
  (1, 'USSD');

-- 
-- Вывод данных для таблицы missed_call
--

-- Таблица smssender.missed_call не содержит данных

-- 
-- Вывод данных для таблицы ready
--

-- Таблица smssender.ready не содержит данных

-- 
-- Вывод данных для таблицы received
--

-- Таблица smssender.received не содержит данных

-- 
-- Вывод данных для таблицы short_message_state
--
INSERT INTO short_message_state
  VALUES (0, 'Submited'),
  (1, 'Delivered'),
  (2, 'Expired'),
  (3, 'Deleted'),
  (4, 'Undeliverable'),
  (5, 'Accepted'),
  (6, 'Unknown'),
  (7, 'Rejected');

-- 
-- Вывод данных для таблицы smpp_cs
--
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

-- 
-- Вывод данных для таблицы sms_texts
--
INSERT INTO sms_texts
  VALUES ('sub_a_notify', 'Я снова на связи. Вы можете мне перезвонить', 'Мен кайра байланыштамын. Сиз мага кайрадан чала аласыз', 'I am in touch again. Yоu can call me back.', 'Men yana aloqadaman. Siz menga qayta qongiroq qilishingiz mumkin', 'Информирование абонента А о том, что абонент Б снова на связи'),
  ('sub_b_from_1_a', 'Вам звонили %N% раз, последний звонок в %LAST%', '', '', '', 'Информирование абонента Б о непринятом звонке, если не оставили голосовое сообщение и звонил один абонент');

-- 
-- Вывод данных для таблицы ussd_commands
--
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

-- 
-- Вывод данных для таблицы ussd_texts
--
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

-- 
-- Включение внешних ключей
-- 
/*!40014 SET foreign_key_checks = @OLD_FOREIGN_KEY_CHECKS */;