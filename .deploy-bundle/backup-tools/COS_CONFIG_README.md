# 腾讯 COS 备份配置说明

以下步骤仅需执行一次，完成后定时任务会自动同步 SQL 备份到 COS。

## 1) 在服务器安装 rclone（如未安装）

```bash
curl https://rclone.org/install.sh | sudo bash
rclone version
```

## 2) 配置 rclone 远端（示例名：`cos`）

```bash
rclone config
```

按提示填写（示例）：

- `n`（new remote）
- name: `cos`
- Storage: `s3`
- provider: `TencentCOS`
- env_auth: `false`
- access_key_id: 你的 `SecretId`
- secret_access_key: 你的 `SecretKey`
- endpoint: 你的地域域名，例如 `cos.ap-guangzhou.myqcloud.com`
- acl / 其它项：按默认即可

可用下面命令验证：

```bash
rclone lsd cos:
```

## 3) 编辑备份配置

编辑服务器文件：`/root/deploy/tools/backup/.env`

二选一：

### 方式 A（推荐）

```bash
BACKUP_REMOTE=cos:你的bucket名/db-backups
```

### 方式 B（分项）

```bash
RCLONE_REMOTE=cos
BACKUP_REMOTE_PATH=你的bucket名/db-backups
```

## 4) 手动测试一次同步

```bash
/root/deploy/tools/backup/sync_offsite.sh
```

## 5) 当前定时任务

- `03:10` 本地数据库备份：`db_backup.sh`
- `03:30` 云端同步：`sync_offsite.sh`

查看任务：

```bash
crontab -l
```

查看日志：

```bash
tail -f /root/deploy/db-backups/backup.log
tail -f /root/deploy/db-backups/cron.log
```
