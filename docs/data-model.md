# 数据模型与接口契约

> 版本：v0.1（2026-11-03）· 配套 `docs/prd.md`、`docs/ui-flows.md`
> 原则：**本地是唯一数据源，同步是附加能力。** 所有表都为未来的双人同步预留字段，但当前不联网。

## 1. 现状（v1，Room version 1）

```sql
posts(
  id          INTEGER PK AUTOINCREMENT,
  photoPath   TEXT,        -- 仅文件名，位于 filesDir/photos
  caption     TEXT,
  placeLabel  TEXT NULL,
  latitude    REAL NULL,
  longitude   REAL NULL,
  createdAt   INTEGER,
  liked       INTEGER
)
```

单表、单图。`PostRepository` 暴露 `posts: Flow<List<Post>>`、`publish`、`delete`、`setLiked`、`photoFile`。

**问题**：多图放不下；没有作者/同步/软删除字段；地点没有坐标系标记（现在统一 GCJ-02）。

## 2. 目标模型（Room version 2）

### 2.1 实体关系

```
local_profile 1 ──── * posts 1 ──── * post_photos
   (本机身份)        (一条记录)        (最多 9 张)
```

### 2.2 `posts`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long PK | 本地自增，只在本机有意义 |
| `remoteId` | String? | 服务端 id，同步后写入；唯一索引 |
| `authorId` | String | 谁发的：本机 profileId 或伴侣的 id |
| `deviceId` | String | 哪台设备创建，排查冲突用 |
| `caption` | String | 配文，可为空 |
| `placeLabel` | String? | 地点名（POI 名或手填） |
| `latitude` / `longitude` | Double? | 坐标 |
| `coordSystem` | String | 固定 `GCJ02`；未来若接入 WGS-84 数据源用它区分 |
| `mood` | String? | 心情枚举（P1），先留空 |
| `createdAt` | Long | 创建时间（毫秒） |
| `updatedAt` | Long | 最后修改时间，**同步冲突判断依据** |
| `deletedAt` | Long? | 软删除时间，同步用墓碑 |
| `syncState` | String | `LOCAL_ONLY` / `PENDING` / `SYNCED` |
| `liked` | Boolean | 仅本机（点赞不上传，避免歧义） |

### 2.3 `post_photos`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long PK | |
| `postId` | Long FK → posts.id（`onDelete = CASCADE`，索引） | |
| `fileName` | String | 位于 `filesDir/photos`，UUID 命名 |
| `position` | Int | 显示顺序 0..8 |
| `width` / `height` | Int? | 占位图比例，避免加载跳动 |

### 2.4 `local_profile`（单行）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | String PK | 本机 UUID，首次启动生成 |
| `displayName` | String | 默认「我」，可改 |
| `partnerId` | String? | 未来配对的伴侣 id |
| `createdAt` | Long | |

## 3. 迁移策略（v1 → v2）

必须**保留用户已有记录**（现在手机上已经有真实数据）。

```sql
-- 1) posts 增加列（带默认值，旧数据自动补）
ALTER TABLE posts ADD COLUMN remoteId TEXT;
ALTER TABLE posts ADD COLUMN authorId TEXT NOT NULL DEFAULT 'local';
ALTER TABLE posts ADD COLUMN deviceId TEXT NOT NULL DEFAULT '';
ALTER TABLE posts ADD COLUMN coordSystem TEXT NOT NULL DEFAULT 'GCJ02';
ALTER TABLE posts ADD COLUMN mood TEXT;
ALTER TABLE posts ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0;
ALTER TABLE posts ADD COLUMN deletedAt INTEGER;
ALTER TABLE posts ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY';

-- 2) updatedAt 用 createdAt 回填
UPDATE posts SET updatedAt = createdAt WHERE updatedAt = 0;

-- 3) 新建照片表，把单图迁成 1 张
CREATE TABLE post_photos (...);
INSERT INTO post_photos(postId, fileName, position)
SELECT id, photoPath, 0 FROM posts;

-- 4) 旧列暂不删除（SQLite DROP COLUMN 依赖版本），标为 deprecated，
--    下一版确认无引用后再做表重建清理。
```

迁移必须有测试：`MigrationTestHelper` 从 v1 schema 迁到 v2，断言记录数与图片数一致。

## 4. Repository 契约

### 4.1 当前目标接口（本地）

```kotlin
data class PostWithPhotos(val post: Post, val photos: List<PostPhoto>)

interface PostRepository {
    fun observePosts(): Flow<List<PostWithPhotos>>   // 已过滤 deletedAt
    suspend fun create(
        drafts: List<File>,
        caption: String,
        place: PlaceSelection?,
        mood: Mood? = null,
    ): Long
    suspend fun update(id: Long, caption: String, place: PlaceSelection?, mood: Mood?)
    suspend fun setLiked(id: Long, liked: Boolean)
    suspend fun delete(id: Long)                     // 软删除 + 删除本地文件
    fun photoFile(fileName: String): File
}
```

**约束**：UI 只依赖这个接口；换成本地/远程混合实现时，UI 不改。

### 4.2 未来同步接口（现在只定义，不实现）

```kotlin
interface PostSyncService {
    suspend fun push(): SyncReport    // 上传 PENDING 记录与照片
    suspend fun pull(since: Long): SyncReport
}

data class SyncReport(val pushed: Int, val pulled: Int, val failed: Int)
```

触发时机：发布后（有网且已登录）、App 启动、手动下拉刷新。

## 5. 未来后端契约（草案，暂不实现）

| 接口 | 说明 |
|---|---|
| `POST /v1/posts` | 创建，body 含 `clientId`(remoteId)、`updatedAt`、内容字段 |
| `PATCH /v1/posts/{id}` | 更新 |
| `DELETE /v1/posts/{id}` | 软删除（服务端记 `deletedAt`） |
| `GET /v1/posts?since={ts}&limit=100` | 增量拉取，含墓碑 |
| `POST /v1/photos` | 上传照片（multipart 或预签名 URL） |

**冲突策略**：`updatedAt` 大者胜（LWW）；删除优先于更新；照片按内容 hash 去重。

**为什么现在不写后端**：单人使用下本地库已足够；后端一旦引入就要处理账号、鉴权、存储成本、隐私合规，收益要等双人场景真正成立才划算。

## 6. 测试策略

| 层 | 测什么 | 工具 |
|---|---|---|
| DAO | 增删改查、排序、软删除过滤、级联删除 | Room in-memory（androidTest） |
| 迁移 | v1 → v2 数据不丢、图片正确迁移 | `MigrationTestHelper` |
| Repository | 文件落盘、草稿清理、多图顺序 | JVM + 临时目录 |
| ViewModel | 状态流转、错误分支 | 协程测试 + 假实现 |

## 7. 与现有代码的对应

| 现有 | 目标 |
|---|---|
| `Post.photoPath` | 迁到 `post_photos`，`Post` 不再持有路径 |
| `PostRepository.publish(draft, ...)` | 改 `create(drafts: List<File>, ...)`，旧签名保留一个默认重载 |
| `PhotoStore` | 文件名语义不变，多图时写入多个文件 |
| `AppDatabase` version 1 | 升到 2，注册 Migration，**不做 destructive** |
