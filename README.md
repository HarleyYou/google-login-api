# Google Login API — 後端 (Spring Boot)

Google 登入系統的 REST API 後端，使用 Spring Boot 3.3.4 / Java 21 實作。

## 技術

- **框架**：Spring Boot 3.3.4
- **語言**：Java 21
- **認證**：HttpOnly Cookie（`SameSite=Lax`）+ SHA-256 密碼 hash
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
| GET  | `/api/auth/me` | 從 cookie 取得並驗證當前使用者 |
| POST | `/api/auth/logout` | 清除 cookie |

> `/api/auth/me` 會同時驗證 cookie 值對應的用戶是否存在，偽造 cookie 無法通過。

## 測試帳號

| Email | 密碼 |
|---|---|
| `user@gmail.com` | `password123` |
| `test@gmail.com` | `test1234` |

## CORS

僅允許來自 `http://localhost:3000` 的請求，並允許 `Content-Type` header（JSON POST 必須）。  
正式環境請修改 `CorsConfig.java` 中的 `allowedOrigins`。

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

## 安全性說明

| 項目 | 實作方式 |
|---|---|
| 密碼儲存 | SHA-256 hash ⚠️ 無 salt，正式環境應改用 bcrypt |
| Cookie | `HttpOnly; SameSite=Lax`，JS 無法讀取 |
| Session 驗證 | `/me` 驗證 cookie 值對應用戶是否存在 |
| CORS | 僅允許指定 origin，需明確設定 `allowedHeaders` |

## 已知限制（學習 / Demo 用途）

- 用戶資料 hardcode 在 `AuthService.java`，無資料庫
- SHA-256 無 salt，正式環境需改 bcrypt 或 Argon2
- 無 rate limiting，正式環境需加防暴力破解機制
