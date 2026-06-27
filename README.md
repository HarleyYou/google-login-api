# Google Login API — 後端 (Spring Boot)

Google 登入系統的 REST API 後端，使用 Spring Boot 3.3.4 / Java 21 實作。

## 技術

- **框架**：Spring Boot 3.3.4
- **語言**：Java 21
- **Session**：伺服器端 SessionStore（隨機 session ID）+ HttpOnly Cookie
- **密碼**：SHA-256 hash（demo 用途，正式環境應改 bcrypt）
- **Port**：8080

## 本機開發

```bash
mvn spring-boot:run
```

## API 端點

| 方法 | 路徑 | 說明 |
|---|---|---|
| POST | `/api/auth/check-email` | 確認 email 是否存在 |
| POST | `/api/auth/login` | 驗證密碼，成功建立 session 並設置 cookie |
| GET  | `/api/auth/me` | 從 session 取得當前使用者 |
| POST | `/api/auth/logout` | 銷毀 session 並清除 cookie |

## Session 機制

- 登入成功後產生 32-byte 隨機 `session_id`（`SecureRandom`），儲存在 `SessionStore`
- Cookie 值為不透明的 session ID，不含任何身份資訊
- 登出時伺服器端移除 session，cookie 無法再使用

## 測試帳號

| Email | 密碼 |
|---|---|
| `user@gmail.com` | `password123` |
| `test@gmail.com` | `test1234` |

## CORS

僅允許來自 `http://localhost:3000` 的請求，需 `Content-Type` header。  
正式環境請修改 `CorsConfig.java` 中的 `allowedOrigins`。

## 專案結構

```
├── src/main/java/com/example/googlelogin/
│   ├── config/CorsConfig.java
│   ├── controller/AuthController.java
│   ├── service/
│   │   ├── AuthService.java      # SHA-256 密碼驗證
│   │   └── SessionStore.java     # 記憶體 session 管理
│   └── model/AuthRequest.java
├── deploy/
│   └── k8s/
│       ├── deployment.yaml       # k8s Deployment（2 replicas、probe）
│       └── service.yaml          # k8s ClusterIP Service
├── .gitlab-ci.yml                # GitLab CI/CD Pipeline
└── Dockerfile
```

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

## GitLab CI/CD

### Pipeline 流程
```
push to main
    ↓
[build] docker build → push to GitLab Registry
    ↓
[deploy] kubectl apply → kubectl set image → rollout status
```

### 需設定的 CI Variables

| 變數 | 說明 |
|---|---|
| `KUBE_CONFIG` | base64 編碼的 kubeconfig |
| `KUBE_NAMESPACE` | 部署的 k8s namespace |

> 部署前請將 `deploy/k8s/deployment.yaml` 中的 `registry.gitlab.com/YOUR_GROUP/...` 換成實際路徑。

## 安全性說明

| 項目 | 實作 |
|---|---|
| Session | 伺服器端儲存，cookie 只含隨機 ID，無法偽造 |
| Cookie | `HttpOnly; Secure; SameSite=Lax` |
| 密碼儲存 | SHA-256 hash ⚠️ 無 salt，正式環境應改 bcrypt/Argon2 |
| 登出 | 伺服器端銷毀 session，cookie 失效 |
| 密碼遮蔽 | `AuthRequest.toString()` 已遮蔽密碼，防止 log 洩漏 |
| 輸入驗證 | email 長度限制 254 字元，密碼限制 128 字元 |

## 已知限制（學習 / Demo 用途）

- 用戶資料 hardcode 在 `AuthService.java`，無資料庫，重啟清除所有 session
- SHA-256 無 salt，正式環境需改 bcrypt 或 Argon2
- 無 rate limiting，正式環境需加防暴力破解機制
- Session 存放於記憶體，多副本部署（k8s 多 pod）需改用 Redis
- Cookie `Secure` flag 在 HTTP 環境下瀏覽器會拒絕設置，本地開發需注意
