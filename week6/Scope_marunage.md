# MARUNAGE

## 何をどこまで実装するか (雇用主側)
今回は事前に入力しておいたシフト希望をもとにシフトを自動生成する
- とりあえず1週間分
    - 1日朝昼夜3時間帯×3ポジション×7日の合計63コマを調整
    - ポジションは3つ（キッチン・ホール・レジ）
    - 各ポジションにつき1人ずつ
    - 事前に21人分csvで用意しておく(本来は入力フォームを用意してデータベースに格納するが，今回は簡易実装)
        - 勤務可能なポジションを7人ずつ割り当てる
    - 最悪ポジションなしで
- ホーム画面と最終シフト確認画面の2場面
    - 雇用主が確認するところまで

## 役割分担

- バウンダリ: 長尾悠生，岩﨑玲
    - メニュー画面
    - 各種ページの工事中リンク
    - 確認画面
- コントローラ: 中原滉希
    - シフト生成機能の呼び出し

        ホーム画面から「シフト呼び出し」ボタンを押すと起動するようにする．

- サービス・リポジトリ: 田口陽介，堀部青夏
    - シフトを自動生成する機能
    - CSVの用意
    - 生成されたシフトを画面に表示する機能

- プレゼン: 中村亮
