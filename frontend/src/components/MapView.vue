<script setup>
import { onMounted, onBeforeUnmount, ref, computed, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})

const mapContainer = ref(null)
const locations = ref([])
const categories = ref([])
const isLocating = ref(false)

// カテゴリフィルター
const filterCategoryId = ref(null)

// ピン追加モーダル
const showAddModal = ref(false)
const pendingLatlng = ref(null)
const addName = ref('')
const addCategoryId = ref(null)

// ピン編集モーダル
const showEditModal = ref(false)
const editingLocId = ref(null)
const editLocName = ref('')
const editLocCategoryId = ref(null)

// スマホサイドバー開閉
const sidebarOpen = ref(false)

// 住所検索
const searchQuery = ref('')
const searchResults = ref([])
const isSearching = ref(false)
const searchDone = ref(false)

async function searchAddress() {
  const q = searchQuery.value.trim()
  if (!q) return
  isSearching.value = true
  searchDone.value = false
  searchResults.value = []
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(q)}&format=json&limit=5`,
      { headers: { 'User-Agent': 'map-app/1.0' } },
    )
    if (!res.ok) throw new Error(`Nominatim failed: ${res.status}`)
    searchResults.value = await res.json()
  } catch (err) {
    console.error('住所検索に失敗:', err)
  } finally {
    isSearching.value = false
    searchDone.value = true
  }
}

function selectSearchResult(result) {
  map.flyTo([parseFloat(result.lat), parseFloat(result.lon)], 16)
  searchResults.value = []
  searchDone.value = false
}

function clearSearch() {
  searchResults.value = []
  searchDone.value = false
}

// カテゴリ追加フォーム
const showAddCategoryForm = ref(false)
const newCategoryName = ref('')
const newCategoryColor = ref('#3B82F6')

// カテゴリ編集フォーム
const editingCategoryId = ref(null)
const editCategoryName = ref('')
const editCategoryColor = ref('#3B82F6')

let map = null
const markerMap = {}

const OSAKA_STATION = [34.7024, 135.4959]
const LOC_API = `${import.meta.env.VITE_API_BASE_URL}/locations`
const CAT_API = `${import.meta.env.VITE_API_BASE_URL}/categories`

const filteredLocations = computed(() =>
  filterCategoryId.value === null
    ? locations.value
    : locations.value.filter((l) => l.categoryId === filterCategoryId.value),
)

// ---- API ----

async function fetchLocations() {
  const res = await fetch(LOC_API)
  if (!res.ok) throw new Error(`GET locations failed: ${res.status}`)
  return res.json()
}

async function fetchCategories() {
  const res = await fetch(CAT_API)
  if (!res.ok) throw new Error(`GET categories failed: ${res.status}`)
  return res.json()
}

async function postLocation(name, lat, lng, categoryId) {
  const res = await fetch(LOC_API, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, latitude: lat, longitude: lng, categoryId }),
  })
  if (!res.ok) throw new Error(`POST location failed: ${res.status}`)
  return res.json()
}

async function putLocation(id, name, categoryId) {
  const res = await fetch(`${LOC_API}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, categoryId }),
  })
  if (!res.ok) throw new Error(`PUT location failed: ${res.status}`)
  return res.json()
}

async function deleteLocation(id) {
  const res = await fetch(`${LOC_API}/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error(`DELETE location failed: ${res.status}`)
}

async function postCategory(name, color) {
  const res = await fetch(CAT_API, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, color }),
  })
  if (!res.ok) throw new Error(`POST category failed: ${res.status}`)
  return res.json()
}

async function putCategory(id, name, color) {
  const res = await fetch(`${CAT_API}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, color }),
  })
  if (!res.ok) throw new Error(`PUT category failed: ${res.status}`)
  return res.json()
}

async function deleteCategory(id) {
  const res = await fetch(`${CAT_API}/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error(`DELETE category failed: ${res.status}`)
}

// ---- マーカー ----

function coloredIcon(color) {
  return L.divIcon({
    className: '',
    html: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 25 41" width="25" height="41">
      <path fill="${color}" stroke="#fff" stroke-width="1.5" d="M12.5 0C5.6 0 0 5.6 0 12.5c0 9.4 12.5 28.5 12.5 28.5S25 21.9 25 12.5C25 5.6 19.4 0 12.5 0z"/>
      <circle cx="12.5" cy="12.5" r="5" fill="white"/>
    </svg>`,
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
  })
}

function categoryColor(categoryId) {
  return categories.value.find((c) => c.id === categoryId)?.color ?? '#3B82F6'
}

function buildPopupHtml(loc) {
  const cat = categories.value.find((c) => c.id === loc.categoryId)
  const catLabel = cat
    ? `<span style="color:${cat.color};font-weight:600;">${cat.name}</span><br>`
    : ''
  return `<b>${loc.name}</b><br>${catLabel}${loc.latitude.toFixed(5)}, ${loc.longitude.toFixed(5)}<br>
    <div style="display:flex;gap:6px;margin-top:6px;">
      <button id="edit-btn-${loc.id}" style="padding:3px 10px;background:#3b5bdb;color:#fff;border:none;border-radius:3px;cursor:pointer;">編集</button>
      <button id="delete-btn-${loc.id}" style="padding:3px 10px;background:#e53e3e;color:#fff;border:none;border-radius:3px;cursor:pointer;">削除</button>
    </div>`
}

function addMarker(loc) {
  const marker = L.marker([loc.latitude, loc.longitude], {
    icon: coloredIcon(categoryColor(loc.categoryId)),
  })
    .addTo(map)
    .bindPopup(buildPopupHtml(loc))

  marker.on('popupopen', () => {
    const delBtn = document.getElementById(`delete-btn-${loc.id}`)
    if (delBtn) delBtn.onclick = () => handleDeleteLoc(loc.id)
    const editBtn = document.getElementById(`edit-btn-${loc.id}`)
    if (editBtn) editBtn.onclick = () => openEditLocModal(loc.id)
  })

  markerMap[loc.id] = marker
}

function refreshMarker(updatedLoc) {
  const marker = markerMap[updatedLoc.id]
  if (!marker) return
  marker.setIcon(coloredIcon(categoryColor(updatedLoc.categoryId)))
  marker.setPopupContent(buildPopupHtml(updatedLoc))
}

// ---- フィルター ----

watch(filterCategoryId, (catId) => {
  for (const loc of locations.value) {
    const marker = markerMap[loc.id]
    if (!marker) continue
    if (catId === null || loc.categoryId === catId) {
      marker.addTo(map)
    } else {
      marker.remove()
    }
  }
})

// ---- ピン操作 ----

function flyToLocation(loc) {
  map.flyTo([loc.latitude, loc.longitude], 16)
  markerMap[loc.id]?.openPopup()
}

async function handleDeleteLoc(id) {
  try {
    await deleteLocation(id)
    markerMap[id]?.remove()
    delete markerMap[id]
    locations.value = locations.value.filter((l) => l.id !== id)
  } catch (err) {
    console.error('ピンの削除に失敗:', err)
  }
}

// ピン追加モーダル

function openAddModal(latlng) {
  pendingLatlng.value = latlng
  addName.value = ''
  addCategoryId.value = categories.value[0]?.id ?? null
  showAddModal.value = true
}

async function confirmAddLocation() {
  if (!pendingLatlng.value) return
  const { lat, lng } = pendingLatlng.value
  const name = addName.value.trim() || 'クリックした場所'
  try {
    const saved = await postLocation(name, lat, lng, addCategoryId.value)
    locations.value.push(saved)
    addMarker(saved)
    showAddModal.value = false
  } catch (err) {
    console.error('ピンの保存に失敗:', err)
  }
}

function cancelAddLocation() {
  showAddModal.value = false
  pendingLatlng.value = null
}

// ピン編集モーダル

function openEditLocModal(id) {
  const loc = locations.value.find((l) => l.id === id)
  if (!loc) return
  editingLocId.value = id
  editLocName.value = loc.name
  editLocCategoryId.value = loc.categoryId
  showEditModal.value = true
}

async function confirmEditLocation() {
  const id = editingLocId.value
  const name = editLocName.value.trim() || 'クリックした場所'
  try {
    const updated = await putLocation(id, name, editLocCategoryId.value)
    const idx = locations.value.findIndex((l) => l.id === id)
    if (idx !== -1) locations.value[idx] = updated
    refreshMarker(updated)
    showEditModal.value = false
  } catch (err) {
    console.error('ピンの更新に失敗:', err)
  }
}

function cancelEditLocation() {
  showEditModal.value = false
}

// ---- カテゴリ操作 ----

async function submitAddCategory() {
  const name = newCategoryName.value.trim()
  if (!name) return
  try {
    const saved = await postCategory(name, newCategoryColor.value)
    categories.value.push(saved)
    newCategoryName.value = ''
    newCategoryColor.value = '#3B82F6'
    showAddCategoryForm.value = false
  } catch (err) {
    console.error('カテゴリの追加に失敗:', err)
  }
}

function startEditCategory(cat) {
  editingCategoryId.value = cat.id
  editCategoryName.value = cat.name
  editCategoryColor.value = cat.color
  showAddCategoryForm.value = false
}

async function submitEditCategory() {
  const id = editingCategoryId.value
  const name = editCategoryName.value.trim()
  if (!name) return
  try {
    const updated = await putCategory(id, name, editCategoryColor.value)
    const idx = categories.value.findIndex((c) => c.id === id)
    if (idx !== -1) categories.value[idx] = updated
    // そのカテゴリに属するマーカーを再描画
    for (const loc of locations.value.filter((l) => l.categoryId === id)) {
      refreshMarker(loc)
    }
    editingCategoryId.value = null
  } catch (err) {
    console.error('カテゴリの更新に失敗:', err)
  }
}

async function handleDeleteCategory(id) {
  const affectedLocIds = locations.value
    .filter((l) => l.categoryId === id)
    .map((l) => l.id)
  try {
    await deleteCategory(id)
    // フィルターリセット
    if (filterCategoryId.value === id) filterCategoryId.value = null
    // カテゴリ一覧から削除
    categories.value = categories.value.filter((c) => c.id !== id)
    // locations の categoryId を null に
    locations.value = locations.value.map((l) =>
      l.categoryId === id ? { ...l, categoryId: null } : l,
    )
    // マーカー再描画
    for (const locId of affectedLocIds) {
      const loc = locations.value.find((l) => l.id === locId)
      if (loc) refreshMarker(loc)
    }
    if (editingCategoryId.value === id) editingCategoryId.value = null
  } catch (err) {
    console.error('カテゴリの削除に失敗:', err)
  }
}

// ---- 現在地 ----

function jumpToCurrentLocation() {
  if (!navigator.geolocation) {
    console.error('このブラウザはGeolocation APIに対応していません')
    return
  }
  isLocating.value = true
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      map.flyTo([pos.coords.latitude, pos.coords.longitude], 16)
      isLocating.value = false
    },
    (err) => {
      console.error('現在地の取得に失敗しました:', err.message)
      isLocating.value = false
    },
  )
}

// ---- ライフサイクル ----

onMounted(async () => {
  map = L.map(mapContainer.value).setView(OSAKA_STATION, 15)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
  }).addTo(map)

  try {
    categories.value = await fetchCategories()
  } catch (err) {
    console.error('カテゴリの取得に失敗:', err)
  }

  try {
    const data = await fetchLocations()
    locations.value = data
    data.forEach((loc) => addMarker(loc))
  } catch (err) {
    console.error('既存ロケーションの取得に失敗:', err)
  }

  map.on('click', (e) => openAddModal(e.latlng))
})

onBeforeUnmount(() => {
  map?.remove()
})
</script>

<template>
  <div class="layout">
    <!-- スマホ：サイドバー背景オーバーレイ -->
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false" />

    <!-- サイドバー -->
    <aside class="sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-header">
        <h2 class="sidebar-title">ピン一覧</h2>
        <button class="close-btn" @click="sidebarOpen = false">✕</button>
      </div>

      <!-- 住所検索 -->
      <div class="search-area">
        <div class="search-row">
          <input
            v-model="searchQuery"
            class="search-input"
            placeholder="住所・施設名で検索"
            @keyup.enter="searchAddress"
            @input="clearSearch"
          />
          <button class="search-btn" :disabled="isSearching" @click="searchAddress">
            {{ isSearching ? '検索中...' : '検索' }}
          </button>
        </div>
        <ul v-if="searchResults.length > 0" class="search-results">
          <li
            v-for="r in searchResults"
            :key="r.place_id"
            class="search-result-item"
            @click="selectSearchResult(r)"
          >{{ r.display_name }}</li>
        </ul>
        <p v-else-if="searchDone && !isSearching" class="search-empty">見つかりませんでした</p>
      </div>

      <!-- カテゴリフィルタータブ -->
      <div class="filter-area">
        <div class="filter-tabs">
          <button
            class="tab-btn"
            :class="{ active: filterCategoryId === null }"
            @click="filterCategoryId = null"
          >すべて</button>

          <div v-for="cat in categories" :key="cat.id" class="tab-group">
            <button
              class="tab-btn"
              :class="{ active: filterCategoryId === cat.id }"
              :style="
                filterCategoryId === cat.id
                  ? { background: cat.color, color: '#fff', borderColor: cat.color }
                  : { borderColor: cat.color, color: cat.color }
              "
              @click="filterCategoryId = cat.id"
            >{{ cat.name }}</button>

            <template v-if="cat.id > 2">
              <button
                class="tab-icon-btn"
                title="編集"
                @click.stop="startEditCategory(cat)"
              >✏</button>
              <button
                class="tab-icon-btn tab-delete"
                title="削除"
                @click.stop="handleDeleteCategory(cat.id)"
              >✕</button>
            </template>
          </div>
        </div>

        <!-- カテゴリ編集フォーム -->
        <div v-if="editingCategoryId !== null" class="inline-form">
          <input
            v-model="editCategoryName"
            class="text-input"
            placeholder="カテゴリ名"
            @keyup.enter="submitEditCategory"
          />
          <div class="color-row">
            <label class="color-label">色</label>
            <input type="color" v-model="editCategoryColor" class="color-input" />
            <span class="color-value">{{ editCategoryColor }}</span>
          </div>
          <div class="form-actions">
            <button class="btn-primary" @click="submitEditCategory">保存</button>
            <button class="btn-secondary" @click="editingCategoryId = null">キャンセル</button>
          </div>
        </div>
      </div>

      <!-- カテゴリ追加 -->
      <div class="add-category-section">
        <button
          class="add-category-btn"
          @click="showAddCategoryForm = !showAddCategoryForm; editingCategoryId = null"
        >＋ カテゴリ追加</button>
        <div v-if="showAddCategoryForm" class="inline-form">
          <input
            v-model="newCategoryName"
            class="text-input"
            placeholder="カテゴリ名"
            @keyup.enter="submitAddCategory"
          />
          <div class="color-row">
            <label class="color-label">色</label>
            <input type="color" v-model="newCategoryColor" class="color-input" />
            <span class="color-value">{{ newCategoryColor }}</span>
          </div>
          <div class="form-actions">
            <button class="btn-primary" @click="submitAddCategory">追加</button>
            <button class="btn-secondary" @click="showAddCategoryForm = false">キャンセル</button>
          </div>
        </div>
      </div>

      <!-- ピン一覧 -->
      <ul class="pin-list">
        <li
          v-for="loc in filteredLocations"
          :key="loc.id"
          class="pin-item"
          @click="flyToLocation(loc)"
        >
          <span class="pin-dot" :style="{ background: categoryColor(loc.categoryId) }" />
          <div class="pin-info">
            <span class="pin-name">{{ loc.name }}</span>
            <span class="pin-coords">{{ loc.latitude.toFixed(4) }}, {{ loc.longitude.toFixed(4) }}</span>
          </div>
        </li>
        <li v-if="filteredLocations.length === 0" class="pin-empty">ピンがありません</li>
      </ul>
    </aside>

    <!-- 地図エリア -->
    <div class="map-area">
      <div ref="mapContainer" class="map" />
      <button class="menu-btn" aria-label="メニューを開く" @click="sidebarOpen = true">☰</button>
      <button class="locate-btn" :disabled="isLocating" @click="jumpToCurrentLocation">
        {{ isLocating ? '取得中...' : '現在地' }}
      </button>

      <!-- ピン追加モーダル -->
      <div v-if="showAddModal" class="modal-overlay" @click.self="cancelAddLocation">
        <div class="modal">
          <h3 class="modal-title">場所を追加</h3>
          <input
            v-model="addName"
            class="text-input"
            placeholder="名前（省略可）"
            autofocus
            @keyup.enter="confirmAddLocation"
          />
          <select v-model="addCategoryId" class="select-input">
            <option :value="null">カテゴリなし</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
          </select>
          <div class="modal-actions">
            <button class="btn-primary" @click="confirmAddLocation">OK</button>
            <button class="btn-secondary" @click="cancelAddLocation">キャンセル</button>
          </div>
        </div>
      </div>

      <!-- ピン編集モーダル -->
      <div v-if="showEditModal" class="modal-overlay" @click.self="cancelEditLocation">
        <div class="modal">
          <h3 class="modal-title">場所を編集</h3>
          <input
            v-model="editLocName"
            class="text-input"
            placeholder="名前"
            @keyup.enter="confirmEditLocation"
          />
          <select v-model="editLocCategoryId" class="select-input">
            <option :value="null">カテゴリなし</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
          </select>
          <div class="modal-actions">
            <button class="btn-primary" @click="confirmEditLocation">保存</button>
            <button class="btn-secondary" @click="cancelEditLocation">キャンセル</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  width: 100vw;
  height: 100vh;
}

/* ---- サイドバー ---- */
.sidebar {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #ddd;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 14px 16px 10px;
  border-bottom: 1px solid #ddd;
  background: #f7f7f7;
}

.sidebar-title {
  margin: 0;
  font-size: 15px;
  font-weight: bold;
}

/* ---- 住所検索 ---- */
.search-area {
  padding: 10px 12px 8px;
  border-bottom: 1px solid #eee;
  position: relative;
}

.search-row {
  display: flex;
  gap: 6px;
}

.search-input {
  flex: 1;
  padding: 6px 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 13px;
  min-width: 0;
}

.search-input:focus {
  outline: none;
  border-color: #6b86e0;
}

.search-btn {
  padding: 6px 10px;
  background: #3b5bdb;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}

.search-btn:hover:not(:disabled) {
  background: #2f4bbf;
}

.search-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.search-results {
  list-style: none;
  margin: 4px 0 0;
  padding: 0;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.search-result-item {
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.4;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item:hover {
  background: #f0f4ff;
}

.search-empty {
  margin: 6px 0 0;
  font-size: 12px;
  color: #999;
}

/* フィルタータブ */
.filter-area {
  border-bottom: 1px solid #eee;
}

.filter-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  padding: 8px 10px 6px;
}

.tab-group {
  display: flex;
  align-items: center;
  gap: 2px;
}

.tab-btn {
  padding: 3px 10px;
  border: 1.5px solid #bbb;
  border-radius: 12px;
  background: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.tab-btn.active {
  background: #555;
  color: #fff;
  border-color: #555;
}

.tab-icon-btn {
  padding: 2px 5px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 11px;
  color: #888;
  border-radius: 3px;
  line-height: 1;
}

.tab-icon-btn:hover {
  background: #eee;
  color: #333;
}

.tab-delete:hover {
  background: #fee;
  color: #e53e3e;
}

/* インラインフォーム */
.inline-form {
  padding: 8px 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 7px;
  background: #fafafa;
  border-top: 1px solid #eee;
}

/* カテゴリ追加 */
.add-category-section {
  padding: 8px 12px;
  border-bottom: 1px solid #eee;
}

.add-category-btn {
  width: 100%;
  padding: 5px 0;
  background: #f0f4ff;
  border: 1.5px dashed #6b86e0;
  border-radius: 4px;
  color: #3b5bdb;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.add-category-btn:hover {
  background: #e2e9ff;
}

/* 色選択行 */
.color-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-label {
  font-size: 12px;
}

.color-input {
  width: 32px;
  height: 26px;
  padding: 0;
  border: 1px solid #ccc;
  border-radius: 3px;
  cursor: pointer;
}

.color-value {
  font-size: 11px;
  color: #666;
}

.form-actions {
  display: flex;
  gap: 6px;
}

/* ピン一覧 */
.pin-list {
  list-style: none;
  margin: 0;
  padding: 0;
  overflow-y: auto;
  flex: 1;
}

.pin-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 9px 14px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}

.pin-item:hover {
  background: #f5f7ff;
}

.pin-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 3px;
}

.pin-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.pin-name {
  font-size: 13px;
  font-weight: 500;
  word-break: break-all;
}

.pin-coords {
  font-size: 10px;
  color: #999;
  margin-top: 2px;
}

.pin-empty {
  padding: 16px;
  color: #aaa;
  font-size: 13px;
}

/* ---- 地図エリア ---- */
.map-area {
  position: relative;
  flex: 1;
}

.map {
  width: 100%;
  height: 100%;
}

.locate-btn {
  position: absolute;
  bottom: 32px;
  right: 16px;
  z-index: 1000;
  padding: 8px 14px;
  background: #fff;
  border: 2px solid #555;
  border-radius: 4px;
  font-size: 13px;
  font-weight: bold;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

.locate-btn:hover:not(:disabled) {
  background: #f0f0f0;
}

.locate-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ---- モーダル共通 ---- */
.modal-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.modal {
  background: #fff;
  border-radius: 8px;
  padding: 24px 20px 20px;
  width: 280px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.modal-title {
  margin: 0;
  font-size: 15px;
  font-weight: bold;
}

.modal-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

/* ---- 共通部品 ---- */
.text-input {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 13px;
  box-sizing: border-box;
}

.text-input:focus {
  outline: none;
  border-color: #6b86e0;
}

.select-input {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 13px;
  background: #fff;
  box-sizing: border-box;
}

.btn-primary {
  padding: 6px 16px;
  background: #3b5bdb;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
}

.btn-primary:hover {
  background: #2f4bbf;
}

.btn-secondary {
  padding: 6px 16px;
  background: #fff;
  color: #555;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
}

.btn-secondary:hover {
  background: #f5f5f5;
}

/* ---- ハンバーガー / クローズボタン（PC では非表示） ---- */
.menu-btn {
  display: none;
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 1001;
  padding: 9px 13px;
  background: rgba(255, 255, 255, 0.95);
  border: 1.5px solid #bbb;
  border-radius: 6px;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.menu-btn:hover {
  background: #f0f0f0;
}

.close-btn {
  display: none;
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #555;
  padding: 4px 8px;
  border-radius: 4px;
  line-height: 1;
  flex-shrink: 0;
}

.close-btn:hover {
  background: #e0e0e0;
}

/* スマホ：背景オーバーレイ */
.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 999;
}

/* ---- スマホ（〜767px）---- */
@media (max-width: 767px) {
  /* サイドバーを画面外に退避 → スライドイン */
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    width: 280px;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.28s ease;
    box-shadow: 4px 0 16px rgba(0, 0, 0, 0.2);
  }

  .sidebar.open {
    transform: translateX(0);
  }

  /* ヘッダーを flex にして ✕ ボタンを右寄せ */
  .sidebar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 12px 10px;
  }

  /* 地図エリアをフルスクリーンに */
  .map-area {
    width: 100%;
    height: 100vh;
  }

  /* ☰ ボタンを表示 */
  .menu-btn {
    display: block;
  }

  /* ✕ ボタンを表示 */
  .close-btn {
    display: block;
  }

  /* 現在地ボタン：タップしやすいサイズに */
  .locate-btn {
    padding: 12px 20px;
    font-size: 15px;
    bottom: 24px;
    right: 12px;
  }

  /* サイドバー内フォント・余白の調整 */
  .sidebar-title {
    font-size: 16px;
  }

  .search-input {
    font-size: 15px;
    padding: 8px 10px;
  }

  .search-btn {
    padding: 8px 14px;
    font-size: 13px;
  }

  .tab-btn {
    padding: 5px 12px;
    font-size: 13px;
  }

  .tab-icon-btn {
    font-size: 13px;
    padding: 3px 7px;
  }

  .add-category-btn {
    padding: 8px 0;
    font-size: 14px;
  }

  .pin-item {
    padding: 12px 14px;
  }

  .pin-name {
    font-size: 14px;
  }

  .pin-coords {
    font-size: 11px;
  }

  .text-input,
  .select-input {
    font-size: 15px;
    padding: 8px 10px;
  }

  .btn-primary,
  .btn-secondary {
    padding: 9px 18px;
    font-size: 14px;
  }
}
</style>
