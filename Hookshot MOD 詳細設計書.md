# Hookshot MOD 詳細設計書

## 1. 文書情報

| 項目 | 内容 |
|---|---|
| システム名 | Hookshot MOD |
| 対象ゲーム | Minecraft Java Edition |
| 対象バージョン | 1.19.2 |
| Mod Loader | Fabric |
| 開発言語 | Java |
| 想定環境 | Client / Server双方 |
| 主用途 | フックショットによる高速移動・敵引き寄せ |
| 開発方式 | AI支援によるフェーズ分割開発 |
| 互換対象 | Better Combat / Simply Swords 等 |
| 設計方針 | 単一責任・疎結合・サーバー権威型 |

---

# 2. 開発目的

Minecraftに、クロスボウをベースとしたフックショット武器を追加する。

単純にプレイヤーを一直線に移動させるだけではなく、

- ブロックへのフック
- Entityへのフック
- Entityの種類による引き寄せ方向変更
- 左右入力による振り子運動
- ジャンプによるスイングジャンプ
- Main Hand / Off Hand対応
- 専用レティクル
- Better Combatとの共存

を備えた、高い操作自由度を持つ移動・戦闘補助武器とする。

---

# 3. 基本コンセプト

Hookshotは、

> 「照準した地点へプレイヤーを瞬間移動させる道具」

ではなく、

> 「フック地点とプレイヤーをロープで接続し、一定時間物理的な力を加える装置」

として実装する。

これにより、

- 横移動
- 斜め移動
- 慣性
- 落下
- ジャンプ
- 振り子運動

を組み合わせた立体的な移動を可能とする。

---

# 4. クラフト仕様

## 4.1 作成素材

使用素材：

- Crossbow × 1
- Lead × 1

基本的にはShapeless Recipeとする。

### レシピ

```text
Crossbow
+
Lead

↓

Hookshot
```

---

# 5. 修理仕様

Hookshotは金床等による修理を可能とする。

修理素材：

```text
Crossbow
```

1個消費することで一定量の耐久値を回復する。

初期値：

```text
最大耐久値：768
修理量：最大耐久値の25%
```

数値については実際のプレイテスト後に調整する。

---

# 6. エンチャント仕様

## 6.1 対応エンチャント

以下を対応対象とする。

```text
Unbreaking
Mending
Curse of Vanishing
```

Flameは弓・クロスボウの戦闘価値を損なわないため、Hookshotでは対応しない。

---

# 7. 将来的な独自エンチャント

初期リリースでは必須としない。

候補：

### Winch

引き寄せ力を増加。

```text
I   +10%
II  +20%
III +30%
```

### Longshot

最大射程を増加。

```text
I   96 block
II 112 block
III 128 block
```

### Momentum

振り子運動時の速度保持性能を向上。

```text
I   10%
II  20%
III 30%
```

独自エンチャントについては基本機能完成後に実装する。

---

# 8. Hand仕様

## 8.1 Main Hand

HookshotをMain Handに装備している場合、

```text
右クリック
```

で発射する。

---

## 8.2 Off Hand

HookshotをOff Handに装備している場合、

```text
左クリック
```

で発射する。

Off Hand時にはHookshotを手で握るのではなく、

```text
左前腕部分にクロスボウを装着
```

しているように描画する。

---

# 9. Better Combatとの競合防止

Better Combatによる両手武器使用中にHookshotを発動した場合、

Hookshot操作を優先する。

初期仕様：

```text
両手持ち状態
    ↓
Hookshot使用
    ↓
両手持ち動作を一時解除
    ↓
片手状態としてHookshotを実行
```

Hookshot終了後については、Better Combat側の通常処理へ制御を戻す。

Better Combat本体へ直接依存しない。

構成：

```text
compat/
    BetterCombatCompat.java
```

Better Combat導入時のみ互換処理を有効化する。

```java
FabricLoader.getInstance()
    .isModLoaded("bettercombat");
```

などによって存在確認を行う。

Better Combatが存在しない環境でもHookshot MOD単体で起動可能とする。

---

# 10. 射程仕様

基本最大射程：

```text
80 blocks
```

プレイヤーのEye Positionから視線方向にRaycastする。

```text
PLAYER
   │
   │
   └────────────────────→ 80 block
```

80ブロック以内に有効な命中対象が存在する場合のみ有効射程と判定する。

---

# 11. レティクル仕様

Hookshot装備中のみ専用レティクルを表示する。

## 白

```text
80 block以内に
フック可能対象あり
```

## 赤

```text
フック可能対象なし
```

将来的な拡張としてEntity判定時に黄色等を追加できる構造とする。

例：

```text
WHITE  = Block
YELLOW = Entity
RED    = None
```

ただし初期実装は白・赤のみとする。

---

# 12. 発射仕様

Hookshotは見た目上クロスボウを使用する。

クロスボウ特有の長いチャージ処理は使用しない。

発射時間：

```text
1～2 tick
```

程度を目標とし、体感上は即時発射とする。

---

# 13. Hook Projectile

独自Entityを使用する。

```java
HookProjectileEntity
```

ArrowEntityそのものは使用しない。

理由：

ArrowEntityには、

- ダメージ
- 回収
- 矢のアイテム化
- 地面刺突状態
- デスポーン
- 矢固有NBT

等の不要な処理が含まれるため。

---

# 14. Hook先端モデル

HookProjectileEntityの表示モデルには、

```text
Minecraft Arrow
```

を使用する。

すなわち、

```text
内部Entity

HookProjectileEntity

表示

Arrow Model
```

とする。

---

# 15. ロープ描画

プレイヤーとHookProjectileEntity間をロープとして表示する。

見た目：

```text
Player Hand
    │
    ╰======================== Arrow Hook
              Lead
```

MinecraftのLeadに近い描画とする。

Lead Entityそのものを生成する方式は使用しない。

Renderer上で、

```text
Player Hand Position
Hook Position
```

を結ぶ。

---

# 16. Hook状態

Hookは以下の状態を持つ。

```java
public enum HookState {

    FLYING,

    ATTACHED_BLOCK,

    ATTACHED_ENTITY,

    RETURNING,

    REMOVED
}
```

---

# 17. Grapple状態

プレイヤー側にはHook状態とは別にGrappleStateを持つ。

```java
public enum GrappleMode {

    NONE,

    PLAYER_PULL,

    ENTITY_PULL,

    SWING
}
```

状態情報例：

```java
public final class GrappleState {

    private UUID hookUuid;

    private UUID hookedEntityUuid;

    private Vec3d anchorPosition;

    private GrappleMode mode;

    private int activeTicks;

    private int fallProtectionTicks;

    private boolean active;
}
```

---

# 18. Entity命中判定

Entityへ命中した場合、

対象Entityの種類によって、

```text
EntityをPlayerへ引っ張る

または

PlayerをEntityへ引っ張る
```

を切り替える。

---

# 19. Player方向へ引き寄せるEntity

以下をHumanType Targetとして扱う。

```text
Villager
Armor Stand
Zombie
Zombie Villager
Piglin
Enderman
```

命中した場合：

```text
TARGET
   ↓
   ↓
PLAYER
```

対象EntityをPlayer方向へ引き寄せる。

---

# 20. Playerが引き寄せられる対象

上記HumanType Target以外の場合、

Player側を命中対象へ引き寄せる。

対象例：

```text
Block
Cow
Pig
Horse
Spider
Iron Golem
その他Entity
```

挙動：

```text
HOOK
  ●
  │
  │
Player
  ↑
```

---

# 21. 引き寄せ時間

最大：

```text
5秒
```

Minecraft内部値：

```text
100 ticks
```

とする。

100tick到達時、自動的にHook状態を解除する。

---

# 22. 基本引力

Hook方向ベクトル：

```java
Vec3d pullDirection =
        anchorPosition
        .subtract(player.getPos())
        .normalize();
```

現在速度に加速度として追加する。

```java
Vec3d nextVelocity =
        player.getVelocity()
        .add(pullDirection.multiply(PULL_FORCE));
```

速度を毎tick完全上書きしてはならない。

禁止：

```java
player.setVelocity(
    pullDirection.multiply(PULL_FORCE)
);
```

理由：

- プレイヤー入力
- 慣性
- 落下
- ジャンプ
- Swing

を消してしまうため。

---

# 23. 初期物理パラメータ

初期調整値：

```text
PULL_FORCE        = 0.12
SIDE_FORCE        = 0.04
SWING_JUMP_FORCE  = 0.32
MAX_SPEED         = 1.8
```

確定値ではない。

プレイテストによって調整する。

---

# 24. Swing判定

Hook接続中にプレイヤーが左右方向入力を行った場合、

GrappleModeを、

```text
PLAYER_PULL
     ↓
SWING
```

として扱う。

---

# 25. Swing物理

Hook地点を中心としてプレイヤーがカーブするようにする。

Hook方向：

```java
Vec3d radial =
    anchor.subtract(playerPos).normalize();
```

横方向：

```java
Vec3d tangent =
    radial.crossProduct(
        new Vec3d(0, 1, 0)
    ).normalize();
```

入力：

```text
A

-tangent

D

+tangent
```

として加速する。

---

# 26. 斜め入力

以下を許可する。

```text
W + A
W + D
S + A
S + D
```

入力に応じて、

```text
Pull Force
+
Tangential Force
```

を合成する。

これによってHook地点を中心とする円弧移動を実現する。

---

# 27. Swing Jump

Hook接続状態でジャンプ入力が行われた場合、

通常ジャンプとは異なるHook専用ジャンプ処理を実行する。

```text
現在のTangential Velocity
+
上方向Force
```

を加える。

概念：

```text
             PLAYER
               ↗

        ● HOOK
```

Hook地点を中心として上方向へ円を描くような軌道を目標とする。

---

# 28. Hook解除

以下の場合にHookを解除する。

```text
5秒経過

プレイヤーによる解除入力

Hook Entity消失

Dimension変更

Player死亡

対象Entity死亡

対象Entity消失
```

---

# 29. 落下ダメージ保護

Hook解除後、

```text
5秒
```

落下ダメージを無効化する。

Minecraft内部：

```text
100 tick
```

GrappleState：

```java
fallProtectionTicks = 100;
```

保護中は、

```java
player.fallDistance = 0;
```

とする。

Slow Falling Effectは使用しない。

理由：

落下速度そのものを変更してしまうため。

---

# 30. Cooldown

## 命中成功

```text
Cooldown = 0
```

すぐ再発射可能。

---

## 命中失敗

```text
Cooldown = 2秒
```

Minecraft内部：

```text
40 tick
```

---

# 31. 耐久値消費

初期仕様：

```text
Block命中       1

Entity命中      2

空振り          1
```

Unbreakingによる軽減処理を適用する。

---

# 32. Client / Server責務

Hookshot MODはServer Authority方式とする。

## Client

担当：

```text
入力取得

レティクル

画面描画

モデル描画

ロープ描画

アニメーション
```

---

## Server

担当：

```text
Hook生成

Raycast再確認

Entity命中判定

Cooldown

耐久消費

引き寄せ物理

落下保護

Grapple状態管理
```

---

# 33. セキュリティ設計

Clientから、

```text
「80ブロック先に命中しました」
```

という結果をServerへ送信してはならない。

Clientから送信する情報は原則、

```text
Hookshot使用要求
```

のみとする。

Server側で、

```text
装備確認

Cooldown確認

プレイヤー状態確認

Raycast

Entity判定

Hook生成
```

を行う。

これにより改造Clientによる、

```text
射程改変

Cooldown無視

耐久無視

任意座標Hook

任意Entity引き寄せ
```

等を防止する。

---

# 34. パッケージ構成

```text
hookshot/
│
├── HookshotMod.java
│
├── HookshotClient.java
│
│
├── item/
│   └── HookshotItem.java
│
├── entity/
│   └── HookProjectileEntity.java
│
├── grapple/
│   ├── GrappleManager.java
│   ├── GrappleState.java
│   ├── GrappleMode.java
│   ├── PlayerPullBehavior.java
│   ├── EntityPullBehavior.java
│   └── SwingPhysics.java
│
├── client/
│   ├── HookProjectileRenderer.java
│   ├── HookRopeRenderer.java
│   ├── HookReticleRenderer.java
│   ├── HookItemRenderer.java
│   └── HookInputHandler.java
│
├── network/
│   ├── HookFirePacket.java
│   └── HookReleasePacket.java
│
├── enchantment/
│
├── compat/
│   └── BetterCombatCompat.java
│
└── registry/
    ├── ModItems.java
    ├── ModEntities.java
    └── ModEnchantments.java
```

---

# 35. 単一責任の原則

## HookshotItem

担当：

```text
アイテムとしての操作
```

物理計算を担当させない。

---

## HookProjectileEntity

担当：

```text
飛翔
衝突
Hook状態
```

プレイヤー操作判定を担当させない。

---

## GrappleManager

担当：

```text
Hook状態全体の管理
```

---

## PlayerPullBehavior

担当：

```text
PlayerをHookへ移動
```

---

## EntityPullBehavior

担当：

```text
EntityをPlayerへ移動
```

---

## SwingPhysics

担当：

```text
振り子運動
```

---

## HookReticleRenderer

担当：

```text
レティクルのみ
```

---

## BetterCombatCompat

担当：

```text
Better Combat互換処理のみ
```

---

# 36. AI支援開発方針

本MODはAIを利用して開発する。

ただし、

```text
MOD全体を一度にAIへ生成させない。
```

必ずフェーズ単位で生成する。

基本フロー：

```text
設計
 ↓
AIコード生成
 ↓
ビルド
 ↓
Minecraft起動
 ↓
単体確認
 ↓
問題修正
 ↓
Git Commit
 ↓
次Phase
```

各Phaseの完了コードを次PhaseのAI入力へ渡す。

---

# 37. AIへコード生成を依頼する際のルール

毎回AIへ以下を提示する。

```text
Minecraft Version
Fabric Loader Version
Fabric API Version
Yarn Mapping Version
Java Version
現在のディレクトリ構造
既存コード
今回変更するクラス
今回変更してはいけないクラス
完了条件
```

AIには原則、

```text
既存コードを省略しない
```

ことを要求する。

---

# 38. 開発フェーズ

---

# Phase 1
## 詳細設計

本書。

### 完了条件

- [x] Hookshot基本仕様決定
- [x] Entity引き寄せ仕様決定
- [x] Player引き寄せ仕様決定
- [x] Swing仕様決定
- [x] Cooldown仕様決定
- [x] 落下保護仕様決定
- [x] Hand仕様決定
- [x] Better Combat互換方針決定
- [x] Client / Server責務決定
- [x] パッケージ構成決定

---

# Phase 2
## Fabricプロジェクト構築

### 実装

Minecraft 1.19.2対応Fabric MOD環境を構築する。

### チェックリスト

- [ ] Fabric 1.19.2プロジェクト作成
- [ ] Javaバージョン確認
- [ ] Gradleビルド成功
- [ ] Fabric API導入
- [ ] MOD ID決定
- [ ] HookshotMod作成
- [ ] HookshotClient作成
- [ ] Minecraft Client起動確認
- [ ] MOD一覧へHookshotが表示される
- [ ] Git初期Commit

### 完了条件

```text
Minecraftが正常起動すること。
```

---

# Phase 3
## Hookshot Item実装

まだProjectileは実装しない。

### チェックリスト

- [ ] ModItems作成
- [ ] HookshotItem作成
- [ ] Item登録
- [ ] Creative Inventory登録
- [ ] Crossbowモデル仮適用
- [ ] 最大耐久値設定
- [ ] クラフトレシピ作成
- [ ] Crossbow修理判定実装
- [ ] ゲーム内入手確認
- [ ] 耐久値確認

### 完了条件

Hookshotを、

```text
作成
取得
装備
修理
```

できる。

---

# Phase 4
## Raycast・レティクル

Projectileはまだ飛ばさない。

### チェックリスト

- [ ] 最大射程80 block定義
- [ ] Player Eye Position取得
- [ ] 視線Raycast
- [ ] Block Hit判定
- [ ] Miss判定
- [ ] Hookshot装備中判定
- [ ] 専用Reticle表示
- [ ] Hit時WHITE
- [ ] Miss時RED
- [ ] Main Hand確認
- [ ] Off Hand確認

### テスト

- [ ] 10 block先Block → WHITE
- [ ] 79 block先Block → WHITE
- [ ] 81 block先Block → RED
- [ ] 空 → RED
- [ ] Hookshot非装備 → 通常レティクル

---

# Phase 5
## Hook Projectile

### チェックリスト

- [ ] HookProjectileEntity作成
- [ ] EntityType登録
- [ ] Renderer登録
- [ ] Arrow Model適用
- [ ] 発射処理実装
- [ ] 80 block制限
- [ ] Block衝突
- [ ] Entity衝突
- [ ] Miss
- [ ] Projectile削除処理

### 完了条件

```text
クリック
↓
Arrow型Hookが飛ぶ
↓
BlockまたはEntityに命中
```

まで正常動作する。

---

# Phase 6
## ロープ描画

### チェックリスト

- [ ] HookRopeRenderer作成
- [ ] Player位置取得
- [ ] Hook位置取得
- [ ] Lead風テクスチャ適用
- [ ] 距離に応じて伸長
- [ ] 1人称確認
- [ ] 3人称確認
- [ ] 他Playerから確認
- [ ] Hook消失時ロープ削除

---

# Phase 7
## Block Grapple

最初は物理処理を単純化する。

### チェックリスト

- [ ] Block Anchor保存
- [ ] GrappleState作成
- [ ] PLAYER_PULL実装
- [ ] PULL_FORCE導入
- [ ] MAX_SPEED導入
- [ ] 100tick制限
- [ ] Hook解除
- [ ] Cooldown成功時0
- [ ] Miss時40tick
- [ ] 落下保護100tick

### 完了条件

```text
BlockへHook
↓
Playerが5秒以内Hook方向へ移動
↓
解除
↓
落下ダメージなし
```

---

# Phase 8
## Entity Grapple

### チェックリスト

- [ ] HumanType判定
- [ ] Villager
- [ ] ArmorStand
- [ ] Zombie
- [ ] ZombieVillager
- [ ] Piglin
- [ ] Enderman
- [ ] EntityPullBehavior作成
- [ ] PlayerPullBehaviorとの切替
- [ ] Entity死亡時解除
- [ ] Entity消失時解除

### テスト

HumanType：

```text
Entity → Player
```

その他：

```text
Player → Entity
```

---

# Phase 9
## Swing Physics

このPhaseまでは横入力による特殊物理を入れない。

### チェックリスト

- [ ] Player入力取得
- [ ] Radial Vector算出
- [ ] Tangent Vector算出
- [ ] A入力
- [ ] D入力
- [ ] W+A
- [ ] W+D
- [ ] S+A
- [ ] S+D
- [ ] SIDE_FORCE実装
- [ ] 速度上限
- [ ] 空中慣性確認
- [ ] Hook中心円弧確認

### 完了条件

Hook地点を中心として、

```text
左右入力
↓
円弧状移動
```

が確認できる。

---

# Phase 10
## Swing Jump

### チェックリスト

- [ ] Hook中Jump検出
- [ ] Tangential Velocity取得
- [ ] 上方向Force追加
- [ ] Jump連打対策
- [ ] 最大速度制御
- [ ] 円弧ジャンプ確認
- [ ] Hook解除との競合確認
- [ ] Fall Protection確認

---

# Phase 11
## Main / Off Hand完成

### Main Hand

- [ ] 右クリック発射
- [ ] 左クリックは通常Attack

### Off Hand

- [ ] 左クリック取得
- [ ] Hookshot装備確認
- [ ] Attack処理抑制
- [ ] ServerへFire要求
- [ ] Hook発射
- [ ] 右クリック競合確認

---

# Phase 12
## 専用モデル・アニメーション

### チェックリスト

- [ ] Crossbowモデル流用
- [ ] Main Hand位置調整
- [ ] Off Hand位置調整
- [ ] 左腕装着位置調整
- [ ] First Person確認
- [ ] Third Person確認
- [ ] 発射アニメーション
- [ ] 即時チャージ表現
- [ ] Rope接続位置調整

---

# Phase 13
## Enchantment

### チェックリスト

- [ ] Unbreaking
- [ ] Mending
- [ ] Curse of Vanishing
- [ ] 金床確認
- [ ] エンチャントテーブル確認

---

# Phase 14
## Better Combat互換

基本機能完成後に実装する。

### チェックリスト

- [ ] Better Combat存在検出
- [ ] Better Combatなし起動確認
- [ ] Better Combatあり起動確認
- [ ] Greatsword装備
- [ ] Two-Hand状態検出
- [ ] Hookshot発動時解除
- [ ] One-Hand状態移行
- [ ] Hook終了
- [ ] Better Combatへ状態返却
- [ ] Simply Swords確認

---

# Phase 15
## Multiplayer検証

### チェックリスト

- [ ] Dedicated Server起動
- [ ] Client接続
- [ ] Player A Hookshot
- [ ] Player BからProjectile確認
- [ ] Rope同期
- [ ] Entity移動同期
- [ ] Cooldown同期
- [ ] 耐久同期
- [ ] 80 block制限
- [ ] 不正Packet確認
- [ ] Dimension移動
- [ ] Logout
- [ ] Death

---

# Phase 16
## Balance Test

### 調整対象

- [ ] PULL_FORCE
- [ ] SIDE_FORCE
- [ ] SWING_JUMP_FORCE
- [ ] MAX_SPEED
- [ ] Grapple時間
- [ ] Miss Cooldown
- [ ] 耐久値
- [ ] Entity引力
- [ ] 射程

数値をコードへ直接散在させない。

```java
HookshotConfig
```

等へ集約する。

---

# Phase 17
## 独自Enchantment

基本機能安定後。

- [ ] Winch
- [ ] Longshot
- [ ] Momentum
- [ ] Enchantment競合
- [ ] Balance調整

---

# Phase 18
## 最終テスト

### 基本機能

- [ ] Craft
- [ ] Repair
- [ ] Durability
- [ ] Enchant
- [ ] Main Hand
- [ ] Off Hand
- [ ] Reticle
- [ ] Block Hook
- [ ] Entity Hook
- [ ] Swing
- [ ] Swing Jump
- [ ] Cooldown
- [ ] Fall Protection

### MOD互換

- [ ] Fabric API
- [ ] Better Combat
- [ ] Simply Swords
- [ ] Figura
- [ ] Iris
- [ ] Sodium

### 環境

- [ ] Single Player
- [ ] LAN
- [ ] Dedicated Server

---

# Phase 19
## Release Build

- [ ] Version番号決定
- [ ] MOD名確認
- [ ] fabric.mod.json確認
- [ ] アイコン設定
- [ ] License確認
- [ ] README作成
- [ ] 操作説明
- [ ] Known Issues作成
- [ ] Gradle Release Build
- [ ] jar生成
- [ ] 新規Minecraft環境へ導入
- [ ] 最終起動確認
- [ ] Git Tag作成

---

# 39. AI利用時のフェーズ運用ルール

1フェーズにつき原則1つの会話単位として扱う。

例：

```text
Phase 4開始

↓

現在のソース一式をAIへ提示

↓

Phase 4のみ実装

↓

ビルド

↓

エラーがあればAIへエラー全文提示

↓

ゲーム起動

↓

チェックリスト実施

↓

全項目成功

↓

Git Commit

↓

Phase 5へ
```

---

# 40. AIへ渡す実装依頼テンプレート

```text
Minecraft Java Edition 1.19.2
Fabric環境でHookshot MODを開発しています。

現在はPhase Xです。

今回実装する内容：
・XXXX
・XXXX
・XXXX

今回変更してよいクラス：
・XXXX.java
・XXXX.java

変更してはいけないクラス：
・XXXX.java

現在のコード：
（コード全文）

今回の完了条件：
・XXXX
・XXXX

単一責任の原則を守ってください。

Minecraft Client側とServer側の責務を分離してください。

マルチプレイでClient入力を信用せず、
重要なゲームロジックはServer側で検証してください。

既存機能を削除しないでください。

変更したファイルについてはコード全文を省略せず提示してください。

Minecraft 1.19.2 / Fabricで使用可能なAPIのみを利用してください。
```

---

# 41. Git運用

Phase単位でCommitする。

例：

```text
phase-02: setup fabric project

phase-03: add hookshot item

phase-04: add hookshot reticle

phase-05: add hook projectile

phase-06: add hook rope renderer

phase-07: implement block grapple

phase-08: implement entity grapple

phase-09: implement swing physics

phase-10: implement swing jump
```

これによってAI生成コードで問題が発生した場合も、

```text
最後に正常だったPhase
```

へ容易に戻せる。

---

# 42. 開発上の重要原則

本MODでは、

```text
見た目
入力
Projectile
状態管理
物理
互換処理
```

を分離する。

特に、

```text
HookshotItem.java
```

へすべての処理を記述することは禁止する。

HookshotItemは発射要求の入口とし、

実際のHook状態管理・物理計算・レンダリングは専用クラスへ委譲する。

---

# 43. 最終完成イメージ

```text
敵・ブロック
        ●
        ╲
         ╲ Lead
          ╲
           PLAYER
```

Hook接続：

```text
PLAYER
   ↓ Pull
       ●
```

横入力：

```text
          ● HOOK
        ／
      ／
 PLAYER
      ↗
```

ジャンプ：

```text
             PLAYER
               ↗

          ● HOOK
```

別地点へ再射出：

```text
●                       ●
 ╲                     ╱
  ╲       PLAYER      ╱
```

プレイヤーの入力と慣性を利用して、

**連続して空間を飛び回れるHookshot**

を最終目標とする。
