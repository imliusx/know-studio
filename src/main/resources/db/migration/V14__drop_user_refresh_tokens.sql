-- 登录态迁移至 Sa-Token + Redis，数据库不再保存 refresh token。
drop table if exists user_refresh_tokens;
