# CI/CD Deployment Guide

Hướng dẫn deploy tự động lên Digital Ocean với GitHub Actions và GitHub Container Registry.

## Setup GitHub Secrets

Vào repository > **Settings** > **Secrets and variables** > **Actions**, thêm các secrets sau:

### Bắt buộc:
- `DO_HOST` - IP hoặc domain của Digital Ocean server
- `DO_USERNAME` - SSH username (thường là `root` hoặc `ubuntu`)
- `DO_PASSWORD` - SSH password để kết nối server
- `GHCR_TOKEN` - Personal Access Token với quyền `read:packages` (để pull images)
- `DB_HOST` - PostgreSQL host:
  - Nếu PostgreSQL trong Docker container khác: dùng container name (ví dụ: `postgres-gvn`) hoặc IP container
  - Nếu PostgreSQL trên host: dùng `host.docker.internal` hoặc `172.17.0.1`
  - Nếu PostgreSQL expose port ra ngoài: dùng IP server và port tương ứng
- `DB_PORT` - PostgreSQL port (mặc định: `5432`)
- `DB_NAME` - Tên database
- `DB_USER` - PostgreSQL username
- `DB_PASSWORD` - PostgreSQL password
- `JWT_SECRET` - Secret key cho JWT (nên dùng string ngẫu nhiên mạnh)

### Tùy chọn:
- `DO_PORT` - SSH port (mặc định: `22`)
- `SERVER_PORT` - Application port (mặc định: `8080`)
- `JWT_ACCESS_EXPIRATION` - Access token expiration (mặc định: `3600`)
- `JWT_REFRESH_EXPIRATION` - Refresh token expiration (mặc định: `7200`)

## Tạo GHCR_TOKEN (cho server pull images)

1. Vào: https://github.com/settings/tokens
2. Click **"Generate new token"** > **"Generate new token (classic)"**
3. Đặt tên: `GHCR Pull Token`
4. Chọn scope: ✅ **`read:packages`**
5. Click **"Generate token"** và copy token
6. Thêm vào GitHub Secrets với tên `GHCR_TOKEN`

**Lưu ý:** `GITHUB_TOKEN` được GitHub tự động cung cấp để push images, không cần setup!

## Setup SSH Password

Chỉ cần thêm SSH password của server vào GitHub Secrets:

1. Vào repository > **Settings** > **Secrets and variables** > **Actions**
2. Click **"New repository secret"**
3. Name: `DO_PASSWORD`
4. Value: Nhập SSH password của server
5. Click **"Add secret"**

**Lưu ý:** Đảm bảo server cho phép password authentication (thường được bật mặc định).

## Deploy

Chỉ cần push code lên branch `main` hoặc `master`:

```bash
git add .
git commit -m "Your commit message"
git push origin main
```

GitHub Actions sẽ tự động:
1. ✅ Build application
2. ✅ Build Docker image
3. ✅ Push image lên GitHub Container Registry
4. ✅ Deploy lên Digital Ocean server

## Kiểm tra Deployment

```bash
# SSH vào server
ssh user@your-server-ip

# Kiểm tra container đang chạy
docker ps | grep gvn-backend

# Xem logs
docker logs -f gvn-backend

# Test health check
curl http://localhost:8080/actuator/health
```

## Troubleshooting

### Container không start
```bash
docker logs gvn-backend
```

### Database connection error
- Kiểm tra `DB_HOST` có đúng không:
  - Nếu PostgreSQL trong container: dùng container name (ví dụ: `postgres-gvn`)
  - Nếu PostgreSQL trên host: dùng `host.docker.internal` (Linux) hoặc `172.17.0.1`
  - Tạo Docker network chung nếu cần: `docker network create app-network`
- Kiểm tra PostgreSQL đang chạy: `docker ps | grep postgres`
- Test connection: `docker exec -it gvn-backend sh` rồi `ping postgres-gvn` (nếu dùng container name)

### Permission denied khi pull image
- Đảm bảo repository là **public** hoặc
- Tạo Personal Access Token với quyền `read:packages` và thay `GITHUB_TOKEN` trong workflow

## Manual Deploy (nếu cần)

```bash
# SSH vào server
ssh user@your-server-ip

# Login và pull image
echo "YOUR_GITHUB_TOKEN" | docker login ghcr.io -u YOUR_USERNAME --password-stdin
docker pull ghcr.io/YOUR_USERNAME/gvn-backend:latest

# Stop old container
docker stop gvn-backend
docker rm gvn-backend

# Run new container
docker run -d \
  --name gvn-backend \
  --restart unless-stopped \
  --network host \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=localhost \
  -e DB_PORT=5432 \
  -e DB_NAME=your_db \
  -e DB_USER=postgres \
  -e DB_PASSWORD=your_password \
  -e JWT_SECRET=your_jwt_secret \
  ghcr.io/YOUR_USERNAME/gvn-backend:latest
```

