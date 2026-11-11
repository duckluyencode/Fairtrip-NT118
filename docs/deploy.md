# Deploy backend lên AWS

Hướng dẫn từng bước dựng hạ tầng và đưa backend TripFair lên AWS (bước B1–B4 trong [roadmap](roadmap.md)), kèm lý do của từng lựa chọn, để đọc xong có thể tự làm lại từ đầu trên một tài khoản khác. Các file dùng trong hướng dẫn nằm ở [backend/deploy/](../backend/deploy/).

## 1. Kiến trúc

```
App Android ──HTTPS──▶ EC2 t3.micro (Elastic IP, tên miền <ip>.sslip.io)
                        ├─ container tripfair-caddy  : nhận 80/443, tự lấy chứng chỉ Let's Encrypt
                        └─ container tripfair-api    : NestJS, cổng 3000 chỉ trong mạng Docker
                                    │ TLS (xác minh chứng chỉ RDS)
                                    ▼
                        RDS MySQL 8.4 db.t4g.micro (subnet riêng, không có đường ra Internet)

SSM Parameter Store : mọi cấu hình và secret (/tripfair/prod/*), EC2 đọc qua IAM role
ECR                 : image tripfair-api và tripfair-migrate, tag = commit
SSM Run Command     : chạy lệnh trên EC2, không mở cổng SSH
```

Nguyên tắc:

- **Least privilege**: security group của API chỉ mở 80/443; của database chỉ nhận 3306 từ security group của API. IAM role của EC2 chỉ đọc được `/tripfair/prod/*` và kéo được hai repository ECR của TripFair.
- **Không SSH**: quản trị máy qua SSM Run Command, nên không có khóa SSH để lộ và không mở cổng 22.
- **Không secret trong code hay image**: secret nằm trong SSM (SecureString, mã hóa bằng KMS); lúc deploy, script trên EC2 ghi chúng vào một file chỉ root đọc được.
- **Migration tách khỏi API**: image `tripfair-migrate` chạy `prisma migrate deploy` rồi thoát, chạy trước khi API khởi động, nên API không bao giờ chạy trên schema cũ hơn code.
- **Không dữ liệu mẫu ở production**: `prisma/seed.ts` từ chối chạy khi `NODE_ENV=production`; dữ liệu địa điểm thật lấy từ Google.

## 2. Chi phí và cách tiết kiệm

TripFair chạy ở region Sydney (`ap-southeast-2`), region mà tài khoản AWS hiện tại được gắn khi tạo. Singapore (`ap-southeast-1`) gần Việt Nam hơn (nhanh hơn khoảng 100 ms mỗi request); script không ghi cứng region nên đổi region chỉ cần đổi biến `$region` dưới đây. Giá tham khảo (Sydney và Singapore gần như nhau), kiểm tra lại trên trang giá của AWS:

| Tài nguyên | Khi chạy | Khi tắt |
| --- | --- | --- |
| EC2 t3.micro | ~0,013 USD/giờ | 0 (chỉ trả ổ EBS 20 GB) |
| RDS db.t4g.micro | ~0,025 USD/giờ | 0 (chỉ trả lưu trữ 20 GB và backup) |
| Elastic IP | 0,005 USD/giờ | 0,005 USD/giờ (mọi địa chỉ IPv4 công khai đều tính phí) |
| ECR | ~0,10 USD/GB/tháng | như khi chạy |

Cách dùng tiết kiệm: chỉ bật EC2 và RDS khi cần (deploy, demo, Android cần kết nối), xong thì tắt (mục 11). RDS tự bật lại sau 7 ngày bị tắt, nên phải tắt lại. Đặt cảnh báo chi phí (mục 3).

## 3. Chuẩn bị (B1)

1. Tài khoản AWS: bật MFA cho root, không dùng root để làm việc hằng ngày; tạo IAM user riêng cho mình.
2. Cài AWS CLI v2 (Windows: `winget install Amazon.AWSCLI`), lưu region một lần (`aws configure set region ap-southeast-2`), rồi đăng nhập bằng IAM user:

   ```bash
   aws login --region ap-southeast-2
   ```

   Lệnh mở trình duyệt để bạn đăng nhập (đăng nhập AWS Console trên web không thay được bước này). Phiên hết hạn sau vài giờ; khi lệnh báo `Your session has expired` thì chạy lại `aws login`. Đổi sang tài khoản khác: `aws logout` rồi `aws login`. Tài khoản tạo theo kiểu "project" chỉ cho đăng nhập ở region của project; sai region thì trang đăng nhập báo `not supported for your project` và chỉ ra lệnh cần chạy.
3. Đặt region cho mỗi cửa sổ PowerShell; các lệnh bên dưới dùng biến `$region`:

   ```powershell
   $region = 'ap-southeast-2'
   $env:AWS_REGION = $region
   ```

4. Cảnh báo chi phí: Billing → Budgets → Create budget → "Monthly cost budget", ví dụ 20 USD, gửi email khi vượt 80%.

Mọi tài nguyên được gắn tag `Project=TripFair` để lọc và tính chi phí riêng; không đụng vào tài nguyên của dự án khác trong cùng tài khoản.

Các lệnh bên dưới viết cho PowerShell trên Windows (`$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'`); trên macOS/Linux bỏ `& $aws` thành `aws` và đổi cú pháp biến.

## 4. Mạng (B3)

Dùng VPC mặc định (đã có subnet public và Internet Gateway) cho EC2, và tạo **hai subnet riêng ở hai Availability Zone** cho RDS (RDS yêu cầu subnet group trải ít nhất 2 AZ). Hai subnet này gắn vào một route table chỉ có route `local`, nên database không có đường ra hay vào Internet.

```powershell
$vpc = (& $aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)
$tag = 'Key=Project,Value=TripFair'
$rt = & $aws ec2 create-route-table --vpc-id $vpc --tag-specifications "ResourceType=route-table,Tags=[{Key=Name,Value=tripfair-db-private},{$tag}]" --query 'RouteTable.RouteTableId' --output text
# Subnet mặc định của VPC (172.31.0.0/16) đã chiếm 172.31.0.0/20, .16.0/20, .32.0/20 (mỗi AZ một cái).
# Xem các dải đang dùng trước, rồi chọn hai dải /20 còn trống, vd. 172.31.128.0/20 và 172.31.144.0/20:
& $aws ec2 describe-subnets --filters "Name=vpc-id,Values=$vpc" --query 'Subnets[].[CidrBlock,AvailabilityZone]' --output text
$a = & $aws ec2 create-subnet --vpc-id $vpc --availability-zone "${region}a" --cidr-block 172.31.128.0/20 --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=tripfair-db-a},{$tag}]" --query 'Subnet.SubnetId' --output text
$c = & $aws ec2 create-subnet --vpc-id $vpc --availability-zone "${region}c" --cidr-block 172.31.144.0/20 --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=tripfair-db-c},{$tag}]" --query 'Subnet.SubnetId' --output text
& $aws ec2 associate-route-table --route-table-id $rt --subnet-id $a
& $aws ec2 associate-route-table --route-table-id $rt --subnet-id $c
& $aws rds create-db-subnet-group --db-subnet-group-name tripfair-db-subnets --db-subnet-group-description 'TripFair database, private subnets' --subnet-ids $a $c --tags $tag
```

Security group:

```powershell
$api = & $aws ec2 create-security-group --group-name tripfair-api-sg --description 'TripFair API: HTTPS from anywhere, no SSH' --vpc-id $vpc --query 'GroupId' --output text
$db = & $aws ec2 create-security-group --group-name tripfair-db-sg --description 'TripFair database: MySQL from the API only' --vpc-id $vpc --query 'GroupId' --output text
# 80 cho Let's Encrypt xác minh tên miền và chuyển hướng sang HTTPS; 443 cho API.
& $aws ec2 authorize-security-group-ingress --group-id $api --ip-permissions 'IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges=[{CidrIp=0.0.0.0/0}]' 'IpProtocol=tcp,FromPort=443,ToPort=443,IpRanges=[{CidrIp=0.0.0.0/0}]'
# Database chỉ nhận kết nối từ máy nào mang security group của API, không theo IP.
& $aws ec2 authorize-security-group-ingress --group-id $db --ip-permissions "IpProtocol=tcp,FromPort=3306,ToPort=3306,UserIdGroupPairs=[{GroupId=$api}]"
```

## 5. Secret và RDS (B3)

Sinh ba secret ngẫu nhiên (mật khẩu master của RDS, mật khẩu user ứng dụng, JWT secret) và lưu thẳng vào SSM dạng SecureString. Không in ra màn hình, không ghi ra file.

```powershell
function New-Secret([int]$n) {
  $chars = [char[]]'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'
  $bytes = New-Object byte[] $n
  [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
  -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })
}
$master = New-Secret 32; $appPw = New-Secret 32; $jwt = New-Secret 48
& $aws ssm put-parameter --name /tripfair/admin/MYSQL_MASTER_PASSWORD --type SecureString --value $master --tags $tag
& $aws ssm put-parameter --name /tripfair/prod/MYSQL_PASSWORD --type SecureString --value $appPw --tags $tag
& $aws ssm put-parameter --name /tripfair/prod/JWT_SECRET --type SecureString --value $jwt --tags $tag
```

Chỉ dùng chữ và số để secret không bị hỏng khi nằm trong URL (`DATABASE_URL`) hay câu lệnh SQL. Mật khẩu master nằm ở `/tripfair/admin/`, ngoài đường dẫn EC2 đọc được.

Tạo RDS:

```powershell
& $aws rds create-db-instance --db-instance-identifier tripfair-db `
  --engine mysql --engine-version 8.4.11 --db-instance-class db.t4g.micro `
  --allocated-storage 20 --storage-type gp3 --storage-encrypted `
  --master-username tripfair_admin --master-user-password $master --db-name tripfair `
  --db-subnet-group-name tripfair-db-subnets --vpc-security-group-ids $db `
  --no-publicly-accessible --no-multi-az --backup-retention-period 1 `
  --auto-minor-version-upgrade --deletion-protection --copy-tags-to-snapshot --tags $tag
Remove-Variable master, appPw, jwt
```

Lý do: MySQL 8.4 LTS giống bản chạy trên máy; `db.t4g.micro` là loại rẻ nhất; mã hóa ổ đĩa; không public; backup tự động (tài khoản gói Free chỉ cho giữ 1 ngày, báo `FreeTierRestrictionError` nếu đặt nhiều hơn; tài khoản trả phí nên đặt 7); `--deletion-protection` chặn xóa nhầm. Chờ khoảng 10 phút tới khi trạng thái là `available`, rồi lấy endpoint:

```powershell
& $aws rds describe-db-instances --db-instance-identifier tripfair-db --query 'DBInstances[0].[DBInstanceStatus,Endpoint.Address]' --output text
```

## 6. ECR (B4)

Hai repository, tag không ghi đè được (một tag luôn là đúng một bản code), quét lỗ hổng khi đẩy, giữ 10 image gần nhất:

```powershell
Set-Content -Path "$env:TEMP\ecr-lifecycle.json" -Encoding ascii -Value '{"rules":[{"rulePriority":1,"description":"Keep the latest 10 images","selection":{"tagStatus":"any","countType":"imageCountMoreThan","countNumber":10},"action":{"type":"expire"}}]}'
foreach ($repo in 'tripfair-api', 'tripfair-migrate') {
  & $aws ecr create-repository --repository-name $repo --image-tag-mutability IMMUTABLE --image-scanning-configuration scanOnPush=true --tags $tag
  & $aws ecr put-lifecycle-policy --repository-name $repo --lifecycle-policy-text "file://$env:TEMP\ecr-lifecycle.json"
}
```

## 7. IAM role cho EC2 (B4)

Role gồm `AmazonSSMManagedInstanceCore` (để SSM Run Command điều khiển được máy) và một policy riêng [app-policy.template.json](../backend/deploy/iam/app-policy.template.json): đọc `/tripfair/prod/*`, giải mã KMS chỉ khi qua SSM, kéo hai image TripFair.

```powershell
$account = & $aws sts get-caller-identity --query Account --output text
(Get-Content backend\deploy\iam\app-policy.template.json) -replace 'ACCOUNT_ID', $account -replace 'REGION', $region | Set-Content "$env:TEMP\app-policy.json" -Encoding ascii
& $aws iam create-role --role-name tripfair-ec2-role --assume-role-policy-document file://backend/deploy/iam/ec2-trust.json --tags $tag
& $aws iam attach-role-policy --role-name tripfair-ec2-role --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
& $aws iam put-role-policy --role-name tripfair-ec2-role --policy-name tripfair-app --policy-document "file://$env:TEMP\app-policy.json"
& $aws iam create-instance-profile --instance-profile-name tripfair-ec2-profile
& $aws iam add-role-to-instance-profile --instance-profile-name tripfair-ec2-profile --role-name tripfair-ec2-role
```

## 8. EC2 (B4)

Amazon Linux 2023 (có sẵn AWS CLI và SSM agent); [user-data.sh](../backend/deploy/user-data.sh) cài Docker ở lần khởi động đầu. Lấy AMI mới nhất qua tham số công khai của AWS thay vì ghi cứng id:

```powershell
$ami = & $aws ssm get-parameter --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 --query Parameter.Value --output text
$subnet = & $aws ec2 describe-subnets --filters "Name=vpc-id,Values=$vpc" Name=default-for-az,Values=true "Name=availability-zone,Values=${region}b" --query 'Subnets[0].SubnetId' --output text
$iid = & $aws ec2 run-instances --image-id $ami --instance-type t3.micro --subnet-id $subnet `
  --security-group-ids $api --iam-instance-profile Name=tripfair-ec2-profile `
  --metadata-options HttpTokens=required,HttpPutResponseHopLimit=1,HttpEndpoint=enabled `
  --credit-specification CpuCredits=standard `
  --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=20,VolumeType=gp3,Encrypted=true,DeleteOnTermination=true}' `
  --user-data file://backend/deploy/user-data.sh `
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=tripfair-api},{Key=Project,Value=TripFair}]' `
  --query 'Instances[0].InstanceId' --output text
& $aws ec2 wait instance-running --instance-ids $iid
$alloc, $ip = (& $aws ec2 allocate-address --domain vpc --query '[AllocationId,PublicIp]' --output text) -split "`t"
& $aws ec2 associate-address --instance-id $iid --allocation-id $alloc
"site: $($ip.Replace('.', '-')).sslip.io"
```

Lý do: `HttpTokens=required` bắt buộc IMDSv2 (chặn lấy trộm credential của role qua lỗi SSRF); `CpuCredits=standard` không bị tính thêm tiền khi dùng CPU nhiều; Elastic IP giữ địa chỉ cố định qua các lần tắt/bật. `sslip.io` biến IP thành tên miền (vd. `13-250-10-20.sslip.io` trỏ về `13.250.10.20`) để có chứng chỉ HTTPS mà không cần mua tên miền.

Kiểm tra SSM thấy máy (vài phút sau khi bật):

```powershell
& $aws ssm describe-instance-information --filters "Key=InstanceIds,Values=$iid" --query 'InstanceInformationList[0].PingStatus' --output text
```

## 9. Cấu hình ứng dụng trong SSM (B4)

Mọi biến môi trường của API nằm dưới `/tripfair/prod/`; tên tham số chính là tên biến (khai báo trong [env.validation.ts](../backend/src/config/env.validation.ts)).

| Tham số | Kiểu | Giá trị |
| --- | --- | --- |
| NODE_ENV | String | `production` |
| PORT | String | `3000` |
| MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE, MYSQL_USER | String | endpoint RDS, `3306`, `tripfair`, `tripfair` |
| MYSQL_PASSWORD | SecureString | đã tạo ở mục 5 |
| MYSQL_SSL_CA | String | `/certs/rds-ca.pem` (đường dẫn trong container) |
| MYSQL_ALLOW_PUBLIC_KEY_RETRIEVAL | String | `false` |
| DATABASE_URL | SecureString | `mysql://tripfair:<MYSQL_PASSWORD>@<endpoint>:3306/tripfair?sslcert=/certs/rds-ca.pem&sslaccept=strict`, dùng cho migration |
| JWT_SECRET | SecureString | đã tạo ở mục 5 |
| TRUST_PROXY | String | `1` (đứng sau Caddy, để đọc đúng IP người dùng khi giới hạn request) |
| SITE_ADDRESS | String | `<ip-gạch-ngang>.sslip.io` |
| SMTP_HOST, SMTP_PORT | String | `smtp.gmail.com`, `465` |
| SMTP_USER, SMTP_FROM | String | địa chỉ Gmail gửi mã |
| SMTP_PASSWORD | SecureString | App Password của Gmail (người dùng tự tạo) |
| GOOGLE_MAPS_API_KEY | SecureString | key Google Cloud, xem [google-cloud.md](google-cloud.md) |

Thêm hoặc sửa một tham số:

```powershell
& $aws ssm put-parameter --name /tripfair/prod/SITE_ADDRESS --type String --value '<ip-gạch-ngang>.sslip.io' --overwrite
```

Secret do người dùng tự nhập (App Password, API key) nên được nhập bằng Console (Systems Manager → Parameter Store → Create parameter, chọn SecureString) để giá trị không nằm trong lịch sử terminal. Chưa có SMTP thì API vẫn chạy, đăng ký bằng email báo `CODE_DELIVERY_FAILED`; chưa có key Google thì tìm kiếm trả rỗng (không có danh mục mẫu ở production).

## 10. Deploy

Các bước 10.1 và 10.2 chỉ làm một lần.

### 10.1 Đưa script lên máy

[deploy.sh](../backend/deploy/deploy.sh) và [Caddyfile](../backend/deploy/Caddyfile) được gửi qua SSM dưới dạng base64 (không cần SSH hay S3):

```powershell
foreach ($f in 'deploy.sh', 'Caddyfile') {
  $b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("backend\deploy\$f"))
  .\backend\deploy\ssm-run.ps1 -InstanceId $iid -Commands @("echo $b64 | base64 -d > /opt/tripfair/$f")
}
```

### 10.2 Tạo user MySQL cho ứng dụng

API đăng nhập RDS bằng user `tripfair` (chỉ có quyền trên database `tripfair`, bắt buộc TLS), không bao giờ dùng master. Để tạo user này, máy EC2 cần đọc mật khẩu master **một lần**: gắn tạm một policy, chạy [create-app-user.sh](../backend/deploy/create-app-user.sh), rồi gỡ policy ngay (khối `finally` gỡ cả khi script lỗi).

```powershell
(Get-Content backend\deploy\iam\admin-read-temp.template.json) -replace 'ACCOUNT_ID', $account -replace 'REGION', $region | Set-Content "$env:TEMP\admin-read.json" -Encoding ascii
& $aws iam put-role-policy --role-name tripfair-ec2-role --policy-name tripfair-setup-temp --policy-document "file://$env:TEMP\admin-read.json"
Start-Sleep -Seconds 15   # IAM cần vài giây để áp dụng
$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes('backend\deploy\create-app-user.sh'))
try {
  .\backend\deploy\ssm-run.ps1 -InstanceId $iid -TimeoutSeconds 300 -Commands @(
    "echo $b64 | base64 -d > /tmp/create-app-user.sh",
    'sh /tmp/create-app-user.sh; rc=$?; rm -f /tmp/create-app-user.sh; exit $rc')
} finally {
  & $aws iam delete-role-policy --role-name tripfair-ec2-role --policy-name tripfair-setup-temp
}
& $aws iam list-role-policies --role-name tripfair-ec2-role   # chỉ còn tripfair-app
```

Kết quả đúng in ra `GRANT ALL PRIVILEGES ON tripfair.*` và `ssl_type = ANY`. Script chạy lại nhiều lần được.

### 10.3 Build và đẩy image

Commit code trước (tag là mã commit), rồi trong `backend/` bằng Git Bash:

```bash
AWS="/c/Program Files/Amazon/AWSCLIV2/aws.exe" sh deploy/push-images.sh
```

[push-images.sh](../backend/deploy/push-images.sh) build cho `linux/amd64` (máy EC2 là x86), đẩy cả hai image với tag là mã commit ngắn. Trên Windows, Docker Desktop không lưu được token ECR (lỗi `stub received bad data`), nên script dùng một thư mục cấu hình Docker tạm rồi xóa đi.

### 10.4 Chạy deploy trên máy

```powershell
.\backend\deploy\ssm-run.ps1 -InstanceId $iid -TimeoutSeconds 900 -Commands @('sh /opt/tripfair/deploy.sh <tag>')
```

`deploy.sh` lần lượt: tải bộ CA của RDS, ghi cấu hình từ SSM ra `/opt/tripfair/app.env` (quyền 600), đăng nhập ECR, kéo hai image, chạy migration, khởi động lại `tripfair-api` và `tripfair-caddy`. Lần đầu Caddy cần khoảng nửa phút để lấy chứng chỉ.

### 10.5 Kiểm tra

```bash
curl https://<ip-gạch-ngang>.sslip.io/health
```

Kết quả đúng: `{"status":"ok","database":"up"}`. Xem log khi có lỗi:

```powershell
.\backend\deploy\ssm-run.ps1 -InstanceId $iid -Commands @('docker ps', 'docker logs --tail 50 tripfair-api', 'docker logs --tail 20 tripfair-caddy')
```

Bản deploy mới: commit, chạy lại 10.3 và 10.4 với tag mới. Quay lại bản cũ: chạy 10.4 với tag cũ (migration không tự lùi; migration đã chạy phải tương thích với code cũ, nên chỉ thêm cột/bảng, không xóa trong cùng một bản).

## 11. Bật và tắt để tiết kiệm

Bật (RDS mất 5–10 phút; container tự chạy lại nhờ `--restart unless-stopped`):

```powershell
& $aws rds start-db-instance --db-instance-identifier tripfair-db
& $aws ec2 start-instances --instance-ids $iid
& $aws rds wait db-instance-available --db-instance-identifier tripfair-db
```

Tắt khi xong việc:

```powershell
& $aws ec2 stop-instances --instance-ids $iid
& $aws rds stop-db-instance --db-instance-identifier tripfair-db
```

Kiểm tra trạng thái:

```powershell
& $aws ec2 describe-instances --instance-ids $iid --query 'Reservations[0].Instances[0].State.Name' --output text
& $aws rds describe-db-instances --db-instance-identifier tripfair-db --query 'DBInstances[0].DBInstanceStatus' --output text
```

## 12. Lỗi đã gặp

| Hiện tượng | Nguyên nhân | Cách xử lý |
| --- | --- | --- |
| `docker login` qua pipe PowerShell báo 400 | PowerShell 5.1 thêm ký tự xuống dòng vào token | Chạy trong Git Bash, hoặc dùng `push-images.sh` |
| `error storing credentials ... stub received bad data` | Credential store của Docker Desktop trên Windows không nhận token ECR dài | Thư mục cấu hình Docker tạm (`docker --config`), như `push-images.sh` |
| Image báo `Cannot find module '.../client.ts'` | Prisma đoán sai đuôi import khi build trong Docker | Giữ `moduleFormat` và `importFileExtension` trong generator của `schema.prisma` |
| API dừng ngay khi khởi động ở production | Thiếu biến bắt buộc (vd. `MYSQL_SSL_CA`) | Thêm tham số vào SSM rồi deploy lại |
| `Your session has expired` | Phiên `aws login` hết hạn | Chạy lại `aws login` |
| `FreeTierRestrictionError ... backup retention period` khi tạo RDS | Tài khoản gói Free giới hạn số ngày giữ sao lưu | `--backup-retention-period 1`, hoặc nâng gói tài khoản |
| `failed commit on ref ... timeout awaiting response headers` khi đẩy image | Mạng chậm khi đẩy layer lớn sang region xa | `push-images.sh` tự thử lại 5 lần và bỏ qua image đã có trên ECR; chạy lại script nếu vẫn lỗi |
| Trang đăng nhập báo `not supported for your project` | Tài khoản gắn với region khác | `aws login --region <region của tài khoản>` |
| Lệnh dài truyền qua `--parameters` bị hỏng dấu nháy | PowerShell 5.1 | `ssm-run.ps1` ghi tham số ra file JSON rồi truyền `file://` |

## 13. Gỡ toàn bộ hạ tầng

Khi không dùng tài khoản nữa (hoặc chuyển sang tài khoản khác), tắt máy vẫn còn tính tiền Elastic IP, ổ đĩa, lưu trữ RDS và image. Muốn hết phí thì phải xóa. Xóa là **không khôi phục được**: sao lưu database trước nếu có dữ liệu cần giữ. Kiểm tra tag `Project=TripFair` trước mỗi lần xóa để không đụng tài nguyên khác trong tài khoản.

Thứ tự (tài nguyên phía sau phụ thuộc vào tài nguyên phía trước):

```powershell
# 1. Elastic IP: gỡ khỏi máy rồi trả lại
& $aws ec2 disassociate-address --association-id <eipassoc-…>
& $aws ec2 release-address --allocation-id <eipalloc-…>
# 2. EC2 (ổ đĩa tự xóa theo vì DeleteOnTermination=true)
& $aws ec2 terminate-instances --instance-ids $iid
& $aws ec2 wait instance-terminated --instance-ids $iid
# 3. RDS: tắt chống xóa (làm được cả khi đang tắt), xóa không giữ snapshot, xóa cả sao lưu tự động
& $aws rds modify-db-instance --db-instance-identifier tripfair-db --no-deletion-protection --apply-immediately
& $aws rds delete-db-instance --db-instance-identifier tripfair-db --skip-final-snapshot --delete-automated-backups
# 4. ECR, SSM, IAM (không tốn tiền nhưng chứa image và secret)
& $aws ecr delete-repository --repository-name tripfair-api --force
& $aws ecr delete-repository --repository-name tripfair-migrate --force
& $aws ssm delete-parameters --names <tối đa 10 tên mỗi lần>
& $aws iam remove-role-from-instance-profile --instance-profile-name tripfair-ec2-profile --role-name tripfair-ec2-role
& $aws iam delete-instance-profile --instance-profile-name tripfair-ec2-profile
& $aws iam detach-role-policy --role-name tripfair-ec2-role --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
& $aws iam delete-role-policy --role-name tripfair-ec2-role --policy-name tripfair-app
& $aws iam delete-role --role-name tripfair-ec2-role
# 5. Mạng, sau khi RDS đã xóa xong
& $aws rds wait db-instance-deleted --db-instance-identifier tripfair-db
& $aws rds delete-db-subnet-group --db-subnet-group-name tripfair-db-subnets
& $aws ec2 delete-security-group --group-id <tripfair-db-sg>   # trước: nó tham chiếu tới SG của API
& $aws ec2 delete-security-group --group-id <tripfair-api-sg>
& $aws ec2 delete-subnet --subnet-id <tripfair-db-a>
& $aws ec2 delete-subnet --subnet-id <tripfair-db-c>
& $aws ec2 delete-route-table --route-table-id <tripfair-db-private>
```

Kiểm tra lại: `aws resourcegroupstaggingapi get-resources --tag-filters Key=Project,Values=TripFair` (API này có thể còn hiện tài nguyên vừa xóa thêm vài phút). Snapshot tự động của RDS biến mất vài phút sau khi xóa database.
