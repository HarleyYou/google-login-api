# Google Login API — 後端 (Spring Boot)

Google 登入系統的 REST API 後端，使用 Spring Boot 3.3.4 / Java 21 實作。

## 技術

- **框架**：Spring Boot 3.3.4
- **語言**：Java 21
- **認證**：HttpOnly Cookie + SHA-256 密碼 hash
- **Port**：8080

## 本機開發

```bash
mvn spring-boot:run
```

## API 端點

| 方法 | 路徑 | 說明 |
|---|---|---|
| POST | `/api/auth/check-email` | 確認 email 是否存在 |
| POST | `/api/auth/login` | 驗證密碼，成功設置 cookie |
| GET  | `/api/auth/me` | 從 cookie 取得當前使用者 |
| POST | `/api/auth/logout` | 清除 cookie |

## 測試帳號

| Email | 密碼 |
|---|---|
| `user@gmail.com` | `password123` |
| `test@gmail.com` | `test1234` |

## Docker

```bash
docker build -t google-login-api .
docker run -p 8080:8080 google-login-api
```

## Kubernetes 部署

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: google-login-api
spec:
  replicas: 1
  selector:
    matchLabels:
      app: google-login-api
  template:
    metadata:
      labels:
        app: google-login-api
    spec:
      containers:
        - name: google-login-api
          image: google-login-api:latest
          ports:
            - containerPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: google-login-api-service
spec:
  selector:
    app: google-login-api
  ports:
    - port: 8080
      targetPort: 8080
```

## CORS

目前僅允許來自 `http://localhost:3000` 的請求。  
正式環境請修改 `CorsConfig.java` 中的 `allowedOrigins`。
