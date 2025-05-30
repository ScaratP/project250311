# 課程管理應用程式 (Course Management App)

這是一個專為學生設計的 Android 課程管理應用程式，提供課程表管理、請假申請、通知提醒和筆記功能。

## 功能特色

### 1. 課程表管理 (Schedule)
- 自動從學校系統同步課程資料
- 視覺化的課程表界面
- 課程詳細資訊查看
- 支援修改課程地點

### 2. 通知提醒系統 (Notice)
- 課程開始前自動通知
- 可設定提前提醒時間
- 支援自定義通知內容

### 3. 請假系統 (Leave)
- 整合學校請假系統
- 記錄請假歷史
- 支援不同類型的請假申請
- 可查看請假統計資料

### 4. 筆記功能 (Notes)
- 支援富文本編輯
- 可變更字體大小、顏色和樣式
- 支援日期和關鍵字搜尋
- 便利貼風格的筆記列表

## 技術特點

- 使用 Kotlin 和 Jetpack Compose 開發的現代 UI
- 採用 MVVM 架構模式
- 使用 Room 資料庫進行本地資料存儲
- 網頁爬蟲整合學校系統
- LiveData 和 Coroutines 實現響應式 UI
- Material Design 3 設計風格

## 系統需求

- Android 12.0 (API 31) 或更高版本
- 網路連接（用於同步學校資料）
- 通知權限（用於課程提醒）

## 安裝方式

1. 從 GitHub Releases 下載最新的 APK 檔案
2. 在 Android 裝置上開啟檔案並安裝
3. 首次啟動時需授予必要權限

## 使用方法

### 課程表同步
1. 點擊主頁面的 "Schedule" 選項
2. 輸入學校系統帳號密碼
3. 等待同步完成，課程表將自動顯示

### 設定課程提醒
1. 在課程表中點擊特定課程
2. 開啟通知開關
3. 系統將在課程開始前 10 分鐘提醒

### 請假申請
1. 點擊主頁面的 "Leave" 選項
2. 選擇 "我要請假"
3. 登入學校請假系統
4. 選擇請假類型、課程和時數

### 筆記功能
1. 點擊主頁面的 "Notes" 選項
2. 點擊右下角 "+" 按鈕新增筆記
3. 使用編輯工具修改文字格式
4. 點擊 "SAVE" 儲存筆記

## 開發者資訊

此應用程式使用以下主要框架和函式庫：
- Kotlin 2.1.0
- Jetpack Compose
- Room Database
- JSoup (網頁爬蟲)
- Material 3 Components
- GSON (JSON 解析)

## 免責聲明

本應用程式為學生專案，與學校官方系統無直接關聯。使用者需自行確保帳號安全。
