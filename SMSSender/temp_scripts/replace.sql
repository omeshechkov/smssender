CALL replace_short_message('Na_svyazi', '996555277699', CONCAT('для отмены 123 ', DATE_FORMAT(now(), '%H%i%s')), '', 109, UUID());
commit;