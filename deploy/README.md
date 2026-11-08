# Triển khai VIEREC lên VPS (upsc.vn) bằng Docker

```
Internet ─80/443─> nginx (HTTPS, file FE) ─/api/*─> app (Spring Boot :8383) ─> mysql (:3306)
                     ↑ chứng chỉ Let's Encrypt do container certbot cấp và tự gia hạn
```

Code lấy từ 2 repo GitHub, clone cạnh nhau trong một thư mục dự án trên VPS:

```
/opt/vierec/
├── VIEREC_BE/          git clone https://github.com/hoangvu8499/VIEREC_BE.git
│   └── deploy/         docker-compose.yml, nginx, vierec.sh, .env (chỉ có trên VPS)
└── VIEREC-_FE/         git clone https://github.com/hoangvu8499/VIEREC-_FE.git
```

Dữ liệu (database, file upload, chứng chỉ) nằm trong Docker volume, không nằm trong thư mục code,
nên `git pull` hay xoá code không làm mất dữ liệu.

## 0. Trỏ tên miền (Tenten)

Trong trang quản lý DNS `upsc.vn` trên tenten.vn, thêm 2 bản ghi **A**: `@` → IP VPS và `www` → IP VPS.
Kiểm tra: `dig +short upsc.vn` và `dig +short www.upsc.vn` đều ra IP VPS.

## 1. Cài Docker trên VPS (một lần)

VPS hiện tại chạy AlmaLinux/Rocky (dòng RHEL, dùng `dnf`, tường lửa `firewalld`):

```bash
dnf -y update && dnf -y install git nano firewalld
timedatectl set-timezone Asia/Ho_Chi_Minh
curl -fsSL https://get.docker.com | sh
systemctl enable --now docker      # trên dòng RHEL Docker không tự chạy sau khi cài

# Swap 2 GB
fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

# Tường lửa
systemctl enable --now firewalld
firewall-cmd --permanent --add-service=ssh --add-service=http --add-service=https && firewall-cmd --reload
```

Trên Ubuntu/Debian thì thay `dnf` bằng `apt`, `firewalld` bằng `ufw`.

## 2. Tạo thư mục dự án và pull code

```bash
mkdir -p /opt/vierec
cd /opt/vierec
git clone https://github.com/hoangvu8499/VIEREC_BE.git
git clone https://github.com/hoangvu8499/VIEREC-_FE.git
```

## 3. Cấu hình

```bash
cd /opt/vierec/VIEREC_BE/deploy
chmod +x vierec.sh backup.sh
./vierec.sh setup         # tạo .env từ .env.example (và clone FE nếu chưa có)
openssl rand -base64 48   # chạy 3 lần, dán vào MYSQL_ROOT_PASSWORD, DB_PASSWORD, JWT_SECRET
nano .env
```

## 4. Database

**Không dùng `DB.sql`:** file đó tạo database `VIEREC` (chữ hoa) trong khi app kết nối `vierec`;
trên Linux tên DB phân biệt hoa thường. Dump schema từ DB thật trên máy bạn rồi đưa lên:

```bash
# Trên máy bạn
mysqldump -uroot -p --no-data vierec > schema.sql
mysqldump -uroot -p --no-create-info vierec roles permissions role_permissions > seed.sql
scp schema.sql seed.sql root@IP_VPS:/opt/vierec/
```

```bash
# Trên VPS
cd /opt/vierec/VIEREC_BE/deploy
./vierec.sh db-import /opt/vierec/schema.sql
./vierec.sh db-import /opt/vierec/seed.sql
```

Không import `mock_data.sql` lên server thật (tài khoản mật khẩu `123456`).

## 5. Chứng chỉ HTTPS (một lần, sau khi DNS đã trỏ đúng)

```bash
./vierec.sh cert email_cua_ban@gmail.com
```

## 6. Start / stop

```bash
./vierec.sh start      # build + chạy tất cả, mở https://upsc.vn
./vierec.sh stop       # dừng tất cả (giữ nguyên dữ liệu)
./vierec.sh restart    # khởi động lại tất cả; hoặc ./vierec.sh restart app
./vierec.sh status     # trạng thái container, RAM/CPU, dung lượng ổ
./vierec.sh logs app   # xem log (app | web | mysql | certbot), Ctrl+C để thoát
```

## 7. Cập nhật code mới

Push code lên nhánh `main` của 2 repo, rồi trên VPS:

```bash
cd /opt/vierec/VIEREC_BE/deploy
./vierec.sh update     # git pull cả 2 repo, build lại, chạy lại
```

Nếu có file SQL mới trong `VIEREC_BE/db/` thì chạy nó trước (`./vierec.sh db-import ...`), vì app dùng
`ddl-auto: validate` và sẽ không khởi động nếu thiếu cột. Sửa `web/nginx.conf` thì `./vierec.sh restart web`.

## 8. Sao lưu

```bash
./vierec.sh backup     # sao lưu ngay vào /opt/vierec-backups
crontab -e             # tự động 2h sáng mỗi ngày, thêm dòng:
0 2 * * * /opt/vierec/VIEREC_BE/deploy/backup.sh >> /var/log/vierec-backup.log 2>&1
```

Bản sao lưu nằm cùng ổ VPS; nên chép thêm ra ngoài (`rclone` lên Google Drive, hoặc `scp` về máy).

Khôi phục DB:

```bash
gunzip -c /opt/vierec-backups/db_XXXX.sql.gz > /tmp/restore.sql && ./vierec.sh db-import /tmp/restore.sql
```

Chuyển file upload cũ từ máy lên (nếu cần): `scp -r uploads root@IP_VPS:/opt/vierec/`, rồi trên VPS

```bash
docker compose cp /opt/vierec/uploads/. app:/app/uploads/
docker compose exec -u root app chown -R app:app /app/uploads
```
