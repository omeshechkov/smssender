SELECT d.message, d.operation_type_id, sm.*, '|', sms.* FROM dispatching d
  LEFT JOIN `short_message` sm ON sm.dispatching_uid = d.uid
  LEFT JOIN `short_message_state` sms ON sms.id = sm.id
-- WHERE d.operation_type_id = 20
ORDER BY sm.`id`, sm.sequence, sms.`timestamp`