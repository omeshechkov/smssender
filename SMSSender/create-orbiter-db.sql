use orbiter;

drop table if exists `shared_lock`;
create table `shared_lock` (
  cluster_id int(11) not null,
  name varchar(255) not null,
  update_time datetime default null,

  constraint `pk_shared_lock` primary key(cluster_id)
)
engine = innodb
avg_row_length = 3276
character set utf8
collate utf8_general_ci;

delimiter $$

drop function if exists `acquire_lock`;
create function `acquire_lock`(p_cluster_id int(11),
                               p_worker_name varchar(255))
returns varchar(255)
begin
    declare v_current_worker_name varchar(255);
    declare v_non_active_time int;

    select t.name,
           timestampdiff(second, t.update_time, now())
       into v_current_worker_name,
            v_non_active_time
     from shared_lock t
     where t.cluster_id = p_cluster_id
     for update;

    if p_worker_name = v_current_worker_name or v_non_active_time > 10 then
      update shared_lock t
        set t.name = p_worker_name,
            t.update_time = now()
       where t.cluster_id = p_cluster_id;

      set v_current_worker_name = p_worker_name;
    end if;

    return v_current_worker_name;
end
$$

delimiter ;

insert into shared_lock(cluster_id, name, update_time) values(1, 'init', now());