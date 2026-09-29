# Sound Subtitle Colors

Color-codes Minecraft's built-in sound subtitles (Options → Accessibility
Settings → Closed Captions) by sound category, so a player who relies on
subtitles can tell what kind of sound just happened without reading the
text first.

This is a small, focused accessibility QoL mod: it changes subtitle text
color only. It does not add a HUD, a minimap, or any gameplay mechanic, and
it does not read or modify anything server-side.

**This mod was built with AI-assisted development** (implementation drafted
by an autonomous Claude Code agent, as part of the Hullspark mod lab) and
checked in the running game before release.

## Colors

Based on the [Okabe–Ito color-blind-safe palette](https://jfly.uni-koeln.de/color/),
with the Weather blue brightened from the original Okabe–Ito value so it stays
readable against this mod's dark, semi-transparent subtitle background:

| Sound category | Examples | Color |
|---|---|---|
| Hostile | zombies, skeletons, creepers | vermillion `#D55E00` |
| Neutral | cows, villagers, most passive/neutral mobs | yellow `#F0E442` |
| Players | other players' voice/actions | sky blue `#56B4E9` |
| Blocks | doors, chests, furnaces, footsteps on blocks | orange `#E69F00` |
| Weather | rain, thunder | blue `#0072FC` (brightened from Okabe–Ito's `#0072B2`) |
| Ambient | cave noises, ambient world sound | bluish green `#009E73` |
| Voice | narrator, other spoken text | reddish purple `#CC79A7` |

The Weather blue was changed because the original Okabe–Ito value has a
contrast ratio of only ~4.0:1 against a black background, below the 4.5:1
usually recommended for text, and real-machine testing found it the hardest
of the 7 colors to read. The brightened value clears 4.5:1 contrast while
staying clearly distinguishable (CIELAB ΔE ≥ 20) from every other color in
this palette — including the Players sky blue it's closest to — under normal
vision, protanopia and deuteranopia. See
[`tools/weather-blue-calc.py`](tools/weather-blue-calc.py) for the script
that produced these numbers.

Master/Music/Records/UI sounds are left in the vanilla subtitle color, since
they are not "where did that sound come from" information.

## Requirements

- Minecraft 1.21.11 (Fabric)
- Fabric Loader ≥ 0.19.0
- Fabric API

## License

MIT — see `LICENSE`.

---

# サウンド字幕の色分け（Sound Subtitle Colors）

Minecraft バニラの字幕機能（オプション → アクセシビリティ → 字幕を表示）に、
音のカテゴリ（敵対 Mob・中立 Mob・プレイヤー・ブロック・天候・環境音・声）ごとの
色分けを追加する小さな QoL / アクセシビリティ MOD です。字幕の文字色だけを変更し、
新しい HUD やミニマップ、ゲームプレイの変更は行いません。サーバー側の読み書きも
一切行いません。

**本 MOD は AI 支援で作成されました**（Hullspark の MOD 工場レーンにおいて、
自律稼働する Claude Code エージェントが実装し、公開前に実際のゲーム画面で
動作を確認しています）。

## 色分け

色覚多様性に配慮した [Okabe–Ito 配色](https://jfly.uni-koeln.de/color/) を基に、
天候（Weather）の青だけは、本MODの字幕背景（黒の半透明）でも読めるよう
Okabe–Ito の元の値（`#0072B2`）より明るい `#0072FC` に変更しています（元の値は
黒背景とのコントラスト比が約4.0:1で、目安の4.5:1に届かなかったため。計算に
使ったスクリプトは `tools/weather-blue-calc.py`）。それ以外の
6色は Okabe–Ito の値のまま。詳細な色一覧は上の英語表を参照。

マスター・音楽・レコード・UI の音は「どこから来た音か」を示す情報ではないため、
バニラのままの色で表示されます。

## 動作要件

- Minecraft 1.21.11（Fabric）
- Fabric Loader 0.19.0 以上
- Fabric API

## ライセンス

MIT — `LICENSE` を参照してください。
